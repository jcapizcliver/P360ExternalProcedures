package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class RestoreColoresForMissings {

	
	public static void main(String[] args) {
		int a = 0;
		int b = 0;
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RESTWrapper rw = new RESTWrapper();
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
		java.util.Map<String, String> vars = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("c:", "opt", "LVP", "desorden", "PROD", "LosColores.csv").toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = line.split(";");
				if(pieces.length > 1)
					vars.put(pieces[0], pieces[1]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("c:", "opt", "LVP", "desorden", "PROD", "Product With Item IDs (7).csv").toFile())))){
			String line = br.readLine();
			String color = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				b++;
				pieces = line.split(",");
				color = vars.get(pieces[1]);
				if(color == null) {
					a++;
					System.out.println(line);
				}else {
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[1] + "'@1")).put("values", new org.json.JSONArray().put(color)));
				}
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Not found: " + a + "/" + b);
	}
	
}
