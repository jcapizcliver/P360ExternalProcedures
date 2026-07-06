package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaValidDirection {

	public static void main(String[] args) {
		String[] pieces = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		for(String ln : content) {
			pieces = ln.split("\t");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'ValidDirection'")).put("values", new org.json.JSONArray().put(pieces[1])));
		}
		org.json.JSONObject response = null;
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
		System.out.println(response == null ? rw.getRawResponse() : response);
	}
	
	private static final java.util.ArrayList<String> content = new java.util.ArrayList<>(
				java.util.Arrays.asList(
						("0	N\r\n"
						+ "1	N\r\n"
						+ "10	N\r\n"
						+ "14	S\r\n"
						+ "15	N\r\n"
						+ "16	N\r\n"
						+ "17	N\r\n"
						+ "18	S\r\n"
						+ "19	N\r\n"
						+ "2	N\r\n"
						+ "25	N\r\n"
						+ "27	N\r\n"
						+ "3	N\r\n"
						+ "4	S\r\n"
						+ "5	S\r\n"
						+ "6	N\r\n"
						+ "7	N\r\n"
						+ "8	N\r\n"
						+ "9	N\r\n"
						+ "98	N\r\n"
						+ "99	N").split("\\r\\n")
				)
			);
}
