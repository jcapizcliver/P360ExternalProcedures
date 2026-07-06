package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class TamanoUnicoFilters {

	private static RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		java.util.Map<String, String> bros = getMeTheBros();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		request.put("columns", columns)
			.put("rows", rows);
		org.json.JSONObject response = null;
		String bro = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\tmp\\acotación tallas universo - Sheet1.tsv")))){
			String line = null;
			String[] pieces = null;
			String sep = "\t";
			String delim = "";
			String esc = "\\";
			while((line = br.readLine()) != null) {
				pieces = rw.parseLine(line, delim, sep, esc);
				bro = bros.get(pieces[0]);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + (bro != null ? bro : pieces[0] + "_TamanoUnico_Proposal_List of Values - Valid Values") + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'"))
						.put("values", new org.json.JSONArray().put(pieces[0]).put("TamanoUnico").put("CreateProposal").put("ListOfValuesFilter").put(pieces[2])));
				if(rows.length() == 200) {
					response = rw.makeRequest("POST", "/list/StandardizationValue", empty, request.toString());
					if(response == null) {
						System.out.println("ERR: " + rw.getRawResponse());
					}else{
						System.out.println(response.getJSONObject("counters"));
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/StandardizationValue", empty, request.toString());
				if(response == null) {
					System.out.println("ERR: " + rw.getRawResponse());
				}else{
					System.out.println(response.getJSONObject("counters"));
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

	private static java.util.Map<String, String> getMeTheBros(){
		java.util.Map<String, String> ids = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("fields", "StandardizationValue.Value,StandardizationValue.StructureGroup->LookupValue.Code");
		qp.put("query",  "StandardizationValue.Characteristic->Characteristic.Identifier equals \"TamanoUnico\" and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\" and StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				for(int i=0; i< rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					System.out.println(values);
					ids.put(values.getString(1), values.getString(0));
				}
			}else {
				System.out.println("ERR msj: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return ids;
	}

}
