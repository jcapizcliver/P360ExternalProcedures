package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CopyLookupReferences {

	public static void main(String[] args) {
		CopyLookupReferences cl = new CopyLookupReferences();
		cl.moveLookup("Characteristics");
	}
	
	private void moveLookup(String lookup) {
		RESTWrapper rww = new RESTWrapper();
		rww.getRw().setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rww.getRw().addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("https://webctep360dev.liverpool.com.mx/rest/V2.0");
		rw.getRw().addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "LookupValueReference.Lookup->Lookup.Identifier"
				+ ",LookupValueReference.LookupValues->LookupValue.Code"
				);
		qp.put("lookup", "'" + lookup + "'");
		qp.put("includeLabels", "true");
		qp.put("sort", "0-ASC;1-ASC");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues"));
		request.put("columns", columns);
		request.put("rows", rows);
		java.util.Map<String, java.util.Map<String, org.json.JSONArray>> cosa = new java.util.TreeMap<>();
		
		rw.collectData("list", "LookupValue", "LookupValueReference", "byLookup", qp, row -> 
			{
				System.out.println(row);
				java.util.Map<String, org.json.JSONArray> inner = cosa.get(row.getJSONObject("object").getString("label"));
				if(inner == null) {
					inner = new java.util.TreeMap<>();
					cosa.put(row.getJSONObject("object").getString("label"), inner);
				}
				org.json.JSONArray arr = inner.get(row.getJSONArray("values").getString(0));
				if(arr == null) {
					arr = new org.json.JSONArray();
					inner.put(row.getJSONArray("values").getString(0), arr);
				}
				arr.put(row.getJSONArray("values").getJSONArray(1).getString(0));
			}
		, System.out::println);
		System.out.println("Now trying...");
		for(java.util.Map.Entry<String, java.util.Map<String, org.json.JSONArray>> entry : cosa.entrySet()) {
			for(java.util.Map.Entry<String, org.json.JSONArray> entry2 : entry.getValue().entrySet()) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'" + lookup + "'")).put("qualification", new org.json.JSONObject().put("refLookup", new org.json.JSONObject().put("id", "'" + entry2.getKey() + "'"))).put("values", new org.json.JSONArray().put(entry2.getValue())));
				if(rows.length() == 100) {
					rww.writeData("list", "LookupValue", "LookupValueReference", new java.util.TreeMap<>(), request, System.out::println);
				}
			}
		}
		if(rows.length() > 0) {
			rww.writeData("list", "LookupValue", "LookupValueReference", new java.util.TreeMap<>(), request, System.out::println);
		}
		
	}
	
}
