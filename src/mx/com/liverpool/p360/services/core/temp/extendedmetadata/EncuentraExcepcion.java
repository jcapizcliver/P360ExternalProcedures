package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class EncuentraExcepcion {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {

	}

	private static java.util.LinkedList<String> getThoseWithFilter(){
		java.util.LinkedList<String> those = new java.util.LinkedList<>();
		java.util.TreeMap<String, String> help = new java.util.TreeMap<>();
		help.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		help.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier");
		help.put("query",  "StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		do {
			help.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", help, null);
			if(response != null ) {
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					those.addLast(values.getString(0));
				}
			}else {
				System.out.println(rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return those;
	}

}
