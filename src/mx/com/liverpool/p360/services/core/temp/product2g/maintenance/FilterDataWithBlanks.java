package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class FilterDataWithBlanks {
	
	
	private static RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://gcpcatpap01.liverpool.com.mx:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValueLang.Value('EnrichmentRejectionMessage',root,\"0000.0000.RK\",'EnrichmentRejectionMessage',-1)"
			);
		qp.put("query", "characteristic('EnrichmentRejectionMessage') contains \"\\n\"");
		qp.put("pageSize", "10000");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnrichmentRejectionMessage',root,\"0000.0000.RK\",'EnrichmentRejectionMessage',-1)"));
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rows.put(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(values.getString(0).replaceAll("\\n", "\\\\n"))));
		});
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp, request, System.out::println);
	}

}
