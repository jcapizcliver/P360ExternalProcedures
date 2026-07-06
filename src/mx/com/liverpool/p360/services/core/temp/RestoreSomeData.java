package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class RestoreSomeData {


	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		long init = System.currentTimeMillis();
//		restoreMasterData();
//		restoreAttributes();
//		restoreCharacteristicsForWhichThereIsAValueDefined();
//		returnStructureGroupCharacteristicCategories();

//		restoreSpecificCharacteristics();
		loadSpecificLookupValues();

//		restoreArticleAndProducts();

//		loadCharacteristicFilters();

		System.out.println(workshop.formatTime(System.currentTimeMillis() - init));
	}

	private static void loadCharacteristicFilters() throws SAXException, IOException, ParserConfigurationException {

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
			}

		}while(currentIndex < totalSize);
		currentIndex = 0;

		java.util.Map<String, String> characteristics = caracteristicasExistentes();
		java.util.Set<String> templates = templatesExistentes();

		System.out.println(characteristics.size());
		System.out.println(templates.size());

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Products");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> products = xmm.listImmediateChildElements(assetsRoot).get("Product");

		rows = new org.json.JSONArray();

		try {
			excavate(products.getFirst(), rows, characteristics, templates, únicoTamaño);
		} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException
				| IOException e) {
			e.printStackTrace();
		}
	}

	private static void excavate(Node n, org.json.JSONArray rows, java.util.Map<String, String> characteristics, java.util.Set<String> templates, java.util.Map<String, String> únicoTamaño) throws KeyManagementException, NoSuchAlgorithmException, JSONException, URISyntaxException, IOException {
		Element el = (Element)n;
		String structureGroupId = el.getAttribute("ID");
		String attributeId = null;
		java.util.LinkedList<Node> attributeLinks = xmm.listImmediateChildElements(n).get("AttributeLink");
		java.util.Map<String, java.util.LinkedList<Node>> vf = null;
		java.util.LinkedList<Node> valueFilter = null;
		java.util.LinkedList<Node> values = null;
		String code = null;
		String value = null;
		Element el1;
		org.json.JSONArray vals = new org.json.JSONArray();
		String lookup = null;

		java.util.regex.Matcher m = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("lookup for given code: ([0-9A-Z/a-z_-]+)");
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> empty = new java.util.TreeMap<>();

		qp.put("query", "LookupValueLang.Name(es) is empty");
		qp.put("lookup", "Party");
		qp.put("fields", "LookupValue.Code");
		qp.put("pageSize", "900");

		org.json.JSONObject column = new org.json.JSONObject().put("identifier", "");
		org.json.JSONArray columns = new org.json.JSONArray().put(column).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONObject payload = new org.json.JSONObject();
		payload.put("columns", columns);
		payload.put("rows", rowsPayload);
		if(structureGroupId.startsWith("EU4-") && attributeLinks != null) {
			for(Node n0 : attributeLinks) {
				el = (Element) n0;
				attributeId = el.getAttribute("AttributeID");
				vf = xmm.listImmediateChildElements(n0);
				if(vf != null) {
					valueFilter = vf.get("ValueFilter");
					if(valueFilter != null && !valueFilter.isEmpty()) {
						lookup = characteristics.get(attributeId);
						if(lookup == null) {
							continue;
						}
						column.put("identifier", "LookupValueReference.LookupValues('" + lookup + "')");
						values = xmm.listImmediateChildElements(valueFilter.getFirst()).get("Value");
						if(values != null) {
							for(Node n1 : values) {
								el1 = (Element) n1;
								code = el1.getAttribute("ID");
								value = el1.getTextContent();
								if(code == null) {
									code = únicoTamaño.get(value);
								}
								if(code != null && !"".equals(code)) {
									vals.put(code);
								}
							}
							System.out.println(structureGroupId + " - " + attributeId + " - " + lookup);
							if(vals.length() > 0) {

								rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + structureGroupId + "_" + attributeId + "'@'TemplateCharacteristic'")).put("values", new org.json.JSONArray().put(vals).put(true)));

								boolean done = false;
								do {
									response = workshop.makeRequest("POST", "/list/LookupValue", empty, payload.toString());
									if(response.getJSONArray("entries").length() > 0) {
										m = p.matcher(response.getJSONArray("entries").getJSONObject(0).getString("message"));
										if(m.find()) {
											int f = -1;
											String mc = m.group(1);
											for(int v=0; v<vals.length(); v++) {
												if(vals.getString(v).equals(mc)) {
													f = v;
													break;
												}
											}
											if(f > -1) {
												vals.remove(f);
												System.out.println(response.getJSONArray("entries").getJSONObject(0).getString("message"));
											}else {
												System.out.println("Couldn't determine missing value. " + response);
												System.exit(6);
											}
										}else {
											System.out.println("Couldn't determine missing value elp. " + response);
											System.exit(6);
										}
									}else {
										done = true;
									}
								}while(!done);
								while(rowsPayload.length() > 0) {
									rowsPayload.remove(0);
								}
								while(vals.length() > 0) {
									vals.remove(0);
								}
							}
						}
					}
				}
			}
		}
		java.util.LinkedList<Node> children = xmm.listImmediateChildElements(n).get("Product");
		if(children != null) {
			for(Node n0 : children) {
				excavate(n0, rows, characteristics, templates, únicoTamaño);
			}
		}

	}

	private static void projectCharacteristics() {

		java.util.LinkedList<org.json.JSONArray> charIds = new java.util.LinkedList<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		int totalSize = 0;
		int currentIndex = 0;

		qp.put("query", "not Characteristic.Identifier is empty and Characteristic.ParentCharacteristic is empty and not Characteristic.Identifier wildcard \"%_Rechazo\"");
		qp.put("fields", "Characteristic.Identifier,CharacteristicLang.Name(es)");
		qp.put("pageSize", "900");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				charIds.addLast(values);
			}
		}while(currentIndex<totalSize);
		currentIndex = 0;
		System.out.println("Caught " + charIds.size() + " characteristics");
		java.util.Collections.sort(charIds, (o1,o2)-> o1.getString(0).compareTo(o2.getString(0)));
		charIds.forEach(System.out::println);

		org.json.JSONObject payload = new org.json.JSONObject();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		payload.put("columns", columns).put("rows", rowsPayload);

		java.util.Map<String, String> empty = new java.util.TreeMap<>();

		for(org.json.JSONArray vals : charIds) {
			rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + vals.getString(0) + "'@'Characteristics'")).put("values", new org.json.JSONArray().put(vals.getString(1)).put(true)));
			if(rowsPayload.length() == 250) {
				System.out.println( workshop.makeRequest("POST", "/list/LookupValue", empty, payload.toString()) );
				while(rowsPayload.length() > 0) {
					rowsPayload.remove(0);
				}
			}
		}
		if(rowsPayload.length() > 0) {
			System.out.println( workshop.makeRequest("POST", "/list/LookupValue", empty, payload.toString()) );
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
	}

	private static java.util.Map<String, String> caracteristicasExistentes(){
		java.util.Map<String, String> characteristics = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		int totalSize = 0;
		int currentIndex = 0;

		qp.put("query", "not Characteristic.Lookup is empty");
		qp.put("fields", "Characteristic.Identifier,Characteristic.Lookup->Lookup.Identifier and not Characteristic.Identifier wildcard \"%_Rechazo\" and Characteristic.ParentCharacteristic is empty");
		qp.put("pageSize", "900");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				characteristics.put(values.getString(0), values.getString(1));
			}
		}while(currentIndex<totalSize);
		currentIndex = 0;

		return characteristics;
	}

	private static java.util.Set<String> templatesExistentes(){
		java.util.Set<String> templates = new java.util.TreeSet<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		int totalSize = 0;
		int currentIndex = 0;

		qp.put("lookup", "PPH_L4_Templates");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("fields", "LookupValue.Code");
		qp.put("pageSize", "900");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				templates.add(values.getString(0));
			}
		}while(currentIndex<totalSize);
		currentIndex = 0;

		return templates;
	}


	private static void loadSpecificLookupValues(String[] lkps) {

		org.json.JSONArray sourceValues = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(en)")).put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(ATG)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		for(String lkp : lkps) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\data\\general_lookup_" + lkp + ".dat")))){
				String line = null;
				while((line = br.readLine()) != null) {
					if(!"null".equals(line)) {
						try {
							sourceValues = new org.json.JSONArray(line);
							values = new org.json.JSONArray().put(sourceValues.getString(1)).put(sourceValues.getString(2)).put(sourceValues.getString(3)).put(sourceValues.getString(4));
							rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + sourceValues.getString(0) + "'@'" + lkp + "'")).put("values", values));
							if(rows.length() == 265) {
								System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
								while(rows.length() > 0) {
									rows.remove(0);
								}
							}
						}catch(org.json.JSONException e) {
							System.out.println("Invalid JSONObject: " + line);
						}
					}
				}
				if(rows.length() > 0) {
					System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void loadSpecificLookupValues() {

		org.json.JSONArray sourceValues = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(en)")).put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(ATG)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		String[] lkps = new String[] {
//				"Direction",
				"Direction",
//				"UnidadDeMedidaLongitudLOV"
				};

		for(String lkp : lkps) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\data\\general_lookup_" + lkp + ".dat")))){
				String line = null;
				while((line = br.readLine()) != null) {
					if(!"null".equals(line)) {
						try {
							sourceValues = new org.json.JSONArray(line);
							values = new org.json.JSONArray().put(sourceValues.getString(1)).put(sourceValues.getString(2)).put(sourceValues.getString(3)).put(sourceValues.getString(4));
							rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + sourceValues.getString(0) + "'@'" + lkp + "'")).put("values", values));
							if(rows.length() == 265) {
								System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
								while(rows.length() > 0) {
									rows.remove(0);
								}
							}
						}catch(org.json.JSONException e) {
							System.out.println("Invalid JSONObject: " + line);
						}
					}
				}
				if(rows.length() > 0) {
					System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String checkLookupValue(String externalId, java.util.Map<String, String> qp) throws ServiceUnavailableException {

		try {
			return workshop.getRc().getRequest("GET", workshop.getBaseUrl() + "/object/LookupValue/" + externalId, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void restoreArticleAndProducts() throws ServiceUnavailableException {

		org.json.JSONObject json = null;
		org.json.JSONObject data = null;
		org.json.JSONObject response = null;
		org.json.JSONObject protocol = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.regex.Matcher m = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("Code value '(.+)' not found in enumeration");
		String illValue = null;
		org.json.JSONArray characteristicRecords = null;
		org.json.JSONObject cr = null;
		org.json.JSONArray values = null;
		String lkpCheck = null;
		String[] pieces = null;
		String code = null;
		String lookup;
		org.json.JSONObject sr = null;
		org.json.JSONArray srr = null;
		java.util.Map<String, String> qps = new java.util.TreeMap<>();
		String externalId = null;
		String nv = null;
		org.json.JSONObject lookupValue = null;
		org.json.JSONObject value = null;
		org.json.JSONArray fileRow = null;
		boolean done = false;
		boolean found = false;

		java.util.Map<String, java.util.Map<String, String>> lookupValuesInServer = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> lookupValuesInFiles = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, java.util.LinkedList<String>>> lookupValuesInFileAsList = new java.util.TreeMap<>();
		java.util.Map<String, String> lkpContent = null;
		java.util.Map<String, String> lkpContentFile = null;
		java.util.Map<String, java.util.LinkedList<String>> lkpContentFileAsList = null;
		java.util.LinkedList<String> lkpCodes = null;

		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp0.put("query", "LookupValue.IsActive = true");
		qp0.put("pageSize", "900");
		int ci = 0;
		int tz = 0;
		org.json.JSONObject rsp = null;
		org.json.JSONArray rws = null;
		org.json.JSONArray vls = null;
		String valTest = null;
		String nc = null;


		System.out.println("Now articles....");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\Article_objects.dat")))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"null".equals(line)) {
					try {
						json = new org.json.JSONObject(line);
						if(json.has("_data")) {
							data = json.getJSONObject("_data");
							data.remove("log");
							data.remove("status");
							done = false;
							do {
								response = workshop.makeRequest("PUT", "/object/Product2G/" + json.getJSONObject("_entityItem").getString("_externalId"), qp, data.toString());
								if(response.getJSONObject("_protocol").getInt("errorCounter") > 0) {
									protocol = response.getJSONObject("_protocol");
									m = p.matcher(protocol.getJSONArray("entries").getJSONObject(0).getString("message"));
									if(m.find()) {
										illValue = m.group(1);
										characteristicRecords = data.getJSONArray("_characteristicRecords");
										for(int i=0; i<characteristicRecords.length(); i++) {
											cr = characteristicRecords.getJSONObject(i);
											if(cr.has("_recordLang")) {
												values = cr.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
												if( values.get(0) instanceof org.json.JSONObject ) {
													if( illValue.equals( values.getJSONObject(0).getString("_code") ) ) {
														externalId = cr.getJSONArray("lookupValue").getJSONObject(0).getString("_externalId");
														lkpCheck = checkLookupValue(workshop.encode( externalId ) , qp);
														if("Response code: 404".equals(lkpCheck)) {
															pieces = externalId.split("@");
															lookup = pieces[1].replaceAll("((^')|('$))", "");
															code = pieces[0].replaceAll("((^')|('$))", "");
															qps.put("lookup", lookup);
															qps.put("query", "LookupValue.Code wildcard \"%" + code + "\" and LookupValue.Code startsWith \"0\"");
															qps.put("fields", "LookupValue.Code");
															qps.put("pageSize", "900");
															sr = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qps, null);
															srr = sr.getJSONArray("rows");
															for(int z = 0; z<srr.length(); z++) {
																nv = srr.getJSONObject(z).getJSONArray("values").getString(0);
																if(nv.matches("^0+" + java.util.regex.Pattern.quote(code))) {
																	cr.getJSONArray("lookupValue").getJSONObject(0).put("_externalId", "'" + nv + "'@'" + lookup + "'");
																	values.put(0, new org.json.JSONObject().put("_code", nv));
																	System.out.println("\t\t\tUsing: " + nv + "||" + cr.getJSONArray("lookupValue").getJSONObject(0).get("_externalId"));
																	found = true;
																	break;
																}
															}
															if(found) {
																found = false;
																break;
															}else {
																System.out.println("\t\tRemoved: " + characteristicRecords.remove(i) );
																break;
															}
														}
//														System.out.println( lkpCheck );
//														System.exit(8);
													}
												}
											}
										}
									}else {
										System.out.println(response.getJSONObject("_protocol"));
										System.out.println("\t" + data);
//										System.exit(9);
									}
								}else {
									System.out.println("Success: " + response );
									done = true;
								}
							}while(!done);
						}
					}catch(org.json.JSONException e) {
						System.out.println("Invalid JSONObject: " + line);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Now Poduct2G");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\Product2G_objects.dat")))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"null".equals(line)) {
					try {
						json = new org.json.JSONObject(line);
						if(json.has("_data")) {
							data = json.getJSONObject("_data");
							data.remove("log");
							data.remove("status");
							characteristicRecords = data.getJSONArray("_characteristicRecords");
							for(int i=0; i<characteristicRecords.length(); i++) {
								cr = characteristicRecords.getJSONObject(i);
								if("LOOKUP".equals(cr.getString("_datatype"))) {
									value = cr.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0);
									lookupValue = cr.getJSONArray("lookupValue").getJSONObject(0);
//									System.out.println(value);
//									System.out.println(lookupValue);
									externalId = lookupValue.getString("_externalId");
									pieces = externalId.split("@");
									lookup = pieces[1].replaceAll("((^')|('$))", "");
									code = pieces[0].replaceAll("((^')|('$))", "");
//									System.out.println(lookup);
//									System.out.println(code);
//									System.out.println("D:\\data\\general_lookup_" + lookup + ".dat");
									lkpContentFileAsList = lookupValuesInFileAsList.get(lookup);
									if(lkpContentFileAsList == null) {
										java.nio.file.Path fp = java.nio.file.Paths.get("D:\\data\\general_lookup_" + lookup + ".dat");
										if(!java.nio.file.Files.exists(fp)) {
											fp = java.nio.file.Paths.get("D:\\data\\lookup_" + lookup + ".dat");
										}
										try(java.io.BufferedReader br0 = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(fp.toString())))){
											String ln = null;
											lkpContent = new java.util.TreeMap<>();
											lkpContentFileAsList = new java.util.TreeMap<>();
											lookupValuesInFiles.put(lookup, lkpContent);
											lookupValuesInFileAsList.put(lookup, lkpContentFileAsList);
											while((ln = br0.readLine()) != null) {
//												System.out.println(ln);
												fileRow = new org.json.JSONArray(ln);
												lkpContent.put(fileRow.getString(0), fileRow.getString(1));
												lkpCodes = lkpContentFileAsList.get(fileRow.getString(1));
												if(lkpCodes == null) {
													lkpCodes = new java.util.LinkedList<>();
													lkpContentFileAsList.put(fileRow.getString(1), lkpCodes);
												}
												lkpCodes.addLast(fileRow.getString(0));
											}
										}catch(java.io.IOException e) {
											e.printStackTrace();
										}
//										System.out.println("LKP from files: " + lookupValuesInFileAsList);
									}
									lkpContent = lookupValuesInServer.get(lookup);
									if(lkpContent == null) {
										lkpContent = new java.util.TreeMap<>();
										lookupValuesInServer.put(lookup, lkpContent);
										qp0.put("lookup", lookup);
										do{
											qp0.put("startIndex", String.valueOf( ci ));
											rsp = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp0, null);
											tz = rsp.getInt("totalSize");
											rws = rsp.getJSONArray("rows");
											for(int k=0; k<rws.length(); k++) {
												ci++;
												vls = rws.getJSONObject(k).getJSONArray("values");
												lkpContent.put(vls.getString(0), vls.getString(1));
											}
										}while(ci < tz);
										ci = 0;
//										System.out.println("LKP from server: " + lookupValuesInServer);
									}
									lkpContent = lookupValuesInServer.get(lookup);
									valTest = lkpContent.get(code);
									if(valTest == null) {
										lkpContentFileAsList = lookupValuesInFileAsList.get(lookup);
										lkpContentFile = lookupValuesInFiles.get(lookup);
										valTest = lkpContentFile.get(code);
										if(valTest == null) {
											System.out.println("PANIC::Data not found: " + code);
											System.out.println(lookup);
											System.out.println(code);
											System.exit(11);
										}
										lkpCodes = lkpContentFileAsList.get(valTest);
										if(lkpCodes == null) {
											System.out.println("PANIC::Data not found in files: " + code);
											System.out.println(lookup);
											System.out.println(code);
											System.exit(11);
										}
										nc = null;
										for(String cd : lkpCodes) {
											if(!cd.equals(code)) {
												nc = lkpContent.get(cd);
												if(nc != null) {
													nc = cd;
													break;
												}
											}
										}
										if(nc != null) {
											value.put("_code", nc);
											lookupValue.put("_externalId", "'" + nc + "'@'" + lookup + "'");
											System.out.println("Así quedó: " + cr);
											System.exit(i);
										}
									}
								}
							}
//							done = false;
//							do {
//								response = workshop.makeRequest("PUT", "/object/Product2G/" + json.getJSONObject("_entityItem").getString("_externalId"), qp, data.toString());
//								System.out.println(response);
//								if(response.getJSONObject("_protocol").getInt("errorCounter") > 0) {
//									protocol = response.getJSONObject("_protocol");
//									m = p.matcher(protocol.getJSONArray("entries").getJSONObject(0).getString("message"));
//									if(m.find()) {
//										illValue = m.group(1);
//										System.out.println("Looking for following ill value: " + illValue + "..." + response.getJSONObject("_protocol"));
//										System.out.println("\t" + data);
//										characteristicRecords = data.getJSONArray("_characteristicRecords");
//										for(int i=0; i<characteristicRecords.length(); i++) {
//											characteristicRecord = characteristicRecords.getJSONObject(i);
//											if(characteristicRecord.has("_recordLang")) {
//												values = characteristicRecord.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
//												if( values.get(0) instanceof org.json.JSONObject ) {
//													if( illValue.equals( values.getJSONObject(0).getString("_code") ) ) {
//														externalId = characteristicRecord.getJSONArray("lookupValue").getJSONObject(0).getString("_externalId");
//														lkpCheck = checkLookupValue(workshop.encode( externalId ) , qp);
//														if("Response code: 404".equals(lkpCheck)) {
//															pieces = externalId.split("@");
//															lookup = pieces[1].replaceAll("((^')|('$))", "");
//															code = pieces[0].replaceAll("((^')|('$))", "");
//															qps.put("lookup", lookup);
//															qps.put("query", "LookupValue.Code wildcard \"%" + code + "\" and LookupValue.Code startsWith \"0\"");
//															qps.put("fields", "LookupValue.Code");
//															qps.put("pageSize", "900");
//															sr = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qps, null);
//															srr = sr.getJSONArray("rows");
//															for(int z = 0; z<srr.length(); z++) {
//																nv = srr.getJSONObject(z).getJSONArray("values").getString(0);
//																if(nv.matches("^0+" + java.util.regex.Pattern.quote(code))) {
//																	characteristicRecord.getJSONArray("lookupValue").getJSONObject(0).put("_externalId", "'" + nv + "'@'" + lookup + "'");
//																	values.put(0, new org.json.JSONObject().put("_code", nv));
//																	System.out.println("\t\t\tUsing: " + nv + "||" + characteristicRecord.getJSONArray("lookupValue").getJSONObject(0).get("_externalId"));
//																	found = true;
//																	break;
//																}
//															}
//															if(found) {
//																found = false;
//																continue;
//															}else {
//																System.out.println("\t\tRemoved: " + characteristicRecords.remove(i) );
//																break;
//															}
//														}
//													}
//												}
//											}
//										}
//									}else {
//										System.out.println(response.getJSONObject("_protocol"));
//										System.out.println("\t" + data);
//										System.exit(9);
//									}
//								}else {
//									System.out.println("Success: " + response );
//									done = true;
//								}
//							}while(!done);
						}
					}catch(org.json.JSONException e) {
						System.out.println("Invalid JSONObject: " + line);
						e.printStackTrace();
						System.exit(10);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

	private static void restoreCharacteristicsForWhichThereIsAValueDefined() {

		java.util.Set<String> ofInterest = characteristicsFromArticleAndProduct();

		java.util.Set<String> restored = new java.util.TreeSet<>();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "not Characteristic.Identifier is empty");
		qp.put("fields", "Characteristic.Identifier");
		qp.put("pageSize", "900");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				restored.add(values.getString(0));
			}
		}while(currentIndex<totalSize);
		currentIndex = 0;

		System.out.println("Found " + restored.size() + " current characteristics");

		java.util.Map<String, org.json.JSONObject> characteristicObjects = recoverSomeCharacteristics();
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		org.json.JSONObject characteristic = null;
		for(java.util.Map.Entry<String, org.json.JSONObject> entry : characteristicObjects.entrySet()) {
			for(String ch : ofInterest) {
				if(entry.getKey().contains(ch)) {
					if(!restored.contains(entry.getKey())) {
						characteristic = characteristicObjects.get(entry.getKey());
						if(characteristic != null) {
							System.out.println( workshop.makeRequest("POST", "/object/Characteristic", empty, characteristic.getJSONObject("_data").toString()) );
						}
						restored.add(entry.getKey());
					}
					break;
				}
			}
		}

	}

	private static java.util.Set<String> characteristicsFromArticleAndProduct(){
		java.util.Set<String> chars = new java.util.TreeSet<>();
		org.json.JSONObject json = null;

		String[] files = new String[] {"Article", "Product2G"};

		org.json.JSONObject data = null;
		org.json.JSONArray characteristicRecords = null;
		org.json.JSONObject characteristicRecord = null;
		org.json.JSONArray children = null;
		org.json.JSONObject child = null;

		for(String fil : files) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\" + fil + "_objects.dat")))){
				String line = null;
				while((line = br.readLine()) != null) {
					if(!"null".equals(line)) {
						try {
							json = new org.json.JSONObject(line);
							data = json.getJSONObject("_data");
							if(data.has("_characteristicRecords")) {
								characteristicRecords = data.getJSONArray("_characteristicRecords");
								for(int i=0; i<characteristicRecords.length(); i++) {
									characteristicRecord = characteristicRecords.getJSONObject(i);
									chars.add(characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
									if(characteristicRecord.has("_children")) {
										children = characteristicRecord.getJSONArray("_children");
										for(int j = 0; j<children.length(); j++) {
											child = children.getJSONObject(j);
											chars.add(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
										}
									}
								}
							}
						}catch(org.json.JSONException e) {
							System.out.println("Invalid JSONObject: " + line);
							e.printStackTrace();
							System.exit(-1);
						}
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		return chars;
	}

	private static void restoreSpecificCharacteristics() {

		java.util.Set<String> restored = new java.util.TreeSet<>();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "not Characteristic.Identifier is empty");
		qp.put("fields", "Characteristic.Identifier");
		qp.put("pageSize", "900");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				restored.add(values.getString(0));
			}
		}while(currentIndex<totalSize);
		currentIndex = 0;

		System.out.println("Found " + restored.size() + " current characteristics");

		java.util.Map<String, org.json.JSONObject> characteristicObjects = recoverSomeCharacteristics();
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		org.json.JSONObject characteristic = null;
		org.json.JSONArray lang = null;
		boolean still = true;
		int i=0;
		String[] characteristics = new String[] {
				"LadiesSizeAtt",
				"ShoeSizeLivAtt",
				"SportsSizeAtt",
				"MenSizeAtt",
				"ChildrenSizeAtt",
				"OpticalSizeAtt",
				"SizeCosmeticsAccAtt",
				"Direction1SizeAtt",
				"Direction3SizeAtt",
				"TamanoDireccion6Att",
				"TamanoDireccion8Att",
				"TamanoPantallaAtt",
				"SB_TCABALLEROS",
				"SB_TCALCETERIA",
				"SB_TDAMAS",
				"SB_TINFANTILES",
				"SB_TJUNIORS",
				"SB_TLENCERIA",
				"SB_TZAPATOS",
				"SB_TBEBES",
				"SB_TROPAINTERIOR",
				"SB_TJOYERIAYACCESORIOS",
				"SB_THOGAR",
				"SB_COLORES"
//				"DenierVaD"
//				"ID",
//				"ParentID"
//				"ProductImage",
//				"ProductImageDetail",
//				"Illustration",
//				"ProductImageSmosh",
//				"ProductVideo",
//				"LiverpoolManual",
//				"NOM",
//				"OwnersManual",
//				"EsSostenible",
//				"CertificadoSostenible"
		};
		for(java.util.Map.Entry<String, org.json.JSONObject> entry : characteristicObjects.entrySet()) {
			for(String ch : characteristics) {
				if(entry.getKey().contains(ch)) {
					if(!restored.contains(entry.getKey())) {
						characteristic = characteristicObjects.get(entry.getKey());
						if(characteristic != null) {
							if(characteristic.getJSONObject("_data").has("lang")) {
								lang = characteristic.getJSONObject("_data").getJSONArray("lang");
								while(i < lang.length()) {
									if(lang.getJSONObject(i).has("defaultValue")) {
										lang.remove(i);
										break;
									}
									i++;
								}
							}
							response = workshop.makeRequest("POST", "/object/Characteristic", empty, characteristic.getJSONObject("_data").toString());
							if(response.getJSONObject("_protocol").getInt("errorCounter") > 0) {
								System.out.println("\t" + characteristic);
							}else {
								System.out.println( response );
							}
						}
						restored.add(entry.getKey());
					}
					break;
				}
			}
		}

	}

	private static void returnStructureGroupCharacteristicCategories() {

		org.json.JSONArray values = null;
		org.json.JSONObject row = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject payload = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.CharacteristicCategories"));
		payload.put("columns", columns).put("rows", rows);

		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\data\\structureGroupCategories.dat")))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"null".equals(line)) {
					try {
						row = new org.json.JSONObject(line);
						values = row.getJSONArray("values");
						values.remove(0);
						rows.put(row);
						if(rows.length() == 250) {
							System.out.println( workshop.makeRequest("POST", "/list/StructureGroup", qp, payload.toString()) );
							while(rows.length() > 0) {
								rows.remove(0);
							}
						}
					}catch(org.json.JSONException e) {
						System.out.println("Invalid JSONObject: " + line);
					}
				}
			}
			if(rows.length() > 0) {
				System.out.println( workshop.makeRequest("POST", "/list/StructureGroup", qp, payload.toString()) );
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

	private static void restoreAttributes() throws SAXException, IOException, ParserConfigurationException {

		java.util.Map<String, org.json.JSONObject> characteristicObjects = recoverSomeCharacteristics();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Products");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> products = xmm.listImmediateChildElements(assetsRoot).get("Product");

		java.util.Set<String> restored = new java.util.TreeSet<>();
		java.util.Set<String> notFound = new java.util.TreeSet<>();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "not Characteristic.Identifier is empty");
		qp.put("fields", "Characteristic.Identifier");
		qp.put("pageSize", "900");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				restored.add(values.getString(0));
			}
		}while(currentIndex<totalSize);
		currentIndex = 0;

		System.out.println("Found: " + restored.size() + " characteristics currently restored");

		for(Node n : products) {
			drill((Element)n, characteristicObjects, restored, notFound);
		}



	}

	private static void drill(Element el, java.util.Map<String, org.json.JSONObject> characteristics, java.util.Set<String> restored, java.util.Set<String> notFound) {

		Element element = null;
		String id = null;
		java.util.Map<String, java.util.LinkedList<Node>> nodeMap = null;
		java.util.LinkedList<Node> attributeLinkList = null;
		java.util.LinkedList<Node> productList = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		org.json.JSONObject characteristic = null;

		nodeMap = xmm.listImmediateChildElements(el);
		if(nodeMap != null) {
			productList = nodeMap.get("Product");
			if(productList != null && !productList.isEmpty()) {
				for(Node n : productList) {
					drill((Element)n, characteristics, restored, notFound);
				}
			}else {
				attributeLinkList = nodeMap.get("AttributeLink");
				if(attributeLinkList != null) {
					for(Node n0 : attributeLinkList) {
						element = (Element)n0;
						id = element.getAttribute("AttributeID");
						characteristic = characteristics.get(id);
						if(characteristic != null) {
							if(!restored.contains(id) && !notFound.contains(id)){
								System.out.println( workshop.makeRequest("POST", "/object/Characteristic", empty, characteristic.getJSONObject("_data").toString()) );
								restored.add(id);
							}
						}else {
							if(!notFound.contains(id)) {
								System.out.println("Couldn't find: " + id);
								notFound.add(id);
							}
						}
					}
				}else {
					System.out.println("Check this: " + el.getAttribute("ID"));
				}
			}
		}

	}

	private static void restoreMasterData() throws SAXException, IOException, ParserConfigurationException {

		java.util.Map<String, org.json.JSONObject> characteristicObjects = recoverSomeCharacteristics();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Products");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> products = xmm.listImmediateChildElements(assetsRoot).get("Product");

		Element el = null;
		String id = null;

		java.util.Map<String, java.util.LinkedList<Node>> attributeLinkNodeMap = null;
		java.util.LinkedList<Node> attributeLinkList = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		org.json.JSONObject characteristic = null;

		for(Node n : products) {
			attributeLinkNodeMap = xmm.listImmediateChildElements(n);
			if(attributeLinkNodeMap != null) {
				attributeLinkList = attributeLinkNodeMap.get("AttributeLink");
				for(Node n0 : attributeLinkList) {
					el = (Element)n0;
					id = el.getAttribute("AttributeID");
					characteristic = characteristicObjects.get(id);
					if(characteristic != null) {
						System.out.println( workshop.makeRequest("POST", "/object/Characteristic", empty, characteristic.getJSONObject("_data").toString()) );
					}else {
						System.out.println("Couldn't find: " + id);
					}
				}
			}
		}

	}

	private static java.util.Map<String, org.json.JSONObject> recoverSomeCharacteristics() {

		java.util.Map<String, org.json.JSONObject> characteristicObjects = new java.util.TreeMap<>();

		org.json.JSONObject json = null;

		java.util.regex.Matcher m = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("'(.+)'");
		String externalId = null;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\Characteristic_objects.dat")))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"null".equals(line)) {
					try {
						json = new org.json.JSONObject(line);
						externalId = json.getJSONObject("_entityItem").getString("_externalId");
						m = p.matcher(externalId);
						if(m.find()) {
							externalId = m.group(1);
							characteristicObjects.put(externalId, json);
						}else {
							System.out.println("Problem identifying id for: " + json);
							System.exit(4);
						}
					}catch(org.json.JSONException e) {
						System.out.println("Invalid JSONObject: " + line);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

		return characteristicObjects;
	}

	private static void restoreLookupValues() {

		String lookupName = null;
		String code = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValue.Code")).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(en)")).put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(ATG)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));

		java.io.File[] files = new java.io.File("D:\\data").listFiles(ff-> ff.getName().startsWith("general_lookup") || ff.getName().startsWith("lookup") );

		int count = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		for(java.io.File f : files) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				lookupName = f.getName().replaceAll(( "(^(" + ( f.getName().startsWith("general_lookup") ?  "general_lookup"  : "lookup" ) + "_))|((\\.dat)$)" ), "");
				String line = null;
				while((line = br.readLine()) != null) {
					count++;
					values = new org.json.JSONArray(line);
					code = values.getString(0);
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + workshop.encode( code ) + "'@'" + workshop.encode(lookupName) + "'")).put("values", values));
					if(rows.length() == 250) {
						response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
						System.out.println("Count: " + count +  ", createdObjects: " + response.getJSONObject("counters").getInt("createdObjects") + ", updated: " + response.getJSONObject("counters").getInt("updatedObjects") + ", errors: " + response.getJSONObject("counters").getInt("objectsWithErrors"));
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
					if(count % 10000 == 0) {
						System.out.print(".");
						if(count % 100000 == 0) {
							System.out.println(count);
						}
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
			System.out.println("Count: " + count +  ", createdObjects: " + response.getJSONObject("counters").getInt("createdObjects") + ", updated: " + response.getJSONObject("counters").getInt("updatedObjects") + ", errors: " + response.getJSONObject("counters").getInt("objectsWithErrors"));
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		System.out.println(count);
	}

	private static void fixLengthValues() {

		String lookupName = null;
		String code = null;

		org.json.JSONArray values = null;

		java.io.File[] files = new java.io.File("D:\\data").listFiles(ff-> ff.getName().startsWith("general_lookup") || ff.getName().startsWith("lookup") );

		int count = 0;

		java.util.Map<Integer, Integer> lengthFreqs = null;
		java.util.Map<Integer, java.util.LinkedList<String>> lengthLists = null;

		java.util.LinkedList<String> list = null;
		java.lang.Integer freq = null;

		StringBuilder sb = new StringBuilder();

		RESTWorkshop workshop = new RESTWorkshop();
		workshop.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		System.out.println(workshop.getRc().getHeader());
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		for(java.io.File f : files) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				lookupName = f.getName().replaceAll(( "(^(" + ( f.getName().startsWith("general_lookup") ?  "general_lookup"  : "lookup" ) + "_))|((\\.dat)$)" ), "");
				System.out.println(lookupName);
				qp.put("lookup", lookupName);
				String line = null;
				lengthFreqs = new java.util.TreeMap<>();
				lengthLists = new java.util.TreeMap<>();
				while((line = br.readLine()) != null) {
					count++;
					values = new org.json.JSONArray(line);
					code = values.getString(0);
					list = lengthLists.get(code.length());
					if(list ==  null) {
						list = new java.util.LinkedList<>();
						lengthLists.put(code.length(), list);
					}
					list.addLast(code);
					freq = lengthFreqs.get(code.length());
					lengthFreqs.put(code.length(), (freq == null ? 0 : freq) + 1);
				}
				if(!lengthLists.isEmpty()) {
					java.util.LinkedList<java.util.Map.Entry<Integer, java.util.LinkedList< String >>> entrySet = new java.util.LinkedList<>( lengthLists.entrySet() );
					java.util.Collections.sort(entrySet, (o1,o2)-> o2.getKey().compareTo(o1.getKey()) );
					int maxLength = entrySet.getFirst().getKey();
					int aggregated = 0;
					Integer maxLengthFreq = lengthFreqs.remove(maxLength);
					java.util.Iterator<java.util.Map.Entry<Integer, Integer>> iter = lengthFreqs.entrySet().iterator();
					java.util.Map.Entry<Integer, Integer> entry;
					while(iter.hasNext()) {
						entry = iter.next();
						aggregated += entry.getValue();
					}
					if(Integer.compare(aggregated, maxLengthFreq) <= maxLength) {
						entrySet.removeFirst();
						for(java.util.Map.Entry<Integer, java.util.LinkedList<String>> codes : entrySet) {
							for(String cd : codes.getValue()) {
								sb.append(sb.length() == 0 ? "" : ",")
									.append("\"").append( cd ).append("\"");
//								.append(cd);
							}
						}
						System.out.println("File matches the case: " + f.getName());
					}else {
						System.out.println("In this case length freqs did not match: " + f.getName());
					}

					if(sb.length() > 0) {
						System.out.println("--->" + sb.toString());
						qp.put("query", "LookupValue.Code in (" + sb.toString() + ")");
						System.out.println( workshop.makeRequest("DELETE", "/list/LookupValue/bySearch", qp, null) );
						sb.setLength(0);
					}
				}
				count = 0;
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		if(sb.length() > 0) {
			qp.put("items", sb.toString());
			System.out.println( workshop.makeRequest("DELETE", "/list/LookupValue/byItems", qp, null) );
			sb.setLength(0);
		}
		System.out.println(count);

	}
}
