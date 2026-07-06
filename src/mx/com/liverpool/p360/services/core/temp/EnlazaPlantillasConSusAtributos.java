package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class EnlazaPlantillasConSusAtributos {

	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = new XMLMisc();

	private static java.util.Set<String> notFoundTamañoÚnico = new java.util.TreeSet<>();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		java.util.Map<String, java.util.Set<String>> lkpCodes = readLookups();
		java.util.Map<String, org.json.JSONArray> destinyBoard = new java.util.TreeMap<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Products");
		Node productsRoot = lst.getFirst();
		java.util.LinkedList<Node> product0 = xmm.listImmediateChildElements(productsRoot).get("Product");
		java.util.Map<String, String> attributeLookups = attributeLookups(rootElement);
		java.util.Map<String, String> lookupsContent = loadTamañoÚnico();
		System.out.println( ((Element)product0.getFirst()).getAttribute("ID") );

		java.util.LinkedList<Node> product1 = xmm.listImmediateChildElements(product0.getFirst()).get("Product");
		for(Node n : product1) {
			processNode(n, destinyBoard, attributeLookups, lookupsContent, lkpCodes);
		}
		org.json.JSONArray rows = null;
		String lookup = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		for(java.util.Map.Entry<String, org.json.JSONArray> entry : destinyBoard.entrySet()) {
			rows = entry.getValue();
			lookup = entry.getKey();
			if(rows.length() > 0) {
				System.out.println(lookup + " - " + workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(" + lookup + ")"))).put("rows", rows).toString()));
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		notFoundTamañoÚnico.forEach(System.out::println);
	}

	private static java.util.Map<String, String> attributeLookups(Element root){
		java.util.Map<String, String> attributeLookups = new java.util.TreeMap<>();
		java.util.LinkedList<Node> attributes = xmm.listImmediateChildElements( xmm.listImmediateChildElements(root).get("AttributeList").getFirst() ).get("Attribute");
		Node lovLink = null;
		for(Node n : attributes) {
			lovLink = xmm.byName(n, "ListOfValueLink");
			if(lovLink != null) {
				attributeLookups.put(((Element)n).getAttribute("ID"), ((Element)lovLink).getAttribute("ListOfValueID"));
			}
		}
		return attributeLookups;
	}

	private static java.util.Map<String, java.util.Set<String>> readLookups(){
		java.util.Map<String, java.util.Set<String>> lkpValues = new java.util.TreeMap<>();
		java.util.Set<String> codes = null;
		org.json.JSONObject resp = null;
		org.json.JSONArray rws = null;
		org.json.JSONArray vls = null;

		java.util.Map<String, String> qparams = new java.util.TreeMap<>();
		qparams.put("fields", "Lookup.Identifier");
		qparams.put("query", "not Lookup.Identifier is empty");
		qparams.put("pageSize", "1000");

		int currentIndex = 0;
		int totalSize = 0;

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("pageSize", "1000");

		int ci = 0;
		int tz = 0;

		String lookup = null;

		do {
			qparams.put("startIndex", String.valueOf(currentIndex));
			resp = workshop.makeRequest("GET", "/list/Lookup/bySearch", qparams, null);
			totalSize = resp.getInt("totalSize");
			rws = resp.getJSONArray("rows");
			for(int j = 0; j<rws.length(); j++) {
				currentIndex++;
				vls = rws.getJSONObject(j).getJSONArray("values");
				lookup = vls.getString(0);
				if(!"TamanoUnico".equals(lookup)) {
					codes = new java.util.TreeSet<>();
					do {
						qp.put("lookup", lookup);
						qp.put("startIndex", String.valueOf(ci));
						response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
						tz = response.getInt("totalSize");
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							ci++;
							values = rows.getJSONObject(i).getJSONArray("values");
							codes.add(values.getString(0));
						}
					}while(ci < tz);
					ci = 0;
					lkpValues.put(lookup, codes);
				}
				System.out.println( 100f*(((float)currentIndex)/(totalSize)) + "%" );
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return lkpValues;
	}

	private static java.util.Map<String, String> loadTamañoÚnico() {

		java.util.Map<String, String> lkpValues = new java.util.TreeMap<>();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("pageSize", "1000");
		qp.put("lookup", "TamanoUnicoLOV");

		int ci = 0;
		int tz = 0;

		do {
			qp.put("startIndex", String.valueOf(ci));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			tz = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				ci++;
				values = rows.getJSONObject(i).getJSONArray("values");
				lkpValues.put(values.getString(1), values.getString(0));
			}
		}while(ci < tz);
		ci = 0;
		return lkpValues;
	}

	private static void processNode(Node n, java.util.Map<String, org.json.JSONArray> destinyBoard, java.util.Map<String, String> attributeLookups, java.util.Map<String, String> tamañoÚnicoLOV, java.util.Map<String, java.util.Set<String>> lkpCodes) {
		String attributeId = null;
		org.json.JSONArray rows = null;
		Element el = null;
		java.util.Map<String, java.util.LinkedList<Node>> attributeChildren = null;
		java.util.LinkedList<Node> vf = null;
		java.util.LinkedList<Node> value = null;
		java.util.Map<String, java.util.LinkedList<Node>> childElements = xmm.listImmediateChildElements(n);
		java.util.Map<String, String> lookupContent = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		String code = null;
		String content = null;
		org.json.JSONArray codes = null;
		java.util.Set<String> cds = null;
		if(childElements == null || childElements.isEmpty()) {
			return;
		}
		java.util.LinkedList<Node> products = childElements.get("Product");
		if(products != null) {
			for(Node node : products) {
				processNode(node, destinyBoard, attributeLookups, tamañoÚnicoLOV, lkpCodes);
			}
		}else {
			String id = ((Element)n).getAttribute("ID");
			if(id.startsWith("EU4-")) {
				String lookup = null;
				java.util.LinkedList<Node> attributeLinks = childElements.get("AttributeLink");
				if(attributeLinks != null) {
					for(Node at : attributeLinks) {
						el = (Element) at;
						attributeId = el.getAttribute("AttributeID");
						lookup = attributeLookups.get( attributeId );
						if(lookup != null) {
							attributeChildren = xmm.listImmediateChildElements(at);
							if(attributeChildren != null) {
								vf = attributeChildren.get("ValueFilter");
								if(vf != null) {
									value = xmm.listImmediateChildElements(vf.getFirst()).get("Value");
									if(value != null) {
										rows = destinyBoard.get(lookup);
										if(rows == null) {
											rows = new org.json.JSONArray();
											destinyBoard.put(lookup, rows);
										}
										codes = new org.json.JSONArray();
										cds = lkpCodes.get(lookup);
										if(cds != null) {
											for(Node v : value) {
												content = v.getTextContent();
												code = "TamanoUnico".equals( attributeId ) ? tamañoÚnicoLOV.get(content) : cds.contains( ((Element)v).getAttribute("ID") ) ? ((Element)v).getAttribute("ID") : null;
												if(code != null) {
													codes.put(code);
												}else {
													notFoundTamañoÚnico.add(content);
												}
											}
											rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "_" + attributeId + "'@'TemplateCharacteristic'")).put("values", new org.json.JSONArray().put(codes)));
											if(rows.length() == 250) {
												System.out.println(id + " - " + attributeId + " (" + lookup + ") - " + workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(" + lookup + ")"))).put("rows", rows).toString()));
												while(rows.length() > 0) {
													rows.remove(0);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}
