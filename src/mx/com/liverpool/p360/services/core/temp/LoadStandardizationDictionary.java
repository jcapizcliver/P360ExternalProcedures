package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class LoadStandardizationDictionary {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) throws ServiceUnavailableException {
		for (String arg : args) {
			java.nio.file.Path path = java.nio.file.Paths.get( arg );
			java.io.File f = path.toFile();
			String rawResponse = null;
			org.json.JSONArray rows = null;

			String dictionaryName = f.getName().replaceAll("(\\.[a-zA-Z0-9_-]+)$", "").replaceAll(" ", "_");
			System.out.println("--->" + dictionaryName);
			try {
				rawResponse = workshop.makeRequest("POST", "/list/StandardizationDictionary", new org.json.JSONObject().put("columns", new org.json.JSONArray()).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + dictionaryName + "'")).put("values", new org.json.JSONArray()) )).toString() );
				System.out.println(rawResponse);
				try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f), java.nio.charset.Charset.forName("UTF-8")))){
					String line = null;
					String[] pieces = null;
					String delim = "\"";
					String sep = "\t";
					String esc = "\\";
					rows = new org.json.JSONArray();
					while((line = br.readLine()) != null) {
						pieces = workshop.parseLine(line, delim, sep, esc);
						rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'" + dictionaryName + "'")).put("values", new org.json.JSONArray().put(pieces[1])));
						if(rows.length() == 200) {
							rawResponse = workshop.makeRequest("POST", "/list/StandardizationValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"))).put("rows", rows).toString());
							while(rows.length() > 0) {
								rows.remove(0);
							}
						}
					}
					if(rows.length() > 0) {
						rawResponse = workshop.makeRequest("POST", "/list/StandardizationValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"))).put("rows", rows).toString());
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			} catch (org.json.JSONException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}
