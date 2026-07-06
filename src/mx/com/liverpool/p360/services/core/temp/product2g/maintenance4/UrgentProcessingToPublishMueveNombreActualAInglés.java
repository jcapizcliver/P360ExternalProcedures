package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class UrgentProcessingToPublishMueveNombreActualAInglés {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Product2G.CurrentStatus = \"Revisión QA\"");
		qp.put("pageSize", "5000");
		qp.put("fields", "Product2GLang.ProductName(es)");
		
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GLang.ProductName(en)")), 2000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(values.getString(0))));
		});
		rh.sendData();
		
		
	}
	
}
