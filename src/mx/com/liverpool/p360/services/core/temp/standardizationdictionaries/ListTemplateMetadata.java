package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ListTemplateMetadata {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		String dictionary = "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla";
//		String dictionary = "GlobalTemplateAttributeConfiguration";
		String template = "EU4-4730321";
		workshop.setBaseUrl("http://172.18.237.162:1512/rest/V1.0");
		workshop.getRc().getHeader().put("Authorization", java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Value"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Dictionary"
			);
		qp.put("query", 
				"StandardizationValue.Characteristic is empty"
				+ " and StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"" + dictionary + "\""
//				   "StandardizationValue.CreationType->LookupValue.Code = \"CreateProposal\""
//				  " StandardizationValue.Value = \"1753325222122\""
//				+ " and StandardizationValue.Property->LookupValue.Code = \"VendorCenterSection\""
//				+ " and StandardizationValue.PropertyValue = \"Datos Básicos\""
			);
		qp.put("dictionaryProxy", "'" + dictionary + "'");
		qp.put("pageSize", "2000");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			System.out.println(row.getJSONArray("values"));
		}, System.out::println);
		rw.deleteData("list", "StandardizationValue", null, "bySearch", qp, System.out::println);
	}
}
