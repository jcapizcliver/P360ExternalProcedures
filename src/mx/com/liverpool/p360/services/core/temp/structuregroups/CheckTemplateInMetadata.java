package mx.com.liverpool.p360.services.core.temp.structuregroups;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CheckTemplateInMetadata {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"StandardizationValue.StructureGroup->LookupValue.Code"
			);
		qp.put("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		qp.put("pageSize", "5000");
		java.util.Map<String, Integer> lst = new java.util.TreeMap<>();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> { 
				Integer freq = lst.get(row.getJSONArray("values").getString(0));
				lst.put(row.getJSONArray("values").getString(0), (freq == null ? 0 : freq ) + 1);
		},  System.out::println);
		lst.entrySet().forEach(System.out::println);
		System.out.println("Hola: " + lst.size());
	}
	
	
	
}
