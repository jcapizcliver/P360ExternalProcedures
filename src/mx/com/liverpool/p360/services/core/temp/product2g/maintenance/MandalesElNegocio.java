package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MandalesElNegocio extends RESTWrapper {

	
	public static void main(String[] args) {
		MandalesElNegocio s = new MandalesElNegocio();
		PubSubGCP ps = new PubSubGCP();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'ExternalStatus'");
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business',-1)->LookupValueLang.Name(es)");
		qp.put("pageSize", "25000");
		org.json.JSONObject msg = new org.json.JSONObject();
		org.json.JSONArray products = new org.json.JSONArray();
		msg.put("products", products);
		s.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			products.put(new org.json.JSONObject().put("proposalId", row.getJSONArray("values").getString(0)).put("Business", row.getJSONArray("values").getJSONArray(1).getString(0) ));
			if(products.length() == 1000) {
				ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
						  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
						  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), msg.toString());
				while(products.length() > 0) {
					products.remove(0);
				}
			}
		}, System.out::println);
		ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
				  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
				  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), msg.toString());
		while(products.length() > 0) {
			products.remove(0);
		}
	}
	
}
