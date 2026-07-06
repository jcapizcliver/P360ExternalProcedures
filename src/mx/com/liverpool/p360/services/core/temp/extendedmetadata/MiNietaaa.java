package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class MiNietaaa {

	private static void simpleRequest() throws ServiceUnavailableException {
		String dictionary = "MiNietaaa";
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://172.18.237.210:1512/rest/V2.0");
		rw.putParameter("dictionary", dictionary);
		rw.putParameter("fields", 
				  "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		rw.putParameter("pageSize", "2000");
		rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
		System.out.println(rw.getRawResponse());
		
	}
	
	private static void simpleRequestDelete() throws ServiceUnavailableException {
		String dictionary = "MiNietaaa";
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Content-Type", "application/x-www-form-urlencoded");
		rw.setBaseUrl("https://172.18.237.210:1512/rest/V2.0");
		rw.putParameter("dictionary", dictionary);
		rw.putParameter("fields", 
				"StandardizationValue.Value"
						+ ",StandardizationValue.AlternativeValue"
						+ ",StandardizationValue.StructureGroup->LookupValue.Code"
						+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
						+ ",StandardizationValue.CreationType->LookupValue.Code"
						+ ",StandardizationValue.Property->LookupValue.Code"
						+ ",StandardizationValue.PropertyValue"
				);
		rw.putParameter("pageSize", "2000");
		rw.makeRequest("DELETE", "/list/StandardizationValue/byDictionary");
		System.out.println(rw.getRawResponse());
		
	}
	// Modelo [SupplierPartNumber]
	// BrandName
	// AcabadosVaD
	public static void main(String[] args) throws ServiceUnavailableException {
		simpleRequest();
		simpleRequestDelete();
//		System.exit(0);
		String dictionary  = "ReferenciaDeDatosDeNegocio_ProductoConfigurable"; // ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla
		String targetDictionary = "MiNietaaa";
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		int a = 0;
		int b = 0;
		rw.setBaseUrl("https://172.18.237.210:1512/rest/V2.0");
		rw.putParameter("dictionary", dictionary);
		rw.putParameter("fields", 
				  "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		rw.putParameter("pageSize", "2000");
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
//		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		int times = 0;
		do {
			rw.putParameter("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null) {
				rows = response.getJSONArray("rows");
				b = response.getInt("totalSize");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					times++;
					if(times == 1)
					addRow(values, rowsPayload, targetDictionary);
					if(rowsPayload.length() == 100) {
						rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
						System.out.println(rw.getRawResponse());
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
		if(rowsPayload.length() > 0) {
			System.out.println(rowsPayload);
			rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
			System.out.println(rw.getRawResponse());
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}
	
	private static void addRow(org.json.JSONArray values, org.json.JSONArray rows, String dictionary) {
		String id = values.getString(0);
		org.json.JSONArray nv = new org.json.JSONArray();
		for(int i=1; i<values.length(); i++) {
			if(i != 2)
				nv.put(values.getString(i));
		}
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + java.util.concurrent.ThreadLocalRandom.current().nextDouble() +"'@'" + dictionary + "'")).put("values", nv));
	}
}
