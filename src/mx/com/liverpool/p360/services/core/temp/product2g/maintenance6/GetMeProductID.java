package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import java.io.PrintWriter;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class GetMeProductID {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> iids = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUs variantes a analizar.txt").toFile())))){
			String line = null;
			int a = 0;
			StringBuilder sb = new StringBuilder();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("pageSize", "6000");
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					a++;
					sb.append(sb.length() == 0 ? "" : ",").append(line);
					if(a % 1000 == 0) {
						qp.put("query", "Article.SKU in (" + sb.toString() + ")");
						rw.collectData("list", "Article", null, "bySearch", qp, row -> {
							iids.add(row.getJSONObject("object").getString("id"));
						});
						sb.setLength(0);
					}
				}
			}
			if(sb.length() > 0) {
				qp.put("query", "Article.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Article", null, "bySearch", qp, row -> {
					iids.add(row.getJSONObject("object").getString("id"));
				});
				sb.setLength(0);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

		java.util.Set<String> pids = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "6000");
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		int a = 0;
		StringBuilder sb = new StringBuilder();
		for(String iid : iids) {
			sb.append(sb.length() == 0 ? "" : ",").append(iid);
			a++;
			if(a % 1000 == 0) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
					pids.add(row.getJSONArray("values").getString(0));
				});
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("items", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
				pids.add(row.getJSONArray("values").getString(0));
			});
			sb.setLength(0);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "PIDs.csv").toFile())))){
			pids.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
