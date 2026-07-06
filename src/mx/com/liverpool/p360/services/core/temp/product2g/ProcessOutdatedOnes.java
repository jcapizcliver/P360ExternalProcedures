package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ProcessOutdatedOnes {

	public static void main(String[] args) {
		ProcessOutdatedOnes p = new ProcessOutdatedOnes();
		p.printThoseInStatusDeleted();
		System.exit(0);
		p.processUpdateToDelete();
	}
	
	private void printThoseInStatusDeleted() {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> emptyQP = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("query", "Product2G.CurrentStatus = 1025");
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus,Product2GLog.CreationDate(PIM)");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					System.out.println(values.getString(0));
				}
				currentIndex += response.getInt("pageSize");
			}else {
				System.out.println("PROBLEM: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
	}
	
	private void processUpdateToDelete() {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> emptyQP = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSSZ");
		java.time.ZonedDateTime dt = null;
		java.time.ZonedDateTime cdtm = java.time.ZonedDateTime.now().minusDays(30);
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		qp.put("query", "characteristic('SKU',-1) is empty and Product2G.CurrentStatus = 10031");
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus,Product2GLog.CreationDate(PIM)");
		qp.put("pageSize", "1200");
		columns.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!"".equals(values.getString(2))) {
						dt = java.time.ZonedDateTime.parse(values.getString(2), dtf);
					}
					if(dt.compareTo(cdtm) <= 0) {
						rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@1")).put("values", new org.json.JSONArray().put(1025)));
						if(rowsPayload.length() == 256) {
							rw.makeRequest("POST", "/list/Product2G/", emptyQP, request.toString());
							System.out.println(rw.getRawResponse());
							while(rowsPayload.length() > 0) {
								rowsPayload.remove(0);
							}
						}
					}
				}
				currentIndex += response.getInt("pageSize");
			}else {
				System.out.println("PROBLEM: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(rowsPayload.length() > 0) {
			rw.makeRequest("POST", "/list/Product2G/", emptyQP, request.toString());
			System.out.println(rw.getRawResponse());
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}
}
