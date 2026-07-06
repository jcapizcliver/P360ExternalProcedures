package mx.com.liverpool.p360.services.core.temp.article;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ExportaImagenes {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		sb.append("Article.SupplierAID");
		sb.append(",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		sb.append(",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_Name',-1)");
		for(int i=0; i<100; i++) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.00" + (i< 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i< 10 ? "0" + i : i) + ".RK\",'ProductImageDetail_Name',-1)");
		}
		for(int i=0; i<100; i++) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("ArticleCharacteristicValueLang.Value('Illustration',\"0000.00" + (i< 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i< 10 ? "0" + i : i) + ".RK\",'Illustration_Name',-1)");
		}
		for(int i=0; i<100; i++) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("ArticleCharacteristicValueLang.Value('ProductImageSmosh',\"0000.00" + (i< 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i< 10 ? "0" + i : i) + ".RK\",'ProductImageSmosh_Name',-1)");
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				sb.toString()
//				   "ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImageDetail_Name',-1)"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0001.RK\",\"0000.0001.RK\",'ProductImageDetail_Name',-1)"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0002.RK\",\"0000.0002.RK\",'ProductImageDetail_Name',-1)"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0003.RK\",\"0000.0003.RK\",'ProductImageDetail_Name',-1)"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0004.RK\",\"0000.0004.RK\",'ProductImageDetail_Name',-1)"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0005.RK\",\"0000.0005.RK\",'ProductImageDetail_Name',-1)"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0006.RK\",\"0000.0006.RK\",'ProductImageDetail_Name',-1)"
			);
//		qp.put("items",  "'1698767481581996'@1");
		qp.put("pageSize", "10000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "DEV", "NombresImagenes.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println( rw.getRw().serializeChunk(new Object[] { "ItemID", "SKU", "ProductImageName", "ProductImageDetail", "Illustration", "ProductImageSmosh" }) );
			rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				String id = values.getString(0);
				String sku = values.getJSONArray(1).getString(0);
				String productImageName = values.getJSONArray(2).getString(0);
				java.util.List<String> nombreDetalle = new java.util.ArrayList<>();
				java.util.List<String> nombreIlustración = new java.util.ArrayList<>();
				java.util.List<String> nombreSmosh = new java.util.ArrayList<>();
				for(int i=2; i<values.length(); i++) {
					if(i <= 101) {
						if(!"".equals(values.getJSONArray(i).getString(0))) {
							nombreDetalle.add(values.getJSONArray(i).getString(0));
						}
					}else if(i <= 201) {
						if(!"".equals(values.getJSONArray(i).getString(0))) {
							nombreIlustración.add(values.getJSONArray(i).getString(0));
						}
					}else if(i<=301) {
						if(!"".equals(values.getJSONArray(i).getString(0))) {
							nombreSmosh.add(values.getJSONArray(i).getString(0));
						}
					}
				}
				if(!nombreDetalle.isEmpty() || !nombreIlustración.isEmpty() || !nombreSmosh.isEmpty() || !"".equals(productImageName)) {
					String del = "\"";
					String sep = ";";
					String esc = "\\";
					pw.println( rw.getRw().serializeChunk(new Object[] { id, sku, productImageName, rw.getRw().serializeChunk(nombreDetalle.toArray(new String[] {}), del, sep, esc), rw.getRw().serializeChunk(nombreIlustración.toArray(new String[] {}), del, sep, esc), rw.getRw().serializeChunk(nombreSmosh.toArray(new String[] {}), del, sep, esc)  }) );
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
