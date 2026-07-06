package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class P360QAWorkshop {

	public static void main(String[] args) throws ServiceUnavailableException {
		RESTWorkshop wq = new RESTWorkshop();
		wq.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		RESTWorkshop wd = new RESTWorkshop();

		String rawResponse = null;
		java.util.Map<String, String> headersDelete = new java.util.TreeMap<>();
		headersDelete.put("Content-Type", "application/x-www-form-urlencoded");
		headersDelete.put("Accept", "application/json");
		headersDelete.put("Authorization", wq.getRc().getHeader().get("Authorization"));

		int currentIndex = 0;
		int totalSize = 0;

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;

		java.util.LinkedList<String> lookupIds = new java.util.LinkedList<>();
		try {
			do{
				rawResponse = wq.makeRequest("GET", "/list/Lookup/bySearch?fields=Lookup.Identifier,LookupLang.Name(es),Lookup.DataType&query=" + java.net.URLEncoder.encode("not Lookup.Identifier is empty", "UTF-8") + "&pageSize=3000&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					lookupIds.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
					currentIndex++;
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
			org.json.JSONArray newRows = new org.json.JSONArray();
			org.json.JSONArray values = null;
			for(String id : lookupIds) {
				if("GroupCharacteristicMetadataExtensionProperty".equals(id)) {
					System.out.println("Inserting: " + id);
					rawResponse = wq.makeRequest("DELETE", "/list/LookupValue/byLookup?lookup=" + java.net.URLEncoder.encode(id, "UTF-8"), null, headersDelete);
					System.out.println(id + " -->" + rawResponse + "<--");

					do {
						try {
							rawResponse = wd.makeRequest("GET", "/list/LookupValue/byLookup?fields=LookupValue.Code,LookupValueLang.Name(es),LookupValueLang.Name(en),LookupValueLang.Description(es),LookupValueLang.Description(en),LookupValue.IsActive&lookup=" + java.net.URLEncoder.encode(id, "UTF-8") + "&pageSize=100&startIndex=" + currentIndex, null);
							response = new org.json.JSONObject(rawResponse);
							totalSize = response.getInt("totalSize");
							rows = response.getJSONArray("rows");
							System.out.println("Got a chunk of rows: " + rows.length() + " of " + totalSize + " for " + id);
							for(int i=0; i<rows.length(); i++) {
								currentIndex++;
								values = rows.getJSONObject(i).getJSONArray("values");
								if(!"".equals(values.getString(0))) {
									newRows.put( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'" + id + "'")).put("values", values) );
								} else {
									System.out.println("\t" + values);
								}
							}
							rawResponse = wq.makeRequest("POST", "/list/LookupValue/", new org.json.JSONObject()
									.put("rows", newRows)
									.put("columns",
											new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "LookupValue.Code"))
											.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
											.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(en)"))
											.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Description(es)"))
											.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Description(en)"))
											.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
											).toString());
							response = new org.json.JSONObject(rawResponse);
							System.out.println(response.remove("counters"));
							while(newRows.length() > 0) {
								newRows.remove(0);
							}
						}catch(org.json.JSONException e) {
							System.out.println("ERROR: " + rawResponse);
							e.printStackTrace();
							totalSize = 0;
							currentIndex = 0;
						}
					}while(currentIndex < totalSize);
					currentIndex = 0;
				}
			}
		} catch (org.json.JSONException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}

	}
}
