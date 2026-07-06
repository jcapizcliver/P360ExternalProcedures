package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class Compare {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int totalSize = 0;
		int currentIndex = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("pageSize", "900");
		qp.put("query",
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
				+ "StandardizationValue.StructureGroup->LookupValue.Code equals \"EU4-113578\" and (StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\" and not (StandardizationValue.PropertyValue equals \"Atributos\"))"
				);
		qp.put("fields",
				  "StandardizationValue.StructureGroup->LookupValue.Code,"
				+ "StandardizationValue.Characteristic->Characteristic.Identifier,"
				+ "StandardizationValue.Property->LookupValue.Code,"
				+ "StandardizationValue.PropertyValue");

		java.util.Set<String> mep = new java.util.TreeSet<>();
		org.json.JSONArray nv = new org.json.JSONArray();
		org.json.JSONArray nrws = new org.json.JSONArray();
		org.json.JSONObject payload = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		payload.put("columns", columns);
		do {
			qp.put("stardIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				mep.add(values.getString(1));
				nv.put(values.getString(1));
				nv.put(values.getString(2));
				nv.put(values.getString(3));
				nrws.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(1) + "." + values.getString(2) + "'@'MetadatosPlantillaCaracteristicaGlobales'")).put("values", nv));
				nv = new org.json.JSONArray();
			}
//			payload.put("rows", nrws);
//			System.out.println( workshop.makeRequest("POST", "/list/StandardizationValue", qp, payload.toString()) );
//			nrws = new org.json.JSONArray();
		}while(currentIndex < totalSize);
		currentIndex = 0;
		int a = 0;
		StringBuilder sb = new StringBuilder();
		nrws = new org.json.JSONArray();
		for(String characteristic : mep) {
			sb.append(a == 0 ? "" : ",").append("\"").append(characteristic).append("\"");
			a++;
			if(a % 15 == 0) {
				qp.put("query",
						  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
						+ "StandardizationValue.StructureGroup->LookupValue.Code equals \"EU4-113578\" and StandardizationValue.Characteristic->Characteristic.Identifier in (" + sb.toString() + ")  and "
						+ "not StandardizationValue.PropertyValue equals \"Atributos\""
						);
				do {
					qp.put("startIndex", String.valueOf(currentIndex));
					response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
					System.out.println("Response: " + response.toString());
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					System.out.println("<::>" + response + "<::>");
					for(int i=0; i<rows.length(); i++) {
						currentIndex ++;
						values = rows.getJSONObject(i).getJSONArray("rows");
						nv.put(values.getString(1));
						nv.put(values.getString(2));
						nv.put(values.getString(3));
						nrws.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(1) + "." + values.getString(2) + "'@'MetadatosPlantillaCaracteristicaGlobales'")).put("values", nv));
						nv = new org.json.JSONArray();
					}
					payload.put("rows", nrws);
					System.out.println( workshop.makeRequest("POST", "/list/StandardizationValue", qp, payload.toString()) );
					nrws = new org.json.JSONArray();
				}while(currentIndex < totalSize);
				currentIndex = 0;
				sb.setLength(0);
			}
		}
		if(a % 15 != 0) {
			System.out.println("Last run: " + sb.toString());
			qp.put("query",
					  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
					+ "StandardizationValue.StructureGroup->LookupValue.Code equals \"EU4-113578\" and StandardizationValue.Characteristic->Characteristic.Identifier in (" + sb.toString() + ")  and "
					+ "not StandardizationValue.PropertyValue equals \"Atributos\""
					);
			do {
				qp.put("startIndex", String.valueOf(currentIndex));
				response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
				System.out.println("Response: " + response.toString());
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex ++;
					values = rows.getJSONObject(i).getJSONArray("rows");
					nv.put(values.getString(1));
					nv.put(values.getString(2));
					nv.put(values.getString(3));
					nrws.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(1) + "." + values.getString(2) + "'@'MetadatosPlantillaCaracteristicaGlobales'")).put("values", nv));
					nv = new org.json.JSONArray();
				}
				payload.put("rows", nrws);
				System.out.println( workshop.makeRequest("POST", "/list/StandardizationValue", qp, payload.toString()) );
				nrws = new org.json.JSONArray();
			}while(currentIndex < totalSize);
			currentIndex = 0;
			sb.setLength(0);
		}
	}

}
