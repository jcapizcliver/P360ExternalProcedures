package mx.com.liverpool.p360.services.core.temp.exports;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class DetermineTemplates {


	public static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		java.util.LinkedList<String> templates = new java.util.LinkedList<>();
		java.util.Set<String> setTemplates = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.StructureGroup->LookupValueLang.Name(es)");
		qp.put("query",
				       "StandardizationValue.Property->LookupValue.Code equals \"SentToVendorCenter\""
				+ " and StandardizationValue.PropertyValue equals \"1\""
				+ " and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response == null) {
				System.out.println("Error querying standardization value: " + rw.getRawResponse());
			}else {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					templates.addLast(values.getString(0) + "," + values.getString(2));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		setTemplates = new java.util.TreeSet<>(templates);
		setTemplates.forEach(System.out::println);
	}
}
