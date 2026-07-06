package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ClearProduct2GEANs {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.EAN")) , 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + arr[0] + "'@1")).put("values", new org.json.JSONArray().put("")));
		});
		parser.parse(java.nio.file.Paths.get(args[0]));
		rh.sendData();
	}
	
}
