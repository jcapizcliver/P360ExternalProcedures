package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class IncorporaGruposATGAPropósitos {

	
	public static void main(String[] args) throws ServiceUnavailableException {
		writeCodeAsPurposes(getATGGroups());
	}
	
	private static void writeCodeAsPurposes(String[][] codes) {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		for(int i=0; i<codes.length; i++) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + codes[i][0] + "'@'CharacteristicPurposes'")).put("values", new org.json.JSONArray().put(codes[i][1]).put(true)));
			if(rows.length() == 200) {
				response = rw.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString());
				if(response == null) {
					System.out.println("ERR: " + rw.getRawResponse());
				}else {
					System.out.println(response.toString());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString());
			if(response == null) {
				System.out.println("ERR: " + rw.getRawResponse());
			}else {
				System.out.println(response.toString());
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private static String[][] getATGGroups() throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.LinkedList<String[]> codes = new java.util.LinkedList<>();
		String[] par = null;
		rw.putParameter("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		rw.putParameter("lookup", "AgrupacionesAtributosATG");
		rw.putParameter("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/byLookup");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					par = new String[2];
					par[0] = values.getString(0);
					par[1] = values.getString(1);
					codes.addLast(par);
				}
			}else {
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return codes.toArray(new String[][] {});
	}
	
}
