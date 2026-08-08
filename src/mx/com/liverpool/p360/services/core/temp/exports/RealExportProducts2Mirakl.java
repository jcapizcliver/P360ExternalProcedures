package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class RealExportProducts2Mirakl {

	private static final String urlDeMkt = PropertiesManager.get("p360.contingency.out.url_mkt"); // "http://172.16.203.141:7287/synch/products";
	private static final String urlDeMktStockout = PropertiesManager.get("p360.contingency.out.url_mkt_stockout"); // "http://172.16.203.141:7287/synch/products/entradaUnica";
	private static final String encoded = 
	    	PropertiesManager.get("p360.contingency.basic_token_auth");
	private static final String fileSystemPrefix = 
//			"C:\\opt\\LVP\\tmp\\";
//			"/P360shared/IDMC/stage/ToMarketplace/";
			java.nio.file.Paths.get("..","stage","ToMarketplace").toString();
	private static final String fileSystemPrefixLvp = 
			java.nio.file.Paths.get("..","stage","ToMultioferta").toString();
//			"/P360shared/IDMC/stage/ToMultioferta/";
//			"C:\\opt\\LVP\\tmp\\";
	private static final String baseUrlDEV = 
			PropertiesManager.get("p360.contingency.base_url");
//			"http://172.18.237.162:1512/rest/V2.0";
//			 "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded, "Accept-Language: es");

	private static final RESTWrapper wrapper = new RESTWrapper();
	private static final RESTWorkshop rw = wrapper.getRw();
	

	private org.json.JSONObject getMeTheCompa(String compa) throws ServiceUnavailableException{
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/object/Product2G/'" + compa + "'@'MASTER'?entityFilter=Product2GLang,Product2GStructureGroupMap,Product2GCharacteristicValue,Product2G,ProductExtraData&includeIds=true&includeLabels=true", null);
			response = new org.json.JSONObject(rawResponse);
		} catch (org.json.JSONException | IOException e) {
			logE(e);
		}
		return response;
	}
	
	private static final String USAGE = "Usage: RealExportProducts2MiraklJdbcBatch <File with IDs or SKUs> -t ID|SKU [-s]\n-t indicates which type of content is in the file: SKU or Proposal IDs\n-s if present, indicates to send the data to destination, default is not send the data.";

	private int products = 0;
	private int dropped = 0;

	private static final long MAX_BATCH_BYTES = 7L * 1024L * 1024L;
	private final java.util.List<String> generatedMarketplaceFiles = new java.util.ArrayList<>();
	private final java.util.List<String> marketplaceBrokerResponses = new java.util.ArrayList<>();
	private boolean marketplaceBrokerFailure = false;
	private boolean marketplaceUnrecognizedResponse = false;
	private final java.util.Map<String, java.util.Map<String, org.json.JSONObject>> templateMetadataSet = new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Map<String, String>> templateStructureGroupAttributeValues = new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Set<String>> templateSets = new java.util.TreeMap<>();
	private final java.util.Map<String, org.json.JSONObject> globalProperties = new java.util.TreeMap<>();
	private final java.util.Set<String> globalSet = new java.util.TreeSet<>();
	private final java.util.Map<Integer, org.json.JSONObject> characteristicMetadataByID = new java.util.HashMap<>();
	private final java.util.Map<Integer, String> characteristicIdentifierByID = new java.util.HashMap<>();

	private final ELog dbLog = new ELog() {
		@Override
		public void logE(Exception e) {
			RealExportProducts2Mirakl.this.logE(e);
		}

		@Override
		public void log(String message) {
			RealExportProducts2Mirakl.this.log(message);
		}
	};
	private final RealExportProductsUtils rutils = new RealExportProductsUtils(dbLog);
	private final DBAccessDataStub dastub = new DBAccessDataStub(dbLog);

	public RealExportProducts2Mirakl() {
		
	}
	
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
		RealExportProducts2Mirakl o = new RealExportProducts2Mirakl();
		o.log("Iniciandoleeeeee");
		try{
			o.logger = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get("..", "logs", "REP2M.log").toFile(), true )));
		}catch(java.io.IOException e) {
			o.logE(e);
		}
		System.out.println("Me arranco " + type + " (sobre: " + data.length + " elementos del archivo)");
		o.log("Me arranco " + type + " (sobre: " + data.length + " elementos del archivo)");
		if (type == 0) {
			o.doIt(cleanIds(data), send, baseUrlDEV);
		} else if (type == 1) {
			java.util.List<String> resolvedIds = new java.util.ArrayList<>();
			for (String skuValue : data) {
				String resolvedId = o.getIdFromSKU(skuValue);
				if (resolvedId != null && !resolvedId.isBlank()) {
					resolvedIds.add(resolvedId);
				}
			}
			o.doIt(resolvedIds.toArray(new String[0]), send, baseUrlDEV);
		}
				o.logger.close();
		o.log("Total: " + o.products + " (dropped: " + o.dropped +  ")");
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
		// El corte real se realiza dentro de doIt con base en bytes UTF-8.
	}
	
	public static void runForProductIds(String[] proposalIds, boolean send)
			throws ServiceUnavailableException {
		runForProductIdsWithResult(proposalIds, send);
	}

	public static ExportRunResult runForProductIdsWithResult(String[] proposalIds, boolean send)
			throws ServiceUnavailableException {
		String[] data = cleanIds(proposalIds);
		RealExportProducts2Mirakl exporter = new RealExportProducts2Mirakl();
		exporter.log("Iniciando RealExportProducts2MiraklJdbcBatch desde arreglo de IDs: " + data.length);
		String rawResult = null;
		try {
			rawResult = exporter.doIt(data, send, baseUrlDEV);
			exporter.log("Total: " + exporter.products + " (dropped: " + exporter.dropped + ")");
		} finally {
			if (exporter.logger != null) {
				exporter.logger.close();
			}
		}

		boolean success =
				send
				&& !exporter.generatedMarketplaceFiles.isEmpty()
				&& !exporter.marketplaceBrokerFailure
				&& !exporter.marketplaceUnrecognizedResponse
				&& exporter.marketplaceBrokerResponses.size()
					== exporter.generatedMarketplaceFiles.size();

		return new ExportRunResult(
				rawResult,
				exporter.generatedMarketplaceFiles,
				exporter.marketplaceBrokerResponses,
				success,
				exporter.marketplaceUnrecognizedResponse);
	}

	public static final class ExportRunResult {
		private final String rawResult;
		private final java.util.List<String> payloadFiles;
		private final java.util.List<String> brokerResponses;
		private final boolean successful;
		private final boolean unrecognizedResponse;

		private ExportRunResult(
				String rawResult,
				java.util.List<String> payloadFiles,
				java.util.List<String> brokerResponses,
				boolean successful,
				boolean unrecognizedResponse) {
			this.rawResult = rawResult;
			this.payloadFiles = java.util.Collections.unmodifiableList(
					new java.util.ArrayList<>(payloadFiles));
			this.brokerResponses = java.util.Collections.unmodifiableList(
					new java.util.ArrayList<>(brokerResponses));
			this.successful = successful;
			this.unrecognizedResponse = unrecognizedResponse;
		}

		public String getRawResult() {
			return rawResult;
		}

		public java.util.List<String> getPayloadFiles() {
			return payloadFiles;
		}

		public java.util.List<String> getBrokerResponses() {
			return brokerResponses;
		}

		public boolean isSuccessful() {
			return successful;
		}

		public boolean hasUnrecognizedResponse() {
			return unrecognizedResponse;
		}
	}
		private static String[] cleanIds( String[] ids )
		{
		  java.util.Set<String> clean = new java.util.LinkedHashSet<String>();

		  if ( ids != null )
		  {
		    for ( int i = 0; i < ids.length; i++ )
		    {
		      String id = ids[i];

		      if ( id != null )
		      {
		        id = id.trim();
		      }

		      if ( id != null && id.length() > 0 )
		      {
		        clean.add( id );
		      }
		    }
		  }

		  return clean.toArray( new String[clean.size()] );
		}
		
	private boolean loaded = false;
	
	private void init(){
		if(loaded)
			return;
		
//		articulosEnviados.clear();
//		atgGroups.clear();
		characteristicMetadataByID.clear();
		characteristicIdentifierByID.clear();
//		rootCharacteristicIDs.clear();
		globalProperties.clear();
		globalSet.clear();
		templateMetadataSet.clear();
		templateStructureGroupAttributeValues.clear();
		templateSets.clear();
		
		loadDatabaseDictionaries();
		System.out.println("Loading characteristic metadata");
		loadCharacteristicMetadata();
		System.out.println("Characteristic metadata loaded");
		System.out.println("Adding global metadata");
		addGlobalData(globalProperties, globalSet);
		System.out.println("Global metadata added");
		addCharacteristicData(globalProperties);
		
		loaded = true;
	}
		
	public String doIt(String[] proposalIds, boolean send, String baseUrl) throws ServiceUnavailableException {
		return doIt(proposalIds, send);
	}
	
	private java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> buildDataMap(org.json.JSONArray cr) {
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> data = new java.util.TreeMap<>();
		String id;
		org.json.JSONObject obj = null;
		java.util.LinkedList<org.json.JSONObject> lst = null;
		for (int i = 0; i < cr.length(); i++) {
			obj = cr.getJSONObject(i);
			id = obj.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = data.get(id);
			if (lst == null) {
				lst = new java.util.LinkedList<>();
				data.put(id, lst);
			}
			lst.addLast(obj);
		}
		return data;
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

	private String grabDescLong2(org.json.JSONArray lang) {
		for (int i = 0; i < lang.length(); i++) {
			if (10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("descriptionLong2")
						? lang.getJSONObject(i).getString("descriptionLong2")
						: null;
			}
		}
		return null;
	}

	private String grabProductDescriptionShort(org.json.JSONArray lang) {
		for (int i = 0; i < lang.length(); i++) {
			if (10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("descriptionShort")
						? lang.getJSONObject(i).getString("descriptionShort")
						: null;
			}
		}
		return null;
	}
	
	private String grabProductName(org.json.JSONArray lang) {
		for(int i=0; i<lang.length(); i++) {
			if(10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key") ) {
				return lang.getJSONObject(i).has("productName") ? lang.getJSONObject(i).getString("productName") : null;
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public String doIt(String[] proposalIds, boolean send) throws ServiceUnavailableException {
		log("Running using baseUrlDEV: " + baseUrlDEV);
		log("Running using fileSystemPrefixLVP: " + fileSystemPrefixLvp);
		log("Running using fileSystemPrefixMKT: " + fileSystemPrefix);
		log("Going over: " + proposalIds.length + " proposalIds");
		System.out.println("Going over: " + proposalIds.length + " proposalIds");
		try(dastub){
			init();
			String proposalId = null;
			StringBuilder result = new StringBuilder();
			int batchNumber = 1;
			try {
				MiraklExportContext batch = createMiraklExportContext();
				boolean procede;
				boolean brk = false;
				for(int index = 0; index<proposalIds.length; index++) {
					MiraklExportContext current = createMiraklExportContext();
					Document doc = current.doc;
					Element spim = current.spim;
					Element attributes = current.attributes;
					Element assets = current.assets;
					Element products = current.products;
					java.util.Map<String, Element> assetMap = current.assetMap;
					java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap = current.assetReferencesMap;
					Document docMKT = current.docMKT;
					Element spimMKT = current.spimMKT;
					Element attributesMKT = current.attributesMKT;
					Element assetsMKT = current.assetsMKT;
					Element productsMKT = current.productsMKT;
					java.util.Map<String, Element> assetMapMKT = current.assetMapMKT;
					java.util.Map<String, java.util.LinkedList<String>> assetReferencesMapMKT = current.assetReferencesMapMKT;
					java.util.LinkedList<String> productosLiverpool = current.productosLiverpool;
					java.util.LinkedList<String> productosMarketplace = current.productosMarketplace;
					org.json.JSONObject reqPublishMessage = current.reqPublishMessage;
					current.currentProposalId = proposalIds[index];
					try {
					procede = true;
					proposalId = proposalIds[index];
					java.nio.file.Path p = java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.migration.to_skip_directory"), proposalId );
					if(java.nio.file.Files.exists(p)) {
						log("Skipped to be sent since this was reciently migrated --->" + proposalId + "<---");
						reqPublishMessage.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@1")).put("values", new org.json.JSONArray().put( "Registro recién migrado, si persiste, solicitar mantenimiento manual" )));
						continue;
					}
					// talla normalizada hacia ATG debe de salir como TC-NormalizedSize
	//				final String[] productsToTestWith = new String[] {proposalId};
					org.json.JSONObject rp = getMeTheCompa(proposalId);
					if(rp == null)
						continue;
					String template = !rp.getJSONObject("_data").has("structureGroupMap") ? null : getPrimaryProductTaxonomyTemplate(rp.getJSONObject("_data").getJSONArray("structureGroupMap")); // rp.getJSONObject("_data").getJSONArray("structureGroupMap").getJSONObject(0).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
					if(template == null) {
						log("No template found for " + proposalId);
						reqPublishMessage.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@1")).put("values",new org.json.JSONArray().put(  "Sin plantilla" )));
						continue;
					}
					java.util.LinkedList<org.json.JSONObject> lst = null;
					java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> dataMap = buildDataMap( rp.getJSONObject("_data").getJSONArray("_characteristicRecords") );
					String itemId = rp.getJSONObject("_entityItem").getString("_externalId").split("@")[0].replaceAll("^'|'$", "");
					org.json.JSONArray characteristicArray = rp.getJSONObject("_data").getJSONArray("_characteristicRecords");
					String business = rp.getJSONObject("_data").has("business") ? rp.getJSONObject("_data").getJSONObject("business").getString("_code") : getMeTheBusiness(characteristicArray);
					String baseSAPObjectType = getSAPObjectType(characteristicArray);
					String descLong = rp.getJSONObject("_data").has("lang") ? grabDescLong( rp.getJSONObject("_data").getJSONArray("lang") ) : null;
					String productName = rp.getJSONObject("_data").has("lang") ? grabProductName( rp.getJSONObject("_data").getJSONArray("lang") ) : null;
					
					if("LVP".equals(business)) {
						productosLiverpool.addLast(proposalId);
					} else if ("MKP".equals(business)) {
						productosMarketplace.addLast(proposalId);
					}else {
						continue;
					}
					
					String descLong2 = rp.getJSONObject("_data").has("lang")
							? grabDescLong2(rp.getJSONObject("_data").getJSONArray("lang"))
							: null;
					String nameLang = rp.getJSONObject("_data").has("lang")
							? grabProductDescriptionShort(rp.getJSONObject("_data").getJSONArray("lang"))
							: null;
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
					String supplierIDLabel = null;
					String mainBarCode = null;
					String sku = null;
					String embeddedCodeWEB = null;
					String embeddedCodeWAP = null;
					String refundPolicy = null;
					
					String clothingSize = null;
					String sizeVaD = null;
					
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
					
					if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
							.getJSONArray("productExtraData").getJSONObject(0).has("brandName")) {
						brandName = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("brandName").getString("_code");
						brandNameLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("brandName").getString("_label");
					}
					if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
							.getJSONArray("productExtraData").getJSONObject(0).has("brandIdS4H")) {
						brandIdS4H = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("brandIdS4H").getString("_code");
						brandIdS4HLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("brandIdS4H").getString("_label");
					}
					if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
							.getJSONArray("productExtraData").getJSONObject(0).has("itemGroup")) {
						itemGroup = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("itemGroup").getString("_code");
						itemGroupLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("itemGroup").getString("_label");
					}
					if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
							.getJSONArray("productExtraData").getJSONObject(0).has("itemGroupS4H")) {
						itemGroupS4H = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("itemGroupS4H").getString("_code");
						itemGroupS4HLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("itemGroupS4H").getString("_label");
					}
					if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
							.getJSONArray("productExtraData").getJSONObject(0).has("direction")) {
						direccion = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("direction").getString("_code");
						direccionLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("direction").getString("_label");
					}
					if (rp.getJSONObject("_data").has("productExtraData")
							&& rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("section")) {
						seccion = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("section").getString("_code");
						seccionLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getJSONObject("section").getString("_label");
					}
					if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
							.getJSONArray("productExtraData").getJSONObject(0).has("supplierID")) {
						supplierID = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("supplierID").getString("_code");
						supplierIDLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("supplierID").getString("_label");
					}
					if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
							.getJSONArray("productExtraData").getJSONObject(0).has("supplierPartNumber")) {
						supplierPartNumber = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
								.getString("supplierPartNumber");
					}
					if (rp.getJSONObject("_data").has("gtin")) {
						mainBarCode = rp.getJSONObject("_data").getString("gtin");
					}
					if (rp.getJSONObject("_data").has("sku")) {
						sku = String.valueOf(rp.getJSONObject("_data").getLong("sku"));
					}
					if (rp.getJSONObject("_data").has("embeddedCodeWEB")) {
						embeddedCodeWEB = rp.getJSONObject("_data").getString("embeddedCodeWEB");
					}
					if (rp.getJSONObject("_data").has("embeddedCodeWAP")) {
						embeddedCodeWAP = rp.getJSONObject("_data").getString("embeddedCodeWAP");
					}
					if (rp.getJSONObject("_data").has("refundPolicy")) {
						refundPolicy = rp.getJSONObject("_data").getString("refundPolicy");
					}
						
					try {
						productType = 
								"00".equals(baseSAPObjectType) ? "MKP".equals(business) ? "SalesItemFamilyMkt" : "SalesItem" : 
									"01".equals(baseSAPObjectType) ? "MKP".equals(business) ? "SalesItemFamilyMkt" : "SalesItemFamily" : "MKP".equals(business) ? "SalesItemFamilyMkt" : "SalesItemFamily";
						String charId = null;
						raw = rw.makeRequest("GET", "/list/Article/byProducts"
								+ "?fields="
									+ java.net.URLEncoder.encode(
											  "Article.SupplierAID,"
											+ "ProductReference.ReferencedSupplierAid(\"" + itemId + "\")"
											,"UTF-8")
								+ "&products=" + java.net.URLEncoder.encode("'" + itemId + "'@1", "UTF-8"), null);
						org.json.JSONObject resp = new org.json.JSONObject(raw);
						upperRows = resp.getJSONArray("rows");
						for(int a = 0; a<upperRows.length(); a++) {
							try{
								firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
							}catch(org.json.JSONException e) {
							}
							raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData", null);
							resp = new org.json.JSONObject(raw);
							resp = resp.getJSONObject("_data"); if (!resp.has("_characteristicRecords")) { System.out.println("No characteristics present for " + firstVariant); log("No characteristics present for " + firstVariant); }
							rows = resp.has("_characteristicRecords") ? resp.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
							org.json.JSONArray children = null;
							String[] chunk = null;
							for(int b = 0; b < rows.length(); b++) {
								imageObject = rows.getJSONObject(b);
								charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								if("SalesItem".equals(productType) && "ProductImage2".equals(charId)) {
									children = imageObject.getJSONArray("_children");
									piKey = imageObject.getJSONObject("_qualification").getString("recordKey");
									for(int c = 0; c<children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
										if("ProductImage_Name2".equals(charId)) {
											piName = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}else if("ProductImage_URL2".equals(charId)) {
											piUrl = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}
									}
								}else if("SalesItem".equals(productType) && "ProductImageDetail2".equals(charId)) {
									children = imageObject.getJSONArray("_children");
									chunk = new String[3];
									chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
									for(int c = 0; c<children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
										if("ProductImageDetail_Name2".equals(charId)) {
											chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}else if("ProductImageDetail_URL2".equals(charId)) {
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
									
								}else if("SalesItem".equals(productType) && "SKU".equals(charId)) {
								}else if("SalesItem".equals(productType) && ("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId))) {
								}else {
									if("ProductImageDeatail2".equals(charId)) {
										log("Anyway ProductImageDetail2: " + productType);
									}else if("ProductImage2".equals(charId)) {
										log("Anyway ProductImage2: " + productType);
									}
								}
							}
						}
						if("SalesItem".equals(productType) && ( piName == null || piUrl == null )) {
							log("(" + business + ") " + productType);
							log("(" + business + ") No tenía imágenes2: " + proposalId);
							log("(" + business + ") Had: " + upperRows.length());
							log("(" + business + ") Raw: " + upperRows);
							reqPublishMessage.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@1")).put("values", new org.json.JSONArray().put( "Sin imágenes \"congeladas\"" )));
							continue;
						}
					} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
						logE(e);
					}
					java.util.Set<String> atributosGeneralesQueSi = templateSets.get(template);
					java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = templateMetadataSet.get(template);
					String brandCode = null;
					if (propiedadesCaracteristicas == null) {
						propiedadesCaracteristicas = new java.util.TreeMap<>();
						atributosGeneralesQueSi = new java.util.TreeSet<>();
						templateSets.put(template, atributosGeneralesQueSi);
						atributosGeneralesQueSi.addAll(globalSet);
						templateMetadataSet.put(template, propiedadesCaracteristicas);
						for (java.util.Map.Entry<String, org.json.JSONObject> entry : globalProperties.entrySet()) {
							org.json.JSONObject copied = copyCharacteristicMetadata(entry.getValue());
							java.util.Iterator<?> propertyKeys = entry.getValue().keys();
							while (propertyKeys.hasNext()) {
								String propertyKey = String.valueOf(propertyKeys.next());
								if (!copied.has(propertyKey)) {
									copied.put(propertyKey, entry.getValue().get(propertyKey));
								}
							}
							propiedadesCaracteristicas.put(entry.getKey(), copied);
						}
						java.util.Map<String, org.json.JSONObject> templateProperties =
								dastub.getTemplateCharacteristicProperties(template);
						propiedadesCaracteristicas.putAll(templateProperties);
						for (java.util.Map.Entry<String, org.json.JSONObject> entry : templateProperties.entrySet()) {
							if ("Y".equals(entry.getValue().optString("RelevantForATG", ""))) {
								atributosGeneralesQueSi.add(entry.getKey());
							}
						}
						templateStructureGroupAttributeValues.put(template,
								dastub.getTemplateStructureGroupAttributeValues(template, 10));
					}
		        	Element product = null;
	
		        	if("MKP".equals(business)) {
		        		product = docMKT.createElement("Product");
		        		product.setAttribute("ID", proposalId);
		        		product.setAttribute("UserTypeID", productType );
		        		product.setAttribute("ParentID", template);
		        		product.setAttribute("Changed", "true");
		        	}else {
		        		product = doc.createElement("Product");
		        		product.setAttribute("ID", proposalId);
		        		product.setAttribute("UserTypeID", productType );
		        		product.setAttribute("ParentID", template);
		        		product.setAttribute("Changed", "true");
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
					String almacenamientoAtt = null;
					String skuType = null;
					String mtart = null;
	
					String pt = null;
					String ptl = null;
	
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
					java.util.Map<String, String> structureAttributeValues =
							templateStructureGroupAttributeValues.get(template);
					if (structureAttributeValues == null) {
						structureAttributeValues = dastub.getTemplateStructureGroupAttributeValues(template, 10);
						templateStructureGroupAttributeValues.put(template, structureAttributeValues);
					}
					for (String attributeName : new String[] {
							"DisplayGroupOrder", "DisplayOrder", "ConfigurableOrder" }) {
						if (!structureAttributeValues.containsKey(attributeName)) {
							continue;
						}
						appendPlainElementValue(structureAttributeValues.get(attributeName), null, attributeName,
								attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
								"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
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
	
//					String sku_ = rp.getJSONObject("_data").has("sku") ? String.valueOf( rp.getJSONObject("_data").getLong("sku") ) : null;
					String ean_ = rp.getJSONObject("_data").has("gtin") ? rp.getJSONObject("_data").getString("gtin") : null;
	//				System.out.println("---------------------------------------------->" + sku_ + " || " + proposalId + rp.getJSONObject("_data"));
					if (descLong != null) {
						appendPlainElementValue(descLong, null, "DescriptionLong", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
								"MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
					if (descLong2 != null) {
						appendPlainElementValue(descLong2, null, "DescriptionLong2", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
								"MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
		        	for(int i = 0; i<characteristicArray.length(); i++) {
		        		characteristic = characteristicArray.getJSONObject(i);
		        		charId = characteristic.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
		        		if("clothingSize".equals(charId)) {
							clothingSize = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						}else if("sizeVaD".equals(charId)) {
							sizeVaD = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						}
		        		if("Business".equals(charId)) {
	        				if("SBB".equals(business)) {
	        					log("Returning since business is SBB.");
	        					brk = true;
	        					break;
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
	        			}else
		        		if("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
		        			if(ean_ == null || "".equals(ean_)) {
		        				ean_ = treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
		        			}
		        		}else
		        		if("SKU".equals(charId)) {
		        			if(sku == null || "".equals(sku)) {
		        				sku = treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
		        			}
		        		}else
		        		if("ProductVideo2".equals(charId)) {
		        			if(characteristic.has("_children")) {
			        			org.json.JSONArray children = characteristic.getJSONArray("_children");
								String[] chunk = new String[3];
								chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
								for(int c = 0; c<children.length(); c++) {
									charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
									if("ProductVideo_Name2".equals(charId)) {
										chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
									}else if("ProductVideo_URL2".equals(charId)) {
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
		        		}else if("OwnersManual2".equals(charId)) {
		        			if(characteristic.has("_children")) {
		        			org.json.JSONArray children = characteristic.getJSONArray("_children");
								String[] chunk = new String[3];
								chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
								for(int c = 0; c<children.length(); c++) {
									charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
									if("OwnersManual_Name2".equals(charId)) {
										chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
									}else if("OwnersManual_URL2".equals(charId)) {
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
		        		}else if("NOM2".equals(charId)) {
		        			if(characteristic.has("_children")) {
			        			org.json.JSONArray children = characteristic.getJSONArray("_children");
								String[] chunk = new String[3];
								chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
								for(int c = 0; c<children.length(); c++) {
									charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
									if("NOM_Name2".equals(charId)) {
										chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
									}else if("NOM_URL2".equals(charId)) {
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
		        			if ("ProductName".equals(charId)) {
								productName = productName != null && !"".equals(productName) ? productName
										: (productName = characteristic.getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getString(0));
							} else if ("Name".equals(charId)) {
								charactName = characteristic.getJSONArray("_recordLang").getJSONObject(0)
										.getJSONArray("values").getString(0);
							} else if("ItemGroupS4H".equals( charId ) || "ItemGroup".equals( charId )) {
		        				if(isBannedForMarketplace(characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"), "ItemGroups", "MATKLLOV")) {
	        						log("Returning since the item group was in the no send to mkt list.");
	        						brk = true;
		        					break;
		        				}
		        				if("ItemGroupS4H".equals( charId )) {
			        				itemGroupS4H = itemGroupS4H == null || "".equals(itemGroupS4H)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_code")
											: itemGroupS4H;
									itemGroupS4HLabel = itemGroupS4HLabel == null || "".equals(itemGroupS4HLabel)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_label")
											: itemGroupS4HLabel;
		        				} else {
									itemGroup = itemGroup == null || "".equals(itemGroup)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_code")
											: itemGroup;
									itemGroupLabel = itemGroupLabel == null || "".equals(itemGroupLabel)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_label")
											: itemGroupLabel;
		        				}
		        				if (!behvo) {
			        				String elese = characteristic.getJSONArray("_recordLang").getJSONObject(0)
			        						.getJSONArray("values").getJSONObject(0).getString("_code");
			        				String dictionary = "ItemGroup".equals(charId)
			        						? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H";
			        				String laetiqueta = queryDictionary(elese, dictionary);
			        				if (laetiqueta != null && !laetiqueta.isBlank()) {
			        					String elcode = dastub.getLookupValueCodeByName(
			        							"SAP_BEHVOLOV", 10, laetiqueta, true);
			        					if (elcode != null && !elcode.isBlank()) {
			        						appendPlainElementValue(laetiqueta, elcode, "SAP_BEHVO",
			        								attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
			        								"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
			        						behvo = true;
			        					}
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
		        			}else if("BrandName".equals(charId) || "BRAND_ID_S4H".equals(charId)) {
		        					if(isBannedForMarketplace(characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"), "Brands", "ZCOMALOV")) {
		        						log("Returning since brand was in no send to mkt list.");
		        						continue;
		        					}
		        					brandName = brandName == null || "".equals(brandName)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
													.getJSONObject(0).getString("_code")
											: brandName;
									brandNameLabel = brandNameLabel == null || "".equals(brandNameLabel)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
													.getJSONObject(0).getString("_label")
											: brandNameLabel;
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
		        			} else if ("ProductType".equals(charId)) {
								pt = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
										.getJSONObject(0).getString("_code");
								ptl = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
										.getJSONObject(0).getString("_label");
								if ("1".equals(pt)) {
									lst = dataMap.get("ItemGroupS4H");
									if (lst != null) {
										itemGroupS4H = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getJSONObject(0).getString("_code");
									}
									lst = dataMap.get("AlmacenamientoAtt");
									if (lst != null) {
										almacenamientoAtt = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getJSONObject(0).getString("_code");
									}
									lst = dataMap.get("MTART_S4H");
									if (lst != null) {
										mtart = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getJSONObject(0).getString("_code");
									}
									lst = dataMap.get("SkuType");
									if (lst != null) {
										skuType = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getJSONObject(0).getString("_code");
									}
									if ("SERV".equals(skuType) && "0001".equals(almacenamientoAtt)) {
										pt = "6";
										ptl = "Digital";
										appendPlainElementValue("Digital", pt, "ProductType", attributeValues, attributes,
												doc, propiedadesCaracteristicas);
									} else if ("DIEN".equals(mtart) && "SB87516".equals(itemGroupS4H)) {
										pt = "6";
										ptl = "Digital";
										appendPlainElementValue("Digital", pt, "ProductType", attributeValues, attributes,
												doc, propiedadesCaracteristicas);
	
									} else {
										appendPlainElementValue(
												characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_label"),
												characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_code"),
												"ProductType", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas
												);
									}
								} else {
									appendPlainElementValue(
											characteristic.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_label"),
											characteristic.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_code"),
											"ProductType", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
								}
							} else {
		        				if("ItemGroup2".equals(charId)) {
									continue;
								}else if(atributosGeneralesQueSi.contains(charId)) {
									if(unosQueQuiero.contains(charId)) {
										heredables.put(charId, characteristic);
									}
									if("LOOKUP".equals(characteristic.getString("_datatype"))){
										boolean skip = false;
										if("Direction".equals(charId) && direccion != null && !"".equals(direccion)) {
											skip = true;
										}
										if("Section".equals(charId) && seccion != null && !"".equals(seccion)) {
											skip = true;
										}
										if("ItemGroup".equals(charId) && itemGroup != null && !"".equals(itemGroup)) {
											skip = true;
										}
										if("ItemGroupS4H".equals(charId) && itemGroupS4H != null && !"".equals(itemGroupS4H)) {
											skip = true;
										}
										if("BrandName".equals(charId) && brandName != null && !"".equals(brandName)) {
											skip = true;
										}
										if("BRAND_ID_S4H".equals(charId) && brandIdS4H != null && !"".equals(brandIdS4H)) {
											skip = true;
										}
										if(!skip) {
											appendPlainElementValue(
													characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
													characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
													charId,
													attributeValues,
													"MKP".equals(business) ? attributesMKT : attributes,
													"MKP".equals(business) ? docMKT : doc,
													propiedadesCaracteristicas);
										}
									}else if(!"NONE".equals(characteristic.getString("_datatype"))) {
										boolean skip = false;
										java.util.LinkedList<String> vals = new java.util.LinkedList<>();
										for(int m=0; m<characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
											vals.addLast( String.valueOf( parseDateForSpecificDateFields( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m), charId) ));
										}
										if("SupplierID".equals(charId) && supplierID != null && !"".equals(supplierID)) {
											skip = true;
										}
										
										if(!skip){
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
		        	}
		        	if(brk) {
		        		continue;
		        	}
		        	if (embeddedCodeWEB != null && !"".equals(embeddedCodeWEB)) {
						appendPlainElementValue(embeddedCodeWEB, null, "EmbedCodeWEB", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
					if (embeddedCodeWAP != null && !"".equals(embeddedCodeWAP)) {
						appendPlainElementValue(embeddedCodeWAP, null, "EmbedCodeWAP", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
					if (refundPolicy != null && !"".equals(refundPolicy)) {
						appendPlainElementValue(refundPolicy, null, "refundPolicy", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
					if (seccion != null && !"".equals(seccion)) {
						appendPlainElementValue(seccionLabel, seccion, "Section", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
					if (direccion != null && !"".equals(direccion)) {
						appendPlainElementValue(direccionLabel, direccion, "Direction", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
					if (supplierPartNumber != null && !"".equals(supplierPartNumber)) {
						appendPlainElementValue(supplierPartNumber, "", "SupplierPartNumber", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
					}
					if (supplierID != null && !"".equals(supplierID)) {
						appendPlainElementValue(supplierIDLabel, supplierID, "SupplierID", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,propiedadesCaracteristicas);
					}
					if(productName != null) {
						name.setTextContent(productName);
						appendPlainElementValue(
								productName,
								null,
								"ProductName",
								attributeValues,
								"MKP".equals(business) ? attributesMKT : attributes,
								"MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					}
					if (!behvo) {
						String elese = "SBB".equals(business) ? itemGroupS4H : itemGroup;
						String dictionary = !"SBB".equals(business)
								? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H";
						String laetiqueta = queryDictionary(elese, dictionary);
						if (laetiqueta != null && !laetiqueta.isBlank()) {
							String elcode = dastub.getLookupValueCodeByName(
									"SAP_BEHVOLOV", 10, laetiqueta, true);
							if (elcode != null && !elcode.isBlank()) {
								appendPlainElementValue(laetiqueta, elcode, "SAP_BEHVO", attributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
								behvo = true;
							}
						}
					}
	
					if (unosQueQuiero.contains("ProductType") && !heredables.containsKey("ProductType")) {
						heredables.put("ProductType",
								new org.json.JSONObject().put("_datatype", "LOOKUP")
										.put("_qualification",
												new org.json.JSONObject().put(
														"characteristic",
														new org.json.JSONObject().put("_code", "ProductType")))
										.put("_recordLang", new org.json.JSONArray()
												.put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(
														new org.json.JSONObject().put("_code", pt).put("_label", ptl))))));
					}
					appendPlainElementValue(itemGroup != null && !"".equals(itemGroup) ? itemGroupLabel : itemGroupS4HLabel,
							itemGroup != null && !"".equals(itemGroup) ? itemGroup : itemGroupS4H, "ItemGroup2",
							attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
					appendPlainElementValue(!"SBB".equals(business) ? itemGroupLabel : itemGroupS4HLabel,
							!"SBB".equals(business) ? itemGroup : itemGroupS4H,
							!"SBB".equals(business) ? "ItemGroup" : "ItemGroupS4H", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,
							propiedadesCaracteristicas);
					appendPlainElementValue(!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel,
							!"SBB".equals(business) ? brandName : brandIdS4H,
							!"SBB".equals(business) ? "BrandName" : "BRAND_ID_S4H", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc,
							propiedadesCaracteristicas);
					appendPlainElementValue(!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel, null,
							"BrandNameATG", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
					appendPlainElementValue(!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel, null, "BrandIDATG",
							attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
	
					if (productName != null && !"".equals(productName)) {
						name.setTextContent(productName);
						appendPlainElementValue(productName, null, "ProductName", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
								"MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
					} else {
						if (charactName != null && !"".equals(charactName)) {
							name.setTextContent(charactName);
							appendPlainElementValue(charactName, null, "ProductName", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas);
						} else {
							if (nameLang != null && !"".equals(nameLang)) {
								name.setTextContent(nameLang);
								appendPlainElementValue(nameLang, null, "ProductName", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
							} else {
								log("Sin product neim, no será posible publicar.");
								System.out.println("Sin product neim, no será posible publicar.");
								reqPublishMessage
										.getJSONArray("rows").put(
												new org.json.JSONObject()
														.put("object",
																new org.json.JSONObject().put("id",
																		"'" + proposalId + "'@1"))
														.put("values", new org.json.JSONArray().put("Sin ProductName")));
								continue;
							}
						}
					}
	
					if (pt == null) {
						lst = dataMap.get("AlmacenamientoAtt");
						if (lst != null) {
							almacenamientoAtt = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
									.getJSONArray("values").getJSONObject(0).getString("_code");
						}
						lst = dataMap.get("MTART_S4H");
						if (lst != null) {
							mtart = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
									.getJSONObject(0).getString("_code");
						}
						lst = dataMap.get("SkuType");
						if (lst != null) {
							skuType = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
									.getJSONObject(0).getString("_code");
						}
						if ("SERV".equals(skuType) && "0001".equals(almacenamientoAtt)) {
							pt = "6";
							ptl = "Digital";
							appendPlainElementValue("Digital", pt, "ProductType", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas);
						} else if ("DIEN".equals(mtart) && "SB87516".equals(itemGroupS4H)) {
							pt = "6";
							ptl = "Digital";
							appendPlainElementValue("Digital", pt, "ProductType", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas);
	
						} else {
							pt = "1";
							ptl = "Soft line";
							appendPlainElementValue("Soft line", pt, "ProductType", attributeValues, "MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas);
						}
					}
					
		        	if("SalesItem".equals(productType) && ean_ == null) {
	        			keyValueEAN = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
	        			keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
	        			keyValueEAN.setTextContent(ean_);
	        			product.appendChild(keyValueEAN);
	        			appendPlainElementValue(
								ean_,
								null,
								"MainBarCode",
								attributeValues,
								"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas
							);
		        	}
		        	if("SalesItem".equals(productType) && sku != null) {
	        			keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
	        			keyValueSKU.setAttribute("KeyID","SKUID");
	        			keyValueSKU.setTextContent( sku );
	        			product.appendChild(keyValueSKU);
	        			appendPlainElementValue(
								sku,
								null,
								"SKU",
								attributeValues,
								"MKP".equals(business) ? attributesMKT : attributes,
								"MKP".equals(business) ? docMKT : doc,
								propiedadesCaracteristicas);
		        	}
		        	if("SalesItem".equals(productType) && tamanoUnico != null && !"".equals(tamanoUnico)) {
		        		talla(clothingSize == null ? sizeVaD : clothingSize, tamanoUnico, business, itemGroup, template, direccion, brandCode, attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas );
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
		        		org.json.JSONObject resp = null;
		        		Element subProduct = null;
		        		Element subAttributeValues = null;
		        		Element varName = null;
		        		String childSAPObjectType = null;
		        		String childSAPObjectTypeLabel = null;
		        		String miraklVariantGroupId = null;
		        		java.util.LinkedList<java.util.LinkedList< String[] >> losdetalles = new java.util.LinkedList<>();
		        		java.util.LinkedList<java.util.LinkedList< String[] >> losesmoshes = new java.util.LinkedList<>();
		        		java.util.LinkedList<java.util.LinkedList< String[] >> lasilustraciones = new java.util.LinkedList<>();
		        		boolean theFirstTime = true;
		        		for(int a = 0; a<upperRows.length(); a++) {
		        			procede = true;
		        			tallaNormalizada = null;
		        			tamanoUnico = null;
		        			color = null;
		        			codigoColor = null;
		        			childSAPObjectType = null;
		        			childSAPObjectTypeLabel = null;
		        			miraklVariantGroupId = null;
		    				try{
		    					firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
		    				}catch(org.json.JSONException e) {
		    				}
	
		        			subProduct = ("MKP".equals(business) ? docMKT : doc).createElement("Product");
		        			subProduct.setAttribute("ID", firstVariant);
		        			subProduct.setAttribute("UserTypeID", "MKP".equals(business) ? "SalesItem" : "SalesItemVariant");
		        			if(!"MKP".equals(business))
		        				subProduct.setAttribute("ParentID", proposalId);
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
								raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData", null);
								resp = new org.json.JSONObject(raw);
								resp = resp.getJSONObject("_data");
								if(!resp.has("_characteristicRecords"))
									continue;
								rows = resp.getJSONArray("_characteristicRecords");
								org.json.JSONArray children = null;
								String[] chunk = null;
								String sku0 = resp.has("sku") ? String.valueOf( resp.getLong("sku") ) : null;
								String ean0 = resp.has("gtin") ? resp.getString("gtin") : null;
								codigoColor = resp.has("articleExtraData") && resp.getJSONArray("articleExtraData").getJSONObject(0).has("coloursLiverpoolAtt") ? resp.getJSONArray("articleExtraData").getJSONObject(0).getJSONObject("coloursLiverpoolAtt").getString("_code") : null;
								color = resp.has("articleExtraData") && resp.getJSONArray("articleExtraData").getJSONObject(0).has("coloursLiverpoolAtt") ? resp.getJSONArray("articleExtraData").getJSONObject(0).getJSONObject("coloursLiverpoolAtt").getString("_label") : null;
								tamanoUnico = resp.has("articleExtraData") && resp.getJSONArray("articleExtraData").getJSONObject(0).has("tamanoUnico") ? resp.getJSONArray("articleExtraData").getJSONObject(0).getJSONObject("tamanoUnico").getString("_code") : null;
								for(int b = 0; b < rows.length(); b++) {
									imageObject = rows.getJSONObject(b);
									charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
									if("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
										ean0 = ean0 != null && !"".equals(ean0) ? ean0 : imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
					        		}else
					        		if("SKU".equals(charId)) {
					        			sku0 = sku0 != null && !"".equals(sku0) ? sku0 : imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
					        		}else
									if("ProductImage2".equals(charId) && imageObject.has("_children")) {
										children = imageObject.getJSONArray("_children");
										piKey = imageObject.getJSONObject("_qualification").getString("recordKey");
										for(int c = 0; c<children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("ProductImage_Name2".equals(charId)) {
												piName = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}else if("ProductImage_URL2".equals(charId)) {
												piUrl = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
									}else if("ProductImageDetail2".equals(charId) && imageObject.has("_children")) {
										children = imageObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
										for(int c = 0; c<children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("ProductImageDetail_Name2".equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}else if("ProductImageDetail_URL2".equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										details.addLast(chunk);
										if(theFirstTime)
											losdetalles.addLast(details);
									}else if("ProductImageSmosh2".equals(charId) && imageObject.has("_children")) {
										children = imageObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
										for(int c = 0; c<children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("ProductImageSmosh_Name2".equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}else if("ProductImageSmosh_URL2".equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										smoshes.addLast(chunk);
										if(theFirstTime)
											losesmoshes.addLast(smoshes);
									}else if("Illustration2".equals(charId) && imageObject.has("_children")) {
										children = imageObject.getJSONArray("_children");
										chunk = new String[4];
										chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
										for(int c = 0; c<children.length(); c++) {
											charId = children.getJSONObject(c).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("Illustration_Name2".equals(charId)) {
												chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}else if("Illustration_URL2".equals(charId)) {
												chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
											}
										}
										chunk[3] = firstVariant;
										illustrations.addLast(chunk);
										if(theFirstTime)
											lasilustraciones.addLast(illustrations);
									} else if("TamanoUnicoSTD".equals(charId)){
										tallaNormalizada = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
									}else if("TamanoUnico".equals(charId)) {
										tamanoUnico = tamanoUnico != null ? tamanoUnico : imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
									}else if("ColoursLiverpoolAtt".equals(charId)) {
										color = color != null ? color : imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
										codigoColor = codigoColor != null ? codigoColor : imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
									}else if("SKU".equals(charId)) {
										sku0 = sku0 == null || "".equals(sku0) ? imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) : sku0;
									}else if("MainBarCode".equals(charId)) {
										ean0 = ean0 == null || "".equals(ean0) ? imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) : ean0;
									}else if("MainBarCodeS4H".equals(charId)) {
										ean0 = ean0 == null || "".equals(ean0) ? imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) : ean0;
									}else if("SAPObjectType".equals(charId)) {
										childSAPObjectTypeLabel = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
										childSAPObjectType = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
									}else if("mirakl-variant-group-id".equals(charId)) {
										miraklVariantGroupId = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
									}else if("ProcedeNoProcede".equals(charId)) {
										procede = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getBoolean(0);
									}else {
										if(atributosGeneralesQueSi.contains(charId)) {
											if(unosQueQuiero.contains(charId)) {
												heredables.put(charId, characteristic);
											}
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
									theFirstTime = false;
								}
								if(sku0 != null && !"".equals(sku0)) {
//									if("MKP".equals(business) && upperRows.length() == 1) {
//										if(sku_ == null || "".equals(sku_)) {
//											sku_ = sku0;
//										}
//									}
									keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
				        			keyValueSKU.setAttribute("KeyID","SKUID");
				        			String skuval =  sku0;
				        			keyValueSKU.setTextContent( skuval );
				        			subProduct.appendChild(keyValueSKU);
				        			appendPlainElementValue(
											sku0,
											null,
											"SKU",
											subAttributeValues,
											"MKP".equals(business) ? attributesMKT : attributes,
	    									"MKP".equals(business) ? docMKT : doc,
											propiedadesCaracteristicas);
								}
								if(ean0 != null) {
									if("MKP".equals(business) && upperRows.length() == 1) {
										if(ean_ == null || "".equals(ean_)) {
											ean_ = ean0;
										}
									}
									keyValueEAN = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
				        			keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
				        			keyValueEAN.setTextContent(ean0);
				        			subProduct.appendChild(keyValueEAN);
				        			appendPlainElementValue(
											ean0,
											null,
											"MainBarCode",
											subAttributeValues,
											"MKP".equals(business) ? attributesMKT : attributes,
	    									"MKP".equals(business) ? docMKT : doc,
											propiedadesCaracteristicas);
								}
	//							if(productName != null) {
	//								name.setTextContent(productName);
	//								appendPlainElementValue(
	//										productName,
	//										null,
	//										"ProductName",
	//										subAttributeValues,
	//										"MKP".equals(business) ? attributesMKT : attributes,
	//    									"MKP".equals(business) ? docMKT : doc,
	//										propiedadesCaracteristicas);
	//							}
								if (productName != null) {
									name.setTextContent(productName);
									appendPlainElementValue(productName, null, "ProductName", subAttributeValues,
											"MKP".equals(business) ? attributesMKT : attributes,
			    									"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
								} else {
									if (charactName != null && !"".equals(charactName)) {
										name.setTextContent(charactName);
										appendPlainElementValue(charactName, null, "ProductName", subAttributeValues,
												"MKP".equals(business) ? attributesMKT : attributes,
				    									"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
									} else {
										if (nameLang != null && !"".equals(nameLang)) {
											name.setTextContent(nameLang);
											appendPlainElementValue(nameLang, null, "ProductName", subAttributeValues,
													"MKP".equals(business) ? attributesMKT : attributes,
					    									"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
										} else {
											log("Sin product neim, no será posible publicar.");
											System.out.println("Sin product neim, no será posible publicar.");
											reqPublishMessage.getJSONArray("rows")
													.put(new org.json.JSONObject()
															.put("object",
																	new org.json.JSONObject().put("id",
																			"'" + proposalId + "'@1"))
															.put("values",
																	new org.json.JSONArray().put("Sin ProductName")));
											continue;
										}
									}
								}
								if (descLong != null) {
									appendPlainElementValue(descLong, null, "DescriptionLong", subAttributeValues,
											"MKP".equals(business) ? attributesMKT : attributes,
			    									"MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
								}
								log("For: " + firstVariant + ", " + procede);
								if(!procede) {
									procede = resp.has("procedeNoProcede") && resp.getBoolean("procedeNoProcede");
								}
								if(!procede) {
									continue;
								}else {
				                	product.appendChild(subProduct);
								}
								if(miraklVariantGroupId == null && "MKP".equals(business)) {
									appendPlainElementValue(
		        							sku,
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
									if("LOOKUP".equals(characteristic.getString("_datatype"))){
										if(!characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).has("_label")) {
											log("FUUUUUUUUFFFFFKKKKKKFFFFFF " + entr + " FUUUUUUUUFFFFFKKKKKKFFFFFF");
										}
//										System.out.println("\t\tAV: " + characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0) + " || " + entr);
										if(characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).length() > 0) {
				        					appendPlainElementValue(
				        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
				        							characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
				        							charId,
				        							subAttributeValues,
				        							"MKP".equals(business) ? attributesMKT : attributes,
		        									"MKP".equals(business) ? docMKT : doc,
				        							propiedadesCaracteristicas);
										}else {
											System.out.println("Pessi: " + entr);
										}
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
										sku,
										null,
										"ParentSKU",
										subAttributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas);
	//							if(childSKU != null && !"".equals(childSKU)) {
	//								appendPlainElementValue(
	//										childSKU,
	//										null,
	//										"SKU",
	//										subAttributeValues,
	//										"MKP".equals(business) ? attributesMKT : attributes,
	//										"MKP".equals(business) ? docMKT : doc,
	//										propiedadesCaracteristicas);
	//							}
	//							if(childMainBarCode != null && !"".equals(childMainBarCode)) {
	//								appendPlainElementValue(
	//										childMainBarCode,
	//										null,
	//										"SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode",
	//										subAttributeValues,
	//										"MKP".equals(business) ? attributesMKT : attributes,
	//										"MKP".equals(business) ? docMKT : doc,
	//										propiedadesCaracteristicas);
	//							}
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
									talla(clothingSize == null ? sizeVaD : clothingSize, tamanoUnico, business, itemGroup, template, direccion, brandCode, subAttributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas );
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
		    				theFirstTime = false;
		    			}
		        		
		        		if(sku != null && !"".equals(sku)) {
			        		keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
		        			keyValueSKU.setAttribute("KeyID","SKUID");
		        			keyValueSKU.setTextContent( sku );
		        			product.appendChild(keyValueSKU);
		        			appendPlainElementValue(
									sku,
									null,
									"SKU",
									attributeValues,
									"MKP".equals(business) ? attributesMKT : attributes,
									"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas);
		        		}
		        		if(ean_ != null && !"".equals(ean_)) {
			        		keyValueEAN = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
		        			keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
		        			keyValueEAN.setTextContent(ean_);
		        			product.appendChild(keyValueEAN);
		        			appendPlainElementValue(
									ean_,
									null,
									"MainBarCode",
									attributeValues,
									"MKP".equals(business) ? attributesMKT : attributes,
										"MKP".equals(business) ? docMKT : doc,
									propiedadesCaracteristicas
								);
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
		        	}else if("SalesItem".equals(productType)) {
						try {
							raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData", null);
							org.json.JSONObject resp = new org.json.JSONObject(raw);
							resp = resp.getJSONObject("_data");
							if(!resp.has("_characteristicRecords"))
								continue;
							if(resp.has("procedeNoProcede")) {
								procede = resp.getBoolean("procedeNoProcede");
								log("El procede: " + procede);
							}
							rows = resp.getJSONArray("_characteristicRecords");
							String sku0 = String.valueOf( resp.has("sku") ? resp.getLong("sku") : "" );
							String ean0 = String.valueOf( resp.has("gtin") ? resp.getString("gtin") : "" );
							for(int b = 0; b < rows.length(); b++) {
								imageObject = rows.getJSONObject(b);
								charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
								if("ProductImage2".equals(charId)) {
								}else if("ProductImageDetail2".equals(charId)) {
								}else if("ProductImageSmosh2".equals(charId)) {
								}else if("Illustration2".equals(charId)) {
								}else if("ProductImage".equals(charId)) {
								}else if("ProductImageDetail".equals(charId)) {
								}else if("ProductImageSmosh".equals(charId)) {
								}else if("Illustration".equals(charId)) {
								}else if("TamanoUnicoSTD".equals(charId)){
								}else if("TamanoUnico".equals(charId)) {
								}else if("ColoursLiverpoolAtt".equals(charId)) {
								}else if("SKU".equals(charId)) {
									sku0 = sku0 != null && !"".equals(sku0) ? sku0 : treatment( imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
								}else if("MainBarCode".equals(charId)) {
									ean0 = ean0 != null && !"".equals(ean0) ? ean0 : imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("MainBarCodeS4H".equals(charId)) {
									ean0 = ean0 != null && !"".equals(ean0) ? ean0 : imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
								}else if("SAPObjectType".equals(charId)) {
								}else if("ProcedeNoProcede".equals(charId)) {
									procede = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getBoolean(0);
								}else {
									if(atributosGeneralesQueSi.contains(charId)) {
										if("LOOKUP".equals(imageObject.getString("_datatype"))){
											appendPlainElementValue(
													imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
													imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
													charId,
													attributeValues,
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
													attributeValues,
													"MKP".equals(business) ? attributesMKT : attributes,
					    								"MKP".equals(business) ? docMKT : doc,
													propiedadesCaracteristicas
												);
										}
									}
								}
							}
							if(sku == null || "".equals(sku)) {
								sku = sku0;
							}
							if(sku != null && !"".equals(sku)) {
								keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
								keyValueSKU.setAttribute("KeyID","SKUID");
								String skuval =  sku;
								keyValueSKU.setTextContent( skuval );
								product.appendChild(keyValueSKU);
								appendPlainElementValue(
										skuval,
										null,
										"SKU",
										attributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
	    								"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas
									);
							}
							if(ean_ == null) {
								ean_ = ean0;
							}
							if(ean_ != null) {
								keyValueEAN = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
								keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
								keyValueEAN.setTextContent(ean_);
								product.appendChild(keyValueEAN);
								appendPlainElementValue(
										ean_,
										null,
										"MainBarCode",
										attributeValues,
										"MKP".equals(business) ? attributesMKT : attributes,
		    								"MKP".equals(business) ? docMKT : doc,
										propiedadesCaracteristicas
									);
							}
							log("For: " + firstVariant + ", " + procede);
							if(!procede) {
								procede = resp.has("procedeNoProcede") && resp.getBoolean("procedeNoProcede");
							}
							if(procede) {
							}
						} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
							logE(e);
						}
			        	}
		        	if (rw.getXmm().listImmediateChildElements(product).get("Product") != null || "SalesItem".equals(productType)) {
			        	if("MKP".equals(business)) {
			        		productsMKT.appendChild(product);
			        	}else {
			        		products.appendChild(product);
			        	}
		        	}else {
		        		log("Dropped. " + product.getAttribute("ID"));
		        		System.out.println("Dropped. " + productType + "---");
		        		System.out.println(rw.getXmm().prettyPrint(product));
		        		this.dropped++;
		        	}
	    				    			System.out.println(sku + " - " + proposalId);
					} finally {
						if (hasProducts(current)) {
							long[] candidateBytes = measureMergedSizes(batch, current);
							if (hasProducts(batch) && exceedsLimit(candidateBytes)) {
								appendResult(result, finishBatch(batch, send, batchNumber++));
								batch = createMiraklExportContext();
							}
							mergeContext(batch, current, true);
							long[] batchBytes = serializedSizes(batch);
							if (exceedsLimit(batchBytes) && batch.proposalIds.size() == 1) {
								log("La propuesta " + current.currentProposalId
										+ " supera individualmente 7 MB (LVP=" + batchBytes[0]
										+ ", MKT=" + batchBytes[1] + "). Se conserva íntegra.");
								appendResult(result, finishBatch(batch, send, batchNumber++));
								batch = createMiraklExportContext();
							}
						} else {
							mergeContext(batch, current, false);
						}
					}
				}
				if (hasPendingWork(batch)) {
					appendResult(result, finishBatch(batch, send, batchNumber));
				}
			} catch (TransformerException e) {
				logE(e);
			} catch (ParserConfigurationException e) {
				logE(e);
			}
			return result.length() == 0 ? null : result.toString();
		}
	}
	

	private static final class MiraklExportContext {
		private Document doc;
		private Element spim;
		private Element attributes;
		private Element assets;
		private Element products;
		private Document docMKT;
		private Element spimMKT;
		private Element attributesMKT;
		private Element assetsMKT;
		private Element productsMKT;
		private String currentProposalId;
		private final java.util.Map<String, Element> assetMap = new java.util.TreeMap<>();
		private final java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap = new java.util.TreeMap<>();
		private final java.util.Map<String, Element> assetMapMKT = new java.util.TreeMap<>();
		private final java.util.Map<String, java.util.LinkedList<String>> assetReferencesMapMKT = new java.util.TreeMap<>();
		private final java.util.LinkedList<String> productosLiverpool = new java.util.LinkedList<>();
		private final java.util.LinkedList<String> productosMarketplace = new java.util.LinkedList<>();
		private final java.util.List<String> proposalIds = new java.util.ArrayList<>();
		private final org.json.JSONObject reqPublishMessage = newRequest(
				"Product2GCharacteristicValueLang.Value('PublishMktMessage',root,\"0000.0000.RK\",'PublishMktMessage',-1)");
	}

	private static org.json.JSONObject newRequest(String identifier) {
		return new org.json.JSONObject()
				.put("columns", new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", identifier)))
				.put("rows", new org.json.JSONArray());
	}

	private MiraklExportContext createMiraklExportContext() throws ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		MiraklExportContext context = new MiraklExportContext();
		context.doc = builder.newDocument();
		context.spim = createStepRoot(context.doc);
		context.attributes = context.doc.createElement("AttributeList");
		context.assets = context.doc.createElement("Assets");
		context.products = context.doc.createElement("Products");
		context.spim.appendChild(context.attributes);
		context.spim.appendChild(context.assets);
		context.spim.appendChild(context.products);

		context.docMKT = builder.newDocument();
		context.spimMKT = createStepRoot(context.docMKT);
		context.attributesMKT = context.docMKT.createElement("AttributeList");
		context.assetsMKT = context.docMKT.createElement("Assets");
		context.productsMKT = context.docMKT.createElement("Products");
		context.spimMKT.appendChild(context.attributesMKT);
		context.spimMKT.appendChild(context.assetsMKT);
		context.spimMKT.appendChild(context.productsMKT);
		return context;
	}

	private Element createStepRoot(Document document) {
		Element root = document.createElement("STEP-ProductInformation");
		root.setAttribute("ExportTime",
				new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
		root.setAttribute("ExportContext", "Context2");
		root.setAttribute("ContextID", "Context2");
		root.setAttribute("WorkspaceID", "Approved");
		root.setAttribute("UseContextLocale", "false");
		document.appendChild(root);
		return root;
	}

	private boolean hasProducts(MiraklExportContext context) {
		return hasDirectElementChild(context.products, "Product")
				|| hasDirectElementChild(context.productsMKT, "Product");
	}

	private boolean hasPendingWork(MiraklExportContext context) {
		return hasProducts(context) || hasRows(context.reqPublishMessage);
	}

	private static boolean hasRows(org.json.JSONObject request) {
		return request != null && request.getJSONArray("rows").length() > 0;
	}

	private static boolean hasDirectElementChild(Element parent, String tagName) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
				return true;
			}
		}
		return false;
	}

	private long[] measureMergedSizes(MiraklExportContext batch, MiraklExportContext current)
			throws ParserConfigurationException, TransformerException {
		Document candidateLvp = cloneDocument(batch.doc);
		Document candidateMkt = cloneDocument(batch.docMKT);
		mergeDocument(candidateLvp, current.attributes, current.assets, current.products);
		mergeDocument(candidateMkt, current.attributesMKT, current.assetsMKT, current.productsMKT);
		return new long[] { serializedSize(candidateLvp), serializedSize(candidateMkt) };
	}

	private long[] serializedSizes(MiraklExportContext context) throws TransformerException {
		return new long[] { serializedSize(context.doc), serializedSize(context.docMKT) };
	}

	private static boolean exceedsLimit(long[] sizes) {
		return sizes[0] > MAX_BATCH_BYTES || sizes[1] > MAX_BATCH_BYTES;
	}

	private void mergeContext(MiraklExportContext destination, MiraklExportContext source,
			boolean includeXml) {
		if (includeXml) {
			mergeDocument(destination.doc, source.attributes, source.assets, source.products);
			mergeDocument(destination.docMKT, source.attributesMKT, source.assetsMKT, source.productsMKT);
			destination.assetMap.putAll(source.assetMap);
			destination.assetReferencesMap.putAll(source.assetReferencesMap);
			destination.assetMapMKT.putAll(source.assetMapMKT);
			destination.assetReferencesMapMKT.putAll(source.assetReferencesMapMKT);
			destination.productosLiverpool.addAll(source.productosLiverpool);
			destination.productosMarketplace.addAll(source.productosMarketplace);
			if (source.currentProposalId != null) {
				destination.proposalIds.add(source.currentProposalId);
			}
		}
		appendRows(destination.reqPublishMessage, source.reqPublishMessage);
	}

	private void mergeDocument(Document destinationDocument, Element sourceAttributes,
			Element sourceAssets, Element sourceProducts) {
		Element root = destinationDocument.getDocumentElement();
		appendUniqueDirectChildren(destinationDocument, directChild(root, "AttributeList"),
				sourceAttributes, "ID");
		appendUniqueDirectChildren(destinationDocument, directChild(root, "Assets"),
				sourceAssets, "ID");
		appendUniqueDirectChildren(destinationDocument, directChild(root, "Products"),
				sourceProducts, "ID");
	}

	private static Element directChild(Element parent, String tagName) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
				return (Element) child;
			}
		}
		throw new IllegalStateException("No se encontró el elemento " + tagName);
	}

	private static void appendUniqueDirectChildren(Document destinationDocument,
			Element destinationParent, Element sourceParent, String idAttribute) {
		for (Node child = sourceParent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!(child instanceof Element)) {
				continue;
			}
			Element sourceElement = (Element) child;
			String id = sourceElement.getAttribute(idAttribute);
			if (id == null || id.isEmpty()
					|| findDirectChildById(destinationParent, sourceElement.getTagName(), id) == null) {
				destinationParent.appendChild(destinationDocument.importNode(sourceElement, true));
			}
		}
	}

	private static Element findDirectChildById(Element parent, String tagName, String id) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				Element element = (Element) child;
				if (tagName.equals(element.getTagName()) && id.equals(element.getAttribute("ID"))) {
					return element;
				}
			}
		}
		return null;
	}

	private static void appendRows(org.json.JSONObject destination, org.json.JSONObject source) {
		org.json.JSONArray destinationRows = destination.getJSONArray("rows");
		org.json.JSONArray sourceRows = source.getJSONArray("rows");
		for (int i = 0; i < sourceRows.length(); i++) {
			destinationRows.put(sourceRows.get(i));
		}
	}

	private Document cloneDocument(Document source) throws ParserConfigurationException {
		Document clone = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		clone.appendChild(clone.importNode(source.getDocumentElement(), true));
		return clone;
	}
	
	private static void removeInvalidXml10ControlCharacters(Node node) {
	    if (node == null) {
	        return;
	    }

	    NamedNodeMap attributes = node.getAttributes();

	    if (attributes != null) {
	        for (int i = 0; i < attributes.getLength(); i++) {
	            Node attribute = attributes.item(i);
	            attribute.setNodeValue(
	                    removeInvalidXml10ControlCharacters(attribute.getNodeValue())
	            );
	        }
	    }

	    switch (node.getNodeType()) {
	        case Node.TEXT_NODE:
	        case Node.CDATA_SECTION_NODE:
	        case Node.COMMENT_NODE:
	        case Node.PROCESSING_INSTRUCTION_NODE:
	            node.setNodeValue(
	                    removeInvalidXml10ControlCharacters(node.getNodeValue())
	            );
	            break;

	        default:
	            break;
	    }

	    NodeList children = node.getChildNodes();

	    for (int i = 0; i < children.getLength(); i++) {
	        removeInvalidXml10ControlCharacters(children.item(i));
	    }
	}

	private static String removeInvalidXml10ControlCharacters(String value) {
	    if (value == null || value.isEmpty()) {
	        return value;
	    }

	    StringBuilder result = new StringBuilder(value.length());

	    for (int offset = 0; offset < value.length();) {
	        int codePoint = value.codePointAt(offset);

	        if (codePoint >= 0x20
	                || codePoint == '\t'
	                || codePoint == '\n'
	                || codePoint == '\r') {
	            result.appendCodePoint(codePoint);
	        }

	        offset += Character.charCount(codePoint);
	    }

	    return result.toString();
	}

	private String serializeMiraklXml(Document document) throws TransformerException {
		removeInvalidXml10ControlCharacters(document);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
		transformer.transform(new DOMSource(document), new StreamResult(output));
		return new String(output.toByteArray(), StandardCharsets.UTF_8)
				.replace("&lt;CRLF&gt;", "&#13;&#10;")
				.replace("<CRLF>", "&#13;&#10;")
				;
	}

	private long serializedSize(Document document) throws TransformerException {
		return serializeMiraklXml(document).getBytes(StandardCharsets.UTF_8).length;
	}

	private String finishBatch(MiraklExportContext batch, boolean send, int batchNumber)
			throws TransformerException {
		sendPublishRows(batch.reqPublishMessage);
		StringBuilder result = new StringBuilder();
		if (hasDirectElementChild(batch.products, "Product")) {
			appendResult(result, finishDocumentBatch(batch.doc, batch.productosLiverpool,
					fileSystemPrefixLvp, urlDeMktStockout, "LVP", send, batchNumber));
		}
		if (hasDirectElementChild(batch.productsMKT, "Product")) {
			appendResult(result, finishDocumentBatch(batch.docMKT, batch.productosMarketplace,
					fileSystemPrefix, urlDeMkt, "MKT", send, batchNumber));
		}
		this.products += directElementCount(batch.products, "Product")
				+ directElementCount(batch.productsMKT, "Product");
		return result.toString();
	}

	private void sendPublishRows(org.json.JSONObject request) {
		if (!hasRows(request)) {
			return;
		}
		java.util.Map<String, String> params = new java.util.HashMap<>();
		params.put("includeObjectsInProtocol", "false");
		wrapper.writeData("list", "Product2G", null, params, request, this::log);
	}

	private String finishDocumentBatch(Document document, java.util.List<String> productIds,
			String directory, String endpoint, String channel, boolean send, int batchNumber)
			throws TransformerException {
		String xmlOutput = serializeMiraklXml(document);
		long bytes = xmlOutput.getBytes(StandardCharsets.UTF_8).length;
		java.nio.file.Path outputDirectory = java.nio.file.Paths.get(directory);
		java.nio.file.Path file = outputDirectory.resolve(
				"pepele_" + channel.toLowerCase() + "_batch"
				+ String.format("%03d", batchNumber) + "_" + System.currentTimeMillis() + ".xml");
		try {
			java.nio.file.Files.createDirectories(outputDirectory);
			java.nio.file.Files.writeString(file, xmlOutput, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logE(e);
		}
		if (java.nio.file.Files.exists(file)) {
			this.generatedMarketplaceFiles.add(file.toString());
		} else {
			this.marketplaceBrokerFailure = true;
		}
		log(channel + " batch " + batchNumber + ": " + productIds.size()
				+ " propuestas, " + bytes + " bytes, archivo " + file);
		System.out.println(channel + " batch " + batchNumber + ": " + productIds.size()
		+ " propuestas, " + bytes + " bytes, archivo " + file);
		if (!send) {
			return file.toString();
		}
		try {
			RestClient client = new RestClient("Content-Type: application/xml", "Accept: application/xml");
			String response = client.getRequest("POST", endpoint, xmlOutput);
			this.marketplaceBrokerResponses.add(response == null ? "" : response);
			if (!isKnownSuccessfulBrokerResponse(response)) {
				this.marketplaceUnrecognizedResponse = true;
			}
			log(channel + " batch " + batchNumber + " enviado para " + productIds + ": " + response);
			System.out.println(channel + " batch " + batchNumber + " enviado para " + productIds + ": " + response);
			return file + "<::>" + response;
		} catch (IOException e) {
			this.marketplaceBrokerFailure = true;
			this.marketplaceBrokerResponses.add("IOException: " + e.getMessage());
			logE(e);
			return file.toString();
		}
	}

	private static boolean isKnownSuccessfulBrokerResponse(String response) {
		if (response == null || response.trim().isEmpty()) {
			return false;
		}

		try {
			org.json.JSONObject json = new org.json.JSONObject(response);
			return !json.optString("tracking_id", "").trim().isEmpty();
		} catch (org.json.JSONException e) {
			return false;
		}
	}

	private static int directElementCount(Element parent, String tagName) {
		int count = 0;
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
				count++;
			}
		}
		return count;
	}

	private static void appendResult(StringBuilder destination, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		if (destination.length() > 0) {
			destination.append(System.lineSeparator());
		}
		destination.append(value);
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
//		log( new RealExportProducts2MiraklJdbcBatch().isBannedForMarketplace("10110", "ItemGroups", "MATKLLOV") );
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
	
	private void addCharacteristicData(java.util.Map<String, org.json.JSONObject> propertiesByCharacteristic) {
		for (java.util.Map.Entry<Integer, org.json.JSONObject> entry : characteristicMetadataByID.entrySet()) {
			String identifier = characteristicIdentifierByID.get(entry.getKey());
			if (identifier == null || identifier.isBlank() || propertiesByCharacteristic.containsKey(identifier)) {
				continue;
			}
			propertiesByCharacteristic.put(identifier, copyCharacteristicMetadata(entry.getValue()));
		}
	}

	private org.json.JSONObject getOrCreateCharacteristicProperties(int characteristicID,
			java.util.Map<String, org.json.JSONObject> target) {
		org.json.JSONObject metadata = characteristicMetadataByID.get(characteristicID);
		String identifier = characteristicIdentifierByID.get(characteristicID);
		if (metadata == null || identifier == null || identifier.isBlank()) {
			return null;
		}
		org.json.JSONObject properties = target.get(identifier);
		if (properties == null) {
			properties = copyCharacteristicMetadata(metadata);
			target.put(identifier, properties);
		}
		return properties;
	}

	private void loadCharacteristicMetadata() {
		java.util.Map<Integer, String> purposeCodes = dastub.getLookupValueCodeMap(2);
		for (org.json.JSONObject row : dastub.getCharacteristicMetadataRows(10)) {
			String identifier = row.optString("identifier", "");
			if (identifier.isBlank()) {
				continue;
			}
			org.json.JSONObject metadata = new org.json.JSONObject()
					.put("name", row.optString("name", ""))
					.put("description", row.optString("description", ""))
					.put("dataType", row.optString("dataType", ""))
					.put("lookup", row.optString("lookup", ""))
					.put("isMultiValue", row.optString("isMultiValue", ""))
					.put("purposes", rutils.resolvePurposeCodes(row.optString("purposesRaw", ""), purposeCodes))
					.put("order", row.optString("order", ""));
			int characteristicID = row.getInt("characteristicID");
			characteristicMetadataByID.put(characteristicID, metadata);
			characteristicIdentifierByID.put(characteristicID, identifier);
		}
	}

	private org.json.JSONObject copyCharacteristicMetadata(org.json.JSONObject source) {
		org.json.JSONArray purposes = new org.json.JSONArray();
		org.json.JSONArray sourcePurposes = source.optJSONArray("purposes");
		if (sourcePurposes != null) {
			for (int i = 0; i < sourcePurposes.length(); i++) {
				purposes.put(sourcePurposes.get(i));
			}
		}
		return new org.json.JSONObject()
				.put("name", source.optString("name", ""))
				.put("description", source.optString("description", ""))
				.put("dataType", source.optString("dataType", ""))
				.put("lookup", source.optString("lookup", ""))
				.put("isMultiValue", source.optString("isMultiValue", ""))
				.put("purposes", purposes)
				.put("order", source.optString("order", ""));
	}

	private void addGlobalData(java.util.Map<String, org.json.JSONObject> propertiesByCharacteristic,
			java.util.Set<String> relevantForAtg) {
		for (org.json.JSONObject row : dastub
				.getStandardizationValueCharacteristicRows("GlobalTemplateAttributeConfiguration")) {
			org.json.JSONObject detail = getOrCreateCharacteristicProperties(
					row.getInt("characteristicID"), propertiesByCharacteristic);
			if (detail == null) {
				continue;
			}
			String property = row.optString("property", "");
			if (!property.isBlank()) {
				detail.put(property, row.optString("propertyValue", ""));
			}
		}
		for (java.util.Map.Entry<String, org.json.JSONObject> entry : propertiesByCharacteristic.entrySet()) {
			if ("Y".equals(entry.getValue().optString("RelevantForATG", ""))) {
				relevantForAtg.add(entry.getKey());
			}
		}
	}
	public void talla(String latallaFromCharacteristic, String latalla, String business, String itemGroup, String template, String direccion, String brand, Element attributeValues, Element attributes, Document doc, java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas) throws ServiceUnavailableException {
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
		if(tallaWeb != null && latallaFromCharacteristic == null) {
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


	private String getAtributoSapLatalla(String itemGroup, String business) {
		String dictionary = "SBB".equals(business)
				? "TallaUnicavsTallaS4H" : "TallaUnicavsTallaERP";
		String value = queryDictionary(itemGroup, dictionary);
		if (value == null || ("".equals(value) && !"SBB".equals(business))) {
			value = queryDictionary(itemGroup, "ItemGroupSAPSizeAttribute");
		}
		return value;
	}
	
	@SuppressWarnings("deprecation")
	private String queryDictionary(String key, String dictionary) {
		return dastub.queryDictionary(key, dictionary);
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
		return dastub.queryVariantOrder(key);
	}
	private static final Logger LOGGER = Logger.getLogger(RealExportProducts2Mirakl.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/real_export_products_mkt-%g.log", 5 * 1024 * 1024, 5, true);
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

    private java.io.PrintWriter logger = null;
    
	private void log(String message){
//		LOGGER.info(message);
//		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","real_export_products_mkt.log").toString(), true)))){
//		  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "]  " + message);
//		}catch(java.io.IOException e){}
		if(logger == null) {
			try{
				logger = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get("..", "logs", "REP2M.log").toFile(), true )));
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		logger.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "]  " + message);
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get( "..","logs","real_export_products_mkt.err").toString(), true)))){
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] BEGIN EXCEPT ");
			ex.printStackTrace(pw);
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] END EXCEPT. ");
		}catch(java.io.IOException e){}
	}

	public java.util.Set<String> YEA;

	private java.util.Map<String, String> mapaDeDirecciones; // = new java.util.TreeMap<>();
	private java.util.Map<String, String> mapaDeDireccionesAtributoTallaWeb; // = new java.util.TreeMap<>();
	private java.util.Map<String, String> mapaDeAtributosFechas; // = new java.util.TreeMap<>();
	
	private java.util.Map<String, String> loadFieldDictionaries() {
		return dastub.getDictionaryValueAlternativeValueMap("RelAttribSTDATG");
	}
	private java.util.Map<String, String> loadFieldTallaATG() {
		return dastub.getDictionaryValueAlternativeValueMap("RelAttribTallaATG");
	}
	
	private java.util.Map<String, String> loadAtributosFecha() {
		return dastub.getDictionaryCharacteristicAlternativeValueMap("ConversionFechaATG");
	}
	
	private java.util.Set<String> loadInheritedFields() {
		return new java.util.TreeSet<>(
				dastub.getDictionaryCharacteristicAlternativeValueMap("CaracteristicasHeredables").keySet());
	}
	
	private void loadDatabaseDictionaries() {
		mapaDeDirecciones = loadFieldDictionaries();
		mapaDeDireccionesAtributoTallaWeb = loadFieldTallaATG();
		mapaDeAtributosFechas = loadAtributosFecha();
		YEA = loadInheritedFields();
	}

}
