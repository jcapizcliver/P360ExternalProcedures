package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import java.io.*;
import org.json.*;
import mx.com.liverpool.p360.services.core.*;
import mx.com.liverpool.p360.services.core.temp.xml.local.neostream.StepXmlStreamingParser.*;

/** Reads immutable STEP snapshots directly; P360 writes use the existing NeoSTEP API writers. */
public final class NeoSQLite {
    static List<Object> items(JSONArray a){List<Object> r=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++)r.add(a.get(i));return r;}
    static Product model(JSONObject j) {
        JSONObject s=j.getJSONObject("source");
        if(s.getInt("contextId")!=1752092||s.getInt("workspaceId")!=200)throw new IllegalArgumentException("SOURCE_SCOPE");
        Product p=new Product(s.getString("id"),s.optString("parentId",null),s.getString("objectType"));
        char[] name=j.optString("name").toCharArray();p.appendName(name,0,name.length);p.openValues();
        for(Object obj:items(j.getJSONArray("attributes"))) {
            JSONObject a=(JSONObject)obj;String attr=a.getString("attributeId");
            JSONArray values=a.getJSONArray("values");boolean multi=a.optBoolean("multiValue");
            if(!multi&&values.length()>1)throw new IllegalArgumentException("MULTIPLE_SINGLE_VALUES "+attr);
            if(multi)p.prepareMultiValue(new MultiValue(attr));
            for(Object value:items(values)){JSONObject v=(JSONObject)value;Value x=new Value(attr,v.optString("id",null),v.optString("unit",null));char[] t=v.optString("text").toCharArray();x.append(t,0,t.length);p.prepareValue(x);p.flushValue();}
            if(multi)p.flushMultiValue();
        }
        p.closeValues();
        for(Object o:items(j.optJSONArray("references"))){
            JSONObject r=(JSONObject)o;
            if("CLASSIFICATION".equals(r.getString("kind")))p.addClassification(new Classification(r.getString("targetId"),r.getString("type")));
        }
        return p;
    }
    static boolean unresolved(Object x){
        if(x instanceof JSONObject){JSONObject o=(JSONObject)x;for(Object key:o.keySet()){String k=key.toString();Object v=o.get(k);if("unresolved".equals(k)&&v instanceof JSONArray&&((JSONArray)v).length()>0)return true;if(unresolved(v))return true;}}
        if(x instanceof JSONArray)for(Object v:items((JSONArray)x))if(unresolved(v))return true;
        return false;
    }
    static final Map<String,JSONObject> sourceModels = new LinkedHashMap<>();
    static java.util.function.Consumer<JSONObject> debtSink;
    static void sourceDebt(Object x,String path,String objectId) {
        if(x instanceof JSONObject){JSONObject j=(JSONObject)x;
            if(j.optJSONArray("unresolved")!=null&&j.getJSONArray("unresolved").length()>0)
                debtSink.accept(new JSONObject().put("kind","SOURCE_PENDING").put("objectId",objectId).put("path",path).put("detail",j));
            for(Object k:j.keySet())if(!"unresolved".equals(k))sourceDebt(j.get(k.toString()),path+"/"+k,objectId);
        } else if(x instanceof JSONArray){int i=0;for(Object v:items((JSONArray)x))sourceDebt(v,path+"/"+(i++),objectId);}
    }
    static Product family(Connection c,String root) throws Exception {
        sourceModels.clear();
        Map<String,Product> products=new LinkedHashMap<>();Map<String,JSONObject> payloads=new LinkedHashMap<>();
        String sql="SELECT e.payload_json,e.payload_sha256 FROM migration_root r JOIN migration_member m ON m.root_id=r.root_id JOIN stage_entity e ON e.entity_id=m.entity_id AND e.run_id=r.run_id WHERE r.root_id=? AND r.state='STAGED' AND e.kind='product'";
        try(PreparedStatement q=c.prepareStatement(sql)){q.setString(1,root);try(ResultSet rs=q.executeQuery()){while(rs.next()){
            String raw=rs.getString(1);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
            if(!hash.equals(rs.getString(2)))throw new IllegalArgumentException("SOURCE_HASH_MISMATCH");
            JSONObject j=new JSONObject(raw);sourceModels.put(j.getJSONObject("source").getString("id"),j);sourceDebt(j,"",j.getJSONObject("source").getString("id"));
            Product p=model(j);if(products.put(p.getId(),p)!=null)throw new IllegalArgumentException("DUPLICATE_MEMBER");payloads.put(p.getId(),j);
        }}}
        if(!products.containsKey(root))throw new IllegalArgumentException("ROOT_NOT_STAGED");
        for(Product p:products.values()){
            if(!p.getId().equals(root)){Product parent=products.get(p.getParentId());if(parent==null)throw new IllegalArgumentException("MISSING_PARENT");parent.addProduct(p);}
            JSONArray children=payloads.get(p.getId()).getJSONArray("children");
            for(Object id:items(children)){Product child=products.get(id.toString());if(child==null||!p.getId().equals(child.getParentId()))throw new IllegalArgumentException("INCOMPLETE_FAMILY");}
        }
        return products.get(root);
    }
    static Map<String,String> statuses() throws Exception {
        Map<String,String> out=new TreeMap<>();RESTWorkshop rw=new RESTWorkshop();
        Path f=Path.of(PropertiesManager.get("p360.contingency.base_directory"),"cache/templates/dictionaries/ExternalStatus");
        try(BufferedReader r=Files.newBufferedReader(f)){String line;while((line=r.readLine())!=null){String[] a=rw.parseLine(line,"\"",";","\\");if(a.length>1)out.put(a[0],a[1]);}}
        if(out.isEmpty())throw new IllegalStateException("EMPTY_EXTERNAL_STATUS_DICTIONARY");return out;
    }
    static void record(Connection c,String root,String state,String reason)throws Exception{
        try(PreparedStatement q=c.prepareStatement("INSERT INTO result_v2(root_id,state,reason,updated_at) VALUES(?,?,?,?) ON CONFLICT(root_id) DO UPDATE SET state=excluded.state,reason=excluded.reason,updated_at=excluded.updated_at")){q.setString(1,root);q.setString(2,state);q.setString(3,reason);q.setString(4,java.time.Instant.now().toString());q.executeUpdate();}
    }
    static void saveDebt(Connection ledger,String root,JSONObject item){
        try{
            String raw=item.toString();String id=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((root+raw).getBytes(StandardCharsets.UTF_8)));
            String now=java.time.Instant.now().toString();
            try(PreparedStatement q=ledger.prepareStatement("INSERT INTO debt(id,root_id,kind,detail,created_at,updated_at) VALUES(?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET updated_at=excluded.updated_at")){
                q.setString(1,id);q.setString(2,root);q.setString(3,item.getString("kind"));q.setString(4,raw);q.setString(5,now);q.setString(6,now);q.executeUpdate();
            }
        }catch(Exception e){throw new IllegalStateException("CANNOT_PERSIST_DEBT",e);}
    }
    static void publish(Connection ledger,String root,JSONObject body,PubSubGCP pub,java.util.function.Consumer<JSONObject> debt)throws Exception {
        try(PreparedStatement q=ledger.prepareStatement("SELECT state FROM pubsub_outbox WHERE root_id=?")){
            q.setString(1,root);try(ResultSet r=q.executeQuery()){if(r.next()){
                if(!"ACKNOWLEDGED".equals(r.getString(1)))debt.accept(new JSONObject().put("kind","PUBSUB_UNCONFIRMED").put("reason","Existing attempt; inspect before retry"));
                return;
            }}
        }
        if(body.getJSONArray("products").length()==0)throw new IllegalStateException("EMPTY_PUBSUB_PAYLOAD");
        try(PreparedStatement q=ledger.prepareStatement("INSERT INTO pubsub_outbox(root_id,payload,state,updated_at) VALUES(?,?,'SENDING',?)")){
            q.setString(1,root);q.setString(2,body.toString());q.setString(3,java.time.Instant.now().toString());q.executeUpdate();
        }
        // Persist the attempt before external publication, even during a cohort transaction.
        if(!ledger.getAutoCommit())ledger.commit();
        String ack=pub.publishMessage(body.toString());
        boolean ok=ack!=null&&ack.matches("[0-9]+");
        try(PreparedStatement q=ledger.prepareStatement("UPDATE pubsub_outbox SET state=?,message_id=?,updated_at=? WHERE root_id=?")){
            q.setString(1,ok?"ACKNOWLEDGED":"UNCONFIRMED");q.setString(2,ack);q.setString(3,java.time.Instant.now().toString());q.setString(4,root);q.executeUpdate();
        }
        if(!ok)debt.accept(new JSONObject().put("kind","PUBSUB_UNCONFIRMED").put("payload",body));
    }
    static int cohortSize(){return Math.max(1,Integer.getInteger("p360.sqlite.cohort.roots",9000));}
    static Product savedFamily(Path dir,String root)throws Exception {
        sourceModels.clear(); JSONObject all=new JSONObject(Files.readString(dir.resolve("source").resolve(root+".json")));
        Map<String,Product> models=new LinkedHashMap<>();
        for(Object key:all.keySet()){String id=key.toString();JSONObject j=all.getJSONObject(id);sourceModels.put(id,j);models.put(id,model(j));}
        for(Product p:models.values())if(!p.getId().equals(root))models.get(p.getParentId()).addProduct(p);
        return models.get(root);
    }
    static Iterable<Product> streamSaved(Path dir,List<String> roots){return ()->new Iterator<Product>(){
        final Iterator<String> it=roots.iterator(); public boolean hasNext(){return it.hasNext();}
        public Product next(){try{return savedFamily(dir,it.next());}catch(Exception e){throw new IllegalStateException(e);}}
    };}
    static void phaseStatus(Path dir,String phase,int roots,int complete)throws Exception {
        Files.writeString(dir.resolve("status.json"),new JSONObject().put("state",phase).put("cohortRoots",roots)
            .put("phaseFamilies",complete).put("at",java.time.Instant.now().toString()).toString());
    }
    /** A bounded input cut replaces an XML file. No writer is constructed or flushed per family. */
    static int processCohort(Connection src,Connection ledger,Path dir,List<String> selected,boolean apply,
            Map<String,String> map,PubSubGCP pub,PrintWriter log)throws Exception {
        List<String> ready=new ArrayList<>();Map<String,String> owners=new HashMap<>();
        Map<String,Integer> counts=new HashMap<>();String[] current={null};
        Files.createDirectories(dir.resolve("source"));Files.createDirectories(dir.resolve("batch-debt"));
        java.util.function.Consumer<JSONObject> route=item->{try{
            JSONObject request=item.optJSONObject("request");
            if(request!=null&&request.optJSONArray("rows")!=null){
                String raw=item.toString();String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
                Path evidence=dir.resolve("batch-debt").resolve(hash+".json");if(!Files.exists(evidence))Files.writeString(evidence,raw);
                Map<String,JSONArray> affected=new LinkedHashMap<>();JSONArray rows=request.getJSONArray("rows");
                for(int i=0;i<rows.length();i++){
                    String id=rows.getJSONObject(i).getJSONObject("object").getString("id");
                    if(id.startsWith("'"))id=id.substring(1,id.lastIndexOf("'"));
                    String root=owners.get(id);if(root==null)throw new IllegalStateException("UNKNOWN_BATCH_OWNER "+id);
                    affected.computeIfAbsent(root,k->new JSONArray()).put(i);
                }
                for(Map.Entry<String,JSONArray> e:affected.entrySet()){
                    saveDebt(ledger,e.getKey(),new JSONObject().put("kind",item.getString("kind")).put("writer",item.optString("writer"))
                        .put("batchEvidence",evidence.toString()).put("batchRowIndexes",e.getValue()).put("scope","BATCH_REQUIRES_ROW_REVIEW"));
                    counts.merge(e.getKey(),1,Integer::sum);
                }
            }else{
                String root=owners.getOrDefault(item.optString("objectId"),current[0]);
                if(root==null)throw new IllegalStateException("DEBT_WITHOUT_ROOT");
                saveDebt(ledger,root,item);counts.merge(root,1,Integer::sum);
            }
        }catch(Exception e){throw new IllegalStateException("CANNOT_SAVE_DEBT",e);}};
        debtSink=route;
        ELog elog=new ELog(){public void log(String m){log.println(java.time.Instant.now()+" "+m);}public void logE(Exception e){e.printStackTrace(log);}};
        ledger.setAutoCommit(false);
        try {
            for(String root:selected){
                if(Files.exists(dir.resolve("STOP")))break;
                current[0]=root;
                try{Product p=family(src,root);for(String id:sourceModels.keySet())owners.put(id,root);
                    Files.writeString(dir.resolve("source").resolve(root+".json"),new JSONObject(sourceModels).toString());ready.add(root);
                }catch(Exception e){route.accept(new JSONObject().put("kind","FAMILY_BLOCKED").put("error",e.toString()));record(ledger,root,"FAILED",e.toString());}
                ledger.commit();phaseStatus(dir,"INDEXING_COHORT",selected.size(),ready.size());
            }
            if(ready.isEmpty())return selected.size();
            StepIndex allIndex=StepXmlStreamingParser.indexProducts(streamSaved(dir,ready));
            if(!apply){for(String root:ready)record(ledger,root,"VALIDATED_SOURCE","interfamily cohort");ledger.commit();return selected.size();}
            try(DBAccessDataStub db=new DBAccessDataStub(elog)){
                StepDbSnapshot snap=StepDbSnapshot.load(db,allIndex);
                Set<String> collided=new HashSet<>();
                Map<String,String> incomingProducts=new HashMap<>(),incomingArticles=new HashMap<>();
                for(int entity:new int[]{1100,1000}){
                    Map<String,String> incoming=entity==1100?allIndex.getProductSkuById():allIndex.getArticleSkuById();
                    Map<String,String> existing=entity==1100?snap.productBySku:snap.articleBySku;
                    Map<String,String> seen=entity==1100?incomingProducts:incomingArticles;
                    for(Map.Entry<String,String> e:incoming.entrySet()){
                        String other=existing.get(e.getValue());if(other!=null&&!other.equals(e.getKey()))collided.add(owners.get(e.getKey()));
                        other=seen.putIfAbsent(e.getValue(),e.getKey());if(other!=null&&!other.equals(e.getKey())){collided.add(owners.get(e.getKey()));collided.add(owners.get(other));}
                    }
                }
                for(String root:collided)record(ledger,root,"COLLISION_REVIEW","Existing or incoming SKU has another Identifier");
                ready.removeAll(collided);ledger.commit();
                if(ready.isEmpty())return selected.size();
                StepWriterPipeline writer=new StepWriterPipeline(db,allIndex,new StepStatusComputer(),map,elog);
                writer.setDebtSink(route);
                String[] phases={"IDENTITY","TEXTS","MODEL","CHARACTERISTICS"};
                for(int phase=0;phase<4;phase++){
                    int complete=0;phaseStatus(dir,phases[phase],ready.size(),0);
                    for(String root:ready){current[0]=root;writer.acceptPhase(phase,savedFamily(dir,root));
                        if(++complete%100==0){ledger.commit();phaseStatus(dir,phases[phase],ready.size(),complete);}}
                    writer.finishPhase(phase);ledger.commit();phaseStatus(dir,phases[phase]+"_FLUSHED",ready.size(),complete);
                }
                Map<String,String> productsAfter=db.getObjectInternalIds(1100,allIndex.getProductIds()),articlesAfter=db.getObjectInternalIds(1000,allIndex.getArticleIds());
                int complete=0;
                for(String root:ready){current[0]=root;Product p=savedFamily(dir,root);StepIndex index=StepXmlStreamingParser.indexProducts(List.of(p));
                    boolean found=index.getProductIds().stream().allMatch(productsAfter::containsKey)&&index.getArticleIds().stream().allMatch(articlesAfter::containsKey);
                    if(!found)route.accept(new JSONObject().put("kind","MISSING_IDENTITIES").put("products",index.getProductIds()).put("articles",index.getArticleIds()));
                    if(found)try{publish(ledger,root,SQLiteMediaAdapter.build(src,root,sourceModels,route),pub,route);}
                    catch(Exception e){route.accept(new JSONObject().put("kind","PUBSUB_BUILD_OR_SEND").put("error",e.toString()));}
                    int debts=counts.getOrDefault(root,0);
                    record(ledger,root,debts>0?"WRITTEN_WITH_DEBT":"WRITTEN_IDENTITIES_VERIFIED","interfamily; debtItems="+debts+"; identitiesVerified="+found+"; fullAttributeComparison=NOT_PERFORMED");
                    ledger.commit();phaseStatus(dir,"PUBSUB_AFTER_FLUSH",ready.size(),++complete);
                }
            }
            return selected.size();
        }catch(Exception e){ledger.rollback();throw e;}
        finally{ledger.setAutoCommit(true);}
    }

    public static void main(String[] args)throws Exception {
        if(args.length<2)throw new IllegalArgumentException("NeoSQLite source.sqlite output-directory [--apply] [--once] [--limit=N] [--roots-file=path]");
        boolean apply=Arrays.asList(args).contains("--apply"),once=Arrays.asList(args).contains("--once");int limit=0;
        for(String a:args)if(a.startsWith("--limit="))limit=Integer.parseInt(a.substring(8));
        String rootsFile=null;for(String arg:args)if(arg.startsWith("--roots-file="))rootsFile=arg.substring(13);
        Path dir=Path.of(args[1]).toAbsolutePath();Files.createDirectories(dir);
        try(var channel=java.nio.channels.FileChannel.open(dir.resolve("worker.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock()){
            if(lock==null)throw new IllegalStateException("ALREADY_RUNNING");Files.writeString(dir.resolve("worker.pid"),""+ProcessHandle.current().pid());
            Class.forName("org.sqlite.JDBC");
            try(Connection src=DriverManager.getConnection("jdbc:sqlite:"+Path.of(args[0]).toAbsolutePath().toUri()+"?mode=ro");Connection ledger=DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("delivery.sqlite"));PrintWriter log=new PrintWriter(Files.newBufferedWriter(dir.resolve("api.log"),StandardOpenOption.CREATE,StandardOpenOption.APPEND),true)){
                try(Statement q=ledger.createStatement()){q.execute("PRAGMA journal_mode=WAL");q.execute("CREATE TABLE IF NOT EXISTS result_v2(root_id TEXT PRIMARY KEY,state TEXT,reason TEXT,updated_at TEXT)");}
                try(Statement q=src.createStatement()){q.execute("PRAGMA busy_timeout=15000");q.execute("ATTACH DATABASE '"+dir.resolve("delivery.sqlite").toString().replace("'","''")+"' AS delivery");}
                String selectionTable="priority_root";
                if(rootsFile!=null){
                    try(Statement q=src.createStatement()){q.execute("CREATE TEMP TABLE requested_root(root_id TEXT PRIMARY KEY)");}
                    int roots=0;
                    try(BufferedReader input=Files.newBufferedReader(Path.of(rootsFile));PreparedStatement q=src.prepareStatement("INSERT OR IGNORE INTO requested_root VALUES(?)")){
                        String line;while((line=input.readLine())!=null){String id=line.strip();if(id.isEmpty())continue;q.setString(1,id);q.addBatch();if(++roots%900==0)q.executeBatch();}q.executeBatch();
                    }
                    if(roots==0)throw new IllegalArgumentException("EMPTY_ROOTS_FILE");selectionTable="requested_root";
                }
                try(Statement q=src.createStatement()){q.execute("PRAGMA query_only=ON");}
                try(Statement q=ledger.createStatement()){
                    q.execute("PRAGMA busy_timeout=15000");
                    q.execute("CREATE TABLE IF NOT EXISTS debt(id TEXT PRIMARY KEY,root_id TEXT,kind TEXT,detail TEXT,state TEXT DEFAULT 'OPEN',created_at TEXT,updated_at TEXT)");
                    q.execute("CREATE INDEX IF NOT EXISTS debt_root ON debt(root_id,state)");
                    q.execute("CREATE TABLE IF NOT EXISTS pubsub_outbox(root_id TEXT PRIMARY KEY,payload TEXT,state TEXT,message_id TEXT,updated_at TEXT)");
                }
                int processed=0;Map<String,String> map=apply?statuses():Map.of();
                PubSubGCP pub=apply?new PubSubGCP(PropertiesManager.get("p360.contingency.gcp.service_account_back"),PropertiesManager.get("p360.contingency.gcp.project_back"),PropertiesManager.get("p360.contingency.gcp.post_products_topic")):null;
                try { while(!Files.exists(dir.resolve("STOP"))){
                    List<String> selected=new ArrayList<>();
                    try(Statement q=src.createStatement();ResultSet r=q.executeQuery("SELECT r.root_id FROM "+selectionTable+" p CROSS JOIN migration_root r ON r.root_id=p.root_id WHERE r.state='STAGED' AND NOT EXISTS(SELECT 1 FROM delivery.result_v2 d WHERE d.root_id=r.root_id) LIMIT "+(limit>0?Math.min(cohortSize(),limit-processed):cohortSize()))){while(r.next())selected.add(r.getString(1));}
                    if(selected.isEmpty()){Files.writeString(dir.resolve("status.json"),new JSONObject().put("state",once?"IDLE":"WAITING_EXTRACTION").put("processedThisLaunch",processed).put("at",java.time.Instant.now().toString()).toString());if(once)break;Thread.sleep(10000);continue;}
                    processed += processCohort(src,ledger,dir,selected,apply,map,pub,log);
                    if(limit>0&&processed>=limit)return;
                } } finally { if(pub!=null)pub.close(); }
            }
        }
    }
}

