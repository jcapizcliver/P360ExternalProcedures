package mx.com.liverpool.p360.services.core.category;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ArmaEquisEmeEle {

	private static final XMLMisc xmm = new XMLMisc();
	
	private void buildIt(String catId) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc = builder.newDocument();
    	Document docMKT = builder.newDocument();
    	Element spim = doc.createElement("STEP-ProductInformation");
    	spim.setAttribute("ExportTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new java.util.Date() ));
    	spim.setAttribute("ExportContext", "Context2");
    	spim.setAttribute("ContextID", "Context2");
    	spim.setAttribute("WorkspaceID", "Approved");
    	spim.setAttribute("UseContextLocale", "false");

    	Element classifications = doc.createElement("Classifications");
    	spim.appendChild(classifications);
    	Element raizClassification = doc.createElement("Classification");
    	raizClassification.setAttribute("ID", "Classification 1 root");
    	raizClassification.setAttribute("UserTypeID", "Classification 1 user-type root");
    	raizClassification.setAttribute("Selected", "false");
    	classifications.appendChild(raizClassification);
    	
    	Element webHierarchyRoot = doc.createElement("Classification");
    	webHierarchyRoot.setAttribute("ID", "WebHierarchyRoot");
    	webHierarchyRoot.setAttribute("UserTypeID", "WebHierarchyRoot");
    	webHierarchyRoot.setAttribute("Selected", "false");
    	raizClassification.appendChild(webHierarchyRoot);
    	
    	
    	
	}
	
	private void colectaElEse() {
		
	}
	
}
