package mx.com.liverpool.p360.services.core.amqp.run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.AgarraloONo;
import mx.com.liverpool.p360.services.core.ConfigurableProduct;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.amqp.CrearArchivosParaSKU;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ProductArticleCharacteristicValueChangeProcessor {

	private boolean running = true;
	
	private final RESTWrapper rw;
	private final RESTWorkshop workshop;
	private final XMLMisc xmm;

	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;
	
	private final org.json.JSONArray rowsSinMarca = new org.json.JSONArray();
	private final org.json.JSONObject requestSinMarca = new org.json.JSONObject()
			.put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TituloSinMarca',root,\"0000.0000.RK\",'TituloSinMarca',-1)")))
			.put("rows", rowsSinMarca)
			;
	
	private final org.json.JSONArray rowsPNP = new org.json.JSONArray();
	private final org.json.JSONObject requestPNP = new org.json.JSONObject()
			.put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.ProcedeNoProcede")))
			.put("rows", rowsPNP)
			;
	
	private final java.util.Map<String, String> qp0;
	
	public ProductArticleCharacteristicValueChangeProcessor() {
		rw = new RESTWrapper();
		workshop = rw.getRw();
		xmm = rw.getXmm();
		qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
	}

	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException, ServiceUnavailableException {

//		java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap = new java.util.TreeMap<>();

		String entity = null;
		String externalId = null;
		String changeSummary = null;

//		String template = null;
//		String ean = null;
//		String ean2 = null;
//		String sku = null;
//		String supplierId = null;
//		String internalStatus = null;
//		String nes = null;
//		String currentStatus = null;
//		org.json.JSONArray rechazos = new org.json.JSONArray();
		org.json.JSONArray changedField = null;

		java.util.Set<String> changedFieldSet = new java.util.TreeSet<>();
		org.json.JSONObject json = new org.json.JSONObject(message);

    	String identifier = null;
    	
//    	String sapObjectType = null;
//    	String fotoTomadaLiverpool = null;
//    	String business = null;
//    	String productName = null;
//    	String section = null;
//    	String assignTakeNoTake = null;
//    	String itemGroup = null;
//    	String itemGroupS4H = null;
//    	String brandName = null;
//    	String brandIdS4H = null;
//    	String brandNameLabel = null;
//    	String brandIdS4HLabel = null;
    	java.util.Map<String, String> datas = new java.util.HashMap<>();
		if(json.has("entityItemChange")) {
	     	entity = json.getJSONObject("entityItemChange").getString("_entity");
	     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
	     	changedField = json.getJSONObject("entityItemChange").has("_changedField") ? json.getJSONObject("entityItemChange").getJSONArray("_changedField") : new org.json.JSONArray();
//			log("A message body: " + json);
			for(int i=0; i<changedField.length(); i++) {
				changedFieldSet.add(changedField.getString(i));
			}

//			log("changeFields: " + changedField);

	     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	Document doc;
	    	
	    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
	    		doc = builder.parse( baos );
	    		doc.getDocumentElement().normalize();
	    	}
	    	
			if(doc != null) {
				Element rootElement = doc.getDocumentElement();
				externalId = json.getJSONObject("entityItemChange").getString("_identifier");

				if("Product2G".equals(entity) || "Article".equals(entity)) {
					
//					if(changedFieldSet.contains("Product2G.CurrentStatus")) {
//					}
					
//					qp.put("entityFilter", entity +"," + entity + "Log," + entity + "CharacteristicValue," + entity + "StructureGroupMap,ProductReference");
//					qp.put("includeIds", "true");
//					qp.put("includeLabels", "true");
//					qp.put("Accept-Language", "es");
//					log("requesting: " + workshop.getBaseUrl() + "/object/" + entity + "/" + internalId);
//					response = workshop.makeRequest("GET", "/object/" + entity + "/" + internalId, qp, null);
//					if(response != null) {
//						template = response.getJSONObject("_data").has("structureGroupMap") ? getPrimaryProductTaxonomyTemplate(response.getJSONObject("_data").getJSONArray("structureGroupMap")) : "";
//						data = response.getJSONObject("_data");
						identifier = externalId;// data.getString("identifier");
//						characteristicRecords = data.has("_characteristicRecords") ? data.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
//						characteristicsToMap(characteristicRecords, characteristicRecordsMap, rechazos);
//						ean = getSimpleValue("MainBarCode", characteristicRecordsMap);
//						ean2 = getSimpleValue("MainBarCodeS4H", characteristicRecordsMap);
//						sku = getSimpleValue("SKU", characteristicRecordsMap);
//						supplierId = getSimpleValue("SupplierID", characteristicRecordsMap);
//						business = getValue("Business", characteristicRecordsMap);
//						sapObjectType = getValue("SAPObjectType", characteristicRecordsMap);
//						fotoTomadaLiverpool = getValue("FotoTomadaLiverpool", characteristicRecordsMap);
//						productName = getSimpleValue("ProductName", characteristicRecordsMap);
//				    	itemGroup = getValue("ItemGroup", characteristicRecordsMap);
//				    	section = getValue("Section", characteristicRecordsMap);
//				    	itemGroupS4H = getValue("ItemGroupS4H", characteristicRecordsMap);
//				    	assignTakeNoTake = getSimpleValue("AssignTakeNoTake", characteristicRecordsMap);
//				    	brandName = getValue("BrandName", characteristicRecordsMap);
//				    	brandIdS4H = getValue("BRAND_ID_S4H", characteristicRecordsMap);
//				    	brandNameLabel = getValueLabel("BrandName", characteristicRecordsMap);
//				    	brandIdS4HLabel = getValueLabel("BRAND_ID_S4H", characteristicRecordsMap);
//						currentStatus  = !data.has("currentStatus")  ? "" : String.valueOf( data.getJSONObject("currentStatus" ).getInt("_key") );
//						internalStatus = !data.has("currentStatus")  ? "" : data.getJSONObject("currentStatus" ).getString("_label");
						DataRequestor dr = new DataRequestor();
						if(changedFieldSet.contains("Product2GCharacteristicValueLang.Value")){
							java.util.LinkedList<Node> crs = xmm.listImmediateChildElements( xmm.byName(rootElement, "product")).get("_characteristicRecords");
							String charId = null;
							boolean toSend = false;
							org.json.JSONArray characteristicRecordsForUpdate = new org.json.JSONArray();
							Node currentValueNode = null;
							Node oldValueNode = null;
							String r = null;
							r = dr.getProductData( new org.json.JSONArray().put(identifier) );
							org.json.JSONObject jr = null;
							org.json.JSONObject jp = null;
							if(r != null) {
								try {
									jr = new org.json.JSONObject(r);
									jp = jr.getJSONArray("items").getJSONObject(0);
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
							String business = jp != null && jp.has("Business") ? jp.getString("Business") : "";
							log(identifier + " - " + business + " (" + jp + ")");
							for(Node cr : crs) {
								charId = xmm.byName( xmm.byName( xmm.byName( cr, "_qualification"), "characteristic"), "_code") .getTextContent();
								String valueChange = "";
								String prevValue = "";
								try {
									currentValueNode = xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_current");
									Node _externalId = xmm.byName( currentValueNode, "_externalId");
									if(_externalId != null) {
										valueChange = _externalId.getTextContent().replaceAll("^'|('@'.+)", "").replaceAll("\\\\'", "'");
									}else {
										valueChange = currentValueNode .getTextContent();
									}
								}catch(NullPointerException e) {
									
								}
								try {
									oldValueNode = xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_old");
									Node _externalId = xmm.byName( oldValueNode, "_externalId");
									if(_externalId != null) {
										prevValue = _externalId.getTextContent().replaceAll("^'|('@'.+)", "").replaceAll("\\\\'", "'");
									}else {
										prevValue = currentValueNode .getTextContent();
									}
//									Node _code = xmm.byName( oldValueNode, "_code");
//									prevValue = (_code != null ? _code : oldValueNode) .getTextContent();
								}catch(NullPointerException e) {
									
								}
								log(charId + ", PrevValue: " + prevValue + ", currentValue: " + valueChange + ". " + ("ProcessingRepublish".equals(charId)) + " _ " + (Boolean.parseBoolean(valueChange)));
								
								String sendResponse = null;
								if("ProcessingRepublish".equals(charId) && Boolean.parseBoolean(valueChange)) {
									appendAtgPendingId(externalId);

									addCharacteristicValue(characteristicRecordsForUpdate, "ProcessingRepublish", false, false, false);
									addCharacteristicValue(characteristicRecordsForUpdate, "PublishMessage", "ATG diferido: Product2G registrado en PACVC_ATG_PENDING_IDS.txt para publicación standalone.", false, false);

									log("ATG deferred. Product2G logged for standalone publish: " + externalId);
								}else if("ProcessingPublishToMkt".equals(charId) && Boolean.parseBoolean(valueChange) && !"SBB".equals(business)) {
									appendMktPendingId(externalId);

									addCharacteristicValue(characteristicRecordsForUpdate, "ProcessingPublishToMkt", false, false, false);
									addCharacteristicValue(characteristicRecordsForUpdate, "PublishMktMessage", "MKT diferido: Product2G registrado en PACVC_MKT_PENDING_IDS.txt para publicación standalone.", false, false);

									log("MKT deferred. Product2G logged for standalone publish: " + externalId);
								}else if("ProductoConfigurable".equals(charId) && "".equals(prevValue) && !"".equals(valueChange)) {
									new ConfigurableProduct().processInput(workshop.getBaseUrl(), externalId, valueChange);
									log("Got launched for producto configurable pv -->" + prevValue + "<--, cv -->" + valueChange + "<--... " + externalId);
								}else if("ProcessingTomarNoTomar".equals(charId) && Boolean.parseBoolean(valueChange)) {
									log("Got launched for Tomar No Tomar...");
									new AgarraloONo().checale(externalId, workshop.getBaseUrl());
									addCharacteristicValue(characteristicRecordsForUpdate, "ProcessingTomarNoTomar", false, false, false);
								}else if("ProductName".equals(charId)) {
									String brandName = jp.getString("BrandName");
									String brandNameS4H = jp.getString("BRAND_ID_S4H");
									String bnl = brandName != null && !"".equals(brandName) ? getBrandLabel(brandName, "ZCOMALOV") : "";
									String bnls4h = brandNameS4H != null && !"".equals(brandNameS4H) ? getBrandLabel(brandNameS4H, "BRAND_IDLOV_S4H") : "";
									log("Change in ProductName. Got label for BrandName: " + bnl + ", BRAND_ID_S4H: " + bnls4h);
									String sinMarca = 
											valueChange == null || (bnls4h == null && bnl == null) ? 
													valueChange == null ? "" : valueChange : 
												valueChange.replaceFirst("(?i)(?<![\\p{L}])" + 
									java.util.regex.Pattern.quote(bnl == null || bnl.isEmpty() ? bnls4h : bnl)
												+ "(?![\\p{L}])", "")
												.replaceAll(" {2,}", " ").trim();
									log("Got Sin Marca: " + sinMarca);
									addCharacteristicValue(characteristicRecordsForUpdate, "TituloSinMarca", sinMarca, false, false);
								}else if("ResendToSKUCreation".equals(charId)) {
									if(Boolean.parseBoolean( valueChange )) {
										CrearArchivosParaSKU crear = new CrearArchivosParaSKU();
										String[] content = crear.creacionDeArchivos(externalId, (short) 0);
										crear.handleContent(externalId, content);
										addCharacteristicValue(characteristicRecordsForUpdate, "ResendToSKUCreation", "", false, false);
									}
								}
								if(
									   "Section".equals(charId)
									|| "ItemGroup".equals(charId)
									|| "ItemGroupS4H".equals(charId)
									|| "BrandName".equals(charId)
									|| "BRAND_ID_S4H".equals(charId)
									|| "Business".equals(charId)
									|| "SupplierID".equals(charId)
									|| "SKU".equals(charId)
									|| "AssignTakeNoTake".equals(charId)
									|| "SAPObjectType".equals(charId)
									|| "FotoTomadaLiverpool".equals(charId)
									|| "MainBarCode".equals(charId)
									|| "MainBarCodeS4".equals(charId)
								) {
									toSend = true;
									datas.put(charId, valueChange);
								} 
							}
							if(characteristicRecordsForUpdate.length() > 0) {
								sendUpdateObjectAPI(externalId, new org.json.JSONObject().put("_characteristicRecords", characteristicRecordsForUpdate));
							}
							if(toSend) {
								if(jp != null) {
									for(java.util.Map.Entry<String, String> entry : datas.entrySet()) {
										jp.put(entry.getKey(), entry.getValue());
									}
									org.json.JSONArray items = new org.json.JSONArray().put(
											jp
//											new org.json.JSONObject()
//											.put("product", externalId)
//											.put("Section", section == null ? "" : section)
//											.put("ItemGroup", itemGroup == null ? "" : itemGroup)
//											.put("ItemGroupS4H", itemGroupS4H == null ? "" : itemGroupS4H)
//											.put("BrandName", brandName == null ? "" : brandName)
//											.put("BRAND_ID_S4H", brandIdS4H == null ? "" : brandIdS4H)
//											.put("Business", business == null ? "" : business)
//											.put("SupplierID", supplierId == null ? "" : supplierId)
//											.put("SKU", sku == null ? "" : sku)
//											.put("AssignTakeNoTake", assignTakeNoTake == null ? "" : assignTakeNoTake)
//											.put("Template", template == null ? "" : template)
//											.put("CurrentStatus", currentStatus == null ? "" : currentStatus)
//											.put("SAPObjectType", sapObjectType == null ? "" : sapObjectType)
//											.put("FotoTomadaLiverpool", fotoTomadaLiverpool == null ? "" : fotoTomadaLiverpool)
//											.put("MainBarCode", ean == null ? "" : ean)
//											.put("MainBarCodeS4H", ean2 == null ? "" : ean2)
									);
									log("Sending data to admin: " + items.getJSONObject(0));
									dr.putProductData( items );
								}
							}
						}
						if(changedFieldSet.contains("ArticleCharacteristicValueLang.Value")){
							java.util.LinkedList<Node> crs = xmm.listImmediateChildElements( xmm.byName(rootElement, "article")).get("_characteristicRecords");
							String charId = null;
							boolean toSend = false;
							boolean toSkuUpdate = false;
							Node currentValueNode = null;
							Node oldValueNode = null;
							String r = null;
							r = dr.getArticleData( new org.json.JSONArray().put(identifier) );
							org.json.JSONObject jr = null;
							org.json.JSONObject jp = null;
							if(r != null) {
								try {
									jr = new org.json.JSONObject(r);
									jp = jr.getJSONArray("items").getJSONObject(0);
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
							for(Node cr : crs) {
								charId = xmm.byName( xmm.byName( xmm.byName( cr, "_qualification"), "characteristic"), "_code") .getTextContent();
								if("TamanoUnico".equals(charId)) {
									String oldTamanoUnico = null;
									String currentTamanoUnico = null;
									try{
										oldTamanoUnico = xmm.byName( xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_old"), "_label") .getTextContent();
									}catch(NullPointerException e) {
										oldTamanoUnico = "";
									}
									log("Old tamaño único: " + oldTamanoUnico);
									try{
										currentTamanoUnico = xmm.byName( xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_current"), "_label") .getTextContent();
										log("Tamaño Único: " + currentTamanoUnico);
//										String brand = getBrand(identifier);
										String brand = null;
										try {
											String r0 = dr.getProductData(new org.json.JSONArray().put(jp.getString("ProductNo")));
											org.json.JSONObject jp0 = null;
											if(r0 != null) {
												org.json.JSONObject jr0 = new org.json.JSONObject(r0);
												jp0 = jr0.getJSONArray("items").getJSONObject(0);
											}
											brand = jp0.getString("BrandName");
											if("".equals(brand)) {
												brand = jp0.getString("BRAND_IDLOV_S4H");
											}
										}catch(NullPointerException | org.json.JSONException e) {
											logE(e);
										}
										log("Brand: " + brand);
										if(brand != null && !"".equals(brand)) {
											String nes = queryDictionary(brand + "<::>" + currentTamanoUnico, "TallaNormalizada");
											log("Equivalence: " + nes);
											currentTamanoUnico = nes != null && !"".equals(nes) ? nes : currentTamanoUnico;
											setTallaNormalizada(currentTamanoUnico, identifier);
										}
									}catch(NullPointerException e) {
										currentTamanoUnico = "";
									}
								}else if("SAPObjectType".equals(charId)) {
									String oldSAPObjectType = null;
									String currentSAPObjectType = null;
									try{
										oldSAPObjectType = xmm.byName( xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_old"), "_label") .getTextContent();
									}catch(NullPointerException e) {
										oldSAPObjectType = "";
									}
									log("Old SAPObjectType: " + oldSAPObjectType);
									try{
										currentSAPObjectType = xmm.byName( xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_current"), "_label") .getTextContent();
										log("SAPObjectType: " + currentSAPObjectType);
										if("Conjunto Look".equals(currentSAPObjectType)) {

										}
									}catch(NullPointerException e) {
										currentSAPObjectType = "";
									}
								}else if("MensajeCreacionSKU".equals(charId)) {
									
								}else if("ProductImage_URL".equals(charId) || "ProductImage".equals(charId)) {
									log("Got a change on an image item: " + charId);
									setProcedeNoProcedeArticle(externalId, cr);
								}else if("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
									String oldValue = null;
									String currentValue = null;
									try{
										oldValue = xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_old") .getTextContent();
									}catch(NullPointerException e) {
										oldValue = "";
									}
									log("Old mainBarCode: " + oldValue);
									try{
										Node currentNode = xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_current");
										if(currentNode != null) {
											currentValue = currentNode.getTextContent();
											log("MainBarCode: " + currentValue);
											String[] info = checkArticle(externalId);
											log(info == null ? "No info collected!" : "Info: " + java.util.Arrays.asList(info));
											appendEAN(currentValue, externalId, info != null ? info[1] : "" );
										}else {
											removeEAN(oldValue);
										}
									}catch(NullPointerException e) {
										currentValue = "";
									}
								}
								if( 
										   "ColoursLiverpoolAtt".equals(charId)
										|| "TamanoUnico".equals(charId)
										|| "ProductImage".equals(charId)
										|| "ProductImage_URL".equals(charId)
										|| "AssignTakeNoTake".equals(charId)
										|| "MainBarCode".equals(charId)
										|| "MainBarCodeS4H".equals(charId)
										|| "SKU".equals(charId)
								) {
									currentValueNode = null;
									oldValueNode = null;
									String valueChange = "";
									try {
										currentValueNode = xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_current");
										Node _externalId = xmm.byName( currentValueNode, "_externalId");
										if(_externalId != null) {
											valueChange = _externalId.getTextContent().replaceAll("^'|('@'.+)", "").replaceAll("\\\\'", "'");
										}else {
											valueChange = currentValueNode .getTextContent();
										}
									}catch(NullPointerException e) {
										
									}
									try {
										oldValueNode = xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_old");
										Node _externalId = xmm.byName( oldValueNode, "_externalId");
										if(_externalId != null) {
										}else {
										}
//										Node _code = xmm.byName( oldValueNode, "_code");
//										prevValue = (_code != null ? _code : oldValueNode) .getTextContent();
									}catch(NullPointerException e) {
										
									}
									if(charId.equals("ProductImage")) {
//										try {
//											log( xmm.prettyPrint(rootElement) );
//										} catch (TransformerException e) {
//											logE(e);
//										}
										java.util.List<Node> children = xmm.listImmediateChildElements(cr).get("_children");
										log("Got children: " + (children != null ? children.size() : "0" ));
										for(Node n : children) {
											if("ProductImage_URL".equals( xmm.byName( xmm.byName( xmm.byName( n, "_qualification"), "characteristic"), "_code") .getTextContent() )) {
												currentValueNode = xmm.byName( xmm.byName( xmm.byName( n, "_recordLang"), "values"), "_current");
												if(currentValueNode != null) {
													Node _code = xmm.byName( currentValueNode, "_code");
													valueChange = (_code != null ? _code : currentValueNode) .getTextContent();
													datas.put(charId, valueChange);
												}
											}
										}
										
									}
									try {
										oldValueNode = xmm.byName( xmm.byName( xmm.byName( cr, "_recordLang"), "values"), "_old");
									}catch(NullPointerException e) {
										
									}
									log("Came dt: " + charId + ", " + valueChange);
									if(!"ProductImage".equals(charId)) datas.put(charId, valueChange);
									toSend = true;
								}
							}
							String sku = null;
							try {
								sku = jp.getString("SKU");
								String r0 = dr.getProductData(new org.json.JSONArray().put(jp.getString("ProductNo")));
								org.json.JSONObject jp0 = null;
								if(r0 != null) {
									org.json.JSONObject jr0 = new org.json.JSONObject(r0);
									jp0 = jr0.getJSONArray("items").getJSONObject(0);
								}
								String internalStatus = jp0.getString("CurrentStatus");
								if( resendToSKU.contains(charId) ) {
									if(
											"1020".equals(internalStatus) 
											|| "1007".equals(internalStatus) 
											|| "1008".equals(internalStatus)
											) {
										toSkuUpdate = true;
									}
								}
							}catch(NullPointerException | org.json.JSONException e) {
								logE(e);
							}
							if(toSkuUpdate) {
								String internalStatus = null;
								String sapObjectType = null;
								String business = null;
								try {
									String r0 = dr.getProductData(new org.json.JSONArray().put(jp.getString("ProductNo")));
									org.json.JSONObject jp0 = null;
									if(r0 != null) {
										org.json.JSONObject jr0 = new org.json.JSONObject(r0);
										jp0 = jr0.getJSONArray("items").getJSONObject(0);
										internalStatus = jp0.getString("CurrentStatus");
										sapObjectType = jp0.getString("SAPObjectType");
										business = jp0.getString("Business");
									}
								}catch(NullPointerException | org.json.JSONException e) {
									logE(e);
								}
								if(sku != null && !"".equals(sku)) {
									CrearArchivosParaSKU caps = new CrearArchivosParaSKU();
									log("Ya vinimos aquí -.- (" + internalStatus + "," + sapObjectType + "," + business + ")");
									String[] info = new String[] { sapObjectType, business, sku, internalStatus }; // checkProduct(externalId);
									log("With actual data: " + java.util.Arrays.asList(info));
									try {
										if("MKP".equals(info[1])) {
											if(!"".equals(info[2])) {
												String[] contenido = caps.creacionDeArchivos(externalId, (short) 0); // CREA
												caps.handleContent(externalId, contenido);
												
												contenido = caps.creacionDeArchivos(externalId, (short) 1); // MODIF
												caps.handleContent(externalId, contenido);
											}else {
												if("".equals(info[2]) && "MKP".equals(info[1])) {
													String[] contenido = caps.creacionDeArchivos(externalId, (short) 0); // CREA
													caps.handleContent(externalId, contenido);
												}
											}
										}else {
											if(!"".equals(info[2])) {
												if(!"SBB".equals(info[1])) {
													String[] contenido = caps.creacionDeArchivos(externalId, (short) 0); // CREA
													caps.handleContent(externalId, contenido);
												}
												
												String[] contenido = caps.creacionDeArchivos(externalId, (short) 1); // MODIF
												caps.handleContent(externalId, contenido);
											}else {
												String[] contenido = caps.creacionDeArchivos(externalId, (short) 0); // CREA
												caps.handleContent(externalId, contenido);
											}
										}
									}catch(Exception e) {
										log("Exception on writing to creat archivos para SKU.");
										logE(e);
									}
								}
							}
							if(toSend) {
								
//						    	String coloursLiverpoolAtt = getValue("ColoursLiverpoolAtt", characteristicRecordsMap);
//						    	String tamanoUnico = getValue("TamanoUnico", characteristicRecordsMap);
//						    	String assignTakeNoTake = getSimpleValue("AssignTakeNoTake", characteristicRecordsMap);
//						    	String[] productImageUrl = new String[1];
//						    	productImageUrl[0] = "";
//						    	qp.clear();
//						    	qp.put("fields", 
//						 			"ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
//						 		);
//						    	qp.put("items", "'" + externalId + "'@1");
//						    	RESTWrapper rw = new RESTWrapper();
//						    	log("Gonna query: " + externalId);
//						 		rw.collectData("list", "Article", null, "byItems", qp, row->{
//						 			org.json.JSONArray values = row.getJSONArray("values");
//						 			log("Values: " + values);
//						 			productImageUrl[0] = values.getJSONArray(0).getString(0);
//						 		}, this::log);
//						 		log("Found productImage: " + productImageUrl[0]);
								if(jp != null) {
									for(java.util.Map.Entry<String, String> entry : datas.entrySet()) {
										jp.put(entry.getKey(), entry.getValue());
									}
//							 		String higherLevelProduct = grabHigherLevelProduct(data);
							 		org.json.JSONArray items = new org.json.JSONArray().put(
							 				jp
	//						 				new org.json.JSONObject()
	//							 				.put("variant", externalId)
	//							 				.put("ColoursLiverpoolAtt", coloursLiverpoolAtt == null ? "" : coloursLiverpoolAtt)
	//							 				.put("TamanoUnico", tamanoUnico == null ? "" : tamanoUnico)
	//							 				.put("ProductImage", productImageUrl[0])
	//							 				.put("AssignTakeNoTake", assignTakeNoTake == null ? "" : assignTakeNoTake)
	//							 				.put("SKU", sku == null ? "" : sku)
	//							 				.put("MainBarCode", ean == null ? "" : ean)
	//							 				.put("MainBarCodeS4H", ean2 == null ? "" : ean2)
	//							 				.put("ProductNo", higherLevelProduct == null ? "" : higherLevelProduct)
							 				);
									dr.putArticleData( items );
									log("Sending data to admin (article): " + items.getJSONObject(0));
								}
							}
						}
						if (changedFieldSet.contains("Product2G.SKU")) {
//							log("Change in SKU: " + identifier + " (" + sku + ")");
						}
						if (changedFieldSet.contains("Product2G.EAN")) {
//							log("Change in Product EAN: " + identifier + " (" + ean + ")");
							java.util.Map<String, java.util.LinkedList<Node>> productsMap = xmm.listImmediateChildElements( xmm.byName(rootElement, "product") );
							java.util.LinkedList<Node> gtinNodes = productsMap.get("gtin");
							if(gtinNodes != null && !gtinNodes.isEmpty()) {
								Node gtin = gtinNodes.getFirst();
								Node current = xmm.byName(gtin, "_current");
								Node old = xmm.byName(gtin, "_old");
								if(old != null) {
									dr.retiraEANProductNo(new org.json.JSONArray().put(old.getTextContent()));
								}
								if(current != null) {
									String s = dr.getProductData(new org.json.JSONArray().put(json.getJSONObject("entityItemChange").getString("_identifier")));
									log("Got: " + s);
									if(s != null) {
										try {
											org.json.JSONObject jr = new org.json.JSONObject(s);
											org.json.JSONArray items = jr.getJSONArray("items");
											items.getJSONObject(0).put("MainBarCode", current.getTextContent());
											log("To send: " + items);
											log( dr.putProductData(items) );
										}catch(org.json.JSONException e) {
											logE(e);
										}
									}
								}
							}
						}
						if (changedFieldSet.contains("Article.EAN")) {
//							log("Change in Article EAN: " + identifier + " (" + ean + ")");
							java.util.Map<String, java.util.LinkedList<Node>> productsMap = xmm.listImmediateChildElements( xmm.byName(rootElement, "article") );
							java.util.LinkedList<Node> gtinNodes = productsMap.get("gtin");
							if(gtinNodes != null && !gtinNodes.isEmpty()) {
								Node gtin = gtinNodes.getFirst();
								Node current = xmm.byName(gtin, "_current");
								Node old = xmm.byName(gtin, "_old");
								if(old != null) {
									dr.retiraEANSupplierAID(new org.json.JSONArray().put(old.getTextContent()));
								}
								if(current != null) {
									String s = dr.getArticleData(new org.json.JSONArray().put(json.getJSONObject("entityItemChange").getString("_identifier")));
									log("Got: " + s);
									if(s != null) {
										try {
											org.json.JSONObject jr = new org.json.JSONObject(s);
											org.json.JSONArray items = jr.getJSONArray("items");
											items.getJSONObject(0).put("MainBarCode", current.getTextContent());
											log("To send: " + items);
											log( dr.putArticleData(items) );
										}catch(org.json.JSONException e) {
											logE(e);
										}
									}
								}
							}
						}
						if ( changedFieldSet.contains("Product2GLang.ProductName") ) {
							String r = null;
							r = dr.getProductData( new org.json.JSONArray().put(identifier) );
							org.json.JSONObject jr = null;
							org.json.JSONObject jp = null;
							if(r != null) {
								try {
									jr = new org.json.JSONObject(r);
									jp = jr.getJSONArray("items").getJSONObject(0);
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
							java.util.Map<String, java.util.LinkedList<Node>> productsMap = xmm.listImmediateChildElements( xmm.byName(rootElement, "product") );
							java.util.LinkedList<Node> langNodes = productsMap.get("lang");
							for(Node ln : langNodes) {
								if("esl".equals( xmm.byName( xmm.byName( xmm.byName(ln, "_qualification"), "language"), "_code").getTextContent() ) ){
									Node pnn = xmm.byName( ln, "productName" );
									if(pnn != null) {
										Node cnn  = xmm.byName( pnn, "_current" );
										if(cnn != null) {
											String valueChange = cnn.getTextContent();
											String brandName = jp.getString("BrandName");
											String brandNameS4H = jp.getString("BRAND_ID_S4H");
											String bnl = brandName != null && !"".equals(brandName) ? getBrandLabel(brandName, "ZCOMALOV") : "";
											String bnls4h = brandNameS4H != null && !"".equals(brandNameS4H) ? getBrandLabel(brandNameS4H, "BRAND_IDLOV_S4H") : "";
											log("Change in ProductName. Got label for BrandName: " + bnl + ", BRAND_ID_S4H: " + bnls4h);
											String sinMarca = removeBrandIgnoreCaseAndAccents(valueChange, bnl == null || bnl.isEmpty() ? bnls4h : bnl);
//													valueChange == null || (bnls4h == null && bnl == null) ? 
//															valueChange == null ? "" : valueChange : 
//														valueChange.replaceFirst("(?i)(?<![\\p{L}])" + 
//											java.util.regex.Pattern.quote(bnl == null || bnl.isEmpty() ? bnls4h : bnl)
//														+ "(?![\\p{L}])", "")
//														.replaceAll(" {2,}", " ").trim();
											log("Got Sin Marca: " + sinMarca);
											rowsSinMarca.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + identifier + "'@1")).put("values", new org.json.JSONArray().put(sinMarca)));
											if(rowsSinMarca.length() == 1000) {
												rw.writeData("list", "Product2G", null, qp0, requestSinMarca, this::log);
											}
										}else {
											try { log("Had productNode but no _current node: " + xmm.prettyPrint(rootElement)); } catch (TransformerException e) { e.printStackTrace(); }
										}
									}else {
										try { log("No productNode." + xmm.prettyPrint(rootElement)); } catch (TransformerException e) { e.printStackTrace(); }
									}
								}
							}
						}
//					}else {
//						log("Error requesting data from object api: " + workshop.getRawResponse());
//						logE(workshop.getException());
//						
//					}
				}else if("StructureGroup".equals(entity)) {
					
				}else if("StandardizationValue".equals(entity)) {
					
				}else if("Characteristic".equals(entity)) {
					
				}else if("LookupValue".equals(entity)) {
					
				}
			}
		}else if(json.has("entityItemsDeleted")){
			log("A message body: " + json);
			if("Article".equals(json.getString("entity"))) {
				org.json.JSONObject entityItemsDeleted = json.getJSONObject("entityItemsDeleted");
				org.json.JSONArray ids = entityItemsDeleted.getJSONArray("_identifier");
				DataRequestor dr = new DataRequestor();
				log( dr.retiraArticulo(ids) );
			}else if("Product2G".equals(json.getString("entity"))) {
				org.json.JSONObject entityItemsDeleted = json.getJSONObject("entityItemsDeleted");
				org.json.JSONArray ids = entityItemsDeleted.getJSONArray("_identifier");
				DataRequestor dr = new DataRequestor();
				log("Product2G delete: " + dr.retiraProducto(ids) );
			}
		}
	}
	
	private String removeBrandIgnoreCaseAndAccents(String productName, String brandName) {
	    if (productName == null || brandName == null || brandName.trim().isEmpty()) {
	        return productName;
	    }

	    String normalizedProduct = java.text.Normalizer.normalize(productName, java.text.Normalizer.Form.NFC);
	    String normalizedBrand = java.text.Normalizer.normalize(brandName, java.text.Normalizer.Form.NFC);

	    return normalizedProduct
	        .replaceFirst("(?iu)(?<![\\p{L}])" + java.util.regex.Pattern.quote(normalizedBrand) + "(?![\\p{L}])", "")
	        .replaceAll(" {2,}", " ")
	        .trim();
	}
	
	public void sendData() {
		if(rowsSinMarca.length() > 0) { log("Gonna send: " + rowsSinMarca.length() + " títulos sin marca.");
			rw.writeData("list", "Product2G", null, qp0, requestSinMarca, this::log);
		}
		if(requestPNP.length() > 0) { log("Gonna send: " + requestPNP.length() + " Procede No Procede.");
			rw.writeData("list", "Article", null, qp0, requestPNP, this::log);
		}
	}
	
//	private String grabHigherLevelProduct(org.json.JSONObject data) {
//		if(data.has("higherLevelProduct")) {
//			org.json.JSONArray higherLevelProducts = data.getJSONArray("higherLevelProduct");
//			org.json.JSONObject higherLevelProduct = higherLevelProducts.getJSONObject(0);
//			return higherLevelProduct.getJSONObject("_qualification").getString("referencedIdentifier");
//		}
//		return null;
//	}
	
	public void setProcedeNoProcede(String externalId) {
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + externalId + "\") = \"" + externalId + "\"");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray charValue = null;
		String articleId = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				charValue = values.getJSONArray(1);
				articleId = values.getString(0);
				response = workshop.makeRequest("PUT", "/object/Article/'" + articleId + "'@'MASTER'", empty, new org.json.JSONObject()
						.put("_characteristicRecords", new org.json.JSONArray()
								.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", "ProcedeNoProcede")))
										.put("_recordLang", new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
												.put("values", new org.json.JSONArray().put(!"".equals(charValue.getString(0))))))).toString());
				if(response != null) {
					log("Put ProcedeNoProcede for: " + articleId);
				}else {
					log("Error while updating \"ProcedeNoProcede\" value. " + workshop.getRawResponse());
				}
			}
		}
	}
	
	private void addCharacteristicValue(org.json.JSONArray characteristicArray, String characteristicId, Object value, boolean isCode, boolean isLabel ) {
		org.json.JSONObject characteristicValue  = new org.json.JSONObject();
		characteristicValue.put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicId)));
		characteristicValue.put("_recordLang",    new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(isCode ? new org.json.JSONObject().put("_code", value) : isLabel ? new org.json.JSONObject().put("_label", value) : value))));
		characteristicArray.put( characteristicValue );
	}
	
	private void sendUpdateObjectAPI(String proposalId, org.json.JSONObject data) {
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("PUT", "/object/Product2G/'" + proposalId + "'@'MASTER'", qp, data.toString());
		log(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	}
	
	private String getBrandLabel(String code, String lkpId) {
		String label = "";
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "global_lookups", lkpId ).toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(pieces.length > 1 && code.equals(pieces[0])) {
					label = pieces[1];
					break;
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return label;
	}
	
	private String queryDictionary(String value, String dictionary) {
		String r = "";
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", dictionary ).toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(pieces[0].equals(value)) {
					r = pieces.length == 1 ? "" : pieces[1];
					break;
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return r;
	}

//	private String queryDictionary(String value, String dictionary) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "StandardizationValue.AlternativeValue");
//		qp.put("query", "StandardizationValue.Value equals \"" + value + "\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dictionary + "\"");
//		qp.put("dictionaryProxy", "'" + dictionary + "'");
//		org.json.JSONObject response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
//		if(response == null) {
//			log("Error querying standardization value: " + workshop.getRawResponse());
//			return null;
//		}
//		else {
//			return response.has("rows") && response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : null;
//		}
//	}

	private void setTallaNormalizada(String tn, String externalId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		log("Setting TamanoUnicoSTD to " + externalId);
		org.json.JSONObject response = workshop.makeRequest("PUT", "/object/Article/'" + externalId + "'@'MASTER'", qp,
				new org.json.JSONObject()
				.put("_characteristicRecords",
						new org.json.JSONArray()
						.put(
							new org.json.JSONObject()
								.put("_recordLang",
										new org.json.JSONArray()
										.put(
											new org.json.JSONObject()
											.put("values",
													new org.json.JSONArray()
													.put(tn)
											)
										)
								)
								.put("_qualification",
										new org.json.JSONObject()
										.put("characteristic",
												new org.json.JSONObject()
												.put("_code", "TamanoUnicoSTD")
										)
								)
						)
				).toString());
		if(response == null) {
			log("Error writing external status: " + workshop.getRawResponse());
		}else {
			log("From setting TamanoUnicoSTD: " + response);
		}
	}

	
	

	public void setProcedeNoProcedeArticle(String externalId, Node cr) {
		java.util.List<Node> children = xmm.listImmediateChildElements(cr).get("_children");
		log("Got children: " + (children != null ? children.size() : "0" ));
		for(Node n : children) {
			if("ProductImage_URL".equals( xmm.byName( xmm.byName( xmm.byName( n, "_qualification"), "characteristic"), "_code") .getTextContent() )) {
				Node currentValueNode = xmm.byName( xmm.byName( xmm.byName( n, "_recordLang"), "values"), "_current");
				if(currentValueNode != null) {
					Node _code = xmm.byName( currentValueNode, "_code");
					String valueChange = (_code != null ? _code : currentValueNode) .getTextContent();
					if(!"".equals(valueChange)) {
						rowsPNP.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put("true")));
						log("Added row for pnp");
						if(rowsPNP.length() == 100) {
							rw.writeData("list", "Article", null, this.qp0, requestPNP, this::log);
						}
					}
			}
		}
//		java.util.Map<String, String> empty = new java.util.TreeMap<>();
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields",
//				   "Article.SupplierAID"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
//		qp.put("query", "Article.SupplierAID equals \"" + externalId + "\"");
//		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		org.json.JSONArray charValue = null;
//		String articleId = null;
//		if(response != null) {
//			rows = response.getJSONArray("rows");
//			for(int i=0; i<rows.length(); i++) {
//				values = rows.getJSONObject(i).getJSONArray("values");
//				charValue = values.getJSONArray(1);
//				articleId = values.getString(0);
//				rowsPNP.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + articleId + "'@1")).put("values", new org.json.JSONArray().put(!"".equals(charValue.getString(0)))));
//				if(rowsPNP.length() == 100) {
//					rw.writeData("list", "Article", null, this.qp0, requestPNP, this::log);
//				}
//				response = workshop.makeRequest("PUT", "/object/Article/'" + articleId + "'@'MASTER'", empty, new org.json.JSONObject()
//						.put("_characteristicRecords", new org.json.JSONArray()
//								.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", "ProcedeNoProcede")))
//										.put("_recordLang", new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
//												.put("values", new org.json.JSONArray().put(!"".equals(charValue.getString(0))))))).toString());
//				if(response != null) {
//					log("Put ProcedeNoProcede for: " + articleId);
//				}else {
//					log("Error while updating \"ProcedeNoProcede\" value. " + workshop.getRawResponse());
//				}
//			}
		}
	}
	
	private void removeEAN(String ean) {
		java.util.Map<String, String[]> data = readEANs();
		data.remove(ean);
		writeEANs(data);
	}
	
	private java.util.Map<String, String[]> readEANs(){
		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.article_ean_file")).toString())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, "\"", ";", "\\");
				data.put(pieces[0], new String[] {pieces[1], pieces[2]});
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return data;
	}
	
	private void writeEANs(java.util.Map<String, String[]> data) {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.article_ean_file")).toString())))){
			for(java.util.Map.Entry<String, String[]> entry : data.entrySet())
				pw.println( workshop.serializeChunk(new String[] { entry.getKey(), entry.getValue()[0], entry.getValue()[1]}, "\"", ";", "\\") );
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	private String[] checkArticle(String id) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"ProductReference.ReferencedSupplierAid"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				);
		qp.put("query", "Article.SupplierAID equals \"" + id + "\"");
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Article/ProductReference/bySearch", qp, null);
		if(response != null && response.getJSONArray("rows").length() > 0) {
			return new String[] { response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
					};
		}else {
			if(response == null)
				log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}
	
	private void appendEAN(String ean, String externalId, String business) {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.article_ean_file")).toString(), true)))){
			pw.println( workshop.serializeChunk(new String[] { ean, externalId, business }, "\"", ";", "\\") );
		}catch(java.io.IOException e) {
			logE(e);
		}
	}

	private final java.util.List<String> resendToSKU = new java.util.ArrayList<>(java.util.Arrays.asList(new String[] {
			"CostobrutoSinIVA",
			"PrecioSugeridocIVA",
			"CostoEnMonedaExtranjera",
			"Descuento1",
			"Descuento2",
			"CostoNetoSinIVA",
			"TextoAdicional",
			"Evento",
			"Licencia",
			"Coleccion",
			"MainBarCode",
			"Status",
			"ProductWidth",
			"ProductDepth",
			"ProductHeight",
			"ProductWeight",
			"VOLUMAtt",
			"PesoBruto",
			"ZHOECJ",
			"ZBRECJ",
			"ZLAECJ",
			"ZBRGCJ",
			"ZVOLCJ",
			"WHSTC",
			"Armado",
			"HNDLCODE",
			"MVGR5",
			"ArgumentoDeVenta",
			"TypeMainBarCode",
			"FechaInicioVigenciaCostoNeto",
			"FechaInicioVigenciaPrecioVenta",
			"TipoDeEtiqueta",
			"FechaInicioVigenciaCostoImportacion",
			"MesdeEntregadeMercancIa",
			"ZBREPQ",
			"ZHOEPQ",
			"ZLAEPQ",
			"ZVOLPQ",
			"ZBRGPQ",
			"ZNTGPQ",
			"DescriptionLong2"
			}));
	
	public void connect(String host, int port, String qName) {
		try{
			connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port + "?wireFormat.maxInactivityDuration=60000&keepAlive=true");
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        responseQueue = session.createQueue(qName);
	        consumer = session.createConsumer(responseQueue);
		}catch(JMSException e){
			e.printStackTrace();
		}
	}
	
	public void process() throws ServiceUnavailableException {
		try{
			log("Start listening for messages...");
			while(running){
				responseMessage = consumer.receive(30);
			    if (responseMessage != null && responseMessage instanceof TextMessage) {
			     	try{
			     		messageProcessor(((TextMessage) responseMessage).getText());
			     	}catch(org.json.JSONException e) {
			     		logE(e);
			     	}
			     	log("Doney");
				}
			}
		}catch(ParserConfigurationException | SAXException | java.io.IOException e){
			logE(e);
		}catch(org.json.JSONException e){
    		logE(e);
    	} catch (JMSException e) {
    		logE(e);
		}finally {
			disconnect();
		}
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}

	private void disconnect() {
		if(connection != null){
			try{
				connection.close();
			}catch(JMSException e){
				e.printStackTrace();
			}
		}
	}

	private static final java.nio.file.Path ATG_PENDING_IDS_FILE =
			java.nio.file.Paths.get("../logs/amqp/productArticleCharactChange/PACVC_ATG_PENDING_IDS.txt");

	private static synchronized void appendAtgPendingId(String externalId) {
		try {
			java.nio.file.Files.createDirectories(ATG_PENDING_IDS_FILE.getParent());

			java.nio.file.Files.write(
					ATG_PENDING_IDS_FILE,
					(externalId + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (java.io.IOException e) {
			LOGGER.log(Level.SEVERE, "No pude escribir ID pendiente de ATG: " + externalId, e);
		}
	}
	
	private static final java.nio.file.Path MKT_PENDING_IDS_FILE =
			java.nio.file.Paths.get("../logs/amqp/productArticleCharactChange/PACVC_MKT_PENDING_IDS.txt");

	private static synchronized void appendMktPendingId(String externalId) {
		try {
			java.nio.file.Files.createDirectories(MKT_PENDING_IDS_FILE.getParent());

			java.nio.file.Files.write(
					MKT_PENDING_IDS_FILE,
					(externalId + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (java.io.IOException e) {
			LOGGER.log(Level.SEVERE, "No pude escribir ID pendiente de MKT: " + externalId, e);
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(ProductArticleCharacteristicValueChangeProcessor.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp/productArticleCharactChange/PACVCactiveMQListener-%g.log", 25 * 1024 * 1024, 10, true);
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
//        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//                new java.io.FileOutputStream("../logs/PACVCactiveMQListener.log", true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "]  " + message);
//        } catch (java.io.IOException e) {
//        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/PACVCactiveMQListener.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

}
