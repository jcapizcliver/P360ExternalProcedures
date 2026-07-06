package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.exports.RealExportProducts2Mirakl;

public class CargaCaracteristicasHeredadas {


	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
//		System.out.println(RealExportProducts2Mirakl.YEA.size());
//		System.exit(0);
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		for(String cid : RealExportProducts2Mirakl.YEA) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + cid + "'@'CaracteristicasHeredables'")).put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("id", "'" + cid + "'"))));
		}
		workshop.makeRequest("POST", "/list/StandardizationValue/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"))).put("rows", rows).toString());
		System.out.println(workshop.getRawResponse());
	}



}
