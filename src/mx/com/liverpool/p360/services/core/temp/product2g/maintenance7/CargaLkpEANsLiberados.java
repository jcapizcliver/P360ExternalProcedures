package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CargaLkpEANsLiberados {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "EANsLiberados.csv").toFile())))){
			String line = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValue.Code")), 1000, request -> rw.writeData("list", "LookupValue", null, qp, request, System.out::println) );
			while((line = br.readLine()) != null) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@'EANsLiberados'")).put("values", new org.json.JSONArray().put(line)));
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
