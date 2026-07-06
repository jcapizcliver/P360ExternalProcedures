package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class FreeToDelete {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "2000");
		java.util.List<String> tbd = new java.util.ArrayList<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("ToDeleteProdNo.txt").toFile())))){
			rw.collectData("list", "Product2G", null, "withoutItem", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				pw.println(values.getString(0));
				tbd.add(row.getJSONObject("object").getString("id"));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		int a = 0;
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		for(String b : tbd) {
			sb.append(sb.length() == 0 ? "" : ",").append(b);
			a++;
			if(a % 5000 == 0) {
				qp0.put("items", sb.toString());
				rw.deleteData("list", "Product2G", null, "byItems", qp0, System.out::println);
				sb.setLength(0);
			}
		}
		if(a % 5000 != 0) {
			qp0.put("items", sb.toString());
			rw.deleteData("list", "Product2G", null, "byItems", qp0, System.out::println);
			sb.setLength(0);
		}
		
	}
	
	
}
