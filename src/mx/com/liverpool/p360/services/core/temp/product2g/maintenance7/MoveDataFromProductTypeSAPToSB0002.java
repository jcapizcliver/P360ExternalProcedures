package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class MoveDataFromProductTypeSAPToSB0002 {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	private static int count = 0;
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SB_0002',root,\"0000.0000.RK\",'SB_0002',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', null, "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			count++;
			if(count == 1) {
				
				return;
			}
			if(row.length == 0) {
				
				return;
			}
			
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[1] + "'@1")).put("values", new org.json.JSONArray().put(row[2])));
		} );
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ProductTypeSAP_SBB_20260721_155916.csv"));
		rh.sendData();
	}
	
}
