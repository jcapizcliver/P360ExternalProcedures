package mx.com.liverpool.p360.services.core.sftp;
import org.json.*;
public final class ReconciliationPlanTest {
    private static int checks;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    public static void main(String[] args){
        JSONObject base=new JSONObject("{product:{identifier:'P1',name:'Base'},articles:[{identifier:'A1',sku:'123',description:''},{identifier:'A2',sku:'456'}]}");
        JSONObject donor=new JSONObject("{product:{identifier:'P2',name:'Other',brand:'B',log:[{text:'omit'}],lang:[{_qualification:{language:{_key:10}},name:'Nombre',ownLog:'omit'}]},articles:[{identifier:'A3',sku:'123',description:'New',ownLog:'omit'},{identifier:'A4',sku:'789'},{identifier:'A2',sku:'123'},{description:'Unknown'}]}");
        JSONObject input=new JSONObject().put("base",base).put("sources",new JSONArray().put(donor));
        String before=input.toString(); JSONObject result=ReconciliationPlan.plan(input);
        check(before.equals(input.toString()),"Must not mutate input snapshots");
        JSONObject p=result.getJSONObject("product");
        check(p.getString("identifier").equals("P1"),"Preserve identity");
        check(p.getString("name").equals("Base"),"Preserve existing value");
        check(p.getString("brand").equals("B"),"Fill missing fields");
        check(!p.has("log"),"Exclude root log");
        check(!p.getJSONArray("lang").getJSONObject(0).has("ownLog"),"Exclude nested logs");
        JSONArray a=result.getJSONArray("articles");
        check(a.length()==3,"Unmatched article proposed; ambiguous ones held");
        check(a.getJSONObject(0).getString("description").equals("New"),"Complement matching SKU");
        check(!a.getJSONObject(0).has("ownLog"),"Exclude article log");
        check(result.getJSONArray("conflicts").length()>0,"Expose conflicts");
        check(result.getJSONArray("changes").getJSONObject(2).getString("action").startsWith("review"),"Ambiguous match held");
        JSONObject target=new JSONObject("{values:[1],empty:''}");
        ProductDataReconciler.mergeObjectMissing(target,new JSONObject("{values:[1,2],empty:'filled'}"),null);
        check(target.getJSONArray("values").length()==2,"Legacy multi-value union retained");
        check(target.getString("empty").equals("filled"),"Legacy empty fill retained");
        JSONObject characteristicBase=new JSONObject("{_characteristicRecords:[{_qualification:{characteristic:{_code:'Colour'},recordKey:'1'},_recordLang:[{_qualification:{language:{_key:10}},values:['Blue']}]}],enabled:false,quantity:0}");
        JSONObject characteristicSource=new JSONObject("{_characteristicRecords:[{_qualification:{characteristic:{_code:'Colour'},recordKey:'1'},_recordLang:[{_qualification:{language:{_key:10}},values:['Red']},{_qualification:{language:{_key:20}},values:['Azul']}]},{_qualification:{characteristic:{_code:'StatusModification'}},_recordLang:[{values:['audit']}]}],enabled:true,quantity:7}");
        JSONObject c=ReconciliationPlan.plan(new JSONObject().put("base",new JSONObject().put("product",characteristicBase)).put("sources",new JSONArray().put(new JSONObject().put("product",characteristicSource)))).getJSONObject("product");
        check(!c.getBoolean("enabled")&&c.getInt("quantity")==0,"False and zero are populated values");
        check(c.getJSONArray("_characteristicRecords").length()==1,"Exclude log characteristic");
        JSONArray langs=c.getJSONArray("_characteristicRecords").getJSONObject(0).getJSONArray("_recordLang");
        check(langs.length()==2,"Complete missing language");
        check(langs.getJSONObject(0).getJSONArray("values").length()==2,"Preserve legacy multi-valued characteristics");
        System.out.println("Reconciliation checks passed: "+checks);
    }
}
