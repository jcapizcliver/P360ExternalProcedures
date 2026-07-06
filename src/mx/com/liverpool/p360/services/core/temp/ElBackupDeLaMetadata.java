package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ElBackupDeLaMetadata {

	public static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		bkp();

	}

	private static void bkp() {

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("fields",
				  "StandardizationValue.Value"
			    + ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
		qp.put("pageSize", "900");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		String delim = "\"";
		String sep = "\t";
		String esc = "\\";
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\desorden\\bkp.metadata.extendida.hola.08.05.2025")))){
			pw.println( workshop.serializeChunk(new String[] {"ID", "TemplateID", "CharacteristicID", "CreationType", "Property", "PropertyValue"}, delim, sep, esc) );
			do {
				qp.put("startIndex", String.valueOf(currentIndex));
				response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
				if(response != null) {
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						pw.println(workshop.serializeChunk(new String[] {
								values.getString(0), values.getString(1),
								values.getString(2), values.getString(3),
								values.getString(4), values.getString(5)}, delim, sep, esc));
					}
				}else {
					System.out.println(workshop.getRawResponse());
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

}
