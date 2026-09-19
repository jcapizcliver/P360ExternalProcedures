package mx.com.liverpool.p360.services.core.sftp;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Resolves each entity independently; an existing Article's parent is authoritative. */
public final class JanaIndividualTargets {
    public interface Lookup {
        JSONObject byId(String entity, String id);
        List<JSONObject> bySku(String entity, String sku);
        List<JSONObject> children(String productId);
    }
    public final String productId, articleId;
    public final boolean newProduct;
    private JanaIndividualTargets(String p, String a, boolean n) { productId=p; articleId=a; newProduct=n; }
    private static boolean blank(String s) { return s==null || s.trim().isEmpty() || "null".equals(s); }
    private static JSONObject unique(List<JSONObject> values, String context) {
        if(values.size()>1) throw new IllegalStateException("Individual ambiguo: "+context);
        return values.isEmpty()?null:values.get(0);
    }
    public static JanaIndividualTargets resolve(String sku, String incoming, Lookup db) {
        if(blank(sku)) throw new IllegalArgumentException("Individual sin SKU");
        String id=blank(incoming)?null:incoming.trim();
        if(id!=null && id.length()==15 && !id.startsWith("S")) id="1"+id;
        JSONObject article=id==null?null:db.byId("Article",id);
        JSONObject product=null;
        boolean explicitProduct=false;
        if(article==null && id!=null) {
            product=db.byId("Product2G",id); explicitProduct=product!=null;
        }
        if(article==null) article=unique(db.bySku("Article",sku),"Article SKU="+sku);
        if(article==null && product!=null) article=unique(db.children(product.getString("identifier")),"variantes del producto");
        if(article!=null) {
            String existingSku=article.optString("sku","");
            if(!blank(existingSku) && !sku.equals(existingSku)) throw new IllegalStateException("SKU de Article no coincide");
            JSONArray parents=article.optJSONArray("higherLevelProduct");
            if(parents!=null && parents.length()>1) throw new IllegalStateException("Individual con varios padres");
            if(parents!=null && parents.length()==1) {
                String parent=parents.getJSONObject(0).getJSONObject("_qualification").getString("referencedIdentifier");
                if(blank(parent)) throw new IllegalStateException("Padre sin Identifier");
                if(explicitProduct && !parent.equals(product.getString("identifier"))) throw new IllegalStateException("Producto explicito contradice padre actual");
                product=db.byId("Product2G",parent);
                if(product==null) throw new IllegalStateException("Padre actual no encontrado: "+parent);
            }
        }
        if(product==null) product=unique(db.bySku("Product2G",sku),"Product2G SKU="+sku);
        String fallback=id==null?"SBB"+sku:id;
        String pid=product==null?fallback:product.getString("identifier");
        String aid=article==null?fallback:article.getString("identifier");
        return new JanaIndividualTargets(pid,aid,product==null);
    }
}
