package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ColocaVariantesEnEliminadaQuitaSkuYEan {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			int a=0;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("pageSize", "6000");
			java.util.Map<String, String> qp0 = new java.util.HashMap<>();
			qp0.put("includeObjectsInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus")).put(new org.json.JSONObject().put("identifier", "Article.SKU")).put(new org.json.JSONObject().put("identifier", "Article.EAN")).put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)")).put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)")).put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)")), 1000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::println) );
			while((line = br.readLine()) != null) {
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(line).append("'@1");
				a++;
				if(a % 1000 == 0) {
					qp.put("products", sb.toString());
					rw.collectData("list", "Article", null, "byProducts", qp, row -> {
						rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("1025").put("").put("").put("").put("").put("")));
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp, row -> {
					rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("1025").put("").put("").put("").put("").put("")));
				});
				sb.setLength(0);
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
