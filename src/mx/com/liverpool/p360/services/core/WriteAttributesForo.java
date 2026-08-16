package mx.com.liverpool.p360.services.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class WriteAttributesForo implements Closeable {

	private DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
		
		@Override
		public void logE(Exception e) {
			WriteAttributesForo.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			WriteAttributesForo.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);
	
	public Object processRequest(String[] args) throws ServiceUnavailableException {
		log("BRAND NEW GLORY. " + args[1]);
		org.json.JSONObject generalResponse = null;
		String sourceFile = null;
		String lookupCharacteristicsFile = null;
		String baseUrl = null;
		String encoded = null;
		RestClient rc = null;
		String rawMessage = null;
		org.json.JSONObject request = null;
		java.nio.file.Path lookupFilePath = null;
		org.json.JSONArray responses = new org.json.JSONArray();
		try{
			sourceFile= args[0];
			lookupCharacteristicsFile = args[1];
			baseUrl = args[2];
			encoded = args[3];
			rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded, "Accept-Language: es");
			lookupFilePath = java.nio.file.Paths.get(lookupCharacteristicsFile);
			if(java.nio.file.Files.exists(lookupFilePath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
				java.nio.file.attribute.FileTime ft = java.nio.file.Files.getLastModifiedTime(lookupFilePath, java.nio.file.LinkOption.NOFOLLOW_LINKS);
				long difference = System.currentTimeMillis() - ft.toMillis();
				boolean needsRefresh = difference >= (1000*60*60*24);
				if(needsRefresh) {
					log("Refreshing...");
					refreshLookupList(lookupFilePath);
//					refreshLookupList(lookupFilePath, rc, baseUrl);
				}
				log("Releasing fis...");
				log("Releasing fos...");
			}else {
				log("Refreshing from fresh...");
				refreshLookupList(lookupFilePath);
//				refreshLookupList(lookupFilePath, rc, baseUrl);
			}
		}catch(java.io.IOException | ArrayIndexOutOfBoundsException e) {
			System.out.println(generalResponse = new org.json.JSONObject().put("Error", "A file that contains the json message to be processed needs to be specified as the first argument, as the second argument, a file that contains a list of characteristic identifiers that are lookup type needs to be specified as well."));
			log(new org.json.JSONObject().put("Error", "A file that contains the json message to be processed needs to be specified as the first argument, as the second argument, a file that contains a list of characteristic identifiers that are lookup type needs to be specified as well.").toString());
			logE(e);
			return generalResponse;
		}
		log("Reading request...");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(sourceFile), java.nio.charset.Charset.forName("UTF-8")))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			while((line = br.readLine()) != null) {
				sb.append(line);
			}
			rawMessage = sb.toString();
		}catch(java.io.IOException e) {
			System.out.println(generalResponse = new org.json.JSONObject().put("Error", "Not a valid json message"));
			log(new org.json.JSONObject().put("Error", "Not a valid json message").toString());
			logE(e);
			return generalResponse;
		}
		org.json.JSONObject rr = new org.json.JSONObject(rawMessage);
		request = rr.has("root") ? rr.getJSONObject("root") : rr;
		log("Request read");
		org.json.JSONArray products = request.getJSONArray("products");
		org.json.JSONObject product = null;
		String proposalId = null;
		String userAction = null;
		String targetRole = null;

		org.json.JSONArray variants = null;
		org.json.JSONObject variant = null;
		org.json.JSONArray characteristicRecords = new org.json.JSONArray();
		org.json.JSONObject characteristicRecord = null;
		String rawResponse = null;
		String statusKey = null;
		org.json.JSONObject writeRequest = null;
		java.util.Map<String, String> nextStatusMap = new java.util.TreeMap<>();
		java.util.Map<String, String> externalStatusMap = new java.util.TreeMap<>();
		loadNextStatusDictionary(nextStatusMap);
		loadExternalStatusDictionary(externalStatusMap);
//		loadNextStatusDictionary(nextStatusMap, rc, baseUrl);
//		loadExternalStatusDictionary(externalStatusMap, rc, baseUrl);
		String nextStatus = null;
		org.json.JSONObject objectAPIResponse = null;
		String currentStatusKey = null;
		String previousStatusKey = null;
		String currentStatusLabel = null;
		String previousStatusLabel = null;
		String descriptionLong = null;
		String productName = null;
		String productImageURL = null;
		org.json.JSONArray multimedia = null;
		org.json.JSONArray photos = null;
		String variantId = null;
		org.json.JSONArray variantResponses = null;
		org.json.JSONArray variantCharacteristicRecords = null;
		org.json.JSONArray structureProblems = new org.json.JSONArray();
		org.json.JSONArray variantStructureProblems = new org.json.JSONArray();
		org.json.JSONObject productRequest = null;
		org.json.JSONObject response = null;
		org.json.JSONArray lang = null;
		org.json.JSONObject langEs = null;
		for(int i=0; i<products.length(); i++) {
			product = products.getJSONObject(i);
			response = new org.json.JSONObject();
			proposalId = (String) product.remove("proposalId");
			userAction = (String) product.remove("userAction");
			targetRole = (String) product.remove("targetRole");
			descriptionLong = (String) product.remove("DescriptionLong");
			log("ProductName: " + (product.has("ProductName") ? product.getString("ProductName") : ""));
			variants = (org.json.JSONArray) product.remove("variants");
			lang = new org.json.JSONArray();
			langEs = new org.json.JSONObject();
			lang.put(langEs);
			langEs.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "esl")));
			if(targetRole == null || userAction == null) {
				responses.put(new org.json.JSONObject().put("Error", "targetRole and userAction keys are mandatory for each product specification."));
				continue;
			}
			if(proposalId == null) {
				responses.put(new org.json.JSONObject().put("Error", "No proposalId specified"));
				continue;
			}
			objectAPIResponse = getEntity("Product2G", proposalId, rc, baseUrl);
			if(objectAPIResponse == null) {
				responses.put(new org.json.JSONObject().put("Message", "Problem found: couldn't find a proposal with id: " + proposalId));
				continue;
			}
			currentStatusKey =  objectAPIResponse.getJSONObject("_data").has("currentStatus") ? String.valueOf( objectAPIResponse.getJSONObject("_data").getJSONObject("currentStatus").getInt("_key") ) : "";
			previousStatusKey = objectAPIResponse.getJSONObject("_data").has("previousStatus") ? String.valueOf( objectAPIResponse.getJSONObject("_data").getJSONObject("previousStatus").getInt("_key") ) : "";
			currentStatusLabel =  objectAPIResponse.getJSONObject("_data").has("currentStatus") ? objectAPIResponse.getJSONObject("_data").getJSONObject("currentStatus").getString("_label") : "";
			previousStatusLabel = objectAPIResponse.getJSONObject("_data").has("previousStatus") ? objectAPIResponse.getJSONObject("_data").getJSONObject("previousStatus").getString("_label") : "";
			statusKey = previousStatusKey + "|" + currentStatusKey + "|" + userAction.substring(0, 1) + "|" + targetRole;
			nextStatus = nextStatusMap.get(statusKey);
			log("ProposalId: " + proposalId + "\tuserAction: " + userAction + "\ttargetRole: " + targetRole + "\tcurrentStatus: " + currentStatusLabel + "\tpreviousStatus: " + previousStatusLabel + "\tnextStatus" + nextStatus);
			multimedia = (org.json.JSONArray) product.remove("multimedia");
			productRequest = new org.json.JSONObject();
			String rjr = null;
			org.json.JSONObject jr0 = null;
			org.json.JSONArray jra = null;
			for(String name : org.json.JSONObject.getNames(product)) {
				rjr = dr.getCharacteristicData(new org.json.JSONArray().put(name));
				log("Un dos trés: " + rjr);
				jr0 = new org.json.JSONObject(rjr);
				jra = jr0.getJSONArray("items");
				log("--->" + jra.getJSONObject(0));
				if(!"".equals(product.get(name))) {
					if("refundPolicy".equals(name)) {
						productRequest.put("refundPolicy", product.get(name));
					}else if("EmbedCodeWAP".equals(name)) {
						productRequest.put("embedCodeWAP", product.get(name));
					}else if("EmbedCodeWEB".equals(name)) {
						productRequest.put("embedCodeWEB", product.get(name));
					}else if("DescriptionLong".equals(name)) {
						langEs.put("descriptionLong", product.getString(name));
					}else if("DescriptionLong2".equals(name)) {
						langEs.put("descriptionLong2", product.getString(name));
					} else if("ProductName".equals(name)){
						productName = product.getString(name);
						log("Came to ProductName: " + productName);
						langEs.put("productName", productName);
						characteristicRecord = new org.json.JSONObject();
						characteristicRecord.put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", name)));
						characteristicRecord.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject()
								.put("values",
										 product.get(name) //.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
								)));
						characteristicRecords.put(characteristicRecord);
					} else {
						characteristicRecord = new org.json.JSONObject();
						characteristicRecord.put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", name)));
						characteristicRecord.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject()
								.put("values",
										!"LOOKUP".equals( jra.getJSONObject(0).getString("dataType") ) ? product.get(name) : "".equals(product.get(name)) ? "" : new org.json.JSONObject().put("_code", product.get(name)) //.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
								)));
						characteristicRecords.put(characteristicRecord);
					}
				}
			}
			if(descriptionLong != null) {
				log("Found description long!");
				langEs.put("descriptionLong", descriptionLong);
			}
			if(langEs.length() > 1) {
				log("Placed langEs");
				productRequest.put("lang", lang);
			}
			if(multimedia != null) {
				appendMultimediaInformation(multimedia, characteristicRecords, structureProblems);
			}
			productRequest.put("_characteristicRecords", characteristicRecords);
			log("Finished product data");
			if(variants != null) {
				log("Going for variants");
				variantResponses = new org.json.JSONArray();
				for(int j=0; j<variants.length(); j++) {
					variant = variants.getJSONObject(j);
					photos = (org.json.JSONArray) variant.remove("photos");
					variantId = (String) variant.remove("variantId");
					variantCharacteristicRecords = new org.json.JSONArray();
					org.json.JSONObject variantResponse = new org.json.JSONObject();
					variantResponse.put("variant", variantId);
					variantResponses.put(variantResponse);
					log("Going for variantId " + variantId);
					if(variantId == null) {
						log("No variantId found" );
						variantResponse.put("message", "Missing variantId").put("type", "problem");
					}else {
						objectAPIResponse = getEntity("Article", variantId, rc, baseUrl);
						if(objectAPIResponse == null) {
							log("No info found for variant.");
							variantResponse.put("message", "Problem found: couldn't find a variant with id: " + variantId).put("type", "problem");
							continue;
						}
						log("Gathering photos...");
						productImageURL = appendPhotos(photos, variantCharacteristicRecords, variantStructureProblems);
						log(productImageURL + "\n\tVariant structure problems: " + variantStructureProblems);
						if(variantStructureProblems.length() > 0) {
							variantResponse.put("message", "Found structure problems in image data.").put("structureProblems", variantStructureProblems).put("type", "problem");
						}log( "Variant: " + variant );
						if(variant.length() == 0) {
							log("Empty variant.");
						}else {
							for(String name : org.json.JSONObject.getNames(variant)) {
								rjr = dr.getCharacteristicData(new org.json.JSONArray().put(name));
								log("Un dos trés (Article): " + rjr);
								jr0 = new org.json.JSONObject(rjr);
								jra = jr0.getJSONArray("items");
								log("Article --->" + jra.getJSONObject(0));
								if(!"".equals(variant.get(name))) {
									characteristicRecord = new org.json.JSONObject();
									characteristicRecord.put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", name)));
									characteristicRecord.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject()
											.put("values", new org.json.JSONArray().put(
													!"LOOKUP".equals( jra.getJSONObject(0).getString("dataType") ) ? variant.get(name) : !"".equals(variant.get(name)) ? new org.json.JSONObject().put("_code", variant.get(name)) : "" // .put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
											))));
									variantCharacteristicRecords.put(characteristicRecord);
								}
							}
						}
						try {
							writeRequest = new org.json.JSONObject();
							if(variantCharacteristicRecords.length() > 0)
								writeRequest.put("_characteristicRecords", variantCharacteristicRecords);
							if(productImageURL != null)
								writeRequest.put("productImageURL", productImageURL);
							if(!"".equals(nextStatus) && nextStatus != null) {
								writeRequest.put("currentStatus", new org.json.JSONObject().put("_key", nextStatus));
							}
							if(!"".equals(currentStatusKey) && currentStatusKey != null) {
								writeRequest.put("previousStatus", new org.json.JSONObject().put("_key", currentStatusKey));
							}
							if(nextStatus != null) {
								writeRequest.put("externalStatus", new org.json.JSONObject().put("_code", externalStatusMap.get(nextStatus)));
							}
							rawResponse = rc.getRequest("PUT", baseUrl + "/object/Article/'" + variantId + "'@'MASTER'", writeRequest.toString());
							log("Sending variant: ----->" + writeRequest + "<-----");
							log(" rawResponse (variant): " + rawResponse);
							try {
								org.json.JSONObject jr = new org.json.JSONObject(rawResponse);
								if(jr.has("_protocol")) {
									jr = jr.getJSONObject("_protocol");
								}
								if(jr.has("errorCounter") && jr.getInt("errorCounter") > 0) {
									variantResponse.put("message", "Problem writing to P360, see entries array.").put("type", "problem").put("entries", jr.getJSONArray("entries"));
								}else if(jr.has("errorCounter") && jr.getInt("errorCounter") == 0){
									variantResponse.put("message", "Write processed.").put("type", "success");
								} else {
									if(jr.has("counters")) {
										jr.put("counters", jr.getJSONObject("counters"));
									}
									if( jr.has("counters") && jr.has("entries") && jr.getJSONArray("entries").length() > 0) {
										variantResponse.put("message", "Problem writing to P360, see entries array.").put("type", "problem").put("entries", jr.getJSONArray("entries"));
									}else if(jr.has("counters")) {
										variantResponse.put("message", "Write processed.").put("type", "success");
									}
								}
							}catch(org.json.JSONException e) { logE(e); }
						} catch (IOException e) {
							logE(e);
							variantResponse.put("message", "problem processing request.").put("type", "problem").put("detail", e.getMessage());
						}
					}
				}
				response.put("variantResponses", variantResponses);
			}
			responses.put(response);
			try {
				if(!"".equals(nextStatus) && nextStatus != null) {
					productRequest.put("currentStatus", new org.json.JSONObject().put("_key", nextStatus));
				}
				if(!"".equals(currentStatusKey) && currentStatusKey != null) {
					productRequest.put("previousStatus", new org.json.JSONObject().put("_key", currentStatusKey));
				}
				if(nextStatus != null) {
					productRequest.put("externalStatus", new org.json.JSONObject().put("_code", externalStatusMap.get(nextStatus)));
				}
				rawResponse = rc.getRequest("PUT", baseUrl + "/object/Product2G/'" + proposalId + "'@'MASTER'", productRequest.toString());
				log("sent: " + productRequest);
				log("From update proposal: " + rawResponse);
				org.json.JSONObject laResp = new org.json.JSONObject(rawResponse);
				org.json.JSONObject protocol = laResp.getJSONObject("_protocol");
				if(protocol.getInt("errorCounter") > 0) {
					generalResponse.put("_protocol", protocol);
				}
			} catch (IOException e) {
				logE(e);
				log("Petición: " + productRequest);
				generalResponse = new org.json.JSONObject().put("Error", "Problema general.");
				response.put("message", "problem processing request.").put("type", "problem").put("detail", e.getMessage());
			} catch (org.json.JSONException e) {
				generalResponse = new org.json.JSONObject().put("Error", "Problema procesando respuesta desde P360.");
				log("### " + rawResponse);
				logE(e);
			}
		}
		System.out.println(generalResponse = new org.json.JSONObject().put("Responses", responses));
		return generalResponse;
	}

	private org.json.JSONObject getEntity(String entity, String externalItemId, RestClient rc, String baseUrl, String entityFilter) throws ServiceUnavailableException{
		org.json.JSONObject response = null;
		String rawResponse = null;
		String url = null;
		try {
			rawResponse = rc.getRequest("GET", url = baseUrl + "/object/" + entity + "/'" + externalItemId + "'@'MASTER'?includeLabels=true&includeIds=true&entityFilter=" + entityFilter, null);
			response = new org.json.JSONObject(rawResponse);
		} catch (org.json.JSONException | IOException e) {
			log(url + "<::> LEAP: " + rawResponse);
			logE(e);
		}
		return response;
	}

	private org.json.JSONObject getEntity(String entity, String externalItemId, RestClient rc, String baseUrl) throws ServiceUnavailableException{
		return getEntity(entity, externalItemId, rc, baseUrl, entity);
	}

	private void refreshLookupList(java.nio.file.Path lookupFilePath) {
		log("Start writing participantes file caché.");

		try (java.io.PrintWriter pw =
				new java.io.PrintWriter(
					new java.io.OutputStreamWriter(
						new java.io.FileOutputStream(lookupFilePath.toFile()),
						java.nio.charset.StandardCharsets.UTF_8))) {

			for (String identifier : dastub.getActiveLookupCharacteristicIdentifiers()) {

				pw.println(identifier);
				log("Just wrote to file: " + identifier);
			}

		} catch (java.io.IOException e) {
			logE(e);
		}
	}

	private void loadExternalStatusDictionary(
			java.util.Map<String, String> externalStatusMap) {

		if (!externalStatusMap.isEmpty()) {
			return;
		}

		externalStatusMap.putAll(
				dastub.getDictionaryValueAlternativeValueMap("ExternalStatus"));
	}

	private void loadNextStatusDictionary(
			java.util.Map<String, String> nextStatusMap) {

		if (!nextStatusMap.isEmpty()) {
			return;
		}

		nextStatusMap.putAll(
				dastub.getDictionaryValueAlternativeValueMap("NextStatus"));
	}
	
//	private void refreshLookupList(java.nio.file.Path lookupFilePath, RestClient rc, String baseUrl) throws ServiceUnavailableException {
//		String rawResponse = null;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		int currentIndex = 0;
//		int totalSize = 0;
//		log("Start writing participantes file caché.");
//		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(lookupFilePath.toFile()), java.nio.charset.Charset.forName("UTF-8")))){
//			do {
//				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch"
//						+ "?query=" + java.net.URLEncoder.encode("Characteristic.DataType = LOOKUP and Characteristic.IsActive = true", "UTF-8")
//						+ "&fields=" + java.net.URLEncoder.encode("Characteristic.Identifier", "UTF-8")
//						+ "&pageSize=2000"
//						+ "&startIndex=" + currentIndex
//						, null);
//				response = new org.json.JSONObject(rawResponse);
//				log("Read response: " + rawResponse);
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					pw.println(values.getString(0));
//					log("Just wrote to file: " + values.getString(0));
//				}
//				currentIndex += response.getInt("pageSize");
//			}while(currentIndex < totalSize);
//		} catch (java.io.IOException e) {
//			logE(e);
//		}
//	}
//
//	private void loadExternalStatusDictionary(java.util.Map<String, String> externalStatusMap, RestClient rc, String baseUrl) {
//		if (!externalStatusMap.isEmpty()) {
//			return;
//		}
//		String rawResponse = null;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONObject row = null;
//		org.json.JSONArray values = null;
//		int currentIndex = 0;
//		int totalSize = 0;
//		try {
//			do {
//				rawResponse = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/byDictionary?dictionary="
//						+ java.net.URLEncoder.encode("ExternalStatus", "UTF-8")
//						+ "&fields=StandardizationValue.Value,StandardizationValue.AlternativeValue&pageSize=200&startIndex="
//						+ currentIndex, null);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				for (int i = 0; i < rows.length(); i++) {
//					row = rows.getJSONObject(i);
//					values = row.getJSONArray("values");
//					externalStatusMap.put(values.getString(0), values.getString(1));
//				}
//				currentIndex += rows.length();
//				totalSize = response.getInt("totalSize");
//			} while (currentIndex < totalSize);
//		} catch (Exception e) {
//			log(rawResponse);
//			logE(e);
//		}
//	}
//
//	private void loadNextStatusDictionary(java.util.Map<String, String> nextStatusMap, RestClient rc, String baseUrl) {
//		if (!nextStatusMap.isEmpty()) {
//			return;
//		}
//		String rawResponse = null;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONObject row = null;
//		org.json.JSONArray values = null;
//		int currentIndex = 0;
//		int totalSize = 0;
//		log(rc.getHeader().toString());
//		try {
//			do {
//				rawResponse = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/byDictionary?dictionary="
//						+ java.net.URLEncoder.encode("NextStatus", "UTF-8")
//						+ "&fields=StandardizationValue.Value,StandardizationValue.AlternativeValue&pageSize=200&startIndex="
//						+ currentIndex, null);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				for (int i = 0; i < rows.length(); i++) {
//					row = rows.getJSONObject(i);
//					values = row.getJSONArray("values");
//					nextStatusMap.put(values.getString(0), values.getString(1));
//				}
//				currentIndex += rows.length();
//				totalSize = response.getInt("totalSize");
//			} while (currentIndex < totalSize);
//		} catch (Exception e) {
//			log(rawResponse);
//			logE(e);
//		}
//	}

	private void appendMultimediaInformation(org.json.JSONArray multimediaArray, org.json.JSONArray characteristicArray, org.json.JSONArray structureProblems) {
		org.json.JSONObject multimedia = null;
		org.json.JSONArray children = null;
		int timesOwnersManual = 0;
		int timesLiverpoolManual = 0;
		int timesProductVideo = 0;
		int timesNOM = 0;
		String recordKey = null;
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
															"LiverpoolManual_Name")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code",
															"LiverpoolManual_URL")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetURL"))))));
					characteristicArray
					.put(new org.json.JSONObject()
							.put("_qualification",
									new JSONObject()
									.put("recordKey", recordKey)
									.put("characteristic",
											new JSONObject().put("_code", "LiverpoolManual")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new JSONObject().put("values", new org.json.JSONArray())))
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
															"ProductVideo_Name")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code", "ProductVideo_URL")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetURL"))))));
					characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new JSONObject().put("_code", "ProductVideo")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new JSONObject().put("values", new org.json.JSONArray())))
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
															"OwnersManual_Name")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code", "OwnersManual_URL")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetURL"))))));
					characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new JSONObject().put("_code", "OwnersManual")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new JSONObject().put("values", new org.json.JSONArray())))
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
															"NOM_Name")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code", "NOM_URL")))
							.put("_recordLang",
									new org.json.JSONArray().put(
											new org.json.JSONObject().put("values", new org.json.JSONArray()
													.put(multimedia.getString("MultimediaAssetURL"))))));
					characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new JSONObject().put("_code", "NOM")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new JSONObject().put("values", new org.json.JSONArray())))
									.put("_children", children));
					timesNOM ++;
				}else {
					log("Multimedia element not found: " + multimedia);
				}
			} catch (org.json.JSONException | NullPointerException e) {
				structureProblems.put(new org.json.JSONObject()
						.put("Message",
								"Error in structure, failed when processing multimedia object structure.")
						.put("Object", multimedia));
			}
		}
	}

	private String appendPhotos(org.json.JSONArray photosArray, org.json.JSONArray characteristicArray, org.json.JSONArray structureProblems) {
		int timesDetailImage = 0;
		int timesIllustration = 0;
		int timesSmosh = 0;
		org.json.JSONObject photo = null;
		org.json.JSONArray children = null;
		String recordKey = null;
		String productImageURL = null;
		for (int j = 0; j < photosArray.length(); j++) {
			photo = photosArray.getJSONObject(j);
			try {
				log("--" + photo.getString("PhotoAssetType"));
				if (photo.getString("PhotoAssetType").startsWith("ProductImageDetail")) {
					recordKey = timesDetailImage == 0 ? "0000.0000.RK" : "0000." + ( timesDetailImage < 10 ? "000" + timesDetailImage : timesDetailImage < 100 ? "00" + timesDetailImage : timesDetailImage < 1000 ? "0" + timesDetailImage : timesDetailImage ) + ".RK";
					children = new org.json.JSONArray();
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code",
															"ProductImageDetail_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code",
															"ProductImageDetail_URL")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					if(photo.has("PhotoAssetStatus")) {
						children.put(
							new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"ProductImageDetail_Status")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new org.json.JSONObject().put("values",
															new org.json.JSONArray()
																	.put(new JSONObject()
																			.put("_qualification",
																					new JSONObject().put(
																							"language",
																							new JSONObject().put(
																									"_code", "zxx")))
																			.put("_label", photo.optString(
																					"PhotoAssetStatus")))))));
					}
					characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new JSONObject().put("_code", "ProductImageDetail")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new JSONObject().put("values", new org.json.JSONArray())))
									.put("_children", children));
					timesDetailImage++;
				} else if (photo.getString("PhotoAssetType").equals("ProductImage")) {
					recordKey = timesDetailImage == 0 ? "0000.0000.RK" : "0000." + ( timesDetailImage < 10 ? "000" + timesDetailImage : timesDetailImage < 100 ? "00" + timesDetailImage : timesDetailImage < 1000 ? "0" + timesDetailImage : timesDetailImage ) + ".RK";
					children = new org.json.JSONArray();
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", "0000.0000.RK")
											.put("characteristic",
													new org.json.JSONObject().put("_code", "ProductImage_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", "0000.0000.RK")
											.put("characteristic",
													new org.json.JSONObject().put("_code", "ProductImage_URL")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					productImageURL = photo.getString("PhotoAssetURL");
					log("PIURL: " + productImageURL);
					if(photo.has("PhotoAssetStatus")) {
						children.put(
							new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", "0000.0000.RK")
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"ProductImage_Status")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new org.json.JSONObject().put("values",
															new org.json.JSONArray()
																	.put(new JSONObject()
																			.put("_qualification",
																					new JSONObject().put(
																							"language",
																							new JSONObject().put(
																									"_code", "zxx")))
																			.put("_label", photo.optString(
																					"PhotoAssetStatus")))))));
					}
					characteristicArray.put(new org.json.JSONObject().put("_qualification",
							new JSONObject()
								.put("recordKey", "0000.0000.RK")
									.put("characteristic", new JSONObject().put("_code", "ProductImage")))
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
													new org.json.JSONObject().put("_code", "Illustration_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code", "Illustration_URL")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					if(photo.has("PhotoAssetStatus")) {
						children.put(
							new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"Illustration_Status")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new org.json.JSONObject().put("values",
															new org.json.JSONArray()
																	.put(new JSONObject()
																			.put("_qualification",
																					new JSONObject().put(
																							"language",
																							new JSONObject().put(
																									"_code", "zxx")))
																			.put("_label", photo.optString(
																					"PhotoAssetStatus")))))));
					}
					characteristicArray.put(new org.json.JSONObject().put("_qualification",
							new JSONObject()
							.put("recordKey", recordKey)
									.put("characteristic", new JSONObject().put("_code", "Illustration")))
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
															"ProductImageSmosh_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code",
															"ProductImageSmosh_URL")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					if(photo.has("PhotoAssetStatus")) {
						children.put(
								new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey",recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImageSmosh_Status")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new org.json.JSONObject().put("values",
																new org.json.JSONArray()
																		.put(new JSONObject()
																				.put("_qualification",
																						new JSONObject().put(
																								"language",
																								new JSONObject().put(
																										"_code", "zxx")))
																				.put("_label", photo.optString(
																						"PhotoAssetStatus")))))));
					}
					characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new JSONObject().put("_code", "ProductImageSmosh")))
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
		return productImageURL;
	}
	
	private static final Logger LOGGER = Logger.getLogger(WriteAttributesForo.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false); // evita que también salga en consola con formato default

            FileHandler fileHandler = new FileHandler("../logs/data_write_from_foro.%g.log", 15 * 1024 * 1024, 10, true); // true = append
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

                    return "[" + timestamp + "]  " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }

	private void log(String message){
		LOGGER.info(message);
	}

	private void logE(Exception ex){
		LOGGER.info("Logging: " + ex.getMessage());
		LOGGER.log( Level.SEVERE, ex.getMessage(), ex);
	}

	@Override
	public void close() throws IOException {
		dastub.close();
	}
}
