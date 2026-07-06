package mx.com.liverpool.p360.services.core.temp.article;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class GetArticleDataToStageLaData {

	private static RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
//		doArticleData();
//		doSKUToProductNo();
//		doSKUToSupplierAID();
//		doArticleAIDToProductNo();
//		doProductNoToSupplierAIDs();
	}
	
	public static void doProductNoToSupplierAIDs() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "ProductReference.ReferencedSupplierAid"
			);
		qp.put("pageSize", "2000");
		java.util.Map<String, String> internalArticleToProductNo = new java.util.TreeMap<>();
		System.out.println("Collecting initial dictionary...");
		rw.collectData("list", "Article", "ProductReference", "withProduct", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			internalArticleToProductNo.put(row.getJSONObject("object").getString("id"), values.getString(0));
		});
		qp.put("fields", "Article.SupplierAID");
		java.util.Map<String, java.util.Set<String>> productToVariants = new java.util.TreeMap<>();
		rw.collectData("list", "Article", null, "withProduct", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String productNo = internalArticleToProductNo.get(row.getJSONObject("object").getString("id"));
			if(productNo == null) {
				System.out.println("PANIC, no match for this: " + row.getJSONObject("object").getString("id"));
				System.exit(0);
			}else {
				java.util.Set<String> set = productToVariants.get(productNo);
				if(set == null) {
					set = new java.util.TreeSet<>();
					productToVariants.put(productNo, set);
				}
				set.add(values.getString(0));
			}
		});
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(
				java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "admin.productno_to_variants").toFile()), java.nio.charset.StandardCharsets.UTF_8))
		){
			System.out.println("Now writing list...");
			productToVariants.entrySet().forEach(s -> pw.println( rw.getRw().serializeChunk(new String[] { s.getKey(), rw.getRw().serializeChunk(s.getValue().toArray(new String[] {}), "\"", ";", "\\") }) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void doArticleAIDToProductNo() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "ProductReference.ReferencedSupplierAid"
			);
		qp.put("pageSize", "2000");
		java.util.Map<String, String> internalArticleToProductNo = new java.util.TreeMap<>();
		System.out.println("Collecting initial dictionary...");
		rw.collectData("list", "Article", "ProductReference", "withProduct", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			internalArticleToProductNo.put(row.getJSONObject("object").getString("id"), values.getString(0));
		});
		qp.put("fields", "Article.SupplierAID");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(
				java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "admin.supplieraid_to_productno").toFile()), java.nio.charset.StandardCharsets.UTF_8))
		){
			System.out.println("Now writing list...");
			rw.collectData("list", "Article", null, "withProduct", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				String productNo = internalArticleToProductNo.get(row.getJSONObject("object").getString("id"));
				if(productNo == null) {
					System.out.println("PANIC, no match for this: " + row.getJSONObject("object").getString("id"));
					System.exit(0);
				}else {
					pw.println(rw.getRw().serializeChunk( new String[] { values.getString(0), productNo } ));
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void doSKUToSupplierAID() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			);
		qp.put("pageSize", "2000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(
				java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "admin.sku_to_supplieraid").toFile()), java.nio.charset.StandardCharsets.UTF_8))
		){
			rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getJSONArray(1).getString(0)))
					pw.println(rw.getRw().serializeChunk( new String[] { values.getJSONArray(1).getString(0), values.getString(0) } ));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void doSKUToProductNo() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			);
		qp.put("pageSize", "2000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(
				java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "admin.sku_to_productno").toFile()), java.nio.charset.StandardCharsets.UTF_8))
		){
			rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getJSONArray(1).getString(0)))
					pw.println(rw.getRw().serializeChunk( new String[] { values.getJSONArray(1).getString(0), values.getString(0) } ));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void doArticleData() {

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt',-1)->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico',-1)->LookupValue.Code"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',root,\"0000.0000.RK\",'ProductImage_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
				+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
			);
		qp.put("pageSize", "2000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "admin.article_data").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				pw.println(rw.getRw().serializeChunk( new String[] { values.getString(0), rw.getRw().serializeChunk(new String[] {values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0), values.getJSONArray(3).getString(0), values.getJSONArray(4).getString(0), values.getJSONArray(5).getString(0), values.getJSONArray(6).getString(0), values.getJSONArray(7).getString(0)}, "\"", ";", "\\") } ));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
