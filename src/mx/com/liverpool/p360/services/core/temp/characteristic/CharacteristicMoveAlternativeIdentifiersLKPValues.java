package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CharacteristicMoveAlternativeIdentifiersLKPValues {

	public static void main(String[] args) throws ServiceUnavailableException {
		String[] extSystems = new String[] {"ECC", "ATG", "S4HANA"};
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		
		RESTWorkshop rwq = new RESTWorkshop();
		rwq.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rwq.addHeader("Authorization", "Basic: " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		rw.putParameter("pageSize", "1200");
		
		for(String extSys : extSystems) {
			System.out.println("Performing: " + extSys);
			rw.putParameter("query",  "not CharacteristicIdentifier.AlternativeIdentifier(" + extSys + ") is empty");
			rw.putParameter("fields", "Characteristic.Identifier,CharacteristicIdentifier.AlternativeIdentifier(" + extSys + ")");
			
			org.json.JSONObject response = null;
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			
			org.json.JSONObject request = new org.json.JSONObject();
			org.json.JSONArray columns = new org.json.JSONArray();
			org.json.JSONArray rowsPayload = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(" + extSys + ")"));
			request.put("columns", columns);
			request.put("rows", rowsPayload);
			int a = 0;
			int b = 0;
			do {
				rw.putParameter("startIndex", String.valueOf(a));
				response = rw.makeRequest("GET", "/list/Characteristic/bySearch");
				if(response != null) {
					b = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						values = rows.getJSONObject(i).getJSONArray("values");
//						System.out.println(rows.getJSONObject(i));
						rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'Characteristics'"))
								.put("values", new org.json.JSONArray().put(values.getString(1))));
						if(rowsPayload.length() == 120) {
							rwq.makeRequest("POST", "/list/LookupValue", qp, request.toString());
							System.out.println(rwq.getRawResponse());
							while(rowsPayload.length() > 0) {
								rowsPayload.remove(0);
							}
						}
					}
					a += response.getInt("pageSize");
				}else {
					System.out.println("Problem: " + rw.getRawResponse());
				}
			}while(a < b);
			a = 0;
			if(rowsPayload.length() > 0) {
				rwq.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				System.out.println(rwq.getRawResponse());
				while(rowsPayload.length() > 0) {
					rowsPayload.remove(0);
				}
			}
		}
	}
}
