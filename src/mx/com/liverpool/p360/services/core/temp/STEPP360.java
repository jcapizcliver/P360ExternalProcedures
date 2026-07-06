package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class STEPP360 {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

	private static boolean readFiles = true;

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("D:\\tmp\\muestras\\BGP_DWH\\DWH_639030950-639031791.xml");
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Assets");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> assetNodeList = xmm.listImmediateChildElements(assetsRoot).get("Asset");

		java.util.Map<String, Element> assetMap = new java.util.TreeMap<>();

		Element el = null;
		for(Node n : assetNodeList) {
			el = (Element)n;
			assetMap.put(el.getAttribute("ID"), el);
		}

		java.util.Map<String, String> fieldAndLookup = new java.util.TreeMap<>();
		java.util.Set<String> lookups = new java.util.TreeSet<>();
		java.util.LinkedList<Node> attributeNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("AttributeList").getFirst()).get("Attribute");
		java.util.LinkedList<Node> listOfValueNodeList = null;

		for(Node n : attributeNodeList) {
			el = (Element) n;
			listOfValueNodeList = xmm.listImmediateChildElements(n).get("ListOfValueLink");
			if(listOfValueNodeList != null) {
				if("ZMEACJ".equals(((Element)n).getAttribute("ID"))) {
					lookups.add( "UnidadDeMedidaLOV" );
					fieldAndLookup.put( ((Element)n).getAttribute("ID") , "UnidadDeMedidaLOV");
				}else {
					lookups.add( ((Element)listOfValueNodeList.getFirst()).getAttribute("ListOfValueID") );
					fieldAndLookup.put( ((Element)n).getAttribute("ID") , ((Element)listOfValueNodeList.getFirst()).getAttribute("ListOfValueID"));
				}
			}
		}

		System.out.println("Cargando lookup values al revés para atributos identificados...");
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.util.Map<String, java.util.Map<String, String>> lookupContent = new java.util.TreeMap<>();
		java.util.Map<String, String> content = null;
		for(String lookup : lookups) {
			content = new java.util.TreeMap<>();
			if(readFiles) {
				try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\cache\\" + (lookup.contains("/") ? lookup.replaceAll("/", "________") : lookup))))){
					String line = null;
					String[] pieces = null;
					while((line = br.readLine()) != null) {
						pieces = workshop.parseLine(line);
						content.put(pieces[0], pieces[1]);
					}
					if("FIBER_PARTLOV".equals(lookup)) {
						System.out.println("***********" + content);
					}
					lookupContent.put(lookup, content);
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			}else {
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\cache\\" + ( lookup.contains("/") ? lookup.replaceAll("/", "________") : lookup ))))){
					do {
						try {
							rawResponse = workshop.makeRequest("GET", "/list/LookupValue/bySearch?query=" + encode("LookupValue.IsActive equals true") + "&lookup=" + encode(lookup) + "&fields=" + encode("LookupValue.Code,LookupValueLang.Name(es)") + "&pageSize=5000&startIndex=" + currentIndex, null);
							response = new org.json.JSONObject(rawResponse);
							totalSize = response.getInt("totalSize");
							rows = response.getJSONArray("rows");
							for(int i=0; i<rows.length(); i++) {
								currentIndex++;
								values = rows.getJSONObject(i).getJSONArray("values");
								content.put(values.getString(1), values.getString(0));
								pw.println(workshop.serializeLine(values.getString(1)) + workshop.getSeparator() + workshop.serializeLine( values.getString(0) ));
							}
							System.out.println(lookup + " (" + currentIndex + "/" + totalSize + ")");
						} catch (org.json.JSONException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
							System.out.println(rawResponse);
							e.printStackTrace();
							totalSize = 0;
						}
					}while(currentIndex < totalSize);
					currentIndex = 0;
					lookupContent.put(lookup, content);
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			}
		}

		String productId = null;

		java.util.LinkedList<Node> productNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Products").getFirst() ).get("Product");
		java.util.LinkedList<Node> childProductNodeList = null;

		for(Node n : productNodeList) {
			el = (Element) n;

			String stepType = null;
			stepType = el.getAttribute("UserTypeID");

			productId = el.getAttribute("ID");
			processProduct(null, stepType, el, lookupContent, fieldAndLookup, assetMap);

			childProductNodeList = xmm.listImmediateChildElements(n).get("Product");
			if(childProductNodeList != null) {
				for(Node n_ : childProductNodeList) {
					el = (Element)n_;
					processProduct(productId, stepType, el, lookupContent, fieldAndLookup, assetMap);
				}
			} else if("SalesItem".equals(stepType)) {
				processProduct(productId, stepType, el, lookupContent, fieldAndLookup, assetMap);
			}
		}
		System.out.println("****** Printing notfound ******");
		notFound.forEach(System.out::println);
	}

//	private static String formatValue() {
//
//	}

	private static java.util.Set<String> notFound = new java.util.TreeSet<>();

	private static void processProduct(String parentId, String stepType, Element el, java.util.Map<String, java.util.Map<String, String>> lookupContent, java.util.Map<String, String> fieldAndLookup, java.util.Map<String, Element> assetMap) throws ServiceUnavailableException {
		String template = null;
		String productId = null;
		String name = null;
		String description = null;
		Node descriptionLong = null;
		java.util.LinkedList<Node> clasificaciones = null;
		java.util.LinkedList<String> linksECC = new java.util.LinkedList<>();
		java.util.LinkedList<String> linksS4H = new java.util.LinkedList<>();
		java.util.LinkedList<String> linksWeb = new java.util.LinkedList<>();

		java.util.LinkedList<Node> assetCrossReferenceNodeList = null;
		Element acr = null;
		String acrt = null;
		org.json.JSONArray images = new org.json.JSONArray();

		java.util.LinkedList<Node> valueNodeList = null;

		org.json.JSONArray characteristicRecords = new org.json.JSONArray();
		org.json.JSONArray structureGroupMap = new org.json.JSONArray();
		org.json.JSONArray lang = new org.json.JSONArray();
		org.json.JSONObject body = new org.json.JSONObject();
		body.put("_characteristicRecords", characteristicRecords);
		body.put("structureGroupMap", structureGroupMap);
		body.put("lang", lang);

		org.json.JSONArray higherLevelProduct = null;
		if(parentId != null) {
			higherLevelProduct = new org.json.JSONArray();
			higherLevelProduct.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("referencedIdentifier", parentId)));
			body.put("higherLevelProduct", higherLevelProduct);

			assetCrossReferenceNodeList = xmm.listImmediateChildElements(el).get("AssetCrossReference");
			if(assetCrossReferenceNodeList != null) {
				for(Node n_ : assetCrossReferenceNodeList) {
					acr = (Element) n_;
					acrt = acr.getAttribute("Type");
					Element assetEl = assetMap.get(acr.getAttribute("AssetID"));
					if(assetEl != null) {
						org.json.JSONObject img = new org.json.JSONObject();
						img.put("PhotoAssetType", "PrimaryProductImage".equals(acrt) ? "ProductImage" : "ProductImage".equals(acrt) ? "ProductImageDetail" : "None" );
						img.put("PhotoAssetName", xmm.byName( assetEl, "Name").getTextContent() );
						java.util.LinkedList<Node> ns = xmm.listImmediateChildElements( assetEl ).get("Values");
						if(ns != null) {
							Node valuesNode = ns.getFirst();
							if(valuesNode != null) {
								img.put("PhotoAssetURL", xmm.byAttributeValue( valuesNode, "AttributeID", "ImageURL").getTextContent() );
							}
						}else {
//							System.out.println("No se pudo ubicar a: " + xmm.byName(assetEl, "Name").getTextContent() + ", " + assetEl.getAttribute("ID"));
						}
						images.put(img);
					}else {
						System.out.println("Asset... from void or another dimension: " + acr.getAttribute("AssetID") + ", " + acr.getAttribute("Type"));
					}
				}
				appendMediaCharacteristic(images, characteristicRecords);
				while(images.length() > 0) {
					images.remove(0);
				}
			}
		}
		template = el.getAttribute("ParentID");
		productId = el.getAttribute("ID");
			name = xmm.byName(el, "Name").getTextContent();
//			System.out.println(stepType + "-->" + productId);
		if(name != null && !"".equals(name.trim())) {
			appendLangElement("descriptionShort", name, lang);
		}
		if(! ("SalesItem".equals(stepType) && parentId != null)) {
			if(template != null && !"".equals(template.trim())) {
				appendStructureGroupMap(template, "PrimaryProductTaxonomy", structureGroupMap);
			}
			clasificaciones = xmm.listImmediateChildElements(el).get("ClassificationReference");
			if(clasificaciones != null) {
				for(Node n_ : clasificaciones) {
					if("GALink_S4H".equals( ((Element)n_).getAttribute("Type") )) {
						linksS4H.addLast(((Element)n_).getAttribute("ClassificationID"));
					}else if("WebsiteLink".equals( ((Element)n_).getAttribute("Type") )) {
						linksWeb.addLast(((Element)n_).getAttribute("ClassificationID"));
					}else if("GALink".equals( ((Element)n_).getAttribute("Type") )) {
						linksECC.addLast(((Element)n_).getAttribute("ClassificationID"));
					}
				}
				appendStructureGroupMap(linksECC, "CommercialECC", structureGroupMap);
				appendStructureGroupMap(linksS4H, "CommercialS4H", structureGroupMap);
				appendStructureGroupMap(linksWeb, "Sitios Web", structureGroupMap);
				linksECC.clear();
				linksS4H.clear();
				linksWeb.clear();
			}

			descriptionLong = xmm.byAttributeValue( xmm.byName(el, "Values") , "AttributeID", "DescriptionLong");
			description = descriptionLong != null ? descriptionLong.getTextContent() : null;
			if(description != null && !"".equals(description.trim())) {
				appendLangElement("descriptionLong", description, lang);
			}

			valueNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(el).get("Values").getFirst() ).get("Value");
			java.util.Set<String> banned = new java.util.TreeSet<>( java.util.Arrays.asList(("ChildrenSizeAtt\r\n"
					+ "CreationDate\r\n"
					+ "CuentaConImagen\r\n"
					+ "DisplayGroupOrder\r\n"
					+ "FirstDateApprove\r\n"
					+ "GarantiaDelFabricanteVaD\r\n"
					+ "QARejectionMessage\r\n"
					+ "UN\r\n"
					+ "UniversalMainBarCode\r\n"
					+ "AE488\r\n"
					+ "LastDateApprove\r\n"
					+ "ParentSKU\r\n"
					+ "ST\r\n"
					+ "UN\r\n"
					+ "UserBuyer\r\n"
					+ "CategorizeRejectionMessage\r\n"
					+ "EnrichmentRejectionMessage\r\n"
					+ "ST\r\n").split("\\r\\n")) );
			for(Node n_ : valueNodeList) {
				if(!banned.contains(((Element)n_).getAttribute("AttributeID"))) {
					if("ST".equals(((Element)n_).getAttribute("AttributeID"))) {
						System.out.println("\n\n\n\n\n\n\n\n\n\n\t\t\t\t***************************************\n\n\n\n");
					}else {
						if("IdentificaNegocio".equals(((Element)n_).getAttribute("AttributeID"))) {
							appendNonMediaCharacteristic(n_.getTextContent(), "LIVERPOOL".equals(((Element)n_).getAttribute("ID")) ? "LVP" : "SUBURBIA".equals(((Element)n_).getAttribute("ID")) ? "SBB" : "MKP", "Business", lookupContent, fieldAndLookup, characteristicRecords);
						}
						if("CountryOfOrigin".equals(((Element)n_).getAttribute("AttributeID"))){
							appendNonMediaCharacteristic(n_.getTextContent(), null, ((Element)n_).getAttribute("AttributeID"), lookupContent, fieldAndLookup, characteristicRecords);
						}else {
							appendNonMediaCharacteristic(n_.getTextContent(), ((Element)n_).getAttribute("ID"), ((Element)n_).getAttribute("AttributeID"), lookupContent, fieldAndLookup, characteristicRecords);
						}
					}
				}
			}
		}else {
			productId = parentId + "V";
		}

		body.put("identifier", productId);
		if(body.getJSONArray("lang").length() > 0) {
			String rawResponse = null;
			try {
				org.json.JSONObject response = null;
				org.json.JSONObject o = null;
				org.json.JSONArray a = new org.json.JSONArray();
				org.json.JSONArray b = new org.json.JSONArray();
				org.json.JSONObject c = null;
				org.json.JSONArray d = new org.json.JSONArray();
				characteristicRecords = body.getJSONArray("_characteristicRecords");
				System.out.println("Initially: " + characteristicRecords.length());
				do {
					rawResponse = workshop.makeRequest("PUT", "/object/" + (parentId == null ? "Product2G" : "Article") + "/'" + productId + "'@'MASTER'", body.toString());
					response = new org.json.JSONObject(rawResponse);
					if(response.getJSONObject("_protocol").getInt("errorCounter") > 0) {
//						String message = response.getJSONObject("_protocol").getJSONArray("entries").getJSONObject(0).getString("message");
//						java.util.regex.Matcher m = java.util.regex.Pattern.compile("Code value '([A-Za-z_0-9-]+)'").matcher(message);
//						if(m.find()) {
//							notFound.add(m.group(1));
//							System.out.println("Not found: " + m.group(1) + " ||| " + rawResponse + "\n\t" + body );
//						}else {
//							System.out.println("A different message: " + message + " ||| " + rawResponse + "\n\t" + body);
//							o = characteristicRecords.remove(0);
//							System.out.println("Quitamos: " + o);
//						}
						o = (org.json.JSONObject) characteristicRecords.remove(characteristicRecords.length() - 1);
						b = new org.json.JSONArray();
						b.put(o);
						c = new org.json.JSONObject();
						c.put("_characteristicRecords", b);
						rawResponse = workshop.makeRequest("PUT", "/object/" + (parentId == null ? "Product2G" : "Article") + "/'" + productId + "'@'MASTER'", c.toString());
						response = new org.json.JSONObject(rawResponse);
						if(response.getJSONObject("_protocol").getInt("errorCounter") > 0) {
							a.put(o);
						}else {
							// Safe
							d.put(o);
						}
						System.out.println("\tSalió: "
								+ o.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") + ": "
								+ o
								+ "\n\t" + characteristicRecords.length()
								+ "\n\t" + rawResponse
								+ "\n\t" + body
								+ "\n"
							)
						;
					}else {
						System.out.println("Victory: " + productId);
						break;
					}
				}while(characteristicRecords.length() > 0);
				System.out.println("Bad eggs: " + new org.json.JSONObject().put("chorizos", a));
				if(a.length() > 0) {
					System.exit(0);
				}
//				rawResponse = workshop.makeRequest("PUT", "/object/" + (parentId == null ? "Product2G" : "Article") + "/'" + productId + "'@'MASTER'", body.toString());
//				System.out.println("___" + rawResponse + "\n\t" + body);
			} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
//			System.exit(0);
		}else {
			System.out.println("Missing: " + productId + " ||| " + body);
		}
	}

	private static void appendLangElement(String key, String value, org.json.JSONArray lang) {
		lang.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))).put(key, value));
	}

	private static void appendStructureGroupMap(java.util.LinkedList<String> ids, String structure, org.json.JSONArray structureGroupMap) {
		for(String id : ids) {
			appendStructureGroupMap(id, structure, structureGroupMap);
		}
	}

	private static void appendStructureGroupMap(String id, String structure, org.json.JSONArray structureGroupMap) {
		structureGroupMap.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + id + "'@'" + structure + "'"))));
	}

	private static void appendNonMediaCharacteristic(String textContent, String code, String name, java.util.Map<String, java.util.Map<String, String>> characteristicsThatAreLookups, java.util.Map<String, String> fieldAndLookup, org.json.JSONArray characteristicArray) {
		org.json.JSONObject charBody = null;
		java.util.Map<String, String> characteristicLookup = null;
		String lookup = fieldAndLookup.get(name);
		characteristicLookup = lookup == null ? null : characteristicsThatAreLookups.get( lookup );
		if("FIBER_PART1".equals(name)) {
			System.out.println(fieldAndLookup.get(name) + "-----------------" + lookup);
		}
//		System.out.println("---->" + name);
		if(code != null && !"".equals(code)) {
			charBody = new org.json.JSONObject()
					.put("_qualification",
							new JSONObject().put("characteristic",
									new JSONObject().put("_code", name)))
					.put("_recordLang", new org.json.JSONArray().put(
							new JSONObject().put("values", new org.json.JSONArray().put(
									new org.json.JSONObject().put("_code", code)))));
			characteristicArray.put(charBody);
		}else
		if (characteristicLookup == null) {
			charBody = new org.json.JSONObject()
					.put("_qualification",
							new JSONObject().put("characteristic",
									new JSONObject().put("_code", name)))
					.put("_recordLang", new org.json.JSONArray().put(new JSONObject().put("values",
							new org.json.JSONArray().put( "AssignTakeNoTakeVideo".equals(name) ? code : textContent ))));
			characteristicArray.put(charBody);
		} else {
			if("AssignTakeNoTakeVideo".equals(name)) {
				charBody = new org.json.JSONObject()
						.put("_qualification",
								new JSONObject().put("characteristic",
										new JSONObject().put("_code", name)))
						.put("_recordLang", new org.json.JSONArray().put(
								new JSONObject().put("values", new org.json.JSONArray().put(
										new org.json.JSONObject().put("_code", code)))));
				characteristicArray.put(charBody);
			}else {
				String codeValue = code != null && !"".equals(code) ? code : characteristicLookup.get(textContent);
				if (codeValue != null) {
					charBody = new org.json.JSONObject()
							.put("_qualification",
									new JSONObject().put("characteristic",
											new JSONObject().put("_code", name)))
							.put("_recordLang", new org.json.JSONArray().put(
									new JSONObject().put("values", new org.json.JSONArray().put(
											new org.json.JSONObject().put("_code", codeValue)))));
					characteristicArray.put(charBody);
				}else {
					System.out.println("Couldn't add: " + name + ": " + textContent + "____" + characteristicLookup);
				}
			}
		}
	}

	private static void appendMediaCharacteristic(org.json.JSONArray photosArray, org.json.JSONArray characteristicArray) {
		int timesDetailImage = 0;
		int timesIllustration = 0;
		int timesSmosh = 0;
		org.json.JSONArray children = null;
		String recordKey = null;
		org.json.JSONObject photo = null;
		for (int j = 0; j < photosArray.length(); j++) {
			photo = photosArray.getJSONObject(j);
			try {
				if (photo.getString("PhotoAssetType").startsWith("ProductImageDetail")) {
					recordKey = timesDetailImage == 0 ? "0000.0000.RK" : "0000." + ( timesDetailImage < 10 ? "000" + timesDetailImage : timesDetailImage < 100 ? "00" + timesDetailImage : timesDetailImage < 1000 ? "0" + timesDetailImage : timesDetailImage ) + ".RK";
					children = new org.json.JSONArray();
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code",
															"ProductImageDetail_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					if(photo.has("PhotoAssetURL")) {
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageDetail_URL")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					}
					if(photo.has("PhotoAssetStatus")) {
						children.put(
							new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"ProductImageDetail_Status")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new org.json.JSONObject().put("values",
															new org.json.JSONArray()
																	.put(new JSONObject()
																			.put("_qualification",
																					new JSONObject().put(
																							"language",
																							new JSONObject().put(
																									"_code", "zxx")))
																			.put("_label", photo.optString(
																					"PhotoAssetStatus")))))));
					}
					characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new JSONObject().put("_code", "ProductImageDetail")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new JSONObject().put("values", new org.json.JSONArray())))
									.put("_children", children));
					timesDetailImage++;
				} else if (photo.getString("PhotoAssetType").equals("ProductImage")) {
					children = new org.json.JSONArray();
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code", "ProductImage_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					if(photo.has("PhotoAssetURL")) {
						children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", "0000.0000.RK")
											.put("characteristic",
													new org.json.JSONObject().put("_code", "ProductImage_URL")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					}
					if(photo.has("PhotoAssetStatus")) {
						children.put(
							new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", "0000.0000.RK")
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"ProductImage_Status")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new org.json.JSONObject().put("values",
															new org.json.JSONArray()
																	.put(new JSONObject()
																			.put("_qualification",
																					new JSONObject().put(
																							"language",
																							new JSONObject().put(
																									"_code", "zxx")))
																			.put("_label", photo.optString(
																					"PhotoAssetStatus")))))));
					}
					characteristicArray.put(new org.json.JSONObject().put("_qualification",
							new JSONObject()
								.put("recordKey", "0000.0000.RK")
									.put("characteristic", new JSONObject().put("_code", "ProductImage")))
							.put("_recordLang",
									new org.json.JSONArray()
											.put(new JSONObject().put("values", new org.json.JSONArray())))
							.put("_children", children));
				} else if (photo.getString("PhotoAssetType").startsWith("Illustration")) {
					recordKey = timesIllustration == 0 ? "0000.0000.RK" : "0000." + ( timesIllustration < 10 ? "000" + timesIllustration : timesIllustration < 100 ? "00" + timesIllustration : timesIllustration < 1000 ? "0" + timesIllustration : timesIllustration ) + ".RK";
					children = new org.json.JSONArray();
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey",recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code", "Illustration_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					if(photo.has("PhotoAssetURL")) {
						children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code", "Illustration_URL")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					}
					if(photo.has("PhotoAssetStatus")) {
						children.put(
							new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"Illustration_Status")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new org.json.JSONObject().put("values",
															new org.json.JSONArray()
																	.put(new JSONObject()
																			.put("_qualification",
																					new JSONObject().put(
																							"language",
																							new JSONObject().put(
																									"_code", "zxx")))
																			.put("_label", photo.optString(
																					"PhotoAssetStatus")))))));
					}
					characteristicArray.put(new org.json.JSONObject().put("_qualification",
							new JSONObject()
							.put("recordKey", recordKey)
									.put("characteristic", new JSONObject().put("_code", "Illustration")))
							.put("_recordLang",
									new org.json.JSONArray()
											.put(new JSONObject().put("values", new org.json.JSONArray())))
							.put("_children", children));
					timesIllustration++;
				} else if (photo.getString("PhotoAssetType").startsWith("ProductImageSmosh")) {
					recordKey = timesSmosh == 0 ? "0000.0000.RK" : "0000." + ( timesSmosh < 10 ? "000" + timesSmosh : timesSmosh < 100 ? "00" + timesSmosh : timesSmosh < 1000 ? "0" + timesSmosh : timesSmosh ) + ".RK";
					children = new org.json.JSONArray();
					children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code",
															"ProductImageSmosh_Name")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
					if(photo.has("PhotoAssetURL")) {
						children.put(new org.json.JSONObject()
							.put("_qualification",
									new org.json.JSONObject()
									.put("recordKey", recordKey)
											.put("characteristic",
													new org.json.JSONObject().put("_code",
															"ProductImageSmosh_URL")))
							.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
					}
					if(photo.has("PhotoAssetStatus")) {
						children.put(
								new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey",recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImageSmosh_Status")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new org.json.JSONObject().put("values",
																new org.json.JSONArray()
																		.put(new JSONObject()
																				.put("_qualification",
																						new JSONObject().put(
																								"language",
																								new JSONObject().put(
																										"_code", "zxx")))
																				.put("_label", photo.optString(
																						"PhotoAssetStatus")))))));
					}
					characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new JSONObject().put("_code", "ProductImageSmosh")))
									.put("_recordLang",
											new org.json.JSONArray()
													.put(new JSONObject().put("values", new org.json.JSONArray())))
									.put("_children", children));
					timesSmosh++;
				}
			} catch (org.json.JSONException | NullPointerException e) {
				e.printStackTrace();
			}
		}
	}

	private static String encode(String value) throws UnsupportedEncodingException {
		return java.net.URLEncoder.encode(value, "UTF-8");
	}
}
