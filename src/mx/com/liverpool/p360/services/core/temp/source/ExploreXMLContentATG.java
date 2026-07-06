package mx.com.liverpool.p360.services.core.temp.source;

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

public class ExploreXMLContentATG {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc;
		doc = builder.parse("C:\\opt\\LVP\\tmp\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		Node assetsRoot = a.get("AttributeList").getFirst();
		java.util.LinkedList<Node> atributos = xmm.listImmediateChildElements(assetsRoot).get("Attribute");
		java.util.Set<String> metadataAttributes = new java.util.TreeSet<>();
		java.util.Map<String, java.util.Set< String >> valoresAtributos = new java.util.TreeMap<>();
		atributos.forEach(at->{
			
				java.util.LinkedList<Node> metaData = xmm.listImmediateChildElements(at).get("MetaData");
				if(metaData != null) {
					java.util.LinkedList<Node> valueNodeList = xmm.listImmediateChildElements( metaData.getFirst() ).get("Value");
					if(valueNodeList != null) {
						valueNodeList.forEach(mvn->{
							metadataAttributes.add(((Element)mvn).getAttribute("AttributeID"));
							java.util.Set<String> lst = null;
							lst = valoresAtributos.get(((Element)mvn).getAttribute("AttributeID"));
							if(lst == null) {
								lst = new java.util.TreeSet<>();
								valoresAtributos.put(((Element)mvn).getAttribute("AttributeID"), lst);
							}
							lst.add(mvn.getTextContent());
						});
					}
				}
			
			}
		);
		metadataAttributes.forEach(v->System.out.println(("AttributeHelpText".equals(v) ? "" : v + " - " + valoresAtributos.get(v) )));
	}
}
