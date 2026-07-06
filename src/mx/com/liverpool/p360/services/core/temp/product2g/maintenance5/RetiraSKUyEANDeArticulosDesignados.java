package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class RetiraSKUyEANDeArticulosDesignados {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("includeObjectsInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SKU")).put(new org.json.JSONObject().put("identifier", "Article.EAN")), 2000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					rh.addRow( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@1")).put("values", new org.json.JSONArray().put("").put("")) );
				}
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
