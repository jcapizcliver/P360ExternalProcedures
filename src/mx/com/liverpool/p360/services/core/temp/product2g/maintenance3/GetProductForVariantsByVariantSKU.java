package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class GetProductForVariantsByVariantSKU {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Article.SKU in (1182708981,1189746063,1189641008,1190224038,1193580421,1189523102,1189749241,1189523072,1189146996,1189533108)");
		java.util.List<String> iids = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "bySearch", qp, row -> iids.add(row.getJSONObject("object").getString("id")));
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<iids.size(); i++) {
			sb.append(sb.length() == 0 ? "" : ",").append(iids.get(i));
		}
		qp.remove("query");
		System.out.println(sb);
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		qp.put("items", sb.toString());
		java.util.Set<String> pids = new java.util.TreeSet<>();
		java.util.Map<String, String> ap = new java.util.HashMap<>();
		rw.collectData("list", "Article", "ProductReference", "byItems", qp, row ->{
			pids.add(row.getJSONArray("values").getString(0));
			ap.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
		});
		qp.clear();
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus,Product2G.PrevStatus,Product2G.ExternalStatus->LookupValue.Code");
		java.util.Map<String, String[]> productStatus = new java.util.HashMap<>();
		sb.setLength(0);
		for(String pid : pids) {
			sb.append(sb.length() == 0 ? "" : ",").append("'").append(pid).append("'@1");
		}
		qp.put("items", sb.toString());
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			productStatus.put(values.getString(0), new String[] {values.getString(1), values.getString(2), values.getString(3)});
		});
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.PrevStatus")).put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus")).put(new org.json.JSONObject().put("identifier", "Article.ExternalStatus")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
		for(java.util.Map.Entry<String, String> entry : ap.entrySet()) {
			String[] pieces = productStatus.get(entry.getValue());
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", entry.getKey() )).put("values", new org.json.JSONArray().put(pieces[0]).put(pieces[1]).put(pieces[2])));
		}
		rh.sendData();
	}
	
}
