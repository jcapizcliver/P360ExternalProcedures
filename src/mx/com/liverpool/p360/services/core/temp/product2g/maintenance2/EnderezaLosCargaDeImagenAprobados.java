package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class EnderezaLosCargaDeImagenAprobados {

	
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Product2G.CurrentStatus = \"Aprobada\" and Product2G.PrevStatus = \"Carga de Imagen\"");
		qp.put("pageSize", "5000");
		java.util.List<String> ids = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			ids.add(row.getJSONObject("object").getString("id"));
		});
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus")), 5000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println));
		java.util.List<String> aids = new java.util.ArrayList<>();
		StringBuilder sb = new StringBuilder();
		int i = 0;
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("pageSize", "5000");
		for(String id : ids) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", id)).put("values", new org.json.JSONArray().put("1023")));
			sb.append(sb.length() == 0 ? "" : ",").append(id);
			i++;
			if(i % 1000 == 0) {
				qp0.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp0, row -> {
					aids.add(row.getJSONObject("object").getString("id"));
				});
				sb.setLength(0);
			}
		}
		rh.sendData();
		if(sb.length() > 0) {
			qp0.put("products", sb.toString());
			rw.collectData("list", "Article", null, "byProducts", qp0, row -> {
				aids.add(row.getJSONObject("object").getString("id"));
			});
			sb.setLength(0);
		}
		rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.PrevStatus")), 5000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println));
		for(String id : aids) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", id)).put("values", new org.json.JSONArray().put("1023")));
		}
		rh.sendData();
	}
	
}
