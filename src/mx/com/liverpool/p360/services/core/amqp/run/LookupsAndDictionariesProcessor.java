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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class LookupsAndDictionariesProcessor {

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
	
	public LookupsAndDictionariesProcessor() {
		rw = new RESTWrapper();
		workshop = rw.getRw();
		xmm = rw.getXmm();
	}

	private static final java.util.Set<String> bannedOnes = new java.util.TreeSet<>();
	
	static {
		bannedOnes.add("GlobalTemplateAttributeConfiguration");
		bannedOnes.add("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		bannedOnes.add("ErroresSKU");
	}

	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException {


		String entity = null;
		String internalId = null;
		String changeSummary = null;

		org.json.JSONArray changedField = null;

		java.util.Set<String> changedFieldSet = new java.util.TreeSet<>();
		org.json.JSONObject json = new org.json.JSONObject(message);

    	String lookupExternalId = null;

		if(json.has("entityItemChange")) {
	     	entity = json.getJSONObject("entityItemChange").getString("_entity");
	     	internalId = json.getJSONObject("entityItemChange").getJSONObject("_entityItem").getString("_internalId");
	     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
	     	changedField = json.getJSONObject("entityItemChange").has("_changedField") ? json.getJSONObject("entityItemChange").getJSONArray("_changedField") : new org.json.JSONArray();
			log("A message body: " + json);
			for(int i=0; i<changedField.length(); i++) {
				changedFieldSet.add(changedField.getString(i));
			}

			log("changeFields: " + changedField);

	     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	Document doc;

	    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
	    		doc = builder.parse( baos );
	    		doc.getDocumentElement().normalize();
	    	}
	    	
			if(doc != null) {
				Element rootElement = doc.getDocumentElement();
				if("StandardizationValue".equals(entity)) {
					String container = json.getJSONObject("entityItemChange").getJSONObject("_container").getString("_externalId");
					container = container.substring(1, container.length() - 1);
					if(!bannedOnes.contains(container)) {
						if(changedFieldSet.contains("StandardizationValue.AlternativeValue")) {
							String id = json.getJSONObject("entityItemChange").getString("_identifier");
							Node stdValNode = xmm.byName(rootElement, "standardizationValue");
							Node ct = xmm.byName(stdValNode, "_changeType");
							if(ct != null && ("CHANGED".equals(ct.getTextContent()) || "CREATED".equals(ct.getTextContent()))) {
								Node altValNode = xmm.byName(stdValNode, "alternativeValue");
								if(altValNode != null) {
									java.util.Map<String, String> dictionaryData = readStdValues(container.replaceAll("/", "<::>"));
									Node cn = xmm.byName(altValNode, "_current");
									if(cn != null) {
										dictionaryData.put(id, cn.getTextContent());
									}else {
										dictionaryData.remove(id);
									}
									keepStdValues(container.replaceAll("/", "<::>"), dictionaryData);
								}
							}
						}else if(changedFieldSet.contains("StandardizationValue.Value")) {
							Node stdValNode = xmm.byName(rootElement, "standardizationValue");
							Node ct = xmm.byName(stdValNode, "_changeType");
							if(ct != null && "CHANGED".equals(ct.getTextContent())) {
								Node valNode = xmm.byName(stdValNode, "value");
								if(valNode != null) {
									java.util.Map<String, String> dictionaryData = readStdValues(container.replaceAll("/", "<::>"));
									Node on = xmm.byName(valNode, "_old");
									Node cn = xmm.byName(valNode, "_current");
									if(cn != null) {
										String oldValue = dictionaryData.remove(on.getTextContent());
										dictionaryData.put(cn.getTextContent(), oldValue);
										keepStdValues(container.replaceAll("/", "<::>"), dictionaryData);
									}
								}
							}
						}
					}else if("GlobalTemplateAttributeConfiguration".equals(container) || "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla".equals(container)) {
						Node stdValNode = xmm.byName(rootElement, "standardizationValue");
						Node ct = xmm.byName(stdValNode, "_changeType");
						if(ct != null && ("CHANGED".equals(ct.getTextContent()) || "CREATED".equals(ct.getTextContent()))) {
							Node value = xmm.byName(stdValNode, "value");
							Node alternativeValue = xmm.byName(stdValNode, "alternativeValue");
							Node propertyNode = xmm.byName(stdValNode, "property");
							Node propertyValueNode = xmm.byName(stdValNode, "propertyValue");
							Node characteristicNode = xmm.byName(stdValNode, "characteristic");
							Node structureGroupNode = xmm.byName(stdValNode, "structureGroup");
							if(structureGroupNode == null) {
								structureGroupNode = xmm.byName(stdValNode, "structure_group");
							}

//							Node _OPropertyNode 		= propertyNode 			== null ? null : xmm.byName(propertyNode, "_old");
//							Node _OPropertyValueNode 	= propertyValueNode 	== null ? null : xmm.byName(propertyValueNode, "_old");
//							Node _OCharacteristicNode 	= characteristicNode 	== null ? null : xmm.byName(characteristicNode, "_old");
//							Node _OStructureGroupNode 	= structureGroupNode 	== null ? null : xmm.byName(structureGroupNode, "_old");
							Node _OValueNode			= value 				== null ? null : xmm.byName(value, "_old");
//							Node _OAlternativeValueNode	= alternativeValue 		== null ? null : xmm.byName(alternativeValue, "_old");
							
							DataRequestor dr = new DataRequestor();
							
							Node _CPropertyNode 		= propertyNode 			== null ? null : xmm.byName(propertyNode, "_current");
							Node _CPropertyValueNode 	= propertyValueNode 	== null ? null : xmm.byName(propertyValueNode, "_current");
							Node _CCharacteristicNode 	= characteristicNode 	== null ? null : xmm.byName(characteristicNode, "_current");
							Node _CStructureGroupNode 	= structureGroupNode 	== null ? null : xmm.byName(structureGroupNode, "_current");
							Node _CValueNode			= value 				== null ? null : xmm.byName(value, "_current");
							Node _CAlternativeValueNode	= alternativeValue 		== null ? null : xmm.byName(alternativeValue, "_current");
							
							String _value = _CValueNode != null ? _CValueNode.getTextContent() : json.getJSONObject("entityItemChange").getString("_identifier");
							String _alternativeValue = _CAlternativeValueNode != null ? _CAlternativeValueNode.getTextContent() : "";
							String _structureGroup = _CStructureGroupNode != null ? xmm.byName(_CStructureGroupNode, "_code").getTextContent() : "";
							String _characteristic = _CCharacteristicNode != null ? xmm.byName(_CCharacteristicNode, "_code").getTextContent() : "";
							String _property = _CPropertyNode != null ? xmm.byName(_CPropertyNode, "_code").getTextContent() : "";
							String _propertyValue = _CPropertyValueNode != null ? _CPropertyValueNode.getTextContent() : "";
							
							String r = dr.getContenidoDeDiccionario(new org.json.JSONArray().put(new org.json.JSONObject().put("diccionario", container).put("idValor", _OValueNode != null ? _OValueNode.getTextContent() : _value)));
							org.json.JSONObject jr = null;
							String cAlternativeValue = null;
							String cStructureGroup = null;
							String cCharacteristic = null;
							String cProperty = null;
							String cPropertyValue = null;
							String cPropertyShortCode = null;
							org.json.JSONObject item = null;
							if(r != null) {
								jr = new org.json.JSONObject(r);
								org.json.JSONArray items = jr.getJSONArray("items");
								item = items.getJSONObject(0);
								cAlternativeValue = item.getString("alternativeValue");
								cStructureGroup = item.getString("structureGroup");
								cCharacteristic = item.getString("characteristic");
								cProperty = item.getString("property");
								cPropertyValue = item.getString("propertyValue");
								cPropertyShortCode = item.getString("propertyShortCode");
							}
							org.json.JSONObject jo = item;
							jo
									.put("diccionario", container)
									.put("idValor", _value)
									.put("alternativeValue", "".equals(_alternativeValue) ? ( cAlternativeValue != null ? cAlternativeValue : "" ) : _alternativeValue)
									.put("structureGroup", "".equals(_structureGroup) ? ( cStructureGroup != null ? cStructureGroup : "" ) : _structureGroup)
									.put("characteristic", "".equals(_characteristic) ? ( cCharacteristic != null ? cCharacteristic : "" ) : _characteristic)
									.put("property", "".equals(_property) ? ( cProperty != null ? cProperty : "" ) : _property)
									.put("propertyValue", "".equals(_propertyValue) ? ( cPropertyValue != null ? cPropertyValue : "" ) : _propertyValue)
									.put("propertyShortCode", "".equals(_property) ? ( cPropertyShortCode != null ? cPropertyShortCode : "" ) : data.get(_property) );
							log("Bout to send: " + jo);
							log( dr.addContenidoDeDiccionario(new org.json.JSONArray()
									.put( jo )
								) );
							if(_OValueNode != null) {
								dr.removeContenidoDeDiccionario(new org.json.JSONArray().put(new org.json.JSONObject().put("diccionario", container).put("idValor", _OValueNode.getTextContent())));
							}
						}
							
						if(changedFieldSet.contains("StandardizationValue.PropertyValue")) {
							stdValNode = xmm.byName(rootElement, "standardizationValue");
							ct = xmm.byName(stdValNode, "_changeType");
							if(ct != null && ("CHANGED".equals(ct.getTextContent()) || "CREATED".equals(ct.getTextContent()))) {
								Node propertyNode = xmm.byName(stdValNode, "property");
								Node propertyValueNode = xmm.byName(stdValNode, "propertyValue");
								Node characteristicNode = xmm.byName(stdValNode, "characteristic");
								if(propertyNode != null && propertyValueNode != null && characteristicNode != null) {
									Node on = xmm.byName(propertyNode, "_old");
									Node cn = xmm.byName(propertyNode, "_current");
									Node pvcn = xmm.byName(propertyValueNode, "_current");
									Node ccn = xmm.byName(characteristicNode, "_current");
									if(on == null && cn != null) {
										if("VendorCenterSection".equals(xmm.byName( cn, "_code").getTextContent())) {
											try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections" ).toFile(), true), java.nio.charset.StandardCharsets.UTF_8))){
												pw.println( rw.getRw().serializeChunk(new Object[] { xmm.byName( ccn, "_code").getTextContent(), pvcn.getTextContent() }) );
											}catch(java.io.IOException e) {
												e.printStackTrace();
											}
										}
									}else if(on != null && cn != null) {
										log("Currently building... on not null and cn not null");
									}else if(on != null && cn == null) {
										log("Currently building... on not null and cn is null");
									}
								}else if ( propertyValueNode != null ) {
									java.util.Map<String, String> qp = new java.util.TreeMap<>();
									qp.put("includeIds", "true");
									qp.put("includeLabels", "true");
									org.json.JSONObject response = rw.getRw().makeRequest("GET", "/object/StandardizationValue/" + internalId, qp, null);
									if(response != null) {
										org.json.JSONObject data = response.getJSONObject("_data");
										String characteristic = data.has("characteristic") ? data.getJSONObject("characteristic").getString("_code") : null;
										if(characteristic != null) {
											String property = data.has("property") ? data.getJSONObject("property").getString("_code") : null;
											if("VendorCenterSection".equals(property)) {
												Node pvcn = xmm.byName(propertyValueNode, "_current");
												Node pvon = xmm.byName(propertyValueNode, "_old");
												java.util.Map<String, String> sections = null;
												if(pvcn != null && pvon == null) {
													try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections" ).toFile(), true), java.nio.charset.StandardCharsets.UTF_8))){
														pw.println( rw.getRw().serializeChunk(new Object[] { characteristic, pvcn.getTextContent() }) );
													}catch(java.io.IOException e) {
														e.printStackTrace();
													}
												} else if(pvcn != null && pvon != null) {
													try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
	//													sections = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections")).parallel().map(rw.getRw()::parseLine).collect(java.util.stream.Collectors.toConcurrentMap(a -> a[0], a -> a[1]));
														String line = null;
														sections = new java.util.HashMap<>();
														String[] pieces = null;
														while((line = br.readLine()) != null) {
															pieces = rw.getRw().parseLine(line);
															sections.put(pieces[0], pieces[1]);
														}
													}catch(java.io.IOException e) {
														logE(e);
													}
													if(sections != null) {
														sections.put(characteristic, pvcn.getTextContent());
														try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
															sections.entrySet().forEach(entry -> pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
														}catch(java.io.IOException e) {
															e.printStackTrace();
														}
													}
												}else if(pvcn == null && pvon != null) {
													try {
														sections = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections")).parallel().map(rw.getRw()::parseLine).collect(java.util.stream.Collectors.toConcurrentMap(a -> a[0], a -> a[1]));
													}catch(java.io.IOException e) {
														logE(e);
													}
													if(sections != null) {
														sections.remove(characteristic);
														try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
															sections.entrySet().forEach(entry -> pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
														}catch(java.io.IOException e) {
															e.printStackTrace();
														}
													}
												}
											}
										}
									}else {
										log("ERROR requesting data for ID: " + internalId + ". GOT: " + rw.getRw().getRawResponse());
									}
								}
							}
						}
					}
					if(changedFieldSet.contains("StandardizationValue.PropertyValue")) {
						org.json.JSONObject responseObject = requestStandardizationValueObject(internalId);
						if(responseObject != null && responseObject.has("totalSize")) {
							org.json.JSONArray rows = responseObject.getJSONArray("rows");
							if(rows.length() > 0) {
								org.json.JSONArray values = rows.getJSONObject(0).getJSONArray("values");
								if("ListOfValuesFilter".equals(values.getString(2))) {
									new PubSubGCP().publishMessage(
											PropertiesManager.get( "p360.contingency.gcp.project_back" )
											, "idmc_put_value-list-relation"
											, PropertiesManager.get( "p360.contingency.gcp.service_account_back" )
											, new org.json.JSONObject()
												.put("valueListRelations", new org.json.JSONArray().put(new org.json.JSONObject().put("idTemplate", values.getString(0)).put("attributeName", values.getString(1)))).toString());
									String templateId = values.getString(0);
									String characteristicId = values.getString(1);
									String lov = values.getString(3);
									String id = values.getString(4);
									if(!"".equals(lov)) {
										java.util.LinkedList<Node> stdValues = xmm.listImmediateChildElements( rootElement).get("standardizationValue");
										if(stdValues != null) {
											for(Node n : stdValues) {
												java.util.LinkedList<Node> pvs = xmm.listImmediateChildElements(n).get("propertyValue");
												if(pvs != null) {
													for(Node npv : pvs) {
														Node cn = xmm.byName(npv, "_current");
														if(cn != null) {
															java.util.Map<String, String[]> cacheFileContents = readCacheFilterContents();
															if("".equals(cn.getTextContent())) {
																cacheFileContents.remove(id);
															}else {
																cacheFileContents.put(id, new String[] { templateId + "_" + characteristicId, lov, cn.getTextContent() });
															}
															rewriteCacheFileContents(cacheFileContents);
														}else {
															java.util.Map<String, String[]> cacheFileContents = readCacheFilterContents();
															cacheFileContents.remove(id);
															rewriteCacheFileContents(cacheFileContents);
														}
													}
												}
											}
										}
									}
								}
							}else {
								log("Now got an empty response from querying else data... ID: " + internalId + ", responseObject: " + responseObject);
							}
						}
					}
				} else if("LookupValue".equals(entity)) {
					if(changedFieldSet.contains("LookupValueLang.Name")) {
						log("Updating per label change...");
						String code = json.getJSONObject("entityItemChange").getString("_identifier");
						String container = json.getJSONObject("entityItemChange").getJSONObject("_container").getString("_externalId");
						container = container.substring(1, container.length() - 1);
						Node lkpValNode = xmm.byName(rootElement, "lookupValue");
						Node ct = xmm.byName(lkpValNode, "_changeType");
						if(ct != null && ( "CHANGED".equals(ct.getTextContent()) || "CHANGED_CHILD".equals(ct.getTextContent()))) {
							Node valNode = xmm.byName( xmm.byName(lkpValNode, "lang"), "name");
							if(valNode != null) {
								java.util.Map<String, String> dictionaryData = readLkpValues(container.replaceAll("/", "<::>"));
								Node cn = xmm.byName(valNode, "_current");
								if(cn != null) {
									String label = cn.getTextContent();
									dictionaryData.put(code, label);
									keepLkpValues(container.replaceAll("/", "<::>"), dictionaryData);
								}
							}
						}
					}
					if(changedFieldSet.contains("LookupValue.Code")) {
						String container = json.getJSONObject("entityItemChange").getJSONObject("_container").getString("_externalId");
						container = container.substring(1, container.length() - 1);
						Node lkpValNode = xmm.byName(rootElement, "lookupValue");
						Node ct = xmm.byName(lkpValNode, "_changeType");
						if(ct != null && "CHANGED".equals(ct.getTextContent())) {
							Node valNode = xmm.byName(lkpValNode, "code");
							if(valNode != null) {
								java.util.Map<String, String> dictionaryData = readLkpValues(container.replaceAll("/", "<::>"));
								Node on = xmm.byName(valNode, "_old");
								Node cn = xmm.byName(valNode, "_current");
								if(cn != null) {
									String label = dictionaryData.remove(on.getTextContent());
									dictionaryData.put(cn.getTextContent(), label);
									keepLkpValues(container.replaceAll("/", "<::>"), dictionaryData);
								}
							}
						}
					}
					java.util.LinkedList<Node> lkpValues = xmm.listImmediateChildElements( rootElement ).get("lookupValue");
					if(lkpValues != null) {
						for(Node n : lkpValues) {
							Node ct = xmm.byName(n, "_changeType");
							Node code = xmm.byName(n, "code");
							Node crr = code == null ? null : xmm.byName(code, "_current");
							if(ct != null && "CREATED".equals(ct.getTextContent()) && crr != null && !"".equals(crr.getTextContent())) {
								java.util.LinkedList<Node> langLst = xmm.listImmediateChildElements(n).get("lang");
								if(langLst != null) {
									for(Node ln : langLst) {
										Node qualif = xmm.byName(ln, "_qualification");
										if(qualif != null) {
											Node language = xmm.byName(qualif, "language");
											if(language != null) {
												Node codeNode = xmm.byName(language, "_code");
												if(codeNode != null) {
													if("esl".equals(codeNode.getTextContent())) {
														Node nameNode = xmm.byName(ln, "name");
														if(nameNode != null) {
															Node nameNodeCurrent = xmm.byName(nameNode, "_current");
															if(nameNodeCurrent != null && !"".equals(nameNodeCurrent.getTextContent())) {
																lookupExternalId = json.getJSONObject("entityItemChange").getJSONObject("_container").getString("_externalId");
																lookupExternalId = lookupExternalId.substring(1, lookupExternalId.length() - 1);
																String changed = lookupExternalId.replaceAll("/", "<::>");
																appendLookupValueToStagedFile(changed, crr.getTextContent(), nameNodeCurrent.getTextContent());
															}
														}
													}
												}
											}
										}
									}
								}
							}else if(ct != null && "CHANGED_CHILD".equals(ct.getTextContent())) {
								String container = json.getJSONObject("entityItemChange").getJSONObject("_container").getString("_externalId");
								container = container.substring(1, container.length() - 1);
								if("MATKLLOV".equals(container) || "MATKLLOV_S4H".equals(container)) {
									String lkpIdentifier = json.getJSONObject("entityItemChange").getString("_identifier");
									java.util.LinkedList<Node> referencesList = xmm.listImmediateChildElements(n).get("references");
									if(referencesList != null) {
										for(Node ref : referencesList) {
											ct = xmm.byName(ref, "_changeType");
											if(ct != null && "CHANGED".equals(ct.getTextContent())) {
												String lkpCode = xmm.byName( xmm.byName(xmm.byName(ref, "_qualification"), "refLookup" ), "_code").getTextContent();
												Node refLookupValues = xmm.byName(ref, "refLookupValues");
												if(refLookupValues != null) {
													java.util.LinkedList<Node> currentList = xmm.listImmediateChildElements(refLookupValues).get("_current");
													if(currentList != null) {
														java.util.LinkedList<String> vals = new java.util.LinkedList<>();
														for(Node current : currentList) {
															vals.addLast(xmm.byName(current, "_code").getTextContent());
														}
														java.util.Map<String, String> dataRef = readItemGroupRefValues(container);
														if(!vals.isEmpty()) {
															dataRef.put(lkpIdentifier + "<::>" + lkpCode, workshop.serializeChunk(vals.toArray(new String[] {}), "\"", ",", "\\"));
														}else {
															dataRef.remove(lkpIdentifier + "<::>" + lkpCode);
														}
														keepItemGroupRefValues(container, dataRef);
													}
												}
											}
										}
									}
								}else if("PartyClassification".equals(container)) {
									String lookupValueCode = json.getJSONObject("entityItemChange").getString("_identifier");
									if("Migrado".equals(lookupValueCode)) {
										java.util.LinkedList<Node> referencesList = xmm.listImmediateChildElements(n).get("references");
										if(referencesList != null) {
											for(Node ref : referencesList) {
												ct = xmm.byName(ref, "_changeType");
												if(ct != null && "CHANGED".equals(ct.getTextContent())) {
													if("Party".equals( xmm.byName( xmm.byName( xmm.byName(ref, "_qualification"), "refLookup"), "_code").getTextContent() )) {
														java.util.LinkedList<Node> currentNodes = xmm.listImmediateChildElements( xmm.byName(ref, "refLookupValues") ).get("_current");
														try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "proveedores_migrados").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
															if(currentNodes != null) {
																currentNodes.forEach(node -> pw.println( xmm.byName(node, "_code").getTextContent() ));
															}
														}catch(java.io.IOException e) {
															logE(e);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}else if(json.has("entityItemsDeleted")){
			org.json.JSONArray identifiers = json.getJSONObject("entityItemsDeleted").getJSONArray("_identifier");
			DataRequestor dr = new DataRequestor();
			org.json.JSONArray items = new org.json.JSONArray();
			String container = json.getJSONObject("entityItemsDeleted").getJSONObject("_container").getString("_externalId").replaceAll("'", "");
			if("GlobalTemplateAttributeConfiguration".equals(container) || "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla".equals(container)) {
				for(int i=0; i<identifiers.length(); i++) {
					items.put(new org.json.JSONObject().put("diccionario", container).put("idValor", identifiers.getString(i)));
				}
				String r = dr.getContenidoDeDiccionario(items);
				if(r != null) {
					org.json.JSONObject jr = new org.json.JSONObject(r);
					org.json.JSONArray is = jr.getJSONArray("items");
					org.json.JSONArray irs = new org.json.JSONArray();
					org.json.JSONObject ir = null;
					for(int m=0; m<is.length(); m++) {
						ir = is.getJSONObject(m);
						if(ir.has("structureGroup") && !"".equals(ir.getString("structureGroup")) && ir.has("characteristic") && !"".equals("characteristic") && ir.has("propertyShortCode") && !"".equals(ir.getString("propertyShortCode"))) {
							irs.put(new org.json.JSONObject().put("template", ir.getString("structureGroup")).put("characteristic", ir.getString("characteristic")).put("property", ir.getString("propertyShortCode")));
						}
					}
					if(irs.length() > 0) {
						log("On hierarchy delete: " + dr.removeTemplateCharacteristicMetaDataByProperty(irs));
					}
				}
				log(dr.removeContenidoDeDiccionario(items));
			}
			log("A message body: " + json);
		}
	}
	
	private void rewriteCacheFileContents(java.util.Map<String, String[]> data) {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.template_cache_filter_file")).toString())))){
			for(java.util.Map.Entry<String, String[]> entry : data.entrySet()) {
				String delim = "\"";
				String sep = ";";
				String esc = "\\";
				pw.println( workshop.serializeChunk(new String[] {entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]}, delim, sep, esc) );
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	private java.util.Map<String, String[]> readCacheFilterContents() {
		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths
				.get(
						PropertiesManager.get("p360.contingency.templates_cache_directory"), 
						PropertiesManager.get("p360.contingency.template_cache_filter_file")
				).toString())))){
			String line = null;
			String[] pieces = null;
			String delim = "\"";
			String sep = ";";
			String esc = "\\";
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				try {
					data.put(pieces[0], new String[] {pieces[1], pieces[2], pieces[3]});
				}catch(ArrayIndexOutOfBoundsException e) {
					log("Not able to read line properly. Skipping");
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return data;
	}
	
	private void keepLkpValues(String container, java.util.Map<String, String> data){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", container).toString())))){
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			for(java.util.Map.Entry<String, String> entry : data.entrySet() ) {
				pw.println(workshop.serializeChunk(new String[] { entry.getKey(), entry.getValue() }, delim, sep, escp));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private java.util.Map<String, String> readLkpValues(String container){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				if(pieces.length > 1)
					data.put(pieces[0], pieces[1]);
				else
					log("Malformed line -->" + line + "<--" );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	private void keepStdValues(String container, java.util.Map<String, String> data){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			for(java.util.Map.Entry<String, String> entry : data.entrySet() ) {
				pw.println(workshop.serializeChunk(new String[] { entry.getKey(), entry.getValue() }, delim, sep, escp));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private java.util.Map<String, String> readStdValues(String container){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ';', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, pieces -> {
			if(pieces.length > 1) {
				data.put(pieces[0], pieces[1]);
			}else {
				log("Malformed line in file " + container + ", please check correct format, skipping.");
			}
		});
		parser.parse(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container));
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
//			String line = null;
//			String delim = "\"";
//			String sep = ";";
//			String escp = "\\";
//			String[] pieces = null;
//			while((line = br.readLine()) != null) {
//				pieces = workshop.parseLine(line, delim, sep, escp);
//				if(pieces.length > 1) {
//					data.put(pieces[0], pieces[1]);
//				}else {
//					log("Malformed line in file " + container + ", please check correct format, skipping.");
//				}
//			}
//		}catch(java.io.IOException e) {
//			log("Problem with " + container);
//			logE(e);
//		}
		return data;
	}
	
	private org.json.JSONObject requestStandardizationValueObject(String internalId){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("items", internalId);
		qp.put("fields", 
					"StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
				+ ",StandardizationValue.Value"
			);
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/StandardizationValue/byItems", qp, null);
		if(response == null || !response.has("totalSize")) {
			log(workshop.getRawResponse());
		}
		return response;
	}
	
	private void keepItemGroupRefValues(String container, java.util.Map<String, String> data){
		if("MATKLLOV".equals(container) || "MATKLLOV_S4H".equals(container)) {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("MATKLLOV".equals(container) ? "p360.contingency.itemgroup_valid_values" : "p360.contingency.itemgroup_valid_values_s4h")).toString())))){
				String delim = "\"";
				String sep = ";";
				String escp = "\\";
				String[] pieces = null;
				for(java.util.Map.Entry<String, String> entry : data.entrySet() ) {
					pieces = entry.getKey().split( java.util.regex.Pattern.quote( "<::>" ));
					pw.println(workshop.serializeChunk(new String[] { pieces[0], pieces[1], entry.getValue() }, delim, sep, escp));
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private java.util.Map<String, String> readItemGroupRefValues(String container){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		if("MATKLLOV".equals(container) || "MATKLLOV_S4H".equals(container)) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("MATKLLOV".equals(container) ? "p360.contingency.itemgroup_valid_values" : "p360.contingency.itemgroup_valid_values_s4h")).toString())))){
				String line = null;
				String delim = "\"";
				String sep = ";";
				String escp = "\\";
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line, delim, sep, escp);
					if(pieces.length == 3) {
						data.put(pieces[0] + "<::>" + pieces[1], pieces[2]);
					}else {
						log("Malformed line -->" + line + "<-- (" + (java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("MATKLLOV".equals(container) ? "p360.contingency.itemgroup_valid_values" : "p360.contingency.itemgroup_valid_values_s4h").toString())) + ")");
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	private void appendLookupValueToStagedFile(String lkpId, String code, String label) {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), 
				"global_lookups",
				lkpId).toString(), true)))){
			String delim = "\"";
			String sep = ";";
			String esc = "\\";
			pw.println( workshop.serializeChunk(new String[] { code, label }, delim, sep, esc) );
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	private static java.util.Map<String, String> toMap(String[] arr){
		java.util.Map<String, String> data = new java.util.HashMap<>();
		String[] a = null;
		for(int i=0; i<arr.length; i++) {
			a = arr[i].split("\t");
			data.put(a[0], a[1]);
		}
		return data;
	}
	

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

	public void setProcedeNoProcedeArticle(String externalId) {
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
		qp.put("query", "Article.SupplierAID equals \"" + externalId + "\"");
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
	
	public void process() {
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
	public static final java.util.Map<String, String> data = toMap( ("ATG	atg\r\n"
			+ "ATGSection	atgSection\r\n"
			+ "AttributeHelpInformation	attributeHelpInformation\r\n"
			+ "BulletAttribute	isBulletAttribute\r\n"
			+ "Business	allowedBusiness\r\n"
			+ "CaptureLevel	captureLevel\r\n"
			+ "CreationType	Tipo de Creación			tipoDeCreacion\r\n"
			+ "DataType	dataType\r\n"
			+ "DefaultValue	defaultValue\r\n"
			+ "DependentAttribute	dependentAttribute\r\n"
			+ "DependentValues	dependentValues\r\n"
			+ "DescripcionLargaPlantilla	Descripción Larga Plantilla			descripcionLargaPlantilla\r\n"
			+ "DocumentoAyudaPlantilla	documentoDeAyudaparaLaPlantilla\r\n"
			+ "ECC	ecc\r\n"
			+ "ECC16	ecc16\r\n"
			+ "EsServicio	esUnServicio\r\n"
			+ "ExpressMandatory	Mandatorio en creación express			mandatorioEnCreacionExpress\r\n"
			+ "GuiaPlantilla	Guía Plantilla			guiaPlantilla\r\n"
			+ "isConfigurable	isConfigurable\r\n"
			+ "IsEditable	isEditable\r\n"
			+ "IsFaceted	filtrableATG\r\n"
			+ "IsMandatory	isMandatory\r\n"
			+ "IsMultiselect	isMultiselect\r\n"
			+ "ListOfValues	listofValues\r\n"
			+ "ListOfValuesFilter	listofValuesValidValues\r\n"
			+ "Max	max\r\n"
			+ "MaxLength	maxLength\r\n"
			+ "Min	min\r\n"
			+ "NameEnglish	Name (English)			nameEnglish\r\n"
			+ "NumeroVersion	templateVersion\r\n"
			+ "OtroDato	otroAtributo\r\n"
			+ "PIM	pim\r\n"
			+ "PlaceholderMandatory	mandatorioEnPlaceholder\r\n"
			+ "ReglaTituloSAP	Reglas creación Titulo SAP?			reglasCreacionTituloSAP\r\n"
			+ "RelevantForATG	relevantForATG\r\n"
			+ "RequiereRepoblamiento	requiereRepoblamento\r\n"
			+ "S4H	s4h\r\n"
			+ "SentToVendorCenter	senttoVendorCenter\r\n"
			+ "Twins	twins\r\n"
			+ "UsedByAI	usedbyAI\r\n"
			+ "ValidationOrCalculationRule	validationorCalculusRule\r\n"
			+ "VariantLevel	variantLevel\r\n"
			+ "VendorCenterSection	vendorCenterSection\r\n"
			+ "VendorCenterSectionSequence	vendorCenterSectionSequence").split("\\r\\n") );



	private static final Logger LOGGER = Logger.getLogger(LookupsAndDictionariesProcessor.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp/lookupsAndDictionariesChange/LADactiveMQListener-%g.log", 25 * 1024 * 1024, 10, true);
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
//                new java.io.FileOutputStream("../logs/LADactiveMQListener.log", true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "]  " + message);
//        } catch (java.io.IOException e) {
//        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/LADactiveMQListener.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

}
