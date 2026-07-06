package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ArticleDataCollector {

	private static final RESTWrapper rw = new RESTWrapper();
//	private static final long init = System.currentTimeMillis();
	
	public static void main(String[] args) {
		int size = 50000;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("pageSize", String.valueOf(size));
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "CurrentArticleIDs.txt").toFile())))){
			rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
				pw.println(row.getJSONObject("object").getString("id"));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
