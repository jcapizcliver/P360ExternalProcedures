package mx.com.liverpool.p360.services.core.net;

import mx.com.liverpool.p360.services.core.PropertiesManager;

public class DataRequestor2 {
	
	public String sendPowerOff() {
		return sendRequest( new org.json.JSONObject().put("action", "quit").put("running", false).toString() );
	}
	public String dump() {
		return sendRequest( new org.json.JSONObject().put("action", "dump").toString() );
	}
	
	public String putProductData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "putProductData").put("items", items).toString() );
	}
	
	public String putArticleData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "putArticleData").put("items", items).toString() );
	}
	
	public String getProductData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "getProductData").put("items", items).toString() );
	}
	
	public String getArticleData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "getArticleData").put("items", items).toString() );
	}
	
	public String putProductExtraData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "putProductExtraData").put("items", items).toString() );
	}
	
	public String putArticleExtraData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "putArticleExtraData").put("items", items).toString() );
	}
	
	public String getProductExtraData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "getProductExtraData").put("items", items).toString() );
	}
	
	public String getArticleExtraData(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "getArticleExtraData").put("items", items).toString() );
	}
	
	public String getProductByVariant(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "productByVariant").put("variants", items).toString() );
	}
	
	public String putSkuSupplierAID(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "skuSupplierAID").put("items", items).toString() );
	}
	
	public String addGlobalMetaData(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "addGlobalMetaData").put("items", items).toString() );
	}
	
	public String getGlobalMetaData() {
		return sendRequest(new org.json.JSONObject().put("action", "getGlobalMetaData").toString() );
	}
	
	public String addTemplateCharacteristicMetaData(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "addTemplateCharacteristicMetaData").put("items", items).toString() );
	}
	
	public String getTemplateCharacteristicMetaDataByTemplate(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "getTemplateCharacteristicMetaDataByTemplate").put("items", items).toString() );
	}
	
	public String getTemplateCharacteristicMetaDataByTemplateCharacteristic(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "getTemplateCharacteristicMetaDataByTemplateCharacteristic").put("items", items).toString() );
	}
	
	public String getTemplateCharacteristicMetaDataByTemplateCharacteristicProperty(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "getTemplateCharacteristicMetaDataByTemplateCharacteristicProperty").put("items", items).toString() );
	}
	
	public String removeGlobalMetaDataEntry(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeGlobalMetaDataEntry").put("items", items).toString() );
	}
	
	public String removeGlobalMetaDataEntryByProperty(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeGlobalMetaDataEntryByProperty").put("items", items).toString() );
	}
	
	public String removeTemplateCharacteristicMetaData(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeTemplateCharacteristicMetaData").put("items", items).toString() );
	}
	
	public String removeTemplateCharacteristicMetaDataByCharacteristic(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeTemplateCharacteristicMetaDataByCharacteristic").put("items", items).toString() );
	}
	
	public String removeTemplateCharacteristicMetaDataByProperty(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeTemplateCharacteristicMetaDataByProperty").put("items", items).toString() );
	}
	
	public String addTemplateName(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "addTemplateName").put("items", items).toString() );
	}
	
	public String getTemplateName(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "getTemplateName").put("items", items).toString() );
	}
	
	public String removeTemplateName(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeTemplateName").put("items", items).toString() );
	}
	
	public String addCharacteristicData(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "addCharacteristicData").put("items", items).toString() );
	}
	
	public String getCharacteristicData(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "getCharacteristicData").put("items", items).toString() );
	}
	
	public String removeCharacteristicData(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeCharacteristicData").put("items", items).toString() );
	}
	
	public String addContenidoDeDiccionario(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "addContenidoDeDiccionario").put("items", items).toString() );
	}
	
	public String getContenidoDeDiccionario(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "getContenidoDeDiccionario").put("items", items).toString() );
	}
	
	public String removeContenidoDeDiccionario(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "removeContenidoDeDiccionario").put("items", items).toString() );
	}
	
	public String retiraEANProductNo(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "retiraEANProductNo").put("items", items).toString());
	}

	public String retiraEANSupplierAID(org.json.JSONArray items) {
		return sendRequest(new org.json.JSONObject().put("action", "retiraEANSupplierAID").put("items", items).toString());
	}
	
	public java.util.Set<String> getVariants(String productNo) {
		java.util.Set<String> s = new java.util.TreeSet<>();
		org.json.JSONArray items = null;
		String resp = sendRequest( new org.json.JSONObject().put("action", "getProductVariants").put("product", productNo).toString() );
		if(resp == null) {
			System.out.println("#######################\t\tTried with: " + new org.json.JSONObject().put("action", "getProductVariants").put("product", productNo).toString());
		}else {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(resp);
				items = jr.getJSONArray("items");
				for(int i=0; i<items.length(); i++) {
					s.add(items.getString(i));
				}
			}catch(org.json.JSONException e) {
				e.printStackTrace();
			}
		}
		return s;
	}
	
	public java.util.Map<String, java.util.Set<String>> articleBySKUs(org.json.JSONArray skus) {
		String resp = sendRequest(
				new org.json.JSONObject()
					.put("action", "variantBySKU")
					.put("skus", skus)
				.toString()
			);
		org.json.JSONObject jsonObject = null;
		java.util.Map<String, java.util.Set<String>> parentChild = new java.util.TreeMap<>();
		java.util.Set<String> lst = null;
		if(resp != null)
			try {
				org.json.JSONObject response = new org.json.JSONObject(resp);
				org.json.JSONArray items = response.getJSONArray("items");
				for(int i = 0; i<items.length(); i++) {
					jsonObject = items.getJSONObject(i);
					if(!"".equals(jsonObject.getString("product")) && !"".equals(jsonObject.getString("article"))) {
						lst = parentChild.get(jsonObject.getString("product"));
						if(lst == null) {
							lst = new java.util.TreeSet<>();
							parentChild.put(jsonObject.getString("product"), lst);
						}
						lst.add(jsonObject.getString("article"));
					}
				}
			}catch(org.json.JSONException e) {
				e.printStackTrace();
			}
		return parentChild;
	}
	
	public java.util.Map<String, java.util.Set<String>> articleBySKUsWithSKUs(org.json.JSONArray skus) {
		String resp = sendRequest(
				new org.json.JSONObject()
					.put("action", "variantBySKU")
					.put("skus", skus)
				.toString()
			);
		org.json.JSONObject jsonObject = null;
		java.util.Map<String, java.util.Set<String>> parentChild = new java.util.TreeMap<>();
		java.util.Set<String> lst = null;
		try {
			org.json.JSONObject response = new org.json.JSONObject(resp);
			org.json.JSONArray items = response.getJSONArray("items");
			for(int i = 0; i<items.length(); i++) {
				jsonObject = items.getJSONObject(i);
				if(!"".equals(jsonObject.getString("product_sku")) && !"".equals(jsonObject.getString("article_sku"))) {
					lst = parentChild.get(jsonObject.getString("product_sku"));
					if(lst == null) {
						lst = new java.util.TreeSet<>();
						parentChild.put(jsonObject.getString("product_sku"), lst);
					}
					lst.add(jsonObject.getString("article_sku")); // < SKU_Papá, [ SKU_Hijo_1, SKU_Hijo_2, ... ] >
				}else if(!"".equals(jsonObject.getString("article_sku"))) {
					lst = parentChild.get(jsonObject.getString("article_sku"));
					if(lst == null) {
						lst = new java.util.TreeSet<>();
						parentChild.put(jsonObject.getString("article_sku"), lst);
					}
					lst.add(jsonObject.getString("article_sku"));
				}
			}
		}catch(org.json.JSONException e) {
			e.printStackTrace();
		}
		return parentChild;
	}
	
	public String productBySKU(org.json.JSONArray skus) {
		String resp = sendRequest(
				new org.json.JSONObject()
					.put("action", "getSkuProductNo")
					.put("items", skus)
				.toString()
			);
		return resp;
	}
	
	public String retiraProducto(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "retiraProducto").put("items", items).toString() );
	}
	
	public String retiraArticulo(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "retiraArticulo").put("items", items).toString() );
	}
	
	public String retiraProductoPorSKU(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "retiraProductoPorSKU").put("items", items).toString() );
	}
	
	public String retiraArticuloPorSKU(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "retiraArticuloPorSKU").put("items", items).toString() );
	}
	
	public String getProductBySKU(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "getSkuProductNo").put("items", items).toString() );
	}
	
	public String skuProductNo(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "skuProductNo").put("items", items).toString() );
	}
	
	public String productNoByEAN(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "productByEAN").put("items", items).toString() );
	}
	
	public String supplierAIDByEAN(org.json.JSONArray items) {
		return sendRequest( new org.json.JSONObject().put("action", "articleByEAN").put("items", items).toString() );
	}
	
	public String articleBySKU(org.json.JSONArray skus) {
		String resp = sendRequest(
				new org.json.JSONObject()
					.put("action", "variantBySKU")
					.put("skus", skus)
				.toString()
			);
		return resp;
	}
	
	protected String sendRequest(String message) {
		String response = null;
		try(
			java.net.Socket socket = new java.net.Socket(PropertiesManager.get("p360.contingency.pvia.host", "localhost"), Integer.parseInt( PropertiesManager.get("p360.contingency.pvia.port", "23540")) );
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream()), true);
			java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
		){
			pw.println(message);
			response = br.readLine();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return response;
	}
	
}
