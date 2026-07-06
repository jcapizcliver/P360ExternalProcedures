package mx.com.liverpool.p360.services.core.temp.dataloader;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class LoadEUMarketplace {

	private final RESTWorkshop rw = new RESTWorkshop();
	private final XMLMisc xmm = rw.getXmm();
	private final java.util.ArrayList<String> nonExisting = new java.util.ArrayList<>();
	private final java.util.Map<String, java.util.ArrayList<String>> entitiesMap = new java.util.TreeMap<>();
	private final java.util.Map<String, String> characteristicDataTypes = new java.util.TreeMap<>();
	
	private final java.util.regex.Pattern sdp = java.util.regex.Pattern.compile("[A-Za-z]{3} [A-Za-z]{3} \\d{0,2} \\d{0,2}:\\d{0,2}:\\d{0,2} [A-Z]{3} \\d{4}");
	private final java.util.regex.Pattern sdp2 = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
	
	public static void main(String[] args) {
//		String a = "Fri Aug 12 10:53:15 CDT 2022";
//		String a = "2025-03-22 09:30:41";
//		log( simpleDataTreatment(a, "DATETIME") );
//		System.exit(0);
//		rw.setBaseUrl("https://webctep360dev.liverpool.com.mx/rest/V2.0");
//		try {
//			readProducts(true, true);
//		} catch (SAXException | IOException | ParserConfigurationException e) {
//			logE(e);
//		}
		
	}
	
	public void readProducts(boolean withSendingProductInfo, boolean withSendingLookupData, java.io.InputStream is) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
    	int productsCount = 0;
    	int variantsCount = 0;
		GeneralOperations go = new GeneralOperations();
		java.util.Map<String, org.json.JSONArray> characteristicDetails = go.collectCharacteristic(rw.getBaseUrl());
//    	for(java.io.File f : files) {
//    		log("\n\n\tProcessing: " + f.getName() + "\n");
			doc = builder.parse( is );
			doc.getDocumentElement().normalize();
			java.util.Map<String, java.util.LinkedList<Node>> childElementsMap = null;
			java.util.LinkedList<Node> productNodes = xmm.listImmediateChildElements( 
															xmm.listImmediateChildElements(doc.getDocumentElement()).get("Products").getFirst()
													).get("Product");
			java.util.LinkedList<Node> classificationNodes = null;
			java.util.LinkedList<Node> childProductNodes = null;
			Element e = null;
			org.json.JSONArray characteristicRecords = new org.json.JSONArray();
			org.json.JSONArray characteristicRecordsForVariant = new org.json.JSONArray();
			org.json.JSONArray structureGroupMapArray = null;
			org.json.JSONArray higherLevelProduct = null;
			org.json.JSONObject requestBody = null;
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("includeLabels", "true");
			qp.put("includeIds", "true");
			java.util.Map<String, String> unidades = new java.util.TreeMap<>();
			String elide = null;
			java.util.Map<String, String> characteristicsWithLookup = new java.util.TreeMap<>();
			java.util.Set<String> productCharacteristics = new java.util.TreeSet<>();
			java.util.Set<String> articleCharacteristics = new java.util.TreeSet<>();
			java.util.Map<String, java.util.Map<String, String>> lkpData = new java.util.TreeMap<>();
			java.util.Set<String> nonActiveCharacteristics = new java.util.TreeSet<>();
			java.util.Map<String, String> characteristicDataTypes = new java.util.TreeMap<>();
			String iid = null;
			String[] extraDataResponse = null;
			characteristicDetails.entrySet().forEach(cd -> {
				if(!"".equals(cd.getValue().getString(4))) {
					characteristicsWithLookup.put(cd.getValue().getString(0), cd.getValue().getString(4));
				}
				if(cd.getValue().getJSONArray(3).toString().contains("Product")) {
					productCharacteristics.add(cd.getValue().getString(0));
				}
				if(cd.getValue().getJSONArray(3).toString().contains("Article")){
					articleCharacteristics.add(cd.getValue().getString(0));
				}
				if(!cd.getValue().getBoolean(7)) {
					nonActiveCharacteristics.add(cd.getValue().getString(0));
				}
				characteristicDataTypes.put(cd.getValue().getString(0), cd.getValue().getString(6));
			});
			for(Node productNode : productNodes) {
				e = (Element) productNode;
//				if(!"S41502479".equals(e.getAttribute("ID")))
//					continue;
				productsCount++;
				childElementsMap = xmm.listImmediateChildElements(productNode);
				classificationNodes = childElementsMap.get("ClassificationReference");
				structureGroupMapArray = collectStructureGroupClassifications(classificationNodes, characteristicRecords);
				elide = e.getAttribute("ID");
				log("Working: " + structureGroupMapArray + " for: " + elide);
				higherLevelProduct = new org.json.JSONArray();
				characteristicRecords = new org.json.JSONArray();
				characteristicRecordsForVariant = new org.json.JSONArray();
				childProductNodes = childElementsMap.get("Product");
				extraDataResponse = processProduct( 
						e, 
						null, 
						null, 
						structureGroupMapArray, 
						characteristicRecords, 
						characteristicRecordsForVariant, 
						unidades, 
						lkpData, 
						characteristicsWithLookup, 
						productCharacteristics, 
						articleCharacteristics, 
						characteristicDataTypes, 
						nonActiveCharacteristics, 
						withSendingLookupData 
					);
				requestBody = new org.json.JSONObject();
				requestBody.put("_characteristicRecords", characteristicRecords);
				requestBody.put("structureGroupMap", structureGroupMapArray);
				//Añadir currentStatus
				requestBody.put("currentStatus", new org.json.JSONObject().put("_code", "1020"));
				if(extraDataResponse[1] != null) {
					requestBody.put("lang", new org.json.JSONArray()
							.put(new org.json.JSONObject()
									.put("_qualification", new org.json.JSONObject()
											.put("language", new org.json.JSONObject()
													.put("_code", "es")))
									.put("descriptionLong", extraDataResponse[1])));
				}
				if(e.hasAttribute("ParentID") && !"".equals(e.getAttribute("ParentID"))) {
					structureGroupMapArray
					.put(new org.json.JSONObject()
							.put( "_qualification", 
									new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + e.getAttribute("ParentID") + "'@'PrimaryProductTaxonomy'"))));
				}else {
					if(!"".equals(extraDataResponse[2]) && extraDataResponse[2] != null)
						structureGroupMapArray.put(new org.json.JSONObject().put( "_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + extraDataResponse[2] + "'@'PrimaryProductTaxonomy'"))));
					if(!extraDataResponse[2].startsWith("EU4")) {
						log("AAAAAAAAAAAAAAAAAAAAAAAAAAAA " + extraDataResponse[3]);
						continue;
					}
						
				}
				elide =  (elide == null || "".equals(elide) ? extraDataResponse[3] : elide);
				if(withSendingProductInfo) {
					log("For Product (" + elide + "): " + requestBody);
					rw.makeRequest("PUT", "/object/Product2G/'" + elide + "'@1", qp, requestBody.toString());
					log(rw.getRawResponse());
					if("00".equals(extraDataResponse[0]) && characteristicRecordsForVariant.length() > 0) {
						addReferencedProduct( elide, higherLevelProduct);
						requestBody = new org.json.JSONObject();
						requestBody.put("_characteristicRecords", characteristicRecordsForVariant);
						requestBody.put("structureGroupMap", structureGroupMapArray);
						requestBody.put("higherLevelProduct", higherLevelProduct);
						// Current Status = 1020 (Creación de SKU) para variantes
						requestBody.put("currentStatus", new org.json.JSONObject().put("_code", "1020"));
						iid = e.getAttribute("ID");
						rw.makeRequest("PUT", "/object/Article/'" + iid + "'@'MASTER'", qp, requestBody.toString());
						log("For Article (" + iid + "): " + requestBody);
						log("Got: " + rw.getRawResponse());
					}else {
//						log("NOP. " + extraDataResponse[0] + " - " + characteristicRecordsForVariant);
					}
				}
				if(childProductNodes != null) {
					for(Node childProductNode : childProductNodes) {
						variantsCount++;
						higherLevelProduct = new org.json.JSONArray();
						characteristicRecords = new org.json.JSONArray();
						characteristicRecordsForVariant = new org.json.JSONArray();
						addReferencedProduct( elide, higherLevelProduct);
						processProduct( (Element) childProductNode, elide, higherLevelProduct, structureGroupMapArray, characteristicRecords, characteristicRecordsForVariant, unidades, lkpData, characteristicsWithLookup, productCharacteristics, articleCharacteristics, characteristicDataTypes, nonActiveCharacteristics, withSendingLookupData );
						requestBody = new org.json.JSONObject();
						requestBody.put("_characteristicRecords", characteristicRecordsForVariant);
						requestBody.put("higherLevelProduct", higherLevelProduct);
						requestBody.put("structureGroupMap", structureGroupMapArray);
						if(withSendingProductInfo) {
							rw.makeRequest("PUT", "/object/Article/'" + ((Element)childProductNode).getAttribute("ID") + "'@'MASTER'", qp, requestBody.toString());
							log("For Article (" + ((Element) childProductNode).getAttribute("ID") + "): " + requestBody + "\n" + rw.getRawResponse());
						}
					}
				}
			}
//			log("Done. " + f.getName());
//    	}
    	log("Características que no tengo:");
    	nonExisting.forEach(System.out::println);
    	log("Products: " + productsCount);
    	log("Variants: " + variantsCount);
	}
	
	private String simpleDataTreatment(String data, String dataType, String field) {
		if("DATETIME".equals(dataType)) {
			java.util.regex.Matcher m = sdp.matcher(data);
			java.util.regex.Matcher m2 = sdp2.matcher(data);
			if(m.find()) {
				java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss VV yyyy");
				java.time.ZonedDateTime cdt = java.time.ZonedDateTime.parse(data.replace("CDT", "America/Mexico_City").replaceAll("CST", "America/Mexico_City"), dtf);
				return cdt.format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") );
			}else if(m2.find()) {
				java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				java.time.LocalDateTime cdt = java.time.LocalDateTime.parse(data, dtf);
				return cdt.format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") );
			}
		}
		if(data.length() > 2000) {
			log("\tSurvival horror (" + field + ")");
			return null;
		}
		return data;
	}
	
	private String[] processProduct(
			Element productElement,
			String parent,
			org.json.JSONArray higherLevelProduct,
			org.json.JSONArray structureGroupMapArray,
			org.json.JSONArray product2GCharacteristicRecords,
			org.json.JSONArray articleCharacteristicRecords,
			java.util.Map<String, String> unidades,
			java.util.Map<String, java.util.Map<String, String>> lookupData,
			java.util.Map<String, String> characteristicsWithLookup,
			java.util.Set<String> product2GCharacteristic,
			java.util.Set<String> articleCharacteristic,
			java.util.Map<String, String> dataTypes,
			java.util.Set<String> nonActiveCharacteristics,
			boolean withSendingLookupData
			) {
		String[] result = null;
		java.util.LinkedList<Node> valueNodes = xmm.listImmediateChildElements( xmm.listImmediateChildElements(productElement).get("Values").getFirst() ).get("Value");
		if(parent != null)
			addReferencedProduct(parent, higherLevelProduct);
		result = processValueNodes(valueNodes, product2GCharacteristicRecords, articleCharacteristicRecords, lookupData, characteristicsWithLookup, product2GCharacteristic , articleCharacteristic, dataTypes, nonActiveCharacteristics);
		addUnidadesDeMedida(unidades, product2GCharacteristicRecords);
		if(withSendingLookupData)
			sendLookupValues(lookupData);
		return result;
	}
	
	private String[] processValueNodes(
			java.util.LinkedList<Node> valueNodes, 
			org.json.JSONArray characteristicRecords, 
			org.json.JSONArray characteristicRecordsForVariant, 
			java.util.Map<String, java.util.Map<String, String>> lookupDataToSend,
			java.util.Map<String, String> characteristicsWithLookup,
			java.util.Set<String> characteristicsForProducts,
			java.util.Set<String> characteristicsForArticles,
			java.util.Map<String, String> dataTypes,
			java.util.Set<String> nonActiveCharacteristics
	) {
		java.util.Map<String, String> content = null;
		String attributeId = null;
		String lookup = null;
		String dataType = null;
		Element e = null;
		String sapObjectType = null;
		String descLong = null;
		String parentId = null;
		String miraklProductId = null;
		String code = null;
		for(Node valueNode : valueNodes) {
			e = (Element) valueNode;
			attributeId = e.getAttribute("AttributeID");
//			log("Found: " + attributeId);
			if("SAPObjectType".equals(attributeId)) {
				sapObjectType = e.getAttribute("ID");
//				log("\n\n\t" + attributeId + " (" + e.getAttribute("ID") + "): " + e.getTextContent() + "\n");
			}
			if(!nonActiveCharacteristics.contains(attributeId))
			if(e.hasAttribute("UnitID")) {
				
			}else {
				lookup = characteristicsWithLookup.get(attributeId);
				if(lookup != null) {
					content = lookupDataToSend.get(lookup);
					if(content == null) {
						content = new java.util.TreeMap<>();
						lookupDataToSend.put(lookup, content);
					}
					code = e.hasAttribute("ID") ? e.getAttribute("ID") : e.getTextContent();
					if(code != null && !"".equals(code)) {
						content.put(code, e.getTextContent());
						
						if(characteristicsForProducts.contains(attributeId)) {
							characteristicRecords.put( createCharacteristicValueObject(attributeId, new org.json.JSONObject().put("_code", code)) );
						}
						if(characteristicsForArticles.contains(attributeId)) {
							characteristicRecordsForVariant.put( createCharacteristicValueObject(attributeId, new org.json.JSONObject().put("_code", code)) );
						}
					}
				}else {
					
					dataType = dataTypes.get(attributeId);
					if("DescriptionLong".equals(attributeId)) {
						descLong = e.getTextContent();
					}else if("ParentID".equals(attributeId)){
						parentId = e.getTextContent();
					}
					else if("mirakl-product-id".equals(attributeId)){
						miraklProductId = e.getTextContent();
					}
					else {
						code = simpleDataTreatment(e.getTextContent(), dataType, attributeId);
						if(!"".equals(code)) {
						if(characteristicsForProducts.contains(attributeId)) {
							characteristicRecords.put( createCharacteristicValueObject(attributeId, code ) );
						}
						if(characteristicsForArticles.contains(attributeId)) {
							characteristicRecordsForVariant.put( createCharacteristicValueObject(attributeId, code ) );
						}
						}
					}
						
				}
			}
		}
		return new String[] { sapObjectType, descLong, parentId , miraklProductId};
	}
	
	private void addReferencedProduct(String productId, org.json.JSONArray higherLevelProduct) {
		higherLevelProduct.put(
				new org.json.JSONObject()
					.put("_qualification", new org.json.JSONObject().put("referencedIdentifier", productId))
			);
	}
	
	private void addUnidadesDeMedida(java.util.Map<String, String> unidades, org.json.JSONArray characteristicRecords) {
		java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
		unidadesVolumen.put("unece.unit.FTQ", "PI3");
		unidadesVolumen.put("unece.unit.MTQ", "M3");
		unidadesVolumen.put("unece.unit.GRM", "G");
		String unidadDeMedidaLongitud = null;
		String unidadDeMedidaVolumen = null;
		String unidadDeMedidaPeso = null;
		String[] atributosLongitud = new String[] { "ProductWidth", "ProductDepth", "ProductHeight", "ZBRECJ", "ZLAECJ", "ZHOECJ", "ZHOEPQ", "ZBREPQ", "ZLAEPQ" };
		String[] atributosVolumen = new String[] { "VOLUMAtt", "ZVOLCJ", "ZVOLPQ" };
		String[] atributosPeso = new String[] { "PesoBruto", "ProductWeight", "ZBRGCJ", "ZNTGCJ", "ZBRGPQ", "ZNTGPQ" };
		String unidadId = null;
		for(String a : atributosLongitud) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaLongitud = unidadesLongitud.get(unidadId);
				break;
			}
		}
		for(String a : atributosVolumen) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaVolumen = unidadesVolumen.get( unidadId );
				break;
			}
		}
		for(String a : atributosPeso) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaPeso = unidadesPeso.get( unidadId );
				break;
			}
		}
		if(unidadDeMedidaLongitud == null) {
//			log("No se obtuvo una unidad de medida de longitud");
		} else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaLongitud", new org.json.JSONObject().put("_code", unidadDeMedidaLongitud) ) );
		}
		if(unidadDeMedidaPeso == null) {
//			log("No se obtuvo unidad de medida de peso");
		}else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaPeso", new org.json.JSONObject().put("_code", unidadDeMedidaPeso) ) );
		}
		if(unidadDeMedidaVolumen == null) {
//			log("No se obtuvo unidad de medida de volumen");
		}else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaVolumen", new org.json.JSONObject().put("_code", unidadDeMedidaVolumen) ) );
		}
		
	}

	private void sendLookupValues(java.util.Map<String, java.util.Map<String, String>> lkps) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		request.put("columns", columns);
		request.put("rows", rows);
		for(java.util.Map.Entry<String, java.util.Map<String, String>> entry : lkps.entrySet()) {
			for(java.util.Map.Entry<String, String> subEntry : entry.getValue().entrySet()) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + subEntry.getKey() + "'@'" + entry.getKey() + "'")).put("values", new org.json.JSONArray().put(subEntry.getValue()).put(true)));
//				System.exit(0);
			}
		}
		rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
//		log(rw.getRawResponse());
		while(rows.length() > 0) {
			rows.remove(0);
		}
		lkps.clear();
	}
	
	private void collectCharacteristicValuesForIndividual(
			java.util.LinkedList<Node> valueNodes, 
			org.json.JSONArray characteristicRecords, 
			org.json.JSONArray characteristicRecordsForVariant, 
			java.util.Map<String, String> unidades
	){
		String attributeId = null;
		String valueId = null;
		String value = null;
		Object data = null;
		Element e = null;
		boolean isLookup = false;
		java.util.ArrayList<String> entities = null;
		int cnt = 0;
		int negocioTimes = 0;
		for(Node valueNode : valueNodes) {
			e = (Element) valueNode;
			attributeId = e.getAttribute("AttributeID");
			if(!nonExisting.contains(attributeId) && !checkCharacteristicExistance(attributeId)) {
				nonExisting.add(attributeId);
			}else {
				if(e.hasAttribute("UnitID")) {
					unidades.put(attributeId, e.getAttribute("UnitID"));
				}
				valueId = e.hasAttribute("ID") ? e.getAttribute("ID") : "TamanoUnico".equals(attributeId) ? e.getTextContent() : null;
				value = e.getTextContent();
				if("EXTWG_S4H".equals(attributeId) || "Negocio".equals(attributeId)) {
					negocioTimes++;
					if(negocioTimes > 1) {
						log("\n\t" + "!!!!");
					}
					capturaNegocio(value, attributeId, characteristicRecords);
				}
				isLookup = checkCharacteristicLookupValue(valueId, value, attributeId);
				entities = entitiesMap.get(attributeId);
				if(entities == null) {
					entities = collectEntities(attributeId);
				}
				if(entities != null) {
					if(valueId != null && !"".equals(valueId)) {
						data = isLookup ? new org.json.JSONObject().put(valueId != null && !"".equals(valueId) ? "_code" : "_label", valueId != null && !"".equals(valueId) ? valueId : formatPlainValue( attributeId, value )) : formatPlainValue(attributeId, value );
						if(data != null) {
							if(entities.contains("Article")) {
								characteristicRecordsForVariant.put( createCharacteristicValueObject(attributeId, data ) );
							}
							if(entities.contains("Product2G")) {
								characteristicRecords.put( createCharacteristicValueObject(attributeId, data ) );
							}
						}
					}
				}
			}
			cnt++;
			log("\t" + (cnt) + "/" + valueNodes.size());
		}
	}
	
	private void capturaNegocio(String valorNegocio, String etiqueta, org.json.JSONArray characteristicRecords) {
		if("EXTWG_S4H".equals(etiqueta)) {
			characteristicRecords.put( createCharacteristicValueObject("Business", new org.json.JSONObject().put("_code", "SBB" )) );
		}else {
			if("MARKETPLACE".equals(valorNegocio)) {
				characteristicRecords.put( createCharacteristicValueObject("Business", new org.json.JSONObject().put("_code", "MKP" )) );
			}else {
				characteristicRecords.put( createCharacteristicValueObject("Business", new org.json.JSONObject().put("_code", "LVP" )) );
			}
		}
	}
	
	private Object formatPlainValue(String characteristic, String value) {
		String dataType = characteristicDataTypes.get(characteristic);
		if("DATETIME".equals(dataType)) {
			try {
				return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format( new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy").parse(value) );
			}catch(java.text.ParseException e) {
				try {
					return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format( new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value) );
				}catch(java.text.ParseException ex) {
					
				}
			}
		} else if("DECIMAL".equals(dataType)) {
			try{
				return new java.math.BigDecimal(value);
			}catch(NumberFormatException e) {
				log("Problem parsing a decimal data, the data: " + value);
				return null;
			}
		} else if("INTEGER".equals(dataType)) {
			try{
				return new java.math.BigDecimal(value).intValue();
			}catch(NumberFormatException e) {
				log("Problem interpreting a number data, the data: " +  value);
				return null;
			}
		}
		return value;
	}
	
	private void collectCharacteristicValues(java.util.LinkedList<Node> valueNodes, org.json.JSONArray characteristicRecords, String entity){
		String attributeId = null;
		String valueId = null;
		String value = null;
		Element e = null;
		java.util.ArrayList<String> entities = null;
		boolean isLookup = false;
		for(Node valueNode : valueNodes) {
			e = (Element) valueNode;
			attributeId = e.getAttribute("AttributeID");
			if(!nonExisting.contains(attributeId) && !checkCharacteristicExistance(attributeId)) {
				nonExisting.add(attributeId);
			}else {
				valueId = e.hasAttribute("ID") ? e.getAttribute("ID") : "TamanoUnico".equals(attributeId) ? e.getTextContent() : null;
				value = e.getTextContent();
				if(valueId != null && !"".equals(valueId)) {
					isLookup = checkCharacteristicLookupValue(valueId, value, attributeId);
				}else {
					isLookup = false;
				}
//				log("### " + attributeId + " - " + isLookup);
				entities = entitiesMap.get(attributeId);
				if(entities == null) {
					entities = collectEntities(attributeId);
				}
				if(!entities.contains(entity)) {
					characteristicRecords.put( createCharacteristicValueObject(attributeId, isLookup && valueId != null && !"".equals(valueId) ? new org.json.JSONObject().put("_code", valueId) : value ) );
				}
			}
		}
	}
	
	private boolean checkCharacteristicLookupValue(String code, String value, String characteristicId) {
		boolean isAlsoLookup = false;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Lookup->Lookup.Identifier,Characteristic.DataType");
		qp.put("query", "Characteristic.Identifier equals \"" + characteristicId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null) {
			String lookup = null;
			if(response.getJSONArray("rows").length() > 0) {
				characteristicDataTypes.put(characteristicId, response .getJSONArray("rows")  .getJSONObject(0).getJSONArray("values").getString(1));
				lookup = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
				if(lookup == null || "".equals(lookup)) {
//					log("La característica no tiene Lookup en P360: " + characteristicId);
				}else {
					isAlsoLookup = true;
					if(!checkValueInLookup(code, lookup)) {
						if(!checkValueInLookupByNameSpanish(value, lookup)) {
							insertLookupValue(code == null || "".equals(code) || "null".equals(code) ? value : code, value, lookup);
						}
					}
				}
			}
		}else{
			log("ERR: " + rw.getRawResponse());
		}
//		log("Característica: " + characteristicId + " - " + isAlsoLookup);
		return isAlsoLookup;
	}
	
	private java.util.ArrayList<String> collectEntities(String characteristicId) {
		java.util.ArrayList<String> entities = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Entities");
		qp.put("query", "Characteristic.Identifier equals \"" + characteristicId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null) {
			if(response.getJSONArray("rows").length() > 0) {
				org.json.JSONArray entitiesArray = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(0);
				entities = new java.util.ArrayList<>();
				for(int i=0; i<entitiesArray.length(); i++) {
					entities.add(entitiesArray.getString(i));
				}
				entitiesMap.put(characteristicId, entities);
			}else {
				log("Not found characteristic: " + characteristicId);
			}
		}else{
			log("ERR: " + rw.getRawResponse());
		}
		return entities;
	}
	
	private boolean checkCharacteristicExistance(String characteristicId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.Identifier equals \"" + characteristicId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		return response != null && response.getJSONArray("rows").length() > 0;
	}
	
	private org.json.JSONArray collectStructureGroupClassifications(java.util.LinkedList<Node> classificationNodes, org.json.JSONArray characteristicRecords){
		org.json.JSONArray characteristicMapArray = new org.json.JSONArray();
		org.json.JSONObject mapEntry = null;
		String classificationId = null;
		String classificationNodeType = null;
		String supplierId = null;
		if(classificationNodes != null) {
			for(Node classificationNode : classificationNodes) {
				classificationNodeType = ((Element)classificationNode).getAttribute("Type");
				classificationId = ((Element)classificationNode).getAttribute("ClassificationID");
				if("WebsiteLink".equals(classificationNodeType)) {
					mapEntry = new org.json.JSONObject();
					mapEntry.put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + classificationId + "'@'Sitios Web'")));
					characteristicMapArray.put(mapEntry);
				}else if("GALink".equals(classificationNodeType)) {
					mapEntry = new org.json.JSONObject();
					mapEntry.put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + classificationId + "'@'" + ( classificationId.endsWith("ECC") ? "CommercialECC" : "CommercialS4H" ) + "'")));
//					characteristicMapArray.put(mapEntry);
				}else if("SupplierLink".equals(classificationNodeType)) {
					supplierId = classificationId.replaceAll("-SupplierProducts", "");
					handleProveedor(supplierId);
					characteristicRecords.put( createCharacteristicValueObject("SupplierID", supplierId) );
				}
			}
		}
		return characteristicMapArray;
	}
	
	private void handleProveedor(String proveedor) {
		boolean existe = checkValueInLookup(proveedor, "Party");
		if(!existe) {
			insertLookupValue(proveedor, "Party");
		}
	}
	
	private void insertLookupValue(String code, String lookup) {
		insertLookupValue(code, null, lookup);
	}
	
	private void insertLookupValue(String code, String value, String lookup) {
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(\"STEP-PROD\")"));
		if(value != null && !"".equals(value)) {
			columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		}
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + code + "'@'" + lookup + "'")).put("values", new org.json.JSONArray().put(true).put(code)));
		if(value != null && !"".equals(value)) {
			rows.getJSONObject(0).getJSONArray("values").put(value);
		}
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONObject response = rw.makeRequest("POST", "/list/LookupValue", empty, request.toString());
		log("LookupValue wrote (" + lookup + "): " + (response == null ? rw.getRawResponse() : response));
	}

	private boolean checkValueInLookup(String value, String lookup) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'" + lookup + "'");
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValue.Code equals \"" + value + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		return response != null && response.getJSONArray("rows").length() > 0;
	}

	private boolean checkValueInLookupByNameSpanish(String value, String lookup) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'" + lookup + "'");
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueLang.Name(es) equals \"" + value + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		return response != null && response.getJSONArray("rows").length() > 0;
	}

	private org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}
	
	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/loadMktProduct.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/loadMktProduct.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
}
