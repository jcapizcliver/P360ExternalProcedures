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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleLog;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class ParseECCErrResponse extends Thread implements Closeable {

	private final RESTWrapper rw = new RESTWrapper();
	
	private boolean running = true;
	

	private DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
		
		@Override
		public void logE(Exception e) {
			ParseECCErrResponse.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			ParseECCErrResponse.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);

	@Override
	public void close() {
		dastub.close();
	}

	private final ParsersTools tools = new ParsersTools(new SimpleLog() {

		public void log(String message) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
					new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECCErrResponse.log").toString(), true)))) {
				pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
						+ "]  " + message);
			} catch (java.io.IOException e) {
			}
		}

		public void logE(Exception ex) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
					new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECCErrResponse.log").toString(), true)))) {
				ex.printStackTrace(pw);
			} catch (java.io.IOException e) {
			}
		}
	}, dr);
	
    // SFTP connection parameters
	private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final String USER = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.ecc.remote_directory_error" );//Remote directory to monitor: /interfase/mer/in/step/P360/prop_error
	private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.ecc.local_processed_dir_error" ));//Path: /u01/stage/ecc.ERR/processed
	private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.ecc.state_file_error" ));//File: processed_ecc.122_ERR.propertiess
	private static boolean USE_CACHE = Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.ecc.use_cache" ));//false;

    private void launchListenerThread() {
		Thread t = new Thread(()->{
			while(running) {
				try(
					java.net.ServerSocket server = new java.net.ServerSocket(23547);
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
    
    @Override
    public void run() {
    	sendData();
    	running = false;
    }
    
	public static void main(String[] args) {
		try(ParseECCErrResponse rn = new ParseECCErrResponse()){
			rn.launchListenerThread();
			Runtime.getRuntime().addShutdownHook(rn);
			while(rn.running) {
				rn.doIt();
				try {
					Thread.sleep(10000);
				}catch(InterruptedException e) {
					rn.logE(e);
				}
			}
		}
	}
	
	private void doIt() {

//        try(SshClient client = SshClient.setUpDefaultClient()){
//        	client.start();
//        	try{

		        try {
					java.nio.file.Files.createDirectories(LOCAL_PROCESSED_DIR);
				} catch (IOException e) {
					e.printStackTrace();
				}
		        java.util.Properties processedState = new java.util.Properties();
//		        if (USE_CACHE && java.nio.file.Files.exists(STATE_FILE)) {
//		            try (InputStream in = java.nio.file.Files.newInputStream(STATE_FILE)) {
//		                processedState.load(in);
//		            }
//		        }
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
				                        if(0l == entry.getAttributes().getSize()) {
				                        	sftp.remove(filePath);
				                        	log("Deleted as it was empty.");
				                        }else {
					                        if(name.toUpperCase().endsWith(".XML")) {
						                        log("Processing: " + name);
						                        try (InputStream input = sftp.read(filePath);
						                             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
						                            copyStream(input, out);
						                            log("Copied.");
						                            // Save locally
						                            Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
						                            java.nio.file.Files.write(localCopy, out.toByteArray());
						                            log("Written to local");
						                            // Update state
						                            processedState.setProperty(name, String.valueOf(remoteModified));
						                            if (USE_CACHE) {
						                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
						                                    processedState.store(stateOut, null);
						                                    log("Kept conf.");
						                                }
						                            }
						
						                            try {
						                    			processFile(localCopy, out);
						                            	sftp.remove(filePath);
						                    		} catch (NullPointerException | ParserConfigurationException | SAXException | IOException e) {
						                    			logE(e);
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
						                            if(!running)
						                            	break;
						                        }
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
		        
//		        try (ClientSession session = client.connect(USER, HOST, PORT)
//		                .verify(10, TimeUnit.SECONDS)
//		                .getSession()) {
//		
//		            FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
//		            keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
//		            keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
//		
//		            session.auth().verify(10, TimeUnit.SECONDS);
//		
//		            try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
//		                while (running) {
//		                    Iterable<DirEntry> entries = sftp.readDir(REMOTE_DIR);
//		                    for (DirEntry entry : entries) {
//		                        String name = entry.getFilename();
//		                        if (name.equals(".") || name.equals("..")) {
//									continue;
//								}
//		
//		                        long remoteModified = entry.getAttributes().getModifyTime().toMillis();
//		                        String previousTimestamp = processedState.getProperty(name);
//		
//		                        if (USE_CACHE && previousTimestamp != null && Long.parseLong(previousTimestamp) == remoteModified) {
//		                            continue;
//		                        }
//		
//		                        String filePath = REMOTE_DIR + "/" + name;
//		                        log("Processing: " + name);
//		                        try (InputStream input = sftp.read(filePath);
//		                             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//		                            copyStream(input, out);
//		                            log("Copied.");
//		                            // Save locally
//		                            Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
//		                            java.nio.file.Files.write(localCopy, out.toByteArray());
//		                            log("Written to local");
//		                            // Update state
//		                            processedState.setProperty(name, String.valueOf(remoteModified));
//		                            if (USE_CACHE) {
//		                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
//		                                    processedState.store(stateOut, null);
//		                                    log("Kept conf.");
//		                                }
//		                            }
//		
//		                            try {
//		                    			processFile(localCopy, out);
//		                            	sftp.remove(filePath);
//		                    		} catch (NullPointerException | ParserConfigurationException | SAXException | IOException e) {
//		                    			logE(e);
//		                    		}
//		                        }catch(java.io.IOException e) { 
//		                        	log("Problem reading file: " + filePath); 
//		                        	logE(e);
//
//		                            processedState.setProperty(name, String.valueOf(remoteModified));
//			                        if (USE_CACHE) {
//		                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
//		                                    processedState.store(stateOut, null);
//		                                }
//		                            }
//		                            if(!running)
//		                            	break;
//		                        }
//		                    }
//		                    log("Sleeping");
//		                    Thread.sleep(10_000);
//		                }
//		            }
//		        }
//        	}catch(IOException | InterruptedException e) {
//        		logE(e);
//        	}
//        	try {
//        		Thread.sleep(10000);
//        	}catch(InterruptedException e) {
//        		log("ERR: Got interrupted");
//        	}
//        }catch(java.io.IOException e) {
//        	logE(e);
//        }
	}

    private void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

	public void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos) throws ParserConfigurationException, SAXException, IOException {
		log("Now processing...");
		MyHandler handler = new MyHandler();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        if(baos != null) {
        	log("Processing with bytes");
        	parser.parse(new java.io.ByteArrayInputStream( baos.toByteArray() ), handler);
        }else {
        	log("Processing with path");
        	parser.parse(path.toFile(), handler);
        }
        java.util.LinkedList<MyHandler.Product> complete = handler.getComplete();
        java.util.LinkedList<MyHandler.MultiValue> values = null;
		StringBuilder sb = new StringBuilder();
		String proposalId = null;
		log("Got complete: " + (complete != null ? complete.size() : "NaN"));
		if(complete != null) {
			for(MyHandler.Product n : complete) {
				if( n.getId() != null ) {
					proposalId = n.getId();
					if(proposalId != null && !"".equals(proposalId) && proposalId.startsWith("1754611")) {
						log("Working on: " + proposalId);
						values = n.getMultiValues();
						if(values != null) {
							log("Going on " + values.size());
							for(MyHandler.MultiValue vn : values) {
								sb.append(sb.length() == 0 ? "" : "<:::>").append(vn.getValue());
							}
							sendError(proposalId, sb.toString());
							sb.setLength(0);
						}
					}
				}else {
					log("No ID found for this: " + baos.toString(java.nio.charset.StandardCharsets.UTF_8));
				}
			}
		}else {
			log("Malformed file content...");
		}
	}
	
	private final org.json.JSONArray columnsProduct = new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
			.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MensajeCreacionSKU',root,\"0000.0000.RK\",'MensajeCreacionSKU',-1)"))
		;
	private final org.json.JSONArray rowsProduct = new org.json.JSONArray();
	private final org.json.JSONObject requestProduct = new org.json.JSONObject().put("columns", columnsProduct).put("rows", rowsProduct);
	private final org.json.JSONArray columnsArticle = new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))
			.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MensajeCreacionSKU',root,\"0000.0000.RK\",'MensajeCreacionSKU',-1)"))
			;
	private final org.json.JSONArray rowsArticle = new org.json.JSONArray();
	private final org.json.JSONObject requestArticle = new org.json.JSONObject().put("columns", columnsArticle).put("rows", rowsArticle);
	
	private void sendError(String id, String message) {
		String currentStatus = null;
		String entity = null;
		String[] info = tools.checkProduct(id);
		if(info == null) {
			info = tools.checkArticle(id);
			if(info == null) {
				log("Identifier not found nor in Product nor in Item: " + id);
			} else {
				entity = "Article";
				if("1020".equals(info[4]) || "".equals(info[4])) {
//					data.put("currentStatus", new org.json.JSONObject().put("_key", 1021));
					currentStatus = "1021";
					log("Gonna change status for id (Article): " + id);
					rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(currentStatus).put(message)));
					log("Added (A, " + rowsArticle.length() + ")");
					if(rowsArticle.length() == 300) {
						java.util.Map<String, String> qp = new java.util.HashMap<>();
						qp.put("includeObjectsInProtocol", "false");
						rw.writeData("list", "Article", null, qp, requestArticle, this::log);
					}
					log("Gonna change status for id (Product): " + id + " - " + message);
					rowsProduct.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + info[0] + "'@1")).put("values", new org.json.JSONArray().put(currentStatus).put(message)));
					log("Added (P, " + rowsProduct.length() + ")");
					if(rowsProduct.length() == 300) {
						java.util.Map<String, String> qp = new java.util.HashMap<>();
						qp.put("includeObjectsInProtocol", "false");
						rw.writeData("list", "Product2G", null, qp, requestProduct, this::log);
					}
				}else {
					log("No applicable: " + id + " - " + java.util.Arrays.asList(info));
				}
//				sendWriteRequest("Article", id, data);
//				sendWriteRequest("Product2G", info[0], data);
			}
		}else {
			entity = "Product2G";
			if("1020".equals(info[3]) || "".equals(info[3])) {
				currentStatus = "1021";
//				data.put("currentStatus", new org.json.JSONObject().put("_key", 1021));
				log("Gonna change status for id (Product): " + id + " - " + message);
				rowsProduct.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(currentStatus).put(message)));
				log("Added (P, " + rowsProduct.length() + ")");
				if(rowsProduct.length() == 300) {
					java.util.Map<String, String> qp = new java.util.HashMap<>();
					qp.put("includeObjectsInProtocol", "false");
					rw.writeData("list", "Product2G", null, qp, requestProduct, this::log);
				}
			}else {
				log("No aplicable for product: " + id + ", " + java.util.Arrays.asList(info));
			}
//			sendWriteRequest("Product2G", id, data);
		}
//		if(entity != null && currentStatus != null) {
			if("Product2G".equals(entity)) {
			}else {
			}
//		}
	}
	
	private void sendData() {
		log("Sending data...");
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		if(rowsProduct.length() > 0) {
			rw.writeData("list", "Product2G", null, qp, requestProduct, this::log);
		}else {
			log("Nothing for product.");
		}
		if(rowsArticle.length() > 0) {
			rw.writeData("list", "Article", null, qp, requestArticle, this::log);
		}else {
			log("Nothing for article.");
		}
	}
	
//	private void sendWriteRequest(String entity, String id, org.json.JSONObject data) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		org.json.JSONObject response = null;
//		log("/object/" + entity + "/'" + id + "'@'MASTER'");
//		response = workshop.makeRequest("PUT", "/object/" + entity + "/'" + id + "'@'MASTER'", qp, data.toString());
//		if(response != null) {
//			log("\tWriting: " + data + "\nNot really an error from writing id: " + id + ": " + response);
//		}else {
//			log("ERR: " + workshop.getRawResponse());
//		}
//	}
	
//	private void addValue(String name, Object value, org.json.JSONArray values) {
//		if(value == null)
//			return;
//		values.put( new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", name))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put( value )))) );
//	}
	
//	private String[] checkProduct(String id) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields",
//				"Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
//				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
//				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool')->LookupValue.Code"
//				+ ",Product2G.CurrentStatus");
//		qp.put("query", "Product2G.ProductNo equals \"" + id + "\"");
//		org.json.JSONObject response = null;
//		response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
//		org.json.JSONArray rows = null;
//		if(response != null) {
//			if(!response.has("rows")) {
//				System.out.println("ERROR, json response: " + response + "\n\tRaw message: " + workshop.getRawResponse());
//			}else {
//				rows = response.getJSONArray("rows");
//				if(rows.length() > 0) {
//					return new String[] { rows.getJSONObject(0).getJSONArray("values").getJSONArray(0).getString(0), rows.getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0), rows.getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0), rows.getJSONObject(0).getJSONArray("values").getString(3) };
//				}
//			}
//		}else {
//			log("ERROR: " + workshop.getRawResponse());
//		}
//		return null;
//	}
	
//	private String[] checkArticle(String id) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", 
//				"ProductReference.ReferencedSupplierAid"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool')->LookupValue.Code"
//				+ ",ProductReference.ReferencedArticle->Product2G.CurrentStatus"
//				);
//		qp.put("query", "Article.SupplierAID equals \"" + id + "\"");
//		org.json.JSONObject response = null;
//		response = workshop.makeRequest("GET", "/list/Article/ProductReference/bySearch", qp, null);
//		if(response != null && response.has("rows") && response.getJSONArray("rows").length() > 0) {
//			return new String[] { response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
//					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
//					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)
//					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3).getString(0)
//					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(4) };
//		}else {
//			log("ERROR: " + workshop.getRawResponse());
//			if(!response.has("rows")) {
//				log("ERROR 2: response also did not contain error tag, rawResponse is -->" + workshop.getRawResponse() + "<--" );
//			}
//		}
//		return null;
//	}

	private static final Logger LOGGER = Logger.getLogger(ParseECCErrResponse.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/ecc/parseECCErrResponse-%g.log", 25 * 1024 * 1024, 10, true);
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
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECCErrResponse.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECCErrResponse.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
	private class MyHandler extends org.xml.sax.helpers.DefaultHandler {
		
		private class MultiValue{
			
			private String attributeId;
			private String value;
			
			public MultiValue(String attributeId) {
				this.attributeId = attributeId;
			}
			
			public String getAttributeId() {
				return attributeId;
			}
			
			public String getValue() {
				return value;
			}
			
			public void setValue(String value) {
				this.value = value;
			}
			
		}
		
		private class Product {
				
			private String id;
			private java.util.LinkedList<MultiValue> multiValues = new java.util.LinkedList<>();
			private MultiValue currentMultiValue = null;
			
			public Product(String id) {
				this.id = id;
			}
			
			public String getId() {
				return id;
			}
			
			public java.util.LinkedList<MultiValue> getMultiValues(){
				return multiValues;
			}
			
			public MultiValue getCurrentMultiValue() {
				return currentMultiValue;
			}
			
			public void setCurrentMultiValue(MultiValue currentMultiValue) {
				this.currentMultiValue = currentMultiValue;
			}
			
			public void addMultiValue() {
				if(currentMultiValue != null) {
					multiValues.addLast(currentMultiValue);
					currentMultiValue = null;
				}
			}
			
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			String name = localName != null && !localName.isEmpty() ? localName : qName;
			if("Product".equals(name)) {
				String id = attributes.getValue("ID");
				Product p = new Product(id);
				stack.addLast(p);
			}else if("Multivalue".equals(name)) {
				if(!stack.isEmpty()) {
					Product p = stack.getLast();
					MultiValue mv = new MultiValue(attributes.getValue("AttributeID"));
					p.setCurrentMultiValue(mv);
				}
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if(!stack.isEmpty()) {
				Product p = stack.getLast();
				MultiValue mv = p.getCurrentMultiValue();
				if(mv != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(mv.getValue() == null ? "" : mv.getValue());
					sb.append(ch, start, length);
					mv.setValue(sb.toString());
				}
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String name = localName != null && !localName.isEmpty() ? localName : qName;
			if("Product".equals(name)) {
				if(!stack.isEmpty()) {
					complete.addLast(stack.removeLast());
				}
			}else if("Multivalue".equals(name)) {
				if(!stack.isEmpty()) {
					Product p = stack.getLast();
					p.addMultiValue();
				}
			}
		}
		
		private final java.util.LinkedList<Product> stack = new java.util.LinkedList<>();
		private final java.util.LinkedList<Product> complete = new java.util.LinkedList<>();
		
		public java.util.LinkedList<Product> getComplete(){
			return complete;
		}
		
	}

}
