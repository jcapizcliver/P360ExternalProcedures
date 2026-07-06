package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RedoToDGFromQA {

	private static RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "Product2G.CurrentStatus = \"Revisión QA\" and characteristic('SKU') is empty");
		org.json.JSONArray rows = new org.json.JSONArray();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			rows.put(new org.json.JSONObject().put( "object", new org.json.JSONObject()
					.put("id", "'" + row.getJSONArray("values").getString(0) + "'@1")).put("values", new org.json.JSONArray().put("Gobierno de Datos")));
		});
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp, new org.json.JSONObject()
				.put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")))
				.put("rows", rows), System.out::println);
	}
	
}
