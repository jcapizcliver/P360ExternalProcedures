package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SKUsDeADosOMás {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		Object[][] mesh = new Object[18000000][];
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,SimpleProduct2GCharacteristicValueLang.Value('SKU',-1)");
		qp.put("pageSize", "50000");
		int[] index = new int[1];
		index[0] = 0;
		java.nio.file.Path p = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsProdArticle");
		if(!java.nio.file.Files.exists(p)) {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(p.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
					org.json.JSONArray values = new org.json.JSONArray();
					mesh[index[0]] = new Object[] { values.getJSONArray(1).getString(0), 'P' };
					index[0] ++;
				});
				qp.put("fields", "Article.SupplierAID,SimpleArticleCharacteristicValueLang.Value('SKU',-1)");
				rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
					org.json.JSONArray values = new org.json.JSONArray();
					mesh[index[0]] = new Object[] { values.getJSONArray(1).getString(0), 'A' };
					index[0] ++;
				});
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(p.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				while((line = br.readLine()) != null) {
					
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		Object[][] m2 = java.util.Arrays.copyOf(mesh, index[0]);
		java.util.Arrays.sort(m2, (o1,o2) -> ((String)o1[0]).compareTo((String)o2[0]) );
		
	}
	
}
