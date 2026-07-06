package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectProductsWithItemsAndNoStatus {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "Product2G.CurrentStatus is empty and Product2G.ProductNo startsWith \"S\"");
		qp.put("pageSize", "50000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ProductNo.txt").toFile())))){
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				pw.println(row.getJSONArray("values").getString(0));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
