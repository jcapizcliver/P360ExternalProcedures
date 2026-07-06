package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ArreglaGuposDeArticulo {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'MATKLLOV'");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.IsActive = true and LookupValueLang.Name(es) wildcard \"% - %\"");
		qp.put("pageSize", "900");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		String code = null;
		String name = null;

		int currentIndex = 0;
		int totalSize = 0;

		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		org.json.JSONObject updateResponse = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					code = values.getString(0);
					name = values.getString(1);
					if(name.startsWith(code)) {
						System.out.println(name + " vs " + name.replaceAll(".+ - ", ""));
						rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + code + "'@'MATKLLOV'")).put("values", new org.json.JSONArray().put(name.replaceAll(".+ - ", "")).put(true)));
						if(rowsPayload.length() == 200) {
							updateResponse = rw.makeRequest("POST", "/list/LookupValue", empty, request.toString());
							if(updateResponse != null) {
								System.out.println(updateResponse.getJSONObject("counters"));
							}else {
								System.out.println(rw.getRawResponse());
							}
							while(rowsPayload.length() > 0) {
								rowsPayload.remove(0);
							}
						}
					}
				}
				if(rowsPayload.length() > 0) {
					updateResponse = rw.makeRequest("POST", "/list/LookupValue", empty, request.toString());
					if(updateResponse != null) {
						System.out.println(updateResponse.getJSONObject("counters"));
					}else {
						System.out.println(rw.getRawResponse());
					}
					while(rowsPayload.length() > 0) {
						rowsPayload.remove(0);
					}
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		if(rowsPayload.length() > 0) {
			updateResponse = rw.makeRequest("POST", "/list/LookupValue", empty, request.toString());
			if(updateResponse != null) {
				System.out.println(updateResponse.getJSONObject("counters"));
			}else {
				System.out.println(rw.getRawResponse());
			}
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}

}
