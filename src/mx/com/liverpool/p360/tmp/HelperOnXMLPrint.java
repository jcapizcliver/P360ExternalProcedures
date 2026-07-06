package mx.com.liverpool.p360.tmp;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class HelperOnXMLPrint {

	private static final XMLMisc xmm = new XMLMisc();

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(args[0]); //& builder.parse("D:\\tmp\\CARDMP36020250501123454.XML");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();

		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(args[1])))){
			pw.println( xmm.prettyPrint(rootElement));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

}
