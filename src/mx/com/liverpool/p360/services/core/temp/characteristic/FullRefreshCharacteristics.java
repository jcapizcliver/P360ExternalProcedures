package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class FullRefreshCharacteristics {

	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,CharacteristicLang.Name(es)");
		qp.put("pageSize", "5000");
		qp.put("query", "Characteristic.IsActive = true and Characteristic.ParentCharacteristic is empty");
		java.util.LinkedList<String[]> datas = new java.util.LinkedList<>();
		RequestHandler insertLookup = new RequestHandler(
				new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
					.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
				, 1000, request -> rw.writeData("list", "LookupValue", null, new java.util.TreeMap<>(), request, System.out::println) );
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
				insertLookup.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row.getJSONArray("values").getString(0) + "'@'Characteristics'")).put("values", new org.json.JSONArray().put(row.getJSONArray("values").getString(1)).put(true)));
				datas.addLast(new String[] { row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1) });
			}, System.out::println);
		insertLookup.sendData();
	}
	
}
