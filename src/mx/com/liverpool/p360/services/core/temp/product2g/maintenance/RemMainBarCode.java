package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RemMainBarCode {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Article.SupplierAID,ArticleExtraHeaderData5.ProductName");
		qp.put("query", "not ArticleExtraHeaderData5.ProductName is empty");
		qp.put("pageSize", "10000");
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
		});
		
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",SimpleArticleCharacteristicValueLang.Value('MainBarCode',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('MainBarCodeS4H',-1)"
			);
		qp.put("query", "Article.CurrentStatus = \"Eliminada\" and ( not characteristic('MainBarCode') is empty or not characteristic('MainBarCodeS4H') is empty )");
		java.util.List<String> estas = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			estas.add(row.getJSONObject("object").getString("id"));
		} );
		int cnt =  0;
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"));
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		for(String es : estas) {
			cnt++;
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", es)).put("values", new org.json.JSONArray().put("").put("")));
			if(cnt % 10000 == 0) {
				rw.writeData("list", "Article", null, qp0, request, System.out::println);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			rw.writeData("list", "Article", null, qp0, request, System.out::println);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

}
