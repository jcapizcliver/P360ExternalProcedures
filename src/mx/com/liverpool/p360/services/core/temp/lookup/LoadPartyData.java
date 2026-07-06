package mx.com.liverpool.p360.services.core.temp.lookup;

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

public class LoadPartyData {

	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		String path = java.nio.file.Paths.get("C:","opt","LVP","desorden","Proveedores Raiz PRO_210825.xml").toString();
//		String path = java.nio.file.Paths.get("C:","opt","LVP","desorden","Proveedores Raiz.xml").toString();
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
//		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		XMLMisc xmm = rw.getXmm();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
    	if(path != null) {
    		doc = builder.parse( path.toString() );
    	}else {
    		doc = null;
    		System.out.println("No path, no byte array provided");
    		return;
    	}
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		java.util.LinkedList<Node> classificationNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Classifications").getFirst()).get("Classification");
		Element el = null;
		String name = null;
		Node metaData = null;
		java.util.LinkedList<Node> metaDataNodeList = null;
		java.util.LinkedList<Node> valueNodeList = null;
		java.util.LinkedList<Node> multiValueNodeList = null;
		java.util.LinkedList<Node> attributeLinkNodeList = null;
		java.util.LinkedList<Node> valueFilterNodeList = null;
		org.json.JSONArray negocios = new org.json.JSONArray();
		org.json.JSONArray tipoProveedorSAP = new org.json.JSONArray();
		org.json.JSONArray tipoDeProveedor = new org.json.JSONArray();
		org.json.JSONArray matkllov = new org.json.JSONArray();
		org.json.JSONArray zcomalov = new org.json.JSONArray();
		org.json.JSONArray matkllovS4h = new org.json.JSONArray();
		org.json.JSONArray brandNameS4h = new org.json.JSONArray();
		Element nameElement = null;
		java.util.Map<String, java.util.LinkedList<Node>> nodeListMap = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject response = null;
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('TipoDeProveedorLOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('TipoProveedorSAPAttLOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV_S4H')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BRAND_IDLOV_S4H')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BusinessQualified')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		request.put("columns", columns);
		request.put("rows", rows);
		GeneralOperations go = new GeneralOperations();
		System.out.println("Collecting lkp values...");
		java.util.Map<String, String> zcomalovData = go.collectLookupValueData(rw, "ZCOMALOV");
		System.out.println("Done with zcomalov");
		java.util.Map<String, String> matkllovData = go.collectLookupValueData(rw, "MATKLLOV");
		System.out.println("Done with matkllov");
		java.util.Map<String, String> brandIdLoVData = go.collectLookupValueData(rw, "BRAND_IDLOV_S4H");
		System.out.println("Done with brand_idlov_s4h");
		java.util.Map<String, String> matkllovS4hData = go.collectLookupValueData(rw, "MATKLLOV_S4H");
		System.out.println("Done with matkllov_s4h");
		java.util.LinkedList<String> sups = new java.util.LinkedList<>();
		if(classificationNodeList != null) {
			for(Node n : classificationNodeList) {
				el = (Element)n;
				if(!"".equals(el.getAttribute("ID").replaceAll("-.+", "")) && el.getAttribute("ID").endsWith("-Supplier") ) {
					if(!el.getAttribute("ID").startsWith("146779-"))
						continue;
					nameElement = (Element) xmm.byName(el, "Name");
					name = nameElement != null ? nameElement.getTextContent() : null;
					sups.addLast(el.getAttribute("ID").replaceAll("-.+", ""));
					nodeListMap = xmm.listImmediateChildElements(el);
					metaDataNodeList = nodeListMap.get("MetaData");
					if(metaDataNodeList != null && !metaDataNodeList.isEmpty()) {
						metaData = metaDataNodeList.getFirst();
						valueNodeList = xmm.listImmediateChildElements(metaData).get("Value");
						multiValueNodeList = xmm.listImmediateChildElements(metaData).get("MultiValue");
						if(valueNodeList != null && !valueNodeList.isEmpty()) {
							for(Node nv : valueNodeList) {
								if("EmailProveedor".equals(((Element)nv).getAttribute("AttributeID"))) {
									
								}else if("TipoDeProveedor".equals(((Element)nv).getAttribute("AttributeID"))) {
									if(!"".equals(((Element)nv).getAttribute("ID")))
										tipoDeProveedor.put( ((Element)nv).getAttribute("ID") );
								}else if("TipoProveedorSAP".equals(((Element)nv).getAttribute("AttributeID"))) {
									if(!"".equals(((Element)nv).getAttribute("ID")))
										tipoProveedorSAP.put( ((Element)nv).getAttribute("ID") );
								}
							}
						}
						if(multiValueNodeList != null && !multiValueNodeList.isEmpty()) {
							for(Node nv : multiValueNodeList) {
								if("NegociosProveedor".equals(((Element)nv).getAttribute("AttributeID"))){
									valueNodeList = xmm.listImmediateChildElements(nv).get("Value");
									for(Node v : valueNodeList) {
										negocios.put(changeBusinessKey( ((Element)v).getAttribute("ID")) );
									}
								}
							}
						}
					}
					attributeLinkNodeList = nodeListMap.get("AttributeLink");
					if(attributeLinkNodeList != null) {
						for(Node nv : attributeLinkNodeList) {
							valueFilterNodeList = xmm.listImmediateChildElements(nv).get("ValueFilter");
							if(valueFilterNodeList != null && !valueFilterNodeList.isEmpty()) {
								valueNodeList = xmm.listImmediateChildElements(valueFilterNodeList.getFirst()).get("Value");
								for(Node v : valueNodeList) {
									if("ItemGroupS4H".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && matkllovS4hData.containsKey(((Element)v).getAttribute("ID")))
											matkllovS4h.put( ((Element)v).getAttribute("ID") );
									}else if("ItemGroup".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && matkllovData.containsKey(((Element)v).getAttribute("ID")))
											matkllov.put( ((Element)v).getAttribute("ID") );
									}else if("BRAND_ID_S4H".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && brandIdLoVData.containsKey(((Element)v).getAttribute("ID")))
											brandNameS4h.put( ((Element)v).getAttribute("ID") );
									}else if("BrandName".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && zcomalovData.containsKey(((Element)v).getAttribute("ID")))
											zcomalov.put( ((Element)v).getAttribute("ID") );
									}
								}
							}
						}
					}
					if("3057659".equals(el.getAttribute("ID").replaceAll("-.+", ""))) {
						System.out.println("--->" + matkllov + "<---");
//						response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
//						if(response != null) {
//							if(response.getJSONObject("counters").getInt("objectsWithErrors") > 0) {
//								response.remove("counters");
//								System.out.println(response);
//							}else {
//								System.out.println(response.getJSONObject("counters"));
//							}
//						}else {
//							System.out.println("Error: " + rw.getRawResponse());
//						}
//						rows.put(new org.json.JSONObject()
//								.put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID").replaceAll("-.+", "") + "'@'Party'"))
//								.put("values", new org.json.JSONArray()
//										.put(tipoDeProveedor)
//										.put(tipoProveedorSAP)
//										.put(matkllov)
//										.put(matkllovS4h)
//										.put(zcomalov)
//										.put(brandNameS4h)
//										.put(negocios)
//										.put(name == null ? "" : name)
//										.put(true)
//									)
//							);
//						while(rows.length() > 0) {
//							rows.remove(0);
//						}
//						System.exit(0);
					}
					rows.put(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID").replaceAll("-.+", "") + "'@'Party'"))
							.put("values", new org.json.JSONArray()
									.put(tipoDeProveedor)
									.put(tipoProveedorSAP)
									.put(matkllov)
									.put(matkllovS4h)
									.put(zcomalov)
									.put(brandNameS4h)
									.put(negocios)
									.put(name == null ? "" : name)
									.put(true)
								)
						);
					tipoDeProveedor = new org.json.JSONArray();
					tipoProveedorSAP = new org.json.JSONArray();
					matkllov = new org.json.JSONArray();
					matkllovS4h = new org.json.JSONArray();
					zcomalov = new org.json.JSONArray();
					brandNameS4h = new org.json.JSONArray();
					negocios = new org.json.JSONArray();
					if(rows.length() == 10) {
						response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
						if(response != null) {
							if(response.getJSONObject("counters").getInt("objectsWithErrors") > 0) {
								response.remove("counters");
								System.out.println(response);
							}else {
								System.out.println(response.getJSONObject("counters"));
							}
						}else {
							System.out.println("Error: " + rw.getRawResponse());
						}
						while(rows.length() > 0) {
							rows.remove(0);
						}
//						sups.forEach(System.out::println);
//						System.out.println("****");
//						sups.clear();
					}
				}
			}
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				if(response != null) {
					System.out.println(response.getJSONObject("counters"));
				}else {
					System.out.println("Error: " + rw.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		
	}
	
	private static String changeBusinessKey(String key) {
		return "LIVERPOOL".equals(key) ? "LVP" : "SUBURBIA".equals(key) ? "SBB" : "ART. MARKETPLACE".equals(key) ? "MKP" : key;
	}
	
}
