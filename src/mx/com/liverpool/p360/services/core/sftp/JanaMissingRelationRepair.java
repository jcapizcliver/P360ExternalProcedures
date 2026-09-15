package mx.com.liverpool.p360.services.core.sftp;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/** Applies only missing associations from a reviewed, immutable candidate file. */
public class JanaMissingRelationRepair {
    interface Gateway {
        JSONObject audit(JSONObject candidate)throws Exception;
        JSONObject article(String id)throws Exception;
        JSONObject product(String id)throws Exception;
        void write(JSONObject request)throws Exception;
    }
    static JSONObject request(String article,String product){
        return new JSONObject().put("columns",new JSONArray().put(new JSONObject().put("identifier","ProductReference.ReferencedSupplierAid")))
            .put("rows",new JSONArray().put(new JSONObject().put("object",new JSONObject().put("id","'"+article+"'@1"))
                .put("qualification",new JSONObject().put("referencedSupplierAid",product)).put("values",new JSONArray().put(product))));
    }
    static boolean parentIs(JSONObject a,String product){
        JSONArray p=a.optJSONArray("higherLevelProduct");return p!=null&&p.length()==1&&product.equals(p.getJSONObject(0).getJSONObject("_qualification").optString("referencedIdentifier"));
    }
    static String sapType(JSONObject data,String section){
        if(data==null)return "";JSONArray rows=data.optJSONArray(section);if(rows==null)return "";
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i),q=row.optJSONObject("_qualification");
            JSONObject market=q==null?null:q.optJSONObject("targetMarket");
            if(market!=null&&"MX".equals(market.optString("_key",market.optString("_code")))){
                JSONObject type=row.optJSONObject("sapObjectType");return type==null?"":type.optString("_code","");
            }
        }
        return "";
    }
    static JSONObject repair(JSONObject c,Gateway g,boolean apply)throws Exception{
        JSONObject r=new JSONObject(c.toString()).put("checkedAt",java.time.Instant.now().toString());
        if(!c.optString("status").equals("MISSING_REFERENCE"))return r.put("result","NOT_A_CANDIDATE");
        String article=c.getString("article"),product=c.getString("expectedProduct");
        if(article.isBlank()||product.isBlank())throw new IllegalArgumentException("Empty Identifier");
        JSONObject fresh=g.audit(c);
        if(!article.equals(fresh.optString("article"))||!product.equals(fresh.optString("expectedProduct")))return r.put("result","TARGET_CHANGED").put("current",fresh);
        if(fresh.optString("status").equals("OK"))return r.put("result","ALREADY_CORRECT");
        if(!fresh.optString("status").equals("MISSING_REFERENCE"))return r.put("result","SKIPPED_CURRENT_STATE").put("current",fresh);
        JSONObject a=g.article(article);
        if(a==null||!c.getString("sku").equals(a.optString("sku")))return r.put("result","ARTICLE_SKU_CHANGED");
        if(!c.getString("attyp").equals(sapType(a,"articleExtraData")))return r.put("result","ARTICLE_TYPE_CHANGED_OR_UNKNOWN");
        JSONObject p=g.product(product);String expectedType=c.getString("attyp").equals("00")?"00":"01";
        if(!expectedType.equals(sapType(p,"productExtraData")))return r.put("result","PRODUCT_TYPE_CHANGED_OR_UNKNOWN");
        JSONArray parents=a.optJSONArray("higherLevelProduct");
        if(parents!=null&&parents.length()>0)return r.put("result",parentIs(a,product)?"ALREADY_CORRECT":"PARENT_NOW_PRESENT");
        if(!apply)return r.put("result","READY");
        g.write(request(article,product));
        JSONObject after=g.article(article);
        if(after==null||!parentIs(after,product))throw new IOException("Write submitted but association not verified; inspect current state before retry");
        return r.put("result","APPLIED_VERIFIED");
    }
    static void plan(Path report,Path out)throws Exception{
        Files.createDirectories(out);Path target=out.resolve("candidates.jsonl");
        if(Files.exists(target))throw new IOException("Candidate file already exists; use a new plan directory");
        Map<String,JSONObject> latest=new TreeMap<>();
        try(BufferedReader b=Files.newBufferedReader(report)){String s;while((s=b.readLine())!=null){JSONObject r=new JSONObject(s);latest.put(r.getString("sku"),r);}}
        Map<String,String> products=new HashMap<>();Set<String> conflicts=new HashSet<>();
        for(JSONObject r:latest.values())if(r.optString("status").equals("MISSING_REFERENCE")){
            String a=r.getString("article"),p=r.getString("expectedProduct"),prior=products.putIfAbsent(a,p);if(prior!=null&&!prior.equals(p))conflicts.add(a);
        }
        int count=0;Set<String> seen=new HashSet<>();Path temp=out.resolve("candidates.jsonl.partial");
        try(BufferedWriter w=Files.newBufferedWriter(temp)){
            for(JSONObject r:latest.values())if(r.optString("status").equals("MISSING_REFERENCE")&&!conflicts.contains(r.getString("article"))&&seen.add(r.getString("article"))){w.write(r.toString());w.newLine();count++;}
        }
        Files.move(temp,target);
        Files.writeString(out.resolve("plan-summary.json"),new JSONObject().put("sourceReport",report.toAbsolutePath().toString()).put("candidates",count).put("conflictingArticles",new JSONArray(conflicts)).toString(2));
        System.out.println("PLAN candidates="+count+" conflicts="+conflicts.size());
    }
    static void run(Path candidates,Path out,boolean apply,int max)throws Exception{
        Files.createDirectories(out);
        try(java.nio.channels.FileChannel ch=java.nio.channels.FileChannel.open(out.resolve("repair.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);java.nio.channels.FileLock lock=ch.tryLock()){
            if(lock==null)throw new IOException("Another repair process is running in this output directory");
            JanaProcessedAudit.Reader db=new JanaProcessedAudit.Reader();RESTWrapper rest=new RESTWrapper();
            Gateway g=new Gateway(){
                public JSONObject audit(JSONObject c)throws Exception{return db.audit(c);}
                public JSONObject article(String id)throws Exception{return db.byId("Article",id);}
                public JSONObject product(String id)throws Exception{return db.byId("Product2G",id);}
                public void write(JSONObject body)throws Exception{
                    Map<String,String> q=new HashMap<>();q.put("includeObjectsInProtocol","true");
                    JSONObject result=rest.getRw().makeRequest("POST","/list/Article/ProductReference",q,body.toString());
                    if(result==null||!result.has("counters")||result.getJSONObject("counters").optInt("errors",-1)!=0)
                        throw new IOException("P360 rejected relation: "+String.valueOf(result));
                }
            };
            String mode=apply?"apply":"check";int count=0,errors=0;Map<String,Integer> totals=new TreeMap<>();
            try(BufferedReader b=Files.newBufferedReader(candidates);BufferedWriter w=Files.newBufferedWriter(out.resolve(mode+"-results.jsonl"),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND)){
                String s;while((s=b.readLine())!=null&&(max==0||count<max)){
                    JSONObject c=new JSONObject(s),result;
                    try{
                        Path source=Paths.get(c.getString("source"));
                        if(!Files.isRegularFile(source)||Files.size(source)!=c.getLong("size")||Files.getLastModifiedTime(source).toMillis()!=c.getLong("mtime"))
                            result=new JSONObject(c.toString()).put("result","SOURCE_CHANGED");
                        else result=repair(c,g,apply);
                    }catch(Exception e){result=new JSONObject(c.toString()).put("result","ERROR").put("error",e.toString());}
                    w.write(result.toString());w.newLine();w.flush();count++;String status=result.getString("result");totals.merge(status,1,Integer::sum);
                    errors=status.equals("ERROR")?errors+1:0;
                    Files.writeString(out.resolve(mode+"-summary.json"),new JSONObject(totals).toString(2));
                    if(count%10==0)System.out.println(mode+" "+count+" "+totals);
                    if(errors>=3)throw new IOException("Stopped after three errors; inspect results before resuming");
                    Thread.sleep(500);
                }
            }
            System.out.println(mode+" COMPLETE "+totals);
        }
    }
    public static void main(String[] args){
        try{
            if(args.length<3)throw new IllegalArgumentException("plan <report> <new-plan-dir> | check|apply <candidates> <output-dir> [max]");
            if(args[0].equals("plan"))plan(Paths.get(args[1]),Paths.get(args[2]));
            else if(args[0].equals("check")||args[0].equals("apply"))run(Paths.get(args[1]),Paths.get(args[2]),args[0].equals("apply"),args.length>3?Integer.parseInt(args[3]):0);
            else throw new IllegalArgumentException("Unknown mode");
            System.exit(0);
        }catch(Exception e){e.printStackTrace();System.exit(1);}
    }
}
