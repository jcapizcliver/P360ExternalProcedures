package mx.com.liverpool.p360.services.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.json.*;

/** Idempotent persistence boundary for AgarraloONo; no cached business data. */
public final class TakeNoTakeWriter {
    private static final Object[] LOCKS = new Object[256];
    private static final AtomicLong SKIPPED = new AtomicLong(), WRITTEN = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(TakeNoTakeWriter.class.getName());
    static { for (int i=0;i<LOCKS.length;i++) LOCKS[i]=new Object(); }
    private TakeNoTakeWriter() { }

    public static JSONObject makeRequest(RESTWorkshop rest,String method,String path,Map<String,String> query,String body) {
        if (!"PUT".equals(method) || body==null ||
                !(path.startsWith("/object/Article/") || path.startsWith("/object/Product2G/")))
            return rest.makeRequest(method,path,query,body);
        JSONObject desired=new JSONObject(body);
        JSONArray records=desired.optJSONArray("_characteristicRecords");
        if(records==null) return rest.makeRequest(method,path,query,body);
        synchronized(LOCKS[(path.hashCode()&0x7fffffff)%LOCKS.length]) {
            Map<String,String> readQuery=new HashMap<>();
            readQuery.put("includeLabels","true");readQuery.put("includeIds","true");
            JSONObject response=rest.makeRequest("GET",path,readQuery,null);
            JSONObject current=response==null?null:response.optJSONObject("_data");
            if(current==null) throw new IllegalStateException("Cannot read current take/no-take state: "+path);
            JSONArray delta=delta(current.optJSONArray("_characteristicRecords"),records);
            if(delta.length()==0) {
                long n=SKIPPED.incrementAndGet();
                if(n==1 || n%250==0) LOG.info("takeNoTake unchanged="+n+" writes="+WRITTEN.get());
                return new JSONObject().put("_unchanged",true).put("_protocol",new JSONObject().put("errorCounter",0));
            }
            JSONObject update=new JSONObject(desired.toString()).put("_characteristicRecords",delta);
            JSONObject result=rest.makeRequest(method,path,query,update.toString());
            if(result!=null) WRITTEN.incrementAndGet();
            return result;
        }
    }

    static JSONArray delta(JSONArray current,JSONArray desired) {
        JSONArray result=new JSONArray();
        JSONObject decision=find(desired,"AssignTakeNoTake");
        JSONObject oldDecision=find(current,"AssignTakeNoTake");
        boolean decisionChanged=decision!=null && !sameRecord(oldDecision,decision);
        for(int i=0;i<desired.length();i++) {
            JSONObject next=desired.getJSONObject(i);String code=code(next);
            JSONObject old=find(current,code);
            if("AdmissionDate".equals(code) && hasValue(old) && !decisionChanged) continue;
            if(!sameRecord(old,next)) result.put(new JSONObject(next.toString()));
        }
        return result;
    }
    private static String code(JSONObject r) {
        JSONObject q=r.optJSONObject("_qualification");
        JSONObject c=q==null?null:q.optJSONObject("characteristic");
        return c==null?"":c.optString("_code","");
    }
    private static JSONObject find(JSONArray records,String code) {
        JSONObject match=null;
        if(records!=null) for(int i=0;i<records.length();i++) {
            JSONObject r=records.optJSONObject(i);
            if(r!=null && code.equals(code(r))) { if(match!=null)return null;match=r; }
        }
        return match;
    }
    private static String language(JSONObject r) {
        JSONObject q=r.optJSONObject("_qualification");JSONObject l=q==null?null:q.optJSONObject("language");
        if(l==null)return "";
        String code=l.optString("_code","");
        if(!code.isEmpty())return code;
        String key=l.optString("_key","");return "-1".equals(key)?"zxx":key;
    }
    private static boolean hasValue(JSONObject r) {
        if(r==null)return false;
        JSONArray langs=r.optJSONArray("_recordLang");
        if(langs!=null)for(int i=0;i<langs.length();i++) {
            JSONObject l=langs.optJSONObject(i);JSONArray a=l==null?null:l.optJSONArray("values");
            if(a!=null)for(int j=0;j<a.length();j++)if(!empty(a.opt(j)))return true;
        }
        return false;
    }
    private static boolean sameRecord(JSONObject old,JSONObject next) {
        if(old==null)return !hasValue(next);
        JSONArray wanted=next.optJSONArray("_recordLang"),existing=old.optJSONArray("_recordLang");
        if(wanted==null||existing==null)return false;
        for(int i=0;i<wanted.length();i++) {
            JSONObject w=wanted.getJSONObject(i);JSONObject found=null;
            for(int j=0;j<existing.length();j++) {
                JSONObject e=existing.getJSONObject(j);
                if(language(w).equals(language(e))) {if(found!=null)return false;found=e;}
            }
            if(found==null || !sameValues(found.optJSONArray("values"),w.optJSONArray("values")))return false;
        }
        return true;
    }
    private static boolean sameValues(JSONArray old,JSONArray next) {
        if(old==null||next==null)return false;
        if(old.length()==0 && next.length()==1 && empty(next.opt(0)))return true;
        if(next.length()==0 && old.length()==1 && empty(old.opt(0)))return true;
        if(old.length()!=next.length())return false;
        for(int i=0;i<old.length();i++)if(!sameValue(old.opt(i),next.opt(i)))return false;
        return true;
    }
    private static boolean empty(Object v){return v==null||v==JSONObject.NULL||"".equals(v);}
    private static boolean sameValue(Object old,Object next) {
        if(empty(old)||empty(next))return empty(old)&&empty(next);
        if(old instanceof JSONObject) {
            JSONObject o=(JSONObject)old;
            if(next instanceof JSONObject) {
                JSONObject n=(JSONObject)next;
                if(n.has("_code"))return n.optString("_code").equals(o.optString("_code"));
                return o.toString().equals(n.toString());
            }
            return String.valueOf(next).equals(o.optString("_code","")) || String.valueOf(next).equals(o.optString("_label",""));
        }
        return String.valueOf(old).equals(String.valueOf(next));
    }
}
