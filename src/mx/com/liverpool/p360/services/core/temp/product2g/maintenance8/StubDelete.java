package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class StubDelete {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
//		qp.put("items", "'1754611683722269'@1,'1754611683722173'@1");
//		rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
//		
//		System.exit(0);
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.clear();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es),LookupValueReference.LookupValues('TipoProveedorSAPAttLOV')->LookupValue.Code");
		qp.put("lookup", "'Party'");
		qp.put("items", "'111316'@'Party'");
		rw.collectData("list", "LookupValue", null, "byItems", qp, System.out::println);
		
	}
	
}
