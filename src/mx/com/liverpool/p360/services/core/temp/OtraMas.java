package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class OtraMas {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		java.util.LinkedList<String> productos = null;
//		productos = propuestas();
//		productos.forEach(OtraMas::setProcedeNoProcede);
		setProcedeNoProcede("1698767480972246");
	}

	private static java.util.LinkedList<String> propuestas(){
		java.util.LinkedList<String> productos = new java.util.LinkedList<>();

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "Product2G.CurrentStatus = 1022");
		qp.put("pageSize", "900");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		do{
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				productos.addLast(values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return productos;
	}

	private static void setProcedeNoProcede(String externalId) {
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + externalId + "\") = \"" + externalId + "\"");
		org.json.JSONObject response = rw.makeRequest("GET", "/list/Article/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray charValue = null;
		String articleId = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				charValue = values.getJSONArray(1);
				articleId = values.getString(0);
				System.out.println(articleId + " - " + charValue);
				response = rw.makeRequest("PUT", "/object/Article/'" + articleId + "'@'MASTER'", empty, new org.json.JSONObject().put("_characteristicRecords", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", "ProcedeNoProcede"))).put("_recordLang", new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))).put("values", new org.json.JSONArray().put(!"".equals(charValue.getString(0))))))).toString());
				if(response != null) {
					System.out.println("Put ProcedeNoProcede for: " + articleId);
				}else {
					System.out.println("Error while updating \"ProcedeNoProcede\" value. " + rw.getRawResponse());
				}
			}
		}
	}

}
