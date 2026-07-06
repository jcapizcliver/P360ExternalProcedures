package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ColocaSAPObjectType {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> list = new java.util.ArrayList<>();
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2G.CurrentStatus"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business',-1)->LookupValue.Code"
			);
		qp.put("query", "characteristic('SAPObjectType') is empty and not Product2G.CurrentStatus is empty and not Product2G.CurrentStatus = 10031");
		qp.put("pageSize", "5000");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			list.add(values.getString(0));
			data.put(values.getString(0), values.getJSONArray(3).getString(0));
		});
		qp.clear();
		org.json.JSONObject req = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"));
		org.json.JSONArray rows = new org.json.JSONArray();
		req.put("columns", columns);
		req.put("rows", rows);
		org.json.JSONObject resp = null;
		for(String pn : list) {
			qp.put("products", "'" + pn + "'@1");
			resp = rw.getRw().makeRequest("GET", "/list/Article/byProducts", qp, null);
			int l = resp.getJSONArray("rows").length();
			if(l > 0) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pn + "'@1")).put("values", new org.json.JSONArray().put(l == 1 ? "00" : "MKP".equals(data.get(pn)) ? "00" : "01")));
			}
		}
		System.out.println(req);
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp, req, System.out::println);
	}
	
	
}
