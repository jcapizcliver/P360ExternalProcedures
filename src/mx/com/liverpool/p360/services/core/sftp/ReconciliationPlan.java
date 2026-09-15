package mx.com.liverpool.p360.services.core.sftp;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline proposal only. Input: {base:{product:{},articles:[]},sources:[...] }.
 * Objects may be raw _data or Object API envelopes. Never executes persistence.
 */
public final class ReconciliationPlan {
    private static final Set<String> EXCLUDED=Set.of("identifier","sku","gtin","supplierAid","higherLevelProduct","catalog","log","ownLog","statusModification");
    private static final Set<String> LOG_CODES=Set.of("statusmodification","log","ownlog");
    private static JSONObject data(JSONObject o) {
        return new JSONObject(o.optJSONObject("_data")!=null?o.getJSONObject("_data").toString():o.toString());
    }
    private static Object clean(Object value) {
        if(value instanceof JSONObject) {
            JSONObject o=(JSONObject)value;
            for(Object keyObject:new ArrayList<>(o.keySet())) {
                String key=String.valueOf(keyObject);
                if(LOG_CODES.contains(key.toLowerCase(Locale.ROOT))) o.remove(key);
                else o.put(key,clean(o.get(key)));
            }
        } else if(value instanceof JSONArray) {
            JSONArray a=(JSONArray)value, kept=new JSONArray();
            for(int i=0;i<a.length();i++) {
                Object item=a.get(i);
                if(item instanceof JSONObject) {
                    String code=ProductDataReconciler.nestedValue((JSONObject)item,"_qualification.characteristic._code");
                    if(LOG_CODES.contains(code.toLowerCase(Locale.ROOT))) continue;
                }
                kept.put(clean(item));
            }
            return kept;
        }
        return value;
    }
    private static Set<String> signatures(JSONObject o) {
        Set<String> s=new LinkedHashSet<>();
        for(String key:List.of("sku","identifier","supplierAid")) {
            String v=o.optString(key,"").trim();
            if(!v.isEmpty()&&!v.equals("null")) s.add((key.equals("sku")?"SKU|":"ID|")+v);
        }
        return s;
    }
    private static void conflicts(String path,Object target,Object source,JSONArray conflicts) {
        if(target instanceof JSONObject && source instanceof JSONObject) {
            JSONObject t=(JSONObject)target,s=(JSONObject)source;
            for(Object keyObject:s.keySet()) {String k=String.valueOf(keyObject); if(!EXCLUDED.contains(k)) conflicts(path+"/"+k,t.opt(k),s.opt(k),conflicts);}
        } else if(!ProductDataReconciler.isEmptyJsonValue(target) && !ProductDataReconciler.isEmptyJsonValue(source)
                && !String.valueOf(target).equals(String.valueOf(source))) {
            conflicts.put(new JSONObject().put("path",path).put("base",target).put("source",source));
        }
    }
    public static JSONObject plan(JSONObject input) {
        JSONObject base=input.getJSONObject("base"), product=data(base.getJSONObject("product"));
        JSONArray articles=new JSONArray(),changes=new JSONArray(),conflicts=new JSONArray();
        JSONArray originals=base.optJSONArray("articles");
        if(originals!=null) for(int i=0;i<originals.length();i++) articles.put(data(originals.getJSONObject(i)));
        JSONArray sources=input.getJSONArray("sources");
        for(int n=0;n<sources.length();n++) {
            JSONObject donor=sources.getJSONObject(n), p=(JSONObject)clean(data(donor.getJSONObject("product")));
            conflicts("/product",product,p,conflicts);
            ProductDataReconciler.mergeObjectMissing(product,p,EXCLUDED);
            JSONArray variants=donor.optJSONArray("articles");
            if(variants==null) continue;
            for(int i=0;i<variants.length();i++) {
                JSONObject source=(JSONObject)clean(data(variants.getJSONObject(i)));
                Set<String> keys=signatures(source); List<Integer> matches=new ArrayList<>();
                for(int j=0;j<articles.length();j++) if(!Collections.disjoint(keys,signatures(articles.getJSONObject(j)))) matches.add(j);
                if(matches.size()==1) {
                    JSONObject target=articles.getJSONObject(matches.get(0));
                    conflicts("/articles/"+matches.get(0),target,source,conflicts);
                    ProductDataReconciler.mergeObjectMissing(target,source,EXCLUDED);
                    changes.put(new JSONObject().put("action","complement").put("sourceIdentifier",source.optString("identifier")).put("targetIndex",matches.get(0)));
                } else if(matches.isEmpty() && !keys.isEmpty()) {
                    articles.put(source);
                    changes.put(new JSONObject().put("action","propose-link").put("sourceIdentifier",source.optString("identifier")));
                } else changes.put(new JSONObject().put("action","review-ambiguous-or-missing-identity").put("sourceIdentifier",source.optString("identifier")).put("matches",matches));
            }
        }
        return new JSONObject().put("mode","OFFLINE_PROPOSAL_ONLY").put("product",product).put("articles",articles)
            .put("changes",changes).put("conflicts",conflicts).put("note","Legacy fill-missing semantics; no timestamps/Mongo precedence applied yet. Never PUT this report directly.");
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2) throw new IllegalArgumentException("input.json output-plan.json");
        JSONObject result=plan(new JSONObject(Files.readString(Path.of(args[0]),StandardCharsets.UTF_8)));
        Files.writeString(Path.of(args[1]),result.toString(2),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    }
}
