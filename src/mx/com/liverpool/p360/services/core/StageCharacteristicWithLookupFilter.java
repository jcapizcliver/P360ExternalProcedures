package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class StageCharacteristicWithLookupFilter {


	public static void main(String[] args) throws ServiceUnavailableException {
		String rawResponse = null;
		String encoded = PropertiesManager.get("p360.contingency.basic_token_auth"); // "cmVzdDpoZWlsZXI=";
		String baseUrl = PropertiesManager.get("p360.contingency.base_url"); // "https://172.18.237.213:1512/rest/V2.0";
//		String baseUrl = "http://172.18.237.162:1512/rest/V2.0";
		RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);
		String baseDirectory = args[0];
		String lookupName = null;
		String delim = "\"";
		String sep = ";";
		String esc = "\\";
		System.out.println("Reading cache file...");
		String characteristic = null;
		String template = null;
		String filter = null;
		int ci = 0;
		int tz = 0;
		org.json.JSONObject r = null;
		org.json.JSONArray rws = null;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(baseDirectory, "template_characteristic_lookup_filter").toString())))){
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch?"
						+ "fields=" + java.net.URLEncoder.encode(
								"StandardizationValue.Value"
								+ ",StandardizationValue.StructureGroup->LookupValue.Code,"
								+ "StandardizationValue.Characteristic->Characteristic.Identifier,"
								+ "StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier,"
								+ "StandardizationValue.PropertyValue"
							, "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
								+ "StandardizationValue.CreationType equals CreateProposal and StandardizationValue.Property equals ListOfValuesFilter and not StandardizationValue.PropertyValue is empty", "UTF-8") + ""
						+ "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'", "UTF-8")
						+ "&pageSize=5000"
						+ "&startIndex=" + ci, null);
				r = new org.json.JSONObject(rawResponse);
				tz = r.getInt("totalSize");
				rws = r.getJSONArray("rows");
				for(int m=0; m< rws.length(); m++) {
					ci++;
					template = rws.getJSONObject(m).getJSONArray("values").getString(1);
					characteristic = rws.getJSONObject(m).getJSONArray("values").getString(2);
					lookupName = rws.getJSONObject(m).getJSONArray("values").getString(3);
					filter  = rws.getJSONObject(m).getJSONArray("values").getString(4);
					pw.println(formatValue(rws.getJSONObject(m).getJSONArray("values").getString(0), delim, sep, esc) + sep + formatValue(template + "_" + characteristic, delim, sep, esc) + sep + formatValue(lookupName, delim, sep, esc) + sep + formatValue(filter, delim, sep, esc));
					if(ci % 1000 == 0) {
						System.out.print(".");
						if( ci % 10000 == 0) {
							System.out.println(ci);
						}
					}
				}
				System.out.println(ci + "/" + tz);
			}while(ci < tz);
		}catch(org.json.JSONException e) {

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}


//	public static String serializeLine(String value, String delimiter, String separator, String escape) throws IllegalArgumentException {
//		return value == null ? "" : value.contains(separator) || value.contains(delimiter) || value.contains("\\".equals(escape) ? "\\" : escape) ? delimiter + value.replaceAll("(?=[" + delimiter + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimiter: value;
//	}

	public static String formatValue(String value, String delimiter, String separator, String escape) {
		return value == null ? "" : value.contains(delimiter) || value.contains(separator) ? delimiter + value.replaceAll("(?=[" + delimiter + separator + "])", "\\".equals(escape) ? escape + escape : escape) + delimiter : value.contains(escape) ? delimiter + value.replaceAll("(?=[" + (escape.equals("\\") ? "\\\\" : escape) + "])", "\\".equals(escape) ? escape + escape : escape) + delimiter : value;
	}
}
