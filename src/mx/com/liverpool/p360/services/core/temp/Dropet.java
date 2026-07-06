package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class Dropet {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String,String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "24004");
		qp.put("query", "GroupOfArticleTemplateValue.Supplier = \"20325\"");
		qp.put("fields", "GroupOfArticleTemplateValue.Template->LookupValueLang.Name(es),GroupOfArticleTemplateValue.Identifier,GroupOfArticleTemplateValue.Dictionary,GroupOfArticleTemplateValue.Supplier,GroupOfArticleTemplateValue.GroupOfArticle,GroupOfArticleTemplateValue.GroupOfArticleName,GroupOfArticleTemplateValue.System,GroupOfArticleTemplateValue.Brand,GroupOfArticleTemplateValue.Template");
		qp.put("pageSize", "900");

		java.util.Set<String> templates = new java.util.TreeSet<>();
		java.util.Set<String> groupOfArticles = new java.util.TreeSet<>();

		do {
			response = workshop.makeRequest("GET", "/list/GroupOfArticleTemplateValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				if(!"".equals(values.getString(0))) {
					templates.add(values.getString(1).replaceAll(".+(?=EU4)", "") + " - " + values.getString(0));
				}
				if(!"".equals(values.getString(5))) {
					groupOfArticles.add(values.getString(4) + " - " + values.getString(5));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		groupOfArticles.forEach(System.out::println);
		System.out.println("*****");
		templates.forEach(System.out::println);
	}
}
