package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MarketplaceSinSKUTodavía {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,SimpleProduct2GCharacteristicValue.LookupValue('Business')->LookupValue.Code,SimpleProduct2GCharacteristicValueLang.Value('SKU',-1),Product2GLog.CreationDate(PIM),Product2GLog.ModificationDate(PIM)");
		qp.put("query", "Product2G.CurrentStatus = \"Creación de SKU\" and characteristic('SKU') is empty and characteristic('Business') = 'MKP'@'BusinessQualified' and Product2GLog.CreationDate(PIM) >= 2026-01-28T00:00:00");
		qp.put("pageSize", "10000");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			System.out.println(row.getJSONArray("values"));
		});
	}
	
}
