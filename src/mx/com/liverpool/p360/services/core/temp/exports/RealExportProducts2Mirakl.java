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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	
	private static final org.json.JSONObject reqPublishMessage = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PublishMktMessage',root,\"0000.0000.RK\",'PublishMktMessage',-1)"))).put("rows", new org.json.JSONArray());

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
	
	private static final String USAGE = "Usage: RealExportProducts2Mirakl <File with IDs or SKUs> -t ID|SKU [-s]\n-t indicates which type of content is in the file: SKU or Proposal IDs\n-s if present, indicates to send the data to destination, default is not send the data.";

	private int products = 0;
	private int dropped = 0;
	
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
		if(type == 0) {
			int b = 0;
			java.util.List<String> losesos = new java.util.ArrayList<>();
			for(int a = 0; a<data.length; a++) {
				b++;
				losesos.add(data[a]);
				if( b == 10 ) {
					String[] ela = losesos.toArray( new String[] {} );
					o.log("Voy a mandar: " + losesos.size() + " || " + ela.length + " || " + b);
					o.doIt( ela , send, baseUrlDEV);
					b = 0;
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
	
	public static void runForProductIds( String[] proposalIds, boolean send )
		    throws ServiceUnavailableException
		{
		  String[] data = cleanIds( proposalIds );

		  RealExportProducts2Mirakl o = new RealExportProducts2Mirakl();
		  o.log( "Iniciando RealExportProducts2Mirakl desde arreglo de IDs: " + data.length );

		  java.util.List<String> batch = new java.util.ArrayList<String>();

		  try
		  {
		    for ( int i = 0; i < data.length; i++ )
		    {
		      batch.add( data[i] );

		      if ( batch.size() == 10 )
		      {
		        String[] chunk = batch.toArray( new String[batch.size()] );
		        o.log( "Voy a mandar Mirakl: " + chunk.length );
		        o.doIt( chunk, send, baseUrlDEV );
		        batch.clear();
		      }
		    }

		    if ( !batch.isEmpty() )
		    {
		      String[] chunk = batch.toArray( new String[batch.size()] );
		      o.log( "Voy a mandar Mirakl final: " + chunk.length );
		      o.doIt( chunk, send, baseUrlDEV );
		    }

		    o.log( "Total: " + o.products + " (dropped: " + o.dropped + ")" );
		  }
		  finally
		  {
		    if ( o.logger != null )
		    {
		      o.logger.close();
		    }
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
        	spim.setAttribute("WorkspaceID", "Approved");
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
        	spimMKT.setAttribute("ExportTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new java.util.Date() ));
        	spimMKT.setAttribute("ExportContext", "Context2");
        	spimMKT.setAttribute("ContextID", "Context2");
        	spimMKT.setAttribute("WorkspaceID", "Approved");
        	spimMKT.setAttribute("UseContextLocale", "false");
        	Element attributesMKT = docMKT.createElement("AttributeList");
        	Element assetsMKT = docMKT.createElement("Assets");
        	java.util.Map<String, Element> assetMapMKT = new java.util.TreeMap<>();
        	java.util.Map<String, java.util.LinkedList<String>> assetReferencesMapMKT = new java.util.TreeMap<>();
        	spimMKT.appendChild(assetsMKT);
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
        	boolean brk = false;
			addGlobalData(globalProperties, globalSet, baseUrlDEV);
			for(int index = 0; index<proposalIds.length; index++) {
				procede = false;
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
				String sku00 = null;
				String ean00 = null;
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
								sku00 = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}else if("SalesItem".equals(productType) && ("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId))) {
								ean00 = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}else {
								if("ProductImageDeatail2".equals(charId)) {
									log("Anyway ProductImageDetail2: " + productType);
								}else if("ProductImage2".equals(charId)) {
									log("Anyway ProductImage2: " + productType);
								}
							}
						}
						if(sku00 == null) {
							sku00 = resp.has("sku") ? String.valueOf( resp.getInt("sku") ) : null;
						}
						if(ean00 == null) {
							ean00 = resp.has("gtin") ? resp.getString("gtin") : null;
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
				String brandCode = null;
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
				String rr = null;
				try {
					rr = rc.getRequest("GET", baseUrlDEV + "/object/StructureGroup/'" + template + "'@'PrimaryProductTaxonomy'?entityFilter=StructureGroupAttribute", null);
					org.json.JSONObject tratando = new org.json.JSONObject(rr);
					org.json.JSONArray attributeRow = tratando.getJSONObject("_data").has("attribute") ? tratando.getJSONObject("_data").getJSONArray("attribute") : new org.json.JSONArray();
					for(int a = 0; a<attributeRow.length(); a++) {
						try{
							String val = attributeRow.getJSONObject(a).getJSONArray("value").getJSONObject(0).getString("value");
							appendPlainElementValue(
									val,
									null,
									attributeRow.getJSONObject(a).getJSONObject("_qualification").getString("nameInKeyLang"),
									attributeValues,
									"MKP".equals(business) ? attributesMKT : attributes,
											"MKP".equals(business) ? docMKT : doc,
													propiedadesCaracteristicas);
						}catch(org.json.JSONException e) {
							log("Value not in expected format: " + attributeRow);
							logE(e);
						}
					}
				} catch (org.json.JSONException | IOException e) {
					log("Error in response, got: " + rr);
					System.out.println("Error in response, got: " + rr);
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

				String sku_ = rp.getJSONObject("_data").has("sku") ? String.valueOf( rp.getJSONObject("_data").getLong("sku") ) : null;
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
	        			if(sku_ == null || "".equals(sku_)) {
	        				sku_ = treatment( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
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
		            					if(rows.length() > 0) {
		            						String elcode = rows.getJSONObject(0).getJSONArray("values").getString(0);
		            						appendPlainElementValue(
		            								laetiqueta,
		            								elcode,
		            								"SAP_BEHVO",
		            								attributeValues,
		            								"MKP".equals(business) ? attributesMKT : attributes,
                    								"MKP".equals(business) ? docMKT : doc,
		            								propiedadesCaracteristicas);
		            						behvo = true;
		            					}else {
		            						log("No SAB_BEHVO found for value: " + elese + "|" + laetiqueta);
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
					String elese = "SBB".equals(business) ? itemGroupS4H : itemGroup; // characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
					try {
						rawResponse = rw.makeRequest("GET",
								"/list/StandardizationValue/bySearch" + "?dictionaryProxy=" + encode(
										"'" + (!"SBB".equals(business) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H") + "'")
										+ "&query=" + encode("StandardizationValue.Value equals \"" + elese + "\"")
										+ "&fields=" + encode("StandardizationValue.AlternativeValue") + "",
								null);
						response = new org.json.JSONObject(rawResponse);
						org.json.JSONArray characteristicRecords = response.getJSONArray("rows");
						log("Querying dictionary: "
								+ (!"SBB".equals(business) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H"));
						String laetiqueta = queryDictionary(elese,
								(!"SBB".equals(business) ? "GpoArtVsEnvase" : "GpoArtVsEnvase_S4H"));
						if (characteristicRecords.length() > 0) {
							rawResponse = rw.makeRequest("GET",
									"/list/LookupValue/bySearch" + "?lookup=" + encode("SAP_BEHVOLOV") + "&query="
											+ encode("LookupValueLang.Name(es) equals \"" + laetiqueta + "\"")
											+ "&fields=" + encode("LookupValue.Code") + "",
									null);
							response = new org.json.JSONObject(rawResponse);
							characteristicRecords = response.getJSONArray("rows");
							if (characteristicRecords.length() > 0) {
								String elcode = characteristicRecords.getJSONObject(0).getJSONArray("values")
										.getString(0);
								appendPlainElementValue(laetiqueta, elcode, "SAP_BEHVO", attributeValues, "MKP".equals(business) ? attributesMKT : attributes, "MKP".equals(business) ? docMKT : doc, propiedadesCaracteristicas);
								behvo = true;
							}
						}
					} catch (java.io.IOException | KeyManagementException | NoSuchAlgorithmException
							| URISyntaxException e) {

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
	        	if("SalesItem".equals(productType) && sku_ == null) {
        			keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
        			keyValueSKU.setAttribute("KeyID","SKUID");
        			keyValueSKU.setTextContent( sku_ );
        			product.appendChild(keyValueSKU);
        			appendPlainElementValue(
							sku_,
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
	        			procede = false;
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
								if("MKP".equals(business) && upperRows.length() == 1) {
									if(sku_ == null || "".equals(sku_)) {
										sku_ = sku0;
									}
								}
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
	        							sku_,
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
									System.out.println("\t\tAV: " + characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0) + " || " + entr);
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
									sku_,
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
	        		
	        		if(sku_ != null && !"".equals(sku_)) {
		        		keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
	        			keyValueSKU.setAttribute("KeyID","SKUID");
	        			keyValueSKU.setTextContent( sku_ );
	        			product.appendChild(keyValueSKU);
	        			appendPlainElementValue(
								sku_,
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
						if(sku_ == null || "".equals(sku_)) {
							sku_ = sku0;
						}
						if(sku_ != null && !"".equals(sku_)) {
							keyValueSKU = ("MKP".equals(business) ? docMKT : doc).createElement("KeyValue");
							keyValueSKU.setAttribute("KeyID","SKUID");
							String skuval =  sku_;
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
	        		this.dropped++;
	        	}
    			System.out.println(sku_ + " - " + proposalId);
			}
			if(reqPublishMessage.getJSONArray("rows").length() > 0) {
				java.util.Map<String, String> qp = new java.util.HashMap<>();
				qp.put("includeObjectsInProtocol", "false");
				wrapper.writeData("list", "Product2G", null, qp, reqPublishMessage, this::log);
			}
			java.util.List<?> l1 = rw.getXmm().listImmediateChildElements(productsMKT).get("Product");
			java.util.List<?> l2 = rw.getXmm().listImmediateChildElements(products).get("Product");
			l1 = l1 == null ? new java.util.ArrayList<>() : l1;
			l2 = l2 == null ? new java.util.ArrayList<>() : l2;
			this.products +=  l1.size() + l2.size();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			String serviceResponse = null;
			
			if(!productosLiverpool.isEmpty()) {
				log("LVP products...");
				java.io.StringWriter writer = new java.io.StringWriter();
				transformer.transform(new DOMSource(doc), new StreamResult(writer));
				String xmlOutput = writer
				        .getBuffer()
				        .toString()
				        .replace("&lt;CRLF&gt;", "&#13;&#10;")
				        .replace("<CRLF>", "&#13;&#10;")
				        .replaceAll("&#0*(?:[0-8]|11|12|1[4-9]|2[0-9]|3[01]);", "")
				        .replaceAll("(?i)&#x0*(?:[0-8]|B|C|E|F|1[0-9A-F]);", "");
				java.nio.file.Path fn = java.nio.file.Paths.get( fileSystemPrefixLvp, "pépele" + System.currentTimeMillis() + ".xml" );
				try {
					java.nio.file.Files.writeString(fn, xmlOutput);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(send) {
					RestClient rc = new RestClient("Content-Type: application/xml", "Accept: application/xml");
					try {
						String ll = null;
						log(ll = "[" + new java.text.SimpleDateFormat().format(new java.util.Date()) + "] (Mkt) Request containing: " + productosLiverpool + " sent (local file is: " + fn + "). Resp: " + (serviceResponse = rc.getRequest("POST", urlDeMktStockout, xmlOutput) ) );
						System.out.println("(From Multioferta) " + ll);
						return fn + "<::>" + serviceResponse;
					} catch (IOException e) {
						logE(e);
					}
				}
			}else {
				log("No LVP products...");
			}
			if(!productosMarketplace.isEmpty()) {
				log("Mkt products...");
				java.io.StringWriter writer = new java.io.StringWriter();
				transformer.transform(new DOMSource(docMKT), new StreamResult(writer));
				String xmlOutput = writer
						.getBuffer()
				        .toString()
				        .replace("&lt;CRLF&gt;", "&#13;&#10;")
				        .replace("<CRLF>", "&#13;&#10;")
				        .replaceAll("&#0*(?:[0-8]|11|12|1[4-9]|2[0-9]|3[01]);", "")
				        .replaceAll("(?i)&#x0*(?:[0-8]|B|C|E|F|1[0-9A-F]);", "")
								;
				java.nio.file.Path fn = java.nio.file.Paths.get( fileSystemPrefix, "pépele" + System.currentTimeMillis() + ".xml" );
				try {
					java.nio.file.Files.writeString(fn, xmlOutput);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(send) {
					RestClient rc = new RestClient("Content-Type: application/xml", "Accept: application/xml");
					try {
						String ll = null;
						log("A total size to send: " + xmlOutput.length());
						log( ll = ("[" + new java.text.SimpleDateFormat().format(new java.util.Date()) + "] (Mkt) Request containing: " + productosMarketplace + " sent (local file is: " + fn + "). Resp: " + (serviceResponse = rc.getRequest("POST", urlDeMkt, xmlOutput) ) ) );
						System.out.println(ll);
						return fn + "<::>" + serviceResponse;
					} catch (IOException e) {
						logE(e);
					}
				}
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
		rw.addHeader("Authorization", RealExportProducts2Mirakl.rw.getRc().getHeader().get("Authorization"));
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


	private String getAtributoSapLatalla(String itemGroup, String business) throws ServiceUnavailableException {
		String value = null;
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(baseUrlDEV);
		rw.addHeader("Authorization", RealExportProducts2Mirakl.rw.getRc().getHeader().get("Authorization"));
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
			rawResponse = rw.makeRequest("GET", "/object/StandardizationValue/" + encode("'" + key + "'@'" + dictionary + "'")
					+ ""
					, null);
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
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get( "..","logs","real_export_products_mkt.log").toString(), true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}

	public static java.util.Set<String> YEA;

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
			mapaDeDirecciones = loadFieldDictionaries();
			mapaDeDireccionesAtributoTallaWeb = loadFieldTallaATG();
			mapaDeAtributosFechas = loadAtributosFecha();
			YEA = loadInheritedFields();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}


}
