package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ArreglaLasGddRQA {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "not Product2G.SKU is empty and Product2G.CurrentStatus = \"Gobierno de Datos\" and not Product2G.PrevStatus = \"Revisión QA\"");
		qp.put("pageSize", "5000");
		java.util.List<String> ids = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> ids.add(row.getJSONObject("object").getString("id")));
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus")).put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 5000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println ) );
		for(String id : ids) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", id)).put("values", new org.json.JSONArray().put("1020").put("1022")));
		}
		rh.sendData();
		forCategory();
	}
	
	private static void forCategory() {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "not Product2G.SKU is empty and Product2G.CurrentStatus = \"Gobierno de Datos\" and not Product2G.PrevStatus = \"Category\"");
		qp.put("pageSize", "5000");
		java.util.List<String> ids = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> ids.add(row.getJSONObject("object").getString("id")));
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus")).put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 5000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println ) );
		for(String id : ids) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", id)).put("values", new org.json.JSONArray().put("1022").put("1023")));
		}
		rh.sendData();
	}
	
}

