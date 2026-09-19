package mx.com.liverpool.p360.services.core.sftp;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Indexed Mongo candidate snapshots. These are identity candidates, not approved mappings. */
public final class MongoDuplicateEvidence {
    static final JsonWriterSettings JSON=JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
    static List<Object> types(Collection<String> strings){List<Object> values=new ArrayList<>();for(String s:strings){values.add(s);if(s.matches("[0-9]+")){try{values.add(Long.parseLong(s));}catch(NumberFormatException ignored){}}}return values;}
    static List<Document> find(MongoCollection<Document> collection,String field,Collection<String> values)throws Exception {
        if(values.isEmpty())return List.of();
        List<Object> keys=types(values);List<Document> result=new ArrayList<>();
        for(int start=0;start<keys.size();start+=500){List<Object> batch=keys.subList(start,Math.min(start+500,keys.size()));
            try(MongoCursor<Document> cursor=collection.find(new Document(field,new Document("$in",batch))).hintString(Map.of("_id","_id_","properties.material","properties.material_1","parent","parent","externalId","externalId_1").get(field)).batchSize(100).maxTime(30,TimeUnit.SECONDS).iterator()){
                while(cursor.hasNext()){result.add(cursor.next());if(result.size()>100000)throw new IllegalStateException("Candidate result exceeds review limit; output not marked complete");}
            }
        }return result;
    }
    static String id(Document d){return String.valueOf(d.get("_id"));}
    static String field(Document d,String key){Object v=d;for(String k:key.split("\\.")){if(!(v instanceof Document))return "";v=((Document)v).get(k);}return v==null?"":String.valueOf(v);}
    static void add(Map<String,Document> target,List<Document> docs){for(Document d:docs){String key=d.get("_id").getClass().getName()+":"+id(d);Document old=target.putIfAbsent(key,d);if(old!=null&&!old.equals(d))throw new IllegalStateException("Mongo document changed between reads");}}
    static JSONArray json(Collection<Document> docs){JSONArray a=new JSONArray();for(Document d:docs)a.put(new JSONObject(d.toJson(JSON)));return a;}
    static void run(String[] args)throws Exception {
        if(args.length<2||args.length>3)throw new IllegalArgumentException("duplicates.csv NEW-output-directory [max-groups]");
        LinkedHashMap<String,List<String>> groups=DuplicateGroupAnalysis.csv(Path.of(args[0]));List<String> all=new ArrayList<>(groups.keySet());if(args.length==3)all=all.subList(0,Math.min(all.size(),Integer.parseInt(args[2])));
        Path out=Path.of(args[1]);Files.createDirectory(out);Files.copy(Path.of(args[0]),out.resolve("input.csv"));
        ConnectionString cs=new ConnectionString(Objects.requireNonNull(System.getenv("MONGODB_URI")));if(!"product".equals(cs.getDatabase()))throw new IllegalArgumentException("Expected product database");
        MongoClientSettings settings=MongoClientSettings.builder().applyConnectionString(cs).applicationName("P360-Duplicate-Analysis-ReadOnly")
            .applyToClusterSettings(b->b.serverSelectionTimeout(20,TimeUnit.SECONDS)).applyToSocketSettings(b->b.connectTimeout(10,TimeUnit.SECONDS).readTimeout(40000,TimeUnit.MILLISECONDS)).applyToConnectionPoolSettings(b->b.maxSize(2)).build();
        int completed=0,withProducts=0,withSkus=0;
        try(MongoClient client=MongoClients.create(settings);BufferedWriter summary=Files.newBufferedWriter(out.resolve("groups.jsonl"),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
            MongoDatabase db=client.getDatabase("product");MongoCollection<Document> products=db.getCollection("products"),skus=db.getCollection("skus");
            for(int start=0;start<all.size();start+=100){List<String> part=all.subList(start,Math.min(start+100,all.size()));Set<String> keys=new LinkedHashSet<>(part);for(String sku:part)keys.addAll(groups.get(sku));
                Instant read=Instant.now();Map<String,Document> pd=new LinkedHashMap<>(),sd=new LinkedHashMap<>();add(pd,find(products,"_id",keys));add(pd,find(products,"properties.material",part));
                Set<String> parentKeys=new LinkedHashSet<>(keys);for(Document d:pd.values())parentKeys.add(id(d));add(sd,find(skus,"parent",parentKeys));add(sd,find(skus,"_id",keys));add(sd,find(skus,"externalId",keys));
                for(String sku:part){Set<String> groupKeys=new LinkedHashSet<>(groups.get(sku));groupKeys.add(sku);List<Document> p=new ArrayList<>(),s=new ArrayList<>();
                    for(Document d:pd.values())if(groupKeys.contains(id(d))||sku.equals(field(d,"properties.material")))p.add(d);
                    Set<String> parents=new HashSet<>(groupKeys);for(Document d:p)parents.add(id(d));
                    for(Document d:sd.values())if(parents.contains(field(d,"parent"))||groupKeys.contains(id(d))||groupKeys.contains(field(d,"externalId")))s.add(d);
                    JSONObject evidence=new JSONObject().put("sku",sku).put("p360Identifiers",groups.get(sku)).put("readStarted",read.toString()).put("readFinished",Instant.now().toString()).put("products",json(p)).put("skus",json(s))
                        .put("mode","CANDIDATE_EVIDENCE_ONLY").put("scope","Indexed matches: products._id against P360 Identifiers/SKU; products.properties.material against SKU; skus.parent against candidate product IDs, skus._id/externalId against P360 Identifiers/SKU. String and integer keys. Not a transactional snapshot; identity and characteristic mappings require review. Zero matches is not proof of absence. No merge or persistence.");
                    DuplicateGroupAnalysis.gzip(out.resolve(sku+".json.gz"),evidence);summary.write(new JSONObject().put("sku",sku).put("products",p.size()).put("skus",s.size()).toString());summary.newLine();summary.flush();completed++;if(!p.isEmpty())withProducts++;if(!s.isEmpty())withSkus++;
                }
                System.out.println(Instant.now()+" groups="+completed+"/"+all.size()+" withProductCandidates="+withProducts+" withSkuCandidates="+withSkus);
            }
        }
        Files.writeString(out.resolve("complete.json"),new JSONObject().put("groups",completed).put("withProductCandidates",withProducts).put("withSkuCandidates",withSkus).put("finished",Instant.now().toString()).toString(2),StandardOpenOption.CREATE_NEW);
    }
    public static void main(String[] args){try{run(args);}catch(Exception e){System.err.println("Mongo evidence failed: "+e.getClass().getSimpleName());System.exit(1);}}
}
