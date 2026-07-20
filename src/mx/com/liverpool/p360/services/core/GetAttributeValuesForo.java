package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.temp.exports.RealExportProducts;

public class GetAttributeValuesForo {

	private static final RESTWrapper rw = new RESTWrapper();

	private static final java.util.Set<String> atributosInternet = new java.util.TreeSet<>();
	private static final java.util.Set<String> atributosSAP = new java.util.TreeSet<>();
	private final java.util.concurrent.ConcurrentLinkedQueue<org.json.JSONObject> responses = new java.util.concurrent.ConcurrentLinkedQueue<>();
	
	private static java.util.Map<String, String> seleccionaLasDesas(String attributeGroup, String baseUrl, String auth){
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.setBaseUrl(baseUrl);
		workshop.addHeader("Authorization", auth);
		java.util.Map<String, String> lasdesas = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "Characteristics");
		qp.put("query", "LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code in (\"" + attributeGroup + "\")");
		qp.put("fields", "LookupValue.Code,LookupValueIdentifier.Code(ECC)");
		qp.put("pageSize", "250");

		int currentIndex = 0;
		int totalSize = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					lasdesas.put(values.getString(0),values.getString(1));
				}
			}else{
				LOGGER.info( workshop.getRawResponse() );
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return lasdesas;
	}

	
	private class Worker implements Runnable{
		
		private final int id;
		private boolean running = true;
		private AgarraloONo aono = new AgarraloONo();
		private final java.util.concurrent.ArrayBlockingQueue<Object[]> tasks;
		
		org.json.JSONObject product = null;
		org.json.JSONArray characteristicRecords = null;
		org.json.JSONArray classifications = null;
		org.json.JSONObject characteristicRecord = null;
		String template = null;
		String characteristicId = null;
		org.json.JSONArray liverpoolManuals = new org.json.JSONArray();
		org.json.JSONArray ownerManuals = new org.json.JSONArray();
		org.json.JSONArray productVideos = new org.json.JSONArray();
		org.json.JSONArray noms = new org.json.JSONArray();
		String childCharId = null;
		String key = null;
		String value = null;
		org.json.JSONObject sap = new org.json.JSONObject();
		org.json.JSONObject internet = new org.json.JSONObject();
		org.json.JSONObject readOnly = new org.json.JSONObject();
		org.json.JSONObject sapVariante = new org.json.JSONObject();
		org.json.JSONObject internetVariante = new org.json.JSONObject();
		org.json.JSONObject readOnlyVariante = new org.json.JSONObject();

		org.json.JSONArray variantResponses = null;
		org.json.JSONObject variantResponse = null;
		org.json.JSONObject photos = null;
		org.json.JSONObject productImage = null;
		org.json.JSONArray detailImages = null;
		org.json.JSONArray smoshImages = null;
		org.json.JSONArray isometrics = null;
		
		org.json.JSONObject cr = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray values = null;
		String productSKU;
		String productEAN;
		java.util.ArrayList<String> otrosDeInteres;
		RestClient rc = null;
		
		public Worker(int id, java.util.concurrent.ArrayBlockingQueue<Object[]> tasks) {
			this.id = id;
			this.tasks = tasks;
			rc = rw.getRw().getRc();


			otrosDeInteres = new java.util.ArrayList<>( java.util.Arrays.asList( (
					  "Name\r\n"
					+ "ColoursLiverpoolAtt\r\n"
					+ "TamanoUnico\r\n"
					+ "SupplierName\r\n"
					+ "SkuType\r\n"
					+ "MTART_S4H\r\n"
					+ "CompletenessAttSAP\r\n"
					+ "UniversalMainBarCode\r\n"
					+ "MainBarCode\r\n"
					+ "MainBarCodeS4H\r\n"
					+ "ParentSKU\r\n"
					+ "Direction\r\n"
					+ "StateSKU\r\n"
					+ "SupplierPartNumber\r\n"
					+ "Section\r\n"
					+ "ItemGroup2\r\n"
					+ "ItemGroup\r\n"
					+ "ItemGroupS4H\r\n"
					+ "BrandNameATG\r\n"
					+ "BrandName\r\n"
					+ "BRAND_ID_S4H\r\n"
					+ "TipoDeToma\r\n"
					+ "GeneroVAD\r\n"
					+ "GenderAtt\r\n"
					+ "ProductType\r\n"
					+ "ObjectTypeName\r\n"
					+ "FirstDateApprove\r\n"
					+ "SKU\r\n"
					+ "StylistWorld\r\n"
					+ "SupplierID\r\n"
					+ "SupplierName\r\n"
					+ "ProductName\r\n"
					+ "DescriptionLong\r\n"
					+ "refundPolicy\r\n"
					+ "EmbedCodeWEB\r\n"
					+ "EmbedCodeWAP\r\n"
					+ "AssignTakeNoTake\r\n"
					+ "AssignTakeNoTakeReason\r\n"
					+ "AssignTakeNoTakeVideo").split("\\r\\n")));
		}
		
		@Override
		public void run() {
			Object[] pn = null;
			while(running) {
				try {
					pn = tasks.poll(10, java.util.concurrent.TimeUnit.MILLISECONDS);
					if(pn != null) {
						log("Checandole...");
						aono.checale((String)pn[0], rw.getRw().getBaseUrl(), (java.util.Set<String>)pn[1]);
						try {
							processRestOfIt((String)pn[0], (java.util.Set<String>)pn[1]);
						} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException | ServiceUnavailableException e) {
							e.printStackTrace();
						} catch(org.json.JSONException e) {
							log("Excp");
							e.printStackTrace();
							log("RR: " + rawResponse);
						}
					}else {
						if(tasks.isEmpty()) {
							setRunning(false);
						}
					}
				}catch(InterruptedException | JSONException | ServiceUnavailableException e) {
					e.printStackTrace();
				}
			}
			log("(" + id + ") Now exiting... ");
		}
		
		public void setRunning(boolean running) {
			this.running = running;
		}
		
		private void processRestOfIt(String currentProductId, java.util.Set<String> losesos) throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException, ServiceUnavailableException {
			response = null;
			productSKU = null;
			productEAN = null;
			rawResponse = rc.getRequest("GET", rw.getRw().getBaseUrl() + "/object/Product2G/'" + currentProductId + "'@'MASTER'?includeIds=true&includeLabels=true&entityFilter=Product2G,Product2GCharacteristicValue,Product2GStructureGroupMap", null);
			product = new org.json.JSONObject(rawResponse);
			log("CURE: " + currentProductId);
			log("# " + id + ". Querying " + currentProductId);
			String descriptionLong = null;
			String descriptionLong2 = null;
			String embedCodeWEB = null;
			String embedCodeWAP = null;
			String refundPolicy = null;
			String gtin = null;
			if(product.has("_data")) {
				log("# " + id + ", had _data (" + currentProductId + ")");
				characteristicRecords = product.getJSONObject("_data").getJSONArray("_characteristicRecords");
				classifications = product.getJSONObject("_data").getJSONArray("structureGroupMap");
				template = getPrimaryProductTaxonomyTemplate(classifications);
				String externalVariantId = null;
				String currentStatus = null;
				String prevStatus = null;
				String externalStatus = null;
				currentStatus = product.getJSONObject("_data").has("currentStatus") ? product.getJSONObject("_data").getJSONObject("currentStatus").getString("_label") : "";
				prevStatus = product.getJSONObject("_data").has("previousStatus") ? product.getJSONObject("_data").getJSONObject("previousStatus").getString("_label") : "";
				externalStatus = product.getJSONObject("_data").has("externalStatus") ? product.getJSONObject("_data").getJSONObject("externalStatus").getString("_label") : "";
				response = new org.json.JSONObject();
				response.put("currentStatus", currentStatus);
				response.put("previousStatus", prevStatus);
				response.put("externalStatus", externalStatus);
				if(product.getJSONObject( "_data" ).has("lang")) {
		       	  org.json.JSONArray lang = product.getJSONObject("_data" ).getJSONArray("lang");
		       	  org.json.JSONObject innerObject = null;
		       	  for(int index=0; index<lang.length(); index++) {
		       		  innerObject = lang.getJSONObject(index);
		       		  if(innerObject.has("descriptionLong") && "esl".equals(innerObject.getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
		       			 descriptionLong = innerObject.getString("descriptionLong");
		      		  }
		       		  if(innerObject.has("descriptionLong2") && "esl".equals(innerObject.getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
		      			  descriptionLong2 = innerObject.getString("descriptionLong2");
		      		  }
		      	  }
			    }
				if(product.getJSONObject( "_data" ).has( "embedCodeWEB" )){
		      	  embedCodeWEB = product.getJSONObject( "_data" ).getString( "embedCodeWEB" );
		        }
				if(product.getJSONObject( "_data" ).has( "embedCodeWAP" )){
		      	  embedCodeWAP = product.getJSONObject( "_data" ).getString( "embedCodeWAP" );
		        }
				if(product.getJSONObject( "_data" ).has( "refundPolicy" )){
		      	  refundPolicy = product.getJSONObject( "_data" ).getString( "refundPolicy" );
		        }
				if(product.getJSONObject( "_data" ).has( "gtin" )){
					gtin = product.getJSONObject( "_data" ).getString( "gtin" );
				}
				if(descriptionLong != null)
					readOnly.put("DescriptionLong", new org.json.JSONObject().put("value", descriptionLong));
				if(descriptionLong2 != null)
					readOnly.put("DescriptionLong2", new org.json.JSONObject().put("value", descriptionLong2));
				if(embedCodeWEB != null)
					readOnly.put("EmbedCodeWEB", new org.json.JSONObject().put("value", embedCodeWEB));
				if(embedCodeWAP != null)
					readOnly.put("EmbedCodeWAP", new org.json.JSONObject().put("value", embedCodeWAP));
				if(refundPolicy != null)
					readOnly.put("refundPolicy", new org.json.JSONObject().put("value", refundPolicy));
				log("# " + id + ", so far: " + response);
				for(int j=0; j<characteristicRecords.length(); j++) {
					characteristicRecord = characteristicRecords.getJSONObject(j);
					characteristicId = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
					if( atributosSAP.contains(characteristicId) ) {
						values = characteristicRecord.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
						if( values.get(0) instanceof org.json.JSONObject ) {
							key = values.getJSONObject(0).getString("_code");
							value = values.getJSONObject(0).getString("_label");
							sap.put(characteristicId, new org.json.JSONObject().put("code", key).put("value", value));
						}else {
							value = values.getString(0);
							sap.put(characteristicId, new org.json.JSONObject().put("value", value));
						}
					}else if( atributosInternet.contains(characteristicId) ) {
						values = characteristicRecord.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
						if( values.get(0) instanceof org.json.JSONObject ) {
							key = values.getJSONObject(0).getString("_code");
							value = values.getJSONObject(0).getString("_label");
							internet.put(characteristicId, new org.json.JSONObject().put("code", key).put("value", value));
						}else {
							value = values.getString(0);
							internet.put(characteristicId, new org.json.JSONObject().put("value", value));
						}
					}else if("ProductVideo".equals(characteristicId)) {
						cr = getMediaElement(characteristicRecord);
						if(cr != null) {
							productVideos.put( cr );
						}
					}else if("LiverpoolManual".equals(characteristicId)) {
						cr = getMediaElement(characteristicRecord);
						if(cr != null) {
							liverpoolManuals.put( cr );
						}
					}else if("OwnersManual".equals(characteristicId)) {
						cr = getMediaElement(characteristicRecord);
						if(cr != null) {
							ownerManuals.put( cr );
						}
					}else if("NOM".equals(characteristicId)) {
						cr = getMediaElement(characteristicRecord);
						if(cr != null) {
							noms.put( cr );
						}
					}else if(otrosDeInteres.contains(characteristicId)) {
						values = characteristicRecord.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
						if("SKU".equals(characteristicId)) {
							productSKU = values.getString(0);
							readOnly.put(characteristicId, new org.json.JSONObject().put("value", values.getString(0)));
						}else if( gtin == null && ( "MainBarCode".equals(characteristicId) || "MainBarCodeS4H".equals(characteristicId) ) ){
							gtin = values.getString(0);
						}else {
							if( values.get(0) instanceof org.json.JSONObject ) {
								key = values.getJSONObject(0).getString("_code");
								value = values.getJSONObject(0).getString("_label");
								readOnly.put(characteristicId, new org.json.JSONObject().put("code", key).put("value", value));
							}else {
								value = values.getString(0);
								readOnly.put(characteristicId, new org.json.JSONObject().put("value", value));
							}
						}
					}
					cr = null;
				}
				DataRequestor dr = new DataRequestor();
				if(gtin != null)
					readOnly.put("MainBarCode", new org.json.JSONObject().put("value", gtin));
				else {
					java.util.Set<String> varIds = dr.getVariants(currentProductId);
					if(varIds != null && varIds.size() == 1) {
						String rp = dr.getArticleData(new org.json.JSONArray().put(varIds.toArray(new String[] {})[0]));
						try {
							org.json.JSONObject jr = new org.json.JSONObject(rp);
							org.json.JSONArray itms = jr.getJSONArray("items");
							org.json.JSONObject item = itms.getJSONObject(0);
							gtin = item.getString("MainBarCode");
							if("".equals(gtin)) {
								gtin = item.getString("MainBarCodeS4H");
							}
							if(gtin != null)
								readOnly.put("MainBarCode", new org.json.JSONObject().put("value", gtin));
						}catch(org.json.JSONException | NullPointerException e) {
							log("Problem collecting data from admin (" + currentProductId + "|" + varIds.toArray(new String[] {})[0] + ")");
						}
					}
				}
				java.util.Set<String> internalArticleIds = dr.getVariants(currentProductId);
				variantResponses = new org.json.JSONArray();
				if(!internalArticleIds.isEmpty()) {
					String vs = null;
					String ve = null;
					for(String internalArticleId : internalArticleIds) {
						if(!"".equals(internalArticleId)) {
							vs = null;
							ve = null;
							rawResponse = rc.getRequest("GET", rw.getRw().getBaseUrl() + "/object/Article/" + rw.getRw().encode("'" + internalArticleId + "'@1") + "?includeIds=true&includeLabels=true&entityFilter=Article,ArticleCharacteristicValue", null);
							product = new org.json.JSONObject(rawResponse);
							characteristicRecords = product.getJSONObject("_data").has("_characteristicRecords") ? product.getJSONObject("_data").getJSONArray("_characteristicRecords") : new org.json.JSONArray();
							externalVariantId = product.getJSONObject("_data").getString("identifier");
							variantResponse = new org.json.JSONObject();
							currentStatus = product.getJSONObject("_data").has("currentStatus") ? product.getJSONObject("_data").getJSONObject("currentStatus").getString("_label") : "";
							prevStatus = product.getJSONObject("_data").has("previousStatus") ? product.getJSONObject("_data").getJSONObject("previousStatus").getString("_label") : "";
							externalStatus = product.getJSONObject("_data").has("externalStatus") ? product.getJSONObject("_data").getJSONObject("externalStatus").getString("_label") : "";
							variantResponse.put("currentStatus", currentStatus);
							variantResponse.put("previousStatus", prevStatus);
							variantResponse.put("externalStatus", externalStatus);
							
							detailImages = new org.json.JSONArray();
							isometrics = new org.json.JSONArray();
							smoshImages = new org.json.JSONArray();
							for(int k=0; k<characteristicRecords.length(); k++) {
								characteristicRecord = characteristicRecords.getJSONObject(k);
								characteristicId = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								if( atributosSAP.contains(characteristicId) ) {
									values = characteristicRecord.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
									if( values.get(0) instanceof org.json.JSONObject ) {
										key = values.getJSONObject(0).getString("_code");
										value = values.getJSONObject(0).getString("_label");
										sapVariante.put(characteristicId, new org.json.JSONObject().put("code", key).put("value", value));
									}else {
										value = values.getString(0);
										sapVariante.put(characteristicId, new org.json.JSONObject().put("value", value));
									}
								}else if( atributosInternet.contains(characteristicId) ) {
									values = characteristicRecord.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
									if( values.get(0) instanceof org.json.JSONObject ) {
										key = values.getJSONObject(0).getString("_code");
										value = values.getJSONObject(0).getString("_label");
										internetVariante.put(characteristicId, new org.json.JSONObject().put("code", key).put("value", value));
									}else {
										value = values.getString(0);
										internetVariante.put(characteristicId, new org.json.JSONObject().put("value", value));
									}
								}else if("ProductImage".equals(childCharId)) {
									cr = getMediaElement(characteristicRecord);
									if(cr != null) {
										productImage = cr;
									}
								}else if("ProductImageDetail".equals(childCharId)) {
									cr = getMediaElement(characteristicRecord);
									if(cr != null) {
										detailImages.put( cr );
									}
								}else if("Illustration".equals(childCharId)) {
									cr = getMediaElement(characteristicRecord);
									if(cr != null) {
										isometrics.put( cr );
									}
								}else if("ProductImageSmosh".equals(childCharId)) {
									cr = getMediaElement(characteristicRecord);
									if(cr != null) {
										smoshImages.put( cr );
									}
								}else if(otrosDeInteres.contains(characteristicId)) {
									values = characteristicRecord.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
									if( values.get(0) instanceof org.json.JSONObject ) {
										key = values.getJSONObject(0).getString("_code");
										value = values.getJSONObject(0).getString("_label");
										readOnlyVariante.put(characteristicId, new org.json.JSONObject().put("code", key).put("value", value));
									}else {
										value = values.getString(0);
										readOnlyVariante.put(characteristicId, new org.json.JSONObject().put("value", value));
									}
									if("MainBarCode".equals(characteristicId) || "MainBarCodeS4H".equals(characteristicId)) {
										ve = values.getString(0);
									}
									if("SKU".equals(characteristicId)) {
										vs = values.getString(0);
									}
								}
							}
							if(vs == null || "".equals(vs)) {
								if(product.getJSONObject("_data").has("sku")) {
									vs = String.valueOf( product.getJSONObject("_data").getInt("sku") );
								}
							}
							if(ve == null || "".equals(ve)) {
								if(product.getJSONObject("_data").has("gtin")) {
									ve = product.getJSONObject("_data").getString("gtin");
								}
							}
							if(vs != null && !"".equals(vs)) {
								readOnlyVariante.put("SKU", new org.json.JSONObject().put("value", vs));
							}
							if(ve != null && !"".equals(ve)) {
								readOnlyVariante.put("MainBarCode", new org.json.JSONObject().put("value", ve));
							}
							log("Elesecau: " + externalVariantId + " <::>" + losesos + "<::>");
							if(losesos.contains(externalVariantId)) {
								String tomarNoTomar = readOnlyVariante.getJSONObject("AssignTakeNoTake").getString("value");
								if("TOMADO".equals(tomarNoTomar)) {
									readOnlyVariante.getJSONObject("AssignTakeNoTake").put("value", "NO TOMAR");
								}
							}
							if(!"".equals(productSKU) && productSKU != null) {
								readOnlyVariante.put("ParentSKU", new org.json.JSONObject().put("value", productSKU));
							}
							photos = new org.json.JSONObject();
							photos.put("ProductImage", productImage);
							if(detailImages.length() > 0) {
								photos.put("ProductImageDetail", detailImages);
							}
							if(isometrics.length() > 0) {
								photos.put("Illustration", isometrics);
							}
							if(smoshImages.length() > 0) {
								photos.put("ProductImageSmosh", smoshImages);
							}
							variantResponse.put("sap", sapVariante);
							variantResponse.put("internet", internetVariante);
							variantResponse.put("readOnly", readOnlyVariante);
							variantResponse.put("photos", photos);
							variantResponse.put("variantId", externalVariantId);
							variantResponses.put(variantResponse);
		
							sapVariante = new org.json.JSONObject();
							internetVariante = new org.json.JSONObject();
							readOnlyVariante = new org.json.JSONObject();
						}
					}
	
				}
			}
			if(productVideos.length() > 0) {
				response.put("productVideos", productVideos);
				productVideos = new org.json.JSONArray();
			}
			if(liverpoolManuals.length() > 0) {
				response.put("liverpoolManuals", liverpoolManuals);
				liverpoolManuals = new org.json.JSONArray();
			}
			if(ownerManuals.length() > 0) {
				response.put("ownerManuals", ownerManuals);
				ownerManuals = new org.json.JSONArray();
			}
			if(noms.length() > 0) {
				response.put("noms", noms);
				noms = new org.json.JSONArray();
			}
			if(variantResponses.length() > 0) {
				response.put("variants", variantResponses);
			}
			if(sap.length() > 0) {
				response.put("SAP", sap);
				sap = new org.json.JSONObject();
			}
			if(internet.length() > 0) {
				response.put("Internet", internet);
				internet = new org.json.JSONObject();
			}
			if(readOnly.length() > 0) {
				response.put("readOnly", readOnly);
				readOnly = new org.json.JSONObject();
			}
			response.put("productId", currentProductId);
			response.put("template", template);
			log("# " + id + " Preparing: " + response);
			responses.add(response);
		}
		
	}
	
	public Object agrupamelos(String rawRequest, String baseURL, String encoded) {
		long init = System.currentTimeMillis();
		org.json.JSONObject generalResponse = null;
		org.json.JSONObject request = null;
		try{
			org.json.JSONObject hola = new org.json.JSONObject(rawRequest);
			request = hola.has("root") ? hola.getJSONObject("root") : hola;
		}catch(org.json.JSONException e) {
			logE(e);
			System.out.println(generalResponse = new org.json.JSONObject().put("Error", "Bad request"));
			return generalResponse;
		}
		org.json.JSONArray products = null;
		try{
			products = request.getJSONArray("products");
		}catch(org.json.JSONException e) {
			System.out.println(generalResponse = new org.json.JSONObject().put("Error", "Missing array \"products\""));
			return generalResponse;
		}
		if(products.length() == 0) {
			log("" + (generalResponse = new org.json.JSONObject().put("Responses", new org.json.JSONArray())));
			return generalResponse;
		}
		try{
			String sku = null;
			DataRequestor dr = new DataRequestor();
			java.util.Map<String, java.util.Set<String>> skusResponse = null;
			org.json.JSONArray skus = new org.json.JSONArray();
			org.json.JSONObject productElement = null;
			log("Going over: " + products.length() + " products.");
			for(int i=0; i<products.length(); i++) {
				productElement = products.getJSONObject(i);
				if(productElement.has("sku")) {
					sku = productElement.getString("sku");
					skus.put(sku);
				}
			}
			// [ "222221", "111112", "3333332", "444442", "555554" ]
			log("About to request data: " + skus.length() + " for SKUs ||");
			skusResponse = articleBySKUsWithSKUs(skus); // dr.articleBySKUsWithSKUs(skus);
			log("Got a response of: " + skusResponse.size() + " products in total. Now iterating... ||");
			org.json.JSONArray responses = new org.json.JSONArray();
			org.json.JSONArray variants = null;
			for(java.util.Map.Entry<String, java.util.Set<String>> entry : skusResponse.entrySet()) {
				variants = new org.json.JSONArray();
				for(String v : entry.getValue()) {
					variants.put(v);
				}
				responses.put(new org.json.JSONObject().put("skuPadre", entry.getKey()).put("skusHijo", variants));
			}
			return responses;
		}catch(Exception e) {
			logE(e);
			log((generalResponse = new org.json.JSONObject().put("Error", "Couldn't parse request")).toString());
		}
		log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		return generalResponse;
	}
	

	
	public java.util.Map<String, java.util.Set<String>> articleBySKUsWithSKUs(org.json.JSONArray skus) {
		log("Hola");
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
			log("RESP: " + resp);
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
			logE(e);
		}
		return parentChild;
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
	
	public Object procesamelo(String rawRequest, String baseURL, String encoded) {
		long init = System.currentTimeMillis();
		org.json.JSONObject generalResponse = null;
		org.json.JSONObject request = null;
		try{
			log("Parsing: " + rawRequest);
			org.json.JSONObject hola = new org.json.JSONObject(rawRequest);
			request = hola.has("root") ? hola.getJSONObject("root") : hola;
		}catch(org.json.JSONException e) {
			logE(e);
			log("" + (generalResponse = new org.json.JSONObject().put("Error", "Bad request")));
			return generalResponse;
		}
		org.json.JSONArray products = null;
		try{
			products = request.getJSONArray("products");
		}catch(org.json.JSONException e) {
			log(generalResponse = new org.json.JSONObject().put("Error", "Missing array \"products\""));
			return generalResponse;
		}
		if(products.length() == 0) {
			log("" + (generalResponse = new org.json.JSONObject().put("Responses", new org.json.JSONArray())));
			return generalResponse;
		}
		try{
			String sku = null;
			DataRequestor dr = new DataRequestor();
			java.util.Map<String, java.util.Set<String>> skusResponse = null;
			org.json.JSONArray skus = new org.json.JSONArray();
			org.json.JSONObject productElement = null;
			log("Going over: " + products.length() + " products.");
			for(int i=0; i<products.length(); i++) {
				productElement = products.getJSONObject(i);
				if(productElement.has("sku")) {
					sku = productElement.getString("sku");
					skus.put(sku);
				}
			}
			log("About to request data: " + skus.length() + " for SKUs");
			skusResponse = articleBySKUs(skus);// dr.articleBySKUs(skus);
			log("Got a response of: " + skusResponse.size() + " products in total. Now iterating...");
//			int nap = Runtime.getRuntime().availableProcessors();
//			nap = nap <= 0 ? 2 : nap;
			int nap = 1;
			log("Using " + nap + " panas.");
			java.util.concurrent.ArrayBlockingQueue<Object[]> tasks = new java.util.concurrent.ArrayBlockingQueue<>(skus.length());
			Worker[] workers = new Worker[nap];
			Thread[] ts = new Thread[nap];
			for(int i=0; i<workers.length; i++) {
				workers[i] = new Worker(i+1, tasks);
				ts[i] = new Thread( workers[i] );
				ts[i].setPriority(Thread.currentThread().getPriority() - 1);
				ts[i].setDaemon(false);
				ts[i].start();
			}
			for(java.util.Map.Entry<String, java.util.Set<String>> entry : skusResponse.entrySet()) {
				tasks.add(new Object[] { entry.getKey(), entry.getValue() });
			}
			log("Now dequeuing the queue");
			for(int i=0; i<workers.length; i++) {
				try {
					ts[i].join();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			org.json.JSONArray responses = new org.json.JSONArray();
			for(org.json.JSONObject j : this.responses) {
				responses.put(j);
			}
			return responses;
		}catch(Exception e) {
			logE(e);
			log((generalResponse = new org.json.JSONObject().put("Error", "Couldn't parse request")).toString());
		}
		log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		return generalResponse;
	}
	
	public java.util.Map<String, java.util.Set<String>> articleBySKUs(org.json.JSONArray skus) {
		log("Holitas");
		String resp = sendRequest(
				new org.json.JSONObject()
					.put("action", "variantBySKU")
					.put("skus", skus)
				.toString()
			);
		log("Resp: " + resp);
		org.json.JSONObject jsonObject = null;
		java.util.Map<String, java.util.Set<String>> parentChild = new java.util.TreeMap<>();
		java.util.Set<String> lst = null;
		try {
			org.json.JSONObject response = new org.json.JSONObject(resp);
			log("Response: " + response);
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
			logE(e);
		}
		return parentChild;
	}
	
	private String getPrimaryProductTaxonomyTemplate(org.json.JSONArray classifications){
		org.json.JSONObject classification = null;
		String externalId = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("'(EU4\\-[0-9]+)'");
		java.util.regex.Matcher m = null;
		for(int i=0; i<classifications.length(); i++) {
			classification = classifications.getJSONObject(i);
			externalId = classification.getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId");
			if(externalId.endsWith("'PrimaryProductTaxonomy'")) {
				m = p.matcher(externalId);
				if(m.find()) {
					return m.group(1);
				} else {
					log("Could not find a match in: " + externalId);
					return null;
				}
			}
		}
		return null;
	}

	private org.json.JSONObject getMediaElement(org.json.JSONObject characteristicRecord) {
		org.json.JSONObject mediaElement = null;
		org.json.JSONObject child = null;
		String childCharId = null;
		if(characteristicRecord.has("_children")) {
			mediaElement = new org.json.JSONObject();
			org.json.JSONArray children = characteristicRecord.getJSONArray("_children");
			for(int k=0; k<children.length(); k++) {
				child = children.getJSONObject(k);
				childCharId = child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
				if(childCharId.endsWith("_Name")) {
					mediaElement.put("MediaAssetName", child.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
				}else if(childCharId.endsWith("_URL")) {
					mediaElement.put("MediaAssetURL", child.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
				}else if(childCharId.endsWith("_Status")) {
					mediaElement.put("MediaAssetType", child.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
				}
			}
		}
		return mediaElement;
	}

	private static final Logger LOGGER = Logger.getLogger(RealExportProducts.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/getProposalsForo-%g.log", 5 * 1024 * 1024, 5, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    java.time.LocalDateTime dateTime =
                        java.time.Instant.ofEpochMilli(record.getMillis())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();

                    String timestamp = dateTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
        
		LOGGER.info("Collecting atributos SAP y de Internet");
		java.util.Map<String, String> attr = null;
		attr = seleccionaLasDesas("CategorySpecificAttributesLVP", rw.getRw().getBaseUrl(), rw.getRw().getRc().getHeader().get("Authorization"));
		attr.keySet().forEach(k->atributosInternet.add(k));
		attr = seleccionaLasDesas("CategorySpecificAttributesS4H", rw.getRw().getBaseUrl(), rw.getRw().getRc().getHeader().get("Authorization"));
		attr.keySet().forEach(k->atributosInternet.add(k));
		attr = seleccionaLasDesas("CategorySpecificAttributesSAP", rw.getRw().getBaseUrl(), rw.getRw().getRc().getHeader().get("Authorization"));
		attr.keySet().forEach(k->atributosSAP.add(k));
	}

	
	private void log(Object message){
		LOGGER.info(String.valueOf(message));
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/getProposalsForo.err", true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}
}
