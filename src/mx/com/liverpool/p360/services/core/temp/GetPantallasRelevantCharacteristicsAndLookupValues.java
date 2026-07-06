package mx.com.liverpool.p360.services.core.temp;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class GetPantallasRelevantCharacteristicsAndLookupValues {

	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final String baseUrl = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	private static String delimiter = "\"";
	private static String separator = ",";
	private static String escape = "\\";

	public static void main(String[] args) throws ServiceUnavailableException {
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int startIndex = 0;
		org.json.JSONArray values = null;
		String url = null;
		java.util.Set<String> characteristics = new java.util.TreeSet<>();
		String dictionaryIdentifier = "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_bpk";
		StringBuilder sb = new StringBuilder();
		int a=0;
		StringBuilder line = new StringBuilder();
		java.util.Set<String> lookups = new java.util.TreeSet<>();
		String lookup = null;
		try{
			do {
				url = baseUrl + "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode( "'" + dictionaryIdentifier + "'", "UTF-8") + ""
						+ "&query=" +
						java.net.URLEncoder.encode(
								"StandardizationValue.StructureGroup equals \"EU4-113578\" and "
										+ "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dictionaryIdentifier + "\"", "UTF-8" ) + ""
										+ "&metaData=true"
										+ "&fields=" +
										java.net.URLEncoder.encode(
												"StandardizationValue.Characteristic->Characteristic.Identifier"
												, "UTF-8")
										+ "&pageSize=1000"
										+ "&startIndex=" + startIndex;
				rawResponse = rc.getRequest("GET", url, null);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					characteristics.add(values.getString(0));
					startIndex++;
				}
				totalSize = response.getInt("totalSize");
			}while(startIndex < totalSize);
			System.out.println("Found: " + characteristics.size() + " characteristics.");
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pantallas_characteristic_data.csv")))){
				pw.println(
						"Identifier" + separator +
						"DataType" + separator +
						"LookupIdentifier" + separator +
						"LookupName_Es" + separator +
						"CharacteristicCategoryID" + separator +
						"CharacteristicCategoryName_Es" + separator +
						"RootCharacteristic" + separator +
						"ParentCharacteristic" + separator +
						"Entities" + separator +
						"IsActive"
						);
				for(String characteristic : characteristics) {
					sb.append(a == 0 ? "" : ",").append(java.net.URLEncoder.encode( "'" + characteristic + "'", "UTF-8" ));
					a++;
					if(a == 20) {
						startIndex = 0;
						do {
							url = baseUrl + "/list/Characteristic/byItems"
									+ "?items=" + sb.toString()
									+ "&metaData=true"
									+ "&fields=" +
										java.net.URLEncoder.encode(
												      "Characteristic.Identifier,"
												    + "Characteristic.DataType,"
												    + "Characteristic.Lookup->Lookup.Identifier,"
												    + "Characteristic.Lookup->LookupLang.Name(es),"
												    + "Characteristic.Category->LookupValue.Code,"
												    + "Characteristic.Category->LookupValueLang.Name(es),"
												    + "Characteristic.RootCharacteristic->Characteristic.Identifier,"
												    + "Characteristic.ParentCharacteristic->Characteristic.Identifier,"
												    + "Characteristic.Entities,"
												    + "Characteristic.IsActive"
												, "UTF-8")
									+ "&resolveItems=true"
									+ "&pageSize=1000"
									+ "&startIndex=" + startIndex;
							rawResponse = rc.getRequest("GET", url, null);
							response = new org.json.JSONObject(rawResponse);
							rows = response.getJSONArray("rows");
							for(int i=0; i<rows.length(); i++) {
								values = rows.getJSONObject(i).getJSONArray("values");
								line
									.append(fv(values.getString(0)))
									.append(separator)
									.append(fv(values.getString(1)))
									.append(separator)
									.append(fv(values.getString(2)))
									.append(separator)
									.append(fv(values.getString(3)))
									.append(separator)
									.append(fv(values.getString(4)))
									.append(separator)
									.append(fv(values.getString(5)))
									.append(separator)
									.append(fv(values.getString(6)))
									.append(separator)
									.append(fv(values.getString(7)))
									.append(separator)
									.append(fv(values.getJSONArray(8).join(";").replaceAll("\"", "")))
									.append(separator)
									.append(fv(values.getString(9)))
									;
								pw.println(line.toString());
								lookup = values.getString(2);
								if(lookup != null && !"".equals(lookup)) {
									lookups.add(lookup);
								}
								line.setLength(0);
								startIndex++;
							}
							totalSize = response.getInt("totalSize");
						}while(startIndex < totalSize);
						a = 0;
						sb.setLength(0);
					}
				}
				if (a != 0) {
					startIndex = 0;
					do {
						url = baseUrl + "/list/Characteristic/byItems"
								+ "?items=" + sb.toString()
								+ "&metaData=true"
								+ "&fields=" +
									java.net.URLEncoder.encode(
											      "Characteristic.Identifier,"
											    + "Characteristic.DataType,"
											    + "Characteristic.Lookup->Lookup.Identifier,"
											    + "Characteristic.Lookup->LookupLang.Name(es),"
											    + "Characteristic.Category->LookupValue.Code,"
											    + "Characteristic.Category->LookupValueLang.Name(es),"
											    + "Characteristic.RootCharacteristic->Characteristic.Identifier,"
											    + "Characteristic.ParentCharacteristic->Characteristic.Identifier,"
											    + "Characteristic.Entities,"
											    + "Characteristic.IsActive"
											, "UTF-8")
								+ "&resolveItems=true"
								+ "&pageSize=1000"
								+ "&startIndex=" + startIndex;
						rawResponse = rc.getRequest("GET", url, null);
						response = new org.json.JSONObject(rawResponse);
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							values = rows.getJSONObject(i).getJSONArray("values");
							line
								.append(fv(values.getString(0)))
								.append(separator)
								.append(fv(values.getString(1)))
								.append(separator)
								.append(fv(values.getString(2)))
								.append(separator)
								.append(fv(values.getString(3)))
								.append(separator)
								.append(fv(values.getString(4)))
								.append(separator)
								.append(fv(values.getString(5)))
								.append(separator)
								.append(fv(values.getString(6)))
								.append(separator)
								.append(fv(values.getString(7)))
								.append(separator)
								.append(fv(values.getJSONArray(8).join(";").replaceAll("\"", "")))
								.append(separator)
								.append(fv(values.getString(9)))
								;
							pw.println(line.toString());
							lookup = values.getString(2);
							if(lookup != null && !"".equals(lookup)) {
								lookups.add(lookup);
							}
							line.setLength(0);
							startIndex++;
						}
						totalSize = response.getInt("totalSize");
					}while(startIndex < totalSize);
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			System.out.println("Now writing lookups...");
			int m = 0;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pantallas_characteristic_lookup_data.csv")))){
				pw.println("LookupIdentifier" + separator + "LookupName" + separator + "LookupValueCode" + separator + "LookupValueName_Es" + separator + "IsActive");
				for(String lkp : lookups) {
					startIndex = 0;
					do {
						url = baseUrl + "/list/LookupValue/bySearch"
								+ "?lookup=" + java.net.URLEncoder.encode(lkp, "UTF-8")
								+ "&query=" + java.net.URLEncoder.encode("LookupValue.IsActive = true", "UTF-8")
								+ "&metaData=true"
								+ "&fields=" +
									java.net.URLEncoder.encode(
											      "LookupValue.Lookup->Lookup.Identifier,"
											    + "LookupValue.Lookup->LookupLang.Name(es),"
											    + "LookupValue.Code,"
											    + "LookupValueLang.Name(es),"
											    + "LookupValue.IsActive"
											, "UTF-8")
								+ "&resolveItems=true"
								+ "&pageSize=1000"
								+ "&startIndex=" + startIndex;
						rawResponse = rc.getRequest("GET", url, null);
						response = new org.json.JSONObject(rawResponse);
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							values = rows.getJSONObject(i).getJSONArray("values");
							line
								.append(fv(values.getString(0)))
								.append(separator)
								.append(fv(values.getString(1)))
								.append(separator)
								.append(fv(values.getString(2)))
								.append(separator)
								.append(fv(values.getString(3)))
								.append(separator)
								.append(fv(values.getString(4)))
								;
							pw.println(line.toString());
							line.setLength(0);
							startIndex++;
							m++;
							if(m % 1000 == 0) {
								System.out.print(".");
								if(m % 10000 == 0) {
									System.out.println();
								}
							}
						}
						totalSize = response.getInt("totalSize");
					}while(startIndex < totalSize);
					sb.setLength(0);
				}
				System.out.println("\nTotal: " + m);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		} catch (org.json.JSONException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
	}

	public static String fv(String value) {
		return value == null ? "" : value.contains(delimiter) || value.contains(separator) ? delimiter + value.replaceAll("(?=[" + delimiter + "])", "\\".equals(escape) ? escape + escape : escape) + delimiter : value;
	}
}
