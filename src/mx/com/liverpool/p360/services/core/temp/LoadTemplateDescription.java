package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
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

public class LoadTemplateDescription {

	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {
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

		java.util.LinkedList<Node> product1 = xmm.listImmediateChildElements(product0.getFirst()).get("Product");
		org.json.JSONArray rows = new org.json.JSONArray();
		for(Node n : product1) {
			processNode(n, rows);
		}
		if(rows.length() > 0) {
			String rawResponse = null;
			try {
				rawResponse = workshop.makeRequest("POST", "/list/StructureGroup", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Description(es)"))).put("rows", rows).toString());
				System.out.println(rawResponse);
				org.json.JSONObject response = new org.json.JSONObject(rawResponse);
			} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException
					| IOException e) {
				e.printStackTrace();
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

	private static void processNode(Node n, org.json.JSONArray rows) throws ServiceUnavailableException {
		String id = ((Element)n).getAttribute("ID");
//		System.out.println(id);
		if(id.startsWith("EU4")) {
			java.util.LinkedList<Node> value = xmm.listImmediateChildElements( xmm.listImmediateChildElements(n).get("Values").getFirst() ).get("Value");
			String desc = null;
			for(Node nv : value) {
				if("TemplateDescription".equals(((Element)nv).getAttribute("AttributeID"))) {
					desc = nv.getTextContent();
					break;
				}
			}
			if(desc != null) {
				rows.put(new org.json.JSONObject().put("values", new org.json.JSONArray().put( desc )).put("object",  new org.json.JSONObject().put("id", "'" + id + "'@'PrimaryProductTaxonomy'")));
				if(rows.length() == 250) {
					String rawResponse = null;
					try {
						rawResponse = workshop.makeRequest("POST", "/list/StructureGroup", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Description(es)"))).put("rows", rows).toString());
						System.out.println(rawResponse);
						org.json.JSONObject response = new org.json.JSONObject(rawResponse);
					} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException
							| IOException e) {
						e.printStackTrace();
					}
					while(rows.length() > 0) {
						rows.remove(0);
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
