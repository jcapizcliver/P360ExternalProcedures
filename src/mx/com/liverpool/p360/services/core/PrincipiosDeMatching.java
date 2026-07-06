package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class PrincipiosDeMatching {

	private static final RESTWrapper rw = new RESTWrapper();
	
	private final java.util.Map<String, java.util.LinkedList< String >> resultsBoard = new java.util.TreeMap<>();
	private final java.util.List<String[]> variantsData = new java.util.ArrayList<>();
	
	private final java.util.Map<String, java.math.BigDecimal> ponderaciones = new java.util.TreeMap<>();
	private final java.math.BigDecimal threshold;
	
	public PrincipiosDeMatching() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.AlternativeValue");
		qp.put("dictionary", "'MatchingWeights'");
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			try {
				java.math.BigDecimal db = new java.math.BigDecimal(values.getString(1));
				ponderaciones.put(values.getString(0), db);
			}catch(NumberFormatException e) {
				log("Unparseable value: " + values);
			}
		}, this::log);
		log("Working with:");
		ponderaciones.forEach((k,v)-> log(k + ": " + v) );
		qp.clear();
		org.json.JSONObject response = rw.getRw().makeRequest("GET", "/object/StandardizationValue/" + rw.getRw().encode("'MatchingThreshold'@'GlobalSyststemProperties'"), qp, null);
		if(response != null && response.has("_data")) {
			String alternativeValue = response.getString("alternativeValue");
			java.math.BigDecimal holder = null;
			try {
				holder = new java.math.BigDecimal(alternativeValue);
			}catch(NumberFormatException e) {
				log("Not a valid value registered: " + alternativeValue + ", using default: 55.");
				holder = new java.math.BigDecimal(55);
			}
			threshold = holder;
		}else {
			log("No matching threshold configuration found, using default: 55.");
			threshold = new java.math.BigDecimal(55);
		}
//		ponderaciones.put("ProductTypeSAP", 20);
//		ponderaciones.put("SB_0002", 20);
//		ponderaciones.put("ItemGroup", 10);
//		ponderaciones.put("ItemGroupS4H", 10);
//		ponderaciones.put("BrandName", 20);
//		ponderaciones.put("BRAND_ID_S4H", 20);
//		ponderaciones.put("SupplierPartNumber", 40);
//		ponderaciones.put("ColoursLiverpoolAtt", 5);
//		ponderaciones.put("TamanoUnico", 5);
	}
	
	public static void main(String[] args) {
		PrincipiosDeMatching pdm = new PrincipiosDeMatching();
		rw.getRw().setBaseUrl("http://172.18.237.162:1512/rest/V2.0");
		rw.getRw().addHeader("Authorization", java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		RESTWrapper rw = new RESTWrapper();
		RESTWorkshop workshop = rw.getRw();
		workshop.setBaseUrl("http://172.18.237.162:1512/rest/V2.0");
		workshop.addHeader("Authorization", java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		String[] pids = "1698767481928042;1698767481928082;1698767481928102;1698767481928126;1698767481928150;1698767481928174;1698767481928198;1698767481928054;1698767481928078;1698767481928106;1698767481928134;1698767481928158;1698767481928182;1698767481928206;1698767481928062;1698767481928094;1698767481928118;1698767481928142;1698767481928166;1698767481928190;1698767481928214".split(";");
		for( String externalId : pids) {
			String internalId = "'" + externalId + "'@1";
			PrincipiosDeMatching pm = new PrincipiosDeMatching();
			String entity = "Product2G";
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("entityFilter", entity +"," + entity + "Log," + entity + "CharacteristicValue");
			qp.put("includeIds", "true");
			qp.put("includeLabels", "true");
//			workshop.setBaseUrl("http://172.18.237.162:1512/rest/V2.0");
//			workshop.addHeader("Authorization", "Basic cmVzdDpoZWlsZXI=");
			pdm.log("requesting: " + workshop.getBaseUrl() + "/object/" + entity + "/" + internalId);
			org.json.JSONObject response = workshop.makeRequest("GET", "/object/" + entity + "/" + internalId, qp, null);
			java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicRecordsMap = new java.util.TreeMap<>();
			org.json.JSONArray rechazos = new org.json.JSONArray();
			if(response != null) {                                                                                                                                                                                                                                                                                                                    
				org.json.JSONObject data = response.getJSONObject("_data");
				org.json.JSONArray characteristicRecords = data.has("_characteristicRecords") ? data.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
				pdm.characteristicsToMap(characteristicRecords, characteristicRecordsMap, rechazos);
				String itemGroup = getValue("ItemGroup", characteristicRecordsMap);
				String itemGroupS4H = getValue("ItemGroupS4H", characteristicRecordsMap);
				String supplierPartNumber = getSimpleValue("SupplierPartNumber", characteristicRecordsMap);
				String brandName = getValue("BrandName", characteristicRecordsMap);
				String brandIdS4H = getValue("BRAND_ID_S4H", characteristicRecordsMap);
				String productTypeSAP = getValue("ProductTypeSAP", characteristicRecordsMap);
				String sb0002 = getValue("SB_0002", characteristicRecordsMap);
	
				java.util.LinkedList<String> participantes = null;
				qp.clear();
				qp.put("fields", 
						  "Article.SupplierAID"
						+ ",SimpleArticleCharacteristicValue.LookupValue('TamanoUnico',-1)->LookupValue.Code"
						+ ",SimpleArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',-1)->LookupValue.Code"
						+ ",SimpleArticleCharacteristicValueLang.Value('SupplierPartNumber',-1)"
					);
				qp.put("products", "'" + externalId + "'@1");
				response = null;
				org.json.JSONArray rows = null;
				org.json.JSONArray values = null;
				String variantId = null;
				response = workshop.makeRequest("GET", "/list/Article/byProducts", qp, null);
				if(response == null || (response != null && !response.has("totalSize"))) {
					pdm.log("Error on getting variants of the product (" + externalId + "): " + workshop.getRawResponse());
				}else {
					rows = response.getJSONArray("rows");
					for(int i=0; i < rows.length(); i++) {
						values = rows.getJSONObject(i).getJSONArray("values");
						variantId = values.getString(0);
						pdm.log("For: " + variantId);
						participantes = pm.run(externalId, new org.json.JSONObject().put("variantId", variantId).put("TamanoUnico", values.getJSONArray(1).getString(0)).put("ColoursLiverpoolAtt", values.getJSONArray(2).getString(0)).put("SupplierPartNumber", values.getJSONArray(3).getString(0)).toString(), workshop, productTypeSAP, sb0002, itemGroup, itemGroupS4H, brandName, brandIdS4H, supplierPartNumber);
						if(!participantes.isEmpty()) {
							pdm.log("Found matches! " + participantes);
						}else {
							pdm.log("No match found");
						}
					}
					pdm.log("Now packing results...");
	//				pm.packResults();
				}
			}else {
				pdm.log("My bad: " + workshop.getRawResponse());
			}
		}
	}
	
	public java.util.LinkedList<String> run(final String proposalId, final String input, final RESTWorkshop rw, String productTypeSAP, String sb0002, String itemGroup, String itemGroupS4H, String brandName, String brandIdS4H, String supplierPartNumber){
		return run(proposalId, input, rw, productTypeSAP, sb0002, itemGroup, itemGroupS4H, brandName, brandIdS4H, supplierPartNumber, true);
	}
	
	public java.util.LinkedList<String> run(final String proposalId, final String input, final RESTWorkshop rw, String productTypeSAP, String sb0002, String itemGroup, String itemGroupS4H, String brandName, String brandIdS4H, String supplierPartNumber, boolean createTask) {
		java.util.LinkedList<String> idsParticipantes = new java.util.LinkedList<>();
		if(input == null) {
			log("{}");
			return idsParticipantes;
		}
		log("Input: " + input);
		String variantId = null;
		org.json.JSONObject json = null;
		try{
			json = new org.json.JSONObject(input);
		}catch(org.json.JSONException e) {
			log("Problem parsing input: " + input);
			return idsParticipantes;
		}
		variantId = json.getString("variantId");
		String[] args = new String[] { json.getString("TamanoUnico"), json.getString("SupplierPartNumber"), json.getString("ColoursLiverpoolAtt") };
		if(variantId == null || args.length == 0) {
			log("No variantId found or no elements visible");
			return idsParticipantes;
		}
		idsParticipantes = collectVariantsOfRelevantProductsForMatchingOld(proposalId == null ? "" : proposalId, itemGroup == null ? "" : itemGroup
				, itemGroupS4H == null ? "" : itemGroupS4H, brandName == null ? "" : brandName, brandIdS4H == null ? "" : brandIdS4H
						, productTypeSAP == null ? "" : productTypeSAP, sb0002 == null ? "" : sb0002, supplierPartNumber == null ? "" : supplierPartNumber, args[2], args[0], args[1], rw, idsParticipantes);
//		if(!idsParticipantes.isEmpty()) {
//			idsParticipantes.addLast(variantId);
//			idsParticipantes.forEach(this::log);
//			org.json.JSONArray boys = new org.json.JSONArray();
//			for(String id : idsParticipantes) {
//				boys.put("'" + id + "'@'MASTER'");
//			}
//			String processID = getMatchingTaskID();
//			processID = processID == null ? "23544" : processID.replaceAll("LookupValue_", "");
//			createWorkflow(boys, processID, "MatchingRevision", "Revisión Matching", rw);
//		}
//		resultsBoard.put(variantId, idsParticipantes);
		org.json.JSONArray boys = new org.json.JSONArray();
		for(String id : idsParticipantes) {
			boys.put("'" + id + "'@'MASTER'");
		}
		String processID = getMatchingTaskID();
		processID = processID == null ? "23544" : processID.replaceAll("LookupValue_", "");
		createWorkflow(boys, processID, "MatchingRevision", "Revisión Matching", rw);
		return idsParticipantes;
	}
	
	/*
	public void packResults() {
		String[] info = null;
		java.math.BigDecimal lacuenta = new java.math.BigDecimal(0);
		java.util.LinkedList<String> refs = null;
		java.util.LinkedList<String> idsParticipantes = null;
		log("Variants data: " + variantsData);
		for(String[] infoBase : variantsData) {
			String brandName = infoBase[2];
			String brandIdS4H = infoBase[3];
			String productTypeSAP = infoBase[4];
			String sb0002 = infoBase[5];
			String itemGroup = infoBase[6];
			String itemGroupS4H = infoBase[7];
			String parentSupplierPartNumber = infoBase[8];
			String color = infoBase[11];
			String talla = infoBase[12];
			String supplierPartNumber = infoBase[13];
			log(brandName + " vs " + info[2]);
			log(brandIdS4H + " vs " + info[3]);
			log(productTypeSAP + " vs " + info[4]);
			log(sb0002 + " vs " + info[5]);
			log(itemGroup + " vs " + info[6]);
			log(itemGroupS4H + " vs " + info[7]);
			log(parentSupplierPartNumber + " vs " + info[8]);
			log(color + " vs " + info[11]);
			log(talla + " vs " + info[12]);
			log(supplierPartNumber + " vs " + info[13]);
			for(int i=1; i<variantsData.size(); i++) {
				info = variantsData.get(i);
				if(brandName != null && !"".equals(info[2]) && !"".equals(brandName) && brandName.equals(info[2])) {
					lacuenta = lacuenta.add( ponderaciones.get("BrandName") );
				}
				if(brandIdS4H != null && !"".equals(info[3]) && !"".equals(brandIdS4H) && brandIdS4H.equals(info[3])) {
					lacuenta = lacuenta.add( ponderaciones.get("BRAND_ID_S4H") );
				}
				if(productTypeSAP != null && !"".equals(info[4]) && !"".equals(productTypeSAP) && productTypeSAP.equals(info[4])) {
					lacuenta = lacuenta.add( ponderaciones.get("ProductTypeSAP") );
				}
				if(sb0002 != null && !"".equals(info[5]) && !"".equals(sb0002) && sb0002.equals(info[5])) {
					lacuenta = lacuenta.add( ponderaciones.get("SB_0002") );
				}
				if(itemGroup != null && !"".equals(info[6]) && !"".equals(itemGroup) && itemGroup.equals(info[6])) {
					lacuenta = lacuenta.add( ponderaciones.get("ItemGroup") );
				}
				if(itemGroupS4H != null && !"".equals(info[7]) && !"".equals(itemGroupS4H) && itemGroupS4H.equals(info[7])) {
					lacuenta = lacuenta.add( ponderaciones.get("ItemGroupS4H") );
				}
				if( (supplierPartNumber != null && !supplierPartNumber.equals(parentSupplierPartNumber)) ) {
					if(supplierPartNumber != null && !"".equals(info[13]) && !"".equals(supplierPartNumber) && supplierPartNumber.equals(info[13])) {
						lacuenta = lacuenta.add( ponderaciones.get("SupplierPartNumber") );
					}
				}else {
					if(parentSupplierPartNumber != null && !"".equals(info[8]) && !"".equals(parentSupplierPartNumber) && parentSupplierPartNumber.equals(info[8])) {
						lacuenta = lacuenta.add( ponderaciones.get("SupplierPartNumber") );
					}
				}
				if(color != null && !"".equals(info[11]) && !"".equals(color) && color.equals(info[11])) {
					lacuenta = lacuenta.add( ponderaciones.get("ColoursLiverpoolAtt") );
				}
				if(talla != null && !"".equals(info[12]) && !"".equals(talla) && talla.equals(info[12])) {
					lacuenta = lacuenta.add( ponderaciones.get("TamanoUnico") );
				}
				log("La cuenta fue de: " + lacuenta);
				if(lacuenta.compareTo(threshold) >= 0) {
					refs = resultsBoard.remove( info[10] );
					idsParticipantes = resultsBoard.get( infoBase[10] );
					idsParticipantes.addAll(refs);
					resultsBoard.put( info[10] , idsParticipantes);
					log("Added: " + info[10]);
				}
				lacuenta = java.math.BigDecimal.ZERO;
			}
		}
		resultsBoard.forEach((k,v)-> v.add(k) );
		java.util.Set<java.util.LinkedList<String>> set = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>(resultsBoard.size()));
		set.addAll(resultsBoard.values());
		java.util.List<java.util.LinkedList<String>> dio = new java.util.ArrayList<>(set);
		dio.forEach(list ->{
			org.json.JSONArray boys = new org.json.JSONArray();
			for(String id : list) {
				boys.put("'" + id + "'@'MASTER'");
			}
			String processID = getMatchingTaskID();
			processID = processID == null ? "23544" : processID.replaceAll("LookupValue_", "");
			createWorkflow(boys, processID, "MatchingRevision", "Revisión Matching", rw.getRw());
		});
	}
	
	
	private String[] collectVariantsOfRelevantProductsForMatching(String brandName, String brandIdS4H, String productTypeSAP, String sb0002, RESTWorkshop workshop) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"ProductReference.ReferencedSupplierAid"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2G.CurrentStatus"
				);
		qp.put("query", 
				  (brandName != null && !"".equals(brandName) ? "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code" + " = '" + brandName + "'@'ZCOMALOV'" : "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code" + " = '" + brandIdS4H + "'@'BRAND_IDLOV_S4H'" )
				+ " or "
				+ (productTypeSAP != null && !"".equals(productTypeSAP) ? "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP')->LookupValue.Code" + " = '" + productTypeSAP + "'@'PE000LOV'" : "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('SB_0002',root,\"0000.0000.RK\",'SB_0002')->LookupValue.Code" + " = '" + sb0002 + "'@'SB_0002LOV'" )
			);
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Article/ProductReference/bySearch", qp, null);
		if(response != null && response.getJSONArray("rows").length() > 0) {
			return new String[] { 
					  response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(4).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(5).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(6)
					};
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}
	
	/*
	private void collectVariantsOfRelevantProductsForMatching(String currentProductNo, String itemGroup, String itemGroupS4H, String brandName, String brandIdS4H, String productTypeSAP, String sb0002, String parentSupplierPartNumber, String color, String talla, String supplierPartNumber, RESTWorkshop workshop, java.util.List<String> idsParticipantes) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",ArticleCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('SB_0002',root,\"0000.0000.RK\",'SB_0002')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",ArticleCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber', -1)"
				+ ",ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico')->LookupValue.Code"
				+ ",Article.CurrentStatus"
				);
		String qr = null;
		qp.put("query", qr =
				  "not ProductReference.ReferencedSupplierAid = \"" + currentProductNo + "\" and ("
						+ ( brandName != null || brandIdS4H != null ?( brandName != null && !"".equals(brandName) ? "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code" + " = \"" + brandName + "\"" : "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code" + " = \"" + brandIdS4H + "\"" ) : "" )
						+ ( itemGroup != null || itemGroupS4H != null ? " or "
						+ (itemGroup != null && !"".equals(itemGroup) ? "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code" + " = \"" + itemGroup + "\"" : "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code" + " = \"" + itemGroupS4H + "\"" ) : "" )
						+ (productTypeSAP != null || sb0002 != null ? " or "
						+ 	(productTypeSAP != null && !"".equals(productTypeSAP) ? "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP')->LookupValue.Code" + " = \"" + productTypeSAP + "\"" : "ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('SB_0002',root,\"0000.0000.RK\",'SB_0002')->LookupValue.Code" + " = \"" + sb0002 + "\"" ) : "" )
					+ ")"
			);
		log("Using: " + qr);
		log("Now collecting candidate variants...");
		java.util.LinkedList<String[]> data = new java.util.LinkedList<>();
		rw.collectData("list", "Article", "ProductReference", "bySearch", qp, row -> data.addLast(new String[] {
					  row.getJSONObject("object").getString("id")
					, row.getJSONArray("values").getString(0)
					, row.getJSONArray("values").getJSONArray(1).getString(0)
					, row.getJSONArray("values").getJSONArray(2).getString(0)
					, row.getJSONArray("values").getJSONArray(3).getString(0)
					, row.getJSONArray("values").getJSONArray(4).getString(0)
					, row.getJSONArray("values").getJSONArray(5).getString(0)
					, row.getJSONArray("values").getJSONArray(6).getString(0)
					, row.getJSONArray("values").getJSONArray(7).getString(0)
					, row.getJSONArray("values").getString(8)
					, "" // Article ID
					, "" // Color
					, "" // Tamaño
					, "" // SPN
					}), this::log);
		log("Collected: " + data.size() + " candidates.");
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int[] innerCounter = new int[2];
		innerCounter[0] = 0;
		qp.clear();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt', root, \"0000.0000.RK\", 'ColoursLiverpoolAtt')->LookupValue.Code"
				+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico', root, \"0000.0000.RK\", 'TamanoUnico')->LookupValue.Code"
				+ ",ArticleCharacteristicValueLang.Value('SupplierPartNumber', root, \"0000.0000.RK\", 'SupplierPartNumber', -1)"
			);
		java.util.ArrayList<String[]> dataMtx = new java.util.ArrayList<>(data);
		data.clear();
		for(String[] pieces : dataMtx) {
			sb.append(sb.length() == 0 ? "" : ",").append(pieces[0]);
			count++;
			if(count == 1000) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Article", null, "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					dataMtx.get(innerCounter[0])[10] = values.getString(0);
					dataMtx.get(innerCounter[0])[11] = values.getJSONArray(1).getString(0);
					dataMtx.get(innerCounter[0])[12] = values.getJSONArray(2).getString(0);
					dataMtx.get(innerCounter[0])[13] = values.getJSONArray(3).getString(0);
					innerCounter[0] ++;
				}, this::log);
				sb.setLength(0);
			}
		}
		if(count > 0) {
			qp.put("items", sb.toString());
			rw.collectData("list", "Article", null, "byItems", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				dataMtx.get(innerCounter[0])[10] = values.getString(0);
				dataMtx.get(innerCounter[0])[11] = values.getJSONArray(1).getString(0);
				dataMtx.get(innerCounter[0])[12] = values.getJSONArray(2).getString(0);
				dataMtx.get(innerCounter[0])[13] = values.getJSONArray(3).getString(0);
				innerCounter[0] ++;
			}, this::log);
			sb.setLength(0);
		}
		log("Now printing ñaca ñaca: ");
		dataMtx.forEach(s -> log( rw.getRw().serializeChunk(s, "\"", ",", "\\") ));
		java.math.BigDecimal lacuenta = java.math.BigDecimal.ZERO;
		for(String[] info : dataMtx) {
//			log(brandName + " vs " + info[2]);
//			log(brandIdS4H + " vs " + info[3]);
//			log(productTypeSAP + " vs " + info[4]);
//			log(sb0002 + " vs " + info[5]);
//			log(itemGroup + " vs " + info[6]);
//			log(itemGroupS4H + " vs " + info[7]);
//			log(parentSupplierPartNumber + " vs " + info[8]);
//			log(color + " vs " + info[11]);
//			log(talla + " vs " + info[12]);
//			log(supplierPartNumber + " vs " + info[13]);
			if(brandName != null && !"".equals(info[2]) && !"".equals(brandName) && brandName.equals(info[2])) {
				lacuenta = lacuenta.add( ponderaciones.get("BrandName") );
			}
			if(brandIdS4H != null && !"".equals(info[3]) && !"".equals(brandIdS4H) && brandIdS4H.equals(info[3])) {
				lacuenta = lacuenta.add( ponderaciones.get("BRAND_ID_S4H") );
			}
			if(productTypeSAP != null && !"".equals(info[4]) && !"".equals(productTypeSAP) && productTypeSAP.equals(info[4])) {
				lacuenta = lacuenta.add( ponderaciones.get("ProductTypeSAP") );
			}
			if(sb0002 != null && !"".equals(info[5]) && !"".equals(sb0002) && sb0002.equals(info[5])) {
				lacuenta = lacuenta.add( ponderaciones.get("SB_0002") );
			}
			if(itemGroup != null && !"".equals(info[6]) && !"".equals(itemGroup) && itemGroup.equals(info[6])) {
				lacuenta = lacuenta.add( ponderaciones.get("ItemGroup") );
			}
			if(itemGroupS4H != null && !"".equals(info[7]) && !"".equals(itemGroupS4H) && itemGroupS4H.equals(info[7])) {
				lacuenta = lacuenta.add( ponderaciones.get("ItemGroupS4H") );
			}
			if( (supplierPartNumber != null && !supplierPartNumber.equals(parentSupplierPartNumber)) ) {
				if(supplierPartNumber != null && !"".equals(info[13]) && !"".equals(supplierPartNumber) && supplierPartNumber.equals(info[13])) {
					lacuenta = lacuenta.add( ponderaciones.get("SupplierPartNumber") );
				}
			}else {
				if(parentSupplierPartNumber != null && !"".equals(info[8]) && !"".equals(parentSupplierPartNumber) && parentSupplierPartNumber.equals(info[8])) {
					lacuenta = lacuenta.add( ponderaciones.get("SupplierPartNumber") );
				}
			}
			if(color != null && !"".equals(info[11]) && !"".equals(color) && color.equals(info[11])) {
				lacuenta = lacuenta.add( ponderaciones.get("ColoursLiverpoolAtt") );
			}
			if(talla != null && !"".equals(info[12]) && !"".equals(talla) && talla.equals(info[12])) {
				lacuenta = lacuenta.add( ponderaciones.get("TamanoUnico") );
			}
			log("La cuenta fue de: " + lacuenta);
			if(lacuenta.compareTo(threshold) >= 0) {
				idsParticipantes.add( info[10] );
				log("Added: " + info[10]);
			}
			lacuenta = java.math.BigDecimal.ZERO;
		}
	}
	*/
	
	private java.util.LinkedList<String> collectVariantsOfRelevantProductsForMatchingOld(String currentProductNo, String itemGroup, String itemGroupS4H, String brandName, String brandIdS4H, String productTypeSAP, String sb0002, String parentSupplierPartNumber, String color, String talla, String supplierPartNumber, RESTWorkshop workshop, java.util.List<String> idsParticipantes) {
		java.util.LinkedList<String> data = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Article.SupplierAID"
				);
//		qp.put("fields", 
//				   "ProductReference.ReferencedSupplierAid"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('SB_0002',root,\"0000.0000.RK\",'SB_0002')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber', -1)"
//				+ ",ProductReference.ReferencedArticle->Product2G.CurrentStatus"
//				);
		/*
		A = BrandName/BRAND_ID_S4H = 20
		B = ItemGroup/ItemGroupS4H = 10
		C = ProductTypeSAP/SB_0002 = 20
		D = SupplierPartNumber     = 40
		E = TamanoUnico            =  5
		F = ColoursLiverpoolAtt    =  5

		SupplierPartNumber + ProductTypeSAP/SB_0002|BrandName/BRAND_ID_S4H 
		SupplierPartNumber + ItemGroup/ItemGroupS4H + TamanoUnico|ColoursLiverpoolAtt
		BrandName/BRAND_ID_S4H + ProductTypeSAP/SB_0002 + ItemGroup/ItemGroupS4H + TamanoUnico|ColoursLiverpoolAtt
		*/
		boolean hayModelo = !"".equals(supplierPartNumber);
		boolean hayMarca = !"".equals(brandName) || !"".equals(brandIdS4H);
		boolean hayProducto = !"".equals(productTypeSAP) || !"".equals(sb0002);
		boolean hayItemGroup = !"".equals(itemGroup) || !"".equals(itemGroupS4H);
		boolean hayColorTalla = !"".equals(color) || !"".equals(talla);
		boolean caso1 = hayModelo && ( hayProducto || hayMarca ) ;
		boolean caso2 = hayModelo && hayItemGroup && hayColorTalla;
		boolean caso3 = hayMarca && hayProducto && hayItemGroup && hayColorTalla;
		if(!caso1 && !caso2 && !caso3) {
			log("No se necesita hacer algo...");
			return data;
		}
		String factorSupplierPartNumber = "characteristic('SupplierPartNumber',-1) = \"" + supplierPartNumber + "\"";
		String factorProducto = "characteristic('" + (!"".equals(productTypeSAP) ? "ProductTypeSAP" : "SB_0002" ) + "') = " + ( !"".equals(productTypeSAP) ? "'" + productTypeSAP + "'@'PE000LOV'" : "'" + sb0002 + "'@'SB_0002LOV'" );
		String factorMarca = "characteristic('" +  (!"".equals(brandName) ? "BrandName" : "BRAND_ID_S4H" ) + "') = " + (!"".equals(brandName) ? "'" + brandName + "'@'ZCOMALOV'" : "'" + brandIdS4H + "'@'BRAND_IDLOV_S4H'") ;
		String factorItemGroup = "characteristic('" +  (!"".equals(itemGroup) ? "ItemGroup" : "ItemGroupS4H" ) + "') = " + (!"".equals(itemGroup) ? "'" + itemGroup + "'@'MATKLLOV'" : "'" + itemGroupS4H + "'@'MATKLLOV_S4H'") ;
		String factorColorTalla = "characteristic('" +  (!"".equals(color) ? "ColoursLiverpoolAtt" : "TamanoUnico" ) + "') = " + (!"".equals(color) ? "'" + color + "'@'C100LOV'" : "'" + talla + "'@'TamanoUnicoLOV'") ;
		String query1 = !caso1 ? null : factorSupplierPartNumber + " and " + ( hayProducto ? factorProducto : factorMarca );
		String query2 = !caso2 ? null : factorSupplierPartNumber + " and " + factorItemGroup + " and " + factorColorTalla;
		String query3 = !caso3 ? null : factorMarca + " and " + factorProducto + " and " + factorItemGroup + " and " + factorColorTalla;
		String query = 
				caso1 && caso2 && caso3 ? (
				  "("
				+ query1
				+ ") "
				+ "or ("
				+ query2
				+ ") or ("
				+ query3
				+ ")"
				) : 
				(caso1 && caso2 )  ? (
						  "("
									+ query1
									+ ") "
									+ "or ("
									+ query2 
									+ ")"
				) : ( caso1 && caso3 ) ? (
						  "("
									+ query1
									+ ") "
									+ "or ("
									+ query3 
									+ ")"
						): (caso2 && caso3) ? (
								  "("
											+ query2
											+ ") "
											+ "or ("
											+ query3 
											+ ")"
											) :
												caso1 ? query1 : caso2 ? query2 : query3
				
			;

		qp.put("query", query);
		log("Using: " + query);
		log("Now collecting candidate variants...");
		rw.collectData("list", "Article", null, "bySearch", qp, row -> data.addLast(row.getJSONArray("values").getString(0)), this::log);
		log("Collected: " + data.size() + " candidates.");
		return data;
	}
	
	private String getMatchingTaskID() {
		String[] holder = new String[1];
		holder[0] = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		rw.writeData("object", "LookupValue", null, qp, new org.json.JSONObject().put("lookup", new org.json.JSONObject().put("_code", "MatchingTaskIDs").put("isActive", true)), rr -> {
			try {
				org.json.JSONObject resp = new org.json.JSONObject(rr);
				holder[0] = resp.getString("_identifier");
			}catch(org.json.JSONException e) {
				log(rr);
				logE(e);
			}
		});
		return holder[0];
	}
	
	private void createWorkflow(org.json.JSONArray internalId, String processId, String workflowId, String status, RESTWorkshop workshop) {
		org.json.JSONObject wkf = new org.json.JSONObject();
		String identifier = "MR_" + processId;
		wkf.put("identifier", identifier);
		wkf.put("label", "Revisión Matching");
		wkf.put("version", "1.0");
		wkf.put("status", new org.json.JSONArray()
				.put(new org.json.JSONObject()
						.put("status", status)
						.put("displayOrder", 1)
						.put("workflowTask", new org.json.JSONObject()
								.put("container", "1")
								.put("entity", "Article")
								.put("workflowServiceEndpoint", "Revision_Matching")
								.put("workflowCommunicationMode", "QUEUE")
								.put("workflowQueueId", "bpm_response")
								.put("userGroup", "RepoblamientoMatching")
								.put("name", "Revisión Matching")
								.put("description", "Tareas que necesitan resolver artículos duplicados.")
							)
					)
				);
		log("To ---");
		rw.writeData("manage", "workflow", null, new java.util.TreeMap<>(), wkf, this::log);
		log("/---");
		ingresaWorkflow(internalId, processId, identifier, status, workshop);
	}

	private void ingresaWorkflow(org.json.JSONArray internalId, String processId, String workflowId, String status, RESTWorkshop workshop) {
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Article");
		org.json.JSONObject response = null;
		rb.put("itemId", internalId);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		log("Sending: " + rb.toString());
		System.out.println("Sending: " + rb.toString());
		response = workshop.makeRequest("POST", "/manage/workflow/status/enter", qp, rb.toString());
		log(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
		System.out.println(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	}

	private String[] checkArticle(String id, RESTWorkshop workshop) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"ProductReference.ReferencedSupplierAid"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('SB_0002',root,\"0000.0000.RK\",'SB_0002')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber', -1)"
				+ ",ProductReference.ReferencedArticle->Product2G.CurrentStatus"
				);
		qp.put("items", id );
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Article/ProductReference/byItems", qp, null);
		if(response != null && response.getJSONArray("rows").length() > 0) {
			return new String[] { 
					  response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(4).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(5).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(6).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(7)
					};
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}

	private String getArticleExtId(String id, RESTWorkshop workshop) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"Article.SupplierAID"
				);
		qp.put("items", id );
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Article/byItems", qp, null);
		if(response != null && response.getJSONArray("rows").length() > 0) {
			return response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0); 
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}

	private static String getSimpleValue(String characteristicIdentifier, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicRecordsMap) {
		java.util.LinkedList<org.json.JSONObject> list = characteristicRecordsMap.get(characteristicIdentifier);
		if(list != null) {
			return list.getLast().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
		}
		return null;
	}

	private static String getValue(String characteristicIdentifier, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicRecordsMap) {
		java.util.LinkedList<org.json.JSONObject> list = characteristicRecordsMap.get(characteristicIdentifier);
		if(list != null) {
			return list.getLast().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
		}
		return null;
	}
	
	private void characteristicsToMap(org.json.JSONArray characteristicRecords, 
			java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap, 
			org.json.JSONArray rechazos){
		java.util.LinkedList<org.json.JSONObject> lst = null;
		org.json.JSONObject characteristicRecord = null;
		String characteristicIdentifier = null;
		org.json.JSONArray children = null;
		org.json.JSONObject child = null;
		org.json.JSONArray losChildren = null;
		org.json.JSONObject losChild = null;
		boolean notEnough = true;
		boolean losNotEnough = true;
		for(int i=0; i<characteristicRecords.length(); i++) {
			characteristicRecord = characteristicRecords.getJSONObject(i);
			characteristicIdentifier = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = characteristicRecordsMap.get(characteristicIdentifier);
			if(lst == null) {
				lst = new java.util.LinkedList<>();
				characteristicRecordsMap.put(characteristicIdentifier, lst);
			}
			lst.addLast(characteristicRecord);
			if(
				characteristicIdentifier.endsWith("_Rechazo") || 
				characteristicIdentifier.equals("Comentario") ||
				characteristicIdentifier.equals("ProductImage") ||
				characteristicIdentifier.equals("ProductImageDetail") ||
				characteristicIdentifier.equals("ProductImageSmosh") ||
				characteristicIdentifier.equals("Illustration")
			) {
				log("Entering to: " + characteristicIdentifier);
				children = characteristicRecord.has("_children") ? characteristicRecord.getJSONArray("_children") : null;
				if(children != null) {
					for(int j=0; j<children.length(); j++) {
						child = children.getJSONObject(j);
						log("\tVisiting: " + child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
						if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").startsWith("rre_")) {
							notEnough = false;
						}else {
							if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").endsWith("_Rejection")) {
								if(child.has("_children")) {
									losChildren = child.getJSONArray("_children");
									for(int k=0; k<losChildren.length(); k++) {
										losChild = losChildren.getJSONObject(k);
										if(losChild.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").startsWith("rre_")) {
											losNotEnough = false;
										}
									}
								}else {
									rechazos.put(child);
									log("Agregamos un kissi: " + characteristicIdentifier);
									log("El kissi: " + child);
								}
								if(losNotEnough) {
									rechazos.put(child);
									log("2Agregamos un kissi: " + characteristicIdentifier);
									log("2El kissi: " + child);
								}
								losNotEnough = true;
								continue;
							}
						}
					}
				}else {
					if(characteristicIdentifier.endsWith("_Rechazo"))
						rechazos.put(characteristicRecord);
				}
				if(notEnough && characteristicIdentifier.endsWith("_Rechazo")) {
					rechazos.put(characteristicRecord);
				}
				log("R: " + rechazos);
				notEnough = true;
			}
		}
	}

	private static final Logger LOGGER = Logger.getLogger(PrincipiosDeMatching.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/PDM-%g.log", 5 * 1024 * 1024, 10, true);
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
	

	private void log(String message){
		LOGGER.info(message);
//		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/match.log", true)))){
//		  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "]  " + message);
//		}catch(java.io.IOException e){}
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/match.log", true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}
}
