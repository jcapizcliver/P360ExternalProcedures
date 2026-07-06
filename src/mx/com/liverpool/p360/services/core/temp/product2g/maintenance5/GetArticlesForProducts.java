package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class GetArticlesForProducts {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> data = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDs2.csv").toFile())))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			int a = 0;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("pageSize", "5000");
			qp.put("fields", "ProductReference.ReferencedSupplierAid");
			while((line = br.readLine()) != null) {
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(line).append("'@1");
				a++;
				if(a == 1000) {
					qp.put("products", sb.toString());
					rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						data.put(row.getJSONObject("object").getString("id"), values.getString(0));
					});
					sb.setLength(0);
					a = 0;
				}
			}
			if(a > 0) {
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					data.put(row.getJSONObject("object").getString("id"), values.getString(0));
				});
				sb.setLength(0);
				a = 0;
			}
			System.out.println("Totales: " + data.size());
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Hola.txt").toFile())))){
				data.entrySet().forEach( entry -> pw.println( rw.getRw().serializeChunk( new Object[] { entry.getKey() } ) ) );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
