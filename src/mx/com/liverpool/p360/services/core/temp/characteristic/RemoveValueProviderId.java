package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class RemoveValueProviderId {

	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		removeValueProviderId( getVaD()[0] );
	}
	
	private static void removeValueProviderId(String[] ids) {
		System.out.println("Updating ValueProviderId");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.ValueProviderId"));
		request.put("columns", columns);
		request.put("rows", rows);
		for(int i=0; i<ids.length; i++) {
			System.out.println("ID: " + ids[i]);
			rows.put(
					new org.json.JSONObject()
						.put("object", new org.json.JSONObject().put("id", "'" + ids[i] + "'"))
						.put("values", new org.json.JSONArray().put(""))
					);
			if(rows.length() == 200) {
				response = rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
				System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
			System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private static String[][] getVaD(){
		java.util.LinkedList<String> vals = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "not Characteristic.ValueProviderId is empty");
		qp.put("fields", "Characteristic.Identifier");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		
		int currentIndex = 0;
		int totalSize = 0;
		
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				totalSize = response.getInt("totalSize");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					vals.addLast(values.getString(0));
				}
			}else {
				System.out.println("Error en petición de características VaD: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return new String[][] {
			vals.toArray(new String[] {}), 
		};
	}

}
