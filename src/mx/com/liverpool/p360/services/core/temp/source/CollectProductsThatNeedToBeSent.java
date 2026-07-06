package mx.com.liverpool.p360.services.core.temp.source;

import java.io.FileNotFoundException;
import java.io.IOException;

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

public class CollectProductsThatNeedToBeSent {

	private static final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
	
	public static void main(String[] args) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		CollectProductsThatNeedToBeSent co = new CollectProductsThatNeedToBeSent();
		java.util.Map<String, String> pIds = new java.util.TreeMap<>();
		java.util.Set<String> vIds = new java.util.TreeSet<>();
		java.util.LinkedList<String> commonPIds = new java.util.LinkedList<>();
		co.collectProposalFromFiles(pIds, vIds);
		co.collectProductsFromP360(commonPIds);
		int a = 0;
		int b = 0;
		for(String p : pIds.keySet()) {
			if(!commonPIds.contains(p)) {
				a++;
				System.out.println(p + " - " + pIds.get(p));
			}else {
				b++;
			}
		}
		System.out.println(a);
		System.out.println(b);
	}
	
	private void collectProductsFromP360(java.util.LinkedList<String> commons) {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code");
		qp.put("query", "characteristic('Business',-1) is empty");
		qp.put("pageSize", "250");
		int a = 0;
		int b = 0;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					commons.add(values.getString(0));
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
	}
	
	private void collectProposalFromFiles(java.util.Map<String, String> productIds, java.util.Set<String> variantIds) throws ParserConfigurationException, FileNotFoundException, SAXException, IOException {
		java.util.LinkedList<String> variants = new java.util.LinkedList<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
    	String basePath = "C:\\opt\\LVP\\desorden\\Migración\\21082025\\data";
    	java.io.File[] files = new java.io.File(basePath).listFiles(ff->ff.getName().endsWith(".xml"));
    	XMLMisc xmm = new XMLMisc();
		for(java.io.File f : files) {
	    	doc = builder.parse(new java.io.FileInputStream(f));
			doc.getDocumentElement().normalize();
			java.util.Map<String, java.util.LinkedList<Node>> childElementsMap = null;
			java.util.LinkedList<Node> productNodes = xmm.listImmediateChildElements( 
															xmm.listImmediateChildElements(doc.getDocumentElement()).get("Products").getFirst()
													).get("Product");
			java.util.LinkedList<Node> childProductNodes = null;
			Element e = null;
			String elide = null;
			String attributeId = null;
			for(Node productNode : productNodes) {
				e = (Element) productNode;
				childElementsMap = xmm.listImmediateChildElements(productNode);
				childProductNodes = childElementsMap.get("Product");
				elide = e.getAttribute("ID");
				java.util.LinkedList<Node> valueNodes = xmm.listImmediateChildElements( xmm.listImmediateChildElements(e).get("Values").getFirst() ).get("Value");
				for(Node vn : valueNodes) {
					e = (Element) vn;
					attributeId = e.getAttribute("AttributeID");
					if("Margen".equals(attributeId)) {
						productIds.put(elide, vn.getTextContent());
						break;
					}
				}
				if(childProductNodes != null) {
					for(Node cpn : childProductNodes) {
						variants.addLast(((Element)cpn).getAttribute("ID"));
					}
				}
			}
		}
		variantIds.addAll(variants);
	}
}
