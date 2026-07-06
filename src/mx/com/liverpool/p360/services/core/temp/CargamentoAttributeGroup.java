package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargamentoAttributeGroup {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		workshop.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		workshop.addHeader("Authorization" , "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, org.json.JSONArray> existingLookupValues = new java.util.TreeMap<>();
		existingLookupValues = collectExistingReferenceAttributeGroupLookupValues();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray existingAttributeGroups = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\tmp\\Mapeo Att STEP VS ECC - Sheet1.tsv")))){
			String delim = "\"";
			String sep = "\t";
			String esc = "\\";
			String line = null;
			String[] header = workshop.parseLine(br.readLine(), delim, sep, esc);
			String[] pieces = null;
			System.out.println(java.util.Arrays.asList(header));
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				existingAttributeGroups = existingLookupValues.get(pieces[0]);
				if(existingAttributeGroups == null) {
					existingAttributeGroups = new org.json.JSONArray();
				}
				addIfNotExist("CategorySpecificAttributesSAP", existingAttributeGroups);
				System.out.println(pieces[0] + " -->" + existingAttributeGroups);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'Characteristics'")).put("values", new org.json.JSONArray().put(pieces[2]).put(existingAttributeGroups).put(true)));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(ECC)")).put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('AttributeGroup')")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString());
		System.out.println(response);
	}

	public static void addIfNotExist(String value, org.json.JSONArray values) {
		if(values.length() == 1 && "".equals(values.getString(0))) {
			values.remove(0);
		}
		for(int i=0; i<values.length(); i++) {
			if(values.getString(i).equals(value)) {
				return;
			}
		}
		values.put(value);
	}

	private static java.util.Map<String, org.json.JSONArray> collectExistingReferenceAttributeGroupLookupValues(){
		java.util.Map<String, org.json.JSONArray> map = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "Characteristics");
		qp.put("query", "not LookupValueIdentifier.Code(ECC) is empty");
		qp.put("fields", "LookupValue.Code,LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code");
		qp.put("pageSize", "900");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				map.put(values.getString(0), values.getJSONArray(1));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return map;
	}

}
