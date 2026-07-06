package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CopiaDatos {

	
	public static void main(String[] args) {
		RESTWrapper rw  = new RESTWrapper();
		RESTWrapper rw2 = new RESTWrapper();
		rw2.getRw().setBaseUrl("https://gcpcatqap01.liverpool.com.mx:1512/rest/V2.0");
		rw2.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		qp0.put("fields", 
				   "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp0.put("pageSize", "15000");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))
			, 5000, request -> rw2.writeData("list", "StandardizationValue", null, qp, request, System.out::println) );
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp0, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rh.addRow(new org.json.JSONObject()
					.put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'"))
					.put("values", new org.json.JSONArray().put(values.getString(1)).put(values.getString(2)).put(values.getString(3)).put(values.getString(4)).put(values.getString(5)).put(values.getString(6))));
		});
		rh.sendData();
	}
	
}
