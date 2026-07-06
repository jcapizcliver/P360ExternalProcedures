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
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("AttributeList");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> atributos = xmm.listImmediateChildElements(assetsRoot).get("Attribute");
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Set<String> atgAttributeGroups = new java.util.TreeSet<>();
		atributos.forEach(at->{
			
				java.util.LinkedList<Node> metaData = xmm.listImmediateChildElements(at).get("MetaData");
				if(metaData != null) {
					java.util.LinkedList<Node> multiValue = xmm.listImmediateChildElements( metaData.getFirst() ).get("MultiValue");
					if(multiValue != null) {
						multiValue.forEach(mvn->{
							if("isAttInGroupAtt".equals(((Element)mvn).getAttribute("AttributeID"))) {
								java.util.LinkedList<Node> vnlist = xmm.listImmediateChildElements(mvn).get("Value");
								if(vnlist != null) {
									rows
										.put(new org.json.JSONObject()
												.put("object", new org.json.JSONObject()
														.put("id", "'" + ((Element)at).getAttribute("ID") + "'@'Characteristics'"))
												.put("values", new org.json.JSONArray()));
									vnlist.forEach(vne->{
										atgAttributeGroups.add(((Element)vne).getAttribute("ID") + "<::>" + vne.getTextContent());
										rows.getJSONObject( rows.length() - 1 ).getJSONArray("values").put(((Element)vne).getAttribute("ID"));
										if(rows.length() == 200) {
//											java.util.Map<String, String> qp = new java.util.TreeMap<>();
//											org.json.JSONObject response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('AgrupacionesAtributosATG')"))).put("rows", rows).toString());
//											System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : "From writing ATG Attribute Group on Characteristics: " + response );
											while(rows.length() > 0) {
												rows.remove(0);
											}
										}
									});
									 
								}
							}
						});
					}
				}
			
			}
		);
//		System.out.println("Found Groups:");
//		atgAttributeGroups.forEach(ag->{
//			System.out.println(ag);
//			String[] pieces = ag.split("<::>");
//			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'AgrupacionesAtributosATG'")).put("values", new org.json.JSONArray().put(true).put(pieces[1])));
//		});
//		workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive")).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))).put("rows", rows).toString());
//		System.out.println( workshop.getRawResponse() );
		
		if(rows.length() > 0) {
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('AgrupacionesAtributosATG')"))).put("rows", rows).toString());
			System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : "From writing ATG Attribute Group on Characteristics: " + response );
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
}
