package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class StageProductsWithNoItems {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.251.2:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "50000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "ProductsWithNoItems").toFile()),java.nio.charset.StandardCharsets.UTF_8))){
			rw.collectData("list", "Product2G", null, "withoutItem", qp, row -> {
				pw.println(row.getJSONArray("values").getString(0));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		qp.put("fields", "Article.SupplierAID");
		qp.put("pageSize", "50000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "ArticlesWithNoProduct").toFile()),java.nio.charset.StandardCharsets.UTF_8))){
			rw.collectData("list", "Article", null, "withoutProduct", qp, row -> {
				pw.println(row.getJSONArray("values").getString(0));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
