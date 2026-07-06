package mx.com.liverpool.p360.services.core.temp.characteristic;

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

public class AsociaATGDisplayGroupsToCharacteristics {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	private static final java.util.Map<String, String> qp = new java.util.TreeMap<>();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc;
		doc = builder.parse("C:\\opt\\LVP\\tmp\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("AttributeList");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> atributos = xmm.listImmediateChildElements(assetsRoot).get("Attribute");
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, org.json.JSONArray> purposes = bringMeThePurposes();
		atributos.forEach(at->{
				org.json.JSONArray currentPurposes = null;
				currentPurposes = purposes.get(((Element)at).getAttribute("ID"));
				if(currentPurposes == null) {
					currentPurposes = new org.json.JSONArray();
					purposes.put(((Element)at).getAttribute("ID"), currentPurposes);
				}
				java.util.LinkedList<Node> metaData = xmm.listImmediateChildElements(at).get("MetaData");
				if(metaData != null) {
					java.util.LinkedList<Node> multiValue = xmm.listImmediateChildElements( metaData.getFirst() ).get("MultiValue");
					if(multiValue != null) {
						multiValue.forEach(mvn->{
							if("isAttInGroupAtt".equals(((Element)mvn).getAttribute("AttributeID"))) {
								java.util.LinkedList<Node> vnlist = xmm.listImmediateChildElements(mvn).get("Value");
								if(vnlist != null) {
									org.json.JSONArray parr = purposes.get(((Element)at).getAttribute("ID"));
									vnlist.forEach(vne->{
										parr.put(new org.json.JSONObject().put("id", "'" + ((Element)vne).getAttribute("ID") + "'@'CharacteristicPurposes'" ));
									});
									org.json.JSONObject row = null;
									rows
									.put(row = new org.json.JSONObject()
											.put("object", new org.json.JSONObject()
													.put("id", "'" + ((Element)at).getAttribute("ID") + "'"))
											.put("values", new org.json.JSONArray().put(parr)));
//									System.out.println(row);
									if(rows.length() == 200) {
										java.util.Map<String, String> qp = new java.util.TreeMap<>();
										org.json.JSONObject response = workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"))).put("rows", rows).toString());
										System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : "From writing ATG Attribute Group on Characteristics: " + response );
										while(rows.length() > 0) {
											rows.remove(0);
										}
									}
								}
							}
						});
					}
				}
			
			}
		);
		if(rows.length() > 0) {
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject response = workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"))).put("rows", rows).toString());
			System.out.println( response == null ? "ERR: " + workshop.getRawResponse() : "From writing ATG Attribute Group on Characteristics: " + response );
			while(rows.length() > 0) {
				rows.remove(0);
			}}
	}
	
	private static java.util.Map<String, org.json.JSONArray> bringMeThePurposes(){
		java.util.Map<String, org.json.JSONArray> purposesMap = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,Characteristic.Purposes");
		qp.put("query", "not Characteristic.Identifier is empty and Characteristic.ParentCharacteristic is empty");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				totalSize = response.getInt("totalSize");
				System.out.println(currentIndex + "/" + totalSize);
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					purposesMap.put(values.getString(0), values.getJSONArray(1));
//					System.out.println(values);
				}
			} else {
				System.out.println("ERR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return purposesMap;
	}
}
