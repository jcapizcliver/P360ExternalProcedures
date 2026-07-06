package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ProveedoresMigrados {

	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.List<String> proveedoresMigrados = new java.util.ArrayList<>();
		qp.put("fields", "LookupValueReference.LookupValues(Party)->LookupValue.Code");
		qp.put("query",  "LookupValue.Code = \"Migrado\"");
		qp.put("lookup", "'PartyClassification'");
		RESTWrapper rw = new RESTWrapper();
		rw.collectData("list", "LookupValue", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(!"".equals(values.getJSONArray(0).getString(0))) {
				proveedoresMigrados.add(values.getJSONArray(0).getString(0));
			}
		});
		System.out.println("Proveedores migrados: " + proveedoresMigrados.size());
	}
	
}