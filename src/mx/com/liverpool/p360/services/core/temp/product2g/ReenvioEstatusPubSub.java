package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ReenvioEstatusPubSub {

	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final java.util.Map<String, String> statusEnum = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> externalStatusEnum = new java.util.TreeMap<>();

	public static void main(String[] args) {
		loadStatusEnum();
		loadExternalStatusEnum();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "Product2G.ProductNo"
				+ ",Product2G.CurrentStatus"
				+ ",Product2G.PrevStatus"
				+ ",Product2G.ExternalStatus->LookupValue.Code"
				+ ",Product2GLog.ModificationDate(HPM)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('SKU',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('MainBarCode',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('MainBarCodeS4H',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('ProductName',-1)"
			);
		qp.put("query", "not Product2G.CurrentStatus is empty");
		qp.put("includeLabel", "true");
		qp.put("pageSize", "10000");
		org.json.JSONArray products = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("products", products);
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String productNo = values.getString(0);
			String currentStatus = statusEnum.get(values.getString(1));
			String prevStatus = statusEnum.get(values.getString(2));
			String extStatus = externalStatusEnum.get(values.getString(3));
			String updatedAt = values.getString(4);
			String sku = values.getJSONArray(5).getString(0);
			String eanLvp = values.getJSONArray(6).getString(0);
			String eanSbb = values.getJSONArray(7).getString(0);
			String productName = values.getJSONArray(8).getString(0);
			org.json.JSONObject product = new org.json.JSONObject();
				/*
				 * 
				    "entityType": "Generic",
		            "externalStatus": "Carga de Imagen",
		            "internalStatus": "Image Load",
		            "nameProduct": "Chamarra de para mujer",
		            "previousStatus": "Purchase Revision",
		            "proposalId": "1754611648591980",
		            "sku": "1187125790",
		            "upcEan": "2050151498735",
		            "updatedAt": "2025-09-29T09:49:26.470Z"
				 * 
				 * */
				product.put("entityType", "Generic");
				product.put("proposalId", productNo);
				product.put("internalStatus", currentStatus == null || currentStatus.isEmpty() ? "" : currentStatus);
				product.put("previousStatus", prevStatus == null || prevStatus.isEmpty() ? "" : prevStatus);
				product.put("externalStatus", extStatus == null || extStatus.isEmpty() ? "" : extStatus);
				product.put("updatedAt", updatedAt);
				product.put("sku", sku);
				product.put("nameProduct", productName);
				product.put("upcEan", eanLvp == null || eanLvp.isEmpty() ? eanSbb : eanLvp);
				products.put(product);
				if(products.length() == 200) {
					sendData(request);
				}
		}, System.out::println);
		if(products.length() > 0) {
			sendData(request);
		}
	}
	
	private static void sendData(org.json.JSONObject jsonResponse) {
		PubSubGCP pub = new PubSubGCP();
		 pub.publishMessage( 
				 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
				 PropertiesManager.get( "p360.contingency.gcp.idmc_put_products" ), 
				 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
				 jsonResponse.toString()
				);
		 org.json.JSONArray products = jsonResponse.getJSONArray("products");
		 while(products.length() > 0) {
			 products.remove(0);
		 }
	}
	
	private static void loadStatusEnum() {
		if (!statusEnum.isEmpty()) {
			return;
		}
		java.util.Map<String, String> headers = rw.getRw().getRc().getHeader();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		try {
			rawResponse = rw.getRw().getRc().getRequest("GET", rw.getRw().getBaseUrl() + "/enum/Enum.Status", null, headers);
			response = new org.json.JSONObject(rawResponse);
			rows = response.getJSONArray("entries");
			for (int i = 0; i < rows.length(); i++) {
				row = rows.getJSONObject(i);
				statusEnum.put(row.getString("key"), row.getString("label"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadExternalStatusEnum() {
		if (!externalStatusEnum.isEmpty()) {
			return;
		}
		java.util.Map<String, String> headers = rw.getRw().getRc().getHeader();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		try {
			do {
				rawResponse = rw.getRw().getRc().getRequest("GET", rw.getRw().getBaseUrl()
						+ "/list/LookupValue/byLookup?lookup=ExternalStatus&fields=LookupValue.Code,LookupValueLang.Name(es)&pageSize=200&startIndex="
						+ currentIndex, null, headers);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for (int i = 0; i < rows.length(); i++) {
					row = rows.getJSONObject(i);
					values = row.getJSONArray("values");
					externalStatusEnum.put(values.getString(0), values.getString(1));
				}
				currentIndex += rows.length();
				totalSize = response.getInt("totalSize");
			} while (currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
