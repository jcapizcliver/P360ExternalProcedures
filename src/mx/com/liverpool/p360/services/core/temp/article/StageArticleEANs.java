package mx.com.liverpool.p360.services.core.temp.article;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class StageArticleEANs {

	
	public static void main(String[] args) {
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
				+ ",ArticleCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValueLang.Name(es)"
				);
		qp.put("pageSize", "1200");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.article_ean_file")).toString())))){
			rw.collectData("list", "Article", null, "byCatalog", qp, row ->
				{
					org.json.JSONArray values = row.getJSONArray("values");
					String mainBarCode = !"".equals(values.getJSONArray(1).getString(0)) ? values.getJSONArray(1).getString(0) : !"".equals(values.getJSONArray(2).getString(0)) ? values.getJSONArray(2).getString(0) : null;
					if(mainBarCode != null) {
						pw.println( rw.getRw().serializeChunk(new String[] { mainBarCode, values.getString(0), values.getJSONArray(3).getString(0) }, "\"", ";", "\\") );
					}
				}
			, System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
