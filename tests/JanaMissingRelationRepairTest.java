package mx.com.liverpool.p360.services.core.sftp;
import org.json.*;
public class JanaMissingRelationRepairTest {
    static JSONObject candidate(){return new JSONObject().put("status","MISSING_REFERENCE").put("sku","123").put("article","A").put("expectedProduct","P").put("attyp","00");}
    static JSONArray type(){return new JSONArray().put(new JSONObject().put("_qualification",new JSONObject().put("targetMarket",new JSONObject().put("_key","MX"))).put("sapObjectType",new JSONObject().put("_code","00")));}
    static class Fake implements JanaMissingRelationRepair.Gateway {
        JSONObject current=candidate(),article=new JSONObject().put("sku","123").put("articleExtraData",type());int writes=0;boolean verify=true;
        public JSONObject audit(JSONObject c){return current;}
        public JSONObject article(String id){return article;}
        public JSONObject product(String id){return new JSONObject().put("productExtraData",type());}
        public void write(JSONObject request){
            writes++;JSONObject row=request.getJSONArray("rows").getJSONObject(0);
            if(!row.getJSONObject("object").getString("id").equals("'A'@1")||!row.getJSONArray("values").getString(0).equals("P"))throw new AssertionError(request);
            if(verify)article.put("higherLevelProduct",new JSONArray().put(new JSONObject().put("_qualification",new JSONObject().put("referencedIdentifier","P"))));
        }
    }
    static void expect(String result,Fake f,boolean apply,int writes)throws Exception{String actual=JanaMissingRelationRepair.repair(candidate(),f,apply).getString("result");if(!result.equals(actual)||writes!=f.writes)throw new AssertionError(actual);}
    public static void main(String[] args)throws Exception{
        expect("READY",new Fake(),false,0);expect("APPLIED_VERIFIED",new Fake(),true,1);
        Fake f=new Fake();f.current.put("status","OK");expect("ALREADY_CORRECT",f,true,0);
        f=new Fake();f.current.put("expectedProduct","OTHER");expect("TARGET_CHANGED",f,true,0);
        f=new Fake();f.current.put("status","WRONG_PARENT");expect("SKIPPED_CURRENT_STATE",f,true,0);
        f=new Fake();f.article.put("sku","999");expect("ARTICLE_SKU_CHANGED",f,true,0);
        f=new Fake();f.article.remove("articleExtraData");expect("ARTICLE_TYPE_CHANGED_OR_UNKNOWN",f,true,0);
        f=new Fake();f.article.put("higherLevelProduct",new JSONArray().put(new JSONObject().put("_qualification",new JSONObject().put("referencedIdentifier","OTHER"))));expect("PARENT_NOW_PRESENT",f,true,0);
        f=new Fake();f.verify=false;try{JanaMissingRelationRepair.repair(candidate(),f,true);throw new AssertionError("Unverified success");}catch(java.io.IOException expected){}
        System.out.println("PASS 9 recovery scenarios; check mode has no writes, apply verifies relationship");
    }
}
