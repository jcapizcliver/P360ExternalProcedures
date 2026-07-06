package mx.com.liverpool.p360.services.core;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

public class StageMissingCharacteristicLookups {

	public static void main(String[] args) throws ServiceUnavailableException {
		if(args.length < 2) {
			System.out.println( new org.json.JSONObject().put("Error", "Uso: StageMissingCharacteristicLookups <BaseDirectory> <refresh:true|false> [<query> <fields>]") );
			return;
		}
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		String url = null;
		int currentIndex = 0;
		int totalSize = 0;
		String encoded = "cmVzdDpoZWlsZXI=";
		String baseUrl = "http://172.18.237.162:1512/rest/V2.0";
		RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);
		String baseDirectory = args[0];
		Boolean refresh = Boolean.parseBoolean(args[1]);
		String query = args.length > 2 ? args[2] : "Characteristic.Identifier in (UnidadDeMedidaVolumen,MesdeEntregadeMercancIa,Business,GarantiaDelFabricanteVAD,UnidadDeMedidaLongitud,UnidadDeMedidaPeso,ColoursLiverpoolAtt,TamanoUnico)";
		String fields = args.length > 3 ? args[3] : "Characteristic.Identifier,Characteristic.Lookup->Lookup.Identifier";
		String k = null;
		String v = null;
		String lookupName = null;
//		String delim = "\"";
//		String sep = ";";
//		String esc = "\\";
//		Yep reader = new Yep();
		String characteristicId = null;
		java.io.File[] globals = new java.io.File(java.nio.file.Paths.get(baseDirectory, "global").toString()).listFiles();
		java.util.Set<String> ei = new java.util.TreeSet<>();
		for (File global : globals) {
			ei.add(global.getName());
		}
		int totalSizeC = 0;
		int currentIndexC = 0;
		org.json.JSONObject responseC = null;
		org.json.JSONArray rowsC = null;
		org.json.JSONArray errorMessages = new org.json.JSONArray();
		org.json.JSONObject globalResponse = new org.json.JSONObject();
		org.json.JSONArray processedEntries = new org.json.JSONArray();
		do {
			try {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?"
						+ "fields=" + java.net.URLEncoder.encode(fields, "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode(query, "UTF-8"), null)
						+ "&pageSize=10000"
						+ "&startIndex=" + currentIndexC;
			} catch (IOException e) {
				e.printStackTrace();
			}
			responseC = new org.json.JSONObject(rawResponse);
			rowsC = responseC.getJSONArray("rows");
			for(int m = 0; m < rowsC.length(); m++) {
				currentIndexC++;
				characteristicId = rowsC.getJSONObject(m).getJSONArray("values").getString(0);
				lookupName = rowsC.getJSONObject(m).getJSONArray("values").getString(1);
				if(refresh || !ei.contains(characteristicId)) {
					try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(baseDirectory, "global", characteristicId).toString())))){
						do {
							url = baseUrl + "/list/LookupValue/byLookup?lookup=" + java.net.URLEncoder.encode(lookupName, "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValue.Code,LookupValueLang.Name(es)", "UTF-8") + "&pageSize=10000&startIndex=" + currentIndex;
							rawResponse = rc.getRequest("GET", url, null);
							response = new org.json.JSONObject(rawResponse);
							rows = response.getJSONArray("rows");
							for(int i=0; i<rows.length(); i++) {
								k = rows.getJSONObject(i).getJSONArray("values").getString(0);
								v = rows.getJSONObject(i).getJSONArray("values").getString(1);
								k = serializeLine(k, "\"", ";", "\\"); // k == null ? "" : k.contains(";") ? "\"" + k.replaceAll("\"", "\\\"") + "\"" : k.replaceAll("\"", "\\\"");
								v = serializeLine(v, "\"", ";", "\\"); // v == null ? "" : v.contains(";") ? "\"" + v.replaceAll("\"", "\\\"") + "\"" : v.replaceAll("\"", "\\\"");
								pw.println(k + ";" + v);
								currentIndex++;
							}
							totalSize = response.getInt("totalSize");
						}while(currentIndex < totalSize);
					}catch(java.io.IOException e) {
						System.out.println(rawResponse);
						e.printStackTrace();
					}
				}
			}
			currentIndex = 0;
			totalSizeC = responseC.getInt("totalSize");
		}while(currentIndexC < totalSizeC);
	}

	public static String serializeLine(String value, String delimiter, String separator, String escape) throws IllegalArgumentException {
		return value == null ? "" : value.contains(separator) || value.contains(delimiter) || value.contains("\\".equals(escape) ? "\\" : escape) ? delimiter + value.replaceAll("(?=[" + delimiter + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimiter: value;
	}
}
