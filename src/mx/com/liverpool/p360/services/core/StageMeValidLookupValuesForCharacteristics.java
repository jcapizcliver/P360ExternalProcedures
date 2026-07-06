package mx.com.liverpool.p360.services.core;

import org.json.JSONObject;


public class StageMeValidLookupValuesForCharacteristics {

	public static void main(String[] args) {
		String rawResp = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		String url = null;
		int currentIndex = 0;
		int totalSize = 0;
		String validValuesFilter = null;
		String dictionary = "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla";
		String encoded = PropertiesManager.get("p360.contingency.basic_token_auth"); // "cmVzdDpoZWlsZXI=";
		String baseUrl = PropertiesManager.get("p360.contingency.base_url"); // "https://172.18.237.210:1512/rest/V2.0";
		RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);
		String baseDirectory = args[0];
		java.util.Map<String, String> characteristicsWithLookup = new java.util.TreeMap<>();
		java.util.Map<String, String> lookupsWithFilter = new java.util.TreeMap<>();
		java.util.Set<String> templateCharacteristicsWithoutFilter = new java.util.TreeSet<>();
		java.util.LinkedList<String> losQueSi = new java.util.LinkedList<>();
		try {
			System.out.println("Going to collect characteristics from metadata table...");
			/*
			if(java.nio.file.Files.exists(java.nio.file.Paths.get(baseDirectory, "characteristics_with_lookup")) &&
					java.nio.file.Files.exists(java.nio.file.Paths.get(baseDirectory, "lookups_with_filter"))) {
				System.out.println("Caché files exist!");
				try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(baseDirectory, "characteristics_with_lookup").toString())))){
					String ln = null;
					String ax = null;
					while((ln = br.readLine()) != null) {
						ax = ln.substring(ln.indexOf(";") + 1);
						characteristicsWithLookup.put(ln.substring(0, ln.indexOf(";")), ax.replaceAll("(^\")|(\"$)", ""));
					}
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
				try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(baseDirectory, "lookups_with_filter").toString())))){
					String ln = null;
					String ax = null;
					while((ln = br.readLine()) != null) {
						ax = ln.substring(ln.indexOf(";") + 1);
						lookupsWithFilter.put(ln.substring(0, ln.indexOf(";")), ax.replaceAll("(^\")|(\"$)", ""));
					}
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			}else {
				*/
				do {
					url = baseUrl + "/list/StandardizationValue/byDictionary?dictionary='" + java.net.URLEncoder.encode( dictionary, "UTF-8") + "'&query=" + java.net.URLEncoder.encode(
							"StandardizationValue.Property equals ListOfValuesFilter or (StandardizationValue.Property->LookupValue.Code equals \"SentToVendorCenter\" and StandardizationValue.Characteristic->Characteristic.DataType equals \"LOOKUP\")", "UTF-8")  + "&fields=" + java.net.URLEncoder.encode( "StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.Property->LookupValue.Code,StandardizationValue.PropertyValue,StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier", "UTF-8") + "&pageSize=10000&startIndex=" + currentIndex;
					rawResp = rc.getRequest( "GET", url , null );
					response = new org.json.JSONObject(rawResp);
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						if("SentToVendorCenter".equals(rows.getJSONObject(i).getJSONArray("values").getString(2)) && !"".equals( rows.getJSONObject(i).getJSONArray("values").getString(4) ) ) {
							characteristicsWithLookup.put(String.valueOf(rows.getJSONObject(i).getJSONArray("values").get(0)) + "<::>" + String.valueOf(rows.getJSONObject(i).getJSONArray("values").get(1)), String.valueOf(rows.getJSONObject(i).getJSONArray("values").get(3)));
						}else {
							validValuesFilter = String.valueOf(rows.getJSONObject(i).getJSONArray("values").get(2));
							if("ListOfValuesFilter".equals(validValuesFilter)) {
								lookupsWithFilter.put(String.valueOf(rows.getJSONObject(i).getJSONArray("values").get(0)) + "<::>" + String.valueOf(rows.getJSONObject(i).getJSONArray("values").get(1)), String.valueOf(rows.getJSONObject(i).getJSONArray("values").get(3)));
							}
						}
						currentIndex++;
					}
					totalSize = response.getInt("totalSize");
					System.out.println("..." + currentIndex + "/" + totalSize);
				}while(currentIndex < totalSize);
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(baseDirectory + java.io.File.separator + "characteristics_with_lookup")))){
					characteristicsWithLookup.forEach((k,v)->pw.println(k + ";" + (v.contains(";") ? "\"" + v.replaceAll("\"", "\\\"") + "\"" : v.replaceAll("\"", "\\\""))));
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(baseDirectory + java.io.File.separator + "lookups_with_filter")))){
					lookupsWithFilter.forEach((k,v)->pw.println(k + ";" + (v.contains(";") ? "\"" + v.replaceAll("\"", "\\\"") + "\"" : v.replaceAll("\"", "\\\""))));
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
//			}
			System.out.println("Collected.");
			int matches = 0;
			for(String key : characteristicsWithLookup.keySet()) {
				if(!lookupsWithFilter.containsKey(key)) {
					templateCharacteristicsWithoutFilter.add(key);
				}else {
					losQueSi.addLast(key);
				}
			}
			System.out.println("Filtered values with lookupValueFilter");
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(baseDirectory + java.io.File.separator + "template_characteristics_without_filter")))){
				templateCharacteristicsWithoutFilter.forEach(s -> pw.println(s));
			}catch(java.io.IOException e) { e.printStackTrace(); }
			java.util.Set<String> currentValidCodes = null;
			String[] validCodes = null;
			String[] elPar = null;
			String[] codes = null;
			String currentLookupId = null;
			System.out.println("Now staging files... (Found " + templateCharacteristicsWithoutFilter.size() + " characteristics without filter, out of " + characteristicsWithLookup.size() + " characteristics with lookup)");
			for(String key : losQueSi) {
				elPar = key.split("<::>");
				currentLookupId = characteristicsWithLookup.get(key);
				if(!java.nio.file.Files.exists(java.nio.file.Paths.get(baseDirectory, elPar[0]))) {
					java.nio.file.Files.createDirectory(java.nio.file.Paths.get(baseDirectory, elPar[0]));
				}
				System.out.println("Working with: " + key);
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(baseDirectory, elPar[0], elPar[1] + ".dat").toFile())))){
					pw.println("Code;ValueLang_es");
					currentIndex = 0;
					validCodes = lookupsWithFilter.get(key).split(",");
					currentValidCodes = new java.util.TreeSet<>();
					for (String validCode : validCodes) {
						currentValidCodes.add(validCode);
					}
					if(currentValidCodes.isEmpty()) {
						System.out.println("Found a fake one: " + elPar[1] + " (within template: " + elPar[0] + ")");
					}else {
						codes = validCodes;
						StringBuilder sb = new StringBuilder();
						int f = 0;
						for(String code : codes){
							try{
								if(code != null && !"".equals(code.trim())){
									sb.append(f == 0 ? "" : ",").append("'").append(code == null ? "" :  code.trim()).append("'@'").append(currentLookupId).append("'");
									f++;
								}
							}catch(NullPointerException ignore){}
						}
						do {
							System.out.println("Checking: " + sb.toString());
							url = baseUrl + "/list/LookupValue/byItems?resolveItems=true&items=" + java.net.URLEncoder.encode( sb.toString(), "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValue.Code,LookupValueLang.Name(es)", "UTF-8") + "&pageSize=500&startIndex=" + currentIndex;
							rawResp = rc.getRequest("GET", url, null);
							response = new org.json.JSONObject(rawResp);
							rows = response.getJSONArray("rows");
							for(int i=0; i<rows.length(); i++) {
								if(currentValidCodes.contains(rows.getJSONObject(i).getJSONArray("values").getString(0))) {
									pw.println( serializeLine(rows.getJSONObject(i).getJSONArray("values").getString(0), "\"", ";", "\\") + ";" + serializeLine(rows.getJSONObject(i).getJSONArray("values").getString(1), "\"", ";", "\\") );
									matches++;
								}
								currentIndex++;
							}
							totalSize = response.getInt("totalSize");
						}while(currentIndex < totalSize);
						currentIndex = 0;
						if(matches == 0) {
							System.out.println("\tDid not find a match for: " + key + ", the values were supposed to be: " + currentValidCodes);
						}else {
							matches = 0;
						}
					}
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}catch(org.json.JSONException e) {
					System.out.println(rawResp);
					e.printStackTrace();
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}

	public static String serializeLine(String value, String delimiter, String separator, String escape) throws IllegalArgumentException {
		return value == null ? "" : value.contains(separator) || value.contains(delimiter) || value.contains("\\".equals(escape) ? "\\" : escape) ? delimiter + value.replaceAll("(?=[" + delimiter + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimiter: value;
	}

//	private static String formatValue(String raw) {
//		return raw == null ? "" : raw.contains(";") ? "\"" + (raw.replaceAll("\"", "\\\"")) + "\"" : (raw.replaceAll("\"", "\\\""));
//	}
}
