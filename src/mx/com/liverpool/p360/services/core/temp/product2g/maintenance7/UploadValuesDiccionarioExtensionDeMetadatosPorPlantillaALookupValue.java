package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class UploadValuesDiccionarioExtensionDeMetadatosPorPlantillaALookupValue {


	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "LookupValue.StructureGroup"))
				.put(new org.json.JSONObject().put("identifier", "LookupValue.Characteristic"))
				.put(new org.json.JSONObject().put("identifier", "LookupValue.CreationType"))
				.put(new org.json.JSONObject().put("identifier", "LookupValue.Property"))
				.put(new org.json.JSONObject().put("identifier", "LookupValue.PropertyValue"))
			, 2000, request -> rw.writeData("list", "LookupValue", null, qp, request, System.out::println) );
		int[] times = new int[] {0};
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			times[0]++;
			if(times[0] == 1) {
				return;
			}
			if(row.length > 0) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[0] + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(row[1]).put(row[2]).put(row[3]).put(row[4]).put(row.length < 6 ? "" : row[5])));
			}
		} );
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "AltaStandardizationValues.txt"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ActualizarDiccionarioDeMetadataDePlantilla.txt"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "MetadataTemplateCharacteristic20260716_020948.csv"));
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ExtensionDeMetadatosVPPP_20260720_094838.csv"));
		rh.sendData();
	}
	
}
