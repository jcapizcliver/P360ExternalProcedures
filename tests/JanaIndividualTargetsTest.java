package mx.com.liverpool.p360.services.core.sftp;
import java.util.*;
import org.json.*;
public class JanaIndividualTargetsTest {
    static class Db implements JanaIndividualTargets.Lookup {
        Map<String,JSONObject> ids=new HashMap<>();
        Map<String,List<JSONObject>> skus=new HashMap<>();
        List<JSONObject> children=new ArrayList<>();
        public JSONObject byId(String e,String id){return ids.get(e+id);}
        public List<JSONObject> bySku(String e,String sku){return skus.getOrDefault(e,Collections.emptyList());}
        public List<JSONObject> children(String p){return children;}
        JSONObject add(String e,String id){JSONObject o=new JSONObject().put("identifier",id);ids.put(e+id,o);return o;}
    }
    static void check(JanaIndividualTargets t,String p,String a,boolean n){if(!p.equals(t.productId)||!a.equals(t.articleId)||n!=t.newProduct)throw new AssertionError(t.productId+"/"+t.articleId);}
    static void parent(JSONObject a,String id){a.put("higherLevelProduct",new JSONArray().put(new JSONObject().put("_qualification",new JSONObject().put("referencedIdentifier",id))));}
    public static void main(String[] args){
        Db d=new Db();JSONObject p=d.add("Product2G","1754611687045238"),a=d.add("Article","1754611687045246").put("sku","5016517048");parent(a,"1754611687045238");
        JSONObject wrong=d.add("Product2G","1754611687045246");d.skus.put("Product2G",List.of(wrong));d.skus.put("Article",List.of(a));
        check(JanaIndividualTargets.resolve("5016517048","754611687045246",d),"1754611687045238","1754611687045246",false);
        check(JanaIndividualTargets.resolve("5016517048",null,d),"1754611687045238","1754611687045246",false);
        check(JanaIndividualTargets.resolve("5016517048","1754611687045238",d),"1754611687045238","1754611687045246",false);
        check(JanaIndividualTargets.resolve("12",null,new Db()),"SBB12","SBB12",true);
        check(JanaIndividualTargets.resolve("12","NEW",new Db()),"NEW","NEW",true);
        Db orphan=new Db();JSONObject oa=orphan.add("Article","A");orphan.skus.put("Article",List.of(oa));JSONObject op=orphan.add("Product2G","P");orphan.skus.put("Product2G",List.of(op));
        check(JanaIndividualTargets.resolve("12","A",orphan),"P","A",false);
        Db onlyProduct=new Db();onlyProduct.add("Product2G","P");check(JanaIndividualTargets.resolve("12","P",onlyProduct),"P","P",false);
        d.skus.put("Article",List.of(a,a));try{JanaIndividualTargets.resolve("5016517048",null,d);throw new AssertionError("Ambiguity accepted");}catch(IllegalStateException expected){}
        d.ids.remove("Product2G1754611687045238");try{JanaIndividualTargets.resolve("5016517048","1754611687045246",d);throw new AssertionError("Missing parent accepted");}catch(IllegalStateException expected){}
        System.out.println("PASS 9 individual scenarios: existing parent wins over duplicate, no PRODUCT_ID, product ID, new IDs, orphan, missing Article, ambiguity, missing parent");
    }
}
