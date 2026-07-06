package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ParseECCResponse {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();
	private static final String BASE_URL = PropertiesManager.get("p360.contingency.base_url");

	static {
		workshop.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		workshop.setBaseUrl(BASE_URL);
	}

    // SFTP connection parameters
	private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final String USER = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.ecc.remote_directory_error" );//Remote directory to monitor: /interfase/mer/in/step/P360/prop_error
	private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.ecc.local_processed_dir_error" ));//Path: /u01/stage/ecc.ERR/processed
	private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.ecc.state_file_error" ));//File: processed_ecc.122_ERR.propertiess
	private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.ecc.use_cache" ));//false;

    

	public static void main(String[] args) throws IOException, InterruptedException, ParseException {

		workshop.setBaseUrl(BASE_URL);
		if(args.length > 0) {
    		USE_CACHE = Boolean.parseBoolean(args[0]);
    	}else {
    		USE_CACHE = true;
    	}

		while(true) {
	        SshClient client = SshClient.setUpDefaultClient();
	        client.start();
	
	        // Ensure local processed dir exists
	        java.nio.file.Files.createDirectories(LOCAL_PROCESSED_DIR);
	
	        // Load state
	        java.util.Properties processedState = new java.util.Properties();
	        if (USE_CACHE && java.nio.file.Files.exists(STATE_FILE)) {
	            try (InputStream in = java.nio.file.Files.newInputStream(STATE_FILE)) {
	                processedState.load(in);
	            }
	        }
	
	        try (ClientSession session = client.connect(USER, HOST, PORT)
	                .verify(10, TimeUnit.SECONDS)
	                .getSession()) {
	
	            FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
	            keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
	            keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
	
	            session.auth().verify(10, TimeUnit.SECONDS);
	
	            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(2025.+)");
	            java.util.regex.Matcher m = null;
	
	            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
	            long base = sdf.parse("20250501000000").getTime();
	            long ft = 0l;
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
	                        System.out.println("Processing: " + name);
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
	
	                            m = p.matcher(name);
	                            if(m.find()) {
	                            	ft = sdf.parse(m.group(1)).getTime();
	                            	if(base > ft) {
	                            		System.out.println("Skipping " + name);
	                            		continue;
	                            	}
	                            }else {
	                            	System.out.println("Not found pattern in: " + name);
	                            	continue;
	                            }
	                            try {
	                    			processFile(out);
	                            	sftp.remove(filePath);
	                    		} catch (ParserConfigurationException | SAXException | IOException e) {
	                    			e.printStackTrace();
	                    		}
	                        }
	                    }
	                    Thread.sleep(10_000);
	                }
	            }
	        } finally {
	            client.stop();
	        }
		}
	}

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

	public static void processFile(java.io.ByteArrayOutputStream baos) throws ParserConfigurationException, SAXException, IOException {
		org.json.JSONObject response = new org.json.JSONObject();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.ResDatetime"));
		request.put("columns", columns);
		request.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(new java.io.ByteArrayInputStream( baos.toByteArray() ));
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();

		java.util.LinkedList<Node> productNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("Products").getFirst()).get("Product");
		java.util.LinkedList<Node> valuesNodeList = null;
		java.util.LinkedList<Node> multiValueNodeList = null;
		java.util.LinkedList<Node> multiValueValueNodeList = null;

		String proposalId = null;
		int counter = 0;
		String creationDate = null;
		for(Node n : productNodeList) {
			if( ((Element)n).hasAttribute("ID") ) {
				proposalId = ((Element)n).getAttribute("ID");
				if(proposalId != null && !"".equals(proposalId)) {
					System.out.println("Working on: " + proposalId);
					deleteFirst(proposalId);
					creationDate = getCreationDate(proposalId);
					System.out.println("CreationDate: " + creationDate);
					valuesNodeList = xmm.listImmediateChildElements(n).get("Values");
					if(valuesNodeList != null) {
						for(Node vn : valuesNodeList) {
							multiValueNodeList = xmm.listImmediateChildElements(vn).get("Multivalue");
							if(multiValueNodeList != null) {
								for(Node mvn : multiValueNodeList) {
									multiValueValueNodeList = xmm.listImmediateChildElements(mvn).get("Value");
									if(multiValueValueNodeList != null) {
										for(Node mvvn : multiValueValueNodeList) {
											counter++;
											rows.put(new org.json.JSONObject()
													.put("values", new org.json.JSONArray().put(mvvn.getTextContent()).put(creationDate))
													.put("object", new org.json.JSONObject().put("id", "'" + proposalId + "_" + counter + "'@'ErroresSKU'")));
											if(rows.length() == 100) {
												response = workshop.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
												if(response == null) {
													System.out.println("ERR: " + workshop.getRawResponse());
												}else{
													System.out.println(response.getJSONObject("counters"));
												}
												while(rows.length() > 0) {
													rows.remove(0);
												}
											}
										}
									}
								}
							}
						}
					}
					counter = 0;
				}
			}else {
				System.out.println("No ID found for this: " + baos.toString(java.nio.charset.StandardCharsets.UTF_8));
			}
		}
		if(rows.length() > 0 ) {
			response = workshop.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
			if(response == null) {
				System.out.println("ERR: " + workshop.getRawResponse());
			}else{
				System.out.println(response.getJSONObject("counters"));
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private static String getCreationDate(String proposalId) {
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Authorization", ParseECCResponse.workshop.getRc().getHeader().get("Authorization"));
		rw.setBaseUrl(BASE_URL);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2GLog.CreationDate(PIM)");
		qp.put("query", "Product2G.ProductNo equals \"" + proposalId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		if(response == null) {
			System.out.println(rw.getRawResponse());
		}else {
			return response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : null;
		}
		return null;
	}
	
	private static void deleteFirst(String proposalId) {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(BASE_URL);
		rw.addHeader("Authorization", ParseECCResponse.workshop.getRc().getHeader().get("Authorization"));
		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ErroresSKU'");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ErroresSKU\" and StandardizationValue.Value wildcard \"" + proposalId + "%\"");
		org.json.JSONObject response = null;
		System.out.println("Deleting: -->" + proposalId + "<--");
		response = rw.makeRequest("DELETE", "/list/StandardizationValue/bySearch", qp, null);
		if(response == null) {
			System.out.println(rw.getRawResponse());
		}else {
			System.out.println("From deleting " + proposalId + ": " + response);
		}
	}

}
