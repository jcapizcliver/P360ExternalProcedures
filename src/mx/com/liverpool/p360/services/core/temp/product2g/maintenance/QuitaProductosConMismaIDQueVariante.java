package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QuitaProductosConMismaIDQueVariante {

	private static RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "10000");
		java.util.List<String> productNos = new java.util.ArrayList<>(100000);
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> productNos.add(row.getJSONArray("values").getString(0)));
		qp.put("fields", "Article.SupplierAID");
		java.util.List<String> supplierAIDs = new java.util.ArrayList<>(100000);
		rw.collectData("list", "Article", null, "byCatalog", qp, row -> supplierAIDs.add(row.getJSONArray("values").getString(0)));
		java.util.Collections.sort(supplierAIDs);
		java.util.List<String> losQueNo = new java.util.ArrayList<>();
		for(String productNo : productNos) {
			if(java.util.Collections.binarySearch(supplierAIDs, productNo) >= 0) {
				losQueNo.add(productNo);
			}
		}
		qp.clear();
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<losQueNo.size(); i++) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'");
			sb.append(losQueNo.get(i));
			sb.append("'@1");
			if(i % 200 == 0) {
				qp.put("items", sb.toString());
				rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
				sb.setLength(0);
			}
		}
		if(!sb.isEmpty()) {
			qp.put("items", sb.toString());
			rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
		}
	}
	
}
