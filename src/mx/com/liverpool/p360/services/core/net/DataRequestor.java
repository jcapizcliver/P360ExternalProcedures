package mx.com.liverpool.p360.services.core.net;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;

public class DataRequestor {

	private static final String DEFAULT_CREATION_TYPE = "CreateProposal";

	private static final String[] PRODUCT_EXTRA_CHARACTERISTICS = new String[] {
			 "supplierShopId"
			,"ProductName"
			,"BuyerRejectionMessage"
			,"SupplierRejectionMessage"
			,"SkuType"
			,"BWSCL"
			,"TImportacion"
			,"Negocio"
			,"EXTWG_S4H"
			,"MesdeEntregadeMercancIa"
			,"Temporada"
			,"BWVOR"
			,"AnoEstacion"
			,"TextoAdicional"
			,"Evento"
			,"CostobrutoSinIVA"
			,"PrecioSugeridocIVA"
			,"Descuento1"
			,"Descuento2"
			,"LABOR"
			,"NORMT"
			,"DescriptionLong"
			,"DescriptionLong2"
			,"Currency"
			,"TypeMainBarCode"
			,"TextoAdicional"
	};

	private static final String[] ARTICLE_EXTRA_CHARACTERISTICS = new String[] {
			 "SkuType"
			,"CostobrutoSinIVA"
			,"PrecioSugeridocIVA"
			,"Descuento1"
			,"Descuento2"
			,"TypeMainBarCode"
	};

	private final DBAccessDataStub dastub;
	
	public DataRequestor(DBAccessDataStub dastub) {
		this.dastub = dastub;
	}
	
	private DBAccessDataStub db() {
		return dastub;
	}

	private String done() {
		return new org.json.JSONObject().put("action", "done").toString();
	}

	private String response(org.json.JSONArray items) {
		return new org.json.JSONObject().put("items", items).toString();
	}

	private String nvl(String value) {
		return value == null ? "" : value;
	}
	public String sendPowerOff() {
		return done();
	}

	public String dump() {
		return done();
	}

	public String putProductData(org.json.JSONArray items) {
		return done();
	}

	public String putArticleData(org.json.JSONArray items) {
		return done();
	}

	public String getProductData(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(db().getProductData(items.getString(i)));
		}
		return response(responseArray);
	}

	public String getArticleData(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(db().getArticleData(items.getString(i)));
		}
		return response(responseArray);
	}

	public String putProductExtraData(org.json.JSONArray items) {
		return done();
	}

	public String putArticleExtraData(org.json.JSONArray items) {
		return done();
	}

	public String getProductExtraData(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(db().getProductExtraData(items.getString(i), PRODUCT_EXTRA_CHARACTERISTICS));
		}
		return response(responseArray);
	}

	public String getArticleExtraData(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(db().getArticleExtraData(items.getString(i), ARTICLE_EXTRA_CHARACTERISTICS));
		}
		return response(responseArray);
	}

	public String getProductByVariant(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(nvl(db().getProductByVariant(items.getString(i))));
		}
		return response(responseArray);
	}

	public String putSkuSupplierAID(org.json.JSONArray items) {
		return done();
	}

	public String addGlobalMetaData(org.json.JSONArray items) {
		return done();
	}

	public String getGlobalMetaData() {
		org.json.JSONArray items = new org.json.JSONArray();
		items.put(db().getGlobalMetadata(DEFAULT_CREATION_TYPE));
		return response(items);
	}

	public String addTemplateCharacteristicMetaData(org.json.JSONArray items) {
		return done();
	}

	public String getTemplateCharacteristicMetaDataByTemplate(org.json.JSONArray items) {
		org.json.JSONArray itemsResponse = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			itemsResponse = db().getTemplateCharacteristicPropertyValue(items.getString(i), DEFAULT_CREATION_TYPE);
		}
		return response(itemsResponse);
	}

	public String getTemplateCharacteristicMetaDataByTemplateCharacteristic(org.json.JSONArray items) {
		org.json.JSONArray itemsResponse = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			org.json.JSONObject item = items.getJSONObject(i);
			String creationType = item.has("creationType") ? item.getString("creationType") : DEFAULT_CREATION_TYPE;
			if(item.has("template") && !"".equals(item.getString("template"))
					&& item.has("characteristic") && !"".equals(item.getString("characteristic"))) {
				itemsResponse = db().getTemplateCharacteristicPropertyValue(
						item.getString("template"),
						item.getString("characteristic"),
						creationType
				);
			}
		}
		return response(itemsResponse);
	}

	public String getTemplateCharacteristicMetaDataByTemplateCharacteristicProperty(org.json.JSONArray items) {
		org.json.JSONArray itemsResponse = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			org.json.JSONObject item = items.getJSONObject(i);
			String creationType = item.has("creationType") ? item.getString("creationType") : DEFAULT_CREATION_TYPE;
			if(item.has("template") && !"".equals(item.getString("template"))
					&& item.has("characteristic") && !"".equals(item.getString("characteristic"))
					&& item.has("property") && !"".equals(item.getString("property"))) {
				itemsResponse = db().getTemplateCharacteristicPropertyValue(
						item.getString("template"),
						item.getString("characteristic"),
						creationType,
						item.getString("property")
				);
			}
		}
		return response(itemsResponse);
	}

	public String removeGlobalMetaDataEntry(org.json.JSONArray items) {
		return done();
	}

	public String removeGlobalMetaDataEntryByProperty(org.json.JSONArray items) {
		return done();
	}

	public String removeTemplateCharacteristicMetaData(org.json.JSONArray items) {
		return done();
	}

	public String removeTemplateCharacteristicMetaDataByCharacteristic(org.json.JSONArray items) {
		return done();
	}

	public String removeTemplateCharacteristicMetaDataByProperty(org.json.JSONArray items) {
		return done();
	}

	public String addTemplateName(org.json.JSONArray items) {
		return done();
	}

	public String getTemplateName(org.json.JSONArray items) {
		org.json.JSONArray itemsResponse = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			itemsResponse.put(db().getTemplateName(items.getString(i)));
		}
		return response(itemsResponse);
	}

	public String removeTemplateName(org.json.JSONArray items) {
		return done();
	}

	public String addCharacteristicData(org.json.JSONArray items) {
		return done();
	}

	public String getCharacteristicData(org.json.JSONArray items) {
		org.json.JSONArray itemsResponse = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			itemsResponse.put(db().getCharacteristicData(items.getString(i)));
		}
		return response(itemsResponse);
	}

	public String removeCharacteristicData(org.json.JSONArray items) {
		return done();
	}

	public String addContenidoDeDiccionario(org.json.JSONArray items) {
		return done();
	}

	public String getContenidoDeDiccionario(org.json.JSONArray items) {
		org.json.JSONArray itemsResponse = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			org.json.JSONObject item = items.getJSONObject(i);
			if(item.has("diccionario") && item.has("idValor")) {
				itemsResponse.put(db().getDictionaryEntry(item.getString("diccionario"), item.getString("idValor")));
			}else {
				itemsResponse.put(new org.json.JSONObject()
						.put("diccionario", item.has("diccionario") ? item.getString("diccionario") : "")
						.put("idValor", item.has("idValor") ? item.getString("idValor") : "")
						.put("structureGroup", "")
						.put("characteristic", "")
						.put("property", "")
						.put("propertyValue", "")
						.put("propertyShortCode", "")
						.put("alternativeValue", ""));
			}
		}
		return response(itemsResponse);
	}

	public String removeContenidoDeDiccionario(org.json.JSONArray items) {
		return done();
	}

	public String retiraEANProductNo(org.json.JSONArray items) {
		return done();
	}

	public String retiraEANSupplierAID(org.json.JSONArray items) {
		return done();
	}

	public java.util.Set<String> getVariants(String productNo) {
		return new java.util.TreeSet<>(db().getProductVariants(productNo));
	}

	public java.util.Map<String, java.util.Set<String>> articleBySKUs(org.json.JSONArray skus) {
		java.util.Map<String, java.util.Set<String>> parentChild = new java.util.TreeMap<>();
		for(int i=0; i<skus.length(); i++) {
			String[] data = db().variantBySKU(skus.getString(i));
			String article = data[1] == null ? "" : data[1];
			String product = data[2] == null ? "" : data[2];
			if(!"".equals(product) && !"".equals(article)) {
				java.util.Set<String> lst = parentChild.get(product);
				if(lst == null) {
					lst = new java.util.TreeSet<>();
					parentChild.put(product, lst);
				}
				lst.add(article);
			}
		}
		return parentChild;
	}

	public java.util.Map<String, java.util.Set<String>> articleBySKUsWithSKUs(org.json.JSONArray skus) {
		java.util.Map<String, java.util.Set<String>> parentChild = new java.util.TreeMap<>();
		for(int i=0; i<skus.length(); i++) {
			String articleSku = skus.getString(i);
			String[] data = db().variantBySKU(articleSku);
			String productSku = data[3] == null ? "" : data[3];
			if(!"".equals(productSku) && !"".equals(articleSku)) {
				java.util.Set<String> lst = parentChild.get(productSku);
				if(lst == null) {
					lst = new java.util.TreeSet<>();
					parentChild.put(productSku, lst);
				}
				lst.add(articleSku);
			}else if(!"".equals(articleSku)) {
				java.util.Set<String> lst = parentChild.get(articleSku);
				if(lst == null) {
					lst = new java.util.TreeSet<>();
					parentChild.put(articleSku, lst);
				}
				lst.add(articleSku);
			}
		}
		return parentChild;
	}

	public String productBySKU(org.json.JSONArray skus) {
		return getProductBySKU(skus);
	}

	public String retiraProducto(org.json.JSONArray items) {
		return done();
	}

	public String retiraArticulo(org.json.JSONArray items) {
		return done();
	}

	public String retiraProductoPorSKU(org.json.JSONArray items) {
		return done();
	}

	public String retiraArticuloPorSKU(org.json.JSONArray items) {
		return done();
	}

	public String getProductBySKU(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(nvl(db().getSkuProductNo(items.getString(i))));
		}
		return response(responseArray);
	}

	public String skuProductNo(org.json.JSONArray items) {
		return done();
	}

	public String productNoByEAN(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(nvl(db().getEanProductNo(items.getString(i))));
		}
		return response(responseArray);
	}

	public String supplierAIDByEAN(org.json.JSONArray items) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<items.length(); i++) {
			responseArray.put(nvl(db().getEanSupplierAid(items.getString(i))));
		}
		return response(responseArray);
	}

	public String articleBySKU(org.json.JSONArray skus) {
		org.json.JSONArray responseArray = new org.json.JSONArray();
		for(int i=0; i<skus.length(); i++) {
			String articleSku = skus.getString(i);
			String[] data = db().variantBySKU(articleSku);
			responseArray.put(new org.json.JSONObject()
					.put("article_sku", articleSku)
					.put("article", data[1] == null ? "" : data[1])
					.put("product", data[2] == null ? "" : data[2])
					.put("product_sku", data[3] == null ? "" : data[3]));
		}
		return response(responseArray);
	}
}
