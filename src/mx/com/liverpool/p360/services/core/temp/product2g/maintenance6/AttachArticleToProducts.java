package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class AttachArticleToProducts {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Variante_Padre_Asociar.csv").toFile())))){
			String line = null;
			String[] pieces = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("includeObjectInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")), 1000, request -> rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println) );
			while((line = br.readLine()) != null) {
				pieces = line.split("\t");
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", pieces[1])).put("values", new org.json.JSONArray().put(pieces[1])));
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
