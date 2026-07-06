package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class TamanoUnicoEtiquetaACode {

	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	private static final java.util.Map<String, java.util.Set<String>> tallas = new java.util.TreeMap<>();


	public static void main(String[] args) {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Map<String, String> tamañoÚnico = new java.util.TreeMap<>();
		java.util.Map<String, String> únicoTamaño = new java.util.TreeMap<>();

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "TamanoUnicoLOV");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("pageSize", "1000");

		int totalSize = 0;
		int currentIndex = 0;

		java.util.LinkedList<org.json.JSONArray> valuesList = new java.util.LinkedList<>();

		do {
			qp.put("startIndex", String.valueOf( currentIndex ) );

			response = workshop.makeRequest("GET", "/list/LookupValue/byLookup", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				tamañoÚnico.put(values.getString(0), values.getString(1));
				únicoTamaño.put(values.getString(1), values.getString(0));
				valuesList.addLast(new org.json.JSONArray().put(rows.getJSONObject(i).getJSONObject("object").getString("id")).put( values.getString(1)));
			}

		}while(currentIndex < totalSize);
		currentIndex = 0;

		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.Code"));
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rowsPayload);

		org.json.JSONObject resp = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();

		for(org.json.JSONArray elvalue : valuesList ) {
			rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", elvalue.getString(0))).put("values", new org.json.JSONArray().put(elvalue.getString(1))));
			if(rowsPayload.length() == 200) {
				resp = workshop.makeRequest("POST", "/list/LookupValue", empty, request.toString());
				if(resp != null ) {
					while(rowsPayload.length() > 0) {
						rowsPayload.remove(0);
					}
				}else {
					System.out.println(workshop.getRawResponse());
				}
			}
		}
		if(rowsPayload.length() > 0) {
			if(rowsPayload.length() == 200) {
				resp = workshop.makeRequest("POST", "/list/LookupValue", empty, request.toString());
				if(resp != null ) {
					while(rowsPayload.length() > 0) {
						rowsPayload.remove(0);
					}
				}else {
					System.out.println(workshop.getRawResponse());
				}
			}
		}
	}
}
