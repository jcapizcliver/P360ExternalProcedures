package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CharacteristicRecordsInspect {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		String entity = "Article";

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("catalog", "TESTING");
		qp.put("query", "not Article.SupplierAID is empty");
		qp.put("fields",entity + "CharacteristicValue.Characteristic->Characteristic.Identifier," +  entity + "CharacteristicValueLang.Value(-1)");
		qp.put("pageSize", "900");
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/" + entity + "/" + entity + "CharacteristicValue/byCatalog", qp, null);
			totalSize = response.getInt("totalSize");
			System.out.println("Tutsi " + currentIndex + "/" + totalSize);
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				if("UnidadDeMedidaVolumen".equals(values.getString(0)) || "UnidadDeMedidaLongitud".equals(values.getString(0))) {
					System.out.println(values);
				}
			}
		}while(currentIndex < totalSize);
		System.out.println("Number reached: " + currentIndex);
		currentIndex = 0;
	}

}
