package mx.com.liverpool.p360.services.core.temp.xml;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CompareMissingIDs {

	
	public static void main(String[] args) {
		java.util.LinkedList<String> ids = new java.util.LinkedList<>();
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("pageSize", "50000");
		qp.put("catalog", "1");
		qp.put("fields", "Product2G.ProductNo");
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> ids.addLast(row.getJSONArray("values").getString(0)), System.out::println);
		qp.put("fields", "Article.SupplierAID");
		rw.collectData("list", "Article", null, "byCatalog", qp, row -> ids.addLast(row.getJSONArray("values").getString(0)), System.out::println);
		java.util.Set<String> idSet = new java.util.TreeSet<>(ids);
		java.util.List<String> missings = null;
		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get(args[0]))){
			missings = lines.filter(s -> !idSet.contains(s)).toList();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(missings != null) {
			missings.forEach(System.out::println);
		}
	}
	
}
