package mx.com.liverpool.p360.services.core.temp.lookup;

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

public class TamañoÚnico {

	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	private static final java.util.Map<String, java.util.Set<String>> tallas = new java.util.TreeMap<>();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Map<String, String> tamañoÚnico = new java.util.TreeMap<>();
		java.util.Map<String, String> únicoTamaño = new java.util.TreeMap<>();

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "TamanoUnicoLOV");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("pageSize", "1000");

		int totalSize = 0;
		int currentIndex = 0;
		System.out.println("Reading Tamaño Único");
		if(java.nio.file.Files.exists(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\TamañoÚnico"))) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( "C:\\opt\\LVP\\desorden\\TamañoÚnico" ) ))){
				String line = null;
				while((line = br.readLine()) != null) {
					values = new org.json.JSONArray(line);
					tamañoÚnico.put(values.getString(0), values.getString(1));
					únicoTamaño.put(values.getString(1), values.getString(0));
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( "C:\\opt\\LVP\\desorden\\TamañoÚnico" ) ))){
				do {
					qp.put("startIndex", String.valueOf( currentIndex ) );

					response = workshop.makeRequest("GET", "/list/LookupValue/byLookup", qp, null);
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						tamañoÚnico.put(values.getString(0), values.getString(1));
						únicoTamaño.put(values.getString(1), values.getString(0));
						pw.println(values);
					}

				}while(currentIndex < totalSize);
				currentIndex = 0;
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Processing file");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\opt\\LVP\\tmp\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();
			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Products");
			Node root = lst.getFirst();
			java.util.LinkedList<Node> products = xmm.listImmediateChildElements(root).get("Product");
			org.json.JSONArray rowsPayload = new org.json.JSONArray();
			for(Node n : products) {
				drillIntoTemplates((Element)n, tamañoÚnico, únicoTamaño, rowsPayload);
			}
//			if(rowsPayload.length() == 200) {
//				System.out.println( workshop.makeRequest("POST", "/list/StandardizationValue", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).put("rows", rowsPayload).toString()) );
//				while(rowsPayload.length() > 0) {
//					rowsPayload.remove(0);
//				}
//			}
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}

		System.out.println(tallas.size());

	}

	private static void drillIntoTemplates(Element e, java.util.Map<String, String> tamañoÚnico, java.util.Map<String, String> únicoTamaño, org.json.JSONArray rowsPayload) {
		String id = e.getAttribute("ID");
		if(id.startsWith("EU4-")) {
			java.util.LinkedList<Node> attributeLinkNodes = xmm.listImmediateChildElements(e).get("AttributeLink");
			Element el = null;
			java.util.LinkedList<String> etiquetas = new java.util.LinkedList<>();
			java.util.LinkedList<String> llaves = new java.util.LinkedList<>();
			java.util.Map<String, String> mapa = new java.util.TreeMap<>();
			for(Node n : attributeLinkNodes) {
				el = (Element)n;
				if("TamanoUnico".equals(el.getAttribute("AttributeID"))) {
					java.util.Set<String> tallas = new java.util.TreeSet<>();
					if(xmm.listImmediateChildElements(el).get("ValueFilter") != null && !xmm.listImmediateChildElements(el).get("ValueFilter").isEmpty()) {
						java.util.LinkedList<Node> nds = xmm.listImmediateChildElements( xmm.listImmediateChildElements(el).get("ValueFilter").getFirst() ).get("Value");
						StringBuilder sb = new StringBuilder();
						StringBuilder sb0 = new StringBuilder();
						int a = 0;
						for(Node n0 : nds) {
							etiquetas.addLast(n0.getTextContent());
							sb.append(a == 0 ? "" : ",");
							sb.append("\"");
							sb.append(n0.getTextContent().replaceAll("\"", "\\\"").replaceAll("null", ""));
							sb.append("\"");
							sb0.append(a == 0 ? "" : ",").append( únicoTamaño.get(n0.getTextContent()) );
							llaves.addLast(únicoTamaño.get(n0.getTextContent()));
							a++;
							tallas.add(n0.getTextContent());
						}
						TamañoÚnico.tallas.put(id, tallas);
//						System.out.println("(" + id + ") Etiquetas: " + etiquetas.size() + " vs " + llaves.size() + " Llaves");
//						System.out.println("Filtro: " + sb.toString());
//						System.out.println("Keys: " + sb0.toString());

//						rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "_" + "TamanoUnico_CreateProposal_ListOfValuesFilter" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(id).put("TamanoUnico").put("CreateProposal").put("ListOfValuesFilter").put(sb0.toString())));
//						if(rowsPayload.length() == 200) {
//							System.out.println( workshop.makeRequest("POST", "/list/StandardizationValue", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).put("rows", rowsPayload).toString()) );
//							while(rowsPayload.length() > 0) {
//								rowsPayload.remove(0);
//							}
//						}
						sb.setLength(0);
					}
				}
			}
		}else {
			java.util.LinkedList<Node> children = xmm.listImmediateChildElements(e).get("Product");
			if(children != null) {
				for(Node n : children) {
					drillIntoTemplates((Element)n, tamañoÚnico, únicoTamaño, rowsPayload);
				}
			}
		}
	}
}
