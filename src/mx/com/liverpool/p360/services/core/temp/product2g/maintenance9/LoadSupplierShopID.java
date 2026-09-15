package mx.com.liverpool.p360.services.core.temp.product2g.maintenance9;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class LoadSupplierShopID {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('supplierShopId',root,\"0000.0000.RK\",'supplierShopId',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println ) );
		try( java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Solo_ShopID_Vacios_STEP").toFile()))) ){
//		try( java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDsSTEP_shopID.csv").toFile()))) ){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = line.split("\\|");
//				pieces = line.split("\t");
				if(pieces.length == 5)
					System.out.println(pieces[0]);
//				if(pieces.length == 5)
//					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(pieces[4])));
			}
//			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
