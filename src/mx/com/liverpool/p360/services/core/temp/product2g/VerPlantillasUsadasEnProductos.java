package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class VerPlantillasUsadasEnProductos {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier");
		qp.put("pageSize", "5000");
		java.util.Map<String, Integer> map = new java.util.TreeMap<>();
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			String tid = row.getJSONArray("values").getJSONArray(0).getString(0);
			Integer freq = map.get(tid);
			map.put(tid, (freq == null ? 0 : freq) + 1);
		}, System.out::println);
		map.entrySet().forEach(System.out::println);
		System.out.println("Total: " + map.size());
	}
	
}
