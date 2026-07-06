package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class RelacionaSKUs {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> skuToSKU = new java.util.HashMap<>();
		java.util.Map<String, String> skuToPID = new java.util.HashMap<>();
		java.util.Map<String, String> skuToAID = new java.util.HashMap<>();
		
		java.util.Set<String> toCheckA = new java.util.TreeSet<>();
		java.util.Set<String> toCheckP = new java.util.TreeSet<>();
		java.util.Map<String, String> paresBase = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = line.split(",");
				if(pieces.length > 1) {
					toCheckA.add(pieces[0]);
					toCheckP.add(pieces[1]);
					paresBase.put(pieces[0], pieces[1]);
				}else {
					toCheckP.add(pieces[0]);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Article.SupplierAID,Article.SKU");
		qp.put("pageSize", "1200");
		StringBuilder sb = new StringBuilder();
		int cnt = 0;
		for( String a : toCheckA ) {
			sb.append( cnt == 0 ? "" : "," ).append(a);
			cnt++;
			if(cnt % 1000 == 0) {
				qp.put("query", "Article.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Article", null, "bySearch", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					skuToAID.put(values.getString(1), values.getString(0));
				});
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("query", "Article.SKU in (" + sb.toString() + ")");
			rw.collectData("list", "Article", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				skuToAID.put(values.getString(1), values.getString(0));
			});
			sb.setLength(0);
		}
		
		qp.put("fields", "Product2G.ProductNo,Product2G.SKU,Product2GExtraData.SAPObjectType(MX)->LookupValue.Code");
		cnt = 0;
		for( String a : toCheckP ) {
			sb.append( cnt == 0 ? "" : "," ).append(a);
			cnt++;
			if(cnt % 1000 == 0) {
				qp.put("query", "Product2G.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					if("00".equals(values.getString(2))) {
						skuToSKU.put(values.getString(1), values.getString(1));
					} else {
						skuToPID.put(values.getString(1), values.getString(0));
					}
				});
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("query", "Product2G.SKU in (" + sb.toString() + ")");
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if("00".equals(values.getString(2))) {
					skuToSKU.put(values.getString(1), values.getString(1));
				} else {
					skuToPID.put(values.getString(1), values.getString(0));
				}
			});
			sb.setLength(0);
		}
		
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")), 1000, request -> rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println) );
		String pid = null;
		String aid = null;
		for(java.util.Map.Entry<String, String> entry : paresBase.entrySet()) {
			aid = skuToAID.get(entry.getKey());
			pid = skuToPID.get(entry.getValue());
			if(aid == null || pid == null) {
				// PANIC
//				throw new IllegalStateException("No pueeess... " + entry);
				System.out.println( "No pueeeesss.... " + entry + " (PID: " + pid + ", AID: " + aid + ")" );
			}else {
				org.json.JSONObject ob = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + aid + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", pid)).put("values", new org.json.JSONArray().put(pid));
				System.out.println(ob);
				rh.addRow(ob);
				System.out.println();
			}
		}
		
		for(java.util.Map.Entry<String, String> entry : skuToSKU.entrySet()) {
			aid = skuToAID.get(entry.getKey());
			pid = skuToPID.get(entry.getValue());
			if(aid == null || pid == null) {
				// PANIC
//				throw new IllegalStateException("No pueeess (2)... " + entry);
				System.out.println( "No pueeeesss.... " + entry + " (PID: " + pid + ", AID: " + aid + ")" );
			}else {
				org.json.JSONObject ob = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + aid + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", pid)).put("values", new org.json.JSONArray().put(pid));
				System.out.println(ob);
				rh.addRow(ob);
				System.out.println();
			}
		}
		rh.sendData();
	}
	
}
