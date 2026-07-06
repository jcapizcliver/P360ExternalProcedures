package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RefreshFieldVendorCenterSections {

	
	public static void main(String[] args) {
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.PropertyValue");
        qp.put("query", "StandardizationValue.Property->LookupValue.Code = \"VendorCenterSection\"");
        qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
        qp.put("pageSize", "25000");
        java.util.Map<String, String> fieldVendorCenterSection = new java.util.TreeMap<>();
        System.out.println("Now collecting from templates...");
        rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
        	org.json.JSONArray values = row.getJSONArray("values");
        	fieldVendorCenterSection.put(values.getString(0), values.getString(1));
        });
        System.out.println("Got: " + fieldVendorCenterSection.size());
        System.out.println("Now collecting from global...");
        qp.put("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
        rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
        	org.json.JSONArray values = row.getJSONArray("values");
        	fieldVendorCenterSection.put(values.getString(0), values.getString(1).trim());
        });
		try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			fieldVendorCenterSection.entrySet().forEach(entry -> pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}
	
}
