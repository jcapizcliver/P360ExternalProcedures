package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.jcapiz.memelos.misc.RestClient;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class LoadCharacteristicsThatAreLookups {

	private static XMLMisc xmm = new XMLMisc();
	private static final String baseUrlDEV = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Set<String> characteristics = new java.util.TreeSet<>();

		int currentIndex = 0;
		int totalSize = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.DataType equals LOOKUP and Characteristic.IsActive equals false", "UTF-8")
					+ "&fields=Characteristic.Identifier&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					characteristics.add(values.getString(0));
					currentIndex++;
				}
			}while(currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
		currentIndex = 0;
		System.out.println("Currently we registered: " + characteristics.size() + " inactive characteristics that are lookup type.");
		try {


			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();
			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("AttributeList");
			Node attributeListRoot = lst.getFirst();
			java.util.LinkedList<Node> attributes = xmm.listImmediateChildElements(attributeListRoot).get("Attribute");

			String attId = null;
			Element en = null;
			java.util.LinkedList<Node> attributeListOfValueLink = null;
			java.util.Map <String, java.util.LinkedList <Node>> mep = null;

			for(Node n : attributes) {
				en = (Element)n;
				attId = en.getAttribute("ID");
				mep = xmm.listImmediateChildElements(n);
				if(mep != null) {
					attributeListOfValueLink = mep.get("ListOfValueLink");
					if(attributeListOfValueLink != null && !attributeListOfValueLink.isEmpty() && characteristics.contains(attId)) {
						System.out.println("Updating " + attId + " to have: " + ((Element)attributeListOfValueLink.getFirst()).getAttribute("ListOfValueID"));
						System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup")).put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + attId + "'")).put("values", new org.json.JSONArray().put(((Element) attributeListOfValueLink.getFirst()).getAttribute("ListOfValueID")).put(true)))).toString()) );
					}
				}else {
					System.out.println("This boy did not have any children: " + attId);
				}
			}

		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static String formatMillis(long millis){
	  	int days = (int)(millis/(1000*60*60*24));
	 	millis -= days*1000*60*60*24;
	  	int hours = (int) (millis/(1000*60*60));
	  	millis -= hours*1000*60*60;
	  	int minutes = (int) (millis/(1000*60));
	  	millis -= minutes*1000*60;
	  	int seconds = (int) (millis/1000);
	  	millis -= seconds*1000;
	  	return
	  		    (days < 10 ? "0" : "") + days + ":"
	  		+ (hours < 10 ? "0" : "") + hours + ":"
	  		+ (minutes < 10 ? "0" : "") + minutes + ":"
	  		+ (seconds < 10 ? "0" : "") + seconds
	  		+ "." + millis;
	  }
}
