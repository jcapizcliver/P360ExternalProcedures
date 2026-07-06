package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ListOfValues {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject responseJ = null;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\Data Dictionary (Entregable) - List of Values (GLOBAL).tsv")))){
			String line = null;
			String[] pieces = null;
			String sep = "\t";
			String delim = "";
			String esc = "\\";
			br.readLine();
			br.readLine();
			int cnt = 0;
			while((line = br.readLine()) != null) {
				cnt++;
				pieces = workshop.parseLine(line, delim, sep, esc);
//				System.out.println(java.util.Arrays.asList(pieces));
				if((cnt <= 3) || !"FIBER_PARTLOV".equals(pieces[0])) {
					continue;
				}
				if("".equals(pieces[1]) && "".equals(pieces[2])) {
					System.out.println(java.util.Arrays.asList(pieces));
					continue;
				}
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ("".equals(pieces[1]) ? pieces[2] : pieces[1]) + "'@'" + pieces[0] +"'" )).put("values", new org.json.JSONArray().put(pieces[2]).put(true)));
				if(rows.length() == 50) {
					responseJ = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject()
							.put("columns",
									new org.json.JSONArray()
										.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
										.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
							)
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
									.put(new org.json.JSONObject().put("LookupValue", "LookupValueLang.Name(es)"))
									.put(new org.json.JSONObject().put("LookupValue", "LookupValue.IsActive"))
						)
						.put("rows", rows).toString());
				System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}


}
