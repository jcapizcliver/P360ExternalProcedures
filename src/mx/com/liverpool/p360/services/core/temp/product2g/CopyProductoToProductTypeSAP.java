package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CopyProductoToProductTypeSAP {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		System.out.println("Loading guys...");
		java.util.Map<String, java.util.Map.Entry<String, String>> productos = getProductos();
		System.out.println("Done loading... " + productos.size() + " vals");
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP',-1)"));
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("rows", rows);
		request.put("columns", columns);

		org.json.JSONArray rowsSBB = new org.json.JSONArray();
		org.json.JSONArray columnsSBB = new org.json.JSONArray();
		columnsSBB.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SB_0002',root,\"0000.0000.RK\",'SB_0002',-1)"));
		org.json.JSONObject requestSBB = new org.json.JSONObject();
		request.put("rows", rows);
		request.put("columns", columns);

		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		for(java.util.Map.Entry<String, java.util.Map.Entry<String, String>> entry : productos.entrySet()) {
			if(!"SBB".equals(entry.getValue().getValue())) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'MASTER'")).put("values", new org.json.JSONArray().put(entry.getValue().getKey())));
				if(rows.length() == 200) {
					response = rw.makeRequest("POST", "/list/Product2G", qp, request.toString());
					System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}else {
				rowsSBB.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'MASTER'")).put("values", new org.json.JSONArray().put(entry.getValue().getKey())));
				if(rowsSBB.length() == 200) {
					response = rw.makeRequest("POST", "/list/Product2G", qp, requestSBB.toString());
					System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
					while(rowsSBB.length() > 0) {
						rowsSBB.remove(0);
					}
				}
			}
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/Product2G", qp, request.toString());
			System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		if(rowsSBB.length() > 0) {
			response = rw.makeRequest("POST", "/list/Product2G", qp, request.toString());
			System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
			while(rowsSBB.length() > 0) {
				rowsSBB.remove(0);
			}
		}
	}

	private static java.util.Map<String, java.util.Map.Entry<String, String>> getProductos(){
		java.util.Map<String, java.util.Map.Entry<String, String>> productos = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("fields", "Product2G.ProductNo,SimpleProduct2GCharacteristicValueLang.Value('Producto',-1),Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code");
		qp.put("query", "not characteristic('Producto',-1) is empty");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					System.out.println(values);
					productos.put(values.getString(0), new java.util.AbstractMap.SimpleEntry<>(values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0)));
				}
			}else {
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return productos;
	}

}
