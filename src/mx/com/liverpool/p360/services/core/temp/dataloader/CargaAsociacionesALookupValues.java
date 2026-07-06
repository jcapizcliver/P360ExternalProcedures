package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaAsociacionesALookupValues {

	public static void main(String[] args) {
		String[] pieces = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"));
		org.json.JSONArray vals = new org.json.JSONArray();
		for(String ln : content) {
			pieces = ln.split("\t");
			vals.put(pieces[0]);
		}
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Brands'@'BannedElementsForMarketplacePublication'")).put("values", new org.json.JSONArray().put(vals)));
		org.json.JSONObject response = null;
		RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
		System.out.println(response == null ? rw.getRawResponse() : response);
	}
	
	private static final java.util.ArrayList<String> content = new java.util.ArrayList<>(
				java.util.Arrays.asList(
						("0000\r\n"
								+ "6600\r\n"
								+ "3610\r\n"
								+ "3660\r\n"
								+ "4190\r\n"
								+ "4920\r\n"
								+ "5450\r\n"
								+ "5570\r\n"
								+ "5610\r\n"
								+ "6460\r\n"
								+ "8170\r\n"
								+ "3619\r\n"
								+ "3768\r\n"
								+ "4618\r\n"
								+ "5211\r\n"
								+ "5728\r\n"
								+ "5908\r\n"
								+ "6262\r\n"
								+ "6803\r\n"
								+ "6806\r\n"
								+ "7600\r\n"
								+ "7715\r\n"
								+ "8183\r\n"
								+ "8729\r\n"
								+ "8731\r\n"
								+ "8743\r\n"
								+ "8745\r\n"
								+ "8882\r\n"
								+ "9550\r\n"
								+ "9683\r\n"
								+ "9693\r\n"
								+ "A166\r\n"
								+ "A350\r\n"
								+ "B410\r\n"
								+ "B411\r\n"
								+ "B493\r\n"
								+ "B661\r\n"
								+ "B884\r\n"
								+ "C098\r\n"
								+ "C314\r\n"
								+ "C511\r\n"
								+ "D649\r\n"
								+ "D836\r\n"
								+ "E026\r\n"
								+ "E244\r\n"
								+ "E426\r\n"
								+ "E949\r\n"
								+ "E968\r\n"
								+ "F170\r\n"
								+ "F668\r\n"
								+ "G135\r\n"
								+ "G196\r\n"
								+ "G197\r\n"
								+ "G214\r\n"
								+ "G366\r\n"
								+ "G518\r\n"
								+ "G864\r\n"
								+ "H019\r\n"
								+ "H268\r\n"
								+ "H327\r\n"
								+ "H598\r\n"
								+ "H610\r\n"
								+ "J133\r\n"
								+ "L422\r\n"
								+ "N512\r\n"
								+ "O141\r\n"
								+ "O182\r\n"
								+ "P302\r\n"
								+ "P497\r\n"
								+ "P767\r\n"
								+ "P904\r\n"
								+ "Q039\r\n"
								+ "Q241\r\n"
								+ "Q341\r\n"
								+ "Q457\r\n"
								+ "R423\r\n"
								+ "R475\r\n"
								+ "R520\r\n"
								+ "R526\r\n"
								+ "R562\r\n"
								+ "S042\r\n"
								+ "S298\r\n"
								+ "S442\r\n"
								+ "S627\r\n"
								+ "T964\r\n"
								+ "U306\r\n"
								+ "V678").split("\\r\\n")
				)
			);
}
