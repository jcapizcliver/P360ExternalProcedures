package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CreaciónDeMetadataGlobalPlantillaAtributo {

	
	public static void main(String[] args) {
		try {
			hechiceros();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	private static String[][] hechiceros() throws ServiceUnavailableException{
		boolean readFromFile = true;
		java.util.LinkedList<String[]> propiedades = new java.util.LinkedList<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.putParameter("fields", 
				   "StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		rw.putParameter("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		rw.putParameter("orderBy", "1-ASC,2-ASC");
		org.json.JSONObject response = null;
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		rw.putParameter("pageSize", "1200");
		String prevTemplate = null;
		String prevCharacteristic = null;
		org.json.JSONObject characteristicInTemplateDetails = new org.json.JSONObject();
		java.util.Map<String, org.json.JSONObject> globalCharacteristicDetails = new java.util.TreeMap<>();
		org.json.JSONObject aux = null;
		java.util.ArrayList<String> sections = new java.util.ArrayList<>();
		sections.add("Datos Básicos");
		sections.add("Datos Logísticos");
		sections.add("Datos de Venta");
		sections.add("Header");
		if(!readFromFile) {
			do {
				rw.putParameter("startIndex", String.valueOf(currentIndex));
				response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
				if(response != null) {
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					System.out.println(currentIndex + "/" + totalSize);
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						if( prevTemplate != null && prevCharacteristic != null && (!prevTemplate.equals(values.getString(0)) || !prevCharacteristic.equals(values.getString(1)) ) ) {
							if( characteristicInTemplateDetails.has("VendorCenterSection") && sections.contains(characteristicInTemplateDetails.getString("VendorCenterSection")) ) {
								aux = globalCharacteristicDetails.get(prevCharacteristic);
								if(aux == null) {
									characteristicInTemplateDetails.put("_characteristic", prevCharacteristic);
									globalCharacteristicDetails.put(prevCharacteristic, characteristicInTemplateDetails);
								}else {
									transferNewItems(characteristicInTemplateDetails, aux);
								}
							}
							characteristicInTemplateDetails = new org.json.JSONObject();
						}
						characteristicInTemplateDetails.put(values.getString(3), values.getString(4));
						prevTemplate = values.getString(0);
						prevCharacteristic = values.getString(1);
					}
				}else {
					System.out.println("ERR: " + rw.getRawResponse());
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
			if(characteristicInTemplateDetails.length() > 0) {
				aux = globalCharacteristicDetails.get(prevCharacteristic);
				if(aux == null) {
					characteristicInTemplateDetails.put("_characteristic", prevCharacteristic);
					globalCharacteristicDetails.put(prevCharacteristic, characteristicInTemplateDetails);
				}else {
					transferNewItems(characteristicInTemplateDetails, aux);
				}
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\desorden\\ConfiguraciónPlantillaCaracterística"), java.nio.charset.StandardCharsets.UTF_8))){
				globalCharacteristicDetails.forEach((k,v)-> {
					pw.println(k + "<:::>" + v);
				});
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			try(java.io.BufferedReader br = new java.io.BufferedReader( new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\ConfiguraciónPlantillaCaracterística"), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = line.split("<:::>");
					globalCharacteristicDetails.put(pieces[0], new org.json.JSONObject(pieces[1]));
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Now writing data to new dictionary...");
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		globalCharacteristicDetails.forEach((k,v)-> {
			for(String name : org.json.JSONObject.getNames(v)) {
				if(!"_tempalte".equals(name) && !"_characteristic".equals(name)) {
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + v.getString("_characteristic") + "<::>CreateProposal<::>" + name + "'@'GlobalTemplateAttributeConfiguration'")).put("values", new org.json.JSONArray().put( new org.json.JSONObject().put("id", "'" + v.getString("_characteristic") + "'") ).put( new org.json.JSONObject().put("id", "'CreateProposal'@'CreationType'") ).put( new org.json.JSONObject().put("id", "'" + name + "'@'GroupCharacteristicMetadataExtensionProperty'") ).put(v.get(name))));
					if(rowsPayload.length() == 120) {
						java.util.Map<String, String> qp = new java.util.TreeMap<>();
						org.json.JSONObject resp = rw.makeRequest("POST", "/list/StandardizationValue", qp, new org.json.JSONObject().put("rows", rowsPayload).put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).toString());
						if(resp != null) {
							System.out.println(resp.getJSONObject("counters"));
						}else {
							System.out.println("ERR: " + rw.getRawResponse());
						}
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
			}
		});
		if(rowsPayload.length() > 0) {
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject resp = rw.makeRequest("POST", "/list/StandardizationValue", qp, new org.json.JSONObject().put("rows", rowsPayload).put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).toString());
			if(resp != null) {
				System.out.println(resp.getJSONObject("counters"));
			}else {
				System.out.println("ERR: " + rw.getRawResponse());
			}
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
		return new String[][] { propiedades.toArray(new String[] {}) };
	}
	
	private static void transferNewItems(org.json.JSONObject no, org.json.JSONObject co) {
		if(no.length() > 0) {
			for(String key : org.json.JSONObject.getNames(no)) {
				if(!co.has(key) || !co.get(key).equals(no.get(key))) {
					co.put(key, no.get(key));
				}
			}
		}
	}
	
}
