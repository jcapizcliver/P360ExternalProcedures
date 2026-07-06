package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class NotificaleLosCambiosDeFiltros {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
				+ "StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\" and StandardizationValue.Property equals ListOfValuesFilter");

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Set<String> templateCharacteristicFilters = new java.util.TreeSet<>();

		do{
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				templateCharacteristicFilters.add(values.getString(0) + "__" + values.getString(1));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		String[] pieces = null;
		org.json.JSONObject pubSubBody = null;
		org.json.JSONArray valueListRelations = new org.json.JSONArray();
		PubSubGCP psd = new PubSubGCP("D:\\tmp\\crp-dev-dig-vccatalog-b74410667aea.json", "", "");
		PubSubGCP psq = new PubSubGCP("D:\\tmp\\crp-qas-dig-vccatalog-416185bab156.json", "", "");
		for(String key : templateCharacteristicFilters) {
			pieces = key.split("__");
			pubSubBody = new org.json.JSONObject().put("idTemplate",pieces[0]).put("attributeName",pieces[1]);
			valueListRelations.put(pubSubBody);
			if(valueListRelations.length() == 100) {

				while(valueListRelations.length() > 0) {
					valueListRelations.remove(0);
				}
			}
		}
		if(valueListRelations.length() > 0) {

			while(valueListRelations.length() > 0) {
				valueListRelations.remove(0);
			}
		}
		System.out.println(response);

	}


}
