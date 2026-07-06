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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;


public class ReportaConjuntoLookSAP {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	private static final XMLMisc xmm = workshop.getXmm();

	private boolean running = true;

    // SFTP connection parameters
	private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final String USER = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.ecc.remote_directory_122" );//Remote directory to monitor: /interfase/mer/in/step/P360/zrtuab122
	private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.ecc.local_processed_dir_122" ));//Path: /u01/stage/ecc.122/processed
	private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.ecc.state_file_122_cl" ));//File:processed_files_zrtuab122_CL.properties
	private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.ecc.use_cache" ));//false;

	private final java.util.Map<String, String> qp = new java.util.HashMap<>();
	
    private void launchListenerThread() {
		Thread t = new Thread(()->{
			while(running) {
				try(
					java.net.ServerSocket server = new java.net.ServerSocket(23552);
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
		
		ReportaConjuntoLookSAP object = new ReportaConjuntoLookSAP();
		object.launchListenerThread();

		if(args.length > 0) {
    		USE_CACHE = Boolean.parseBoolean(args[0]);
    	}else {
    		USE_CACHE = true;
    	}
		log("Starting");
		while(object.running) {
	        SshClient client = SshClient.setUpDefaultClient();
	        client.start();
	
	        try {
				java.nio.file.Files.createDirectories(LOCAL_PROCESSED_DIR);
			} catch (IOException e) {
				logE(e);
			}
	        log("Reading state");
	        // Load state
	        java.util.Properties processedState = new java.util.Properties();
	        if (USE_CACHE && java.nio.file.Files.exists(STATE_FILE)) {
	            try (InputStream in = java.nio.file.Files.newInputStream(STATE_FILE)) {
	                processedState.load(in);
	            } catch (IOException e) {
					logE(e);
				}
	        }
	        log("Openning client...");
	        try (ClientSession session = client.connect(USER, HOST, PORT)
	                .verify(10, TimeUnit.SECONDS)
	                .getSession()) {
	
	            FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
	            keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
	            keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
	
	            session.auth().verify(10, TimeUnit.SECONDS);
	
	            try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
	                while (object.running) {
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
	
	                        if(!name.matches("^(GenericXMLconjunto).+")) {
	                        	continue;
	                        }
	                        log("Processing: " + name);
	                        try (InputStream input = sftp.read(filePath);
	                        	     ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024)) {

	                        	    copyStream(input, out);

	                        	    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
	                        	    java.nio.file.Files.write(localCopy, out.toByteArray());

	                        	    processFile(java.nio.file.Paths.get(filePath), out);

	                        	    if (USE_CACHE) {
	                        	        markProcessed(processedState, name, remoteModified);
	                        	    }

	                        	    try {
	                        	        sftp.remove(filePath);
	                        	    } catch (IOException removeError) {
	                        	        log("File processed, but remote remove failed: " + filePath);
	                        	        logE(removeError);

	                        	        if (isClosedSftp(removeError)) {
	                        	            throw removeError;
	                        	        }
	                        	    }

	                        	} catch (ParserConfigurationException | SAXException e) {
	                        	    log("Problem parsing file, remote file was NOT removed: " + filePath);
	                        	    logE(e);

	                        	} catch (IOException e) {
	                        	    log("Problem reading/processing file, file was NOT marked as processed: " + filePath);
	                        	    logE(e);

	                        	    if (isClosedSftp(e)) {
	                        	        throw e;
	                        	    }

	                        	    if (!object.running) {
	                        	        break;
	                        	    }
	                        	}
	                    }
	                    Thread.sleep(10_000);
	                }
	            } catch (InterruptedException e) {
					logE(e);
				}
	        } catch (java.io.IOException e1) {
				logE(e1);
			} finally {
	            client.stop();
	        }
		}
	}
	
	private static void markProcessed(java.util.Properties processedState, String name, long remoteModified) throws IOException {
	    processedState.setProperty(name, String.valueOf(remoteModified));

	    try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
	        processedState.store(stateOut, null);
	    }
	}

	private static boolean isClosedSftp(Throwable t) {
	    while (t != null) {
	        String msg = String.valueOf(t.getMessage());

	        if (msg.contains("client is closed")
	                || msg.contains("Channel is closed")
	                || msg.contains("session is closed")
	                || msg.contains("Connection reset")
	                || msg.contains("Broken pipe")) {
	            return true;
	        }

	        t = t.getCause();
	    }

	    return false;
	}
	
	public void runOnSftp() throws ParserConfigurationException, SAXException {

		qp.put("includeObjectsInProtocol", "false");
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
	                                if (!name.startsWith("GenericXMLconjunto")) continue;
	                                entries.add(e);
	                            }
	                            java.util.Map<Long, java.util.List<DirEntry>> byTs = new java.util.TreeMap<>();
	                            for (DirEntry e : entries) {
	                                long ts = extractTsFromNameMillis(e.getFilename());
	                                if (ts < 0) ts = eventTimeMillis(e);
	                                byTs.computeIfAbsent(ts, k -> new java.util.ArrayList<>()).add(e);
	                            }

	                            for (var group : byTs.values()) {

	                                for (DirEntry ent : group) {
	                             	   String name = ent.getFilename();
	                            	   String filePath = REMOTE_DIR + "/" + name;
	                                    if (name.startsWith("GenericXMLconjunto")) {
			                                try (InputStream input = sftp.read(filePath);
			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		
			                                    copyStream(input, out);
		
			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
				
					                            try {
							                        log("(Products) Processing: " + name);
							                        if(laPrimeraBez) {
							                        	laPrimeraBez = false;
							                        }
					                            	processFile(java.nio.file.Paths.get(filePath), out);
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
	                            }
		                        laPrimeraBez = true;
		                        if(!running)
                                	break;
	                        } catch (Exception sftpBroken) {
	                            log("SFTP/session se rompió; reconecto en el siguiente ciclo.");
	                            logE(sftpBroken);
	                        }
//                            sendData();
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
	
	public void runOnSftp2() throws ParserConfigurationException, SAXException {
		qp.put("includeObjectsInProtocol", "false");
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
//        SAXParser parser = factory.newSAXParser();
        
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
	                                if (!name.startsWith("GenericXMLattributes") && !name.startsWith("GenericXMLconjunto")) continue;
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
	                                    if (name.startsWith("GenericXMLconjunto")) {
			                                try (InputStream input = sftp.read(filePath);
			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		
			                                    copyStream(input, out);
		
			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
				
					                            try {
							                        log("(Products) Processing: " + name);
							                        if(laPrimeraBez) {
//							                        	cargaLasCosas();
							                        	laPrimeraBez = false;
							                        }
					                            	processFile(java.nio.file.Paths.get(filePath), out);
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
	                            }
		                        laPrimeraBez = true;
		                        if(!running)
                                	break;
	                        } catch (Exception sftpBroken) {
	                            log("SFTP/session se rompió; reconecto en el siguiente ciclo.");
	                            logE(sftpBroken);
	                        }
//                            sendData();
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

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
    
	public static void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos) throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		org.xml.sax.InputSource is = new org.xml.sax.InputSource(new java.io.InputStreamReader( new java.io.ByteArrayInputStream( baos.toByteArray() ), java.nio.charset.StandardCharsets.UTF_8 ));
    	doc = builder.parse(is);
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();

		java.util.LinkedList<Node> products = xmm.listImmediateChildElements(rootElement).get("Product");
		java.util.LinkedList<Node> conjuntoLookList = null;

		String idConjunto = null;
		String fechaEnv = null;
		String descr = null;
		String ffn = null;
		String fin = null;

		String attributeId = null;
		String value = null;

		java.util.LinkedList<Node> listaAtributoCL = null;
		java.util.LinkedList<Node> miembrosCL = null;
		java.util.LinkedList<Node> atributosMiembroCL = null;

		Element el = null;

		org.json.JSONObject conjunto = null;
		org.json.JSONArray miembros = null;
		org.json.JSONObject miembro = null;

		PubSubGCP ps = new PubSubGCP();
		
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		
		if(products != null) {
			for(Node n : products) {
				conjuntoLookList = xmm.listImmediateChildElements(n).get("Values");
				for( Node cln : conjuntoLookList ) {
					listaAtributoCL = xmm.listImmediateChildElements(cln).get("Value");
					miembrosCL = xmm.listImmediateChildElements(cln).get("VALUE");
					conjunto = new org.json.JSONObject();
					miembros = new org.json.JSONArray();
					fin = null;
					ffn = null;
					descr = null;
					fechaEnv = null;
					idConjunto = null;
					for(Node acl : listaAtributoCL) {
						el = (Element) acl;
						attributeId = el.getAttribute("AttributeID");
						value = el.getTextContent();
						if("ZZFECHA_ENV".equals(attributeId)) {
							fechaEnv = value;
						}else if("ZZID_CONJUNTO".equals(attributeId)) {
							idConjunto = value;
						}else if("ZZFEC_INI".equals(attributeId)) {
							if(fin == null) {
								fin = value;
							} else {
								ffn = value;
							}
						}else if("ZZFEC_FIN".equals(attributeId)) {
							ffn = value;
						}else if("ZZDES_CONJUNTO".equals(attributeId)) {
							descr = value;
						}
					}
					log(idConjunto + " <::>" + descr);
					int[] currentStatus = new int[1];
					currentStatus[0] = 1026;
					if(idConjunto != null) {
						java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
						qp0.put("fields", "Product2G.CurrentStatus");
						qp0.put("items", "'" + idConjunto + "'@1");
						rw.collectData("list", "Product2G", null, "byItems", qp0, row -> {
							org.json.JSONArray values0 = row.getJSONArray("values");
							String cs = values0.getString(0);
							try{
								currentStatus[0] = "".equals(cs) ? -1 
										: Integer.parseInt(cs);
							}catch(NumberFormatException e) {
								currentStatus[0] = -1;
								logE(e);
							}
						} );
					}
					if(currentStatus[0] == 1026) {
						for(Node m : miembrosCL) {
							miembro = new org.json.JSONObject();
							atributosMiembroCL = xmm.listImmediateChildElements(m).get("Value");
							log("Processing VALUE tag");
							for(Node atm : atributosMiembroCL) {
								log("Processing value within VALUE. " + ((Element)atm).getAttribute("AttributeID") + " - " + atm.getTextContent());
								el = (Element)atm;
								attributeId = el.getAttribute("AttributeID");
								if("MATNR".equals(attributeId)) {
									miembro.put("sku", atm.getTextContent());
								}else if("ZZMAIN".equals(attributeId)) {
									miembro.put("itemPrincipal", Boolean.parseBoolean( atm.getTextContent() ));
								}else if("ZZSEQUENCE".equals(attributeId)) {
									miembro.put("sequence", atm.getTextContent());
								}else if("ZZSTATUS".equals(attributeId)) {
									miembro.put("status", atm.getTextContent());
								}
							}
							miembros.put(miembro);
						}
						conjunto.put("lookupGroupId", idConjunto);
						conjunto.put("approvedAt", fechaEnv);
						conjunto.put("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
						String end = ffn;
						try{
							end = getFarthest(fin, ffn);
						}catch(java.text.ParseException e) {
							e.printStackTrace();
						}
						conjunto.put("startAt", fin.equals(end) ? ffn : fin  );
						conjunto.put("endAt", end);
						conjunto.put("status", "En Proceso Foro");
						conjunto.put("variants", miembros);
						String[] details = checkProduct(idConjunto);
						if(details != null && !"".equals(details[0])) {
							rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id",  "'" + idConjunto + "'@'MASTER'")).put("values",
									new org.json.JSONArray().put(descr).put(idConjunto).put(fin).put(ffn).put(fechaEnv).put("CLK").put(currentStatus[0]).put("ConLookUnCatLevel1")));
							org.json.JSONObject resp = workshop.makeRequest("POST", "/list/Product2G", qp,
									new org.json.JSONObject()
									.put("columns",
											new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionShort(es)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('StartDate',root,\"0000.0000.RK\",'StartDate',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EndDate',root,\"0000.0000.RK\",'EndDate',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaEnvio',root,\"0000.0000.RK\",'FechaEnvio',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
											.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('ConjuntoLookRoot')"))
											)
									.put("rows", rows )
									.toString() );
							log( resp == null ? "ERR: " + workshop.getRawResponse() : String.valueOf(resp) );
						}else {
							rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id",  "'" + idConjunto + "'@'MASTER'")).put("values",
								new org.json.JSONArray().put(descr).put(idConjunto).put(fin).put(ffn).put(fechaEnv).put("CLK").put(currentStatus[0]).put("ConLookUnCatLevel1")));
							org.json.JSONObject resp = workshop.makeRequest("POST", "/list/Product2G", qp,
									new org.json.JSONObject()
									.put("columns",
											new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionShort(es)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('StartDate',root,\"0000.0000.RK\",'StartDate',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EndDate',root,\"0000.0000.RK\",'EndDate',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaEnvio',root,\"0000.0000.RK\",'FechaEnvio',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"))
											.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
											.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('ConjuntoLookRoot')"))
											)
									.put("rows", rows )
									.toString() );
							log( resp == null ? "ERR: " + workshop.getRawResponse() : String.valueOf(resp) );
						}
						handleObjectAPIUpdate(idConjunto, miembros, workshop.getBaseUrl());
						log(conjunto.toString());
						ps.publishMessage(
								  PropertiesManager.get("p360.contingency.gcp.project_back")
								, "idmc_post_look"
								, PropertiesManager.get("p360.contingency.gcp.service_account_back")
								, conjunto.toString());
					}
				}
			}
		}else {
			log("Malformed file contents...");
		}
	}
	
	private static void handleObjectAPIUpdate(String externalId, org.json.JSONArray members, String baseUrl ) {
//		RESTWorkshop rw = new RESTWorkshop();
//		if(baseUrl != null) {
//			rw.setBaseUrl(baseUrl);
//		}
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray records = new org.json.JSONArray();
		org.json.JSONObject record = null;
		for(int i=0; i<members.length(); i++) {
			record = new org.json.JSONObject();
			record.put("_qualification", new org.json.JSONObject()
					.put("recordKey", "0000." + paddNumber(i) + ".RK")
					.put("characteristic", new org.json.JSONObject().put("_code", "CLReference")));
			record.put("_children", 
									new org.json.JSONArray()
										.put(
											new org.json.JSONObject()
												.put("_qualification", 
														new org.json.JSONObject()
															.put("recordKey", "0000." + paddNumber(i) + ".RK")
															.put("characteristic", new org.json.JSONObject().put("_code", "CLReference_SKU")))
												.put("_recordLang", new org.json.JSONArray().put(
														new org.json.JSONObject()
															.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))) .put("values", new org.json.JSONArray().put( members.getJSONObject(i).getString("sku") ))
														)
													)
											)
										.put(
												new org.json.JSONObject()
												.put("_qualification", 
														new org.json.JSONObject()
															.put("recordKey", "0000." + paddNumber(i) + ".RK")
															.put("characteristic", new org.json.JSONObject().put("_code", "CLReference_IsMain")))
												.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))).put("values", new org.json.JSONArray().put( members.getJSONObject(i).getBoolean("itemPrincipal") ))))
												)
										.put(
												new org.json.JSONObject()
												.put("_qualification", 
														new org.json.JSONObject()
															.put("recordKey", "0000." + paddNumber(i) + ".RK")
															.put("characteristic", new org.json.JSONObject().put("_code", "CLReference_Sequence")))
												.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))).put("values", new org.json.JSONArray().put( Integer.parseInt( members.getJSONObject(i).getString("sequence") ) ))))
												)
										.put(
												new org.json.JSONObject()
												.put("_qualification", 
														new org.json.JSONObject()
															.put("recordKey", "0000." + paddNumber(i) + ".RK")
															.put("characteristic", new org.json.JSONObject().put("_code", "CLReference_Status")))
												.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))	.put("values", new org.json.JSONArray().put( members.getJSONObject(i).getString("status") ))))
												)
					  );
			records.put(record);
		}
		request.put("_characteristicRecords", records);
		org.json.JSONObject rsp = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + externalId + "'@1", new java.util.TreeMap<>(), request.toString());
		log("Sent: " + request);
		log("Got: " + rsp);
		log("Reponse from trying to update a product (" + externalId + "): " + rw.getRw().getRawResponse());
	}
	
	private static String paddNumber(int a) {
		StringBuilder sb = new StringBuilder();
		String sa = String.valueOf(a);
		for(int i=0; i<(4-sa.length()); i++) {
			sb.append("0");
		}
		sb.append(sa);
		return sb.toString();
	}
	
	private static String[] checkProduct(String id) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"Product2G.CurrentStatus");
		qp.put("query", "Product2G.ProductNo equals \"" + id + "\" and not Product2G.CurrentStatus is empty");
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		org.json.JSONArray rows = null;
		if(response != null && response.has("rows")) {
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				return new String[] { rows.getJSONObject(0).getJSONArray("values").getString(0) };
			}
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}

	private static String getFarthest(String d1, String d2) throws java.text.ParseException {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		java.util.Date date1 = sdf.parse(d1);
		java.util.Date date2 = sdf.parse(d2);
		return date1.compareTo(date2) > 0 ? d1 : d2;

	}

	private static final Logger LOGGER = Logger.getLogger(ReportaConjuntoLookSAP.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/ecc/conjuntoLook-%s.log", 25 * 1024 * 1024, 10, true);
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

	private static void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","conjuntoLook.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	private static void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","conjuntoLook.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

}
