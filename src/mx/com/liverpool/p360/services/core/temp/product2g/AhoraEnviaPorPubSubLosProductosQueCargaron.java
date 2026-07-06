package mx.com.liverpool.p360.services.core.temp.product2g;

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
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class AhoraEnviaPorPubSubLosProductosQueCargaron {

	static {
		System.out.println("Need to specify -DsourceDir, as well as -DcurrentlyProcessed, first one is a directory containing files with names ending in .xml, last one is file containing only a list of proposalIds to be skipped when encountered, this is, a bare flat file with a line per proposalId, no headers.");
	}
	
	private static final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
	private static final RESTWorkshop rwGP = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.servlets_url"), "Content-Type: application/json", "Accept: application/json");
	private static final org.json.JSONArray PRODUCTS_ARRAY = new org.json.JSONArray();
	private static final org.json.JSONObject PRODUCTS_OBJECT = new org.json.JSONObject().put("products", PRODUCTS_ARRAY);
	private static final String pubSubSA = PropertiesManager.get("p360.contingency.gcp.service_account_back");
	private static final String pubSubProject = PropertiesManager.get("p360.contingency.gcp.project_back");
	private static final String topic = PropertiesManager.get("p360.contingency.gcp.post_products_topic");
	private static final PubSubGCP ps = new PubSubGCP(pubSubSA,  pubSubProject,  topic);
	private static final java.nio.file.Path basePath = java.nio.file.Paths.get( System.getProperty("sourceDir") );
	private static final java.nio.file.Path fbdnIDs = java.nio.file.Paths.get( System.getProperty("currentlyProcessed") );
	
	public static void main(String[] args) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		AhoraEnviaPorPubSubLosProductosQueCargaron a = new AhoraEnviaPorPubSubLosProductosQueCargaron();
		a.processOverProposals();
	}
	
	private void sendProposal(String id) {
		PRODUCTS_ARRAY.put(new org.json.JSONObject().put("sku", "").put("proposalId", id));
		if(PRODUCTS_ARRAY.length() < 100) {
			return;
		}
		for(int i=0; i<PRODUCTS_ARRAY.length(); i++) {
			System.out.println("Sending: " + PRODUCTS_ARRAY.get(i));
		}
		sendProposals();
	}
	
	private void sendProposals() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject holi = new org.json.JSONObject().put("input", PRODUCTS_OBJECT.toString());
		rwGP.makeRequest("POST", "", qp, holi.toString());
		System.out.println(rwGP.getRawResponse());
		org.json.JSONArray losesos = new org.json.JSONArray(rwGP.getRawResponse());
		for(int i=0; i<losesos.length(); i++) {
			if(losesos.getJSONObject(i).has("currentStatus")) {
				System.out.println("Sent. " + ps.publishMessage(losesos.getJSONObject(i).toString()) + " - " + (PRODUCTS_ARRAY.getJSONObject(i).has("proposalId") ? "proposalId: " + PRODUCTS_ARRAY.getJSONObject(i).getString("proposalId") + " - " : "") + (PRODUCTS_ARRAY.getJSONObject(i).has("sku") ? "sku: " + PRODUCTS_ARRAY.getJSONObject(i).getString("sku") : ""));
			}else {
				System.out.println("Not sent. " + ps.publishMessage(losesos.getJSONObject(i).toString()) + " - " + (PRODUCTS_ARRAY.getJSONObject(i).has("proposalId") ? "proposalId: " + PRODUCTS_ARRAY.getJSONObject(i).getString("proposalId") + " - " : "") + (PRODUCTS_ARRAY.getJSONObject(i).has("sku") ? "sku: " + PRODUCTS_ARRAY.getJSONObject(i).getString("sku") : ""));
			}
		}
		while(PRODUCTS_ARRAY.length() > 0) {
			PRODUCTS_ARRAY.remove(0);
		}
	}
	
	private void sourceForbiddenProposalIds(java.util.LinkedList<String> ids) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(fbdnIDs.toString())))){
			String ln = null;
			while((ln = br.readLine()) != null) {
				ids.addLast(ln);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private void processOverProposals() throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		java.util.LinkedList<String> productIds = new java.util.LinkedList<>();
		collectProposalFromFiles(productIds);
		java.util.LinkedList<String> idsList = new java.util.LinkedList<>();
		sourceForbiddenProposalIds(idsList);
		java.util.Set<String> idsSet = new java.util.TreeSet<>(idsList);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "not Product2G.ProductNo is empty");
		qp.put("pageSize", "1200");
		int a = 0;
		int b = 0;
		int c = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					if(productIds.contains(values.getString(0))) {
						if(!idsSet.contains(values.getString(0))) {
							sendProposal(values.getString(0));
							c++;
						}
					}
				}
				a += response.getInt("pageSize");
				System.out.println(a + "/" + b);
			}else {
			}
		}while(a < b);
		a = 0;
		if(PRODUCTS_ARRAY.length() > 0) {
			sendProposals();
		}
		System.out.println(c);
	}
	
	private void collectProposalFromFiles(java.util.LinkedList<String> productIds) throws ParserConfigurationException, FileNotFoundException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
    	java.io.File[] files = new java.io.File(basePath.toString()).listFiles(ff->ff.getName().endsWith(".xml"));
    	XMLMisc xmm = new XMLMisc();
		for(java.io.File f : files) {
	    	doc = builder.parse(new java.io.FileInputStream(f));
			doc.getDocumentElement().normalize();
			java.util.LinkedList<Node> productNodes = xmm.listImmediateChildElements(xmm.listImmediateChildElements(doc.getDocumentElement()).get("Products").getFirst()).get("Product");
			Element e = null;
			String elide = null;
			for(Node productNode : productNodes) {
				e = (Element) productNode;
				elide = e.getAttribute("ID");
				if(e.getAttribute("ParentID").startsWith("EU4-"))
					productIds.addLast(elide);
			}
		}
	}
	
}
