package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ReviewSKUPresence {

	private static RESTWrapper rw = new RESTWrapper();

	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("pageSize", "20000");
		qp.put("query", "not characteristic('SKU',-1) is empty");
		java.util.Map<String, String> skuToProductNo = new java.util.TreeMap<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> skuToProductNo.put(row.getJSONArray("values").getJSONArray(1).getString(0), row.getJSONArray("values").getString(0)), System.out::println);
		
		qp.put("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("pageSize", "20000");
		qp.put("query", "not characteristic('SKU',-1) is empty");
		java.util.Map<String, String> skuToSupplierAID = new java.util.TreeMap<>();
		rw.collectData("list", "Article", null, "bySearch", qp, row -> skuToSupplierAID.put(row.getJSONArray("values").getJSONArray(1).getString(0), row.getJSONArray("values").getString(0)), System.out::println);
		
		java.util.LinkedList<String> skusDeInteresGen = new java.util.LinkedList<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUMigraciónWorkshop", "faltantes_gen_unique"))){
			lns.forEach(skusDeInteresGen::addLast);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.LinkedList<String> skusDeInteresVar = new java.util.LinkedList<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUMigraciónWorkshop", "faltantes_var"))){
			lns.forEach(skusDeInteresVar::addLast);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		java.util.LinkedList<String> losQueNoGen = new java.util.LinkedList<>();
		java.util.LinkedList<String> losQueNoVar = new java.util.LinkedList<>();
		for(String id : skusDeInteresGen) {
			if(!skuToProductNo.containsKey(id)) {
				losQueNoGen.addLast(id);
			}
		}
		for(String id : skusDeInteresVar) {
			if(!skuToSupplierAID.containsKey(id)) {
				losQueNoVar.addLast(id);
			}
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUMigraciónWorkshop", "real_faltantes_gen").toFile())))){
			losQueNoGen.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUMigraciónWorkshop", "real_faltantes_var").toFile())))){
			losQueNoVar.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
