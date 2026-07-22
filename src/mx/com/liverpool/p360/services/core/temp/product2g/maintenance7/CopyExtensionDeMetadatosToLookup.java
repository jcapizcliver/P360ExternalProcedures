package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CopyExtensionDeMetadatosToLookup {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "StandardizationValue.Value"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("dictionary", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("pageSize", "1000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "LookupValue.StructureGroup"))
					.put(new org.json.JSONObject().put("identifier", "LookupValue.Characteristic"))
					.put(new org.json.JSONObject().put("identifier", "LookupValue.CreationType"))
					.put(new org.json.JSONObject().put("identifier", "LookupValue.Property"))
					.put(new org.json.JSONObject().put("identifier", "LookupValue.PropertyValue"))
				, 1000, request -> rw.writeData("list", "LookupValue", null, qp0, request, System.out::println) );
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(values.getString(1)).put(values.getString(2)).put(values.getString(3)).put(values.getString(4)).put(values.getString(5))));
		});
		rh.sendData();
	}
	
	
}

