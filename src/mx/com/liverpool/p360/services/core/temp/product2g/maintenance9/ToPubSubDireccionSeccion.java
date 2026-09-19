package mx.com.liverpool.p360.services.core.temp.product2g.maintenance9;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ToPubSubDireccionSeccion {

	private static final RESTWrapper rw = new RESTWrapper();
	
	private static final PubSubGCP pubPostProducts = new PubSubGCP(
		    PropertiesManager.get("p360.contingency.gcp.service_account_back"),
		    PropertiesManager.get("p360.contingency.gcp.project_back"),
		    PropertiesManager.get("p360.contingency.gcp.post_products_topic")
		);
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GExtraData.Direccion(MX)->LookupValueLang.Name(es),Product2GExtraData.Section(MX)->LookupValueLang.Name(es)");
		qp.put("items", 
				"'1754611686216860'@1" +
				",'1754611686221330'@1" +
				",'1754611686223221'@1" +
				",'1754611686182897'@1" +
				",'1754611686226495'@1" +
				",'1754611686229502'@1" +
				",'1754611686241801'@1"
			);
		org.json.JSONObject bdy = new org.json.JSONObject();
		org.json.JSONArray productRows = new org.json.JSONArray();
		bdy.put("products", productRows);
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String dirL = values.getString(1);
			String secL = values.getString(2);
			productRows.put( new org.json.JSONObject().put("proposalId", values.getString(0))
					.put("basicData", new org.json.JSONObject().put("Direction", dirL).put("Section", secL)));
//			productRows.put( new org.json.JSONObject().put("proposalId", values.getString(0))
//					.put("Direction", dirL).put("Section", secL));
			if(productRows.length() == 2) {
				System.out.println( pubPostProducts.publishMessage( bdy.toString() ) + ": " + bdy );
				while(productRows.length() > 0 ) {
					productRows.remove(0);
				}
			}
		});
		System.out.println( pubPostProducts.publishMessage( bdy.toString() ) + ": " + bdy );
		while(productRows.length() > 0 ) {
			productRows.remove(0);
		}
	}
	
}
