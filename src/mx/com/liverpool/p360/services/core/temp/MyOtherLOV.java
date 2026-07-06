package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class MyOtherLOV {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		/*
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject response = null;
		org.json.JSONArray columns = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\EXPORTSBBGRUPOS - Sheet1.tsv")))){
			String line = null;
			String delim = "";
			String sep = "\t";
			String esc = "\\";
			String[] header = workshop.parseLine(br.readLine(), delim, sep, esc);
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'MATKLLOV_S4H'")).put("values", new org.json.JSONArray().put(pieces[1]).put(true)));
				if(rows.length() == 10) {
					response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
					System.out.println( response != null ? response : "ERR: " + workshop.getRawResponse() );
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
			System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : response );
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		System.exit(0);
		*/
		echaleOtrosNeims();
	}

	private static void echaleOtrosNeims() {
		System.out.println("While I perform");
		java.util.Map<String, String> loQueHay = hola();
		System.out.println("Ahora los neims...");
		java.util.LinkedList<String> sinNeims = losSinNeim();
		String aux = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		org.json.JSONObject response = null;
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("columns", columns);
		request.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		System.out.println("... Performing...");
		for(String n : sinNeims) {
			aux = loQueHay.get(n);
			if(aux != null) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + n + "'@'MATKLLOV_S4H'")).put("values", new org.json.JSONArray().put(aux).put(true)));
//				System.out.println(rows.get(0));
//				System.exit(0);
				if(rows.length() == 300) {
					response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
					System.out.println(response == null ? workshop.getRawResponse() : response);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
			System.out.println(response == null ? workshop.getRawResponse() : response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

	private static java.util.Map<String, String> hola(){
		java.util.Map<String, String> grupos = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", "CommercialS4H");
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es)");
		qp.put("query", "StructureGroup.Identifier wildcard \"%-L4%\"");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					grupos.put(values.getString(0).replaceAll("-.+", ""), values.getString(1));
				}
			}else {
				System.out.println("Lep: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return grupos;
	}

	private static java.util.LinkedList<String> losSinNeim(){
		java.util.LinkedList<String> grupos = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "MATKLLOV_S4H");
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueLang.Name(es) is empty");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					grupos.addLast(values.getString(0));
				}
			}else {
				System.out.println("Lep: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return grupos;
	}

}
