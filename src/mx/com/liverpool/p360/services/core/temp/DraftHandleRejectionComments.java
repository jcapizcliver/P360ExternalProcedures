package mx.com.liverpool.p360.services.core.temp;

import org.json.JSONObject;

import com.jcapiz.memelos.misc.RestClient;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class DraftHandleRejectionComments {

	private static XMLMisc xmm = new XMLMisc();
	private static final String baseUrlDEV = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded, "Accept-Language: es");

	private static org.json.JSONObject readSample(){
		org.json.JSONObject samp = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\modifiedFields (5).json")))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			while((line = br.readLine()) != null) {
				sb.append(line);
			}
			samp = new org.json.JSONObject(sb.toString());
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return samp;
	}

	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		try {
			processModifiedFields(readSample());
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static void processModifiedFields(org.json.JSONObject product) {
		org.json.JSONArray characteristicArray = new org.json.JSONArray();
		org.json.JSONArray userRemarks = null;
		org.json.JSONObject userRemark;
		if(product.has("userRemarks")) {
			if(product.has("userRemarks")) {
				userRemarks = product.getJSONArray("userRemarks");
				for(int i=0; i<userRemarks.length(); i++) {
					userRemark = userRemarks.getJSONObject(i);
					addComplexCharacteristicValue(userRemark, i, characteristicArray);
				}
			}
		}
		org.json.JSONObject modifiedFields = (org.json.JSONObject) product.remove("modifiedFields");
		if(product.has("proposalId")) {
			if(modifiedFields != null) {

				org.json.JSONObject basicData = (org.json.JSONObject) modifiedFields.remove("basicData");
				org.json.JSONObject attributes = (org.json.JSONObject) modifiedFields.remove("attributes");
				org.json.JSONObject logisticData = (org.json.JSONObject) modifiedFields.remove("logisticData");
				org.json.JSONObject datosVenta = (org.json.JSONObject) modifiedFields.remove("datosVenta");
				org.json.JSONObject multimedia = (org.json.JSONObject) modifiedFields.remove("multimedia");

				int max = 0;

				org.json.JSONObject p2g = getProduct2GObject(product.getString("proposalId"));
				String id = p2g.getJSONObject("_entityItem").getString("_internalId");
				org.json.JSONArray characteristicRecords = p2g.getJSONObject("_data").getJSONArray("_characteristicRecords");
				java.util.Map<String, java.util.LinkedList<org.json.JSONObject> > mep = new java.util.TreeMap<>();
				java.util.Map<String, org.json.JSONObject > mepsiman = new java.util.TreeMap<>();
				java.util.LinkedList<org.json.JSONObject> lst = null;
				org.json.JSONObject charRec = null;
				org.json.JSONArray melemes = new org.json.JSONArray();
				for(int i=0; i<characteristicRecords.length(); i++) {
					charRec = characteristicRecords.getJSONObject(i);
					trincaleLosLookups(charRec);
					lst = mep.get(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
					if(lst == null) {
						lst = new java.util.LinkedList<>();
						mep.put(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), lst);
					}
					lst.addLast(charRec);
				}
//				System.out.println(mep.get("OwnersManual"));
				java.util.LinkedList<org.json.JSONObject> multimediaElementlList = mep.get("OwnersManual");
				if(multimediaElementlList != null){
					for(org.json.JSONObject elm : multimediaElementlList) {
						chagama(elm, mep, mepsiman, "OwnersManual");
					}
				}
				multimediaElementlList = mep.get("ProductVideo");
				if(multimediaElementlList != null){
					for(org.json.JSONObject elm : multimediaElementlList) {
//						System.out.println("Chagama PV: " + elm);
						chagama(elm, mep, mepsiman, "ProductVideo");
					}
				}
				multimediaElementlList = mep.get("LiverpoolManual");
				if(multimediaElementlList != null){
					for(org.json.JSONObject elm : multimediaElementlList) {
//						System.out.println("Chagama LM: " + elm);
						chagama(elm, mep, mepsiman, "LiverpoolManual");
					}
				}
//				System.out.println("MmChagamM");
				for(String name : org.json.JSONObject.getNames(multimedia)) {
//					System.out.println("Vamos a tomar los neims de: " + name);
					for(String subNeim : org.json.JSONObject.getNames(multimedia.getJSONObject(name))) {
//						System.out.println("-->" + subNeim);
						lst = mep.get(name + "_" + subNeim);
						if(lst != null) {
							for(org.json.JSONObject rej : lst) {
								deactivateBoy(rej, name);
							}
							max = getMaxBoy(lst);
							max++;
							addComplexCharacteristicNamedValue(multimedia.getJSONObject(name).getJSONArray(subNeim).getJSONObject(0), max, mepsiman.get(name + "_" + subNeim).getJSONArray("_children"), name, true);
							melemes.put(mepsiman.get(name + "_" + subNeim));
//							System.out.println("____" + melemes);
						}else {
//							System.out.println("Nel... ");
						}
					}
				}
//				System.out.println("*****");
				org.json.JSONArray normalCharacteristics = new org.json.JSONArray();
				if(basicData != null) {
					normalCharacteristics.put(basicData);
				}
				if(attributes != null) {
					normalCharacteristics.put(attributes);
				}
				if(logisticData != null) {
					normalCharacteristics.put(logisticData);
				}
				if(datosVenta != null) {
					normalCharacteristics.put(datosVenta);
				}
				org.json.JSONObject elm = null;
				String theName = null;
				for(int j = 0; j<normalCharacteristics.length(); j++) {
					elm = normalCharacteristics.getJSONObject(j);
					for(String name : org.json.JSONObject.getNames(elm)) {
						userRemarks = elm.getJSONArray(name);
						userRemark = userRemarks.getJSONObject(0);
						theName = name + "_Rechazo";
						lst = mep.get(theName);
						for(org.json.JSONObject le : lst) {
							deactivateBoy(le, name);
						}
						max = getMaxBoy(lst);
						max++;
						lst.forEach(o -> melemes.put(o));
						addComplexCharacteristicNamedValue(userRemark, max, melemes, name);
//						System.out.println("Done with: " + name);
					}
				}
				if(melemes.length() > 0) {
					System.out.println( new org.json.JSONObject().put("_characteristicRecords", melemes) );
					try {
						System.out.println( rc.getRequest("PUT", baseUrlDEV + "/object/Product2G/" + id + "?includeLabels=true", new org.json.JSONObject().put("_characteristicRecords", melemes) .toString()) );
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else {

				}
			}
		}
		if(modifiedFields != null) {
			org.json.JSONObject variantes = (org.json.JSONObject) modifiedFields.remove("variants");
			if(variantes != null) {
				org.json.JSONObject variant = null;
				for(String variantId : org.json.JSONObject.getNames(variantes)) {
					variant = variantes.getJSONObject(variantId);

					org.json.JSONObject basicData = (org.json.JSONObject) variant.remove("basicData");
					org.json.JSONObject attributes = (org.json.JSONObject) variant.remove("attributes");
					org.json.JSONObject logisticData = (org.json.JSONObject) variant.remove("logisticData");
					org.json.JSONObject datosVenta = (org.json.JSONObject) variant.remove("datosVenta");
					org.json.JSONObject photos = (org.json.JSONObject) variant.remove("photos");

					int max = 0;

					org.json.JSONObject article = getArticleObject(variantId);
					String id = article.getJSONObject("_entityItem").getString("_internalId");
					org.json.JSONArray characteristicRecords = article.getJSONObject("_data").getJSONArray("_characteristicRecords");
					java.util.Map<String, java.util.LinkedList<org.json.JSONObject> > mep = new java.util.TreeMap<>();
					java.util.Map<String, org.json.JSONObject > mepsiman = new java.util.TreeMap<>();
					java.util.LinkedList<org.json.JSONObject> lst = null;
					org.json.JSONObject charRec = null;
					org.json.JSONArray melemes = new org.json.JSONArray();
					for(int i=0; i<characteristicRecords.length(); i++) {
						charRec = characteristicRecords.getJSONObject(i);
						trincaleLosLookups(charRec);
						lst = mep.get(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
						if(lst == null) {
							lst = new java.util.LinkedList<>();
							mep.put(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), lst);
						}
						lst.addLast(charRec);
					}
	//				System.out.println(mep.get("OwnersManual"));
					java.util.LinkedList<org.json.JSONObject> multimediaElementlList = null;
					multimediaElementlList = mep.get("ProductImage");
					if(multimediaElementlList != null){
						for(org.json.JSONObject elm : multimediaElementlList) {
							chagama(elm, mep, mepsiman, "ProductImage");
						}
					}
					multimediaElementlList = mep.get("ProductImageDetail");
					if(multimediaElementlList != null){
						for(org.json.JSONObject elm : multimediaElementlList) {
							chagama(elm, mep, mepsiman, "ProductImageDetail");
						}
					}
					multimediaElementlList = mep.get("ProductImageSmosh");
					if(multimediaElementlList != null){
						for(org.json.JSONObject elm : multimediaElementlList) {
							chagama(elm, mep, mepsiman, "ProductImageSmosh");
						}
					}
					multimediaElementlList = mep.get("Illustration");
					if(multimediaElementlList != null){
						for(org.json.JSONObject elm : multimediaElementlList) {
							chagama(elm, mep, mepsiman, "Illustration");
						}
					}
					for(String name : org.json.JSONObject.getNames(photos)) {
						System.out.println("Vamos a tomar los neims de: " + name);
						for(String subNeim : org.json.JSONObject.getNames(photos.getJSONObject(name))) {
							System.out.println("-->" + subNeim);
							lst = mep.get(name + "_" + subNeim);
							if(lst != null) {
								for(org.json.JSONObject rej : lst) {
									deactivateBoy(rej, name);
								}
								max = getMaxBoy(lst);
								max++;
								addComplexCharacteristicNamedValue(photos.getJSONObject(name).getJSONArray(subNeim).getJSONObject(0), max, mepsiman.get(name + "_" + subNeim).getJSONArray("_children"), name, true);
								melemes.put(mepsiman.get(name + "_" + subNeim));
								System.out.println("____" + melemes);
//							}else {
//								System.out.println("Nel (A)... ");
							}
						}
					}
	//				System.out.println("*****");
					org.json.JSONArray normalCharacteristics = new org.json.JSONArray();
					if(basicData != null) {
						normalCharacteristics.put(basicData);
					}
					if(attributes != null) {
						normalCharacteristics.put(attributes);
					}
					if(logisticData != null) {
						normalCharacteristics.put(logisticData);
					}
					if(datosVenta != null) {
						normalCharacteristics.put(datosVenta);
					}
					org.json.JSONObject elm = null;
					String theName = null;
					for(int j = 0; j<normalCharacteristics.length(); j++) {
						elm = normalCharacteristics.getJSONObject(j);
						for(String name : org.json.JSONObject.getNames(elm)) {
							userRemarks = elm.getJSONArray(name);
							userRemark = userRemarks.getJSONObject(0);
							theName = name + "_Rechazo";
							lst = mep.get(theName);
							for(org.json.JSONObject le : lst) {
								deactivateBoy(le, name);
							}
							max = getMaxBoy(lst);
							max++;
							lst.forEach(o -> melemes.put(o));
							addComplexCharacteristicNamedValue(userRemark, max, melemes, name);
	//						System.out.println("Done with: " + name);
						}
					}
					if(melemes.length() > 0) {
						System.out.println("Variant: " +  new org.json.JSONObject().put("_characteristicRecords", melemes) );
						try {
							System.out.println( rc.getRequest("PUT", baseUrlDEV + "/object/Product2G/" + id + "?includeLabels=true", new org.json.JSONObject().put("_characteristicRecords", melemes) .toString()) );
						} catch (Exception e) {
							e.printStackTrace();
						}
					}else {

					}
				}
			}
		}
	}

	private static java.util.LinkedList<org.json.JSONObject> chagamizalos(org.json.JSONObject chagama) {
		java.util.LinkedList<org.json.JSONObject> chagamitas = new java.util.LinkedList<>();
		org.json.JSONArray children = chagama.has("_children") ? chagama.getJSONArray("_children") : null;
		if(children != null) {
			chagamitas.addLast(chagama);
		}
		return chagamitas;
	}

	private static void quitalLosKeisAlChagama(org.json.JSONObject chagama) {
		if(chagama.has("_datatype") && chagama.getString("_datatype").equals("LOOKUP") && chagama.has("_recordLang")) {
			chagama.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).remove("_key");
		}
		org.json.JSONArray children = chagama.has("_children") ? chagama.getJSONArray("_children") : null;
		if(children != null) {
			for(int i=0; i<children.length(); i++) {
				quitalLosKeisAlChagama(children.getJSONObject(i));
			}
		}
	}

	private static void chagama(org.json.JSONObject chagama, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> meps, java.util.Map<String, org.json.JSONObject> pepsiman, String parentTag) {
		String chagamaBaseName = chagama.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
		String chagamaName = null;
		org.json.JSONArray children = chagama.getJSONArray("_children");
		org.json.JSONObject child = null;
		org.json.JSONObject qualification = null;
		org.json.JSONObject characteristic = null;
		org.json.JSONArray recordLang = null;
		org.json.JSONObject recordLangEntry = null;
		org.json.JSONArray values = null;
		String name = null;
		java.util.LinkedList<org.json.JSONObject> chagamitas = new java.util.LinkedList<>();
		for(int i=0; i<children.length(); i++) {
			child = children.getJSONObject(i);
			qualification = child.getJSONObject("_qualification");
			characteristic = qualification.getJSONObject("characteristic");
			name = characteristic.getString("_code");
			if( (chagamaBaseName + "_Name").equals(name) ) {
				recordLang = child.getJSONArray("_recordLang");
				recordLangEntry = recordLang.getJSONObject(0);
				values = recordLangEntry.getJSONArray("values");
				chagamaName = values.getString(0);
			}else if( (chagamaBaseName + "_Rejection").equals(name) ) {
				chagamitas.addLast(child);
				quitalLosKeisAlChagama(child);
			}
		}
		meps.put(parentTag + "_" + chagamaName, chagamitas);
		pepsiman.put(parentTag + "_" + chagamaName, chagama);
	}

	private static int getMaxBoy(java.util.LinkedList<org.json.JSONObject> lst) {
		String recordKey = null;
		String[] recordKeyParts = null;
		Integer firstPart = null;
		Integer secondPart = null;
		int max = -1;
		for(org.json.JSONObject ent : lst) {
			recordKey = ent.getJSONObject("_qualification").getString("recordKey");
			recordKeyParts = recordKey.split("\\.");
			firstPart = Integer.parseInt(recordKeyParts[0]);
			if(firstPart == 0) {
				secondPart = Integer.parseInt(recordKeyParts[1]);
				max = max > secondPart ? max : secondPart;
			}
		}
		return max;
	}

	private static void tratamientoChagama( java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> mep, String id ) {
		java.util.LinkedList<org.json.JSONObject> lst = null;
		String baseName = "VoltajeKVaD";
		lst = mep.get(baseName + "_Rechazo");
		lst.forEach(System.out::println);
		for(org.json.JSONObject ent : lst) {
			deactivateBoy(ent, baseName);
		}
		org.json.JSONArray ola = new org.json.JSONArray();
		for(org.json.JSONObject ent : lst) {
			ola.put(ent);
		}
		try {
			System.out.println( rc.getRequest("PUT", baseUrlDEV + "/object/Product2G/" + id, new org.json.JSONObject().put("_characteristicRecords", ola) .toString()) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void trincaleLosLookups(org.json.JSONObject j) {
		j.remove("lookupValue");
		org.json.JSONArray children = j.has("_children") ? j.getJSONArray("_children") : null;
		if(children != null) {
			for(int i=0; i<children.length(); i++) {
				trincaleLosLookups(children.getJSONObject(i));
			}
		}
	}

	private static void deactivateBoy(org.json.JSONObject boy, String baseName) {
		org.json.JSONArray children = boy.getJSONArray("_children");
		org.json.JSONObject lindependent = null;
		for(int i=0; i<children.length(); i++) {
			if(("rem_" + baseName).equals(children.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
				lindependent = getMeTheUnlocalized( children.getJSONObject(i).getJSONArray("_recordLang"));
				if(lindependent == null) {
					// PANIC
				}else {
					lindependent.getJSONArray("values").getJSONObject(0).remove("_key");
					lindependent.getJSONArray("values").getJSONObject(0).put("_code", "CS02");
				}
			}
		}
	}

	private static org.json.JSONObject getMeTheUnlocalized(org.json.JSONArray recordLang){
		for(int i=0; i<recordLang.length(); i++) {
			if("zxx".equals(recordLang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
				return recordLang.getJSONObject(i);
			}
		}
		return null;
	}

	/******
	 *
	 * Look at me ese
	 *
	 * ***************/
	private static void addComplexCharacteristicValue(org.json.JSONObject userRemark, int i, org.json.JSONArray characteristicArray) {
		org.json.JSONArray children = new org.json.JSONArray();
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
								.put("recordKey", i == 0 ? "0000.0000.RK" : "000" + i + ".0000.RK")
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												"RejectionLog_Comment")))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("comment"))))));
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
								.put("recordKey", i == 0 ? "0000.0000.RK" : "000" + i + ".0000.RK")
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												"RejectionLog_Date")))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("date"))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
										.put("recordKey",
												i == 0 ? "0000.0000.RK" : "000" + i + ".0000.RK")
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"RejectionLog_SubmittingRole")))
//						.put("_recordLang",
//								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
//									new org.json.JSONArray().put(
//										new JSONObject()
//										.put("_qualification",
//											new JSONObject().put("language",
//												new JSONObject().put("_code", "es")))
//										.put("_label", userRemark.getString("submittingRole"))))))
						)
						;
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
										.put("recordKey",
												i == 0 ? "0000.0000.RK" : "000" + i + ".0000.RK")
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"RejectionLog_TargetRole")))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("targetRole")))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
										.put("recordKey",
												i == 0 ? "0000.0000.RK" : "000" + i + ".0000.RK")
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"RejectionLog_Status")))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("status")))))));
		characteristicArray.put(new org.json.JSONObject().put("_qualification",
				new JSONObject().put("recordKey", i == 0 ? "0000.0000.RK" : "000" + i + ".0000.RK")
						.put("characteristic", new JSONObject().put("_code", "RejectionLog")))
				.put("_recordLang",
						new org.json.JSONArray()
								.put(new JSONObject().put("values", new org.json.JSONArray())))
				.put("_children", children));
	}


	/******
	 *
	 * Look at me ese
	 *
	 * ***************/
	private static void addComplexCharacteristicNamedValue(org.json.JSONObject userRemark, int i, org.json.JSONArray characteristicArray, String mainCharacteristicIdentifier) {
		addComplexCharacteristicNamedValue(userRemark, i, characteristicArray, mainCharacteristicIdentifier, false);
	}

	/******
	 *
	 * Look at me ese
	 *
	 * ***************/
	private static void addComplexCharacteristicNamedValue(org.json.JSONObject userRemark, int i, org.json.JSONArray characteristicArray, String mainCharacteristicIdentifier, boolean multimedia) {
		org.json.JSONArray children = new org.json.JSONArray();
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
								.put("recordKey", i == 0 ? "0000.0000.RK" : "0000." + ( i < 10 ? "000" + i : i < 100 ? "00" + i : i < 1000 ? "0" + i : i ) + ".RK")
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												(multimedia ? mainCharacteristicIdentifier + "_AdditionalComment" : "msj_" + mainCharacteristicIdentifier) )))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("comment"))))));
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
						.put("recordKey", i == 0 ? "0000.0000.RK" : "0000." + ( i < 10 ? "000" + i : i < 100 ? "00" + i : i < 1000 ? "0" + i : i ) + ".RK")
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												"rmum_" + mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("date"))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
									.put("recordKey", i == 0 ? "0000.0000.RK" : "0000." + ( i < 10 ? "000" + i : i < 100 ? "00" + i : i < 1000 ? "0" + i : i ) + ".RK")
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rre_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("submittingRole")))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", i == 0 ? "0000.0000.RK" : "0000." + ( i < 10 ? "000" + i : i < 100 ? "00" + i : i < 1000 ? "0" + i : i ) + ".RK")
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rrd_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("targetRole")))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", i == 0 ? "0000.0000.RK" : "0000." + ( i < 10 ? "000" + i : i < 100 ? "00" + i : i < 1000 ? "0" + i : i ) + ".RK")
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rem_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("status")))))));
		characteristicArray.put(new org.json.JSONObject().put("_qualification",
				new JSONObject()
				.put("recordKey", i == 0 ? "0000.0000.RK" : "0000." + ( i < 10 ? "000" + i : i < 100 ? "00" + i : i < 1000 ? "0" + i : i ) + ".RK")
						.put("characteristic", new JSONObject().put("_code", mainCharacteristicIdentifier + (multimedia ? "_Rejection" : "_Rechazo"))))
				.put("_recordLang",
						new org.json.JSONArray()
								.put(new JSONObject().put("values", new org.json.JSONArray())))
				.put("_children", children));
	}


	private static org.json.JSONObject getProduct2GObject(String proposalId){
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject request = null;
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/object/Product2G/'" + proposalId + "'@'MASTER'?entityFilter=Product2GCharacteristicValue&includeIds=true", null);
			response = new org.json.JSONObject(rawResponse);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private static org.json.JSONObject getArticleObject(String proposalId){
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject request = null;
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/object/Article/'" + proposalId + "'@'MASTER'?entityFilter=ArticleCharacteristicValue&includeIds=true", null);
			System.out.println(rawResponse);
			response = new org.json.JSONObject(rawResponse);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private static String formatMillis(long millis){
	  	int days = (int)(millis/(1000*60*60*24));
	 	millis -= days*1000*60*60*24;
	  	int hours = (int) (millis/(1000*60*60));
	  	millis -= hours*1000*60*60;
	  	int minutes = (int) (millis/(1000*60));
	  	millis -= minutes*1000*60;
	  	int seconds = (int) (millis/1000);
	  	millis -= seconds*1000;
	  	return
	  		    (days < 10 ? "0" : "") + days + ":"
	  		+ (hours < 10 ? "0" : "") + hours + ":"
	  		+ (minutes < 10 ? "0" : "") + minutes + ":"
	  		+ (seconds < 10 ? "0" : "") + seconds
	  		+ "." + millis;
	  }
}
