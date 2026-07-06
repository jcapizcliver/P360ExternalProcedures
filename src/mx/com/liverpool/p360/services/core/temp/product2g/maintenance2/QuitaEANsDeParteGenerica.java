package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class QuitaEANsDeParteGenerica {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("query", "Product2GLog.CreationDate(PIM) >= 2026-03-24T18:49:00");
		rw.deleteData("list", "Product2G", null, "bySearch", qp0, System.out::println);
		System.exit(0);
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.EAN")), 10000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile().getName())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				if("1100".equals(pieces[2])) {
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[1] + "'@1")).put("values", new org.json.JSONArray().put("")));
				}
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
