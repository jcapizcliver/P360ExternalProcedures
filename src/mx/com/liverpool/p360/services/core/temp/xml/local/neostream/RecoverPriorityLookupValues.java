package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.Instant;
import org.json.*;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/** Standalone priority debt recovery. All P360 mutations go through list REST API. */
public final class RecoverPriorityLookupValues {
    final RESTWrapper rest=new RESTWrapper();
    final Path dir;
    final Set<String> done=new HashSet<>();
    final Set<String> createdHere=new HashSet<>();
    final Map<String,Boolean> allowed=new HashMap<>();
    int created,existing,inactive,failed,ok,rejected;
    RecoverPriorityLookupValues(Path dir){this.dir=dir;}
    static String quote(String s){return "'"+s.replace("\\","\\\\").replace("'","\\'")+"'";}
    static String key(JSONObject x){return new JSONArray().put(x.getString("lookup")).put(x.getString("code")).toString();}
    static boolean active(Object x){return Boolean.TRUE.equals(x)||"true".equalsIgnoreCase(String.valueOf(x))||"1".equals(String.valueOf(x));}
    void event(String file,JSONObject x)throws Exception {
        x.put("at",Instant.now().toString());
        byte[] data=(x.toString()+"\n").getBytes(StandardCharsets.UTF_8);
        try(FileChannel f=FileChannel.open(dir.resolve(file),StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.APPEND)){
            java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(data);while(b.hasRemaining())f.write(b);f.force(true);
        }
    }
    void status(String phase)throws Exception {
        JSONObject j=new JSONObject().put("at",Instant.now().toString()).put("phase",phase).put("created",created)
            .put("existingActive",existing).put("excludedInactive",inactive).put("failedCodes",failed)
            .put("replayedOK",ok).put("replayedFailed",rejected);
        Files.writeString(dir.resolve("status.json"),j.toString());System.out.println(j);
    }
    // Read includes inactive values; an empty result is accepted only from a valid API response.
    Boolean exists(JSONObject c){
        String code=c.getString("code"); List<Boolean> found=new ArrayList<>();
        Map<String,String> p=new TreeMap<>();p.put("lookup",quote(c.getString("lookup")));
        p.put("fields","LookupValue.Code,LookupValue.IsActive");p.put("pageSize","100");
        p.put("query","LookupValue.Code equals \""+code.replace("\\","\\\\").replace("\"","\\\"")+"\"");
        rest.collectData("list","LookupValue",null,"bySearch",p,row->{
            JSONArray v=row.getJSONArray("values");
            if(!code.equals(v.getString(0)))throw new IllegalStateException("NON_EXACT_LOOKUP_RESULT");
            found.add(active(v.opt(1)));
        },error->{throw new IllegalStateException("LOOKUP_READ_FAILED");},false);
        if(found.size()>1)throw new IllegalStateException("DUPLICATE_LOOKUP_CODE");
        return found.isEmpty()?null:found.get(0);
    }
    JSONObject write(String entity,JSONObject payload)throws Exception {
        final String[] response={null}; Map<String,String> p=new TreeMap<>();p.put("includeObjectsInProtocol","false");
        rest.writeData("list",entity,null,p,new JSONObject(payload.toString()),r->response[0]=r);
        if(response[0]==null)throw new IllegalStateException("NO_API_RESPONSE");
        return new JSONObject(response[0]);
    }
    static boolean clean(JSONObject response){
        JSONObject c=response.optJSONObject("counters");return c!=null&&c.optInt("errors",-1)==0&&c.optInt("objectsWithErrors",0)==0;
    }
    void candidate(JSONObject c)throws Exception {
        String k=key(c);JSONObject result=new JSONObject(c.toString());
        try{
            Boolean state=exists(c);
            if(state==null){
                JSONObject req=new JSONObject().put("columns",new JSONArray().put(new JSONObject().put("identifier","LookupValueLang.Name(es)")))
                    .put("rows",new JSONArray().put(new JSONObject().put("object",new JSONObject().put("id",quote(c.getString("code"))+"@"+quote(c.getString("lookup"))))
                        .put("values",new JSONArray().put(c.getString("name")))));
                // First create without activation. Only a confirmed creation by this campaign may be activated.
                event("requests.ndjson",new JSONObject().put("kind","CREATE_LOOKUP_VALUE").put("candidate",c).put("request",req));
                JSONObject response=write("LookupValue",req); result.put("response",response);
                if(!clean(response))throw new IllegalStateException("CREATE_REJECTED");
                if(response.getJSONObject("counters").optInt("createdObjects",0)==1){
                    event("codes.ndjson",new JSONObject(c.toString()).put("status","CREATED_PENDING_VERIFICATION").put("response",response));createdHere.add(k);
                }
                state=exists(c);
                if(state==null)throw new IllegalStateException("CREATE_NOT_VISIBLE");
                if(state){created++;result.put("status","CREATED_VERIFIED_ACTIVE");}
            }else if(state){existing++;result.put("status","EXISTS_ACTIVE");}
            if(Boolean.FALSE.equals(state)&&createdHere.contains(k)){
                JSONObject req=new JSONObject().put("columns",new JSONArray().put(new JSONObject().put("identifier","LookupValue.IsActive")))
                    .put("rows",new JSONArray().put(new JSONObject().put("object",new JSONObject().put("id",quote(c.getString("code"))+"@"+quote(c.getString("lookup"))))
                        .put("values",new JSONArray().put(true))));
                event("requests.ndjson",new JSONObject().put("kind","ACTIVATE_NEW_CAMPAIGN_VALUE").put("candidate",c).put("request",req));
                JSONObject response=write("LookupValue",req);result.put("activationResponse",response);
                if(!clean(response))throw new IllegalStateException("NEW_VALUE_ACTIVATION_REJECTED");
                state=exists(c);if(!Boolean.TRUE.equals(state))throw new IllegalStateException("NEW_VALUE_NOT_ACTIVE");
                created++;result.put("status","CREATED_VERIFIED_ACTIVE");
            }
            if(Boolean.FALSE.equals(state)){inactive++;result.put("status","EXCLUDED_INACTIVE");}
            allowed.put(k,Boolean.TRUE.equals(state));
        }catch(Exception e){failed++;allowed.put(k,false);result.put("status","FAILED").put("error",e.toString());}
        event("codes.ndjson",result);
    }
    void replay(List<JSONObject> cells)throws Exception {
        if(cells.isEmpty())return;
        JSONObject first=cells.get(0);JSONArray rows=new JSONArray();for(JSONObject c:cells)rows.put(c.getJSONObject("row"));
        JSONObject req=new JSONObject().put("columns",new JSONArray().put(new JSONObject().put("identifier",first.getString("column")))).put("rows",rows);
        String batch=UUID.randomUUID().toString();
        event("requests.ndjson",new JSONObject().put("kind","REPLAY").put("batch",batch).put("entity",first.getString("entity")).put("request",req));
        JSONObject response;
        try{response=write(first.getString("entity"),req);}catch(Exception e){response=new JSONObject().put("error",e.toString());}
        event("responses.ndjson",new JSONObject().put("batch",batch).put("response",response));
        // Mixed success is retained for explicit review; never declare every row successful from HTTP alone.
        boolean success=clean(response);
        for(JSONObject c:cells){String id=c.getString("id");event("cells-result.ndjson",new JSONObject().put("id",id).put("batch",batch).put("status",success?"OK":"REVIEW_REJECTED_BATCH").put("object",c.getJSONObject("row").getJSONObject("object")));
            if(success){done.add(id);ok++;}else rejected++;
        }
        status("REPLAY");
    }
    void run()throws Exception {
        Path codes=dir.resolve("codes.ndjson");
        if(Files.exists(codes))for(String line:Files.readAllLines(codes)){JSONObject j=new JSONObject(line);JSONObject r=j.optJSONObject("response");
            if(r!=null&&clean(r)&&r.getJSONObject("counters").optInt("createdObjects",0)==1)createdHere.add(key(j));
        }
        Path results=dir.resolve("cells-result.ndjson");
        if(Files.exists(results))for(String line:Files.readAllLines(results)){try{JSONObject j=new JSONObject(line);if("OK".equals(j.optString("status")))done.add(j.getString("id"));}catch(Exception e){throw new IllegalStateException("INVALID_CHECKPOINT",e);}}
        status("CREATE_VALUES");
        for(String line:Files.readAllLines(dir.resolve("candidates.ndjson"))){if(Files.exists(dir.resolve("STOP"))){status("STOPPED");return;}candidate(new JSONObject(line));if((created+existing+inactive+failed)%25==0)status("CREATE_VALUES");}
        Map<String,List<JSONObject>> groups=new LinkedHashMap<>();
        for(String line:Files.readAllLines(dir.resolve("cells.ndjson"))){JSONObject c=new JSONObject(line);if(done.contains(c.getString("id"))||!Boolean.TRUE.equals(allowed.get(key(c))))continue;
            groups.computeIfAbsent(c.getString("entity")+"\n"+c.getString("column"),k->new ArrayList<>()).add(c);
        }
        for(List<JSONObject> group:groups.values())for(int i=0;i<group.size();i+=900){if(Files.exists(dir.resolve("STOP"))){status("STOPPED");return;}replay(group.subList(i,Math.min(i+900,group.size())));}
        status("FINISHED");
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=2||!"--apply".equals(args[1]))throw new IllegalArgumentException("Usage: RecoverPriorityLookupValues <plan-directory> --apply");
        Path dir=Path.of(args[0]);try(FileChannel f=FileChannel.open(dir.resolve("worker.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=f.tryLock()){
            if(lock==null)throw new IllegalStateException("ALREADY_RUNNING");new RecoverPriorityLookupValues(dir).run();
        }
    }
}
