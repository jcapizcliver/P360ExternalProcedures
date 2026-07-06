package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class IdentificaProductosDeMás {

	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "50000");
		java.util.LinkedList<String> productNumbers = new java.util.LinkedList<>();
		java.util.LinkedList<String> supplierAids = new java.util.LinkedList<>();
		rw.collectData("list", "Product2G", null, "withoutItem", qp, row -> productNumbers.addLast(row.getJSONArray("values").getString(0)), System.out::println);
		System.out.println("Products without any item: " + productNumbers.size());
		qp.put("fields", "Article.SupplierAID");
		rw.collectData("list", "Article", null, "withProduct", qp, row -> supplierAids.addLast(row.getJSONArray("values").getString(0)), System.out::println);
		System.out.println("Articles with product: " + supplierAids.size());
		java.util.Set<String> articleSet = new java.util.TreeSet<>(supplierAids);
		supplierAids.clear();
		java.util.LinkedList<String> fakeProducts = new java.util.LinkedList<>();
		for(String productNo : productNumbers) {
			if(articleSet.contains(productNo)) {
				fakeProducts.addLast(productNo);
			}
		}
		System.out.println(fakeProducts.size());
//		qp.clear();
//		qp.put("includeObjectsInProtocol", "false");
//		RequestHandler setToDeleted = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 10000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println));
//		for(String productNo : fakeProducts) {
//			setToDeleted.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productNo + "'@1")).put("values", new org.json.JSONArray().put("1025")));
//		}
//		setToDeleted.sendData();
//		System.exit(0);
		System.out.println("Now deleting...");
		StringBuilder sb = new StringBuilder();
		qp.clear();
		int counter = 0;
		for(String productNo : fakeProducts) {
			sb.append(sb.length() > 0 ? "," : "");
			sb.append("'");
			sb.append(productNo);
			sb.append("'@1");
			counter++;
			if(counter == 1000) {
				qp.put("items", sb.toString());
				rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
				sb.setLength(0);
				counter = 0;
			}
		}
		if(counter > 0) {
			qp.put("items", sb.toString());
			rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
			sb.setLength(0);
		}
	}
	
}
