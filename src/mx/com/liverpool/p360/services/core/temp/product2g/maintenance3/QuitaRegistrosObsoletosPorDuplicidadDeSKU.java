package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class QuitaRegistrosObsoletosPorDuplicidadDeSKU {

	/*
	 * Clase que quita productos que tienen una variante con un SKU igual al SKU de otra variante.
	 * Coloca el estado en "Eliminada" para las propuestas que tienen un SKU según reglas de prefijo de productos.
	 **************************************************************************************************************/
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		int[] counter = {0};
		java.util.List<String[]> datas = new java.util.ArrayList<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			counter[0]++;
			if(counter[0] == 1) {
				
			}else {
				if(arr.length > 0)
					datas.add( arr.length < 2 ? new String[] {arr[0], ""} : arr );
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		System.out.println("Collected " + datas.size() + " datas.");
		java.util.Collections.sort(datas, (o1,o2) -> o1[1].compareTo(o2[1]) );
		java.util.List<String> rugals = new java.util.ArrayList<>();
		String sap = null;
		String p360 = null;
		String step = null;
		String toDelete = null;
		java.util.List<String> toDeleteList = new java.util.ArrayList<>();
		java.util.List<String> losEse = new java.util.ArrayList<>();
		java.util.List<String> losDiesiseis = new java.util.ArrayList<>();
		java.util.List<String> losSAP = new java.util.ArrayList<>();
		String[] prevData = null;
		try(
			java.io.PrintWriter pw  = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("NecesitaDesempatePorMismoOrigen").toFile())));
			java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("NecesitaDesempatePorMismoOrigenP360").toFile())));
			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("NecesitaDesempatePorMismoOrigenEse").toFile())));
			java.io.PrintWriter pw3 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("NecesitaDesempatePorMismoOrigenSAP").toFile())));
		){
			for(String[] data : datas) {
				if(prevData != null && !prevData[1].equals(data[1])) {
					if(rugals.size() > 1) {
						if(rugals.size() == 2) {
							sap = rugals.get(0).startsWith("LVP") || rugals.get(0).startsWith("SBB") ? rugals.get(0) : rugals.get(1).startsWith("LVP") || rugals.get(1).startsWith("SBB") ? rugals.get(1) : null ;
							p360 = rugals.get(0).length() == 16 ? rugals.get(0) : rugals.get(1).length() == 16 ? rugals.get(1) : null;
							if(sap == null && p360 != null) {
								step = p360.equals(rugals.get(0)) ? rugals.get(1) : rugals.get(0);
							}else if(sap != null && p360 == null) {
								step = sap.equals( rugals.get(0)) ? rugals.get(1) : rugals.get(0);
							}
							if( sap != null && step != null ) {
								toDelete = sap;
							}else if(step != null && p360 != null) {
								toDelete = step;
							}else if(sap != null && p360 != null){
								toDelete = sap;
							}else {
								pw.println( rw.getRw().serializeChunk(prevData) );
							}
							if(toDelete != null) {
								toDeleteList.add(toDelete);
							}
							sap = null;
							step = null;
							p360 = null;
							toDelete = null;
						}else {
							for(String r : rugals) {
								if(r.startsWith("LVP") || r.startsWith("SBB")) {
									losSAP.add(r);
								}else if(r.length() == 16) {
									losDiesiseis.add(r);
								}else {
									losEse.add(r); // S
								}
							}
							if(!losDiesiseis.isEmpty()) {
								toDeleteList.addAll(losSAP);
								toDeleteList.addAll(losEse);
								if(losDiesiseis.size() > 1) {
									losDiesiseis.forEach(pw1::println);
								}
							}else if(!losEse.isEmpty()) {
								toDeleteList.addAll(losSAP);
								if(losEse.size() > 1) {
									losEse.forEach(pw2::println);
								}
							}else if(!losSAP.isEmpty()) {
								if(losSAP.size() > 1) {
									losSAP.forEach(pw3::println);
								}
							}
							losDiesiseis.clear();
							losSAP.clear();
							losEse.clear();
						}
					}
					rugals.clear();
				}
				rugals.add(data[0]);
				prevData = data;
			}
			if(rugals.size() > 1) {
				if(rugals.size() == 2) {
					sap = rugals.get(0).startsWith("LVP") || rugals.get(0).startsWith("SBB") ? rugals.get(0) : rugals.get(1).startsWith("LVP") || rugals.get(1).startsWith("SBB") ? rugals.get(1) : null ;
					p360 = rugals.get(0).length() == 16 ? rugals.get(0) : rugals.get(1).length() == 16 ? rugals.get(1) : null;
					if(sap == null && p360 != null) {
						step = p360.equals(rugals.get(0)) ? rugals.get(1) : rugals.get(0);
					}else if(sap != null && p360 == null) {
						step = sap.equals( rugals.get(0)) ? rugals.get(1) : rugals.get(0);
					}
					if( sap != null && step != null ) {
						toDelete = sap;
					}else if(step != null && p360 != null) {
						toDelete = step;
					}else if(sap != null && p360 != null){
						toDelete = sap;
					}else {
						pw.println( rw.getRw().serializeChunk(prevData) );
					}
					if(toDelete != null) {
						toDeleteList.add(toDelete);
					}
					sap = null;
					step = null;
					p360 = null;
					toDelete = null;
				}else {
					for(String r : rugals) {
						if(r.startsWith("LVP") || r.startsWith("SBB")) {
							losSAP.add(r);
						}else if(r.length() == 16) {
							losDiesiseis.add(r);
						}else {
							losEse.add(r); // S
						}
					}
					if(!losDiesiseis.isEmpty()) {
						toDeleteList.addAll(losSAP);
						toDeleteList.addAll(losEse);
						if(losDiesiseis.size() > 1) {
							losDiesiseis.forEach(pw1::println);
						}
					}else if(!losEse.isEmpty()) {
						toDeleteList.addAll(losSAP);
						if(losEse.size() > 1) {
							losEse.forEach(pw2::println);
						}
					}else if(!losSAP.isEmpty()) {
						if(losSAP.size() > 1) {
							losSAP.forEach(pw3::println);
						}
					}
					losDiesiseis.clear();
					losSAP.clear();
					losEse.clear();
				}
			}
			System.out.println("To delete: " + toDeleteList.size());
			rugals.clear();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("fields", "ProductReference.ReferencedSupplierAid");
			qp.put("pageSize", "5000");
			StringBuilder sb = new StringBuilder();
			int a = 0;
			java.util.Map<String, String> artToProd = new java.util.HashMap<>();
			for(String td : toDeleteList) {
				sb.append( sb.length() == 0 ? "" : ",").append("'").append(td).append("'@1");
				a++;
				if(a % 1000 == 0) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
						artToProd.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
					artToProd.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
				});
				sb.setLength(0);
			}
			java.util.Set<String> prodsToDelete = new java.util.TreeSet<>( artToProd.values() );
			java.util.Map<String, String> qp0 = new java.util.HashMap<>();
			qp0.put("includeObjectsInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")) , 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
			for(String td : prodsToDelete) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + td + "'@1")).put("values", new org.json.JSONArray().put("Eliminada")));
			}
			rh.sendData();
			rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SKU")) , 1000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::println) );
			for(String td : toDeleteList) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + td + "'@1")).put("values", new org.json.JSONArray().put("")));
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
