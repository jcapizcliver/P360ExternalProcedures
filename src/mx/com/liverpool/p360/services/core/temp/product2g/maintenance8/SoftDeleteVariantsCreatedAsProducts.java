package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class SoftDeleteVariantsCreatedAsProducts {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.ProductNo")), 2000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_PIM_MASTER_20260814_083435.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Individuales_Mkp_P360_To_Go_20260813_201317.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_PIM_MASTER_20260813_195844.csv").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
//				System.out.println(line);
				if(!"".equals(line)) {
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@1")).put("values", new org.json.JSONArray().put("__" + line)));
				}
//				break;
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
