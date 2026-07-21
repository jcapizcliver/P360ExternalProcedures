package mx.com.liverpool.p360.services.core;

import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class EchamelosCompa {

//	private final String baseUrl; // = "http://172.18.237.162:1512/rest/V2.0";
//	private final RestClient rc; // = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	public EchamelosCompa(String baseUrl, String encoded) {
//		this.baseUrl = baseUrl;
//		rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);
	}
	
	public org.json.JSONObject processRequest(String[] args) throws ServiceUnavailableException {
		org.json.JSONObject globalResponse = null;
		String rawRequest = args[0];
		org.json.JSONObject r = null;
		org.json.JSONArray rws = null;
		org.json.JSONArray responses = new org.json.JSONArray();
		java.util.Set<String> productId = new java.util.TreeSet<>();
//		StringBuilder productItems = new StringBuilder();
//		int timesProduct = 0;
		org.json.JSONArray matrix = null;
		DataRequestor dr = new DataRequestor();
		try {
			org.json.JSONObject request = new org.json.JSONObject(rawRequest);
			if(request.has("products")) {
				rws = request.getJSONArray("products");
				String sku = null;
				String proposalId = null;
//				String response = null;
				for(int i=0; i<rws.length(); i++) {
					r = rws.getJSONObject(i);
					log("Going for: " + r);
					if(r.has("sku")) {
						String resp = dr.productBySKU(new org.json.JSONArray().put(sku));
						if(resp != null) {
							try {
								org.json.JSONObject jr = new org.json.JSONObject(resp);
								org.json.JSONArray items = jr.getJSONArray("items");
								proposalId = String.valueOf( items.get(0) );
								if("".equals(proposalId)) {
									proposalId = null;
								}
							}catch(org.json.JSONException e) {
								logE(e);
							}
						}
						if(proposalId == null) {
							resp = dr.articleBySKU(new org.json.JSONArray().put(sku));
							if(resp != null) {
								try {
									org.json.JSONObject jr = new org.json.JSONObject(resp);
									org.json.JSONArray items = jr.getJSONArray("items");
									proposalId = String.valueOf( items.getJSONObject(0).get("product") );
									if("".equals(proposalId)) {
										proposalId = null;
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
						}
						if(proposalId == null) {
							if(r.has("proposalId")) {
								resp = dr.getProductByVariant(new org.json.JSONArray().put(r.get("proposalId")));
								if(resp != null) {
									try{
										org.json.JSONObject jr = new org.json.JSONObject(resp);
										org.json.JSONArray items = jr.getJSONArray("items");
										if(!"".equals(items.get(0))) {
											proposalId = String.valueOf( items.get(0) );
										}
									}catch(org.json.JSONException e) {
										logE(e);
									}
								}
							}
						}
						if(proposalId == null) {
							resp = dr.getProductData(new org.json.JSONArray().put(r.get("proposalId")));
							if(resp != null) {
								try {
									org.json.JSONObject jr = new org.json.JSONObject(resp);
									org.json.JSONArray items = jr.getJSONArray("items");
									org.json.JSONObject item = items.getJSONObject(0);
									if(!"".equals(item.get("CurrentStatus"))) {
										proposalId = String.valueOf( r.get("proposalId") );
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
						}
					}else if(r.has("proposalId")) {
						String resp = null;
						if(proposalId == null) {
							if(r.has("proposalId")) {
								resp = dr.getProductByVariant(new org.json.JSONArray().put(r.get("proposalId")));
								if(resp != null) {
									try{
										org.json.JSONObject jr = new org.json.JSONObject(resp);
										org.json.JSONArray items = jr.getJSONArray("items");
										if(!"".equals(items.get(0))) {
											proposalId = String.valueOf( items.get(0) );
										}
									}catch(org.json.JSONException e) {
										logE(e);
									}
								}
							}
						}
						if(proposalId == null) {
							resp = dr.getProductData(new org.json.JSONArray().put(r.get("proposalId")));
							if(resp != null) {
								try {
									org.json.JSONObject jr = new org.json.JSONObject(resp);
									org.json.JSONArray items = jr.getJSONArray("items");
									org.json.JSONObject item = items.getJSONObject(0);
									log("Eléjele: " + item + " (" + r.get("proposalId") + ")");
									if(!"".equals(item.get("CurrentStatus"))) {
										proposalId = String.valueOf( r.get("proposalId") );
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
						}
					}
					if(proposalId != null) {
						productId.add(proposalId);
						proposalId = null;
					}else {
						log("Not found anything for: " + r);
						responses.put(new org.json.JSONObject().put("message", "Not found.").put("originalObject", r));
					}
					/*
					if(r.has("sku")){
						sku = r.get("sku");
						response = queryExistance(true, true, sku);
						if(response == null) {
							response = queryExistance(false, true, sku);
							if(response == null) {
								if(r.has("proposalId")) {
									proposalId = r.get("proposalId");
									response = queryExistance(true, false, proposalId);
									if(response == null) {
										response = queryExistance(false, false, proposalId);
										if(response == null) {
											responses.put(new org.json.JSONObject().put("message", "Not found.").put("originalObject", r));
										}else {
											// FOUND IT
										}
									}else {
										// FOUND IT
									}
								}else {
									responses.put(new org.json.JSONObject().put("message", "Not found.").put("originalObject", r));
								}
							}else {
								// FOUND IT
							}
						}else {
							// FOUND IT
						}
					}else if(r.has("proposalId")) {
						proposalId = r.get("proposalId");
						response = queryExistance(true, false, proposalId);
						if(response == null) {
							response = queryExistance(false, false, proposalId);
							if(response == null) {
								if(r.has("sku")) {
									sku = r.get("sku");
									response = queryExistance(true, true, sku);
									if(response == null) {
										response = queryExistance(false, true, sku);
										if(response == null) {
											responses.put(new org.json.JSONObject().put("message", "Not found.").put("originalObject", r));
										}else {
											// FOUND IT
										}
									}else {
										// FOUND IT
									}
								}else {
									responses.put(new org.json.JSONObject().put("message", "Not found.").put("originalObject", r));
								}
							}else {
								// FOUND IT
							}
						}else {
							// FOUND IT
						}
					}else {
						responses.put(new org.json.JSONObject().put("message", "No valid keys supplied, \"sku\" or \"proposalId\" are expected.").put("originalObject", r));
					}
					if(response != null) {
						productId.add(response);
					}
					 */
				}
				log("Now collecting data for: " + productId);
				matrix = collectData(productId);
//				for(String pid : productId) {
//					log("Querying for pid: " + pid);
//					productItems.append(timesProduct == 0 ? "" : ",").append("'").append(pid).append("'@'MASTER'");
//					timesProduct++;
//				}
//				matrix = collectProductLevelInformation(productItems.toString());
			}else {
//				matrix = collectProductLevelInformationForSample();
//				log("Epale YOU: " + matrix);
			}
			globalResponse = new org.json.JSONObject().put("columns", new org.json.JSONArray()
					.put("ID")
					.put("VID")
					.put("ParentSKU")
					.put("SupplierID")
					.put("supplierShopId")
					.put("Business")
					.put("SAPObjectType")
					.put("SKU")
					.put("ProductName")
					.put("MainBarCode")
					.put("MainBarCodeS4H")
					.put("BuyerRejectionMessage")
					.put("SupplierRejectionMessage")
					.put("ItemGroup")
					.put("Section")
					.put("Status")
					.put("SkuType")
					.put("BWSCL")
					.put("TImportacion")
					.put("Negocio")
					.put("EXTWG_S4H")
					.put("FotoTomadaLiverpool")
					.put("MesdeEntregadeMercancIa")
					.put("Temporada")
					.put("BWVOR")
					.put("AnoEstacion")
					.put("SupplierPartNumber")
					.put("TextoAdicional")
					.put("Evento")
					.put("CostobrutoSinIVA")
					.put("PrecioSugeridocIVA")
					.put("Descuento1")
					.put("Descuento2")
					.put("NORMT")
					.put("LABOR")
					.put("BrandName")
					.put("BRAND_ID_S4H")
					.put("DescriptionLong")
					.put("DescriptionLong2")
					.put("ColoursLiverpoolAtt")
					.put("TamanoUnico")
					.put("TypeMainBarCode")
					.put("Currency")
					).put("values", matrix);
//			System.out.println();
		}catch(org.json.JSONException e) {
			logE(e);
		}
		return globalResponse;
	}

//	private String queryExistance(boolean isArticle, boolean isSKU, String value) throws ServiceUnavailableException {
//		if(value == null || "".equals(value)) {
//			return null;
//		}
//		String r = null;
//		String rawResponse = null;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		try {
//			String url = this.baseUrl + "/list/" + (isArticle ? "Article" : "Product2G") + "/byItems?" + (isSKU ? "query=" + java.net.URLEncoder.encode(isArticle ? "characteristic('SKU', -1) equals \"" + value + "\"" : "characteristic('SKU',-1) equals \"" + value + "\"", "UTF-8") : "query=" + java.net.URLEncoder.encode(isArticle ? "Article.SupplierAID equals \"" + value + "\"" : "Product2G.ProductNo equals \"" + value + "\"", "UTF-8")) + "&metaData=true" + "&fields=" + java.net.URLEncoder.encode(isArticle ? "Article.SupplierAID" : "Product2G.ProductNo", "UTF-8");
//			try {
//				rawResponse = rc.getRequest("GET", url, null);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				if(rows != null && !org.json.JSONObject.NULL.equals(rows) && rows.length() > 0) {
//					r = String.valueOf( rows.getJSONObject(0).getJSONArray("values").get(0) );
//					if(isArticle) {
//						rawResponse = rc.getRequest("GET", baseUrl + "/object/Article/'" + r + "'@'MASTER'?entityFilter=ProductReference", null);
//						response = new org.json.JSONObject(rawResponse);
//						r = String.valueOf( response.getJSONObject("_data").getJSONArray("higherLevelProduct").getJSONObject(0).getJSONObject("_qualification").get("referencedIdentifier") );
//					}
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (org.json.JSONException e) {
//				e.printStackTrace();
//			}
//		} catch (UnsupportedEncodingException e) {
//			logE(e);
//		}
//		return r;
//	}

//	private org.json.JSONArray collectProductLevelInformationForSample() throws ServiceUnavailableException{
//		org.json.JSONArray r = new org.json.JSONArray();
//		String rawResponse = null;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		org.json.JSONArray newValues = null;
//		org.json.JSONArray variantValues = null;
//		int epa = 0;
//		int currentIndex = 0;
//		int totalSize = 0;
//		int cnt = 0;
//		int rowComplete = 0;
//		try {
//			do {
//				String url = this.baseUrl + "/list/Product2G/bySearch?query=" 
//						+ java.net.URLEncoder.encode("not Product2G.ProductNo is empty", "UTF-8") 
//						+ "&fields=" 
//						+ java.net.URLEncoder.encode(
//								   "Product2G.ProductNo"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierID,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(supplierShopId,-1)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(Business,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(SAPObjectType,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(SKU,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(ProductName,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(MainBarCode,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(MainBarCodeS4H,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(BuyerRejectionMessage,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierRejectionMessage,-1)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(ItemGroup,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(Section,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(Status,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(SkuType,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(BWSCL,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(TImportacion,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(Negocio,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(EXTWG_S4H,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(FotoTomadaLiverpool,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(MesdeEntregadeMercancIa,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(Temporada,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(BWVOR,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(AnoEstacion,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierPartNumber,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(TextoAdicional,-1)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(Evento,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(CostobrutoSinIVA,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(PrecioSugeridocIVA,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(Descuento1,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(Descuento2,-1)"
//								+ ",SimpleProduct2GCharacteristicValueLang.Value(NORMT,-1)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(LABOR,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(BrandName,-1)->LookupValueLang.Name(es)"
//								+ ",SimpleProduct2GCharacteristicValue.LookupValue(BRAND_ID_S4H,-1)->LookupValueLang.Name(es)"
//								+ ",Product2GLang.DescriptionLong(es)"
//								+ ",Product2GLang.DescriptionLong2(es)"
//							, "UTF-8") 
//						+ "&pageSize=256&startIndex=" + currentIndex;
//				try {
//					rawResponse = rc.getRequest("GET", url, null);
//					response = new org.json.JSONObject(rawResponse);
//					totalSize = response.getInt("totalSize");
//					rows = response.getJSONArray("rows");
//					Object o = null;
//					if(rows != null && !org.json.JSONObject.NULL.equals(rows) && rows.length() > 0) {
//						for(int i=0; i<rows.length(); i++) {
//							values = rows.getJSONObject(i).getJSONArray("values");
//							newValues = new org.json.JSONArray();
//							newValues.put(values.get(0));
//							newValues.put("");
//							newValues.put("");
//							for(int j=1; j<values.length(); j++) {
//								if(!"".equals(values.get(j))) {
//									rowComplete++;
//								}
//								epa = j;
//								o = values.get(j);
//								newValues.put( o instanceof org.json.JSONArray ? values.getJSONArray(j).get(0) : String.valueOf(o));
//							}
//							if( (rowComplete/(values.length() - 2)) >= 0.7 ) {
//								r.put(newValues);
//								variantValues = collectArticleLevelInformation(String.valueOf( newValues.get(0) ), String.valueOf( newValues.get(6) ), values);
//								for(int k=0; k<variantValues.length(); k++) {
//									r.put(variantValues.getJSONArray(k));
//								}
//								cnt++;
//								if(cnt == 50) {
//									currentIndex = totalSize;
//									break;
//								}
//							}
//							rowComplete = 0;
//							currentIndex++;
//						}
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				} catch (org.json.JSONException e) {
//					log("Epa " + epa + ". " + rawResponse);
//				}
//			}while(currentIndex < totalSize);
//		} catch (UnsupportedEncodingException e) {
//			logE(e);
//		}
//		return r;
//	}
	
	private org.json.JSONArray collectData(java.util.Set<String> externalIds) {
		String resp = null;
		String resp2 = null;
		org.json.JSONArray r = new org.json.JSONArray();
		org.json.JSONArray newValues = null;
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items0 = new org.json.JSONArray();
		org.json.JSONArray items = new org.json.JSONArray();
		org.json.JSONArray items2 = new org.json.JSONArray();
		java.util.List<String> asList = new java.util.ArrayList<>(externalIds);
		asList.forEach(items::put);
		asList.forEach(items0::put);
		log("Los pedidos: " + externalIds);
		resp = dr.getProductData(items);
		resp2 = dr.getProductExtraData(items);
		log("ProductExtraDataResponse to see: " + resp2);
		if(resp != null && resp2 != null) {
			try{
				org.json.JSONObject jr = new org.json.JSONObject(resp);
				org.json.JSONObject jr2 = new org.json.JSONObject(resp2);
				items = jr.getJSONArray("items");
				items2 = jr2.getJSONArray("items");
				org.json.JSONObject item = null;
				org.json.JSONObject item2 = null;
				for(int i=0; i<items.length(); i++) {
					newValues = new org.json.JSONArray();
					log("Over: " + items0.get(i));
					item = items.getJSONObject(i);
					item2 = items2.getJSONObject(i);
					newValues.put(item.get("product"));
					newValues.put("");
					newValues.put("");
//					.put("Section", values[0])
//					.put("ItemGroup", values[1])
//					.put("ItemGroupS4H", values[2])
//					.put("BrandName", values[3])
//					.put("BRAND_ID_S4H", values[4])
//					.put("Business", values[5])
//					.put("SKU", values[6])
//					.put("SupplierID", values[7])
//					.put("Template", values[8])
//					.put("CurrentStatus", values[9])
//					.put("AssignTakeNoTake", values[10])
//					.put("SAPObjectType", values[11])
//					.put("FotoTomadaLiverpool", values.length > 12 ? values[12] : "")
//					.put("MainBarCode", values.length > 13 ? values[13] : "")
//					.put("MainBarCodeS4H", values.length > 14 ? values[14] : "")
//					.put("SupplierPartNumber", values.length > 15 ? values[15] : "")
					newValues.put(item.get("SupplierID"));
					newValues.put(item2.get("supplierShopId"));
					newValues.put(item.get("Business"));
					newValues.put(item.get("SAPObjectType"));
					newValues.put(item.get("SKU"));
					newValues.put(item2.get("ProductName"));
					newValues.put(item.get("MainBarCode"));
					newValues.put(item.get("MainBarCodeS4H"));
					newValues.put(item2.get("BuyerRejectionMessage"));
					newValues.put(item2.get("SupplierRejectionMessage"));
					newValues.put(item.get("ItemGroup"));
					newValues.put(item.get("Section"));
					newValues.put(item.get("CurrentStatus"));
					newValues.put(item2.get("SkuType"));
					newValues.put(item2.get("BWSCL"));
					newValues.put(item2.get("TImportacion"));
					newValues.put(item2.get("Negocio"));
					newValues.put(item2.get("EXTWG_S4H"));
					newValues.put(item.get("FotoTomadaLiverpool"));
					newValues.put(item2.get("MesdeEntregadeMercancIa"));
					newValues.put(item2.get("Temporada"));
					newValues.put(item2.get("BWVOR"));
					newValues.put(item2.get("AnoEstacion"));
					newValues.put(item.get("SupplierPartNumber"));
					newValues.put(item2.get("TextoAdicional"));
					newValues.put(item2.get("Evento"));
					newValues.put(item2.get("CostobrutoSinIVA"));
					newValues.put(item2.get("PrecioSugeridocIVA"));
					newValues.put(item2.get("Descuento1"));
					newValues.put(item2.get("Descuento2"));
					newValues.put(item2.get("NORMT"));
					newValues.put(item2.get("LABOR"));
					newValues.put(item.get("BrandName"));
					newValues.put(item.get("BRAND_ID_S4H"));
					newValues.put(item2.get("DescriptionLong"));
					newValues.put(item2.get("DescriptionLong2"));
					newValues.put("");
					newValues.put("");
					newValues.put(item2.get("TypeMainBarCode"));
					newValues.put(item2.get("Currency"));
					log("Los VALIUS (" + asList.get(i) + "): " + newValues);
					r.put(newValues);
					java.util.Set<String> varIds = dr.getVariants( String.valueOf( item.get("product") ));
					org.json.JSONArray varIdsArray = new org.json.JSONArray();
					varIds.forEach(varIdsArray::put);
					String r0 = dr.getArticleData(varIdsArray);
					String r1 = dr.getArticleExtraData(varIdsArray);
					log("\t\tArticleExtraData: " + r1);
					if(r0 != null && r1 != null) {
						try {
							org.json.JSONObject jr0 = new org.json.JSONObject(r0);
							org.json.JSONObject jr1 = new org.json.JSONObject(r1);
							org.json.JSONArray a0 = jr0.getJSONArray("items");
							org.json.JSONArray a1 = jr1.getJSONArray("items");
							org.json.JSONObject i0 = null;
							org.json.JSONObject i1 = null;
							org.json.JSONArray newValuesVariant = null;
							for(int j=0; j<a0.length(); j++) {
								i0 = a0.getJSONObject(j);
								i1 = a1.getJSONObject(j);
								newValuesVariant = new org.json.JSONArray();
								newValuesVariant.put("");
//								 item.get("supplierShopId")
//									,item.get("ProductName")
//									,item.get("BuyerRejectionMessage")
//									,item.get("SupplierRejectionMessage")
//									,item.get("SkuType")
//									,item.get("BWSCL")
//									,item.get("TImportacion")
//									,item.get("Negocio")
//									,item.get("EXTWG_S4H")
//									,item.get("MesdeEntregadeMercancIa")
//									,item.get("Temporada")
//									,item.get("BWVOR")
//									,item.get("AnoEstacion")
//									,item.get("TextoAdicional")
//									,item.get("Evento")
//									,item.get("CostobrutoSinIVA")
//									,item.get("PrecioSugeridocIVA")
//									,item.get("Descuento1")
//									,item.get("Descuento2")
//									,item.get("NORMT")
//									,item.get("LABOR")
//									,item.get("DescriptionLong")
//									,item.get("DescriptionLong2")
								newValuesVariant.put(i0.get("variant"));
								newValuesVariant.put(item.get("SKU"));
								newValuesVariant.put(item.get("SupplierID"));
								newValuesVariant.put(item2.get("supplierShopId"));
								newValuesVariant.put(item.get("Business"));
								newValuesVariant.put(item.get("SAPObjectType"));
								newValuesVariant.put(i0.get("SKU"));
								newValuesVariant.put( deriveName( String.valueOf( item2.get("ProductName") ), String.valueOf( i0.get("TamanoUnico") ), String.valueOf( i0.get("ColoursLiverpoolAtt") ) ));
								newValuesVariant.put(i0.get("MainBarCode"));
								newValuesVariant.put(i0.get("MainBarCodeS4H"));
								newValuesVariant.put(item2.get("BuyerRejectionMessage"));
								newValuesVariant.put(item2.get("SupplierRejectionMessage"));
								newValuesVariant.put(item.get("ItemGroup"));
								newValuesVariant.put(item.get("Section"));
								newValuesVariant.put(item.get("CurrentStatus"));
								newValuesVariant.put(item2.get("SkuType"));
								newValuesVariant.put(item2.get("BWSCL"));
								newValuesVariant.put(item2.get("TImportacion"));
								newValuesVariant.put(item2.get("Negocio"));
								newValuesVariant.put(item2.get("EXTWG_S4H"));
								newValuesVariant.put(item.get("FotoTomadaLiverpool"));
								newValuesVariant.put(item2.get("MesdeEntregadeMercancIa"));
								newValuesVariant.put(item2.get("Temporada"));
								newValuesVariant.put(item2.get("BWVOR"));
								newValuesVariant.put(item2.get("AnoEstacion"));
								newValuesVariant.put(i0.get("SupplierPartNumber"));
								newValuesVariant.put(item2.get("TextoAdicional"));
								newValuesVariant.put(item2.get("Evento"));
								newValuesVariant.put(i1.get("CostobrutoSinIVA") == null || "".equals(i1.get("CostobrutoSinIVA")) ? item2.has("CostobrutoSinIVA") && item2.get("CostobrutoSinIVA") != null && !"".equals(item2.get("CostobrutoSinIVA")) ? item2.get("CostobrutoSinIVA") : "" : i1.get("CostobrutoSinIVA"));
								newValuesVariant.put(i1.get("PrecioSugeridocIVA") == null || "".equals(i1.get("PrecioSugeridocIVA")) ? item2.has("PrecioSugeridocIVA") && item2.get("PrecioSugeridocIVA") != null && !"".equals(item2.get("PrecioSugeridocIVA")) ? item2.get("PrecioSugeridocIVA") : "" : i1.get("PrecioSugeridocIVA"));
								newValuesVariant.put(i1.get("Descuento1"));
								newValuesVariant.put(i1.get("Descuento2"));
								newValuesVariant.put(item2.get("NORMT"));
								newValuesVariant.put(item2.get("LABOR"));
								newValuesVariant.put(item.get("BrandName"));
								newValuesVariant.put(item.get("BRAND_ID_S4H"));
								newValuesVariant.put(item2.get("DescriptionLong"));
								newValuesVariant.put(item2.get("DescriptionLong2"));
								newValuesVariant.put(i0.get("ColoursLiverpoolAtt"));
								newValuesVariant.put(i0.get("TamanoUnico"));
								newValuesVariant.put(item2.get("TypeMainBarCode") == null || "".equals(item2.get("TypeMainBarCode")) ? i1.has("TypeMainBarCode") && i1.get("TypeMainBarCode") != null && !"".equals(i1.get("TypeMainBarCode")) ? i1.get("TypeMainBarCode") : "" : item2.get("TypeMainBarCode"));
								newValuesVariant.put(item2.get("Currency"));
								r.put(newValuesVariant);
							}
						}catch(org.json.JSONException e) {
							logE(e);
						}
					}
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
//			String url = this.baseUrl + "/list/Product2G/byItems?items=" + java.net.URLEncoder.encode(items, "UTF-8") + "&fields=" + java.net.URLEncoder.encode(
//					  "Product2G.ProductNo"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierID,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(supplierShopId,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Business,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(SAPObjectType,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SKU,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(ProductName,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(MainBarCode,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(MainBarCodeS4H,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(BuyerRejectionMessage,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierRejectionMessage,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(ItemGroup,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Section,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Status,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(SkuType,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BWSCL,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(TImportacion,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Negocio,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(EXTWG_S4H,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(FotoTomadaLiverpool,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(MesdeEntregadeMercancIa,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Temporada,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BWVOR,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(AnoEstacion,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierPartNumber,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(TextoAdicional,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Evento,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(CostobrutoSinIVA,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(PrecioSugeridocIVA,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(Descuento1,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(Descuento2,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(NORMT,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(LABOR,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BrandName,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BRAND_ID_S4H,-1)->LookupValueLang.Name(es)"
//					+ ",Product2GLang.DescriptionLong(es)"
//					+ ",Product2GLang.DescriptionLong2(es)"
//				, "UTF-8");
//			try {
//				rawResponse = rc.getRequest("GET", url, null);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				Object o = null;
//				if(rows != null && !org.json.JSONObject.NULL.equals(rows) && rows.length() > 0) {
//					for(int i=0; i<rows.length(); i++) {
//						values = rows.getJSONObject(i).getJSONArray("values");
//						newValues = new org.json.JSONArray();
//						newValues.put(values.get(0));
//						newValues.put("");
//						newValues.put("");
//						for(int j=1; j<values.length(); j++) {
//							epa = j;
//							o = values.get(j);
//							newValues.put(String.valueOf( o instanceof org.json.JSONArray ? values.getJSONArray(j).get(0) : o));
//						}
//						newValues.put("");
//						newValues.put("");
//						r.put(newValues);
//						variantValues = collectArticleLevelInformation(newValues.get(0), newValues.get(6), values);
//						for(int k=0; k<variantValues.length(); k++) {
//							r.put(variantValues.getJSONArray(k));
//						}
//					}
//				}else {
//					log("Problem processing request: " + rawResponse);
//				}
//			} catch (KeyManagementException e) {
//				logE(e);
//			} catch (NoSuchAlgorithmException e) {
//				logE(e);
//			} catch (URISyntaxException e) {
//				logE(e);
//			} catch (IOException e) {
//				logE(e);
//			} catch (org.json.JSONException e) {
//				log("Epa " + epa + ". " + rawResponse);
//			}
		return r;
	}
	
	private String deriveName(String name, String tamanoUnico, String color) {
		if(name == null)
			return null;
		String nn = name + ", " + tamanoUnico + ", " + color ;
		nn = nn.replaceAll(",{2,}", ",");
		return nn;
	}
	
//	private String nonEmpty(String v1, String v2) {
//		return !"".equals(v1) ? v1 : v2;
//	}

//	private org.json.JSONArray collectProductLevelInformation(String items) throws ServiceUnavailableException{
//		org.json.JSONArray r = new org.json.JSONArray();
//		String rawResponse = null;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		org.json.JSONArray newValues = null;
//		org.json.JSONArray variantValues = null;
//		int epa = 0;
//		try {
//			String url = this.baseUrl + "/list/Product2G/byItems?items=" + java.net.URLEncoder.encode(items, "UTF-8") + "&fields=" + java.net.URLEncoder.encode(
//					  "Product2G.ProductNo"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierID,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(supplierShopId,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Business,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(SAPObjectType,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SKU,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(ProductName,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(MainBarCode,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(MainBarCodeS4H,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(BuyerRejectionMessage,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierRejectionMessage,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(ItemGroup,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Section,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Status,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(SkuType,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BWSCL,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(TImportacion,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Negocio,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(EXTWG_S4H,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(FotoTomadaLiverpool,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(MesdeEntregadeMercancIa,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Temporada,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BWVOR,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(AnoEstacion,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(SupplierPartNumber,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(TextoAdicional,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(Evento,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(CostobrutoSinIVA,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(PrecioSugeridocIVA,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(Descuento1,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(Descuento2,-1)"
//					+ ",SimpleProduct2GCharacteristicValueLang.Value(NORMT,-1)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(LABOR,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BrandName,-1)->LookupValueLang.Name(es)"
//					+ ",SimpleProduct2GCharacteristicValue.LookupValue(BRAND_ID_S4H,-1)->LookupValueLang.Name(es)"
//					+ ",Product2GLang.DescriptionLong(es)"
//					+ ",Product2GLang.DescriptionLong2(es)"
//				, "UTF-8");
//			try {
//				rawResponse = rc.getRequest("GET", url, null);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				Object o = null;
//				if(rows != null && !org.json.JSONObject.NULL.equals(rows) && rows.length() > 0) {
//					for(int i=0; i<rows.length(); i++) {
//						values = rows.getJSONObject(i).getJSONArray("values");
//						newValues = new org.json.JSONArray();
//						newValues.put(values.get(0));
//						newValues.put("");
//						newValues.put("");
//						for(int j=1; j<values.length(); j++) {
//							epa = j;
//							o = values.get(j);
//							newValues.put(String.valueOf( o instanceof org.json.JSONArray ? values.getJSONArray(j).get(0) : o));
//						}
//						newValues.put("");
//						newValues.put("");
//						r.put(newValues);
//						variantValues = collectArticleLevelInformation( String.valueOf( newValues.get(0) ), String.valueOf( newValues.get(6) ), values);
//						for(int k=0; k<variantValues.length(); k++) {
//							r.put(variantValues.getJSONArray(k));
//						}
//					}
//				}else {
//					log("Problem processing request: " + rawResponse);
//				}
//			} catch (IOException e) {
//				logE(e);
//			} catch (org.json.JSONException e) {
//				log("Epa " + epa + ". " + rawResponse);
//			}
//		} catch (UnsupportedEncodingException e) {
//			logE(e);
//		}
//		return r;
//	}

//	private org.json.JSONArray collectArticleLevelInformation(String product, String sku, org.json.JSONArray productValues) throws ServiceUnavailableException{
//		org.json.JSONArray r = new org.json.JSONArray();
//		String rawResponse = null;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		org.json.JSONArray newValues = null;
//		int epa = 0;
//		try {
//			String url = this.baseUrl + "/list/Article/bySearch"
//					+ "?query=" + java.net.URLEncoder.encode("ProductReference.ReferencedSupplierAid(\"" + product + "\") equals \"" + product + "\"", "UTF-8") + "&fields="
//		+ java.net.URLEncoder.encode(
//				"Article.SupplierAID"
//				+ ",SimpleArticleCharacteristicValue.LookupValue(SAPObjectType,-1)->LookupValueLang.Name(es)"
//				+ ",SimpleArticleCharacteristicValueLang.Value(SKU,-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value(MainBarCode,-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value(MainBarCodeS4H,-1)"
//				+ ",SimpleArticleCharacteristicValue.LookupValue(Status,-1)->LookupValueLang.Name(es)"
//				+ ",SimpleArticleCharacteristicValue.LookupValue(SkuType,-1)->LookupValueLang.Name(es)"
//				+ ",SimpleArticleCharacteristicValueLang.Value('SupplierPartNumber',-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value('CostobrutoSinIVA',-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value('PrecioSugeridocIVA',-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value('Descuento1',-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value('Descuento2',-1)"
//				+ ",SimpleArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',-1)->LookupValueLang.Name(es)"
//				+ ",SimpleArticleCharacteristicValue.LookupValue('TamanoUnico',-1)->LookupValueLang.Name(es)"
//			, "UTF-8");
//			try {
//				Object o = null;
//				rawResponse = rc.getRequest("GET", url, null);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				if(rows != null && !org.json.JSONObject.NULL.equals(rows) && rows.length() > 0) {
//					for(int i=0; i<rows.length(); i++) {
//						values = rows.getJSONObject(i).getJSONArray("values");
//						newValues = new org.json.JSONArray();
//						newValues.put("");
//						newValues.put(values.get(0));
//						newValues.put(sku);
//						log("<::>" + productValues.length() + "\n\t<::>" + productValues + "<::>");
//						for(int j=1; j<productValues.length(); j++) {
//							newValues.put(returnValue(productValues, values, j));
//						}
//						o = values.get(ARTICLE_HEADER.get("ColoursLiverpoolAtt"));
//						newValues.put( String.valueOf( o instanceof org.json.JSONArray ? ((org.json.JSONArray)o).get(0) : o ) );
//						o = values.get(ARTICLE_HEADER.get("TamanoUnico"));
//						newValues.put( String.valueOf( o instanceof org.json.JSONArray ? ((org.json.JSONArray)o).get(0) : o ) );
//					}
//					r.put(newValues);
//				}else {
//					log("Problem processing request: " + rawResponse);
//				}
//			} catch (IOException e) {
//				logE(e);
//			} catch (org.json.JSONException e) {
//				log("Epa " + epa + ". " + rawResponse);
//				logE(e);
//			}
//		} catch (UnsupportedEncodingException e) {
//			logE(e);
//		}
//		return r;
//	}

//	private String returnValue(org.json.JSONArray productValues, org.json.JSONArray variantValues, int i) {
//		if(ARTICLE_HEADER.isEmpty()) {
//			for(int a = 0; a<A_HEADER.length; a++) {
//				ARTICLE_HEADER.put(A_HEADER[a], a);
//			}
//		}
//		String value = null;
//		Integer index = null;
//		log("--->" + (i-1));
//		index = ARTICLE_HEADER.get(HEADER[i-1]);
//		if(index != null) {
//			Object o = null;
//			value = String.valueOf( variantValues.get(index) instanceof org.json.JSONArray ? variantValues.getJSONArray(index).get(0) : variantValues.get(index) );
//			if("".equals(value) || value == null) {
//				o = productValues.get(i);
//				return String.valueOf( o instanceof org.json.JSONArray ? productValues.getJSONArray(i).get(0) : o );
//			}else {
//				return value;
//			}
//		}
//		return String.valueOf( productValues.get(i) instanceof org.json.JSONArray ? productValues.getJSONArray(i).get(0) : productValues.get(i) );
//	}

	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/ec.log", true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/ec.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
//	private static final String[] A_HEADER = new String[] {
//			"SAPObjectType",
//			"SKU",
//			"MainBarCode",
//			"MainBarCodeS4H",
//			"Status",
//			"SkuType",
//			"SupplierPartNumber",
//			"CostobrutoSinIVA",
//			"PrecioSugeridocIVA",
//			"Descuento1",
//			"Descuento2",
//			"ColoursLiverpoolAtt",
//			"TamanoUnico"
//	};
	
//	private static final java.util.Map<String, Integer> ARTICLE_HEADER = new java.util.TreeMap<>();
	
//	private static final String[] HEADER = new String[] {
//													"SupplierID",
//													"supplierShopId",
//													"Business",
//													"SAPObjectType",
//													"SKU",
//													"ProductName",
//													"MainBarCode",
//													"MainBarCodeS4H",
//													"BuyerRejectionMessage",
//													"SupplierRejectionMessage",
//													"ItemGroup",
//													"Section",
//													"Status",
//													"SkuType",
//													"BWSCL",
//													"TImportacion",
//													"Negocio",
//													"EXTWG_S4H",
//													"FotoTomadaLiverpool",
//													"MesdeEntregadeMercancIa",
//													"Temporada",
//													"BWVOR",
//													"AnoEstacion",
//													"SupplierPartNumber",
//													"TextoAdicional",
//													"Evento",
//													"CostobrutoSinIVA",
//													"PrecioSugeridocIVA",
//													"Descuento1",
//													"Descuento2",
//													"NORMT",
//													"LABOR",
//													"BrandName",
//													"BRAND_ID_S4H",
//													"DescriptionLong",
//													"DescriptionLong2"
//												};
}
