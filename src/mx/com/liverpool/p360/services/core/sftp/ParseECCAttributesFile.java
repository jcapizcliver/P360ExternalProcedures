package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleLog;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122AttributesHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.Value;

public class ParseECCAttributesFile implements SimpleLog, Closeable {

	private DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
		
		@Override
		public void logE(Exception e) {
			ParseECCAttributesFile.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			ParseECCAttributesFile.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);
	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	private static final String BASE_URL = PropertiesManager.get( "p360.contingency.base_url" );

	private boolean running = true;

    // SFTP connection parameters
	private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final String USER = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.ecc.remote_directory_122" );//Remote directory to monitor: /interfase/mer/in/step/P360/zrtuab122
	private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.ecc.local_processed_dir_122" ));//Path: /u01/stage/ecc.122/processed
	private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.ecc.state_file_att" ));//File: processed_ecc.122_attributes.properties
	private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.ecc.use_cache" ));//false;

	private static final java.util.Map<String, String> characteristicsAndLookups = new java.util.HashMap<>();
	private final ParsersTools tools = new ParsersTools(this, dr);

	private final java.util.Map<String, String> dataTypes = new java.util.TreeMap<>();
	private final java.util.Map<String, String> lkps = new java.util.TreeMap<>();
	private final java.util.Map<String, String> qp = new java.util.HashMap<>();
    private java.util.Map<String, java.util.Map<String, String>> diccionarios = new java.util.TreeMap<>();
	private java.util.Map<String, java.util.Map<String, String>> map = new java.util.TreeMap<>();
	private java.util.Map<String, java.util.Map<String, String>> mapB = new java.util.TreeMap<>();
	private java.util.Map<String, String> eccFieldMapping = new java.util.TreeMap<>();
	private java.util.Map<String, String> eccToCharID = new java.util.TreeMap<>();
	private java.util.Map<String, String> globalDataTypes = new java.util.TreeMap<>();
	
	private final int bs = 5000;
    private long metadataLoadedAt;
    private final java.util.Set<String> cachedSizes=new java.util.TreeSet<>();
	

	private java.util.LinkedList<String> product2GCharacteristics = new java.util.LinkedList<>();
	private java.util.LinkedList<String> articleCharacteristics = new java.util.LinkedList<>();

    private final org.json.JSONArray columnsArticle = new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.TamanoUnico(MX)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)"))
    		;
    private final org.json.JSONArray rowsArticle = new org.json.JSONArray();
    private final org.json.JSONObject requestArticle = new org.json.JSONObject().put("columns", columnsArticle).put("rows", rowsArticle);

    private void launchListenerThread() {
		Thread t = new Thread(()->{
			while(running) {
				try(
					java.net.ServerSocket server = new java.net.ServerSocket(23548);
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
			sendData();
		});
		t.start();
	}

    public void cargaLasCosas() {
		long init = System.currentTimeMillis();
		qp.put("includeObjectsInProtocol", "false");
		log("Material refresh...");
		collectLookupCharacteristics(dataTypes, lkps);
		for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
			tools.collectLookupValues(lkps.get( entry.getKey() ), map, mapB, entry.getValue());
		}
		log("Refreshed took: " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
    
    public ParseECCAttributesFile() {
    	qp.put("includeObjectsInProtocol", "false");
    }
    
    public static void main(String[] args) {
    	
    	try(ParseECCAttributesFile object = new ParseECCAttributesFile()){
	    	object.launchListenerThread();
	    	while(object.running) {
	    		try {
					object.runOnSftp();
				} catch (ParserConfigurationException | SAXException e) {
	    			object.logE(e);
				}
	    		try {
	    			Thread.sleep(600000);
	    		}catch(InterruptedException e) {
	    			object.logE(e);
	    		}
	    	}
    	}catch(java.io.IOException e) {
    		e.printStackTrace();
    	}
    }
    
	public void runOnSftp() throws ParserConfigurationException, SAXException {
        if(DurableSftpQueue.enabled()) {
            DurableSftpQueue.run("ecc", "GenericXMLattributes", () -> running, (body, filename) -> {
                var factory=javax.xml.parsers.SAXParserFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
                ECC122AttributesHandler handler=new ECC122AttributesHandler();
                factory.newSAXParser().parse(new java.io.ByteArrayInputStream(body),handler);
                DurableSftpQueue.records(handler.getCollected(), product -> {
                    processFile(java.util.Collections.singletonList(product), filename);
                    sendData();
                }, this::log);
            }, this::log);
            return;
        }

		qp.put("includeObjectsInProtocol", "false");
		workshop.setBaseUrl(BASE_URL);
    	USE_CACHE = true;

		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();

        java.util.Properties processedState = new java.util.Properties();
        if (USE_CACHE && java.nio.file.Files.exists(STATE_FILE)) {
            try (InputStream in = java.nio.file.Files.newInputStream(STATE_FILE)) {
                processedState.load(in);
            } catch (IOException e) {
            	logE(e);
			}
        }	
        try (SshClient client = SshClient.setUpDefaultClient()) {
            // Mejor cargar la key en el client (no por archivo)
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
                            Iterable<DirEntry> entries = sftp.readDir(REMOTE_DIR);

                            for (DirEntry entry : entries) {
                                String name = entry.getFilename();
                                if (name.equals(".") || name.equals("..")) continue;

                                if (!name.startsWith("GenericXMLattributes")) {
                                    continue;
                                }

                                String filePath = REMOTE_DIR + "/" + name;

                                try (InputStream input = sftp.read(filePath);
                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                                    copyStream(input, out);

                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
                                    java.nio.file.Files.write(localCopy, out.toByteArray());

                                    ECC122AttributesHandler handler = new ECC122AttributesHandler();
                                    log("Processing: " + name);
                                    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(out.toByteArray())) {
                                        parser.parse(bais, handler);
                                    }
                                    processFile(handler.getCollected(), name);

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

                        } catch (Exception sftpBroken) {
                            log("SFTP/session se rompió; reconecto en el siguiente ciclo.");
                            logE(sftpBroken);
                        }
                    }

                } catch (Exception connectOrAuthError) {
                    log("No se pudo conectar/auth; reintento en el siguiente ciclo.");
                    logE(connectOrAuthError);
                }
                sendData();
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

	}

    public static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
    
	public void processFile(java.util.List<ECC122AttributesHandler.Product> products) throws ParserConfigurationException, SAXException, IOException {
        processFile(products, null);
    }

    public void processFile(java.util.List<ECC122AttributesHandler.Product> products, String sourceFile) throws ParserConfigurationException, SAXException, IOException {
		if (products == null) throw new IOException("Malformed ECC attribute document");
		ensureEccMetadataAvailable();

        java.util.Set<String> sizes=cachedSizes;
        if(!DurableSftpQueue.enabled() || System.currentTimeMillis()-metadataLoadedAt>900000) {
            collectLookupCharacteristics(dataTypes, lkps); sizes.clear();
            loadSizeAttributesMap(sizes); loadDiccionarios(new java.util.LinkedList<>(eccToCharID.keySet()));
            metadataLoadedAt=System.currentTimeMillis();
        }
		for (ECC122AttributesHandler.Product product : products) {
			String sku = AttributeImportSupport.eccSku(product.getValues());
			if (sku == null || sku.isBlank()) throw new IOException("Missing MATNR in ECC attributes");
			java.util.Map<String, java.util.List<String>> incoming = AttributeImportSupport.eccAttributes(product.getAttributes());
			String missingPath = PropertiesManager.get("p360.contingency.ecc.unmapped_attributes_file");
            Path deferred = missingPath == null || missingPath.isBlank()
                    ? LOCAL_PROCESSED_DIR.resolveSibling("unmapped-attributes.jsonl") : Paths.get(missingPath);
            ECCUnmappedAttributes.deferUnmapped(incoming, eccToCharID, deferred, sourceFile, sku);
            if (incoming.isEmpty()) continue;
			String[] info = checkProductBySKU(sku);
			String productId = info == null ? null : info[0];
			String itemId = checkArticleBySKU(sku);
			if (itemId != null) {
				String parent = AttributeImportSupport.parentOf(workshop, itemId);
				if (parent != null) productId = parent;
			}
			boolean articleExpected = itemId != null || info == null || "00".equals(info[1]);
			org.json.JSONArray productRecords = new org.json.JSONArray();
			org.json.JSONArray articleRecords = new org.json.JSONArray();
			String sizeLabel = null;
			for (java.util.Map.Entry<String, java.util.List<String>> entry : incoming.entrySet()) {
				String characteristic = eccToCharID.get(entry.getKey());
				if (characteristic == null) {
					throw new IOException("No ECC mapping for " + entry.getKey() + ", SKU " + sku);
				}
				String type = dataTypes.get(characteristic);
				String lookup = lkps.get(characteristic);
				if (type == null || type.isBlank()) throw new IOException("No datatype for " + characteristic);
				if ("LOOKUP".equals(type) && lookup != null && !map.containsKey(lookup)) {
					tools.collectLookupValues(lookup, map, mapB, type);
				}
				org.json.JSONArray converted = new org.json.JSONArray();
				for (String raw : entry.getValue()) {
					Object value = JanaAttributeParser.resolve(characteristic, type, raw, lookup, map, mapB, this::log);
					if (value == null) throw new IOException("Unresolved value for " + characteristic + ", SKU " + sku);
					converted.put(value);
					if (sizes.contains(characteristic)) {
						java.util.Map<String, String> dictionary = diccionarios.get(entry.getKey() + "LOV");
						String label = dictionary == null ? null : dictionary.get(raw);
						if (label != null && !label.isBlank()) sizeLabel = label;
					}
				}
				boolean forProduct = product2GCharacteristics.contains(characteristic);
				boolean forArticle = articleCharacteristics.contains(characteristic);
				if (!forProduct && !forArticle) throw new IOException("No entity for " + characteristic);
				if (forProduct) JanaAttributeParser.addValues(characteristic, converted, productRecords);
				if (forArticle && articleExpected) JanaAttributeParser.addValues(characteristic, converted, articleRecords);
			}
			Object sizeValue = null;
			if (sizeLabel != null && articleExpected) {
				tools.collectLookupValues("TamanoUnicoLOV", map, mapB, "LOOKUP");
				sizeValue = JanaAttributeParser.resolve("TamanoUnico", "LOOKUP", sizeLabel, "TamanoUnicoLOV", map, mapB, this::log);
				if (sizeValue == null) throw new IOException("Unresolved TamanoUnico for SKU " + sku);
				JanaAttributeParser.addValues("TamanoUnico", new org.json.JSONArray().put(sizeValue), articleRecords);
			}
			AttributeImportSupport.requireTargets(sku, productId, itemId, productRecords.length() > 0,
					articleExpected && (articleRecords.length() > 0 || incoming.containsKey("C100")));
			AttributeImportSupport.write(workshop, "Product2G", productId, productRecords);
			AttributeImportSupport.write(workshop, "Article", itemId, articleRecords);
			// Only include supplied extra fields; absent size/color must keep their current value.
			if (itemId != null && (sizeLabel != null || incoming.containsKey("C100"))) {
				org.json.JSONArray columns = new org.json.JSONArray();
				org.json.JSONArray values = new org.json.JSONArray();
				if (sizeLabel != null) {
					columns.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.TamanoUnico(MX)"));
					values.put(sizeLabel);
				}
				if (incoming.containsKey("C100")) {
					columns.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)"));
					values.put(incoming.get("C100").get(0));
				}
				org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows",
						new org.json.JSONArray().put(new org.json.JSONObject().put("object",
								new org.json.JSONObject().put("id", AttributeImportSupport.objectId(itemId))).put("values", values)));
				AttributeImportSupport.requireSuccess(workshop.makeRequest("POST", "/list/Article", qp, request.toString()), "Article extras " + itemId);
			}
			log("Attributes written for SKU " + sku + ": Product2G=" + productId + " (" + productRecords.length()
					+ "), Article=" + itemId + " (" + articleRecords.length() + ")");
		}
	}

	public void sendData() {
		log("Sending data for attributes file...");
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
		if(rowsArticle.length() > 0) {
//			log("Los estos: "  + requestArticle);
			rw.writeData("list", "Article", null, qp, requestArticle, this::log);
		}
	}
	
	public void setMetadataCache(
	        java.util.Map<String, String> eccToCharID,
	        java.util.Map<String, String> eccFieldMapping,
	        java.util.List<String> product2GCharacteristics,
	        java.util.List<String> articleCharacteristics
	) {
	    this.eccToCharID.clear();
	    this.eccToCharID.putAll(eccToCharID);

	    this.eccFieldMapping.clear();
	    this.eccFieldMapping.putAll(eccFieldMapping);

	    this.product2GCharacteristics.clear();
	    this.product2GCharacteristics.addAll(product2GCharacteristics);

	    this.articleCharacteristics.clear();
	    this.articleCharacteristics.addAll(articleCharacteristics);
	}

	public void setMetadataCache(
	        java.util.Map<String, String> eccToCharID,
	        java.util.Map<String, String> eccFieldMapping,
	        java.util.List<String> product2GCharacteristics,
	        java.util.List<String> articleCharacteristics,
	        java.util.Map<String, String> globalDataTypes
	) {
	    setMetadataCache(
	            eccToCharID,
	            eccFieldMapping,
	            product2GCharacteristics,
	            articleCharacteristics
	    );

	    this.globalDataTypes.clear();
	    if (globalDataTypes != null) {
	        this.globalDataTypes.putAll(globalDataTypes);
	    }
	}
	
//	private void loadEqECCAttributes(java.util.Map<String, String> map) {
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		int currentIndex = 0;
//		int totalSize = 0;
//		qp.put("query",  "not CharacteristicIdentifier.AlternativeIdentifier(ECC) is empty");
//		qp.put("fields", "CharacteristicIdentifier.AlternativeIdentifier(ECC),Characteristic.Identifier");
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
//			if(response != null && response.has("totalSize")) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					map.put(values.getString(0),values.getString(1));
//				}
//				currentIndex += response.getInt("pageSize");
//			}else {
//				log("ERROR: " + workshop.getRawResponse());
//			}
//		}while(currentIndex < totalSize);
//		currentIndex = 0;
//	}
	

	private void ensureEccMetadataAvailable() {
		/*
		 * Normalmente estos mapas llegan por setMetadataCache() desde
		 * ParseECC122Response. No los reemplazamos: sólo completamos faltantes.
		 *
		 * El cache/characteristics contiene:
		 *   [0] Characteristic.Identifier
		 *   [1] Characteristic.DataType
		 *   [2] AlternativeIdentifier(ECC)
		 *   ...
		 */
		java.nio.file.Path cacheFile = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics");

		try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(
				cacheFile,
				java.nio.charset.StandardCharsets.UTF_8)) {

			String line;
			while ((line = br.readLine()) != null) {
				String[] pcs = workshop.parseLine(line, "\"", ";", "\\");
				if (pcs == null || pcs.length < 3) {
					continue;
				}

				String characteristicId = pcs[0] == null ? "" : pcs[0].trim();
				String dataType = pcs[1] == null ? "" : pcs[1].trim();
				String eccId = pcs[2] == null ? "" : pcs[2].trim();

				if (!characteristicId.isEmpty() && !dataType.isEmpty()) {
					globalDataTypes.putIfAbsent(characteristicId, dataType);
				}

				if (!characteristicId.isEmpty() && !eccId.isEmpty()) {
					eccFieldMapping.putIfAbsent(characteristicId, eccId);
					eccToCharID.putIfAbsent(eccId, characteristicId);
				}
			}

			log("ECC Attributes metadata ready. mappings="
					+ eccToCharID.size()
					+ ", fieldMappings="
					+ eccFieldMapping.size()
					+ ", dataTypes="
					+ globalDataTypes.size());

		} catch (java.io.IOException e) {
			logE(e);
		}

		/*
		 * Sólo fallback: si no llegaron las listas por setMetadataCache(),
		 * recupera la clasificación de entidades que ya usaba el flujo viejo.
		 */
		if (product2GCharacteristics.isEmpty() && articleCharacteristics.isEmpty()) {
			tools.collectCharacteristicsByEntity(
					product2GCharacteristics,
					articleCharacteristics);

			log("ECC Attributes entities ready. product2G="
					+ product2GCharacteristics.size()
					+ ", article="
					+ articleCharacteristics.size());
		}
	}

	private void loadSizeAttributesMap(java.util.Set<String> atributos) {
		java.util.Map<String, String> relAttrib =
				dastub.getDictionaryValueAlternativeValueMap("RelAttribSTDATG");
		atributos.addAll(relAttrib.keySet());
		log("Loaded size characteristics from DB. Count: " + atributos.size());
	}
	
	private void loadDiccionarios(java.util.LinkedList<String> atributos) {
		java.util.Map<String, String> mp = null;
		for(String att : atributos) {
			mp = diccionarios.get(att + "LOV");
			if(mp == null) {
				log("\tLoading data for: " + att + "LOV");
				mp = loadDiccionario(att + "LOV");
				log("\t\tGot data: " + mp);
				diccionarios.put(att + "LOV", mp);
			}
		}
	}
	
	private java.util.Map<String, String> loadDiccionario(String diccionario) {
		java.util.Map<String, String> atributos = new java.util.TreeMap<>();
		java.util.List<org.json.JSONObject> rows = dastub.getLookupValueCodeNameExternalCodeRows( diccionario, 10, null, true );
		for(org.json.JSONObject row : rows) {
			String code = row.optString("code", "");
			if(code == null || code.isBlank()) {
				continue;
			}
			atributos.put(code, row.optString("name", ""));
		}
		log("Loaded lookup from DB: " + diccionario + ", values=" + atributos.size());
		return atributos;
	}
	
//	private void collectCharacteristicsByEntity(java.util.LinkedList<String> product2G, java.util.LinkedList<String> article) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "Characteristic.Identifier,Characteristic.Entities");
//		qp.put("query", "not CharacteristicIdentifier.AlternativeIdentifier(ECC) is empty and Characteristic.IsActive = true");
//		qp.put("pageSize", "1200");
//		int currentIndex = 0;
//		int totalSize = 0;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		org.json.JSONArray entities = null;
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
//			if(response != null && response.has("totalSize")) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					entities = values.getJSONArray(1);
//					for(int j = 0; j<entities.length(); j++) {
//						if("Product2G".equals(entities.getString(j))) {
//							product2G.addLast(values.getString(0));
//						}else if("Article".equals(entities.getString(j))) {
//							article.addLast(values.getString(0));
//						}
//					}
//				}
//				currentIndex += response.getInt("pageSize");
//			}else {
//				log("ERR: " + workshop.getRawResponse());
//			}
//		}while(currentIndex < totalSize);
//		currentIndex = 0;
//	}
	
	private String checkArticleBySKU(String sku) {
		String resp = dr.articleBySKU(new org.json.JSONArray().put(sku));
		if(resp != null) {
			try {
				org.json.JSONObject r = new org.json.JSONObject(resp);
				org.json.JSONArray items = r.getJSONArray("items");
				org.json.JSONObject j = items.getJSONObject(0);
				return !"".equals( j.getString("article") ) ? j.getString("article") : null;
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
		return null;
	}
	
	private String[] checkProductBySKU(String sku) {
		String[] resp = tools.checkProductBySKU(sku);
		if(resp != null) {
			return new String[] { resp[0], resp[1], resp[2] };
		}
		return null;
	}
	
	private void getDataTypes(java.util.List<String> eccNames, java.util.Map<String, String> dataTypes) {
		String charID = null;
		String dt = null;
		for(String att : eccNames) {
			charID = eccToCharID.get(att);
			if(charID != null) {
				dt = globalDataTypes.get(charID);
				dataTypes.put(charID, dt);
			}
		}
	}
	
	private void collectLookupValues(String charId, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB, String dataType){
		if("LOOKUP".equals(dataType)) {
			java.util.Map<String, String> codeLabel = map.get(charId);
			if(codeLabel == null) {
				codeLabel = collectLookupValues(charId);
				if(codeLabel != null) {
					map.put(charId, codeLabel);
				}
			}
			codeLabel = mapB.get(charId);
			if(codeLabel == null) {
				codeLabel = collectLookupValuesBackwards(charId);
				if(codeLabel != null) {
					mapB.put(charId, codeLabel);
				}
			}
		}else if(dataType == null) {
			log("Attribute not known: " + charId);
		}
	}
	
	private String getMeTheLookup(String characteristicIdentifier) {
		if(characteristicsAndLookups.isEmpty()) {
			try(java.io.BufferedReader br = 
					new java.io.BufferedReader(
							new java.io.InputStreamReader(
									new java.io.FileInputStream(
											java.nio.file.Paths.get(
													PropertiesManager.get("p360.contingency.templates_cache_directory"), 
													"characteristic_and_lookups").toString())))){
				String line = null;
				String delim = "\"";
				String sep = ";";
				String escp = "\\";
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line, delim, sep, escp);
					if(!"".equals(pieces[0])) {
						characteristicsAndLookups.put(pieces[0], pieces[1]);
					}
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		String name = characteristicsAndLookups.get(characteristicIdentifier);
		log("Asking for: " + characteristicIdentifier + ", got: " + name);
		return name;
	}
	
	private java.util.Map<String, String> collectLookupValues(String charId) {
		java.util.Map<String, String> keyValues = new java.util.TreeMap<>();
		log("Loading regular data for: " + charId);
		String lookup = getMeTheLookup(charId);
		if(lookup == null || "".equals(lookup)) {
			return null;
		}
		String container = lookup.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				keyValues.put(pieces[0], pieces[1]);
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return keyValues;
	}
	
	private java.util.Map<String, String> collectLookupValuesBackwards(String charId) {
		java.util.Map<String, String> keyValues = new java.util.TreeMap<>();
		String lookup = getMeTheLookup(charId);
		if(lookup == null || "".equals(lookup)) {
			return null;
		}
		String container = lookup.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				keyValues.put(pieces[1], pieces[0]);
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return keyValues;
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
		if(rows.length() == bs) {
			rw.writeData("list", entity, null, qp, request, this::log);
		}
	}
	
	private void addValue(String name, String entity, String objectId, Object value) {
		if(value == null)
			return;
		creaPeticion(entity, objectId, name, value);
	}
	
	private void collectLookupCharacteristics(java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> lookups) {
		for (org.json.JSONObject row : dastub.getCharacteristicIntegrationMetadataRows()) {
			String id = row.optString("identifier", "");
			if (id.isBlank()) continue;
			characteristicsInfo.put(id, row.optString("dataType", ""));
			lookups.put(id, row.optString("lookup", ""));
			String ecc = row.optString("ecc", "");
			if (!ecc.isBlank()) {
				eccToCharID.put(ecc, id);
				eccFieldMapping.put(id, ecc);
			}
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ParseECCAttributesFile.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/ecc/parseECC122ResponseAttributes2-%g.log", 25 * 1024 * 1024, 10, true);
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
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECC122ResponseAttributes2.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	@Override
	public final void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECC122ResponseAttributes2.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

	@Override
	public void close() throws IOException {
		dastub.close();
	}
    
}
