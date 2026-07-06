package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ReponSKUsArticulosLVP {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("pageSize", "1000");
		qp.put("query", "Article.SupplierAID startsWith \"LVP\" and characteristic('SKU') is empty");
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put( "identifier", "ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)")).put(new org.json.JSONObject().put("identifier", "Article.SKU")), 1000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::println));
		rw.collectData("list", "Article", null, "bySearch", qp, row ->{
			org.json.JSONArray values = row.getJSONArray("values");
			String sku = values.getString(0).replaceAll("^LVP", "");
			rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(sku)));
		} );
		rh.sendData();
	}
	
}
