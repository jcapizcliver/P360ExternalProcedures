package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class BajaLaData {

	private static final RESTWrapper rw = new RESTWrapper();
	
	private static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"StandardizationValue.Value"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.AlternativeValue"
			);
		qp.put("pageSize", "25000");
		qp.put("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "standardizationValues", "data_" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date())).toFile())))){
			rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
				
			}, null);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
