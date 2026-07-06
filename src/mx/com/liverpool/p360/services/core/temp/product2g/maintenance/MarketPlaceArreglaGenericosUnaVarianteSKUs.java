package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MarketPlaceArreglaGenericosUnaVarianteSKUs {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, java.util.List<String>> vars = new java.util.HashMap<>();
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GLog.CreationDate(PIM),Product2GLog.ModificationDate(PIM)");
		qp.put("query", "Product2G.ProductNo startsWith \"175461166\" and Product2G.CurrentStatus = \"Creación de SKU\" and not characteristic('SKU') is empty");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(row.getJSONArray("values"));
			vars.put(values.getString(0), new java.util.ArrayList<>());
			sb.append(sb.length() == 0 ? "" : ",").append(row.getJSONObject("object").getString("id"));
		});
		qp.clear();
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		qp.put("products", sb.toString());
		rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			java.util.List<String> lst = vars.get(values.getString(0));
			if(lst == null) {
				System.out.println("-.- " + row.getJSONObject("object").getString("id"));
			}else {
				lst.add(row.getJSONObject("object").getString("id"));
			}
		});
		java.util.List<String> participants = new java.util.ArrayList<>();
		for(java.util.Map.Entry<String, java.util.List<String>> entry : vars.entrySet()) {
			if(entry.getValue().size() == 1) {
				participants.add(entry.getKey());
			}
		}
		System.out.println("These: " + participants.size());
//		System.exit(0);
		
		StringBuilder items = new StringBuilder();
		java.util.Set<String> losQueSi = new java.util.TreeSet<>();
		for(String id : participants) {
			items.append(items.length() == 0 ? "" : ",").append("'").append(id).append("'@1");
		}
		qp.clear();
		qp.put("fields", "ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
		qp.put("products", items.toString());
		qp.put("pageSize", "10000");
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(row.getJSONObject("object").getString("id") + " - " + values);
			if(!"".equals(values.getJSONArray(0).getString(0))) {
				losQueSi.add(row.getJSONObject("object").getString("id"));
			}
		});
		
		qp = new java.util.HashMap<>();
		qp.put("items", items.toString());
		qp.put("fields", "Product2G.ProductNo,SimpleProduct2GCharacteristicValueLang.Value('SKU',-1)");
		java.util.Map<String, String> data = new java.util.HashMap<>();
		org.json.JSONObject req = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		req.put("columns", columns);
		req.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"));
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rows.put(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("999" + values.getString(0).substring(9))));
			data.put(values.getString(0), values.getJSONArray(1).getString(0));
		});
		java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
		qp1.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Product2G", null, qp1, req, System.out::println);
		qp.clear();
		qp.put("fields", "Article.SupplierAID");
		qp.put("products", items.toString());
		qp.put("pageSize", "10000");
		for(java.util.Map.Entry<String, String> entry : data.entrySet()) {
			System.out.println(entry.getKey() + ";" + entry.getValue());
		}
		java.util.Map<String, String> qp2 = new java.util.HashMap<>();
		qp2.put("fields", "ProductReference.ReferencedSupplierAid");
		qp2.put("products", items.toString());
		java.util.Map<String, String> rel = new java.util.HashMap<>();
		rw.collectData("list", "Article", "ProductReference", "byProducts", qp2, row -> {
			rel.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
		});
		columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"));
		req.put("columns", columns);
		org.json.JSONArray rows2 = new org.json.JSONArray();
		req.put("rows", rows2);
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			String product = rel.get(row.getJSONObject("object").getString("id"));
			System.out.println("For " + row.getJSONObject("object").getString("id") + ", got: " + product);
			if(product != null) {
				String sku = data.get(product);
				rows2.put(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(sku)));
			}
		});
		rw.writeData("list", "Article", null, qp1, req, System.out::println);
		qp.clear();
		
		columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"));
		org.json.JSONArray rows3 = new org.json.JSONArray();
		req.put("columns", columns);
		req.put("rows", rows3);
		for(String id : participants) {
			rows3.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(losQueSi.contains(vars.get(id).get(0)) ? "Revisión QA" : "Carga de Imagen")));
		}
		data.forEach((k,v)->System.out.println(k + " - " + v));
		rw.writeData("list", "Product2G", null, qp1, req, System.out::println);
	}

}
