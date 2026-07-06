package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ArticleDataCollectorTest {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				   "ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico')->LookupValue.Code"
				+ ",ArticleCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber',-1)"
				+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
				+ ",ArticleCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProcedeNoProcede',root,\"0000.0000.RK\",'ProcedeNoProcede',-1)"
				);
		qp.put("pageSize", "10000");
		StringBuilder sb = new StringBuilder();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "CurrentArticleIDs.txt").toFile())))){
			String line = null;
			int count = 0;
			while((line = br.readLine()) != null) {
				sb.append(sb.length() == 0 ? "" : ",").append(line);
				count++;
				if(count == 5000)
					break;
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Collecting...");
		qp.put("items", sb.toString());
		rw.collectData("list", "Article", null, "byItems", qp, row -> System.out.println(row.getJSONArray("values")));
	}
	
	

}
