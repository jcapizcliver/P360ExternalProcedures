package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class HolaMaiLov {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, java.util.Set<String>> gruposSBB = new java.util.TreeMap<>();
		java.util.Set<String> marcas = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\EXPORTMARCASYGRUPOS - Sheet1.tsv")))){
			String line = null;
			String delim = "";
			String sep = "\t";
			String esc = "\\";
			String[] header = workshop.parseLine(br.readLine(), delim, sep, esc);
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				marcas = gruposSBB.get(pieces[1]);
				if(marcas == null) {
					marcas = new java.util.TreeSet<>();
					gruposSBB.put(pieces[1], marcas);
				}
				marcas.add(pieces[2]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		org.json.JSONArray marcasArray = null;
		org.json.JSONObject response = null;
		org.json.JSONArray columns = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BRAND_IDLOV_S4H')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		for(java.util.Map.Entry<String, java.util.Set<String>> entry : gruposSBB.entrySet()) {
			marcasArray = new org.json.JSONArray();
			for(String m : entry.getValue()) {
				marcasArray.put(m);
			}
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'MATKLLOV_S4H'")).put("values", new org.json.JSONArray().put(
					marcasArray
					).put(true)));
//			System.out.println(rows.get(0));
//			System.exit(0);
			if(rows.length() == 10) {
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
				System.out.println( response != null ? response : "ERR: " + workshop.getRawResponse() );
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
			System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : response );
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

}
