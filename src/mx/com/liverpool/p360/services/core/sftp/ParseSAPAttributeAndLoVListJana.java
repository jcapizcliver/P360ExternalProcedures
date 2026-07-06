package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
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

public class ParseSAPAttributeAndLoVListJana {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();
	private static final String BASE_URL = PropertiesManager.get("p360.contingency.base_url");
//			"http://172.18.237.162:1512/rest/V2.0";

	static {
		workshop.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		workshop.setBaseUrl(BASE_URL);
	}

    // SFTP connection parameters
	private static final String HOST = PropertiesManager.get( "p360.contingency.s4h.host" ); //SFTP server address: 172.18.184.26
    private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.s4h.port" ));//SFTP server port: 22
    private static final String USER = PropertiesManager.get( "p360.contingency.s4h.userp360" );// SFTP username: userp360
    private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.s4h.private_key_path" ));//Path to private key: /home/P360admin/.ssh/id_rsa
    private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.s4h.remote_directory_lovs" );//Remote directory to monitor: ../../interfase/mer/out/step
    private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.s4h.local_processed_dir_lovs" ));//Path: /u01/stage/SBB_LoVs/processed
    private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.s4h.state_file_lovs" ));//File: processed_sbb_LoVs.properties
    private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.s4h.use_cache" ));//false;

    
	private static final java.util.ArrayList<String> currentLookups = currentLookups();
	private static final java.util.Set<String> allCatalogs = collectAllCatalogs();


	public static void main(String[] args) throws IOException, InterruptedException, ParseException, ServiceUnavailableException {

		workshop.setBaseUrl(BASE_URL);
//		long init = System.currentTimeMillis();
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

        try (ClientSession session = client.connect(USER, HOST, PORT)
                .verify(10, TimeUnit.SECONDS)
                .getSession()) {

            FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
            keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
            keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);

            session.auth().verify(10, TimeUnit.SECONDS);

            java.util.regex.Pattern p = java.util.regex.Pattern.compile("LOV(2025.+)");
            java.util.regex.Matcher m = null;

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
            long base = sdf.parse("20250505000000").getTime();
//            long base = sdf.parse("20250501123400").getTime();
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

                        m = p.matcher(name);
                        if(m.find()) {
                        	ft = sdf.parse(m.group(1)).getTime();
                        	if(base > ft) {
                        		log("Skipping " + name);
                        		continue;
                        	}
                        }else {
                        	log("Not found pattern in: " + name);
                        	continue;
                        }
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
                            try {
                    			processFile(java.nio.file.Paths.get(filePath), out);
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

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

	public static void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {

		java.util.Map<String, java.util.LinkedList<String>> currentLookupContentsInServer = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> currentLookupContentsInFile = new java.util.TreeMap<>();
		java.util.Map<String, java.util.ArrayList<String>> currentlyUsedValues = new java.util.TreeMap<>();
		java.util.ArrayList<String> valuesInUse = null;

		java.util.LinkedList<String> toDelete = null;
		java.util.Map<String, String> toInsert = new java.util.TreeMap<>();
		java.util.LinkedList<String> codesToDisable = new java.util.LinkedList<>();

		java.util.LinkedList<String> contenido = null;
		java.util.Map<String, String> currentFileMap = null;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(new java.io.ByteArrayInputStream( baos.toByteArray() ));
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();

		log(xmm.listImmediateChildElements(rootElement));
		log(xmm.listImmediateChildElements( xmm.listImmediateChildElements(rootElement).get("ListsOfValues").getFirst()));

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

			lovRawId = ((Element)n).getAttribute("ID");

			nameNode = xmm.byName(n, "Name");
			if(nameNode != null) {
				lovRawName = nameNode.getTextContent();
			}

			validationNode = xmm.byName(n, "Validation");
			if(validationNode != null) {
				lovRawValidation = validationNode.getTextContent();
				if(lovRawValidation != null && !"".equals(lovRawValidation.trim())) {
					log("$$$ ->" + lovRawValidation + "<-" + lovRawId);
				}
			}

			valuesNodeList = xmm.listImmediateChildElements(n).get("Value");
			if(valuesNodeList != null) {
				currentFileMap = new java.util.TreeMap<>();
				for(Node vn : valuesNodeList) {
					code = ((Element)vn).getAttribute("ID");
					value = vn.getTextContent();
					currentFileMap.put(code, value);
				}
				log("... Collected " + currentFileMap.size() + " entries. " + lovRawId + " in " + path.toString());
				currentLookupContentsInFile.put(lovRawId, currentFileMap);
				if(!currentLookups.contains(lovRawId)) {
					createLookup(lovRawId, lovRawName);
				}
				valuesInUse = currentlyUsedValues.get(lovRawId + "");
				if(valuesInUse == null) {
					valuesInUse = collectCurrentlyUsedValuesByCharacteristics(lovRawId + "");
					currentlyUsedValues.put(lovRawId + "", valuesInUse);
				}
				contenido = collectLoveCodes(lovRawId + "");
				currentLookupContentsInServer.put(lovRawId + "", contenido);

				toInsert = notInServer(contenido, currentFileMap);
				log("Found " + toInsert.size() + " values new to server");
				toDelete = extraInServer(contenido, currentFileMap, valuesInUse, codesToDisable);
				log("Found " + toDelete.size() + " values extra in server");

				if(Boolean.parseBoolean( PropertiesManager.get( "p360.contingency.s4h.with_lookup_values_delete" ))) {
					deleteValues(toDelete, lovRawId + "");
					disableCodes(lovRawId + "", codesToDisable);
				}
				
				inserValues(toInsert, lovRawId + "");
				codesToDisable.clear();
			}

		}
	}

	private static void disableCodes(String lookup, java.util.LinkedList<String> codesToDisable) {
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", ParseSAPAttributeAndLoVListJana.workshop.getRc().getHeader().get("Authorization"));
		workshop.setBaseUrl(BASE_URL);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONObject response = null;
		for(String code : codesToDisable) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + code + "'@'" + lookup + "'")).put("values", new org.json.JSONArray().put(false)));
			if(rows.length() == 100) {
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				try{
					log(response.getJSONObject("counters"));
				}catch(org.json.JSONException e) {
					e.printStackTrace();
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
			try{
				log(response.getJSONObject("counters"));
			}catch(org.json.JSONException e) {
				e.printStackTrace();
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

	private static java.util.ArrayList<String> collectCurrentlyUsedValuesByCharacteristics(String lookup) throws ServiceUnavailableException {
		java.util.LinkedList<String> currentlyUsedValues = new java.util.LinkedList<>();
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", ParseSAPAttributeAndLoVListJana.workshop.getRc().getHeader().get("Authorization"));
		workshop.setBaseUrl(BASE_URL);

		workshop.putParameter("fields", "Characteristic.Identifier");
		workshop.putParameter("query", "Characteristic.Lookup->Lookup.Identifier equals \"" + lookup + "LOV\"");

		org.json.JSONObject response = new org.json.JSONObject();
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Set<String> cs = new java.util.TreeSet<>();

		do {
			workshop.putParameter("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					cs.add(values.getString(0));
				}
			}else {
				log("Problem making request: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		for(String cid : cs) {
			currentlyUsedValues.addAll( valoresUsadosPorPA(cid) );
		}
		return new java.util.ArrayList<>(currentlyUsedValues);
	}

	public static java.util.Set<String> valoresUsadosPorPA(String cid) throws ServiceUnavailableException{
		log("\nGoing on: " + cid);
		java.util.Set<String> lkpValues = new java.util.TreeSet<>();
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", ParseSAPAttributeAndLoVListJana.workshop.getRc().getHeader().get("Authorization"));
		workshop.setBaseUrl(BASE_URL);
		workshop.putParameter("pageSize", "1200");
		allCatalogs.add("MASTER");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int currentIndex = 0;

		for(String catalog : allCatalogs) {
			workshop.putParameter("catalog", catalog);
			workshop.putParameter("fields", "ArticleCharacteristicValue.LookupValue('" + cid + "',root,\"0000.0000.RK\",'" + cid + "')->LookupValue.Code");
			workshop.putParameter("query", "not ArticleCharacteristicValue.LookupValue('" + cid + "',root,\"0000.0000.RK\",'" + cid + "') is empty");
			do {
				workshop.putParameter("startIndex", String.valueOf(currentIndex));
				response = workshop.makeRequest("GET", "/list/Article/bySearch");
				if(response != null) {
					totalSize = response.getInt("totalSize");
					currentIndex += response.getInt("pageSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						lkpValues.add(rows.getJSONObject(i).getJSONArray("values").getJSONArray(0).getString(0));
					}
				}else {
					log("## ERROR: " + workshop.getRawResponse());
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;

			workshop.putParameter("fields", "Product2GCharacteristicValue.LookupValue('" + cid + "',root,\"0000.0000.RK\",'" + cid + "')->LookupValue.Code");
			workshop.putParameter("query", "not Product2GCharacteristicValue.LookupValue('" + cid + "',root,\"0000.0000.RK\",'" + cid + "') is empty");
			do {
				workshop.putParameter("startIndex", String.valueOf(currentIndex));
				response = workshop.makeRequest("GET", "/list/Product2G/bySearch");
				if(response != null) {
					totalSize = response.getInt("totalSize");
					currentIndex += response.getInt("pageSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						lkpValues.add(rows.getJSONObject(i).getJSONArray("values").getJSONArray(0).getString(0));
					}
				}else {
					log("## ERROR: " + workshop.getRawResponse());
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}
		log("Found: " + lkpValues.size());
		return lkpValues;
	}

	public static java.util.Set<String> collectAllCatalogs() {
		java.util.Set<String> allCatalogs = new java.util.TreeSet<>();
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", ParseSAPAttributeAndLoVListJana.workshop.getRc().getHeader().get("Authorization"));
		workshop.setBaseUrl(BASE_URL);
		workshop.putParameter("fields", "SupplierCatalog.Identifier");

		try {
			org.json.JSONObject response = workshop.makeRequest("GET", "/list/SupplierCatalog/all");
			if(response != null) {
				log(response);
				org.json.JSONArray rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					allCatalogs.add(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}
			}else {
				log("## ERROR: " + workshop.getRawResponse());
			}
		}catch(ServiceUnavailableException e) {
			e.printStackTrace();
		}
		return allCatalogs;
	}

	public static void inserValues(java.util.Map<String, String> values, String lookup) {
		if(values.isEmpty()) {
			return;
		}
		log("Going to insert " + values.size() + " values on " + lookup);
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
					log("Error: " + workshop.getRawResponse());
				}else {
					log(counter + "/" + values.size() + " - " + (response.has("counters") ? response.get("counters") : response ));
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rows).toString().toString());
			if(response == null) {
				log("Error: " + workshop.getRawResponse());
			}else {
				log(counter + "/" + values.size() + " - " + (response.has("counters") ? response.get("counters") : response ));
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

	public static void createLookup(String code, String name) {
		org.json.JSONObject response = null;
		response = workshop.makeRequest("POST", "/list/Lookup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + code + "'")).put("values", new org.json.JSONArray().put(name)))).toString());
		log(response == null ? workshop.getRawResponse() : "On creating lookup \"" + code + "\" (" + name + "): " + response);
	}

	public static void deleteValues(java.util.LinkedList<String> toDelete, String lookup) throws ServiceUnavailableException {
		if(toDelete.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.addHeader("Authorization", ParseSAPAttributeAndLoVListJana.workshop.getRc().getHeader().get("Authorization"));
		workshop.setBaseUrl(BASE_URL);
		workshop.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		int counter = 0;
		int deleted = 0;
		log("Going to delete: " + toDelete.size() + " values in server.");
		int times = 0;
		boolean corrupted = false;
		java.util.LinkedList<String> remaining = new java.util.LinkedList<>();
		for(String value : toDelete) {
			remaining.addLast(value);
			if(corrupted) {
				continue;
			}
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("\"");
			sb.append(value);
			sb.append("\"");
			counter++;
			if(counter == 120) {
				qp.put("lookup", lookup);
				qp.put("query", "LookupValue.Code in (" + sb.toString() + ")");
				log("Deleting values from " + lookup + " ... " + toDelete.size());
				org.json.JSONObject response = workshop.makeRequest("DELETE", "/list/LookupValue/bySearch", qp, null);
				if(!response.has("counters") && !response.getJSONObject("counters").has("errors")) {
					log("Did not contain \"errors\". " + response);
					System.exit(0);
				}
				if(response.getJSONObject("counters").getInt("errors") > 0) {
					corrupted = true;
					log("Got corrupted.");
					continue;
				}else {
					deleted += counter;
					remaining.clear();
					log(response == null ? "Error: " + workshop.getRawResponse() : deleted + "/" + toDelete.size() );
					times++;
					try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("/u01/stage/ECC_140/log/" + lookup + "_" + times)))){ pw.println(response); }catch(java.io.IOException e) { e.printStackTrace(); }
					sb.setLength(0);
					counter = 0;
				}
			}
		}
		if(corrupted) {
			deleteCorrupted(toDelete, lookup);
		}else {
			if(counter > 0) {
				deleted += counter;
				qp.put("lookup", lookup);
				qp.put("query", "LookupValue.Code in (" + sb.toString() + ")");
				log("Deleting values from " + lookup + " ... " + toDelete.size());
				org.json.JSONObject response = workshop.makeRequest("DELETE", "/list/LookupValue/bySearch", qp, null);
				log(response == null ? "Error: " + workshop.getRawResponse() : deleted + "/" + toDelete.size() );
			}
		}
	}

	public static void deleteCorrupted(java.util.LinkedList<String> toDelete, String lookup) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		workshop.addHeader("Authorization", ParseSAPAttributeAndLoVListJana.workshop.getRc().getHeader().get("Authorization"));
		rw.setBaseUrl(BASE_URL);
		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		org.json.JSONObject response = null;
		java.util.LinkedList<String> conflictingValues = new java.util.LinkedList<>();
		java.util.LinkedList<org.json.JSONObject> corruptedMessages = new java.util.LinkedList<>();
		for(String code : toDelete) {
			rw.putParameter("items", "'" + code + "'@'" + lookup + "'");
			response = rw.makeRequest("DELETE", "/list/LookupValue/byItems");
			if(response == null) {
				log("ERROR: " + rw.getRawResponse());
				System.exit(0);
			}else {
				if(response.getJSONObject("counters").getInt("errors") == 0) {
					conflictingValues.addLast(code);
				}else {
					corruptedMessages.addLast(response.getJSONArray("entries").getJSONObject(0));
				}
			}
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("/u01/stage/ECC_140/conflictingValues/" + lookup)))){
			conflictingValues.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("/u01/stage/ECC_140/corruptedMessages/" + lookup)))){
			conflictingValues.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	public static java.util.Map<String, String> notInServer(java.util.LinkedList<String> codesInServer, java.util.Map<String, String> contenidoFile){
		java.util.Map<String, String> nuevas = new java.util.TreeMap<>();
		for(java.util.Map.Entry<String, String> entry : contenidoFile.entrySet()) {
			if(!codesInServer.contains(entry.getKey()) && !"".equals(entry.getKey())) {
				nuevas.put(entry.getKey().replaceAll("'", "\\\\'"), entry.getValue());
			}
		}
		return nuevas;
	}

	public static java.util.LinkedList<String> extraInServer(java.util.LinkedList<String> codesInServer, java.util.Map<String, String> contenidoFile, java.util.ArrayList<String> valuesInUse, java.util.LinkedList<String> codesToDisable){
		java.util.LinkedList<String> extra = new java.util.LinkedList<>();
		for(String code : codesInServer) {
			if(!contenidoFile.containsKey(code)) {
				if(!valuesInUse.contains(code)) {
					extra.addLast(code);
				}else {
					codesToDisable.addLast(code);
				}
			}
		}
		return extra;
	}

	public static java.util.ArrayList<String> currentLookups(){
//		log("%%%%% Collecting lookups...");
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
				log("GetCurrentLookups ### ERR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
//		log(codes.size() + " lookups collected %%%%%.");
		return new java.util.ArrayList<>(codes);
	}

	public static java.util.LinkedList<String> collectLoveCodes(String lookup){
		long init = System.currentTimeMillis();
//		log("Obtaining info for: " + lookup);
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
				log("### ERR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
//		log("Done collecting values for " + lookup + " (" + codes.size() + " values)");
//		log(workshop.formatTime(System.currentTimeMillis() - init));
		return codes;

	}

	private static final Logger LOGGER = Logger.getLogger(ParseSAPAttributeAndLoVListJana.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/s4h/parseECC140Response-%g.log", 25 * 1024 * 1024, 10, true);
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
    
    private static void log(Object message) {
    		log(String.valueOf(message));
    }
	
	private static void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","	.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}
}
