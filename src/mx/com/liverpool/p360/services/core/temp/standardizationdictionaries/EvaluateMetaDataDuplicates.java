package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EvaluateMetaDataDuplicates {

	private static final RESTWrapper rw = new RESTWrapper();
	
	private static void readToDelete() {
		java.util.concurrent.ConcurrentLinkedQueue<java.util.LinkedList<String>> listOfLines = new java.util.concurrent.ConcurrentLinkedQueue<>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "metadata and dup keys5"))){
			stream.parallel().map(s -> rw.getRw().parseLine(s) ).forEach(arr -> listOfLines.add(new java.util.LinkedList<>( java.util.Arrays.asList(arr))) );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		String dp = "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'";
		String[] pieces = null;
		int count = 0;
		java.util.concurrent.ConcurrentLinkedQueue<java.util.LinkedList<String>> toSend = new java.util.concurrent.ConcurrentLinkedQueue<>();
		listOfLines.parallelStream().map(EvaluateMetaDataDuplicates::toDelete).filter(lst -> lst != null);
		for(java.util.LinkedList<String> list : listOfLines) {
			pieces = toDelete(list);
			if(pieces != null) {
				for(int i=0; i<pieces.length; i++) {
					sb.append(sb.length() == 0 ? "" : ",").append("'").append(pieces[i]).append(dp);
					count++;
					if(count == 500) {
						qp.put("items", sb.toString()); 
						rw.deleteData("list", "StandardizationValue", null, "byItems", qp, System.out::println); 
						count = 0;
						sb.setLength(0);
					}
				}
			}
		}
		if(count > 0) {
			qp.put("items", sb.toString()); 
			rw.deleteData("list", "StandardizationValue", null, "byItems", qp, System.out::println); 
			count = 0;
			sb.setLength(0);
		}
	}
	
	public static void main(String[] args) {
//		readToDelete();
//		System.exit(0);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "StandardizationValue.Value"
			    + ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("pageSize", "25000");
		qp.put("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		java.util.Map<String, java.util.LinkedList<String>> valuesToValues = new java.util.TreeMap<>();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			java.util.LinkedList<String> values = null;
			org.json.JSONArray vals = row.getJSONArray("values");
			String key = vals.getString(1) + "<::>" + vals.getString(2) + "<::>" + vals.getString(3) + "<::>" + vals.getString(4);
			values = valuesToValues.get(key);
			if(values == null) {
				values = new java.util.LinkedList<>();
				valuesToValues.put(key, values);
			}
			values.addLast(vals.getString(0));
		}, System.out::println);
		java.util.LinkedList<java.util.Map.Entry<String, java.util.LinkedList<String>>> entries = new java.util.LinkedList<>( valuesToValues.entrySet() );
		java.util.Collections.sort(entries, (o1,o2) -> Integer.compare( o1.getValue().size(), o2.getValue().size()) );
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		StringBuilder sb = new StringBuilder();
		int[] times = new int[1];
		times[0] = 0;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "metadata and dup keys5").toFile())))){
			entries.forEach(entry -> { 
				if(entry.getValue().size() > 1) { 
					pw.println( rw.getRw().serializeChunk(entry.getValue().toArray(new String[] {})) ); 
//					String[] td = toDelete(entry.getValue()); 
//					for(int i=0; i<td.length; i++) { 
//						sb.append(sb.length() == 0 ? "" : ",").append("\"").append(td[i]).append("\""); times[0]++; 
//						if(times[0] % 100 == 0) { 
//							empty.put("identifiers", sb.toString()); 
//							empty.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'"); 
//							rw.deleteData("list", "StandardizationValue", null, "byIdentifiers", empty, System.out::println); sb.setLength(0); 
//						} 
//					} 
				} 
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(times[0] % 100 != 0) { 
//			empty.put("identifiers", sb.toString()); 
//			empty.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'"); 
//			rw.deleteData("list", "StandardizationValue", null, "byIdentifiers", empty, System.out::println); 
//			sb.setLength(0);
		}
		readToDelete();
	}

	private static String[] toDelete(java.util.LinkedList<String> keys) {
		if(keys == null || keys.isEmpty() || keys.size() == 1) {
			return null;
		}
		java.util.Collections.sort(keys, (o1,o2)-> Integer.compare( o2.length(), o1.length() ));
		java.util.LinkedList<String> toDelete = new java.util.LinkedList<>();
		String currentLongest = null;
		for(String a : keys) {
			currentLongest = currentLongest == null ? a : currentLongest.length() < a.length() ? a : currentLongest;
		}
		for(String a : keys) {
			if(!currentLongest.equals(a)) {
				toDelete.add(a);
			}
		}
//		System.out.println("Surviving: " + currentLongest + ",\t\tDeleting: " + toDelete);
		return toDelete.toArray(new String[] {});
	}
	
}
