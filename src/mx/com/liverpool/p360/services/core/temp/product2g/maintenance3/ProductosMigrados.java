package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ProductosMigrados {

	private static final RESTWrapper rw = new RESTWrapper();

	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		
//		RequestHandler rh = new RequestHandler(
//				new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.Migrado"))
//				, 20000
//				, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		
		RequestHandler rhA = new RequestHandler(
				new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.Migrado"))
				, 20000
				, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
		
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("PasQueSí").toFile())))){
//			String line = null;
//			while((line = br.readLine()) != null) {
//				if(!"".equals(line)) {
//					rh.addRow( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@1")).put("values", new org.json.JSONArray().put("true")) );
//				}
//			}
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("HijQueSí").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					rhA.addRow( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@1")).put("values", new org.json.JSONArray().put("true")) );
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
//		rh.sendData();
		rhA.sendData();
		/*
		 *
		 * 	Lees archivo, recolectas de a 1000 en 1000, defines el qp items = sb.toString(), clear...
		 * 
		 **/
//		rw.deleteData("list", "Product2G", null, "byItems", qp, null);
		
	}
	
}
