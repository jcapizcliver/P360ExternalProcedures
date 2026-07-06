package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectSpecificStatistics {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('WHSTC',root,\"0000.0000.RK\",'WHSTC')->LookupValue.Code"
			);
		qp.put("query", "not Product2GCharacteristicValue.LookupValue('WHSTC',root,\"0000.0000.RK\",'WHSTC') is empty");
		qp.put("pageSize", "5000");
		java.util.Map<String, Integer> values = new java.util.TreeMap<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				Integer freq = values.get(row.getJSONArray("values").getJSONArray(1).getString(0));
				values.put(row.getJSONArray("values").getJSONArray(1).getString(0), (freq == null ? 0 : freq) + 1); 
				if("0003".equals(row.getJSONArray("values").getJSONArray(1).getString(0))) {
					System.out.println(row.getJSONArray("values"));
				}
			}, System.out::println);
		values.entrySet().forEach(System.out::println);
	}
	
}
