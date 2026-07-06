package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class VeSiTengoEsosSKUs {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "PROD", "Source1").toFile())))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			int a = 0;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("fields", "Article.SupplierAID,Article.SKU");
			qp.put("pageSize", "1000");
			java.util.Map<String, String> data = new java.util.HashMap<>();
			java.util.List<String> skus = new java.util.ArrayList<>();
			while((line = br.readLine()) != null) {
				skus.add(line);
				sb.append(sb.length() > 0 ? ",":"").append(line);
				a++;
				if(a % 1000 == 0) {
					qp.put("query", "Article.SKU in (" + sb.toString() + ")");
					rw.collectData("list", "Article", null, "bySearch", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						data.put(values.getString(1), values.getString(0));
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("query", "Article.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Article", null, "bySearch", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					data.put(values.getString(1), values.getString(0));
				});
				sb.setLength(0);
			}
			java.util.List<String> missings = new java.util.ArrayList<>();
			for(String sku : skus) {
				if(!data.containsKey(sku)) {
					missings.add(sku);
				}
			}
			System.out.println("Missing skus: ");
			missings.forEach(System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
