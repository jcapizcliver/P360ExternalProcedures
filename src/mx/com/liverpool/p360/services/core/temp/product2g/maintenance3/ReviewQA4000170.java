package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class ReviewQA4000170 {


	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Product2GExtraData.SupplierID(MX)->LookupValue.Code = \"4000170\" and Product2G.CurrentStatus = \"Revisión QA\"");
		qp.put("pageSize", "2000");
		qp.put("fields", "Product2G.ProductNo");
		try(DBAccessDataStub dastub = new DBAccessDataStub(new ELog() {
			
			@Override
			public void logE(Exception e) {
			}
			
			@Override
			public void log(String message) {
			}
		})){
			DataRequestor dr = new DataRequestor(dastub);
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				java.util.Set<String> varIds = dr.getVariants(values.getString(0));
				org.json.JSONArray items = new org.json.JSONArray();
				varIds.forEach(items::put);
				boolean withImage = false;
				items = new org.json.JSONObject( dr.getArticleData(items) ).getJSONArray("items");
				for(int i=0; i<items.length(); i++) {
	//				System.out.println("Some overtime: " + items.getJSONObject(i).getString("variant") + " - " + items.getJSONObject(i).getString("ProductNo"));
					if( !"".equals(items.getJSONObject(i).getString("ProductImage")) ) {
						withImage = true;
					}
	//				System.out.println("\t-->" + items.getJSONObject(i).getString("ProductImage") + "<--");
				}
				if(!withImage) {
	//				System.out.println("Should send: " + new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("1004")));
					rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("1004")));
				}
			});
			rh.sendData();
		}
	}
	
}
