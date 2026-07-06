package mx.com.liverpool.p360.services.core.net;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SendProductAndArticleData extends RESTWrapper {

	public static void main(String[] args) {
		SendProductAndArticleData s = new SendProductAndArticleData();
		s.sendData();
	}
	
	private void sendData() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("pageSize", "20000");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				+ ",Product2G.CurrentStatus"
				+ ",Product2GCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)->LookupValue.Code"
			);
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		System.out.println("Now collecting data...");
		collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("product", values.getString(0));
			obj.put("Section", values.getJSONArray(1).getString(0));
			obj.put("ItemGroup", values.getJSONArray(2).getString(0));
			obj.put("ItemGroupS4H", values.getJSONArray(3).getString(0));
			obj.put("BrandName", values.getJSONArray(4).getString(0));
			obj.put("BRAND_ID_S4H", values.getJSONArray(5).getString(0));
			obj.put("Business", values.getJSONArray(6).getString(0));
			obj.put("SupplierID", values.getJSONArray(7).getString(0));
			obj.put("SKU", values.getJSONArray(8).getString(0));
			obj.put("Template", values.getJSONArray(9).getString(0));
			obj.put("CurrentStatus", values.getString(10));
			obj.put("AssignTakeNoTake", values.getJSONArray(11).getString(0));
			obj.put("SAPObjectType", values.getJSONArray(12).getString(0));
			obj.put("FotoTomadaLiverpool", values.getJSONArray(13).getString(0));
			obj.put("MainBarCode", values.getJSONArray(14).getString(0));
			obj.put("MainBarCodeS4H", values.getJSONArray(15).getString(0));
			items.put(obj);
			if(items.length() == 50000) {
				System.out.println("Sending product2g data... " + items.length() + " items");
				String resp = dr.putProductData(items);
				System.out.println("Got response: " + resp);
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		}, System.out::println);
		if(items.length() > 0) {
			System.out.println("Sending product2g data... " + items.length() + " items");
			String resp = dr.putProductData(items);
			System.out.println("Got response: " + resp);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
		System.out.println("Products collected.");
		qp.put("fields", 
			   "Article.SupplierAID"
			+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico')->LookupValue.Code"
			+ ",ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt')->LookupValue.Code"
			+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
			+ ",ArticleCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
			+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
		);
		System.out.println("Now collecting article data...");
		qp.put("pageSize", "5000");
		collectData("list", "Article", null, "byCatalog", qp, row->{
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("variant", values.getString(0));
			obj.put("ColoursLiverpoolAtt", values.getJSONArray(2).getString(0));
			obj.put("TamanoUnico", values.getJSONArray(1).getString(0));
			obj.put("ProductImage", values.getJSONArray(3).getString(0));
			obj.put("AssignTakeNoTake", values.getJSONArray(4).getString(0));
			obj.put("SKU", values.getJSONArray(5).getString(0));
			obj.put("MainBarCode", "".equals( values.getJSONArray(6).getString(0) ) ? values.getJSONArray(7).getString(0) : values.getJSONArray(6).getString(0));
			obj.put("ProductNo", getProductNo(row.getJSONObject("object").getString("id")));
			items.put(obj);
			if(items.length() == 50000) {
				System.out.println("Going to send article data: " + items.length() + " items");
				String resp = dr.putArticleData(items);
				System.out.println("Got response: " + resp);
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		}, System.out::println);
		if(items.length() > 0) {
			System.out.println("Going to send some data... " + items.length() + " items");
			String resp = dr.putArticleData(items);
			System.out.println("Got response: " + resp);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
		System.out.println("Articles data collected...");
	}
	
	private String getProductNo(String internalId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("entityFilter", "ProductReference");
		org.json.JSONObject response = getRw().makeRequest("GET", "/object/Article/" + internalId, qp, null);
		if(response != null && response.has("_data")) {
			org.json.JSONObject data = response.getJSONObject("_data");
			if(data.has("higherLevelProduct")) {
				return data.getJSONArray("higherLevelProduct").getJSONObject(0).getJSONObject("_qualification").getString("referencedIdentifier");
			}
		}
		return null;
	}
	
}
