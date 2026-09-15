package mx.com.liverpool.p360.services.core.temp.exports;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.bson.json.JsonMode;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only Oracle/Mongo evidence report. Never exports products or modifies business data. */
public final class EcommDeliveryAudit {
    static final JsonWriterSettings JSON=JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
    static final String INPUT="""
        WITH input_ids AS (SELECT /*+ materialize */ j.orden,j.product_id FROM JSON_TABLE(?, '$[*]'
          COLUMNS(orden FOR ORDINALITY,product_id VARCHAR2(200) PATH '$')) j)
        """;
    static final String AUDIT_SQL=INPUT+"""
        , ids AS (SELECT /*+ materialize */ DISTINCT product_id FROM input_ids), b AS (
          SELECT /*+ leading(d t) use_nl(t) index(t IX_ENVIO_PROD_ID_FECHA) */
            t.ID,t.PRODUCT_ID,t.SKU,t.FECHA_SOLICITUD,t.FECHA_RESULTADO,t.ESTADO,t.ENTREGADO,t.MENSAJE,t.ORIGEN,
            ROW_NUMBER() OVER(PARTITION BY t.PRODUCT_ID ORDER BY t.FECHA_SOLICITUD DESC,t.ID DESC) rn
          FROM ids d JOIN P360_EXPLOIT.TB_ENVIO_PRODUCTO_BITACORA t ON t.PRODUCT_ID=d.product_id
          WHERE t.SISTEMA='ECOMM'
        ), pairs AS (
          SELECT /*+ materialize leading(d i) use_hash(i) full(i) */ DISTINCT i."ProductID" product_id,i."EnvioATGXMLID" xml_id
          FROM ids d JOIN P360_EXPLOIT.TB_ENVIO_ATG_ITEMS i ON i."ProductID"=d.product_id
        ), a AS (
          SELECT p.product_id,x.ID xml_id,x."CreationTime" creation_time,x."EnvioATGExecID" exec_id,
            ROW_NUMBER() OVER(PARTITION BY p.product_id ORDER BY x."CreationTime" DESC,x.ID DESC) rn
          FROM pairs p JOIN P360_EXPLOIT.TB_ENVIO_ATG_XML x ON x.ID=p.xml_id
        )
        SELECT f.orden FILA_ARCHIVO,f.product_id PRODUCT_ID,b.ID BITACORA_ID,b.SKU SKU_BITACORA,
          b.ESTADO BITACORA_ESTADO,b.ENTREGADO BITACORA_ENTREGADO,b.MENSAJE BITACORA_MENSAJE,
          TO_CHAR(b.FECHA_SOLICITUD,'YYYY-MM-DD"T"HH24:MI:SS.FF6TZH:TZM') BITACORA_FECHA_SOLICITUD,
          TO_CHAR(b.FECHA_RESULTADO,'YYYY-MM-DD"T"HH24:MI:SS.FF6TZH:TZM') BITACORA_FECHA_RESULTADO,b.ORIGEN BITACORA_ORIGEN,
          a.xml_id ATG_XML_ID,TO_CHAR(a.creation_time,'YYYY-MM-DD"T"HH24:MI:SS.FF6') ATG_CREATION_TIME,
          a.exec_id ATG_EXEC_ID,e."Status" ATG_ESTADO,e."Message" ATG_MENSAJE
        FROM input_ids f LEFT JOIN b ON b.PRODUCT_ID=f.product_id AND b.rn=1
          LEFT JOIN a ON a.product_id=f.product_id AND a.rn=1
          LEFT JOIN P360_EXPLOIT.TB_ENVIO_ATG_EXEC e ON e.ID=a.exec_id ORDER BY f.orden
        """;
    static final String SKU_SQL=INPUT+"""
        SELECT /*+ leading(i p d) use_nl(p d) index(p "XAK2_ArticleRevision") index(d "XAK1_ArticleDetail") */
          i.orden FILA_ARCHIVO,i.product_id PRODUCT_ID,COUNT(DISTINCT d."Res_Int_02") SKU_COUNT,
          MIN(d."Res_Int_02") SKU_ACTUAL,COUNT(DISTINCT p.ID) PRODUCT_REVISION_COUNT
        FROM input_ids i LEFT JOIN PIM_MASTER."ArticleRevision" p ON p."Identifier"=i.product_id
          AND p."EntityID"=1100 AND p."RevisionID"=1 AND p."CatalogID"=1
          AND p."DeletionTimestamp"=TIMESTAMP '9999-12-31 00:00:00.0'
        LEFT JOIN PIM_MASTER."ArticleDetail" d ON d."ArticleRevisionID"=p.ID
          AND d."DeletionTimestamp"=TIMESTAMP '9999-12-31 00:00:00.0'
        GROUP BY i.orden,i.product_id ORDER BY i.orden
        """;
    static final List<String> MONGO_FIELDS=List.of("MONGO_EN_PRODUCTS","MONGO_COINCIDENCIAS","MONGO_IDS_COINCIDENTES",
        "MONGO_ID","MONGO_CRUCE_POR","MONGO_NOMBRE","MONGO_MARCA","MONGO_ESTADO","MONGO_ACTIVO","MONGO_PUBLICADO",
        "MONGO_MARKETPLACE","MONGO_CATEGORIAS","MONGO_TIPO_PRODUCTO","MONGO_CREATED_AT_UTC","MONGO_MODIFIED_AT_UTC",
        "MONGO_LAST_UPDATED_UTC","MONGO_ACTUALIZACION_UTC","MONGO_CAMPO_ACTUALIZACION","MONGO_ANTIGUEDAD_DIAS",
        "MONGO_RECIENTE","MONGO_RECIENTE_DIAS","MONGO_ACTUALIZADO_DESPUES_SOLICITUD","MONGO_CONSULTADO_UTC");
    record Match(Document document,Set<String> fields,Instant readAt) {}
    record Updated(Instant time,String field) {}
    static final class Options {
        Path input,out,uriFile; int days=7;
        static Options parse(String[] a) {
            if(a.length<2)throw new IllegalArgumentException("Required: ids.txt NEW-output-directory [--recent-days N] [--mongo-uri-file path]");
            Options o=new Options();o.input=Path.of(a[0]);o.out=Path.of(a[1]);
            for(int i=2;i<a.length;i+=2){if(i+1>=a.length)throw new IllegalArgumentException("Missing option value");
                switch(a[i]){case "--recent-days":o.days=Integer.parseInt(a[i+1]);break;case "--mongo-uri-file":o.uriFile=Path.of(a[i+1]);break;default:throw new IllegalArgumentException("Unknown option");}}
            if(o.days<1||o.days>3650)throw new IllegalArgumentException("recent-days must be 1..3650");return o;
        }
    }
    static List<String> readIds(Path file)throws IOException {
        List<String> ids=new ArrayList<>();int line=0;
        for(String raw:Files.readAllLines(file,StandardCharsets.UTF_8)){
            line++;String s=raw.replace("\uFEFF","").trim();if(s.isEmpty())continue;
            if(ids.isEmpty()&&Set.of("PRODUCT_ID","IDENTIFIER","ID").contains(s.toUpperCase(Locale.ROOT)))continue;
            if(!s.matches("[A-Za-z0-9_.@-]{1,200}"))throw new IllegalArgumentException("Use one Identifier per line; invalid line "+line);
            ids.add(s);if(ids.size()>50000)throw new IllegalArgumentException("Maximum 50000 input lines");
        }if(ids.isEmpty())throw new IllegalArgumentException("Empty input");return ids;
    }
    static String csv(String s){return "\""+Objects.toString(s,"").replace("\"","\"\"")+"\"";}
    static Connection oracle()throws Exception {
        String url=System.getenv("ORACLE_JDBC_URL");
        if(url==null||url.isBlank())return new QuickJdbcConnectionManager().openConnection(false);
        Properties p=new Properties();p.setProperty("user",Objects.requireNonNull(System.getenv("ORACLE_JDBC_USER")));
        p.setProperty("password",Objects.requireNonNull(System.getenv("ORACLE_JDBC_PASSWORD")));
        p.setProperty("oracle.jdbc.ReadTimeout","360000");p.setProperty("oracle.net.CONNECT_TIMEOUT","15000");
        return DriverManager.getConnection(url,p);
    }
    static List<LinkedHashMap<String,String>> query(Connection c,String sql,Clob input)throws Exception {
        List<LinkedHashMap<String,String>> rows=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(sql)){p.setQueryTimeout(300);p.setFetchSize(1000);p.setClob(1,input);
            try(ResultSet r=p.executeQuery()){int n=r.getMetaData().getColumnCount();while(r.next()){
                LinkedHashMap<String,String> row=new LinkedHashMap<>();for(int i=1;i<=n;i++)row.put(r.getMetaData().getColumnLabel(i),Objects.toString(r.getString(i),""));rows.add(row);
            }}
        }return rows;
    }
    static void assignSku(Map<String,String> r,Map<String,String> s) {
        int n=Integer.parseInt(s.get("SKU_COUNT"));String current=n==1?s.get("SKU_ACTUAL"):"";
        String key=n>1?"":!current.isEmpty()?current:r.get("SKU_BITACORA");
        r.put("SKU_P360_ACTUAL",current);r.put("SKU_CONSULTA_MONGO",key);
        r.put("SKU_FUENTE",n>1?"AMBIGUO":!current.isEmpty()?"P360_ACTUAL":!key.isEmpty()?"BITACORA_HISTORICA":"SIN_SKU");
        r.put("P360_REVISIONES_ENCONTRADAS",s.get("PRODUCT_REVISION_COUNT"));
    }
    static String scalar(Object v){return v==null?"":String.valueOf(v);}
    static Instant instant(Object v){
        if(v instanceof java.util.Date)return ((java.util.Date)v).toInstant();
        if(v instanceof String)try{return OffsetDateTime.parse((String)v).toInstant();}catch(Exception ignored){}
        return null;
    }
    static Updated updated(Document d){
        Updated best=new Updated(null,"");for(String k:List.of("modifiedAt","lastUpdated")){Instant t=instant(d.get(k));if(t!=null&&(best.time==null||t.isAfter(best.time)))best=new Updated(t,k);}return best;
    }
    static String iso(Instant t){return t==null?"":t.toString();}
    static String recent(Instant stamp,Instant read,int days){return stamp==null?"SIN_FECHA":stamp.isAfter(read)?"FECHA_FUTURA":stamp.isBefore(read.minus(Duration.ofDays(days)))?"NO":"SI";}
    static Map<String,List<Match>> mongo(Collection<String> skus,String uri,Path evidence)throws Exception {
        ConnectionString cs=new ConnectionString(uri);if(!"product".equals(cs.getDatabase()))throw new IllegalArgumentException("Expected product database");
        MongoClientSettings config=MongoClientSettings.builder().applyConnectionString(cs).applicationName("P360-EcommDeliveryAudit-ReadOnly")
            .applyToClusterSettings(b->b.serverSelectionTimeout(20,TimeUnit.SECONDS))
            .applyToSocketSettings(b->b.connectTimeout(10,TimeUnit.SECONDS).readTimeout(40,TimeUnit.SECONDS))
            .applyToConnectionPoolSettings(b->b.maxSize(2)).build();
        Map<String,List<Match>> results=new HashMap<>();List<String> all=new ArrayList<>(skus);
        Document projection=new Document();for(String k:List.of("_id","properties.material","name","brand","status","isActive","isPublished","isMarketPlace","categories","productType","createdAt","modifiedAt","lastUpdated"))projection.append(k,1);
        try(MongoClient client=MongoClients.create(config);BufferedWriter w=Files.newBufferedWriter(evidence,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
            MongoCollection<Document> c=client.getDatabase("product").getCollection("products");
            // Discover usable indexes instead of assuming a custom index name.
            Map<String,String> indexes=new HashMap<>();for(Document ix:c.listIndexes().maxTime(20,TimeUnit.SECONDS)){
                Document keys=(Document)ix.get("key");if(keys!=null&&!keys.isEmpty()){String first=keys.keySet().iterator().next();if(Set.of("_id","properties.material").contains(first)&&!ix.containsKey("partialFilterExpression")&&!ix.containsKey("collation"))indexes.putIfAbsent(first,ix.getString("name"));}}
            if(!indexes.keySet().containsAll(List.of("_id","properties.material")))throw new IllegalStateException("Required SKU indexes missing");
            for(int start=0;start<all.size();start+=100){List<String> part=all.subList(start,Math.min(start+100,all.size()));List<Object> keys=new ArrayList<>();
                for(String s:part){keys.add(s);if(s.matches("[0-9]+"))try{keys.add(Long.valueOf(s));}catch(NumberFormatException ignored){}}
                Map<String,Document> docs=new LinkedHashMap<>();
                for(String field:List.of("_id","properties.material"))try(MongoCursor<Document> it=c.find(new Document(field,new Document("$in",keys))).projection(projection).hintString(indexes.get(field)).batchSize(100).maxTime(25,TimeUnit.SECONDS).iterator()){
                    while(it.hasNext()){Document d=it.next();String id=d.get("_id").getClass().getName()+":"+d.get("_id");Document old=docs.putIfAbsent(id,d);
                        if(old!=null&&!old.equals(d))throw new IllegalStateException("Document changed across SKU lookups; retry audit");if(docs.size()>20000)throw new IllegalStateException("Too many SKU matches");}}
                Instant read=Instant.now();for(Document d:docs.values()){Document props=(Document)d.get("properties");String material=props==null?"":scalar(props.get("material"));
                    for(String sku:part){Set<String> fields=new LinkedHashSet<>();if(sku.equals(scalar(d.get("_id"))))fields.add("_id");if(sku.equals(material))fields.add("properties.material");
                        if(!fields.isEmpty()){results.computeIfAbsent(sku,k->new ArrayList<>()).add(new Match(d,fields,read));w.write(new Document("sku",sku).append("matchFields",new ArrayList<>(fields)).append("readAt",read.toString()).append("product",d).toJson(JSON));w.newLine();}}
                }w.flush();System.out.println("MONGO "+Math.min(start+100,all.size())+"/"+all.size()+" skus; matched="+results.size());
            }
        }return results;
    }
    static void appendMongo(Map<String,String> r,List<Match> matches,int days){
        for(String k:MONGO_FIELDS)r.put(k,"");r.put("MONGO_RECIENTE_DIAS",String.valueOf(days));r.put("MONGO_COINCIDENCIAS",String.valueOf(matches.size()));
        r.put("MONGO_EN_PRODUCTS",!matches.isEmpty()?"SI":r.get("SKU_CONSULTA_MONGO").isEmpty()?"SIN_SKU_PARA_CONSULTAR":"NO_ENCONTRADO_POR_SKU");
        if(matches.isEmpty())return;
        Match m=matches.stream().max(Comparator.comparing((Match x)->Objects.requireNonNullElse(updated(x.document).time,Instant.MIN)).thenComparing(x->scalar(x.document.get("_id")))).orElseThrow();
        Document d=m.document;Updated up=updated(d);
        r.put("MONGO_IDS_COINCIDENTES",String.join("|",matches.stream().map(x->scalar(x.document.get("_id"))).sorted().toList()));
        String[][] fields={{"MONGO_ID","_id"},{"MONGO_NOMBRE","name"},{"MONGO_MARCA","brand"},{"MONGO_ESTADO","status"},{"MONGO_ACTIVO","isActive"},{"MONGO_PUBLICADO","isPublished"},{"MONGO_MARKETPLACE","isMarketPlace"},{"MONGO_TIPO_PRODUCTO","productType"}};
        for(String[] f:fields)r.put(f[0],scalar(d.get(f[1])));
        r.put("MONGO_CATEGORIAS",new Document("values",d.get("categories")).toJson());r.put("MONGO_CRUCE_POR",String.join("|",m.fields));
        r.put("MONGO_CREATED_AT_UTC",iso(instant(d.get("createdAt"))));r.put("MONGO_MODIFIED_AT_UTC",iso(instant(d.get("modifiedAt"))));r.put("MONGO_LAST_UPDATED_UTC",iso(instant(d.get("lastUpdated"))));
        r.put("MONGO_ACTUALIZACION_UTC",iso(up.time));r.put("MONGO_CAMPO_ACTUALIZACION",up.field);r.put("MONGO_CONSULTADO_UTC",iso(m.readAt));
        r.put("MONGO_RECIENTE",recent(up.time,m.readAt,days));
        if(up.time!=null)r.put("MONGO_ANTIGUEDAD_DIAS",String.format(Locale.ROOT,"%.3f",Duration.between(up.time,m.readAt).toMillis()/86400000.0));
        Instant request=instant(r.get("BITACORA_FECHA_SOLICITUD"));r.put("MONGO_ACTUALIZADO_DESPUES_SOLICITUD",up.time==null||request==null?"SIN_FECHA_COMPARABLE":up.time.isBefore(request)?"NO":"SI");
    }
    static Map<String,Integer> counts(Collection<? extends Map<String,String>> rows,String key){
        Map<String,Integer> out=new TreeMap<>();for(Map<String,String> r:rows)out.merge(r.getOrDefault(key,"").isEmpty()?"SIN_DATO":r.get(key),1,Integer::sum);return out;
    }
    static void run(Options o)throws Exception {
        List<String> ids=readIds(o.input);String uri=o.uriFile==null?System.getenv("MONGODB_URI"):Files.readString(o.uriFile).trim();if(uri==null||uri.isBlank())throw new IllegalArgumentException("MONGODB_URI or --mongo-uri-file required");
        Files.createDirectory(o.out);Instant started=Instant.now();Files.write(o.out.resolve("input.ids"),ids,StandardCharsets.UTF_8);System.out.println("START "+started+" inputRows="+ids.size());
        ExecutorService net=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"audit-jdbc-timeout");t.setDaemon(true);return t;});
        List<LinkedHashMap<String,String>> rows;
        try(Connection c=oracle()){
            c.setAutoCommit(false);c.setReadOnly(true);c.setNetworkTimeout(net,360000);try(Statement s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            Clob input=c.createClob();try{input.setString(1,"["+String.join(",",ids.stream().map(s->"\""+s+"\"").toList())+"]");
                rows=query(c,AUDIT_SQL,input);List<LinkedHashMap<String,String>> skus=query(c,SKU_SQL,input);
                if(rows.size()!=ids.size()||skus.size()!=ids.size())throw new IllegalStateException("Oracle row count mismatch");
                for(int i=0;i<ids.size();i++){if(!ids.get(i).equals(rows.get(i).get("PRODUCT_ID"))||!ids.get(i).equals(skus.get(i).get("PRODUCT_ID")))throw new IllegalStateException("Oracle row alignment mismatch");assignSku(rows.get(i),skus.get(i));}
            }finally{input.free();}c.rollback();
        }finally{net.shutdownNow();}
        Instant oracleFinished=Instant.now();Set<String> skus=new TreeSet<>();for(Map<String,String> r:rows)if(!r.get("SKU_CONSULTA_MONGO").isEmpty())skus.add(r.get("SKU_CONSULTA_MONGO"));
        Map<String,List<Match>> mongo=mongo(skus,uri,o.out.resolve("mongo-evidence.jsonl"));
        Path partial=o.out.resolve("resultados.csv.partial");try(BufferedWriter w=Files.newBufferedWriter(partial,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
            boolean header=false;for(Map<String,String> r:rows){appendMongo(r,mongo.getOrDefault(r.get("SKU_CONSULTA_MONGO"),List.of()),o.days);
                if(!header){w.write('\uFEFF');w.write(String.join(",",r.keySet().stream().map(EcommDeliveryAudit::csv).toList()));w.newLine();header=true;}
                w.write(String.join(",",r.values().stream().map(EcommDeliveryAudit::csv).toList()));w.newLine();}
        }
        Map<String,Map<String,String>> unique=new LinkedHashMap<>();for(Map<String,String> r:rows)unique.put(r.get("PRODUCT_ID"),r);
        Document summary=new Document("inputRows",ids.size()).append("uniqueIds",unique.size()).append("recentDays",o.days).append("startedUtc",started.toString()).append("oracleFinishedUtc",oracleFinished.toString()).append("finishedUtc",Instant.now().toString())
            .append("bitacoraEstado",counts(unique.values(),"BITACORA_ESTADO")).append("atgEstado",counts(unique.values(),"ATG_ESTADO")).append("mongo",counts(unique.values(),"MONGO_EN_PRODUCTS")).append("reciente",counts(unique.values(),"MONGO_RECIENTE"));
        Files.writeString(o.out.resolve("resumen.json"),summary.toJson(JsonWriterSettings.builder().indent(true).build()),StandardCharsets.UTF_8);
        Files.writeString(o.out.resolve("LEEME.txt"),"Read-only audit. Product log: latest FECHA_SOLICITUD, ID, ECOMM only. ATG: latest CreationTime, XML.ID; creation is not proof of final ecommerce ingestion. SKU: current Product2G RevisionID=1 CatalogID=1, then historical log if absent; multiple current SKUs are not guessed. Mongo: indexed SKU matches in products._id/properties.material; no match does not prove absence under other keys. Multiple matches remain counted; selected document has newest modifiedAt/lastUpdated. Dates without timezone are not guessed; ATG CreationTime remains timezone-less. Recent means 0.."+o.days+" days before the document read. Mongo reads are batched, not globally transactional. Oracle queries share one read-only snapshot. Completion requires complete.json. No product exports or business-data writes.\n",StandardCharsets.UTF_8);
        Files.move(partial,o.out.resolve("resultados.csv"));Files.writeString(o.out.resolve("complete.json"),summary.toJson(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);System.out.println("COMPLETE ids="+unique.size()+" output="+o.out);
    }
    public static void main(String[] args){
        if(args.length==1&&Set.of("--help","-h").contains(args[0])){System.out.println("EcommDeliveryAudit ids.txt NEW-output-directory [--recent-days N] [--mongo-uri-file path]\nOracle: ORACLE_JDBC_URL/USER/PASSWORD, or existing server.properties through QuickJdbcConnectionManager. Mongo: MONGODB_URI or URI file. No writes/exports.");return;}
        try{run(Options.parse(args));}catch(Exception e){System.err.println("Audit failed (no complete marker): "+e.getClass().getSimpleName());if(e instanceof SQLException)System.err.println("Oracle error code="+((SQLException)e).getErrorCode());System.exit(1);}
    }
}
