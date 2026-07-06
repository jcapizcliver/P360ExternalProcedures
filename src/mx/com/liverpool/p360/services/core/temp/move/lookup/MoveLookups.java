package mx.com.liverpool.p360.services.core.temp.move.lookup;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class MoveLookups {

	
	private static final RESTWorkshop rwd = new RESTWorkshop();
	private static final RESTWorkshop rwp = new RESTWorkshop();
	
	static {
		rwp.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rwp.addHeader("Authorization", "Basic: " + java.util.Base64.getEncoder().encodeToString(("jcapizc:algolindo").getBytes()));
	}
	
	public static void main(String[] args) {
		MoveLookups ml = new MoveLookups();
		ml.collectLookups();
	}
	
	private void collectLookups() {
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupLang.Description(es)"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		java.util.Map<String, String> emptyQp = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		qp.put("fields", "Lookup.Identifier,LookupLang.Name(es),LookupLang.Description(es)");
		qp.put("query", "not Lookup.Identifier is empty");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rwd.makeRequest("GET", "/list/Lookup/bySearch", qp, null);
			if(response != null) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")).put("values", new org.json.JSONArray().put(values.getString(1)).put(values.getString(2))));
					if(rowsPayload.length() == 300) {
						rwp.makeRequest("POST", "/list/Lookup", emptyQp, request.toString());
						System.out.println(rwp.getRawResponse());
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println(rwd.getRawResponse());
			}
		}while(a < b);
		a = 0;
		if(rowsPayload.length() > 0) {
			rwp.makeRequest("POST", "/list/Lookup", emptyQp, request.toString());
			System.out.println(rwp.getRawResponse());
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}
	
}
