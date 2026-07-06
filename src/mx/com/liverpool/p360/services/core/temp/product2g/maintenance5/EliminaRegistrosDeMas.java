package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class EliminaRegistrosDeMas {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> toBeGone = new java.util.ArrayList<>();
		java.util.Map<String, String> qpw = new java.util.HashMap<>();
		qpw.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.SKU")), 1000, request -> rw.writeData("list", "Product2G", null, qpw, request, System.out::println) );
		try( java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile()))) ){
			String line = br.readLine();
			String[] pieces = null;
			java.util.List<String> bag = new java.util.ArrayList<>();
			String prevId = null;
			java.util.List<String> losDiesiseis = new java.util.ArrayList<>();
			java.util.List<String> losSAP = new java.util.ArrayList<>();
			java.util.List<String> losEse = new java.util.ArrayList<>();
			while((line = br.readLine()) != null) {
				pieces = line.split(",");
				if(pieces.length > 0) {
					if(pieces.length > 1) {
						if("999".equals(pieces[1])) {
							rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(pieces[0].replaceFirst("S", "999"))));
						}else {
							if(prevId != null && !prevId.equals(pieces[1])) {
								for(String r : bag) {
									if(r.startsWith("LVP") || r.startsWith("SBB")) {
										losSAP.add(r);
									}else if(r.length() == 16) {
										losDiesiseis.add(r);
									}else {
										losEse.add(r);
									}
								}
								if(!losDiesiseis.isEmpty()) {
									toBeGone.addAll(losSAP);
									toBeGone.addAll(losEse);
								}else if(!losEse.isEmpty()) {
									toBeGone.addAll(losSAP);
								}
								losDiesiseis.clear();
								losSAP.clear();
								losEse.clear();
								bag.clear();
							}
							bag.add(pieces[0]);
							prevId = pieces[1];
						}
					}
				}
			}
			if(!bag.isEmpty()) {
				for(String r : bag) {
					if(r.startsWith("LVP") || r.startsWith("SBB")) {
						losSAP.add(r);
					}else if(r.length() == 16) {
						losDiesiseis.add(r);
					}else {
						losEse.add(r);
					}
				}
				if(!losDiesiseis.isEmpty()) {
					toBeGone.addAll(losSAP);
					toBeGone.addAll(losEse);
				}else if(!losEse.isEmpty()) {
					toBeGone.addAll(losSAP);
				}
				losDiesiseis.clear();
				losSAP.clear();
				losEse.clear();
				bag.clear();
			}
			rh.sendData();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("fields", "ProductReference.ReferencedSupplierAid");
			qp.put("pageSize", "5000");
			StringBuilder sb = new StringBuilder();
			int a = 0;
			rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 1000, request -> rw.writeData("list", "Product2G", null, qpw, request, System.out::println) );
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("Foregone").toFile())))){
				for( String tbg : toBeGone ) {
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + tbg + "'@1")).put("values", new org.json.JSONArray().put("Eliminada")));
					sb.append(sb.length() == 0 ? "" : ",").append("'").append(tbg).append("'@1");
					a++;
					if(a == 1000) {
						qp.put("products", sb.toString());
						rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
							pw.println( row.getJSONObject("object").getString("id") + "," + row.getJSONArray("values").getString(0) );
						});
						a = 0;
						sb.setLength(0);
					}
				}
				if(a > 0) {
					qp.put("products", sb.toString());
					rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
						pw.println( row.getJSONObject("object").getString("id") + "," + row.getJSONArray("values").getString(0) );
					});
					a = 0;
					sb.setLength(0);
				}
				rh.sendData();
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
