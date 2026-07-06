package mx.com.liverpool.p360.services.core.temp.workflow;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class SalidaManualCategoryDesdeArthivo {

	
	public static void main(String[] args) {
		String processId = "23541";
		String workflowId = "CategoryRevision";
		String status = "Revisión Category";
		String internalId = "'1754611647184010'@1";
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		workshop.setBaseUrl(PropertiesManager.get("p360.contingency.base_url"));
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		java.util.List<String> ids = readIDs("C:\\opt\\LVP\\desorden\\PROD\\Export Only IDs (43).csv");
		org.json.JSONArray itemIds = new org.json.JSONArray();
		org.json.JSONObject response = null;
		for(String id : ids) {
			internalId = id;
			itemIds.put(internalId);
		}
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("POST", "/manage/workflow/status/leave", qp, rb.toString());
		System.out.println(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	}
	
	private static java.util.List<String> readIDs(String filePath){
		java.util.List<String> ids = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(filePath).toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				ids.add("'" + line + "'@1");
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return ids;
	}
	
}
