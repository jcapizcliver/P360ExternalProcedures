package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class TratamientoTallaNormalizada {

	private static RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int totalSize = 0;
		int currentIndex = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		java.util.Map<String, String> tallasNormalizadas = new java.util.TreeMap<>();

		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));

		org.json.JSONArray payloadRows = new org.json.JSONArray();

		java.util.Map<String, String> elp = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Set<String>> elp0 = new java.util.TreeMap<>();
		java.util.Set<String> el = null;
		String aux = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\aeropostale_std.csv")))){
			String line = null;
			String delim = "\"";
			String sep = "\t";
			String esc = "\\";
			String[] pieces = null;
			String key = null;
			br.readLine();
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				if(!"".equals(pieces[4])) {
					aux = elp.get(pieces[3]);
					if(aux != null) {
						if(!aux.equals(pieces[4])) {
							System.out.println("Conflict. (" + elp0.get(pieces[3]) + ") " + aux + " != " + pieces[4] + " (" + pieces[1] + ")");
						}else {
							System.out.println("Safe");
						}
					}else {
						elp.put(pieces[3], pieces[4]);
					}
					el = elp0.get(pieces[3]);
					if(el == null) {
						el = new java.util.TreeSet<>();
					}
					el.add(pieces[1]);
					tallasNormalizadas.put(key = (pieces[0] + "<::>" + pieces[3] ), pieces[4]);
					payloadRows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + key + "'@'TallaNormalizadaAeropostale'")).put("values", new org.json.JSONArray().put(pieces[4])));
					if(payloadRows.length() == 200) {
						response = workshop.makeRequest("POST", "/list/StandardizationValue", qp, new org.json.JSONObject().put("rows", payloadRows).put("columns", columns).toString());
						while(payloadRows.length() > 0) {
							payloadRows.remove(0);
						}
					}
				}
			}
//			System.out.println(elp);
			if(payloadRows.length() > 0) {
				workshop.makeRequest("POST", "/list/StandardizationValue", qp, new org.json.JSONObject().put("rows", payloadRows).put("columns", columns).toString());
				while(payloadRows.length() > 0) {
					payloadRows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

}
