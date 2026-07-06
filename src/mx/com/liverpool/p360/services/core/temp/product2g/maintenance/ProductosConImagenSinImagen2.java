package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ProductosConImagenSinImagen2 {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<100; i++) {
			sb.append(",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",'ProductImageDetail_URL',-1)");
		}
		for(int i=0; i<100; i++) {
			sb.append(",ArticleCharacteristicValueLang.Value('Illustration',\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",'Illustration_URL',-1)");
		}
		for(int i=0; i<100; i++) {
			sb.append(",ArticleCharacteristicValueLang.Value('ProductImageSmosh',\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",'ProductImageSmosh_URL',-1)");
		}
		for(int i=0; i<100; i++) {
			sb.append(",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",'ProductImageDetail_Name',-1)");
		}
		for(int i=0; i<100; i++) {
			sb.append(",ArticleCharacteristicValueLang.Value('Illustration',\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",'Illustration_Name',-1)");
		}
		for(int i=0; i<100; i++) {
			sb.append(",ArticleCharacteristicValueLang.Value('ProductImageSmosh',\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",\"0000.00" + (i < 10 ? "0" + i : i) + ".RK\",'ProductImageSmosh_Name',-1)");
		}
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage2',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL2',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_Name',-1)"
				+ sb.toString()
			);
		qp.put("query", "Article.SupplierAID startsWith \"17546116\" and Article.CurrentStatus = \"Aprobada\"");
		qp.put("pageSize", "25000");
		java.util.List<String> ids = new java.util.ArrayList<>();
		org.json.JSONArray characteristicRecords = new org.json.JSONArray();
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			int i=0;
			if("".equals(values.getJSONArray(2).getString(0))) {
				characteristicRecords.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "ProductImage2")))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray()))).put("_children", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "ProductImage_URL2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(1).getString(0)))))).put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "ProductImage_Name2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(3).getString(0))))))));
				for(int j = 4; j<104; j++) {
					if(!"".equals(values.getJSONArray(j).getString(0))) {
						characteristicRecords.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "ProductImageDetail2")))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray()))).put("_children", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "ProductImageDetail_URL2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(j).getString(0)))))).put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "ProductImageDetail_Name2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(j + 300).getString(0))))))));
						i++;
					}
				}
				i = 0;
				for(int j = 104; j<204; j++) {
					if(!"".equals(values.getJSONArray(j).getString(0))) {
						characteristicRecords.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "Illustration2")))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray()))).put("_children", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "Illustration_URL2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(j).getString(0)))))).put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "Illustration_Name2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(j + 300).getString(0))))))));
						i++;
					}
				}
				i = 0;
				for(int j = 204; j<304; j++) {
					if(!"".equals(values.getJSONArray(j).getString(0))) {
						characteristicRecords.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "ProductImageSmosh2")))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray()))).put("_children", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "ProductImageSmosh_URL2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(j).getString(0)))))).put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("recordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("parentRecordKey", "0000.0000.00" + (i < 10 ? "0" + i : i) + ".RK").put("characteristic", new org.json.JSONObject().put("_code", "ProductImageSmosh_Name2"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(values.getJSONArray(j + 300).getString(0))))))));
						i++;
					}
				}
				i = 0;
				ids.add(values.getString(0));
				rw.writeData("PUT", "object", "Article", "'" + values.getString(0) + "'@1", qp0, new org.json.JSONObject().put("_characteristicRecords", characteristicRecords), System.out::println);
//				System.out.println(characteristicRecords);
//				System.out.println(values.getString(0));
//				System.exit(0);
			}
		});
		ids.forEach(System.out::println);
	}

}
