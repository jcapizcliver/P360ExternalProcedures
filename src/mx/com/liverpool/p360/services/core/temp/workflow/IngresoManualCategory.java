package mx.com.liverpool.p360.services.core.temp.workflow;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class IngresoManualCategory extends RESTWrapper {

	
	public static void main(String[] args) {
		IngresoManualCategory imc = new IngresoManualCategory();
		String processId = "23541";
		String workflowId = "CategoryRevision";
		String status = "Revisión Category";
		String internalId = "'1754611647184010'@1";
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONArray itemIds = new org.json.JSONArray();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "10000");
		qp.put("query", "Product2G.CurrentStatus = \"Category\"");
		imc.collectData("list", "Product2G", null, "bySearch", qp, row->{
			itemIds.put("'" + row.getJSONArray("values").getString(0) + "'@1");
		}, System.out::println);
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONObject response = null;
		itemIds.put(internalId);
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		response = imc.getRw().makeRequest("POST", "/manage/workflow/status/enter", qp0, rb.toString());
		System.out.println(response == null ? "ERR: " + imc.getRw().getRawResponse() : response.toString());
	}
	
}
