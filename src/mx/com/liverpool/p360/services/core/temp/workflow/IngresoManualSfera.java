package mx.com.liverpool.p360.services.core.temp.workflow;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class IngresoManualSfera extends RESTWrapper {

	
	public static void main(String[] args) {
		IngresoManualSfera imq = new IngresoManualSfera();
		String processId = "1234567";
		String workflowId = "Sfera";
		String status = "Nuevo producto Sfera";
//		String internalId = "'1754611647219982'@1";
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONArray itemIds = new org.json.JSONArray();
		itemIds.put("'LVP1187382024'@1");
//		qp.put("fields", "Product2G.ProductNo");
//		qp.put("pageSize", "10000");
//		qp.put("query", "Product2G.CurrentStatus = \"Revisión QA\"");
//		imq.collectData("list", "Product2G", null, "bySearch", qp, row->{
//			itemIds.put("'" + row.getJSONArray("values").getString(0) + "'@1");
//		}, System.out::println);
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONObject response = null;
//		itemIds.put(internalId);
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		response = imq.getRw().makeRequest("POST", "/manage/workflow/status/enter", qp0, rb.toString());
		System.out.println(response == null ? "ERR: " + imq.getRw().getRawResponse() : response.toString());
	}
	
}
