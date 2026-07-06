package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class DetachProduct2GEAN {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(DetachProduct2GEAN.class.getName());
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.EAN")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, log::info) );
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			String[] pieces = null;
//			DataRequestor dr = new DataRequestor();
//			org.json.JSONArray items = new org.json.JSONArray();
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
//				items.put(pieces[1]);
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put("")));
//				if(items.length() == 2000) {
//					log.info( dr.retiraEANProductNo(items) );
//					while(items.length() > 0) {
//						items.remove(0);
//					}
//				}
			}
			rh.sendData();
//			if(items.length() > 0) {
//				log.info( dr.retiraEANProductNo(items) );
//				while(items.length() > 0) {
//					items.remove(0);
//				}
//			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
