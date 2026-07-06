package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MkpIndividuals {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "Product2G.ProductNo,SimpleProduct2GCharacteristicValueLang.Value('SKU',-1),Product2GLog.CreationDate(PIM)");
//		qp.put("query", 
//				       "not Product2G.ProductNo startsWith \"S\""
//				+ " and not characteristic('SKU') startsWith \"999\""
//				+ " and not characteristic('SKU') is empty"
//				+ " and characteristic('Business') = 'MKP'@'BusinessQualified'"
//			);
//		qp.put("pageSize", "10000");
//		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
//			org.json.JSONArray values = row.getJSONArray("values");
//			System.out.println(values);
//		});

		qp.put("fields", "Article.SupplierAID,SimpleArticleCharacteristicValueLang.Value('SKU',-1),ArticleLog.CreationDate(PIM)");
		qp.put("query", 
				       "not Article.SupplierAID startsWith \"S\""
				+ " and characteristic('SKU') startsWith \"999\""
				+ " and not characteristic('SKU') is empty"
//				+ " and characteristic('Business') = 'MKP'@'BusinessQualified'"
			);
		qp.put("pageSize", "10000");
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
		});
//		qp.put("fields", "ProductReference.ReferencedSupplierAid");
//		qp.put("products", "'1754611668480459'@1,'1754611668480423'@1");
//		rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
//			System.out.println(row.getJSONArray("values"));
//		});
	}
	
}
