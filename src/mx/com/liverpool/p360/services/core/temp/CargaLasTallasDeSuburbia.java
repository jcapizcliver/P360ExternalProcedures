package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaLasTallasDeSuburbia {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\ItemGroupS4HTallas.txt")))){
			String delim = "";
			String sep = "\t";
			String esc = "\\";
			String line = null;
//			String[] header = rw.parseLine(br.readLine(), delim, sep, esc);
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.parseLine(line, delim, sep, esc);
				if("".equals(pieces[0])) {
					continue;
				} else if("SB87405".equals(pieces[0])) {
					System.out.println("Found it: " + java.util.Arrays.asList(pieces));
				}
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'TallaUnicavsTallaS4H'")).put("values", new org.json.JSONArray().put(pieces[1])));
				if(rows.length() == 200) {
					response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
					System.out.println( response != null ? response : rw.getRawResponse() );
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
				System.out.println( response != null ? response : rw.getRawResponse() );
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {

		}

	}

}
