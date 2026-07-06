package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class RealExportProductsSTEP {

	private static final String encoded = 
	    	PropertiesManager.get("p360.contingency.basic_token_auth");
	private static final String fileSystemPrefixLvp = 
			java.nio.file.Paths.get("..","stage","ToSTEP").toString();
//			"/P360shared/IDMC/stage/ToMultioferta/";
//			"C:\\opt\\LVP\\tmp\\";
	private static final String baseUrlDEV = 
			PropertiesManager.get("p360.contingency.base_url");
//			"http://172.18.237.162:1512/rest/V2.0";
//			 "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded, "Accept-Language: es");

	private static final RESTWorkshop rw = new RESTWorkshop();

	private org.json.JSONObject getMeTheCompa(String compa) throws ServiceUnavailableException{
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/object/Product2G/'" + compa + "'@'MASTER'?entityFilter=Product2GLang,Product2GStructureGroupMap,Product2GCharacteristicValue&includeIds=true&includeLabels=true", null);
			response = new org.json.JSONObject(rawResponse);
		} catch (IOException e) {
			logE(e);
		}
		return response;
	}
	
	private static final String USAGE = "Usage: RealExportProducts2Mirakl <File with IDs or SKUs> -t ID|SKU [-s]\n-t indicates which type of content is in the file: SKU or Proposal IDs\n-s if present, indicates to send the data to destination, default is not send the data.";

	public static void main(String[] args) throws ServiceUnavailableException {
		if(args.length < 1) {
			System.out.println(USAGE);
			return;
		}
		String source = args[0];
		int type = 0;
		boolean send = false;
		if(args.length > 1) {
			java.util.LinkedList<String> extra = new java.util.LinkedList<>(java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 1, args.length)));
			if(!extra.contains("-t") || extra.getLast().equals("-t")) {
				System.out.println(USAGE);
				return;
			}
			String arg = null;
			for(int i=0; i<extra.size(); i++) {
				arg = extra.get(i);
				if("-s".equals(arg)) {
					send = true;
				}else if("-t".equals(extra.get(i)) && i < extra.size() - 1) {
					type = "ID".equals(extra.get(i+1)) ? 0 : "SKU".equals(extra.get(i+1)) ? 1 : -1;
					if(type == -1) {
						System.out.println(USAGE);
						return;
					}
					i++;
				}else {
				}
			}
		}
		String[] data = sourceContent(source);
		RealExportProductsSTEP o = new RealExportProductsSTEP();
		System.out.println("Me arranco");
		if(type == 0) {
			for(String d : data) {
				o.doIt(new String[] {d}, send, baseUrlDEV);
			}
		}else if(type == 1) {
			String[] pedazos = data;
			for (String element : pedazos) {
				o.doIt(new String[] { o.getIdFromSKU( element ) }, send, baseUrlDEV);
			}
		}
	}
	
	private static String[] sourceContent(String source) {
		java.util.LinkedList<String> lines = new java.util.LinkedList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(source)))){
			String line = null;
			while((line = br.readLine()) != null) {
				lines.addLast(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return lines.toArray(new String[] {});
	}

	public String getIdFromSKU(String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "characteristic('SKU',-1) equals \"" + sku + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		if(response == null) {
		}else{
			rows = response.getJSONArray("rows");
			if(rows.length() > 0 ) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			}else {
			}
		}
		return null;
	}

	public void processBatch(String[] proposalIds) {
		java.util.ArrayList<String> batch = new java.util.ArrayList<>();
		for(String proposalId : proposalIds) {
			batch.add(proposalId);
			if(batch.size() == 10) {
				//doIt(batch.toArray(new String[]{}));
				batch.clear();
			}
		}
		if(!batch.isEmpty()) {
			//doIt(batch.toArray(new String[]{}));
			batch.clear();
		}
	}
	
	public String doIt(String[] proposalIds, boolean send, String baseUrl) throws ServiceUnavailableException {
		rw.setBaseUrl(baseUrl);
		rw.addHeader("Authorization", "Basic " + encoded);
		return doIt(proposalIds, send);
	}
	
	private String getSAPObjectType(org.json.JSONArray characteristics) {
		String productType = null;
		for(int i = 0; i<characteristics.length(); i++) {
			if("SAPObjectType".equals(characteristics.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
				productType = characteristics.getJSONObject(i).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
			}
		}
		return productType;
	}
	
	private String grabDescLong(org.json.JSONArray lang) {
		for(int i=0; i<lang.length(); i++) {
			if(10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key") ) {
				return lang.getJSONObject(i).has("descriptionLong") ? lang.getJSONObject(i).getString("descriptionLong") : null;
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public String doIt(String[] proposalIds, boolean send) throws ServiceUnavailableException {
		log("Running using baseUrlDEV: " + baseUrlDEV);
		log("Running using fileSystemPrefixLVP: " + fileSystemPrefixLvp);
		String proposalId = null;
        try {
        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	Document doc = builder.newDocument();
        	Document docMKT = builder.newDocument();
        	Element spim = doc.createElement("STEP-ProductInformation");
        	spim.setAttribute("ExportTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new java.util.Date() ));
        	spim.setAttribute("ExportContext", "Context2");
        	spim.setAttribute("ContextID", "Context2");
        	spim.setAttribute("WorkspaceID", "Main");
        	spim.setAttribute("UseContextLocale", "false");
        	Element attributes = doc.createElement("AttributeList");
        	Element assets = doc.createElement("Assets");
        	java.util.Map<String, Element> assetMap = new java.util.TreeMap<>();
        	java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap = new java.util.TreeMap<>();
        	spim.appendChild(assets);
        	Element products = doc.createElement("Products");
        	doc.appendChild(spim);
        	spim.appendChild(products);

        	Element spimMKT = docMKT.createElement("STEP-ProductInformation");
        	spimMKT.setAttribute("ExportContext", "Context2");
        	spimMKT.setAttribute("ContextID", "Context2");
        	spimMKT.setAttribute("WorkspaceID", "Main");
        	spimMKT.setAttribute("UseContextLocale", "false");
        	Element attributesMKT = docMKT.createElement("AttributeList");
        	Element assetsMKT = docMKT.createElement("Assets");
        	java.util.Map<String, Element> assetMapMKT = new java.util.TreeMap<>();
        	java.util.Map<String, java.util.LinkedList<String>> assetReferencesMapMKT = new java.util.TreeMap<>();
//        	spimMKT.appendChild(assetsMKT);
        	Element productsMKT = docMKT.createElement("Products");
        	docMKT.appendChild(spimMKT);
        	spimMKT.appendChild(productsMKT);
        	
        	java.util.LinkedList<String> productosLiverpool = new java.util.LinkedList<>();
        	java.util.LinkedList<String> productosMarketplace = new java.util.LinkedList<>();
        	final java.util.Map<String, java.util.Map<String, org.json.JSONObject>> templateMetadataSet =new java.util.TreeMap<>();
        	final java.util.Map<String, java.util.Set<String>> templateSets = new java.util.TreeMap<>();
        	final java.util.Map<String, org.json.JSONObject> globalProperties = new java.util.TreeMap<>();
        	final java.util.Set<String> globalSet = new java.util.TreeSet<>();
        	boolean procede;
			addGlobalData(globalProperties, globalSet, baseUrlDEV);
			log("Checking over " + proposalIds.length);
			for(int index = 0; index<proposalIds.length; index++) {
				procede = false;
				proposalId = proposalIds[index];
				// talla normalizada hacia ATG debe de salir como TC-NormalizedSize
//				final String[] productsToTestWith = new String[] {proposalId};
				org.json.JSONObject rp = getMeTheCompa(proposalId);
				String template = !rp.getJSONObject("_data").has("structureGroupMap") ? null : getPrimaryProductTaxonomyTemplate(rp.getJSONObject("_data").getJSONArray("structureGroupMap")); // rp.getJSONObject("_data").getJSONArray("structureGroupMap").getJSONObject(0).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
				String itemId = rp.getJSONObject("_entityItem").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
				org.json.JSONArray characteristicArray = rp.getJSONObject("_data").getJSONArray("_characteristicRecords");
				String business = getMeTheBusiness(characteristicArray);
				String baseSAPObjectType = getSAPObjectType(characteristicArray);
				String descLong = rp.getJSONObject("_data").has("lang") ? grabDescLong( rp.getJSONObject("_data").getJSONArray("lang") ) : null;
				
				if("LVP".equals(business)) {
					productosLiverpool.addLast(proposalId);
					log("Is LVP");
				} else if ("MKP".equals(business)) {
					productosMarketplace.addLast(proposalId);
					log("Is MKT");
				}else {
					log("Others");
				}
				String productType = null;
				String piName = null;
				String piUrl = null;
				String piKey = null;
				java.util.LinkedList<String[]> details = new java.util.LinkedList<>();
				java.util.LinkedList<String[]> smoshes = new java.util.LinkedList<>();
				java.util.LinkedList<String[]> illustrations = new java.util.LinkedList<>();
				String raw = null;
				String firstVariant = null;
				org.json.JSONObject imageObject = null;
				String tamanoUnico = null;
				String tallaNormalizada = null;
				String codigoColor = null;
				String color = null;
				org.json.JSONArray rows = null;
				org.json.JSONArray upperRows = null;
				try {
					productType = 
							"00".equals(baseSAPObjectType) ? "MKP".equals(business) ? "SalesItemFamilyMkt" : "SalesItem" : 
								"01".equals(baseSAPObjectType) ? "MKP".equals(business) ? "SalesItemFamilyMkt" : "SalesItemFamily" : null;
					if(productType == null) {
						log("Problem identifying productType, got: " + baseSAPObjectType);
						throw new IllegalStateException("Unknown productType: " + baseSAPObjectType);
					}else {
						log("productType: " + productType);
					}
					String charId = null;
					raw = rw.makeRequest("GET", "/list/Article/bySearch"
							+ "?fields="
								+ java.net.URLEncoder.encode(
										  "Article.SupplierAID,"
										+ "ProductReference.ReferencedSupplierAid(\"" + itemId + "\")"
										,"UTF-8")
							+ "&query=" + java.net.URLEncoder.encode("ProductReference.ReferencedSupplierAid(\"" + itemId + "\") equals \""+itemId+"\"", "UTF-8"), null);
					org.json.JSONObject resp = new org.json.JSONObject(raw);
					upperRows = resp.getJSONArray("rows");
//					productType = upperRows.length() == 1 ? "SalesItem" : "SalesItemFamilyMkt";
					for(int a = 0; a<upperRows.length(); a++) {
						try{
							firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
						}catch(org.json.JSONException e) {
						}
						raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue", null);
						resp = new org.json.JSONObject(raw);
						resp = resp.getJSONObject("_data");
						rows = resp.getJSONArray("_characteristicRecords");
						org.json.JSONArray children = null;
						String[] chunk = null;
						for(int b = 0; b < rows.length(); b++) {
							imageObject = rows.getJSONObject(b);
							charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
							if("SalesItem".equals(productType) && "ProductImage".equals(charId)) {
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
							}else if("SalesItem".equals(productType) && "ProductImageDetail".equals(charId)) {
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
							} else if("SalesItem".equals(productType) && "TamanoUnicoSTD".equals(charId)){
								tallaNormalizada = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}else if("SalesItem".equals(productType) && "TamanoUnico".equals(charId)) {
								tamanoUnico = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
							}else if("SalesItem".equals(productType) && "ColoursLiverpoolAtt".equals(charId)) {
								color = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
								codigoColor = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
							}else if("SAPObjectType".equals(charId)) {
								
							}
						}
					}
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					logE(e);
				}
				String rawResponse = null;
				org.json.JSONObject response = null;
				rows = null;
				int currentIndex = 0;
				int totalSize = 0;
				org.json.JSONArray values = null;
				currentIndex = 0;
				String prevC = null;
				currentIndex = 0;
				java.util.Set<String> atributosGeneralesQueSi = null;
				java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = null;
				String currC = null;
				prevC = null;
				String itemGroup = null;
				String itemGroupLabel = null;
				String direccion = null;
				String brandCode = null;
				String genericSKU = null;
				org.json.JSONObject prop = new org.json.JSONObject();
				org.json.JSONArray prevV = null;
				propiedadesCaracteristicas = templateMetadataSet.get(template);
				atributosGeneralesQueSi = templateSets.get(template);
				if(propiedadesCaracteristicas == null) {
					propiedadesCaracteristicas = new java.util.TreeMap<>();
					atributosGeneralesQueSi = new java.util.TreeSet<>();
					templateSets.put(template, atributosGeneralesQueSi);
					atributosGeneralesQueSi.addAll(globalSet);
					templateMetadataSet.put(template, propiedadesCaracteristicas);
					for(java.util.Map.Entry<String, org.json.JSONObject> globalPropertiesEntry : globalProperties.entrySet()) {
						propiedadesCaracteristicas.put(globalPropertiesEntry.getKey(), globalPropertiesEntry.getValue());
					}
					try {
						do {
							rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/StandardizationValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'", "UTF-8")
									+ "&query="
										+ java.net.URLEncoder.encode(
											"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
											+ " and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\""
//											+ " and (StandardizationValue.Property->LookupValue.Code equals \"IsMultiselect\""
//											+ " or StandardizationValue.Property->LookupValue.Code equals \"RelevantForATG\""
//											+ " or StandardizationValue.Property->LookupValue.Code equals \"IsMandatory\""
//											+ " or StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSectionSequence\")"
											+ " and StandardizationValue.StructureGroup->LookupValue.Code equals \"" + template + "\""
										, "UTF-8")
									+ "&fields="
										+ java.net.URLEncoder.encode(
											  "StandardizationValue.StructureGroup->LookupValue.Code"
											+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
											+ ",StandardizationValue.Property->LookupValue.Code"
											+ ",StandardizationValue.PropertyValue"
											+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
											+ ",StandardizationValue.Characteristic->CharacteristicLang.Description(es)"
											+ ",StandardizationValue.Characteristic->Characteristic.DataType"
											+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
											+ ",StandardizationValue.Characteristic->Characteristic.IsMultiValue"
											+ ",StandardizationValue.Characteristic->Characteristic.Purposes->LookupValue.Code"
											+ ",StandardizationValue.Characteristic->Characteristic.Order"
										, "UTF-8") 
									+ "&orderBy=1-ASC"
									+ "&pageSize=1000"
									+ "&startIndex=" + currentIndex, null);
							response = new org.json.JSONObject(rawResponse);
							totalSize = response.getInt("totalSize");
							rows = response.getJSONArray("rows");
							for(int i=0; i<rows.length(); i++) {
								values = rows.getJSONObject(i).getJSONArray("values");
								currC = values.getString(1);
								if(prevC != null && !prevC.equals(currC)) {
									prop.put("name", prevV.getString(4));
									prop.put("description", prevV.getString(5));
									prop.put("dataType", prevV.getString(6));
									prop.put("lookup", prevV.getString(7));
									prop.put("isMultiValue", prevV.getString(8));
									prop.put("purposes", prevV.getJSONArray(9));
									prop.put("order", prevV.getString(10));
									propiedadesCaracteristicas.put(prevC, prop);
									if(prop.getJSONArray("purposes").length() == 1 && prop.getJSONArray("purposes").getString(0).equals(""))
										prop.getJSONArray("purposes").remove(0);
									if(prop.has("RelevantForATG") && "Y".equals(prop.getString("RelevantForATG")))
										atributosGeneralesQueSi.add(prevC);
									prop = new org.json.JSONObject();
								}
								prop.put(values.getString(2), values.getString(3));
								prevC = currC;
								prevV = values;
								currentIndex++;
							}
						}while(currentIndex < totalSize);
						currentIndex = 0;
					} catch (org.json.JSONException | IOException e) {
						logE(e);
					}
					if(prop.length() > 0) {
						prop.put("name", prevV.getString(4));
						prop.put("description", prevV.getString(5));
						prop.put("dataType", prevV.getString(6));
						prop.put("lookup", prevV.getString(7));
						prop.put("isMultiValue", prevV.getString(8));
						propiedadesCaracteristicas.put(prevC, prop);
						if(prop.has("RelevantForATG") && "Y".equals(prop.getString("RelevantForATG")))
							atributosGeneralesQueSi.add(prevC);
						prop = new org.json.JSONObject();
					}
				}

	        	Element product = null;

	        	if("MKP".equals(business)) {
	        		product = docMKT.createElement("Product");
	        		product.setAttribute("UserTypeID", productType );
	        		product.setAttribute("ParentID", template);
	        		product.setAttribute("Changed", "true");
//	        		productsMKT.appendChild(product);
	        	} else {
	        		product = doc.createElement("Product");
	        		product.setAttribute("UserTypeID", productType );
	        		product.setAttribute("ParentID", template);
	        		product.setAttribute("Changed", "true");
//	        		products.appendChild(product);
	        	}

	        	/*********************
	        	 * El atributo en las entidades Changed="true", tiene el efecto
	        	 * de que broker ignorda todo lo que no tenga Changed="true".
	        	****************************************************************/
	        	Element name = ("MKP".equals(business) ? docMKT : doc).createElement("Name");
	        	name.setAttribute("Changed", "true");
	        	product.appendChild(name);

	        	Element keyValueSKU = null;
	        	Element keyValueEAN = null;

	        	Element attributeValues = null;

	        	String charId = null;
	        	org.json.JSONObject characteristic = null;

	        	boolean behvo = false;

	        	java.util.ArrayList<String> unosQueQuiero = new java.util.ArrayList<>(YEA);
	        	java.util.Map<String, org.json.JSONObject> heredables = new java.util.TreeMap<>();

	        	attributeValues = ("MKP".equals(business) ? docMKT : doc).createElement("Values");
	        	product.appendChild(attributeValues);
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
	    					"MKP".equals(business) ? assetMapMKT : assetMap,
	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
	    					product,
	    					"MKP".equals(business) ? assetsMKT : assets,
	    					"MKP".equals(business) ? docMKT : doc,
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
		    					"MKP".equals(business) ? assetMapMKT : assetMap,
    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
    	    					product,
    	    					"MKP".equals(business) ? assetsMKT : assets,
    	    					"MKP".equals(business) ? docMKT : doc,
		    					firstVariant
		    					);
					}
				}

	        	if("SalesItem".equals(productType) && tallaNormalizada != null && !"".equals(tallaNormalizada)) {
					appendPlainElementValue(
							tallaNormalizada,
							null,
							"TC-NormalizedSize",
							attributeValues,
							"MKP".equals(business) ? attributesMKT : attributes,
							"MKP".equals(business) ? docMKT : doc,
							propiedadesCaracteristicas);
				}
	        	if("SalesItem".equals(productType) && color != null && !"".equals(color)) {
	            	appendPlainElementValue(
	    					color,
	    					codigoColor,
	    					"ColoursLiverpoolAtt",
	    					attributeValues,
	    					"MKP".equals(business) ? attributesMKT : attributes,
							"MKP".equals(business) ? docMKT : doc,
	    					propiedadesCaracteristicas);
	        	}
	        	log("********************* PT: " + productType);
				String rr = null;
				try {
					rr = rc.getRequest("GET", baseUrlDEV + "/object/StructureGroup/'" + template + "'@'PrimaryProductTaxonomy'?entityFilter=StructureGroupAttribute", null);
					org.json.JSONObject tratando = new org.json.JSONObject(rr);
					org.json.JSONArray attributeRow = tratando.getJSONObject("_data").getJSONArray("attribute");
					for(int a = 0; a<attributeRow.length(); a++) {
//						if("DisplayGroupOrder".equals(attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"))) {
							String val = attributeRow.getJSONObject(a).getJSONArray("value").getJSONObject(0).getString("value");
							appendPlainElementValue(
	    							val,
	    							null,
	    							attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"),
	    							attributeValues,
	    							"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
	    							propiedadesCaracteristicas);
//						}
					}
				} catch (IOException e) {
					logE(e);
				}

				if(descLong != null) {
					appendPlainElementValue(
							descLong,
							null,
							"DescriptionLong",
							attributeValues,
							"MKP".equals(business) ? attributesMKT : attributes,
							"MKP".equals(business) ? docMKT : doc,
							propiedadesCaracteristicas);
				}
				
	        	for(int i = 0; i<characteristicArray.length(); i++) {
	        		characteristic = characteristicArray.getJSONObject(i);
	        		charId = characteristic.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
	        		log("Tutul té - " + characteristic);
	        		if("Business".equals(charId)) {
//        				business = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
        				if("SBB".equals(business)) {
        					log("Returning since business is SBB.");
        					return null;
        				}
        				if("MKP".equals(business)) {
        		        	product.setAttribute("UserTypeID", productType = "SalesItem".equals(productType) ? "SalesItem" : "SalesItemFamilyMkt" );
        					appendPlainElementValue(
    								"true",
    								"1",
    								"isMarketPlace",
    								attributeValues,
    								"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
    								propiedadesCaracteristicas);
        				}else {
        					product.setAttribute("UserTypeID", productType = "SalesItem".equals(productType) ? "SalesItem" : "SalesItemFamily" );
        				}
        			}else if("Direction".equals(charId)) {
        				direccion = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
        			}
	        		if("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
	        			keyValueEAN = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
	        			keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
	        			String eanval = treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
	        			keyValueEAN.setTextContent(eanval);
	        			product.appendChild(keyValueEAN);
	        		}
	        		if("SKU".equals(charId)) {
	        			keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
	        			keyValueSKU.setAttribute("KeyID","SKUID");
	        			String skuval = treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
	        			genericSKU = skuval;
	        			keyValueSKU.setTextContent( skuval );
	        			product.appendChild(keyValueSKU);
	        			appendPlainElementValue(
								skuval,
								null,
								"SKU",
								attributeValues,
								"MKP".equals(business) ? attributesMKT : attributes,
								"MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
	        		}else
	        		if("ProductVideo".equals(charId)) {
	        			if(characteristic.has("_children")) {
		        			org.json.JSONArray children = characteristic.getJSONArray("_children");
							String[] chunk = new String[3];
							chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
							for(int c = 0; c<children.length(); c++) {
								charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								if("ProductVideo_Name".equals(charId)) {
									chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("ProductVideo_URL".equals(charId)) {
									chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}
							}
		        			appendPlainElementValue(
		        					chunk[1],
									null,
									"Video",
									attributeValues,
									"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas);
	        			}
	        		}else if("OwnersManual".equals(charId)) {
	        			if(characteristic.has("_children")) {
	        			org.json.JSONArray children = characteristic.getJSONArray("_children");
							String[] chunk = new String[3];
							chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
							for(int c = 0; c<children.length(); c++) {
								charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								if("OwnersManual_Name".equals(charId)) {
									chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("OwnersManual_URL".equals(charId)) {
									chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}
							}
		        			appendMediaAsset(
		        					chunk[0],
		        					chunk[1],
		        					"OwnersManual", // String assetType,
		        					chunk[2],
		        					"Manual de Propietario", // String assetValueTextContent,
		        					"OwnersManualURL", // String assetValueAttributeId,
		        					"OwnersManual", // String assetUserTypeId,
		        					"OwnersManual", // String assetKeyPrefix,
		        					itemId,
		        					characteristic,
		        					"OwnersManual", // String baseAssetTypeName,
			    					"MKP".equals(business) ? assetMapMKT : assetMap,
	    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
	    	    					product,
	    	    					"MKP".equals(business) ? assetsMKT : assets,
	    	    					"MKP".equals(business) ? docMKT : doc,
	    							proposalId
		        					);
	        			}
	        		}else if("NOM".equals(charId)) {
	        			if(characteristic.has("_children")) {
		        			org.json.JSONArray children = characteristic.getJSONArray("_children");
							String[] chunk = new String[3];
							chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
							for(int c = 0; c<children.length(); c++) {
								charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								if("NOM_Name".equals(charId)) {
									chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("NOM_URL".equals(charId)) {
									chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}
							}
		        			appendMediaAsset(
		        					chunk[0],
		        					chunk[1],
		        					"NOM", // String assetType,
		        					chunk[2],
		        					"NOM", // String assetValueTextContent,
		        					"ImageURL", // String assetValueAttributeId,
		        					"NOM", // String assetUserTypeId,
		        					"NOM", // String assetKeyPrefix,
		        					itemId,
		        					characteristic,
		        					"NOM", // String baseAssetTypeName,
			    					"MKP".equals(business) ? assetMapMKT : assetMap,
	    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
	    	    					product,
	    	    					"MKP".equals(business) ? assetsMKT : assets,
	    	    					"MKP".equals(business) ? docMKT : doc,
	    							proposalId
		        					);
	        			}
	        		}else {
	        			if("ItemGroupS4H".equals( charId ) || "ItemGroup".equals( charId )) {
	        				if(isBannedForMarketplace(characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"), "ItemGroups", "MATKLLOV")) {
        						log("Returning since the item group was in the no send to mkt list.");
	        					return null;
	        				}
	        				if(!behvo) {
		        				String elese = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
		        				try {
		        					rawResponse = rw.makeRequest("GET", "/list/StandardizationValue/bySearch"
		        							+ "?dictionaryProxy=" + encode("'" + ("ItemGroup".equals( charId ) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H") + "'")
		        							+ "&query=" + encode("StandardizationValue.Value equals \"" + elese + "\"")
		        							+ "&fields=" + encode("StandardizationValue.AlternativeValue")
		        							+ ""
		        							, null);
		        					response = new org.json.JSONObject(rawResponse);
		        					rows = response.getJSONArray("rows");
		        					String laetiqueta = queryDictionary(elese, ("ItemGroup".equals( charId ) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H"));
		        					if(rows.length() > 0) {
		        						rawResponse = rw.makeRequest("GET", "/list/LookupValue/bySearch"
		            							+ "?lookup=" + encode("SAP_BEHVOLOV")
		            							+ "&query=" + encode("LookupValueLang.Name(es) equals \"" + laetiqueta + "\"")
		            							+ "&fields=" + encode("LookupValue.Code")
		            							+ ""
		            							, null);
		            					response = new org.json.JSONObject(rawResponse);
		            					rows = response.getJSONArray("rows");
		            					String elcode = rows.getJSONObject(0).getJSONArray("values").getString(0);
		            					if(rows.length() > 0) {
		            						appendPlainElementValue(
		            								laetiqueta,
		            								elcode,
		            								"SAP_BEHVO",
		            								attributeValues,
		            								"MKP".equals(business) ? attributesMKT : attributes,
                    								"MKP".equals(business) ? docMKT : doc,
		            								propiedadesCaracteristicas);
		            						behvo = true;
		            					}
		        					}
		        				}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {

		        				}
		        				appendPlainElementValue(
		        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
		        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
		        						"ItemGroup2",
		        						attributeValues,
		        						"MKP".equals(business) ? attributesMKT : attributes,
        								"MKP".equals(business) ? docMKT : doc,
		        						propiedadesCaracteristicas);
	        				}
	        				appendPlainElementValue(
	        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
	        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
	        						charId,
	        						attributeValues,
	        						"MKP".equals(business) ? attributesMKT : attributes,
    								"MKP".equals(business) ? docMKT : doc,
	        						propiedadesCaracteristicas);
	        				itemGroup = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
	        				itemGroupLabel = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
	        			}else
	        				if("BrandName".equals(charId) || "BRAND_ID_S4H".equals(charId)) {
	        					if(isBannedForMarketplace(characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"), "Brands", "ZCOMALOV")) {
	        						log("Returning since brand was in no send to mkt list.");
	        						return null;
	        					}
	        				appendPlainElementValue(
	        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
	        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
	        							charId,
	        							attributeValues,
	        							"MKP".equals(business) ? attributesMKT : attributes,
    									"MKP".equals(business) ? docMKT : doc,
	        							propiedadesCaracteristicas);
	        				appendPlainElementValue(
	        						characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
	        						null,
	        						"BrandNameATG",
	        						attributeValues,
	        						"MKP".equals(business) ? attributesMKT : attributes,
    								"MKP".equals(business) ? docMKT : doc,
	        						propiedadesCaracteristicas);
	        				appendPlainElementValue(
	    							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
	    							null,
	    							"BrandIDATG",
	    							attributeValues,
	    							"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
	    							propiedadesCaracteristicas);
	        				brandCode = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
	        			}else if("supplierShopId".equals(charId)){
							heredables.put(charId, characteristic);
	        				if("LVP".equals(business)) {
	        					appendPlainElementValue(
		    							"9999",
		    							null,
		    							"supplierShopId",
		    							attributeValues,
		    							"MKP".equals(business) ? attributesMKT : attributes,
    									"MKP".equals(business) ? docMKT : doc,
		    							propiedadesCaracteristicas);
	        				}else {
	        					appendPlainElementValue(
		    							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0),
		    							null,
		    							"supplierShopId",
		    							attributeValues,
		    							"MKP".equals(business) ? attributesMKT : attributes,
    									"MKP".equals(business) ? docMKT : doc,
		    							propiedadesCaracteristicas);
	        				}
	        			}else {
	        				if("ItemGroup2".equals(charId)) {
								continue;
							}else /* if(atributosGeneralesQueSi.contains(charId)) */ {
								if(unosQueQuiero.contains(charId)) {
									heredables.put(charId, characteristic);
								}
								if("ProductName".equals(charId)) {
									name.setTextContent(characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
								}
								if("LOOKUP".equals(characteristic.getString("_datatype"))){
									appendPlainElementValue(
											characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
											characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
											charId,
											attributeValues,
											"MKP".equals(business) ? attributesMKT : attributes,
											"MKP".equals(business) ? docMKT : doc,
											propiedadesCaracteristicas);
								}else if(!"NONE".equals(characteristic.getString("_datatype"))) {
									java.util.LinkedList<String> vals = new java.util.LinkedList<>();
									for(int m=0; m<characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
										vals.addLast( String.valueOf( parseDateForSpecificDateFields( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m), charId) ));
									}
									appendPlainElementValue(
											String.join(",", vals),
											null,
											charId,
											attributeValues,
											"MKP".equals(business) ? attributesMKT : attributes,
											"MKP".equals(business) ? docMKT : doc,
											propiedadesCaracteristicas);
								}
								
							}
	        			}
	        		}
	        	}
	        	log("Finished characteristics...");
	        	if("SalesItem".equals(productType) && tamanoUnico != null && !"".equals(tamanoUnico)) {
	        		talla(tamanoUnico, business, itemGroup, template, direccion, brandCode, attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas );
	        		appendPlainElementValue(
							tamanoUnico,
							null,
							"TamanoUnico",
							attributeValues,
							"MKP".equals(business) ? attributesMKT : attributes,
							"MKP".equals(business) ? docMKT : doc,
							propiedadesCaracteristicas
							);
				}
	        	
	        	if( ( "SalesItemFamilyMkt".equals(productType) || "SalesItemFamily".equals(productType) ) ) {
	        		log("Entered to family processor");
	        		org.json.JSONObject resp = null;
	        		Element subProduct = null;
	        		Element subAttributeValues = null;
	        		Element varName = null;
	        		String childSKU = null;
	        		String childMainBarCode = null;
	        		String childSAPObjectType = null;
	        		String childSAPObjectTypeLabel = null;
	        		String miraklVariantGroupId = null;
	        		java.util.LinkedList<java.util.LinkedList< String[] >> losdetalles = new java.util.LinkedList<>();
	        		java.util.LinkedList<java.util.LinkedList< String[] >> losesmoshes = new java.util.LinkedList<>();
	        		java.util.LinkedList<java.util.LinkedList< String[] >> lasilustraciones = new java.util.LinkedList<>();
	        		for(int a = 0; a<upperRows.length(); a++) {
	        			procede = false;
	        			tallaNormalizada = null;
	        			tamanoUnico = null;
	        			color = null;
	        			codigoColor = null;
	        			childSKU = null;
	        			childMainBarCode = null;
	        			childSAPObjectType = null;
	        			childSAPObjectTypeLabel = null;
	        			miraklVariantGroupId = null;
	    				try{
	    					firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
	    				}catch(org.json.JSONException e) {
	    				}

	        			subProduct = ("MKP".equals(business) ? docMKT : doc).createElement("Product");
//	        			subProduct.setAttribute("ID", firstVariant);
	        			subProduct.setAttribute("UserTypeID", "MKP".equals(business) ? "SalesItem" : "SalesItemVariant");
	        			subProduct.setAttribute("Changed", "true");
	                	subAttributeValues = ("MKP".equals(business) ? docMKT : doc).createElement("Values");
	                	varName = ("MKP".equals(business) ? docMKT : doc).createElement("Name");
	                	subProduct.appendChild(varName);
	                	subProduct.appendChild(subAttributeValues);
	                	if("MKP".equals(business)) {
	                		appendPlainElementValue(
    								"true",
    								"1",
    								"isMarketPlace",
    								subAttributeValues,
    								"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
    								propiedadesCaracteristicas);
        				}
	                	details = new java.util.LinkedList<>();
	                	smoshes = new java.util.LinkedList<>();
	                	illustrations = new java.util.LinkedList<>();
	    				try {
							raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue", null);
							resp = new org.json.JSONObject(raw);
							resp = resp.getJSONObject("_data");
							if(!resp.has("_characteristicRecords"))
								continue;
							rows = resp.getJSONArray("_characteristicRecords");
							org.json.JSONArray children = null;
							String[] chunk = null;
							for(int b = 0; b < rows.length(); b++) {
								imageObject = rows.getJSONObject(b);
								charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								if("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
				        			keyValueEAN = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
				        			keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
				        			keyValueEAN.setTextContent(imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
				        			subProduct.appendChild(keyValueEAN);
				        		}
				        		if("SKU".equals(charId)) {
				        			keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
				        			keyValueSKU.setAttribute("KeyID","SKUID");
				        			String skuval = treatment( imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
				        			keyValueSKU.setTextContent( skuval );
				        			subProduct.appendChild(keyValueSKU);
				        		}
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
									if(imageObject.has("_children")) {
										children = imageObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
										for(int c = 0; c<children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("ProductImageDetail_Name".equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}else if("ProductImageDetail_URL".equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										details.addLast(chunk);
										losdetalles.addLast(details);
									}
								}else if("ProductImageSmosh".equals(charId)) {
									if(imageObject.has("_children")) {
										children = imageObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
										for(int c = 0; c<children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("ProductImageSmosh_Name".equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}else if("ProductImageSmosh_URL".equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										smoshes.addLast(chunk);
										losesmoshes.addLast(smoshes);
									}
								}else if("Illustration".equals(charId)) {
									if(imageObject.has("_children")) {
										children = imageObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
										for(int c = 0; c<children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("Illustration_Name".equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}else if("Illustration_URL".equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										illustrations.addLast(chunk);
										lasilustraciones.addLast(illustrations);
									}
								} else if("TamanoUnicoSTD".equals(charId)){
									tallaNormalizada = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("TamanoUnico".equals(charId)) {
									tamanoUnico = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
								}else if("ColoursLiverpoolAtt".equals(charId)) {
									color = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
									codigoColor = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}else if("SKU".equals(charId)) {
									childSKU = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("MainBarCode".equals(charId)) {
									childMainBarCode = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("MainBarCodeS4H".equals(charId)) {
									childMainBarCode = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("SAPObjectType".equals(charId)) {
									childSAPObjectTypeLabel = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
									childSAPObjectType = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}else if("mirakl-variant-group-id".equals(charId)) {
									miraklVariantGroupId = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("ProcedeNoProcede".equals(charId)) {
									procede = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getBoolean(0);
								}else {
									if("LOOKUP".equals(imageObject.getString("_datatype"))){
										appendPlainElementValue(
												imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
												imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
												charId,
												subAttributeValues,
												"MKP".equals(business) ? attributesMKT : attributes,
		    									"MKP".equals(business) ? docMKT : doc,
												propiedadesCaracteristicas
												);
									}else if(!"NONE".equals(imageObject.getString("_datatype"))) {
										java.util.LinkedList<String> vals = new java.util.LinkedList<>();
										for(int m=0; m<imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
											vals.addLast( String.valueOf( parseDateForSpecificDateFields( imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m), charId) ));
										}
										appendPlainElementValue(
												String.join(",", vals),
												null,
												charId,
												subAttributeValues,
												"MKP".equals(business) ? attributesMKT : attributes,
		    									"MKP".equals(business) ? docMKT : doc,
												propiedadesCaracteristicas);
									}
								}
							}
							if(!procede) {
								continue;
							}else {
			                	product.appendChild(subProduct);
							}
							if(miraklVariantGroupId == null && "MKP".equals(business)) {
								appendPlainElementValue(
	        							genericSKU,
	        							null,
	        							"mirakl-variant-group-id",
	        							subAttributeValues,
	        							"MKP".equals(business) ? attributesMKT : attributes,
    									"MKP".equals(business) ? docMKT : doc,
	        							propiedadesCaracteristicas);
							}
							varName.setTextContent( name.getTextContent() + ", " + tamanoUnico + ", " + color );

							for(java.util.Map.Entry<String, org.json.JSONObject> entr : heredables.entrySet()) {
								charId = entr.getKey();
								characteristic = entr.getValue();
								if("MainBarCode".equals(charId)) {
									continue;
								}
								if("LOOKUP".equals(characteristic.getString("_datatype"))){
		        					appendPlainElementValue(
		        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
		        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
		        							charId,
		        							subAttributeValues,
		        							"MKP".equals(business) ? attributesMKT : attributes,
        									"MKP".equals(business) ? docMKT : doc,
		        							propiedadesCaracteristicas);
		        				}else if(!"NONE".equals(characteristic.getString("_datatype"))) {
		        					java.util.LinkedList<String> vals = new java.util.LinkedList<>();
		        					for(int m=0; m<characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
		        						vals.addLast( String.valueOf( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m) ));
		        					}
		        					appendPlainElementValue(
		        							String.join(",", vals),
		        							null,
		        							charId,
		        							subAttributeValues,
		        							"MKP".equals(business) ? attributesMKT : attributes,
        									"MKP".equals(business) ? docMKT : doc,
		        							propiedadesCaracteristicas);
		        				}
							}


							if(descLong != null) {
								appendPlainElementValue(
										descLong,
										null,
										"DescriptionLong",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
							}
							appendPlainElementValue(
									genericSKU,
									null,
									"ParentSKU",
									subAttributeValues,
									"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas);
							if(childSKU != null && !"".equals(childSKU)) {
								appendPlainElementValue(
										childSKU,
										null,
										"SKU",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
							}
							if(childMainBarCode != null && !"".equals(childMainBarCode)) {
								appendPlainElementValue(
										childMainBarCode,
										null,
										"SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
							}
							if(childSAPObjectType != null && !"".equals(childSAPObjectType)) {
								appendPlainElementValue(
										childSAPObjectTypeLabel,
										childSAPObjectType,
										"SAPObjectType",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
							}
							if(tamanoUnico != null && !"".equals(tamanoUnico)) {
								appendPlainElementValue(
										tamanoUnico,
										null,
										"TamanoUnico",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
								talla(tamanoUnico, business, itemGroup, template, direccion, brandCode, subAttributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas );
							}
							if(tallaNormalizada != null && !"".equals(tallaNormalizada)) {
								appendPlainElementValue(
										tallaNormalizada,
										null,
										"TC-NormalizedSize",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
							}
							if(color != null && !"".equals(color)) {
								appendPlainElementValue(
										color,
										codigoColor,
										"ColoursLiverpoolAtt",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
							}
							appendPlainElementValue(
	        						itemGroupLabel,
	        						itemGroup,
	        						"ItemGroup2",
	        						subAttributeValues,
	        						"MKP".equals(business) ? attributesMKT : attributes,
    								"MKP".equals(business) ? docMKT : doc,
	        						propiedadesCaracteristicas);
							if(piName != null && piUrl != null && piKey != null ) {
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
				    					"MKP".equals(business) ? assetMapMKT : assetMap,
		    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
    	    							subProduct,
		    	    					"MKP".equals(business) ? assetsMKT : assets,
		    	    					"MKP".equals(business) ? docMKT : doc,
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
					    					"MKP".equals(business) ? assetMapMKT : assetMap,
			    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
			    	    					subProduct,
			    	    					"MKP".equals(business) ? assetsMKT : assets,
			    	    					"MKP".equals(business) ? docMKT : doc,
					    					firstVariant
					    					);
								}
							}
							if(smoshes != null && !smoshes.isEmpty() ) {
								for(String[] dt : smoshes) {
									appendMediaAsset(
											dt[0],
											dt[1],
					    					"ProductImageSmosh", // String assetType,
					    					dt[2],
					    					"Imagen Smosh Producto", // String assetValueTextContent,
					    					"ImageURL", // String assetValueAttributeId,
					    					"ProductImageSmosh", // String assetUserTypeId,
					    					"SmoshImg", // String assetKeyPrefix,
					    					itemId,
					    					characteristic,
					    					"ProductImageSmosh", // String baseAssetTypeName,
					    					"MKP".equals(business) ? assetMapMKT : assetMap,
			    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
			    	    					subProduct,
			    	    					"MKP".equals(business) ? assetsMKT : assets,
			    	    					"MKP".equals(business) ? docMKT : doc,
					    					firstVariant
					    					);
								}
							}
							if(illustrations != null && !illustrations.isEmpty() ) {
								for(String[] dt : illustrations) {
									appendMediaAsset(
											dt[0],
											dt[1],
					    					"Illustration", // String assetType,
					    					dt[2],
					    					"Imagen Isométrica del Producto", // String assetValueTextContent,
					    					"ImageURL", // String assetValueAttributeId,
					    					"Illustration", // String assetUserTypeId,
					    					"Illustration", // String assetKeyPrefix,
					    					itemId,
					    					characteristic,
					    					"Illustration", // String baseAssetTypeName,
					    					"MKP".equals(business) ? assetMapMKT : assetMap,
			    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
			    	    					subProduct,
			    	    					"MKP".equals(business) ? assetsMKT : assets,
			    	    					"MKP".equals(business) ? docMKT : doc,
					    					firstVariant
					    					);
								}
							}


						} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
							logE(e);
						}
	    			}

					if(piName != null && piUrl != null && piKey != null ) {
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
		    					"MKP".equals(business) ? assetMapMKT : assetMap,
    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
    	    					product,
    	    					"MKP".equals(business) ? assetsMKT : assets,
    	    					"MKP".equals(business) ? docMKT : doc,
		    					proposalId
		    					);
					}
					for(java.util.LinkedList<String[]> eldetalle : losdetalles) {
						for(String[] dt : eldetalle) {
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
			    					"MKP".equals(business) ? assetMapMKT : assetMap,
	    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
	    	    					product,
	    	    					"MKP".equals(business) ? assetsMKT : assets,
	    	    					"MKP".equals(business) ? docMKT : doc,
			    					dt[3]
			    					);
						}
					}
					for(java.util.LinkedList<String[]> elesmoshes : losesmoshes) {
						for(String[] dt : elesmoshes) {
							appendMediaAsset(
									dt[0],
									dt[1],
			    					"ProductImageSmosh", // String assetType,
			    					dt[2],
			    					"Imagen Smosh Producto", // String assetValueTextContent,
			    					"ImageURL", // String assetValueAttributeId,
			    					"ProductImageSmosh", // String assetUserTypeId,
			    					"SmoshImg", // String assetKeyPrefix,
			    					itemId,
			    					characteristic,
			    					"ProductImageSmosh", // String baseAssetTypeName,
			    					"MKP".equals(business) ? assetMapMKT : assetMap,
	    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
	    	    					product,
	    	    					"MKP".equals(business) ? assetsMKT : assets,
	    	    					"MKP".equals(business) ? docMKT : doc,
			    					firstVariant
			    					);
						}
					}
					for(java.util.LinkedList<String[]> lailustracion : lasilustraciones) {
						for(String[] dt : lailustracion) {
							appendMediaAsset(
									dt[0],
									dt[1],
			    					"Illustration", // String assetType,
			    					dt[2],
			    					"Imagen Isométrica del Producto", // String assetValueTextContent,
			    					"ImageURL", // String assetValueAttributeId,
			    					"Illustration", // String assetUserTypeId,
			    					"Illustration", // String assetKeyPrefix,
			    					itemId,
			    					characteristic,
			    					"Illustration", // String baseAssetTypeName,
			    					"MKP".equals(business) ? assetMapMKT : assetMap,
	    	    					"MKP".equals(business) ? assetReferencesMapMKT : assetReferencesMap,
	    	    					product,
	    	    					"MKP".equals(business) ? assetsMKT : assets,
	    	    					"MKP".equals(business) ? docMKT : doc,
			    					firstVariant
			    					);
						}
					}
	        	}
//	        	log("'bout to finish: " + rw.getXmm().listImmediateChildElements(product).get("Product"));
	        	log("'bout to finish 2: " + productType);
	        	if (rw.getXmm().listImmediateChildElements(product).get("Product") != null || "SalesItem".equals(productType)) {
		        	if("MKP".equals(business)) {
		        		productsMKT.appendChild(product);
		        	}else {
		        		products.appendChild(product);
		        	}
	        	}
			}
			log("Finishing and now about to send messages...");
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			
//			if(!productosLiverpool.isEmpty()) {
				log("LVP products...");
//				java.io.StringWriter writer = new java.io.StringWriter();
//				transformer.transform(new DOMSource(doc), new StreamResult(writer));
//				String xmlOutput = writer.getBuffer().toString()
//						.replace("&lt;CRLF&gt;", "&#13;&#10;")
//					    .replace("<CRLF>", "&#13;&#10;");
//				System.out.println(xmlOutput);
//				String fn = java.nio.file.Paths.get( fileSystemPrefixLvp, "pépele" + System.currentTimeMillis() + ".xml" ).toString();
//		        try {
//					java.nio.file.Files.writeString(java.nio.file.Paths.get(fn), xmlOutput, java.nio.charset.StandardCharsets.UTF_8);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}

//				if(send) {
//					RestClient rc = new RestClient("Content-Type: application/xml", "Accept: application/xml");
//					try {
//						log("[" + new java.text.SimpleDateFormat().format(new java.util.Date()) + "] (Mkt) Request containing: " + java.util.Arrays.asList(proposalIds) + " sent. Resp: " + (serviceResponse = rc.getRequest("POST", urlDeMktStockout, xmlOutput) ) );
//						return fn + "<::>" + serviceResponse;
//					} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
//						logE(e);
//					}
//				}
//			}else {
//				log("No LVP products...");
//			}
			log("Came to last part ,,,");
			if(!productosMarketplace.isEmpty()) {
//				log("Mkt products...");
				java.io.StringWriter writer = new java.io.StringWriter();
				transformer.transform(new DOMSource(docMKT), new StreamResult(writer));
				String xmlOutput = writer.getBuffer().toString()
						.replace("&lt;CRLF&gt;", "&#13;&#10;")
					    .replace("<CRLF>", "&#13;&#10;");
				String fn = java.nio.file.Paths.get( fileSystemPrefixLvp, "pépele" + System.currentTimeMillis() + ".xml" ).toString();
				System.out.println("Wrote: " + fn);
		        try {
					java.nio.file.Files.writeString(java.nio.file.Paths.get(fn), xmlOutput, java.nio.charset.StandardCharsets.UTF_8);
				} catch (IOException e) {
					e.printStackTrace();
				}
//				java.io.StringWriter writer = new java.io.StringWriter();
//				transformer.transform(new DOMSource(docMKT), new StreamResult(writer));
//				String xmlOutput = writer.getBuffer().toString();
//				String fn = java.nio.file.Paths.get( fileSystemPrefix, "pépele" + System.currentTimeMillis() + ".xml" ).toString();
//				transformer.transform(new DOMSource(docMKT), new StreamResult(new java.io.File(fn)));
//				if(send) {
//					RestClient rc = new RestClient("Content-Type: application/xml", "Accept: application/xml");
//					try {
//						log("[" + new java.text.SimpleDateFormat().format(new java.util.Date()) + "] (Mkt) Request containing: " + java.util.Arrays.asList(proposalIds) + " sent. Resp: " + (serviceResponse = rc.getRequest("POST", urlDeMkt, xmlOutput) ) );
//						return fn + "<::>" + serviceResponse;
//					} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
//						logE(e);
//					}
//				}
			}else {
				log("No MKT products...");
			}
		} catch (TransformerException e) {
			logE(e);
		} catch (ParserConfigurationException e) {
			logE(e);
		}
        return null;
	}
	
	private Object parseDateForSpecificDateFields(Object value, String charId) {
		if(value == null)
			return null;
		String formato = mapaDeAtributosFechas.get(charId);
		if(formato != null) {
			try {
				return new java.text.SimpleDateFormat( formato ).format( new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse( ((String)value).replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.") ) );
			}catch(java.text.ParseException e) {
				
			}
		}
		return value;
	}
	
//	private boolean isBannedBrand(String brand) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("lookup", "'BannedBrandsForMarketplacePublication'");
//		qp.put("query", "LookupValue.IsActive = true and LookupValue.Code equals \"" + brand + "\"");
//		org.json.JSONObject response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//		if(response != null) {
//			return response.getJSONArray("rows").length() > 0;
//		}else {
//			log("Problem querying banned brand for market place: " + brand);
//		}
//		return false;
//	}
//	
//	public static void main(String[] args) {
//		log( new RealExportProducts2Mirakl().isBannedForMarketplace("10110", "ItemGroups", "MATKLLOV") );
//	}
	
	private boolean isBannedForMarketplace(String value, String rule, String ref) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'BannedElementsForMarketplacePublication'");
		qp.put("fields", "LookupValue.Code,LookupValueReference.LookupValues(" + ref + ")");
		qp.put("query", "LookupValue.IsActive = true and LookupValue.Code equals \"" + rule + "\" and LookupValueReference.LookupValues(" + ref + ")->LookupValue.Code equals \"" + value + "\" ");
		org.json.JSONObject response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		if(response != null) {
			log("From querying if banned element: " + response.toString() + ", rule: " + rule + ", value: " + value + ", ref: " + ref);
			return response.getJSONArray("rows").length() > 0;
		}else {
			log(rw.getRawResponse());
			log("Problem querying banned criteria for market place, value: " + value + ", rule: " + rule + ", ref: " + ref);
		}
		return false;
	}
	
	private String getMeTheBusiness(org.json.JSONArray characteristicRecords) {
		org.json.JSONObject characteristic = null;
		for(int i=0; i<characteristicRecords.length(); i++) {
			characteristic = characteristicRecords.getJSONObject(i);
			if("Business".equals(characteristic.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code")))
				return characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
		}
		return null;
	}
	
	private void addGlobalData(java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas, java.util.Set<String> losQueSi, String baseUrl) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		rw.addHeader("Authorization", RealExportProductsSTEP.rw.getRc().getHeader().get("Authorization"));
		rw.putParameter("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
		rw.putParameter("fields", 
				   "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
				+ ",StandardizationValue.Characteristic->CharacteristicLang.Description(es)"
				+ ",StandardizationValue.Characteristic->Characteristic.DataType"
				+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
				+ ",StandardizationValue.Characteristic->Characteristic.IsMultiValue"
				+ ",StandardizationValue.Characteristic->Characteristic.Purposes->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Order"
			);
		rw.putParameter("query", 
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
			);
		rw.putParameter("orderBy", "0-ASC");
		rw.putParameter("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		org.json.JSONObject detail = new org.json.JSONObject();
		org.json.JSONArray prevValues = null;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(prevValues != null && !prevValues.getString(0).equals(values.getString(0))) {
						detail.put("name", prevValues.getString(3));
						detail.put("description", prevValues.getString(4));
						detail.put("dataType", prevValues.getString(5));
						detail.put("lookup", prevValues.getString(6));
						detail.put("isMultiValue", prevValues.getString(7));
						detail.put("purposes", prevValues.getJSONArray(8));
						detail.put("order", prevValues.getString(9));
						propiedadesCaracteristicas.put(prevValues.getString(0), detail);
						if(detail.getJSONArray("purposes").length() == 1 && detail.getJSONArray("purposes").getString(0).equals(""))
							detail.getJSONArray("purposes").remove(0);
						if(detail.has("RelevantForATG") && "Y".equals(detail.getString("RelevantForATG")))
							losQueSi.add(prevValues.getString(0));
						detail = new org.json.JSONObject();
					}
					detail.put(values.getString(1), values.getString(2));
					prevValues = values;
				}
			}else {
				log("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(detail.length() > 0) {
			detail.put("name", prevValues.getString(3));
			detail.put("description", prevValues.getString(4));
			detail.put("dataType", prevValues.getString(5));
			detail.put("lookup", prevValues.getString(6));
			detail.put("isMultiValue", prevValues.getString(7));
			detail.put("purposes", prevValues.getJSONArray(8));
			detail.put("order", prevValues.getString(9));
			propiedadesCaracteristicas.put(prevValues.getString(0), detail);
			if(detail.getJSONArray("purposes").length() == 1 && detail.getJSONArray("purposes").getString(0).equals(""))
				detail.getJSONArray("purposes").remove(0);
			if(detail.has("RelevantForATG") && "Y".equals(detail.getString("RelevantForATG")))
				losQueSi.add(prevValues.getString(0));
			detail = null;
		}
	}

	public void talla(String latalla, String business, String itemGroup, String template, String direccion, String brand, Element attributeValues, Element attributes, Document doc, java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas) throws ServiceUnavailableException {
		String elcampoLatalla = null;
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		if(elcampoLatalla == null) {
			log("Bad combination to determine laTalla, itemGroup: " + itemGroup + ", business: " + business);
			return;
		}
		log("Looking for: " + itemGroup + " and " + business + " in laTalla, got: " + elcampoLatalla + ", latalla es un diccionario: " + latalla);
		String stdDictionary = mapaDeDirecciones.get(elcampoLatalla);
		String lanuevatalla = queryDictionary(latalla, stdDictionary);
		log("RA: Latalla: " + latalla + ", eldiccionarioSTD: " + stdDictionary + ", lanuevatalla: " + lanuevatalla);
		lanuevatalla = lanuevatalla == null ? latalla : lanuevatalla;
		String tallaWeb = mapaDeDireccionesAtributoTallaWeb.get(elcampoLatalla);
		log("Latalla: " + tallaWeb + ", elcampolatalla: " + elcampoLatalla + ", querying dictionary for latalla: " + direccion);
		String reqTransf = queryDictionary(direccion, "ValidDirection");
		if("S".equals(reqTransf)) {
			String lallave = itemGroup + brand + latalla;
			log("Querying a dictionary as lallave: " + lallave);
			String clothingSize = queryDictionary(lallave, "TallasInfantilesVsMarca");
			if(clothingSize != null) {
				lanuevatalla = clothingSize;
			}
		}
		if(tallaWeb != null) {
			appendPlainElementValue(
					lanuevatalla,
					null,
					 tallaWeb,
					attributeValues,
					attributes,
					doc,
					propiedadesCaracteristicas);
		}
		String sequence = getTheVariantSequence(latalla, template);
		if(sequence != null && !"".equals(sequence))
			appendPlainElementValue(
					sequence,
					null,
					"variantOrder",
					attributeValues,
					attributes,
					doc,
					propiedadesCaracteristicas);
	}


	private String getAtributoSapLatalla(String itemGroup, String business) throws ServiceUnavailableException {
		String value = null;
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(baseUrlDEV);
		rw.addHeader("Authorization", RealExportProductsSTEP.rw.getRc().getHeader().get("Authorization"));
		String dp = ("SBB".equals(business) ? "TallaUnicavsTallaS4H" : "TallaUnicavsTallaERP");
		rw.putParameter("dictionaryProxy", "'" + dp + "'");
		rw.putParameter("fields", "StandardizationValue.AlternativeValue");
		rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dp + "\" and StandardizationValue.Value equals \"" + itemGroup + "\"");

		org.json.JSONObject response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
		if(response != null) {
			org.json.JSONArray rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				value = rows.getJSONObject(0).getJSONArray("values").getString(0);
			}
		}else {
			log("###$$ ERROR: " + rw.getRawResponse());
		}
		if(value == null || "".equals(value) && !"SBB".equals(business)) {
			dp = ("ItemGroupSAPSizeAttribute");
			rw.putParameter("dictionaryProxy", "'" + dp + "'");
			rw.putParameter("fields", "StandardizationValue.AlternativeValue");
			rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dp + "\" and StandardizationValue.Value equals \"" + itemGroup + "\"");

			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
			if(response != null) {
				org.json.JSONArray rows = response.getJSONArray("rows");
				if(rows.length() > 0) {
					value = rows.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}else {
				log("###$$ ERROR: " + rw.getRawResponse());
			}
		}
		return value;
	}
	
	@SuppressWarnings("deprecation")
	private String queryDictionary(String key, String dictionary) throws ServiceUnavailableException {
		String rawResponse = null;
		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
		try {
			String url = null;
			rawResponse = rw.makeRequest("GET", url = "/object/StandardizationValue/" + encode("'" + key + "'@'" + dictionary + "'")
					+ ""
					, null);
			log("Raw response: " + rawResponse);
			response = new org.json.JSONObject(rawResponse);
//			rows = response.getJSONArray("rows");
//			log("Querying: " + key + " in: " + dictionary + ", got: " + response);
//			log("URL: " + url);
//			if(rows.length() > 0) {
//				return rows.getJSONObject(0).getJSONArray("values").getString(0);
//			}
			if(response.has("_data") && response.getJSONObject("_data").has("alternativeValue")) {
				return response.getJSONObject("_data").getString("alternativeValue");
			}
		}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException e){
			logE(e);
		}catch(org.json.JSONException e) {
			logE(e);
			log("ERR: " + rawResponse);
		}
		return null;
	}

	private String encode(String val) {
		try {
			return java.net.URLEncoder.encode(val, "UTF-8");
		}catch(java.io.IOException e) {

		}
		return null;
	}

	private String getPrimaryProductTaxonomyTemplate(org.json.JSONArray classifications){
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
					log("Could not find a match in: " + externalId);
					return null;
				}
			}
		}
		return null;
	}

	private String treatment(String val) {
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

	private void appendMediaAsset(
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
			Document doc,
			String seedId
		) {
//		Element assetCrossReference = doc.createElement("AssetCrossReference");
		org.json.JSONObject cc = null;
		String assetId = assetKeyPrefix + "-" + seedId + (assetKey != null ? assetKey : characteristic.getJSONObject("_qualification").getString("recordKey"));
		if(name != null) {
//			assetCrossReference.setAttribute("AssetID", assetId);
//			assetCrossReference.setAttribute("Type", assetType);
//			assetCrossReference.setAttribute("Changed", "true");
//			product.appendChild(assetCrossReference);
		}else {
			cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_Name");
			if(cc != null) {
//				assetCrossReference.setAttribute("AssetID", assetId);
//				assetCrossReference.setAttribute("Type", assetType);
//				assetCrossReference.setAttribute("Changed", "true");
//				product.appendChild(assetCrossReference);
			}
		}
		Element asset = assetMap.get(assetId);
		Element assetName = null;
		Element assetValues = null;
		Element assetValue = null;
		java.util.LinkedList<String> referencesList = null;
		if(asset == null) {
			asset = doc.createElement("Asset");
			assetMap.put(assetId, asset);
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
				if("ProductImage".equals(assetUserTypeId)) {
					assetValue.setTextContent("largeImage=" + url);
				}else {
					assetValue.setTextContent(url);
				}
				assetValues.appendChild(assetValue);
			}else {
				cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_URL");
				if(cc != null) {
					if("ProductImage".equals(assetUserTypeId)) {
						url = cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						assetValue.setTextContent("largeImage=" + url );
					}else {
						assetValue.setTextContent(url);
					}
					assetValues.appendChild(assetValue);
				}else {
					return;
				}
			}
			if("ProductImage".equals(assetUserTypeId)) {
				assetValue = doc.createElement("Value");
				assetValues.appendChild(assetValue);
				assetValue.setAttribute("AttributeID", "ImageKey");
				assetValue.setTextContent("lg-Imagen Producto");
			}
			if(name != null) {
				assetName = doc.createElement("Name");
				assetName.setTextContent(name);
				asset.appendChild(assetName);
			}
			referencesList = new java.util.LinkedList<>();
			referencesList.addLast(itemId);
			assetReferencesMap.put(assetId, referencesList);
			assets.appendChild(asset);
		}else {
			referencesList = assetReferencesMap.get(assetId);
			if(referencesList == null) {
				referencesList = new java.util.LinkedList<>();
				assetReferencesMap.put(assetId, referencesList);
			}
			if(!referencesList.contains(assetId)) {
				referencesList.addLast(assetId);
			}
		}
	}
	
	private void appendPlainElementValue(
			String textValue, 
			String code, 
			String attributeId, 
			Element attributeValues, 
			Element attributes, 
			Document doc, 
			java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas 
//			java.util.Map<String, String> atgGroups
			) throws ServiceUnavailableException {
		org.json.JSONObject prop = null;
		String stdDict = null;
		String nv = null;
		Element attributeValue = doc.createElement("Value");
		attributeValues.appendChild(attributeValue);
		attributeValue.setAttribute("AttributeID", attributeId);
		if(code != null) {
			attributeValue.setAttribute("ID", code);
		}
		if(textValue != null) {
			stdDict = mapaDeDirecciones.get(attributeId);
			if(stdDict != null) {
				nv = queryDictionary(textValue, stdDict);
				if(nv != null) {
					textValue = nv;
				}
			}
		}
		attributeValue.setTextContent(textValue);

		attributeValue.setAttribute("Changed", "true");
		Element metaData = doc.createElement("MetaData");
		Element valueElement = null;
		Element metaDataMultiValue = null;
//		String groupLabel = null;
		java.util.LinkedList<String> grupos = null;
		Element attribute = doc.createElement("Attribute");
		attribute.setAttribute("ID", attributeId);
		prop = propiedadesCaracteristicas.get(attributeId);
		if(prop != null) {
			attribute.setAttribute("MultiValued", prop.has("IsMultiselect") ? "1".equals(prop.getString("IsMultiselect")) ? "true" : "false" : "false");
			attribute.setAttribute("Mandatory", prop.has("IsMandatory") ? "1".equals(prop.getString("IsMandatory")) ? "true" : "false" : "false");
			if(!prop.has("name")) {
				log("No Name found for: " + attributeId);
			}else {
				Element metadataAttribute = doc.createElement("Name");
				metadataAttribute.setTextContent(prop.getString("name"));
				attribute.appendChild(metadataAttribute);
				if(prop.has("order")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "DisplaySequence");
					metadataAttribute.setTextContent(prop.getString("order"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("name")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "DisplayName");
					metadataAttribute.setTextContent(prop.getString("name"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("description")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "AttributeHelpText");
					metadataAttribute.setTextContent(prop.getString("description"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("isConfigurable")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "isConfigurable");
					metadataAttribute.setTextContent(prop.getString("isConfigurable"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("purposes")) {
					org.json.JSONArray purposes = prop.getJSONArray("purposes");
					grupos = new java.util.LinkedList<>();
					for(int i=0; i<purposes.length(); i++) {
						if("CreationModificationAtributesIIEP".equals(purposes.getString(i))) {
						}else if("isFaceted".equals(purposes.getString(i))) {
							valueElement = doc.createElement("Value");
							valueElement.setTextContent("true");
							valueElement.setAttribute("ID", "Y");
							valueElement.setAttribute("AttributeID", purposes.getString(i));
							metaData.appendChild(valueElement);
						}else if("isConfigurable".equals(purposes.getString(i))) {
							valueElement = doc.createElement("Value");
							valueElement.setTextContent("true");
							valueElement.setAttribute("ID", "Y");
							valueElement.setAttribute("AttributeID", purposes.getString(i));
							metaData.appendChild(valueElement);
						}else {
							if(purposes.getString(i).endsWith("GPO")) {
								grupos.addLast(purposes.getString(i));
							}
						}
					}
					if(!grupos.isEmpty()) {
						metaDataMultiValue = doc.createElement("MultiValue");
//						for(String grupo : grupos) {
//							groupLabel = atgGroups.get(grupo);
//							if(groupLabel != null) {
//								valueElement = doc.createElement("Value");
//								valueElement.setTextContent(groupLabel);
//								valueElement.setAttribute("ID", grupo);
//								metaDataMultiValue.appendChild(valueElement);
//							}
//						}
						if(metaDataMultiValue.getChildNodes().getLength() > 0) {
							metaDataMultiValue.setAttribute("AttributeID", "isAttInGroupAtt");
							metaData.appendChild(metaDataMultiValue);
						}
					}
				}
			}
		}else {
			// PANIC
//			log("PANIC: No property was found for characteristic: " + attributeId);
		}
		attribute.setAttribute("FullTextIndexed", "false");
		attribute.setAttribute("ProductMode", "Normal");
		attribute.setAttribute("ExternallyMaintained", "true");
		attribute.setAttribute("Derived", "false");
		attribute.setAttribute("HierarchicalFiltering", "false");
		attribute.setAttribute("ClassificationHierarchicalFiltering", "false");
		attribute.setAttribute("Referenced", "true");
		attributes.appendChild(attribute);
		attribute.appendChild(metaData);
		if(prop != null && prop.has("VendorCenterSectionSequence")) {
			Element attributeMetaDataValue = doc.createElement("Value");
			attributeMetaDataValue.setAttribute("AttributeID", "DisplaySequence");
			attributeMetaDataValue.setTextContent(prop.getString("VendorCenterSectionSequence"));
			metaData.appendChild(attributeMetaDataValue);
		}
		Element attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "AtributoCalculadoObjetos");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("Ultimo Usuario: N/A |  Fecha: N/A");
		metaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttVaDySAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("0");
		metaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttSAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("N/A");
		metaData.appendChild(attributeMetaDataValue);
	}

//	private void appendPlainElementValue(
//			String textValue, 
//			String code, 
//			String attributeId, 
//			Element attributeValues, 
//			Element attributes, 
//			Document doc, 
//			java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas
//	) {
//		org.json.JSONObject prop = null;
//		String stdDict = null;
//		String nv = null;
//		Element attributeValue = doc.createElement("Value");
//		attributeValues.appendChild(attributeValue);
//		attributeValue.setAttribute("AttributeID", attributeId);
//		if(code != null) {
//			attributeValue.setAttribute("ID", code);
//		}
//		if(textValue != null) {
//			stdDict = mapaDeDirecciones.get(attributeId);
//			if(stdDict != null) {
//				nv = queryDictionary(textValue, stdDict);
//				if(nv != null) {
//					textValue = nv;
//				}
//			}
//		}
//		attributeValue.setTextContent(textValue);
//
//		attributeValue.setAttribute("Changed", "true");
//
//		Element attribute = doc.createElement("Attribute");
//		attribute.setAttribute("ID", attributeId);
//		prop = propiedadesCaracteristicas.get(attributeId);
//		if(prop != null) {
//			attribute.setAttribute("MultiValued", prop.has("IsMultiselect") ? "1".equals(prop.getString("IsMultiselect")) ? "true" : "false" : "false");
//			attribute.setAttribute("Mandatory", prop.has("IsMandatory") ? "1".equals(prop.getString("IsMandatory")) ? "true" : "false" : "false");
//			if(!prop.has("name")) {
//			}else {
//				Element attributeName = doc.createElement("Name");
//				attributeName.setTextContent(prop.getString("name"));
//				attribute.appendChild(attributeName);
//			}
//		}else {
//			// PANIC
//			log("PANIC: No property was found for characteristic: " + attributeId);
//		}
//		attribute.setAttribute("FullTextIndexed", "false");
//		attribute.setAttribute("ProductMode", "Normal");
//		attribute.setAttribute("ExternallyMaintained", "true");
//		attribute.setAttribute("Derived", "false");
//		attribute.setAttribute("HierarchicalFiltering", "false");
//		attribute.setAttribute("ClassificationHierarchicalFiltering", "false");
//		attribute.setAttribute("Referenced", "true");
//		attributes.appendChild(attribute);
//		Element attributeMetaData = doc.createElement("MetaData");
//		attribute.appendChild(attributeMetaData);
//		if(prop != null && prop.has("VendorCenterSectionSequence")) {
//			Element attributeMetaDataValue = doc.createElement("Value");
//			attributeMetaDataValue.setAttribute("AttributeID", "DisplaySequence");
//			attributeMetaDataValue.setTextContent(prop.getString("VendorCenterSectionSequence"));
//			attributeMetaData.appendChild(attributeMetaDataValue);
//		}
//		Element attributeMetaDataValue = doc.createElement("Value");
//		attributeMetaDataValue.setAttribute("AttributeID", "AtributoCalculadoObjetos");
//		attributeMetaDataValue.setAttribute("Derived", "true");
//		attributeMetaDataValue.setTextContent("Ultimo Usuario: N/A |  Fecha: N/A");
//		attributeMetaData.appendChild(attributeMetaDataValue);
//		attributeMetaDataValue = doc.createElement("Value");
//		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttVaDySAP");
//		attributeMetaDataValue.setAttribute("Derived", "true");
//		attributeMetaDataValue.setTextContent("0");
//		attributeMetaData.appendChild(attributeMetaDataValue);
//		attributeMetaDataValue = doc.createElement("Value");
//		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttSAP");
//		attributeMetaDataValue.setAttribute("Derived", "true");
//		attributeMetaDataValue.setTextContent("N/A");
//		attributeMetaData.appendChild(attributeMetaDataValue);
//	}

	private org.json.JSONObject getMeAssetChildValue(org.json.JSONObject hola, String childCharacteristic) {
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

	private String getTheVariantSequence(String latalla, String template) {
		String rawMap = queryVariantOrder(template);
		if(rawMap != null) {
			String[] pieces = rawMap.split(",");
			String[] smallPieces = null;
			for(int i=0; i<pieces.length; i++) {
				smallPieces = pieces[i].split("\\=");
				if(smallPieces[0].equals(latalla)) {
					return smallPieces[1];
				}
			}
		}
		return null;
	}
	
	private String queryVariantOrder(String key) {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'VariantOrder'");
		qp.put("query", "StandardizationValue.Value wildcard \"%-" + key.replaceAll("^.+-", "") + "\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"VariantOrder\"");
		qp.put("fields", "StandardizationValue.PropertyValue");
		try {
			response  = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				if(rows.length() > 0) {
					return rows.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}else {
				log("<::>" + rw.getRawResponse());
			}
		}catch(org.json.JSONException e) {
			log("ERR: " + rw.getRawResponse());
//			System.exit(0);
		}
		return null;
	}

	private void log(String message){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","real_export_products_stp.log").toString(), true)))){
		  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "]  " + message);
		}catch(java.io.IOException e){}
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get( "..","logs","real_export_products_stp.log").toString(), true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}

	public static java.util.Set<String> YEA; /* = new java.util.TreeSet<>(java.util.Arrays.asList(("SKU\r\n"
			+ "BrandName\r\n"
			+ "BRAND_ID_S4H\r\n"
			+ "EXTWG_S4H\r\n"
			+ "Section\r\n"
			+ "ZBREPQ\r\n"
			+ "ProductName\r\n"
			+ "PesoBruto\r\n"
			+ "MesdeEntregadeMercancIa\r\n"
			+ "ChildrenSizeAtt\r\n"
			+ "ZHOECJ\r\n"
			+ "clothingSize\r\n"
			+ "TypeMainBarCode\r\n"
			+ "IDTallaERP\r\n"
			+ "ZMEACJ\r\n"
			+ "DisplayGroupOrder\r\n"
			+ "TextoAdicional\r\n"
			+ "ZBRGPQ\r\n"
			+ "ProductType\r\n"
			+ "TImportacion\r\n"
			+ "ZNTGCJ\r\n"
			+ "WESCH\r\n"
			+ "AnoEstacion\r\n"
			+ "ProductTypeSAP2\r\n"
			+ "IEPS\r\n"
			+ "VOLUMAtt\r\n"
			+ "ConditionforPublish\r\n"
			+ "BaseUnitOfMeasure\r\n"
			+ "ZHOEPQ\r\n"
			+ "ZLAEPQ\r\n"
			+ "StateSKU\r\n"
			+ "ImpuestoALaVenta\r\n"
			+ "ZLAECJ\r\n"
			+ "Negocio\r\n"
			+ "ZBRGCJ\r\n"
			+ "Temporada\r\n"
			+ "SAP_BEHVO\r\n"
			+ "ZVOLPQ\r\n"
			+ "ItemGroup2\r\n"
			+ "MaterialAtt\r\n"
			+ "ProductTypeSAPTEMP\r\n"
			+ "BrandNameATG\r\n"
			+ "UniversalMainBarCode\r\n"
			+ "BrandIDATG\r\n"
			+ "ZBRECJ\r\n"
			+ "Status\r\n"
			+ "ZVOLCJ\r\n"
			+ "IDLastParent\r\n"
			+ "ZNTGPQ\r\n"
			).split("\\r\\n")));
	*/
	
//	public static final java.lang.String[] lineasMapaDeDirecciones = ("ShoeSizeLivAtt	TallaZapatos\r\n"
//			+ "LadiesSizeAtt	TallaDamas\r\n"
//			+ "SportsSizeAtt	TallaDeportes\r\n"
//			+ "MenSizeAtt	TallaCaballeros\r\n"
//			+ "ChildrenSizeAtt	TallaInfantiles\r\n"
//			+ "OpticalSizeAtt	TallaOptica\r\n"
//			+ "SizeCosmeticsAccAtt	TallaCosmeticos\r\n"
//			+ "SferaSizeAtt	TallaSfera\r\n"
//			+ "Direction1SizeAtt	TamañoDirección1\r\n"
//			+ "Direction3SizeAtt	TamañoDirección3\r\n"
//			+ "TamanoDireccion6Att	TamañoDirección6\r\n"
//			+ "TamanoDireccion8Att	TamañoDirección8\r\n"
//			+ "TamanoPantallaAtt	TamañoPantalla\r\n"
//			+ "SB_TCABALLEROS	SB_TCaballeros\r\n"
//			+ "SB_TCALCETERIA	SB_TCalceteria\r\n"
//			+ "SB_TDAMAS	SB_TDamas\r\n"
//			+ "SB_TINFANTILES	SB_TInfantiles\r\n"
//			+ "SB_TJUNIORS	SB_TJuniors\r\n"
//			+ "SB_TLENCERIA	SB_TLenceria\r\n"
//			+ "SB_TZAPATOS	SB_TZapatos\r\n"
//			+ "SB_TBEBES	SB_TBebes\r\n"
//			+ "SB_TROPAINTERIOR	SB_TRopaInterior\r\n"
//			+ "SB_TJOYERIAYACCESORIOS	SB_TJoyeriayAccesorios\r\n"
//			+ "SB_THOGAR	SB_THogar\r\n"
//			+ "SB_0106	SB_0106\r\n"
//			+ "SB_0107	SB_0107\r\n"
//			+ "SB_0025	SB_0025\r\n"
//			+ "SB_T_HARDLINE	SBTHardline\r\n"
//			+ "SB_T_TECNO_ENTREN	SBTTecnoEntren").split("\\r\\n");
//	
//	public static final java.lang.String[] lineasDireccionTallaWeb = ("TallaZapatos	clothingSize\r\n"
//			+ "TallaDamas	clothingSize\r\n"
//			+ "TallaDeportes	clothingSize\r\n"
//			+ "TallaCaballeros	clothingSize\r\n"
//			+ "TallaInfantiles	clothingSize\r\n"
//			+ "TallaOptica	clothingSize\r\n"
//			+ "TallaCosmeticos	clothingSize\r\n"
//			+ "TallaSfera	clothingSize\r\n"
//			+ "TamañoDirección1	SizeVaD\r\n"
//			+ "TamañoDirección3	SizeVaD\r\n"
//			+ "TamañoDirección6	SizeVaD\r\n"
//			+ "TamañoDirección8	SizeVaD\r\n"
//			+ "TamañoPantalla	SizeVaD\r\n"
//			+ "SB_TCaballeros	clothingSize\r\n"
//			+ "SB_TCalceteria	clothingSize\r\n"
//			+ "SB_TDamas	clothingSize\r\n"
//			+ "SB_TInfantiles	clothingSize\r\n"
//			+ "SB_TJuniors	clothingSize\r\n"
//			+ "SB_TLenceria	clothingSize\r\n"
//			+ "SB_TZapatos	clothingSize\r\n"
//			+ "SB_TBebes	clothingSize\r\n"
//			+ "SB_TRopaInterior	clothingSize\r\n"
//			+ "SB_TJoyeriayAccesorios	clothingSize\r\n"
//			+ "SB_THogar	SizeVaD\r\n"
//			+ "SB_0106	SizeVaD\r\n"
//			+ "SB_0107	SizeVaD\r\n"
//			+ "SB_0025	SizeVaD\r\n"
//			+ "SBTHardline	SizeVaD\r\n"
//			+ "SBTTecnoEntren	SizeVaD").split("\\r\\n");

	public static java.util.Map<String, String> mapaDeDirecciones; // = new java.util.TreeMap<>();
	public static java.util.Map<String, String> mapaDeDireccionesAtributoTallaWeb; // = new java.util.TreeMap<>();
	public static java.util.Map<String, String> mapaDeAtributosFechas; // = new java.util.TreeMap<>();
	
	private static java.util.Map<String, String> loadFieldDictionaries() throws ServiceUnavailableException {
		java.util.Map<String, String> mapa = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(baseUrlDEV);
		rw.getRc().getHeader().put("Authorization", "Basic: " + encoded);
		rw.putParameter("dictionary", "RelAttribSTDATG");
		rw.putParameter("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0;i<rows.length();i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					mapa.put(values.getString(0), values.getString(1));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return mapa;
	}

	private static java.util.Map<String, String> loadFieldTallaATG() throws ServiceUnavailableException {
		java.util.Map<String, String> mapaDeDirecciones = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(baseUrlDEV);
		rw.getRc().getHeader().put("Authorization", "Basic: " + encoded);
		rw.putParameter("dictionary", "RelAttribTallaATG");
		rw.putParameter("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0;i<rows.length();i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					mapaDeDirecciones.put(values.getString(0), values.getString(1));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return mapaDeDirecciones;
	}
	
	private static java.util.Map<String, String> loadAtributosFecha() throws ServiceUnavailableException {
		java.util.Map<String, String> mapa = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(baseUrlDEV);
		rw.getRc().getHeader().put("Authorization", "Basic: " + encoded);
		rw.putParameter("dictionary", "ConversionFechaATG");
		rw.putParameter("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.AlternativeValue");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0;i<rows.length();i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					mapa.put(values.getString(0), values.getString(1));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return mapa;
	}
	
	private static java.util.Set<String> loadInheritedFields() throws ServiceUnavailableException{
		java.util.Set<String> mapa = new java.util.TreeSet<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(baseUrlDEV);
		rw.getRc().getHeader().put("Authorization", "Basic: " + encoded);
		rw.putParameter("dictionary", "CaracteristicasHeredables");
		rw.putParameter("fields", "StandardizationValue.Characteristic->Characteristic.Identifier");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0;i<rows.length();i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					mapa.add(values.getString(0));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return mapa;
	}
	
	static {
		try {
			YEA = loadInheritedFields();
			mapaDeDirecciones = loadFieldDictionaries();
			mapaDeDireccionesAtributoTallaWeb = loadFieldTallaATG();
			mapaDeAtributosFechas = loadAtributosFecha();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}


}
