package mx.com.liverpool.p360.services.core.temp.lookup;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class LoadLookupContentFromSTEPXML {

	
	public static void main(String[] args) {
		
		RESTWorkshop rw = new RESTWorkshop(true, "https://webctep360pro.liverpool.com.mx/rest/V2.0", "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
//		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
//		rw.makeRequest("GET", "/object/Lookup/3085", qp0, null);
//		System.out.println(rw.getRawResponse());
//		System.exit(0);
		XMLMisc xmm = rw.getXmm();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		java.nio.file.Path path = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "ele ka pés", "step-15566293271882182364-exported.xml");
		java.nio.file.Path path = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "ele ka pés", "step-7376726717382597010-exported (1).xml");
//		java.nio.file.Path path = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "ele ka pés", "LOV Att VAD PRO (1).xml");
		try(java.io.FileInputStream fis = new java.io.FileInputStream(path.toString())){
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	Document doc;
    		doc = builder.parse( fis );
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();
			java.util.Map<String, java.util.LinkedList<Node>> map = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> listsOfValues = map.get("ListsOfValues");
			Element listsOfValuesElement = (Element) listsOfValues.getFirst();
			map = xmm.listImmediateChildElements(listsOfValuesElement);
			java.util.LinkedList<Node> listOfValuesNodes = map.get("ListOfValue");
			Element listOfValuesElement = null;
			Node listOfValuesNameElement = null;
			String listOfValuesId = null;
			String listOfValuesName = null;
			org.json.JSONObject response = null;
			org.json.JSONObject request = new org.json.JSONObject();
			org.json.JSONArray columns = new org.json.JSONArray();
			org.json.JSONArray rows = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)"));
			request.put("columns", columns);
			request.put("rows", rows);
			java.util.LinkedList<Node> valueNodes = null;
			Element valueElement = null;
			String valueId = null;
			String valueText = null;
			int counter = 0;
			for(Node listOfValuesNode : listOfValuesNodes) {
				listOfValuesElement = (Element) listOfValuesNode;
				listOfValuesId = listOfValuesElement.getAttribute("ID");
				listOfValuesNameElement = xmm.byName(listOfValuesNode, "Name");
				listOfValuesName = listOfValuesNameElement != null ? listOfValuesNameElement.getTextContent() : null;
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + listOfValuesId + "'")).put("values", new org.json.JSONArray().put(listOfValuesName != null ? listOfValuesName : "")));
				if(rows.length() == 200) {
					response = rw.makeRequest("POST", "/list/Lookup", qp, request.toString());
					if(response == null) {
						System.out.println("-->" + rw.getRawResponse() + "<--");
						rw.getException().printStackTrace();
					}else {
						System.out.println(response);
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			counter = 0;
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/Lookup", qp, request.toString());
				if(response == null) {
					System.out.println("-->" + rw.getRawResponse() + "<--");
					rw.getException().printStackTrace();
				}else {
					System.out.println(response);
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
			columns.remove(0);
			columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
			columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
			System.out.println("EI **");
			for(Node listOfValuesNode : listOfValuesNodes) {
				listOfValuesElement = (Element) listOfValuesNode;
				listOfValuesId = listOfValuesElement.getAttribute("ID");
				System.out.println("Hola: " + listOfValuesId);
				if(!"TamanoUnicoLOV".equals(listOfValuesId))
					continue;
				System.out.println("PASS");
				map = xmm.listImmediateChildElements(listOfValuesNode);
				valueNodes = map.get("Value");
				if(valueNodes != null) {
					for(Node valueNode : valueNodes) {
						counter++;
						valueElement = (Element) valueNode;
						valueId = valueElement.hasAttribute("ID") ? valueElement.getAttribute("ID") : valueElement.getTextContent();
						valueText = valueElement.getTextContent();
						rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + valueId.replaceAll("'", "\\\\'") + "'@'" + listOfValuesId + "'")).put("values", new org.json.JSONArray().put(valueText).put(true)));
						if(rows.length() == 200) {
							response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
							if(response == null) {
								System.out.println("-->" + rw.getRawResponse() + "<--");
								rw.getException().printStackTrace();
							}else {
								System.out.println(response.remove("counters"));
							}
							while(rows.length() > 0) {
								rows.remove(0);
							}
						}
					}
					System.out.println(":: " + counter);
				}
			}
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				if(response == null) {
					System.out.println("-->" + rw.getRawResponse() + "<--");
					rw.getException().printStackTrace();
				}else {
					System.out.println(response);
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
	}
	
}
