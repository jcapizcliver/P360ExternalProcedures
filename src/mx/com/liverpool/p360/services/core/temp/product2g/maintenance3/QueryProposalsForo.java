package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QueryProposalsForo {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("http://localhost:8080/process-engine/public/rt/");
		rw.getRw().removeHeader("Authorization");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("Source1").toFile())))){
			String line = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			while((line = br.readLine()) != null) {
				rw.getRw().makeRequest("POST", "GetProposalsForo", qp, new org.json.JSONObject().put("products", new org.json.JSONArray().put(new org.json.JSONObject().put("sku", line))).toString());
				System.out.println(line + " - " + (new org.json.JSONArray( rw.getRw().getRawResponse()).length() == 0 ? "No" : "Ya está" ));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
