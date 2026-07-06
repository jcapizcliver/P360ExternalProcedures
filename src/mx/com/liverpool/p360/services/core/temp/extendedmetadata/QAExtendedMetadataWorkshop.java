package mx.com.liverpool.p360.services.core.temp.extendedmetadata;

import java.io.IOException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class QAExtendedMetadataWorkshop {

	private static final RESTWorkshop rw = new RESTWorkshop();
	private static final XMLMisc xmm = rw.getXmm();
	
	public static void main(String[] args) throws ServiceUnavailableException {
		
//		procedure0();
//		procedure1();
//		procedure2();
//		procedure3();
//		try {
//			procedure4("C:\\opt\\LVP\\desorden\\Proveedores Raiz.xml");
//		} catch (SAXException | IOException | ParserConfigurationException e) {
//			e.printStackTrace();
//		}
//		procedure7();
		System.exit(0);
		
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rw.putParameter("query", "not StandardizationDictionary.Identifier is empty");
		rw.putParameter("fields", "StandardizationDictionary.Identifier");
		org.json.JSONObject response = rw.makeRequest("GET", "/list/StandardizationDictionary/bySearch");
		org.json.JSONArray values = null;
		if(response != null) {
			org.json.JSONArray rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				if(values.toString().contains(" "))
					System.out.println(values);
			}
		}else {
			System.out.println(rw.getRawResponse());
		}
	}
	
	private static void procedure0() {
		String a = "[\"EU4-27320066<::>TamanoUnico<::>ListOfValuesFilter\",\"\",\"EU4-27320066\",\"TamanoUnico\",\"CreateProposal\",\"ListOfValuesFilter\",\"SIN TAMAÑO,G,M,\\\"20\\\\\\\"\\\",UNITALLA,40 L,55 L,12,\\\"12\\\\\\\"\\\",42 L,32 L,SET,\\\"24\\\\\\\"\\\",XG,CH,XCH,71 L\",\"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"]";
		org.json.JSONArray arr = new org.json.JSONArray(a);
		String b = "EU4-27320066_TamanoUnico;TamanoUnicoLOV;\"SIN TAMAÑO,G,M,\\\"20\\\\\"\\\",UNITALLA,40 L,55 L,12,\\\"12\\\\\"\\\",42 L,32 L,SET,\\\"24\\\\\"\\\",XG,CH,XCH,71 L\""; // arr.getString(6);
		RESTWorkshop rw = new RESTWorkshop();
		String delim = "\"";
		String sep = ";";
		String sep2 = ",";
		String esc = "\\";
		System.out.println(b);
		System.out.println("****");
		String[] pieces = rw.parseLine(b, delim, sep, esc);
		String[] secondLevelPieces = null;
		for(int i=0; i<pieces.length; i++) {
			secondLevelPieces = rw.parseLine(pieces[i], delim, sep2, esc);
			if(secondLevelPieces.length > 1) {
				for(int j=0; j<secondLevelPieces.length; j++) {
					System.out.println("\t" + secondLevelPieces[j]);
				}
			}else {
				System.out.println(pieces[i]);
			}
		}
	}
	
	private static void procedure1() throws ServiceUnavailableException {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "respaldo_qa", "MetadataExtensionValues.dat").toString())))){
			RESTWorkshop rw = new RESTWorkshop();
			rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
			rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
			rw.putParameter("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
			rw.putParameter("fields", 
					  "StandardizationValue.Value"
					+ ",StandardizationValue.AlternativeValue"
					+ ",StandardizationValue.StructureGroup->LookupValue.Code"
					+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
					+ ",StandardizationValue.CreationType->LookupValue.Code"
					+ ",StandardizationValue.Property->LookupValue.Code"
					+ ",StandardizationValue.PropertyValue"
					+ ",StandardizationValue.Dictionary->StandardizationDictionary.Identifier");
			rw.putParameter("pageSize", "1200");
			org.json.JSONObject response = null;
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			int a = 0;
			int b = 0;
			do {
				rw.putParameter("startIndex", String.valueOf(a));
				response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
				if(response != null) {
					b = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						values = rows.getJSONObject(i).getJSONArray("values");
						pw.println(values);
					}
					a += response.getInt("pageSize");
				}else {
					System.out.println(rw.getRawResponse());
				}
				System.out.println(a + "/" + b);
			}while(a < b);
			a = 0;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void procedure3() throws ServiceUnavailableException {
			RESTWorkshop rw = new RESTWorkshop();
			rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
			rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
			rw.putParameter("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
			rw.putParameter("fields", 
					"StandardizationValue.Value"
							+ ",StandardizationValue.AlternativeValue"
							+ ",StandardizationValue.StructureGroup->LookupValue.Code"
							+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
							+ ",StandardizationValue.CreationType->LookupValue.Code"
							+ ",StandardizationValue.Property->LookupValue.Code"
							+ ",StandardizationValue.PropertyValue"
							+ ",StandardizationValue.Dictionary->StandardizationDictionary.Identifier");
			rw.putParameter("pageSize", "1200");
			org.json.JSONObject response = null;
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			int a = 0;
			int b = 0;
//			do {
//				rw.putParameter("startIndex", String.valueOf(a));
				response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
				if(response != null) {
					b = response.getInt("totalSize");
					System.out.println(b);
//					rows = response.getJSONArray("rows");
//					for(int i=0; i<rows.length(); i++) {
//						values = rows.getJSONObject(i).getJSONArray("values");
//						pw.println(values);
//					}
//					a += response.getInt("pageSize");
				}else {
					System.out.println(rw.getRawResponse());
				}
//			}while(a < b);
			a = 0;
	}
	
	private static void procedure2() throws ServiceUnavailableException {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "respaldo_qa", "MetadataExtensionValues.dat").toString())))){
			RESTWorkshop rw = new RESTWorkshop();
			rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
			rw.putParameter("query", "not Characteristic.Identifier is empty");
			rw.putParameter("fields",
					"Characteristic.Identifier"
//					  "StandardizationValue.Value"
//					+ ",StandardizationValue.AlternativeValue"
//					+ ",StandardizationValue.StructureGroup->LookupValue.Code"
//					+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
//					+ ",StandardizationValue.CreationType->LookupValue.Code"
//					+ ",StandardizationValue.Property->LookupValue.Code"
//					+ ",StandardizationValue.PropertyValue"
//					+ ",StandardizationValue.Dictionary->StandardizationDictionary.Identifier"
				);
			rw.putParameter("pageSize", "1200");
			org.json.JSONObject response = null;
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch");
			if(response != null) {
				System.out.println(response.getInt("totalSize"));
			}
//			org.json.JSONArray rows = null;
//			org.json.JSONArray values = null;
//			int a = 0;
//			int b = 0;
//			do {
//				rw.putParameter("startIndex", String.valueOf(a));
//				response = rw.makeRequest("GET", "/list/Characteristic/bySearch");
//				if(response != null) {
//					b = response.getInt("totalSize");
//					rows = response.getJSONArray("rows");
//					for(int i=0; i<rows.length(); i++) {
//						values = rows.getJSONObject(i).getJSONArray("values");
//						pw.println(values);
//					}
//					a += response.getInt("pageSize");
//				}else {
//					System.out.println(rw.getRawResponse());
//				}
//				System.out.println(a + "/" + b);
//			}while(a < b);
//			a = 0;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private static void procedure4(String path) throws SAXException, IOException, ParserConfigurationException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
    	if(path != null) {
    		doc = builder.parse( path.toString() );
    	}else {
    		doc = null;
    		log("No path, no byte array provided");
    		return;
    	}
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		java.util.LinkedList<Node> classificationNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Classifications").getFirst()).get("Classification");
		Element el = null;
		String name = null;
		Node metaData = null;
		java.util.LinkedList<Node> metaDataNodeList = null;
		java.util.LinkedList<Node> valueNodeList = null;
		java.util.LinkedList<Node> multiValueNodeList = null;
		java.util.LinkedList<Node> attributeLinkNodeList = null;
		java.util.LinkedList<Node> valueFilterNodeList = null;
		org.json.JSONArray negocios = new org.json.JSONArray();
		org.json.JSONArray tipoProveedorSAP = new org.json.JSONArray();
		org.json.JSONArray tipoDeProveedor = new org.json.JSONArray();
		org.json.JSONArray matkllov = new org.json.JSONArray();
		org.json.JSONArray zcomalov = new org.json.JSONArray();
		org.json.JSONArray matkllovS4h = new org.json.JSONArray();
		org.json.JSONArray brandNameS4h = new org.json.JSONArray();
		Element nameElement = null;
		java.util.Map<String, java.util.LinkedList<Node>> nodeListMap = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject response = null;
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('TipoDeProveedorLOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('TipoProveedorSAPAttLOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV_S4H')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BRAND_IDLOV_S4H')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BusinessQualified')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		request.put("columns", columns);
		request.put("rows", rows);
		GeneralOperations go = new GeneralOperations();
		System.out.println("Collecting lkp values...");
		java.util.Map<String, String> zcomalovData = go.collectLookupValueData(rw.getBaseUrl(), "ZCOMALOV");
		System.out.println("Done with zcomalov");
		java.util.Map<String, String> matkllovData = go.collectLookupValueData(rw.getBaseUrl(), "MATKLLOV");
		System.out.println("Done with matkllov");
		java.util.Map<String, String> brandIdLoVData = go.collectLookupValueData(rw.getBaseUrl(), "BRAND_IDLOV_S4H");
		System.out.println("Done with brand_idlov_s4h");
		java.util.Map<String, String> matkllovS4hData = go.collectLookupValueData(rw.getBaseUrl(), "MATKLLOV_S4H");
		System.out.println("Done with matkllov_s4h");
		if(classificationNodeList != null) {
			for(Node n : classificationNodeList) {
				el = (Element)n;
				if(!"".equals(el.getAttribute("ID").replaceAll("-.+", ""))) {
					nameElement = (Element) xmm.byName(el, "Name");
					name = nameElement != null ? nameElement.getTextContent() : null;
					nodeListMap = xmm.listImmediateChildElements(el);
					metaDataNodeList = nodeListMap.get("MetaData");
					if(metaDataNodeList != null && !metaDataNodeList.isEmpty()) {
						metaData = metaDataNodeList.getFirst();
						valueNodeList = xmm.listImmediateChildElements(metaData).get("Value");
						multiValueNodeList = xmm.listImmediateChildElements(metaData).get("MultiValue");
						if(valueNodeList != null && !valueNodeList.isEmpty()) {
							for(Node nv : valueNodeList) {
								if("EmailProveedor".equals(((Element)nv).getAttribute("AttributeID"))) {
									
								}else if("TipoDeProveedor".equals(((Element)nv).getAttribute("AttributeID"))) {
									if(!"".equals(((Element)nv).getAttribute("ID")))
										tipoDeProveedor.put( ((Element)nv).getAttribute("ID") );
								}else if("TipoProveedorSAP".equals(((Element)nv).getAttribute("AttributeID"))) {
									if(!"".equals(((Element)nv).getAttribute("ID")))
										tipoProveedorSAP.put( ((Element)nv).getAttribute("ID") );
								}
							}
						}
						if(multiValueNodeList != null && !multiValueNodeList.isEmpty()) {
							for(Node nv : multiValueNodeList) {
								if("NegociosProveedor".equals(((Element)nv).getAttribute("AttributeID"))){
									valueNodeList = xmm.listImmediateChildElements(nv).get("Value");
									for(Node v : valueNodeList) {
										negocios.put(new org.json.JSONObject().put("id", changeBusinessKey( ((Element)v).getAttribute("ID")) ));
									}
								}
							}
						}
					}
					attributeLinkNodeList = nodeListMap.get("AttributeLink");
					if(attributeLinkNodeList != null) {
						for(Node nv : attributeLinkNodeList) {
							valueFilterNodeList = xmm.listImmediateChildElements(nv).get("ValueFilter");
							if(valueFilterNodeList != null && !valueFilterNodeList.isEmpty()) {
								valueNodeList = xmm.listImmediateChildElements(valueFilterNodeList.getFirst()).get("Value");
								for(Node v : valueNodeList) {
									if("ItemGroupS4H".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && matkllovS4hData.containsKey(((Element)v).getAttribute("ID")))
											matkllovS4h.put( ((Element)v).getAttribute("ID") );
									}else if("ItemGroup".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && matkllovData.containsKey(((Element)v).getAttribute("ID")))
											matkllov.put( ((Element)v).getAttribute("ID") );
									}else if("BRAND_ID_S4H".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && brandIdLoVData.containsKey(((Element)v).getAttribute("ID")))
											brandNameS4h.put( ((Element)v).getAttribute("ID") );
									}else if("BrandName".equals(((Element)nv).getAttribute("AttributeID"))) {
										if(!"".equals(((Element)v).getAttribute("ID")) && zcomalovData.containsKey(((Element)v).getAttribute("ID")))
											zcomalov.put( ((Element)v).getAttribute("ID") );
									}
								}
							}
						}
					}
					rows.put(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID").replaceAll("-.+", "") + "'@'Party'"))
							.put("values", new org.json.JSONArray()
									.put(tipoDeProveedor)
									.put(tipoProveedorSAP)
									.put(matkllov)
									.put(matkllovS4h)
									.put(zcomalov)
									.put(brandNameS4h)
									.put(negocios)
									.put(name == null ? "" : name)
									.put(true)
								)
						);
					tipoDeProveedor = new org.json.JSONArray();
					tipoProveedorSAP = new org.json.JSONArray();
					matkllov = new org.json.JSONArray();
					matkllovS4h = new org.json.JSONArray();
					zcomalov = new org.json.JSONArray();
					brandNameS4h = new org.json.JSONArray();
					negocios = new org.json.JSONArray();
					if(rows.length() == 1000) {
						response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
						if(response != null) {
							if(response.getJSONObject("counters").getInt("objectsWithErrors") > 0) {
								response.remove("counters");
								System.out.println(response);
							}else {
								System.out.println(response.getJSONObject("counters"));
							}
						}else {
							System.out.println("Error: " + rw.getRawResponse());
						}
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}
			}
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				if(response != null) {
					System.out.println(response.getJSONObject("counters"));
				}else {
					System.out.println("Error: " + rw.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		
	}
	
	private static void procedure7() {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		try(java.io.BufferedReader br = 
				new java.io.BufferedReader(
						new java.io.InputStreamReader(
								new java.io.FileInputStream(
										java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "relacion numero de imageness por plantilla.txt").toFile()
									), java.nio.charset.StandardCharsets.ISO_8859_1
							)
					)
			){
			String delim = "\"";
			String sep = "|";
			String esc = "";
			java.util.Arrays.asList(rw.parseLine(br.readLine(), delim, sep, esc)).forEach(System.out::println);
			String ln = null;
			String[] pieces = null;
			org.json.JSONObject request = new org.json.JSONObject();
			org.json.JSONArray columns = new org.json.JSONArray();
			org.json.JSONArray rows = new org.json.JSONArray();
			org.json.JSONObject response = null;
			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
			request.put("columns", columns);
			request.put("rows", rows);
			int times = 0;
			while((ln = br.readLine()) != null) {
				pieces = rw.parseLine(ln, delim, sep, esc);
				addPack(pieces[0], "ProductImage", "Fotografías", "1", "1", rows);
				addPack(pieces[0], "ProductImageDetail", "Fotografías", "0", pieces[2], rows);
				addPack(pieces[0], "Illustration", "Fotografías", "0", pieces[3], rows);
				addPack(pieces[0], "ProductImageSmosh", "Fotografías", "0", pieces[4], rows);
				if(!"".equals(pieces[4])) {
					System.out.println("This one has ProductImageSmosh: " + ln);
				}
				addPack(pieces[0], "LiverpoolManual", "Multimedia", "0", pieces[5], rows);
				if(!"".equals(pieces[5])) {
					System.out.println("This one has LiverpoolManual: " + ln);
				}
				addPack(pieces[0], "OwnersManual", "Multimedia", "0", pieces[6], rows);
				if(!"".equals(pieces[6])) {
					System.out.println("This one has OwnersManual: " + ln);
				}
				addPack(pieces[0], "NOM", "Multimedia", "0", pieces[7], rows);
				if(!"".equals(pieces[7])) {
					System.out.println("This one has NOM: " + ln);
				}
				times++;
				if(times % 120 == 0) {
					response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
					if(response != null) {
						if(response.getJSONObject("counters").getInt("objectsWithErrors") > 0) {
							response.remove("counters");
							System.out.println(response);
						}else {
							System.out.println(response.getJSONObject("counters"));
						}
					}else {
						System.out.println("ERROR: " + rw.getRawResponse());
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
				if(response != null) {
					if(response.getJSONObject("counters").getInt("objectsWithErrors") > 0) {
						response.remove("counters");
						System.out.println(response);
					}else {
						System.out.println(response.getJSONObject("counters"));
					}
				}else {
					System.out.println("ERROR: " + rw.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void addPack(String templateId, String characteristic, String vcs, String min, String max, org.json.JSONArray rows) {
		if(!"".equals(max)) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "<::>" + characteristic + "<::>CreateProposal<::>VendorCenterSection'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(templateId).put(characteristic).put("CreateProposal").put("VendorCenterSection").put(vcs) ));
//			RESTWorkshop rw = new RESTWorkshop();
//			rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
//			java.util.Map<String, String> qp = new java.util.TreeMap<>();
//			org.json.JSONObject request = new org.json.JSONObject();
//			org.json.JSONArray columns = new org.json.JSONArray();
//			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
//			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
//			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
//			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
//			columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
//			request.put("columns", columns);
//			request.put("rows", rows);
//			rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
//			System.out.println(rw.getRawResponse());
//			System.out.println(rows);
//			System.exit(0);
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "<::>" + characteristic + "<::>CreateProposal<::>SentToVendorCenter'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(templateId).put(characteristic).put("CreateProposal").put("SentToVendorCenter").put("1") ));
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "<::>" + characteristic + "<::>CreateProposal<::>Business'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(templateId).put(characteristic).put("CreateProposal").put("Business").put("Liverpool Suburbia Marketplace") ));
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "<::>" + characteristic + "<::>CreateProposal<::>VariantLevel'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(templateId).put(characteristic).put("CreateProposal").put("VariantLevel").put("1") ));
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "<::>" + characteristic + "<::>CreateProposal<::>Min'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(templateId).put(characteristic).put("CreateProposal").put("Min").put(min) ));
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "<::>" + characteristic + "<::>CreateProposal<::>Max'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(templateId).put(characteristic).put("CreateProposal").put("Max").put(max) ));
		}
	}
	
	private static String changeBusinessKey(String key) {
		return "LIVERPOOL".equals(key) ? "LVP" : "SUBURBIA".equals(key) ? "SBB" : "ART. MARKETPLACE".equals(key) ? "MKP" : key;
	}
	
	private static void log(String message) {
		System.out.println(message);
	}

}
