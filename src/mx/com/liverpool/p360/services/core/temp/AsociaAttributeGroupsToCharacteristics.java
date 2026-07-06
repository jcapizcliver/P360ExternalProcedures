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

public class AsociaAttributeGroupsToCharacteristics {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	private static final java.util.Map<String, String> qp = new java.util.TreeMap<>();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		workshop.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		workshop.addHeader("Authorization" , "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc;
		doc = builder.parse("C:\\opt\\LVP\\tmp\\step-4611212669058297112-exported.xml");
//		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("AttributeList");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> atributos = xmm.listImmediateChildElements(assetsRoot).get("Attribute");

		java.util.Set<String> attributeGroupIds = new java.util.TreeSet<>();
		org.json.JSONArray rows = new org.json.JSONArray();

		atributos.forEach(at->{
			java.util.LinkedList<Node> attributeGroups = null;
			attributeGroups = xmm.listImmediateChildElements(at).get("AttributeGroupLink");
			org.json.JSONArray attributeGroupLinkIds = new org.json.JSONArray();
			attributeGroups.forEach(atgl->{
				attributeGroupIds.add(((Element)atgl).getAttribute("AttributeGroupID"));
				attributeGroupLinkIds.put(((Element)atgl).getAttribute("AttributeGroupID"));
			});
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ((Element)at).getAttribute("ID") + "'@'Characteristics'")).put("values", new org.json.JSONArray().put(attributeGroupLinkIds).put(true)));
			if(rows.length() == 250) {
				System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(AttributeGroup)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString()).getJSONObject("counters") );
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		);
		if(rows.length() > 0) {
			System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(AttributeGroup)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString()) );
		}
	}
}
