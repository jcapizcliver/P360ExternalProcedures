package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RestClient;

public class MasterDataRejectionEntitiesUpdate {


	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final String baseUrl = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) {

		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
						+ java.net.URLEncoder.encode("Characteristic.Category equals MasterData_Rechazo and Characteristic.UpperBound = 1", "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=2"
						+ "&startIndex=" + currentIndex
						, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					values.put(100);
				}
				System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic",
						new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
								.put(new org.json.JSONObject().put("identifier", "Characteristic.UpperBound"))
								)
						.put("rows", rows).toString()) );
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
