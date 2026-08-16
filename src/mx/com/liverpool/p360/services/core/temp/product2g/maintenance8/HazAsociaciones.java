package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class HazAsociaciones {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")), 1000, request -> rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println) );
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_PIM_MASTER_20260810_171855.csv").toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = line.split(",");
				if(pieces.length < 2) {
					System.out.println("? " + line);
				}else {
//					if("1754611668815750".equals(pieces[0]))
						rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[1] + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", pieces[0])).put("values", new org.json.JSONArray().put(pieces[1])));
				}
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
}
