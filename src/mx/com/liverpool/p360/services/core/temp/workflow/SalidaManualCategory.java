package mx.com.liverpool.p360.services.core.temp.workflow;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class SalidaManualCategory {

	
	public static void main(String[] args) {
		String processId = "23541";
		String workflowId = "CategoryRevision";
		String status = "Revisión Category";
		String internalId = "'LVP1184768042'@1";
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		workshop.setBaseUrl(PropertiesManager.get("p360.contingency.base_url"));
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONArray itemIds = new org.json.JSONArray();
		org.json.JSONObject response = null;
		itemIds.put(internalId);
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("POST", "/manage/workflow/status/leave", qp, rb.toString());
		System.out.println(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	} 
	
}
