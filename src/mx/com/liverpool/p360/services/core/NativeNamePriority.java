package mx.com.liverpool.p360.services.core;
import org.json.JSONArray;
import org.json.JSONObject;
/** Native Spanish names take precedence; characteristics remain the last fallback. */
public final class NativeNamePriority {
 private NativeNamePriority() {}
 public static JSONObject apply(JSONObject response) {
  JSONObject data=response==null?null:response.optJSONObject("_data");
  if(data==null)return response;
  applyField(data,"Name","descriptionShort");
  applyField(data,"ProductName","productName");
  return NativeModelPriority.prepare(response);
 }
 private static void applyField(JSONObject data,String name,String property) {
  JSONArray languages=data.optJSONArray("lang"); String nativeValue=null;
  for(int i=0;languages!=null && i<languages.length();i++) {
   JSONObject row=languages.optJSONObject(i); if(row==null)continue;
   JSONObject q=row.optJSONObject("_qualification");
   JSONObject language=q==null?null:q.optJSONObject("language");
   if(language==null)continue;
   String code=language.optString("_code","");
   if(!"esl".equalsIgnoreCase(code) && !"es".equalsIgnoreCase(code) && language.optInt("_key",-1)!=10)continue;
   Object value=row.opt(property);
   if(value instanceof String && !((String)value).trim().isEmpty()) {nativeValue=(String)value;break;}
  }
  if(nativeValue==null)return;
  JSONArray records=data.optJSONArray("_characteristicRecords");
  if(records==null){records=new JSONArray();data.put("_characteristicRecords",records);}
  boolean found=false;
  for(int i=0;i<records.length();i++) {
   JSONObject r=records.optJSONObject(i);if(r==null)continue;
   JSONObject q=r.optJSONObject("_qualification"); JSONObject c=q==null?null:q.optJSONObject("characteristic");
   if(c!=null && name.equals(c.optString("_code"))) {setValue(r,nativeValue);found=true;}
  }
  if(!found){JSONObject r=new JSONObject().put("_qualification",new JSONObject().put("characteristic",new JSONObject().put("_code",name)));setValue(r,nativeValue);records.put(r);}
 }
 private static void setValue(JSONObject record,String value) {
  record.put("_recordLang",new JSONArray().put(new JSONObject().put("values",new JSONArray().put(value))));
 }
}