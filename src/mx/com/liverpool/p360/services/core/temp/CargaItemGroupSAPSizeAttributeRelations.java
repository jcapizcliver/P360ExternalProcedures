package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaItemGroupSAPSizeAttributeRelations {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		java.util.LinkedList<String[]> pairs = new java.util.LinkedList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\tmp\\EXPORTTALLASTAMAÑOSQA - Sheet1.tsv")))){
			String line = null;
			String delim = "\"";
			String sep = "\t";
			String esc = "\\";
			String[] header = rw.parseLine(br.readLine(), delim, sep, esc);
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.parseLine(line, delim, sep, esc);
				pairs.addLast(new String[] {pieces[4], pieces[7]});
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		pairs.forEach(pair->System.out.println(pair[0] + " - " + pair[1]));
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Active"));
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		for(String[] pair : pairs) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pair[0] + "'@'ItemGroupSAPSizeAttribute'")).put("values", new org.json.JSONArray().put(pair[1]).put(true)));
			if(rows.length() == 250) {
				response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
				if(response != null) {

				}else {
					System.out.println(rw.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

}
