package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ActualizaSAPObjectType {


	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("includeObjectsInProtocol", "false");
				RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SAPObjectType(MX)")).put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)")), 200, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
			while((line = br.readLine()) != null) {
				
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}


}
