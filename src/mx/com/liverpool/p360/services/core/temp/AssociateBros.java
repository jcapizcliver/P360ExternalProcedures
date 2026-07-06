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

public class AssociateBros {

	private static XMLMisc xmm = new XMLMisc();
	private static final String baseUrlDEV = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded);

	private static final java.util.Set<String> currentCharacteristicCategories = new java.util.TreeSet<>();
	private static final java.util.Set<String> missingCategories = new java.util.TreeSet<>();
	private static final java.util.Set<String> missclassifiedCharacteristics = new java.util.TreeSet<>();
	private static final java.util.Set<String> characteristicsInMasterData = new java.util.TreeSet<>();
	private static final java.util.Set<String> characteristicsInLogisticData = new java.util.TreeSet<>();
	private static final java.util.Set<String> dissabledCharacteristics = new java.util.TreeSet<>();
	private static final java.util.Set<String> theseNeedToBeTurnedOn = new java.util.TreeSet<>();
	private static final java.util.Map<String, String> currentCharacteristics = new java.util.TreeMap<>();


	private static final java.util.Set<String> attributeCharacteristics = new java.util.TreeSet<>();

	private static final java.util.Map<String, java.util.LinkedList<String>> templateMissingCharacteristics = new java.util.TreeMap<>();
	private static final java.util.Map<String, java.util.LinkedList<String>> templateMissplacedCharacteristics = new java.util.TreeMap<>();

	public static void main(String[] args) {
//		reassociateProductAndItemsToPantallas(); // For those records that lost classification to PPT
//		System.exit(0);
		long init = System.currentTimeMillis();
//		enableDissabledCharacteristics();
//		System.exit(0);
		loadCurrentCharacteristicCategories();
		loadCurrentCharacteristics();
		loadMasterDataCharacteristic();
		loadDissabledCharacteristics();
		System.out.println("Now going to process pph source file...");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();

			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Products");
			Node productsRoot = lst.getFirst();
			Node webHierarcyRoot = xmm.byAttributeValue(productsRoot, "ID", "ProductsSuppliersPortal");
			String newName = null;

			java.util.LinkedList<Node> primerosNodos = null;

			Element jerarquiaWeb = (Element) webHierarcyRoot;

			newName = "PrimaryProductTaxonomy";
			primerosNodos = xmm.listImmediateChildElements(jerarquiaWeb).get("Product");
			for(Node pn : primerosNodos) {
				System.out.println("Ostia " + ((Element)pn).getAttribute("ID"));
//				detectMissingAttributesForTemplate(pn, newName);
				processTemplateAttributes(pn, newName);
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pph_missing_characteristics_by_template")))){ templateMissingCharacteristics.forEach((k,v)->pw.println(k + ";" + String.join(",", v))); }catch(java.io.IOException e) { e.printStackTrace(); }
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pph_dissabled_characteristics")))){ theseNeedToBeTurnedOn.forEach((k)->pw.println(k)); }catch(java.io.IOException e) { e.printStackTrace(); }
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pph_missplaced_characteristics_by_template")))){ templateMissplacedCharacteristics.forEach((k,v)->pw.println(k + ";" + String.join(",", v))); }catch(java.io.IOException e) { e.printStackTrace(); }
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pph_characteristics_that_are_for_item_only")))){ attributeCharacteristics.forEach(at->pw.println(at)); }catch(java.io.IOException e) { e.printStackTrace(); }
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pph_missing_categories")))){ missingCategories.forEach(at->pw.println(at)); }catch(java.io.IOException e) { e.printStackTrace(); }
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\pph_missclassified_characteristics")))){ missclassifiedCharacteristics.forEach(at->pw.println(at)); }catch(java.io.IOException e) { e.printStackTrace(); }
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static void reassociateProductAndItemsToPantallas() {
		System.out.println("Loading orphane boys...");
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int currentIndex = 0;
		org.json.JSONArray nr = new org.json.JSONArray();
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Product2G/bySearch?query=" + java.net.URLEncoder.encode("Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy) is empty", "UTF-8") + "&fields=Product2G.ProductNo&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					nr.put(new org.json.JSONObject().put("object", rows.getJSONObject(i).getJSONObject("object")).put("values", new org.json.JSONArray().put("EU4-113578")));
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Product2G", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap(PrimaryProductTaxonomy)"))).put("rows", nr).toString());
			System.out.println(rawResponse);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		System.exit(0);
		nr = new org.json.JSONArray();
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Article/bySearch?query=" + java.net.URLEncoder.encode("ArticleStructureMap.StructureGroup(PrimaryProductTaxonomy) is empty", "UTF-8") + "&fields=Article.SupplierAID&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					nr.put(new org.json.JSONObject().put("object", rows.getJSONObject(i).getJSONObject("object")).put("values", new org.json.JSONArray().put("EU4-113578")));
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Article", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleStructureMap.ManualMap(PrimaryProductTaxonomy)"))).put("rows", nr).toString());
			System.out.println(rawResponse);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadDissabledCharacteristics() {
		System.out.println("Loading dissabled characteristic categories...");
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int currentIndex = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.IsActive equals false", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					dissabledCharacteristics.add(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}

			}while(currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void enableDissabledCharacteristics() {
		java.util.LinkedList<String> characteristics = new java.util.LinkedList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\pph_dissabled_characteristics")))){
			String line = null;
			while((line = br.readLine()) != null) {
				characteristics.addLast(line);
			}
		}catch(java.io.IOException e) { e.printStackTrace(); }
		String rawResponse = null;
		org.json.JSONArray rows = null;
		try {
			rows = new org.json.JSONArray();
			for(String characteristic : characteristics) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + characteristic + "'")).put("values", new org.json.JSONArray().put(characteristic).put(true)));
			}
			rawResponse = rc.getRequest("POST", baseUrlDEV + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Category")).put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rows).toString());
			System.out.println(rawResponse);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadCurrentCharacteristicCategories() {
		System.out.println("Loading characteristic categories...");
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int currentIndex = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/LookupValue/byLookup?lookup=CharacteristicCategories&fields=LookupValue.Code&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					currentCharacteristicCategories.add(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}

			}while(currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadLogisticDataCharacteristic() { //DatosLogisticos
		System.out.println("Loading logistic data characteristics...");
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int currentIndex = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code equals \"DatosLogisticos\"", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					characteristicsInLogisticData.add(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}

			}while(currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadMasterDataCharacteristic() {
		System.out.println("Loading master data characteristics...");
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int currentIndex = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code equals \"Master Data\"", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					characteristicsInMasterData.add(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}

			}while(currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void loadCurrentCharacteristics() {
		System.out.println("Loading current characteristics");
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.IsActive equals true", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("Characteristic.Identifier,Characteristic.Category->LookupValue.Code", "UTF-8") + "&pageSize=500&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					currentCharacteristics.put(values.getString(0), values.getString(1));
				}

			}while(currentIndex < totalSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void processTemplateAttributes(Node n, String structureId) {
		Element el = (Element)n;
		String elementId = el.getAttribute("ID");
		if(elementId.startsWith("EU4")) {
			java.util.LinkedList<Node> attributeLinks = xmm.listImmediateChildElements(el).get("AttributeLink");
			String category = null;
			String attributeId = null;
			org.json.JSONArray categories = new org.json.JSONArray();
			categories.put("Master Data");
			for(Node at : attributeLinks) {
				category = currentCharacteristics.get(attributeId = ((Element)at).getAttribute("AttributeID"));
				attributeCharacteristics.add(attributeId);
				if(category == null) {
					missingCategories.add(attributeId);
				} else
				if(!category.equals(attributeId) && !characteristicsInMasterData.contains(attributeId) && !characteristicsInLogisticData.contains(attributeId)) {
					missclassifiedCharacteristics.add(attributeId);
				} else {
					categories.put(attributeId);
				}
			}
			try {
				System.out.println("Values for (" + elementId + ")" +
					rc.getRequest("POST", baseUrlDEV + "/list/StructureGroup", new org.json.JSONObject().put("columns",
						new org.json.JSONArray()
							.put(new org.json.JSONObject().put("identifier", "StructureGroup.CharacteristicCategories"))
						).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + elementId + "'@'" + structureId + "'")).put("values", new org.json.JSONArray().put(categories)))).toString()
					)
				)
				;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else {
			java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Product");
			if(children != null && !children.isEmpty()) {
				for(Node nn : children) {
					processTemplateAttributes(nn, structureId);
				}
			}
		}
	}

	private static void detectMissingAttributesForTemplate(Node n, String structureId) {
		Element el = (Element)n;
		String elementId = el.getAttribute("ID");
		if(elementId.startsWith("EU4")) {
			java.util.LinkedList<Node> attributeLinks = xmm.listImmediateChildElements(el).get("AttributeLink");
			String category = null;
			String attributeId = null;
			java.util.LinkedList<String> missplacedCharacteristics = new java.util.LinkedList<>();
			java.util.LinkedList<String> missingCharacteristics = new java.util.LinkedList<>();
			for(Node at : attributeLinks) {
				category = currentCharacteristics.get(attributeId = ((Element)at).getAttribute("AttributeID"));
				if(category == null) {
					if(!dissabledCharacteristics.contains(attributeId)) {
						missingCharacteristics.addLast(attributeId);
					}else {
						theseNeedToBeTurnedOn.add(attributeId);
					}
				} else
				if(!category.equals(attributeId) && !characteristicsInMasterData.contains(attributeId)) {
					missplacedCharacteristics.add(attributeId);
				}
			}
			if(!missingCharacteristics.isEmpty()) {
				templateMissingCharacteristics.put(elementId, missingCharacteristics);
			}
			if(!missplacedCharacteristics.isEmpty()) {
				templateMissplacedCharacteristics.put(elementId, missplacedCharacteristics);
			}
		}else {
			java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Product");
			if(children != null && !children.isEmpty()) {
				for(Node nn : children) {
					detectMissingAttributesForTemplate(nn, structureId);
				}
			}
		}
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
