package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class SetTituloSinMarca {

	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		rw.setBaseUrl(PropertiesManager.get("p360.contingency.base_url"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				 "Product2GCharacteristicValueLang.Value('ProductName', root, \"0000.0000.RK\", 'ProductName', -1)"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName', root, \"0000.0000.RK\", 'BrandName')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H', root, \"0000.0000.RK\", 'BRAND_ID_S4H')->LookupValueLang.Name(es)"
			);
		
//		StringBuilder sb = new StringBuilder();
//		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Migración", "Flujos", "ordenados"))){
//			lns.forEach(s -> {
//				sb.append(sb.length() == 0 ? "'" : ",'").append( s ).append("'@1");
//			});
//		}catch(java.io.IOException e) {
//		}
//		System.out.println(sb.toString());
//		1754611681357938
//		1754611681367311
		qp.put("query", 
				"Product2G.ProductNo in (\"LVP1197212355\",\"LVP1200330711\")"
//				"Product2G.ProductNo in (\"1754611648785764\",\"1754611648785879\",\"1754611648785939\")"
//				"Product2G.ProductNo = \"1698767481648622\""
//				"not characteristic('ProductName',-1) is empty"
				);
//		qp.put("items", sb.toString());
		qp.put("pageSize", "1000");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductName',root,\"0000.0000.RK\",'ProductName',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TituloSinMarca',root,\"0000.0000.RK\",'TituloSinMarca',-1)"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		String productName = null;
		String sinMarca = null;
		String marca = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
//			response = rw.makeRequest("GET", "/list/Product2G/byItems", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					System.out.println(values);
					productName = values.getJSONArray(0).getString(0);
					marca = values.getJSONArray(1).getString(0);
					if("".equals(marca)) {
						marca = values.getJSONArray(2).getString(0);
					}
					productName = productName.replaceAll(" {2,}", " ");
					sinMarca = marca != null && !"".equals(marca) ? productName.replaceAll("(?iu)" + java.util.regex.Pattern.quote(marca), "").replaceAll(" {2,}", " ").trim() : productName;
					System.out.println(productName + "<::>" + sinMarca);
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", rows.getJSONObject(i).getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put(productName).put(sinMarca)));
					if(rowsPayload.length() == 100) {
						rw.makeRequest("POST", "/list/Product2G", empty, request.toString());
						System.out.println(rw.getRawResponse());
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println(rw.getRawResponse());
			}
			System.out.println(a + "/" + b);
		}while(a < b);
		a = 0;
		if(rowsPayload.length() > 0) {
			rw.makeRequest("POST", "/list/Product2G", empty, request.toString());
			System.out.println(rw.getRawResponse());
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}
	
}
