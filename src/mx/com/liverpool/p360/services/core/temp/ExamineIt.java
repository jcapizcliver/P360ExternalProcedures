package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ExamineIt {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final String baseUrlDEV = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	private static final RESTWorkshop rw = new RESTWorkshop();
	private static final XMLMisc xmm = rw.getXmm();

	private static void echaselos(Node n, org.json.JSONArray rows0) {
		String id = ((Element)n).getAttribute("ID");
		if(id != null && id.startsWith("EU4")) {
			java.util.LinkedList<Node> kisi = xmm.listImmediateChildElements(n).get("AttributeLink");
	        Node n1 = null;
	        Node n2 = null;
	        for(Node k : kisi) {
	        	n1 = xmm.byName(k, "MetaData");
	        	if(n1 != null) {
	        		n2 = xmm.byAttributeValue(n1, "AttributeID", "RelevantForATG");
	        		if(n2 != null) {
	        			rows0.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + new java.util.Date().getTime() + "_" + ((Element)k).getAttribute("AttributeID") + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(id).put(((Element)k).getAttribute("AttributeID")).put("CreateProposal").put("RelevantForATG").put(n2.getTextContent())));
	        		}
	        	}
	        }
		}else {
			java.util.Map<String, java.util.LinkedList<Node>> losKisi = xmm.listImmediateChildElements(n);
			java.util.LinkedList<Node> kisi = losKisi.get("Product");
			if(kisi != null) {
				for(Node k : kisi) {
					echaselos(k, rows0);
				}
			}
		}
	}

	private static org.json.JSONObject getMeTheCompa(String compa) throws ServiceUnavailableException{
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/object/Product2G/'" + compa + "'@'MASTER'?entityFilter=Product2GStructureGroupMap,Product2GCharacteristicValue&includeLabels=true", null);
			response = new org.json.JSONObject(rawResponse);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	public static void main(String[] args) throws ServiceUnavailableException {
		// talla normalizada hacia ATG debe de salir como TC-NormalizedSize
		final String[] productsToTestWith = new String[] {"1698767480968356"};
		org.json.JSONObject rp = getMeTheCompa(productsToTestWith[0]);
		String template = getPrimaryProductTaxonomyTemplate(rp.getJSONObject("_data").getJSONArray("structureGroupMap")); // rp.getJSONObject("_data").getJSONArray("structureGroupMap").getJSONObject(0).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
		String itemId = rp.getJSONObject("_entityItem").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
		org.json.JSONArray characteristicArray = rp.getJSONObject("_data").getJSONArray("_characteristicRecords");
		String[] webCategory = new String[] {"cat1240607"};
		String productType = null;
		System.out.println(template + " - " + itemId);

		String piName = null;
		String piUrl = null;
		String piKey = null;

		java.util.LinkedList<String[]> details = new java.util.LinkedList<>();

		String raw = null;
		String firstVariant = null;
		org.json.JSONObject imageObject = null;
		String tamanoUnico = null;
		String tallaNormalizada = null;
		String codigoColor = null;
		String color = null;
		try {
			String charId = null;
			raw = workshop.makeRequest("GET", "/list/Article/bySearch"
					+ "?fields="
						+ java.net.URLEncoder.encode(
								  "Article.SupplierAID,"
								+ "ProductReference.ReferencedSupplierAid(\"" + itemId + "\")"
								,"UTF-8")
					+ "&query=" + java.net.URLEncoder.encode("ProductReference.ReferencedSupplierAid(\"" + itemId + "\") equals \""+itemId+"\"", "UTF-8"), null);
			System.out.println(raw);
			org.json.JSONObject resp = new org.json.JSONObject(raw);
			org.json.JSONArray rows = null;
			org.json.JSONArray upperRows = resp.getJSONArray("rows");
			productType = upperRows.length() == 1 ? "SalesItem" : "SalesItemFamily";
			for(int a = 0; a<upperRows.length(); a++) {
				try{
					firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
					System.out.println("Using: " + firstVariant);
				}catch(org.json.JSONException e) {
					System.out.println(upperRows.getJSONObject(a));
					System.out.println("No manches.");
					System.exit(0);
				}
				raw = workshop.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue", null);
				resp = new org.json.JSONObject(raw);
				resp = resp.getJSONObject("_data");
				rows = resp.getJSONArray("_characteristicRecords");
				org.json.JSONArray children = null;
				String[] chunk = null;
				for(int b = 0; b < rows.length(); b++) {
					imageObject = rows.getJSONObject(b);
					charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
					if("ProductImage".equals(charId)) {
						children = imageObject.getJSONArray("_children");
						piKey = imageObject.getJSONObject("_qualification").getString("recordKey");
						for(int c = 0; c<children.length(); c++) {
							charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
							if("ProductImage_Name".equals(charId)) {
								piName = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}else if("ProductImage_URL".equals(charId)) {
								piUrl = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}
						}
					}else if("ProductImageDetail".equals(charId)) {
						children = imageObject.getJSONArray("_children");
						chunk = new String[3];
						chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
						for(int c = 0; c<children.length(); c++) {
							charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
							if("ProductImageDetail_Name".equals(charId)) {
								chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}else if("ProductImageDetail_URL".equals(charId)) {
								chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}
						}
						details.addLast(chunk);
					} else if("TamanoUnicoSTD".equals(charId)){
						tallaNormalizada = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
					}else if("TamanoUnico".equals(charId)) {
						tamanoUnico = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
					}else if("ColoursLiverpoolAtt".equals(charId)) {
						color = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
						codigoColor = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
					}
				}
				if(piName != null && piUrl != null) {
					break;
				}
			}
			if(piName == null && piUrl == null)
			{
				System.out.println("Cannot find an image.");
				System.exit(0);
			}
			System.out.println("Using: " + firstVariant);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		System.out.println(piName + "\t" + piUrl + "\t" + piKey);
		System.out.println("*******");
		details.forEach(d->System.out.println(d[0] + "\t" + d[1] + "\t" + d[2]));
//		XMLMisc xmm = new XMLMisc();
//		java.util.Map<String, java.util.LinkedList<Node>> map = null;
//		java.util.LinkedList<Node> nodes = null;
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setNamespaceAware(true);
//        DocumentBuilder builder = null;
//        Document doc = null;
//        try{
//        	builder = factory.newDocumentBuilder();
//        	doc = builder.parse(new java.io.File("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml"));
//        }catch(java.io.IOException e) {
//        	e.printStackTrace();
//        } catch (SAXException e) {
//			e.printStackTrace();
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		}
//        org.json.JSONArray rows0 = new org.json.JSONArray();
//        if(doc != null) {
//	        doc.getDocumentElement().normalize();
//	        Element rootElement = doc.getDocumentElement();
//	        Element products    = (Element) xmm.byName(rootElement,     "Products");
//	        Element product = (Element) xmm.byName(products, "Product");
//	        java.util.Map<String, java.util.LinkedList<Node>> losKisi = xmm.listImmediateChildElements(product);
//	        java.util.LinkedList<Node> kisi = null;
//	        kisi = losKisi.get("AttributeLink");
//	        Node n = null;
//	        Node n2 = null;
//	        for(Node k : kisi) {
//	        	n = xmm.byName(k, "MetaData");
//	        	if(n != null) {
//	        		n2 = xmm.byAttributeValue(n, "AttributeID", "RelevantForATG");
//	        		if(n2 != null) {
//	        			rows0.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ((Element)k).getAttribute("AttributeID") + "'@'CaracteristicasATG'")).put("values", new org.json.JSONArray().put(((Element)k).getAttribute("AttributeID")).put(((Element)n2).getTextContent())));
//	        		}
//	        	}
//	        }
//	        kisi = losKisi.get("Product");
//	        for(Node k : kisi) {
//	        	echaselos(k, rows0);
//	        }
//        }
//      try {
//			System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/StandardizationValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"))).put("rows", rows0).toString()) );
//		} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException
//				| IOException e) {
//			e.printStackTrace();
//		}
//        for(int i=0; i<rows0.length(); i++) {
//        	System.out.println(rows0.getJSONObject(i));
//        }
//        System.out.println(rows0.length());
//        org.json.JSONArray rows1 = new org.json.JSONArray();
//        for(int i=0; i<rows0.length(); i++) {
//        	rows1.put(rows0.getJSONObject(i));
//        	if((i+1) % 1000 == 0) {
//        		try {
//        			rc.getRequest("POST", baseUrlDEV + "/list/StandardizationValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).put("rows", rows1).toString());
//        			System.out.println(i + 1);
//        			rows1 = new org.json.JSONArray();
//        		} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException
//        				| IOException e) {
//        			e.printStackTrace();
//        		}
//        	}
//        }
//        if(rows1.length() > 0)
//	        try {
//				System.out.println( rc.getRequest("POST", baseUrlDEV + "/list/StandardizationValue", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property")).put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).put("rows", rows1).toString()) );
//			} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException
//					| IOException e) {
//				e.printStackTrace();
//			}
//        System.exit(0);

		String[] gruposParaElAtg = (
				  "CategorySpecificAttributesLVP\r\n"
				+ "CategorySpecificAttributesSAP\r\n"
				+ "SAP_Attributes\r\n"
				+ "VariantsSpecificAttributes\r\n"
				+ "SalesItemMarketingDescriptions\r\n"
				+ "ATG_Attributes\r\n"
				+ "ConjuntoLookMaintenance\r\n"
				+ "ConjuntoLookMetadata").split("\\r\\n");
		StringBuilder sb = new StringBuilder();
		for(int a = 0; a<gruposParaElAtg.length; a++ ) {
			sb.append(a == 0 ? "" : ",");
			sb.append("\"");
			sb.append(gruposParaElAtg[a]);
			sb.append("\"");
		}
		java.util.Set<String> atgs = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "Characteristics");
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code in (" + sb.toString() + ")");
		qp.put("pageSize", "900");
		int ci = 0;
		int tz = 0;
		org.json.JSONObject responsi = null;
		org.json.JSONArray rowsi = null;
		org.json.JSONArray valsi = null;
		do {
			qp.put("startIndex", String.valueOf(ci));
			responsi = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(responsi != null) {
				rowsi = responsi.getJSONArray("rows");
				for(int a = 0; a<rowsi.length(); a++) {
					ci++;
					valsi = rowsi.getJSONObject(a).getJSONArray("values");
					atgs.add(valsi.getString(0));
				}
			}
		}while(ci < tz);
		ci = 0;
		java.util.Set<String> losDelAtg = new java.util.TreeSet<>(java.util.Arrays.asList(gruposParaElAtg));
//		Yep yep = new Yep();
//		String delimiter = "\"";
//		String separator = ",";
//		String escape = "\\";
//		java.util.Map<String, java.util.LinkedList<String>> elements = new java.util.TreeMap<>();
//		java.util.LinkedList<String> attributeGroups = null;
//		String[] pieces = null;
//		String[] groups = null;
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\element_list_attribute_groups.csv")))){
//			br.readLine();
//			String line = null;
//			while((line = br.readLine()) != null) {
//				pieces = yep.parseLine(line, delimiter, separator, escape);
//				if(pieces[1] != null && !"".equals(pieces[1])) {
//					attributeGroups = new java.util.LinkedList<>();
//					groups = pieces[1].split(";");
//					for(int i=0; i<groups.length; i++) {
//						attributeGroups.addLast(groups[i]);
//					}
//					elements.put(pieces[0], attributeGroups);
//				}
//			}
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
		int elmax = 0;
//		for(java.util.Map.Entry<String, java.util.LinkedList<String>> entry : elements.entrySet()) {
//			elmax = entry.getValue().size() > elmax ? entry.getValue().size() : elmax;
//			System.out.println(entry.getKey() + "<::>" + entry.getValue());
//		}
		System.out.println("Done. El max: " + elmax);
		String url = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONArray values = null;
		java.util.LinkedList<org.json.JSONObject> entradasJerarquia = new java.util.LinkedList<>();
		int times = 0;
//		try {
//			rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/GeneralPurposeDictionary/bySearch?"
//					+ "&query=" + java.net.URLEncoder.encode("not GeneralPurposeDictionary.Identifier is empty", "UTF-8") +
//					"&fields="
//					+ "GeneralPurposeDictionary.Identifier", null);
//			System.out.println(rawResponse);
//			response = new org.json.JSONObject(rawResponse);
//			rows = response.getJSONArray("rows");
//			for(int i=0; i<rows.length(); i++) {
//				System.out.println(rows.getJSONObject(i).getJSONArray("values"));
//			}
//			System.exit(0);

			java.util.LinkedList<String> helper = new java.util.LinkedList<>();
			/*
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/CharacteristicAttributeGroup/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'CharacteristicAttributeGroup'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("CharacteristicAttributeGroup.Dictionary->GeneralPurposeDictionary.Identifier equals \"CharacteristicAttributeGroup\"", "UTF-8") +
						"&fields=" + java.net.URLEncoder.encode(
						  "CharacteristicAttributeGroup.Identifier,"
						+ "CharacteristicAttributeGroup.Characteristic->Characteristic.Identifier,"
						+ "CharacteristicAttributeGroup.AttributeGroup", "UTF-8") + "&pageSize=1000&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					for(int j=0; j<values.getJSONArray(2).length(); j++) {
						helper.addLast(values.getJSONArray(2).getString(j));
					}
					for(String hlp : helper) {
						if(losDelAtg.contains(hlp)) {
							atgs.add(values.getString(1));
							break;
						}
					}
					helper.clear();
					currentIndex++;
				}
			}while(currentIndex < totalSize);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		*/
		currentIndex = 0;
//		String template = "EU4-53812521";
		String characteristicIdentifier = null;
		String prevC = null;
		java.util.Map<String, String> elMapa = new java.util.TreeMap<>();
		java.util.Set<String> atributosGeneralesQueSi = new java.util.TreeSet<>();
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/StandardizationValue/bySearch?dictionaryProxy='" + java.net.URLEncoder.encode("CaracteristicasATG", "UTF-8") + "'"
						+ "&query=" + java.net.URLEncoder.encode(
								"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"CaracteristicasATG\""
								+ " and not StandardizationValue.AlternativeValue equals \"N\"", "UTF-8")
						+ "&fields="
						+ java.net.URLEncoder.encode(
						"StandardizationValue.Characteristic->Characteristic.Identifier", "UTF-8") + "&pageSize=1000&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println(totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					atributosGeneralesQueSi.add(values.getString(0));
					currentIndex++;
				}
			}while(currentIndex < totalSize);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Atributos Generales que si: " + atributosGeneralesQueSi.size());
		currentIndex = 0;
		java.util.Set<String> atributosDeInteres = new java.util.TreeSet<>();
		System.out.println("Probando si " + template + " tiene cosos que se van al a te gé...");
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/StandardizationValue/bySearch?dictionaryProxy='" + java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla", "UTF-8") + "'"
						+ "&query=" + java.net.URLEncoder.encode(
								"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
								+ " and StandardizationValue.StructureGroup equals \"" + template + "\"", "UTF-8")
						+ "&fields="
						+ java.net.URLEncoder.encode("StandardizationValue.StructureGroup->LookupValue.Code,"
						+ "StandardizationValue.Characteristic->Characteristic.Identifier,"
						+ "StandardizationValue.Property->LookupValue.Code,"
						+ "StandardizationValue.PropertyValue", "UTF-8") + "&orderBy=1-ASC&pageSize=1000&startIndex=" + currentIndex, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println(totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					characteristicIdentifier = values.getString(1);
					if(prevC != null && !"".equals(prevC) && !prevC.equals(characteristicIdentifier)) {
						if((atgs.contains(prevC) && (elMapa.get("RelevantForATG") == null || !"N".equals(elMapa.get("RelevantForATG"))))) {
							atributosDeInteres.add(prevC);
							System.out.println("Este sí se va al ATG: " + prevC);
						}
						elMapa.clear();
					}
					elMapa.put(values.getString(2), values.getString(3));
					prevC = characteristicIdentifier;
					currentIndex++;
				}
			}while(currentIndex < totalSize);
			atributosGeneralesQueSi.forEach(a-> { if(!"".equals(a)) {
				System.out.println("Este también se va al ATG: " + a);
			} });
			atributosDeInteres.addAll(atributosGeneralesQueSi);
		} catch (IOException e) {
			e.printStackTrace();
		}
		currentIndex = 0;
		java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = new java.util.TreeMap<>();
		String currC = null;
		prevC = null;
		org.json.JSONObject prop = new org.json.JSONObject();
		org.json.JSONArray prevV = null;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/StandardizationValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'", "UTF-8")
						+ "&query="
							+ java.net.URLEncoder.encode(
								"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
								+ " and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\""
								+ " and (StandardizationValue.Property->LookupValue.Code equals \"IsMultiselect\""
								+ " or StandardizationValue.Property->LookupValue.Code equals \"IsMandatory\""
								+ " or StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSectionSequence\")"
								+ " and StandardizationValue.Characteristic->Characteristic.Identifier in (" + String.join(",", atributosDeInteres) + ")"
										+ " and StandardizationValue.StructureGroup->LookupValue.Code equals \"" + template + "\""
							, "UTF-8")
						+ "&fields="
						+ java.net.URLEncoder.encode(
						  "StandardizationValue.StructureGroup->LookupValue.Code,"
						+ "StandardizationValue.Characteristic->Characteristic.Identifier,"
						+ "StandardizationValue.Property->LookupValue.Code,"
						+ "StandardizationValue.PropertyValue,"
						+ "StandardizationValue.Characteristic->CharacteristicLang.Name(es)", "UTF-8") + "&orderBy=1-ASC&pageSize=1000&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println(totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					currC = values.getString(1);
					System.out.println(values);
					if(prevC != null && !prevC.equals(currC)) {
						prop.put("name", prevV.getString(4));
						propiedadesCaracteristicas.put(prevC, prop);
						prop = new org.json.JSONObject();
					}
					prop.put(values.getString(2), values.getString(3));
					prevC = currC;
					prevV = values;
					currentIndex++;
				}
			}while(currentIndex < totalSize);
			if(prop.length() > 0) {
				propiedadesCaracteristicas.put(prevC, prop);
			}
		} catch (org.json.JSONException | IOException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		currentIndex = 0;

		java.util.LinkedList<org.json.JSONObject> rescataLaRaiz = new java.util.LinkedList<>();
//		org.json.JSONObject raizWebLiverpool = null;
//		org.json.JSONObject raizWebSuburbia = null;
//		org.json.JSONObject raizWebToysRUs = null;
//		org.json.JSONObject raizWebWestElm = null;
//		org.json.JSONObject raizWebWilliamsSonoma = null;
//		org.json.JSONObject raizWebGAP = null;
//		org.json.JSONObject raizWebBananaRepublic = null;

		java.util.Map<String, org.json.JSONObject> multisitios = precargaJerarquiasWeb("Sitios Web", rescataLaRaiz);

//		java.util.Map<String, org.json.JSONObject> losepasWebLiverpool = precargaJerarquiasWeb("WebLiverpool", rescataLaRaiz);
//		raizWebLiverpool = rescataLaRaiz.remove();
//		java.util.Map<String, org.json.JSONObject> losepasWebSuburbia = precargaJerarquiasWeb("WebSuburbia", rescataLaRaiz);
//		raizWebSuburbia = rescataLaRaiz.remove();
//		java.util.Map<String, org.json.JSONObject> losepasWebToysRUs = precargaJerarquiasWeb("WebToysRUs", rescataLaRaiz);
//		raizWebToysRUs = rescataLaRaiz.remove();
//		java.util.Map<String, org.json.JSONObject> losepasWebWestElm = precargaJerarquiasWeb("WebWestElm", rescataLaRaiz);
//		raizWebWestElm = rescataLaRaiz.remove();
//		java.util.Map<String, org.json.JSONObject> losepasWebWilliamsSonoma = precargaJerarquiasWeb("WebWilliamsSonoma", rescataLaRaiz);
//		raizWebWilliamsSonoma = rescataLaRaiz.remove();
//		java.util.Map<String, org.json.JSONObject> losepasWebGAP = precargaJerarquiasWeb("WebGAP", rescataLaRaiz);
//		raizWebGAP = rescataLaRaiz.remove();
//		java.util.Map<String, org.json.JSONObject> losepasWebBananaRepublic = precargaJerarquiasWeb("WebBananaRepublic", rescataLaRaiz);
//		raizWebBananaRepublic = rescataLaRaiz.remove();

		java.util.Map<String, java.util.Map<String, org.json.JSONObject>> globalMap = new java.util.TreeMap<>();
		multisitios.forEach((k,v)->globalMap.put(k, multisitios));
//		losepasWebLiverpool.forEach((k,v)->globalMap.put(k, losepasWebLiverpool));
//		losepasWebSuburbia.forEach((k,v)->globalMap.put(k, losepasWebSuburbia));
//		losepasWebToysRUs.forEach((k,v)->globalMap.put(k, losepasWebToysRUs));
//		losepasWebWestElm.forEach((k,v)->globalMap.put(k, losepasWebWestElm));
//		losepasWebWilliamsSonoma.forEach((k,v)->globalMap.put(k, losepasWebWilliamsSonoma));
//		losepasWebGAP.forEach((k,v)->globalMap.put(k, losepasWebGAP));
//		losepasWebBananaRepublic.forEach((k,v)->globalMap.put(k, losepasWebBananaRepublic));

		java.util.Map<String, org.json.JSONObject> hierarchyHelper = null;
		org.json.JSONObject entry = null;
		org.json.JSONObject entryHelper = null;

        try {

        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	Document doc = builder.newDocument();

        	Element spim = doc.createElement("STEP-ProductInformation");
        	spim.setAttribute("ExportTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new java.util.Date() ));
        	spim.setAttribute("ExportContext", "Context2");
        	spim.setAttribute("ContextID", "Context2");
        	spim.setAttribute("WorkspaceID", "Approved");
        	spim.setAttribute("UseContextLocale", "false");

        	Element attributes = doc.createElement("AttributeList");
        	spim.appendChild(attributes);

        	Element attribute = null;
        	Element attributeName = null;
        	Element attributeMetaData = null;
        	Element attributeMetaDataValue = null;

        	Element assets = doc.createElement("Assets");

        	/******
        	 *	Si se aprobó normal, es el mensaje del ejemplo, si se aprueba por repoblamiento, se tiene otro.
        	 ********/
        	Element asset = null;
        	Element assetName = null;
        	Element assetValues = null;
        	Element assetValue = null;
        	java.util.Map<String, Element> assetMap = new java.util.TreeMap<>();
        	java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap = new java.util.TreeMap<>();
        	java.util.LinkedList<String> referencesList = null;
        	String assetId = null;
        	spim.appendChild(assets);

        	Element products = doc.createElement("Products");
        	doc.appendChild(spim);
        	spim.appendChild(products);

        	Element product = null;

        	product = doc.createElement("Product");
        	product.setAttribute("ID", productsToTestWith[0]);
        	product.setAttribute("UserTypeID", "SalesItem");
        	product.setAttribute("ParentID", template);
        	product.setAttribute("Changed", "true");
        	products.appendChild(product);

        	/*********************
        	 * El atributo en las entidades Changed="true", tiene el efecto
        	 * de que broker ignorda todo lo que no tenga Changed="true".
        	****************************************************************/
        	Element name = doc.createElement("Name");
        	name.setTextContent("EJEMPLO DESDE P360");
        	name.setAttribute("Changed", "true");
        	product.appendChild(name);

        	Element keyValueSKU = null;
        	Element keyValueEAN = null;
        	Element assetCrossReference = null;

        	Element classificationReference = null;
        	classificationReference = doc.createElement("ClassificationReference");
        	classificationReference.setAttribute("ClassificationID", webCategory[0]);
        	classificationReference.setAttribute("Type", "WebsiteLink");
        	classificationReference.setAttribute("Changed", "true");
        	product.appendChild(classificationReference);

        	Element attributeValues = null;
        	Element attributeValue = null;

        	String charId = null;
        	org.json.JSONObject characteristic = null;
        	org.json.JSONObject cc = null;

        	boolean behvo = false;

        	/***************
        	 *
        	 *
        	 *
        	 *
        	 *******************/
        	Element classifications = doc.createElement("Classifications");
        	spim.appendChild(classifications);

        	Element raizClassification = doc.createElement("Classification");
        	raizClassification.setAttribute("ID", "Classification 1 root");
        	raizClassification.setAttribute("UserTypeID", "Classification 1 user-type root");
        	raizClassification.setAttribute("Selected", "false");
        	Element raizClassificationName = doc.createElement("Name");
        	raizClassificationName.setTextContent("Clasificaciones");
        	raizClassification.appendChild(raizClassificationName);
        	Element webHierarchyRoot = doc.createElement("Classification");
        	webHierarchyRoot.setAttribute("ID", "WebHierarchyRoot");
        	webHierarchyRoot.setAttribute("UserTypeID", "WebHierarchyRoot");
        	webHierarchyRoot.setAttribute("Selected", "false");
        	Element webHierarchyRootName = doc.createElement("Name");
        	webHierarchyRootName.setTextContent("Sitios Web");
        	webHierarchyRoot.appendChild(webHierarchyRootName);
        	raizClassification.appendChild(webHierarchyRoot);
        	classifications.appendChild(raizClassification);

        	Element helperElement = null;
        	Element prevHelperElement = null;
        	java.util.Map<String, Element> tableroDeControl = new java.util.TreeMap<>();

        	for (String element : webCategory) {
        		hierarchyHelper = globalMap.get(element);
        		if(hierarchyHelper != null) {
        			entry = hierarchyHelper.get(element);
        			entryHelper = entry;
        			helperElement = pacheleWeb(entryHelper, doc);
        			if(!tableroDeControl.containsKey(entryHelper.getString("identifier"))) {
        				tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
        			}
        			while(entryHelper.has("parentIdentifier") && !"".equals(entryHelper.get("parentIdentifier")) && !tableroDeControl.containsKey(entryHelper.getString("parentIdentifier"))) {
        				prevHelperElement = helperElement;
        				entryHelper = hierarchyHelper.get(entryHelper.getString("parentIdentifier"));
        				helperElement = pacheleWeb(entryHelper, doc);
        				if(helperElement == null) {
        					System.out.println("PANIC: No element could be made from: " + entryHelper);
        					break;
        				}
        				helperElement.appendChild(prevHelperElement);
        				tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
        			}
        		}else {
        			System.out.println("Category not found... " + element);
        		}
        	}

        	for(org.json.JSONObject laRaiz : rescataLaRaiz) {
        		helperElement = tableroDeControl.get(laRaiz.getString("identifier"));
        		if(helperElement != null) {
        			webHierarchyRoot.appendChild(helperElement);
        		}
        	}

        	attributeValues = doc.createElement("Values");
        	product.appendChild(attributeValues);

        	if(tamanoUnico != null && !"".equals(tamanoUnico)) {
				appendPlainElementValue(
						null,
						tamanoUnico,
						null,
						"TamanoUnico",
						attributeValues,
						attributeMetaData,
						attributeMetaDataValue,
						attributes,
						doc,
						prop,
						propiedadesCaracteristicas);
			}
        	if(tallaNormalizada != null && !"".equals(tallaNormalizada)) {
				appendPlainElementValue(
						null,
						tallaNormalizada,
						null,
						"TC-NormalizedSize",
						attributeValues,
						attributeMetaData,
						attributeMetaDataValue,
						attributes,
						doc,
						prop,
						propiedadesCaracteristicas);
			}
        	if(color != null && !"".equals(color)) {

            	appendPlainElementValue(
    					null,
    					color,
    					codigoColor,
    					"ColoursLiverpolAtt",
    					attributeValues,
    					attributeMetaData,
    					attributeMetaDataValue,
    					attributes,
    					doc,
    					prop,
    					propiedadesCaracteristicas);
        	}

			String rr = null;
			try {
				rr = rc.getRequest("GET", baseUrlDEV + "/object/StructureGroup/'" + template + "'@'PrimaryProductTaxonomy'?entityFilter=StructureGroupAttribute", null);
				org.json.JSONObject tratando = new org.json.JSONObject(rr);
				org.json.JSONArray attributeRow = tratando.getJSONObject("_data").getJSONArray("attribute");
				for(int a = 0; a<attributeRow.length(); a++) {
					if("DisplayGroupOrder".equals(attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"))) {
						String val = attributeRow.getJSONObject(a).getJSONArray("value").getJSONObject(0).getString("value");
						appendPlainElementValue(
    							null,
    							val,
    							null,
    							"DisplayGroupOrder",
    							attributeValues,
    							attributeMetaData,
    							attributeMetaDataValue,
    							attributes,
    							doc,
    							prop,
    							propiedadesCaracteristicas);
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}


			if( piName != null && piUrl != null && piKey != null ) {
				appendMediaAsset(
    					piName,
    					piUrl,
    					"PrimaryProductImage", // String assetType,
    					piKey,
    					"Imagen Producto", // String assetValueTextContent,
    					"ImageURL", // String assetValueAttributeId,
    					"ProductImage", // String assetUserTypeId,
    					"ProductImage", // String assetKeyPrefix,
    					itemId,
    					characteristic,
    					"ProductImage", // String baseAssetTypeName,
    					assetMap,
    					assetReferencesMap,
    					product,
    					assets,
    					classificationReference,
    					doc,
    					firstVariant
    					);
			}
			if( details != null && !details.isEmpty() ) {
				for(String[] dt : details) {
					appendMediaAsset(
							dt[0],
							dt[1],
	    					"ProductImage", // String assetType,
	    					dt[2],
	    					"Imagen Detalle Producto", // String assetValueTextContent,
	    					"ImageURL", // String assetValueAttributeId,
	    					"ProductImageDetail", // String assetUserTypeId,
	    					"ProductImageDetail", // String assetKeyPrefix,
	    					itemId,
	    					characteristic,
	    					"ProductImageDetail", // String baseAssetTypeName,
	    					assetMap,
	    					assetReferencesMap,
	    					product,
	    					assets,
	    					classificationReference,
	    					doc,
	    					firstVariant
	    					);
				}
			}

        	for(int i = 0; i<characteristicArray.length(); i++) {
        		characteristic = characteristicArray.getJSONObject(i);
        		charId = characteristic.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
        		if("MainBarCode".equals(charId)) {
        			keyValueEAN = doc.createElement("KeyValue");
        			keyValueEAN.setAttribute("KeyID","EANKey");
        			keyValueEAN.setTextContent(characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
        			product.appendChild(keyValueEAN);
        		}
        		if("SKU".equals(charId)) {
        			keyValueSKU = doc.createElement("KeyValue");
        			keyValueSKU.setAttribute("KeyID","SKUID");
        			String skuval = treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
        			keyValueSKU.setTextContent( skuval );
        			product.appendChild(keyValueSKU);
        			appendPlainElementValue(
							null,
							skuval,
							null,
							"SKU",
							attributeValues,
							attributeMetaData,
							attributeMetaDataValue,
							attributes,
							doc,
							prop,
							propiedadesCaracteristicas);
        		}else
        		if("ProductVideo".equals(charId)) {
        			appendMediaAsset(
        					null,
        					null,
        					"Video", // String assetType,
        					null,
        					"Video Producto", // String assetValueTextContent,
        					"VideoURL", // String assetValueAttributeId,
        					"Video", // String assetUserTypeId,
        					"ProductVideo", // String assetKeyPrefix,
        					itemId,
        					characteristic,
        					"ProductVideo", // String baseAssetTypeName,
        					assetMap,
        					assetReferencesMap,
        					product,
        					assets,
        					classificationReference,
        					doc,
        					productsToTestWith[0]
        					);
        		}else if("OwnersManual".equals(charId)) {
        			appendMediaAsset(
        					null,
        					null,
        					"OwnersManual", // String assetType,
        					null,
        					"Manual de Propietario", // String assetValueTextContent,
        					"OwnersManualURL", // String assetValueAttributeId,
        					"OwnersManual", // String assetUserTypeId,
        					"OwnersManual", // String assetKeyPrefix,
        					itemId,
        					characteristic,
        					"OwnersManual", // String baseAssetTypeName,
        					assetMap,
        					assetReferencesMap,
        					product,
        					assets,
        					classificationReference,
        					doc,
        					productsToTestWith[0]
        					);
        		}else {
        			if("ItemGroupS4H".equals( charId ) || "ItemGroup".equals( charId )) {
        				if(!behvo) {
	        				String elese = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
	        				try {
	        					rawResponse = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch"
	        							+ "?dictionaryProxy=" + encode("'" + ("ItemGroup".equals( charId ) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H") + "'")
	        							+ "&query=" + encode("StandardizationValue.Value equals \"" + elese + "\"")
	        							+ "&fields=" + encode("StandardizationValue.AlternativeValue")
	        							+ ""
	        							, null);
	        					System.out.println(rawResponse);
	        					response = new org.json.JSONObject(rawResponse);
	        					rows = response.getJSONArray("rows");
	        					String laetiqueta = queryDictionary(elese, "'" + ("ItemGroup".equals( charId ) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H") + "'");
	        					if(rows.length() > 0) {
	        						rawResponse = workshop.makeRequest("GET", "/list/LookupValue/bySearch"
	            							+ "?lookup=" + encode("SAP_BEHVOLOV")
	            							+ "&query=" + encode("LookupValueLang.Name(es) equals \"" + laetiqueta + "\"")
	            							+ "&fields=" + encode("LookupValue.Code")
	            							+ ""
	            							, null);
	            					response = new org.json.JSONObject(rawResponse);
	            					rows = response.getJSONArray("rows");
	            					if(rows.length() > 0) {
	            						String elcode = rows.getJSONObject(0).getJSONArray("values").getString(0);
	            						appendPlainElementValue(
	            								null,
	            								laetiqueta,
	            								elcode,
	            								"SAP_BEHVO",
	            								attributeValues,
	            								attributeMetaData,
	            								attributeMetaDataValue,
	            								attributes,
	            								doc,
	            								prop,
	            								propiedadesCaracteristicas);
	            						behvo = true;
	            						System.out.println("¡Lo pusimos! (" + rows.getJSONObject(0).getJSONArray("values").getString(0) + ")");
	            					}
	        					}
	        				}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {

	        				}
	        				appendPlainElementValue(
	        						null,
	        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
	        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
	        						"ItemGroup2",
	        						attributeValues,
	        						attributeMetaData,
	        						attributeMetaDataValue,
	        						attributes,
	        						doc,
	        						prop,
	        						propiedadesCaracteristicas);
        				}
        				appendPlainElementValue(
        						null,
        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
        						charId,
        						attributeValues,
        						attributeMetaData,
        						attributeMetaDataValue,
        						attributes,
        						doc,
        						prop,
        						propiedadesCaracteristicas);

        			}else
        				if("BrandName".equals(charId) || "BRAND_ID_S4H".equals(charId)) {
        					appendPlainElementValue(
        							null,
        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
        							null,
        							charId,
        							attributeValues,
        							attributeMetaData,
        							attributeMetaDataValue,
        							attributes,
        							doc,
        							prop,
        							propiedadesCaracteristicas);
        				appendPlainElementValue(
        						null,
        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
        						null,
        						"BrandNameATG",
        						attributeValues,
        						attributeMetaData,
        						attributeMetaDataValue,
        						attributes,
        						doc,
        						prop,
        						propiedadesCaracteristicas);
        				appendPlainElementValue(
    							null,
    							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
    							null,
    							"BrandIDATG",
    							attributeValues,
    							attributeMetaData,
    							attributeMetaDataValue,
    							attributes,
    							doc,
    							prop,
    							propiedadesCaracteristicas);
        			}else {
        				if("ItemGroup2".equals(charId)) {
							continue;
						}
        				if("LOOKUP".equals(characteristic.getString("_datatype"))){
        					appendPlainElementValue(
        							null,
        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
        							charId,
        							attributeValues,
        							attributeMetaData,
        							attributeMetaDataValue,
        							attributes,
        							doc,
        							prop,
        							propiedadesCaracteristicas);
        				}else if(!"NONE".equals(characteristic.getString("_datatype"))) {
        					java.util.LinkedList<String> vals = new java.util.LinkedList<>();
        					for(int m=0; m<characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
        						vals.addLast( String.valueOf( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m) ));
        					}
        					appendPlainElementValue(
        							null,
        							String.join(",", vals),
        							null,
        							charId,
        							attributeValues,
        							attributeMetaData,
        							attributeMetaDataValue,
        							attributes,
        							doc,
        							prop,
        							propiedadesCaracteristicas);
        				}
        			}
        		}
        	}

        	TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String xmlOutput = writer.getBuffer().toString();

            /*
            RestClient rc = new RestClient("Content-Type: application/xml", "Accept: application/xml");
            try {
				System.out.println( rc.getRequest("POST", "http://gcpcatqap04.liverpool.com.mx:8080/process-engine/public/rt/SendToBrokerPIMStageATG", xmlOutput) );
			} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
            try {
//				System.out.println( rc.getRequest("POST", "http://172.18.237.165:8080/process-engine/public/rt/SendToBrokerPIMStageATGDev", xmlOutput) );
            	System.out.println( rc.getRequest("POST", "http://gcpcatqap04.liverpool.com.mx:8080/process-engine/public/rt/SendToBrokerPIMStageOMS", xmlOutput) );
            } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
            	e.printStackTrace();
            }
            try {
//				System.out.println( rc.getRequest("POST", "http://172.18.237.165:8080/process-engine/public/rt/SendToBrokerPIMStageATGDev", xmlOutput) );
            	System.out.println( rc.getRequest("POST", "http://gcpcatqap04.liverpool.com.mx:8080/process-engine/public/rt/SendToBrokerPIMStageAprobadosHaciaVC", xmlOutput) );
            } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
            	e.printStackTrace();
            }
            */
            transformer.transform(new DOMSource(doc), new StreamResult(new java.io.File("C:\\opt\\LVP\\tmp\\pépele00.xml")));
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

	}

	private static String queryDictionary(String key, String dictionary) throws ServiceUnavailableException {
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		try {
			rawResponse = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch"
					+ "?dictionaryProxy=" + encode(dictionary)
					+ "&query=" + encode("StandardizationValue.Value equals \"" + key + "\"")
					+ "&fields=" + encode("StandardizationValue.AlternativeValue")
					+ ""
					, null);
			response = new org.json.JSONObject(rawResponse);
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			}
		}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException e){

		}
		return null;
	}

	private static String encode(String val) {
		try {
			return java.net.URLEncoder.encode(val, "UTF-8");
		}catch(java.io.IOException e) {

		}
		return null;
	}

	private static String getPrimaryProductTaxonomyTemplate(org.json.JSONArray classifications){
		org.json.JSONObject classification = null;
		String externalId = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("'(EU4\\-[0-9]+)'");
		java.util.regex.Matcher m = null;
		for(int i=0; i<classifications.length(); i++) {
			classification = classifications.getJSONObject(i);
			externalId = classification.getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId");
			if(externalId.endsWith("'PrimaryProductTaxonomy'")) {
				m = p.matcher(externalId);
				if(m.find()) {
					return m.group(1);
				} else {
					System.out.println("Could not find a match in: " + externalId);
					return null;
				}
			}
		}
		return null;
	}

	private static String treatment(String val) {
		StringBuilder sb = new StringBuilder();
		int i=0;
		while(val.charAt(i) == '0') {
			i++;
		}
		while(i < val.length()) {
			sb.append(val.charAt(i));
			i++;
		}
		return sb.toString();
	}

	private static void appendMediaAsset(
			String name,
			String url,
			String assetType,
			String assetKey,
			String assetValueTextContent,
			String assetValueAttributeId,
			String assetUserTypeId,
			String assetKeyPrefix,
			String itemId,
			org.json.JSONObject characteristic,
			String baseAssetTypeName,
			java.util.Map <String, Element> assetMap,
			java.util.Map <String, java.util.LinkedList <String>> assetReferencesMap,
			Element product,
			Element assets,
			Element classificationReference,
			Document doc,
			String seedId
		) {
		Element assetCrossReference = doc.createElement("AssetCrossReference");
		org.json.JSONObject cc = null;
		String assetId = assetKeyPrefix + "-" + seedId + (assetKey != null ? assetKey : characteristic.getJSONObject("_qualification").getString("recordKey"));
		if(name != null) {
			assetCrossReference.setAttribute("AssetID", assetId);
			assetCrossReference.setAttribute("Type", assetType);
			assetCrossReference.setAttribute("Changed", "true");
			product.appendChild(assetCrossReference);
		}else {
			cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_Name");
			if(cc != null) {
				assetCrossReference.setAttribute("AssetID", assetId);
				assetCrossReference.setAttribute("Type", assetType);
				assetCrossReference.setAttribute("Changed", "true");
				product.appendChild(assetCrossReference);
			}
		}
		Element asset = assetMap.get(assetId);
		Element assetName = null;
		Element assetValues = null;
		Element assetValue = null;
		java.util.LinkedList<String> referencesList = null;
		if(asset == null) {
			asset = doc.createElement("Asset");
			asset.setAttribute("ID", assetId);
			asset.setAttribute("UserTypeID", assetUserTypeId /* "Video" */);
			asset.setAttribute("Selected", "false");
			asset.setAttribute("Referenced", "true");
			if(cc != null) {
    			assetName = doc.createElement("Name");
    			assetName.setTextContent(cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
    			asset.appendChild(assetName);
			}
			assetValues = doc.createElement("Values");
			asset.appendChild(assetValues);
			assetValue = doc.createElement("Value");
			assetValues.appendChild(assetValue);
			assetValue.setAttribute("AttributeID", "getObjectType");
			assetValue.setTextContent(assetValueTextContent /* "Video Producto" */);
			assetValue = doc.createElement("Value");
			assetValue.setAttribute("AttributeID", assetValueAttributeId /* "VideoURL" */);
			if(url != null) {
				assetValue.setTextContent(url);
				assetValues.appendChild(assetValue);
			}else {
				cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_URL");
				if(cc != null) {
					assetValue.setTextContent(cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
					assetValues.appendChild(assetValue);
				}
			}
			referencesList = new java.util.LinkedList<>();
			referencesList.addLast(itemId);
			assetReferencesMap.put(assetId, referencesList);
			assets.appendChild(asset);
		}else {
			referencesList = assetReferencesMap.get(assetId);
			if(!referencesList.contains(assetId)) {
				referencesList.addLast(assetId);
			}
		}
	}

	private static void appendPlainElementValue(org.json.JSONArray values, String textValue, String code, String attributeId, Element attributeValues, Element attributeMetaData, Element attributeMetaDataValue, Element attributes, Document doc, org.json.JSONObject prop, java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas) {
		Element attributeValue = doc.createElement("Value");
		attributeValues.appendChild(attributeValue);

		attributeValue.setAttribute("AttributeID", attributeId);
		if(values != null) {
			java.util.LinkedList<String> vals = new java.util.LinkedList<>();
			for(int m=0; m<values.length(); m++) {
				vals.addLast( String.valueOf( values.get(m) ));
			}
			attributeValue.setTextContent( String.join(",", vals) );
		}else {
			if(code != null) {
				attributeValue.setAttribute("ID", code);
			}
			attributeValue.setTextContent(textValue);
		}

		attributeValue.setAttribute("Changed", "true");

		Element attribute = doc.createElement("Attribute");
		attribute.setAttribute("ID", attributeId);
		prop = propiedadesCaracteristicas.get(attributeId);
		if(prop != null) {
			attribute.setAttribute("MultiValued", prop.has("IsMultiselect") ? "1".equals(prop.getString("IsMultiselect")) ? "true" : "false" : "false");
			attribute.setAttribute("Mandatory", prop.has("IsMandatory") ? "1".equals(prop.getString("IsMandatory")) ? "true" : "false" : "false");
			if(!prop.has("name")) {
				System.out.println("No Name found for: " + attributeId);
			}else {
				Element attributeName = doc.createElement("Name");
				attributeName.setTextContent(prop.getString("name"));
				attribute.appendChild(attributeName);
			}
		}else {
			// PANIC
			System.out.println("PANIC: No property was found for characteristic: " + attributeId);
		}
		attribute.setAttribute("FullTextIndexed", "false");
		attribute.setAttribute("ProductMode", "Normal");
		attribute.setAttribute("ExternallyMaintained", "true");
		attribute.setAttribute("Derived", "false");
		attribute.setAttribute("HierarchicalFiltering", "false");
		attribute.setAttribute("ClassificationHierarchicalFiltering", "false");
		attribute.setAttribute("Referenced", "true");
		attributes.appendChild(attribute);
		attributeMetaData = doc.createElement("MetaData");
		attribute.appendChild(attributeMetaData);
		if(prop != null && prop.has("VendorCenterSectionSequence")) {
			attributeMetaDataValue = doc.createElement("Value");
			attributeMetaDataValue.setAttribute("AttributeID", "DisplaySequence");
			attributeMetaDataValue.setTextContent(prop.getString("VendorCenterSectionSequence"));
			attributeMetaData.appendChild(attributeMetaDataValue);
		}
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "AtributoCalculadoObjetos");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("Ultimo Usuario: N/A |  Fecha: N/A");
		attributeMetaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttVaDySAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("0");
		attributeMetaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttSAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("N/A");
		attributeMetaData.appendChild(attributeMetaDataValue);
	}

	private static Element pacheleWeb(JSONObject node, Document doc) {
		Element metaData = null;
		Element value = null;
		String aux = null;
        if (node != null) {
        	metaData = doc.createElement("MetaData");
            Element classificationElement = doc.createElement("Classification");
            classificationElement.setAttribute("Selected", "true");
            classificationElement.appendChild(metaData);
            if (node.has("name_es")) {
            	value = doc.createElement("Value");
            	value.setAttribute("AttributeID", "DisplayName");
            	value.setTextContent
            	(node.getString("name_es"));
            	metaData.appendChild(value);
            }
            if (node.has("identifier")) {
                classificationElement.setAttribute("ID", node.optString("identifier", ""));
            }
            if (node.has("level")) {
            	aux = String.valueOf(node.get("level"));
                classificationElement.setAttribute("UserTypeID", "0".equals(aux) ? "WebsiteRoot" : "WebLevel" + aux );
            }
            if (node.has("parentIdentifier") && node.getString("parentIdentifier").startsWith("cat")) {
            	value = doc.createElement("Value");
            	value.setAttribute("AttributeID", "parentCategoryID");
            	value.setTextContent(node.getString("parentIdentifier"));
                metaData.appendChild(value);
            }
            return classificationElement;
        }
        return null;
    }

	private static java.util.Map<String, org.json.JSONObject> precargaJerarquiasWeb(String structureId, java.util.LinkedList<org.json.JSONObject> ondeVaLaRaiz) throws ServiceUnavailableException{
		String url = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONArray values = null;
		java.util.LinkedList<org.json.JSONObject> entradasJerarquia = new java.util.LinkedList<>();
		java.util.Map<String, org.json.JSONObject> losepas = new java.util.TreeMap<>();
		try {
			do {
				url = baseUrlDEV + "/list/StructureGroup/byStructure?structure=" + java.net.URLEncoder.encode( structureId ,"UTF-8") + "&fields=StructureGroup.Identifier,StructureGroupLang.Name(es),StructureGroup.Level,StructureGroup.ParentIdentifier,StructureGroupLang.Description(es),StructureGroupAttributeValue.Value(groupType,es,DEFAULT)&metaData=true&pageSize=5000&startIndex=" + currentIndex;
				rawResponse = rc.getRequest("GET", url, null);
//				System.out.println(rawResponse);
//				System.exit(0);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i< rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					entradasJerarquia.addLast(new org.json.JSONObject().put("identifier", values.getString(0)).put("name_es", values.getString(1)).put("level", Integer.parseInt(values.getString(2))).put("parentIdentifier", values.getString(3)).put("description", values.getString(4)));
					currentIndex++;
				}
				System.out.println(currentIndex + "/" + totalSize);
			}while(currentIndex < totalSize);
			System.out.println("Sorting data... " + structureId);
			java.util.Collections.sort(entradasJerarquia, (o1,o2)->{
					int cmp = Integer.valueOf(o1.getInt("level")).compareTo(Integer.valueOf(o2.getInt("level")));
					if(cmp == 0) {
						cmp = o1.getString("parentIdentifier").compareTo(o2.getString("parentIdentifier"));
						return cmp;
					}
					return cmp;
				});
//			System.out.println("Result");
//			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\jsons_ordenados")))){
//				entradasJerarquia.forEach(e->pw.println(e));
//			}catch(java.io.IOException e) {
//				e.printStackTrace();
//			}
//			System.out.println("Done.");
			org.json.JSONObject miEpa = null;
			for(org.json.JSONObject entrada : entradasJerarquia) {
				if(!losepas.containsKey(entrada.getString("identifier"))) {
					losepas.put(entrada.getString("identifier"), entrada);
				}
				miEpa = losepas.get(entrada.get("parentIdentifier"));
				if(miEpa != null) {
					if(!miEpa.has("children")) {
						miEpa.put("children", new org.json.JSONArray());
					}
					miEpa.getJSONArray("children").put(entrada);
					entrada.put("parentIdentifier", miEpa.getString("identifier"));
				}
			}
			ondeVaLaRaiz.addLast(entradasJerarquia.getFirst());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}catch(org.json.JSONException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		return losepas;
	}

	private static org.json.JSONObject getMeAssetChildValue(org.json.JSONObject hola, String childCharacteristic) {
		if(hola == null || (!hola.has("_children"))) {
			return null;
		}
		org.json.JSONArray children = hola.getJSONArray("_children");
		for(int i=0; i<children.length(); i++) {
			if(children.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").equals(childCharacteristic)) {
				return children.getJSONObject(i);
			}
		}
		return null;
	}


	private static void v() {

		// <ClassificationReference ClassificationID="catst19895633" Type="WebsiteLink" Changed="true"/>
	}


}
