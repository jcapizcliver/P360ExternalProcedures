package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class StageItemGroupRels {

 	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		processRels("MATKLLOV", java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.itemgroup_valid_values")).toString());
		processRels("MATKLLOV_S4H", java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.itemgroup_valid_values_s4h")).toString());
	}
	
	private static void processRels(String lookup, String targetFile) {
		java.util.Map<String, String> data = internalToExternalIds(lookup);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValueReference.Lookup->Lookup.Identifier,LookupValueReference.LookupValues->LookupValue.Code");
		qp.put("lookup", "'" + lookup + "'");
		qp.put("pageSize", "1500");
		java.util.Map<String, java.util.LinkedList<String>> aggregation = new java.util.TreeMap<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(targetFile)))){
			rw.collectData("list", "LookupValue", "LookupValueReference", "byLookup", qp, row ->{
				org.json.JSONArray values = row.getJSONArray("values");
				String id = data.get( row.getJSONObject("object").getString("id") );
				String lookupId = values.getString(0);
				String key = id + "<::>" + lookupId;
				java.util.LinkedList<String> lst = aggregation.get(key);
				if(lst == null) {
					lst = new java.util.LinkedList<>();
					aggregation.put(key, lst);
				}
				org.json.JSONArray refValues = values.getJSONArray(1);
				for(int i=0; i<refValues.length(); i++)
					lst.addLast(refValues.getString(i));
			}, System.out::println);
			String[] pieces = null;
			for(java.util.Map.Entry<String, java.util.LinkedList<String>> entry : aggregation.entrySet()) {
				pieces = entry.getKey().split("<::>");
				pw.println( rw.getRw().serializeChunk(new String[] { pieces[0], pieces[1], rw.getRw().serializeChunk(entry.getValue().toArray(new String[] {}), "\"", ",", "\\") }, "\"", ";", "\\") );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static java.util.Map<String, String> internalToExternalIds(String lookup){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",   "LookupValue.Code");
		qp.put("pageSize", "1500");
		qp.put("lookup", "'" + lookup + "'");
		rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> data.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0)));
		return data;
	}
	
}
