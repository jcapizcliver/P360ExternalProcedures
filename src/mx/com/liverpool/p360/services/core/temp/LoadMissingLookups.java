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

public class LoadMissingLookups {

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

		java.util.Set<String> lookups = new java.util.TreeSet<>();

		int currentIndex = 0;
		int totalSize = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Lookup/bySearch?query=" + java.net.URLEncoder.encode("not Lookup.Identifier is empty", "UTF-8")
					+ "&fields=Lookup.Identifier&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					lookups.add(values.getString(0));
					currentIndex++;
				}
			}while(currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
		currentIndex = 0;
		System.out.println("Currently we registered: " + lookups.size() + " lookups.");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();
			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("ListsOfValues");
			Node listOfValuesRoot = lst.getFirst();
			java.util.LinkedList<Node> listOfValues = xmm.listImmediateChildElements(listOfValuesRoot).get("ListOfValue");

			String lkpId = null;
			Element en = null;
			Element lookupName = null;
			java.util.LinkedList<Node> lookupValues = null;
			org.json.JSONArray losmemejes = new org.json.JSONArray();

			for(Node n : listOfValues) {
				en = (Element)n;
				lkpId = en.getAttribute("ID");
				if(!lookups.contains(lkpId)) {
					System.out.println("Loading data for: " + lkpId);
					lookupName = (Element) xmm.byName(n, "Name");
					System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/Lookup", new org.json.JSONObject()
							.put("columns", new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)"))
									.put(new org.json.JSONObject().put("identifier", "LookupLang.Name(en)")))
							.put("rows", new org.json.JSONArray()
									.put(new org.json.JSONObject()
											.put("object", new org.json.JSONObject().put("id", "'" + lkpId + "'"))
											.put("values", new org.json.JSONArray()
													.put(lookupName.getTextContent())
													.put(lookupName.getTextContent())))).toString()) );
					lookupValues = xmm.listImmediateChildElements(n).get("Value");
					if(lookupValues != null) {
						for(Node lkpv : lookupValues) {
							en = (Element) lkpv;
							losmemejes.put(
									new org.json.JSONObject()
									.put("object", new org.json.JSONObject().put("id", "'" + en.getAttribute("ID") + "'@'" + lkpId + "'"))
									.put("values", new org.json.JSONArray()
											.put(en.getTextContent())
											.put(en.getTextContent())
											.put(true)));
							if(losmemejes.length() == 500) {
								System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/LookupValue", new org.json.JSONObject()
										.put("columns", new org.json.JSONArray()
												.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
												.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(en)"))
												.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive")))
										.put("rows", losmemejes).toString()));
								while(losmemejes.length() > 0) {
									losmemejes.remove(0);
								}
							}
						}
						if(losmemejes.length() > 0) {
							System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/LookupValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(en)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", losmemejes).toString()));
							while(losmemejes.length() > 0) {
								losmemejes.remove(0);
							}
						}
					}
				}else {
					System.out.println("Lookup already in base " + lkpId);
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
