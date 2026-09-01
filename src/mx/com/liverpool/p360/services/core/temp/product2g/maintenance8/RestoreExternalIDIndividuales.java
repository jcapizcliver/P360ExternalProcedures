package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class RestoreExternalIDIndividuales {

	private static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeFieldsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.ProductNo")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_PIM_MASTER_20260813_205145.csv").toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = line.split(",");
				if(pieces.length == 3) {
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", pieces[0] + "@1")).put("values", new org.json.JSONArray().put(pieces[2])));
					break;
				}
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
}
