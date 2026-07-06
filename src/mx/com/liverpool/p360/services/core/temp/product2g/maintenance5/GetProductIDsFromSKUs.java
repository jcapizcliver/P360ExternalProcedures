package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class GetProductIDsFromSKUs {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> data = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUs.txt").toFile())))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			int a = 0;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("pageSize", "5000");
			qp.put("fields", "Product2G.ProductNo,Product2G.SKU");
			while((line = br.readLine()) != null) {
				sb.append(sb.length() == 0 ? "" : ",").append(line);
				a++;
				if(a == 1000) {
					qp.put("query", "Product2G.SKU in (" + sb.toString() + ")");
					rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						data.put(values.getString(0), values.getString(1));
					});
					sb.setLength(0);
					a = 0;
				}
			}
			if(a > 0) {
				qp.put("query", "Product2G.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					data.put(values.getString(0), values.getString(1));
				});
				sb.setLength(0);
				a = 0;
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDs.txt").toFile())))){
				data.entrySet().forEach( entry -> pw.println( rw.getRw().serializeChunk( new Object[] { entry.getKey() } ) ) );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
