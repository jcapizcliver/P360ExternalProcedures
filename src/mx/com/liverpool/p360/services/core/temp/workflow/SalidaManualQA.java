package mx.com.liverpool.p360.services.core.temp.workflow;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SalidaManualQA {

	
	public static void main(String[] args) {
		String processId = "23540";
		String workflowId = "QARevision";
		String status = "Revisión QA";
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		workshop.setBaseUrl(PropertiesManager.get("p360.contingency.base_url"));
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", "Product2G.ProductNo");
		qp0.put("pageSize", "2000");
		qp0.put("query", "Product2G.CurrentStatus = \"Aprobada\"");
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONArray itemIds = new org.json.JSONArray();
		rb.put("itemId", itemIds);
		int[] a = new int[] {0};
		a[0] = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp0, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String internalId = "'" + values.getString(0) + "'@1";
			itemIds.put(internalId);
			a[0]++;
			if(a[0] % 1000 == 0) {
				org.json.JSONObject response = workshop.makeRequest("POST", "/manage/workflow/status/enter", qp, rb.toString());
				System.out.println(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
				while(itemIds.length() > 0) {
					itemIds.remove(0);
				}
			}
		});
		if( itemIds.length() > 0 ) {
			org.json.JSONObject response = workshop.makeRequest("POST", "/manage/workflow/status/enter", qp, rb.toString());
			System.out.println(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
			while(itemIds.length() > 0) {
				itemIds.remove(0);
			}
		}
	}
	
}
