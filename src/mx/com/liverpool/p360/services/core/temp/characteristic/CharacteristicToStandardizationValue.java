package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CharacteristicToStandardizationValue {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("pageSize", "50000");
		qp.put("query", "not Characteristic.Identifier is empty");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")), 20000, request -> rw.writeData("list", "StandardizationValue", null, qp0, request, System.out::println));
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'ProyeccióDeCaracterísticas'")).put("values", new org.json.JSONArray().put(values.getString(0))));
		});
		rh.sendData();
	}
	
}
