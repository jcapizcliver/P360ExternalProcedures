package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class RestoreVariantDataSKUEAN {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "true");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")).put(new org.json.JSONObject().put("identifier", "Product2G.SKU")).put(new org.json.JSONObject().put("identifier", "Product2G.EAN")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', '\t', null, "\n\r", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length == 0) {
				return;
			}
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[0] + "'@1")).put("values", new org.json.JSONArray().put("1021").put(row[14]).put(row[13])));
		} );
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ToRestoreData.csv"));
		rh.sendData();
	}
	
}
