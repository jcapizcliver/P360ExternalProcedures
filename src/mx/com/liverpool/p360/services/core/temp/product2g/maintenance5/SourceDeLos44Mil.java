package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class SourceDeLos44Mil {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Source_de_los_44_mil.txt").toFile())))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus");
			qp.put("pageSize", "1000");
			qp.put("formatData", "true");
			int a = 0;
			java.util.List<String> toMove = new java.util.ArrayList<>();
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "EstatusGen_44_mil_07_May_2026.txt").toFile())))){
				while((line = br.readLine()) != null) {
					sb.append(sb.length() == 0 ? "" : ",").append("'").append(line).append("'@1");
					a++;
					if(a == 1000) {
						qp.put("items", sb.toString());
						rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
							org.json.JSONArray values = row.getJSONArray("values");
							pw.println(values);
							if(!"Eliminada".equals(values.getString(1)) && !"Aprobada".equals(values.getString(1))) {
								toMove.add(values.getString(0));
							}
						});
						a = 0;
						sb.setLength(0);
					}
				}
				if(a > 0) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						pw.println(values);
						if(!"Eliminada".equals(values.getString(1)) && !"Aprobada".equals(values.getString(1))) {
							toMove.add(values.getString(0));
						}
					});
					a = 0;
					sb.setLength(0);
				}
				toMove.forEach(System.out::println);
				java.util.Map<String, String> qp0 = new java.util.HashMap<>();
				RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 2000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
				toMove.forEach(b -> rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + b + "'@1")).put("values", new org.json.JSONArray().put("Aprobada"))));
				rh.sendData();
			}
		}catch(java.io.IOException e) {
			
		}
		
	}
	
}
