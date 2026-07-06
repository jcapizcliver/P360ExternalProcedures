package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ImportTemplateCharacteristicsFilter {

	
	private static String[] hola1 = ("2XCH,26-25,26X29,31-32,27-32,3X,25-26,2XCH/30,UNITALLA,29X28,SIN TAMAÑO,XG/28,2XCH/32,17,32X30,27-34,31-27,XXCH,42-32,8M,XG/30,26-29,30-27,38X,0 R,12 C,27-27,31X30,31X32,11M,M/28,26,31X28,2 L,30-25,2XCH/CH,29-32,33-32,32X32,25X28,31-34,26-28,12 AÑOS,29X32,30,31-26,16M,M/32,0X,8,28-29,40-32,11,36,29-30,31,30X30,36-32,29-29,G/30,M,10 C,28X31,38,6 C,2,10M,27X32,10,22M,5,14M,34-32,28X28,27X30,6 M,31-28,5M,31X29,7,25X30,4XG,33-28,2XCH/34,00 C,CH/34,33,36X,2XG/32,33-29,27-25,0,12 R,12M,38-30,50,16 C,G,24-30,46-32,25-31,27 R,G-XG,29-34,24-29,32-32,27-28,4 L,20 C,25X32,27-30,24-25,26X28,10 R,13,12,28 R,32-27,1 R,23-32,2 M,22,15 R,33-27,13 R,1 C,21,40-30,28-25,2 R,18,2XG/28,XCH,30-28,29X31,30-29,6 R,30X28,9,29-33,M/30,32-34,26X32,25,32-26,4X,44-32,2 C,7 R,XCH/28,42,25-32,24 R,3,7 M,26-27,31-30,30X32,3 C,6P,28-32,27X28,28X29,32,40,27,3 R,2P,23-30,12 L,33-30,28-28,30-34,XCH/32,27X29,30-26,1X,14,00,24-27,CH/30,10 L,4 M,28X30,28X27,48,10P,32-29,1 M,31-31,26X30,33-31,XG,24,7 C,19,31 R,31X31,34-30,28-31,25-30,30X29,27-29,18R,32-28,30-31,33X32,1,20R,29,12P,52,56,28-30,25 R,18 C,38-32,XXG,3XG,CH-M,25-25,28X32,58,2XG/34,28-26,29X30,11 R,16,20M,23,9 C,32-31,XCH-CH,00 R,34,24-28,6 L,5 C,EG,42-30,4 R,15,25-29,26-34,XG/32,28-34,29X29,27X31,29 R,3M,0 C,14 C,46,27-31,26X31,20,48-32,54,CH/28,28-27,29-31,29-28,CH,44,5 R,24-31,6,18M,26-30,14P,25-28,26 R,25-27,29-27,8 C,M/34,14 R,26-26,XCH/30,14 L,24M,9 R,30X31,27X27,CH/32,16R,26-31,33-33,9 M,30-30,2X,33-34,2XG,2XG/30,4,30 R,26-32,G/32,32-30,XCH/34,36-30,8 R,M-G,4 C,24-32,30-32,4P,31-29,28,G/34,8P").split(",");
	private static String[] hola2 = ("9 C,25-27,30-32,24-30,26X28,2XG,00 C,4X,20 C,38X,G/30,10P,30-27,20,33-27,31,00,3 C,1 C,26-25,33X32,30-30,28-30,58,18,27X31,27-25,24-29,16R,38-32,31-30,14 C,27-27,33-33,26X29,24 R,34-32,4P,6,18M,29,27-34,36-32,26X30,2XG/28,32-27,20R,31-34,31-27,7,12,14 R,2 M,52,31X32,XXCH,G-XG,29-31,24,2XG/30,44-32,XCH/32,32-31,12M,28X31,29X30,2 C,19,33-28,31-26,26-31,28-26,2XCH,M/30,29-29,26-29,32,25 R,25,4,28X28,SIN TAMAÑO,16M,27X29,14,30-26,26-27,40-32,30X28,15 R,4 M,25-32,30-34,8M,26-32,16,48-32,40-30,42-32,6P,6 R,33-32,31-32,33-30,18R,5 R,25X30,3X,27X27,8P,31-28,11,8 R,10M,CH,2X,9,13,24-31,4 C,31X30,5M,10,EG,0 C,15,44,XCH/28,18 C,28-27,36,28-32,CH-M,XCH-CH,30,27X30,12 C,12 AÑOS,29X29,27-31,28-31,6 C,36X,25-26,25-28,10 C,24-27,28X30,16 C,XCH,30X30,36-30,4 R,XG,XXG,4XG,9 M,29-28,48,3 R,32-32,23,3M,2XG/34,26-28,24-28,27X28,1X,14P,2 R,26-26,33-29,31X31,28X32,38,8 C,12P,29-27,3XG,2XCH/CH,26,30-31,XG/30,56,28 R,23-32,9 R,42,29-34,29X32,54,32-30,M-G,14 L,G/32,32X30,10 L,25-29,34,27,31X29,28-34,1 R,M/28,M/34,32-29,25-25,30X31,G/34,29X31,31-29,2P,31X28,2 L,33-34,2XG/32,32-28,32X32,28X29,27-32,29-30,24-32,6 M,34-30,24-25,26X31,8,11 R,2XCH/32,2,6 L,27 R,0 R,7 M,28-29,27-28,20M,5,25X28,31-31,23-30,28-28,13 R,1 M,22M,25-31,32-26,5 C,21,CH/30,24M,28X27,CH/28,0X,30-25,10 R,4 L,27-30,G,22,27-29,40,7 R,XG/32,26X32,2XCH/34,14M,38-30,28-25,CH/34,30 R,M/32,2XCH/30,26-30,30X32,17,12 R,29-33,1,31 R,0,30-29,26-34,XG/28,33-31,50,25-30,32-34,29X28,30X29,25X32,29 R,XCH/34,3,30-28,00 R,42-30,46-32,29-32,27X32,7 C,M,UNITALLA,46,11M,XCH/30,33,28,26 R,CH/32,12 L,2R,16 R,18 R,0 R,12 R").split(",");

	
	public static void main3(String[] args) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Accept: application/json", "Content-Type: application/x-www-form-urlencoded", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
				+ "StandardizationValue.Value wildcard \"%<::>TamanoUnico\" and StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\"");
		rw.putParameter("fields", "StandardizationValue.Value");
		rw.putParameter("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		rw.makeRequest("DELETE", "/list/StandardizationValue/bySearch");
		System.out.println(rw.getRawResponse());
	}
	
	public static void main2(String[] args) {
		java.util.List<String> setInFile = java.util.Arrays.asList(hola1);
		java.util.List<String> setInP360 = java.util.Arrays.asList(hola2);
		System.out.println("********** Now in file that are not in P360");
		for(String a : hola1) {
			if(!setInP360.contains(a)) {
				System.out.println(a);
			}
		}
		System.out.println("********** Now in P360 that are not in file");
		for(String a : hola2) {
			if(!setInFile.contains(a)) {
				System.out.println(a);
			}
		}
	}
	
	private static void collectCurrentFilters(java.util.Map<String, String> local, java.util.Map<String, String> global){
    	String dictionary = "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla";
    	String dictionaryGlobal = "GlobalTemplateAttributeConfiguration";
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", "Basic cmVzdDozVnVzJDl4MUU4bSQ=");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				    "StandardizationValue.Value"
				  + ",StandardizationValue.AlternativeValue"
				  + ",StandardizationValue.StructureGroup->LookupValue.Code"
				  + ",StandardizationValue.Characteristic->Characteristic.Identifier"
				  + ",StandardizationValue.Property->LookupValue.Code"
				  + ",StandardizationValue.PropertyValue"
				);
		qp.put("query", "StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\" and StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dictionary + "\"");
		qp.put("dictionaryProxy", "'" + dictionary + "'");
		qp.put("pageSize", "1200");
		int a = 0;
		int b = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null && response.has("rows")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!"".equals(values.getString(3))) {
						local.put(values.getString(2) + "<::>" + values.getString(3), values.getString(0));
						System.out.println(values.getString(2) + "<::" + values.getString(3));
					}
				}
				a += response.getInt("pageSize");
			}
		}while(a < b);
		a = 0;
		qp.put("query", "StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\" and StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dictionaryGlobal + "\"");
//		qp.put("dictionaryProxy", "'" + dictionaryGlobal + "'");
//		do {
//			qp.put("startIndex", String.valueOf(a));
//			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
//			if(response != null && response.has("rows")) {
//				b = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					global.put(values.getString(3), values.getString(0));
//				}
//				a += response.getInt("pageSize");
//			}
//		}while(a < b);
		a = 0;
	}
	
	
	public static void main(String[] args) {
		java.io.File[] fls = new java.io.File( java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "plantillas").toString() ).listFiles(ff -> ff.getName().startsWith("step-12216846557667656844-exported.xml"));
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", "Basic cmVzdDozVnVzJDl4MUU4bSQ=");
		org.json.JSONObject response = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		request.put("columns", columns);
		request.put("rows", rows);
		XMLMisc xmm = rw.getXmm();
		java.util.Map<String, String> emptyqp = new java.util.TreeMap<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder;
    	java.util.Map<String, String> filtrosActuales = new java.util.TreeMap<>();
//    	collectCurrentFilters(filtrosActuales, new java.util.TreeMap<>());
    	String currentId = null;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = null;
	    	try{
				doc = builder.parse( new java.io.FileInputStream( fls[0] ) );
			}catch(java.io.IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
			if(doc != null) {
				doc.getDocumentElement().normalize();
				Element rootElement = doc.getDocumentElement();
				StringBuilder sb = new StringBuilder();
				java.util.LinkedList<Node> productNodeList1 = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Products").getFirst()).get("Product");
				for(Node pn1 : productNodeList1) {
					java.util.LinkedList<Node> productNodeList2  = xmm.listImmediateChildElements( pn1 ).get("Product");
					for(Node pn2 : productNodeList2) {
						java.util.LinkedList<Node> productNodeList3  = xmm.listImmediateChildElements( pn2 ).get("Product");
						for(Node pn3 : productNodeList3) {
							java.util.LinkedList<Node> productNodeList4  = xmm.listImmediateChildElements( pn3 ).get("Product");
							for(Node pn4 : productNodeList4) {
//								System.out.println(((Element)pn4).getAttribute("ID"));
								java.util.LinkedList<Node> attributeLink = xmm.listImmediateChildElements( pn4 ).get("AttributeLink");
								if(attributeLink != null) {
									for(Node at : attributeLink) {
										java.util.LinkedList<Node> valueFilter = xmm.listImmediateChildElements( at ).get("ValueFilter");
										if(valueFilter != null) {
											sb.setLength(0);
//											System.out.println("\t" + ((Element)at).getAttribute("AttributeID"));
											java.util.LinkedList<Node> values = xmm.listImmediateChildElements( valueFilter.getFirst() ).get("Value");
											if(values != null) {
												for(Node vn : values) {
													Element el = (Element)vn;
													sb.append(sb.length() == 0 ? "" : ",").append(el.hasAttribute("ID") ? el.getAttribute("ID") : el.getTextContent());
												}
											}
												
											if(!("EU4-2389484".equals(((Element)pn4).getAttribute("ID")) && "TamanoUnico".equals(((Element)at).getAttribute("AttributeID"))) /* && sb.toString().contains("\"") */ ) {
//												System.out.println("\t\t" + sb.toString());
												currentId = filtrosActuales.get(((Element)pn4).getAttribute("ID") + "<::>" + ((Element)at).getAttribute("AttributeID"));
												if(currentId == null) {
													currentId = ((Element)pn4).getAttribute("ID") + "<::>" + ((Element)at).getAttribute("AttributeID") + "<::>ListOfValuesFilter";
												}
												org.json.JSONObject o = null;
												rows.put(o = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + currentId + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(((Element)pn4).getAttribute("ID")).put(((Element)at).getAttribute("AttributeID")).put("CreateProposal").put("ListOfValuesFilter").put(sb.toString())));
//												System.out.println(o);
												if(rows.length() == 100) {
													response = rw.makeRequest("POST", "/list/StandardizationValue", emptyqp, request.toString());
													System.out.println(response);
													while(rows.length() > 0) {
														rows.remove(0);
													}
//													System.exit(0);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if(rows.length() > 0) {
					response = rw.makeRequest("POST", "/list/StandardizationValue", emptyqp, request.toString());
					System.out.println(response);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}	
}
