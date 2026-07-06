package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class PonFotoTomadaLiverpoolLVPs {

	public static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "1000");
		qp.put("query", "Product2G.ProductNo startsWith \"LVP\"");
		org.json.JSONArray rows = new org.json.JSONArray();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@1")).put("values", new org.json.JSONArray().put(new org.json.JSONArray().put("Y"))));
		});
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)"))).put("rows", rows), System.out::println);
	}
	
}

