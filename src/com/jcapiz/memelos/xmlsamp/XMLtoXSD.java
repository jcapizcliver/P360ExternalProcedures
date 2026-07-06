package com.jcapiz.memelos.xmlsamp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLtoXSD{

	private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String INDENT = "    ";

    // Reserved XML attributes and namespaced attributes (colon `:`)
    private static final Set<String> RESERVED_XML_ATTRIBUTES = new HashSet<>(Arrays.asList(
            "xmlns", "xmlns:xsi", "xsi:schemaLocation", "xml:lang", "xml:space", "xml:base"
    ));

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java XMLToXSDGenerator <input.xml> <output.xsd>");
            return;
        }

        String xmlFilePath = args[0];
        String xsdFilePath = args[1];

        try {
            File xmlFile = new File(xmlFilePath);
            File xsdFile = new File(xsdFilePath);

            generateXSD(xmlFile, xsdFile);
            System.out.println("XSD file successfully generated: " + xsdFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateXSD(File xmlFile, File xsdFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        Element rootElement = doc.getDocumentElement();
        StringWriter xsdContent = new StringWriter();
        Set<String> definedElements = new HashSet<>();
        Map<String, Integer> globalElementCounts = new HashMap<>();

        // Step 1: Collect all element names to ensure they are defined before being referenced
        collectElementNames(rootElement, globalElementCounts);

        // Step 2: Start schema and define all elements first (before using references)
        xsdContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xsdContent.append("<xs:schema xmlns:xs=\"" + XSD_NAMESPACE + "\">\n");
        defineAllElements(globalElementCounts, xsdContent, 1, definedElements);

        // Step 3: Generate correct references inside the structure
        processElement(rootElement, definedElements, xsdContent, 1, globalElementCounts);

        // Close schema
        xsdContent.append("</xs:schema>\n");

        // Write to XSD file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(xsdFile))) {
            writer.write(xsdContent.toString());
        }
    }

    private static void collectElementNames(Element element, Map<String, Integer> globalElementCounts) {
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getNodeName();
                if (!childName.contains(":")) {  // Ignore elements with a colon (namespaced elements)
                    globalElementCounts.merge(childName, 1, Integer::sum);
                    collectElementNames((Element) child, globalElementCounts);
                }
            }
        }
    }

    private static void defineAllElements(Map<String, Integer> globalElementCounts, StringWriter xsdContent, int depth, Set<String> definedElements) {
        String indent = INDENT.repeat(depth);

        for (String elementName : globalElementCounts.keySet()) {
            if (!definedElements.contains(elementName)) {
                xsdContent.append(indent).append("<xs:element name=\"").append(elementName).append("\">\n");
                xsdContent.append(indent).append(INDENT).append("<xs:complexType>\n");
                xsdContent.append(indent).append(INDENT).append(INDENT).append("<xs:sequence>\n");
                xsdContent.append(indent).append(INDENT).append(INDENT).append("</xs:sequence>\n");
                xsdContent.append(indent).append(INDENT).append("</xs:complexType>\n");
                xsdContent.append(indent).append("</xs:element>\n");

                definedElements.add(elementName);
            }
        }
    }

    private static void processElement(Element element, Set<String> definedElements, StringWriter xsdContent, int depth, Map<String, Integer> globalElementCounts) {
        String elementName = element.getNodeName();
        if (elementName.contains(":"))
		 {
			return; // Ignore elements with a colon (namespaced elements)
		}

        NodeList children = element.getChildNodes();
        Map<String, Integer> childElementCount = new HashMap<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getNodeName();
                if (!childName.contains(":")) {  // Ignore namespaced child elements
                    childElementCount.merge(childName, 1, Integer::sum);
                }
            }
        }

        String indent = INDENT.repeat(depth);
        xsdContent.append(indent).append("<xs:element name=\"").append(elementName).append("\">\n");
        xsdContent.append(indent).append(INDENT).append("<xs:complexType>\n");
        xsdContent.append(indent).append(INDENT).append(INDENT).append("<xs:sequence>\n");

        for (Map.Entry<String, Integer> entry : childElementCount.entrySet()) {
            String childName = entry.getKey();
            int count = entry.getValue();
            String maxOccurs = (count > 1 || globalElementCounts.get(childName) > 1) ? "unbounded" : "1";

            if (definedElements.contains(childName)) {
                xsdContent.append(indent).append(INDENT).append(INDENT)
                          .append("<xs:element ref=\"").append(childName)
                          .append("\" minOccurs=\"0\" maxOccurs=\"").append(maxOccurs).append("\"/>\n");
            }
        }

        xsdContent.append(indent).append(INDENT).append(INDENT).append("</xs:sequence>\n");

        NamedNodeMap attrMap = element.getAttributes();
        for (int i = 0; i < attrMap.getLength(); i++) {
            Node attr = attrMap.item(i);
            String attrName = attr.getNodeName();
            if (!RESERVED_XML_ATTRIBUTES.contains(attrName.toLowerCase()) && !attrName.contains(":")) { // Ignore namespaced attributes
                xsdContent.append(indent).append(INDENT).append(INDENT)
                          .append("<xs:attribute name=\"").append(attrName)
                          .append("\" type=\"xs:string\"/>\n");
            }
        }

        xsdContent.append(indent).append(INDENT).append("</xs:complexType>\n");
        xsdContent.append(indent).append("</xs:element>\n");
    }
}
