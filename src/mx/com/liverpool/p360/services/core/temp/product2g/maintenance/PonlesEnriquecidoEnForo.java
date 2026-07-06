package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class PonlesEnriquecidoEnForo extends RESTWrapper {

	public static void main(String[] args) {
		PonlesEnriquecidoEnForo a = new PonlesEnriquecidoEnForo();
		a.hazlo();
	}
	
	public void hazlo() {
		int bs = 25000;
		java.util.Map<String, String> qpi = new java.util.TreeMap<>();
		qpi.put("includeObjectsInProtocol", "false");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "Product2G.PrevStatus = \"1026\"");
		qp.put("pageSize", String.valueOf( bs ));
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnriquecidoEnForo',root,\"0000.0000.RK\",'EnriquecidoEnForo',-1)")), bs, request -> {
			writeData("list", "Product2G", null, qpi, request, System.out::println);
		});
		collectData("list", "Product2G", null, "bySearch", qp, row -> rh.addRow( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", row.getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put(new org.json.JSONArray().put( true ))) ) );
		rh.sendData();
	}
	
}
