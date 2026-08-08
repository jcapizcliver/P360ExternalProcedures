package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
import org.w3c.dom.Node;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class RealExportProductsExpressOMS {

	private static final String urlDeOMS = PropertiesManager.get("p360.contingency.out.url_oms"); // "https://brokerqa.liverpool.com.mx:7053/oms/Int100/Items";
	private static final String encoded  = PropertiesManager.get("p360.contingency.basic_token_auth");// "cmVzdDpoZWlsZXI=";
	private static final java.nio.file.Path fileSystemPrefixOMS = java.nio.file.Paths.get("..","stage","ToOMS");
	private static final String baseUrlDEV = PropertiesManager.get("p360.contingency.base_url");
	private static final RESTWrapper wrapper = new RESTWrapper();
	private static final RESTWorkshop rw = wrapper.getRw();
	private static final RestClient rc = rw.getRc();

	private final java.util.Map<String, java.util.Map<String, org.json.JSONObject>> templateMetadataSet =new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Set<String>> templateSets = new java.util.TreeMap<>();
	private final java.util.Map<String, org.json.JSONObject> globalProperties = new java.util.TreeMap<>();
	private final java.util.Set<String> globalSet = new java.util.TreeSet<>();
	
	private final java.util.Set<String> articulosEnviados = new java.util.TreeSet<>();
    
    private final int OMS_BATCH_SIZE = 10;

    private boolean frozenImagesExists = true;

	public RealExportProductsExpressOMS() {
		System.out.println("Adding global metadata");
		addGlobalData(globalProperties, globalSet, baseUrlDEV);
		System.out.println("Global metadata added");
		addCharacteristicData(globalProperties, baseUrlDEV);
	}
	
	private org.json.JSONObject getMeTheCompa(String compa) throws ServiceUnavailableException{
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/object/Product2G/'" + compa + "'@'MASTER'"
					+ "?entityFilter=Product2GStructureGroupMap,Product2GCharacteristicValue,Product2G,Product2GLang,ProductExtraData&includeIds=true&includeLabels=true", null);
			response = new org.json.JSONObject(rawResponse);
		} catch (org.json.JSONException | IOException e) {
			logE(e);
		}
		return response;
	}

	private static final String USAGE = "Usage: RealExportProducts <File with IDs or SKUs> -t ID|SKU [-s]\n-t indicates which type of content is in the file: SKU or Proposal IDs\n-s if present, indicates to send the data to destination, default is not send the data.";

	public static void main(String[] args) throws ServiceUnavailableException, IOException {
		
		if(args.length < 1) {
			System.out.println(USAGE);
			return;
		}
		String source = args[0];
		int type = 0;
		boolean send = false;
		if(args.length > 1) {
			java.util.LinkedList<String> extra = 
					new java.util.LinkedList<>(java.util.Arrays.asList(
							java.util.Arrays.copyOfRange(args, 1, args.length)));
			if(!extra.contains("-t") || extra.getLast().equals("-t")) {
				System.out.println(USAGE);
				return;
			}
			String arg = null;
			for(int i=0; i<extra.size(); i++) {
				arg = extra.get(i);
				if("-s".equals(arg)) {
					send = true;
				} else if("-t".equals(extra.get(i)) && i < extra.size() - 1) {
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
		RealExportProductsExpressOMS o = new RealExportProductsExpressOMS();
		if(type == 0) {
			java.util.List<String> losesos = new java.util.ArrayList<>();
			for(int a = 0; a<data.length; a++) {
				losesos.add(data[a]);
				if( (a+1) % 10 == 0 ) {					
					o.doIt( losesos.toArray(new String[] {}) , send, baseUrlDEV);
					losesos = new java.util.ArrayList<>();
				}
			}
			if(!losesos.isEmpty()) {
				o.doIt( losesos.toArray(new String[] {}) , send, baseUrlDEV);
			}
		}else if(type == 1) {
			String[] pedazos = data;
			for (String element : pedazos) {
				o.doIt(new String[] { o.getIdFromSKU( element ) }, send, baseUrlDEV);
			}
		}
	}
	
	
	private static String[] sourceContent(String source) {
		java.util.Set<String> lines = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(source)))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line))
					lines.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return lines.toArray(new String[] {});
	}

	public String getIdFromSKU(String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "characteristic('SKU',-1) wildcard \"" + sku + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
//		System.out.println(sku + " - " + rw.getRawResponse());
		if(response == null) {
		}else{
			rows = response.getJSONArray("rows");
			if(rows.length() > 0 ) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			}else {
				log("SKU not found: " + sku);
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
	
	public String doIt( String[] proposalIds, boolean sendIt, String baseUrl ) throws ServiceUnavailableException {
		log("Going over: " + proposalIds.length);
		return doIt(proposalIds, sendIt);
	}

	
	private String grabDescLong(org.json.JSONArray lang) {
		for(int i=0; i<lang.length(); i++) {
			if(10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key") ) {
				return lang.getJSONObject(i).has("descriptionLong") ? lang.getJSONObject(i).getString("descriptionLong") : null;
			}
		}
		return null;
	}
	
	private String grabDescLong2(org.json.JSONArray lang) {
		for(int i=0; i<lang.length(); i++) {
			if(10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key") ) {
				return lang.getJSONObject(i).has("descriptionLong2") ? lang.getJSONObject(i).getString("descriptionLong2") : null;
			}
		}
		return null;
	}
	
	private String grabProductName(org.json.JSONArray lang) {
		for(int i=0; i<lang.length(); i++) {
			if(10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("productName") ? lang.getJSONObject(i).getString("productName") : null;
			}
		}
		return null;
	}
	
	private String grabProductDescriptionShort(org.json.JSONArray lang) {
		for(int i=0; i<lang.length(); i++) {
			if(10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("descriptionShort") ? lang.getJSONObject(i).getString("descriptionShort") : null;
			}
		}
		return null;
	}
	
	@SuppressWarnings("deprecation")
	public String doIt(String[] proposalIds, boolean sendIt) throws ServiceUnavailableException {
		if(proposalIds == null || proposalIds.length == 0) {
			return null;
		}

		frozenImagesExists = Boolean.parseBoolean( PropertiesManager.get("p360.contingency.useFrozenImages", "true") );

	    if(proposalIds.length > OMS_BATCH_SIZE) {
	        StringBuilder aggregated = new StringBuilder();

	        for(int from = 0; from < proposalIds.length; from += OMS_BATCH_SIZE) {
	            int to = Math.min(from + OMS_BATCH_SIZE, proposalIds.length);

	            String[] batch = java.util.Arrays.copyOfRange(proposalIds, from, to);

	            log("OMS doIt batch " + (from + 1) + "-" + to + " de " + proposalIds.length);

	            String response = doIt(batch, sendIt);

	            if(response != null && !"".equals(response)) {
	                if(aggregated.length() > 0) {
	                    aggregated.append("<;;>");
	                }
	                aggregated.append(response);
	            }
	        }

	        return aggregated.length() == 0 ? null : aggregated.toString();
	    }

		java.util.Map<String, String> prodToSKU = new java.util.HashMap<>();
		java.util.Map<String, String> artToSKU = new java.util.HashMap<>();
		System.out.println("Doing it " + (proposalIds == null ? "NaN" : proposalIds.length) );
		log("Running using baseUrlDEV: " + baseUrlDEV);
		log("Running using fileSystemPrefixOMS: " + fileSystemPrefixOMS);
    	System.out.println("OK");
    	java.util.List<String> productos = new java.util.ArrayList<>();
		String proposalId = null;
		StringBuilder aggregatedMessage = new StringBuilder();
        try {

        	/***
        	 * 
        	 *	Creación del archivo XML 
        	 * 
        	 *********************************/
        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	Document doc = builder.newDocument();
        	
			java.util.LinkedList<org.json.JSONObject> rescataLaRaiz = new java.util.LinkedList<>();

        	Element spim = doc.createElement("STEP-ProductInformation");

        	System.out.println("Elpis");
        	spim.setAttribute("ExportTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new java.util.Date() ));
        	spim.setAttribute("ExportContext", "Context2");
        	spim.setAttribute("ContextID", "Context2");
        	spim.setAttribute("WorkspaceID", "Approved");
        	spim.setAttribute("UseContextLocale", "false");

        	Element attributes = doc.createElement("AttributeList"); // <AttributeList></AttributeList>
        	spim.appendChild(attributes); // <STEP-ProductInformation ExportTime="">  <AttributeList></AttributeList> </STEP-ProductInformation>

        	Element assets = doc.createElement("Assets"); // <Assets></Assets>

        	/******
        	 *	Si se aprobó normal, es el mensaje del ejemplo, si se aprueba por repoblamiento, se tiene otro.
        	 ********/
			java.util.Map<String, Element> assetMap = new java.util.TreeMap<>();
			java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap = new java.util.TreeMap<>();
        	spim.appendChild(assets); // <STEP-ProductInformation ExportTime="">  <AttributeList></AttributeList> <Assets></Assets> </STEP-ProductInformation>

        	Element products = doc.createElement("Products");
        	doc.appendChild(spim);
        	spim.appendChild(products);

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
        	
			System.out.println("chars added");
			for(int index = 0; index<proposalIds.length; index++) {
				proposalId = proposalIds[index];
				log("--->" + proposalId + "<---");
				System.out.println("--->" + proposalId + "<---");
				final String[] productsToTestWith = new String[] {proposalId};
				org.json.JSONObject rp = getMeTheCompa(proposalId);
				if(rp == null || !rp.getJSONObject("_data").has("_characteristicRecords")) {
					System.out.println("Returning this " + proposalId + " due to lack of data.");
					continue;
				}
				org.json.JSONArray characteristicArray = rp.getJSONObject("_data").getJSONArray("_characteristicRecords");
				String sapObjectType = null;
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("sapObjectType") ) {
					sapObjectType = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("sapObjectType").getString("_code");
				}else {
					sapObjectType = getSAPObjectType(characteristicArray);
				}
				java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> dataMap = buildDataMap(rp.getJSONObject("_data").getJSONArray("_characteristicRecords"));
				String business = null;
				try{
					if(rp.getJSONObject("_data").has("business")) {
						business = rp.getJSONObject("_data").getJSONObject("business").getString("_code");
					}else {
						if(dataMap.containsKey("Business")){
							business = dataMap.get("Business").getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
						}
					}
				}catch(org.json.JSONException e) {
					logE(e);
				}
				java.util.LinkedList<org.json.JSONObject> lst = null;
				String template = !rp.getJSONObject("_data").has("structureGroupMap") ? null : getPrimaryProductTaxonomyTemplate(rp.getJSONObject("_data").getJSONArray("structureGroupMap")); // rp.getJSONObject("_data").getJSONArray("structureGroupMap").getJSONObject(0).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
				if(template == null) {
					log("Sin plantilla");
					System.out.println("Skipped due to Sin plantilla");
					template = "UnCatLevel1";
				}
				String itemId = rp.getJSONObject("_entityItem").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
				String productType = null;
				String descLong = rp.getJSONObject("_data").has("lang") ? grabDescLong( rp.getJSONObject("_data").getJSONArray("lang") ) : null;
				String descLong2 = rp.getJSONObject("_data").has("lang") ? grabDescLong2( rp.getJSONObject("_data").getJSONArray("lang") ) : null;
				String productName = rp.getJSONObject("_data").has("lang") ? grabProductName( rp.getJSONObject("_data").getJSONArray("lang") ) : null;
				String nameLang = rp.getJSONObject("_data").has("lang") ? grabProductDescriptionShort( rp.getJSONObject("_data").getJSONArray("lang") ) : null;
				String charactName = null;
				
				String brandName = null;
				String brandIdS4H = null;
				String brandNameLabel = null;
				String brandIdS4HLabel = null;
				String itemGroup = null;
				String itemGroupS4H = null;
				String itemGroupLabel = null;
				String itemGroupS4HLabel = null;
				String direccion = null;
				String direccionLabel = null;
				String seccion = null;
				String seccionLabel = null;
				String supplierPartNumber = null;
				String supplierID = null;
				String mainBarCode = null;
				String sku = null;
				String embeddedCodeWEB = null;
				String embeddedCodeWAP = null;
				String refundPolicy = null;
				
				String raw = null;
				String firstVariant = null;
				org.json.JSONObject imageObject = null;
				String tamanoUnico = null;
				String tallaNormalizada = null;
				String codigoColor = null;
				String color = null;
	
				String piName = null;
				String piUrl = null;
				String piKey = null;
	
				java.util.LinkedList<String[]> details = new java.util.LinkedList<>();
				java.util.LinkedList<String[]> smoshes = new java.util.LinkedList<>();
				java.util.LinkedList<String[]> illustrations = new java.util.LinkedList<>();
				
				org.json.JSONArray characteristicRecords = null;
				org.json.JSONArray upperRows = null;
				
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("brandName") ) {
					brandName = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("brandName").getString("_code");
					brandNameLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("brandName").getString("_label");
				}
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("brandIdS4H") ) {
					brandIdS4H = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("brandIdS4H").getString("_code");
					brandIdS4HLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("brandIdS4H").getString("_label");
				}
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("itemGroup") ) {
					itemGroup = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("itemGroup").getString("_code");
					itemGroupLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("itemGroup").getString("_label");
				}
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("itemGroupS4H") ) {
					itemGroupS4H = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("itemGroupS4H").getString("_code");
					itemGroupS4HLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("itemGroupS4H").getString("_label");
				}
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("direction") ) {
					direccion = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("direction").getString("_code");
					direccionLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("direction").getString("_label");
				}
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("section") ) {
					seccion = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("section").getString("_code");
					seccionLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("section").getString("_label");
				}
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("supplierID") ) {
					supplierID = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("supplierID").getString("_code");
				}
				if( rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("supplierPartNumber") ) {
					supplierPartNumber = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getString("supplierPartNumber");
				}
				if( rp.getJSONObject("_data").has("gtin") ) {
					mainBarCode = rp.getJSONObject("_data").getString("gtin");
				}
				if( rp.getJSONObject("_data").has("sku") ) {
					sku = String.valueOf( rp.getJSONObject("_data").getLong("sku") );
				}
				if( rp.getJSONObject("_data").has("embeddedCodeWEB") ) {
					embeddedCodeWEB = rp.getJSONObject("_data").getString("embeddedCodeWEB");
				}
				if( rp.getJSONObject("_data").has("embeddedCodeWAP") ) {
					embeddedCodeWAP = rp.getJSONObject("_data").getString("embeddedCodeWAP");
				}
				if( rp.getJSONObject("_data").has("refundPolicy") ) {
					refundPolicy = rp.getJSONObject("_data").getString("refundPolicy");
				}
				/***
				 * 
				 * 	Esto es para individual
				 * 
				 * 
				 * ****/
				try {
					String charId = null;
					raw = rw.makeRequest("GET", "/list/Article/bySearch"
							+ "?fields="
								+ java.net.URLEncoder.encode(
										   "Article.SupplierAID"
										+ ",ProductReference.ReferencedSupplierAid(\"" + itemId + "\")"
										+ ",Article.SKU"
										,"UTF-8")
							+ "&query=" + java.net.URLEncoder.encode("ProductReference.ReferencedSupplierAid(\"" + itemId + "\") equals \""+itemId+"\"", "UTF-8"), null);
					org.json.JSONObject resp = new org.json.JSONObject(raw);
					System.out.println(resp);
					upperRows = resp.getJSONArray("rows");
					if((sku == null || "".equals(sku)) && upperRows.length() == 1) {
						try{
							sku = upperRows.getJSONObject(0).getJSONArray("values").getString(2);
						}catch(org.json.JSONException e) {
						}
						firstVariant = null;
					}
					productType = "00".equals(sapObjectType) && !"MKP".equals(business) ? "SalesItem" : "00".equals(sapObjectType) && "MKP".equals(business) ? "SalesItemFamilyMkt" : !"00".equals(sapObjectType) && "MKP".equals(business) ? "SalesItemFamilyMkt" : "SalesItemFamily";
					System.out.println(upperRows.length());
					for(int a = 0; a<upperRows.length(); a++) {
						try{
							firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
						}catch(org.json.JSONException e) {
						}
						raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData", null);
						resp = new org.json.JSONObject(raw);
						resp = resp.getJSONObject("_data");
						if(!resp.has("_characteristicRecords")) {
							log("No characteristic records for: " + itemId);
							System.out.println("No characteristic records for: " + itemId);
							continue;
						}
						characteristicRecords = resp.getJSONArray("_characteristicRecords");
						org.json.JSONArray children = null;
						String[] chunk = null;
						for(int b = 0; b < characteristicRecords.length(); b++) {
							imageObject = characteristicRecords.getJSONObject(b);
							charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
							if(("ProductImage" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
								if(imageObject.has("_children")) {
									children = imageObject.getJSONArray("_children");
									piKey = imageObject.getJSONObject("_qualification").getString("recordKey");
									for(int c = 0; c<children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
										if(("ProductImage_Name" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											piName = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}else if(("ProductImage_URL" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											piUrl = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}
									}
								}
							}else if(("ProductImageDetail" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
								if(imageObject.has("_children")) {
									children = imageObject.getJSONArray("_children");
									chunk = new String[3];
									chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
									for(int c = 0; c<children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
										if(("ProductImageDetail_Name" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}else if(("ProductImageDetail_URL" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}
									}
									details.addLast(chunk);
								}
							}else if(("ProductImageSmosh" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
								if(imageObject.has("_children")) {
									children = imageObject.getJSONArray("_children");
									chunk = new String[3];
									chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
									for(int c = 0; c<children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
										if(("ProductImageSmosh_Name" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}else if(("ProductImageSmosh_URL" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}
									}
									smoshes.addLast(chunk);
								}
							}else if(("Illustration" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
								if(imageObject.has("_children")) {
									children = imageObject.getJSONArray("_children");
									chunk = new String[3];
									chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
									for(int c = 0; c<children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
										if(("Illustration_Name" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}else if(("Illustration_URL" + ( frozenImagesExists ? "2" : "" ) ).equals(charId)) {
											chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}
									}
									illustrations.addLast(chunk);
								}
							} else if("SalesItem".equals(productType) && "TamanoUnicoSTD".equals(charId)){
								tallaNormalizada = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}else if("SalesItem".equals(productType) && "TamanoUnico".equals(charId)) {
								tamanoUnico = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
							}else if("SalesItem".equals(productType) && "ColoursLiverpoolAtt".equals(charId)) {
								color = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
								codigoColor = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
							}
						}
						if(piName != null && piUrl != null) {
							break;
						}
					}
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					logE(e);
				}
				String rawResponse = null;
				org.json.JSONObject response = null;
				characteristicRecords = null;
				int currentIndex = 0;
				int totalSize = 0;
				org.json.JSONArray values = null;
				currentIndex = 0;
				String prevC = null;
				java.util.Set<String> atributosGeneralesQueSi = null;
				currentIndex = 0;
				java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = null;
				String currC = null;
				prevC = null;
				org.json.JSONObject prop = new org.json.JSONObject();
				org.json.JSONArray prevV = null;
				propiedadesCaracteristicas = templateMetadataSet.get(template);
				atributosGeneralesQueSi = templateSets.get(template);
				System.out.println("Propiedades de característcias.");
				if(propiedadesCaracteristicas == null) {
					propiedadesCaracteristicas = new java.util.TreeMap<>();
					atributosGeneralesQueSi = new java.util.TreeSet<>();
					templateSets.put(template, atributosGeneralesQueSi);
					atributosGeneralesQueSi.addAll(globalSet);
					templateMetadataSet.put(template, propiedadesCaracteristicas);
					for(java.util.Map.Entry<String, org.json.JSONObject> globalPropertiesEntry : globalProperties.entrySet()) {
						propiedadesCaracteristicas.put(globalPropertiesEntry.getKey(), globalPropertiesEntry.getValue());
					}
					log("Going to request Propiedades Característicias: ");
					try {
						do {
							rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/StandardizationValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'", "UTF-8")
									+ "&query="
										+ java.net.URLEncoder.encode(
											"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
											+ " and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\""
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
							characteristicRecords = response.getJSONArray("rows");
							for(int i=0; i<characteristicRecords.length(); i++) {
								currentIndex++;
								values = characteristicRecords.getJSONObject(i).getJSONArray("values");
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

	        	product = doc.createElement("Product"); // <Product></Product>
	        	product.setAttribute("ID", productsToTestWith[0]);
	        	product.setAttribute("UserTypeID", productType );
	        	product.setAttribute("ParentID", template);
	        	product.setAttribute("Changed", "true");
	        	productos.add(proposalId);
	        	/*********************
	        	 * El atributo en las entidades Changed="true", tiene el efecto
	        	 * de que broker ignorda todo lo que no tenga Changed="true".
	        	****************************************************************/
	        	Element name = doc.createElement("Name");
	        	name.setAttribute("Changed", "true");
	        	product.appendChild(name);

	        	Element keyValueSKU = null;
	        	Element keyValueEAN = null;

	        	Element attributeValues = null;

	        	String charId = null;
	        	org.json.JSONObject characteristic = null;

	        	boolean behvo = false;
	        	String almacenamientoAtt = null;
	        	String skuType = null;
	        	String mtart = null;
	        	String pt = null;
	        	String ptl = null;

	        	Element helperElement = null;
	        	java.util.Map<String, Element> tableroDeControl = new java.util.TreeMap<>();

	        	for(org.json.JSONObject laRaiz : rescataLaRaiz) {
	        		helperElement = tableroDeControl.get(laRaiz.getString("identifier"));
	        		if(helperElement != null) {
	        			webHierarchyRoot.appendChild(helperElement);
	        		}
	        	}

	        	java.util.ArrayList<String> unosQueQuiero = new java.util.ArrayList<>(YEA);
	        	java.util.Map<String, org.json.JSONObject> heredables = new java.util.TreeMap<>();
	        	
	        	attributeValues = doc.createElement("Values");
	        	product.appendChild(attributeValues);
	        	System.out.println("Product type: " + productType);
	        	if ("SalesItem".equals(productType) && piName != null && piUrl != null && piKey != null) {
					appendMediaAsset(piName, piUrl, "PrimaryProductImage", // String assetType,
							piKey, "Imagen Producto", // String assetValueTextContent,
							"ImageURL", // String assetValueAttributeId,
							"ProductImage", // String assetUserTypeId,
							"ProductImage", // String assetKeyPrefix,
							itemId, characteristic, "ProductImage", // String baseAssetTypeName,
							assetMap, assetReferencesMap, product, assets, doc, firstVariant);
				}
				if ("SalesItem".equals(productType) && details != null && !details.isEmpty()) {
					for (String[] dt : details) {
						appendMediaAsset(dt[0], dt[1], "ProductImage", // String assetType,
								dt[2], "Imagen Detalle Producto", // String assetValueTextContent,
								"ImageURL", // String assetValueAttributeId,
								"ProductImageDetail", // String assetUserTypeId,
								"ProductImageDetail", // String assetKeyPrefix,
								itemId, characteristic, "ProductImageDetail", // String baseAssetTypeName,
								assetMap, assetReferencesMap, product, assets, doc, firstVariant);
					}
				}
				if ("SalesItem".equals(productType) && smoshes != null && !smoshes.isEmpty()) {
					for (String[] dt : smoshes) {
						appendMediaAsset(dt[0], dt[1], "ProductImageSmosh", // String assetType,
								dt[2], "Imagen Smosh Producto", // String assetValueTextContent,
								"ImageURL", // String assetValueAttributeId,
								"ProductImageSmosh", // String assetUserTypeId,
								"SmoshImg", // String assetKeyPrefix,
								itemId, characteristic, "ProductImageDetail", // String baseAssetTypeName,
								assetMap, assetReferencesMap, product, assets, doc, firstVariant);
					}
				}
				if ("SalesItem".equals(productType) && illustrations != null && !illustrations.isEmpty()) {
					for (String[] dt : illustrations) {
						appendMediaAsset(dt[0], dt[1], "Illustration", // String assetType,
								dt[2], "Imagen Isométrica del Producto", // String assetValueTextContent,
								"ImageURL", // String assetValueAttributeId,
								"Illustration", // String assetUserTypeId,
								"Illustration", // String assetKeyPrefix,
								itemId, characteristic, "Illustration", // String baseAssetTypeName,
								assetMap, assetReferencesMap, product, assets, doc, firstVariant);
					}
				}
				if ("SalesItem".equals(productType) && tallaNormalizada != null && !"".equals(tallaNormalizada)) {
					appendPlainElementValue(tallaNormalizada, null, "TC-NormalizedSize", attributeValues, attributes,
							doc, propiedadesCaracteristicas);
				}
				if ("SalesItem".equals(productType) && color != null && !"".equals(color)) {
					appendPlainElementValue(color, codigoColor, "ColoursLiverpoolAtt", attributeValues, attributes, doc,
							propiedadesCaracteristicas);
				}
	        	
				String rr = null;
				try {
					rr = rc.getRequest("GET", 
							baseUrlDEV 
								+ "/object/StructureGroup/'" + template + "'@'PrimaryProductTaxonomy'?entityFilter=StructureGroupAttribute", null);
					org.json.JSONObject tratando = new org.json.JSONObject(rr);
					org.json.JSONArray attributeRow = tratando.getJSONObject("_data").has("attribute") ? tratando.getJSONObject("_data").getJSONArray("attribute") : new org.json.JSONArray();
					for(int a = 0; a<attributeRow.length(); a++) {
						if("DisplayGroupOrder".equals(attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"))) {
							try {
								String val = attributeRow.getJSONObject(a).getJSONArray("value").getJSONObject(0).getString("value");
								appendPlainElementValue(
		    							val,
		    							null,
		    							attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"),
		    							attributeValues,
		    							attributes,
		    							doc,
		    							propiedadesCaracteristicas);
							}catch(org.json.JSONException ex) {
								log("No DisplayOrder could be retrieved: " + attributeRow.getJSONObject(a));
							}
						}else if("DisplayOrder".equals(attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"))) {
							try {
								String val = attributeRow.getJSONObject(a).getJSONArray("value").getJSONObject(0).getString("value");
								appendPlainElementValue(
		    							val,
		    							null,
		    							attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"),
		    							attributeValues,
		    							attributes,
		    							doc,
		    							propiedadesCaracteristicas);
							}catch(org.json.JSONException ex) {
								log("No DisplayOrder could be retrieved: " + attributeRow.getJSONObject(a));
							}
						}else if("ConfigurableOrder".equals(attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"))) {
							try {
								String val = attributeRow.getJSONObject(a).getJSONArray("value").getJSONObject(0).getString("value");
								appendPlainElementValue(
		    							val,
		    							null,
		    							attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"),
		    							attributeValues,
		    							attributes,
		    							doc,
		    							propiedadesCaracteristicas);
							}catch(org.json.JSONException ex) {
								log("No DisplayOrder could be retrieved: " + attributeRow.getJSONObject(a));
							}	
						}
					}
				} catch (IOException e) {
					logE(e);
				}
				System.out.println("Properties from PPT added.");
				if(descLong != null) {
					appendPlainElementValue(
							descLong,
							null,
							"DescriptionLong",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas
							);
				}
				if(descLong2 != null) {
					appendPlainElementValue(
							descLong2,
							null,
							"DescriptionLong2",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas
							);
				}
				
				if(business != null) {
					if("MKP".equals(business)) {
    					appendPlainElementValue(
								"true",
								"1",
								"isMarketPlace",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas);
    				}
				}
	        	for(int i = 0; i<characteristicArray.length(); i++) {
	        		characteristic = characteristicArray.getJSONObject(i);
	        		charId = characteristic.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
	        		if("Business".equals(charId)) {
	        			if(business == null || "".equals(business)) {
	        				business = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
	        			}
        			}else if("Direction".equals(charId)) {
        				direccion = direccion == null || "".equals(direccion) ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : direccion;
        			}else if("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
	        			String eanval = (treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) ) );
	        			if((mainBarCode == null || "".equals(mainBarCode)) && !"".equals(eanval)) {
	        				mainBarCode = eanval;
	        			}
	        		}else if("SupplierPartNumber".equals(charId)) {
	        			supplierPartNumber = supplierPartNumber != null && !"".equals(supplierPartNumber) ? supplierPartNumber : characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
	        		}else if("SupplierID".equals(charId)) {
	        			supplierID = supplierID != null && !"".equals(supplierID) ? supplierID : characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
	        		}else if("SKU".equals(charId)) {
	        			String skuval = treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
	        			if( (sku == null || "".equals(sku)) && !"".equals(skuval)) {
	        				sku = skuval;
	        			}else {
	        				System.out.println("No change on it. (sku)");
	        			}
	        		} else {
	        			if("ProductName".equals( charId )) {
	        				productName = productName != null && !"".equals(productName) ? productName : ( productName =  characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
	        			}else if("Name".equals( charId )) {
	        				charactName = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
	        			} else if("ItemGroupS4H".equals( charId ) || "ItemGroup".equals( charId )) {
	        				if("ItemGroupS4H".equals( charId )) {
	        					itemGroupS4H = itemGroupS4H == null || "".equals(itemGroupS4H) ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : itemGroupS4H;
	        					itemGroupS4HLabel = itemGroupS4HLabel == null || "".equals(itemGroupS4HLabel) ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label") : itemGroupS4HLabel;
	        				}else if("ItemGroup".equals( charId )) {
	        					itemGroup = itemGroup == null || "".equals(itemGroup) ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : itemGroup;
	        					itemGroupLabel = itemGroupLabel == null || "".equals(itemGroupLabel) ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label") : itemGroupLabel;
	        				}
	        			}else if("BrandName".equals(charId) || "BRAND_ID_S4H".equals(charId)) {
	        				brandName = brandName == null || "".equals(brandName) ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : brandName;
	        				brandNameLabel = brandNameLabel == null || "".equals(brandNameLabel) ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label") : brandNameLabel;
	        			}else if("ProductType".equals(charId)){
							pt = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
							ptl = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
							if("1".equals(pt)) {
								lst = dataMap.get("ItemGroupS4H");
								if(lst != null) {
									itemGroupS4H = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}
								lst = dataMap.get("AlmacenamientoAtt");
								if(lst != null) {
									almacenamientoAtt = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}
								lst = dataMap.get("MTART_S4H");
								if(lst != null) {
									mtart = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}
								lst = dataMap.get("SkuType");
								if(lst != null) {
									skuType = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}
								if("SERV".equals(skuType) && "0001".equals(almacenamientoAtt)) {
									pt = "6";
									ptl = "Digital";
									appendPlainElementValue(
											"Digital",
			    							pt,
			    							"ProductType",
			    							attributeValues,
			    							attributes,
			    							doc,
			    							propiedadesCaracteristicas);
								}else if("DIEN".equals(mtart) && "SB87516".equals(itemGroupS4H)) {
									pt = "6";
									ptl = "Digital";
									appendPlainElementValue(
											"Digital",
											pt,
											"ProductType",
											attributeValues,
											attributes,
											doc,
											propiedadesCaracteristicas);
									
								}else {
									appendPlainElementValue(
											characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
											characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
											"ProductType",
											attributeValues,
											attributes,
											doc,
											propiedadesCaracteristicas);
								}
							}else {
								appendPlainElementValue(
										characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
										characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
										"ProductType",
										attributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas);
							}
	        			}else {
	        				if("ItemGroup2".equals(charId)) {
								continue;
							}else {
								if(atributosGeneralesQueSi.contains(charId)) {
									if("Section".equals(charId) && (seccion == null || "".equals(seccion))) {
										appendPlainElementValue(
												characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
												characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
												"Section",
												attributeValues,
												attributes,
												doc,
												propiedadesCaracteristicas);
									} else if("Direction".equals(charId) && (direccion == null || "".equals(direccion))){
										appendPlainElementValue(
												characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
												characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
												"Direction",
												attributeValues,
												attributes,
												doc,
												propiedadesCaracteristicas);
									}else {
										if(unosQueQuiero.contains(charId)) {
											heredables.put(charId, characteristic);
										}
										if("LOOKUP".equals(characteristic.getString("_datatype"))){
											appendPlainElementValue(
													characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
													characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
													charId,
													attributeValues,
													attributes,
													doc,
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
													attributes,
													doc,
													propiedadesCaracteristicas);
										}
									}
								}
							}
	        			}
	        		}
	        	}
	        	System.out.println("Main characteristic values were iterated.");
	        	if(embeddedCodeWEB != null && !"".equals(embeddedCodeWEB)) {
	        		appendPlainElementValue(
							embeddedCodeWEB,
							null,
							"EmbedCodeWEB",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
	        	}
	        	if(embeddedCodeWAP != null && !"".equals(embeddedCodeWAP)) {
	        		appendPlainElementValue(
							embeddedCodeWAP,
							null,
							"EmbedCodeWAP",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
	        	}
	        	if(refundPolicy != null && !"".equals(refundPolicy)) {
	        		appendPlainElementValue(
	        				refundPolicy,
	        				null,
							"refundPolicy",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
	        	}
	        	if(seccion != null && !"".equals(seccion)) {
	        		appendPlainElementValue(
							seccionLabel,
							seccion,
							"Section",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
	        	}
	        	if(direccion != null && !"".equals(direccion)) {
	        		appendPlainElementValue(
							direccionLabel,
							direccion,
							"Direction",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
	        	}
	        	if(supplierPartNumber != null && !"".equals(supplierPartNumber)) {
	        		appendPlainElementValue(
	        				supplierPartNumber,
	        				"",
	        				"SupplierPartNumber",
	        				attributeValues,
	        				attributes,
	        				doc,
	        				propiedadesCaracteristicas
	        				);
	        	}
	        	if(supplierID != null && !"".equals(supplierID)) {
	        		appendPlainElementValue(
	        				supplierID,
	        				"",
	        				"SupplierID",
	        				attributeValues,
	        				attributes,
	        				doc,
	        				propiedadesCaracteristicas
	        				);
	        	}
	        	
	        	if(!behvo) {
    				String elese = "SBB".equals(business) ? itemGroupS4H : itemGroup; // characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
    				try {
    					rawResponse = rw.makeRequest("GET", "/list/StandardizationValue/bySearch"
    							+ "?dictionaryProxy=" + encode("'" + (!"SBB".equals(business) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H") + "'")
    							+ "&query=" + encode("StandardizationValue.Value equals \"" + elese + "\"")
    							+ "&fields=" + encode("StandardizationValue.AlternativeValue")
    							+ ""
    							, null);
    					response = new org.json.JSONObject(rawResponse);
    					characteristicRecords = response.getJSONArray("rows");
    					log("Querying dictionary: " + (!"SBB".equals(business) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H"));
    					String laetiqueta = queryDictionary(elese, (!"SBB".equals(business) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H"));
    					if(characteristicRecords.length() > 0) {
    						rawResponse = rw.makeRequest("GET", "/list/LookupValue/bySearch"
        							+ "?lookup=" + encode("SAP_BEHVOLOV")
        							+ "&query=" + encode("LookupValueLang.Name(es) equals \"" + laetiqueta + "\"")
        							+ "&fields=" + encode("LookupValue.Code")
        							+ ""
        							, null);
        					response = new org.json.JSONObject(rawResponse);
        					characteristicRecords = response.getJSONArray("rows");
        					if(characteristicRecords.length() > 0) {
        						String elcode = characteristicRecords.getJSONObject(0).getJSONArray("values").getString(0);
        						appendPlainElementValue(
        								laetiqueta,
        								elcode,
        								"SAP_BEHVO",
        								attributeValues,
        								attributes,
        								doc,
        								propiedadesCaracteristicas);
        						behvo = true;
        					}
    					}
    				}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {

    				}
				}
	        	appendPlainElementValue(
	        			itemGroup != null && !"".equals(itemGroup) ? itemGroupLabel : itemGroupS4HLabel,
    					itemGroup != null && !"".equals(itemGroup) ? itemGroup : itemGroupS4H,
						"ItemGroup2",
						attributeValues,
						attributes,
						doc,
						propiedadesCaracteristicas
					);
	        	appendPlainElementValue(
	        			!"SBB".equals(business) ? itemGroupLabel : itemGroupS4HLabel,
    					!"SBB".equals(business) ? itemGroup : itemGroupS4H,
						!"SBB".equals(business) ? "ItemGroup" : "ItemGroupS4H",
						attributeValues,
						attributes,
						doc,
						propiedadesCaracteristicas
	        		);
				appendPlainElementValue(
						!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel ,
						!"SBB".equals(business) ? brandName : brandIdS4H ,
						!"SBB".equals(business) ? "BrandName" : "BRAND_ID_S4H" ,
						attributeValues,
						attributes,
						doc,
						propiedadesCaracteristicas
					);
				appendPlainElementValue(
						!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel ,
						null,
						"BrandNameATG",
						attributeValues,
						attributes,
						doc,
						propiedadesCaracteristicas
					);
				appendPlainElementValue(
						!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel ,
						null,
						"BrandIDATG",
						attributeValues,
						attributes,
						doc,
						propiedadesCaracteristicas
					);
	        	
				if(productName != null && !"".equals(productName)) {
					name.setTextContent(productName);
					appendPlainElementValue(
							productName,
							null,
							"ProductName",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
				}else {
					if(charactName != null && !"".equals(charactName)) {
						name.setTextContent(charactName);
						appendPlainElementValue(
								charactName,
								null,
								"ProductName",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas);
					}else {
						if(nameLang != null && !"".equals(nameLang)) {
							name.setTextContent(nameLang);
							appendPlainElementValue(
									nameLang,
									null,
									"ProductName",
									attributeValues,
									attributes,
									doc,
									propiedadesCaracteristicas);
						}else {
							log("Sin product neim, no será posible publicar.");
							System.out.println("Sin product neim, no será posible publicar a ATG pero como esto es OMS, sí se va.");
							continue;
						}
					}
				}
	        	
	        	if(pt == null) {
					lst = dataMap.get("AlmacenamientoAtt");
					if(lst != null) {
						almacenamientoAtt = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
					}
					lst = dataMap.get("MTART_S4H");
					if(lst != null) {
						mtart = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
					}
					lst = dataMap.get("SkuType");
					if(lst != null) {
						skuType = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
					}
					if("SERV".equals(skuType) && "0001".equals(almacenamientoAtt)) {
						pt = "6";
						ptl = "Digital";
						appendPlainElementValue(
								"Digital",
    							pt,
    							"ProductType",
    							attributeValues,
    							attributes,
    							doc,
    							propiedadesCaracteristicas);
					}else if("DIEN".equals(mtart) && "SB87516".equals(itemGroupS4H)) {
						pt = "6";
						ptl = "Digital";
						appendPlainElementValue(
								"Digital",
								pt,
								"ProductType",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas);
						
					}else {
						pt = "1";
						ptl = "Soft line";
						appendPlainElementValue(
								"Soft line",
    							pt,
    							"ProductType",
    							attributeValues,
    							attributes,
    							doc,
    							propiedadesCaracteristicas);
					}
	        	}
	        	if(unosQueQuiero.contains("ProductType") && !heredables.containsKey("ProductType")) {
	        		heredables.put("ProductType", new org.json.JSONObject().put("_datatype", "LOOKUP").put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", "ProductType"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", pt).put("_label", ptl))))));
	        	}
	        	if(unosQueQuiero.contains("SupplierPartNumber") && !heredables.containsKey("SupplierPartNumber") && !"".equals(supplierPartNumber) && supplierPartNumber != null) {
	        		heredables.put("SupplierPartNumber", new org.json.JSONObject().put("_datatype", "TEXT").put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", "ProductType"))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(supplierPartNumber)))));
	        	}
	        	System.out.println("Working as " + productType + " productType");
	        	if(productType.startsWith("SalesItemFamily")) {
	        		org.json.JSONObject resp = null;
	        		Element subProduct = null;
	        		Element subAttributeValues = null;
	        		Element varName = null;
	        		String childSAPObjectType = null;
	        		String childSAPObjectTypeLabel = null;
					java.util.LinkedList<java.util.LinkedList<String[]>> losdetalles = new java.util.LinkedList<>();
					java.util.LinkedList<java.util.LinkedList<String[]>> losesmoshes = new java.util.LinkedList<>();
					java.util.LinkedList<java.util.LinkedList<String[]>> lasilustraciones = new java.util.LinkedList<>();
					boolean theFirstTime = true;
	        		org.json.JSONObject characteristicObject = null;
	        		for(int a = 0; a<upperRows.length(); a++) {
	        			tallaNormalizada = null;
	        			tamanoUnico = null;
	        			color = null;
	        			codigoColor = null;
	        			childSAPObjectType = null;
	        			childSAPObjectTypeLabel = null;
	    				try{
	    					firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
	    				}catch(org.json.JSONException e) {
	    				}
	        			subProduct = doc.createElement("Product");
	        			subProduct.setAttribute("ID", firstVariant);
	        			subProduct.setAttribute("UserTypeID", "SalesItemFamily".equals(productType) ? "SalesItemVariant" : "SalesItem");
	        			subProduct.setAttribute("ParentID", productsToTestWith[0]);
	        			subProduct.setAttribute("Changed", "true");
	                	subAttributeValues = doc.createElement("Values");
	                	varName = doc.createElement("Name");
	                	subProduct.appendChild(varName);
	                	subProduct.appendChild(subAttributeValues);
	                	if("MKP".equals(business)) {
	                		appendPlainElementValue(
    								"true",
    								"1",
    								"isMarketPlace",
    								subAttributeValues,
    								attributes,
    								doc,
    								propiedadesCaracteristicas);
        				}

						details = new java.util.LinkedList<>();
						smoshes = new java.util.LinkedList<>();
						illustrations = new java.util.LinkedList<>();
	                	
	    				try {
							raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData", null);
							resp = new org.json.JSONObject(raw);
							resp = resp.getJSONObject("_data");
							if(!resp.has("_characteristicRecords")) {
								log("Variante no tenía características (" + firstVariant + ")");
								System.out.println("Variante no tenía características (" + firstVariant + ")");
								continue;
							}
							characteristicRecords = resp.getJSONArray("_characteristicRecords");
							org.json.JSONArray children = null;
							String[] chunk = null;
							String sku0 = resp.has("sku") ? String.valueOf( resp.getLong("sku") ) : null;
							String ean0 = resp.has("gtin") ? resp.getString("gtin") : null;
							codigoColor = resp.has("articleExtraData") && resp.getJSONArray("articleExtraData").getJSONObject(0).has("coloursLiverpoolAtt") ? resp.getJSONArray("articleExtraData").getJSONObject(0).getJSONObject("coloursLiverpoolAtt").getString("_code") : null;
							color = resp.has("articleExtraData") && resp.getJSONArray("articleExtraData").getJSONObject(0).has("coloursLiverpoolAtt") ? resp.getJSONArray("articleExtraData").getJSONObject(0).getJSONObject("coloursLiverpoolAtt").getString("_label") : null;
							tamanoUnico = resp.has("articleExtraData") && resp.getJSONArray("articleExtraData").getJSONObject(0).has("tamanoUnico") ? resp.getJSONArray("articleExtraData").getJSONObject(0).getJSONObject("tamanoUnico").getString("_code") : null;
							log("Came to here, color: " + color + ", tamanoUnico: " + tamanoUnico);
							java.util.List<String> misaidis = new java.util.ArrayList<>();
							for(int b = 0; b < characteristicRecords.length(); b++) {
								characteristicObject = characteristicRecords.getJSONObject(b);
								charId = characteristicObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								misaidis.add(charId);
								if("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
				        			ean0 = ean0 == null || "".equals(ean0) ? characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) : ean0;
				        		}else if("SKU".equals(charId)) {
				        			sku0 = sku0 == null || "".equals(sku0) ? characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) : sku0;
				        		} else if("TamanoUnicoSTD".equals(charId)){
									tallaNormalizada = characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("TamanoUnico".equals(charId)) {
									tamanoUnico = tamanoUnico != null ? tamanoUnico : characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
								}else if("ColoursLiverpoolAtt".equals(charId)) {
									color = color != null ? color : characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
									codigoColor = codigoColor != null ? codigoColor : characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}else if("SAPObjectType".equals(charId)) {
									childSAPObjectTypeLabel = characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
									childSAPObjectType = characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}else if("ProcedeNoProcede".equals(charId)) {
								}else if (("ProductImage" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									if (characteristicObject.has("_children")) {
										children = characteristicObject.getJSONArray("_children");
										piKey = characteristicObject.getJSONObject("_qualification")
												.getString("recordKey");
										for (int c = 0; c < children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification")
													.getJSONObject("characteristic").getString("_code");
											if (("ProductImage_Name" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												piName = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											} else if (("ProductImage_URL" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												piUrl = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
									}
								} else if (("ProductImageDetail" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									if (characteristicObject.has("_children")) {
										children = characteristicObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = characteristicObject.getJSONObject("_qualification")
												.getString("recordKey");
										for (int c = 0; c < children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification")
													.getJSONObject("characteristic").getString("_code");
											if (("ProductImageDetail_Name" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											} else if (("ProductImageDetail_URL" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										details.addLast(chunk);
										if (theFirstTime)
											losdetalles.addLast(details);
									}
								} else if (("ProductImageSmosh" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									if (characteristicObject.has("_children")) {
										children = characteristicObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = characteristicObject.getJSONObject("_qualification")
												.getString("recordKey");
										for (int c = 0; c < children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification")
													.getJSONObject("characteristic").getString("_code");
											if (("ProductImageSmosh_Name" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											} else if (("ProductImageSmosh_URL" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										smoshes.addLast(chunk);
										if (theFirstTime)
											losesmoshes.addLast(smoshes);
									}
								} else if (("Illustration" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									if (characteristicObject.has("_children")) {
										children = characteristicObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = characteristicObject.getJSONObject("_qualification")
												.getString("recordKey");
										for (int c = 0; c < children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification")
													.getJSONObject("characteristic").getString("_code");
											if (("Illustration_Name" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											} else if (("Illustration_URL" + (frozenImagesExists ? "2" : ""))
													.equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
														.getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										illustrations.addLast(chunk);
										if (theFirstTime)
											lasilustraciones.addLast(illustrations);
									}
								} else {
									if(atributosGeneralesQueSi.contains(charId)) {
										if("LOOKUP".equals(characteristicObject.getString("_datatype"))){
											appendPlainElementValue(
													characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
													characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
													charId,
													subAttributeValues,
													attributes,
													doc,
													propiedadesCaracteristicas);
										}else if(!"NONE".equals(characteristicObject.getString("_datatype"))) {
											java.util.LinkedList<String> vals = new java.util.LinkedList<>();
											for(int m=0; m<characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
												vals.addLast( String.valueOf( parseDateForSpecificDateFields( characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m), charId) ));
											}
											appendPlainElementValue(
													String.join(",", vals),
													null,
													charId,
													subAttributeValues,
													attributes,
													doc,
													propiedadesCaracteristicas);
										}
									}
								}
							}
							System.out.println("Smoshes: " + smoshes);
							System.out.println("Illustrations: " + illustrations);
							System.out.println("Details: " + details);
							if(sku0 == null || "".equals(sku0)) {
								sku0 = sku;
							}
							if(sku0 != null && !"".equals(sku0)) {
								if(sku == null || "".equals(sku)) {
									sku = sku0;
								}
								keyValueSKU = doc.createElement("KeyValue");
			        			keyValueSKU.setAttribute("KeyID","SKUID");
			        			keyValueSKU.setTextContent( sku0 );
			        			subProduct.appendChild(keyValueSKU);
			        			artToSKU.put(firstVariant, sku0);
								appendPlainElementValue(
										sku0,
										null,
										"SKU",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas
									);
							}
							if(ean0 == null || "".equals(ean0)) {
								ean0 = mainBarCode;
							}
							if(ean0 != null && !"".equals(ean0)) {
								if(mainBarCode == null || "".equals(mainBarCode)) {
									mainBarCode = ean0;
								}
								keyValueEAN = doc.createElement("KeyValue");
			        			keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
			        			keyValueEAN.setTextContent(ean0);
			        			subProduct.appendChild(keyValueEAN);
		        				appendPlainElementValue(
										ean0,
										null,
										"SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas
									);
							}
							if(!misaidis.contains("ProductName") && "".equals(name.getTextContent())) {
								System.out.println("Sin product name (" + proposalId + ")");
								continue;
							}
							articulosEnviados.add(firstVariant);
							product.appendChild(subProduct);
							varName.setTextContent( name.getTextContent() + (tamanoUnico != null && !"null".equals(tamanoUnico) && !"".equals(tamanoUnico) ? ", " + tamanoUnico : "" ) + (color != null && !"null".equals(color) && !"".equals(color) ? ", " + color : "" ) );
							for(java.util.Map.Entry<String, org.json.JSONObject> entr : heredables.entrySet()) {
								if(!misaidis.contains(entr.getKey())) {
									charId = entr.getKey();
									characteristic = entr.getValue();
									if("LOOKUP".equals(characteristic.getString("_datatype"))){
			        					appendPlainElementValue(
			        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
			        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
			        							charId,
			        							subAttributeValues,
			        							attributes,
			        							doc,
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
			        							subAttributeValues,
			        							attributes,
			        							doc,
			        							propiedadesCaracteristicas);
			        				}
								}
							}

							if(productName != null) {
								name.setTextContent(productName);
								appendPlainElementValue(
										productName,
										null,
										"ProductName",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas);
							}else {
								if(charactName != null && !"".equals(charactName)) {
									name.setTextContent(charactName);
									appendPlainElementValue(
											charactName,
											null,
											"ProductName",
											subAttributeValues,
											attributes,
											doc,
											propiedadesCaracteristicas);
								}else {
									if(nameLang != null && !"".equals(nameLang)) {
										name.setTextContent(nameLang);
										appendPlainElementValue(
												nameLang,
												null,
												"ProductName",
												subAttributeValues,
												attributes,
												doc,
												propiedadesCaracteristicas);
									}else {
										log("Sin product neim, no será posible publicar.");
										System.out.println("Sin product neim, no será posible publicar.");
										continue;
									}
								}
							}
							if(descLong != null) {
								appendPlainElementValue(
										descLong,
										null,
										"DescriptionLong",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas
									);
							}
							appendPlainElementValue(
									sku,
									null,
									"ParentSKU",
									subAttributeValues,
									attributes,
									doc,
									propiedadesCaracteristicas);
							if(childSAPObjectType != null && !"".equals(childSAPObjectType)) {
								appendPlainElementValue(
										childSAPObjectTypeLabel,
										childSAPObjectType,
										"SAPObjectType",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas);
							}
							if(tamanoUnico != null && !"".equals(tamanoUnico)) {
								appendPlainElementValue(
										tamanoUnico,
										null,
										"TamanoUnico",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas);
							}
							if(tallaNormalizada != null && !"".equals(tallaNormalizada)) {
								appendPlainElementValue(
										tallaNormalizada,
										null,
										"TC-NormalizedSize",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas);
							}
							if(color != null && !"".equals(color)) {
								appendPlainElementValue(
										color,
										codigoColor,
										"ColoursLiverpoolAtt",
										subAttributeValues,
										attributes,
										doc,
										propiedadesCaracteristicas);
							}
							appendPlainElementValue(
	        						"".equals(itemGroupLabel) ? itemGroupS4HLabel : itemGroupLabel,
	        						"".equals(itemGroup) ? itemGroupS4H : itemGroup,
	        						"ItemGroup2",
	        						subAttributeValues,
	        						attributes,
	        						doc,
	        						propiedadesCaracteristicas);
							if (piName != null && piUrl != null && piKey != null) {
								appendMediaAsset(piName, piUrl, "PrimaryProductImage", // String assetType,
										piKey, "Imagen Producto", // String assetValueTextContent,
										"ImageURL", // String assetValueAttributeId,
										"ProductImage", // String assetUserTypeId,
										"ProductImage", // String assetKeyPrefix,
										itemId, characteristic, "ProductImage", // String baseAssetTypeName,
										assetMap, assetReferencesMap, subProduct, assets, doc, firstVariant);
							}
							if (details != null && !details.isEmpty()) {
								for (String[] dt : details) {
									appendMediaAsset(dt[0], dt[1], "ProductImage", // String assetType,
											dt[2], "Imagen Detalle Producto", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"ProductImageDetail", // String assetUserTypeId,
											"ProductImageDetail", // String assetKeyPrefix,
											itemId, characteristic, "ProductImageDetail", // String baseAssetTypeName,
											assetMap, assetReferencesMap, subProduct, assets, doc, dt[3]);
								}
							}
							if (smoshes != null && !smoshes.isEmpty()) {
								for (String[] dt : smoshes) {
									appendMediaAsset(dt[0], dt[1], "ProductImageSmosh", // String assetType,
											dt[2], "Imagen Smosh Producto", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"ProductImageSmosh", // String assetUserTypeId,
											"SmoshImg", // String assetKeyPrefix,
											itemId, characteristic, "ProductImageSmosh", // String baseAssetTypeName,
											assetMap, assetReferencesMap, subProduct, assets, doc, firstVariant);
								}
							}
							if (illustrations != null && !illustrations.isEmpty()) {
								for (String[] dt : illustrations) {
									appendMediaAsset(dt[0], dt[1], "Illustration", // String assetType,
											dt[2], "Imagen Isométrica del Producto", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"Illustration", // String assetUserTypeId,
											"Illustration", // String assetKeyPrefix,
											itemId, characteristic, "Illustration", // String baseAssetTypeName,
											assetMap, assetReferencesMap, subProduct, assets, doc, firstVariant);
								}
							}

						} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
							logE(e);
							log("Exception on processing child. " + e.getMessage());
						}
	    			}

		        	if(sku != null && !"".equals(sku)) {
			        	keyValueSKU = doc.createElement("KeyValue");
						keyValueSKU.setAttribute("KeyID","SKUID");
						String skuval =  sku;
						keyValueSKU.setTextContent( skuval );
						product.appendChild(keyValueSKU);
						prodToSKU.put(proposalId, skuval);
						appendPlainElementValue(
								skuval,
								null,
								"SKU",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas);
		        	}
		        	if(mainBarCode != null && !"".equals(mainBarCode)) {
						keyValueEAN = doc.createElement("KeyValue");
						keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
						keyValueEAN.setTextContent(mainBarCode);
						product.appendChild(keyValueEAN);
						appendPlainElementValue(
								mainBarCode,
								null,
								"SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas);
		        	}
	        	}else {
				try {
					raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData", null);
					org.json.JSONObject resp = new org.json.JSONObject(raw);
					resp = resp.getJSONObject("_data");
					if(!resp.has("_characteristicRecords")) {
						System.out.println("No characteristic records.");
						continue;
					}
					characteristicRecords = resp.getJSONArray("_characteristicRecords");
					String sku0 = null;
					String ean0 = null;
					for(int b = 0; b < characteristicRecords.length(); b++) {
						imageObject = characteristicRecords.getJSONObject(b);
						charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
						
						if("TamanoUnicoSTD".equals(charId)){
						}else if("TamanoUnico".equals(charId)) {
						}else if("ColoursLiverpoolAtt".equals(charId)) {
						}else if("SKU".equals(charId)) {
							sku0 = treatment( imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
						}else if("MainBarCode".equals(charId)) {
							ean0 = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						}else if("MainBarCodeS4H".equals(charId)) {
							ean0 = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						}else if("SAPObjectType".equals(charId)) {
						}else if("ProcedeNoProcede".equals(charId)) {
						}else {
							if(atributosGeneralesQueSi.contains(charId)) {
								if("LOOKUP".equals(imageObject.getString("_datatype"))){
									appendPlainElementValue(
											imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
											imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
											charId,
											attributeValues,
											attributes,
											doc,
											propiedadesCaracteristicas);
								}else if(!"NONE".equals(imageObject.getString("_datatype"))) {
									java.util.LinkedList<String> vals = new java.util.LinkedList<>();
									for(int m=0; m<imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
										vals.addLast( String.valueOf( parseDateForSpecificDateFields( imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m), charId) ));
									}
									appendPlainElementValue(
											String.join(",", vals),
											null,
											charId,
											attributeValues,
											attributes,
											doc,
											propiedadesCaracteristicas);
								}
							}
						}
					}
					
					if( sku0 == null || "".equals(sku0)) {
						sku0 = sku;
					}else {
						if(sku == null || "".equals(sku)) {
							sku = sku0;
						}
					}
					keyValueSKU = doc.createElement("KeyValue");
					keyValueSKU.setAttribute("KeyID","SKUID");
					String skuval =  sku0;
					keyValueSKU.setTextContent( skuval );
					product.appendChild(keyValueSKU);
					prodToSKU.put(proposalId, skuval);
    				appendPlainElementValue(
							skuval,
							null,
							"SKU",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
					if(ean0 == null || "".equals(ean0)) {
						ean0 = mainBarCode;
					}else {
						if(mainBarCode == null || "".equals(mainBarCode)) {
							mainBarCode = ean0;
						}
					}
					keyValueEAN = doc.createElement("KeyValue");
					keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
					keyValueEAN.setTextContent(ean0);
					product.appendChild(keyValueEAN);
    				appendPlainElementValue(
							ean0,
							null,
							"SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode",
							attributeValues,
							attributes,
							doc,
							propiedadesCaracteristicas);
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					logE(e);
				}
				
	        	}
	        	if(rw.getXmm().listImmediateChildElements(product).get("Product") != null || ("SalesItem".equals(productType))) {
	        		products.appendChild(product);
	        		System.out.println("Added. (" + proposalId + ")");
	        		log("Added. (" + proposalId + ")");
	        	}else {
	        		log("Not added (" + proposalId + ")");
	        		System.out.println("Not added (" + proposalId + ")");
	        	}
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			java.util.LinkedList<Node> misProductos = rw.getXmm()
			        .listImmediateChildElements(products)
			        .get("Product");

			if(misProductos != null) {
			    for(Node n : misProductos) {
			        Element productElement = (Element) n;

			        if("SalesItemFamilyMkt".equals(productElement.getAttribute("UserTypeID"))) {
			            productElement.setAttribute("UserTypeID", "SalesItemFamily");
			        }
			    }
			}

			/*
			 * Esto debe pasar ANTES de serializar a StringWriter,
			 * no después.
			 */
			if(classifications.getParentNode() != null) {
			    spim.removeChild(classifications);
			}

			if(attributes.getParentNode() != null) {
			    spim.removeChild(attributes);
			}

			String fnO = java.nio.file.Paths
			        .get(fileSystemPrefixOMS.toString(), "pépele" + System.currentTimeMillis() + ".xml")
			        .toString();

			java.io.StringWriter writer = new java.io.StringWriter();

			transformer.transform(new DOMSource(doc), new StreamResult(writer));

			String xmlOutput = writer.getBuffer().toString()
			        .replace("&lt;CRLF&gt;", "&#13;&#10;")
			        .replace("<CRLF>", "&#13;&#10;");

			try {
			    writer.close();
			} catch(java.io.IOException e) {
			    logE(e);
			}

			try(java.io.PrintWriter pw =
			        new java.io.PrintWriter(
			                new java.io.OutputStreamWriter(
			                        new java.io.FileOutputStream(fnO),
			                        java.nio.charset.StandardCharsets.UTF_8))) {

			    pw.println(xmlOutput);

			} catch(java.io.IOException e) {
			    logE(e);
			}

			java.util.List<Node> ma = rw.getXmm()
			        .listImmediateChildElements(products)
			        .get("Product");

			System.out.println("Now going to send. " + (ma != null ? ma.size() : 0) + " on " + fnO);
			if(!productos.isEmpty())
			if(sendIt) {
				System.out.println("Sending FechaYHoraDePublicacion 111");

			    RestClient rc = new RestClient("Content-Type: application/xml", "Accept: application/xml");

			    try {
			        String sendResponse = null;
			        log("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
			            + "] (OMS) Request sent for "
			            + java.util.Arrays.asList(proposalIds)
			            + ": "
			            + (sendResponse = rc.getRequest("POST", urlDeOMS, xmlOutput))
			            + " ("
			            + fnO
			            + ")"
			        );
			        return aggregatedMessage.append("<;;>").append(fnO + "<::>" + sendResponse).toString();
			    } catch (IOException e) {
			        logE(e);
			    }
			}
		} catch (TransformerException e) {
			e.printStackTrace();
			logE(e);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			logE(e);
		}
        return null;
	}
	
	public static String stackTraceToString(Throwable t) {
	    if (t == null) {
	        return null;
	    }

	    java.io.StringWriter sw = new java.io.StringWriter();
	    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
	    t.printStackTrace(pw);
	    pw.flush();

	    return sw.toString();
	}
	
	private void addCharacteristicData(java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas, String baseUrl) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
			rw.addHeader("Authorization", RealExportProductsExpressOMS.rw.getRc().getHeader().get("Authorization"));
		}
		rw.putParameter("fields", 
				   "Characteristic.Identifier"
				+ ",CharacteristicLang.Name(es)"
				+ ",CharacteristicLang.Description(es)"
				+ ",Characteristic.DataType"
				+ ",Characteristic.Lookup->Lookup.Identifier"
				+ ",Characteristic.IsMultiValue"
				+ ",Characteristic.Purposes->LookupValue.Code"
				+ ",Characteristic.Order"
			);
		rw.putParameter("query", 
				  "Characteristic.ParentCharacteristic is empty"
			);
		rw.putParameter("orderBy", "0-ASC");
		rw.putParameter("pageSize", "2000");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		org.json.JSONObject detail = new org.json.JSONObject();
		org.json.JSONArray prevValues = null;
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(prevValues != null && !prevValues.getString(0).equals(values.getString(0)) && !propiedadesCaracteristicas.containsKey(prevValues.getString(0))) {
						detail.put("name", prevValues.getString(1));
						detail.put("description", prevValues.getString(2));
						detail.put("dataType", prevValues.getString(3));
						detail.put("lookup", prevValues.getString(4));
						detail.put("isMultiValue", prevValues.getString(5));
						detail.put("purposes", prevValues.getJSONArray(6));
						detail.put("order", prevValues.getString(7));
						propiedadesCaracteristicas.put(prevValues.getString(0), detail);
						if(detail.getJSONArray("purposes").length() == 1 && detail.getJSONArray("purposes").getString(0).equals(""))
							detail.getJSONArray("purposes").remove(0);
						detail = new org.json.JSONObject();
					}
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
			if(!propiedadesCaracteristicas.containsKey(prevValues.getString(0))) {
				propiedadesCaracteristicas.put(prevValues.getString(0), detail);
				if(detail.getJSONArray("purposes").length() == 1 && detail.getJSONArray("purposes").getString(0).equals(""))
					detail.getJSONArray("purposes").remove(0);
			}
			detail = null;
		}
	}
	
	private java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> buildDataMap(org.json.JSONArray cr){
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> data = new java.util.TreeMap<>();
		String id;
		org.json.JSONObject obj = null;
		java.util.LinkedList<org.json.JSONObject> lst = null;
		for(int i=0; i<cr.length(); i++) {
			obj = cr.getJSONObject(i);
			id = obj.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = data.get(id);
			if(lst == null) {
				lst = new java.util.LinkedList<>();
				data.put(id, lst);
			}
			lst.addLast(obj);
		}
		return data;
	}
	private String getSAPObjectType(org.json.JSONArray cr) {
		String id;
		org.json.JSONObject c;
		for(int i=0; i<cr.length(); i++) {
			c = cr.getJSONObject(i);
			id = c.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			if("SAPObjectType".equals(id)) {
				return c.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
			}
		}
		return null;
	}
	
	private Object parseDateForSpecificDateFields(Object value, String charId) {
		if(value == null)
			return null;
		String formato = mapaDeAtributosFechas.get(charId);
		if(formato != null) {
			try {
				log( value + " AGAINST " + formato + " FOR " + charId + ":  " + new java.text.SimpleDateFormat( formato ).format( new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse( ((String)value).replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.") ) ));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if(formato != null) {
			try {
				return new java.text.SimpleDateFormat( formato ).format( new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse( ((String)value).replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.") ) );
			}catch(java.text.ParseException e) {
				
			}
		}
		return value;
	}
	
	private void addGlobalData(java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas, java.util.Set<String> losQueSi, String baseUrl) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
			rw.addHeader("Authorization", RealExportProductsExpressOMS.rw.getRc().getHeader().get("Authorization"));
		}
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
			if(response != null) {
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

	@SuppressWarnings("deprecation")
	private String queryDictionary(String key, String dictionary) throws ServiceUnavailableException {
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			rawResponse = rw.makeRequest("GET", /* url = */ "/object/StandardizationValue/" + encode("'" + key + "'@'" + dictionary + "'")
					+ ""
					, null);
			response = new org.json.JSONObject(rawResponse);
			if(response.has("_data") && response.getJSONObject("_data").has("alternativeValue")) {
				return response.getJSONObject("_data").getString("alternativeValue");
			}
		}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException e){
			log("ERR: " + rawResponse);
		}catch(org.json.JSONException e) {
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
		Element assetCrossReference = doc.createElement("AssetCrossReference");
		org.json.JSONObject cc = null;
		String assetId = assetKeyPrefix + "-" + seedId + (assetKey != null ? assetKey : characteristic.getJSONObject("_qualification").getString("recordKey"));
		java.util.LinkedList<String> ids = assetReferencesMap.get(assetId);
		if(ids == null || !ids.contains(assetId)) {
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
		}
		Element asset = assetMap.get(assetId);
		Element assetName = null;
		Element assetValues = null;
		Element assetValue = null;
		java.util.LinkedList<String> referencesList = null;
		System.out.println( "AssetID: " + assetId );
		if(asset == null) {
			System.out.println("Appending asset");
			asset = doc.createElement("Asset");
			assetMap.put(assetId, asset);
			asset.setAttribute("ID", assetId);
			asset.setAttribute("UserTypeID", assetUserTypeId /* "Video" */);
			asset.setAttribute("Selected", "false");
			asset.setAttribute("Referenced", "true");
			if(name != null) {
    			assetName = doc.createElement("Name");
    			assetName.setTextContent(name);
    			asset.appendChild(assetName);
			}else {
				if(cc != null) {
	    			assetName = doc.createElement("Name");
	    			assetName.setTextContent(cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
	    			asset.appendChild(assetName);
				}else {
					cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_Name");
					if(cc != null) {
		    			assetName = doc.createElement("Name");
		    			assetName.setTextContent(cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
		    			asset.appendChild(assetName);
					}
				}
			}
			System.out.println("Name: " + name);
			assetValues = doc.createElement("Values");
			asset.appendChild(assetValues);
			assetValue = doc.createElement("Value");
			if(!"ProductVideo".equals(assetUserTypeId)) {
				assetValues.appendChild(assetValue);
			}
			assetValue.setAttribute("AttributeID", "getObjectType");
			assetValue.setTextContent(assetValueTextContent /* "Video Producto" */);
			assetValue = doc.createElement("Value");
			assetValue.setAttribute("AttributeID", assetValueAttributeId /* "VideoURL" */);
			if(url != null) {
				if("ProductImage".equals(assetUserTypeId)) {
					assetValue.setTextContent("largeImage=" + url + ",smallImage=" + url + ",thumbnail=" + url);
				}else {
					assetValue.setTextContent(url);
				}
				assetValues.appendChild(assetValue);
			}else {
				cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_URL");
				if(cc != null) {
					if("ProductImage".equals(assetUserTypeId)) {
						url = cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						assetValue.setTextContent("largeImage=" + url + ",smallImage=" + url + ",thumbnail=" + url);
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
				assetValue.setTextContent("sm-Imagen Producto,lg-Imagen Producto,xl-Imagen Producto");
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
	
	private void appendPlainElementValue(String textValue, String code, String attributeId, Element attributeValues, Element attributes, Document doc, java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas) throws ServiceUnavailableException {
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
		java.util.LinkedList<String> grupos = null;
		Element attribute = doc.createElement("Attribute");
		attribute.setAttribute("ID", attributeId);
		prop = propiedadesCaracteristicas.get(attributeId);
		Element metadataAttribute = doc.createElement("Name");
		metadataAttribute.setTextContent(prop != null && !prop.has("name") ? attributeId : prop != null ? prop.getString("name") : attributeId);
		attribute.appendChild(metadataAttribute);
		if(prop != null) {
			attribute.setAttribute("MultiValued", prop.has("IsMultiselect") ? "1".equals(prop.getString("IsMultiselect")) ? "true" : "false" : "false");
			attribute.setAttribute("Mandatory", prop.has("IsMandatory") ? "1".equals(prop.getString("IsMandatory")) ? "true" : "false" : "false");
//			if(!prop.has("name")) {
//				log("No Name found for: " + attributeId);
//			}else {
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
							valueElement = doc.createElement("Value");
							valueElement.setTextContent("true");
							valueElement.setAttribute("ID", "Y");
							valueElement.setAttribute("AttributeID", purposes.getString(i));
							metaData.appendChild(valueElement);
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
				}
//			}
		}else {
			// PANIC
			log("PANIC: No property was found for characteristic: " + attributeId);
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

	private static final Logger LOGGER = Logger.getLogger(RealExportProductsExpressOMS.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/real_export_products_oms-%g.log", 5 * 1024 * 1024, 5, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    java.time.LocalDateTime dateTime =
                        java.time.Instant.ofEpochMilli(record.getMillis())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();

                    String timestamp = dateTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }
    
	private void log(String message){
		LOGGER.info(message);
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get("..","logs","real_export_products_oms.log").toString(), true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}

//	private static void v() {

		// <ClassificationReference ClassificationID="catst19895633" Type="WebsiteLink" Changed="true"/>
//	}
	
	private static java.util.Set<String> YEA; /* = new java.util.TreeSet<>(java.util.Arrays.asList((
			  "SKU\r\n"
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
	
	/*
	public static void main(String[] args) {
		
		RESTWorkshop rw = new RESTWorkshop();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Active"));
		String[] pieces = null;
		for(String tupla : lineasDireccionTallaWeb) {
			pieces = tupla.split("\t");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'RelAttribTallaATG'")).put("values", new org.json.JSONArray().put(pieces[1]).put(true)));
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
		if(response == null) {
			System.out.println("Error: " + rw.getRawResponse());
		}else {
			System.out.println("Yosh: " + response);
		}
	}
	*/
	
	/*
	public static void main(String[] args) {
		
		RESTWorkshop rw = new RESTWorkshop();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Active"));
		String[] pieces = null;
		for(String tupla : lineasMapaDeDirecciones) {
			pieces = tupla.split("\t");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'RelAttribSTDATG'")).put("values", new org.json.JSONArray().put(pieces[1]).put(true)));
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
		if(response == null) {
			System.out.println("Error: " + rw.getRawResponse());
		}else {
			System.out.println("Yosh: " + response);
		}
	}
	*/

	/*
	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Value"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Active"));
		
		java.util.Map<String, String> yeah = new java.util.TreeMap<>();
		String[] pieces = null;
		for(String tupla : lineasMapaDeDirecciones) {
			pieces = tupla.split("\t");
			yeah.put(pieces[1], pieces[0]);
		}
		
		pieces = null;
		for(String tupla : lineasDireccionTallaWeb) {
			pieces = tupla.split("\t");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'RelAttribTallaATG'")).put("values", new org.json.JSONArray().put( yeah.get(pieces[0]) ).put(pieces[1]).put(true)));
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
		if(response == null) {
			System.out.println("Error: " + rw.getRawResponse());
		}else {
			System.out.println("Yosh: " + response);
		}
	}
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
//	
//	public static final java.lang.String[] lineasDireccionTallaWeb = (
//			  "TallaZapatos	clothingSize\r\n"
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
			if(response != null && response.has("rows") && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0;i<rows.length();i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					mapa.put(values.getString(0), values.getString(1));
				}
				currentIndex += response.getInt("pageSize");
			}else {
//				System.out.println(rw.getRc().getHeader());
				System.out.println(rw.getRawResponse());
				if(rw.getException() != null) {
//					logE(rw.getException());
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
		System.out.println("Loading...");
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0;i<rows.length();i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					mapaDeDirecciones.put(values.getString(0), values.getString(1));
				}
				currentIndex += response.getInt("pageSize");
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		System.out.println("Loaded... " + mapaDeDirecciones.size());
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
					values = rows.getJSONObject(i).getJSONArray("values");
					mapa.put(values.getString(0), values.getString(1));
				}
				currentIndex += response.getInt("pageSize");
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
					values = rows.getJSONObject(i).getJSONArray("values");
					mapa.add(values.getString(0));
				}
				currentIndex += response.getInt("pageSize");
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return mapa;
	}
	
	static {
		try {
			mapaDeDirecciones = loadFieldDictionaries();
			mapaDeDireccionesAtributoTallaWeb = loadFieldTallaATG();
			mapaDeAtributosFechas = loadAtributosFecha();
			System.out.println("Oki");
			YEA = loadInheritedFields();
			System.out.println("Oki2");
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}

}
