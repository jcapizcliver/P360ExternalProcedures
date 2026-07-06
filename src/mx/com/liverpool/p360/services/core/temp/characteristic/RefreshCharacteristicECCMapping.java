package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RefreshCharacteristicECCMapping {

	
	public static void main(String[] args) {
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> fieldECCMapping = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,CharacteristicIdentifier.AlternativeIdentifier(ECC)");
        qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.Identifier wildcard \"%_Rechazo\" and not CharacteristicIdentifier.AlternativeIdentifier(ECC) is empty");
        qp.put("pageSize", "10000");
        rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
        	org.json.JSONArray values = row.getJSONArray("values");
        	if(!"".equals(values.getString(1)))
        		fieldECCMapping.put(values.getString(1).trim(), values.getString(0));
        });
		try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			fieldECCMapping.entrySet().forEach(entry -> pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}
	
}
