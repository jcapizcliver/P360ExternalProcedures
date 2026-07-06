package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class InspeccionDeLookupValues {


	public static void main(String[] args) throws ServiceUnavailableException {
		RESTWorkshop workshop = new RESTWorkshop();
		// https://webctep360dev.liverpool.com.mx/rest/V2.0/list/LookupValue/LookupValueIdentifier/byLookup?lookup=ZCOMALOV
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		java.util.LinkedList<String> lookups = new java.util.LinkedList<>();
		java.util.LinkedList<String> lookupsToCheck = new java.util.LinkedList<>();
		int totalSize = 0;
		int rowCount = 0;
		int currentIndex = 0;

		try {
			 rawResponse = workshop.makeRequest("GET", "/list/Lookup/bySearch?fields=Lookup.Identifier&pageSize=500&query=" + java.net.URLEncoder.encode("not Lookup.Identifier is empty", "UTF-8"), null);
			 response = new org.json.JSONObject(rawResponse);
			 rows = response.getJSONArray("rows");
			 for(int i=0; i<rows.length(); i++) {
				 lookups.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
			 }

			 for(String lookup : lookups) {
				 do {
					 rawResponse = workshop.makeRequest("GET", "/list/LookupValue/byLookup"
					 		+ "?fields=" + java.net.URLEncoder.encode("", "UTF-8")
					 		+ "&startCount=" + currentIndex
					 		+ "&pageSize=2000"
					 		+ "&lookup=" + java.net.URLEncoder.encode(lookup, "UTF-8"), null);
				 }while(currentIndex < totalSize);
			 }

			 System.exit(0);

			 for(String lookup : lookups) {
				 rawResponse = workshop.makeRequest("GET", "/list/LookupValue/LookupValueIdentifier/byLookup?fields=&lookup=" + java.net.URLEncoder.encode(lookup, "UTF-8"), null);
				 response = new org.json.JSONObject(rawResponse);
				 totalSize = response.getInt("totalSize");
				 rows = response.getJSONArray("rows");
				 System.out.println(lookup + ": " + totalSize);
				 if(totalSize > 0) {
					 lookupsToCheck.addLast(lookup);
				 }
			 }

		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}

		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\chanclas")))){
			lookupsToCheck.forEach(el->pw.println(el));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
}
