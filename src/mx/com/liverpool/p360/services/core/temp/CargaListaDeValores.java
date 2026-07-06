package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaListaDeValores {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> moneda = new java.util.TreeMap<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\MonedaLOV.csv")))){
			String line = null;
			String[] pieces = null;
			String sep = "\t";
			String delim = "\"";
			String esc = "\\";
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				moneda.put(pieces[0], pieces[1]);
				System.out.println(java.util.Arrays.asList(pieces));
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[1] + "'@'CurrencyLOV'")).put("values", new org.json.JSONArray().put(pieces[0])));
			}
			System.out.println(workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject()
					.put("columns",
							new org.json.JSONArray()
								.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")))
					.put("rows", rows).toString()));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

}
