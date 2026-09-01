package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ProposalIDSku {

	
	public static void main(String[] args) {
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ProposalId_SKU.csv").toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("includeObjectsInProtocol", "false");
			RESTWrapper rw = new RESTWrapper();
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.SKU")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
			while((line = br.readLine()) != null) {
				pieces = line.split(",");
				if(pieces.length == 2) {
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(pieces[1])));
				}
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
