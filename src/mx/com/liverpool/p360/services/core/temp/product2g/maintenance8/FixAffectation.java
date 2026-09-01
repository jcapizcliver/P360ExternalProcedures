package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class FixAffectation {

	
	public static void main(String[] args) throws ParserConfigurationException, SAXException {
		args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\Affectation.jsonl" };
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			org.json.JSONObject json = null;
			org.json.JSONObject entityItemChange = null;
			String changeSummary = null;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			String internalId = null;
			XMLMisc xmm = new XMLMisc();
			String oldIdentifier = null;
			String oldSKU = null;
			String oldCurrentStatus = null;
			String currID = null;
			RESTWrapper rw = new RESTWrapper();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("includeObjectsInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.ProductNo")).put(new org.json.JSONObject().put("identifier", "Product2G.SKU")).put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
			int count = 0;
			int nots = 0;
			int mehs = 0;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDsToPubSub__").toFile())))){
				while((line = br.readLine()) != null) {
					json = new org.json.JSONObject(line);
					if(json.has("entityItemChange")) {
						entityItemChange = json.getJSONObject("entityItemChange");
						changeSummary = entityItemChange.getString("_changeSummary");
						internalId = entityItemChange.getJSONObject("_entityItem").getString("_internalId");
				    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
				    		doc = builder.parse( baos );
				    		doc.getDocumentElement().normalize();
				    	}
				    	if(doc != null) {
							Element rootElement = (Element) doc.getDocumentElement();
							Node _n0 = xmm.byName((Node) rootElement, "product");
							if(_n0 != null) {
								Node _n1 = xmm.byName( xmm.byName((Node) rootElement, "product"), "identifier");
								Node _n2 = xmm.byName( xmm.byName((Node)rootElement, "product"), "sku");
								Node _n3 = xmm.byName( xmm.byName((Node)rootElement, "product"), "currentStatus");
								oldIdentifier = "";
								oldSKU = "";
								oldCurrentStatus = "";
								if(_n1 != null) {
									currID = xmm.byName( _n1, "_current") .getTextContent();
									if(currID.startsWith("__")) {
										count++;
										oldIdentifier =    xmm.byName( _n1, "_old") .getTextContent();
										if(_n2 != null) {
											oldSKU =           xmm.byName( _n2, "_old") .getTextContent();
										}
										if(_n3 != null) {
											Node _n4 = xmm.byName( _n3, "_old");
											if(_n4 != null) {
												oldCurrentStatus = xmm.byName( _n4, "_key") .getTextContent();
											}else {
												oldCurrentStatus = "";
											}
										}
										rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId)).put("values", new org.json.JSONArray().put(oldIdentifier).put(oldSKU).put(oldCurrentStatus)));
										pw.println( oldIdentifier );
									}
								}
							}else {
								mehs++;
							}
				    	}
					}else {
						nots++;
					}
				}
				rh.sendData();
				System.out.println(count);
				System.out.println(nots);
				System.out.println(mehs);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
