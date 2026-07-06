package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class TryOnProductVariantInfoAdmin {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {

		java.util.Map<String, java.util.List<String>> vars = new java.util.HashMap<>();
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GLog.CreationDate(PIM),Product2GLog.ModificationDate(PIM)");
		qp.put("query", "Product2G.ProductNo startsWith \"175461166\" and Product2G.CurrentStatus = \"Creación de SKU\" and characteristic('SKU') is empty");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(row.getJSONArray("values"));
			vars.put(values.getString(0), new java.util.ArrayList<>());
			sb.append(sb.length() == 0 ? "" : ",").append(row.getJSONObject("object").getString("id"));
		});
		qp.clear();
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		qp.put("products", sb.toString());
		rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			java.util.List<String> lst = vars.get(values.getString(0));
			if(lst == null) {
				System.out.println("-.- " + row.getJSONObject("object").getString("id"));
			}else {
				lst.add(row.getJSONObject("object").getString("id"));
			}
		});
		java.util.List<String> participants = new java.util.ArrayList<>();
		for(java.util.Map.Entry<String, java.util.List<String>> entry : vars.entrySet()) {
			if(entry.getValue().size() == 1) {
				participants.add(entry.getKey());
			}
		}
		System.out.println("These: " + participants.size() + "/" + vars.size());
		qp.clear();
		qp.put("fields", "Article.SupplierAID");
		qp.put("products", sb.toString());
		DataRequestor dr = new DataRequestor();
		java.util.List<String> thoseWhoAre = new java.util.ArrayList<>();
		java.util.List<String> thoseConsulted = new java.util.ArrayList<>();
		java.util.List<String> notIn = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String resp = null;
			System.out.println( resp = dr.getProductByVariant(new org.json.JSONArray().put(values.getString(0))) );
			org.json.JSONObject r = new org.json.JSONObject(resp);
			org.json.JSONArray items = r.getJSONArray("items");
			if(!"".equals(items.get(0))) {
				thoseWhoAre.add(values.getString(0));
			}else {
				notIn.add(values.getString(0));
			}
			thoseConsulted.add(values.getString(0));
		});
		System.out.println("A: " + thoseWhoAre.size() + "/" + thoseConsulted.size());
		System.out.println("Not in: ");
		notIn.forEach(System.out::println);
	}

}
