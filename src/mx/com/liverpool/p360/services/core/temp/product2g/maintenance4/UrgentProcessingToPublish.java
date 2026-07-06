package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class UrgentProcessingToPublish {

	public static final String URGENT = "URGENT2";
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GLang.Remarks(en)")), 2000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDs.txt").toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(URGENT)));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		rh.sendData();
		
	}
	
}
