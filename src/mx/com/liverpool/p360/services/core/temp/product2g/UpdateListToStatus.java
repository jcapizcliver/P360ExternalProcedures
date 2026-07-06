package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class UpdateListToStatus {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"));
		request.put("columns", columns);
		request.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		try(java.io.BufferedReader br  = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\hola\\MissunderstoodIndividualsToDelete")))){
			String line = null;
			while((line = br.readLine()) != null) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + line + "'@1")).put("values", new org.json.JSONArray().put("1025")));
				if(rows.length() == 5000) {
					rw.writeData("list", "Product2G", null, qp, request, System.out::println);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp, request, System.out::println);
		}
	}
	
}
