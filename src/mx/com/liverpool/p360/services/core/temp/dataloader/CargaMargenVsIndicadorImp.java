package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaMargenVsIndicadorImp {

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
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'MargenVsIndicadorImp'")).put("values", new org.json.JSONArray().put(pieces[1])));
		}
		org.json.JSONObject response = null;
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
		System.out.println(response == null ? rw.getRawResponse() : response);
	}
	
	private static final java.util.ArrayList<String> content = new java.util.ArrayList<>(
				java.util.Arrays.asList(
						("1	1\r\n"
								+ "2	1.16\r\n"
								+ "3	1.08\r\n"
								+ "4	1.16\r\n"
								+ "E0	1\r\n"
								+ "E1	1.11\r\n"
								+ "E2	1.16\r\n"
								+ "E3	1\r\n"
								+ "E8	1.08\r\n"
								+ "G1	1.1948\r\n"
								+ "G2	1.2006\r\n"
								+ "G3	1.2122\r\n"
								+ "G4	1.08\r\n"
								+ "G5	1.2528\r\n"
								+ "I1	1.1948\r\n"
								+ "I2	1.2006\r\n"
								+ "I3	1.2122\r\n"
								+ "I4	1.08\r\n"
								+ "I5	1.2528\r\n"
								+ "J5	1.2528\r\n"
								+ "M1	1.3662\r\n"
								+ "M2	1.404\r\n"
								+ "M3	1.6524\r\n"
								+ "M4	1.35\r\n"
								+ "P1	1.3986\r\n"
								+ "P2	1.4616\r\n"
								+ "P5	1.6872\r\n"
								+ "P6	1.7632\r\n"
								+ "W1	1.40415\r\n"
								+ "W2	1.4674\r\n"
								+ "W3	1.443\r\n"
								+ "W4	1.508\r\n"
								+ "W5	1.6983\r\n"
								+ "W6	1.7748\r\n"
								+ "W7	1.3875\r\n"
								+ "W8	1.45").split("\\r\\n")
				)
			);
}
