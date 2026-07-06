package mx.com.liverpool.p360.services.core.temp.source;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class SourcePPHL4TemplatesFromEUXMLFile {

	public static void main(String[] args) {
		SourcePPHL4TemplatesFromEUXMLFile ppc = new SourcePPHL4TemplatesFromEUXMLFile();
		try {
			ppc.loadTemplates();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private final RESTWrapper rw = new RESTWrapper();
	private final XMLMisc xmm = rw.getXmm();
	
	private void loadTemplates() throws ParserConfigurationException, SAXException, IOException {
		java.nio.file.Path path = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "Jerarquia completa niveles 1 a 4.xml");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse( path.toString() );
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		java.util.LinkedList<Node> productNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Products").getFirst() ).get("Product");
		if(productNodeList != null) {
			for(Node n : productNodeList) {
				iterateNode(n, request);
			}
			if(rows.length() > 0) {
				rw.writeData("list", "LookupValue", null, new java.util.TreeMap<>(), request, System.out::println);
			}
		}
	}
	
	private void iterateNode(Node n, org.json.JSONObject request) {
		java.util.LinkedList<Node> childNodes = xmm.listImmediateChildElements(n).get("Product");
		if(childNodes != null) {
			Element el = null;
			for(Node n0 : childNodes) {
				el = (Element) n0;
				if(el.getAttribute("ID").startsWith("EU4-")) {
					Node nn = xmm.byName(n0, "Name");
					org.json.JSONArray rows = request.getJSONArray("rows");
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID") + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray().put( nn == null ? "" : nn.getTextContent() ).put(true)));
					if(rows.length() == 100) {
						rw.writeData("list", "LookupValue", null, new java.util.TreeMap<>(), request, System.out::println);
					}
				}else {
					iterateNode(n0, request);
				}
			}
		}
	}
	
}
