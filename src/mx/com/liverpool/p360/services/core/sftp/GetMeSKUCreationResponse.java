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

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class GetMeSKUCreationResponse {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

    // SFTP connection parameters
    private static final String HOST = "172.16.204.243"; // SFTP server address
    private static final int PORT = 22; // SFTP server port
    private static final String USER = "userp360"; // SFTP username
    private static final Path PRIVATE_KEY_PATH = Paths.get("/home/P360admin/.ssh/id_rsa"); // Path to private key
    private static final String REMOTE_DIR = "../../interfase/mer/in/step/zrtuab122"; // Remote directory to monitor
    private static final Path STATE_FILE = Paths.get("processed_files_zrtuab122.properties");
	private static final java.util.ArrayList<String> currentLookups = currentLookups();

    private static boolean USE_CACHE = false;

	public static void main(String[] args) throws IOException, InterruptedException, ParseException {
//		long init = System.currentTimeMillis();
		if(args.length > 0) {
    		USE_CACHE = Boolean.parseBoolean(args[0]);
    	}else {
    		USE_CACHE = true;
    	}

        SshClient client = SshClient.setUpDefaultClient();
        client.start();

        // Ensure local processed dir exists
//        java.nio.file.Files.createDirectories(LOCAL_PROCESSED_DIR);

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
//                            Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
//                            java.nio.file.Files.write(localCopy, out.toByteArray());

                            // Update state
                            processedState.setProperty(name, String.valueOf(remoteModified));
                            if (USE_CACHE) {
                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
                                    processedState.store(stateOut, null);
                                }
                            }
                            try {
                    			processFile(java.nio.file.Paths.get(filePath), out);
                    		} catch (ParserConfigurationException | SAXException | IOException e) {
                    			e.printStackTrace();
                    		}
//                            uploadDataFrom122(filePath, out);
//                            if(!USE_CACHE)
//                            	break;
                        }
                    }
                    Thread.sleep(10_000);
                }
            }
        } finally {
            client.stop();
        }

//		System.out.println("All done. " + workshop.formatTime(System.currentTimeMillis() - init) );
	}

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

	public static void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos) throws ParserConfigurationException, SAXException, IOException {

		java.util.Map<String, java.util.LinkedList<String>> currentLookupContentsInServer = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> currentLookupContentsInFile = new java.util.TreeMap<>();


		java.util.LinkedList<String> toDelete = null;
		java.util.Map<String, String> toInsert = new java.util.TreeMap<>();

		java.util.LinkedList<String> contenido = null;
		java.util.Map<String, String> currentFileMap = null;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(new java.io.ByteArrayInputStream( baos.toByteArray() ));
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();

		System.out.println(xmm.listImmediateChildElements(rootElement));
		System.out.println(xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("ListsOfValues").getFirst()));

		java.util.LinkedList<Node> listOfValueNodeList = xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("ListsOfValues").getFirst()).get("ListOfValue");
		java.util.LinkedList<Node> valuesNodeList = null;

		Node idNode = null;
		Node nameNode = null;
		Node validationNode = null;

		Node codeNode = null;
		Node valueNode = null;

		String lovRawId = null;
		String lovRawName = null;
		String lovRawValidation = null;

		String code = null;
		String value = null;

		for(Node n : listOfValueNodeList) {

			idNode = xmm.byName(n, "Id");
			if(idNode != null) {
				lovRawId = idNode.getTextContent();
			}else {
				// PANIC, NO ID
				throw new IllegalStateException("No Id tag found within ListOfValue tag.");
			}

			nameNode = xmm.byName(n, "Name");
			if(nameNode != null) {
				lovRawName = nameNode.getTextContent();
			}

			validationNode = xmm.byName(n, "Validation");
			if(validationNode != null) {
				lovRawValidation = validationNode.getTextContent();
				if(lovRawValidation != null && !"".equals(lovRawValidation.trim())) {
					System.out.println("$$$ ->" + lovRawValidation + "<-" + lovRawId);
				}
			}

			valuesNodeList = xmm.listImmediateChildElements(n).get("Values");
			if(valuesNodeList != null) {
				currentFileMap = new java.util.TreeMap<>();
				for(Node vn : valuesNodeList) {
					codeNode = xmm.byName(vn, "Id");
					valueNode = xmm.byName(vn, "Value");

					if(codeNode != null) {
						code = codeNode.getTextContent();
					}else {
						// PANIC, NO CODE FOUND
						throw new IllegalStateException("No Id tag found for a Values tag within LoV: " + lovRawId);
					}
					if(valueNode != null) {
						value = valueNode.getTextContent();
					}
					currentFileMap.put(code, value);
				}
				System.out.println("... Collected " + currentFileMap.size() + " entries.");
				currentLookupContentsInFile.put(lovRawId, currentFileMap);
				if(!currentLookups.contains(lovRawId + "LOV")) {
					createLookup(lovRawId + "LOV", lovRawName + "LOV");
				}
				contenido = collectLoveCodes(lovRawId + "LOV");
				currentLookupContentsInServer.put(lovRawId + "LOV", contenido);

				toInsert = notInServer(contenido, currentFileMap);
				toDelete = extraInServer(contenido, currentFileMap);
				deleteValues(toDelete, lovRawId + "LOV");
				inserValues(toInsert, lovRawId + "LOV");
			}

		}

	}

	public static void inserValues(java.util.Map<String, String> values, String lookup) {
		if(values.isEmpty()) {
			return;
		}
		System.out.println("Going to insert " + values.size() + " values on " + lookup);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		int counter = 0;
		for(java.util.Map.Entry<String, String> entry : values.entrySet()) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'" + lookup + "'")).put("values", new org.json.JSONArray().put(entry.getValue()).put(true)));
			if(rows.length() == 100) {
				counter+=rows.length();
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString().toString());
				if(response == null) {
					System.out.println("Error: " + workshop.getRawResponse());
				}else {
					System.out.println(counter + "/" + values.size() + " - " + (response.has("counters") ? response.get("counters") : response ));
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString().toString());
			if(response == null) {
				System.out.println("Error: " + workshop.getRawResponse());
			}else {
				System.out.println(counter + "/" + values.size() + " - " + (response.has("counters") ? response.get("counters") : response ));
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

	public static void createLookup(String code, String name) {
		org.json.JSONObject response = null;
		response = workshop.makeRequest("POST", "/list/Lookup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + code + "'")).put("values", new org.json.JSONArray().put(name)))).toString());
		System.out.println(response == null ? workshop.getRawResponse() : "On creating lookup \"" + code + "\" (" + name + "): " + response);
	}

	public static void deleteValues(java.util.LinkedList<String> toDelete, String lookup) {
		if(toDelete.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		int counter = 0;
		int deleted = 0;
		for(String value : toDelete) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("\"");
			sb.append(value);
			sb.append("\"");
			counter++;
			if(counter == 120) {
				deleted += counter;
				qp.put("lookup", lookup);
				qp.put("query", "LookupValue.Code in (" + sb.toString() + ")");
				System.out.println("Deleting values from " + lookup + " ... " + toDelete.size());
				org.json.JSONObject response = workshop.makeRequest("DELETE", "/list/LookupValue/bySearch", qp, null);
				System.out.println(response == null ? "Error: " + workshop.getRawResponse() : deleted + "/" + toDelete.size() + " - " + (response.has("counters") ? response.getJSONObject("counters") : response));
				sb.setLength(0);
				counter = 0;
			}
		}
		if(counter > 0) {
			deleted += counter;
			qp.put("lookup", lookup);
			qp.put("query", "LookupValue.Code in (" + sb.toString() + ")");
			System.out.println("Deleting values from " + lookup + " ... " + toDelete.size());
			org.json.JSONObject response = workshop.makeRequest("DELETE", "/list/LookupValue/bySearch", qp, null);
			System.out.println(response == null ? "Error: " + workshop.getRawResponse() : deleted + "/" + toDelete.size() + " - " + (response.has("counters") ? response.getJSONObject("counters") : response));
		}
	}

	public static java.util.Map<String, String> notInServer(java.util.LinkedList<String> codesInServer, java.util.Map<String, String> contenidoFile){
		java.util.Map<String, String> nuevas = new java.util.TreeMap<>();
		for(java.util.Map.Entry<String, String> entry : contenidoFile.entrySet()) {
			if(!codesInServer.contains(entry.getKey())) {
				nuevas.put(entry.getKey(), entry.getValue());
			}
		}
		return nuevas;
	}

	public static java.util.LinkedList<String> extraInServer(java.util.LinkedList<String> codesInServer, java.util.Map<String, String> contenidoFile){
		java.util.LinkedList<String> extra = new java.util.LinkedList<>();
		for(String code : codesInServer) {
			if(!contenidoFile.containsKey(code)) {
				extra.addLast(code);
			}
		}
		return extra;
	}

	public static java.util.ArrayList<String> currentLookups(){
		System.out.println("%%%%% Collecting lookups...");
		java.util.LinkedList<String> codes = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "not Lookup.Identifier is empty");
		qp.put("fields", "Lookup.Identifier");
		qp.put("pageSize", "1000");

		int currentIndex = 0;
		int totalSize = 0;

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Lookup/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					codes.addLast(values.getString(0));
				}
			}else {
				System.out.println("GetCurrentLookups ### ERR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		System.out.println(codes.size() + " lookups collected %%%%%.");
		return new java.util.ArrayList<>(codes);
	}

	public static java.util.LinkedList<String> collectLoveCodes(String lookup){
		long init = System.currentTimeMillis();
		System.out.println("Obtaining info for: " + lookup);
		java.util.LinkedList<String> codes = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", lookup);
		qp.put("fields", "LookupValue.Code");
		qp.put("pageSize", "1000");

		int currentIndex = 0;
		int totalSize = 0;

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/byLookup", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					codes.addLast(values.getString(0));
				}
			}else {
				System.out.println("### ERR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		System.out.println("Done collecting values for " + lookup + " (" + codes.size() + " values)");
		System.out.println(workshop.formatTime(System.currentTimeMillis() - init));
		return codes;

	}
}
