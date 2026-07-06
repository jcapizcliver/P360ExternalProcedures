package mx.com.liverpool.p360.services.core.temp;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class BusquedaNombresDeProveedores {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws ParserConfigurationException, FileNotFoundException, SAXException, IOException, TransformerException {
		proveedoresConocidos();
	}

	public static java.util.Map<Long, Node> proveedoresConocidos() throws ParserConfigurationException, FileNotFoundException, SAXException, IOException, TransformerException {

		final String sourcePath = "C:\\Users\\jcapizc\\Downloads\\step-472799325730140560-exported.xml";

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		Document document = documentBuilder.parse(new java.io.File(sourcePath));
		document.getDocumentElement().normalize();

		Element documentElement = document.getDocumentElement();

		java.util.LinkedList<Node> clasificaciones = xmm.listImmediateChildElements(documentElement).get("Classifications");
		java.util.LinkedList<Node> listaNodosProveedor = xmm.listImmediateChildElements(clasificaciones.getFirst()).get("Classification");

		java.util.Map<Long, Node> supplierNodesByIds = new java.util.TreeMap<>();

		for(Node supplierNode : listaNodosProveedor) {
			try{
				supplierNodesByIds.put(Long.parseLong( ((Element)supplierNode).getAttribute("ID").replaceAll("-.+", "") ), supplierNode);
			}catch(NumberFormatException e) {
			}
		}
		System.out.println(supplierNodesByIds.size() + " vs " + listaNodosProveedor.size());
		return supplierNodesByIds;
	}



}
