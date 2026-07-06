package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class Mikazukinoyaiba {

    // SFTP connection parameters
    private static final String HOST = "172.16.204.243"; // SFTP server address
    private static final int PORT = 22; // SFTP server port
    private static final String USER = "userp360"; // SFTP username
    private static final Path PRIVATE_KEY_PATH = Paths.get("/home/P360admin/.ssh/id_rsa"); // Path to private key
    private static final String REMOTE_DIR = "../../interfase/mer/in/step/P360/zrtuab122"; // Remote directory to monitor
    private static final Path LOCAL_PROCESSED_DIR = Paths.get("processed");

    private static final Path STATE_FILE = Paths.get("processed_files.properties");
    private static final Path SEQUENCE_FILE = Paths.get("upload_sequence.properties");

    private static boolean USE_CACHE = false;

    private static RESTWorkshop workshop = new RESTWorkshop();
    private static XMLMisc xmm = workshop.getXmm();

    private static java.util.Map<String, String> qp = new java.util.TreeMap<>();

	private static final java.util.Map<String, String> relacionIdentificadores = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> lookups = new java.util.TreeMap<>();
	private static final java.util.LinkedList<String> columnAlignmentForListPOST = new java.util.LinkedList<>();
	private static final org.json.JSONArray columnsProduct = new org.json.JSONArray();
	private static final org.json.JSONArray columnsArticle = new org.json.JSONArray();

	public static void main(String[] args) throws Exception {

    	if(args.length > 0) {
    		USE_CACHE = Boolean.parseBoolean(args[0]);
    	}else {
    		USE_CACHE = true;
    	}

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

        collectMappings();

        try (ClientSession session = client.connect(USER, HOST, PORT)
                .verify(10, TimeUnit.SECONDS)
                .getSession()) {

            FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
            keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
            keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);

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
                            continue; // already processed and unmodified
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
                            uploadDataFrom122(filePath, out);
                            if(!USE_CACHE) {
								break;
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

    private static void collectMappings() {

    	org.json.JSONObject response = null;
    	org.json.JSONArray rows = null;
    	org.json.JSONArray values = null;

    	int currentIndex = 0;
    	int totalSize = 0;

    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
    	qp.put("query", "not CharacteristicIdentifier.AlternativeIdentifier(ECC) is empty");
    	qp.put("fields", "Characteristic.Identifier,CharacteristicIdentifier.AlternativeIdentifier(ECC),Characteristic.DataType,Characteristic.Lookup->Lookup.Identifier");
    	qp.put("pageSize", "900");

    	do{
    		qp.put("startIndex", String.valueOf(currentIndex));
    		response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
    		totalSize = response.getInt("totalSize");
    		rows = response.getJSONArray("rows");
    		for(int i=0; i<rows.length(); i++) {
    			currentIndex++;
    			values = rows.getJSONObject(i).getJSONArray("values");
    			if(!"".equals(values.getString(1))) {
    				columnAlignmentForListPOST.addLast(values.getString(1));
    				columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('" + values.getString(0) + "',root,\"0000.0000.RK\",'" + values.getString(0) + "',-1)"));
    				columnsArticle.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('" + values.getString(0) + "',root,\"0000.0000.RK\",'" + values.getString(0) + "',-1)"));
	    			relacionIdentificadores.put(values.getString(1), values.getString(0));
	    			if("LOOKUP".equals(values.getString(2))) {
	    				lookups.put(values.getString(1), values.getString(3));
	    			}
    			}
    		}
    	}while(currentIndex < totalSize);
    	currentIndex = 0;

    	System.out.println("Collected.");

    }

    private static void uploadDataFrom122(String fileName, java.io.ByteArrayOutputStream baos) {
    	try {

    		org.json.JSONArray globalRows = new org.json.JSONArray();

    		org.json.JSONArray rows = new org.json.JSONArray();
    		org.json.JSONObject response = null;

    		org.json.JSONArray characteristicRecords = null;

    		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	Document doc;
			doc = builder.parse(new java.io.ByteArrayInputStream( baos.toByteArray() ));
			doc.getDocumentElement().normalize();

			Element rootElement = doc.getDocumentElement();

			java.util.LinkedList<Node> productsNodeList = xmm.listImmediateChildElements(rootElement).get("Products");
			java.util.LinkedList<Node> valuesNodeList = null;
			java.util.LinkedList<Node> valueNodeList = null;

			String proposalId = null;
			String sku = null;
			String fecha = null;
			String attyp = null;

			Node attypNode = null;
			Node proposalIdNode = null;

			String attributeId = null;

			String characteristicIdentifier = null;
			String lookup = null;

			if(productsNodeList != null && !productsNodeList.isEmpty()) {
				java.util.LinkedList<Node> products = xmm.listImmediateChildElements( productsNodeList.getFirst() ).get("Product");
				if(products != null) {
					for(Node productNode : products) {
						sku = null;
						fecha = null;
						attyp = null;
						if(((Element)productNode).hasAttribute("ZNPRST")) {
							valuesNodeList = xmm.listImmediateChildElements(productNode).get("Values");
							if(valuesNodeList != null && !valuesNodeList.isEmpty()) {
								proposalIdNode = xmm.byAttributeValue(valuesNodeList.getFirst(), "AttributeID", "ZNPRST");
								if(proposalIdNode != null) {
									proposalId = proposalIdNode.getTextContent();
								}
								sku = xmm.byAttributeValue(valuesNodeList.getFirst(), "AttributeID", "MATNR").getTextContent();
								fecha = xmm.byAttributeValue(valuesNodeList.getFirst(), "AttributeID", "ERSDA").getTextContent();
								attypNode = xmm.byAttributeValue(valuesNodeList.getFirst(), "AttributeID", "ATTYP");
								if(attypNode != null && proposalId != null && !"".equals(proposalId)) {
									attyp = attypNode.getTextContent();
									rows.put(
											new org.json.JSONObject()
												.put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@'MASTER'"))
												.put("values", new org.json.JSONArray().put(sku).put(fecha)));
									aggregaRow(proposalId, valuesNodeList.getFirst(), globalRows);
								}else {
									if(attypNode == null || "".equals(attyp)) {
										System.out.println("\tNot found an ATTYP, but: sku=" + sku + ", fecha=" + fecha + ", proposalId=" + proposalId);
									} else {
										valueNodeList = xmm.listImmediateChildElements(valuesNodeList.getFirst()).get("Value");
										if(valueNodeList != null) {
											characteristicRecords = new org.json.JSONArray();
											for(Node valueNode : valueNodeList) {
												attributeId = ((Element)valueNode).getAttribute("AttributeID");
												characteristicIdentifier = relacionIdentificadores.get(attributeId);
												if(characteristicIdentifier != null) {
													lookup = lookups.get(attributeId);
													armaCaracteristica(characteristicIdentifier, valueNode.getTextContent(), lookup != null, characteristicRecords);
												}
											}
											if(characteristicRecords.length() > 0) {
												response = workshop.makeRequest("POST", characteristicIdentifier, lookups, new org.json.JSONObject().put("_characteristicRecords", characteristicRecords).toString());
												System.out.println("Response from creating a new one: " + response);
											}
										}
									}
								}
							}
						}else {
							System.out.println("Element without pal: " + productNode);
							NamedNodeMap nnm = productNode.getAttributes();
							StringBuilder sb = new StringBuilder();
							for(int f = 0; f < nnm.getLength(); f++) {
								sb.append(f == 0 ? "" : ",").append(nnm.item(f).getNodeName() + ": " + nnm.item(f).getNodeValue());
							}
							if(sb.length() > 0) {
								System.out.println(sb.toString());
							}
						}
					}
					System.out.println("Gathered rows: " + rows.length());
					if(rows.length() > 0) {
						org.json.JSONObject request = new org.json.JSONObject()
								.put("columns",
										new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", ("02".equals(attyp) ? "Article" : "Product2G") + "CharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
											.put(new org.json.JSONObject().put("identifier", ("02".equals(attyp) ? "Article" : "Product2G") + "CharacteristicValueLang.Value('SKUCreationDate',root,\"0000.0000.RK\",'SKUCreationDate',-1)")))
								.put("rows", rows);
						System.out.println("\tNow sending..." + request);
						response = workshop.makeRequest("POST", "/list/" + ("02".equals(attyp) ? "Article" : "Product2G"), qp,
								request.toString());
						System.out.println("Sent and got: " + response);
					}
				}else {
					System.out.println("No products found in file: " + fileName);
				}
			}else {
				System.out.println("File did not contain Products: " + fileName);
			}

    	} catch (ParserConfigurationException e) {
			System.out.println("\tBad file: " + fileName + " (ParseConfigurationException: " + e.getMessage() + ")");
		} catch (SAXException e) {
			System.out.println("\tBad file: " + fileName + " (SAXException: " + e.getMessage() + ")");
		} catch (IOException e) {
			e.printStackTrace();
		}

    }

    private static void aggregaRow(String id, Node valuesNode, org.json.JSONArray rows) {
    	org.json.JSONObject row = new org.json.JSONObject();
    	org.json.JSONArray values = new org.json.JSONArray();
    	Node attributeNode = null;
    	for(String column : columnAlignmentForListPOST) {
    		attributeNode = xmm.byAttributeValue(valuesNode, "AttributeID", column);
    		values.put(attributeNode != null ? attributeNode.getTextContent() : "");
    	}
    	row.put("object", new org.json.JSONObject().put("id", "'" + id + "'@'MASTER'"));
    	row.put("values", values);
    }

    private static void armaCaracteristica(String characteriticIdentifier, String value, boolean isLookup, org.json.JSONArray characteristicRecords) {
    	characteristicRecords.put( new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteriticIdentifier))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")).put("values", new org.json.JSONArray().put( isLookup ? new org.json.JSONObject().put("_code", value) : value ))))) );
    }

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    public static String writeToSftp(SftpClient sftp, byte[] content, String remoteBasePath) throws IOException {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        Properties sequenceProps = new Properties();
        int sequence = 1;

        if (Files.exists(SEQUENCE_FILE)) {
            try (InputStream in = Files.newInputStream(SEQUENCE_FILE)) {
                sequenceProps.load(in);
                String lastDate = sequenceProps.getProperty("date");
                String lastSeq = sequenceProps.getProperty("seq");
                if (date.equals(lastDate) && lastSeq != null) {
                    sequence = Integer.parseInt(lastSeq) + 1;
                }
            }
        }

        String fileName = String.format("%s_%03d.XML", date, sequence);
        String fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;

        try (OutputStream os = sftp.write(fullPath)) {
            os.write(content);
        }

        try (OutputStream out = Files.newOutputStream(SEQUENCE_FILE)) {
            sequenceProps.setProperty("date", date);
            sequenceProps.setProperty("seq", String.valueOf(sequence));
            sequenceProps.store(out, null);
        }

        return fullPath;
    }
}

