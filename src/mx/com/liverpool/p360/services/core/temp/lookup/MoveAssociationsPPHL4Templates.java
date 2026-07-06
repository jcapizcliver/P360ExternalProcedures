package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class MoveAssociationsPPHL4Templates {

	
	public static void main(String[] args) throws ServiceUnavailableException {
//		String baseLookup = "PPH_L4_Templates";
//		String baseLookup = "Party";
//		String baseLookup = "MATKLLOV";
		String baseLookup = "Characteristics";
		java.util.Map<String, String> data = collectLkpContent(baseLookup);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rwq = new RESTWorkshop();
		RESTWorkshop rw  = new RESTWorkshop();
		rwq.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rw.putParameter("lookup", baseLookup);
		rw.putParameter("fields", "LookupValueReference.Lookup->Lookup.Identifier"
				+ ",LookupValueReference.LookupValues->LookupValue.Code"
				+ ",LookupValueReference.LookupValues->LookupValueLang.Name(es)"
				+ ",LookupValueReference.InclusionMode");
		rw.putParameter("pageSize", "1200");
		int a = 0;
		int b = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		org.json.JSONArray ids = null;
		do {
			rw.putParameter("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/LookupValue/LookupValueReference/byLookup");
			if(response != null) {
				rows = response.getJSONArray("rows");
				a += response.getInt("pageSize");
				for(int i=0; i<rows.length(); i++){
					values = rows.getJSONObject(i).getJSONArray("values");
					ids =  values.getJSONArray(1);
					rowsPayload.put(
							new org.json.JSONObject()
								.put("object", new org.json.JSONObject().put("id", "'" + data.get(rows.getJSONObject(i).getJSONObject("object").getString("id")) + "'@'" + baseLookup + "'"))
								.put("values", new org.json.JSONArray().put(ids))
								
								.put("qualification", new org.json.JSONObject().put("refLookup", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")))
						);
					if(rowsPayload.length() == 100) {
						rwq.makeRequest("POST", "/list/LookupValue/LookupValueReference", qp, request.toString());
						System.out.println(rwq.getRawResponse());
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
			}else {
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
		rwq.makeRequest("POST", "/list/LookupValue/LookupValueReference", qp, request.toString());
		System.out.println(rwq.getRawResponse());
		while(rowsPayload.length() > 0) {
			rowsPayload.remove(0);
		}
	}
	
	private static java.util.Map<String, String> collectLkpContent(String lookup) throws ServiceUnavailableException{
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		RESTWorkshop rw  = new RESTWorkshop();
		rw.putParameter("lookup", lookup);
		rw.putParameter("fields", "LookupValue.Code");
		rw.putParameter("pageSize", "1200");
		int a = 0;
		int b = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		do {
			rw.putParameter("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/LookupValue/byLookup");
			if(response != null) {
				rows = response.getJSONArray("rows");
				a += response.getInt("pageSize");
				for(int i=0; i<rows.length(); i++){
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(rows.getJSONObject(i).getJSONObject("object").getString("id"), values.getString(0));
				}
			}else {
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
		return data;
	}
	
}
