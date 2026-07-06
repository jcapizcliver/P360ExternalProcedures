package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class SAPItemGroupIntoLookupValues {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es)");
		qp.put("query", "StructureGroup.Identifier wildcard \"%-L4%\"");
		qp.put("structure", "CommercialECC");
		qp.put("pageSize", "900");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		request.put("columns", columns);
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		request.put("rows", rowsPayload);
		org.json.JSONObject postResponse = null;
		java.util.Map<String, String> emptyQp = new java.util.TreeMap<>();
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0).replaceAll("-.+", "") + "'@'MATKLLOV'")).put("values", new org.json.JSONArray().put(values.getString(1)).put(true)));
					if(rowsPayload.length() == 250) {
						postResponse = workshop.makeRequest("POST", "/list/LookupValue", emptyQp, request.toString());
						if(postResponse == null) {
							System.out.println(workshop.getRawResponse());
						}else {
							System.out.println("Dale. " + postResponse.getJSONObject("counters"));
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
			postResponse = workshop.makeRequest("POST", "/list/LookupValue", emptyQp, request.toString());
			if(postResponse == null) {
				System.out.println(workshop.getRawResponse());
			}else {
				System.out.println("Dale. " + postResponse.getJSONObject("counters"));
			}
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
		qp.put("structure", "CommercialS4H");
		System.out.println("Going commercialS4H");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0).replaceAll("-.+", "") + "'@'MATKLLOV_S4H'")).put("values", new org.json.JSONArray().put(values.getString(1)).put(true)));
					if(rowsPayload.length() == 250) {
						postResponse = workshop.makeRequest("POST", "/list/LookupValue", emptyQp, request.toString());
						if(postResponse == null) {
							System.out.println(workshop.getRawResponse());
						}else {
							System.out.println("Dale. " + postResponse.getJSONObject("counters"));
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
			postResponse = workshop.makeRequest("POST", "/list/LookupValue", emptyQp, request.toString());
			if(postResponse == null) {
				System.out.println(workshop.getRawResponse());
			}else {
				System.out.println("Dale. " + postResponse.getJSONObject("counters"));
			}
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}

}
