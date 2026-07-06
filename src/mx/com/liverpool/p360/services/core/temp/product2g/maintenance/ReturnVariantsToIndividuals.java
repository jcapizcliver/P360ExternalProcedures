package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ReturnVariantsToIndividuals {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("query", "characteristic('SAPObjectType') = '01'@'ATTYP_LOV'");
		qp.put("pageSize", "10000");
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONObject req = new org.json.JSONObject();
		req.put("columns", columns);
		req.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"));
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			rows.put(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("00")));
		});
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Article", null, qp0, req, System.out::println);
	}
	
}
