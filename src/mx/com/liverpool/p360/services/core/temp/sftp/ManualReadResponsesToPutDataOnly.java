package mx.com.liverpool.p360.services.core.temp.sftp;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122ResponseHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.Product122;
import mx.com.liverpool.p360.services.core.sftp.handlers.Value;

public class ManualReadResponsesToPutDataOnly implements Closeable {


	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();

	private static final java.util.Map<String, String> eccFieldMapping = new java.util.TreeMap<>();
	

	private DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
		
		@Override
		public void logE(Exception e) {
			ManualReadResponsesToPutDataOnly.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			ManualReadResponsesToPutDataOnly.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);

	@Override
	public void close() {
		dastub.close();
	}

	static {
		if(java.nio.file.Files.notExists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")))) {
			try{
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")));
			}catch(java.io.IOException e) {
				
			}
		}
		if(java.nio.file.Files.notExists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"))) {
			try{
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"));
			}catch(java.io.IOException e) {
				
			}
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Characteristic.Identifier"
				+ ",Characteristic.DataType"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(ECC)"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(ATG)"
				+ ",Characteristic.Lookup->Lookup.Identifier"
			);
		qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "1500");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toString())))){
			rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> pw.println( workshop.serializeChunk(new Object[] { 
					 row.getJSONArray("values").getString(0)
					,row.getJSONArray("values").getString(1)
					,row.getJSONArray("values").getString(2)
					,row.getJSONArray("values").getString(3)
					,row.getJSONArray("values").getString(4)
					,row.getJSONArray("values").getString(5)
				}, "\"", ";", "\\") ), System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
//		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics"))){
//			lns.map(s -> workshop.parseLine(s, "\"", ";", "\\")).collect(java.util.stream.Collectors.toMap(arr -> arr[0], arr -> java.util.Arrays.copyOfRange(arr, 1, arr.length))).entrySet().forEach(entry -> {
//				eccFieldMapping.put(entry.getKey(), entry.getValue()[1]);
//			});
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pcs = null;
			while((line = br.readLine()) != null) {
				pcs = workshop.parseLine(line, "\"", ";", "\\");
				if(pcs.length > 2)
					eccFieldMapping.put(pcs[0], pcs[2]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		qp.clear();
		qp.put("fields", "Characteristic.Identifier,Characteristic.Entities");
		qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "10000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_entities").toFile())))){
			rw.collectData("list", "Characteristic", null, "bySearch", qp, row->{
				org.json.JSONArray entities = row.getJSONArray("values").getJSONArray(1);
				java.util.LinkedList<String> entitiesList = new java.util.LinkedList<>();
				for(int i=0; i<entities.length(); i++) {
					entitiesList.addLast(entities.getString(i));
				}
				pw.println( rw.getRw().serializeChunk( new String[] { row.getJSONArray("values").getString(0), rw.getRw().serializeChunk( entitiesList.toArray(new String[] {}) ) }, "\"", ";", "\\" ) );
			}, System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) throws ServiceUnavailableException {
		java.util.Set<String> filePathsRaw = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(args[0]), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			while((line = br.readLine()) != null) {
				filePathsRaw.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(!filePathsRaw.isEmpty()) {
			try(ManualReadResponsesToPutDataOnly m = new ManualReadResponsesToPutDataOnly()){
				for(String rawPath : filePathsRaw) {
					try {
						m.processFile(java.nio.file.Paths.get(rawPath));
					} catch (ParserConfigurationException | SAXException | java.io.IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void processFile(java.nio.file.Path path) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {
		long init = System.currentTimeMillis();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.ResDatetime"));
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONArray product2GCharacteristicRecords = new org.json.JSONArray();
		org.json.JSONArray articleCharacteristicRecords = new org.json.JSONArray();
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> attributeValues = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> newAttributeValues = new java.util.TreeMap<>();
		java.util.Map<String, String> dataTypes = new java.util.TreeMap<>();
		java.util.LinkedList<String> product2GCharacteristics = new java.util.LinkedList<>();
		java.util.LinkedList<String> articleCharacteristics = new java.util.LinkedList<>();
		java.util.Map<String, String> unidades = new java.util.TreeMap<>();
		java.util.Map<String, String> articleHigherLevelProduct = new java.util.TreeMap<>();
		java.util.Map<String, String> articleHigherLevelProductNotReadyYet = new java.util.TreeMap<>();
		java.util.Map<String, String> articleBusiness = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> map = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> mapB = new java.util.TreeMap<>();
		String znprst = null;
		String negocio = null;
		String matkl;
		String sku = null;
		String satnr = null;
		String attyp = null;
		String sapBehvo = null;
		String fshId = null;
		String itemId = null;
		String mtart = null;
		String ae253 = null;
		String pe000 = null;
		String[] info = null;
		int cnt = 0;
		org.json.JSONObject data = null;
		java.util.Map<String, String> articleSupplierAIDToSKU = new java.util.TreeMap<>();
		java.util.Map<String, String> skuToArticleSupplierAID = new java.util.TreeMap<>();
		collectCharacteristicsByEntity(product2GCharacteristics, articleCharacteristics);
		java.util.Map<String, String> lkps = new java.util.TreeMap<>();
		java.util.LinkedList<Product122> products = null;
		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        ECC122ResponseHandler erh = new ECC122ResponseHandler();
        parser.parse(new java.io.File(path.toString()), erh);
        products = erh.getCollected();
        log("FOUND: " + products.size() + " products");
		for(Product122 p : products) {
			znprst = p.getProposalId();
			log("P: " + p.getProposalId());
			for(Value vn : p.getValues()) {
				if(!"".equals(vn.getText()) && vn.getText() != null){
					sb.append(sb.length() > 0 ? "," : "")
						.append(vn.getAttributeId())
					;
					attributeValues.put(vn.getAttributeId(), vn.getText() == null ? "" : vn.getText());
					if(vn.getText() != null && vn.getText().matches("^(unece\\.unit\\.)[A-Z0-9]+$")) {
						unidades.put(vn.getAttributeId(), vn.getText());
					}else {
					}
				}
				if("MATNR".equals(vn.getAttributeId()) /* || "EXTWG".equals(vn.getAttributeId()) || "LIFNR".equals(vn.getAttributeId()) */) {
					log("MATNR ----------------->" + vn.getText());
				}
			}
			collectLookupCharacteristics(sb.toString(), dataTypes, lkps);
			sb.setLength(0);
			for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
				collectLookupValues(lkps.get( entry.getKey() ), map, mapB, entry.getValue());
			}
			for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
				try{
					if(!unidades.containsKey(entry.getKey()) && product2GCharacteristics.contains(entry.getKey()) ) {
						addValue(entry.getKey(), resolveDataType(entry.getKey(), entry.getValue(), lkps.get(entry.getKey()), attributeValues.get(eccFieldMapping.get(entry.getKey())), map, mapB), product2GCharacteristicRecords );
					}
					if(!unidades.containsKey(entry.getKey()) && articleCharacteristics.contains(entry.getKey()) ) {
						addValue(entry.getKey(), resolveDataType(entry.getKey(), entry.getValue(), lkps.get(entry.getKey()), attributeValues.get(eccFieldMapping.get(entry.getKey())), map, mapB), articleCharacteristicRecords );
					}
				}catch(IllegalArgumentException e) {
					logE(e);
				}
			}
			agregaUnidadesDeMedida(unidades, product2GCharacteristicRecords, eccFieldMapping);
			negocio = attributeValues.get( eccFieldMapping.get("Negocio") );
			sku = attributeValues.get( "MATNR" );
			matkl = attributeValues.get( "MATKL" );
			pe000 = attributeValues.get("PE000");
			sapBehvo = attributeValues.get("SAP_BEHVO");
			fshId = attributeValues.get("FSH_ID");
			
			try {
				calculaProductType(sapBehvo, matkl, fshId, negocio, ae253, mtart, mtart, articleCharacteristicRecords, workshop);
			} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
				e.printStackTrace();
			}
			itemId = null;
			satnr = attributeValues.get( "SATNR" );
			attyp = attributeValues.get( "ATTYP" );
			znprst = znprst == null ? attributeValues.get( "ZNPRST" ) : znprst;
			if(znprst != null && sku != null) {
				articleSupplierAIDToSKU.put(sku, znprst);
				skuToArticleSupplierAID.put(znprst, sku);
			}
			if(znprst != null && !"".equals(znprst)) {
				info = checkProduct(znprst);
				if( info == null ) {
					info = checkArticle(znprst);
					if( info != null ) {
						if("00".equals(info[1]) && !"MKP".equals(info[2])) {
							addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), product2GCharacteristicRecords );
							sendWriteRequest("Product2G", info[0], product2GCharacteristicRecords, info[3], info[4]);
							newAttributeValues.put(info[0], attributeValues);
						}else if("00".equals(info[1]) && "MKP".equals(info[2])) {
							checkParentVariantsCompleteness(info[0], znprst, product2GCharacteristicRecords, info[3], info[4]);
							newAttributeValues.put(info[0], attributeValues);
						}
						addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
						sendWriteRequest("Article", znprst, articleCharacteristicRecords, null, null);
						log("Was Article (" + znprst + "), " + java.util.Arrays.asList(info));
					}else {
						log("Not a known product (" + znprst + ") <:>" + negocio + "<:>");
						if("00".equals(attyp)) {
							addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), product2GCharacteristicRecords);
							addValue("Business", new org.json.JSONObject().put("_code", "LVP" ), product2GCharacteristicRecords);
							addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), articleCharacteristicRecords);
							sendWriteRequest("Article",  znprst, articleCharacteristicRecords, null, null);
							sendWriteRequestProduct(znprst, pe000, negocio, product2GCharacteristicRecords);
							newAttributeValues.put(znprst, attributeValues);
							articleBusiness.put(znprst, negocio);
							articleHigherLevelProduct.put(znprst, znprst);
						}else if("01".equals(attyp)) {
							addValue("SAPObjectType", new org.json.JSONObject().put("_code", "01"), product2GCharacteristicRecords);
							addValue("Business", new org.json.JSONObject().put("_code", "LVP" ), product2GCharacteristicRecords);
							sendWriteRequestProduct(znprst, pe000, negocio, product2GCharacteristicRecords);
							newAttributeValues.put(znprst, attributeValues);
						}else {
							addValue("SAPObjectType", new org.json.JSONObject().put("_code", "02" ), articleCharacteristicRecords);
							sendWriteRequest("Article", "LVP" + sku, articleCharacteristicRecords, null, null);
							/** Easy with that satnr, need to get the real Id, maybe by iterating over all components or by querying from system. **/
							articleHigherLevelProductNotReadyYet.put(znprst, satnr);
							articleBusiness.put(znprst, negocio);
						}
					}
				}else {
					log("Was Product (" + znprst + "), " + java.util.Arrays.asList(info));
					addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), product2GCharacteristicRecords );
					if("00".equals(info[0])) {
						itemId = getArticleIdFromProduct(znprst);
						log("Article.SupplierAID: " + itemId);
						addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
						sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
					}
					if("MKP".equals(info[1])) {
						removeCharacteristicFromRecords("SKU", product2GCharacteristicRecords);
						addValue("SKU", "999" + znprst.substring(9), product2GCharacteristicRecords );
					}
					newAttributeValues.put(znprst, attributeValues);
					sendWriteRequest("Product2G", znprst, product2GCharacteristicRecords, info[2], info[3]);
				}
			}else {
				log("No znprst found");
				if(sku != null && !"".equals(sku)) {
					info = checkProductBySKU(sku);
					if(info != null) {
						log("Counter a");
						log("Found SKU in product");
						log("-->" + java.util.Arrays.asList(info));
						addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), product2GCharacteristicRecords );
						sendWriteRequest("Product2G", info[0], product2GCharacteristicRecords, info[3], info[4]);
						if("00".equals(info[1])) {
							addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
							sendWriteRequest("Article", info[0], articleCharacteristicRecords, null, null);
							newAttributeValues.put(info[0], attributeValues);
						}
						newAttributeValues.put(info[0], attributeValues);
						log("Value sent fo writting");
					} else {
						itemId = checkArticleBySKU(sku);
						if(itemId != null) {
							log("Counter b");
							log("Found SKU in attribute.");
							addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
							sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
						} else {
							log("Brand new SKU for P360: " + sku + " (" + znprst + ")");
							log("Not a known product (" + znprst + ") <:>" + negocio + "<:>");
							if("00".equals(attyp)) {
								addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), product2GCharacteristicRecords);
								addValue("Business", new org.json.JSONObject().put("_code", "LVP" ), product2GCharacteristicRecords);
								addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), articleCharacteristicRecords);
								sendWriteRequest("Article",   "LVP" + sku, articleCharacteristicRecords, null, null);
								sendWriteRequestProduct("LVP" + sku, pe000, negocio, product2GCharacteristicRecords);
								newAttributeValues.put("LVP" + sku, attributeValues);
								articleBusiness.put("LVP" + sku, negocio);
								articleHigherLevelProduct.put("LVP" + sku, "LVP" + sku);
							}else if("01".equals(attyp)) {
								addValue("SAPObjectType", new org.json.JSONObject().put("_code", "01"), product2GCharacteristicRecords);
								addValue("Business", new org.json.JSONObject().put("_code", "LVP" ), product2GCharacteristicRecords);
								sendWriteRequestProduct("LVP" + sku, pe000, negocio, product2GCharacteristicRecords);
								newAttributeValues.put("LVP" + sku, attributeValues);
							}else {
								addValue("SAPObjectType", new org.json.JSONObject().put("_code", "02" ), articleCharacteristicRecords);
								sendWriteRequest("Article", "LVP" + sku, articleCharacteristicRecords, null, null);
								articleHigherLevelProduct.put("LVP" + sku, "LVP" + satnr);
								articleBusiness.put("LVP" + sku, negocio);
							}
						}
					}
				} else {
					log("No SKU found either!");
				}
			}
			negocio = null;
			sku = null;
			satnr = null;
			znprst = null;
			attyp = null;
			itemId = null;
			info = null;
			product2GCharacteristicRecords = new org.json.JSONArray();
			articleCharacteristicRecords = new org.json.JSONArray();
			articleBusiness.clear();
			unidades.clear();
			attributeValues = new java.util.TreeMap<>();
			cnt++;
			log(cnt + "/" + products.size());
		}
		log("Writing relationships");
		org.json.JSONArray items = new org.json.JSONArray();
		org.json.JSONObject item = null;
		for( java.util.Map.Entry<String, String> entry : articleHigherLevelProduct.entrySet() ) {
			item = new org.json.JSONObject();
			item.put("supplierAID", entry.getKey());
			item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
			item.put("productNo", entry.getValue());
			items.put(item);
			data = new org.json.JSONObject();
			data.put("higherLevelProduct", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("referencedIdentifier", entry.getValue()))));
			updateArticleHigherLevelProduct(entry.getKey(), data);
		}
		String parentId = null;
		for( java.util.Map.Entry<String, String> entry : articleHigherLevelProductNotReadyYet.entrySet() ) {
			parentId = skuToArticleSupplierAID.get(entry.getValue());
			if(parentId == null) {
				log("En el archivo no estaba el padre referenciado por el valor de SKU: " + entry.getValue() + " para la variante con id de sistema: " + entry.getKey());
				String response = dr.productBySKU( new org.json.JSONArray().put(entry.getValue()) );
				try {
					org.json.JSONObject jr = new org.json.JSONObject(response);
					org.json.JSONArray ir = jr.getJSONArray("items");
					parentId = ir.getString(0);
					log("Recuperamos el padre gracias al admin: " + parentId + " para SKU: " + entry.getValue() + ", de la propuesta variante: " + entry.getKey());
				}catch(org.json.JSONException e) {
					logE(e);
				}
			}
			if(parentId != null) {
				item = new org.json.JSONObject();
				item.put("supplierAID", entry.getKey());
				item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
				item.put("parentId", parentId);
				items.put(item);
			}
		}
		dr.putSkuSupplierAID(items);
		log("Done processing file. [" + path.toString().replaceAll(".+" + java.util.regex.Pattern.quote( java.io.File.separator ), "") + "] " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	private void agregaClasificacion(String itemGroup, org.json.JSONObject data) {
		if(itemGroup == null || "".equals(itemGroup)) {
			return;
		}
		org.json.JSONArray structureGroupMap = null; //		
		if(data.has("structureGroupMap")) {
			structureGroupMap = data.getJSONArray("structureGroupMap") ;
		}else {
			structureGroupMap = new org.json.JSONArray();
			data.put("structureGroupMap", structureGroupMap);
		}
		for(int i=0; i<structureGroupMap.length(); i++) {
			if(structureGroupMap.getJSONObject(i).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").endsWith("'@'CommercialECC'")) {
				return;
			}
		}
		structureGroupMap.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + itemGroup + "-L5ECC'@'CommercialECC'"))));
	}
	
	private void updateArticleHigherLevelProduct(String articleId, org.json.JSONObject data) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = data;
		org.json.JSONObject response = null;
		log("/object/Article/'" + articleId + "'@'MASTER'");
		response = workshop.makeRequest("PUT", "/object/Article/'" + articleId + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			log("On writing Article Id (for relationship establishment): " + articleId + ": " + response);
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
	}
	
	private void agregaUnidadesDeMedida(java.util.Map<String, String> unidades, org.json.JSONArray characteristicRecords, java.util.Map<String, String> eccFieldMapping) {
		java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
		unidadesVolumen.put("unece.unit.FTQ", "PI3");
		unidadesVolumen.put("unece.unit.MTQ", "M3");
		unidadesVolumen.put("unece.unit.GRM", "G");
		String unidadDeMedidaLongitud = null;
		String unidadDeMedidaVolumen = null;
		String unidadDeMedidaPeso = null;
		String[] atributosLongitud = new String[] { "MEABM", "ZMEACJ", "ZMEAPQ" };
		String[] atributosVolumen = new String[] { "VOLEH", "ZVOLEH", "ZVOLEHPQ" };
		String[] atributosPeso = new String[] { "GEWEI", "ZGEWCJ", "ZGEWPQ" };
		String unidadId = null;
//		log("Gonna do this: " + eccFieldMapping);
//		log("Gonna check units: " + unidades);
		for(String a : atributosLongitud) {
			unidadId = unidades.get( a );
			if(unidadId != null) {
				unidadDeMedidaLongitud = unidadesLongitud.get(unidadId);
				break;
//			}else {
//				log("\tUnidad de medida de longitud no conocida: " + unidadId + ", attribute: " + a);
			}
		}
		for(String a : atributosVolumen) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaVolumen = unidadesVolumen.get( unidadId );
				break;
//			}else {
//				log("\tUnidad de medida de volumen no conocida: " + unidadId + ", attribute: " + a);
			}
		}
		for(String a : atributosPeso) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaPeso = unidadesPeso.get( unidadId );
				break;
//			}else {
//				log("\tUnidad de medida de peso no conocida: " + unidadId + ", attribute: " + a);
			}
		}
		if(unidadDeMedidaLongitud == null) {
//			log("No se obtuvo una unidad de medida de longitud");
		} else {
//			log("Added Unidad de Medida de Longitud: " + unidadDeMedidaLongitud);
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaLongitud", new org.json.JSONObject().put("_code", unidadDeMedidaLongitud) ) );
		}
		if(unidadDeMedidaPeso == null) {
//			log("No se obtuvo unidad de medida de peso");
		}else {
//			log("Added Unidad de Medida de Peso: " + unidadDeMedidaPeso);
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaPeso", new org.json.JSONObject().put("_code", unidadDeMedidaPeso) ) );
		}
		if(unidadDeMedidaVolumen == null) {
//			log("No se obtuvo unidad de medida de volumen");
		}else {
//			log("Added Unidad de Medida de Volumen: " + unidadDeMedidaVolumen);
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaVolumen", new org.json.JSONObject().put("_code", unidadDeMedidaVolumen) ) );
		}
	}

	private static org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}
	
	private void checkParentVariantsCompleteness(String productId, String itemId, org.json.JSONArray characteristicRecords, String fotosTomaLiverpool, String currentStatus) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				if(!itemId.equals(values.getString(0)) && "".equals(values.getJSONArray(1).getString(0)))
					return;
			}
			addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), characteristicRecords );
			removeCharacteristicFromRecords("SKU", characteristicRecords);
			addValue("SKU", "999" + productId.substring(9), characteristicRecords );
			sendWriteRequest("Product2G", productId, characteristicRecords, fotosTomaLiverpool, currentStatus);
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
	}
	
	private void removeCharacteristicFromRecords(String charId, org.json.JSONArray characteristicRecords) {
		if(charId == null || characteristicRecords == null)
			return;
		java.util.LinkedList<Integer> toRemove = new java.util.LinkedList<>();
		for(int i=0; i<characteristicRecords.length(); i++) {
			if(charId.equals(characteristicRecords.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
				toRemove.addFirst(i);
			}
		}
		for(Integer i : toRemove) {
			characteristicRecords.remove(i);
		}
	}
	
	private String getArticleIdFromProduct(String productId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows.length() == 1) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			}
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}
	
	private void sendWriteRequestProduct(String id, String itemGroup, String negocio, org.json.JSONArray characteristicRecords) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("_characteristicRecords", characteristicRecords);
		if(id.startsWith("LVP")) {
			request.put("currentStatus", 
					new org.json.JSONObject().put("_key", 
							"SFERA".equals(negocio) ? 1021 : 
								"DUTY FREE".equals(negocio) ? 1001 : 
									"MARCAS PROPIAS".equals(negocio) ? 1002 : 
										"REGULAR".equals(negocio) ? 1002 : 
											"SERVICIOS".equals(negocio) ? 1002 : 1001 )
				);
			request.put("previousStatus", new org.json.JSONObject().put("_key", 1020));
			request.put("externalStatus", 
					new org.json.JSONObject().put("_Code", 
							"SFERA".equals(negocio) ? "EnProcesoLiverpool" : 
								"DUTY FREE".equals(negocio) ? "PropuestaGenerada" : 
									"MARCAS PROPIAS".equals(negocio) ? "EnProcesoLiverpool" : 
										"REGULAR".equals(negocio) ? "EnProcesoLiverpool" : 
											"SERVICIOS".equals(negocio) ? "EnProcesoLiverpool" : "PropuestaGenerada" )
				);
		}
		if( "MARCAS PROPIAS".equals(negocio) || "REGULAR".equals(negocio) || "SERVICIOS".equals(negocio) ) {
			addValue("EnriquecidoEnForo", true, characteristicRecords );
		} else {
			addValue("EnriquecidoEnForo", true, characteristicRecords );
		}
		agregaClasificacion(itemGroup, request);
		org.json.JSONObject response = null;
		log("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()) + "] Setting classification for ECC: " + request);
		response = workshop.makeRequest("PUT", "/object/Product2G/'" + id + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			log(String.valueOf( response ));
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
	}
	
	private void sendWriteRequest(String entity, String id, org.json.JSONArray characteristicRecords, String fotoTomadaLiverpool, String currentStatus) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		if(characteristicRecords != null)
			request.put("_characteristicRecords", characteristicRecords);
		log("---->" + entity + ", ---->" + currentStatus);
		org.json.JSONObject response = null;
		log("/object/" + entity + "/'" + id + "'@'MASTER'");
		response = workshop.makeRequest("PUT", "/object/" + entity + "/'" + id + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			log(String.valueOf( response ));
		}else {
			log("ERR: " + workshop.getRawResponse());
			log("REQ is: " + request);
		}
	}
	
	private String[] checkProductBySKU(String sku) {
		String resp = dr.productBySKU(new org.json.JSONArray().put(sku));
		if(resp != null) {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(resp);
				org.json.JSONArray items = jr.getJSONArray("items");
				String pid = items.getString(0);
				if(!"".equals(pid)) {
					String[] res = checkProduct(pid);
					return res == null ? null : new String[] { pid, res[0], res[1], res[2], res[3] };
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
		return null;
	}
	
	private String checkArticleBySKU(String sku) {
		String resp = dr.articleBySKU(new org.json.JSONArray().put(sku));
		if(resp != null) {
			try {
				org.json.JSONObject r = new org.json.JSONObject(resp);
				org.json.JSONArray items = r.getJSONArray("items");
				org.json.JSONObject j = items.getJSONObject(0);
				return !"".equals( j.getString("product") ) ? j.getString("product") : null;
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
		return null;
	}
	
	private String[] checkProduct(String id) {
		try {
				String resp = dr.getProductData(new org.json.JSONArray().put(id));
				if(resp != null) {
					org.json.JSONObject rj = new org.json.JSONObject(resp);
					org.json.JSONArray items = rj.getJSONArray("items");
					org.json.JSONObject j = items.getJSONObject(0);
					log("Me lo pidieron (producto): " + j);
					if(
						j.has("SAPObjectType") 
						&& !"".equals(j.getString("SAPObjectType")) 
						&& !"02".equals(j.getString("SAPObjectType")))
						return new String[] {
								 j.getString("SAPObjectType")
								,j.getString("Business")
								,j.getString("FotoTomadaLiverpool")
								,j.getString("CurrentStatus")
						};
					else return null;
				}
		}catch(org.json.JSONException e) {
			logE(e);
		}
		return null;
	}
	
	private String[] checkArticle(String id) {
		String resp = dr.getProductByVariant(new org.json.JSONArray().put(id));
		if(resp != null) {
			try {
				org.json.JSONObject rj = new org.json.JSONObject(resp);
				org.json.JSONArray items = rj.getJSONArray("items");
				String pid = items.getString(0);
				if(!"".equals(pid)) {
					resp = dr.getProductData(new org.json.JSONArray().put(pid));
					rj = new org.json.JSONObject(resp);
					items = rj.getJSONArray("items");
					org.json.JSONObject j = items.getJSONObject(0);
					log("Me lo pidieron (item): " + j);
					return new String[] {
							 pid
							,j.getString("SAPObjectType")
							,j.getString("Business")
							,j.getString("FotoTomadaLiverpool")
							,j.getString("CurrentStatus")
					};
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
		return null;
	}
	
	private void collectCharacteristicsByEntity(java.util.LinkedList<String> product2G, java.util.LinkedList<String> article) {
		try( java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_entities")) ){
			lns.forEach(s -> {
				String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
				String[] entities = workshop.parseLine(pieces[1], "\"", ",", "\\");
				for(int i=0; i<entities.length; i++) {
					if("Product2G".equals(entities[i])) {
						product2G.addLast(pieces[0]);
					}else if("Article".equals(entities[i])) {
						article.addLast(pieces[0]);
					}
				}
			});
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	private void collectLookupValues(String lkpId, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB, String dataType){
		if("LOOKUP".equals(dataType) && lkpId != null) {
			java.util.Map<String, String> data = null; // map.get(lkpId);
			data = getData(lkpId);
			map.put(lkpId, data);
			data = getDataB(lkpId);
			mapB.put(lkpId, data);
		}
	}
	
	private java.util.Map<String, String> getData(String lkpId){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		if(lkpId != null) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")))){
				lns.forEach(s -> {
					String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
					data.put(pieces[0], pieces[1]);
				});
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		return data;
	}
	
	private java.util.Map<String, String> getDataB(String lkpId){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		if(lkpId != null) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")))){
				lns.forEach(s -> {
					String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
					data.put(pieces[1], pieces[0]);
				});
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		return data;
	}
	
//	private String getMeTheLookup(String characteristicIdentifier) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "Characteristic.Lookup->Lookup.Identifier");
//		qp.put("query", "Characteristic.Identifier equals \"" + characteristicIdentifier + "\"");
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
//		if(response != null) {
//			rows = response.getJSONArray("rows");
//			if(rows.length() > 0) {
//				return rows.getJSONObject(0).getJSONArray("values").getString(0);
//			}
//		}
//		return null;
//	}
	
//	private java.util.Map<String, String> collectLookupValues(String charId) {
//		java.util.Map<String, String> keyValues = new java.util.TreeMap<>();
//		log("Loading regular data for: " + charId);
//		String lookup = getMeTheLookup(charId);
//		if(lookup == null || "".equals(lookup)) {
//			return null;
//		}
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
//		qp.put("query", "LookupValue.IsActive = true");
//		qp.put("lookup", "'" + lookup + "'");
//		qp.put("pageSize", "1200");
//		int currentIndex = 0;
//		int totalSize = 0;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//			if(response != null) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					currentIndex++;
//					values = rows.getJSONObject(i).getJSONArray("values");
//					keyValues.put(values.getString(0), values.getString(1));
//				}
//			}else {
//				log("ERR: " + workshop.getRawResponse());
//			}
//		}while(currentIndex < totalSize);
//		currentIndex = 0;
//		return keyValues;
//	}
	
//	private java.util.Map<String, String> collectLookupValuesBackwards(String charId) {
//		java.util.Map<String, String> keyValues = new java.util.TreeMap<>();
//		String lookup = getMeTheLookup(charId);
//		if(lookup == null || "".equals(lookup)) {
//			return null;
//		}
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "LookupValueLang.Name(es),LookupValue.Code");
//		qp.put("query", "LookupValue.IsActive = true");
//		qp.put("lookup", "'" + lookup + "'");
//		qp.put("pageSize", "1200");
//		int currentIndex = 0;
//		int totalSize = 0;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//			totalSize = response.getInt("totalSize");
//			rows = response.getJSONArray("rows");
//			for(int i=0; i<rows.length(); i++) {
//				currentIndex++;
//				values = rows.getJSONObject(i).getJSONArray("values");
//				keyValues.put(values.getString(0), values.getString(1));
//			}
//		}while(currentIndex < totalSize);
//		currentIndex = 0;
//		return keyValues;
//	}
	
	private Object resolveDataType(String charId, String dataType, String lkpId, String value, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB) {
		if(dataType != null && value != null) {
			if("LOOKUP".equals(dataType)) {
				String code = null;
				String label = null;
				if("UnidadDeMedidaPeso".equals(charId)) {
					value = unidadesPeso.get(value);
				}else if("UnidadDeMedidaLongitud".equals(charId)) {
					value = unidadesLongitud.get(value);
				}else if("UnidadDeMedidaVolumen".equals(charId)) {
					value = unidadesVolumen.get(value);
				}else {
					java.util.Map<String, String> lkp = map.get(lkpId);
					if(lkp != null) {
						label = lkp.get(value);
					}
					java.util.Map<String, String> lkpB = mapB.get(lkpId);
					if(lkpB != null && label == null) {
						code = lkpB.get(value);
					}
					if(code == null && label == null) {
						log("Unknown value found: " + value + " for Characteristic: " + charId + ", data for core: " + ( lkp == null ? null : lkp.size() > 20 ? "Too long to show" : lkp));
					}
				}
				if(label == null && code == null)
					return null;
				return new org.json.JSONObject().put( "_code", label != null ? value : code);
			}else if("INTEGER".equals(dataType)) {
				try{
					return new java.math.BigDecimal(value).intValue();
				}catch(NumberFormatException e) {
					logE(e);
				}
			}else if("DECIMAL".equals(dataType)) {
				try {
					return new java.math.BigDecimal(value).floatValue();
				}catch(NumberFormatException e) {
					logE(e);
				}
			}else if("BOOLEAN".equals(dataType)) {
				return Boolean.parseBoolean(value);
			}else if("DATE".equals(dataType)) {
				try{
					return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.text.SimpleDateFormat().parse(value));
				}catch(java.text.ParseException e) {
					logE(e);
				}
			}
		}
		return value;
	}
	
	private void addValue(String name, Object value, org.json.JSONArray values) {
		if(value == null)
			return;
		values.put( new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", name))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put( value )))) );
	}
	
	private void collectLookupCharacteristics(String names, java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> lkps){
		java.util.Set<String> pieces = new java.util.TreeSet<>(java.util.Arrays.asList( names.split(",") ) );
		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] erg = null;
			while((line = br.readLine()) != null) {
				erg = workshop.parseLine(line, "\"", ";", "\\");
				if(erg != null && erg.length == 6) {
					data.put(erg[2], erg);
				}else {
					log("Pieza malformada: --->" + line + "<---");
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		/*
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics"))){
			lns.map(s -> workshop.parseLine(s, "\"", ";", "\\")).forEach(arr -> data.put(arr[2], arr))
			;
//			  .entrySet().forEach(entry -> {
//				if((!"".equals(entry.getValue()[1]) && pieces.contains(entry.getValue()[1])) || (!"".equals(entry.getValue()[2]) && pieces.contains(entry.getValue()[2]))) {
//					characteristicsInfo.put(entry.getKey(), entry.getValue()[0]);
//					lkps.put(entry.getKey(), entry.getValue()[4]);
//				}
//				if(names.contains("NUMTP")) {
//					log("------------------------------------->" + );
//				}
//			});
		}catch(java.io.IOException e) {
			logE(e);
		}
		*/
		String[] d ;
		for(String piece : pieces) {
			d = data.get(piece);
			if(d != null) {
				characteristicsInfo.put(d[0], d[1]);
				lkps.put(d[0], d[5]);
//				log("Se agregó: " + piece + " (" + d[0] + "), con tipo de dato: " + d[1] + ", y LKP: " + d[5]);
			}else {
//				log("No se encontró este atributo SAP: --->" + piece +"<---");
			}
		}
		/*
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,Characteristic.DataType,CharacteristicIdentifier.AlternativeIdentifier(ECC)");
		qp.put("query", "CharacteristicIdentifier.AlternativeIdentifier(ECC) in (" + names + ")");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				if("".equals(values.getString(0))) {
					log("Not found a characteristic for: " + pieces[i]);
				}else {
					characteristicsInfo.put(values.getString(0), values.getString(1));
					eccMapping.put(values.getString(0), values.getString(2));
				}
			}
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
		*/
	}
	
	private void calculaProductType(String sapBehvo1, String itemGroup, String fshId, String negocio, String almacenamientoAtt, String skuType, String mtart, org.json.JSONArray newCharacteristicRecords, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ServiceUnavailableException {
		String sapBehvo = null;
		if("Liverpool".equals(negocio) || "Marketplace".equals(negocio)) {
			int month = Integer.parseInt( new java.text.SimpleDateFormat("MM").format(new java.util.Date()) );
			int year = Integer.parseInt( new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) ) + (month < 11 ? 0 : 1);
			newCharacteristicRecords.put( createCharacteristicValueObject("AnoEstacion", String.valueOf(year) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("Temporada", new org.json.JSONObject().put("_code", "0003") ) );
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase", rw);
		}else if("Suburbia".equals(negocio)) {
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase_S4H", rw);
			if(fshId != null && fshId.length() >= 4) {
				newCharacteristicRecords.put( createCharacteristicValueObject("FSH_SEASON_YEAR",  fshId.subSequence(0, 4)) );
			}
		}
		if(sapBehvo == null || "".equals(sapBehvo)) {
			return;
		}
		sapBehvo = sapBehvo.substring(0,2);
		if(sapBehvo1 == null || "".equals(sapBehvo1)) {
			newCharacteristicRecords.put( createCharacteristicValueObject("SAP_BEHVO", new org.json.JSONObject().put("_code", sapBehvo.substring(0,2) )) );
		}
		if(sapBehvo != null && !"".equals(sapBehvo)) {
			System.out.println("Got " + sapBehvo + " for SAP_BEHVO.");
			String thevalue = "1";
			try{
				org.json.JSONArray rws = new org.json.JSONObject( rw.getRc().getRequest("GET", rw.getBaseUrl() + "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \"" + sapBehvo.substring(0,2) + "\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8")
						, null) ).getJSONArray("rows");
				System.out.println("Checking sapBehvo: " + sapBehvo);
				if(rws.length() > 0) {
					thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
			newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", thevalue) ) );
			System.out.println("Placing value: " + thevalue + " for ProductType");
		} else {
//			newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "1") ) );
//			newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "1") ) );
			System.out.println("No SAP_BEHVO found, placing value 1.");
			if(almacenamientoAtt != null && !"".equals(almacenamientoAtt) && "0001".equals(almacenamientoAtt) && "SERV".equals(skuType)) {
				newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "6") ) );
			}else if("DIEN".equals(mtart) && "SB87516".equals(itemGroup)){
				newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "6") ) );
			}else {
				newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "1") ) );
			}
		}
	}
	
	private String lookupValue(String value, String standardizationDictionary, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException {
		String container = standardizationDictionary.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				if(value.equals(pieces[0]))
					return pieces[1];
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return null;
		/*
		String rr = null;
		org.json.JSONObject resp = null;
		org.json.JSONArray rows = null;
		rr = rw.getRc().getRequest("GET", rw.getBaseUrl() + "/list/StandardizationValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'" + standardizationDictionary + "'", "UTF-8")
			+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8") + "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + standardizationDictionary + "\" and StandardizationValue.Value equals \"" + value + "\"", "UTF-8"), null);
		System.out.println("Value: " + value + " in " + standardizationDictionary + ": " + rr);
		resp = new org.json.JSONObject(rr);
		rows = resp.getJSONArray("rows");
		if(rows.length() > 0) {
			return rows.getJSONObject(0).getJSONArray("values").getString(0);
		}
		return null;
		*/
	}

	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","manualReadResponse.log").toString(), true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","manualReadResponse.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

	private static final java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();

	static {
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesPeso.put("unece.unit.GRM", "G");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
	}
}
