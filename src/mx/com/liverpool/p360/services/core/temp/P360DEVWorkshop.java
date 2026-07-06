package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class P360DEVWorkshop {


	public static void main(String[] args) throws ServiceUnavailableException, JSONException {
		RESTWorkshop workshop = new RESTWorkshop();

		String rawResponse = null;
		java.util.Map<String, String> headersDelete = new java.util.TreeMap<>();
		headersDelete.put("Content-Type", "application/x-www-form-urlencoded");
		headersDelete.put("Accept", "application/json");
		headersDelete.put("Authorization", workshop.getRc().getHeader().get("Authorization"));

		int currentIndex = 0;
		int totalSize = 0;
		int batchCount = 0;

		StringBuilder sb = new StringBuilder();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\SizeERPvsUniqueSize.csv")))) {
			String line = null;
			String[] pieces = null;
			rows = new org.json.JSONArray();
			while((line = br.readLine()) != null) {
				pieces = line.split("\t");
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'SizeERPvsUniqueSize'")).put("values", new org.json.JSONArray().put(pieces[1])));
				if(rows.length() == 1000) {
					rawResponse = workshop
							.makeRequest("POST", "/list/StandardizationValue",
									new org.json.JSONObject()
									.put("rows", rows)
									.put("columns",
											new org.json.JSONArray()
												.put(
													new org.json.JSONObject()
														.put("identifier", "StandardizationValue.AlternativeValue")
												)
										).toString() );
					System.out.println(rawResponse);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				rawResponse = workshop
						.makeRequest("POST", "/list/StandardizationValue",
								new org.json.JSONObject()
								.put("rows", rows)
								.put("columns",
										new org.json.JSONArray()
											.put(
												new org.json.JSONObject()
													.put("identifier", "StandardizationValue.AlternativeValue")
											)
									).toString() );
				System.out.println(rawResponse);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
			rawResponse = workshop.makeRequest("POST", "/list/StandardizationValue/", null);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}

		/*
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\LookupTableTallaUnicavsTallaERP.csv")))) {
			String line = null;
			String[] pieces = null;
			rows = new org.json.JSONArray();
			while((line = br.readLine()) != null) {
				pieces = line.split("\t");
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'TallaUnicavsTallaERP'")).put("values", new org.json.JSONArray().put(pieces[1])));
				if(rows.length() == 1000) {
					rawResponse = workshop
							.makeRequest("POST", "/list/StandardizationValue",
									new org.json.JSONObject()
									.put("rows", rows)
									.put("columns",
											new org.json.JSONArray()
												.put(
													new org.json.JSONObject()
														.put("identifier", "StandardizationValue.AlternativeValue")
												)
										).toString() );
					System.out.println(rawResponse);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				rawResponse = workshop
						.makeRequest("POST", "/list/StandardizationValue",
								new org.json.JSONObject()
								.put("rows", rows)
								.put("columns",
										new org.json.JSONArray()
											.put(
												new org.json.JSONObject()
													.put("identifier", "StandardizationValue.AlternativeValue")
											)
									).toString() );
				System.out.println(rawResponse);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
			rawResponse = workshop.makeRequest("POST", "/list/StandardizationValue/", null);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		*/
		System.exit(0);

	}
}
