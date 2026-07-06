package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ProductosQueSonVariantes {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		System.out.println("Now collecting article references to products");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		qp.put("pageSize", "4000");
		qp.put("query", "not ProductReference.ReferencedSupplierAid is empty");
		java.util.Map<String, String> articleToParent = new java.util.TreeMap<>();
		rw.collectData("list", "Article", "ProductReference", "bySearch", qp, row -> {
			articleToParent.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
		}, System.out::println);
		System.out.println("Now collecting articles");
		java.util.Map<String, String> realArticleToParent = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.remove("query");
		rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
			String refProduct = articleToParent.get(row.getJSONObject("object").getString("id"));
			if(refProduct != null) {
				realArticleToParent.put(row.getJSONArray("values").getString(0), refProduct);
			}
		});
		System.out.println("Now collecting products without items");
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "1200");
		qp.remove("query");
		java.util.LinkedList<String> pids = new java.util.LinkedList<>();
		rw.collectData("list", "Product2G", null, "withoutItem", qp, row -> pids.addLast(row.getJSONArray("values").getString(0)));
		System.out.println("Now evaluating...");
		int a = 0;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\desorden\\PROD\\variantesQueSeCrearonComoIndividualPeroQueTienenFamiliaYEstánEnElla")))){
			for(String pid : pids) {
				if(realArticleToParent.containsKey(pid)) {
					pw.println(pid);
					a++;
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Found: " + a + " suspects.");
	}
	
}
