package mx.com.liverpool.p360.services.core.temp.exports;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class WorkSample {

	private static final RESTWrapper rw = new RESTWrapper();
	
	private String prevC = null;
	private org.json.JSONArray prevV = null;
	private org.json.JSONObject prop = new org.json.JSONObject();
	
	private java.util.Set<String> atributosGeneralesQueSi = new java.util.TreeSet<>();
	
	public static void main(String[] args) {
		WorkSample ws = new WorkSample();
		java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = ws.sample("EU4-59187635");
		propiedadesCaracteristicas.forEach( (k,v) -> System.out.println( k + "<::>" + v ));
//		try(DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
//			
//			@Override
//			public void logE(Exception e) {
//				e.printStackTrace();
//			}
//			
//			@Override
//			public void log(String message) {
//				System.out.println(message);
//			}
//		} )){
//			java.util.Map<String, String> kv = dastub.getLookupValueCodeNameMap("ATGAttributeGroups", 10, true);
//			System.out.println(kv);
//		}
	}
	
	private final java.util.Map<String, org.json.JSONObject> sample(String template) {
		java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = new java.util.HashMap<>();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("lookup", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", 
				  "LookupValue.CreationType->LookupValue.Code equals \"CreateProposal\""
				+ " and LookupValue.StructureGroup->LookupValue.Code equals \"" + template + "\""
				);
		qp.put("fields",
					   "LookupValue.StructureGroup->LookupValue.Code"
					+ ",LookupValue.Characteristic->Characteristic.Identifier"
					+ ",LookupValue.Property->LookupValue.Code"
					+ ",LookupValue.PropertyValue"
					+ ",LookupValue.Characteristic->CharacteristicLang.Name(es)"
					+ ",LookupValue.Characteristic->CharacteristicLang.Description(es)"
					+ ",LookupValue.Characteristic->Characteristic.DataType"
					+ ",LookupValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
					+ ",LookupValue.Characteristic->Characteristic.IsMultiValue"
					+ ",LookupValue.Characteristic->Characteristic.Purposes->LookupValue.Code"
					+ ",LookupValue.Characteristic->Characteristic.Order"
				);
		qp.put("orderBy", "1-ASC");
		rw.collectData("list", "LookupValue", null, "bySearch", qp, characteristicRecords -> {
			for (int i = 0; i < characteristicRecords.length(); i++) {
				org.json.JSONArray values = characteristicRecords.getJSONArray("values");
				String currC = values.getString(1);
				if (prevC != null && !prevC.equals(currC)) {
					prop.put("name", prevV.getString(4));
					prop.put("description", prevV.getString(5));
					prop.put("dataType", prevV.getString(6));
					prop.put("lookup", prevV.getString(7));
					prop.put("isMultiValue", prevV.getString(8));
					prop.put("purposes", prevV.getJSONArray(9));
					prop.put("order", prevV.getString(10));
					propiedadesCaracteristicas.put(prevC, prop);
					if (prop.getJSONArray("purposes").length() == 1
							&& prop.getJSONArray("purposes").getString(0).equals(""))
						prop.getJSONArray("purposes").remove(0);
					if (prop.has("RelevantForATG") && "Y".equals(prop.getString("RelevantForATG")))
						atributosGeneralesQueSi.add(prevC);
					prop = new org.json.JSONObject();
				}
				prop.put(values.getString(2), values.getString(3));
				prevC = currC;
				prevV = values;
			}
		});
		if (prop.length() > 0) {
			prop.put("name", prevV.getString(4));
			prop.put("description", prevV.getString(5));
			prop.put("dataType", prevV.getString(6));
			prop.put("lookup", prevV.getString(7));
			prop.put("isMultiValue", prevV.getString(8));
			propiedadesCaracteristicas.put(prevC, prop);
			if (prop.has("RelevantForATG") && "Y".equals(prop.getString("RelevantForATG")))
				atributosGeneralesQueSi.add(prevC);
			prop = new org.json.JSONObject();
		}
		return propiedadesCaracteristicas;
	}
	
}
