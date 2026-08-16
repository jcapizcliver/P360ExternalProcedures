package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RevisaLosMasterPacks {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValueLang.Value('ZLAEPQ',root,\"0000.0000.RK\",'ZLAEPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZBREPQ',root,\"0000.0000.RK\",'ZBREPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZHOEPQ',root,\"0000.0000.RK\",'ZHOEPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZVOLPQ',root,\"0000.0000.RK\",'ZVOLPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZBRGPQ',root,\"0000.0000.RK\",'ZBRGPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZNTGPQ',root,\"0000.0000.RK\",'ZNTGPQ',-1)"
			);
		qp.put("pageSize", "5000");
		qp.put("query", "characteristic('ZNTGPQ') = \"0.0\" or characteristic('ZBRGPQ') = \"0.0\" or characteristic('ZVOLPQ') = \"0.0\" or characteristic('ZHOEPQ') = \"0.0\" or characteristic('ZBREPQ') = \"0.0\" or characteristic('ZLAEPQ') = \"0.0\"");
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		columns
			.put(
				new org.json.JSONObject().put("identifier", 
						"Product2GCharacteristicValueLang.Value('ZLAEPQ',root,\"0000.0000.RK\",'ZLAEPQ',-1)"
					)
				)
			.put(
				new org.json.JSONObject().put("identifier", 
						"Product2GCharacteristicValueLang.Value('ZBREPQ',root,\"0000.0000.RK\",'ZBREPQ',-1)"
					)
				)
			.put(
				new org.json.JSONObject().put("identifier", 
						"Product2GCharacteristicValueLang.Value('ZHOEPQ',root,\"0000.0000.RK\",'ZHOEPQ',-1)"
					)
				)
			.put(
				new org.json.JSONObject().put("identifier", 
						"Product2GCharacteristicValueLang.Value('ZVOLPQ',root,\"0000.0000.RK\",'ZVOLPQ',-1)"
					)
				)
			.put(
				new org.json.JSONObject().put("identifier", 
						"Product2GCharacteristicValueLang.Value('ZBRGPQ',root,\"0000.0000.RK\",'ZBRGPQ',-1)"
					)
				)
			.put(
				new org.json.JSONObject().put("identifier", 
						"Product2GCharacteristicValueLang.Value('ZNTGPQ',root,\"0000.0000.RK\",'ZNTGPQ',-1)"
					)
				)
			;
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> rows.put(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("").put("").put("").put("").put("").put(""))));
		java.util.Map<String, String> qp1 = new java.util.HashMap<>();
		qp1.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp1, new org.json.JSONObject().put("columns", columns).put("rows", rows), System.out::println);
	}
	
}
