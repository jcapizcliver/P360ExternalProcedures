package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class Temp {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		String[] elements = ("A8	1\r\n"
				+ "A9	1\r\n"
				+ "BT	0\r\n"
				+ "HB	1\r\n"
				+ "HL	7\r\n"
				+ "IC	1\r\n"
				+ "MK	1\r\n"
				+ "SL	1\r\n"
				+ "T8	1\r\n"
				+ "T9	1").split("\\r\\n");
		String[] pair = null;
		org.json.JSONArray rws = new org.json.JSONArray();
		org.json.JSONObject payload = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Value"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		payload.put("columns", columns);
		for (String element : elements) {
			pair = element.split("\t");
			rws.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pair[0] + "_" + pair[1] + "'@'BEHVO_LookupTable'")).put("values", new org.json.JSONArray().put(pair[0]).put(pair[1])));
		}
		payload.put("rows", rws);
		java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
		System.out.println("____--____" + workshop.makeRequest("POST", "/list/StandardizationValue", qp1, payload.toString()));

		System.exit(0);

		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("query", "ProductReference.ReferencedSupplierAid(\"1698767480920006\") equals \"1698767480920006\"");
		qp0.put("fields", "Article.SupplierAID,ProductReference.ReferencedSupplierAid(\"1698767480920006\"),ArticleCharacteristicValueLang.Value(ProductImageDetail,\"0000.0000.RK\",\"0000.0000.RK\",ProductImageDetail_URL,-1),ArticleCharacteristicValueLang.Value(ProductImage,\"0000.0000.RK\",\"0000.0000.RK\",ProductImage_URL,-1)");
		System.out.println( workshop.makeRequest("GET", "/list/Article/bySearch", qp0, null) );
		System.exit(0);
		refresh();
		System.exit(0);

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

//		System.out.println( workshop.makeRequest("POST", "/list/StandardizationDictionary", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray()).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray()))).toString()) );
//		System.out.println( workshop.makeRequest("POST", "/list/StandardizationDictionary", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationDictionary.Identifier"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_OLD")))).toString()) );

		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
//		RESTWorkshop rw = new RESTWorkshop();
//		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
//		System.out.println( rw.makeRequest("DELETE", "/list/StandardizationValue/bySearch", qp, null) );

		qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.CreationType->LookupValue.Code,StandardizationValue.Property->LookupValue.Code,StandardizationValue.PropertyValue");
		response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
		rows = response.getJSONArray("rows");
		for(int i=0; i<rows.length(); i++) {
			System.out.println(rows.getJSONObject(i).getJSONArray("values"));
		}
	}


	private static void refresh(){

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "not Characteristic.Lookup is empty");

		int ci = 0;
		int tz = 0;

		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("baseDirectory", "/u01/stage/cache/templates");

		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("http://172.18.237.165:8080/process-engine");
		rw.getRc().getHeader().remove("Authorization");

		do{
			qp.put("startIndex", String.valueOf( ci ));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			tz = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				ci++;
				values = rows.getJSONObject(i).getJSONArray("values");
				qp0.put("characteristic", values.getString(0));
				System.out.println( rw.makeRequest("GET", "/public/rt/RefreshLookupValues", qp0, null) );
			}
		}while(ci < tz);
		String url = "http://172.18.237.165:8080/process-engine/public/rt/RefreshLookupValues?characteristic=Currency&baseDirectory=/u01/stage/cache/templates";
	}

}
