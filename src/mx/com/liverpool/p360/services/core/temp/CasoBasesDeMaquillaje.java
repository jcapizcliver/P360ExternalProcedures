package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CasoBasesDeMaquillaje {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and StandardizationValue.StructureGroup->LookupValueLang.Name(es) equals \"Pantallas\" and StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\" and not StandardizationValue.PropertyValue equals \"Atributos\"");
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("pageSize", "1000");

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Set<String> characteristics = new java.util.TreeSet<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				characteristics.add(values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		characteristics.forEach(System.out::println);

		System.out.println("*******");
		StringBuilder sb = new StringBuilder();
		for(String characteristic : characteristics) {
			sb.append(sb.length() == 0 ? "" : ",").append("\"").append(characteristic).append("\"");
		}

		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and StandardizationValue.StructureGroup->LookupValueLang.Name(es) equals \"Pantallas\" and StandardizationValue.Characteristic->Characteristic.Identifier in (" + sb.toString() + ")");
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.CreationType->LookupValue.Code,StandardizationValue.Property->LookupValue.Code,StandardizationValue.PropertyValue");

		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows", rowsPayload);

		java.util.Map<String, String> emptyQP = new java.util.TreeMap<>();

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'EU4-28122113_" + values.getString(0) + "_" + values.getString(1) + "_" + values.getString(2) + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put("EU4-28122113").put(values.getString(0)).put(values.getString(1)).put(values.getString(2)).put(values.getString(3))));
				if(rowsPayload.length() == 100) {
					System.out.println( workshop.makeRequest("POST", "/list/StandardizationValue", emptyQP, request.toString()).getJSONObject("counters") );
					while(rowsPayload.length() > 0) {
						rowsPayload.remove(0);
					}
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(rowsPayload.length() > 0) {
			System.out.println( workshop.makeRequest("POST", "/list/StandardizationValue", emptyQP, request.toString()).getJSONObject("counters") );
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}

}
