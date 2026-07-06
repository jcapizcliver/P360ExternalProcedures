package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RevisaEstadoPosteriosASKU {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus,Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)->LookupValue.Code");
		StringBuilder sb = new StringBuilder();
		String[] IDS = getIDs();
		for(String id : IDS) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'");
			sb.append(id);
			sb.append("'@1");
		}
		qp.put("items", sb.toString());
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeLabels", "true");
		qp0.put("includeIds", "true");
		qp0.put("entityFilter", "Product2G,Product2GCharacteristicValue");
		java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
			String id = values.getString(0);
			String currentStatus = values.getString(1);
			String fotoTomadaLiverpool = values.getJSONArray(2).getString(0);
			System.out.println("Entering to determine images...");
//			org.json.JSONObject resp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + id + "'@1");
			org.json.JSONObject data = new org.json.JSONObject(); //resp.getJSONObject("_data");
			collectNumberOfImages(id, fotoTomadaLiverpool, data);
			System.out.println("--------->" + data + "<---------");
			rw.getRw().makeRequest("PUT", "/object/Product2G/'" + id + "'@1", qp1, data.toString());
			System.out.println(rw.getRw().getRawResponse());
		});
	}
	
	private static void collectNumberOfImages(String productId, String fotosTomaLiverpool, org.json.JSONObject data) {
		int lacuenta = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"Article.SupplierAID"
				+ ",ProductReference.ReferencedSupplierAid(\"" + productId + "\")"
				+ ",ArticleCharacteristicValueLang.Value(ProductImageDetail,\"0000.0000.RK\",\"0000.0000.RK\",ProductImageDetail_URL,-1)"
				+ ",ArticleCharacteristicValueLang.Value(ProductImage,\"0000.0000.RK\",\"0000.0000.RK\",ProductImage_URL,-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		response = rw.getRw().makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null && response.has("rows")) {
			org.json.JSONArray rows = response.getJSONArray("rows");
			org.json.JSONArray values = null;
			org.json.JSONArray details = null;
			org.json.JSONArray principal = null;
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				details = values.getJSONArray(2);
				principal = values.getJSONArray(3);
				for(int j=0; j<details.length(); j++) {
					if(!"".equals(details.getString(j)))
						lacuenta++;
				}
				for(int j=0; j<principal.length(); j++){
					if(!"".equals(principal.getString(j)))
						lacuenta++;
				}
			}
			System.out.println(fotosTomaLiverpool + " - " + data);
			if("Corregido".equals(fotosTomaLiverpool) || (("N".equals(fotosTomaLiverpool) || "".equals(fotosTomaLiverpool) )&& lacuenta > 0 )) {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1022));
			}else if("Y".equals(fotosTomaLiverpool)) {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1002));
			}else {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1004));
			}
			System.out.println("Status placed: " + data.get("currentStatus"));
		}else {
			System.out.println("ERROR: " + rw.getRw().getRawResponse());
		}
		System.out.println("Done with pictures and status stuff...");
	}
	
	private static final String[] getIDs() {
		java.util.List<String> data = new java.util.ArrayList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", 
				"Product2G.CurrentStatus = \"Carga de Imagen\""
//				"Product2G.ProductNo = \"1698767481740211\""
			);
		qp.put("pageSize", "5000");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> data.add(row.getJSONArray("values").getString(0)));
		return data.toArray(new String[] {});
	}
	
//	private static final String[] IDS = (
//			  "1698767481740452\r\n"
//			+ "1698767481740474\r\n"
//			+ "1698767481744038\r\n"
//			+ "1698767481745014\r\n"
//			+ "1698767481745093\r\n"
//			+ "1698767481756564"
//			).split("\\r\\n");
	
}
