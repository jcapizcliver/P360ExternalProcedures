package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class PrendeLasCaracteristicasApagadas {

	public static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		enableCharacteristics();
	}
	
	private static void enableCharacteristics() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.IsActive = false");
		org.json.JSONObject r = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				System.out.println(response.getInt("totalSize"));
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					do {
						org.json.JSONObject rw0 = null;
						rw0 = new org.json.JSONObject()
								.put("columns", new org.json.JSONArray()
										.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive")))
								.put("rows", new org.json.JSONArray().put( new org.json.JSONObject()
										.put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'"))
										.put("values", new org.json.JSONArray().put(true))));
						r = rw.makeRequest("POST", "/list/Characteristic", empty, rw0.toString());
						System.out.println(r == null ? "ERR::" + rw.getRawResponse() : r);
					}while(rw.getRawResponse().contains("timeout"));
				}
			}else{
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
	}
}
