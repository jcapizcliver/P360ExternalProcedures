package mx.com.liverpool.p360.services.core;

import org.json.JSONObject;

public class CreateProposalFrozenMediaURLs {

	private String input = null;
	private String response = null;

	private boolean deleteInputFile = true;
	
	private final java.util.Map<String, String> charCategories = new java.util.TreeMap<>();


	private final RESTWrapper rw = new RESTWrapper();
	private final RESTWorkshop workshop; // = new RESTWorkshop(true, baseUrl, "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded, "Accept-Language: es");

	private final String objectAPIProduct2GURL; // = baseUrl + "/object/Product2G";
	private final String objectAPIArticleURL; // = baseUrl + "/object/Article";

	private org.json.JSONArray responses = new org.json.JSONArray();
	private org.json.JSONArray notFound = new org.json.JSONArray();

	private org.json.JSONObject genericResponse = null;
	private org.json.JSONArray variantResponsesArray = new org.json.JSONArray();

	private org.json.JSONArray genericFieldErrors = new org.json.JSONArray();


	private final RestClient rc; // = workshop.getRc();

	private java.util.Map<String, String> nextStatusMap = new java.util.TreeMap<>();
	private java.util.Map<String, String> externalStatusMap = new java.util.TreeMap<>();

	private final long myId;
	
	public CreateProposalFrozenMediaURLs(String baseUrl, String encoded, long myId) {
		this.myId = myId;
		this.workshop = rw.getRw();
		this.rc = workshop.getRc();
		this.objectAPIProduct2GURL = baseUrl + "/object/Product2G";
		this.objectAPIArticleURL = baseUrl + "/object/Article";
	}

	public String doIt(String[] args) {
		return doIt(args, false);
	}

	public String doIt(String[] args, boolean ex) {
		if (!ImageTrafficLimiter.tryAcquire()) {
			log("Rejected final-media request: inFlight=" + ImageTrafficLimiter.getInFlight());
			return ImageTrafficLimiter.busyResponse();
		}
		try {
			try {
			input = args[0];
			log("Input received, chars=" + (input == null ? 0 : input.length()));
			if (args.length > 2) {
				deleteInputFile = Boolean.parseBoolean( args[2] );
				if (args.length > 3) {
					if (args.length > 4) {
					}
				}
			}
			return run();
		} catch (ArrayIndexOutOfBoundsException e) {
			logE(e);
			System.err.print(
					"Invalid number of arguments for java routine. Mandatories are: <inputFile> <baseCacheDir>, optionals are: <dictionary> <nextStatusDictionaryId> <externalStatusDictionaryId>, in that order.");
		}
		return null;
		} finally {
			ImageTrafficLimiter.release();
		}
	}


	public String getCodigoSBB(String cammelCase) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(workshop.getBaseUrl());
		rw.addHeader("Authorization", rw.getRc().getHeader().get("Authorization"));
		rw.putParameter("lookup", "SB_COLORESLOV");
		rw.putParameter("fields", "LookupValue.Code");
		rw.putParameter("query", "LookupValue.IsActive = true and LookupValueLang.Name(es) equals \"" + cammelCase + "\"");

		org.json.JSONObject response = null;

		response = rw.makeRequest("GET", "/list/LookupValue/bySearch");

		return response == null ? null : response.has("rows") && response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : null;

	}

	public String toCammelCase(String value) {
		StringBuilder sb = new StringBuilder();
		String piece = null;
		java.util.LinkedList<String> tokens = new java.util.LinkedList<>();
		for(int i=0; i<value.length(); i++) {
			piece = value.substring(i, i+1);
			if(tokens.isEmpty()) {
				tokens.addLast(piece.toUpperCase());
			}else {
				if(" ".equals(tokens.getLast())) {
					tokens.addLast(piece.toUpperCase());
				}else {
					tokens.addLast(piece.toLowerCase());
				}
			}
		}
		for(String t : tokens) {
			sb.append(t);
		}
		return sb.toString();
	}

	private String run() {
		long init = System.currentTimeMillis();
		JSONObject request = null;
		String rawResp = null;
		String externalProductId = null;
		org.json.JSONArray variantes = null;
		JSONObject variante = null;
		org.json.JSONArray multimediaArray = null;
		JSONObject multimedia = null;
		try {
			request = new JSONObject(input);
			org.json.JSONArray products = (org.json.JSONArray) request.remove("products");
			products = products == null ? new org.json.JSONArray() : products;
			JSONObject product = null;
			for (int i = 0; i < products.length(); i++) {
				request = null;
				rawResp = null;
				externalProductId = null;
				variantes = null;
				variante = null;
				multimediaArray = null;
				multimedia = null;
				
				product = products.getJSONObject(i);
				org.json.JSONArray children = null;
				String recordKey = null;
				multimediaArray = (org.json.JSONArray) product.remove("multiMedia");
				variantes = (org.json.JSONArray) product.remove("variants");
				variantes = variantes == null ? new org.json.JSONArray() : variantes;
				org.json.JSONArray characteristicArray = new org.json.JSONArray();
				children = null;
				externalProductId = product.has("proposalId") ? product.getString("proposalId") : null;
				int timesOwnersManual = 0;
				int timesLiverpoolManual = 0;
				int timesProductVideo = 0;
				int timesNOM = 0;
				multimediaArray = multimediaArray == null ? new org.json.JSONArray() : multimediaArray;
				recordKey = null;
				for (int j = 0; j < multimediaArray.length(); j++) {
					try {
						multimedia = multimediaArray.getJSONObject(j);
						if (multimedia.getString("MultimediaAssetType").startsWith("LiverpoolManual")) {
							recordKey = timesLiverpoolManual == 0 ? "0000.0000.RK" : "0000." + ( timesLiverpoolManual < 10 ? "000" + timesLiverpoolManual : timesLiverpoolManual < 100 ? "00" + timesLiverpoolManual : timesLiverpoolManual < 1000 ? "0" + timesLiverpoolManual : timesLiverpoolManual ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"LiverpoolManual_Name2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"LiverpoolManual_URL2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
											.put("characteristic",
													new JSONObject().put("_code", "LiverpoolManual2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray())))
									.put("_children", children));
							timesLiverpoolManual++;
						} else if (multimedia.getString("MultimediaAssetType").startsWith("ProductVideo")) {
							recordKey = timesProductVideo == 0 ? "0000.0000.RK" : "0000." + ( timesProductVideo < 10 ? "000" + timesProductVideo : timesProductVideo < 100 ? "00" + timesProductVideo : timesProductVideo < 1000 ? "0" + timesProductVideo : timesProductVideo ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"ProductVideo_Name2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code", "ProductVideo_URL2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
									.put(new org.json.JSONObject()
											.put("_qualification",
													new JSONObject()
													.put("recordKey", recordKey)
															.put("characteristic",
																	new JSONObject().put("_code", "ProductVideo2")))
											.put("_recordLang",
													new org.json.JSONArray().put(
															new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray())))
											.put("_children", children));
							timesProductVideo++;
						} else if (multimedia.getString("MultimediaAssetType").startsWith("OwnersManual")) {
							recordKey = timesOwnersManual == 0 ? "0000.0000.RK" : "0000." + ( timesOwnersManual < 10 ? "000" + timesOwnersManual : timesOwnersManual < 100 ? "00" + timesOwnersManual : timesOwnersManual < 1000 ? "0" + timesOwnersManual : timesOwnersManual ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"OwnersManual_Name2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code", "OwnersManual_URL2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
									.put(new org.json.JSONObject()
											.put("_qualification",
													new JSONObject()
													.put("recordKey", recordKey)
															.put("characteristic",
																	new JSONObject().put("_code", "OwnersManual2")))
											.put("_recordLang",
													new org.json.JSONArray().put(
															new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray())))
											.put("_children", children));
							timesOwnersManual++;
						} else if (multimedia.getString("MultimediaAssetType").startsWith("NOM")) {
							recordKey = timesNOM == 0 ? "0000.0000.RK" : "0000." + ( timesNOM < 10 ? "000" + timesNOM : timesNOM < 100 ? "00" + timesNOM : timesNOM < 1000 ? "0" + timesNOM : timesNOM ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"NOM_Name2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code", "NOM_URL2")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
									.put(new org.json.JSONObject()
											.put("_qualification",
													new JSONObject()
													.put("recordKey", recordKey)
															.put("characteristic",
																	new JSONObject().put("_code", "NOM2")))
											.put("_recordLang",
													new org.json.JSONArray().put(
															new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray())))
											.put("_children", children));
							timesNOM ++;
						}else {
							log("Multimedia element not found: " + multimedia);
						}
					} catch (org.json.JSONException | NullPointerException e) {
					}
				}

				try {

						log("UwU :>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
						JSONObject reqObj = new org.json.JSONObject();
						if(characteristicArray != null && characteristicArray.length() > 0) {
							reqObj.put("_characteristicRecords", characteristicArray);
						}

						log("External Product Id 2: " + (externalProductId));
						if(externalProductId != null && !"".equals(externalProductId)) {
							log("Product2G PUT " + externalProductId + ", characteristicRecords=" + characteristicArray.length());
							rawResp = this.rc.getRequest("PUT",
									this.objectAPIProduct2GURL + "/'" + externalProductId + "'@'MASTER'?includeLabels=true", reqObj.toString());
						}else {
							log("Product2G POST, characteristicRecords=" + characteristicArray.length());
							rawResp = this.rc.getRequest("POST",
									this.objectAPIProduct2GURL + "?includeLabels=true", reqObj.toString());
						}
						org.json.JSONObject jo = new org.json.JSONObject(rawResp);
						if(jo.has("_protocol") && jo.getJSONObject("_protocol").getInt("errorCounter") > 0) {
							log("Problem: " + jo + ", given req: " + reqObj.toString());
							throw new org.json.JSONException("Problema persistiendo datos. Solicitar ayuda y presentar el siguiente código: " + myId + ", junto con la siguiente estampa temporal: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
						}
						log("Product2G response chars=" + (rawResp == null ? 0 : rawResp.length()));
						/** If there are any errors, should report them back **/
						try {
							JSONObject rsp = new JSONObject(rawResp);
							genericResponse = new JSONObject();
							genericResponse
									.put("proposalId", externalProductId =
											rsp.getJSONObject("_entityItem").getString("_externalId").split("@")[0]
													.replaceAll("^'|'$", ""));
							variantResponsesArray = new org.json.JSONArray();
							EliminaImagenesDeVariantes eliminator = new EliminaImagenesDeVariantes();
							int deleteBatchSize = ImageTrafficLimiter.getDeleteBatchSize();
							for (int start = 0; start < variantes.length(); start += deleteBatchSize) {
								int end = Math.min(variantes.length(), start + deleteBatchSize);
								java.util.LinkedHashSet<String> idsToDelete = new java.util.LinkedHashSet<>();
								for (int j = start; j < end; j++) {
									String variantId = variantes.getJSONObject(j).optString("variantId", null);
									if (variantId != null && !variantId.isEmpty()) {
										idsToDelete.add(variantId);
									}
								}
								if (!idsToDelete.isEmpty()) {
									log("Deleting final media in one batch: variants=" + idsToDelete.size());
									eliminator.deleteAssets2(EliminaImagenesDeVariantes.toItems(idsToDelete));
								}
								for (int j = start; j < end; j++) {
									variante = variantes.getJSONObject(j);
									processVariant(externalProductId, variante, this.rc);
								}
							}
							org.json.JSONArray tru = new org.json.JSONArray();
							for(int m=0; m<variantResponsesArray.length(); m++) {
								if(variantResponsesArray.getJSONObject(m).length() == 0) {
								}else {
									variantResponsesArray.getJSONObject(m).put("variantPosition", m);
									tru.put(variantResponsesArray.getJSONObject(m));
								}
							}
							genericResponse.put("variants", tru);
							genericResponse.put("fieldProblems", genericFieldErrors);
							responses.put(genericResponse);
						} catch (org.json.JSONException e) {
							logE(e);
							log("There was an exception: " + e.getMessage() + ", received: " + rawResp);
							try{
								genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
								genericResponse.put("Error", new JSONObject(rawResp));
							}catch(org.json.JSONException ex) {
								genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
								genericResponse.put("Error", rawResp);
							}
							responses.put(genericResponse);
						}
				} catch (Exception e) {
					logE(e);
					log("There was an exception: " + e.getMessage() + ", received: " + rawResp);
					try{
						genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
						genericResponse.put("Error", rawResp == null ? "Not known error" : "Error al procesar petición. " + e.getMessage());
					}catch(org.json.JSONException ex) {
						genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
						genericResponse.put("Error", "Error al procesar petición: " + ex.getMessage());
					}
					responses.put(genericResponse);
				}
				genericFieldErrors = new org.json.JSONArray();
				genericResponse = new JSONObject();
				variantResponsesArray = new org.json.JSONArray();
			}
		} catch (Exception e) {
			try {
				response = new org.json.JSONObject().put("Error", "Petición mal formada.").toString();
			} catch (org.json.JSONException ignore) {
			}
			logE(e);
		}

		response = response != null ? response
				: new JSONObject().put("responses", responses).put("notFoundOrInactiveCharacteristics", notFound)
						.toString();
		responses = new org.json.JSONArray();
		notFound = new org.json.JSONArray();
		charCategories.clear();
		genericResponse = null;
		genericFieldErrors = new org.json.JSONArray();
		nextStatusMap.clear();
		externalStatusMap.clear();
		if(deleteInputFile) {
		}
		log("Elapsed time response: " + workshop.formatTime(System.currentTimeMillis() - init));
		return response;
	}
	

	private void processVariant(String objectId, JSONObject variant,
			RestClient rc)
			throws Exception {
		String rawResp = null;
		String internalItemId = null;
		JSONObject resp = null;
		org.json.JSONArray photosArray = null;
		JSONObject photo = null;
		String externalItemId = null;
		org.json.JSONArray structureProblems = new org.json.JSONArray();
		try {
			log("Came to variant");
			if (!variant.has("variantId")) {
				/** Make request using object API to generate an ID **/
				return;
			} else {
				externalItemId = variant.getString("variantId");
				rawResp = rc.getRequest("GET", objectAPIArticleURL + "/'" + externalItemId
						+ "'@'MASTER'?includeLabels=false&includeIds=true&entityFilter=Article", null);
				resp = new JSONObject(rawResp);
				internalItemId = !resp.has("_entityItem") ? null
						: resp.getJSONObject("_entityItem").optString("_internalId", null);
			}
			if (internalItemId == null) {
				/** There was an error **/
				log("MeSi (no Article.SupplierAID found)");
				return;
			}
			variant.remove("variantId");
			photosArray = (org.json.JSONArray) variant.remove("photos");
			if(photosArray == null) {
				photosArray = new org.json.JSONArray();
			}
			org.json.JSONArray characteristicArray = new org.json.JSONArray();
			int timesDetailImage = 0;
			int timesIllustration = 0;
			int timesSmosh = 0;
			org.json.JSONArray children = null;
			String recordKey = null;
			for (int j = 0; j < photosArray.length(); j++) {
				photo = photosArray.getJSONObject(j);
				try {
					if (photo.getString("PhotoAssetType").startsWith("ProductImageDetail")) {
						recordKey = timesDetailImage == 0 ? "0000.0000.RK" : "0000." + ( timesDetailImage < 10 ? "000" + timesDetailImage : timesDetailImage < 100 ? "00" + timesDetailImage : timesDetailImage < 1000 ? "0" + timesDetailImage : timesDetailImage ) + ".RK";
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageDetail_Name2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageDetail_URL2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						characteristicArray
								.put(new org.json.JSONObject()
										.put("_qualification",
												new JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new JSONObject().put("_code", "ProductImageDetail2")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new JSONObject().put("values", new org.json.JSONArray())))
										.put("_children", children));
						timesDetailImage++;
					} else if (photo.getString("PhotoAssetType").equals("ProductImage")) {
						log("Adding a productImage ");
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", "0000.0000.RK")
												.put("characteristic",
														new org.json.JSONObject().put("_code", "ProductImage_Name2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", "0000.0000.RK")
												.put("characteristic",
														new org.json.JSONObject().put("_code", "ProductImage_URL2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						characteristicArray.put(new org.json.JSONObject().put("_qualification",
								new JSONObject()
									.put("recordKey", "0000.0000.RK")
										.put("characteristic", new JSONObject().put("_code", "ProductImage2")))
								.put("_recordLang",
										new org.json.JSONArray()
												.put(new JSONObject().put("values", new org.json.JSONArray())))
								.put("_children", children));
					} else if (photo.getString("PhotoAssetType").startsWith("Illustration")) {
						recordKey = timesIllustration == 0 ? "0000.0000.RK" : "0000." + ( timesIllustration < 10 ? "000" + timesIllustration : timesIllustration < 100 ? "00" + timesIllustration : timesIllustration < 1000 ? "0" + timesIllustration : timesIllustration ) + ".RK";
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey",recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code", "Illustration_Name2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code", "Illustration_URL2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						characteristicArray.put(new org.json.JSONObject().put("_qualification",
								new JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic", new JSONObject().put("_code", "Illustration2")))
								.put("_recordLang",
										new org.json.JSONArray()
												.put(new JSONObject().put("values", new org.json.JSONArray())))
								.put("_children", children));
						timesIllustration++;
					} else if (photo.getString("PhotoAssetType").startsWith("ProductImageSmosh")) {
						recordKey = timesSmosh == 0 ? "0000.0000.RK" : "0000." + ( timesSmosh < 10 ? "000" + timesSmosh : timesSmosh < 100 ? "00" + timesSmosh : timesSmosh < 1000 ? "0" + timesSmosh : timesSmosh ) + ".RK";
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageSmosh_Name2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageSmosh_URL2")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						characteristicArray
								.put(new org.json.JSONObject()
										.put("_qualification",
												new JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new JSONObject().put("_code", "ProductImageSmosh2")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new JSONObject().put("values", new org.json.JSONArray())))
										.put("_children", children));
						timesSmosh++;
					}
				} catch (org.json.JSONException | NullPointerException e) {
					structureProblems.put(new org.json.JSONObject()
							.put("Message", "Error in structure, failed when processing photo object structure. Mandatory attributes are: PhotoAssetURL and PhotoAssetName")
							.put("Object", photo));
				}
			}
			JSONObject reqObj = new org.json.JSONObject().put("_characteristicRecords", characteristicArray);
			log("Prepared variant " + externalItemId + ", characteristicRecords=" + characteristicArray.length());
			try {
				rc.getHeader().put("Accept-Language", "es");
				rawResp = rc.getRequest("PUT", this.objectAPIArticleURL + "/" + internalItemId + "?includeLabels=true",
						reqObj.toString());
				log("Variant PUT " + externalItemId + ", responseChars=" + (rawResp == null ? 0 : rawResp.length()));
				/** If there are any errors, should report them back **/
				try {
					resp = new JSONObject(rawResp);
					variantResponsesArray.put(new JSONObject()
							.put("structureProblems", structureProblems.length() > 0 ? structureProblems : null)
							.put("variantId", resp.getJSONObject("_entityItem").getString("_externalId").split("@")[0]
									.replaceAll("^'|'$", "")));
				} catch (org.json.JSONException e) {
					logE(e);
					log("Caught an exception: " + rawResp + "___<_>___" + externalItemId);
					variantResponsesArray.put(new JSONObject().put("variantId", externalItemId).put("error", rawResp));
				}
			} catch (org.json.JSONException e) {
				logE(e);
			}
		} catch (Exception e) {
			log("RawResp: " + rawResp);
			logE(e);
		}
	}


	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CreateProposalFrozenMediaURLs.class.getName());

	static {
		try {
			LOGGER.setUseParentHandlers(false);
			java.util.logging.FileHandler fh = new java.util.logging.FileHandler("../logs/imagenes_dos-%g.log", 25 * 1024 * 1024, 10, true);
			fh.setEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
			fh.setFormatter(new java.util.logging.Formatter() {
				@Override
				public String format(java.util.logging.LogRecord record) {
					String ts = java.time.Instant.ofEpochMilli(record.getMillis())
							.atZone(java.time.ZoneId.systemDefault())
							.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
					return "[" + ts + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
				}
			});
			LOGGER.addHandler(fh);
			LOGGER.setLevel(java.util.logging.Level.ALL);
		} catch (java.io.IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private void log(String message) {
		LOGGER.info("(" + myId + ") " + message);
	}

	private void logE(Exception ex) {
		LOGGER.log(java.util.logging.Level.SEVERE, "(" + myId + ") " + ex.getMessage(), ex);
	}
}