package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class LeerCaracteristicasSinCategoría {

	private static final RESTWrapper rw = new RESTWrapper();
	private static StringBuilder sb = new StringBuilder();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.251.2:1512/rest/V2.0");
		rw.getRw().addHeader("Authorization", "Basic cmVzdDozVnVzJDl4MUU4bSQ=");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "Characteristic.Identifier"
				+ ",Characteristic.IsActive"
				+ ",Characteristic.Entities");
		qp.put("query", "Characteristic.Category is empty and Characteristic.RootCharacteristic is empty");
		qp.put("pageSize", "5000");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"));
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(Boolean.parseBoolean(values.getString(1))) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")).put("values", new org.json.JSONArray().put(false)));
			}
		});
		System.out.println("Writing: " + rows.length() + " values.");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		if(rows.length() > 0) {
			rw.writeData("list", "Characteristic", null, qp0, request, System.out::println);
		}
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'").append(values.getString(0)).append("'");
		});
		rw.getRw().addHeader("Content-Type", "application/x-www-urlencoded");
		qp0.remove("includeObjectsInProtocol");
		qp0.put("items", sb.toString());
		rw.deleteData("list", "Characteristic", null, "byItems", qp0, System.out::println);
	}
	
}
