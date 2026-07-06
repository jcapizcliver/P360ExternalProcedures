package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class RetiraMetadataGlobalDePlantillas {
	
	public static void main(String[] args) throws ServiceUnavailableException {
		java.util.LinkedList<String> globalData = new java.util.LinkedList<>();
		addGlobalData(globalData, null);
		for(String attribute : globalData) {
			deleteFromTemplateCharacteristicMetadataDefinition(attribute);
		}
	}
	
	private static void deleteFromTemplateCharacteristicMetadataDefinition(String attribute) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.putParameter("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		rw.putParameter("query", 
				       "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.Characteristic->Characteristic.Identifier equals \"" + attribute + "\""
			);
		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		org.json.JSONObject response = null;
		response = rw.makeRequest("DELETE", "/list/StandardizationValue/bySearch");
		if(response != null) {
			System.out.println(response);
		}else {
			System.out.println("ERR: " + rw.getRawResponse());
		}
	}
	
	private static void addGlobalData(java.util.LinkedList<String> attributes, String baseUrl) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null)
			rw.setBaseUrl(baseUrl);
		rw.putParameter("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
		rw.putParameter("fields", 
				   "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
				+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValueLog.ModificationDate(PIM)"
				+ ",StandardizationValueLog.CreationDate(PIM)"
			);
		rw.putParameter("query", 
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
			);
		rw.putParameter("orderBy", "0-ASC");
		rw.putParameter("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		org.json.JSONObject detail = new org.json.JSONObject();
		org.json.JSONArray prevValues = null;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(prevValues != null && !prevValues.getString(0).equals(values.getString(0))) {
						attributes.addLast(prevValues.getString(0));
						detail = new org.json.JSONObject();
					}
					detail.put("characteristic", values.getString(0));
					detail.put("friendlyName", values.getString(1));
					detail.put(values.getString(2), values.getString(3));
					prevValues = values;
				}
			}else {
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(detail.length() > 0) {
			attributes.addLast(prevValues.getString(0));
			detail = null;
		}
	}
	
}
