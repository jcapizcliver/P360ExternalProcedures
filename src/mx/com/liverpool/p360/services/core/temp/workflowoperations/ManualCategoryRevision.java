package mx.com.liverpool.p360.services.core.temp.workflowoperations;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ManualCategoryRevision {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		while(true) {
			try {
				org.json.JSONObject rb = new org.json.JSONObject();
				rb.put("processId", "23541");
				rb.put("workflowId", "CategoryRevision");
				rb.put("status", "Revisión Category");
				rb.put("entity", "Product2G");
				org.json.JSONArray itemIds = new org.json.JSONArray();
				org.json.JSONObject response = null;
				org.json.JSONArray rows = null;
				java.util.Map<String, String> qp = new java.util.TreeMap<>();
				qp.put("query", "Product2G.CurrentStatus = 1023");
				int currentIndex = 0;
				int totalSize = 0;
				do {
					qp.put("startIndex", String.valueOf(currentIndex));
					response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
					if(response != null) {
						totalSize = response.getInt("totalSize");
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							currentIndex++;
							itemIds.put(rows.getJSONObject(i).getJSONObject("object").getString("id"));
						}
						rb.put("itemId", itemIds);
//						rw.setBaseUrl("https://webctep360dev.liverpool.com.mx/rest/V1.0");
						System.out.println(rb);
						response = rw.makeRequest("POST", "/manage/workflow/status/enter", qp, rb.toString());
						System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
						while(itemIds.length() > 0) {
							itemIds.remove(0);
						}
					} else {
						System.out.println( rw.getRawResponse() );
					}
				}while(currentIndex < totalSize);
				currentIndex = 0;
				System.out.println("Now sleeping...");
				Thread.sleep(1000*30);
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
