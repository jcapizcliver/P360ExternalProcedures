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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleLog;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ParseJanaAttributesFile implements Closeable {

	private final DBAccessDataStub dastub = new DBAccessDataStub(new ELog() {
		@Override
		public void log(String message) {
			ParseJanaAttributesFile.this.log(message);
		}

		@Override
		public void logE(Exception e) {
			ParseJanaAttributesFile.this.logE(e);
		}
	});

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	private static final XMLMisc xmm = workshop.getXmm();
	private static final String BASE_URL = PropertiesManager.get("p360.contingency.base_url");
//			"https://webctep360dev.liverpool.com.mx/rest/V2.0";
//			"http://172.18.237.162:1512/rest/V2.0";

	static {
		workshop.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		workshop.setBaseUrl(BASE_URL);
	}
	
	private final ParsersTools tools = new ParsersTools(new SimpleLog() {
			
			@Override
			public final void log(String message) {
				try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
						new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJana122ResponseAttributes.log").toString(), true)))) {
					pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
							+ "]  " + message);
				} catch (java.io.IOException e) {
				}
			}
	
			@Override
			public final void logE(Exception ex) {
				try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
						new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJana122ResponseAttributes.log").toString(), true)))) {
					ex.printStackTrace(pw);
				} catch (java.io.IOException e) {
				}
			}
		}, new DataRequestor(dastub));

    // SFTP connection parameters
	 	private static final String HOST = PropertiesManager.get( "p360.contingency.s4h.host" ); //SFTP server address: 172.18.184.26
	    private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.s4h.port" ));//SFTP server port: 22
	    private static final String USER = PropertiesManager.get( "p360.contingency.s4h.userp360" );// SFTP username: userp360
	    private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.s4h.remote_directory_122" );//Remote directory to monitor: /interfase/mer/out/step/P360/zrtuab122
	    private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.s4h.local_processed_dir_122" ));//Path: /u01/stage/SBB_122/processed
	    private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.s4h.state_file_att" ));//File: processed_s4h.122_attributes.properties
	    private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.s4h.use_cache" ));//false;
	    private final java.util.Map<String, String> lkps = new java.util.TreeMap<>();
	    private static final java.util.Map<String, String> qp0 = new java.util.HashMap<>();
	    
	    private final org.json.JSONObject reqColor = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)"))).put("rows", new org.json.JSONArray());
	    private final org.json.JSONObject reqTalla = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.TamanoUnico(MX)"))).put("rows", new org.json.JSONArray());

	    private void addColor(String id, String color) {
	    	org.json.JSONArray rows = reqColor.getJSONArray("rows");
	    	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(color)));
	    	if(rows.length() == 1000) {
	    		rw.writeData("list", "Article", null, qp0, reqColor, this::log);
	    	}
	    }

	    private void addTalla(String id, String talla) {
	    	org.json.JSONArray rows = reqTalla.getJSONArray("rows");
	    	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(talla)));
	    	if(rows.length() == 1000) {
	    		rw.writeData("list", "Article", null, qp0, reqTalla, this::log);
	    	}
	    }
	    
	    private void sendData() {
	    	if(reqColor.getJSONArray("rows").length() > 0) {
	    		rw.writeData("list", "Article", null, qp0, reqColor, this::log);
	    	}
	    	if(reqTalla.getJSONArray("rows").length() > 0) {
	    		rw.writeData("list", "Article", null, qp0, reqTalla, this::log);
	    	}
	    }

    private java.util.Map<String, java.util.Map<String, String>> diccionarios = new java.util.TreeMap<>();
	private java.util.Map<String, java.util.Map<String, String>> map = new java.util.TreeMap<>();
	private java.util.Map<String, java.util.Map<String, String>> mapB = new java.util.TreeMap<>();
	private java.util.Map<String, String> s4hFieldMapping = new java.util.TreeMap<>();
	

    public static void main(String[] args) {
    	try(ParseJanaAttributesFile object = new ParseJanaAttributesFile()){
	    	while(true) {
	    		object.runOnSftp(args);
	    		try {
	    			Thread.sleep(600000);
	    		}catch(InterruptedException e) {
	    			object.logE(e);
	    		}
	    	}
    	}
    	
//    	ParseECCAttributesFile parser = new ParseECCAttributesFile();
//    	try {
//        	parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\GenericXMLproducts20250529070308.XML"), null);
//        	parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\GenericXMLproducts20250603093107.XML"), null);
//        	parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\GenericXMLproducts20250616104642.XML"), null);
    		
//    		parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\muestra_ecc\\GenericXMLproducts20250616104634.XML"), null);
//    		parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\muestra_ecc\\GenericXMLproducts20250618131634.XML"), null);
    		
//    		parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\muestra_ecc\\GenericXMLproducts20250625160354.XML"), null);
//    		parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\muestra_ecc\\GenericXMLproducts20250625160354.XML"), null);
//    		parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\muestra_ecc\\GenericXMLproducts20250625124919.XML"), null);
//    		parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\muestra_ecc\\GenericXMLproducts20250625093137.XML"), null);
//    		parser.processFile(java.nio.file.Paths.get("C:\\opt\\LVP\\desorden\\muestra_ecc\\GenericXMLproducts20250624173134.XML"), null);
//		} catch (ParserConfigurationException | SAXException | IOException e) {
//			e.printStackTrace();
//		}
    }
    
	public void runOnSftp(String[] args) {
		qp0.put("includeObjectsInProtocol", "false");
		if(args.length > 0) {
    		USE_CACHE = Boolean.parseBoolean(args[0]);
    	}else {
    		USE_CACHE = true;
    	}

        try(SshClient client = SshClient.setUpDefaultClient()){
        	java.nio.file.Files.createDirectories(LOCAL_PROCESSED_DIR);
	        client.start();
        	try {
                java.util.Properties processedState = new java.util.Properties();
                if (USE_CACHE && java.nio.file.Files.exists(STATE_FILE)) {
                    try (InputStream in = java.nio.file.Files.newInputStream(STATE_FILE)) {
                        processedState.load(in);
                    }
                }

                try (ClientSession session = client.connect(USER, HOST, PORT)
                        .verify(10, TimeUnit.SECONDS)
                        .getSession()) {

//	                    FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
//	                    keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
//	                    keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);

                    session.auth().verify(10, TimeUnit.SECONDS);

                    try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                        while (true) {
                            Iterable<DirEntry> entries = sftp.readDir(REMOTE_DIR);
                            for (DirEntry entry : entries) {
                                String name = entry.getFilename();
                                if (name.equals(".") || name.equals("..")) {
        							continue;
        						}

                                long remoteModified = entry.getAttributes().getModifyTime().toMillis();
                                String previousTimestamp = processedState.getProperty(name);

                                if (USE_CACHE && previousTimestamp != null && Long.parseLong(previousTimestamp) == remoteModified) {
                                    continue;
                                }

                                String filePath = REMOTE_DIR + "/" + name;
                                log("Processing: " + name);
                                try (InputStream input = sftp.read(filePath);
                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                    copyStream(input, out);

                                    // Save locally
                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
                                    java.nio.file.Files.write(localCopy, out.toByteArray());

                                    // Update state
                                    processedState.setProperty(name, String.valueOf(remoteModified));
                                    if (USE_CACHE) {
                                        try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
                                            processedState.store(stateOut, null);
                                        }
                                    }

                                    if(!name.startsWith("GenericXMLattributes")) {
                                		log("Skipping " + name);
                                    	continue;
                                    }
                                    try {
                                    	processFile(out);
		                            	sftp.remove(filePath);
                            		} catch (ParserConfigurationException | SAXException | IOException e) {
                            			e.printStackTrace();
                            		}
                                }catch(java.io.IOException e) { 
		                        	log("Problem reading file: " + filePath); 
		                        	logE(e);

		                            processedState.setProperty(name, String.valueOf(remoteModified));
			                        if (USE_CACHE) {
		                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
		                                    processedState.store(stateOut, null);
		                                }
		                            }
//		                            if(!running)
//		                            	break;
		                        }
                            }
                            Thread.sleep(10_000);
                            sendData();
                        }
                    }catch(InterruptedException e) {
                    	logE(e);
                    }
                }
        	}catch(IOException e) {
        		logE(e);
        	}finally {
        		client.stop();
        	}
        	try {
        		Thread.sleep(10000);
        	}catch(InterruptedException e) {
        		
        	}
        } catch (java.io.IOException e1) {
			e1.printStackTrace();
		}
	}

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
    
	public void processFile(java.io.ByteArrayOutputStream baos) throws ParserConfigurationException, SAXException, IOException  {
		
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.ResDatetime"));
		request.put("columns", columns);
		request.put("rows", rows);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
    	if(baos != null){
    		doc = builder.parse( new java.io.ByteArrayInputStream( baos.toByteArray()) );
    	}else {
    		doc = null;
    		log("No path, no byte array provided");
    		return;
    	}
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		java.util.LinkedList<Node> productNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Products").getFirst()).get("Product");
		Element el = null;
		org.json.JSONArray product2GCharacteristicRecords = new org.json.JSONArray();
		org.json.JSONArray articleCharacteristicRecords = new org.json.JSONArray();
		Element values = null;
		String sku = null;
		java.util.LinkedList<Node> attributeValueNodeList = null;
		int driver = 0;
		String attId = null;
		String value = null;
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> dataTypes = new java.util.TreeMap<>();
		java.util.Map<String, String> attributeValues = new java.util.TreeMap<>();
		java.util.LinkedList<String> product2GCharacteristics = new java.util.LinkedList<>();
		java.util.LinkedList<String> articleCharacteristics = new java.util.LinkedList<>();
		java.util.Map<String, String> mapEqs = new java.util.TreeMap<>(); 
		java.util.Set<String> atributosTalla = new java.util.TreeSet<>();
		java.util.Map<String, String> diccionario = null;
		String productId = null;
		String itemId = null;
		String[] info = null;
		String valorTU = null;
		String attEq = null;
		java.util.LinkedList<String> listaDeDiccionarios = new java.util.LinkedList<>();
		collectLookupCharacteristics(dataTypes, s4hFieldMapping, lkps);
		loadEqS4HANAAttributes(mapEqs);
		loadSizeAttributesMap(atributosTalla);
		log("Resolving over: " + atributosTalla);
		log("Also resolving over: " + mapEqs);
		mapEqs.keySet().forEach(a -> listaDeDiccionarios.addLast( a ));
		log("Using list of dictionaries: " + listaDeDiccionarios);
		loadDiccionarios(listaDeDiccionarios);
		collectCharacteristicsByEntity(product2GCharacteristics, articleCharacteristics);
		log("Collecting lookup values for TamanoUnico");
		
		tools.collectLookupValues("TamanoUnicoLOV", map, mapB, "LOOKUP");
		tools.collectLookupValues("C100LOV", map, mapB, "LOOKUP");
		tools.collectLookupValues("SB_COLORESLOV", map, mapB, "LOOKUP");
		log("Done collecting lookup values for TamanoUnico");
		if(productNodeList != null) {
			for(Node pn : productNodeList) {
				el = (Element)pn;
				values = (Element) xmm.byName(el, "Values");
				sku = xmm.byName(values, "Value").getTextContent();
				if(sku == null || "".equals(sku)) {
					log("No SKU provided: " + sku);
					continue;
				}else {
					log("<:::::>GOT sku: " + sku);
				}
				sku = sku.replaceAll("^0+", "");
				attributeValueNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(values).get("Attributes").getFirst() ).get("Value");
				valorTU = "SIN TAMAÑO";
				String color = null;
				for(Node avn : attributeValueNodeList) {
					driver++;
					if( driver == 2) {
						attId = avn.getTextContent();
						sb.append( sb.length() > 0 ? "," : "" );
						sb.append(attId);
					}else if( driver == 3 ) {
						value = avn.getTextContent();
						if(!"".equals(value)) {
							if("SB_COLORES".equals(attId)) {
								color = value;
							}
							attEq = mapEqs.get(attId);
							log("Value: " + value + ", Checking if contained: -->" + attId + " -> " + attEq + "<-- in " + atributosTalla);
							if(attEq != null && atributosTalla.contains(attEq)) {
								diccionario = this.diccionarios.get( attId + "LOV" );
								if(diccionario != null) {
									valorTU = diccionario.get(value);
									if(valorTU == null) {
										log("No value found un current attribute dictionary -->" + value + "<--");
										valorTU = "SIN TAMAÑO";
									}else {
										log("Present, value added -->" + valorTU + "<--");
									}
								}else {
									log("No dictionary for: " + attId + " -> " + attEq);
								}
							}else {
								log("Not a size att.");
							}
							attributeValues.put(attId, value);
						}
						driver = 0;
						attId = null;
						value = null;
					}
				}
				if(valorTU != null) {
					log("Adding value to TU: " + valorTU);
					addValue("TamanoUnico", resolveDataType("TamanoUnico", "LOOKUP", valorTU, map, mapB), articleCharacteristicRecords );
				}
				log("Collecting over: " + sb.toString());
//				log("Collected: " + dataTypes + " dataTypes and, " + s4hFieldMapping + " S4H field mappings...");
				sb.setLength(0);
				for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
					tools.collectLookupValues( lkps.get( entry.getKey() ), map, mapB, entry.getValue());
					try{
						if(product2GCharacteristics.contains(entry.getKey()) ) {
							addValue(entry.getKey(), resolveDataType(entry.getKey(), entry.getValue(), attributeValues.get(s4hFieldMapping.get(entry.getKey())), map, mapB), product2GCharacteristicRecords );
						}
						if(articleCharacteristics.contains(entry.getKey()) ) {
							addValue(entry.getKey(), resolveDataType(entry.getKey(), entry.getValue(), attributeValues.get(s4hFieldMapping.get(entry.getKey())), map, mapB), articleCharacteristicRecords );
						}
					}catch(IllegalArgumentException e) {
						logE(e);
					}
				}
				log("Done going over dataTypes...");
				info = checkProductBySKU(sku);
				if(info != null && !"".equals(sku)) {
					log("Found SKU in product");
					productId = info[0];
					sendWriteRequest("Product2G", productId, product2GCharacteristicRecords, null, null);
					if("00".equals(info[1])) {
						addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
						itemId = checkArticleBySKU(sku);
						if(itemId != null) {
							sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
						}
					}
				} else {
					itemId = checkArticleBySKU(sku);
					if(itemId != null) {
						log("Found SKU in article.");
						sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null);
					}else {
						log("********************* No product found for: " + sku);
					}
				}
				if(itemId != null) {
					if(color != null && !"".equals(color)) {
						java.util.Map<String, String> s1 = map.get("SB_COLORESLOV");
						String lblColor = s1.get(color);
						if(lblColor != null) {
							java.util.Map<String, String> c2 = mapB.get("C100LOV");
							String c100 = c2.get(lblColor);
							if(c100 != null) {
								addColor(itemId, c100);
							}
						}
					}
					if(valorTU != null && !"".equals(valorTU)) {
						addTalla(itemId, valorTU);
					}
				}
				valorTU = null;
//				try {
//					log( xmm.prettyPrint(rootElement) );
//				} catch (TransformerException e) {
//					e.printStackTrace();
//				}
//				log("HERE");
//				System.exit(0);
				sku = null;
				itemId = null;
				info = null;
				attributeValues.clear();
				product2GCharacteristicRecords = new org.json.JSONArray();
				articleCharacteristicRecords = new org.json.JSONArray();
				attributeValues.clear();
			}
		}
	}
	
	private void loadEqS4HANAAttributes(java.util.Map<String, String> map) {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("query",  "not CharacteristicIdentifier.AlternativeIdentifier(S4HANA) is empty");
		qp.put("fields", "CharacteristicIdentifier.AlternativeIdentifier(S4HANA),Characteristic.Identifier");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					map.put(values.getString(0),values.getString(1));
				}
				currentIndex += response.getInt("pageSize");
			}else {
				log("ERROR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
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
		java.util.List<org.json.JSONObject> rows =
				dastub.getLookupValueCodeNameExternalCodeRows(
						diccionario,
						10,
						null,
						true);
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
	
//	private java.util.Map<String, String> loadDiccionario(String diccionario) {
//		java.util.Map<String, String> atributos = new java.util.TreeMap<>();
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		int currentIndex = 0;
//		int totalSize = 0;
//		qp.put("query", "LookupValue.IsActive = true");
//		qp.put("lookup", diccionario);
//		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//			if(response != null && response.has("totalSize")) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					atributos.put(values.getString(0),values.getString(1));
//				}
//				currentIndex += response.getInt("pageSize");
//			}else {
//				log("ERROR: " + workshop.getRawResponse());
//			}
//		}while(currentIndex < totalSize);
//		currentIndex = 0;
//		return atributos;
//	}
	
	private void loadSizeAttributesMap(java.util.Set<String> atributos) {
		java.util.Map<String, String> relAttrib = dastub.getDictionaryCharacteristicAlternativeValueMap("RelAttribSTDATG");
		atributos.addAll(relAttrib.keySet());
		log("Loaded size characteristics from DB. Count: " + atributos.size());
	}
	
	private void collectCharacteristicsByEntity(java.util.LinkedList<String> product2G, java.util.LinkedList<String> article) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,Characteristic.Entities");
		qp.put("query", "not CharacteristicIdentifier.AlternativeIdentifier(S4HANA) is empty and Characteristic.IsActive = true");
		qp.put("pageSize", "1200");
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray entities = null;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					entities = values.getJSONArray(1);
					for(int j = 0; j<entities.length(); j++) {
						if("Product2G".equals(entities.getString(j))) {
							product2G.addLast(values.getString(0));
						}else if("Article".equals(entities.getString(j))) {
							article.addLast(values.getString(0));
						}
					}
				}
				currentIndex += response.getInt("pageSize");
			}else {
				log("ERR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
	}
	
	private void sendWriteRequest(String entity, String id, org.json.JSONArray characteristicRecords, String fotoTomadaLiverpool, String currentStatus) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("_characteristicRecords", characteristicRecords);
		org.json.JSONObject response = null;
		log("/object/" + entity + "/'" + id + "'@'MASTER'");
		response = workshop.makeRequest("PUT", "/object/" + entity + "/'" + id + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			log("\tWriting: " + characteristicRecords + "\nNot really an error from writing id: " + id + ": " + response);
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
	}
	
	private String checkArticleBySKU(String sku) {
		return dastub.getSkuSupplierAid(sku);
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("query",  "characteristic('SKU',-1) equals \"" + sku + "\"");
//		qp.put("fields", "Article.SupplierAID");
//		org.json.JSONObject response = null;
//		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
//		return (response != null && response.getJSONArray("rows").length() > 0) ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : null;
	}
	
	private String[] checkProductBySKU(String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query",  "characteristic('SKU',-1) equals \"" + sku + "\"");
		qp.put("fields", 
				"Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code");
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		return (response != null && response.getJSONArray("rows").length() > 0) ? new String[] { 
				response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)} : null;
	}
	
	private void collectLookupCharacteristics(java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> sbbMapping, java.util.Map<String, String> lkps){
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] erg = null;
			while((line = br.readLine()) != null) {
				erg = workshop.parseLine(line, "\"", ";", "\\");
				if(erg != null && erg.length == 6) {
					characteristicsInfo.put(erg[0], erg[1]);
					lkps.put(erg[0], erg[5]);
					sbbMapping.put(erg[0], erg[3]);
				}else {
					log("Pieza malformada: --->" + line + "<---");
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
//	private void collectLookupCharacteristics(String names, java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> s4hMapping){
//		String[] pieces = names.split(",");
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "Characteristic.Identifier,Characteristic.DataType,CharacteristicIdentifier.AlternativeIdentifier(S4HANA)");
//		qp.put("query", "CharacteristicIdentifier.AlternativeIdentifier(S4HANA) in (" + names + ")");
//		qp.put("pageSize", "1200");
//		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
//		log("Immediatly from response: " + workshop.getRawResponse());
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		if(response != null) {
//			rows = response.getJSONArray("rows");
//			for(int i=0; i<rows.length(); i++) {
//				values = rows.getJSONObject(i).getJSONArray("values");
//				if("".equals(values.getString(0))) {
//					log("Not found a characteristic for: " + pieces[i]);
//				}else {
//					characteristicsInfo.put(values.getString(0), values.getString(1));
//					s4hMapping.put(values.getString(0), values.getString(2));
//				}
//			}
//		}else {
//			log("ERR: " + workshop.getRawResponse());
//		}
//	}
	
//	private void collectLookupValues(String charId, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB, String dataType){
//		if("LOOKUP".equals(dataType)) {
//			java.util.Map<String, String> codeLabel = map.get(charId);
//			if(codeLabel == null) {
//				codeLabel = collectLookupValues(charId);
//				if(codeLabel != null) {
//					map.put(charId, codeLabel);
//				}
//			}
//			codeLabel = mapB.get(charId);
//			if(codeLabel == null) {
//				codeLabel = collectLookupValuesBackwards(charId);
//				if(codeLabel != null) {
//					mapB.put(charId, codeLabel);
//				}
//			}
//		}else if(dataType == null) {
//			log("Attribute not known: " + charId);
//		}
//	}
	
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
//		qp.put("lookup", lookup);
//		qp.put("pageSize", "1200");
//		int currentIndex = 0;
//		int totalSize = 0;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//			if(response != null && response.has("totalSize")) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					keyValues.put(values.getString(0), values.getString(1));
//				}
//				currentIndex += response.getInt("pageSize");
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
//		qp.put("lookup", lookup);
//		qp.put("pageSize", "1200");
//		int currentIndex = 0;
//		int totalSize = 0;
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//			if(response != null && response.has("totalSize")) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					keyValues.put(values.getString(0), values.getString(1));
//				}
//				currentIndex += response.getInt("pageSize");
//			}else {
//				log("Problem in collect lkp values back: " + workshop.getRawResponse());
//			}
//		}while(currentIndex < totalSize);
//		currentIndex = 0;
//		return keyValues;
//	}
	
	private Object resolveDataType(String charId, String dataType, String value, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB) {
		if(dataType != null && value != null) {
			if("LOOKUP".equals(dataType)) {
				String code = null;
				String label = null;
				if("UnidadDeMedidaPeso".equals(charId)) {
				}else if("UnidadDeMedidaLongitud".equals(charId)) {
				}else if("UnidadDeMedidaVolumen".equals(charId)) {
				}else {
					java.util.Map<String, String> lkp = map.get(charId);
					if(lkp != null) {
						label = lkp.get(value);
					}
					java.util.Map<String, String> lkpB = mapB.get(charId);
					if(lkpB != null) {
						code = lkpB.get(value);
					}
					if(code == null && label == null) {
						log("Unknown value found: " + value + " for Characteristic: " + charId);
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


	private static final Logger LOGGER = Logger.getLogger(ParseJanaAttributesFile.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/s4h/parseJana122ResponseAttributes-%g.log", 25 * 1024 * 1024, 10, true);
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
	
	
	private void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJana122ResponseAttributes.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJana122ResponseAttributes.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

	@Override
	public void close(){
		dastub.close();
	}
    
}
