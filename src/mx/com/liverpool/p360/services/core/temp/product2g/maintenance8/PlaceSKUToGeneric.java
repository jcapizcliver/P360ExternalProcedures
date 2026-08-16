package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class PlaceSKUToGeneric {

	private static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_PIM_MASTER_20260812_174010.csv").toFile())))){
			String line = br.readLine();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("includeObjectsInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.SKU")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
			while((line = br.readLine()) != null) {
//				System.out.println(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@1")).put("values", new org.json.JSONArray().put(line.substring(3))));
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@1")).put("values", new org.json.JSONArray().put(line.substring(3))));
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
