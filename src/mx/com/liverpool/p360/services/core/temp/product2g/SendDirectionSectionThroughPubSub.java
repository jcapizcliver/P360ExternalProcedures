package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SendDirectionSectionThroughPubSub {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		PubSubGCP ps = new PubSubGCP();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("pageSize", "10000");
		qp.put("fields", 
				"Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Direction',root,\"0000.0000.RK\",'Direction')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValueLang.Name(es)"
			);
		org.json.JSONObject body = new org.json.JSONObject();
		org.json.JSONArray elements = new org.json.JSONArray();
		body.put("products", elements);
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			elements.put(new org.json.JSONObject()
					.put("proposalId", values.getString(0))
					.put("basicData", new org.json.JSONObject().put("Direction", values.getJSONArray(1).getString(0)).put("Section", values.getJSONArray(2).getString(0)))
					.put("header", new org.json.JSONObject().put("Business", values.getJSONArray(3).getString(0)))
				);
			if(elements.length() == 10000) {
	        	ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
								  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
								  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), body.toString());
				while(elements.length() > 0) {
					elements.remove(0);
				}
			}
		}, System.out::println);
		if(elements.length() > 0) {
			ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
					PropertiesManager.get( "p360.contingency.gcp.idmc_put_products" ), 
					PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), body.toString());
			while(elements.length() > 0) {
				elements.remove(0);
			}
		}
	}
	
}
