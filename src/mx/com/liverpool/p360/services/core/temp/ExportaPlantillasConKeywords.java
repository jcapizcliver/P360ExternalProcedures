package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ExportaPlantillasConKeywords {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", "PrimaryProductTaxonomy");
		qp.put("query",  "StructureGroup.Identifier wildcard \"EU4-%\"");
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Synonym(es),StructureGroupLang.Name(es),StructureGroupLang.Description(es)");
		qp.put("pageSize", "900");
		qp.put("startIndex", "0");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		String[] pieces = new String[6];
		System.out.println("Loading template-group of articles.");
		java.util.Map<String, org.json.JSONArray> templateGroupOrArticles = getTemplateGroupOfArticle();
		System.out.println("Operating over " + templateGroupOrArticles.size());
		org.json.JSONArray groupOfArticles = null;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\plantillas.csv")))){
			pw.println( workshop.serializeChunk( new String[] {"#Template", "GroupOfArticles", "Keywords", "Name", "Description", "Products"} ) );
			do {
				qp.put("startIndex", String.valueOf(currentIndex));
				response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					pieces[0] = values.getString(0);
					pieces[1] = (groupOfArticles = templateGroupOrArticles.get(pieces[0])) != null ? workshop.joinJSONArray(groupOfArticles, ";") : "";
					pieces[2] = workshop.joinJSONArray( curacionDeKeywords( values.getJSONArray(1) ), ";") ;
					pieces[3] = values.getString(2).replaceAll(" ?\\(.+\\)", "").trim();
					pieces[4] = values.getString(3).replaceAll("(?<!\r)(\n)$", "\\\\n");
					pieces[5] = "";
					if(String.valueOf( values.get(1) ).contains("Máscara de privación sensorial")) {
						System.out.println(values);
					}
					pw.println( workshop.serializeChunk(pieces) );
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private static org.json.JSONArray curacionDeKeywords(org.json.JSONArray keywords){
		org.json.JSONArray nk = new org.json.JSONArray();
		String[] pieces = null;
		String val = null;
		for(int i=0; i<keywords.length(); i++) {
			val = keywords.getString(i);
			if(val.contains("\n")) {
				pieces = val.split("\\n");
				for (String element : pieces) {
					nk.put(element);
				}
			}else {
				nk.put(val);
			}
			if(val.contains( "Antifaz de sumisión" )) {
				System.out.println("\t" + val);
				System.out.println("\t" + nk);
				System.out.println("\t" + java.util.Arrays.asList(pieces));
			}
		}
		return nk;
	}

	private static java.util.Map<String, org.json.JSONArray> getTemplateGroupOfArticle(){
		java.util.Map<String, org.json.JSONArray> arrays = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "24004");
		qp.put("query",  "not GroupOfArticleTemplateValue.Template is empty and not GroupOfArticleTemplateValue.GroupOfArticle is empty");
		qp.put("fields", "GroupOfArticleTemplateValue.Template->LookupValue.Code,GroupOfArticleTemplateValue.GroupOfArticle");
		qp.put("pageSize", "900");
		qp.put("orderBy", "0-ASC");
		qp.put("startIndex", "0");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		String template = null;
		String groupOfArticle = null;
		String prevTemplate = null;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/GroupOfArticleTemplateValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			org.json.JSONArray groupOfArticles = new org.json.JSONArray();
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				template = values.getString(0);
				groupOfArticle = values.getString(1);
				if(prevTemplate != null && !prevTemplate.equals(template)) {
					arrays.put(prevTemplate, groupOfArticles);
					groupOfArticles = new org.json.JSONArray();
				}
				groupOfArticles.put(groupOfArticle);
				prevTemplate = template;
			}
			if(groupOfArticles.length() > 0) {
				arrays.put(prevTemplate, groupOfArticles);
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return arrays;
	}

}
