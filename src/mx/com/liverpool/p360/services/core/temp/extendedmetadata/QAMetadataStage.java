package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class QAMetadataStage {

	
	public static void main(String[] args) {
//		RESTWorkshop rw = new RESTWorkshop();
//		rw.putParameter("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_OLD");
//		org.json.JSONObject response = null;
//		response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
//		if(response != null) {
//			System.out.println(response);
//			System.out.println(response.getJSONObject("counters"));
//		}else {
//			System.out.println("ERROR: " + rw.getRawResponse());
//		}
		try {
			collectData();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	private static void collectData() throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		RESTWorkshop rwd = new RESTWorkshop();
		rwd.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		rw.putParameter("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_OLD");
		rw.putParameter("fields", "StandardizationValue.Value");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		StringBuilder sb = new StringBuilder();
		do {
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null) {
				a = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					sb.append(sb.length() == 0 ? "" : ",").append("'").append(values.getString(0).replaceAll("'", "\\\\'")).append("'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_OLD'");
				}
				System.out.println(a);
				if("".equals(sb.toString())) {
					System.out.println("No rows processed... " + rw.getRawResponse());
				}else {
					rwd.putParameter("items", sb.toString());
					rwd.makeRequest("DELETE", "/list/StandardizationValue/byItems");
					System.out.println(rwd.getRawResponse());
					sb.setLength(0);
				}
			}
		}while(a > 0);
		a = 0;
	}
	
}
