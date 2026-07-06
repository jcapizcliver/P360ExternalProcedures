package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class EjemploError {

	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "dropMe" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'"))
				.put("values", new org.json.JSONArray()
						.put("Mock")
						.put("Mocka" )
						.put("Mocko" )
						.put("Mopet" )
						.put("Chill")
					)
				);
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
			if(response == null) {
				System.out.println("ERR: " + rw.getRawResponse());
			}else {
				System.out.println("RESP: " + response);
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
}
