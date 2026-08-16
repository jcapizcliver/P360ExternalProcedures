package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class LoadKeywords {

	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		workshop.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\opt\\LVP\\tmp\\step-4611212669058297112-exported.xml");
//		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Products");
		Node productsRoot = lst.getFirst();
		java.util.LinkedList<Node> product0 = xmm.listImmediateChildElements(productsRoot).get("Product");

		System.out.println( ((Element)product0.getFirst()).getAttribute("ID") );

		java.util.LinkedList<Node> product1 = xmm.listImmediateChildElements(product0.getFirst()).get("Product");
		org.json.JSONArray rows = new org.json.JSONArray();
		for(Node n : product1) {
			processNode(n, rows);
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		if(rows.length() > 0) {
			String rawResponse = null;
			try {
				response = workshop.makeRequest("POST", "/list/StructureGroup", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Synonym(es)"))).put("rows", rows).toString());
				if(response != null) {
					System.out.println("RESP: " + response);
				}else {
					System.out.println(workshop.getRawResponse());
				}
//				System.out.println(rawResponse);
//				new org.json.JSONObject(rawResponse);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

	private static void processNode(Node n, org.json.JSONArray rows) {
		String id = ((Element)n).getAttribute("ID");
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		System.out.println(id);
		if(id.startsWith("EU4")) {
			java.util.LinkedList<Node> multiValues = xmm.listImmediateChildElements( xmm.listImmediateChildElements(n).get("Values").getFirst() ).get("MultiValue") ;
			java.util.LinkedList<Node> value = null;
			if(multiValues != null) {
				for(Node mv : multiValues) {
					if(((Element)mv).getAttribute("AttributeID").equals("KeyWords")) {
						org.json.JSONArray values = new org.json.JSONArray();
						value = xmm.listImmediateChildElements(mv).get("Value");
						for(Node nv : value) {
							values.put(nv.getTextContent());
						}
						rows.put(new org.json.JSONObject()
								.put("object",  new org.json.JSONObject().put("id", "'" + id + "'@'PrimaryProductTaxonomy'")
										)
								.put("values", new org.json.JSONArray().put( values ))
								);
						if(rows.length() == 250) {
							String rawResponse = null;
							try {
								response = workshop.makeRequest("POST", "/list/StructureGroup", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Synonym(es)"))).put("rows", rows).toString());
								if(response != null) {
									System.out.println(response);
								}else {
									System.out.println(workshop.getRawResponse());
								}
//								System.out.println(rawResponse);
//								response = new org.json.JSONObject(rawResponse);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							while(rows.length() > 0) {
								rows.remove(0);
							}
						}
					}
				}
			}
		}else {
			java.util.LinkedList<Node> child = xmm.listImmediateChildElements(n).get("Product");
			if(child != null && !child.isEmpty()) {
				for(Node n0: child) {
					processNode(n0, rows);
				}
			}
		}
	}

}
