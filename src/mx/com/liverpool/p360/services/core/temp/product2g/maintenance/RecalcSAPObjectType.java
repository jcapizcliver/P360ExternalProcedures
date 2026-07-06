package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RecalcSAPObjectType {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)->LookupValue.Code");
		qp.put("pageSize", "50000");
		System.out.println(rw.getRw().getBaseUrl());
		org.json.JSONArray rows = new org.json.JSONArray();
		rw.collectData("list", "Product2G", null, "withItem", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String sapObjectType = values.getJSONArray(1).getString(0);
//			System.out.println(values);
			if("01".equals(sapObjectType)) {
				java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
				qp1.put("fields", "Article.SupplierAID,ArticleCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)->LookupValue.Code");
				qp1.put("products", "'" + values.getString(0) + "'@1");
				qp1.put("pageSize", "10000");
				rw.collectData("list", "Article", null, "byProducts", qp1, row1 -> {
					if("01".equals(sapObjectType) && !"02".equals(row1.getJSONArray("values").getJSONArray(1).getString(0)))
						rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row1.getJSONArray("values").getString(0) + "'@1")).put("values", new org.json.JSONArray().put("02")));
				});
			}
		});
		System.out.println(rows);
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Article", null, qp0, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"))).put("rows", rows), System.out::println);
	}
	
}
