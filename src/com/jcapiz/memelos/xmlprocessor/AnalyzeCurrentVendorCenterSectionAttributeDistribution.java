package com.jcapiz.memelos.xmlprocessor;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jcapiz.memelos.misc.RestClient;

public class AnalyzeCurrentVendorCenterSectionAttributeDistribution {

	private java.util.Map<String, String> attributeToVendorCenterSection = new java.util.TreeMap<>();

	public void collectReferenceAttributesVendorCenterSection() {
		RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic YWRtaW46bGl2ZXJwb29s");
		String url = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentCount = 0;
		java.util.LinkedList<String> entries = new java.util.LinkedList<>();
		try {
			do {
				url = "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StandardizationValue/bySearch?dictionaryProxy=17001&query=" + java.net.URLEncoder.encode("StandardizationValue.Property equals \"Vendor Center Section\"", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.PropertyValue,StandardizationValue.Characteristic->CharacteristicLang.Name(es)", "UTF-8") + "&startIndex=" + currentCount;
				rawResponse = rc.getRequest("GET", url, null);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					row = rows.getJSONObject(i);
					values = row.getJSONArray("values");
					entries.addLast(values.getString(1) + " || " + values.getString(0) + " (" + values.getString(2) + ")");
					attributeToVendorCenterSection.put(values.getString(0), values.getString(1));
				}
				totalSize = response.getInt("totalSize");
				currentCount += response.getInt("rowCount");
			}while(currentCount < totalSize);
			java.util.Collections.sort(entries, (o1,o2)-> o1.compareTo(o2) );
			entries.forEach(System.out::println);
		}catch(Exception e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
	}

	public java.util.Map<String, String> getAttributeToVendorCenterSection(){
		return attributeToVendorCenterSection;
	}

	private void checkTemplateValuesPerSpecificAttribute() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder =null;
		Document document = null;
		NodeList parentNodes = null;
		Element parentElement = null;
		try {
			if(builder == null)
			{
			  builder = factory.newDocumentBuilder();
			}
			document = builder.parse(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml"));
			document.getDocumentElement().normalize();
			parentNodes = document.getElementsByTagName("STEP-ProductInformation");
			parentElement = (Element) parentNodes.item(0); // STEP-ProductInformation Products Product Product Product Product Product Values Value
			NodeList rootNodes = parentElement.getChildNodes(); //("Products").item(0)).getElementsByTagName("Product");
			NodeList productL0Nodes = null;
			NodeList productL1Nodes = null;
			NodeList productL2Nodes = null;
			NodeList productL3Nodes = null;
			NodeList productL4Nodes = null;
			Node productL4 = null;
			NodeList valuesList = null;
			NodeList valueList = null;
			Node value = null;
			java.util.Map<String, String> nameGuides = new java.util.TreeMap<>();
			String currentTemplateID = null;
			String currentTemplateName = null;
			String templateNameGuide = null;
			for(int i=0; i<rootNodes.getLength(); i++) {
				if(Node.ELEMENT_NODE == rootNodes.item(i).getNodeType() && "Products".equals(rootNodes.item(i).getNodeName())) {
					productL0Nodes = rootNodes.item(i).getChildNodes();
					for(int j=0; j<productL0Nodes.getLength(); j++) {
						if(Node.ELEMENT_NODE == productL0Nodes.item(j).getNodeType() && "Product".equals(productL0Nodes.item(j).getNodeName())) {
							productL1Nodes = productL0Nodes.item(j).getChildNodes();
							for(int k=0; k<productL1Nodes.getLength(); k++) {
								if(Node.ELEMENT_NODE == productL1Nodes.item(k).getNodeType() && "Product".equals(productL1Nodes.item(k).getNodeName())) {
									productL2Nodes = productL1Nodes.item(k).getChildNodes();
									for(int l=0; l<productL2Nodes.getLength(); l++) {
										if(Node.ELEMENT_NODE == productL2Nodes.item(l).getNodeType() && "Product".equals(productL2Nodes.item(l).getNodeName()) ) {
											productL3Nodes = productL2Nodes.item(l).getChildNodes();
											for(int m = 0; m<productL3Nodes.getLength(); m++) {
												if(Node.ELEMENT_NODE == productL3Nodes.item(m).getNodeType() && "Product".equals(productL3Nodes.item(m).getNodeName())) {
													productL4Nodes = productL3Nodes.item(m).getChildNodes();
													for(int n=0; n<productL4Nodes.getLength(); n++) {
														productL4 = productL4Nodes.item(n);
														if(Node.ELEMENT_NODE == productL4.getNodeType() && "Product".equals(productL4.getNodeName())) {
															currentTemplateID = ((Element)productL4).getAttribute("ID");
															valuesList = productL4.getChildNodes();
															for(int a = 0; a<valuesList.getLength(); a++) {
																if(Node.ELEMENT_NODE == valuesList.item(a).getNodeType() && "Values".equals(valuesList.item(a).getNodeName())) {
																	valueList = valuesList.item(a).getChildNodes();
																	for(int b = 0; b<valueList.getLength(); b++) {
																		if(Node.ELEMENT_NODE == valueList.item(b).getNodeType() && "Value".equals(valueList.item(b).getNodeName())) {
																			value = valueList.item(b);
																			if("OrderOfAtributesForName".equals( ((Element)value).getAttribute("AttributeID") )){
																				templateNameGuide = value.getTextContent();
																			}
																		}
																	}
																}else if(Node.ELEMENT_NODE == valuesList.item(a).getNodeType() && "Name".equals(valuesList.item(a).getNodeName())) {
																	currentTemplateName = valuesList.item(a).getTextContent();
																}
															}
															nameGuides.put(currentTemplateID + "_" + currentTemplateName, templateNameGuide);
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
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\OrderOfAtributesForName")))){
				nameGuides.forEach((k,v) ->{
					String[] id = k.split("_");
					pw.println(id[0] + "|" + id[1] + "|" + v);
				});
			}
		} catch(ParserConfigurationException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		AnalyzeCurrentVendorCenterSectionAttributeDistribution a = new AnalyzeCurrentVendorCenterSectionAttributeDistribution();
		a.checkTemplateValuesPerSpecificAttribute();
	}
}
