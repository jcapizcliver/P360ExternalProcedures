package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ArreglaSectionSBB {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", "CommercialS4H");
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es)");
		qp.put("query", "StructureGroup.Identifier wildcard \"%-L3SH\"");
		qp.put("pageSize", "1200");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		org.json.JSONObject writeResponse = null;
		
		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			if(response == null) {
				System.out.println("Error requesting data: " + rw.getRawResponse());
			}else {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0).replaceAll("-.+", "") + "'@'Section'")).put("values", new org.json.JSONArray().put(values.getString(0).replaceAll("-.+", "") + " - " + values.getString(1)).put(true)));
					if(rowsPayload.length() == 250) {
						writeResponse = rw.makeRequest("POST", "/list/LookupValue", empty, request.toString());
						if(writeResponse != null) {
							System.out.println(writeResponse.getJSONObject("counters"));
						}else {
							System.out.println("Error: " + rw.getRawResponse());
						}
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
				
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(rowsPayload.length() > 0) {
			writeResponse = rw.makeRequest("POST", "/list/LookupValue", empty, request.toString());
			if(writeResponse != null) {
				System.out.println(writeResponse.getJSONObject("counters"));
			}else {
				System.out.println("Error: " + rw.getRawResponse());
			}
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}

}
