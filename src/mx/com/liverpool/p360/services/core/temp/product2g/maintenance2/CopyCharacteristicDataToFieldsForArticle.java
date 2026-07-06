package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CopyCharacteristicDataToFieldsForArticle {
	
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
//		rw.getRw().setBaseUrl("https://172.18.237.210:1512/rest/V2.0");
//		rw.getRw().addHeader("Authorization", java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico')->LookupValue.Code"
				+ ",ArticleCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber',-1)"
				+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
				+ ",ArticleCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProcedeNoProcede',root,\"0000.0000.RK\",'ProcedeNoProcede',-1)"
			);
		qp.put("query", "Article.SupplierAID startsWith \"17546116\" and not (Article.CurrentStatus = \"Revisión QA\" or Article.CurrentStatus = \"Category\")");
		qp.put("pageSize", "10000");
		org.json.JSONArray columns = new org.json.JSONArray();
		columns
			.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)"))
			.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.TamanoUnico(MX)"))
			.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SupplierPartNumber(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Article.SKU"))
			.put(new org.json.JSONObject().put("identifier", "Article.EAN"))
			.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Article.Business"))
			.put(new org.json.JSONObject().put("identifier", "Article.ProductImageURL"))
			.put(new org.json.JSONObject().put("identifier", "Article.ProcedeNoProcede"))
		;
//		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler(columns, 10000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::println));
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String color 				= values.getJSONArray(0).getString(0);
			String talla 				= values.getJSONArray(1).getString(0);
			String supplierPartNumber 	= values.getJSONArray(2).getString(0);
			String sku 					= values.getJSONArray(3).getString(0);
			String mainBarCode 			= values.getJSONArray(4).getString(0);
			if("".equals(mainBarCode)) {
				mainBarCode 			= values.getJSONArray(5).getString(0);
			}
			mainBarCode = mainBarCode.replaceAll("\\s+", "").trim();
            mainBarCode = !"".equals(mainBarCode) && mainBarCode.matches("^[0-9]+$") ? mainBarCode : "";
			String sapObjectType 		= values.getJSONArray(6).getString(0);
			String business 			= values.getJSONArray(7).getString(0);
			String productImageURL 		= values.getJSONArray(8).getString(0);
			String procedeNoProcede 	= values.getJSONArray(9).getString(0);
			org.json.JSONObject rw = new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray()
					.put(color)
					.put(talla)
					.put(supplierPartNumber)
					.put(sku)
					.put(mainBarCode)
					.put(sapObjectType)
					.put(business)
					.put(productImageURL)
					.put(procedeNoProcede)
				);
			rh.addRow(rw);
//			rows.put(rw);
//			if(rows.length() == 30) {
//				System.out.println(new org.json.JSONObject().put("columns", columns).put("rows", rows));
//				System.exit(0);
//			}
		}, rr -> {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(rr);
				if(jr.getJSONObject("counters").getInt("errors") > 0) {
					org.json.JSONArray entries = jr.getJSONArray("entries");
					org.json.JSONArray rws = rh.getRows();
					for(int i=0; i<entries.length(); i++) {
						if("Article_ArticleType.Ean".equals(entries.getJSONObject(i).getString("propertyLabel"))) {
							System.out.println("This --->" + rws.getJSONObject(entries.getJSONObject(i).getInt("row")).getJSONArray("values").getString(4));
						}
					}
				}
			}catch(org.json.JSONException e) {
				e.printStackTrace();
			}
		});
		rh.sendData();
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
}
