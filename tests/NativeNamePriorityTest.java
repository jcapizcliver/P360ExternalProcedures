package mx.com.liverpool.p360.services.core;
import org.json.*;
public class NativeNamePriorityTest {
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 static String value(JSONObject r,String name){JSONArray a=r.getJSONObject("_data").getJSONArray("_characteristicRecords");for(int i=0;i<a.length();i++){JSONObject c=a.getJSONObject(i);if(name.equals(c.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code")))return c.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);}return null;}
 public static void main(String[] args){
 JSONObject r=new JSONObject("{_data:{lang:[{_qualification:{language:{_code:'en'}},descriptionShort:'wrong'},{_qualification:{language:{_key:10}},descriptionShort:'Native name',productName:'Native product'}],_characteristicRecords:[{_qualification:{characteristic:{_code:'Name'}},_recordLang:[{values:['old']}]},{_qualification:{characteristic:{_code:'ProductName'}},_recordLang:[]}]}}");
 NativeNamePriority.apply(r);check(value(r,"Name").equals("Native name"));check(value(r,"ProductName").equals("Native product"));
 r.getJSONObject("_data").put("lang",new JSONArray("[{_qualification:{language:{_code:'esl'}},descriptionShort:' ',productName:null}]"));NativeNamePriority.apply(r);check(value(r,"Name").equals("Native name"));check(value(r,"ProductName").equals("Native product"));
 JSONObject absent=new JSONObject("{_data:{lang:[{_qualification:{language:{_code:'es'}},descriptionShort:'Only native',productName:'P'}]}}");NativeNamePriority.apply(absent);check(value(absent,"Name").equals("Only native"));check(value(absent,"ProductName").equals("P"));
 JSONObject empty=new JSONObject("{_data:{lang:[null,{}]}}");NativeNamePriority.apply(empty);check(!empty.getJSONObject("_data").has("_characteristicRecords"));check(NativeNamePriority.apply(null)==null);
 String before=r.toString();NativeNamePriority.apply(r);check(before.equals(r.toString()));System.out.println("PASS "+n+" native-name checks");
 }
}