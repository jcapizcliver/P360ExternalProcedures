package mx.com.liverpool.p360.services.core.sftp;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.json.*;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/** JDBC revalidation, List API writes only, at most 900 rows per POST. */
public final class SapBatchRecovery {
    static final int BATCH=900;
    static final String ACTIVE=JanaDatabaseAudit.ACTIVE;
    static Connection connect() throws Exception {
        Properties p=new Properties();
        p.setProperty("user",Objects.requireNonNull(System.getenv("ORACLE_JDBC_USER")));
        p.setProperty("password",Objects.requireNonNull(System.getenv("ORACLE_JDBC_PASSWORD")));
        p.setProperty("oracle.jdbc.ReadTimeout","180000");
        return DriverManager.getConnection(Objects.requireNonNull(System.getenv("ORACLE_JDBC_URL")),p);
    }
    static JanaDatabaseAudit.Snapshot snapshot(Connection c,List<JSONObject> rows,boolean relation)throws Exception {
        Set<String> ids=new HashSet<>(),skus=new HashSet<>();
        for(JSONObject r:rows){ids.add(r.getString(relation?"article":"identifier"));if(relation)ids.add(r.getString("expectedProduct"));skus.add(r.getString("sku"));if(relation&&!r.optString("parentSku").isEmpty())skus.add(r.getString("parentSku"));}
        JanaDatabaseAudit.Snapshot db=new JanaDatabaseAudit.Snapshot();
        JanaDatabaseAudit.entities(c,db,ids,false);JanaDatabaseAudit.entities(c,db,skus,true);
        if(relation)JanaDatabaseAudit.parents(c,db);
        return db;
    }
    static Map<Long,Set<String>> types(Connection c,JanaDatabaseAudit.Snapshot db)throws Exception {
        List<Long> ids=new ArrayList<>(db.revisions.keySet());Map<Long,Set<String>> result=new HashMap<>();
        for(int start=0;start<ids.size();start+=500){List<Long> sub=ids.subList(start,Math.min(start+500,ids.size()));
            String sql="SELECT /*+ leading(d ar l) use_nl(ar l) */ d.\"ArticleRevisionID\",l.\"Code\" FROM \"ArticleDomain\" d JOIN \"ArticleRevision\" ar ON ar.\"ID\"=d.\"ArticleRevisionID\" JOIN PIM_MAIN.\"LookupValueRevision\" l ON l.\"LookupValueID\"=CASE WHEN ar.\"EntityID\"=1000 THEN d.\"Res_Int_03\" ELSE d.\"Res_Int_08\" END AND l.\"RevisionID\"=1 AND l.\"DeletionTimestamp\"="+ACTIVE+" WHERE d.\"DeletionTimestamp\"="+ACTIVE+" AND d.\"ArticleRevisionID\" IN ("+JanaDatabaseAudit.placeholders(sub.size())+")";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setQueryTimeout(60);p.setFetchSize(2000);for(int i=0;i<sub.size();i++)p.setLong(i+1,sub.get(i));try(ResultSet r=p.executeQuery()){while(r.next())result.computeIfAbsent(r.getLong(1),k->new HashSet<>()).add(r.getString(2));}}
        }return result;
    }
    static JanaDatabaseAudit.Node node(JanaDatabaseAudit.Snapshot db,String entity,String id){List<JanaDatabaseAudit.Node> list=db.identifiers.get((entity.equals("Article")?1000:1100)+":"+id);return list!=null&&list.size()==1?list.get(0):null;}
    static boolean sameType(Map<Long,Set<String>> types,JanaDatabaseAudit.Node n,String type){Set<String> t=n==null?null:types.get(n.revision);return t!=null&&t.size()==1&&t.contains(type);}
    static String eligible(JSONObject r,JanaDatabaseAudit.Snapshot db,Map<Long,Set<String>> types,boolean relation){
        String entity=relation?"Article":r.getString("entity");String id=r.getString(relation?"article":"identifier"),sku=r.getString("sku");
        JanaDatabaseAudit.Node a=node(db,entity,id);if(a==null)return "MISSING_OR_AMBIGUOUS_TARGET";
        if(relation){
            JanaDatabaseAudit.Node p=node(db,"Product2G",r.getString("expectedProduct"));if(p==null)return "MISSING_OR_AMBIGUOUS_PRODUCT";
            if(!sku.equals(a.sku))return "ARTICLE_SKU_CHANGED";
            if(a.parents.size()==1&&a.parents.contains(p.id))return "ALREADY_CORRECT";
            if(!a.parents.isEmpty())return "PARENT_ALREADY_PRESENT";
            String type=r.getString("attyp"),pt=type.equals("02")?"01":type;
            if(!sameType(types,a,type)||!sameType(types,p,pt))return "TYPE_CHANGED_OR_UNKNOWN";
            String ps=type.equals("02")?r.optString("parentSku"):sku;
            if(!ps.equals(p.sku))return "PRODUCT_SKU_CHANGED";
            List<JanaDatabaseAudit.Node> matches=db.skus.get("1100:"+ps);
            if(matches==null||matches.size()!=1||!matches.get(0).id.equals(p.id))return "AMBIGUOUS_PRODUCT_SKU";
        }else{
            if(sku.equals(a.sku))return "ALREADY_CORRECT";
            if(a.sku!=null&&!a.sku.isBlank())return "EXISTING_SKU_PRESERVED";
            if(!sameType(types,a,r.getString("expectedType"))){
                Set<String> actualTypes=types.get(a.revision);
                // Only an explicit XML ID can tolerate an absent SAP type for a missing SKU.
                boolean explicitMissingType=(actualTypes==null||actualTypes.isEmpty())
                    && id.equals(r.optString("incomingId"))
                    && r.getString("expectedType").equals(r.optString("attyp"));
                if(!explicitMissingType)return "TYPE_CHANGED_OR_UNKNOWN";
            }
            List<JanaDatabaseAudit.Node> matches=db.skus.get(a.entity+":"+sku);
            if(matches!=null&&!matches.isEmpty())return "SKU_ALREADY_ASSIGNED_ELSEWHERE";
        }return "READY";
    }
    static JSONObject request(List<JSONObject> rows,boolean relation,String entity){
        JSONObject body=new JSONObject().put("columns",new JSONArray().put(new JSONObject().put("identifier",relation?"ProductReference.ReferencedSupplierAid":entity+".SKU"))).put("rows",new JSONArray());
        for(JSONObject r:rows){String id=r.getString(relation?"article":"identifier");JSONObject row=new JSONObject().put("object",new JSONObject().put("id","'"+id+"'@1"));
            if(relation)row.put("qualification",new JSONObject().put("referencedSupplierAid",r.getString("expectedProduct"))).put("values",new JSONArray().put(r.getString("expectedProduct")));
            else row.put("values",new JSONArray().put(Long.parseLong(r.getString("sku"))));body.getJSONArray("rows").put(row);
        }return body;
    }
    static boolean verified(JSONObject r,JanaDatabaseAudit.Snapshot db,boolean relation){
        JanaDatabaseAudit.Node n=node(db,relation?"Article":r.getString("entity"),r.getString(relation?"article":"identifier"));
        return n!=null&&(relation?n.parents.size()==1&&n.parents.contains(r.getString("expectedProduct")):r.getString("sku").equals(n.sku));
    }
    static void writeResult(BufferedWriter out,JSONObject r,String result,Map<String,Integer> counts)throws Exception{
        JSONObject copy=new JSONObject(r.toString()).put("result",result).put("checkedAt",java.time.Instant.now().toString());out.write(copy.toString());out.newLine();counts.merge(result,1,Integer::sum);
    }
    static void run(Path input,Path output,boolean relation,boolean apply,int maxBatches)throws Exception{
        Files.createDirectories(output);Files.createDirectories(output.resolve("batches"));
        try(java.nio.channels.FileChannel lc=java.nio.channels.FileChannel.open(output.resolve("run.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);java.nio.channels.FileLock lock=lc.tryLock()){
            if(lock==null)throw new IOException("Campaign already running");
            Map<String,String> targetValues=new HashMap<>();Set<String> conflicts=new HashSet<>();
            try(BufferedReader b=Files.newBufferedReader(input)){String line;while((line=b.readLine())!=null){JSONObject r=new JSONObject(line);String k=(relation?"Article":r.getString("entity"))+":"+r.getString(relation?"article":"identifier"),v=r.getString(relation?"expectedProduct":"sku"),old=targetValues.putIfAbsent(k,v);if(old!=null&&!old.equals(v))conflicts.add(k);}}
            if(!conflicts.isEmpty())throw new IOException("Conflicting targets in candidate file: "+conflicts.size());
            Set<String> seen=new HashSet<>();Map<String,Integer> counts=new TreeMap<>();int batch=0;
            String run=Long.toString(System.currentTimeMillis());RESTWrapper rest=apply?new RESTWrapper():null;
            try(Connection c=connect();BufferedReader b=Files.newBufferedReader(input);BufferedWriter result=Files.newBufferedWriter(output.resolve(run+"-results.jsonl"))){
                String pending=null;
                while(true){List<JSONObject> rows=new ArrayList<>();String entity=null;
                    while(rows.size()<BATCH){String line=pending!=null?pending:b.readLine();pending=null;if(line==null)break;JSONObject r=new JSONObject(line);String e=relation?"Article":r.getString("entity");
                        if(entity!=null&&!e.equals(entity)){pending=line;break;}entity=e;String key=e+":"+r.getString(relation?"article":"identifier");if(seen.add(key))rows.add(r);
                    }
                    if(rows.isEmpty())break;if(maxBatches>0&&batch>=maxBatches)break;batch++;
                    JanaDatabaseAudit.Snapshot db=snapshot(c,rows,relation);Map<Long,Set<String>> types=types(c,db);List<JSONObject> ready=new ArrayList<>();
                    for(JSONObject r:rows){String status=eligible(r,db,types,relation);if(status.equals("READY"))ready.add(r);else writeResult(result,r,status,counts);}
                    if(!ready.isEmpty()){
                        JSONObject body=request(ready,relation,entity);String prefix=run+"-"+String.format("%06d",batch);
                        Files.writeString(output.resolve("batches/"+prefix+"-request.json"),body.toString());
                        if(apply){
                            JSONObject response=rest.getRw().makeRequest("POST",relation?"/list/Article/ProductReference":"/list/"+entity,Map.of("includeObjectsInProtocol","true"),body.toString());
                            Files.writeString(output.resolve("batches/"+prefix+"-response.json"),String.valueOf(response));
                            if(response==null||!response.has("counters")||response.getJSONObject("counters").optInt("errors",-1)!=0)throw new IOException("Batch rejected or uncertain; inspect "+prefix);
                            JanaDatabaseAudit.Snapshot after=null;boolean all=false;
                            for(int attempt=0;attempt<3;attempt++){after=snapshot(c,ready,relation);all=true;for(JSONObject r:ready)if(!verified(r,after,relation)){all=false;break;}if(all)break;Thread.sleep(2000);}
                            for(JSONObject r:ready)writeResult(result,r,verified(r,after,relation)?"APPLIED_VERIFIED":"UNVERIFIED",counts);
                            if(!all){result.flush();throw new IOException("Post-write verification failed; inspect "+prefix);}
                        }else for(JSONObject r:ready)writeResult(result,r,"READY",counts);
                    }
                    result.flush();Files.writeString(output.resolve("summary.json"),new JSONObject(counts).put("batch",batch).put("apply",apply).put("updatedAt",java.time.Instant.now().toString()).toString(2));
                    System.out.println("BATCH "+batch+" rows="+rows.size()+" posted="+(apply?ready.size():0)+" "+counts);
                }
            }
            Files.writeString(output.resolve("complete.json"),new JSONObject(counts).put("apply",apply).put("limitedRun",maxBatches>0).toString(2));
        }
    }
    public static void main(String[] a){try{if(a.length<4)throw new IllegalArgumentException("relations|skus check|apply input output [maxBatches]");if(!Set.of("relations","skus").contains(a[0])||!Set.of("check","apply").contains(a[1]))throw new IllegalArgumentException("Invalid mode");run(Paths.get(a[2]),Paths.get(a[3]),a[0].equals("relations"),a[1].equals("apply"),a.length>4?Integer.parseInt(a[4]):0);System.exit(0);}catch(Exception e){e.printStackTrace();System.exit(1);}}
}
