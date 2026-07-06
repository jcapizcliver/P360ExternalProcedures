package com.jcapiz.memelos.misc;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.jcapiz.memelos.p360processor.P360Misc;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class CheckDWHFileVsP360 {

	public static void main(String[] args) {
		P360Misc pm = new P360Misc();
		XMLMisc xm = new XMLMisc();
		String characteristicsFileName = "D:\\tmp\\p360_dev_characteristics.json";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder =null;
		Document document = null;
		Node n = null;
		java.util.Map<String, java.util.LinkedList<Node>> childNodes = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		java.util.Map<String, org.json.JSONObject> characteristics = null;
		long initCollectCharacteristics = System.currentTimeMillis();
		System.out.println("Collecting characteristics...");
		characteristics = readFromFile(characteristicsFileName);
		if(characteristics.isEmpty()) {
			try {
				characteristics = pm.collectCharacteristics();
				keepToFile(characteristics, pm, characteristicsFileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		java.util.LinkedList<String> characteristicsNotFoundInP360 = new java.util.LinkedList<>();
		java.util.Map<String, java.util.LinkedList<String>> filesCharacteristicsNotFound = new java.util.TreeMap<>();
		java.util.LinkedList<Node> nl = null;
		System.out.println("Done collecting characteristics. " + pm.formatMillis(System.currentTimeMillis() - initCollectCharacteristics));
		try(java.io.FileInputStream fis = new java.io.FileInputStream("D:\\tmp\\muestras\\BGP\\BGP_82279530\\635332892-635456471.xml")){
			document = builder.parse(fis);
			document.getDocumentElement().normalize();
			childNodes = xm.listImmediateChildElements(document);
			nl = childNodes.get("STEP-ProductInformation");
			if(nl != null) {
				n = nl.getFirst();
				childNodes = xm.listImmediateChildElements(n);
				n = childNodes.get("AttributeGroupList").getFirst();
				childNodes = xm.listImmediateChildElements(n);

				n = childNodes.get("AttributeGroup").getFirst();
				childNodes = xm.listImmediateChildElements(n);
				n = childNodes.get("Name").getFirst();
				if(!characteristics.containsKey(n.getNodeName())) {
					characteristicsNotFoundInP360.addLast(n.getNodeName());
				}
			}
		}catch(java.io.IOException | SAXException e) {
			e.printStackTrace();
		}
		System.exit(0);
		long initFilesRead = System.currentTimeMillis();
		if(builder != null && characteristics != null) {
			java.io.File[] fls = new java.io.File("D:\\tmp\\muestras\\BGP\\BGP_82326611").listFiles(ff->ff.getName().endsWith(".xml"));
			for(java.io.File f : fls) {
				try(java.io.FileInputStream fis = new java.io.FileInputStream(f)){
					document = builder.parse(fis);
					document.getDocumentElement().normalize();
					childNodes = xm.listImmediateChildElements(document);
					nl = childNodes.get("STEP-ProductInformation");
					if(nl != null) {
						n = nl.getFirst();
						childNodes = xm.listImmediateChildElements(n);
						n = childNodes.get("AttributeList").getFirst();
						childNodes = xm.listImmediateChildElements(n);
						n = childNodes.get("Attribute").getFirst();
						childNodes = xm.listImmediateChildElements(n);
						n = childNodes.get("Name").getFirst();
						if(!characteristics.containsKey(n.getNodeName())) {
							characteristicsNotFoundInP360.addLast(n.getNodeName());
						}
					}
				}catch(java.io.IOException | SAXException e) {
					e.printStackTrace();
				}
				filesCharacteristicsNotFound.put(f.getName(), characteristicsNotFoundInP360);
				characteristicsNotFoundInP360 = new java.util.LinkedList<>();
				System.out.println("Done reading " + f.getName());
			}
			System.out.println("Done reading files. " + pm.formatMillis(System.currentTimeMillis() - initFilesRead));
			filesCharacteristicsNotFound.entrySet().forEach(System.out::println);
		}
	}

	public static java.util.Map<String, org.json.JSONObject> readFromFile(String filePath){
		java.util.Map<String, org.json.JSONObject> characteristics = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(filePath)))){
			String linea = null;
			StringBuilder sb = new StringBuilder();
			while((linea = br.readLine()) != null) {
				sb.append(linea);
			}
			org.json.JSONArray content = new org.json.JSONArray(sb.toString());
			sb.setLength(0);
		org.json.JSONObject currentObject = null;
		for(int i=0; i<content.length(); i++) {
			currentObject = content.getJSONObject(i);
			characteristics.put(currentObject.getString("Identifier"), currentObject);
			}
		}catch(java.io.IOException e) { e.printStackTrace(); }
		return characteristics;
	}

	private static void keepToFile(java.util.Map<String, org.json.JSONObject> characteristics, P360Misc pm, String characteristicsFileName) {
		try {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(characteristicsFileName)))){
				org.json.JSONArray content = new org.json.JSONArray();
				org.json.JSONObject o = null;
				for(java.util.Map.Entry<String, org.json.JSONObject> entry : characteristics.entrySet()) {
					o = entry.getValue();
					o.put("Identifier", entry.getKey());
					content.put(o);
				}
				pw.println(content);
			}catch(java.io.IOException e) { e.printStackTrace(); }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
