package mx.com.liverpool.p360.services.core;
import java.util.concurrent.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Coalesces identical in-flight batches without rejecting independent requests. */
public final class ForoRequestGate {
    private static final ConcurrentHashMap<String,CompletableFuture<String>> RUNNING=new ConcurrentHashMap<>();
    private ForoRequestGate() { }
    public static String execute(String input,Callable<String> action)throws Exception {
        String key=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(canonical(new JSONObject(input)).getBytes(StandardCharsets.UTF_8)));
        CompletableFuture<String> mine=new CompletableFuture<>(),existing=RUNNING.putIfAbsent(key,mine);
        if(existing!=null) {
            try{return existing.get();}
            catch(ExecutionException e){throw new IllegalStateException("Shared request failed",e.getCause());}
        }
        try {
            String result=action.call();mine.complete(result);return result;
        } catch(Exception e){mine.completeExceptionally(e);throw e;}
        catch(Error e){mine.completeExceptionally(e);throw e;}
        finally {RUNNING.remove(key,mine);}
    }
    private static String canonical(Object value) {
        if(value instanceof JSONObject) {
            JSONObject o=(JSONObject)value;List<String> keys=new ArrayList<>();
            for(Object k:o.keySet())keys.add(String.valueOf(k));Collections.sort(keys);
            StringJoiner j=new StringJoiner(",","{","}");for(String k:keys)j.add(JSONObject.quote(k)+":"+canonical(o.get(k)));return j.toString();
        }
        if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringJoiner j=new StringJoiner(",","[","]");for(int i=0;i<a.length();i++)j.add(canonical(a.get(i)));return j.toString();}
        if(value instanceof String)return JSONObject.quote((String)value);return String.valueOf(value);
    }
}
