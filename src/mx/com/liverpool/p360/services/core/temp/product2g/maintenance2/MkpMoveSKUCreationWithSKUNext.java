package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MkpMoveSKUCreationWithSKUNext {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject req = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))).put("rows", rows);
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GLog.CreationDate(PIM),Product2GLog.ModificationDate(PIM)");
		qp.put("query", "Product2GLog.CreationDate(PIM) >= 2026-03-01T00:00:00 and Product2G.ProductNo startsWith \"175461166\" and Product2G.CurrentStatus = \"Creación de SKU\" and characteristic('Business') = 'MKP'@'BusinessQualified' and not characteristic('SKU') is empty");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
			rows.put(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("Revisión QA")));
		});
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp, req, System.out::println);
	}
	
}
