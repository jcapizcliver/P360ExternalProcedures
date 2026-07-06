package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SendExtStatusToPubSub extends RESTWrapper {

	
	public static void main(String[] args) {
		SendExtStatusToPubSub s = new SendExtStatusToPubSub();
		PubSubGCP ps = new PubSubGCP();
		java.util.LinkedList<String> ids = new java.util.LinkedList<>();
//		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "samples", "ei"))){
//		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "prods_req_ext_status"))){
//			stream.forEach(ids::addLast);
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("fields", "Product2G.ProductNo");
		s.collectData("list", "Product2G", null, "byCatalog", qp0, row -> ids.addLast(row.getJSONArray("values").getString(0)), System.out::println);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("lookup", "'ExternalStatus'");
		java.util.Map<String, String> escl = new java.util.TreeMap<>();
		s.collectData("list", "LookupValue", null, "byLookup", qp, row -> escl.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)), System.out::println);
		qp.clear();
		qp.put("fields", "Product2G.ProductNo,Product2G.ExternalStatus->LookupValue.Code");
		qp.put("pageSize", "25000");
		StringBuilder sb = new StringBuilder();
		int a = 0;
		org.json.JSONObject msg = new org.json.JSONObject();
		org.json.JSONArray products = new org.json.JSONArray();
		msg.put("products", products);
		for(String id : ids) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'");
			sb.append(id);
			sb.append("'@1");
			a++;
			if(a % 2000 == 0) {
				qp.put("items", sb.toString());
				s.collectData("list", "Product2G", null, "byItems", qp, row -> {
					products.put(new org.json.JSONObject().put("proposalId", row.getJSONArray("values").getString(0)).put("externalStatus", escl.get( row.getJSONArray("values").getString(1) )));
				}, System.out::println);
				sb.setLength(0);
				ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
						  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
						  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), msg.toString());
				while(products.length() > 0) {
					products.remove(0);
				}
			}
		}
		if(a % 2000 != 0) {
			qp.put("items", sb.toString());
			s.collectData("list", "Product2G", null, "byItems", qp, row -> {
				products.put(new org.json.JSONObject().put("proposalId", row.getJSONArray("values").getString(0)).put("externalStatus", escl.get( row.getJSONArray("values").getString(1) )));
			}, System.out::println);
			sb.setLength(0);
			ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
					  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
					  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), msg.toString());
			while(products.length() > 0) {
				products.remove(0);
			}
		}
	}
	
}
