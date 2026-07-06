package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ArreglaFSHLOV {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int totalSize = 0;
		int currentIndex = 0;

		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONObject updateResponse = null;

		String lookup = "FSH_IDLOV";

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", lookup);
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("pageSize", "900");

		System.out.println(".");
		do{
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				if(values.getString(1).contains(values.getString(0))) {
					System.out.println("---->" + values);
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'" + lookup + "'")).put("values", new org.json.JSONArray().put(false)));
					if(rowsPayload.length() == 300) {
						updateResponse = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rowsPayload).toString());
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
				System.out.println(values);
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(rowsPayload.length() > 0) {
			updateResponse = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rowsPayload).toString());
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}
}
