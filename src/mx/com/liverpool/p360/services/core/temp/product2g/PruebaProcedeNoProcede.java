package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.amqp.P360ActiveMQBPMStage;

public class PruebaProcedeNoProcede {

	public static void main(String[] args) {
		try {
			all();
		} catch (ServiceUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void all() throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "Product2G.ProductNo");
//		qp.put("fields", "Article.");
		qp.put("query",  "characteristic('ProcedeNoProcede') is empty" /* "Product2G.ProductNo equals \"LVP1033594279\"" /* "Product2G.CurrentStatus = 1022" */ );
		qp.put("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		P360ActiveMQBPMStage pam = new P360ActiveMQBPMStage();
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				System.out.println(response.getInt("totalSize"));
				rows = response.getJSONArray("rows");
				totalSize = response.getInt("totalSize");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					pam.setProcedeNoProcede(values.getString(0));
				}
			}else {
				
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
	}
}
