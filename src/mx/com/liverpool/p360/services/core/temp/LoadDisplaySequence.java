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

public class LoadDisplaySequence {

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
		java.util.LinkedList<Node> lst = a.get("AttributeList");
		Node productsRoot = lst.getFirst();
		java.util.LinkedList<Node> attributeList = xmm.listImmediateChildElements(productsRoot).get("Attribute");

		System.out.println( ((Element) attributeList.getFirst()).getAttribute("ID") );
		org.json.JSONArray rows = new org.json.JSONArray();

		for(Node n : attributeList) {
			processNode(n, rows);
		}
		if(rows.length() > 0) {
			String rawResponse = null;
			try {
				rawResponse = workshop.makeRequest("POST", "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Order"))).put("rows", rows).toString());
				System.out.println(rawResponse);
			} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

	private static void processNode(Node n, org.json.JSONArray rows) throws ServiceUnavailableException {
		String id = ((Element)n).getAttribute("ID");
		java.util.LinkedList<Node> metadata = xmm.listImmediateChildElements( xmm.listImmediateChildElements(n).get("MetaData").getFirst() ).get("Value") ;
		if(metadata != null) {
			for(Node mv : metadata) {
				if(((Element)mv).getAttribute("AttributeID").equals("DisplaySequence")) {
					rows.put(new org.json.JSONObject().put("values", new org.json.JSONArray().put( Integer.parseInt( mv.getTextContent() ) )).put("object",  new org.json.JSONObject().put("id", "'" + id + "'")));
					if(rows.length() == 500) {
						String rawResponse = null;
						try {
							rawResponse = workshop.makeRequest("POST", "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Order"))).put("rows", rows).toString());
							System.out.println(rawResponse);
						} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException | IOException e) {
							e.printStackTrace();
						}
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}
			}
		}
	}

}
