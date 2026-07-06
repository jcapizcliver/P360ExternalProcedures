package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CharactLabels {
private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> moneda = new java.util.TreeMap<>();
		org.json.JSONObject responseJ = null;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\tmp\\att confi - Att y nombre.tsv")))){
			String line = null;
			String[] pieces = null;
			String sep = "\t";
			String delim = "\"";
			String esc = "\\";
			br.readLine();
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				moneda.put(pieces[0], pieces[1]);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] +"'" )).put("values", new org.json.JSONArray().put(pieces[1])));
				if(rows.length() == 50) {
					responseJ = workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject()
							.put("columns",
									new org.json.JSONArray()
										.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)")))
							.put("rows", rows).toString());
					System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				responseJ = workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)")))
						.put("rows", rows).toString());
				System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}


}
