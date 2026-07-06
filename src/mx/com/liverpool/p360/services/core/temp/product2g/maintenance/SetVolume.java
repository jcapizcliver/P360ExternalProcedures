package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class SetVolume {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2GCharacteristicValueLang.Value('ProductHeight',root,\"0000.0000.RK\",'ProductHeight',-1),Product2GCharacteristicValueLang.Value('ProductDepth',root,\"0000.0000.RK\",'ProductDepth',-1),Product2GCharacteristicValueLang.Value('ProductWidth',root,\"0000.0000.RK\",'ProductWidth',-1)");
		qp.put("query", "characteristic('VOLUMAtt') is empty and not characteristic('ProductHeight') is empty and not characteristic('ProductDepth') is empty and not characteristic('ProductWidth') is empty");
		qp.put("pageSize", "1200");
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('VOLUMAtt',root,\"0000.0000.RK\",'VOLUMAtt',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println));
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject toAdd = new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( new java.math.BigDecimal(values.getJSONArray(0).getString(0)).multiply(new java.math.BigDecimal(values.getJSONArray(1).getString(0)).multiply(new java.math.BigDecimal(values.getJSONArray(2).getString(0)))).doubleValue() ));
			System.out.println(". " + values);
			System.out.println("R1. Adding: " + toAdd);
			rh.addRow( toAdd );
		});
		rh.sendData();
		
		RequestHandler rh2 = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZVOLCJ',root,\"0000.0000.RK\",'ZVOLCJ',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println));
		qp.put("fields", "Product2GCharacteristicValueLang.Value('ZHOECJ',root,\"0000.0000.RK\",'ZHOECJ',-1),Product2GCharacteristicValueLang.Value('ZLAECJ',root,\"0000.0000.RK\",'ZLAECJ',-1),Product2GCharacteristicValueLang.Value('ZBRECJ',root,\"0000.0000.RK\",'ZBRECJ',-1)");
		qp.put("query", "characteristic('ZVOLCJ') is empty and not characteristic('ZHOECJ') is empty and not characteristic('ZLAECJ') is empty and not characteristic('ZBRECJ') is empty");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject toAdd = new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( new java.math.BigDecimal(values.getJSONArray(0).getString(0)).multiply(new java.math.BigDecimal(values.getJSONArray(1).getString(0)).multiply(new java.math.BigDecimal(values.getJSONArray(2).getString(0)))).doubleValue() ));
			System.out.println(". " + values);
			System.out.println("R2. Adding: " + toAdd);
			rh2.addRow( toAdd );
		});
		rh2.sendData();

		RequestHandler rh3 = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZVOLPQ',root,\"0000.0000.RK\",'ZVOLPQ',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println));
		qp.put("fields", "Product2GCharacteristicValueLang.Value('ZHOEPQ',root,\"0000.0000.RK\",'ZHOEPQ',-1),Product2GCharacteristicValueLang.Value('ZBREPQ',root,\"0000.0000.RK\",'ZBREPQ',-1),Product2GCharacteristicValueLang.Value('ZLAEPQ',root,\"0000.0000.RK\",'ZLAEPQ',-1)");
		qp.put("query", "characteristic('ZVOLPQ') is empty and not characteristic('ZHOEPQ') is empty and not characteristic('ZBREPQ') is empty and not characteristic('ZLAEPQ') is empty");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject toAdd = new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( new java.math.BigDecimal(values.getJSONArray(0).getString(0)).multiply(new java.math.BigDecimal(values.getJSONArray(1).getString(0)).multiply(new java.math.BigDecimal(values.getJSONArray(2).getString(0)))).doubleValue() ));
			System.out.println(". " + values);
			System.out.println("R3. Adding: " + toAdd);
			rh3.addRow( toAdd );
		});
		rh3.sendData();
	}

}
