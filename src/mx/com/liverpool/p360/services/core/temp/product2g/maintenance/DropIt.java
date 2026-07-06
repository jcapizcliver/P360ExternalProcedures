package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DropIt {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus");
		qp.put("formatData", "true");
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> System.out.println( row.getJSONArray("values") ));
	}
	
}
