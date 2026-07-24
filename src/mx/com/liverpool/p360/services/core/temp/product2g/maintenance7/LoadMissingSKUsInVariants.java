package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class LoadMissingSKUsInVariants {

	
	private static final RESTWrapper rw = new RESTWrapper();
	private static int a = 0;
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SKU")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			
			a++;
			if(row.length == 0) {
				return;
			}
			
			if(a == 1) {
				return;
			}
			
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row + "'@1")).put("values", new org.json.JSONArray().put(row[0].substring(3))));
			
		});
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SBB no SKU vars_20260723_105701.csv"));
		rh.sendData();
		
	}
	
}
