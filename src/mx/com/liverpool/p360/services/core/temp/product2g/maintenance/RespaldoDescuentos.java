package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class RespaldoDescuentos {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "SimpleProduct2GCharacteristicValueLang.Value('Descuento1',-1),SimpleProduct2GCharacteristicValueLang.Value('Descuento2',-1)");
		qp.put("query", "not characteristic('Descuento1') is empty or not characteristic('Descuento2') is empty");
		qp.put("pageSize", "50000");
		java.util.List<String[]> elementos = new java.util.ArrayList<>();
		java.util.List<String[]> elementosA = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			elementos.add(new String[] { row.getJSONObject("object").getString("id"), values.getJSONArray(0).getString(0), values.getJSONArray(1).getString(0) });
		});
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Descuentos").toFile())))){
			elementos.forEach( arr -> pw.println(rw.getRw().serializeChunk(arr)) );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		qp.put("fields", "SimpleArticleCharacteristicValueLang.Value('Descuento1',-1),SimpleArticleCharacteristicValueLang.Value('Descuento2',-1)");
		qp.put("query", "not characteristic('Descuento1') is empty or not characteristic('Descuento2') is empty");

		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			elementosA.add(new String[] { row.getJSONObject("object").getString("id"), values.getJSONArray(0).getString(0), values.getJSONArray(1).getString(0) });
		});
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "DescuentosArticulos").toFile())))){
			elementosA.forEach( arr -> pw.println(rw.getRw().serializeChunk(arr)) );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		System.out.println("Quitando datos (" + elementos.size() + ")");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)")).put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)")), 10000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
		for(String[] data : elementos) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", data[0])).put("values", new org.json.JSONArray().put("").put("")));
		}
		rh.sendData();
		
		System.out.println("Quitando datos artículos (" + elementosA.size() + ")");
		RequestHandler rhA = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)")).put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)")), 10000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::println) );
		for(String[] data : elementosA) {
			rhA.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", data[0])).put("values", new org.json.JSONArray().put("").put("")));
		}
		rhA.sendData();
		
		System.out.println("Deshabilitando Descuento1");
		rw.writeData("list", "Characteristic", null, qp0, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Descuento1'")).put("values", new org.json.JSONArray().put(false)))), System.out::println);
		System.out.println("Deshabilitando Descuento2");
		rw.writeData("list", "Characteristic", null, qp0, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Descuento2'")).put("values", new org.json.JSONArray().put(false)))), System.out::println);

		System.out.println("Cambiando tipo de dato Descuento1");
		rw.writeData("list", "Characteristic", null, qp0, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Descuento1'")).put("values", new org.json.JSONArray().put("DECIMAL")))), System.out::println);
		System.out.println("Cambiando tipo de dato Descuento2");
		rw.writeData("list", "Characteristic", null, qp0, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Descuento2'")).put("values", new org.json.JSONArray().put("DECIMAL")))), System.out::println);
		
		System.out.println("Habilitando Descuento1");
		rw.writeData("list", "Characteristic", null, qp0, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Descuento1'")).put("values", new org.json.JSONArray().put(true)))), System.out::println);
		System.out.println("Habilitando Descuento2");
		rw.writeData("list", "Characteristic", null, qp0, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Descuento2'")).put("values", new org.json.JSONArray().put(true)))), System.out::println);

		System.out.println("Regresando datos...");
		for(String[] data : elementos) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", data[0])).put("values", new org.json.JSONArray().put(data[1]).put(data[2])));
		}
		rh.sendData();

		for(String[] data : elementosA) {
			rhA.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", data[0])).put("values", new org.json.JSONArray().put(data[1]).put(data[2])));
		}
		rhA.sendData();
	}
	
}
