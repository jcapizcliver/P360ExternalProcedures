package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class TratamientoColoresYSusCodigos {

	private static RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		java.util.Map<String, String> c100lov = new java.util.TreeMap<>();
		java.util.Map<String, String> sb_coloreslov = new java.util.TreeMap<>();
		java.util.Set<String> codigos = new java.util.TreeSet<>();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int totalSize = 0;
		int currentIndex = 0;


		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "C100LOV");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("pageSize", "900");

		do{
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				if(c100lov.containsKey(values.getString(1))) {
					if(c100lov.get(values.getString(1)).length() < values.getString(0).length()) {
						c100lov.put(values.getString(1), values.getString(0));
					}
				} else {
					c100lov.put(values.getString(1), values.getString(0));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		qp.put("lookup", "SB_COLORESLOV");
		do{
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				if(sb_coloreslov.containsKey(values.getString(1))) {
					if(sb_coloreslov.get(values.getString(1)).length() < values.getString(0).length()) {
						sb_coloreslov.put(values.getString(1), values.getString(0));
					}
				} else {
					sb_coloreslov.put(values.getString(1), values.getString(0));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\colors_by_template_hex.csv")))){
			String line = null;
			String[] pieces = null;
			String delim = "\"";
			String sep = ";";
			String esc = "\\";
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				codigos.add(pieces[6] + "_" + pieces[7]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

		java.util.Map<String, String> coincidenciasC100LOV = new java.util.TreeMap<>();
		java.util.Map<String, String> coincidenciasSB_COLORESLOV = new java.util.TreeMap<>();

		String[] pair = null;
		String code = null;
		for(String codigo : codigos) {
			pair = codigo.split("_");
			code = c100lov.get(pair[0]);
			if(code != null) {
				coincidenciasC100LOV.put(code, pair[1]);
			}
			code = sb_coloreslov.get(pair[0]);
			if(code != null) {
				coincidenciasSB_COLORESLOV.put(code, pair[1]);
			}
		}

		System.out.println("Coincidencias C100LOV");
		coincidenciasC100LOV.forEach((k,v)->System.out.println(k + "-" + v));
		System.out.println("\n\nCoincidencias SB_COLORESLOV");
		coincidenciasSB_COLORESLOV.forEach((k,v)->System.out.println(k + "-" + v));

		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(ATG)"));
		rows = new org.json.JSONArray();

		System.out.println("Uploading data...");

		for(java.util.Map.Entry<String, String> entry : coincidenciasC100LOV.entrySet()) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'SB_COLORESLOV'")).put("values", new org.json.JSONArray().put(entry.getValue())));
			if(rows.length() == 300) {
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
				System.out.println(response);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
			System.out.println(response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}
}
