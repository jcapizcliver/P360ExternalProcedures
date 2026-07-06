package mx.com.liverpool.p360.services.core;

import java.io.IOException;

public class LaLimpiezaDedaTos {

	private static final String baseURL = "";
	private static final String encoded = "";

	public static void main(String[] args) {

		try {
			String rawResponse = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded).getRequest("GET", baseURL + "/object/Product2G/'0040444894'@'MASTER'?entityFilter=Product2GCharacteristicValue", null);
			org.json.JSONObject response = new org.json.JSONObject(rawResponse);
			org.json.JSONArray characteristicRecords = response.getJSONObject("_data").getJSONArray("_characteristicRecords");
		} catch ( IOException | ServiceUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void calculaCampo1(org.json.JSONObject objectFromObjectAPI) {
		/***********************************************************************************************
		 *
		 * {
		 * 		"_characteristicRecords": [
		 * 			{
		 * 				"_qualification": {"characteristic": {"_code": "SAPObjectType"} },
		 * 				"_recordLang": [
		 * 					{
		 * 						"lang": [ { "_qualification": { "language": { "_code": "zxx" } } } ],
		 * 						"value": [ "hola" ]
		 * 					}
		 * 				]
		 * 			}
		 *   	]
		 * }
		 *
		 *
		 *****************************************************************************************************/
	}

	private void calculaCampo2(org.json.JSONObject objectFromObjectAPI) {
		/****************************************************************************************************
		 *
		 * {
		 * 		"_characteristicRecords": [
		 * 			{
		 * 				"_qualification": {"characteristic": {"_code": "SAPObjectType"} },
		 * 				"_recordLang": [
		 * 					{
		 * 						"lang": [ { "_qualification": { "language": { "_code": "zxx" } } } ],
		 * 						"value": [ "hola" ]
		 * 					}
		 * 				]
		 * 			}
		 *   	]
		 * }
		 *
		 *
		 ****************************************************************************************************/
	}
}
