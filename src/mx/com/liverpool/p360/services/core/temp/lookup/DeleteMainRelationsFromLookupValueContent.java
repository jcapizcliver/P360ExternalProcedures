package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class DeleteMainRelationsFromLookupValueContent {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://gcpcatqap01.liverpool.com.mx:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
//		qp.put("lookup", "'PPH_L4_Templates'");
//		qp.put("lookup", "'Party'");
		qp.put("lookup", "'MATKLLOV'");
		qp.put("pageSize", "2000");
//		qp.put("fields", "LookupValue.Code,LookupValueReference.LookupValues('ItemGroupProductLOV')");
//		rw.collectData("list", "LookupValue", null, "byLookup", qp, System.out::println);
//		System.exit(0);
		java.util.Map<String, String> qpw = new java.util.HashMap<>();
		qpw.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ItemGroupProductLOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ItemGroupConProductoSBBLOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV_S4H')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BRAND_IDLOV_S4H')"))
			, 500, request -> rw.writeData("list", "LookupValue", null, qpw, request, System.out::println) );
		rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> {
			rh.addRow(new org.json.JSONObject()
					.put("object", row.getJSONObject("object"))
					.put("values", 
						new org.json.JSONArray()
							.put(new org.json.JSONArray())
							.put(new org.json.JSONArray())
							.put(new org.json.JSONArray())
							.put(new org.json.JSONArray())
							.put(new org.json.JSONArray())
							.put(new org.json.JSONArray())
						)
				);
		});
		rh.sendData();
	}
	
}
