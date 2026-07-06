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

public class PartyAgregaTipoProveedorSAP {

	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		long init = System.currentTimeMillis();
		crearProveedores();
		System.out.println(workshop.formatTime(System.currentTimeMillis() - init));
	}

	private static void crearProveedores() throws SAXException, IOException, ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\opt\\LVP\\desorden\\step-472799325730140560-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Classifications");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> suppliers = xmm.listImmediateChildElements(assetsRoot).get("Classification");

		org.json.JSONArray columns = new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('TipoProveedorSAPAttLOV')"));
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		Element el;
		java.util.LinkedList<Node> metaDatas = null;
		Element tipoProveedorSAPNode = null;

		String id = null;
		String tipoProveedorSAP = null;

		for(Node n : suppliers) {
			el = (Element) n;
			id = el.getAttribute("ID");
			metaDatas = xmm.listImmediateChildElements(n).get("MetaData");
			if(metaDatas != null && !metaDatas.isEmpty()) {
				tipoProveedorSAPNode = (Element) xmm.byAttributeValue( metaDatas.getFirst(), "AttributeID", "TipoProveedorSAP" );
				if(tipoProveedorSAPNode != null) {
					tipoProveedorSAP = tipoProveedorSAPNode.hasAttribute("ID") ? tipoProveedorSAPNode.getAttribute("ID") : "";
				} else {
					tipoProveedorSAP = "";
				}
			} else {
				tipoProveedorSAP = "";
			}
			if(!"".equals(tipoProveedorSAP) && tipoProveedorSAP != null) {
				rows.put(
						new org.json.JSONObject()
						.put("object", new org.json.JSONObject().put("id", "'" + id.replaceAll("-.+", "") + "'@'Party'"))
						.put("values", new org.json.JSONArray().put(tipoProveedorSAP)));
				if(rows.length() == 250) {
					response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
					System.out.println(response);
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
			System.out.println(response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

}
