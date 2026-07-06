package mx.com.liverpool.p360.services.core.temp.characteristic;

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

public class IncorporaMetadataCaracterísticas {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	private static final java.util.Map<String, String> qp = new java.util.TreeMap<>();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
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
		atributos.forEach(at->{
				java.util.Map<String, String> data = new java.util.TreeMap<>();
				java.util.LinkedList<Node> metaData = xmm.listImmediateChildElements(at).get("MetaData");
				if(metaData != null) {
					java.util.LinkedList<Node> value = xmm.listImmediateChildElements( metaData.getFirst() ).get("Value");
					if(value != null) {
						org.json.JSONArray purposes = new org.json.JSONArray();
						value.forEach(vn->{
							if("CreationModificationAtributesIIEP".equals(((Element)vn).getAttribute("AttributeID")) || "isFaceted".equals(((Element)vn).getAttribute("AttributeID")) || "isConfigurable".equals(((Element)vn).getAttribute("ID"))) {
								purposes.put(((Element)vn).getAttribute("AttributeID"));
							}else if("AttributeHelpText".equals(((Element)vn).getAttribute("AttributeID"))) {
								data.put("desc", vn.getTextContent());
							}else if("DisplaySequence".equals(((Element)vn).getAttribute("AttributeID"))) {
								data.put("order", vn.getTextContent());
							}
						});
						org.json.JSONObject row = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + (((Element)at).getAttribute("ID")) + "'")).put("values", new org.json.JSONArray().put( data.containsKey("desc") ? data.get("desc") : "").put( data.containsKey("order") ? Long.parseLong( data.get("order") ) : null ).put(purposes));
						System.out.println("--->" + row);
						rows.put(row);
						data.clear();
						if(rows.length() == 200) {
							java.util.Map<String, String> qp = new java.util.TreeMap<>();
							org.json.JSONObject response = workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject()
								.put("columns", new org.json.JSONArray()
										.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Description(es)"))
										.put(new org.json.JSONObject().put("identifier", "Characteristic.Order"))
										.put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"))
									)
								.put("rows", rows).toString());
							System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : "From writing Characteristic data: " + response );
							while(rows.length() > 0) {
								rows.remove(0);
							}
						}
					}
				}
			
			}
		);
		if(rows.length() > 0) {
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject response = workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject()
				.put("columns", new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Description(es)"))
						.put(new org.json.JSONObject().put("identifier", "Characteristic.Order"))
						.put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"))
					)
				.put("rows", rows).toString());
			System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : "From writing Characteristic data: " + response );
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
}
