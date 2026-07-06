package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class LoadTMU01 {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject responseJ = null;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\EXPORTTMU01 - Sheet1.tsv")))){
			String line = null;
			String[] pieces = null;
			String sep = "\t";
			String delim = "";
			String esc = "\\";
			br.readLine();
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] +"'@'TMU01LOV'" )).put("values", new org.json.JSONArray().put(pieces[1]).put(true)));
				if(rows.length() == 50) {
					responseJ = workshop.makeRequest("POST", "/list/Lookup", qp, new org.json.JSONObject()
							.put("columns",
									new org.json.JSONArray()
										.put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)")))
							.put("rows", rows).toString());
					System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				responseJ = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
									.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive")))
						.put("rows", rows).toString());
				System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}


}
