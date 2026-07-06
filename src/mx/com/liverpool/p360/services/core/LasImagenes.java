package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.temp.product2g.maintenance.EliminaImagenesDeVariantes;

public class LasImagenes {
	
	private static final RESTWrapper rw = new RESTWrapper();

	public String doIt(String arg) {
		long init = System.currentTimeMillis();
		log("----<----");
		String input = arg;
		org.json.JSONObject request = new JSONObject(input);
		org.json.JSONArray products = (org.json.JSONArray) request.remove("products");
		products = products == null ? new org.json.JSONArray() : products;
		JSONObject product = null;
		Boolean replaceAssets = null;
		org.json.JSONArray variantes = null;
		org.json.JSONObject variant = null;
		org.json.JSONArray photosArray = null;
		org.json.JSONObject photo = null;
		String productId = null;
		String variantId = null;
		EliminaImagenesDeVariantes eliminator = new EliminaImagenesDeVariantes();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeLabels", "true");
		qp.put("includeIds", "true");
		org.json.JSONArray responses = new org.json.JSONArray();
		org.json.JSONObject productResponse = null;
		org.json.JSONArray variantResponses = null;
		org.json.JSONArray structureProblems = null;
		int[] iter = new int[1];
		iter[0] = 0;
		replaceAssets = request.has("replaceAssets") && request.getBoolean("replaceAssets");
		if(replaceAssets) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < products.length(); i++) {
				product = products.getJSONObject(i);
				variantes = product.getJSONArray("variants");
				for(int j=0; j<variantes.length(); j++) {
					sb.append(sb.length() == 0 ? "" : ",");
					sb.append("'");
					sb.append(variantes.getJSONObject(j).getString("variantId"));
					sb.append("'@1");
				}
			}
			eliminator.deleteAssets(sb.toString());
			sb.setLength(0);
		}
		for (int i = 0; i < products.length(); i++) {
			long pin = System.currentTimeMillis();
			iter[0] = i;
			product = products.getJSONObject(i);
			productId = product.getString("proposalId");
			variantes = (org.json.JSONArray) product.remove("variants");
			variantes = variantes == null ? new org.json.JSONArray() : variantes;
			productResponse = new org.json.JSONObject();
			variantResponses = new org.json.JSONArray();
			productResponse.put("variantResponses", variantResponses);
			responses.put(productResponse);
			structureProblems = new org.json.JSONArray();
			for(int j=0; j<variantes.length(); j++) {
				variant = variantes.getJSONObject(j);
				variantId = variant.getString("variantId");
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
				for (int k = 0; k < photosArray.length(); k++) {
					photo = photosArray.getJSONObject(k);
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
				JSONObject reqObj = new org.json.JSONObject().put("_characteristicRecords", characteristicArray);
				rw.writeData("PUT", "object", "Article", "'" + variantId + "'@1", qp, reqObj, rr ->{
					try {
						org.json.JSONObject vr = new org.json.JSONObject(rr);
						responses.getJSONObject(iter[0]).getJSONArray("variantResponses").put(vr);
					}catch(org.json.JSONException e) {
						responses.getJSONObject(iter[0]).getJSONArray("variantResponses").put(new org.json.JSONObject().put("error", rr));
					}
					log("Resp for update variant: " + rr);
				});
				responses.getJSONObject(i).getJSONArray("variantResponses").getJSONObject(j).put("structureProblems", structureProblems);
			}
			log(productId + " sent in " + rw.getRw().formatTime(System.currentTimeMillis() - pin) + " (" + variantes.length() + ")");
		}
		log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		org.json.JSONObject response = new JSONObject().put("responses", responses);
		return response.toString();
	}

	private static final Logger LOGGER = Logger.getLogger(LasImagenes.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/receive_media-%g.log", 25 * 1024 * 1024, 10, true);
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
    }

	private void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream("../logs/receive_media.log", true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
//					+ "] " + message);
//		} catch (java.io.IOException e) {
//		}
	}

//	private void logE(Exception ex) {
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream("../logs/receive_media.log", true)))) {
//			ex.printStackTrace(pw);
//		} catch (java.io.IOException e) {
//		}
//	}
	
}
