package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MandaPubSubNegocio {

	
	private static RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		PubSubGCP ps = new PubSubGCP();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("/", "u01", "workshop", "aLosQueLesPuseBusiness").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			int count = 0;
			org.json.JSONObject body = new org.json.JSONObject();
			org.json.JSONArray products = new org.json.JSONArray();
			body.put("products", products);
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", 
					   "Product2G.ProductNo"
					+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business',-1)->LookupValueLang.Name(es)"
					+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				);
			qp.put("includeLabels", "true");
//			line = "S16995902";
			while((line = br.readLine()) != null) {
				sb.append(sb.length() == 0 ? "" : ",");
				sb.append("'").append(line).append("'@1");
				count++;
				if(count % 1000 == 0) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						products.put(new org.json.JSONObject().put("proposalId", values.getString(0)).put("business", values.getJSONArray(1).getString(0)));
					});
					ps.publishMessage( 
							 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
							 PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
							 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
							 body.toString()
						);
					while(products.length() > 0) {
						products.remove(0);
					}
					sb.setLength(0);
				}
			}
			if(count % 1000 != 0) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					products.put(new org.json.JSONObject().put("proposalId", values.getString(0)).put("template", values.getJSONArray(2).getString(0)).put("business", values.getJSONArray(1).getString(0)));
				});
				ps.publishMessage( 
						 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
						 PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
						 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
						 body.toString()
					);
				while(products.length() > 0) {
					products.remove(0);
				}
				sb.setLength(0);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
