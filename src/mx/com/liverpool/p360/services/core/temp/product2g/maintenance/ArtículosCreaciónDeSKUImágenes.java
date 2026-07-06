package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ArtículosCreaciónDeSKUImágenes {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImageDetail_URL',-1)"
			);
		qp.put("query", "Article.CurrentStatus = \"Creación de SKU\"");
		qp.put("pageSize", "10000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "stage", "cache", "SKUCreationWithImages").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			rw.collectData("list", "Article", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getJSONArray(1).getString(0)) || !"".equals(values.getJSONArray(2).getString(0)))
					pw.println( values.getString(0) );
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
