package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

public class StageGlobalLookupValues {

	public static void main(String[] args) throws ServiceUnavailableException {
		long init = System.currentTimeMillis();
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		String url = null;
		int currentIndex = 0;
		int totalSize = 0;
		String encoded = args[1];// "cmVzdDpoZWlsZXI=";
		String baseUrl = args[2];// "http://172.18.237.162:1512/rest/V2.0";
		boolean isLookupId = args.length > 3 ? Boolean.parseBoolean(args[3]) : false; //characteristicIsLookupID
		RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);
		String baseDirectory = args[0];
		java.util.Set<String> characteristicsWithoutFilter = new java.util.TreeSet<>();
		String[] pieces = null;
		String k = null;
		String v = null;
		String lookupName = null;
		String line = null;
		String delim = "\"";
		String sep = ";";
		String esc = "\\";
		log("Reading cache file...");
		Yep reader = new Yep();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(baseDirectory, "template_characteristics_without_filter").toString())))){
			while((line = br.readLine()) != null) {
				pieces = reader.parseLine(line, delim, sep, esc);
				characteristicsWithoutFilter.add(pieces[0].split("<::>")[1]);
			}
		} catch (IOException e) {
			logE(e);
		}
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get(baseDirectory, "global_lookups"))) {
			try{
				java.nio.file.Files.createDirectory(java.nio.file.Paths.get(baseDirectory, "global_lookups"));
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		java.util.Set<String> processedLookups = new java.util.TreeSet<>();
		String characteristic = args.length > 3 ? args[3] : null;
		int ci = 0;
		int tz = 0;
		org.json.JSONObject r = null;
		org.json.JSONArray rws = null;
		log("Using base directory: " + baseDirectory);
		log("Using base baseURL: " + baseUrl);
		try {
			if(isLookupId) {
				lookupName = characteristic;
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(baseDirectory, "global_lookups", lookupName).toString())))){
					do {
						url = baseUrl + "/list/LookupValue/bySearch"
								+ "?lookup=" + java.net.URLEncoder.encode(lookupName.replaceAll("<::>", "/"), "UTF-8")
								+ "&query=" + java.net.URLEncoder.encode("LookupValue.IsActive = true", "UTF-8")
								+ "&fields=" + java.net.URLEncoder.encode("LookupValue.Code,LookupValueLang.Name(es)", "UTF-8")
								+ "&pageSize=10000&startIndex=" + currentIndex;
						rawResponse = rc.getRequest("GET", url, null);
						response = new org.json.JSONObject(rawResponse);
						rows = response.getJSONArray("rows");
						log("Got: " + rows.length() + " rows");
						for(int i=0; i<rows.length(); i++) {
							k = rows.getJSONObject(i).getJSONArray("values").getString(0);
							v = rows.getJSONObject(i).getJSONArray("values").getString(1);
							k = serializeLine(k, delim, sep, esc); // k == null ? "" : k.contains(";") ? "\"" + k.replaceAll("\"", "\\\"") + "\"" : k.replaceAll("\"", "\\\"");
							v = serializeLine(v, delim, sep, esc); // v == null ? "" : v.contains(";") ? "\"" + v.replaceAll("\"", "\\\"") + "\"" : v.replaceAll("\"", "\\\"");
							pw.println(k + ";" + v);
							currentIndex++;
						}
						totalSize = response.getInt("totalSize");
					}while(currentIndex < totalSize);
					currentIndex = 0;
					processedLookups.add(lookupName);
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			}else {
				if(characteristic != null) {
					log("Staging only one characteristic: " + characteristic);
				}
				do {
					rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch"
							+ "?fields=" + java.net.URLEncoder.encode("Characteristic.Lookup->Lookup.Identifier,Characteristic.Identifier", "UTF-8")
							+ "&query=" + java.net.URLEncoder.encode("not Characteristic.Lookup is empty" + (characteristic != null ? " and Characteristic.Identifier equals \"" + characteristic : "\""), "UTF-8") + "&pageSize=10000&startIndex=" + ci, null);
					r = new org.json.JSONObject(rawResponse);
					rws = r.getJSONArray("rows");
					for(int m=0; m< rws.length(); m++) {
						ci++;
						lookupName = rws.getJSONObject(m).getJSONArray("values").getString(0);
						lookupName = lookupName.replaceAll("/", "<::>");
						characteristic = rws.getJSONObject(m).getJSONArray("values").getString(1);
						log("before staging...");
						if(!processedLookups.contains(lookupName)) {
							log("Proceding... " + java.nio.file.Paths.get(baseDirectory, "global_lookups", lookupName).toString());
							try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(baseDirectory, "global_lookups", lookupName).toString())))){
								do {
									url = baseUrl + "/list/LookupValue/bySearch"
											+ "?lookup=" + java.net.URLEncoder.encode(lookupName.replaceAll("<::>", "/"), "UTF-8")
											+ "&query=" + java.net.URLEncoder.encode("LookupValue.IsActive = true", "UTF-8")
											+ "&fields=" + java.net.URLEncoder.encode("LookupValue.Code,LookupValueLang.Name(es)", "UTF-8")
											+ "&pageSize=10000&startIndex=" + currentIndex;
									rawResponse = rc.getRequest("GET", url, null);
									response = new org.json.JSONObject(rawResponse);
									rows = response.getJSONArray("rows");
									log("Got: " + rows.length() + " rows");
									for(int i=0; i<rows.length(); i++) {
										k = rows.getJSONObject(i).getJSONArray("values").getString(0);
										v = rows.getJSONObject(i).getJSONArray("values").getString(1);
										k = serializeLine(k, delim, sep, esc); // k == null ? "" : k.contains(";") ? "\"" + k.replaceAll("\"", "\\\"") + "\"" : k.replaceAll("\"", "\\\"");
										v = serializeLine(v, delim, sep, esc); // v == null ? "" : v.contains(";") ? "\"" + v.replaceAll("\"", "\\\"") + "\"" : v.replaceAll("\"", "\\\"");
										pw.println(k + ";" + v);
										currentIndex++;
									}
									totalSize = response.getInt("totalSize");
								}while(currentIndex < totalSize);
								currentIndex = 0;
								processedLookups.add(lookupName);
							}catch(java.io.IOException e) {
								e.printStackTrace();
							}
						}
					}
					tz = r.getInt("totalSize");
				}while(ci < tz);
				ci = 0;
			}
		}catch(org.json.JSONException e) {
			logE(e);
		} catch (UnsupportedEncodingException e1) {
			logE(e1);
		} catch (IOException e1) {
			logE(e1);
		}
		log("Took: " + formatTime(System.currentTimeMillis() - init));
	}

	public static String serializeLine(String value, String delimiter, String separator, String escape) throws IllegalArgumentException {
		return value == null ? "" : value.contains(separator) || value.contains(delimiter) || value.contains("\\".equals(escape) ? "\\" : escape) ? delimiter + value.replaceAll("(?=[" + delimiter + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimiter: value;
	}

	public static String formatTime(long millis) {
	  	int days = (int)(millis/(1000*60*60*24));
	 	millis -= days*1000*60*60*24;
	  	int hours = (int) (millis/(1000*60*60));
	  	millis -= hours*1000*60*60;
	  	int minutes = (int) (millis/(1000*60));
	  	millis -= minutes*1000*60;
	  	int seconds = (int) (millis/1000);
	  	millis -= seconds*1000;
	  	return
	  		    (days < 10 ? "0" : "") + days + ":"
	  		+ (hours < 10 ? "0" : "") + hours + ":"
	  		+ (minutes < 10 ? "0" : "") + minutes + ":"
	  		+ (seconds < 10 ? "0" : "") + seconds
	  		+ "." + millis;
	}

	private static void log(String message){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/stage_global_lookup_values.log", true)))){
		  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "]  " + message);
		}catch(java.io.IOException e){}
	}

	private static void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/stage_global_lookup_values.log", true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}

}
