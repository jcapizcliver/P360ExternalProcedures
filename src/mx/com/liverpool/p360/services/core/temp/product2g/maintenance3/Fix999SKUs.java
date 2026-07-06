package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class Fix999SKUs {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "Product2G.SKU = 999");
		qp.put("pageSize", "5000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.SKU")), 2000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(values.getString(0).startsWith("S")) {
				rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(values.getString(0).replaceAll("S", "999"))));
			}
		});
		rh.sendData();
	}
	
}
