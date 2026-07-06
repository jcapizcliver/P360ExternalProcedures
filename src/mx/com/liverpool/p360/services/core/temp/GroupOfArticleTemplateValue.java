package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class GroupOfArticleTemplateValue {


	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "24004");
		qp.put("metaData", "true");
		qp.put("query", "GroupOfArticleTemplateValue.Supplier = \"95150\"");
//		qp.put("query", "GroupOfArticleTemplateValue.Supplier = \"111838\"");
		qp.put("fields",  "GroupOfArticleTemplateValue.Template->LookupValueLang.Name(es),"
						+ "GroupOfArticleTemplateValue.Identifier,"
						+ "GroupOfArticleTemplateValue.Dictionary,"
						+ "GroupOfArticleTemplateValue.Supplier,"
						+ "GroupOfArticleTemplateValue.GroupOfArticle,"
						+ "GroupOfArticleTemplateValue.System,"
						+ "GroupOfArticleTemplateValue.Brand,"
						+ "GroupOfArticleTemplateValue.Template->LookupValue.Code");

		int totalSize = 0;
		int currentIndex = 0;

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Set<String> gruposDeArticulo = new java.util.TreeSet<>();
		java.util.Set<String> marcas = new java.util.TreeSet<>();
		java.util.Set<String> negocios = new java.util.TreeSet<>();

		java.util.Map<String, java.util.Set<String>> marcasPorNegocio = new java.util.TreeMap<>();
		java.util.Set<String> aux = null;

		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONObject payload = new org.json.JSONObject();


		do{
			response = workshop.makeRequest("GET", "/list/GroupOfArticleTemplateValue/bySearch", qp, null);
			System.out.println(response);
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				gruposDeArticulo.add(values.getString(4));
				marcas.add(values.getJSONArray(6).toString());
				for(int j=0; j<values.getJSONArray(5).length(); j++) {
					aux = marcasPorNegocio.get(values.getJSONArray(5).getString(j));
					if(aux == null) {
						aux = new java.util.TreeSet<>();
						marcasPorNegocio.put(values.getJSONArray(5).getString(j), aux);
					}
					aux.add(values.getJSONArray(5).getString(j));
				}
				System.out.println(values.get(7));
				System.out.println(values.getString(0) + "_" + values.getString(1) + "_" + values.getString(4) + "_" + values.getJSONArray(6) + "___" + values.getJSONArray(5));
//				rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", rows.getJSONObject(i).getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put(new org.json.JSONArray().put("0924").put("0925").put("0659").put("2033").put("1242").put("0445"))));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

//		System.out.println( response = workshop.makeRequest("POST", "/list/GroupOfArticleTemplateValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Brand"))).put("rows", rowsPayload).toString()) );

		org.json.JSONObject pubSubPayload = new org.json.JSONObject();
		org.json.JSONArray marcasArray = new org.json.JSONArray();
		for(java.util.Map.Entry<String, java.util.Set<String>> entry : marcasPorNegocio.entrySet()) {
			for(String marca : entry.getValue()) {
				marcasArray.put(marca);
			}
			pubSubPayload.put(entry.getKey(), marcasArray);
		}

		System.out.println("*******");
		gruposDeArticulo.forEach(System.out::println);
		System.out.println("*******");
		marcas.forEach(System.out::println);

	}
}
