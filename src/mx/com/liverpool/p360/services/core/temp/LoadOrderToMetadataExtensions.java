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

public class LoadOrderToMetadataExtensions {
	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {

		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.util.Set<String> set = new java.util.TreeSet<>();
		do {
			try {
				rawResponse = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode(
								"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" "
								+ "and StandardizationValue.CreationType equals CreateProposal "
								+ "and StandardizationValue.Property equals VendorCenterSection "
								+ "and StandardizationValue.PropertyValue equals \"Datos Logísticos\" "
								+ "and StandardizationValue.StructureGroup->LookupValue.Code equals \"EU4-113578\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.Characteristic->Characteristic.Identifier", "UTF-8")
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					set.add(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}
			} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("AttributeList");
		Node productsRoot = lst.getFirst();
		java.util.LinkedList<Node> attributeList = xmm.listImmediateChildElements(productsRoot).get("Attribute");

		System.out.println( ((Element) attributeList.getFirst()).getAttribute("ID") );
		rows = new org.json.JSONArray();

		for(Node n : attributeList) {
			processNode(n, rows, set);
		}
		if(rows.length() > 0) {
			rawResponse = null;
			try {
				rawResponse = workshop.makeRequest("POST", "/list/StandardizationValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).put("rows", rows).toString());
				System.out.println(rawResponse);
			} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

	private static void processNode(Node n, org.json.JSONArray rows, java.util.Set<String> losQueSi) throws ServiceUnavailableException {
		String id = ((Element)n).getAttribute("ID");
		if(losQueSi.contains(id)) {
			java.util.LinkedList<Node> metadata = xmm.listImmediateChildElements( xmm.listImmediateChildElements(n).get("MetaData").getFirst() ).get("Value") ;
			if(metadata != null) {
				for(Node mv : metadata) {
					if(((Element)mv).getAttribute("AttributeID").equals("DisplaySequence")) {
						rows.put(new org.json.JSONObject().put("values", new org.json.JSONArray().put("EU4-113578").put(id).put("CreateProposal").put("VendorCenterSectionSequence").put( Integer.parseInt( mv.getTextContent() ) )).put("object",  new org.json.JSONObject().put("id", "'EU4-113578_" + id + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")));
						if(rows.length() == 500) {
							String rawResponse = null;
							try {
								rawResponse = workshop.makeRequest("POST", "/list/StandardizationValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).put("rows", rows).toString());
								System.out.println("--->" + rawResponse);
							} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException | IOException e) {
								e.printStackTrace();
							}
							while(rows.length() > 0) {
								rows.remove(0);
							}
							System.exit(0);
						}
					}
				}
			}
		}
	}
}
