package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class AhoraLasDeSBB {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('AttributeGroup')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\Mapeo Att STEP VS S4H - Sheet1.tsv")))){
			String delim = "";
			String sep = "\t";
			String esc = "\\";
			String line = null;
			String[] header = rw.parseLine(br.readLine(), delim, sep, esc);
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.parseLine(line, delim, sep, esc);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'Characteristics'")).put("values", new org.json.JSONArray().put(pieces[1]).put(new org.json.JSONArray().put("CategorySpecificAttributesS4H")).put(true)));
				if(rows.length() == 200) {
					response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
					System.out.println( response != null ? response : rw.getRawResponse() );
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				System.out.println( response != null ? response : rw.getRawResponse() );
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {

		}

	}

}
