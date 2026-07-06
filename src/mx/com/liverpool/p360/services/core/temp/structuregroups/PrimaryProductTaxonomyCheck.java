package mx.com.liverpool.p360.services.core.temp.structuregroups;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class PrimaryProductTaxonomyCheck {

	public static void main(String[] args) {
		PrimaryProductTaxonomyCheck ppc = new PrimaryProductTaxonomyCheck();
		ppc.checkExistance();
		try {
			ppc.loadTemplates();
			ppc.useParentId = true;
			ppc.loadTemplates();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private final RESTWrapper rw = new RESTWrapper();
	private final XMLMisc xmm = rw.getXmm();
	
	private boolean useParentId = false;
	
	private void checkExistance() {
		java.util.Set<String> data = collectIDs();
		java.util.LinkedList<String> nd = new java.util.LinkedList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "uniquePFase2Lvl4").toString())))){
			String line = null;
			while((line = br.readLine()) != null) {
				nd.addLast(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		for(String ln : nd) {
			if(!data.contains(ln)) {
				System.out.println(ln);
			}
		}
	}
	
	private java.util.Set<String> collectIDs(){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", "PrimaryProductTaxonomy");
		qp.put("fields", "StructureGroup.Identifier");
		qp.put("query", "StructureGroup.Identifier wildcard \"EU%\"");
		final java.util.LinkedList<String> data = new java.util.LinkedList<>();
		rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			for(int i=0; i<values.length(); i++) {
				data.addLast(values.getString(0));
			}
		});
		return new java.util.TreeSet<>(data);
	}
	
	private void loadTemplates() throws ParserConfigurationException, SAXException, IOException {
		java.nio.file.Path path = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "Jerarquia completa niveles 1 a 4.xml");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse( path.toString() );
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"));
		if(useParentId)
			columns.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"));
		java.util.LinkedList<Node> productNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Products").getFirst() ).get("Product");
		if(productNodeList != null) {
			for(Node n : productNodeList) {
				iterateNode(n, ((Element)n).getAttribute("ID"), request);
			}
			if(rows.length() > 0) {
				rw.writeData("list", "StructureGroup", null, new java.util.TreeMap<>(), request, System.out::println);
			}
		}
	}
	
	private void iterateNode(Node n, String parentId, org.json.JSONObject request) {
		java.util.LinkedList<Node> childNodes = xmm.listImmediateChildElements(n).get("Product");
		if(childNodes != null) {
			Element el = null;
			for(Node n0 : childNodes) {
				el = (Element) n0;
				Node nn = xmm.byName(n0, "Name");
				org.json.JSONArray rows = request.getJSONArray("rows");
				if(useParentId) {
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID") + "'@'PrimaryProductTaxonomy'")).put("values", new org.json.JSONArray().put( nn == null ? "" : nn.getTextContent() ).put(parentId)));
				}else {
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID") + "'@'PrimaryProductTaxonomy'")).put("values", new org.json.JSONArray().put( nn == null ? "" : nn.getTextContent() )));
				}
				if(rows.length() == 100) {
					rw.writeData("list", "StructureGroup", null, new java.util.TreeMap<>(), request, System.out::println);
				}
				if(!el.getAttribute("ID").startsWith("EU4-")) {
					iterateNode(n0, el.getAttribute("ID"), request);
				}
			}
		}
	}
	
}
