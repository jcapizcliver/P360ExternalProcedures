package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class UpdateToEmptyKeyRelationships {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.Code"));
		request.put("columns", columns);
		request.put("", rows);
	}
	
}
