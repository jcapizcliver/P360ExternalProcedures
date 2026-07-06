package mx.com.liverpool.p360.services.core.temp.move.characteristics;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ElParchePorqueLaRiegan {

	private final RESTWrapper rw = new RESTWrapper();
	
	private void algolindo() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", 
				"Characteristic.Identifier wildcard \"mdr_%\""
				+ "or Characteristic.Identifier wildcard \"msj_%\""
				+ "or Characteristic.Identifier wildcard \"rem_%\""
				+ "or Characteristic.Identifier wildcard \"rma_%\""
				+ "or Characteristic.Identifier wildcard \"rmum_%\""
				+ "or Characteristic.Identifier wildcard \"rrd_%\""
				+ "or Characteristic.Identifier wildcard \"rre_%\""
				);
		qp.put("pageSize", "600");
		java.util.LinkedList<String> ids = new java.util.LinkedList<>();
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> ids.addLast(row.getJSONArray("values").getString(0)) , this::log);
		// Rutina para armar batches con los ids para actualizar el Characteristic.IsActive en false
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"));
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		for(String id : ids) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'")).put("values", new org.json.JSONArray().put(true)));
			if(rows.length() == 100) {
				rw.writeData("list", "Characteristic", null, empty, request, System.out::println);
			}
		}
		if(rows.length() > 0) {
			rw.writeData("list", "Characteristic", null, empty, request, System.out::println);
		}
		// Eliminar..
		rw.deleteData("list", "Characteristic", null, "bySearch", qp, System.out::println);
	}
	
	private void log(String message) {
		System.out.println(message);
	}
	
}
