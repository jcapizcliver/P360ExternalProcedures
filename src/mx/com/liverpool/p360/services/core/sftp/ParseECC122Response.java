package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.SimpleLog;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122AttributesHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122ResponseHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.Product122;
import mx.com.liverpool.p360.services.core.sftp.handlers.Value;
import mx.com.liverpool.p360.services.core.temp.exports.RealExportProductsExpressOMS;

public class ParseECC122Response extends Thread implements SimpleLog {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	private static final String BASE_URL = workshop.getBaseUrl();
	
	private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final String USER = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.ecc.remote_directory_122" );//Remote directory to monitor: /interfase/mer/in/step/P360/zrtuab122
	private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.ecc.local_processed_dir_122" ));//Path: /u01/stage/ecc.122/processed
	private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.ecc.state_file_122" ));//File: processed_ecc.122.properties
	private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.ecc.use_cache" ));//false;

	private final PubSubGCP postProductsPubSub = new PubSubGCP(
	        PropertiesManager.get("p360.contingency.gcp.service_account_back"),
	        PropertiesManager.get("p360.contingency.gcp.project_back"),
	        PropertiesManager.get("p360.contingency.gcp.post_products_topic")
	);
	
	private static final java.util.Map<String, String> charIDToECC = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> eccToCharID = new java.util.TreeMap<>();
	private static final java.util.LinkedList<String> product2GCharacteristics = new java.util.LinkedList<>();
	private static final java.util.LinkedList<String> articleCharacteristics = new java.util.LinkedList<>();
	private static final java.util.Map<String, String> articleHigherLevelProduct = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> articleSupplierAIDToSKU = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> skuToArticleSupplierAID = new java.util.TreeMap<>();
	private static final java.util.List<String> variantesConImagen = new java.util.ArrayList<>();
	private static final java.util.List<String> propuestasRevisadas = new java.util.ArrayList<>();

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
	

	private static final ParseECCAttributesFile attp = new ParseECCAttributesFile();
	
	private final java.util.List<String> pids = new java.util.ArrayList<>(); 
	
	private static final ParsersTools tools = new ParsersTools(new SimpleLog() {
		
		@Override
		public final void log(String message) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
					new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECC122Response.log").toString(), true)))) {
				pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
						+ "]  " + message);
			} catch (java.io.IOException e) {
			}
		}

		@Override
		public final void logE(Exception ex) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
					new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECC122Response.log").toString(), true)))) {
				ex.printStackTrace(pw);
			} catch (java.io.IOException e) {
			}
		}
	});
	
	@Override
	public void run() {
		try {
			attp.sendData();
			sendData();
		}finally {
			closeResources();
			this.running = false;
		}
	}
	
	private static final org.json.JSONObject requestStatus = new org.json.JSONObject()
				.put("columns", new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))
						.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
						.put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus"))
					)
				.put("rows", new org.json.JSONArray())
			;
	private static final org.json.JSONObject requestCommercialECC = new org.json.JSONObject()
			.put("columns", new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('CommercialECC')"))
				)
			.put("rows", new org.json.JSONArray())
		;
	

	private final org.json.JSONArray rowsSKU = new org.json.JSONArray();
	private final org.json.JSONArray columnsSKU = new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "Product2G.SKU"))
			;
	private final org.json.JSONObject requestSKU = new org.json.JSONObject().put("columns", columnsSKU).put("rows", rowsSKU);
    private final org.json.JSONArray rows = new org.json.JSONArray();
    private final org.json.JSONArray columns = new org.json.JSONArray()
    			.put(new org.json.JSONObject().put("identifier", "Product2G.EAN"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.SKU"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.Business"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionShort(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Direccion(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Section(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.ItemGroup(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.BrandName(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Negocio(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SAPObjectType(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierID(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierPartNumber(MX)"))
			;
    private final org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows", rows);
    private final org.json.JSONArray columnsArticle = new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "Article.EAN"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.SKU"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.Business"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleLang.DescriptionShort(es)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SupplierPartNumber(MX)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)"))
    		;
    private final org.json.JSONArray rowsArticle = new org.json.JSONArray();
    private final org.json.JSONObject requestArticle = new org.json.JSONObject().put("columns", columnsArticle).put("rows", rowsArticle);

	private final java.util.Map<String, String> dataTypes = new java.util.TreeMap<>();
	private final java.util.Map<String, String> lkps = new java.util.TreeMap<>();
	private final java.util.Map<String, String> qp = new java.util.HashMap<>();
	private final java.util.Map<String, java.util.Map<String, String>> map = new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Map<String, String>> mapB = new java.util.TreeMap<>();

	private boolean running = true;
	
	static {
		if(java.nio.file.Files.notExists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")))) {
			try{
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")));
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		if(java.nio.file.Files.notExists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"))) {
			try{
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"));
			}catch(java.io.IOException e) {
				e.printStackTrace();
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
		qp.put("query", "Characteristic.IsActive = true and not Characteristic.Entities is empty and Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "5000");
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
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pcs = null;
			while((line = br.readLine()) != null) {
				pcs = workshop.parseLine(line, "\"", ";", "\\");
				if(pcs.length > 2) {
					charIDToECC.put(pcs[0], pcs[2]);
					eccToCharID.put(pcs[2], pcs[0]);
				}
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
		tools.collectCharacteristicsByEntity(product2GCharacteristics, articleCharacteristics);
	}
	
	private void cargaLasCosas() {
		long init = System.currentTimeMillis();
		log("Material refresh...");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Characteristic.Identifier"
				+ ",Characteristic.DataType"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(ECC)"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(ATG)"
				+ ",Characteristic.Lookup->Lookup.Identifier"
			);
		qp.put("query", "Characteristic.IsActive = true and not Characteristic.Entities is empty and Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "5000");
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
		collectLookupCharacteristics(dataTypes, lkps);
		for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
			tools.collectLookupValues(lkps.get( entry.getKey() ), map, mapB, entry.getValue());
		}
		articleHigherLevelProduct.clear();
		articleSupplierAIDToSKU.clear();
		skuToArticleSupplierAID.clear();
		variantesConImagen.clear();
		propuestasRevisadas.clear();
		qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImageDetail_URL',-1)"
			);
		qp.put("query", "Article.CurrentStatus = \"Creación de SKU\"");
		qp.put("pageSize", "10000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "stage", "cache", "SKUCreationWithImages").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			rw.collectData("list", "Article", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getJSONArray(1).getString(0)) || !"".equals(values.getJSONArray(2).getString(0)))
					variantesConImagen.add(values.getString(0));
					pw.println( values.getString(0) );
			});
		}catch(java.io.IOException e) {
			logE(e);
		}
		log("Refreshed took: " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
    private void launchListenerThread() {
		Thread t = new Thread(()->{
			while(running) {
				try(
					java.net.ServerSocket server = new java.net.ServerSocket(23546);
					java.net.Socket cli = server.accept();
					java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(cli.getInputStream()));
					java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(cli.getOutputStream()))
				){
					try{
						org.json.JSONObject req = new org.json.JSONObject(br.readLine());
						String action = req.getString("action");
						if("finish".equals(action.toLowerCase())) {
							this.running = false;
						}
					}catch(org.json.JSONException e) {
						logE(e);
					}
				}catch(java.io.IOException e) {
					logE(e);
				}
				try {
					Thread.sleep(100);
				}catch(InterruptedException e) {
					logE(e);
				}
			}
			log("Finishing...");
		});
		t.start();
	}

    public static void main(String[] args) {
    	if(args.length > 1 && "demo".equals(args[0])) {
    		ParseECC122Response parse = new ParseECC122Response();
        	parse.cargaLasCosas();
        	attp.cargaLasCosas();
    		try {
				parse.processFile(java.nio.file.Paths.get(args[1]), null, null);
			} catch (ServiceUnavailableException | ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
    		parse.sendData();
    	}else {
	    	ParseECC122Response object = new ParseECC122Response();
	    	Runtime.getRuntime().addShutdownHook(object);
	    	object.launchListenerThread();
	    	try {
				object.runOnSftp();
			} catch (ParserConfigurationException | SAXException e) {
				e.printStackTrace();
			}
    	}
    }
    

    static long eventTimeMillis(DirEntry e) {
        String name = e.getFilename();
        long t = extractTsFromNameMillis(name);
        if (t >= 0) return t;
        try {
            var attrs = e.getAttributes();
            if (attrs != null && attrs.getModifyTime() != null) {
                return attrs.getModifyTime().toMillis();
            }
        } catch (Exception ignore) {}
        return Long.MAX_VALUE;
    }

    static long extractTsFromNameMillis(String name) {
        var m = java.util.regex.Pattern
            .compile("(\\d{8}_\\d{6})|(\\d{14})")
            .matcher(name);

        if (!m.find()) return -1L;

        String ts14 = (m.group(1) != null) ? m.group(1).replace("_", "") : m.group(2); // yyyyMMddHHmmss

        var ldt = java.time.LocalDateTime.parse(
            ts14,
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );

        return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    public void prepareLocalReplay() {
        cargaLasCosas();
        attp.cargaLasCosas();
    }

    public void flushPendingWrites() {
        attp.sendData();
        sendData();
    }
    
	public void runOnSftp() throws ParserConfigurationException, SAXException {
		qp.put("includeObjectsInProtocol", "false");
		workshop.setBaseUrl(BASE_URL);
    	USE_CACHE = true;
        java.util.Properties processedState = new java.util.Properties();
        if (USE_CACHE && java.nio.file.Files.exists(STATE_FILE)) {
            try (InputStream in = java.nio.file.Files.newInputStream(STATE_FILE)) {
                processedState.load(in);
            } catch (IOException e) {
				logE(e);
			}
        }

		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        
		log("Starting...");
		boolean laPrimeraBez = true;
		 try (SshClient client = SshClient.setUpDefaultClient()) {
	            client.setKeyIdentityProvider(new FileKeyPairProvider(PRIVATE_KEY_PATH));
	            client.start();
	            while (running) {
	                try (ClientSession session = client.connect(USER, HOST, PORT)
	                        .verify(15, TimeUnit.SECONDS)
	                        .getSession()) {

	                    session.auth().verify(15, TimeUnit.SECONDS);

	                    try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {

	                        new Thread(()->{
	                        	while (running) {
	                        	    try {
	                        	        sftp.stat(".");
	                        	    } catch (Exception e) {
	                        	        log("Keepalive failed: " + e.getMessage());
	                        	        break;
	                        	    }
	                        	    try {
										Thread.sleep(30_000);
									} catch (InterruptedException e) {
										logE(e);
									}
	                        	}
	                        }).start();
	                        
	                        try {
	                            Iterable<DirEntry> entriesIt = sftp.readDir(REMOTE_DIR);
	                            
	                            java.util.List<DirEntry> entries = new java.util.ArrayList<>();
	                            for (DirEntry e : entriesIt) {
	                                String name = e.getFilename();
	                                if (name.equals(".") || name.equals("..")) continue;
	                                if (!name.startsWith("GenericXMLattributes") && !name.startsWith("GenericXMLproducts")) continue;
	                                entries.add(e);
	                            }
	                            java.util.Map<Long, java.util.List<DirEntry>> byTs = new java.util.TreeMap<>();
	                            for (DirEntry e : entriesIt) {
	                                long ts = extractTsFromNameMillis(e.getFilename());
	                                if (ts < 0) ts = eventTimeMillis(e);
	                                byTs.computeIfAbsent(ts, k -> new java.util.ArrayList<>()).add(e);
	                            }

	                            for (var group : byTs.values()) {

	                                for (DirEntry ent : group) {
	                             	   String name = ent.getFilename();
	                            	   String filePath = REMOTE_DIR + "/" + name;
	                                    if (name.startsWith("GenericXMLproducts")) {
			                                try (InputStream input = sftp.read(filePath);
			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		
			                                    copyStream(input, out);
		
			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
				
					                            try {
							                        log("(Products) Processing: " + name);
							                        if(laPrimeraBez) {
							                        	cargaLasCosas();
							                        	attp.cargaLasCosas();
							                        	attp.setMetadataCache(
							                                    eccToCharID,
							                                    charIDToECC,
							                                    product2GCharacteristics,
							                                    articleCharacteristics
							                            );
							                        	laPrimeraBez = false;
							                        }
					                            	processFile(java.nio.file.Paths.get(filePath), out, sftp);
					                    		} catch (ParserConfigurationException | SAXException | IOException e) {
					                    			logE(e);
					                    		}
					                            sftp.remove(filePath);
		
					                            if (USE_CACHE) {
					                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
					                                    processedState.store(stateOut, null);
					                                }
					                            }
					                            if(!running)
					                            	break;
			                                } catch (Exception perFileError) {
			                                    logE(perFileError);
			                                    String msg = String.valueOf(perFileError.getMessage());
			                                    if (msg.contains("client is closed") || msg.contains("Channel is closed")) {
			                                        throw perFileError;
			                                    }
			                                }
	                                    	
	                                    }
	                                }
	                                if(!running)
	                                	break;
	                                for (DirEntry ent : group) {
                                    	String name = ent.getFilename();
 	                            	    String filePath = REMOTE_DIR + "/" + name;
	                                    if (name.startsWith("GenericXMLattributes")) {
	                                    	if(laPrimeraBez) {
					                        	cargaLasCosas();
					                        	attp.cargaLasCosas();
					                        	attp.setMetadataCache(
					                                    eccToCharID,
					                                    charIDToECC,
					                                    product2GCharacteristics,
					                                    articleCharacteristics
					                            );
					                        	laPrimeraBez = false;
					                        }
 			                                try (InputStream input = sftp.read(filePath);
 			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
 		
 			                                    copyStream(input, out);
 		
 			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
 			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
 			                                    ECC122AttributesHandler handler = new ECC122AttributesHandler();
 			                                    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(out.toByteArray())) {
 			                                        parser.parse(bais, handler);
 							                        log("(Attributes) Processing: " + name);
 					                            	attp.processFile(handler.getCollected());
 					                    		} catch (ParserConfigurationException | SAXException | IOException e) {
 					                    			e.printStackTrace();
 					                    		}
 					                            sftp.remove(filePath);
 		
 					                            if (USE_CACHE) {
 					                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
 					                                    processedState.store(stateOut, null);
 					                                }
 					                            }
 					                            if(!running)
 					                            	break;
 			                                } catch (Exception perFileError) {
 			                                    logE(perFileError);
 			                                    String msg = String.valueOf(perFileError.getMessage());
 			                                    if (msg.contains("client is closed") || msg.contains("Channel is closed")) {
 			                                        throw perFileError;
 			                                    }
 			                                }
	                                    }
	                                }
	                            }
		                        laPrimeraBez = true;
		                        if(!running)
                                	break;
	                        } catch (Exception sftpBroken) {
	                            log("SFTP/session se rompió; reconecto en el siguiente ciclo.");
	                            logE(sftpBroken);
	                        }
	                        attp.sendData();
                            sendData();
	                    }

	                } catch (Exception connectOrAuthError) {
	                    log("No se pudo conectar/auth; reintento en el siguiente ciclo.");
	                    logE(connectOrAuthError);
	                }

	                try {
	                    Thread.sleep(10_000);
	                } catch (InterruptedException ie) {
	                    Thread.currentThread().interrupt();
	                    running = false;
	                }
	            }
	        } catch (java.io.IOException e) {
				logE(e);
			}
		 closeResources();
	}

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

	public void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos, SftpClient sftp) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {
		long init = System.currentTimeMillis();
		org.json.JSONArray product2GCharacteristicRecords = new org.json.JSONArray();
		org.json.JSONArray articleCharacteristicRecords = new org.json.JSONArray();
		java.util.Map<String, String> attributeValues = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> newAttributeValues = new java.util.TreeMap<>();
		java.util.Map<String, String> unidades = new java.util.TreeMap<>();
		java.util.Map<String, String> articleHigherLevelProductNotReadyYet = new java.util.TreeMap<>();
		String znprst = null;
		String negocio = null;
		String matkl;
		String sku = null;
		String ean = null;
		String name = null;
		String direccion = null;
		String seccion = null;
		String brandName = null;
		String supplier = null;
		String modelo = null;
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
		java.util.Map<String, java.util.List< java.util.Map<String, String> >> misniños = new java.util.HashMap<>();
		java.util.List<java.util.Map<String, String>> niños = null;
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
        if(baos != null) {
        	java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream( baos.toByteArray() );
        	parser.parse(bais, erh);
        	bais.close();
        	baos.close();
        }else {
        	parser.parse(new java.io.File(path.toString()), erh);
        }
        products = erh.getCollected();
        log("FOUND: " + products.size() + " products");
		java.util.List<String> nuevosValores = new java.util.ArrayList<>();
		
		for(Product122 p : products) {
			znprst = p.getProposalId();
			log("P: " + p.getProposalId());
			for(Value vn : p.getValues()) {
				if(!"".equals(vn.getText()) && vn.getText() != null){
					if(!excluyeAtributo(vn)) {
						attributeValues.put(vn.getAttributeId(), vn.getText() == null ? "" : vn.getText());
						if(vn.getText() != null && vn.getText().matches("^(unece\\.unit\\.)[A-Z0-9]+$")) {
							unidades.put(vn.getAttributeId(), vn.getText());
						}else {
						}
					}
				}
				if("MATNR".equals(vn.getAttributeId()) /* || "EXTWG".equals(vn.getAttributeId()) || "LIFNR".equals(vn.getAttributeId()) */) {
					log("MATNR ----------------->" + vn.getText());
				}
			}
			log("Opening");
			log("Mappings: " + charIDToECC.size());
			log("Negocio: " + charIDToECC.get("Negocio"));
			negocio = attributeValues.get( charIDToECC.get("Negocio") );
			sku = attributeValues.get( "MATNR" );
			matkl = attributeValues.get( "MATKL" );
			pe000 = attributeValues.get("PE000");
			sapBehvo = attributeValues.get("SAP_BEHVO");
			fshId = attributeValues.get("FSH_ID");
			
			itemId = null;
			satnr = attributeValues.get( "SATNR" );
			attyp = attributeValues.get( "ATTYP" );
			znprst = znprst == null ? attributeValues.get( "ZNPRST" ) : znprst;
			ean = attributeValues.get( "EAN11_EAN" );
			name = attributeValues.get( "MAKTX" );
			direccion = attributeValues.get( "ZDIR" );
			seccion = attributeValues.get( "ZSEC" );
			brandName = attributeValues.get( "BRAND_NAME" );
			supplier = attributeValues.get( "LIFNR" );
			modelo = attributeValues.get( "IDNLF" );
			if(znprst != null && sku != null) {
				articleSupplierAIDToSKU.put(sku, znprst);
				skuToArticleSupplierAID.put(znprst, sku);
			}
			String externalId = null;
			String entity = null;
			DataRequestor dr = new DataRequestor();
			
			if(znprst != null && !"".equals(znprst)) {
				externalId = znprst;
				log("Processing este: " + znprst);
				info = tools.checkProduct(znprst);
				if(info != null && ("MKP".equals(info[1]) || "1004".equals(info[3]))) {
					log("Undoing...");
					info = null;
				}
				if( info == null ) {
					info = tools.checkArticle(znprst);
					if( info != null ) {
						resuelveEmpateDeArticulo(sku, znprst);
						if(!"".equals(satnr)) {
							conciliaRelacionArticuloProducto(
							        znprst,
							        sku,
							        info[0],
							        satnr,
							        articleHigherLevelProductNotReadyYet,
							        dr
							);
						}else { log("Avoiding conciliación para " + sku + "(" + znprst + ", " + java.util.Arrays.asList(info) + ")"); }
						
						sendArticleSKUToAdmin(znprst, sku, dr);
						log("PeléPe. SAPObjectType: " + info[1] + ", Business: " + info[2]);
						if("00".equals(info[1]) && !"MKP".equals(info[2])) {
							entity = "Article";
							addValue("MensajeCreacionSKU", "Article", znprst, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
							sendWriteRequest("Product2G", info[0], product2GCharacteristicRecords, info[3], info[4]);
							newAttributeValues.put(info[0], attributeValues);
						}else if("00".equals(info[1]) && "MKP".equals(info[2])) {
							entity = "Article";
							log("MariMba... " + java.util.Arrays.asList(info));
							checkParentVariantsCompleteness(info[0], znprst, product2GCharacteristicRecords, "", info[4]);
							newAttributeValues.put(info[0], attributeValues);
						}else {
							entity = "Article";
						}
						addValue("MensajeCreacionSKU", "Article", znprst, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
						log("Was Article (" + znprst + "), " + ( info != null ? java.util.Arrays.asList(info) : "NaA") );
					}else {
						log("Not a known product (" + znprst + ") <:>" + negocio + "<:>");
						if("00".equals(attyp)) {
							String originalznprst = znprst;
							String productZNPRST = null;
							znprst = chooseProperProductZNPRST(sku, znprst);
							productZNPRST = znprst;
							entity = "Individual";
							addValue("SAPObjectType", "Product2G", znprst, "00" );
							addValue("Business", "Product2G", znprst, "LVP" );
							sendWriteRequestProduct(znprst, matkl, pe000, negocio, product2GCharacteristicRecords);
							
							znprst = chooseProperArticleZNPRST(sku, originalznprst);
							addValue("SAPObjectType", "Article", znprst, "00" );
							sendWriteRequest("Article",  znprst, articleCharacteristicRecords, null, null);
							nuevosValores.add(znprst);
							newAttributeValues.put(znprst, attributeValues);
							articleHigherLevelProduct.put(znprst, productZNPRST);
							org.json.JSONArray items = new org.json.JSONArray();
							if(!"".equals(sku)) {
								items.put(new org.json.JSONObject().put("productNo", productZNPRST).put("sku", sku));
								log("From registering to admin 00: " + dr.skuProductNo( items ) );
								items.remove(0);
								items.put(new org.json.JSONObject().put("productNo", productZNPRST).put("sku", sku).put("supplierAID", znprst));
								log("From registering to admin 00: " + dr.putSkuSupplierAID( items ) );
							}
							
						}else if("01".equals(attyp)) {
							entity = "Product2G";
							znprst = chooseProperProductZNPRST(sku, znprst);
							sendProductSKUToAdmin(znprst, sku, dr);
							addValue("SAPObjectType", "Product2G", znprst, "01");
							addValue("Business", "Product2G", znprst, "LVP" );
							sendWriteRequestProduct(znprst, matkl, pe000, negocio, product2GCharacteristicRecords);
							nuevosValores.add(znprst);
							newAttributeValues.put(znprst, attributeValues);
							org.json.JSONArray items = new org.json.JSONArray();
							if(!"".equals(sku)) {
								items.put(new org.json.JSONObject().put("productNo", znprst).put("sku", sku));
								log("From registering to admin 01: " + dr.skuProductNo( items ) );
							}
						}else if("02".equals(attyp)) {
							znprst = chooseProperArticleZNPRST(sku, znprst);
							sendArticleSKUToAdmin(znprst, sku, dr);
							addValue("SAPObjectType", "Article", znprst, "02" );
							sendWriteRequest("Article", znprst, articleCharacteristicRecords, null, null);
							/** Easy with that satnr, need to get the real Id, maybe by iterating over all components or by querying from system. **/
							conciliaRelacionArticuloProducto(
							        znprst,
							        sku,
							        null,
							        satnr,
							        articleHigherLevelProductNotReadyYet,
							        dr
							);
						}
					}
				} else {
					if("02".equals(attyp)) {
						conciliaRelacionArticuloProducto(
						        externalId,
						        sku,
						        null,
						        satnr,
						        articleHigherLevelProductNotReadyYet,
						        dr
						);
						entity = "Article";
						sendArticleSKUToAdmin(externalId, sku, dr);
						addValue("MensajeCreacionSKU", "Article", itemId, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
						sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
					}else {
						ProductMergeDecision mergeDecision = resuelveEmpateDeProducto(sku, znprst);
						if(mergeDecision.manualReview) {
						    log("No se continúa escritura automática por empate inseguro. sku=" + sku + ", reason=" + mergeDecision.reason);
						    entity = null;
						} else {
							
							if(mergeDecision.shouldMerge) {
//						        resuelveCombinación(mergeDecision.id1, mergeDecision.id2);
						    }
							
							znprst = mergeDecision.productIdToUse;
							externalId = znprst;
	
						    info = tools.checkProduct(znprst);
						    
						    log("Was Product (" + znprst + "), " + java.util.Arrays.asList(info));
						    entity = "Product2G";
	
						    addValue("MensajeCreacionSKU", "Product2G", znprst, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
						    sendProductSKUToAdmin(znprst, sku, dr);
	
						    if(info != null && "00".equals(info[0])) {
						        itemId = getArticleIdFromProduct(znprst);
						        log("Article.SupplierAID: " + itemId);
						        addValue("MensajeCreacionSKU", "Article", itemId, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
						        sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
						    }
	
						    if(info != null && "MKP".equals(info[1])) {
						        addValue("SKU", "Product2G", znprst, "999" + znprst.substring(znprst.startsWith("S") ? 1 : 7));
						    }
	
						    newAttributeValues.put(znprst, attributeValues);
	
						    if(info != null) {
						        sendWriteRequest("Product2G", znprst, product2GCharacteristicRecords, info[2], info[3]);
						    }
						}
					}
				}
			}else {
				externalId = "LVP" + sku;
				log("No znprst found");
				if(sku != null && !"".equals(sku)) {
					info = tools.checkProductBySKU(sku);
					if(info != null) {
						// Pero hay más de uno al consultar P360, entonces hay que decidir.
						if("02".equals(attyp)) {
							conciliaRelacionArticuloProducto(
							        externalId,
							        sku,
							        null,
							        satnr,
							        articleHigherLevelProductNotReadyYet,
							        dr
							);
							entity = "Article";
							sendArticleSKUToAdmin(externalId, sku, dr);
							addValue("MensajeCreacionSKU", "Article", itemId, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
							sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
						}else {
							externalId = info[0];
							entity = "Product2G";
							log("-->" + java.util.Arrays.asList(info));
							addValue("MensajeCreacionSKU", "Product2G", info[0], "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
							sendWriteRequest("Product2G", info[0], product2GCharacteristicRecords, info[3], info[4]);
							if("00".equals(info[1])) {
								addValue("MensajeCreacionSKU", "Article", info[0], "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
								sendWriteRequest("Article", info[0], articleCharacteristicRecords, null, null);
								newAttributeValues.put(info[0], attributeValues);
								itemId = tools.checkArticleBySKU(sku);
								if(itemId != null && !"".equals(itemId) && !"null".equals(itemId))
									articleHigherLevelProduct.put(itemId, externalId);
							}
							sendProductSKUToAdmin(externalId, sku, dr);
							newAttributeValues.put(info[0], attributeValues);
						}
					} else {
						itemId = tools.checkArticleBySKU(sku);
						if(itemId != null) {
							// Pero hay más de uno al consultar directamente P360, entonces hay que decidir...
							externalId = itemId;
							entity = "Article";
							if("00".equals(attyp)) {
								entity = "Individual";
								String pid = externalId;
								String rr = dr.getProductByVariant(new org.json.JSONArray().put(itemId));
								if(rr != null) {
									try {
										org.json.JSONObject j = new org.json.JSONObject(rr);
										org.json.JSONArray itms = j.getJSONArray("items");
										if(!"".equals(itms.getString(0))) {
											pid = itms.getString(0);
											rr = dr.getProductData(new org.json.JSONArray().put(pid));
											if(rr != null) {
												j = new org.json.JSONObject(rr);
												itms = j.getJSONArray("items");
												org.json.JSONObject itm = itms.getJSONObject(0);
												log("Individual a partir de SKU plano. Encontramos sku en artículo. " + itm);
												addValue("MensajeCreacionSKU", "Product2G", pid, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
												sendWriteRequest("Product2G", pid, product2GCharacteristicRecords, itm.getString("FotoTomadaLiverpool"), itm.getString("CurrentStatus"));
											}
										}
									}catch(org.json.JSONException e) {
										log("Couldn't parse json response from pvia.");
									}
								}else {
									sendWriteRequest("Product2G", pid, product2GCharacteristicRecords, "Y", "1020");
								}
								
								sendProductSKUToAdmin(externalId, sku, dr);
								sendArticleSKUToAdmin(externalId, sku, dr);
								
								sendWriteRequestProduct(externalId, matkl, pe000, negocio, product2GCharacteristicRecords);
								articleHigherLevelProduct.put(externalId, pid);
								org.json.JSONArray items = new org.json.JSONArray();
								if(!"".equals(sku)) {
									items.put(new org.json.JSONObject().put("productNo", pid).put("sku", sku));
									log("From registering to admin 00: " + dr.skuProductNo( items ) );
								}
								niños = misniños.get(externalId);
								if(niños == null) {
									niños = new java.util.ArrayList<>();
									misniños.put(externalId, niños);
								}
								niños.add(attributeValues);
							}else if("02".equals(attyp)) {
								conciliaRelacionArticuloProducto(
								        externalId,
								        sku,
								        null,
								        satnr,
								        articleHigherLevelProductNotReadyYet,
								        dr
								);
							}
							sendArticleSKUToAdmin(externalId, sku, dr);
							addValue("MensajeCreacionSKU", "Article", itemId, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()) );
							sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
						} else {
							log("Brand new SKU for P360: " + sku + " (" + znprst + ")");
							log("Not a known product (" + znprst + ") <:>" + negocio + "<:>");
							if("00".equals(attyp)) {
								sendProductSKUToAdmin(externalId, sku, dr);
								sendArticleSKUToAdmin(externalId, sku, dr);
								entity = "Individual";
								addValue("SAPObjectType", "Product2G", externalId, "00" );
								addValue("Business", "Product2G", externalId, "LVP" );
								addValue("SAPObjectType", "Article", externalId, "00" );
								sendWriteRequest("Article",   externalId, articleCharacteristicRecords, null, null);
								sendWriteRequestProduct(externalId, matkl, pe000, negocio, product2GCharacteristicRecords);
								nuevosValores.add(externalId);
								newAttributeValues.put(externalId, attributeValues);
								articleHigherLevelProduct.put(externalId, externalId);
								articleHigherLevelProductNotReadyYet.put(externalId, externalId);
								org.json.JSONArray items = new org.json.JSONArray();
								if(!"".equals(sku)) {
									items.put(new org.json.JSONObject().put("productNo", externalId).put("sku", sku));
									log("From registering to admin 00: " + dr.skuProductNo( items ) );
								}
								niños = misniños.get(externalId);
								if(niños == null) {
									niños = new java.util.ArrayList<>();
									misniños.put(externalId, niños);
								}
								niños.add(attributeValues);
							}else if("01".equals(attyp)) {
								sendProductSKUToAdmin(externalId, sku, dr);
								entity = "Product2G";
								addValue("SAPObjectType", "Product2G", externalId, "01" );
								addValue("Business", "Product2G", externalId, "LVP" );
								sendWriteRequestProduct(externalId, matkl, pe000, negocio, product2GCharacteristicRecords);
								nuevosValores.add(externalId);
								newAttributeValues.put(externalId, attributeValues);
								org.json.JSONArray items = new org.json.JSONArray();
								if(!"".equals(sku)) {
									items.put(new org.json.JSONObject().put("productNo", externalId).put("sku", sku));
									log("From registering to admin: " + dr.skuProductNo( items ));
								}
							}else if("02".equals(attyp)){
								entity = "Article";
								sendArticleSKUToAdmin(externalId, sku, dr);
								addValue("SAPObjectType", "Article", externalId, "02" );
								sendWriteRequest("Article", externalId, articleCharacteristicRecords, null, null);
								znprst = chooseProperArticleZNPRST(sku, "LVP" + sku);
								conciliaRelacionArticuloProducto(
										znprst,
								        sku,
								        null,
								        satnr,
								        articleHigherLevelProductNotReadyYet,
								        dr
								);
							}
						}
					}
				} else {
					log("No SKU found either!");
				}
			}
			if(externalId != null && !unidades.isEmpty() && "Product2G".equals(entity))
				agregaUnidadesDeMedida(unidades, charIDToECC, externalId);
			if(externalId != null && ("Product2G".equals(entity) || "Individual".equals(entity)) ) {
				String productId = externalId;
				try {
					if("Individual".equals(entity)) {
						String ya = dr.getArticleData(new org.json.JSONArray().put(externalId));
						if(ya != null) {
							try {
								org.json.JSONObject jya = new org.json.JSONObject(ya);
								org.json.JSONArray itms = jya.getJSONArray("items");
								org.json.JSONObject itm = itms.getJSONObject(0);
								if(itm.has("ProductNo")  &&  !"".equals(itm.getString("ProductNo"))) {
									productId = itm.getString("ProductNo");
								}
							}catch(org.json.JSONException e) {
								logE(e);
							}
						}
					}
					calculaProductType(sapBehvo, matkl, fshId, negocio, ae253, mtart, mtart, productId, workshop);
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					e.printStackTrace();
				}
			}
			if("Product2G".equals(entity)) {
				try {
					brandName = brandName != null ? mapB.get("ZCOMALOV").get(brandName) : "";
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray()
							.put(nvl( ean ))
							.put(sku)
							.put(determineBusiness(nvl( negocio )))
							.put(nvl( name ))
							.put(nvl( direccion ))
							.put(nvl( seccion) )
							.put(nvl( matkl ))
							.put(brandName)
							.put(nvl( negocio ))
							.put(nvl( attyp) )
							.put(nvl( supplier ))
							.put(nvl( modelo ))));
					if(rows.length() == 1000) {
						rw.writeData("list", "Product2G", null, qp, request, this::log);
					}
				}catch(NullPointerException e) {
					logE(e);
					log("Was trying with: BrandName -->" + brandName + "<-- LKP content -->" + (null == mapB.get("ZCOMALOV") ) + "<--" );
				}
			}else if("Article".equals(entity)) {
				rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray()
						.put(nvl( ean ))
						.put(sku)
						.put(determineBusiness(nvl( negocio )))
						.put(nvl( name ))
						.put(nvl( modelo ))
						.put(nvl( attyp) )
					));
				if(rowsArticle.length() == 1000) {
					rw.writeData("list", "Article", null, qp, requestArticle, this::log);
				}
			}else if("Individual".equals(entity)) {
				String productId = externalId;
				try {
					String ya = dr.getArticleData(new org.json.JSONArray().put(externalId));
					if(ya != null) {
						try {
							org.json.JSONObject jya = new org.json.JSONObject(ya);
							org.json.JSONArray itms = jya.getJSONArray("items");
							org.json.JSONObject itm = itms.getJSONObject(0);
							if(itm.has("ProductNo")  &&  !"".equals(itm.getString("ProductNo"))) {
								productId = itm.getString("ProductNo");
							}
						}catch(org.json.JSONException e) {
							logE(e);
						}
					}
					brandName = brandName != null ? mapB.get("ZCOMALOV").get(brandName) : "";
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray()
							.put(sku)
							.put(determineBusiness(nvl( negocio )))
							.put(nvl( name ))
							.put(nvl( direccion ))
							.put(nvl( seccion) )
							.put(nvl( matkl ))
							.put(brandName)
							.put(nvl( negocio ))
							.put(nvl( attyp) )
							.put(nvl( supplier ))
							.put(nvl( modelo ))));
					if(rows.length() == 1000) {
						rw.writeData("list", "Product2G", null, qp, request, this::log);
					}
				}catch(NullPointerException e) {
					logE(e);
					log("Was trying with: BrandName -->" + brandName + "<-- LKP content -->" + (null == mapB.get("ZCOMALOV") ) + "<--" );
				}
				rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray()
						.put(nvl( ean ))
						.put(sku)
						.put(determineBusiness(nvl( negocio )))
						.put(nvl( name ))
						.put(nvl( modelo ))
						.put(nvl( attyp) )
					));
				if(rowsArticle.length() == 1000) {
					rw.writeData("list", "Article", null, qp, requestArticle, this::log);
				}
			}
			String charId = null;
			for(java.util.Map.Entry<String, String> entry : attributeValues.entrySet()) {
				charId = eccToCharID.get(entry.getKey());
				if(charId != null) {
					try{
						if("Product2G".equals(entity)) {
							if(!unidades.containsKey(entry.getKey()) && product2GCharacteristics.contains(charId) ) {
								addValue(charId, "Product2G", externalId, entry.getValue() );
							}
						}else if("Article".equals(entity)) {
							if(!unidades.containsKey(entry.getKey()) && articleCharacteristics.contains(charId) ) {
								addValue(charId, "Article", externalId, entry.getValue() );
							}
						}
					}catch(IllegalArgumentException e) {
						logE(e);
					}
				}
			}
			
			negocio = null;
			sku = null;
			satnr = null;
			znprst = null;
			attyp = null;
			itemId = null;
			info = null;
			matkl = null;
			pe000 = null;
			sapBehvo = null;
			fshId = null;
			ean = null;
			name = null;
			direccion = null;
			seccion = null;
			brandName = null;
			supplier = null;
			modelo = null;
			product2GCharacteristicRecords = new org.json.JSONArray();
			articleCharacteristicRecords = new org.json.JSONArray();
			unidades.clear();
			attributeValues = new java.util.TreeMap<>();
			cnt++;
			log(cnt + "/" + products.size());
			if(!running) {
				return;
			}
		}
		log("Writing relationships");
		org.json.JSONArray items = new org.json.JSONArray();
		org.json.JSONObject item = null;
		String parentId = null;
		DataRequestor dr = new DataRequestor();
		for( java.util.Map.Entry<String, String> entry : articleHigherLevelProductNotReadyYet.entrySet() ) {
			parentId = skuToArticleSupplierAID.get(entry.getValue());
			if(parentId == null) {
				log("En el archivo no estaba el padre referenciado por el valor de SKU: " + entry.getValue() + " para la variante con id de sistema: " + entry.getKey());
				String response = dr.productBySKU( new org.json.JSONArray().put(entry.getValue()) );
				try {
					org.json.JSONObject jr = new org.json.JSONObject(response);
					org.json.JSONArray ir = jr.getJSONArray("items");
					parentId = ir.getString(0);
					log(
						  "Recuperamos el padre gracias al admin: " 
					    + parentId 
					    + " para SKU: " 
					    + entry.getValue() 
					    + ", de la propuesta variante: " 
					    + entry.getKey()
					);
				}catch(org.json.JSONException e) {
					logE(e);
				}
			}
			if(parentId != null) {
				item = new org.json.JSONObject();
				item.put("supplierAID", entry.getKey());
				item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
				item.put("productNo", parentId);
				items.put(item);
			}
		}
		dr.putSkuSupplierAID(items);
		arremangalos(newAttributeValues, misniños);
		log("Done processing file. [" + path.toString().replaceAll(".+" + java.util.regex.Pattern.quote( java.io.File.separator ), "") + "] " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	private String conciliaRelacionArticuloProducto(
	        String articleId,
	        String sku,
	        String currentParentId,
	        String satnr,
	        java.util.Map<String, String> articleHigherLevelProductNotReadyYet,
	        DataRequestor dr
	) {
	    if(articleId == null || "".equals(articleId) || "null".equals(articleId)) {
	        return null;
	    }

	    if(satnr == null || "".equals(satnr)) {
	        log("No se puede conciliar padre de variante porque SATNR viene vacío. articleId=" + articleId + ", sku=" + sku);
	        return null;
	    }

	    String targetParentId = null;
	    log("Entramos a ver por " + articleId + ", con su satnr: " + satnr);
	    targetParentId = resolveProductIdBySku(satnr, dr);;
	    log("Resolvió a: " + targetParentId);

	    if(targetParentId == null || "".equals(targetParentId)) {
	    	log("No pude con el targetParentId, preguntando por SKU");
	        String response = dr.productBySKU(new org.json.JSONArray().put(satnr));
	        log("Resultado: " + response);
	        if(response != null) {
	            try {
	                org.json.JSONObject jr = new org.json.JSONObject(response);
	                org.json.JSONArray items = jr.getJSONArray("items");
	                if(items.length() > 0 && !"".equals(items.getString(0))) {
	                    targetParentId = items.getString(0);
	                }
	            } catch(org.json.JSONException e) {
	                logE(e);
	            }
	        }
	    }

	    if(targetParentId == null || "".equals(targetParentId)) {
//	        String[] p360Parent = tools.checkProductBySKUOnP360(satnr);
//	        log("Preguntando a P360...");
//	        if(p360Parent != null) {
//	            targetParentId = p360Parent[0];
//	        }
	        log("Obtuvimos: " + targetParentId);
	    }

	    if(targetParentId == null || "".equals(targetParentId)) {
	        articleHigherLevelProductNotReadyYet.put(articleId, satnr);
	        log("Padre pendiente. articleId=" + articleId + ", sku=" + sku + ", satnr=" + satnr);
	        return null;
	    }

	    if(currentParentId == null || "".equals(currentParentId) || "null".equals(currentParentId)) {
	        String response = dr.getProductByVariant(new org.json.JSONArray().put(articleId));
	        if(response != null) {
	            try {
	                org.json.JSONObject jr = new org.json.JSONObject(response);
	                org.json.JSONArray items = jr.getJSONArray("items");
	                if(items.length() > 0 && !"".equals(items.getString(0))) {
	                    currentParentId = items.getString(0);
	                }
	            } catch(org.json.JSONException e) {
	                logE(e);
	            }
	        }
	    }

	    if(currentParentId != null && !"".equals(currentParentId) && !"null".equals(currentParentId) && !targetParentId.equals(currentParentId)) {
	        java.util.Map<String, String> qp = new java.util.HashMap<>();
	        log("Now deleting current article relationship to current parent (" + articleId + " || " + currentParentId + " || " + targetParentId + ")");
	        qp.put("items", "'" + articleId + "'@1");
	        rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
	        log("Se quitó relación anterior de Article. articleId=" + articleId + ", oldParent=" + currentParentId + ", newParent=" + targetParentId);
	    }

	    articleHigherLevelProduct.put(articleId, targetParentId);

	    org.json.JSONArray items = new org.json.JSONArray();
	    items.put(new org.json.JSONObject()
	            .put("supplierAID", articleId)
	            .put("sku", sku)
	            .put("productNo", targetParentId));

	    log("From conciliación: " + dr.putSkuSupplierAID(items));

	    return targetParentId;
	}
	
	private String resolveProductIdBySku(String sku, DataRequestor dr) {
	    if(sku == null || "".equals(sku)) {
	        return null;
	    }

	    String response = dr.productBySKU(new org.json.JSONArray().put(sku));

	    if(response != null) {
	        try {
	            org.json.JSONObject jr = new org.json.JSONObject(response);
	            org.json.JSONArray items = jr.getJSONArray("items");

	            if(items.length() > 0 && !"".equals(items.getString(0))) {
	                return items.getString(0);
	            }
	        } catch(org.json.JSONException e) {
	            logE(e);
	        }
	    }

//	    String[] p360Parent = tools.checkProductBySKUOnP360(sku);
//
//	    if(p360Parent != null) {
//	        return p360Parent[0];
//	    }

	    return null;
	}
	
	private void resuelveCombinación(String id1, String id2) {
		if(id1 != null && id2 != null && !id1.equals(id2)) {
			org.json.JSONObject response1 = rw.getRw().makeRequest("GET", "/object/Product2G/'" + rw.getRw().encode(id1) + "'@1?includeIds=true&includeLabels=true");
			org.json.JSONObject response2 = rw.getRw().makeRequest("GET", "/object/Product2G/'" + rw.getRw().encode(id2) + "'@1?includeIds=true&includeLabels=true");
			
			org.json.JSONObject data1 = response1 != null && response1.has("_data") ? response1.getJSONObject("_data") : null;
			org.json.JSONObject data2 = response2 != null && response2.has("_data") ? response2.getJSONObject("_data") : null;
			
			if(data1 != null && data2 != null) {
				
				/**********************************************************
				 * 
				 * Hacer la lógica de comparación y merge y todo en data1
				 * 
				 ***************************************************************/
				
				java.util.List<String> itemsOfProduct1 = collectArticleObjectIdsByProduct(id1);
			    java.util.List<String> itemsOfProduct2 = collectArticleObjectIdsByProduct(id2);

			    java.util.List<VariantInfo> variants1 = loadVariantInfos(itemsOfProduct1, "id1");
			    java.util.List<VariantInfo> variants2 = loadVariantInfos(itemsOfProduct2, "id2");

			    java.util.Map<String, java.util.List<VariantInfo>> signatureIndex = buildSignatureIndex(variants1, variants2);

			    VariantMergeDecision decision = decideVariantMovement(variants2, signatureIndex);

			    deleteProductReferencesFromArticles(itemsOfProduct2);

			    createProductReferencesToId1(toArticleObjectIds(decision.articlesToMoveToProduct1), id1);

			    clearSkuAndEanFromArticles(decision.duplicatedArticlesToDetachAndClean);

			    mergeMissingProductData(data1, data2);
			    clearProductSkuAndEan(data2);

			    java.util.Map<String, String> qp = new java.util.HashMap<>();

			    org.json.JSONObject write1 = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + rw.getRw().encode(id1) + "'@1", qp, data1.toString());

			    if(write1 == null) {
			        log("PANIC: from id1=" + id1 + ". rawResponse=" + rw.getRw().getRawResponse());
			        return;
			    }

			    qp.clear();

			    org.json.JSONObject write2 = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + rw.getRw().encode(id2) + "'@1", qp, data2.toString());

			    if(write2 == null) {
			        log("PANIC: from id2=" + id2 + ". rawResponse=" + rw.getRw().getRawResponse());
			        return;
			    }

			    log("Combinación finished. id1=" + id1
			            + ", id2=" + id2
			            + ", moveToId1=" + decision.articlesToMoveToProduct1.size()
			            + ", cleanDuplicated=" + decision.duplicatedArticlesToDetachAndClean.size());
				
			}
		}
	}
	
	private java.util.List<String> toArticleObjectIds(java.util.List<VariantInfo> variants) {
	    java.util.List<String> ids = new java.util.ArrayList<>();

	    for(VariantInfo variant : variants) {
	        ids.add(variant.articleObjectId);
	    }

	    return ids;
	}
	
	private void mergeMissingProductData(org.json.JSONObject data1, org.json.JSONObject data2) {
	    java.util.Set<String> excluded = new java.util.HashSet<>();

	    excluded.add("identifier");
	    excluded.add("sku");
	    excluded.add("gtin");
	    excluded.add("statusModification");
	    excluded.add("log");
	    excluded.add("ownLog");

	    mergeObjectMissing(data1, data2, excluded);
	}
	
	private void clearProductSkuAndEan(org.json.JSONObject data) {
	    data.put("sku", org.json.JSONObject.NULL);
	    data.put("gtin", org.json.JSONObject.NULL);
	    clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);
	}
	
	private java.util.List<VariantInfo> loadVariantInfos(java.util.List<String> articleObjectIds, String productOwner) {
	    java.util.List<VariantInfo> result = new java.util.ArrayList<>();

	    for(String articleObjectId : articleObjectIds) {
	        org.json.JSONObject response = rw.getRw().makeRequest(
	                "GET",
	                "/object/Article/" + articleObjectId + "?includeIds=true&includeLabels=true"
	        );

	        org.json.JSONObject data = response != null && response.has("_data")
	                ? response.getJSONObject("_data")
	                : null;

	        if(data == null) {
	            log("No se pudo leer Article. owner=" + productOwner + ", article=" + articleObjectId);
	            continue;
	        }

	        java.util.Set<String> signatures = buildVariantSignatures(data);

	        if(signatures.isEmpty()) {
	            log("Article sin firma útil. owner=" + productOwner + ", article=" + articleObjectId);
	        }

	        result.add(new VariantInfo(articleObjectId, productOwner, data, signatures));
	    }

	    return result;
	}
	
	private java.util.Set<String> buildVariantSignatures(org.json.JSONObject data) {
	    java.util.Set<String> signatures = new java.util.LinkedHashSet<>();

	    String sku = stringValue(data.opt("sku"));
	    String gtin = stringValue(data.opt("gtin"));

	    if(!isBlank(sku)) {
	        signatures.add("SKU|" + normalize(sku));
	    }

	    if(!isBlank(gtin)) {
	        signatures.add("EAN|" + normalize(gtin));
	    }

	    java.util.Map<String, String> characteristicValues = extractCharacteristicValues(data);

	    addSignatureIfPresent(signatures, "SKU", characteristicValues.get("SKU"));
	    addSignatureIfPresent(signatures, "MainBarCode", characteristicValues.get("MainBarCode"));
	    addSignatureIfPresent(signatures, "MainBarCodeS4H", characteristicValues.get("MainBarCodeS4H"));

	    String color = firstNotBlank(
	            extractMxExtraDataValue(data, "coloursLiverpoolAtt"),
	            characteristicValues.get("ColoursLiverpoolAtt")
	    );

	    String size = firstNotBlank(
	            extractMxExtraDataValue(data, "tamanoUnico"),
	            characteristicValues.get("TamanoUnico")
	    );

	    String supplierPartNumber = firstNotBlank(
	            extractMxExtraDataValue(data, "supplierPartNumber"),
	            characteristicValues.get("SupplierPartNumber")
	    );

	    if(!isBlank(color) && !isBlank(size) && !isBlank(supplierPartNumber)) {
	        signatures.add("COLOR_SIZE_MODEL|" + normalize(color) + "|" + normalize(size) + "|" + normalize(supplierPartNumber));
	    }

	    return signatures;
	}
	
	private java.util.Map<String, String> extractCharacteristicValues(org.json.JSONObject data) {
	    java.util.Map<String, String> valuesByCode = new java.util.HashMap<>();

	    org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

	    if(records == null) {
	        return valuesByCode;
	    }

	    for(int i = 0; i < records.length(); i++) {
	        org.json.JSONObject record = records.optJSONObject(i);

	        if(record == null) {
	            continue;
	        }

	        String code = nestedValue(record, "_qualification.characteristic._code");

	        if(isBlank(code)) {
	            continue;
	        }

	        if(!VARIANT_MATCH_CHARACTERISTICS.contains(code)) {
	            continue;
	        }

	        String value = firstRecordValue(record);

	        if(!isBlank(value)) {
	            valuesByCode.put(code, value);
	        }
	    }

	    return valuesByCode;
	}

	private String firstRecordValue(org.json.JSONObject record) {
	    org.json.JSONArray recordLang = record.optJSONArray("_recordLang");

	    if(recordLang == null) {
	        return "";
	    }

	    for(int i = 0; i < recordLang.length(); i++) {
	        org.json.JSONObject lang = recordLang.optJSONObject(i);

	        if(lang == null) {
	            continue;
	        }

	        org.json.JSONArray values = lang.optJSONArray("values");

	        if(values == null || values.length() == 0) {
	            continue;
	        }

	        Object value = values.opt(0);

	        if(value == null || value == org.json.JSONObject.NULL) {
	            continue;
	        }

	        if(value instanceof org.json.JSONObject) {
	            org.json.JSONObject valueObject = (org.json.JSONObject) value;

	            String code = valueObject.optString("_code", "");
	            if(!isBlank(code)) {
	                return code;
	            }

	            String label = valueObject.optString("_label", "");
	            if(!isBlank(label)) {
	                return label;
	            }

	            org.json.JSONObject key = valueObject.optJSONObject("_key");
	            if(key != null) {
	                String externalId = key.optString("_externalId", "");
	                if(!isBlank(externalId)) {
	                    return externalId;
	                }

	                String internalId = key.optString("_internalId", "");
	                if(!isBlank(internalId)) {
	                    return internalId;
	                }
	            }

	            return valueObject.toString();
	        }

	        return String.valueOf(value).trim();
	    }

	    return "";
	}

	private String extractMxExtraDataValue(org.json.JSONObject data, String fieldName) {
	    String value = extractMxExtraDataValueFromArray(data.optJSONArray("extraData"), fieldName);

	    if(!isBlank(value)) {
	        return value;
	    }

	    return extractMxExtraDataValueFromArray(data.optJSONArray("productExtraData"), fieldName);
	}

	private String extractMxExtraDataValueFromArray(org.json.JSONArray array, String fieldName) {
	    if(array == null) {
	        return "";
	    }

	    for(int i = 0; i < array.length(); i++) {
	        org.json.JSONObject item = array.optJSONObject(i);

	        if(item == null) {
	            continue;
	        }

	        String targetMarket = firstNotBlank(
	                nestedValue(item, "_qualification.targetMarket._code"),
	                nestedValue(item, "_qualification.targetMarket._key"),
	                nestedValue(item, "_qualification.targetMarket._label")
	        );

	        if(!"MX".equalsIgnoreCase(targetMarket) && !"Mexico".equalsIgnoreCase(targetMarket)) {
	            continue;
	        }

	        Object rawValue = item.opt(fieldName);

	        if(rawValue == null || rawValue == org.json.JSONObject.NULL) {
	            continue;
	        }

	        if(rawValue instanceof org.json.JSONObject) {
	            org.json.JSONObject object = (org.json.JSONObject) rawValue;

	            String code = object.optString("_code", "");
	            if(!isBlank(code)) {
	                return code;
	            }

	            String label = object.optString("_label", "");
	            if(!isBlank(label)) {
	                return label;
	            }

	            org.json.JSONObject key = object.optJSONObject("_key");
	            if(key != null) {
	                String externalId = key.optString("_externalId", "");
	                if(!isBlank(externalId)) {
	                    return externalId;
	                }

	                String internalId = key.optString("_internalId", "");
	                if(!isBlank(internalId)) {
	                    return internalId;
	                }
	            }

	            return object.toString();
	        }

	        return String.valueOf(rawValue).trim();
	    }

	    return "";
	}
	
	private void addSignatureIfPresent(java.util.Set<String> signatures, String name, String value) {
	    if(!isBlank(value)) {
	        signatures.add(name + "|" + normalize(value));
	    }
	}

	private String stringValue(Object value) {
	    if(value == null || value == org.json.JSONObject.NULL) {
	        return "";
	    }

	    return String.valueOf(value).trim();
	}

	private String normalize(String value) {
	    if(value == null) {
	        return "";
	    }

	    return value.trim().toUpperCase(java.util.Locale.ROOT);
	}

	private java.util.Map<String, java.util.List<VariantInfo>> buildSignatureIndex(
	        java.util.List<VariantInfo> variants1,
	        java.util.List<VariantInfo> variants2) {

	    java.util.Map<String, java.util.List<VariantInfo>> index = new java.util.LinkedHashMap<>();

	    addToSignatureIndex(index, variants1);
	    addToSignatureIndex(index, variants2);

	    return index;
	}

	private void addToSignatureIndex(
	        java.util.Map<String, java.util.List<VariantInfo>> index,
	        java.util.List<VariantInfo> variants) {

	    for(VariantInfo variant : variants) {
	        for(String signature : variant.signatures) {
	            index.computeIfAbsent(signature, k -> new java.util.ArrayList<>()).add(variant);
	        }
	    }
	}
	
	private void deleteProductReferencesFromArticles(java.util.List<String> itemsOfTheProduct) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    StringBuilder sb = new StringBuilder();
	    int a = 0;

	    for(String internalArticleId : itemsOfTheProduct) {
	        sb.append(sb.length() == 0 ? "" : ",").append(internalArticleId);
	        a++;

	        if(a % 1000 == 0) {
	            qp.put("items", sb.toString());
	            rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
	            sb.setLength(0);
	            qp.clear();
	        }
	    }

	    if(sb.length() > 0) {
	        qp.put("items", sb.toString());
	        rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
	        sb.setLength(0);
	        qp.clear();
	    }
	}

	private void createProductReferencesToId1(java.util.List<String> itemsOfTheProduct, String id1) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    qp.put("includeObjectsInProtocol", "false");

	    RequestHandler rh = new RequestHandler(
	            new org.json.JSONArray().put(
	                    new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")
	            ),
	            1000,
	            request -> rw.writeData("list", "Article", "ProductReference", qp, request, this::log)
	    );

	    for(String internalId : itemsOfTheProduct) {
	        rh.addRow(
	                new org.json.JSONObject()
	                        .put("object", new org.json.JSONObject().put("id", internalId))
	                        .put("qualification", new org.json.JSONObject().put("referencedSupplierAid", id1))
	                        .put("values", new org.json.JSONArray().put(id1))
	        );
	    }

	    rh.sendData();
	}

	private void mergeObjectMissing(org.json.JSONObject target, org.json.JSONObject source, java.util.Set<String> excludedKeys) {
	    for(Object keyObject : source.keySet()) {
	        String key = String.valueOf(keyObject);

	        if(excludedKeys != null && excludedKeys.contains(key)) {
	            continue;
	        }

	        Object sourceValue = source.opt(key);

	        if(isEmptyJsonValue(sourceValue)) {
	            continue;
	        }

	        Object targetValue = target.opt(key);

	        if(isEmptyJsonValue(targetValue)) {
	            target.put(key, cloneJsonValue(sourceValue));
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONObject && targetValue instanceof org.json.JSONObject) {
	            mergeObjectMissing((org.json.JSONObject) targetValue, (org.json.JSONObject) sourceValue, null);
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONArray && targetValue instanceof org.json.JSONArray) {
	            mergeArrayMissing(key, (org.json.JSONArray) targetValue, (org.json.JSONArray) sourceValue);
	        }
	    }
	}
	
	private java.util.List<String> collectArticleObjectIdsByProduct(String productIdentifier) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    qp.put("pageSize", "2000");
	    qp.put("products", "'" + productIdentifier + "'@1");

	    java.util.List<String> items = new java.util.ArrayList<>();

	    rw.collectData("list", "Article", null, "byProducts", qp, row -> {
	        items.add(row.getJSONObject("object").getString("id"));
	    });

	    return items;
	}
	
	private void mergeArrayMissing(String sectionName, org.json.JSONArray targetArray, org.json.JSONArray sourceArray) {
	    java.util.Map<String, org.json.JSONObject> targetByKey = new java.util.LinkedHashMap<>();

	    for(int i = 0; i < targetArray.length(); i++) {
	        Object value = targetArray.opt(i);

	        if(value instanceof org.json.JSONObject) {
	            org.json.JSONObject object = (org.json.JSONObject) value;
	            targetByKey.put(buildArrayItemKey(sectionName, object), object);
	        }
	    }

	    for(int i = 0; i < sourceArray.length(); i++) {
	        Object sourceValue = sourceArray.opt(i);

	        if(!(sourceValue instanceof org.json.JSONObject)) {
	            if(!arrayContainsEquivalentValue(targetArray, sourceValue)) {
	                targetArray.put(cloneJsonValue(sourceValue));
	            }
	            continue;
	        }

	        org.json.JSONObject sourceObject = (org.json.JSONObject) sourceValue;
	        String sourceKey = buildArrayItemKey(sectionName, sourceObject);
	        org.json.JSONObject targetObject = targetByKey.get(sourceKey);

	        if(targetObject == null) {
	            targetArray.put(new org.json.JSONObject(sourceObject.toString()));
	        } else {
	            mergeObjectMissing(targetObject, sourceObject, null);
	        }
	    }
	}

	private String buildArrayItemKey(String sectionName, org.json.JSONObject object) {
	    if("lang".equals(sectionName)) {
	        return "lang|" + nestedValue(object, "_qualification.language._key");
	    }

	    if("structureGroupMap".equals(sectionName)) {
	        return "structureGroupMap|" + objectKey(object.optJSONObject("_qualification"), "structureGroup");
	    }

	    if("attribute".equals(sectionName)) {
	        String identifier = object.optString("identifier", "");
	        if(!isBlank(identifier)) {
	            return "attribute|" + identifier;
	        }

	        return "attribute|" + nestedValue(object, "_qualification.nameInKeyLang");
	    }

	    if("_characteristicRecords".equals(sectionName)) {
	        String characteristic = objectKey(object.optJSONObject("_qualification"), "characteristic");
	        String recordKey = nestedValue(object, "_qualification.recordKey");
	        String parentRecordKey = nestedValue(object, "_qualification.parentRecordKey");
	        return "_characteristicRecords|" + characteristic + "|" + recordKey + "|" + parentRecordKey;
	    }

	    if("productExtraData".equals(sectionName)) {
	        return "productExtraData|" + objectKey(object.optJSONObject("_qualification"), "targetMarket");
	    }

	    if("value".equals(sectionName)) {
	        String lang = nestedValue(object, "_qualification.language._key");
	        String identifier = nestedValue(object, "_qualification.identifier");
	        return "value|" + lang + "|" + identifier;
	    }

	    if("_recordLang".equals(sectionName)) {
	        return "_recordLang|" + nestedValue(object, "_qualification.language._key");
	    }

	    return sectionName + "|" + object.toString();
	}

	private String objectKey(org.json.JSONObject parent, String childName) {
	    if(parent == null) {
	        return "";
	    }

	    org.json.JSONObject child = parent.optJSONObject(childName);

	    if(child == null) {
	        return "";
	    }

	    org.json.JSONObject key = child.optJSONObject("_key");

	    if(key != null) {
	        String externalId = key.optString("_externalId", "");
	        String internalId = key.optString("_internalId", "");
	        String entityId = String.valueOf(key.opt("_entityId"));
	        return firstNotBlank(externalId, internalId, entityId);
	    }

	    String externalId = child.optString("_externalId", "");
	    String internalId = child.optString("_internalId", "");
	    String code = child.optString("_code", "");
	    String keyValue = String.valueOf(child.opt("_key"));

	    return firstNotBlank(externalId, internalId, code, keyValue);
	}

	private String nestedValue(org.json.JSONObject object, String path) {
	    if(object == null || isBlank(path)) {
	        return "";
	    }

	    String[] parts = path.split("\\.");
	    Object current = object;

	    for(String part : parts) {
	        if(!(current instanceof org.json.JSONObject)) {
	            return "";
	        }

	        current = ((org.json.JSONObject) current).opt(part);

	        if(current == null || current == org.json.JSONObject.NULL) {
	            return "";
	        }
	    }

	    return String.valueOf(current);
	}

	private void clearSkuAndEanFromArticles(java.util.List<VariantInfo> variants) {
	    for(VariantInfo variant : variants) {
	        org.json.JSONObject data = variant.data;

	        data.put("sku", org.json.JSONObject.NULL);
	        data.put("gtin", org.json.JSONObject.NULL);

	        clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);

	        org.json.JSONObject writeResponse = rw.getRw().makeRequest(
	                "PUT",
	                "/object/Article/" + variant.articleObjectId,
	                new java.util.HashMap<>(),
	                data.toString()
	        );

	        if(writeResponse == null) {
	            log("PANIC: fallo PUT Article " + variant.articleObjectId + ". rawResponse=" + rw.getRw().getRawResponse());
	            return;
	        }
	    }
	}

	private void clearCharacteristicRecords(org.json.JSONObject data, java.util.Set<String> characteristicCodesToClear) {
	    org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

	    if(records == null) {
	        return;
	    }

	    org.json.JSONArray kept = new org.json.JSONArray();

	    for(int i = 0; i < records.length(); i++) {
	        org.json.JSONObject record = records.optJSONObject(i);

	        if(record == null) {
	            kept.put(records.opt(i));
	            continue;
	        }

	        String code = nestedValue(record, "_qualification.characteristic._code");

	        if(characteristicCodesToClear.contains(code)) {
	            log("Quitando characteristicRecord de identidad: " + code);
	            continue;
	        }

	        kept.put(record);
	    }

	    data.put("_characteristicRecords", kept);
	}
	
	private VariantMergeDecision decideVariantMovement(
	        java.util.List<VariantInfo> variants2,
	        java.util.Map<String, java.util.List<VariantInfo>> signatureIndex) {

	    java.util.List<VariantInfo> articlesToMoveToProduct1 = new java.util.ArrayList<>();
	    java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean = new java.util.ArrayList<>();

	    for(VariantInfo variant2 : variants2) {
	        if(variant2.signatures.isEmpty()) {
	            log("Article de id2 sin firma útil; se mueve por conservación. article=" + variant2.articleObjectId);
	            articlesToMoveToProduct1.add(variant2);
	            continue;
	        }

	        boolean matchedAgainstId1 = false;
	        java.util.Set<String> matchedRefsForLog = new java.util.LinkedHashSet<>();

	        for(String signature : variant2.signatures) {
	            java.util.List<VariantInfo> refs = signatureIndex.get(signature);

	            if(refs == null || refs.isEmpty()) {
	                continue;
	            }

	            for(VariantInfo ref : refs) {
	                matchedRefsForLog.add(signature + " -> " + ref.toString());

	                if(ref.belongsToProduct1()) {
	                    matchedAgainstId1 = true;
	                }
	            }
	        }

	        if(matchedAgainstId1) {
	            duplicatedArticlesToDetachAndClean.add(variant2);
	            log("Article duplicado contra id1; se despoja SKU/EAN. article="
	                    + variant2.articleObjectId
	                    + ", matches="
	                    + matchedRefsForLog);
	        } else {
	            articlesToMoveToProduct1.add(variant2);
	            log("Article de id2 sin coincidencia contra id1; se mueve a id1. article="
	                    + variant2.articleObjectId
	                    + ", matches="
	                    + matchedRefsForLog);
	        }
	    }

	    return new VariantMergeDecision(articlesToMoveToProduct1, duplicatedArticlesToDetachAndClean);
	}

	private static class VariantMergeDecision {
	    final java.util.List<VariantInfo> articlesToMoveToProduct1;
	    final java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean;

	    VariantMergeDecision(
	            java.util.List<VariantInfo> articlesToMoveToProduct1,
	            java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean) {

	        this.articlesToMoveToProduct1 = articlesToMoveToProduct1;
	        this.duplicatedArticlesToDetachAndClean = duplicatedArticlesToDetachAndClean;
	    }
	}
	
	private static class VariantInfo {
	    final String articleObjectId;
	    final String productOwner;
	    final org.json.JSONObject data;
	    final java.util.Set<String> signatures;

	    VariantInfo(String articleObjectId, String productOwner, org.json.JSONObject data, java.util.Set<String> signatures) {
	        this.articleObjectId = articleObjectId;
	        this.productOwner = productOwner;
	        this.data = data;
	        this.signatures = signatures;
	    }

	    boolean belongsToProduct1() {
	        return "id1".equals(productOwner);
	    }

	    @Override
	    public String toString() {
	        return productOwner + ":" + articleObjectId;
	    }
	}

	private boolean arrayContainsEquivalentValue(org.json.JSONArray array, Object value) {
	    String valueString = String.valueOf(value);

	    for(int i = 0; i < array.length(); i++) {
	        Object current = array.opt(i);

	        if(String.valueOf(current).equals(valueString)) {
	            return true;
	        }
	    }

	    return false;
	}

	private Object cloneJsonValue(Object value) {
	    if(value instanceof org.json.JSONObject) {
	        return new org.json.JSONObject(((org.json.JSONObject) value).toString());
	    }

	    if(value instanceof org.json.JSONArray) {
	        return new org.json.JSONArray(((org.json.JSONArray) value).toString());
	    }

	    return value;
	}

	private boolean isEmptyJsonValue(Object value) {
	    if(value == null || value == org.json.JSONObject.NULL) {
	        return true;
	    }

	    if(value instanceof String) {
	        return ((String) value).trim().isEmpty();
	    }

	    if(value instanceof org.json.JSONArray) {
	        return ((org.json.JSONArray) value).length() == 0;
	    }

	    if(value instanceof org.json.JSONObject) {
	        return ((org.json.JSONObject) value).length() == 0;
	    }

	    return false;
	}

	private boolean isBlank(String value) {
	    return value == null || value.trim().isEmpty();
	}

	private String firstNotBlank(String... values) {
	    if(values == null) {
	        return "";
	    }

	    for(String value : values) {
	        if(!isBlank(value) && !"null".equalsIgnoreCase(value)) {
	            return value;
	        }
	    }

	    return "";
	}
	
//	private void resolveVariantParent(String znprst, String pid, String satnr) {
//		String[] pinf = tools.checkProductBySKU(satnr);
//		if(pinf != null && !pid.equals(pinf[0])) {
//			if("00".equals(pinf[1])) {
//				java.util.Map<String, String> qp = new java.util.HashMap<>();
//				qp.put("items", "'" + znprst + "'@1");
//				rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
//			}
//		}else {
//			java.util.Map<String, String> qp = new java.util.HashMap<>();
//			qp.put("items", "'" + znprst + "'@1");
//			rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
//		}
//	}
	
	private String chooseProperProductZNPRST(String sku, String znprst) {
		String chosenOne = znprst;
		String[] info = tools.checkProductBySKU(sku);
		String externalId = null;
		if(info != null) {
			externalId = info[0];
			if(!znprst.equals(externalId)) {
				return externalId;
			}
		}
		return chosenOne;
	}
	
	private String chooseProperArticleZNPRST(String sku, String znprst) {
		String chosenOne = znprst;
		String externalId = tools.checkArticleBySKU(sku);
		if(externalId != null && !znprst.equals(externalId)) {
			return externalId;
		}
		return chosenOne;
	}
	
	private ProductMergeDecision resuelveEmpateDeProducto(String sku, String znprst) {
	    ProductMergeDecision decision = new ProductMergeDecision();
	    decision.productIdToUse = znprst;
	    decision.shouldMerge = false;
	    decision.manualReview = false;

	    String[] info = tools.checkProductBySKU(sku);

	    if(info == null) {
	        decision.reason = "No existing Product2G by SKU";
	        return decision;
	    }

	    String externalId = info[0];

	    if(externalId == null || "".equals(externalId) || znprst.equals(externalId)) {
	        decision.productIdToUse = znprst;
	        decision.reason = "Same Product2G or empty existing";
	        return decision;
	    }

	    String winner = decideProductWinner(znprst, externalId);
	    String loser = winner.equals(znprst) ? externalId : znprst;

	    decision.productIdToUse = winner;
	    decision.id1 = winner;
	    decision.id2 = loser;

	    if(winner == null || loser == null) {
	        decision.manualReview = true;
	        decision.reason = "Could not decide winner";
	        return decision;
	    }

	    if(isSameOriginUnsafe(znprst, externalId)) {
	        decision.manualReview = true;
	        decision.shouldMerge = false;
	        decision.reason = "Same-origin duplicate Product2G. current=" + znprst + ", existing=" + externalId;

	        log("PANIC, necesita generarse una tarea de resolución de duplicados. (current: " + znprst + " | existing: " + externalId + ")");
//	        logProductDuplicate(znprst, externalId);

	        return decision;
	    }

	    decision.shouldMerge = true;
	    decision.reason = "Merge allowed. winner=" + winner + ", loser=" + loser;

	    return decision;
	}
	
	private String decideProductWinner(String currentId, String existingId) {
	    int currentRank = originRank(currentId);
	    int existingRank = originRank(existingId);

	    if(existingRank > currentRank) {
	        return existingId;
	    }

	    if(currentRank > existingRank) {
	        return currentId;
	    }

	    return currentId;
	}

	private int originRank(String id) {
	    if(id == null || "".equals(id)) {
	        return 0;
	    }

	    if(id.length() == 16) {
	        return 3; // P360
	    }

	    if(id.startsWith("S")) {
	        return 2; // STEP
	    }

	    if(id.startsWith("LVP")) {
	        return 1; // SAP/local fallback
	    }

	    return 0;
	}

	private boolean isSameOriginUnsafe(String currentId, String existingId) {
	    return originRank(currentId) == originRank(existingId);
	}
	
	private void resuelveEmpateDeArticulo(String sku, String znprst) {
		
		/***********************************************/
		
		String externalId = tools.checkArticleBySKU(sku);
		if(externalId != null) {
			if(!znprst.equals(externalId)) {
				if(znprst.length() == 16 && externalId.length() == 16) {
					// PANIC, necesita generarse una tarea de resolución de duplicados
					log("PANIC, necesita generarse una tarea de resolución de duplicados. (current: " + znprst + " | existing: " + externalId + ")");
					java.util.Map<String, String> qp = new java.util.HashMap<>();
					qp.put("includeObjectsInProtocol", "false");
					RequestHandler rh = new RequestHandler( 
							new org.json.JSONArray()
							.put(new org.json.JSONObject().put("identifier", "ArticleLog.Remarks(es)"))
							, 1000
							, request -> rw.writeData("list", "Article", null, qp, request, this::log) 
							);
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + znprst)));
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + znprst + "'@1")).put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + externalId)));
					rh.sendData();
				}else if(znprst.startsWith("S") && externalId.startsWith("S")) {
					// PANIC, necesita generarse una tarea de resolución de duplicados
					log("PANIC, necesita generarse una tarea de resolución de duplicados. (current: " + znprst + " | existing: " + externalId + ")");
					java.util.Map<String, String> qp = new java.util.HashMap<>();
					qp.put("includeObjectsInProtocol", "false");
					RequestHandler rh = new RequestHandler( 
							new org.json.JSONArray()
							.put(new org.json.JSONObject().put("identifier", "ArticleLog.Remarks(es)"))
							, 1000
							, request -> rw.writeData("list", "Article", null, qp, request, this::log) 
							);
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + znprst)));
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + znprst + "'@1")).put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + externalId)));
					rh.sendData();
				}else if(externalId.startsWith("LVP")) {
					java.util.Map<String, String> qp = new java.util.HashMap<>();
					qp.put("includeObjectsInProtocol", "false");
					RequestHandler rh = new RequestHandler( 
							new org.json.JSONArray()
								.put(new org.json.JSONObject().put("identifier", "Article.SKU"))
								.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
								.put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))
								.put(new org.json.JSONObject().put("identifier", "ArticleLog.Remarks(es)"))
							, 1000
							, request -> rw.writeData("list", "Article", null, qp, request, this::log) 
						);
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put("").put("").put("Eliminada").put("Eliminado por arribo de producto con ID establecido por sistema PIM (" + (znprst.length() == 16 ? "P360" : "STEP") + ")")));
					rh.sendData();
				}
			}
		}
		
		/***********************************************/
		
	}
	
	private String nvl(String val) {
		return val == null ? "" : val;
	}
	
	private void sendArticleSKUToAdmin(String supplierAID, String sku, DataRequestor dr) {
		String r = dr.getArticleData(new org.json.JSONArray().put(supplierAID));
		if(r != null) {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(r);
				org.json.JSONArray itms = jr.getJSONArray("items");
				itms.getJSONObject(0).put("SKU", sku);
				log("Sending new SKU: " + dr.putArticleData(itms) );
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
	}
	
	private void sendProductSKUToAdmin(String productNo, String sku, DataRequestor dr) {
		pids.add(productNo);
		String r = dr.getProductData(new org.json.JSONArray().put(productNo));
		if(r != null) {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(r);
				org.json.JSONArray itms = jr.getJSONArray("items");
				itms.getJSONObject(0).put("SKU", sku);
				log("Sending new SKU (prod): " + dr.putProductData(itms) );
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
	}
	
	private void sendParentChildRel() {
		org.json.JSONArray items = new org.json.JSONArray();
		org.json.JSONObject item = null;
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject req = new org.json.JSONObject();
		req.put("columns", columns);
		req.put("rows", rows);
		for( java.util.Map.Entry<String, String> entry : articleHigherLevelProduct.entrySet() ) {
			item = new org.json.JSONObject();
			item.put("supplierAID", entry.getKey());
			item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
			item.put("productNo", entry.getValue());
			items.put(item);
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue())).put("values", new org.json.JSONArray().put(entry.getValue())));
			if(rows.length() == 5000) {
				rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
			}
		}
		if(rows.length() > 0) {
			rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
		}
	}
	
	private void sendData() {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		if(rowsSKU.length() > 0) { 
			log("Sending skus (" + rowsSKU.length() + ")");
			rw.writeData("list", "Product2G", null, qp, requestSKU, this::log);
		}
		if(rows.length() > 0) { 
			log("sending block (product, " + rows.length() + ")");
			rw.writeData("list", "Product2G", null, qp, request, this::log);
		}
		if(rowsArticle.length() > 0) { 
			log("sending bloc (article, " + rowsArticle.length() + ")");
			rw.writeData("list", "Article", null, qp, requestArticle, this::log);
		}
		if(requestStatus.getJSONArray("rows").length() > 0) {
			log("Sending (status): " + requestStatus.getJSONArray("rows").length());
			rw.writeData("list", "Product2G", null, qp, requestStatus, this::log);
		}
		if(requestCommercialECC.getJSONArray("rows").length() > 0) {
			log("Sending (com ecc): " + requestCommercialECC.getJSONArray("rows").length());
			rw.writeData("list", "Product2G", null, qp, requestCommercialECC, this::log);
		}
		for(java.util.Map.Entry<String, org.json.JSONObject> entry : peticiones.entrySet()) {
			if(entry.getValue().getJSONArray("rows").length() > 0) {
				log("La esa (el produtto): " + entry.getKey());
				rw.writeData("list", "Product2G", null, qp, entry.getValue(), this::log);
			}
		}
		for(java.util.Map.Entry<String, org.json.JSONObject> entry : peticionesArticles.entrySet()) {
			if(entry.getValue().getJSONArray("rows").length() > 0) {
				log("La esa (art): " + entry.getKey());
				rw.writeData("list", "Article", null, qp, entry.getValue(), this::log);
			}
		}
		sendParentChildRel();
		RealExportProductsExpressOMS repO = new RealExportProductsExpressOMS();
    	repO.doIt(pids.toArray(new String[] {}), true);
    	pids.clear();
	}
	
	private String determineBusiness(String negocio) {
		return "".equals(negocio) ? null : "MARKETPLACE".equals(negocio) ? "Marketplace" : "Liverpool" ;
	}
	
	private java.util.Map<String, String> readFieldSections() {
		try {
			java.util.Map<String, String> data = new java.util.TreeMap<>();
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections").toFile() ), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					data.put(pieces[0], pieces[1]);
				}
			}
			return data;
		} catch (IOException e) {
			logE(e);
		}
		return null;
	}
	
	private java.util.Map<String, String> readECCFieldMappings() {
		try {
			java.util.Map<String, String> data = new java.util.TreeMap<>();
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping" ).toFile() ), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					data.put(pieces[0], pieces[1]);
				}
			}
			return data;
		} catch (IOException e) {
			logE(e);
		}
		return null;
	}
	
	private void arremangalos(java.util.Map<String, java.util.Map<String, String>> newAttributeValues, java.util.Map<String, java.util.List<java.util.Map<String, String>>> misniños) {
//		RESTWorkshop rw = new RESTWorkshop();
//		rw.addHeader("Authorization", ParseECC122Response.workshop.getRc().getHeader().get("Authorization"));
//		rw.setBaseUrl(PropertiesManager.get("p360.contingency.servlets_url"));
//		rw.getRc().getHeader().remove("Authorization");
		java.util.Map<String, String> fieldVendorCenterSections = readFieldSections();
		java.util.Map<String, String> eccFieldMapping = readECCFieldMappings();
		String charId = null;
		String vendorCenterSection = null;
		String vendorCenterSectionKey = null;
		org.json.JSONObject msgBody = null;
		org.json.JSONObject section = null;
		java.util.Map<String, String> equivalenciaVCS = cargaMapaDeEquivalenciaDeSeccionDeVendorCenter();
		org.json.JSONArray productos = new org.json.JSONArray();
		String supplier = null;
		java.util.Set<String> proveedoresMigrados = cargaProveedoresMigrados();
		java.util.Map<String, String> equivExt = cargaEquivalenciaEstatusExterno();
		java.util.Map<String, String> extStatus = cargaEstatusExterno();
		String statusCode = null;
		String extwg = null;
		String negocio = null;
		String status = null;
		java.util.List<java.util.Map<String, String>> niños = null;
		for(java.util.Map.Entry<String, java.util.Map<String, String>> entry : newAttributeValues.entrySet()) {
			msgBody = new org.json.JSONObject();
			msgBody.put("proposalId", entry.getKey());
			for(java.util.Map.Entry<String, String> entry0 : entry.getValue().entrySet() ) {
				charId = eccFieldMapping.get(entry0.getKey());
				if(charId != null) {
					vendorCenterSection = fieldVendorCenterSections.get(charId);
					if(vendorCenterSection != null) {
						vendorCenterSectionKey = equivalenciaVCS.get(vendorCenterSection);
						if(vendorCenterSectionKey != null) {
							section = msgBody.has(vendorCenterSectionKey) ? msgBody.getJSONObject(vendorCenterSectionKey) : null;
							if(section == null) {
								section = new org.json.JSONObject();
								msgBody.put(vendorCenterSectionKey, section);
							}
							section.put(charId, entry0.getValue());
						}
					}
				}
			}
			extwg = entry.getValue().get("EXTWG");
			log(entry.getKey() + " - We see: " + extwg + " as business.");
			status = 
					"SFERA".equals(extwg) ? "Gobierno de Datos" 
				  : "DUTY FREE".equals(extwg) ? "Propuesta Generada" 
				  : "MARCAS PROPIAS".equals(extwg) ? "Pendiente Inicio Enriquecimiento" 
				  : "REGULAR".equals(extwg) ? "Pendiente Inicio Enriquecimiento" 
				  : "SERVICIOS".equals(extwg) ? "Pendiente Inicio Enriquecimiento" 
				  : "Propuesta Generada" ;
			statusCode = String.valueOf( 
					  "SFERA".equals(extwg) ? 1021 
					: "DUTY FREE".equals(extwg) ? 1001 
					: "MARCAS PROPIAS".equals(extwg) ? 1002 
					: "REGULAR".equals(extwg) ? 1002 
					: "SERVICIOS".equals(extwg) ? 1002 
					: 1001);
			negocio = determineBusiness(extwg);
			if(negocio != null && !"".equals(negocio)) {
				msgBody.put("Business", negocio);
			}
			if(status != null && !"".equals(status) && statusCode != null && !"".equals(statusCode)) {
				msgBody.put("currentStatus", status);
				status = equivExt.get(statusCode);
				status = extStatus.get(status);
				msgBody.put("externalStatus", status);
				msgBody.put("previousStatus", "SKU Creation");
			}
			supplier = entry.getValue().get("LIFNR");
			niños = misniños.get(entry.getKey());
			if(niños != null) {
				org.json.JSONArray variants = new org.json.JSONArray();
				for(java.util.Map<String, String> data : niños) {
					org.json.JSONObject variant = new org.json.JSONObject();
					variant.put("variantId", "LVP" + data.get("MATNR"));
					variant.put("SKU", data.get("MATNR"));
					String ean = data.get("EAN11_EAN");
					if(ean != null ) {
						variant.put("MainBarCode", ean);
					}
					variants.put(variant);
				}
				msgBody.put("variants", variants);
			}
			if(entry.getKey().length() >= 15) {
				msgBody.put("owner", "P360");
			}else {
				if(supplier != null) {
					msgBody.put("supplier", supplier);
					if(proveedoresMigrados.contains(supplier)) {
						msgBody.put("owner", "P360");
					}else {
						msgBody.put("owner", "STEP");
					}
				}else {
					if(proveedoresMigrados.isEmpty()) {
						msgBody.put("owner", "STEP");
					}else {
						msgBody.put("owner", "");
					}
				}
			}
			productos.put(msgBody);
		}
		postProductsPubSub.publishMessage(
		        new org.json.JSONObject()
		                .put("products", productos)
		                .toString()
		);
		log( "Sent." );
	}
	
	private void closeResources() {
	    try {
	        postProductsPubSub.close();
	    } catch (Exception e) {
	        logE(e);
	    }
	}
	
	private java.util.Map<String, String> cargaEstatusExterno(){
		java.util.Map<String, String> data = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
				new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", "ExternalStatus").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] partes = null;
			while((line = br.readLine()) != null) {
				partes = rw.getRw().parseLine(line);
				data.put(partes[0], partes.length == 1 ? "" : partes[1]);
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return data;
	}
	
	private java.util.Map<String, String> cargaEquivalenciaEstatusExterno(){
		java.util.Map<String, String> data = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
				new java.io.FileInputStream(
						java.nio.file.Paths.get(
								PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", "ExternalStatus").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] partes = null;
			while((line = br.readLine()) != null) {
				partes = rw.getRw().parseLine(line);
				data.put(partes[0], partes.length == 1 ? "" : partes[1]);
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return data;
	}
	
	private java.util.Set<String> cargaProveedoresMigrados(){
		java.util.Set<String> data = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
				new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "proveedores_migrados").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] partes = null;
			while((line = br.readLine()) != null) {
				partes = rw.getRw().parseLine(line);
				data.add(partes[0]);
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return data;
	}
	
	private java.util.Map<String, String> cargaMapaDeEquivalenciaDeSeccionDeVendorCenter(){java.util.Map<String, String> data = new java.util.HashMap<>();
	try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(
			java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", "SeccionesEntradaUnicaCatalogacion").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
		String line = null;
		String[] partes = null;
		while((line = br.readLine()) != null) {
			partes = rw.getRw().parseLine(line);
			data.put(partes[0], partes.length == 1 ? "" : partes[1]);
		}
	}catch(java.io.IOException e) {
		logE(e);
	}
	return data;
	}
	
	private void agregaClasificacion(String itemGroup, String product, String id) throws java.io.IOException {
		if(itemGroup == null || "".equals(itemGroup)) {
			return;
		}
		int internalId = forbiddenChalice(itemGroup, product);
		requestCommercialECC.getJSONArray("rows")
			.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(internalId + "@10001")))
		;
		if(requestCommercialECC.getJSONArray("rows").length() == 10000) {
			rw.writeData("list", "Product2G", null, qp, requestCommercialECC, this::log);
		}
	}
	
	private int forbiddenChalice(String itemGroup, String product) throws IOException {
		int id = -1;
		JdbcConfig jdbcConfig = initJdbcConfig();
		try(java.sql.Connection con = openConnection(jdbcConfig, true)){
			if(product != null && !"".equals(product)) {
				try(java.sql.PreparedStatement pstmnt = con.prepareStatement("select \"StructureGroupID\" from PIM_MAIN.\"StructureGroupDetail\" bb inner join PIM_MAIN.\"StructureGroupRevision\" aa on aa.ID = bb.\"StructureGroupRevisionID\" and aa.\"RevisionID\" = 1 and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'  and aa.\"StructureID\" = 10001 and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' where \"NodeType\" = 'leaf' and \"ParentIdentifier\" = ? and \"Identifier\" = ?")){
					pstmnt.setString(1, itemGroup + "-L4ECC");
					pstmnt.setString(2, product + "-L5ECC");
					try(java.sql.ResultSet rs = pstmnt.executeQuery()){
						if(rs.next()) {
							id = rs.getInt(1);
						}else {
							id = processMissingPair(itemGroup, product);
						}
					}
				}
			}else {
				try(java.sql.PreparedStatement pstmnt = con.prepareStatement("select \"StructureGroupID\" from PIM_MAIN.\"StructureGroupDetail\" bb inner join PIM_MAIN.\"StructureGroupRevision\" aa on aa.ID = bb.\"StructureGroupRevisionID\" and aa.\"RevisionID\" = 1 and aa.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0'  and aa.\"StructureID\" = 10001 and bb.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' where \"NodeType\" = 'leaf' and \"Identifier\" = ?")){
					pstmnt.setString(1, itemGroup + "-L4ECC");
					try(java.sql.ResultSet rs = pstmnt.executeQuery()){
						if(rs.next()) {
							id = rs.getInt(1);
						}
					}
				}
			}
		}catch(ClassNotFoundException | java.sql.SQLException e) {
			logE(e);
		}
		return id;
	}
	
	private int processMissingPair(String itemGroup, String product) {
		org.json.JSONObject req = new org.json.JSONObject();
		req.put("level", 5);
		req.put("nodeType", "leaf");
		req.put("parent", new org.json.JSONObject().put("_externalId", "'" + itemGroup + "'@10001"));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		org.json.JSONObject resp = rw.getRw().makeRequest("PUT", "/object/StructureGroup/'" + product + "-L5ECC'@10001", qp, req.toString());
		if(resp != null && resp.has("_entityItem")) {
			return Integer.parseInt( resp.getJSONObject("_entityItem").getString("_internalId").split("@")[0] );
		}
		return -1;
	}
	
	private void collectNumberOfImages(String productId, String fotosTomaLiverpool) {
		int lacuenta = 0;
		DataRequestor dr = new DataRequestor();
		String rsp = dr.getProductData( new org.json.JSONArray().put( productId ));
		org.json.JSONObject jr = new org.json.JSONObject(rsp);
		org.json.JSONArray items = jr.getJSONArray("items");
		org.json.JSONObject j0 = items.getJSONObject(0);
		String business = j0.getString("Business");
		if(!propuestasRevisadas.contains(productId)) {
			java.util.Set<String> variants = dr.getVariants(productId);
			org.json.JSONArray itms = new org.json.JSONArray();
			variants.forEach(itms::put);
			String rp = dr.getArticleData(itms);
			try {
				jr = new org.json.JSONObject(rp);
				items = jr.getJSONArray("items");
				for(int i=0; i<items.length(); i++) {
					if(!"".equals(items.getJSONObject(i).getString("ProductImage"))) {
						lacuenta++;
						variantesConImagen.add(items.getJSONObject(i).getString("variant"));
					}
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
			propuestasRevisadas.add(productId);
		}
		qp.put("includeObjectsInProtocol", "false");
		log("Las imágenes...");
		if("MKP".equals(business) || ( "Corregido".equals(fotosTomaLiverpool) || (("N".equals(fotosTomaLiverpool) || "".equals(fotosTomaLiverpool)) && lacuenta > 0 ) )) {
			log("1");
			requestStatus.getJSONArray("rows")
				.put(new org.json.JSONObject()
						.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
						.put("values", new org.json.JSONArray().put(1020).put(1022).put("EnProcesoLiverpool")));
		}else if("Y".equals(fotosTomaLiverpool)) {
			log("2");
			requestStatus.getJSONArray("rows")
			.put(new org.json.JSONObject()
					.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
					.put("values", new org.json.JSONArray().put(1020).put(1002).put("EnProcesoLiverpool")));
		}else {
			log("3");
			requestStatus.getJSONArray("rows")
			.put(new org.json.JSONObject()
					.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
					.put("values", new org.json.JSONArray().put(1020).put(1004).put("CargaDeImagen")));
		}
		if(requestStatus.getJSONArray("rows").length() == 10000) {
			log("Sending skus (" + rowsSKU.length() + ")");
			if(rowsSKU.length() > 0) { 
				rw.writeData("list", "Product2G", null, qp, requestSKU, this::log);
			}
			log("sending block (product, " + rows.length() + ")");
			if(rows.length() > 0) { 
				rw.writeData("list", "Product2G", null, qp, request, this::log);
			}
			log("sending bloc (article, " + rowsArticle.length() + ")");
			if(rowsArticle.length() > 0) { 
				rw.writeData("list", "Article", null, qp, requestArticle, this::log);
			}
			rw.writeData("list", "Product2G", null, qp, requestStatus, this::log);
		}
		log("Done with pictures and status stuff...");
	}
	
	private void agregaUnidadesDeMedida(
			  java.util.Map<String, String> unidades
			, java.util.Map<String, String> eccFieldMapping
			, String externalId
	) {
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
		for(String a : atributosLongitud) {
			unidadId = unidades.get( a );
			if(unidadId != null) {
				unidadDeMedidaLongitud = unidadesLongitud.get(unidadId);
				break;
			}
		}
		for(String a : atributosVolumen) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaVolumen = unidadesVolumen.get( unidadId );
				break;
			}
		}
		for(String a : atributosPeso) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaPeso = unidadesPeso.get( unidadId );
				break;
			}
		}
		if(unidadDeMedidaLongitud == null) {
		} else {
			log("Adding la iunit (long): " + unidadDeMedidaLongitud);
			addValue("UnidadDeMedidaLongitud", "Product2G", externalId, unidadDeMedidaLongitud);
		}
		if(unidadDeMedidaPeso == null) {
		}else {
			log("Adding la iunit (peso): " + unidadDeMedidaPeso);
			addValue("UnidadDeMedidaPeso", "Product2G", externalId, unidadDeMedidaPeso);
		}
		if(unidadDeMedidaVolumen == null) {
		}else {
			log("Adding la iunit (vol): " + unidadDeMedidaVolumen);
			addValue("UnidadDeMedidaVolumen", "Product2G", externalId, unidadDeMedidaVolumen);
		}
	}
	
	private boolean excluyeAtributo(Value v) {

		java.util.List<String> mPack = new java.util.ArrayList<>();
		mPack.add("ZLAEPQ");
		mPack.add("ZBREPQ");
		mPack.add("ZHOEPQ");
		mPack.add("ZVOLPQ");
		mPack.add("ZBRGPQ");
		mPack.add("ZNTGPQ");
		if(mPack.contains(v.getAttributeId())) {
			try{
				float val = Float.parseFloat(v.getText());
				return val == 0f;
			}catch(NumberFormatException | NullPointerException e) {
				
			}
		}
		return false;
	}

	private void checkParentVariantsCompleteness(String productId, String itemId, org.json.JSONArray characteristicRecords, String fotosTomaLiverpool, String currentStatus) {
		log("\n\t\tEntering to here! ---> " + productId);
		boolean pass = true;
		DataRequestor dr = new DataRequestor();
		java.util.Set<String> vs = dr.getVariants(productId);
		org.json.JSONArray items = new org.json.JSONArray();
		for(String v : vs) {
			items.put(v);
		}
		org.json.JSONArray items2 = null;
		String resp = dr.getArticleData(items);
		if(resp != null) {
			try{
				org.json.JSONObject jr = new org.json.JSONObject(resp);
				items2 = jr.getJSONArray("items");
				for(int i=0; i<items2.length(); i++) {
					if(items2.getJSONObject(i).has("SKU")) {
						if("".equals(items2.getJSONObject(i).getString("SKU"))) {
							log("This brother does not possess SKU. " + items.getString(i));
							pass = false;
						}
					}
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
		if(pass) {
			sendProductSKUToAdmin(productId, "999" + productId.substring(productId.startsWith("S") ? 1 : 7), dr);
			rowsSKU.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put("999" + productId.substring(productId.startsWith("S") ? 1 : 7))));
			if(rowsSKU.length() == 2000) {
				rw.writeData("list", "Product2G", null, qp, requestSKU, this::log);
			}
			addValue("MensajeCreacionSKU", "Product2G", productId, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format( new java.util.Date()) );
			addValue("SKU", "Product2G", productId, "999" + productId.substring(productId.startsWith("S") ? 1 : 7) );
			log("Desde entering here!, FotoTomadaLVP: " + fotosTomaLiverpool);
			sendWriteRequest("Product2G", productId, characteristicRecords, fotosTomaLiverpool, currentStatus);
		}
	}
	
	private String getArticleIdFromProduct(String productId) {
		DataRequestor dr = new DataRequestor();
		java.util.Set<String> variantIds = dr.getVariants(productId);
		return variantIds != null && !variantIds.isEmpty() ? variantIds.toArray(new String[] {})[0] : null;
	}
	
	private void sendWriteRequestProduct(String id, String itemGroup, String product, String negocio, org.json.JSONArray characteristicRecords) {
		if(id.startsWith("LVP")) {
			String prevStatus = "1020";
			String currentStatus = String.valueOf(
						"SFERA".equals(negocio) ? 1021 
								: "DUTY FREE".equals(negocio) ? 1001 
										: "MARCAS PROPIAS".equals(negocio) ? 1002 
												: "REGULAR".equals(negocio) ? 1002 
														: "SERVICIOS".equals(negocio) ? 1002 
																: 1001
					);
			String externalStatus = 
					"SFERA".equals(negocio) ? "EnProcesoLiverpool"                     : 
						"DUTY FREE".equals(negocio) ? "PropuestaGenerada"              : 
							"MARCAS PROPIAS".equals(negocio) ? "EnProcesoLiverpool"    : 
								"REGULAR".equals(negocio) ? "EnProcesoLiverpool"       : 
									"SERVICIOS".equals(negocio) ? "EnProcesoLiverpool" : "PropuestaGenerada"
					;
			if("SFERA".equals(negocio)) {
				ingresaWorkflow(id, "1234567", "Sfera", "Nuevo producto Sfera");
			}
			requestStatus.getJSONArray("rows")
				.put(new org.json.JSONObject()
					.put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(prevStatus).put(currentStatus).put(externalStatus)));
			if(requestStatus.getJSONArray("rows").length() == 10000) {
				log("Sending skus (" + rowsSKU.length() + ")");
				if(rowsSKU.length() > 0) { 
					rw.writeData("list", "Product2G", null, qp, requestSKU, this::log);
				}
				log("sending block (product, " + rows.length() + ")");
				if(rows.length() > 0) { 
					rw.writeData("list", "Product2G", null, qp, request, this::log);
				}
				log("sending bloc (article, " + rowsArticle.length() + ")");
				if(rowsArticle.length() > 0) { 
					rw.writeData("list", "Article", null, qp, requestArticle, this::log);
				}
				rw.writeData("list", "Product2G", null, qp, requestStatus, this::log);
			}
			if( "MARCAS PROPIAS".equals(negocio) || "REGULAR".equals(negocio) || "SERVICIOS".equals(negocio) ) {
				addValue("EnriquecidoEnForo", "Product2G", id, true );
			}else {
				addValue("EnriquecidoEnForo", "Product2G", id, false );
			}
			addValue("FotoTomadaLiverpool", "Product2G", id, "Y");
		}
		try {
			agregaClasificacion(itemGroup, product, id);
		} catch (java.io.IOException e) {
			logE(e);
		}
	}

	private void ingresaWorkflow(String internalId, String processId, String workflowId, String status) {
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONArray itemIds = new org.json.JSONArray();
		org.json.JSONObject response = null;
		itemIds.put(internalId);
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("POST", "/manage/workflow/status/enter", qp, rb.toString());
		log(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	}
	
	private void sendWriteRequest(String entity, String id, org.json.JSONArray characteristicRecords, String fotoTomadaLiverpool, String currentStatus) {
		if("Product2G".equals(entity) && currentStatus != null
				&& (
						   "1020".equals(currentStatus) 
						|| "".equals(currentStatus) 
						|| currentStatus == null 
						|| "1021".equals(currentStatus)
						|| "10031".equals(currentStatus)
					) 
		) {
			log("Entering to determine images...");
			collectNumberOfImages(id, fotoTomadaLiverpool);
		} else if ("Product2G".equals(entity) && currentStatus != null) {
			log("Not a correct status at this time... " + currentStatus);
		}
	}
	
	private static final java.util.Map<String, org.json.JSONObject> peticiones = new java.util.HashMap<>();
	private static final java.util.Map<String, org.json.JSONObject> peticionesArticles = new java.util.HashMap<>();
	
	private final void creaPeticion(String entity, String externalId, String llave, Object valor) {
		String dataType = dataTypes.get(llave);
		if(dataType == null) {
			collectLookupCharacteristics(dataTypes, lkps);
			dataType = dataTypes.get(llave);
		}
		if(dataType == null) {
			return;
		}
		org.json.JSONObject request = null;
		request = ("Product2G".equals(entity) ? peticiones : peticionesArticles).get(llave);
		if(request == null) {
			request = new org.json.JSONObject();
			org.json.JSONArray columns = new org.json.JSONArray();
			org.json.JSONArray rows = new org.json.JSONArray();
			request.put("columns", columns);
			request.put("rows", rows);
			columns.put(new org.json.JSONObject().put("identifier", entity + "CharacteristicValueLang.Value('" + llave + "',root,\"0000.0000.RK\",'" + llave + "',-1)" ));
			("Product2G".equals(entity) ? peticiones : peticionesArticles).put(llave, request);
		}
		org.json.JSONArray rows = request.getJSONArray("rows");
		String v = null;
		v = 
			  "BrandName".equals(llave) ? mapB.get("ZCOMALOV").get(valor)
			: "LicenseDescription".equals(llave) ? mapB.get("ZZLICLOV").get(valor)
			: "ZIDKTEMP".equals(llave) ? mapB.get("ZIDKTEMPLOV").get(valor)
			: "WHSTC".equals(llave) ? mapB.get("WHSTCLOV").get(valor)
			: String.valueOf( valor );
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put( 
				v
			)));
		if(rows.length() == 10000) {
			rw.writeData("list", entity, null, qp, request, this::log);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private void addValue(String name, String entity, String objectId, Object value) {
		if(value == null)
			return;
		creaPeticion(entity, objectId, name, value);
	}
	
	private void collectLookupCharacteristics(java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> lkps){
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] erg = null;
			while((line = br.readLine()) != null) {
				erg = workshop.parseLine(line, "\"", ";", "\\");
				if(erg != null && erg.length == 6) {
					characteristicsInfo.put(erg[0], erg[1]);
					lkps.put(erg[0], erg[5]);
				}else {
					log("Pieza malformada: --->" + line + "<---");
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	private void calculaProductType(String sapBehvo1, String itemGroup, String fshId, String negocio, String almacenamientoAtt, String skuType, String mtart, String externalId, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ServiceUnavailableException {
		String sapBehvo = null;
		if("Liverpool".equals(negocio) || "Marketplace".equals(negocio)) {
			int month = Integer.parseInt( new java.text.SimpleDateFormat("MM").format(new java.util.Date()) );
			int year = Integer.parseInt( new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) ) + (month < 11 ? 0 : 1);
			addValue("AnoEstacion", "Product2G", externalId, String.valueOf(year));
			addValue("Temporada", "Product2G", externalId, "0003");
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase", rw);
		}else if("Suburbia".equals(negocio)) {
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase_S4H", rw);
			if(fshId != null && fshId.length() >= 4) {
				addValue("FSH_SEASON_YEAR", "Product2G", externalId, fshId.subSequence(0, 4));
			}
		}
		if(sapBehvo == null || "".equals(sapBehvo)) {
			return;
		}
		sapBehvo = sapBehvo.substring(0,2);
		if(sapBehvo1 == null || "".equals(sapBehvo1)) {
			addValue("SAP_BEHVO", "Product2G", externalId, sapBehvo.substring(0,2));
		}
		if(sapBehvo != null && !"".equals(sapBehvo)) {
			String thevalue = "1";
			try{
				org.json.JSONArray rws = new org.json.JSONObject( rw.getRc().getRequest("GET", rw.getBaseUrl() + "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \"" + sapBehvo.substring(0,2) + "\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8")
						, null) ).getJSONArray("rows");
				if(rws.length() > 0) {
					thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
			addValue("ProductType", "Product2G", externalId, thevalue);
		} else {
			if(almacenamientoAtt != null && !"".equals(almacenamientoAtt) && "0001".equals(almacenamientoAtt) && "SERV".equals(skuType)) {
				addValue("ProductType", "Product2G", externalId, "6");
			}else if("DIEN".equals(mtart) && "SB87516".equals(itemGroup)){
				addValue("ProductType", "Product2G", externalId, "6");
			}else {
				addValue("ProductType", "Product2G", externalId, "1");
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
	}
	
	private static final java.util.Set<String> IDENTITY_CHARACTERISTICS =
	        new java.util.HashSet<>(java.util.Arrays.asList(
	                "SKU",
	                "MainBarCode",
	                "MainBarCodeS4H"
	        ));

	private static final java.util.Set<String> VARIANT_MATCH_CHARACTERISTICS =
	        new java.util.HashSet<>(java.util.Arrays.asList(
	                "SKU",
	                "MainBarCode",
	                "MainBarCodeS4H",
	                "ColoursLiverpoolAtt",
	                "TamanoUnico",
	                "SupplierPartNumber"
	        ));
	

    private JdbcConfig initJdbcConfig() throws IOException {
    	JdbcConfig config = new JdbcConfig();
    	Path propertiesPath = resolveServerPropertiesPath();
        Properties raw = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath))
        {
          raw.load(in);
		}
        config.jdbcDriver = resolveRequiredProperty(raw, "db.master.pool.jdbcDriver");
        config.jdbcUrl = resolveRequiredProperty(raw, "db.master.pool.jdbcUrl");
        config.user = resolveRequiredProperty(raw, "db.master.user");
        config.password = resolveRequiredProperty(raw, "db.master.password");
        return config;
    }
    
    private static Path resolveServerPropertiesPath()
    {
      String path = System.getenv("P360_SERVER_PROPERTIES");

      if (path == null || path.trim().isEmpty())
      {
        path = "/u01/Informatica/server.properties";
      }
      else
      {
      }

      Path resolved = Paths.get(path).toAbsolutePath().normalize();

      if (!Files.exists(resolved))
      {
        throw new IllegalArgumentException("No existe server.properties en: " + resolved);
      }

      if (!Files.isRegularFile(resolved))
      {
        throw new IllegalArgumentException("La ruta no es archivo: " + resolved);
      }

      return resolved;
    }

    private static String resolveRequiredProperty(Properties raw, String key)
    {
      String value = resolvePropertyValue(raw, key, new HashSet<String>());

      if (value == null || value.trim().isEmpty())
      {
        throw new IllegalArgumentException("No se encontró la property requerida: " + key);
      }

      return value.trim();
    }

    private static String resolvePropertyValue(Properties raw, String key, Set<String> visiting)
    {
      if (visiting.contains(key))
      {
        throw new IllegalArgumentException("Referencia circular detectada en properties para la clave: " + key);
      }

      String value = raw.getProperty(key);
      if (value == null)
      {
        return null;
      }

      visiting.add(key);

      Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
      StringBuffer sb = new StringBuffer();

      while (matcher.find())
      {
        String referencedKey = matcher.group(1);
        String referencedValue = resolvePropertyValue(raw, referencedKey, visiting);

        if (referencedValue == null)
        {
          throw new IllegalArgumentException("No se pudo resolver la property referenciada: " + referencedKey);
        }

        matcher.appendReplacement(sb, Matcher.quoteReplacement(referencedValue));
      }

      matcher.appendTail(sb);
      visiting.remove(key);

      return sb.toString();
    }
	
	private java.sql.Connection openConnection(JdbcConfig jdbcConfig, boolean autoCommit)
		    throws java.sql.SQLException, ClassNotFoundException
		{
		  Class.forName(jdbcConfig.jdbcDriver);

		  java.sql.Connection connection = java.sql.DriverManager.getConnection(
		      jdbcConfig.jdbcUrl,
		      jdbcConfig.user,
		      jdbcConfig.password
		  );

		  connection.setAutoCommit(autoCommit);

		  return connection;
		}
	
	  private final class JdbcConfig
	  {
	    private String jdbcDriver;
	    private String jdbcUrl;
	    private String user;
	    private String password;
	  }
	
	private static class ProductMergeDecision {
	    String productIdToUse;
	    String id1;
	    String id2;
	    boolean shouldMerge;
	    boolean manualReview;
	    String reason;
	}
//    private static final Path SEQUENCE_FILE = Paths.get("upload_sequence_to_F40.properties");
	
	private static final Logger LOGGER = Logger.getLogger(ParseECC122Response.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/ecc/parseECC122Response-%g.log", 25 * 1024 * 1024, 10, true);
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

	@Override
	public final void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECC122Response.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	@Override
	public final void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECC122Response.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
