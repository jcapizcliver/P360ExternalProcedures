package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AdvanceMKP {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "Product2G.CurrentStatus = \"Creación de SKU\" and characteristic('Business') = 'MKP'@'BusinessQualified' and not characteristic('SKU') is empty");
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("", "");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			
		});
	}
	
}
