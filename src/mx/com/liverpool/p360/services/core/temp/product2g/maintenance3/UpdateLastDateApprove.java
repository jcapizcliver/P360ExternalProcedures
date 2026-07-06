package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class UpdateLastDateApprove {
	
	
	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValueLang.Value('FechaUltimaPublicacion',root,\"0000.0000.RK\",'FechaUltimaPublicacion',-1),Product2GCharacteristicValueLang.Value('FirstDateApprove',root,\"0000.0000.RK\",'FirstDateApprove',-1)");
		qp.put("pageSize", "5000");
		qp.put("query", "Product2G.ProductNo startsWith \"1754611\" and (Product2G.StatusModification contains \"Aprobada\" or Product2G.StatusModification contains \"Approved\")");
		java.util.List<String> ids = new java.util.ArrayList<>();
		StringBuilder sb = new StringBuilder();
		int a = 0;
		java.util.Map<String, String[]> productToFirstDateApprove = new java.util.HashMap<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			ids.add(row.getJSONObject("object").getString("id"));
			productToFirstDateApprove.put(values.getString(0), new String[] { values.getJSONArray(1).getString(0), values.getJSONArray(1).getString(0) });
			System.out.println(values);
		});
		qp.clear();
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		qp.put("pageSize", "7000");
		java.util.Map<String, String> qp1 = new java.util.HashMap<>();
		qp1.put("fields", "Article.ProcedeNoProcede");
		qp1.put("pageSize", "7000");
		java.util.Map<String, String> qp2 = new java.util.HashMap<>();
		qp2.put("includeObjectsInProtocol", "false");
		java.util.Map<String, String> varToProd = new java.util.HashMap<>();
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.FirstDateApproved")).put(new org.json.JSONObject().put("identifier", "Article.LastDateApproved")), 1000, request -> rw.writeData("list", "Article", null, qp2, request, System.out::println) );
		for(String id : ids) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
			a++;
			if(a % 1000 == 0) {
				System.out.println("products=" + sb.toString());
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					varToProd.put(row.getJSONObject("object").getString("id"), values.getString(0));
				});
				qp1.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp1, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					if(Boolean.parseBoolean( values.getString(0) )) {
						String productNo = varToProd.get(row.getJSONObject("object").getString("id"));
						if(productNo != null) {
							String[] data = productToFirstDateApprove.get(productNo);
							if(data != null) {
//								System.out.println("Would be adding: " + new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( !"".equals(data[1]) ? data[1] : data[0]).put(data[0])));
								rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( !"".equals(data[1]) ? data[1] : data[0]).put(data[0])));
							}else {
								System.out.println("This had no data collected x.x (" + productNo + ")");
							}
						}else {
							System.out.println("This variant was not collected previously with a parent product: " + row.getJSONObject("object").getString("id"));
						}
					}else {
//						System.out.println("Este no procede: " + row.getJSONObject("object").getString("id"));
					}
				});
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("products", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				varToProd.put(row.getJSONObject("object").getString("id"), values.getString(0));
			});
			qp1.put("products", sb.toString());
			rw.collectData("list", "Article", null, "byProducts", qp1, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(Boolean.parseBoolean( values.getString(0) )) {
					String productNo = varToProd.get(row.getJSONObject("object").getString("id"));
					if(productNo != null) {
						String[] data = productToFirstDateApprove.get(productNo);
						if(data != null) {
//							System.out.println("Would be adding: " + new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( !"".equals(data[1]) ? data[1] : data[0]).put(data[0])));
							rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put( !"".equals(data[1]) ? data[1] : data[0]).put(data[0])));
						}else {
							System.out.println("This had no data collected x.x (" + productNo + ")");
						}
					}else {
						System.out.println("This variant was not collected previously with a parent product: " + row.getJSONObject("object").getString("id"));
					}
				}else {
					System.out.println("Este no procede: " + row.getJSONObject("object").getString("id"));
				}
			});
			sb.setLength(0);
		}
		rh.sendData();
	}

}
