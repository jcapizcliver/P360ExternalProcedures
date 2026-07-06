package mx.com.liverpool.p360.services.core;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class RESTWrapper {

	private final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"), "Accept-Language: es");
	
	public XMLMisc getXmm() {
		return rw.getXmm();
	}
	
	public RESTWorkshop getRw() {
		return rw;
	}
	
	public void collectData( String api, String entity, String subEntity, String report, java.util.Map<String, String> qp, SimpleResponseProcessor srp ) {
		collectData(api, entity, subEntity, report, qp, srp, null);
	}

	public void collectData( String api, String entity, String subEntity, String report, java.util.Map<String, String> qp, SimpleResponseProcessor srp, ErrorResponseProcessor erp) {
		collectData( api, entity, subEntity, report, qp, srp, erp, true );
	}

	public void collectData( String api, String entity, String subEntity, String report, java.util.Map<String, String> qp, SimpleResponseProcessor srp, ErrorResponseProcessor erp, boolean printProgress) {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int a = 0;
		int b = 0;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/" + api + "/" + entity + ( subEntity == null || "".equals(subEntity) ? "" : "/" + subEntity ) + "/" + report, qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					srp.processResult(rows.getJSONObject(i));
				}
				a += response.getInt("pageSize");
				if(printProgress)
					System.out.println(a + "/" + b);
			} else {
				if(erp == null) {
					System.out.println(rw.getRawResponse());
				}else {
					erp.processError(rw.getRawResponse());
				}
			}
		}while(a < b);
		a = 0;
	}
	
	public void writeData(String method, String api, String entity, String subEntity, java.util.Map<String, String> qp, org.json.JSONObject request, SimpleWriteProcessor swp ) {
		rw.makeRequest(method, "/" + api + "/" + entity + ( subEntity == null || "".equals(subEntity) ? "" : "/" + subEntity ), qp, request.toString());
		swp.process(rw.getRawResponse());
		if(request.has("rows")) {
			org.json.JSONArray rows = request.getJSONArray("rows");
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	public void writeData(String api, String entity, String subEntity, java.util.Map<String, String> qp, org.json.JSONObject request, SimpleWriteProcessor swp ) {
		rw.makeRequest("POST", "/" + api + "/" + entity + ( subEntity == null || "".equals(subEntity) ? "" : "/" + subEntity ), qp, request.toString());
		swp.process(rw.getRawResponse());
		if(request.has("rows")) {
			org.json.JSONArray rows = request.getJSONArray("rows");
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	public void deleteData(String api, String entity, String subEntity, String report, java.util.Map<String, String> qp, SimpleDeleteProcessor sdp) {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(this.rw.getBaseUrl());
		rw.addHeader("Authorization", this.rw.getRc().getHeader().get("Authorization"));
		rw.addHeader("Content-Type", "application/x-www-form-urlencoded");
		rw.makeRequest("DELETE", "/" + api + "/" + entity + ( subEntity == null || "".equals(subEntity) ? "" : "/" + subEntity ) + "/" + report, qp, null);
		sdp.processResult(rw.getRawResponse());
	}
}
