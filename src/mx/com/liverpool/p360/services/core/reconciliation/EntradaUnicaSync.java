package mx.com.liverpool.p360.services.core.reconciliation;

import com.mongodb.*;
import com.mongodb.client.*;
import org.bson.Document;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import mx.com.liverpool.p360.services.core.*;

/** Bounded read-only P360/Mongo comparison. Updates go exclusively through existing Pub/Sub contract. */
public final class EntradaUnicaSync implements AutoCloseable {
 static final String LIVE="timestamp '9999-12-31 00:00:00'";
 static final int PAGE=300;
 static final List<String> PRODUCT=List.of("currentStatus","prevStatus","sku","ean","template","direction","section","itemGroup","name");
 static final List<String> ARTICLE=List.of("currentStatus","prevStatus","sku","ean","colour","size");
 static final ExecutorService NET=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"entrada-sync-timeout");t.setDaemon(true);return t;});
 final Connection db;
 final MongoClient mongo;
 final MongoDatabase mdb;
 final RESTWrapper rest=new RESTWrapper();
 final Map<String,String> status=new HashMap<>();
 final Map<Long,String[]> lookups=new LinkedHashMap<>();
 final Path dir;
 final boolean send;
 final long maxRows;
 final Map<Long,String> characteristicIds=new LinkedHashMap<>();
 PubSubGCP pub,putPub;
 long processed,sent,changed;
 long taxonomy;
 record Row(long revision,String id,int entity,Map<String,String> values,Map<String,String> wire,Set<String> parents,Set<String> errors){}
 EntradaUnicaSync(Path dir,boolean send,long maxRows)throws Exception{
  this.dir=dir;this.send=send;this.maxRows=maxRows;
  db=new QuickJdbcConnectionManager().openConnection(true);db.setNetworkTimeout(NET,90000);
  String uri=System.getenv("P360_ENTRADA_MONGO_URI");if(uri==null||uri.isBlank())throw new IllegalArgumentException("P360_ENTRADA_MONGO_URI required");
  mongo=MongoClients.create(MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri))
   .applicationName("P360-EntradaUnica-Sync").applyToClusterSettings(b->b.serverSelectionTimeout(20,TimeUnit.SECONDS))
   .applyToSocketSettings(b->b.connectTimeout(10,TimeUnit.SECONDS).readTimeout(45000,TimeUnit.MILLISECONDS))
   .applyToConnectionPoolSettings(b->b.maxSize(3)).build());
  mdb=mongo.getDatabase("BD_CAT_PRODUCTS");
  for(String e:List.of("Enum.Status","Enum.ProductStatus")){
   JSONObject r=rest.getRw().makeRequest("GET","/enum/"+e,Map.of(),null);
   if(r==null||r.optJSONArray("entries")==null)throw new IOException("Cannot load "+e);
   for(int i=0;i<r.getJSONArray("entries").length();i++){JSONObject v=r.getJSONArray("entries").getJSONObject(i);status.put(e+":"+v.getString("key"),v.getString("label"));}
  }
  try(PreparedStatement p=sql("select \"CharacteristicID\",\"Identifier\" from PIM_MAIN.\"CharacteristicRevision\" where \"RevisionID\"=1 and \"DeletionTimestamp\"="+LIVE+" and \"Identifier\" in (N'SKU',N'MainBarCode',N'MainBarCodeS4H',N'ColoursLiverpoolAtt',N'TamanoUnico',N'SizeVaD')");ResultSet r=p.executeQuery()){while(r.next())characteristicIds.put(r.getLong(1),r.getString(2));}
  try(PreparedStatement p=sql("select \"StructureID\" from PIM_MAIN.\"StructureRevision\" where \"Identifier\"=N'PrimaryProductTaxonomy' and \"RevisionID\"=1 and \"DeletionTimestamp\"="+LIVE);ResultSet r=p.executeQuery()){
   if(!r.next())throw new IOException("PrimaryProductTaxonomy absent");taxonomy=r.getLong(1);if(r.next())throw new IOException("Ambiguous taxonomy");
  }
 }
 PreparedStatement sql(String text,Object... args)throws SQLException{
  PreparedStatement p=db.prepareStatement(text);p.setQueryTimeout(60);p.setFetchSize(500);
  for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;
 }
 static String str(Object v){return v==null||v==JSONObject.NULL?"":String.valueOf(v).trim();}
 static Map<String,String> strings(Document d){Map<String,String> out=new LinkedHashMap<>();if(d!=null)d.forEach((k,v)->out.put(k,str(v)));return out;}
 static String in(Collection<?> ids){return String.join(",",Collections.nCopies(ids.size(),"?"));}
 static List<String> fields(int entity){return entity==1100?PRODUCT:ARTICLE;}
 static String kind(int entity){return entity==1100?"products":"variants";}
 static String key(int entity){return entity==1100?"proposalId":"variantId";}
 static Object at(Document d,String path){Object o=d;for(String k:path.split("\\.")){if(!(o instanceof Map))return null;o=((Map<?,?>)o).get(k);}return o;}
 static String observed(Document d,String f){
  return switch(f){
   case "currentStatus"->str(at(d,"status.internal"));case "prevStatus"->str(at(d,"status.previous"));
   case "ean"->str(d.get("upcEan"));case "template"->str(at(d,"template.identifier"));
   case "direction"->str(d.get("address"));
   case "section"->{String x=str(at(d,"section.idLevel"));yield x.isEmpty()?str(at(d,"section.description")):x.replaceFirst("-L[0-9].*$","");}
   case "name"->str(d.get("nameProduct"));default->str(d.get(f));
  };
 }
 String enumLabel(int entity,String value,Set<String> errors){
  if(value.isBlank())return "";String s=status.get((entity==1100?"Enum.ProductStatus":"Enum.Status")+":"+value);
  if(s==null){errors.add("UNKNOWN_STATUS:"+value);return "";}return s;
 }
 void lookupLoad(Set<Long> ids)throws Exception{
  ids.removeAll(lookups.keySet());ids.remove(0L);if(ids.isEmpty())return;
  List<Long> all=new ArrayList<>(ids);
  for(int start=0;start<all.size();start+=PAGE){
   List<Long> part=all.subList(start,Math.min(start+PAGE,all.size()));
   try(PreparedStatement p=sql("select v.\"LookupValueID\",v.\"Code\",l.\"Name\" from PIM_MAIN.\"LookupValueRevision\" v left join PIM_MAIN.\"LookupValueLang\" l on l.\"LookupValueRevisionID\"=v.\"ID\" and l.\"LanguageID\"=10 and l.\"DeletionTimestamp\"="+LIVE+" where v.\"RevisionID\"=1 and v.\"DeletionTimestamp\"="+LIVE+" and v.\"LookupValueID\" in ("+in(part)+")",part.toArray());ResultSet r=p.executeQuery()){
    while(r.next())lookups.put(r.getLong(1),new String[]{str(r.getString(2)),str(r.getString(3))});
   }
  }
 }
 String lv(String id,int n){if(id==null||id.isBlank())return "";String[] v=lookups.get(Long.parseLong(id));return v==null?"":v[n];}
 Map<Long,Row> rows(List<Long> ids)throws Exception{
  Map<Long,Row> out=new LinkedHashMap<>();if(ids.isEmpty())return out;
  String sql="select /*+ leading(a) use_nl(d) index(a \"PK_ArticleRevision\") index(d \"XAK1_ArticleDetail\") */ a.\"ID\",a.\"Identifier\",a.\"EntityID\",d.\"CurrentStatus\",d.\"Res_Int_03\",d.\"Res_Int_02\",d.\"EAN\" from \"ArticleRevision\" a join \"ArticleDetail\" d on d.\"ArticleRevisionID\"=a.\"ID\" and d.\"DeletionTimestamp\"="+LIVE+" where a.\"ID\" in ("+in(ids)+") and a.\"RevisionID\"=1 and a.\"EntityID\" in (1000,1100) and a.\"DeletionTimestamp\"="+LIVE;
  try(PreparedStatement p=sql(sql,ids.toArray());ResultSet r=p.executeQuery()){
   while(r.next()){
    long id=r.getLong(1);if(out.containsKey(id)){out.get(id).errors.add("MULTIPLE_DETAIL");continue;}
    Set<String> errors=new LinkedHashSet<>();Map<String,String> v=new LinkedHashMap<>();int entity=r.getInt(3);
    v.put("currentStatus",enumLabel(entity,str(r.getString(4)),errors));v.put("prevStatus",enumLabel(entity,str(r.getString(5)),errors));
    v.put("sku",str(r.getString(6)));v.put("ean",str(r.getString(7)));
    out.put(id,new Row(id,r.getString(2),entity,v,new LinkedHashMap<>(),new LinkedHashSet<>(),errors));
   }
  }
  Set<Long> lookupIds=new HashSet<>();Map<Long,String[]> dom=new HashMap<>();
  try(PreparedStatement p=sql("select /*+ index(x \"XAK1_ArticleDomain\") */ x.\"ArticleRevisionID\",x.\"Res_Int_01\",x.\"Res_Int_02\",x.\"Res_Int_03\",x.\"Res_Int_04\" from \"ArticleDomain\" x where x.\"ArticleRevisionID\" in ("+in(ids)+") and x.\"DeletionTimestamp\"="+LIVE,ids.toArray());ResultSet r=p.executeQuery()){
   while(r.next()){long rev=r.getLong(1);Row row=out.get(rev);if(row==null)continue;String[] a={str(r.getString(2)),str(r.getString(3)),str(r.getString(4)),str(r.getString(5))};if(dom.containsKey(rev)&&!Arrays.equals(dom.get(rev),a))row.errors.add("MULTIPLE_DOMAIN");dom.put(rev,a);for(String s:a)if(!s.isEmpty())lookupIds.add(Long.parseLong(s));}
  }
  lookupLoad(lookupIds);
  for(Row r:out.values()){String[] a=dom.get(r.revision);if(a==null)continue;
   if(r.entity==1100){r.values.put("direction",lv(a[0],0));r.wire.put("direction",lv(a[0],1));r.values.put("section",lv(a[1],0));r.wire.put("section",lv(a[1],1));
    boolean s4=a[2].isEmpty();r.values.put("itemGroup",lv(s4?a[3]:a[2],1));r.wire.put("itemGroupKey",s4?"EXTWG_S4H":"MATKLLOV");
   }else{r.values.put("size",lv(a[0],1));r.values.put("colour",lv(a[1],1));}
  }
  try(PreparedStatement p=sql("select /*+ index(l IX_ARTLANG_TUNE_01) */ l.\"ArticleRevisionID\",l.\"Res_Text250_01\" from \"ArticleLang\" l where l.\"ArticleRevisionID\" in ("+in(ids)+") and l.\"LanguageID\"=10 and l.\"DeletionTimestamp\"="+LIVE,ids.toArray());ResultSet r=p.executeQuery()){
   while(r.next()){Row row=out.get(r.getLong(1));if(row!=null&&row.entity==1100)putUnique(row,"name",str(r.getString(2)));}
  }
  try(PreparedStatement p=sql("select /*+ index(s \"XAK1_ArticleStructureMap\") */ s.\"ArticleRevisionID\",s.\"StructureGroupIdentifier\" from \"ArticleStructureMap\" s where s.\"ArticleRevisionID\" in ("+in(ids)+") and s.\"StructureID\"=? and s.\"DeletionTimestamp\"="+LIVE,concat(ids,taxonomy));ResultSet r=p.executeQuery()){
   while(r.next()){Row row=out.get(r.getLong(1));if(row!=null&&row.entity==1100)putUnique(row,"template",str(r.getString(2)));}
  }
  try(PreparedStatement p=sql("select /*+ index(x \"XIE3_ArticleReference\") */ x.\"ArticleRevisionID\",x.\"RefExtArtIdentifier\" from \"ArticleReference\" x where x.\"ArticleRevisionID\" in ("+in(ids)+") and x.\"RefEntityID\"=1100 and x.\"DeletionTimestamp\"="+LIVE,ids.toArray());ResultSet r=p.executeQuery()){
   while(r.next()){Row row=out.get(r.getLong(1));if(row!=null&&row.entity==1000&&!str(r.getString(2)).isEmpty())row.parents.add(r.getString(2));}
  }

  List<Long> fallback=out.values().stream().filter(r->(r.entity==1100?List.of("sku","ean"):List.of("sku","ean","colour","size")).stream().anyMatch(f->str(r.values.get(f)).isEmpty())).map(Row::revision).toList();
  if(!fallback.isEmpty()&&!characteristicIds.isEmpty()){
   Map<Long,Map<String,Set<String>>> values=new HashMap<>();
   List<Object[]> raw=new ArrayList<>();Set<Long> valueLookups=new HashSet<>();
   String q="select /*+ index(cv \"XAK1_ArticleCharactValue\") */ cv.\"ArticleRevisionID\",cv.\"CharacteristicID\",cv.\"Value\",cv.\"LookupValueID\" from \"ArticleCharactValue\" cv where cv.\"ArticleRevisionID\" in ("+in(fallback)+") and cv.\"DeletionTimestamp\"="+LIVE+" and cv.\"CharacteristicID\" in ("+in(characteristicIds.keySet())+")";
   List<Object> args=new ArrayList<>(fallback);args.addAll(characteristicIds.keySet());
   try(PreparedStatement p=sql(q,args.toArray());ResultSet r=p.executeQuery()){while(r.next()){long lid=r.getLong(4);if(lid!=0)valueLookups.add(lid);raw.add(new Object[]{r.getLong(1),r.getLong(2),str(r.getString(3)),lid});}}
   lookupLoad(valueLookups);
   for(Object[] a:raw){String value=(String)a[2];if(value.isEmpty())value=lv(String.valueOf(a[3]),1);if(!value.isEmpty())values.computeIfAbsent((Long)a[0],x->new HashMap<>()).computeIfAbsent(characteristicIds.get((Long)a[1]),x->new HashSet<>()).add(value);}
   for(Row r:out.values())for(String f:List.of("sku","ean","colour","size")){
    if(!str(r.values.get(f)).isEmpty())continue;
    List<String> keys=switch(f){case "sku"->List.of("SKU");case "ean"->List.of("MainBarCode","MainBarCodeS4H");case "colour"->List.of("ColoursLiverpoolAtt");default->List.of("TamanoUnico","SizeVaD");};
    Set<String> vals=new HashSet<>();for(String k:keys)vals.addAll(values.getOrDefault(r.revision,Map.of()).getOrDefault(k,Set.of()));
    if(vals.size()==1)r.values.put(f,vals.iterator().next());else if(vals.size()>1)r.errors.add("AMBIGUOUS_CHARACTERISTIC:"+f);
   }
  }
  return out;
 }
 static Object[] concat(List<Long> ids,long last){List<Object> a=new ArrayList<>(ids);a.add(last);return a.toArray();}
 static void putUnique(Row r,String key,String value){if(value.isEmpty())return;String old=r.values.putIfAbsent(key,value);if(old!=null&&!old.equals(value))r.errors.add("MULTIPLE_"+key);}
 Map<String,List<Document>> documents(int entity,Collection<String> ids)throws Exception{
  Map<String,List<Document>> result=new HashMap<>();if(ids.isEmpty())return result;
  Document projection=new Document(key(entity),1).append("status",1).append("sku",1).append("upcEan",1).append("template.identifier",1).append("address",1).append("section",1).append("itemGroup",1).append("nameProduct",1).append("colour",1).append("size",1);
  try(MongoCursor<Document> c=mdb.getCollection(kind(entity)).find(new Document(key(entity),new Document("$in",ids))).projection(projection).batchSize(PAGE).maxTime(30,TimeUnit.SECONDS).iterator()){
   while(c.hasNext()){Document d=c.next();result.computeIfAbsent(str(d.get(key(entity))),k->new ArrayList<>()).add(d);}
  }return result;
 }
 static boolean equal(Row r,Document d,String f){
  String wanted=str(r.values.get(f)),actual=observed(d,f);
  if(wanted.equals(actual))return true;
  if(f.equals("direction")||f.equals("section")){if(actual.equals(r.wire.get(f)))return true;return code(wanted).equals(code(actual));}
  return false;
 }
 static String code(String v){return v.replaceFirst("^(?:SB)?(0*[0-9]+)(?:[ -].*)?$","$1").replaceFirst("^0+(?!$)","");}
 static List<String> diff(Row r,Document d){List<String> out=new ArrayList<>();for(String f:fields(r.entity))if(!equal(r,d,f))out.add(f);return out;}
 static JSONObject payload(Row r,List<String> changes,String parent){
  JSONObject obj=new JSONObject().put(r.entity==1100?"proposalId":"variantId",r.id);
  for(String f:changes){String v=str(r.values.get(f));if(v.isEmpty())continue;
   String k=switch(f){case "prevStatus"->"previousStatus";case "ean"->"MainBarCode";case "sku"->"SKU";case "colour"->"ColoursLiverpoolAtt";case "size"->"TamanoUnico";case "direction"->"Direction";case "section"->"Section";case "name"->"ProductName";case "itemGroup"->r.wire.getOrDefault("itemGroupKey","MATKLLOV");default->f;};
   if(r.entity==1100&&Set.of("sku","ean","name","direction","section","itemGroup").contains(f)){
    String section=Set.of("sku","ean","name").contains(f)?"header":"basicData";
    if(!obj.has(section))obj.put(section,new JSONObject());
    obj.getJSONObject(section).put(k,r.wire.getOrDefault(f,v));
   }else obj.put(k,v);
  }
  if(r.entity==1000)return new JSONObject().put("proposalId",parent).put("variants",new JSONArray().put(obj));return obj;
 }
 static void mergePayload(Map<String,JSONObject> batch,JSONObject item){
  String id=item.getString("proposalId");JSONObject previous=batch.putIfAbsent(id,item);if(previous==null)return;
  for(Object rawKey:item.keySet()){String key=String.valueOf(rawKey);if(!key.equals("proposalId")){
   if(key.equals("variants")&&previous.has(key)){JSONArray src=item.getJSONArray(key),dest=previous.getJSONArray(key);for(int i=0;i<src.length();i++)dest.put(src.get(i));}
   else previous.put(key,item.get(key));
  }}
 }
 static void csv(Writer w,String... cells)throws IOException{for(int i=0;i<cells.length;i++){if(i>0)w.write(',');w.write('"');w.write(str(cells[i]).replace("\"","\"\""));w.write('"');}w.write("\r\n");}
 void comparePage(Map<Long,Row> rows,BufferedWriter changes,Writer products,Writer variants)throws Exception{
  Map<Integer,Map<String,List<Document>>> docs=new HashMap<>();
  for(int entity:List.of(1100,1000))docs.put(entity,documents(entity,rows.values().stream().filter(r->r.entity==entity).map(Row::id).toList()));
  Set<String> parentIds=new HashSet<>();for(Row r:rows.values())parentIds.addAll(r.parents);
  Map<String,List<Document>> parents=new HashMap<>();
  List<String> pl=new ArrayList<>(parentIds);for(int i=0;i<pl.size();i+=PAGE)parents.putAll(documents(1100,pl.subList(i,Math.min(i+PAGE,pl.size()))));
  Map<String,JSONObject> grouped=new LinkedHashMap<>();JSONArray namesBatch=new JSONArray();List<Document> expected=new ArrayList<>();
  for(Row r:rows.values()){processed++;Writer report=r.entity==1100?products:variants;
   List<Document> found=docs.get(r.entity).getOrDefault(r.id,List.of());
   if(!r.errors.isEmpty()){csv(report,r.id,"AMBIGUOUS_P360",String.join(";",r.errors));continue;}
   if(found.isEmpty()){csv(report,r.id,"MISSING_MONGO","Sin documento; no se crea registro incompleto");continue;}
   if(found.size()!=1){csv(report,r.id,"DUPLICATE_MONGO","MÃ¡s de un documento para el ID");continue;}
   List<String> diffs=diff(r,found.get(0));if(diffs.isEmpty()){csv(report,r.id,"OK","");continue;}changed++;
   List<String> updates=diffs.stream().filter(f->!str(r.values.get(f)).isEmpty()&&!f.equals("itemGroup")).toList();
   String parent="";
   if(r.entity==1000){List<String> valid=r.parents.stream().filter(id->parents.getOrDefault(id,List.of()).size()==1).toList();if(valid.size()!=1){csv(report,r.id,"AMBIGUOUS_PARENT","No hay un padre Ãºnico en Mongo");continue;}parent=valid.get(0);}
   if(updates.isEmpty()){csv(report,r.id,diffs.contains("itemGroup")?"MAPPING_REVIEW":"SOURCE_EMPTY",String.join(";",diffs));continue;}
   Document actual=new Document();for(String field:fields(r.entity))actual.append(field,observed(found.get(0),field));
   Document expect=new Document("observed",actual).append("id",r.id).append("entity",r.entity).append("revision",r.revision).append("expected",new Document(r.values)).append("wire",new Document(r.wire)).append("fields",diffs).append("updatedFields",updates);
   List<String> postUpdates=updates.stream().filter(f->!f.equals("name")).toList();
   if(!postUpdates.isEmpty())mergePayload(grouped,payload(r,postUpdates,parent));
   if(updates.contains("name"))namesBatch.put(new JSONObject().put("proposalId",r.id).put("entityType","Generic").put("nameProduct",r.values.get("name")));
   expected.add(expect);csv(report,r.id,send?"PENDING_VERIFY":"MISMATCH",String.join(";",diffs));
  }
  JSONArray batch=new JSONArray(grouped.values());
  if(batch.length()>0||namesBatch.length()>0){
   String name=String.format("batch-%08d",processed);JSONObject body=new JSONObject().put("products",batch);
   Files.writeString(dir.resolve(name+".json"),body.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
   Files.writeString(dir.resolve(name+"-planned.jsonl"),expected.stream().map(Document::toJson).collect(java.util.stream.Collectors.joining("\n","", "\n")),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
   String message="";
   if(send&&batch.length()>0){if(pub==null)pub=new PubSubGCP(PropertiesManager.get("p360.contingency.gcp.service_account_back"),PropertiesManager.get("p360.contingency.gcp.project_back"),PropertiesManager.get("p360.contingency.gcp.post_products_topic"));
    message=pub.publishMessage(body.toString());if(message==null||message.isBlank())throw new IOException("Unconfirmed PubSub delivery; inspect "+name+" before resuming");

   }
   if(namesBatch.length()>0){
    JSONObject namesBody=new JSONObject().put("products",namesBatch);
    Files.writeString(dir.resolve(name+"-names.json"),namesBody.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    if(send){if(putPub==null)putPub=new PubSubGCP(PropertiesManager.get("p360.contingency.gcp.service_account_back"),PropertiesManager.get("p360.contingency.gcp.project_back"),PropertiesManager.get("p360.contingency.gcp.idmc_put_products"));
     String nameId=putPub.publishMessage(namesBody.toString());if(nameId==null||nameId.isBlank())throw new IOException("Unconfirmed names delivery: "+name);message+=(message.isEmpty()?"":";")+nameId;}
   }
   if(send)sent+=expected.size();
   for(Document e:expected){e.append("messageId",message).append("sentAt",Instant.now().toString());changes.write(e.toJson());changes.newLine();}changes.flush();
   System.out.println("PUBSUB batch="+name+" records="+expected.size()+" messageId="+message+" send="+send);
  }
  products.flush();variants.flush();if(send)Thread.sleep(250);System.out.println("PROGRESS processed="+processed+" differences="+changed+" sent="+sent);
 }
 void run()throws Exception{
  Files.createDirectory(dir);
  try(var lock=java.nio.channels.FileChannel.open(dir.resolve("run.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lease=lock.tryLock()){
   if(lease==null)throw new IOException("Already running");long high;
   try(PreparedStatement p=sql("select max(\"ID\") from \"ArticleRevision\"");ResultSet r=p.executeQuery()){r.next();high=r.getLong(1);}
   Files.writeString(dir.resolve("run.json"),new Document("started",Instant.now().toString()).append("maxRevision",high).append("send",send).append("scope",Boolean.getBoolean("p360.sync.p360Scan")?"P360":"Mongo").toJson());
   try(BufferedWriter expected=Files.newBufferedWriter(dir.resolve("expected.jsonl"));BufferedWriter p=Files.newBufferedWriter(dir.resolve("products_initial.csv"));BufferedWriter a=Files.newBufferedWriter(dir.resolve("articles_initial.csv"))){
    csv(p,"ID","STATUS","REASON");csv(a,"ID","STATUS","REASON");
    if(Boolean.getBoolean("p360.sync.p360Scan")||Long.getLong("p360.sync.afterRevision",0L)>0){
long cursor=Long.getLong("p360.sync.afterRevision",0L);
    while(processed<maxRows){List<Long> ids=new ArrayList<>();
     try(PreparedStatement q=sql("select /*+ index(ar \"PK_ArticleRevision\") */ ar.\"ID\" from \"ArticleRevision\" ar where ar.\"ID\">? and ar.\"ID\"<=? and ar.\"RevisionID\"=1 and ar.\"EntityID\" in (1000,1100) and ar.\"DeletionTimestamp\"="+LIVE+" order by ar.\"ID\" fetch first "+Math.min(PAGE,maxRows-processed)+" rows only",cursor,high);ResultSet r=q.executeQuery()){while(r.next())ids.add(r.getLong(1));}
     if(ids.isEmpty())break;comparePage(rows(ids),expected,p,a);cursor=ids.get(ids.size()-1);Files.writeString(dir.resolve("cursor.txt"),String.valueOf(cursor));
    }
    }else{
     for(int entity:List.of(1100,1000)){
      if(Integer.getInteger("p360.sync.entity",entity)!=entity)continue;
      MongoCollection<Document> collection=mdb.getCollection(kind(entity));
      Document last=collection.find().sort(new Document("_id",-1)).projection(new Document("_id",1)).limit(1).maxTime(30,TimeUnit.SECONDS).first();
      if(last==null)continue;Object highId=last.get("_id"),cursor=null;
      while(processed<maxRows){
       Document range=new Document("$lte",highId);if(cursor!=null)range.append("$gt",cursor);
       List<Document> page=collection.find(new Document("_id",range)).sort(new Document("_id",1)).projection(new Document(key(entity),1)).limit((int)Math.min(PAGE,maxRows-processed)).maxTime(30,TimeUnit.SECONDS).into(new ArrayList<>());
       if(page.isEmpty())break;cursor=page.get(page.size()-1).get("_id");
       Set<String> names=new LinkedHashSet<>();Writer report=entity==1100?p:a;
       for(Document d:page){String id=str(d.get(key(entity)));if(id.isEmpty()){processed++;csv(report,str(d.get("_id")),"MISSING_IDENTIFIER","Sin "+key(entity));}else names.add(id);}
       if(!names.isEmpty()){
        Map<String,List<Long>> revisions=new HashMap<>();List<Object> args=new ArrayList<>(names);args.add(entity);
        String query="select /*+ index(ar \"XAK2_ArticleRevision\") */ ar.\"ID\",ar.\"Identifier\" from \"ArticleRevision\" ar where ar.\"Identifier\" in ("+in(names)+") and ar.\"EntityID\"=? and ar.\"RevisionID\"=1 and ar.\"DeletionTimestamp\"="+LIVE;
        try(PreparedStatement q=sql(query,args.toArray());ResultSet r=q.executeQuery()){while(r.next())revisions.computeIfAbsent(r.getString(2),k->new ArrayList<>()).add(r.getLong(1));}
        List<Long> ids=new ArrayList<>();for(String name:names){List<Long> found=revisions.getOrDefault(name,List.of());if(found.size()==1)ids.add(found.get(0));else{processed++;csv(report,name,found.isEmpty()?"MISSING_P360":"DUPLICATE_P360","Revision activa 1, EntityID="+entity);}}
        Map<Long,Row> loaded=rows(ids);for(long id:ids)if(!loaded.containsKey(id)){processed++;csv(report,String.valueOf(id),"MISSING_DETAIL","Sin ArticleDetail activo");}
        comparePage(loaded,expected,p,a);
       }
       Files.writeString(dir.resolve("cursor-"+entity+".json"),new Document("_id",cursor).toJson());p.flush();a.flush();
      }
     }
    }
   }
   if(send&&sent>0){Files.writeString(dir.resolve("waiting-until.txt"),Instant.now().plusSeconds(600).toString());System.out.println("WAIT_VERIFY seconds=600 sent="+sent);Thread.sleep(600000);verify();}
   Files.writeString(dir.resolve("complete.json"),new Document("finished",Instant.now().toString()).append("processed",processed).append("different",changed).append("sent",sent).toJson());
  }
 }
 void verify()throws Exception{
  try(BufferedReader in=Files.newBufferedReader(dir.resolve("expected.jsonl"));BufferedWriter p=Files.newBufferedWriter(dir.resolve("products_verification.csv"));BufferedWriter a=Files.newBufferedWriter(dir.resolve("articles_verification.csv"))){
   csv(p,"ID","STATUS","REASON");csv(a,"ID","STATUS","REASON");List<Document> batch=new ArrayList<>();String line;
   while((line=in.readLine())!=null){batch.add(Document.parse(line));if(batch.size()==PAGE){verifyBatch(batch,p,a);batch.clear();}}if(!batch.isEmpty())verifyBatch(batch,p,a);
  }
 }
 void verifyBatch(List<Document> expected,Writer p,Writer a)throws Exception{
  Map<Long,Row> now=rows(expected.stream().map(d->((Number)d.get("revision")).longValue()).toList());
  for(int entity:List.of(1100,1000)){List<Document> selected=expected.stream().filter(d->d.getInteger("entity")==entity).toList();
   Map<String,List<Document>> latest=documents(entity,selected.stream().map(d->d.getString("id")).toList());
   for(Document e:selected){String id=e.getString("id");List<Document> docs=latest.getOrDefault(id,List.of());String state,reason="";
    if(docs.isEmpty()){state="MISSING_MONGO";reason="No encontrado despuÃ©s del envÃ­o";}else if(docs.size()!=1){state="DUPLICATE_MONGO";reason="MÃ¡s de un documento";}else{
     List<String> missing=new ArrayList<>();Document want=(Document)e.get("expected");for(String f:e.getList("updatedFields",String.class)){Row snapshot=new Row(0,id,entity,strings(want),strings(e.get("wire",Document.class)),Set.of(),Set.of());if(!equal(snapshot,docs.get(0),f))missing.add(f);}state=missing.isEmpty()?"OK":"MISMATCH";reason=String.join(";",missing);
     if(missing.isEmpty()){List<String> deferred=new ArrayList<>(e.getList("fields",String.class));deferred.removeAll(e.getList("updatedFields",String.class));if(!deferred.isEmpty()){state="OK_PARTIAL";reason="Sin modificar: "+String.join(";",deferred);}}
    }
    Row current=now.get(((Number)e.get("revision")).longValue());Document want=(Document)e.get("expected");
    if(current==null){state="SOURCE_CHANGED";reason="P360 ya no esta activo";}
    else if(!current.errors.isEmpty()){state="SOURCE_CHANGED";reason=String.join(";",current.errors);}
    else {List<String> changed=new ArrayList<>();for(String f:e.getList("updatedFields",String.class))if(!str(want.get(f)).equals(str(current.values.get(f))))changed.add(f);if(!changed.isEmpty()){state="SOURCE_CHANGED";reason=String.join(";",changed);}}
    csv(entity==1100?p:a,id,state,reason);
   }
  }p.flush();a.flush();
 }
 @Override public void close()throws Exception{if(pub!=null)pub.close();if(putPub!=null)putPub.close();mongo.close();db.close();}
 public static void main(String[] args)throws Exception{
  if(args.length<1)throw new IllegalArgumentException("NEW-output-directory [--send] [--limit=N]");
  boolean send=Arrays.asList(args).contains("--send");long max=Long.MAX_VALUE;
  for(String s:args)if(s.startsWith("--limit="))max=Long.parseLong(s.substring(8));
  if(max<1)throw new IllegalArgumentException("--limit must be positive");
  try(EntradaUnicaSync app=new EntradaUnicaSync(Path.of(args[0]),send,max)){if(Arrays.asList(args).contains("--verify-only"))app.verify();else app.run();}
 }
}
