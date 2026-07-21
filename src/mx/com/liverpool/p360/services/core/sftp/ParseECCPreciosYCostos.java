package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
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

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.DBAccessDataStub.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.SimpleLog;

public class ParseECCPreciosYCostos extends Thread implements SimpleLog {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	private static final String BASE_URL = workshop.getBaseUrl();
	
	private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final String USER = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.ecc.remote_directory_pyc" );//Remote directory to monitor: /interfase/mer/in/step/P360/zrtuab122
	private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.ecc.local_processed_dir_pyc" ));//Path: /u01/stage/ecc.122/processed
	
	private final DBAccessDataStub dastub = new DBAccessDataStub( new ELog() { 
			@Override 
			public void log(String message) { 
				ParseECCPreciosYCostos.this.log(message); 
			}
			
			@Override 
			public void logE(Exception e) { 
				ParseECCPreciosYCostos.this.logE(e); 
			} 
		}  
	);
	private final java.util.Map<String, String> qp = new java.util.HashMap<>();

	private final PubSubGCP postProductsPubSub = new PubSubGCP(
	        PropertiesManager.get("p360.contingency.gcp.service_account_back"),
	        PropertiesManager.get("p360.contingency.gcp.project_back"),
	        PropertiesManager.get("p360.contingency.gcp.post_products_topic")
	);
	
	@Override
	public void run() {
		closeResources();
		this.running = false;
	}

	private void closeResources() {
	    try {
	        postProductsPubSub.close();
	    } catch (Exception e) {
	        logE(e);
	    }
	}
	
	private void sendData() {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		if(requestCosto.getJSONArray("rows").length() > 0) { 
			log("sending block costo (product, " + requestCosto.getJSONArray("rows").length() + ")");
			rw.writeData("list", "Product2G", null, qp, requestCosto, this::log);
		}
		if(requestPrecio.getJSONArray("rows").length() > 0) { 
			log("sending block precio (product, " + requestPrecio.getJSONArray("rows").length() + ")");
			rw.writeData("list", "Product2G", null, qp, requestPrecio, this::log);
		}
		if(requestCostoA.getJSONArray("rows").length() > 0) { 
			log("sending block costo (article, " + requestCostoA.getJSONArray("rows").length() + ")");
			rw.writeData("list", "Article", null, qp, requestCostoA, this::log);
		}
		if(requestPrecioA.getJSONArray("rows").length() > 0) { 
			log("sending block precio (article, " + requestPrecioA.getJSONArray("rows").length() + ")");
			rw.writeData("list", "Article", null, qp, requestPrecioA, this::log);
		}
	}
	
	private final org.json.JSONObject requestCosto = new org.json.JSONObject()
				.put("columns", new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CostobrutoSinIVA',root,\"0000.0000.RK\",'CostobrutoSinIVA',-1)"))
					)
				.put("rows", new org.json.JSONArray())
			;
	private final org.json.JSONObject requestPrecio = new org.json.JSONObject()
			.put("columns", new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PrecioSugeridocIVA',root,\"0000.0000.RK\",'PrecioSugeridocIVA',-1)"))
					)
			.put("rows", new org.json.JSONArray())
			;
	private final org.json.JSONObject requestCostoA = new org.json.JSONObject()
			.put("columns", new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CostobrutoSinIVA',root,\"0000.0000.RK\",'CostobrutoSinIVA',-1)"))
					)
			.put("rows", new org.json.JSONArray())
			;
	private final org.json.JSONObject requestPrecioA = new org.json.JSONObject()
			.put("columns", new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PrecioSugeridocIVA',root,\"0000.0000.RK\",'PrecioSugeridocIVA',-1)"))
					)
			.put("rows", new org.json.JSONArray())
			;
	
	private boolean running = true;
	
	
    private void launchListenerThread() {
		Thread t = new Thread(()->{
			while(running) {
				try(
					java.net.ServerSocket server = new java.net.ServerSocket(23553);
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
    		ParseECCPreciosYCostos parse = new ParseECCPreciosYCostos();
    		try {
				parse.processFile(java.nio.file.Paths.get(args[1]), null, null, (byte) Integer.parseInt(args[1]));
			} catch (ServiceUnavailableException | ParserConfigurationException | SAXException | IOException e) {
				e.printStackTrace();
			}
    		parse.sendData();
    	}else {
	    	ParseECCPreciosYCostos object = new ParseECCPreciosYCostos();
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
    
    public void flushPendingWrites() {
        sendData();
    }
    
	public void runOnSftp() throws ParserConfigurationException, SAXException {
		workshop.setBaseUrl(BASE_URL);
		qp.put("includeObjectsInProtocol", "false");

		log("Starting...");
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
	                                if (!name.startsWith("STCTS") && !name.startsWith("STPRS")) continue;
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
	                                    if (name.startsWith("STCTS")) {
			                                try (InputStream input = sftp.read(filePath);
			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		
			                                    copyStream(input, out);
		
			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
				
					                            try {
							                        log("(Costo) Processing: " + name);
					                            	processFile(java.nio.file.Paths.get(filePath), out, sftp, (byte) 0);
					                    		} catch (ParserConfigurationException | SAXException | IOException e) {
					                    			logE(e);
					                    		}
//					                            sftp.remove(filePath);
		
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
	                                    if (name.startsWith("STPRS")) {
 			                                try (InputStream input = sftp.read(filePath);
 			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
 		
 			                                    copyStream(input, out);
 		
 			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
 			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
 			                                    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(out.toByteArray())) {
 							                        log("(Precio) Processing: " + name);
					                            	processFile(java.nio.file.Paths.get(filePath), out, sftp, (byte) 1);
 					                    		} catch (java.io.IOException e) {
 					                    			e.printStackTrace();
 					                    		}
// 					                            sftp.remove(filePath);
 		
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
		                        if(!running)
                                	break;
	                        } catch (Exception sftpBroken) {
	                            log("SFTP/session se rompió; reconecto en el siguiente ciclo.");
	                            logE(sftpBroken);
	                        }
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
	}

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

	public void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos, SftpClient sftp, byte isPrecios) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {
		long init = System.currentTimeMillis();
        if(baos != null) {
        	org.json.JSONArray productos = new org.json.JSONArray();
        	java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream( baos.toByteArray() );
        	try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(bais))){
        		String line = null;
        		String[] pieces = null;
        		String[] data = null;
        		org.json.JSONObject chosenOne = null;
        		while((line = br.readLine()) != null) {
        			pieces = line.split("\\|");
        			if(pieces.length == 2) {
        				data = dastub.getSkuData(pieces[0]);
        				if(data != null && data.length == 2 && !"".equals(data[0]) && !"".equals(data[1])) {
        					(chosenOne = (isPrecios == 1 ? ("1000".equals(data[1]) ? requestPrecioA : requestPrecio) : ("1000".equals(data[1]) ? requestCostoA : requestCosto))).getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + data[0] + "'@1")).put("values", new org.json.JSONArray().put(pieces[1])));
        					org.json.JSONObject msgBody = new org.json.JSONObject();
        					if("1000".equals(data[1])) {
        						String productId = dastub.getProductByVariant(data[0]);
        						if(productId != null && !"".equals(productId)) {
            						msgBody.put("proposalId", productId).put("variants", new org.json.JSONArray().put(new org.json.JSONObject().put("variantId", data[0]).put(isPrecios == 1 ? "PrecioSugeridocIVA" : "CostobrutoSinIVA", pieces[1])));
            						productos.put(msgBody);
            						log("Message body: " + msgBody);
            						System.out.println("Message body: " + new org.json.JSONObject().put("products", productos));
                					postProductsPubSub.publishMessage(
                					        new org.json.JSONObject().put("products", productos).toString()
                					);
        						}
        					}else {
        						msgBody.put("proposalId", data[0]).put( isPrecios == 1 ? "PrecioSugeridocIVA" : "CostobrutoSinIVA", pieces[1]);
	        					productos.put(msgBody);
	        					log("Message body: " + new org.json.JSONObject().put("products", productos).toString());
	        					System.out.println("Message body: " + msgBody);
            					postProductsPubSub.publishMessage( new org.json.JSONObject().put("products", productos).toString()
	        					);
        					}
        					if(chosenOne.getJSONArray("rows").length() == 1000) {
        						rw.writeData("list", "1000".equals(data[1]) ? "Article" : "Product2G", null, qp, chosenOne, this::log);
        					}
        				}else {
        					log("No known data for SKU: " + pieces[0] + ": " + (data != null ? java.util.Arrays.asList(data) : "Empty data"));
        				}
        			}else {
        				log("No valid line in file: " + line);
        			}
        		}
        	}catch(java.io.IOException e) {
        		logE(e);
        	}
        	bais.close();
        	baos.close();
        }
		log("Done processing file. [" + path.toString().replaceAll(".+" + java.util.regex.Pattern.quote( java.io.File.separator ), "") + "] " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	private static final Logger LOGGER = Logger.getLogger(ParseECCPreciosYCostos.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/ecc/parsePrecios_y_Costos-%g.log", 25 * 1024 * 1024, 10, true);
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
	}

	@Override
	public final void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs", "sftp", "ecc","parsePrecios_y_Costos.err").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
