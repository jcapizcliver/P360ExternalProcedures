package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class IdentifyCategoriesByECCFieldMapping {

	
	public static void main(String[] args) {
		java.util.Map<String, String> fieldECCMapping = new java.util.TreeMap<>();
		java.util.Map<String, String> fieldVendorCenterSection = new java.util.TreeMap<>();
		java.util.List<String> eccFields = null;
		try {
			eccFields = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("SAPAttributeIDs"), java.nio.charset.StandardCharsets.UTF_8);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(eccFields != null) {
			RESTWrapper rw = new RESTWrapper();
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.PropertyValue");
	        qp.put("query", "StandardizationValue.Property->LookupValue.Code = \"VendorCenterSection\"");
	        qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
	        qp.put("pageSize", "25000");
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
	        	fieldVendorCenterSection.put(values.getString(0), values.getString(1));
	        });
	        qp.clear();
	        qp.put("fields", "Characteristic.Identifier,CharacteristicIdentifier.AlternativeIdentifier(ECC)");
	        qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.Identifier wildcard \"%_Rechazo\" and not CharacteristicIdentifier.AlternativeIdentifier(ECC) is empty");
	        qp.put("pageSize", "10000");
	        rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
	        	org.json.JSONArray values = row.getJSONArray("values");
	        	if(!"".equals(values.getString(1)))
	        		fieldECCMapping.put(values.getString(1), values.getString(0));
	        });
	        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("KnownSection").toFile()))); java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("UnknownSections").toFile())))){
	        	eccFields.forEach(f -> {
	        		String characteristicIdentifier = fieldECCMapping.get(f);
	        		if(characteristicIdentifier != null) {
	        			pw.println( rw.getRw().serializeChunk(new Object[] { f, characteristicIdentifier, nvl(fieldVendorCenterSection.get(characteristicIdentifier)) }) );
	        		}else {
	        			pw2.println(f);
	        		}
	        	});
	        }catch(java.io.IOException e) {
	        	e.printStackTrace();
	        }
		}
	}
	
	private static String nvl(String val) {
		return val == null ? "" : val;
	}
	
}
