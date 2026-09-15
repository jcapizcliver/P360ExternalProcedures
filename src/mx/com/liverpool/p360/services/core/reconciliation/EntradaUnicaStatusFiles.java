package mx.com.liverpool.p360.services.core.reconciliation;

import com.mongodb.*;
import com.mongodb.client.*;
import org.bson.Document;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;

/** Status-only disk pipeline. Source reads only; publication uses the existing guarded publisher. */
public final class EntradaUnicaStatusFiles {
 static void line(Writer w,String... parts)throws IOException {
  for(String s:parts)if(s.indexOf('\t')>=0||s.indexOf('\n')>=0||s.indexOf('\r')>=0)throw new IOException("Unexpected tab/newline in source field");
  w.write(String.join("\t",parts));w.write('\n');
 }
 static void sqlExport(String mode,Path output)throws Exception {
  String live=EntradaUnicaSync.LIVE;
  // Deliberately no joins or Oracle sorts: sequential narrow exports, joined on disk.
  String q=mode.equals("revisions")?
   "select /*+ full(a) no_parallel(a) */ a.\"ID\",a.\"EntityID\",a.\"Identifier\" from \"ArticleRevision\" a where a.\"RevisionID\"=1 and a.\"EntityID\" in (1000,1100) and a.\"DeletionTimestamp\"="+live:
   "select /*+ full(d) no_parallel(d) */ d.\"ArticleRevisionID\",d.\"CurrentStatus\" from \"ArticleDetail\" d where d.\"DeletionTimestamp\"="+live;
  Path tmp=Path.of(output+".partial");long n=0;
  try(Connection db=new QuickJdbcConnectionManager().openConnection(true)) {
   db.setNetworkTimeout(EntradaUnicaSync.NET,90000);
   try(PreparedStatement s=db.prepareStatement(q);BufferedWriter w=Files.newBufferedWriter(tmp)) {
    s.setFetchSize(10000);s.setQueryTimeout(600);
    try(ResultSet r=s.executeQuery()) {int cols=r.getMetaData().getColumnCount();
     while(r.next()){String[] values=new String[cols];for(int i=0;i<cols;i++)values[i]=EntradaUnicaSync.str(r.getString(i+1));line(w,values);if(++n%500000==0){w.flush();System.out.println(mode+" rows="+n+" at="+Instant.now());}}
    }
   }
  }
  Files.move(tmp,output);System.out.println("DONE "+mode+" rows="+n);
 }
 static void mongoExport(String collection,Path output)throws Exception {
  String uri=System.getenv("P360_ENTRADA_MONGO_URI");String key=collection.equals("products")?"proposalId":"variantId";String entity=collection.equals("products")?"1100":"1000";
  Path tmp=Path.of(output+".partial");long n=0,missing=0;
  try(MongoClient client=MongoClients.create(MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri)).applicationName("P360-status-file-export")
   .applyToClusterSettings(b->b.serverSelectionTimeout(20,TimeUnit.SECONDS)).applyToSocketSettings(b->b.connectTimeout(10,TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS)).build());BufferedWriter w=Files.newBufferedWriter(tmp)) {
   MongoCollection<Document> c=client.getDatabase("BD_CAT_PRODUCTS").getCollection(collection);
   // A streaming cursor: no secondary lookup per document and no database sort.
   try(MongoCursor<Document> cursor=c.find().projection(new Document(key,1).append("status.internal",1).append("_id",0)).batchSize(10000).iterator()) {
    while(cursor.hasNext()){Document d=cursor.next();String id=EntradaUnicaSync.str(d.get(key));if(id.isEmpty())missing++;else line(w,entity+"|"+id,EntradaUnicaSync.observed(d,"currentStatus"));if(++n%500000==0){w.flush();System.out.println(collection+" rows="+n+" at="+Instant.now());}}
   }
  }
  Files.move(tmp,output);System.out.println("DONE "+collection+" rows="+n+" missingIdentifier="+missing);
 }
 static Set<String> acknowledged(Path prior,Path expected)throws Exception {
  Map<String,Document> byId=new LinkedHashMap<>();
  try(BufferedReader r=Files.newBufferedReader(prior.resolve("expected.jsonl"))){String l;while((l=r.readLine())!=null){Document d=Document.parse(l);if(EntradaUnicaSync.str(d.get("messageId")).isEmpty())throw new IOException("Unconfirmed prior expected record");byId.put(d.get("entity")+"|"+d.getString("id"),d);}}
  // Require all saved plans to have an ACK. Never automatically replay uncertain sends.
  try(var files=Files.list(prior)){for(Path p:files.filter(x->x.toString().endsWith("-planned.jsonl")).toList()){
   Path ack=p.resolveSibling(p.getFileName().toString().replace("-planned.jsonl","-post.ack"));if(!Files.exists(ack)||Files.readString(ack).isBlank())throw new IOException("Unconfirmed prior batch: "+p.getFileName());
   for(String l:Files.readAllLines(p)){Document d=Document.parse(l);String k=d.get("entity")+"|"+d.getString("id");if(!byId.containsKey(k))throw new IOException("ACK missing from prior ledger: "+k);}
  }}
  try(BufferedWriter w=Files.newBufferedWriter(expected)){for(Document d:byId.values()){w.write(d.toJson());w.newLine();}}
  return byId.keySet();
 }
 static void send(Path directory,Path candidates,Path prior,boolean publish)throws Exception {
  if(!EntradaUnicaSync.STATUS_ONLY)throw new IllegalArgumentException("Requires currentStatusOnly=true");
  Files.createDirectory(directory);
  Set<String> acknowledged=acknowledged(prior,directory.resolve("expected.jsonl"));
  try(EntradaUnicaSync app=new EntradaUnicaSync(directory,publish,Long.MAX_VALUE);
      BufferedReader input=Files.newBufferedReader(candidates);
      BufferedWriter ledger=Files.newBufferedWriter(directory.resolve("expected.jsonl"),StandardOpenOption.APPEND);
      BufferedWriter p=Files.newBufferedWriter(directory.resolve("products_initial.csv"));BufferedWriter a=Files.newBufferedWriter(directory.resolve("articles_initial.csv"))) {
   EntradaUnicaSync.csv(p,"ID","STATUS","REASON");EntradaUnicaSync.csv(a,"ID","STATUS","REASON");
   List<Long> revisions=new ArrayList<>();long already=0;String l;
   while((l=input.readLine())!=null){String[] v=l.split("\t",-1);if(acknowledged.contains(v[0])){already++;continue;}revisions.add(Long.parseLong(v[1]));
    if(revisions.size()==EntradaUnicaSync.PAGE){app.comparePage(app.rows(revisions),ledger,p,a);revisions.clear();}
   }
   if(!revisions.isEmpty())app.comparePage(app.rows(revisions),ledger,p,a);
   ledger.flush();p.flush();a.flush();
   System.out.println("SEND_DONE new="+app.sent+" previous="+acknowledged.size()+" excludedFromReplay="+already);
   if(publish){Files.writeString(directory.resolve("waiting-until.txt"),Instant.now().plusSeconds(600).toString());System.out.println("WAIT_VERIFY seconds=600");Thread.sleep(600000);app.verify();}
   Files.writeString(directory.resolve("complete.json"),new Document("finished",Instant.now().toString()).append("newSent",app.sent).append("priorConfirmed",acknowledged.size()).append("revalidated",app.processed).toJson());
  }
 }
 public static void main(String[] args)throws Exception {
  switch(args[0]){
   case "revisions","detail"->sqlExport(args[0],Path.of(args[1]));
   case "products","variants"->mongoExport(args[0],Path.of(args[1]));
   case "labels"->{try(EntradaUnicaSync app=new EntradaUnicaSync(Path.of(args[1]),false,1)){Files.writeString(Path.of(args[1]),new Document(new LinkedHashMap<>(app.status)).toJson());}}
   case "send","audit"->send(Path.of(args[1]),Path.of(args[2]),Path.of(args[3]),args[0].equals("send"));
   default->throw new IllegalArgumentException("Unknown phase");
  }
 }
}
