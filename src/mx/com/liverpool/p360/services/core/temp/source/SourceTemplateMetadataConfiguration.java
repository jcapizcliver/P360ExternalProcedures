package mx.com.liverpool.p360.services.core.temp.source;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class SourceTemplateMetadataConfiguration {

	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		XMLMisc xmm = rw.getXmm();
		java.util.Map<String, String> emptyqp = new java.util.TreeMap<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder;
    	String templateId = null;
    	String attributeId = null;
    	String baseUrl = PropertiesManager.get("p360.contingency.base_url"); // "https://webctep360pro.liverpool.com.mx/rest/V2.0";
    	String dictionary = "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla";
    	String dictionaryGlobal = "GlobalTemplateAttributeConfiguration";
    	StringBuilder sb = new StringBuilder();
    	GeneralOperations go = new GeneralOperations();
    	java.util.Map<String, org.json.JSONArray> data = null;
    	java.util.Map<String, org.json.JSONObject> truData = null;
    	java.util.Map<String, String> eccMappings = go.collectCharacteristicAlternativeIdentifier(rw, "ECC");
    	java.util.Map<String, String> s4hMappings = go.collectCharacteristicAlternativeIdentifier(rw, "S4HANA");
    	java.util.Set<String> activeCharacteristics = go.listActiveBaseCharacteristics(rw);
    	java.util.Set<String> global = go.collectDistinctCharacteristicInDictionary(rw, "GlobalTemplateAttributeConfiguration", 
    			"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
    			+ " and StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\""
    			);
    	/*
    	java.util.Set<String> logistico = go.collectDistinctCharacteristicInDictionary(baseUrl, "GlobalTemplateAttributeConfiguration", 
    			"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
    			+ " and StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\""
    			+ " and StandardizationValue.PropertyValue equals \"Datos Logísticos\"");
    	java.util.Set<String> deVenta = go.collectDistinctCharacteristicInDictionary(baseUrl, "GlobalTemplateAttributeConfiguration", 
    			"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
    			+ " and StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\""
    			+ " and StandardizationValue.PropertyValue equals \"Datos de Venta\"");
    	java.util.Set<String> header =  go.collectDistinctCharacteristicInDictionary(baseUrl, "GlobalTemplateAttributeConfiguration", 
    			"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
    			+ " and StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\""
    			+ " and StandardizationValue.PropertyValue equals \"Header\"");
    	java.util.Set<String> basic =  go.collectDistinctCharacteristicInDictionary(baseUrl, "GlobalTemplateAttributeConfiguration", 
    			"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
    			+ " and StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\""
    			+ " and StandardizationValue.PropertyValue equals \"Datos Básicos\"");
    	*/
//    	System.out.println("Logistic: " + logistico);
//    	System.out.println("De Ven: " + deVenta);
//    	System.out.println("Header: " + header);
//    	System.out.println("Basic: " + basic);
//    	System.exit(0);
    	java.util.Map<String, java.util.Map<String, org.json.JSONObject>> currentMetadata = new java.util.TreeMap<>();
    	System.out.println("Collecting reference data...");
    	java.util.Map<String, org.json.JSONObject> fieldCompositionLibrary = 
//    			null; 
    	 go.collectAttributeDefinitions(new RESTWorkshop(), dictionary);
    	java.util.Map<String, org.json.JSONObject> fieldCompositionLibraryGlobal = 
//    			null; 
    	go.collectAttributeDefinitions(rw, dictionaryGlobal);
    	System.out.println("Done collecting, now performing...");
    	org.json.JSONObject objectReference = null;
    	org.json.JSONObject baseDefinition = null;
    	boolean mandatory = false;
    	StringBuilder business = new StringBuilder();
    	System.out.println("Loading current template characteristic properties...");
    	java.util.Map<String, String> currentTemplateCharProper = 
//    			null; 
    	 go.loadTemplateCharProperties(rw, dictionary);
    	System.out.println("Done loading current template characteristic properties...");
    	org.json.JSONObject request = new org.json.JSONObject();
    	org.json.JSONArray columns = new org.json.JSONArray();
    	org.json.JSONArray rows = new org.json.JSONArray();
    	org.json.JSONObject response = null;
    	String currId = null;
    	request.put("columns", columns);
    	request.put("rows", rows);
    	columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
    	columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
    	columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
    	columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
    	columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
    	rw.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
    	rw.setBaseUrl(baseUrl);
    	
    	String relAtg = null;
    	Element relAtgEl = null;
    	String name = null;
    	Element nameElement = null;
    	java.util.LinkedList<String> templeits = new java.util.LinkedList<>();
		try (
			java.io.PrintWriter pw 
				= new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "log3").toString())))
//				= new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "log2").toString())))
//				= new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "log").toString())))
		){
			builder = factory.newDocumentBuilder();
			Document doc = null;
			int daCount = 0;
			java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "plantillas").toString() ).listFiles(ff -> ff.getName().startsWith("Plantillas Fase 2 Lvl4.xml"));
//			java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "plantillas").toString() ).listFiles(ff -> ff.getName().startsWith("step-12216846557667656844-exported.xml"));
//			java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "plantillas").toString() ).listFiles(ff -> ff.getName().startsWith("plantillas 56.xml"));
//			java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:","opt", "LVP", "tmp").toString() ).listFiles(ff -> ff.getName().startsWith("step-5976648975116505142-exported"));
//			java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:","opt", "LVP", "tmp").toString() ).listFiles(ff -> ff.getName().startsWith("step-15678558410167377171-exported"));
//			java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "muestras_plantillas").toString() ).listFiles(ff -> ff.getName().startsWith("step-"));
//			java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:\\","opt", "LVP", "desorden", "muestras_plantillas").toString() ).listFiles(ff -> ff.getName().startsWith("PPH_"));
			for(java.io.File fl : fls) {
				try{
					doc = builder.parse( new java.io.FileInputStream( fl ) );
				}catch(java.io.IOException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}
				if(doc != null) {
					doc.getDocumentElement().normalize();
					Element rootElement = doc.getDocumentElement();
					java.util.LinkedList<Node> productNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Products").getFirst()).get("Product");
					java.util.LinkedList<Node> attributeLinkList = null;
					java.util.LinkedList<Node> valueFilterList = null;
					java.util.LinkedList<Node> valueList = null;
					java.util.LinkedList<Node> metaData = null;
					java.util.LinkedList<Node> metaDataValue = null;
					org.json.JSONObject metadata = null;
					Element el = null;
					for(Node n : productNodeList) {
						templateId = ((Element)n).getAttribute("ID");
						nameElement = (Element) xmm.byName(n, "Name");
						if(nameElement != null) {
							name = nameElement.getTextContent();
//							rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray().put(name)));
						}else {
							continue;
						}
//						if(1 == 1) {
//							continue;
//						}
//						System.out.println(templateId);
//						if("EU4-82661701".equals(templateId)) {
//							System.out.println("Heare...");
//						}
						daCount++;
						templeits.addLast(templateId);
						truData = currentMetadata.get(templateId);
						if(truData == null) {
							data = go.gatherTemplateMetaData(rw, dictionary, templateId);
							truData = go.parseToProperties(data);
							currentMetadata.put(templateId, truData);
						}
						attributeLinkList = xmm.listImmediateChildElements(n).get("AttributeLink");
						if(attributeLinkList != null) {
							for(Node an : attributeLinkList) {
								attributeId = ((Element)an).getAttribute("AttributeID");
								if(global.contains(attributeId)) {
									continue;
								}
								mandatory = ((Element)an).hasAttribute("Mandatory") && Boolean.parseBoolean(((Element)an).getAttribute("Mandatory"));
								metadata = truData.get(attributeId);
								valueFilterList = xmm.listImmediateChildElements( an ).get("ValueFilter");
								metaData = xmm.listImmediateChildElements( an ).get("MetaData");
								if(metaData != null && !metaData.isEmpty()) {
									relAtgEl = (Element) xmm.byAttributeValue(metaData.getFirst(), "AttributeID", "RelevantForATG");
									if(relAtgEl != null) {
										relAtg = relAtgEl.getAttribute("ID");
//										System.out.println("Retrieved... ***********");
									}else {
										relAtg = null;
									}
								}else {
									relAtg = null;
								}
								if(valueFilterList != null && !valueFilterList.isEmpty()) {
									if(metadata != null) {
									}else {
										valueList = xmm.listImmediateChildElements(valueFilterList.getFirst()).get("Value");
										for(Node vn : valueList) {
											el = (Element) vn;
											sb.append(sb.length() == 0 ? "" : ",");
											sb.append(rw.serializeLine( el.hasAttribute("ID") ? el.getAttribute("ID") : el.getTextContent() ));
										}
									}
								}else {
//									System.out.println("No value filter for " + attributeId);
								}
								baseDefinition = new org.json.JSONObject();
								objectReference = fieldCompositionLibrary.get(attributeId);
								if(objectReference == null) {
									objectReference = fieldCompositionLibraryGlobal.get(attributeId);
									if(objectReference == null) {
										if(!activeCharacteristics.contains(attributeId)) {
//											System.out.println("\tNot a known characteristic: " + attributeId);
										}else {
											if(eccMappings.containsKey(attributeId)) {
												business.append("Liverpool");
											}
											if(s4hMappings.containsKey(attributeId)) {
												business.append( business.length() > 0 ? " " : "" ).append("Suburbia");
											}
											if(business.length() > 0) {
												baseDefinition.put("Business", business.toString());
												business.setLength(0);
											}else {
												baseDefinition.put("Business", "Liverpool Suburbia Marketplace");
											}
											baseDefinition.put("VendorCenterSection", "ColoursLiverpoolAtt".equals(attributeId) || "TamanoUnico".equals(attributeId) ? "Producto" : "Atributos");
											baseDefinition.put("SentToVendorCenter", "1");
											baseDefinition.put("IsEditable", "1");
											if(sb.length() > 0) {
												baseDefinition.put("ListOfValuesFilter", sb.toString());
											}
											baseDefinition.put("IsMandatory", mandatory);
										}
									}
								}else {
								}
								if(objectReference != null) {
									try {
										baseDefinition.put("Business", objectReference.get("Business"));
										baseDefinition.put("VendorCenterSection", objectReference.get("VendorCenterSection"));
										baseDefinition.put("SentToVendorCenter", objectReference.get("SentToVendorCenter"));
										if(objectReference.has("RelevantForATG")) {
											baseDefinition.put("RelevantForATG", objectReference.get("RelevantForATG"));
										}
										if(objectReference.has("IsEditable")) {
											baseDefinition.put("IsEditable", objectReference.get("IsEditable"));
										}
										if(objectReference.has("Max")) {
											baseDefinition.put("Max", objectReference.get("Max"));
										}
										if(objectReference.has("Min")) {
											baseDefinition.put("Min", objectReference.get("Min"));
										}
										if(objectReference.has("MaxLength")) {
											baseDefinition.put("MaxLength", objectReference.get("MaxLength"));
										}else {
											if(objectReference.has("DataType")) {
												baseDefinition.put("MaxLength", "100");
											}
										}
										if(!baseDefinition.has("RelevantForATG") && relAtg != null) {
											baseDefinition.put("RelevantForATG", relAtg);
										}
										baseDefinition.put("IsMandatory", mandatory);
										if(sb.length() > 0) {
											baseDefinition.put("ListOfValuesFilter", sb.toString());
										}
									}catch(org.json.JSONException e) {
										e.printStackTrace();
									}
								}else {
//									System.out.println("No definition for: " + attributeId);
								}
								if(baseDefinition != null && baseDefinition.length() > 0) {
//									System.out.println("Using base Definition (" + attributeId + "): "  + baseDefinition);
									for(String nm : org.json.JSONObject.getNames(baseDefinition)) {
										currId = currentTemplateCharProper.get(templateId + "<::>" + attributeId + "<::>" + nm);
										currId = currId == null ? templateId + "<::>" + attributeId + "<::>" + nm : currId;
										try {
										rows.put(new org.json.JSONObject()
												.put("object", new org.json.JSONObject()
														.put("id", "'" + currId.replaceAll("'", "\\\'") + "'@'" + dictionary + "'"))
												.put("values", new org.json.JSONArray()
														.put(templateId)
														.put(attributeId)
														.put("CreateProposal")
														.put(nm)
														.put(baseDefinition.get(nm))));
										}catch(org.json.JSONException e) {
											e.printStackTrace();
											System.out.println(nm + " || " + baseDefinition);
											System.exit(0);
										}
										System.out.println("Adding " + nm + " - " + baseDefinition.get(nm));
										System.out.println("Template added: " + templateId);
										if(rows.length() == 100) {
											response = rw.makeRequest("POST", "/list/StandardizationValue", emptyqp, request.toString());
											System.out.println( response == null ? "ERROR: " + rw.getRawResponse() : response );
											while(rows.length() > 0) {
												rows.remove(0);
											}
										}
									}
								}
								sb.setLength(0);
							}
						}
					}
					if(rows.length() > 0) {
						response = rw.makeRequest("POST", "/list/StandardizationValue", emptyqp, request.toString());
						System.out.println( response == null ? "ERROR: " + rw.getRawResponse() : response );
						while(rows.length() > 0) {
							rows.remove(0);
						}
						java.util.Map<String, String> qp = new java.util.TreeMap<>();
						rw.makeRequest("POST", "/list/LookupValue/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))).put("rows", rows).toString());
						System.out.println(rw.getRawResponse());
					}
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Processed templates:");
		for(String tm : templeits) {
			System.out.println(tm);
		}
		System.out.println(templeits.size());
	}
	
}
