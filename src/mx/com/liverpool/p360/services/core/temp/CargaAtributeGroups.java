package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class CargaAtributeGroups {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	private static final java.util.Map<String, String> qp = new java.util.TreeMap<>();

	public static void main(String args[]) throws SAXException, IOException, ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("AttributeGroupList");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> attributeGroup = xmm.listImmediateChildElements(assetsRoot).get("AttributeGroup") ;


		attributeGroup.forEach(at->{
				excavaciónHaciaStructureGroup(at);
			}
		);

	}

	private static org.json.JSONArray excavaciónHaciaStructureGroup(Node attributeGroup) {

		Element el = (Element) attributeGroup;
		org.json.JSONArray childIds = new org.json.JSONArray();

		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(attributeGroup).get("AttributeGroup");

		if(children == null) {
			System.out.println("Just writing an attribute group value: " + workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID") + "'@'AttributeGroup'")).put("values", new org.json.JSONArray().put( xmm.byName(el, "Name").getTextContent() ).put(true)))).toString()));
		}else {
			for(Node child : children) {
				childIds.put(((Element)child).getAttribute("ID"));
				excavaciónHaciaStructureGroup(child);
			}
			System.out.println("Writing " + childIds.length() + "  to " + el.getAttribute("ID") + ". " + workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(AttributeGroup)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID") + "'@'AttributeGroup'")).put("values", new org.json.JSONArray().put( xmm.byName(el, "Name").getTextContent() ).put(childIds).put(true)))).toString()));
		}

		return childIds;
	}

}
