package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.jcapiz.memelos.misc.RestClient;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class EpaleLasWeb {

	private static XMLMisc xmm = new XMLMisc();
	private static final String baseUrl = 
//			"https://webctep360pro.liverpool.com.mx/rest/V2.0";
			"https://webctep360qas.liverpool.com.mx/rest/V2.0";
//			"https://webctep360dev.liverpool.com.mx/rest/V2.0";
//	private static final String encoded = "cmVzdDozVnVzJDl4MUU4bSQ=";
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded);

	private static boolean write = true;
	private static boolean firstCreate = true;

	private static java.util.Set<String> dominion = new java.util.TreeSet<>();
	private static java.util.LinkedList<String> groupFeatures = new java.util.LinkedList<>();

	public static void main(String[] args) {
//		laswebGroupFeatures();
		
		
		lapepeacheGroupFeatures();
//		dominion.forEach((k)->groupFeatures.addLast(k));
		lapepeache();
		System.exit(0);
		long init = System.currentTimeMillis();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		/*
		int tz = 0;
		StringBuilder sb = new StringBuilder();
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Structure/bySearch?query=" + java.net.URLEncoder.encode("Structure.Identifier wildcard \"%\"", "UTF-8") + "&fields=Structure.Identifier", null);
			response = new org.json.JSONObject(rawResponse);
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				System.out.println(" " + values.getString(0) + "...");
			}
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/StructureGroup/byStructure?structure=Sitios%20Web&pageSize=500", null);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					sb.append(sb.length() == 0 ? "" : ",").append(rows.getJSONObject(i).getJSONObject("object").getString("id"));
				}
				System.out.println( new RestClient("Content-Type: application/x-www-form-urlencoded", "Accept: application/json", "Authorization: Basic " + encoded)
						.getRequest("DELETE", baseUrlDEV + "/list/StructureGroup/byItems?items=" + java.net.URLEncoder.encode( sb.toString(), "UTF-8"), null) );
				sb.setLength(0);
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/StructureGroup/byStructure?structure=Sitios%20Web&pageSize=2", null);
				response = new org.json.JSONObject(rawResponse);
				tz = response.getInt("totalSize");
				System.out.println("Total size: " + tz);
			}while(tz > 0);
		} catch (Exception e) {
			e.printStackTrace();
		}


		System.exit(0);
		*/

		/*
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Structure/bySearch?query=" + java.net.URLEncoder.encode("Structure.Identifier wildcard \"Web%\"", "UTF-8") + "&fields=Structure.Identifier", null);
			response = new org.json.JSONObject(rawResponse);
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				System.out.println("Deleting structure groups for " + values.getString(0) + "...");
				System.out.println( new RestClient("Content-Type: application/x-www-form-urlencoded", "Accept: application/json", "Authorization: Basic " + encoded).getRequest("DELETE", baseUrlDEV + "/list/StructureGroup/byStructure?structure=" + java.net.URLEncoder.encode(values.getString(0), "UTF-8"), null) );
				System.out.println("Now deleting structure...");
				System.out.println( new RestClient("Content-Type: application/x-www-form-urlencoded", "Accept: application/json", "Authorization: Basic " + encoded).getRequest("DELETE", baseUrlDEV + "/list/Structure/bySearch?query=" + java.net.URLEncoder.encode("Structure.Identifier equals \"" + values.getString(0) + "\"", "UTF-8"), null) );
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		System.exit(0);
		*/
		 // WebCategoryHierarchy
		try {


			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\opt\\LVP\\desorden\\Sitios Web\\step-7782736266119711378-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();

			/*
			System.out.println(rootElement);
			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Classifications");
			Node classificationRoot = lst.getFirst();
			System.out.println(classificationRoot);
			Node webHierarcyRoot = xmm.byAttributeValue( xmm.byAttributeValue(classificationRoot, "ID", "Classification 1 root"), "ID", "WebHierarchyRoot");
			System.out.println(webHierarcyRoot);
			java.util.LinkedList<Node> jerarquiasWeb = xmm.listImmediateChildElements(webHierarcyRoot).get("Classification");

			org.json.JSONObject request = null;
			String newName = "Web";

			org.json.JSONArray rowsSin = null;
			java.util.LinkedList<Node> primerosNodos = null;
			for(Node jerarquiaWeb : jerarquiasWeb) {
				newName = "Web" + xmm.byName(jerarquiaWeb, "Name").getTextContent();
				rawResponse = rc.getRequest("POST", baseUrlDEV + "/object/Structure", (request = new org.json.JSONObject().put("identifier", newName).put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))).put("name", ((Element)jerarquiaWeb).getAttribute("ID")))).put("levels", 8).put("mappingType", true).put("mappingLevel", true).put("type", new org.json.JSONObject().put("_code", 3))).toString());
				primerosNodos = xmm.listImmediateChildElements(jerarquiaWeb).get("Classification");
				for(Node pn : primerosNodos) {
					collectInfo((Element)pn, null, newName, rows = new org.json.JSONArray(), rowsSin = new org.json.JSONArray());
					if(rows.length() > 0 || rowsSin.length() > 0) {
						try {
							System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/StructureGroup",
									new org.json.JSONObject().put("columns",
											new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
											.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
											.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
											)
									.put("rows", rowsSin).toString()) );
						} catch (Exception e) {
							e.printStackTrace();
						}
						while(rowsSin.length() > 0) {
							rowsSin.remove(0);
						}
						System.out.println("Ostia");
						try {
							System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/StructureGroup", new org.json.JSONObject().put("columns",
									new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))
									).put("rows", rows).toString()) );
						} catch (Exception e) {
							e.printStackTrace();
						}
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}
			}
        	*/


			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Classifications");
			Node classificationRoot = lst.getFirst();
			Node webHierarcyRoot = xmm.byAttributeValue( 
//					xmm.byAttributeValue(classificationRoot, "ID", "Classification 1 root")
					classificationRoot
					, "ID", "WebHierarchyRoot");
			java.util.LinkedList<Node> jerarquiasWeb = xmm.listImmediateChildElements(webHierarcyRoot).get("Classification");

			org.json.JSONObject request = null;
			String newName = "Web";

			org.json.JSONArray rowsSin = null;
			java.util.LinkedList<Node> primerosNodos = null;

			Element jerarquiaWeb = (Element) webHierarcyRoot;

//			for(Node jerarquiaWeb : jerarquiasWeb) {
				newName = jerarquiaWeb.getAttribute("ID"); //xmm.byName(jerarquiaWeb, "Name").getTextContent();
				rawResponse = rc.getRequest("POST", baseUrl + "/object/Structure", (request = new org.json.JSONObject().put("identifier", newName)
						.put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject()
								.put("language", new org.json.JSONObject().put("_code", "es")))
								.put("name", xmm.byName(jerarquiaWeb, "Name").getTextContent())))
						.put("levels", 9).put("mappingType", true).put("mappingLevel", true)
						.put("type", new org.json.JSONObject().put("_code", 3))).toString());
				
				primerosNodos = xmm.listImmediateChildElements(jerarquiaWeb).get("Classification");
				primerosNodos.forEach((pn)->System.out.println(((Element)pn).getAttribute("ID")));
				System.out.println("Working with: " + newName);
				for(Node pn : primerosNodos) {
					collectInfo(((Element)pn), newName, newName, rows = new org.json.JSONArray(), rowsSin = new org.json.JSONArray());
					if(rows.length() > 0 || rowsSin.length() > 0) {
						if(firstCreate) {
							try {
								System.out.println( rc.getRequest("POST", baseUrl + "/list/StructureGroup",
										new org.json.JSONObject().put("columns",
												new org.json.JSONArray()
												.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
												.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
												.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
												)
										.put("rows", rowsSin).toString()) );
							} catch (Exception e) {
								e.printStackTrace();
							}
							while(rowsSin.length() > 0) {
								rowsSin.remove(0);
							}
						}
//						System.out.println("Ostia");
						try {
							System.out.println(
									rc.getRequest("POST", baseUrl + "/list/StructureGroup", new org.json.JSONObject().put("columns",
									new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))
									).put("rows", rows).toString())
									)
							;
						} catch (Exception e) {
							e.printStackTrace();
						}
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}
//			}


		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static void lapepeache() {
		long init = System.currentTimeMillis();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject request = null;

//		try {
//			rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Structure/bySearch?query=" + java.net.URLEncoder.encode("Structure.Identifier wildcard \"PrimaryProductTaxonomy\"", "UTF-8") + "&fields=Structure.Identifier", null);
//			response = new org.json.JSONObject(rawResponse);
//			rows = response.getJSONArray("rows");
//			for(int i=0; i<rows.length(); i++) {
//				values = rows.getJSONObject(i).getJSONArray("values");
//				System.out.println("Deleting structure groups for " + values.getString(0) + "...");
//				System.out.println( new RestClient("Content-Type: application/x-www-form-urlencoded", "Accept: application/json", "Authorization: Basic " + encoded).getRequest("DELETE", baseUrlDEV + "/list/StructureGroup/byStructure?structure=" + java.net.URLEncoder.encode(values.getString(0), "UTF-8"), null) );
//				System.out.println("Now deleting structure...");
//				System.out.println( new RestClient("Content-Type: application/x-www-form-urlencoded", "Accept: application/json", "Authorization: Basic " + encoded).getRequest("DELETE", baseUrlDEV + "/list/Structure/bySearch?query=" + java.net.URLEncoder.encode("Structure.Identifier equals \"" + values.getString(0) + "\"", "UTF-8"), null) );
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

//		System.exit(0);
		System.out.println("Now going to process pph source file...");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\plantillas\\Jerarquia completa niveles 1 a 4 (1).xml"));
//			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();

			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Products");
			Node productsRoot = lst.getFirst();
			Node webHierarchyRoot = xmm.byAttributeValue(productsRoot, "ID", "ProductsSuppliersPortal");
			if(webHierarchyRoot == null) {
				webHierarchyRoot = xmm.byAttributeValue(productsRoot, "ID", "ProductsSuppliersPortal");
			}
			String newName = null;

			org.json.JSONArray rowsSin = null;
			java.util.LinkedList<Node> primerosNodos = null;

			Element jerarquiaWeb = (Element) webHierarchyRoot;

			newName = "PrimaryProductTaxonomy";
//			rawResponse = rc.getRequest("POST", baseUrl + "/object/Structure", (request = new org.json.JSONObject().put("identifier", newName).put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))).put("name", ((Element)jerarquiaWeb).getAttribute("ID")))).put("levels", 4).put("mappingType", true).put("mappingLevel", true).put("type", new org.json.JSONObject().put("_code", 4))).toString());
//			primerosNodos = xmm.listImmediateChildElements(jerarquiaWeb).get("Product");
			if(jerarquiaWeb == null) {
				primerosNodos = xmm.listImmediateChildElements(productsRoot).get("Product");
			}else {
				primerosNodos = xmm.listImmediateChildElements(jerarquiaWeb).get("Product");
			}
//			primerosNodos.forEach((pn)->System.out.println(((Element)pn).getAttribute("ID")));
			for(Node pn : primerosNodos) {
				collectInfoPePeAche(((Element)pn), newName, newName, rows = new org.json.JSONArray(), rowsSin = new org.json.JSONArray());
				if(write && ( rows.length() > 0 || rowsSin.length() > 0 ) ){
					if(firstCreate) {
						try {
							org.json.JSONArray columns = null;
							columns = new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"));
							for(String groupFeature : groupFeatures) {
								columns.put(new org.json.JSONObject().put("identifier", "StructureGroupAttributeValue.Value(" + groupFeature + ",es,DEFAULT)"));
							}
							System.out.println( rc.getRequest("POST", baseUrl + "/list/StructureGroup",
									new org.json.JSONObject().put("columns",
											columns
											)
									.put("rows", rowsSin).toString()) );
						} catch (Exception e) {
							e.printStackTrace();
						}
						while(rowsSin.length() > 0) {
							rowsSin.remove(0);
						}
					}
					try {
						org.json.JSONArray columns = null;
						columns = new org.json.JSONArray()
								.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
								.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
								.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
								.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"));
						for(String groupFeature : groupFeatures) {
							columns.put(new org.json.JSONObject().put("identifier", "StructureGroupAttributeValue.Value(" + groupFeature + ",es,DEFAULT)"));
						}
						rawResponse = rc.getRequest("POST", baseUrl + "/list/StructureGroup", new org.json.JSONObject().put("columns",columns).put("rows", rows).toString());
						response = new org.json.JSONObject(rawResponse);
						response = (org.json.JSONObject) response.remove("counters");
						System.out.println(response);
					} catch (Exception e) {
						e.printStackTrace();
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}


		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static void laswebGroupFeatures() {
		long init = System.currentTimeMillis();
		System.out.println("Now going to process lasweb source file...");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\opt\\LVP\\desorden\\Sitios Web\\step-7782736266119711378-exported.xml");
//			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\Clasificaciones.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();

			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Classifications");
			Node classificationRoot = lst.getFirst();
			Node webHierarcyRoot = xmm.byAttributeValue( 
//					xmm.byAttributeValue(classificationRoot, "ID", "Classification 1 root")
					classificationRoot
				, "ID", "WebHierarchyRoot");

			String newName = null;

			java.util.LinkedList<Node> primerosNodos = null;

			Element jerarquiaWeb = (Element) webHierarcyRoot;

			newName = jerarquiaWeb.getAttribute("ID"); //xmm.byName(jerarquiaWeb, "Name").getTextContent();
			primerosNodos = xmm.listImmediateChildElements(jerarquiaWeb).get("Classification");
//			for(Node pn : primerosNodos) {
//				collectStructureValuesWeBs(((Element)pn), newName);
//			}
//			System.exit(0);
			for(Node pn : primerosNodos) {
				workOnlyOnGroupFeaturesWeBs(((Element)pn), jerarquiaWeb.getAttribute("ID"));
			}
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static void collectStructureValuesWeBs(Element n, String structureId) {
		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Classification");
		Node valuesNode = xmm.byName(n, "MetaData");
		if(valuesNode != null) {
			java.util.Map<String, java.util.LinkedList<Node>> mepes = xmm.listImmediateChildElements( valuesNode );
			java.util.LinkedList<Node> metadataValueNodes = mepes.get("Value");
			String attributeId = null;
			for(Node nv : metadataValueNodes) {
				attributeId = ((Element)nv).getAttribute("AttributeID");
				if(!dominion.contains(attributeId)) {
					try {
						System.out.println( rc.getRequest("POST", baseUrl + "/object/StructureFeature",
								new org.json.JSONObject()
									.put("identifier", attributeId)
									.put("structure", new org.json.JSONObject().put("_externalId", "'" + structureId + "'"))
									.put("datatype", new org.json.JSONObject().put("_code", "String"))
									.put("type", new org.json.JSONObject().put("_key", 4))
									.put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("name", attributeId).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es")))))
								.toString()) );
					} catch (Exception e) {
						e.printStackTrace();
					}
					dominion.add(attributeId);
				}
			}
		}
		if(children != null && !children.isEmpty()) {
			for(Node nn : children) {
				collectStructureValuesWeBs((Element)nn, structureId);
			}
		}
	}

	private static void workOnlyOnGroupFeaturesWeBs(Element n, String structureId) {
		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Classification");
		String elementId = n.getAttribute("ID");
		Node valuesNode = xmm.byName(n, "MetaData");
		String attributeId = null;
		String value = null;
		org.json.JSONArray attributes = new org.json.JSONArray();
		org.json.JSONObject attribute = null;
		if(valuesNode != null) {
			java.util.Map<String, java.util.LinkedList<Node>> mepes = xmm.listImmediateChildElements( valuesNode );
			java.util.LinkedList<Node> metadataValueNodes = mepes.get("Value");
			for(Node nv : metadataValueNodes) {
				attributeId = ((Element)nv).getAttribute("AttributeID");
				value = nv.getTextContent();
				attribute = new org.json.JSONObject();
				attribute
					.put("_qualification", new org.json.JSONObject().put( "nameInKeyLang", attributeId) )
					.put("datatype", new org.json.JSONObject().put("_code", "String"))
					.put("type", new org.json.JSONObject().put("_key", 4))
					.put("isInherited", true)
					.put("structureAttribute", new org.json.JSONObject().put("_externalId", "'" + attributeId + "'@'" + structureId + "'"))
					.put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))).put("name", attributeId)))
					.put("value", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es")).put("identifier", "DEFAULT")).put("value", value)))
				;
				attributes.put(attribute);
			}
			try {
//				System.out.println("Making a request with: " + attribute + " for StructureID: " + elementId);
				System.out.println( rc.getRequest("PUT", baseUrl + "/object/StructureGroup/'" + elementId + "'@'" + structureId + "'", new org.json.JSONObject().put("attribute", attributes).toString()) );
//				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(children != null && !children.isEmpty()) {
			for(Node nn : children) {
				workOnlyOnGroupFeaturesWeBs((Element)nn, structureId);
			}
		}
	}

	private static void lapepeacheGroupFeatures() {
		long init = System.currentTimeMillis();
		System.out.println("Now going to process pph source file...");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\plantillas\\Jerarquia completa niveles 1 a 4 (1).xml"));
//			doc = builder.parse("C:\\opt\\LVP\\tmp\\step-4611212669058297112-exported.xml");
//			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();

			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Products");
			Node productsRoot = lst.getFirst();
			Node webHierarchyRoot = xmm.byAttributeValue(productsRoot, "ID", "ProductsSuppliersPortal");
			String newName = null;

			java.util.LinkedList<Node> primerosNodos = null;

			Element jerarquiaWeb = (Element) webHierarchyRoot;

			newName = "PrimaryProductTaxonomy";
			if(jerarquiaWeb == null) {
				primerosNodos = xmm.listImmediateChildElements(productsRoot).get("Product");
			}else {
				primerosNodos = xmm.listImmediateChildElements(jerarquiaWeb).get("Product");
			}
//			for(Node pn : primerosNodos) {
//				collectStructureValuesPePeAche(((Element)pn), newName);
//			}
//			System.exit(0);
			for(Node pn : primerosNodos) {
				workOnlyOnGroupFeaturesPePeAche(((Element)pn), newName);
			}
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static void workOnlyOnGroupFeaturesPePeAche(Element n, String structureId) {
		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Product");
		String elementId = n.getAttribute("ID");
		Node valuesNode = xmm.byName(n, "Values");
		String attributeId = null;
		String value = null;
		org.json.JSONArray attributes = new org.json.JSONArray();
		org.json.JSONObject attribute = null;
		if(valuesNode != null) {
			java.util.Map<String, java.util.LinkedList<Node>> mepes = xmm.listImmediateChildElements( valuesNode );
			java.util.LinkedList<Node> metadataValueNodes = mepes.get("Value");
			for(Node nv : metadataValueNodes) {
				attributeId = ((Element)nv).getAttribute("AttributeID");
				value = nv.getTextContent();
				attribute = new org.json.JSONObject();
				attribute
					.put("_qualification", new org.json.JSONObject().put( "nameInKeyLang", attributeId) )
					.put("datatype", new org.json.JSONObject().put("_code", "String"))
					.put("type", new org.json.JSONObject().put("_key", 4))
					.put("isInherited", true)
					.put("structureAttribute", new org.json.JSONObject().put("_externalId", "'" + attributeId + "'@'" + structureId + "'"))
					.put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))).put("name", attributeId)))
					.put("value", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es")).put("identifier", "DEFAULT")).put("value", value)))
				;
				attributes.put(attribute);
			}
			try {
				if("EU4-54147774".equals(elementId)) {
					System.out.println("***********" + elementId + "***********\n\t" + baseUrl + "\n\t" + attributes);
				}
				System.out.println(elementId + "\t\t<::>\t" + rc.getRequest("PUT", baseUrl + "/object/StructureGroup/'" + elementId + "'@'" + structureId + "'", new org.json.JSONObject().put("attribute", attributes).toString()) );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(children != null && !children.isEmpty()) {
			for(Node nn : children) {
				workOnlyOnGroupFeaturesPePeAche((Element)nn, structureId);
			}
		}
	}

	private static void collectInfo(Element n, String parentId, String structureId, org.json.JSONArray rows, org.json.JSONArray rowsSin) {
		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Classification");
		if(rows.length() == 110) {
			if(firstCreate) {
				try {
					System.out.println( new org.json.JSONObject( rc.getRequest("POST", baseUrl+ "/list/StructureGroup",
							new org.json.JSONObject().put("columns",
									new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
									.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
									)
							.put("rows", rowsSin).toString()) ).remove("counters")
							);
				} catch (Exception e) {
					e.printStackTrace();
				}
				while(rowsSin.length() > 0) {
					rowsSin.remove(0);
				}
			}
			System.out.println("job performed (" + structureId + ")");
//			System.out.println(rows);
			try {
				System.out.println( "UPD: " +
						new org.json.JSONObject( rc.getRequest("POST", baseUrl + "/list/StructureGroup", new org.json.JSONObject().put("columns",
						new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
						.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))
						).put("rows", rows).toString()) ) /*.remove("counters")*/
						)
						;
			} catch (Exception e) {
				e.printStackTrace();
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		String elementId = n.getAttribute("ID");
		String userType = n.getAttribute("UserTypeID");
		Node nameNode = xmm.byName(n, "Name");
		String name = null;
		if(nameNode != null) {
			name = nameNode.getTextContent();
		}
//		java.util.Map<String, java.util.LinkedList<Node>> mepes = xmm.listImmediateChildElements( xmm.byName(n, "MetaData") );
//		java.util.LinkedList<Node> metadataValueNodes = mepes.get("Value");
		org.json.JSONObject olg = null;
		rowsSin.put(new org.json.JSONObject()
				.put("object", new org.json.JSONObject().put("id", "'" + elementId + "'@'" + structureId + "'"))
				.put("values", new org.json.JSONArray()
						.put(name + " (" + elementId + ")")
						.put(name + " (" + elementId + ")")
						.put(userType)
					)
			);
		rows.put(olg = new org.json.JSONObject()
				.put("object", new org.json.JSONObject().put("id", "'" + elementId + "'@'" + structureId + "'"))
				.put("values", new org.json.JSONArray().put(name + " (" + elementId + ")").put(name + " (" + elementId + ")").put(userType).put(parentId))
			);
//		if("catst8920678".equals(elementId) || "catst8920687".equals(elementId) || "catst8920690".equals(elementId)) {
//			System.out.println("This is the boi: " + olg);
//			try {
//				System.out.println(
//						rc.getRequest("POST", baseUrl + "/list/StructureGroup", new org.json.JSONObject().put("columns",
//						new org.json.JSONArray()
//						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
//						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
//						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
//						.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))
//						).put("rows", new org.json.JSONArray().put(olg)).toString())
//						)
//						;
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			while(rows.length() > 0) {
//				rows.remove(0);
//			}
//		}
		if(children != null && !children.isEmpty()) {
			for(Node nn : children) {
				collectInfo((Element)nn, elementId, structureId, rows, rowsSin);
			}
		}
	}

	private static void collectInfoPePeAche(Element n, String parentId, String structureId, org.json.JSONArray rows, org.json.JSONArray rowsSin) {
		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Product");
		if(write && rows.length() == 1) {
			if(firstCreate) {
				try {
					org.json.JSONArray columns = null;
					columns = new org.json.JSONArray()
							.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
							.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
							.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"));
					for(String groupFeature : groupFeatures) {
						columns.put(new org.json.JSONObject().put("identifier", "StructureGroupAttributeValue.Value(" + groupFeature + ",es,DEFAULT)"));
					}
					System.out.println( rc.getRequest("POST", baseUrl + "/list/StructureGroup",
							new org.json.JSONObject().put("columns",
									columns
									)
							.put("rows", rowsSin).toString()) );
				} catch (Exception e) {
					e.printStackTrace();
				}
				while(rowsSin.length() > 0) {
					rowsSin.remove(0);
				}
			}
			try {
				org.json.JSONArray columns = null;
				columns = new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"))
						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))
						.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Note(es)"))
						.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"));
				for(String groupFeature : groupFeatures) {
					columns.put(new org.json.JSONObject().put("identifier", "StructureGroupAttributeValue.Value(" + groupFeature + ",es,DEFAULT)"));
				}
				org.json.JSONObject request = new org.json.JSONObject().put("columns",columns).put("rows", rows);
				String rawResponse = rc.getRequest("POST", baseUrl + "/list/StructureGroup", request.toString());
				org.json.JSONObject response = new org.json.JSONObject(rawResponse);
				response = (org.json.JSONObject) response.remove("counters");
				System.out.println(response);
				System.out.println("--->" + request);
//				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		String elementId = n.getAttribute("ID");
		String userType = n.getAttribute("UserTypeID");
		Node nameNode = xmm.byName(n, "Name");
		String name = null;
		if(nameNode != null) {
			name = nameNode.getTextContent();
		}
		Node valuesNode = xmm.byName(n, "Values");
		java.util.Map<String, String> valuesMap = new java.util.TreeMap<>();
		if(valuesNode != null) {
			java.util.Map<String, java.util.LinkedList<Node>> mepes = xmm.listImmediateChildElements( valuesNode );
			java.util.LinkedList<Node> metadataValueNodes = mepes.get("Value");
			for(Node nv : metadataValueNodes) {
				valuesMap.put(((Element)nv).getAttribute("AttributeID"), nv.getTextContent());
			}
		}
		org.json.JSONArray values1 = new org.json.JSONArray();
		org.json.JSONArray values2 = new org.json.JSONArray();
		values1.put(name + " (" + elementId + ")").put(name + " (" + elementId + ")").put(userType);
		values2.put(name + " (" + elementId + ")").put(name + " (" + elementId + ")").put(userType).put(parentId);
		putAttributeValues(values1, groupFeatures, valuesMap);
		putAttributeValues(values2, groupFeatures, valuesMap);
		rowsSin.put(new org.json.JSONObject()
				.put("object", new org.json.JSONObject().put("id", "'" + elementId + "'@'" + structureId + "'"))
				.put("values", values1)
			);
		rows.put(new org.json.JSONObject()
				.put("object", new org.json.JSONObject().put("id", "'" + elementId + "'@'" + structureId + "'"))
				.put("values", values2)
			);
		if(children != null && !children.isEmpty()) {
			for(Node nn : children) {
				collectInfoPePeAche((Element)nn, elementId, structureId, rows, rowsSin);
			}
		}
	}

	private static void putAttributeValues(org.json.JSONArray values, java.util.LinkedList<String> attributeValues, java.util.Map<String, String> valuesMap) {
		String value = null;
		for(String attributeValue : attributeValues) {
			value = valuesMap.get(attributeValue);
			values.put(value == null ? "" : value);
		}
	}

	private static void collectStructureValuesPePeAche(Element n, String structureId) {
//		try {
//			System.out.println(new org.json.JSONObject()
//					.put("datatype", new org.json.JSONObject().put("_code", "String"))
//					.put("type", new org.json.JSONObject().put("_key", 4))
//					.put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("name", "Algolindo").put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es")))))
//				.toString());
//			System.out.println( rc.getRequest("GET", baseUrlDEV + "/object/StructureFeature/'RutaWEB_CatID'@'PrimaryProductTaxonomy'", null) );
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		System.exit(0);
		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Product");
		Node valuesNode = xmm.byName(n, "Values");
		if(valuesNode != null) {
			java.util.Map<String, java.util.LinkedList<Node>> mepes = xmm.listImmediateChildElements( valuesNode );
			java.util.LinkedList<Node> metadataValueNodes = mepes.get("Value");
			String attributeId = null;
			for(Node nv : metadataValueNodes) {
				attributeId = ((Element)nv).getAttribute("AttributeID");
				if(!dominion.contains(attributeId)) {
					try {
						System.out.println( rc.getRequest("POST", baseUrl + "/object/StructureFeature",
								new org.json.JSONObject()
									.put("identifier", attributeId)
									.put("structure", new org.json.JSONObject().put("_externalId", "'" + structureId + "'"))
									.put("datatype", new org.json.JSONObject().put("_code", "String"))
									.put("type", new org.json.JSONObject().put("_key", 4))
									.put("lang", new org.json.JSONArray().put(new org.json.JSONObject().put("name", attributeId).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es")))))
								.toString()) );
					} catch (Exception e) {
						e.printStackTrace();
					}
					dominion.add(attributeId);
				}
			}
		}
		if(children != null && !children.isEmpty()) {
			for(Node nn : children) {
				collectStructureValuesPePeAche((Element)nn, structureId);
			}
		}
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
