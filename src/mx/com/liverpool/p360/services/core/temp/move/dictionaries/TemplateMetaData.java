package mx.com.liverpool.p360.services.core.temp.move.dictionaries;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class TemplateMetaData {

	
	public static void main(String[] args) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rw.putParameter("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		rw.putParameter("fields", "StandardizationValue.StructureGroup->LookupValue.Code");
		rw.putParameter("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.util.Set<String> templates = new java.util.TreeSet<>();
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					templates.add(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}
				currentIndex += response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		templates.forEach(System.out::println);
	}
	
}
