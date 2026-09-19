package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.util.*;
import java.util.function.Consumer;
import org.json.*;
import mx.com.liverpool.p360.services.core.InboundSkuResolver;
import mx.com.liverpool.p360.services.core.temp.xml.local.neostream.StepXmlStreamingParser.StepIndex;

/** Keeps source snapshots immutable; routes every REST row and publication to one owner per entity/SKU. */
public final class NeoSQLiteIdentityRouter {
    public static final String VERSION="sku-owner-20260917";
    public interface Lookup {
        String resolve(int entity,String sku,String source);
        String redirect(int entity,String source);
    }
    private final Map<String,String> products=new LinkedHashMap<>(),articles=new LinkedHashMap<>();
    private final Map<String,JSONObject> conflicts=new LinkedHashMap<>();
    private final Lookup lookup;
    public NeoSQLiteIdentityRouter(StepIndex index,InboundSkuResolver guard,Consumer<JSONObject> journal) {
        this(index,new Lookup(){
            public String resolve(int entity,String sku,String source){return guard.resolve(entity,sku,source);}
            public String redirect(int entity,String source){return guard.redirect(entity,source);}
        },journal);
    }
    public NeoSQLiteIdentityRouter(StepIndex index,Lookup lookup,Consumer<JSONObject> journal) {
        this.lookup=lookup;
        prepare(1100,index.getProductIds(),index.getProductSkuById(),products,journal);
        prepare(1000,index.getArticleIds(),index.getArticleSkuById(),articles,journal);
    }
    private void prepare(int entity,Set<String> ids,Map<String,String> skus,Map<String,String> targets,Consumer<JSONObject> journal) {
        List<String> sorted=new ArrayList<>(ids);
        sorted.sort(Comparator.comparingInt((String id)->-InboundSkuResolver.rank(id)).thenComparing(id->id));
        for(String id:sorted) {
            String sku=skus.get(id),target;
            try {target=lookup.resolve(entity,sku,id);}
            catch(IllegalStateException e) {
                String message=Objects.toString(e.getMessage(),"");
                if(!(message.startsWith("SKU_TARGET_HAS_OTHER_SKU ")||message.startsWith("SKU_ALIAS_CONFLICT ")||message.startsWith("SKU_ALIAS_TARGET_MISSING ")||message.startsWith("SKU_ALIAS_CYCLE ")))throw e;
                JSONObject issue=new JSONObject().put("kind","IDENTITY_CONFLICT").put("entity",entity).put("objectId",id)
                    .put("sourceIdentifier",id).put("sku",sku==null?"":sku).put("error",message).put("policy",VERSION);
                conflicts.put(entity+":"+id,issue);journal.accept(issue);continue;
            }
            if(target==null||target.isBlank())throw new IllegalStateException("SQLITE_IDENTITY_UNRESOLVED "+entity+":"+id);
            targets.put(id,target);
            if(!id.equals(target))journal.accept(new JSONObject().put("policy",VERSION).put("entity",entity)
                .put("sourceIdentifier",id).put("targetIdentifier",target).put("sku",sku==null?"":sku)
                .put("state","ROUTED_TO_OWNER").put("fullConsolidation",false));
        }
    }
    public static Set<String> skus(StepIndex index) {
        Set<String> result=new TreeSet<>(index.getProductSkus());result.addAll(index.getArticleSkus());return result;
    }
    public JSONArray conflicts(StepIndex index) {
        JSONArray result=new JSONArray();
        for(String id:index.getProductIds())if(conflicts.containsKey("1100:"+id))result.put(conflicts.get("1100:"+id));
        for(String id:index.getArticleIds())if(conflicts.containsKey("1000:"+id))result.put(conflicts.get("1000:"+id));
        return result;
    }
    public String target(int entity,String original) {
        String value=(entity==1100?products:articles).get(original);
        if(value==null)throw new IllegalStateException("SQLITE_UNINDEXED_WRITE "+entity+":"+original);
        return value;
    }
    private String parent(String id) {
        String result=products.get(id);if(result==null)result=lookup.redirect(1100,id);
        if(result==null||result.isBlank())throw new IllegalStateException("SQLITE_PARENT_UNRESOLVED "+id);
        return result;
    }
    public Set<String> targets(int entity) { return new LinkedHashSet<>((entity==1100?products:articles).values()); }
    private static String external(String objectId) {
        if(!objectId.startsWith("'")||!objectId.endsWith("'@1"))throw new IllegalArgumentException("Unexpected SQLite object id: "+objectId);
        return objectId.substring(1,objectId.length()-3);
    }
    /** Keep publications pending for rejected associations; never announce an unaccepted parent. */
    public static Set<String> failedRelationSources(JSONObject debt) {
        JSONArray source=debt.getJSONObject("sourceRequest").getJSONArray("rows");
        JSONArray sent=debt.getJSONObject("request").getJSONArray("rows");
        Set<String> all=new LinkedHashSet<>(),affected=new LinkedHashSet<>();
        Map<String,Set<String>> sourcesByTarget=new HashMap<>();
        for(int i=0;i<source.length();i++) {
            String id=external(source.getJSONObject(i).getJSONObject("object").getString("id"));all.add(id);
            String target=external(sent.getJSONObject(i).getJSONObject("object").getString("id"));
            sourcesByTarget.computeIfAbsent(target,k->new LinkedHashSet<>()).add(id);
        }
        try {
            JSONObject response=new JSONObject(debt.getString("response"));JSONArray entries=response.getJSONArray("entries");
            int matched=0,errors=response.getJSONObject("counters").getInt("errors");
            for(int i=0;i<entries.length();i++) {
                JSONObject entry=entries.getJSONObject(i);if(!"ERROR".equals(entry.optString("severity")))continue;
                JSONObject object=entry.optJSONObject("object");String label=object==null?"":object.optString("label");
                Set<String> ids=sourcesByTarget.get(label);if(ids==null)return all;affected.addAll(ids);matched++;
            }
            if(errors>0&&matched==errors&&!affected.isEmpty())return affected;
        }catch(Exception ignored) { }
        return all; // Unknown/transport result: preserve the whole affected batch for verification.
    }
    public JSONObject request(String entity,String child,JSONObject request) {
        int type="Product2G".equals(entity)?1100:"Article".equals(entity)?1000:0;
        if(type==0)throw new IllegalArgumentException("Unexpected SQLite entity: "+entity);
        JSONObject result=new JSONObject(request.toString());JSONArray rows=result.getJSONArray("rows"),columns=result.getJSONArray("columns");
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i),object=row.getJSONObject("object");
            object.put("id","'"+target(type,external(object.getString("id")))+"'@1");
            if("ProductReference".equals(child)) {
                JSONObject q=row.optJSONObject("qualification");
                if(q!=null&&q.has("referencedSupplierAid"))q.put("referencedSupplierAid",parent(q.getString("referencedSupplierAid")));
                JSONArray values=row.getJSONArray("values");
                for(int c=0;c<columns.length();c++)if("ProductReference.ReferencedSupplierAid".equals(columns.getJSONObject(c).getString("identifier")))values.put(c,parent(values.getString(c)));
            }
        }
        return result;
    }
    /** Only identity fields are translated; attributes and asset IDs retain their existing mapping. */
    public JSONObject publication(JSONObject body) {
        JSONObject result=new JSONObject(body.toString());JSONArray list=result.getJSONArray("products");
        for(int i=0;i<list.length();i++) {
            JSONObject product=list.getJSONObject(i);product.put("proposalId",target(1100,product.getString("proposalId")));
            JSONArray variants=product.optJSONArray("variants");
            if(variants==null)continue;
            Map<String,JSONObject> unique=new LinkedHashMap<>();
            for(int j=0;j<variants.length();j++) {
                JSONObject variant=variants.getJSONObject(j);String id=target(1000,variant.getString("variantId"));variant.put("variantId",id);
                JSONObject previous=unique.get(id);
                if(previous==null)unique.put(id,variant);else mergeMissing(previous,variant);
            }
            product.put("variants",new JSONArray(unique.values()));
        }
        return result;
    }
    private static void mergeMissing(JSONObject target,JSONObject donor) {
        for(Object key:donor.keySet()) {
            String k=key.toString();Object value=donor.get(k),current=target.opt(k);
            if(current==null||current==JSONObject.NULL||current instanceof String&&((String)current).isBlank())target.put(k,value);
            else if(current instanceof JSONObject&&value instanceof JSONObject)mergeMissing((JSONObject)current,(JSONObject)value);
            else if(current instanceof JSONArray&&value instanceof JSONArray){JSONArray a=(JSONArray)current,b=(JSONArray)value;Set<String> seen=new HashSet<>();for(int i=0;i<a.length();i++)seen.add(a.get(i).toString());for(int i=0;i<b.length();i++)if(seen.add(b.get(i).toString()))a.put(b.get(i));}
        }
    }
}
