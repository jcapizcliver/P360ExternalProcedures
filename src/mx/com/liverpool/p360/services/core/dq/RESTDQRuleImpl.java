package mx.com.liverpool.p360.services.core.dq;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public abstract class RESTDQRuleImpl implements RESTDQRule {

	protected static final RESTWrapper rw = new RESTWrapper();
	
	protected String getCharacteristicValue(org.json.JSONObject characteristic) {
		return getCharacteristicValue(characteristic, false);
	}

	protected org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}

	protected String getCharacteristicValue(org.json.JSONObject characteristic, boolean getCode) {
		if(characteristic == null) {
			return "";
		}
		String dataType = characteristic.has("_datatype") ? characteristic.getString("_datatype") : "";
		if("LOOKUP".equals(dataType)) {
			try{
				return characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).has(getCode ? "_code" : "_label") ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString(getCode ? "_code" : "_label") : "";
			}catch(org.json.JSONException e){
				throw e;
			}
		}else {
			return String.valueOf( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) );
		}
	}
	
}
