package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class RevisaItemGroup {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Set<String> actualCodes = new java.util.TreeSet<>();
		java.util.Set<String> actualCodesInHierarchy = new java.util.TreeSet<>();
		java.util.Set<String> codesInLibroDeProducto = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("lookup", "MATKLLOV");
		qp.put("metaData", "true");
		qp.put("pageSize", "500");
		do{
			qp.put("startIndex", "" + currentIndex);
			response = workshop.makeRequest("GET", "/list/LookupValue/byLookup", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				actualCodes.add(values.getString(0));
				if("629015190".equals(values.getString(0))) {
					System.out.println("JUMP");
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		qp.remove("lookup");
		qp.put("structure", "CommercialECC");
		qp.put("fields", "StructureGroup.Identifier");
		qp.put("query", "StructureGroup.Identifier wildcard \"%-L4ECC\"");
		do{
			qp.put("startIndex", "" + currentIndex);
			response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				actualCodesInHierarchy.add(values.getString(0).replaceAll("-.+", ""));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		qp.remove("structure");
		qp.put("dictionaryProxy", "24004");
		qp.put("fields", "GroupOfArticleTemplateValue.GroupOfArticle");
		qp.put("query", "not GroupOfArticleTemplateValue.GroupOfArticle is empty");
		do{
			qp.put("startIndex", "" + currentIndex);
			response = workshop.makeRequest("GET", "/list/GroupOfArticleTemplateValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				codesInLibroDeProducto.add(values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		System.out.println("Actual Codes: " + actualCodes.size());
		System.out.println("Actual Codes in Hierarchy: " + actualCodesInHierarchy.size());
		System.out.println("Codes in libro de producto: " + codesInLibroDeProducto.size());

		java.util.Set<String> news = new java.util.TreeSet<>();

		actualCodesInHierarchy.forEach(code-> { if(!actualCodes.contains(code)) {news.add(code);} } );
		codesInLibroDeProducto.forEach(code-> { if(!actualCodes.contains(code)) {news.add(code);} } );

		System.out.println("New codes: " + news.size());
		System.out.println("New codes: " + news);

	}
}
