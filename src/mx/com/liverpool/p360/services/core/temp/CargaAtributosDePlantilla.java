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

public class CargaAtributosDePlantilla {


	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	private static final java.util.Map<String, String> qp = new java.util.TreeMap<>();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
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
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> plantillas = xmm.listImmediateChildElements( xmm.listImmediateChildElements(assetsRoot).get("Product").getFirst() ).get("Product");

		plantillas.forEach(plantilla->perforaciónHaciaPlantillas(plantilla));

		System.exit(0);

	}

	private static void perforaciónHaciaPlantillas(Node node) {
		java.util.LinkedList<Node> plantillas = xmm.listImmediateChildElements(node).get("Product");
		if(plantillas != null) {
			for(Node plantilla : plantillas) {
				perforaciónHaciaPlantillas(plantilla);
			}
		}else {
			Element element = (Element) node;
			String id = element.getAttribute("ID");
			java.util.LinkedList<Node> attributeLinks = xmm.listImmediateChildElements(element).get("AttributeLink");
//			if("EU4-113578".equals(id)) {
				System.out.println("Came here... " + attributeLinks);
				if(attributeLinks == null) {

				}else {
					org.json.JSONArray rows = new org.json.JSONArray();
					org.json.JSONArray attributeLinkIds = new org.json.JSONArray();
					for(Node n : attributeLinks) {
						attributeLinkIds.put(((Element)n).getAttribute("AttributeID"));
					}
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray().put(attributeLinkIds)));
					System.out.println(attributeLinkIds.length() + " attributes for " + id + "<::>" + workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put( new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(Characteristics)") )).put("rows", rows).toString()) );
				}
//			}
		}
	}


}
