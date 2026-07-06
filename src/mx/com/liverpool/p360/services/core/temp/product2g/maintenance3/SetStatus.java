package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SetStatus {

	private static RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))).put("rows", new org.json.JSONArray()
					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'S94559114'@1")).put("values", new org.json.JSONArray().put("1022")))
					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'S94559110'@1")).put("values", new org.json.JSONArray().put("1022")))
					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'S94559109'@1")).put("values", new org.json.JSONArray().put("1022")))
					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'S94559117'@1")).put("values", new org.json.JSONArray().put("1022")))
					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'S94559116'@1")).put("values", new org.json.JSONArray().put("1022")))
					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'S94559112'@1")).put("values", new org.json.JSONArray().put("1022")))
				)
			, System.out::println);
	}
	
}
