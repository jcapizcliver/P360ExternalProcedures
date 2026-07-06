package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CompareDataFromFileAndTables {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		org.json.JSONArray identifier = null;
		java.util.LinkedList<String> identifiers = new java.util.LinkedList<>();
		CompareDataFromFileAndTables cdffat = new CompareDataFromFileAndTables();
		java.util.concurrent.ConcurrentLinkedQueue<org.json.JSONObject> jsonElements = new java.util.concurrent.ConcurrentLinkedQueue<>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "samples", "deleted_data_from_today"))){
			System.out.println("Start to ");
			stream.parallel().map( cdffat::removeLogParts ).map( cdffat::toJSONObject ).filter( j -> j != null).forEach(jsonElements::add);;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		for(org.json.JSONObject j : jsonElements) {
			identifier = j.getJSONObject("entityItemsDeleted").getJSONArray("_identifier");
			for(int i = 0; i<identifier.length(); i++) {
				identifiers.addLast(identifier.getString(i));
			}
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Value");
		qp.put("pageSize", "20000");
		qp.put("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		java.util.LinkedList<String> elements = new java.util.LinkedList<>();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> elements.addLast(row.getJSONArray("values").getString(0)), System.out::println);
		java.util.Set<String> es = new java.util.TreeSet<>(elements);
		java.util.LinkedList<String> estosNoEstán = new java.util.LinkedList<>();
		for(String id : identifiers) {
			if(!es.contains(id)) {
				estosNoEstán.addLast(id);
			}
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "samples", "ids_currently_not_in_p360").toFile())))){
			estosNoEstán.forEach(pw::println);
		}catch(java.io.IOException e) {
			
		}
	}
	
	private String removeLogParts(String entry) {
		return entry.replaceFirst(".+ A message body: ", "");
	}
	
	private org.json.JSONObject toJSONObject(String line){
		
		try {
			return new org.json.JSONObject(line);
		}catch(org.json.JSONException e) {
			System.out.println(line);
			System.exit(1);
		}
		return null;
		
	}
	
}
