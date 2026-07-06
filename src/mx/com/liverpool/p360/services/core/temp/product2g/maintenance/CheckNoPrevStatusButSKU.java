package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CheckNoPrevStatusButSKU {


	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2G.PrevStatus"
				+ ",Product2G.CurrentStatus"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			);
		qp.put("query", "characteristic('SAPObjectType') = '00'@'ATTYPLOV' and Product2G.CurrentStatus = 1003");
//		qp.put("query", "Product2G.PrevStatus is empty and not characteristic('SKU') is empty");
		qp.put("pageSize", "5000");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			if(!row.getJSONArray("values").getString(0).startsWith("LVP"))
				System.out.println(row.getJSONArray("values").getString(0));
//			System.out.println(row.getJSONArray("values"));
		});
	}
	
}
