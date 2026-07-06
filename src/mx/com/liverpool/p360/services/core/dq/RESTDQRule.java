package mx.com.liverpool.p360.services.core.dq;

public interface RESTDQRule {

	void processData( java.util.Map<String, org.json.JSONObject> sourceData, org.json.JSONArray records );
	
}
