package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaAtributosSuburbia {


	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		
		workshop.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		workshop.addHeader("Authorization" , "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));

		org.json.JSONArray rows = new org.json.JSONArray();

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject responseJ = null;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\tmp\\Mapeo Att STEP VS S4H - Sheet1.tsv")))){
			String line = null;
			String[] pieces = null;
			String sep = "\t";
			String delim = "";
			String esc = "\\";
			br.readLine();
			int cnt = 0;
			while((line = br.readLine()) != null) {
				cnt++;
				pieces = workshop.parseLine(line, delim, sep, esc);
				System.out.println(java.util.Arrays.asList(pieces));
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'Characteristics'" )).put("values", 
						new org.json.JSONArray()
//						.put(pieces[1])
						.put(pieces[2])
						.put(true)
						));
				if(rows.length() == 50) {
					responseJ = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject()
							.put("columns",
									new org.json.JSONArray()
//										.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
										.put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(S4HANA)"))
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
				responseJ = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
//									.put(new org.json.JSONObject().put("identifier",  "LookupValueLang.Name(es)"))
									.put(new org.json.JSONObject().put("identifier",  "LookupValueIdentifier.Code(S4HANA)"))
									.put(new org.json.JSONObject().put("identifier",  "LookupValue.IsActive"))
						)
						.put("rows", rows).toString());
				System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}



}
