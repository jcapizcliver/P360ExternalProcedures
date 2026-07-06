package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ReviewProductsNoExternalStatus extends RESTWrapper {

	
	public static void main(String[] args) {
		ReviewProductsNoExternalStatus r = new ReviewProductsNoExternalStatus();
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		qp.put("dictionary",  "ExternalStatus");
		r.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> data.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)), System.out::println);
		qp.clear();
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus");
		qp.put("query", "not Product2G.CurrentStatus is empty and not Product2G.PrevStatus is empty and Product2G.ExternalStatus is empty");
		qp.put("pageSize", "25000");
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler handler = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus")), 25000, request -> {
			r.writeData("list", "Product2G", null, qp0, request, System.out::println);
		});
		r.collectData("list", "Product2G", null, "bySearch", qp, row ->{
			handler.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row.getJSONArray("values").getString(0) + "'@1")).put("values", new org.json.JSONArray().put(data.get(row.getJSONArray("values").getString(1)))));
		}, System.out::println);
		handler.sendData();
	}
		
}
