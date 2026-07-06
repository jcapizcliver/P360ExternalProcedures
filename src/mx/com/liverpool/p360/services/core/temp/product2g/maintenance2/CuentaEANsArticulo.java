package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CuentaEANsArticulo {

	
	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus,Product2G.PrevStatus,Product2GExtraData.SupplierID(MX)->LookupValue.Code");
		qp.put("pageSize", "5000");
		qp.put("query", "Product2GExtraData.SupplierID(MX)->LookupValue.Code = \"4000170\" and not Product2G.EAN is empty");
		java.util.Map<String, String[]> data = new java.util.HashMap<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			data.put(values.getString(0), new String[] { values.getString(1), values.getString(2), values.getJSONArray(3).getString(0) });
		});
		StringBuilder sb = new StringBuilder();
		int a = 0;
		java.util.Map<String, String> qp1 = new java.util.HashMap<>();
		qp1.put("fields", "ProductReference.ArticleSupplierAid");
		qp1.put("pageSize", "5000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", "Article.SupplierAID,Article.EAN");
		qp0.put("pageSize", "5000");
		for(String s : data.keySet()) {
			sb.append(sb.length() == 0 ? "" : ",").append(s);
			a++;
			if(a  % 1000 == 0) {
				rw.collectData("Article", "ProductReference", "byProducts", s, qp0, null);
				sb.setLength(0);
			}
		}
	}
	
}
