package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class StageStandardizationValues {

	private static final RESTWrapper RW = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code");
		qp.put("lookup", "'NoStageableDictionaries'");
		qp.put("pageSize", "500");
		java.util.Set<String> forbidden = new java.util.TreeSet<>();
		RW.collectData("list", "LookupValue", null, "byLookup", qp, row -> forbidden.add(row.getJSONArray("values").getString(0)), System.out::println);
		
		qp.clear();
		qp.put("fields", "StandardizationDictionary.Identifier");
		qp.put("query", "not StandardizationDictionary.Identifier is empty");
		qp.put("pageSize", "500");
		java.util.LinkedList<String> stdIds = new java.util.LinkedList<>();
		RW.collectData("list", "StandardizationDictionary", null, "bySearch", qp, row ->{ if(!forbidden.contains(row.getJSONArray("values").getString(0))) stdIds.addLast(row.getJSONArray("values").getString(0)); }, System.out::println);
		
		qp.clear();
		qp.put("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		qp.put("pageSize", "600");
		for(String id : stdIds) {
			qp.put("dictionary", "'" + id + "'");
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", id.replaceAll("/", "<::>")).toString()), java.nio.charset.StandardCharsets.UTF_8))){
				RW.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> pw.println( RW.getRw().serializeChunk(new String[] { row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1) }, "\"", ";", "\\") ), System.out::println);
			}catch(java.io.IOException e) { 
				e.printStackTrace(); 
			}
			
		}
	}
	
}
