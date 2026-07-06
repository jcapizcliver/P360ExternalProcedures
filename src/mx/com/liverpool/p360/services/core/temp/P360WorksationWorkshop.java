package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class P360WorksationWorkshop {


	public static void main(String[] args) throws ServiceUnavailableException {
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.setBaseUrl("http://192.168.68.60:1512/rest/V2.0");
		workshop.getRc().getHeader().put("Authorization", java.util.Base64.getEncoder().encodeToString("jcapiz:algolindo".getBytes()));

		String rawResponse = null;
		java.util.Map<String, String> headersDelete = new java.util.TreeMap<>();
		headersDelete.put("Content-Type", "application/x-www-form-urlencoded");
		headersDelete.put("Accept", "application/json");
		headersDelete.put("Authorization", workshop.getRc().getHeader().get("Authorization"));

		java.util.LinkedList<String> objectsToDeactivate = new java.util.LinkedList<>();
		int currentIndex = 0;
		int totalSize = 0;
		int batchCount = 0;

		StringBuilder sb = new StringBuilder();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;

		try {
			rawResponse = workshop.makeRequest("DELETE", "/list/Article/byCatalog", null, headersDelete);
			System.out.println(rawResponse);
			rawResponse = workshop.makeRequest("DELETE", "/list/Product2G/byCatalog", null, headersDelete);
			System.out.println(rawResponse);
			rawResponse = workshop.makeRequest("DELETE", "/list/Lookup/bySearch?query=" + java.net.URLEncoder.encode("not Lookup.Identifier is empty", "UTF-8"), null, headersDelete);
			System.out.println(rawResponse);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}

		System.exit(0);

		do {
			objectsToDeactivate.clear();
			try {
				do {
					rawResponse = workshop.makeRequest("GET", "/list/Characteristic/bySearch?fields=" + java.net.URLEncoder.encode("Characteristic.Identifier,Characteristic.ParentCharacteristic->Characteristic.Identifier,Characteristic.IsActive", "UTF-8") + "&pageSize=2500&query=" + java.net.URLEncoder.encode("(not Characteristic.ParentCharacteristic is empty) and Characteristic.IsActive equals true", "UTF-8"), null) + "&startIndex=" + currentIndex;
					response = new org.json.JSONObject(rawResponse);
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						objectsToDeactivate.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
						currentIndex++;
					}
				}while(currentIndex < totalSize);
				currentIndex = 0;
			} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
			if(objectsToDeactivate.isEmpty()) {
				System.out.println("Tampere");
				try {
					do {
						rawResponse = workshop.makeRequest("GET", "/list/Characteristic/bySearch?pageSize=2500&fields=Characteristic.Identifier&query=" + java.net.URLEncoder.encode("(not Characteristic.ParentCharacteristic is empty) and Characteristic.IsActive equals false", "UTF-8"), null) + "&startIndex=" + currentIndex;
						response = new org.json.JSONObject(rawResponse);
						totalSize = response.getInt("totalSize");
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							objectsToDeactivate.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
							currentIndex++;
						}
					}while(currentIndex < totalSize);
					currentIndex = 0;
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					e.printStackTrace();
				}
			}
			if(objectsToDeactivate.isEmpty()) {
				try {
					do {
						rawResponse = workshop.makeRequest("GET", "/list/Characteristic/bySearch?fields=Characteristic.Identifier&pageSize=2500&query=" + java.net.URLEncoder.encode("Characteristic.IsActive equals true", "UTF-8"), null) + "&startIndex=" + currentIndex;
						response = new org.json.JSONObject(rawResponse);
						totalSize = response.getInt("totalSize");
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							objectsToDeactivate.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
							currentIndex++;
						}
					}while(currentIndex < totalSize);
					currentIndex = 0;
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					e.printStackTrace();
				}
			}
			if(objectsToDeactivate.isEmpty()) {
				System.out.println("Tampere");
				try {
					do {
						rawResponse = workshop.makeRequest("GET", "/list/Characteristic/bySearch?fields=Characteristic.Identifier&pageSize=2500&query=" + java.net.URLEncoder.encode("Characteristic.IsActive equals false", "UTF-8"), null) + "&startIndex=" + currentIndex;
						response = new org.json.JSONObject(rawResponse);
						totalSize = response.getInt("totalSize");
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							objectsToDeactivate.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
							currentIndex++;
						}
					}while(currentIndex < totalSize);
					currentIndex = 0;
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					e.printStackTrace();
				}
			}else {
				rows = new org.json.JSONArray();
				for(String currentId : objectsToDeactivate) {
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + currentId + "'@'MASTER'")).put("values", new org.json.JSONArray().put(false)));
					batchCount++;
					if(batchCount % 60 == 0) {
						System.out.println("Deactivating characteristic batch...");
						try {
							rawResponse = workshop.makeRequest("POST", "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive")) ).put("rows", rows).toString());
							try{
								response = new org.json.JSONObject(rawResponse);
								System.out.println(response.getJSONObject("counters"));
							}catch(org.json.JSONException e) {
								System.out.println("Response not a JSONObject: " + rawResponse);
							}
							while(rows.length() > 0) {
								rows.remove(0);
							}
						} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
							e.printStackTrace();
						}
					}
				}
				if(batchCount % 2000 != 0) {
					System.out.println("Deactivating characteristic batch...");
					try {
						rawResponse = workshop.makeRequest("POST", "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rows).toString());
						System.out.println(rawResponse);
					} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
						e.printStackTrace();
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
				batchCount = 0;
			}
			System.out.println("Now going to delete (" + objectsToDeactivate.size() + ")");
			for(String currentId : objectsToDeactivate) {
				sb.append(sb.length() == 0 ? "" : ",").append("\"").append(currentId).append("\"");
				batchCount++;
				if(batchCount % 200 == 0) {
					System.out.println("Deleting batch...");
					try {
						rawResponse = workshop.makeRequest("DELETE", "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode( "Characteristic.Identifier in (" + sb.toString() + ")", "UTF-8"), null, headersDelete);
						System.out.println("---->" + rawResponse + "<----");
						sb.setLength(0);
					} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
						e.printStackTrace();
					}
				}
			}
			if(batchCount % 200 != 0) {
				System.out.println("Deleting batch... " + sb.toString());
				try {
					rawResponse = workshop.makeRequest("DELETE", "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode( "Characteristic.Identifier in (" + sb.toString() + ")", "UTF-8"), null, headersDelete);
					System.out.println("--->" + rawResponse + "<---");
					sb.setLength(0);
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					e.printStackTrace();
				}
			}
			batchCount = 0;
		}while(!objectsToDeactivate.isEmpty());

		java.util.LinkedList<String> lookupIdentifiers = new java.util.LinkedList<>();

		try {
			do {
				rawResponse = workshop.makeRequest("GET", "/list/Lookup/bySearch"
						+ "?fields="
							+ java.net.URLEncoder.encode("Lookup.Identifier", "UTF-8")
						+ "&pageSize=2500"
						+ "&query="
							+ java.net.URLEncoder.encode("not Lookup.Identifier is empty", "UTF-8"), null)
						+ "&startIndex=" + currentIndex;
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					lookupIdentifiers.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
					currentIndex++;
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}

		System.out.println("Found: " + lookupIdentifiers.size() + " lookup identifiers");

		for(String lookupIdentifier : lookupIdentifiers) {
			try {
				System.out.println("Deleting " + lookupIdentifier + " content.");
				rawResponse = workshop.makeRequest("DELETE", "/list/LookupValue/byLookup?lookup=" + java.net.URLEncoder.encode(lookupIdentifier, "UTF-8"), null, headersDelete);
				System.out.println("Response was --->" + rawResponse + "<---");
				System.out.println("Now deleting " + lookupIdentifier + ".");
				rawResponse = workshop.makeRequest("DELETE", "/list/Lookup/bySearch?query=" + java.net.URLEncoder.encode("Lookup.Identifier equals \"" + lookupIdentifier + "\"", "UTF-8"), null, headersDelete);
				System.out.println("Response was --->" + rawResponse + "<---");
			} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
		}

	}
}
