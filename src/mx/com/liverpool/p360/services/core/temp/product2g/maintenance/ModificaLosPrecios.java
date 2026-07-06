package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ModificaLosPrecios {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
//		devuelveLosDatos();
//		devuelveLosDatosArticle();
//		guardaLosDatosArticle();
//		quitaLosDatosArticle();
//		consultaDatosArticle();
//		quitaLosDatos();
//		consultaDatos();
	}
	
	private static void devuelveLosDatos() {
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)")).put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)")), 25000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::print) );
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "productsAndDiscounts").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(new org.json.JSONArray().put(pieces[1])).put(pieces[2])));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		rh.sendData();
		
	}
	
	private static void devuelveLosDatosArticle() {
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)")).put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)")), 25000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::print) );
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "productsAndDiscountsArticle").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(new org.json.JSONArray().put(pieces[1])).put(pieces[2])));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		rh.sendData();
		
	}
	
	private static void quitaLosDatos() {
		java.util.List<String> ids = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "productsAndDiscounts").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				ids.add(pieces[0]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)")).put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)")), 25000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::print) );
		for(String id : ids) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put("").put("")));
		}
		rh.sendData();
	}
	
	private static void consultaDatos() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
			   "Product2G.ProductNo"
			+ ",Product2GCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)"
			+ ",Product2GCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)");
		qp.put("pageSize", "25000");
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(!"".equals(values.getJSONArray(1).getString(0)) || !"".equals(values.getJSONArray(2).getString(0))) {
				System.out.println(values);
			}
		});
	}
	
	private static void guardaLosDatos() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
			   "Product2G.ProductNo"
			+ ",Product2GCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)"
			+ ",Product2GCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)");
		qp.put("pageSize", "25000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "productsAndDiscounts").toFile(), true), java.nio.charset.StandardCharsets.UTF_8))){
			rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getJSONArray(1).getString(0)) || !"".equals(values.getJSONArray(2).getString(0))) {
					System.out.println(values);
					pw.println( rw.getRw().serializeChunk(new Object[] { values.getString(0), values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0) }) );
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private static void quitaLosDatosArticle() {
		java.util.List<String> ids = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "productsAndDiscountsArticle").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				ids.add(pieces[0]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Vimos estos: " + ids.size());
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)"))
				.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)")), 25000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::print) );
		for(String id : ids) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put("").put("")));
		}
		rh.sendData();
	}
	
	private static void consultaDatosArticle() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
			   "Article.SupplierAID"
			+ ",ArticleCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)"
			+ ",ArticleCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)");
		qp.put("pageSize", "25000");
		rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(!"".equals(values.getJSONArray(1).getString(0)) || !"".equals(values.getJSONArray(2).getString(0))) {
				System.out.println(values);
			}
		});
	}
	
	private static void guardaLosDatosArticle() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
			   "Article.SupplierAID"
			+ ",ArticleCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)"
			+ ",ArticleCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)");
		qp.put("pageSize", "25000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "productsAndDiscountsArticle").toFile(), true), java.nio.charset.StandardCharsets.UTF_8))){
			rw.collectData("list", "Article", null, "byCatalog", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getJSONArray(1).getString(0)) || !"".equals(values.getJSONArray(2).getString(0))) {
					System.out.println(values);
					pw.println( rw.getRw().serializeChunk(new Object[] { values.getString(0), values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0) }) );
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
