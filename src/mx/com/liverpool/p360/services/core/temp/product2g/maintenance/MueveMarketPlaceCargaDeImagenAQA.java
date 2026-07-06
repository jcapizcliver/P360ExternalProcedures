package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class MueveMarketPlaceCargaDeImagenAQA {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus,Product2G.PrevStatus,Product2G.ExternalStatus");
		qp.put("query", "Product2G.CurrentStatus = \"Carga de Imagen\" and characteristic('Business') = 'MKP'@'BusinessQualified' and Product2G.ProductNo startsWith \"175461166\"");
		qp.put("pageSize", "10000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 10000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println));
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			System.out.println(row.getJSONArray("values"));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", row.getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put("Revisión QA")));
		});
		rh.sendData();
	}
	
}
