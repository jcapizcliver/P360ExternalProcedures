package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class UpdateSKUForThoseWithGood {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "5000");
		qp.put("query", "(Product2G.SKU is empty or Product2G.SKU = 999) and (Product2G.CurrentStatus = \"Revisión QA\" or Product2G.CurrentStatus = \"Rechazo QA\" or Product2G.CurrentStatus = \"Category\" or Product2G.CurrentStatus = \"Aprobada\") and Product2G.ProductNo startsWith \"175461166\"");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.SKU")).put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			String productId = row.getJSONArray("values").getString(0);
			String sku = "999" + productId.substring(9);
			rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( sku ).put( sku )));
		});
		rh.sendData();
	}
	
	
}
