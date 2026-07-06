package mx.com.liverpool.p360.services.xmlutils;

import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class XMLMisc {

	public java.util.Map<String, Node> getMeItsChildsByIDAttribute(Node n, String idAttribute){
		java.util.Map<String, Node> childNodes = new java.util.TreeMap<>();
		NodeList nl = n.getChildNodes();
		for(int i=0; i<nl.getLength(); i++) {
			if(nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				if( ((Element)nl.item(i)).getAttribute(idAttribute) != null ) {
					childNodes.put(((Element)nl.item(i)).getAttribute(idAttribute), nl.item(i));
				}
			}
		}
		return childNodes;
	}

	public java.util.Map<String, java.util.LinkedList<Node>> listImmediateChildElements(Node n){
		java.util.Map<String, java.util.LinkedList<Node>> childNodeNames = new java.util.TreeMap<>();
		java.util.LinkedList<Node> currentList = null;
		NodeList nl = n.getChildNodes();
		for(int i=0; i<nl.getLength(); i++) {
			if(nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				currentList = childNodeNames.get(nl.item(i).getNodeName());
				if(currentList == null) {
					currentList = new java.util.LinkedList<>();
					childNodeNames.put(nl.item(i).getNodeName(), currentList);
				}
				currentList.addLast(nl.item(i));
			}
		}
		return childNodeNames;
	}

	public java.util.Map<String, Node> listNodeAttributes(Node n){
		java.util.TreeMap<String, Node> attributes = new java.util.TreeMap<>();
		NamedNodeMap nnm = n.getAttributes();
		for(int i=0; i<nnm.getLength(); i++) {
			attributes.put(nnm.item(i).getLocalName(), nnm.item(i));
		}
		return attributes;
	}

	public Node byName(Node n, String name) {
		NodeList nl = n.getChildNodes();
		for(int i=0; i<nl.getLength(); i++) {
			if(nl.item(i).getNodeType() == Node.ELEMENT_NODE && nl.item(i).getNodeName().equals(name)) {
				return nl.item(i);
			}
		}
		return null;
	}

	public Node byAttributeValue(Node n, String attributeId, String attributeValue) {
		NodeList nl = n.getChildNodes();
		for(int i=0; i<nl.getLength(); i++) {
			if(nl.item(i).getNodeType() == Node.ELEMENT_NODE && ((Element)nl.item(i)).hasAttribute(attributeId) && attributeValue.equals(((Element)nl.item(i)).getAttribute(attributeId))) {
				return nl.item(i);
			}
		}
		return null;
	}

	public String prettyPrint(Node node) throws TransformerException {
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();

	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

	    normalizeTextNodes(node);

	    StringWriter writer = new StringWriter();
	    transformer.transform(new DOMSource(node), new StreamResult(writer));

	    return writer.toString();
	}

	private static void normalizeTextNodes(Node node) {
	    if (node.getNodeType() == Node.TEXT_NODE) {
	        Text textNode = (Text) node;
	        String original = textNode.getWholeText();
	        String normalized = Pattern.compile("\\s+").matcher(original).replaceAll(" ").trim();
	        textNode.setData(normalized);
	    } else {
	        NodeList children = node.getChildNodes();
	        for (int i = 0; i < children.getLength(); i++) {
	            normalizeTextNodes(children.item(i));
	        }
	    }
	}

}
