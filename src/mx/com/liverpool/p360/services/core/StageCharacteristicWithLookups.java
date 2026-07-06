package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class StageCharacteristicWithLookups {

	public static void main(String[] args) throws ServiceUnavailableException {
		String rawResponse = null;
//		String encoded = "cmVzdDpoZWlsZXI=";
//		String baseUrl = "https://172.18.237.213:1512/rest/V2.0";
//		String baseUrl = "http://172.18.237.162:1512/rest/V2.0";
		RESTWrapper rw = new RESTWrapper();
		RestClient rc = rw.getRw().getRc(); //new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);
		String baseUrl = rw.getRw().getBaseUrl();
		String baseDirectory = args[0];
		String lookupName = null;
		String delim = "\"";
		String sep = ";";
		String esc = "\\";
		System.out.println("Reading cache file...");
		String characteristic = null;
		int ci = 0;
		int tz = 0;
		org.json.JSONObject r = null;
		org.json.JSONArray rws = null;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(baseDirectory, "characteristic_and_lookups").toString())))){
			pw.println(formatValue("Characteristic", delim, sep, esc) + sep + formatValue("Lookup", delim, sep, esc));
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?"
						+ "fields=" + java.net.URLEncoder.encode("Characteristic.Lookup->Lookup.Identifier,Characteristic.Identifier", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("not Characteristic.Lookup is empty", "UTF-8") + ""
						+ "&pageSize=10000"
						+ "&startIndex=" + ci, null);
				r = new org.json.JSONObject(rawResponse);
				rws = r.getJSONArray("rows");
				for(int m=0; m< rws.length(); m++) {
					lookupName = rws.getJSONObject(m).getJSONArray("values").getString(0);
					characteristic = rws.getJSONObject(m).getJSONArray("values").getString(1);
					pw.println(formatValue(characteristic, delim, sep, esc) + sep + formatValue(lookupName, delim, sep, esc));
				}
				ci += r.getInt("pageSize");
				tz = r.getInt("totalSize");
				System.out.println(ci + "/" + tz);
			}while(ci < tz);
			ci = 0;
		}catch(org.json.JSONException e) {

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}


	public static String serializeLine(String value, String delimiter, String separator, String escape) throws IllegalArgumentException {
		return value == null ? "" : value.contains(separator) || value.contains(delimiter) || value.contains("\\".equals(escape) ? "\\" : escape) ? delimiter + value.replaceAll("(?=[" + delimiter + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimiter: value;
	}

	public static String formatValue(String value, String delimiter, String separator, String escape) {
		return value == null ? "" : value.contains(delimiter) || value.contains(separator) ? delimiter + value.replaceAll("(?=[" + separator + "])", "\\".equals(escape) ? escape + escape : escape) + delimiter : value;
	}
}
