package mx.com.liverpool.p360.services.core;

import org.json.JSONObject;

public class CheckMissingSections {

	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) {
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int startIndex = 0;
		org.json.JSONArray values = null;
		String url = null;
		java.util.Map<String, String> pantallasTemplateCharacteristicSections = new java.util.TreeMap<>();
		String dictionaryIdentifier = "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_bpk";
		try{
			do {
				url = "http://172.18.237.162:1512/rest/V2.0/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode( "'" + dictionaryIdentifier + "'", "UTF-8") + ""
						+ "&query=" +
							java.net.URLEncoder.encode( "("
									+ "StandardizationValue.Property equals VendorCenterSection"
									+ ") and "
									+ "StandardizationValue.StructureGroup equals \"EU4-113578\" and "
									+ "StandardizationValue.CreationType equals Proposal and "
									+ "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dictionaryIdentifier + "\"", "UTF-8" ) + ""
						+ "&metaData=true"
						+ "&fields=" +
							java.net.URLEncoder.encode(
									  "StandardizationValue.Characteristic->Characteristic.Identifier,"
									+ "StandardizationValue.PropertyValue"
									, "UTF-8")
						+ "&pageSize=1000"
						+ "&startIndex=" + startIndex;
				rawResponse = rc.getRequest("GET", url, null);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					pantallasTemplateCharacteristicSections.put(values.getString(0), values.getString(1));
					startIndex++;
				}
				totalSize = response.getInt("totalSize");
			}while(startIndex < totalSize);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
		dictionaryIdentifier = "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla";
		java.util.Map<String, String> pantallasTemplateCharacteristicSections2 = new java.util.TreeMap<>();
		try{
			do {
				url = "http://172.18.237.162:1512/rest/V2.0/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode( "'" + dictionaryIdentifier + "'", "UTF-8") + ""
						+ "&query=" +
							java.net.URLEncoder.encode( "("
									+ "StandardizationValue.Property equals VendorCenterSection"
									+ ") and "
									+ "StandardizationValue.StructureGroup equals \"EU4-113578\" and "
									+ "StandardizationValue.CreationType equals Proposal and "
									+ "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dictionaryIdentifier + "\"", "UTF-8" ) + ""
						+ "&metaData=true"
						+ "&fields=" +
							java.net.URLEncoder.encode(
									  "StandardizationValue.Characteristic->Characteristic.Identifier,"
									+ "StandardizationValue.PropertyValue"
									, "UTF-8")
						+ "&pageSize=1000"
						+ "&startIndex=" + startIndex;
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					pantallasTemplateCharacteristicSections2.put(values.getString(0), values.getString(1));
					startIndex++;
				}
				totalSize = response.getInt("totalSize");
			}while(startIndex < totalSize);
			System.out.println("Characteristics with VendorCenterSections (1): " + pantallasTemplateCharacteristicSections.size() + "\nCharacteristics with VendorCenterSections (2): " + pantallasTemplateCharacteristicSections2.size() + "\n***********************\n");
			java.util.Set<String> notFound = new java.util.TreeSet<>();
			for(java.util.Map.Entry<String, String> entry : pantallasTemplateCharacteristicSections.entrySet()) {
				if(!pantallasTemplateCharacteristicSections2.containsKey(entry.getKey())) {
					notFound.add(entry.getKey());
				}
			}
			notFound.forEach(System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

}
