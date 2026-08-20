package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.temp.exports.EnvioAtgDao.EnvioAtgRow;

public class RealExportProducts {

	private static final String urlDeATG = PropertiesManager.get("p360.contingency.out.url_atg");// "http://172.16.203.46:7089/pimstepatg/service/";
	private static final String urlDeOMS = PropertiesManager.get("p360.contingency.out.url_oms"); // "https://brokerqa.liverpool.com.mx:7053/oms/Int100/Items";
	private static final java.nio.file.Path fileSystemPrefixOMS = java.nio.file.Paths.get("..", "stage", "ToOMS");
	private static final java.nio.file.Path fileSystemPrefix = java.nio.file.Paths.get("/", "u01", "workshop", "stage","ToATG");
	private static final String baseUrlDEV = PropertiesManager.get("p360.contingency.base_url");
	private static final RESTWrapper wrapper = new RESTWrapper();
	private static final RESTWorkshop rw = wrapper.getRw();
	private static final RestClient rc = rw.getRc();

	private static final String host = PropertiesManager.get("p360.contingency.dwh.host");
	private static final int port = Integer.parseInt(PropertiesManager.get("p360.contingency.dwh.port", "22"));
	private static final String user = PropertiesManager.get("p360.contingency.dwh.user");
	private static final java.nio.file.Path privateKeyPath = java.nio.file.Paths.get("/home/P360admin/.ssh/id_rsa");

	private final java.util.Map<String, java.util.Map<String, org.json.JSONObject>> templateMetadataSet = new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Map<String, String>> templateStructureGroupAttributeValues = new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Set<String>> templateSets = new java.util.TreeMap<>();
	private final java.util.Map<String, org.json.JSONObject> globalProperties = new java.util.TreeMap<>();
	private final java.util.Set<String> globalSet = new java.util.TreeSet<>();

	private final ELog dbLog = new ELog() {
		@Override
		public void logE(Exception e) {
			RealExportProducts.this.logE(e);
		}

		@Override
		public void log(String message) {
			RealExportProducts.this.log(message);
		}
	};

	private final RealExportProductsUtils rutils = new RealExportProductsUtils(dbLog);
	private final DBAccessDataStub dastub = new DBAccessDataStub(dbLog);
	private final java.util.Map<String, String> atgGroups = loadLookupGroups();
	private final java.util.Map<Integer, org.json.JSONObject> characteristicMetadataByID = new java.util.HashMap<>();
	private final java.util.Map<Integer, String> characteristicIdentifierByID = new java.util.HashMap<>();
	private final java.util.Set<Integer> rootCharacteristicIDs = new java.util.TreeSet<>();

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private boolean frozenImagesExists = true;
	private boolean exploitLayerExists = true;
	private boolean toDwhOnly = false;
	private static final long MAX_BATCH_BYTES = 7L * 1024L * 1024L;
	
	private final java.util.List<String> generatedAtgFiles = new java.util.ArrayList<>();
	private final java.util.List<String> atgBrokerResponses = new java.util.ArrayList<>();

	private boolean atgBrokerFailure = false;

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

	public String newExecutionId() {
		byte[] randomBytes = new byte[16]; // 128 bits
		RANDOM.nextBytes(randomBytes);

		String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

		String timestamp = LocalDateTime.now().format(TS_FORMAT);

		return "EXEC-" + timestamp + "-" + randomPart;
	}

	public JdbcConfig initJdbcConfig() throws IOException {
		JdbcConfig config = new JdbcConfig();
		Path propertiesPath = resolveServerPropertiesPath();
		Properties raw = new Properties();
		try (InputStream in = Files.newInputStream(propertiesPath)) {
			raw.load(in);
		}
		config.jdbcDriver = resolveRequiredProperty(raw, "db.master.pool.jdbcDriver");
		config.jdbcUrl = resolveRequiredProperty(raw, "db.master.pool.jdbcUrl");
		config.user = resolveRequiredProperty(raw, "db.master.user");
		config.password = resolveRequiredProperty(raw, "db.master.password");
		return config;
	}

	private static Path resolveServerPropertiesPath() {
		String path = System.getenv("P360_SERVER_PROPERTIES");

		if (path == null || path.trim().isEmpty()) {
			path = "/u01/Informatica/server.properties";
		} else {
		}

		Path resolved = Paths.get(path).toAbsolutePath().normalize();

		if (!Files.exists(resolved)) {
			throw new IllegalArgumentException("No existe server.properties en: " + resolved);
		}

		if (!Files.isRegularFile(resolved)) {
			throw new IllegalArgumentException("La ruta no es archivo: " + resolved);
		}

		return resolved;
	}

	private static String resolveRequiredProperty(Properties raw, String key) {
		String value = resolvePropertyValue(raw, key, new HashSet<String>());

		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("No se encontró la property requerida: " + key);
		}

		return value.trim();
	}

	private static String resolvePropertyValue(Properties raw, String key, Set<String> visiting) {
		if (visiting.contains(key)) {
			throw new IllegalArgumentException("Referencia circular detectada en properties para la clave: " + key);
		}

		String value = raw.getProperty(key);
		if (value == null) {
			return null;
		}

		visiting.add(key);

		Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String referencedKey = matcher.group(1);
			String referencedValue = resolvePropertyValue(raw, referencedKey, visiting);

			if (referencedValue == null) {
				throw new IllegalArgumentException("No se pudo resolver la property referenciada: " + referencedKey);
			}

			matcher.appendReplacement(sb, Matcher.quoteReplacement(referencedValue));
		}

		matcher.appendTail(sb);
		visiting.remove(key);

		return sb.toString();
	}

	public RealExportProducts() {
//		loadDatabaseDictionaries();
//		System.out.println("Loading characteristic metadata");
//		loadCharacteristicMetadata();
//		System.out.println("Characteristic metadata loaded");
//		System.out.println("Adding global metadata");
//		addGlobalData(globalProperties, globalSet);
//		System.out.println("Global metadata added");
//		addCharacteristicData(globalProperties);
	}

	private org.json.JSONObject getMeTheCompa(String compa) throws ServiceUnavailableException {
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			rawResponse = rc.getRequest("GET", baseUrlDEV + "/object/Product2G/'" + compa + "'@'MASTER'"
					+ "?entityFilter=Product2GStructureGroupMap,Product2GCharacteristicValue,Product2G,Product2GLang,ProductExtraData&includeIds=true&includeLabels=true",
					null);
			response = new org.json.JSONObject(rawResponse);
		} catch (org.json.JSONException | IOException e) {
			logE(e);
		}
		return response;
	}

	private static final String USAGE = "Usage: RealExportProductsJdbcBatch <File with IDs or SKUs> -t ID|SKU [-s]\n-t indicates which type of content is in the file: SKU or Proposal IDs\n-s if present, indicates to send the data to destination, default is not send the data.";

	public static void main(String[] args) throws ServiceUnavailableException, IOException {

		if (args.length < 1) {
			System.out.println(USAGE);
			return;
		}
		String source = args[0];
		int type = 0;
		boolean send = false;
		boolean toDwh = false;
		if (args.length > 1) {
			java.util.LinkedList<String> extra = new java.util.LinkedList<>(
					java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 1, args.length)));
			if (!extra.contains("-t") || extra.getLast().equals("-t")) {
				System.out.println(USAGE);
				return;
			}
			String arg = null;
			for (int i = 0; i < extra.size(); i++) {
				arg = extra.get(i);
				if ("-s".equals(arg)) {
					send = true;
				} else if ("-dwh".equals(arg)) {
					toDwh = true;
				} else if ("-t".equals(extra.get(i)) && i < extra.size() - 1) {
					type = "ID".equals(extra.get(i + 1)) ? 0 : "SKU".equals(extra.get(i + 1)) ? 1 : -1;
					if (type == -1) {
						System.out.println(USAGE);
						return;
					}
					i++;
				} else {
				}
			}
		}
		String[] data = sourceContent(source);
		RealExportProducts o = new RealExportProducts();
		o.frozenImagesExists = Boolean.parseBoolean(PropertiesManager.get("p360.contingency.useFrozenImages", "true"));
		o.exploitLayerExists = Boolean.parseBoolean(PropertiesManager.get("p360.contingency.useExploitLayer", "true"));
		o.toDwhOnly = toDwh;
		if (type == 0) {
			o.doIt(data, send, baseUrlDEV);
		} else if (type == 1) {
			java.util.List<String> ids = new java.util.ArrayList<String>();
			for (String sku : data) {
				String id = o.getIdFromSKU(sku);
				if (id != null && !id.trim().isEmpty()) {
					ids.add(id);
				}
			}
			o.doIt(ids.toArray(new String[ids.size()]), send, baseUrlDEV);
		}
	}

	private void toDWH(String content) {
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		try (ClientSession session = client.connect(user, host, port).verify(10, TimeUnit.SECONDS).getSession()) {
			FileKeyPairProvider keyProvider = new FileKeyPairProvider(privateKeyPath);
			keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
			keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
			session.auth().verify(10, TimeUnit.SECONDS);
			String fecha = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
			String fileName = "eilstep_" + fecha + ".xml";
			try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
				writeToSftp(sftp, content, PropertiesManager.get("p360.contingency.dwh.remote_directory_base"),
						fileName);
			}
		} catch (java.io.IOException e) {
			log("Could not send request: " + e.getMessage());
			e.printStackTrace();
		} finally {
			client.stop();
		}
	}

	private static String[] sourceContent(String source) {
		java.util.Set<String> lines = new java.util.TreeSet<>();
		try (java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.InputStreamReader(new java.io.FileInputStream(source)))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				if (!"".equals(line))
					lines.add(line);
			}
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
		return lines.toArray(new String[] {});
	}

	public String getIdFromSKU(String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "characteristic('SKU',-1) wildcard \"" + sku + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
//		System.out.println(sku + " - " + rw.getRawResponse());
		if (response == null) {
		} else {
			rows = response.getJSONArray("rows");
			if (rows.length() > 0) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			} else {
				log("SKU not found: " + sku);
			}
		}
		return null;
	}

	public void processBatch(String[] proposalIds) {
		// Conservado por compatibilidad. El corte real por tamaño se realiza dentro de doIt.
	}

	public static void runForProductIds(String[] proposalIds, boolean send)
			throws ServiceUnavailableException, IOException {
		runForProductIds(proposalIds, send, false);
	}

	public static void runForProductIds(String[] proposalIds, boolean send, boolean toDwh)
			throws ServiceUnavailableException, IOException {
		String[] data = cleanIds(proposalIds);

		RealExportProducts o = new RealExportProducts();
		o.frozenImagesExists = Boolean.parseBoolean(PropertiesManager.get("p360.contingency.useFrozenImages", "true"));
		o.exploitLayerExists = Boolean.parseBoolean(PropertiesManager.get("p360.contingency.useExploitLayer", "true"));
		o.toDwhOnly = toDwh;
		o.doIt(data, send, baseUrlDEV);
	}

	private static String[] cleanIds(String[] ids) {
		java.util.Set<String> clean = new java.util.LinkedHashSet<String>();

		if (ids != null) {
			for (int i = 0; i < ids.length; i++) {
				String id = ids[i];

				if (id != null) {
					id = id.trim();
				}

				if (id != null && id.length() > 0) {
					clean.add(id);
				}
			}
		}

		return clean.toArray(new String[clean.size()]);
	}

	private boolean loaded = false;
	
	private void init(){
		if(loaded)
			return;
		
//		articulosEnviados.clear();
		atgGroups.clear();
		characteristicMetadataByID.clear();
		characteristicIdentifierByID.clear();
		rootCharacteristicIDs.clear();
		globalProperties.clear();
		globalSet.clear();
		templateMetadataSet.clear();
		templateStructureGroupAttributeValues.clear();
		templateSets.clear();
		
		loadDatabaseDictionaries();
		System.out.println("Loading characteristic metadata");
		loadCharacteristicMetadata();
		System.out.println("Characteristic metadata loaded");
		System.out.println("Adding global metadata");
		addGlobalData(globalProperties, globalSet);
		System.out.println("Global metadata added");
		addCharacteristicData(globalProperties);
		
		loaded = true;
	}
	
	public String doIt(String[] proposalIds, boolean sendIt, String baseUrl) throws ServiceUnavailableException {
		log("Going over: " + proposalIds.length);
		return doIt(proposalIds, sendIt);
	}

	private String grabDescLong(org.json.JSONArray lang) {
		for (int i = 0; i < lang.length(); i++) {
			if (10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("descriptionLong") ? lang.getJSONObject(i).getString("descriptionLong")
						: null;
			}
		}
		return null;
	}

	private String grabDescLong2(org.json.JSONArray lang) {
		for (int i = 0; i < lang.length(); i++) {
			if (10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("descriptionLong2")
						? lang.getJSONObject(i).getString("descriptionLong2")
						: null;
			}
		}
		return null;
	}

	private String grabProductName(org.json.JSONArray lang) {
		for (int i = 0; i < lang.length(); i++) {
			if (10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("productName") ? lang.getJSONObject(i).getString("productName") : null;
			}
		}
		return null;
	}

	private String grabProductDescriptionShort(org.json.JSONArray lang) {
		for (int i = 0; i < lang.length(); i++) {
			if (10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
				return lang.getJSONObject(i).has("descriptionShort")
						? lang.getJSONObject(i).getString("descriptionShort")
						: null;
			}
		}
		return null;
	}

	private java.sql.Connection openConnection(JdbcConfig jdbcConfig, boolean autoCommit)
			throws java.sql.SQLException, ClassNotFoundException {
		Class.forName(jdbcConfig.jdbcDriver);

		java.sql.Connection connection = java.sql.DriverManager.getConnection(jdbcConfig.jdbcUrl, jdbcConfig.user,
				jdbcConfig.password);

		connection.setAutoCommit(autoCommit);

		return connection;
	}

	@SuppressWarnings("deprecation")
	public String doIt(String[] proposalIds, boolean sendIt) throws ServiceUnavailableException {
		try(dastub){
			init();
			String execId = newExecutionId();
			long envioAtgExecId = -1;
			JdbcConfig jdbcConfig = null;
			if (exploitLayerExists) {
				try {
					jdbcConfig = initJdbcConfig();
					try (java.sql.Connection con = openConnection(jdbcConfig, true)) {
						envioAtgExecId = EnvioAtgDao.insertarExec(con, execId, "STARTED");
					} catch (ClassNotFoundException | java.sql.SQLException e) {
						logE(e);
					}
				} catch (java.io.IOException e) {
					log("Could not get db connection, please use logs to later insert activity.");
					logE(e);
				}
			} else {
				log("Not using exploit layer");
			}
	
			System.out.println("Doing it " + (proposalIds == null ? "NaN" : proposalIds.length));
			java.util.Date reqDate = new java.util.Date();
			log("Running using baseUrlDEV: " + baseUrlDEV);
			log("Running using fileSystemPrefixOMS: " + fileSystemPrefixOMS);
			log("Running using fileSystemPrefixATG: " + fileSystemPrefix);
			if (proposalIds == null) {
				return null;
			}
	
			String proposalId = null;
			StringBuilder aggregatedMessage = new StringBuilder();
			int batchNumber = 1;
	
			try {
				ExportContext batch = createExportContext();
				final ArmaConjuntoLookSTEP acl = new ArmaConjuntoLookSTEP(mapaDeAtributosFechas);
				System.out.println("chars added");
	
				for (int index = 0; index < proposalIds.length; index++) {
					ExportContext current = createExportContext();
					Document doc = current.doc;
					Element spim = current.spim;
					Element attributes = current.attributes;
					Element assets = current.assets;
					Element products = current.products;
					Element classifications = current.classifications;
					Element webHierarchyRoot = current.webHierarchyRoot;
					Element webHierarchyRootName = current.webHierarchyRootName;
					java.util.Map<String, Element> assetMap = current.assetMap;
					java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap = current.assetReferencesMap;
					java.util.Map<String, String> prodToSKU = current.prodToSKU;
					java.util.Map<String, String> artToSKU = current.artToSKU;
					java.util.List<String> productos = current.productos;
					java.util.Set<String> articulosEnviados = current.articulosEnviados;
					java.util.LinkedList<org.json.JSONObject> rescataLaRaiz = current.rescataLaRaiz;
					java.util.Map<String, java.util.Map<String, org.json.JSONObject>> globalMap = current.globalMap;
					org.json.JSONObject reqPublishMessage = current.reqPublishMessage;
					org.json.JSONObject reqLastApprovedCategories = current.reqLastApprovedCategories;
					org.json.JSONObject reqAPublishMessage = current.reqAPublishMessage;
					org.json.JSONObject req = current.req;
					org.json.JSONObject req2 = current.req2;
					java.util.Map<String, org.json.JSONObject> hierarchyHelper = null;
					org.json.JSONObject entry = null;
					org.json.JSONObject entryHelper = null;
					boolean procede = true;
					proposalId = proposalIds[index];
					current.currentProposalId = proposalId;
	
					try {
						java.nio.file.Path p = java.nio.file.Paths
								.get(PropertiesManager.get("p360.contingency.migration.to_skip_directory"), proposalId);
						if (java.nio.file.Files.exists(p)) {
							log("Skipped to be sent since this was reciently migrated --->" + proposalId + "<---");
							System.out
									.println("Skipped to be sent since this was reciently migrated --->" + proposalId + "<---");
							reqPublishMessage.getJSONArray("rows")
									.put(new org.json.JSONObject()
											.put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@1"))
											.put("values", new org.json.JSONArray().put(
													"Registro recién migrado, si persiste, solicitar mantenimiento manual")));
							java.nio.file.Files.delete(p);
							continue;
						}
						log("--->" + proposalId + "<---");
						System.out.println("--->" + proposalId + "<---");
						// talla normalizada hacia ATG debe de salir como TC-NormalizedSize
						final String[] productsToTestWith = new String[] { proposalId };
						org.json.JSONObject rp = getMeTheCompa(proposalId);
						if (rp == null || !rp.getJSONObject("_data").has("_characteristicRecords")) {
							System.out.println("Returning this " + proposalId + " due to lack of data.");
							continue;
						}
						org.json.JSONArray characteristicArray = rp.getJSONObject("_data")
								.getJSONArray("_characteristicRecords");
						String sapObjectType = null;
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("sapObjectType")) {
							sapObjectType = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("sapObjectType").getString("_code");
						} else {
							sapObjectType = getSAPObjectType(characteristicArray);
						}
						java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> dataMap = buildDataMap(
								rp.getJSONObject("_data").getJSONArray("_characteristicRecords"));
						String business = null;
						try {
							if (rp.getJSONObject("_data").has("business")) {
								business = rp.getJSONObject("_data").getJSONObject("business").getString("_code");
							} else {
								if (dataMap.containsKey("Business")) {
									business = dataMap.get("Business").getFirst().getJSONArray("_recordLang").getJSONObject(0)
											.getJSONArray("values").getJSONObject(0).getString("_code");
								}
							}
						} catch (org.json.JSONException e) {
							logE(e);
						}
						java.util.LinkedList<org.json.JSONObject> lst = null;
						if (sapObjectType != null && "CLK".equals(sapObjectType)) {
							Element product = acl.handleCLK(proposalId, rp.getJSONObject("_data"), dataMap,
									webHierarchyRootName, rescataLaRaiz, globalMap, doc, assetMap, assetReferencesMap, assets,
									attributes, atgGroups, baseUrlDEV);
							products.appendChild(product);
							continue;
						}
						String template = !rp.getJSONObject("_data").has("structureGroupMap") ? null
								: getPrimaryProductTaxonomyTemplate(
										rp.getJSONObject("_data").getJSONArray("structureGroupMap")); // rp.getJSONObject("_data").getJSONArray("structureGroupMap").getJSONObject(0).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").split("@")[0].replaceAll("^'|'$",
																										// "");
						if (template == null) {
							reqPublishMessage
									.getJSONArray(
											"rows")
									.put(new org.json.JSONObject()
											.put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@1"))
											.put("values", new org.json.JSONArray().put("Sin plantilla")));
							log("Skipped due to Sin plantilla " + proposalId);
							System.out.println("Skipped due to Sin plantilla " + proposalId);
							continue;
						}
						String itemId = rp.getJSONObject("_entityItem").getString("_externalId").split("@")[0]
								.replaceAll("^'|'$", "");
						String[] webCategory = getWebCategory(rp.getJSONObject("_data").getJSONArray("structureGroupMap")); // new
																															// String[]
																															// {"cat1240607"};
						String productType = null;
						String descLong = rp.getJSONObject("_data").has("lang")
								? grabDescLong(rp.getJSONObject("_data").getJSONArray("lang"))
								: null;
						String descLong2 = rp.getJSONObject("_data").has("lang")
								? grabDescLong2(rp.getJSONObject("_data").getJSONArray("lang"))
								: null;
						String productName = rp.getJSONObject("_data").has("lang")
								? grabProductName(rp.getJSONObject("_data").getJSONArray("lang"))
								: null;
						String nameLang = rp.getJSONObject("_data").has("lang")
								? grabProductDescriptionShort(rp.getJSONObject("_data").getJSONArray("lang"))
								: null;
						String charactName = null;
		
						String brandName = null;
						String brandIdS4H = null;
						String brandNameLabel = null;
						String brandIdS4HLabel = null;
						String itemGroup = null;
						String itemGroupS4H = null;
						String itemGroupLabel = null;
						String itemGroupS4HLabel = null;
						String direccion = null;
						String direccionLabel = null;
						String seccion = null;
						String seccionLabel = null;
						String supplierPartNumber = null;
						String supplierID = null;
						String supplierIDLabel = null;
						String mainBarCode = null;
						String sku = null;
						String embeddedCodeWEB = null;
						String embeddedCodeWAP = null;
						String refundPolicy = null;
						String lastApprovedCategories = null;
						String clothingSize = null;
						String sizeVaD = null;
		
						String piName = null;
						String piUrl = null;
						String piKey = null;
		
						java.util.LinkedList<String[]> details = new java.util.LinkedList<>();
						java.util.LinkedList<String[]> smoshes = new java.util.LinkedList<>();
						java.util.LinkedList<String[]> illustrations = new java.util.LinkedList<>();
		
						String raw = null;
						String firstVariant = null;
						org.json.JSONObject imageObject = null;
						String tamanoUnico = null;
						String tallaNormalizada = null;
						String codigoColor = null;
						String color = null;
		
						org.json.JSONArray characteristicRecords = null;
						org.json.JSONArray upperRows = null;
		
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("brandName")) {
							brandName = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("brandName").getString("_code");
							brandNameLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("brandName").getString("_label");
						}
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("brandIdS4H")) {
							brandIdS4H = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("brandIdS4H").getString("_code");
							brandIdS4HLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("brandIdS4H").getString("_label");
						}
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("itemGroup")) {
							itemGroup = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("itemGroup").getString("_code");
							itemGroupLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("itemGroup").getString("_label");
						}
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("itemGroupS4H")) {
							itemGroupS4H = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("itemGroupS4H").getString("_code");
							itemGroupS4HLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("itemGroupS4H").getString("_label");
						}
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("direction")) {
							direccion = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("direction").getString("_code");
							direccionLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("direction").getString("_label");
						}
						if (rp.getJSONObject("_data").has("productExtraData")
								&& rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("section")) {
							seccion = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("section").getString("_code");
							seccionLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getJSONObject("section").getString("_label");
						}
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("supplierID")) {
							supplierID = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("supplierID").getString("_code");
							supplierIDLabel = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("supplierID").getString("_label");
						}
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("supplierPartNumber")) {
							supplierPartNumber = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getString("supplierPartNumber");
						}
						if (rp.getJSONObject("_data").has("gtin")) {
							mainBarCode = rp.getJSONObject("_data").getString("gtin");
						}
						if (rp.getJSONObject("_data").has("sku")) {
							sku = String.valueOf(rp.getJSONObject("_data").getLong("sku"));
						}
						if (rp.getJSONObject("_data").has("embeddedCodeWEB")) {
							embeddedCodeWEB = rp.getJSONObject("_data").getString("embeddedCodeWEB");
						}
						if (rp.getJSONObject("_data").has("embeddedCodeWAP")) {
							embeddedCodeWAP = rp.getJSONObject("_data").getString("embeddedCodeWAP");
						}
						if (rp.getJSONObject("_data").has("refundPolicy")) {
							refundPolicy = rp.getJSONObject("_data").getString("refundPolicy");
						}
						if (rp.getJSONObject("_data").has("productExtraData") && rp.getJSONObject("_data")
								.getJSONArray("productExtraData").getJSONObject(0).has("lastApprovedCategories")) {
							lastApprovedCategories = rp.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0)
									.getString("lastApprovedCategories");
						}
		
						/***
						 * 
						 * Esto es para individual
						 * 
						 * 
						 ****/
						try {
							String charId = null;
							raw = rw.makeRequest("GET", "/list/Article/bySearch" + "?fields="
									+ java.net.URLEncoder.encode("Article.SupplierAID"
											+ ",ProductReference.ReferencedSupplierAid(\"" + itemId + "\")" + ",Article.SKU",
											"UTF-8")
									+ "&query=" + java.net.URLEncoder.encode("ProductReference.ReferencedSupplierAid(\""
											+ itemId + "\") equals \"" + itemId + "\"", "UTF-8"),
									null);
							org.json.JSONObject resp = new org.json.JSONObject(raw);
							upperRows = resp.getJSONArray("rows");
							productType = "00".equals(sapObjectType) && !"MKP".equals(business) ? "SalesItem"
									: "00".equals(sapObjectType) && "MKP".equals(business) ? "SalesItemFamilyMkt"
											: !"00".equals(sapObjectType) && "MKP".equals(business) ? "SalesItemFamilyMkt"
													: "SalesItemFamily";
							if (/* !"SalesItemFamilyMkt".equals(productType) && */ (sku == null || "".equals(sku)) && upperRows.length() == 1) {
								try {
									System.out.println("Hola " + sku + "||" + upperRows);
									sku = upperRows.getJSONObject(0).getJSONArray("values").getString(2);
								} catch (org.json.JSONException e) {
								}
								firstVariant = null;
							}
							for (int a = 0; a < upperRows.length(); a++) {
								try {
									firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
								} catch (org.json.JSONException e) {
								}
								raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant
										+ "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData",
										null);
								resp = new org.json.JSONObject(raw);
								resp = resp.getJSONObject("_data");
								if (!resp.has("_characteristicRecords")) {
									log("No characteristic records for: " + itemId);
									System.out.println("No characteristic records for: " + itemId);
									continue;
								}
								characteristicRecords = resp.getJSONArray("_characteristicRecords");
								org.json.JSONArray children = null;
								String[] chunk = null;
								for (int b = 0; b < characteristicRecords.length(); b++) {
									imageObject = characteristicRecords.getJSONObject(b);
									charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic")
											.getString("_code");
									if (("ProductImage" + (frozenImagesExists ? "2" : "")).equals(charId)) {
										if (imageObject.has("_children")) {
											children = imageObject.getJSONArray("_children");
											piKey = imageObject.getJSONObject("_qualification").getString("recordKey");
											for (int c = 0; c < children.length(); c++) {
												charId = children.getJSONObject(c).getJSONObject("_qualification")
														.getJSONObject("characteristic").getString("_code");
												if (("ProductImage_Name" + (frozenImagesExists ? "2" : "")).equals(charId)) {
													piName = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												} else if (("ProductImage_URL" + (frozenImagesExists ? "2" : ""))
														.equals(charId)) {
													piUrl = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												}
											}
										}
									} else if (("ProductImageDetail" + (frozenImagesExists ? "2" : "")).equals(charId)) {
										if (imageObject.has("_children")) {
											children = imageObject.getJSONArray("_children");
											chunk = new String[3];
											chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
											for (int c = 0; c < children.length(); c++) {
												charId = children.getJSONObject(c).getJSONObject("_qualification")
														.getJSONObject("characteristic").getString("_code");
												if (("ProductImageDetail_Name" + (frozenImagesExists ? "2" : ""))
														.equals(charId)) {
													chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												} else if (("ProductImageDetail_URL" + (frozenImagesExists ? "2" : ""))
														.equals(charId)) {
													chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												}
											}
											details.addLast(chunk);
										}
									} else if (("ProductImageSmosh" + (frozenImagesExists ? "2" : "")).equals(charId)) {
										if (imageObject.has("_children")) {
											children = imageObject.getJSONArray("_children");
											chunk = new String[3];
											chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
											for (int c = 0; c < children.length(); c++) {
												charId = children.getJSONObject(c).getJSONObject("_qualification")
														.getJSONObject("characteristic").getString("_code");
												if (("ProductImageSmosh_Name" + (frozenImagesExists ? "2" : ""))
														.equals(charId)) {
													chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												} else if (("ProductImageSmosh_URL" + (frozenImagesExists ? "2" : ""))
														.equals(charId)) {
													chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												}
											}
											smoshes.addLast(chunk);
										}
									} else if (("Illustration" + (frozenImagesExists ? "2" : "")).equals(charId)) {
										if (imageObject.has("_children")) {
											children = imageObject.getJSONArray("_children");
											chunk = new String[3];
											chunk[2] = imageObject.getJSONObject("_qualification").getString("recordKey");
											for (int c = 0; c < children.length(); c++) {
												charId = children.getJSONObject(c).getJSONObject("_qualification")
														.getJSONObject("characteristic").getString("_code");
												if (("Illustration_Name" + (frozenImagesExists ? "2" : "")).equals(charId)) {
													chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												} else if (("Illustration_URL" + (frozenImagesExists ? "2" : ""))
														.equals(charId)) {
													chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").getString(0);
												}
											}
											illustrations.addLast(chunk);
										}
									} else if ("SalesItem".equals(productType) && "TamanoUnicoSTD".equals(charId)) {
										tallaNormalizada = imageObject.getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getString(0);
									} else if ("SalesItem".equals(productType) && "TamanoUnico".equals(charId)) {
										tamanoUnico = imageObject.getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getJSONObject(0).getString("_label");
									} else if ("SalesItem".equals(productType) && "ColoursLiverpoolAtt".equals(charId)) {
										color = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
												.getJSONObject(0).getString("_label");
										codigoColor = imageObject.getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getJSONObject(0).getString("_code");
									}
								}
								if (piName != null && piUrl != null) {
									break;
								}
							}
							if (piName == null || piUrl == null) {
								log("No tenía imágenes2: " + proposalId);
								System.out.println("No tenía imágenes2 " + proposalId);
								reqPublishMessage.getJSONArray("rows")
										.put(new org.json.JSONObject()
												.put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@1"))
												.put("values", new org.json.JSONArray().put("Sin imágenes \"congeladas\"")));
								continue;
							}
						} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
							logE(e);
						}
						System.out.println("Sí habían imágenes");
						characteristicRecords = null;
						java.util.Set<String> atributosGeneralesQueSi = null;
						java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = null;
						String brandCode = null;
						propiedadesCaracteristicas = templateMetadataSet.get(template);
						atributosGeneralesQueSi = templateSets.get(template);
						if (propiedadesCaracteristicas == null) {
							propiedadesCaracteristicas = new java.util.TreeMap<>();
							atributosGeneralesQueSi = new java.util.TreeSet<>();
							templateSets.put(template, atributosGeneralesQueSi);
							atributosGeneralesQueSi.addAll(globalSet);
							templateMetadataSet.put(template, propiedadesCaracteristicas);
							for (java.util.Map.Entry<String, org.json.JSONObject> globalPropertiesEntry : globalProperties.entrySet()) {
								propiedadesCaracteristicas.put(globalPropertiesEntry.getKey(), globalPropertiesEntry.getValue());
							}
							log("Going to request Propiedades Características: ");
							java.util.Map<String, org.json.JSONObject> propiedadesDePlantilla =
									dastub.getTemplateCharacteristicProperties(template);
							propiedadesCaracteristicas.putAll(propiedadesDePlantilla);
							for (java.util.Map.Entry<String, org.json.JSONObject> templateEntry : propiedadesDePlantilla.entrySet()) {
								org.json.JSONObject propiedades = templateEntry.getValue();
								if (propiedades != null && "Y".equals(propiedades.optString("RelevantForATG"))) {
									atributosGeneralesQueSi.add(templateEntry.getKey());
								}
							}
							templateStructureGroupAttributeValues.put(template,
									dastub.getTemplateStructureGroupAttributeValues(template, 10));
						}
											Element product = null;
		
						product = doc.createElement("Product"); // <Product></Product>
						product.setAttribute("ID", productsToTestWith[0]);
						product.setAttribute("UserTypeID", productType);
						product.setAttribute("ParentID", template);
						product.setAttribute("Changed", "true");
						productos.add(proposalId);
						/*********************
						 * El atributo en las entidades Changed="true", tiene el efecto de que broker
						 * ignorda todo lo que no tenga Changed="true".
						 ****************************************************************/
						Element name = doc.createElement("Name");
						name.setAttribute("Changed", "true");
						product.appendChild(name);
		
						Element keyValueSKU = null;
						Element keyValueEAN = null;
		
						Element attributeValues = null;
		
						String charId = null;
						org.json.JSONObject characteristic = null;
		
						boolean behvo = false;
						String almacenamientoAtt = null;
						String skuType = null;
						String mtart = null;
		
						String pt = null;
						String ptl = null;
		
						Element helperElement = null;
						Element prevHelperElement = null;
						java.util.Map<String, Element> tableroDeControl = new java.util.TreeMap<>();
						if (webCategory != null) {
		
							if (lastApprovedCategories != null && !"".equals(lastApprovedCategories)
									&& webCategory.length > 0) {
								java.util.List<String> refCats = java.util.Arrays.asList(lastApprovedCategories.split(","));
								java.util.List<String> webCatList = java.util.Arrays.asList(webCategory);
								for (String refCat : refCats) {
									if (!webCatList.contains(refCat)) {
										Element classificationReference = null;
										classificationReference = doc.createElement("DeleteClassificationReference");
										classificationReference.setAttribute("ClassificationID", refCat);
										classificationReference.setAttribute("Type", "WebsiteLink");
										product.appendChild(classificationReference);
									}
								}
							}
							StringBuilder sb = new StringBuilder();
							for (String element : webCategory) {
								sb.append(sb.length() == 0 ? "" : ",").append(element);
								hierarchyHelper = globalMap.get(element);
								if (hierarchyHelper != null) {
									entry = hierarchyHelper.get(element);
									entryHelper = entry;
									helperElement = pacheleWeb(entryHelper, doc);
									if (!tableroDeControl.containsKey(entryHelper.getString("identifier"))) {
										tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
									}
									while (entryHelper.has("parentIdentifier")
											&& !"".equals(entryHelper.get("parentIdentifier"))
											&& !tableroDeControl.containsKey(entryHelper.getString("parentIdentifier"))) {
										prevHelperElement = helperElement;
										entryHelper = hierarchyHelper.get(entryHelper.getString("parentIdentifier"));
										helperElement = pacheleWeb(entryHelper, doc);
										if (helperElement == null) {
											log("PANIC: No element could be made from: " + entryHelper);
											break;
										}
										helperElement.appendChild(prevHelperElement);
										tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
									}
								} else {
									log("Category not found... " + element);
								}
								Element classificationReference = null;
								classificationReference = doc.createElement("ClassificationReference");
								classificationReference.setAttribute("ClassificationID", element);
								classificationReference.setAttribute("Type", "WebsiteLink");
								classificationReference.setAttribute("Changed", "true");
								product.appendChild(classificationReference);
							}
							if (sendIt)
								reqLastApprovedCategories
										.getJSONArray(
												"rows")
										.put(new org.json.JSONObject()
												.put("object", new org.json.JSONObject().put("id", "'" + proposalId + "'@1"))
												.put("values", new org.json.JSONArray().put(sb.toString())));
						}
		
						for (org.json.JSONObject laRaiz : rescataLaRaiz) {
							helperElement = tableroDeControl.get(laRaiz.getString("identifier"));
							if (helperElement != null) {
								webHierarchyRoot.appendChild(helperElement);
							}
						}
		
						java.util.ArrayList<String> unosQueQuiero = new java.util.ArrayList<>(YEA);
						java.util.Map<String, org.json.JSONObject> heredables = new java.util.TreeMap<>();
		
						attributeValues = doc.createElement("Values");
						product.appendChild(attributeValues);
						if ("SalesItem".equals(productType) && piName != null && piUrl != null && piKey != null) {
							appendMediaAsset(piName, piUrl, "PrimaryProductImage", // String assetType,
									piKey, "Imagen Producto", // String assetValueTextContent,
									"ImageURL", // String assetValueAttributeId,
									"ProductImage", // String assetUserTypeId,
									"ProductImage", // String assetKeyPrefix,
									itemId, characteristic, "ProductImage", // String baseAssetTypeName,
									assetMap, assetReferencesMap, product, assets, doc, firstVariant);
						}
						if ("SalesItem".equals(productType) && details != null && !details.isEmpty()) {
							for (String[] dt : details) {
								appendMediaAsset(dt[0], dt[1], "ProductImage", // String assetType,
										dt[2], "Imagen Detalle Producto", // String assetValueTextContent,
										"ImageURL", // String assetValueAttributeId,
										"ProductImageDetail", // String assetUserTypeId,
										"ProductImageDetail", // String assetKeyPrefix,
										itemId, characteristic, "ProductImageDetail", // String baseAssetTypeName,
										assetMap, assetReferencesMap, product, assets, doc, firstVariant);
							}
						}
						if ("SalesItem".equals(productType) && smoshes != null && !smoshes.isEmpty()) {
							for (String[] dt : smoshes) {
								appendMediaAsset(dt[0], dt[1], "ProductImageSmosh", // String assetType,
										dt[2], "Imagen Smosh Producto", // String assetValueTextContent,
										"ImageURL", // String assetValueAttributeId,
										"ProductImageSmosh", // String assetUserTypeId,
										"SmoshImg", // String assetKeyPrefix,
										itemId, characteristic, "ProductImageDetail", // String baseAssetTypeName,
										assetMap, assetReferencesMap, product, assets, doc, firstVariant);
							}
						}
						if ("SalesItem".equals(productType) && illustrations != null && !illustrations.isEmpty()) {
							for (String[] dt : illustrations) {
								appendMediaAsset(dt[0], dt[1], "Illustration", // String assetType,
										dt[2], "Imagen Isométrica del Producto", // String assetValueTextContent,
										"ImageURL", // String assetValueAttributeId,
										"Illustration", // String assetUserTypeId,
										"Illustration", // String assetKeyPrefix,
										itemId, characteristic, "Illustration", // String baseAssetTypeName,
										assetMap, assetReferencesMap, product, assets, doc, firstVariant);
							}
						}
						if ("SalesItem".equals(productType) && tallaNormalizada != null && !"".equals(tallaNormalizada)) {
							appendPlainElementValue(tallaNormalizada, null, "TC-NormalizedSize", attributeValues, attributes,
									doc, propiedadesCaracteristicas, atgGroups);
						}
						if ("SalesItem".equals(productType) && color != null && !"".equals(color)) {
							appendPlainElementValue(color, codigoColor, "ColoursLiverpoolAtt", attributeValues, attributes, doc,
									propiedadesCaracteristicas, atgGroups);
						}
		
						java.util.Map<String, String> structureGroupAttributeValues =
								templateStructureGroupAttributeValues.get(template);
						if (structureGroupAttributeValues == null) {
							structureGroupAttributeValues =
									dastub.getTemplateStructureGroupAttributeValues(template, 10);
							templateStructureGroupAttributeValues.put(template, structureGroupAttributeValues);
						}
	
						String[] structureGroupAttributeNames = new String[] {
								"DisplayGroupOrder",
								"DisplayOrder",
								"ConfigurableOrder"
						};
						for (String attributeName : structureGroupAttributeNames) {
							if (!structureGroupAttributeValues.containsKey(attributeName)) {
								continue;
							}
							appendPlainElementValue(
									structureGroupAttributeValues.get(attributeName),
									null,
									attributeName,
									attributeValues,
									attributes,
									doc,
									propiedadesCaracteristicas,
									atgGroups);
						}
											if (descLong != null) {
							appendPlainElementValue(descLong, null, "DescriptionLong", attributeValues, attributes, doc,
									propiedadesCaracteristicas, atgGroups);
						}
						if (descLong2 != null) {
							appendPlainElementValue(descLong2, null, "DescriptionLong2", attributeValues, attributes, doc,
									propiedadesCaracteristicas, atgGroups);
						}
		
						if (business != null) {
							if ("MKP".equals(business)) {
								appendPlainElementValue("true", "1", "isMarketPlace", attributeValues, attributes, doc,
										propiedadesCaracteristicas, atgGroups);
							}
						}
						for (int i = 0; i < characteristicArray.length(); i++) {
							characteristic = characteristicArray.getJSONObject(i);
							charId = characteristic.getJSONObject("_qualification").getJSONObject("characteristic")
									.getString("_code");
							if("clothingSize".equals(charId)) {
								clothingSize = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}else if("sizeVaD".equals(charId)) {
								sizeVaD = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}
							if ("Business".equals(charId)) {
								if (business == null || "".equals(business)) {
									business = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
								}
							} else if ("Direction".equals(charId)) {
								direccion = direccion == null || "".equals(direccion)
										? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
												.getJSONObject(0).getString("_code")
										: direccion;
							} else if ("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
								String eanval = (treatment(characteristic.getJSONArray("_recordLang").getJSONObject(0)
										.getJSONArray("values").getString(0)));
								if ((mainBarCode == null || "".equals(mainBarCode)) && !"".equals(eanval)) {
									mainBarCode = eanval;
								}
							} else if ("SupplierPartNumber".equals(charId)) {
								supplierPartNumber = supplierPartNumber != null && !"".equals(supplierPartNumber)
										? supplierPartNumber
										: characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
												.getString(0);
							} else if ("SupplierID".equals(charId)) {
								supplierID = supplierID != null && !"".equals(supplierID) ? supplierID : characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							} else if ("SKU".equals(charId)) {
								String skuval = treatment(characteristic.getJSONArray("_recordLang").getJSONObject(0)
										.getJSONArray("values").getString(0));
								if ((sku == null || "".equals(sku)) && !"".equals(skuval)) {
									sku = skuval;
								} else {
									System.out.println("No change on it. (sku)");
								}
							} else if ("ProductVideo2".equals(charId)) {
								if (characteristic.has("_children")) {
									org.json.JSONArray children = characteristic.getJSONArray("_children");
									String[] chunk = new String[3];
									chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
									for (int c = 0; c < children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification")
												.getJSONObject("characteristic").getString("_code");
										if (("ProductVideo_Name" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0);
										} else if (("ProductVideo_URL" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0);
										}
									}
									appendPlainElementValue(chunk[1], null, "Video", attributeValues, attributes, doc,
											propiedadesCaracteristicas, atgGroups);
								}
							} else if ("OwnersManual2".equals(charId)) {
								if (characteristic.has("_children")) {
									org.json.JSONArray children = characteristic.getJSONArray("_children");
									String[] chunk = new String[3];
									chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
									for (int c = 0; c < children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification")
												.getJSONObject("characteristic").getString("_code");
										if (("OwnersManual_Name" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0);
										} else if (("OwnersManual_URL" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0);
										}
									}
									appendMediaAsset(chunk[0], chunk[1], "OwnersManual", // String assetType,
											chunk[2], "Manual de Propietario", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"OwnersManual", // String assetUserTypeId,
											"OwnersManual", // String assetKeyPrefix,
											itemId, characteristic, "OwnersManual", // String baseAssetTypeName,
											assetMap, assetReferencesMap, product, assets, doc, productsToTestWith[0]);
								}
							} else if ("NOM2".equals(charId)) {
								if (characteristic.has("_children")) {
									org.json.JSONArray children = characteristic.getJSONArray("_children");
									String[] chunk = new String[3];
									chunk[2] = characteristic.getJSONObject("_qualification").getString("recordKey");
									for (int c = 0; c < children.length(); c++) {
										charId = children.getJSONObject(c).getJSONObject("_qualification")
												.getJSONObject("characteristic").getString("_code");
										if (("NOM_Name" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0);
										} else if (("NOM_URL" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0);
										}
									}
									appendMediaAsset(chunk[0], chunk[1], "NOM", // String assetType,
											chunk[2], "NOM", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"NOM", // String assetUserTypeId,
											"NOM", // String assetKeyPrefix,
											itemId, characteristic, "OwnersManual", // String baseAssetTypeName,
											assetMap, assetReferencesMap, product, assets, doc, productsToTestWith[0]);
								}
							} else {
								if ("ProductName".equals(charId)) {
									productName = productName != null && !"".equals(productName) ? productName
											: (productName = characteristic.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0));
								} else if ("Name".equals(charId)) {
									charactName = characteristic.getJSONArray("_recordLang").getJSONObject(0)
											.getJSONArray("values").getString(0);
								} else if ("ItemGroupS4H".equals(charId) || "ItemGroup".equals(charId)) {
									if ("ItemGroupS4H".equals(charId)) {
										itemGroupS4H = itemGroupS4H == null || "".equals(itemGroupS4H)
												? characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_code")
												: itemGroupS4H;
										itemGroupS4HLabel = itemGroupS4HLabel == null || "".equals(itemGroupS4HLabel)
												? characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_label")
												: itemGroupS4HLabel;
									} else if ("ItemGroup".equals(charId)) {
										itemGroup = itemGroup == null || "".equals(itemGroup)
												? characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_code")
												: itemGroup;
										itemGroupLabel = itemGroupLabel == null || "".equals(itemGroupLabel)
												? characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_label")
												: itemGroupLabel;
									}
								} else if ("BrandName".equals(charId) || "BRAND_ID_S4H".equals(charId)) {
									brandName = brandName == null || "".equals(brandName)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
													.getJSONObject(0).getString("_code")
											: brandName;
									brandNameLabel = brandNameLabel == null || "".equals(brandNameLabel)
											? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
													.getJSONObject(0).getString("_label")
											: brandNameLabel;
								} else if ("ProductType".equals(charId)) {
									pt = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
											.getJSONObject(0).getString("_code");
									ptl = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
											.getJSONObject(0).getString("_label");
									if ("1".equals(pt)) {
										lst = dataMap.get("ItemGroupS4H");
										if (lst != null) {
											itemGroupS4H = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_code");
										}
										lst = dataMap.get("AlmacenamientoAtt");
										if (lst != null) {
											almacenamientoAtt = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_code");
										}
										lst = dataMap.get("MTART_S4H");
										if (lst != null) {
											mtart = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_code");
										}
										lst = dataMap.get("SkuType");
										if (lst != null) {
											skuType = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getJSONObject(0).getString("_code");
										}
										if ("SERV".equals(skuType) && "0001".equals(almacenamientoAtt)) {
											pt = "6";
											ptl = "Digital";
											appendPlainElementValue("Digital", pt, "ProductType", attributeValues, attributes,
													doc, propiedadesCaracteristicas, atgGroups);
										} else if ("DIEN".equals(mtart) && "SB87516".equals(itemGroupS4H)) {
											pt = "6";
											ptl = "Digital";
											appendPlainElementValue("Digital", pt, "ProductType", attributeValues, attributes,
													doc, propiedadesCaracteristicas, atgGroups);
		
										} else {
											appendPlainElementValue(
													characteristic.getJSONArray("_recordLang").getJSONObject(0)
															.getJSONArray("values").getJSONObject(0).getString("_label"),
													characteristic.getJSONArray("_recordLang").getJSONObject(0)
															.getJSONArray("values").getJSONObject(0).getString("_code"),
													"ProductType", attributeValues, attributes, doc, propiedadesCaracteristicas,
													atgGroups);
										}
									} else {
										appendPlainElementValue(
												characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_label"),
												characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).getString("_code"),
												"ProductType", attributeValues, attributes, doc, propiedadesCaracteristicas,
												atgGroups);
									}
								} else {
									if ("ItemGroup2".equals(charId)) {
										continue;
									} else {
										if (atributosGeneralesQueSi.contains(charId)) {
											if ("Section".equals(charId) && (seccion == null || "".equals(seccion))) {
												appendPlainElementValue(
														characteristic.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_label"),
														characteristic.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_code"),
														"Section", attributeValues, attributes, doc, propiedadesCaracteristicas,
														atgGroups);
											} else if ("Direction".equals(charId)
													&& (direccion == null || "".equals(direccion))) {
												appendPlainElementValue(
														characteristic.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_label"),
														characteristic.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_code"),
														"Direction", attributeValues, attributes, doc,
														propiedadesCaracteristicas, atgGroups);
											} else {
												if (unosQueQuiero.contains(charId)) {
													heredables.put(charId, characteristic);
												}
												if ("LOOKUP".equals(characteristic.getString("_datatype"))) {
													boolean skip = false;
													if("Direction".equals(charId) && direccion != null && !"".equals(direccion)) {
														skip = true;
													}
													if("Section".equals(charId) && seccion != null && !"".equals(seccion)) {
														skip = true;
													}
													if("ItemGroup".equals(charId) && itemGroup != null && !"".equals(itemGroup)) {
														skip = true;
													}
													if("ItemGroupS4H".equals(charId) && itemGroupS4H != null && !"".equals(itemGroupS4H)) {
														skip = true;
													}
													if("BrandName".equals(charId) && brandName != null && !"".equals(brandName)) {
														skip = true;
													}
													if("BRAND_ID_S4H".equals(charId) && brandIdS4H != null && !"".equals(brandIdS4H)) {
														skip = true;
													}
													if(!skip) {
														appendPlainElementValue(
																characteristic.getJSONArray("_recordLang").getJSONObject(0)
																		.getJSONArray("values").getJSONObject(0)
																		.getString("_label"),
																characteristic.getJSONArray("_recordLang").getJSONObject(0)
																		.getJSONArray("values").getJSONObject(0).getString("_code"),
																charId, attributeValues, attributes, doc,
																propiedadesCaracteristicas, atgGroups);
													}
												} else if (!"NONE".equals(characteristic.getString("_datatype"))) {
													boolean skip = false;
													if("SupplierPartNumber".equals(charId) && supplierPartNumber != null && !"".equals(supplierPartNumber)) {
														skip = true;
													}
													if("SupplierID".equals(charId) && supplierID != null && !"".equals(supplierID)) {
														skip = true;
													}
													if(!skip) {
														java.util.LinkedList<String> vals = new java.util.LinkedList<>();
														for (int m = 0; m < characteristic.getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").length(); m++) {
															vals.addLast(
																	String.valueOf(parseDateForSpecificDateFields(
																			characteristic.getJSONArray("_recordLang")
																					.getJSONObject(0).getJSONArray("values").get(m),
																			charId)));
														}
														appendPlainElementValue(String.join(",", vals), null, charId,
																attributeValues, attributes, doc, propiedadesCaracteristicas,
																atgGroups);
													}
												}
											}
										}
									}
								}
							}
						}
						if (embeddedCodeWEB != null && !"".equals(embeddedCodeWEB)) {
							appendPlainElementValue(embeddedCodeWEB, null, "EmbedCodeWEB", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						}
						if (embeddedCodeWAP != null && !"".equals(embeddedCodeWAP)) {
							appendPlainElementValue(embeddedCodeWAP, null, "EmbedCodeWAP", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						}
						if (refundPolicy != null && !"".equals(refundPolicy)) {
							appendPlainElementValue(refundPolicy, null, "refundPolicy", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						}
						if (seccion != null && !"".equals(seccion)) {
							appendPlainElementValue(seccionLabel, seccion, "Section", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						}
						if (direccion != null && !"".equals(direccion)) {
							appendPlainElementValue(direccionLabel, direccion, "Direction", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						}
						if (supplierPartNumber != null && !"".equals(supplierPartNumber)) {
							appendPlainElementValue(supplierPartNumber, "", "SupplierPartNumber", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						}
						if (supplierID != null && !"".equals(supplierID)) {
							appendPlainElementValue(supplierIDLabel, supplierID, "SupplierID", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						}
		
						if (!behvo) {
							String elese = "SBB".equals(business) ? itemGroupS4H : itemGroup;
							String dictionary = !"SBB".equals(business)
									? "GpoArtVsEnvase"
									: "GpoArtVsEnvase_S4H";
							log("Querying dictionary: " + dictionary);
							String laetiqueta = queryDictionary(elese, dictionary);
							if (laetiqueta != null && !laetiqueta.isBlank()) {
								String elcode = dastub.getLookupValueCodeByName(
										"SAP_BEHVOLOV", 10, laetiqueta, true);
								if (elcode != null && !elcode.isBlank()) {
									appendPlainElementValue(laetiqueta, elcode, "SAP_BEHVO",
											attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
									behvo = true;
								}
							}
						}
											appendPlainElementValue(itemGroup != null && !"".equals(itemGroup) ? itemGroupLabel : itemGroupS4HLabel,
								itemGroup != null && !"".equals(itemGroup) ? itemGroup : itemGroupS4H, "ItemGroup2",
								attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						appendPlainElementValue(!"SBB".equals(business) ? itemGroupLabel : itemGroupS4HLabel,
								!"SBB".equals(business) ? itemGroup : itemGroupS4H,
								!"SBB".equals(business) ? "ItemGroup" : "ItemGroupS4H", attributeValues, attributes, doc,
								propiedadesCaracteristicas, atgGroups);
						appendPlainElementValue(!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel,
								!"SBB".equals(business) ? brandName : brandIdS4H,
								!"SBB".equals(business) ? "BrandName" : "BRAND_ID_S4H", attributeValues, attributes, doc,
								propiedadesCaracteristicas, atgGroups);
						appendPlainElementValue(!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel, null,
								"BrandNameATG", attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
						appendPlainElementValue(!"SBB".equals(business) ? brandNameLabel : brandIdS4HLabel, null, "BrandIDATG",
								attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
		
						if (productName != null && !"".equals(productName)) {
							name.setTextContent(productName);
							appendPlainElementValue(productName, null, "ProductName", attributeValues, attributes, doc,
									propiedadesCaracteristicas, atgGroups);
						} else {
							if (charactName != null && !"".equals(charactName)) {
								name.setTextContent(charactName);
								appendPlainElementValue(charactName, null, "ProductName", attributeValues, attributes, doc,
										propiedadesCaracteristicas, atgGroups);
							} else {
								if (nameLang != null && !"".equals(nameLang)) {
									name.setTextContent(nameLang);
									appendPlainElementValue(nameLang, null, "ProductName", attributeValues, attributes, doc,
											propiedadesCaracteristicas, atgGroups);
								} else {
									log("Sin product neim, no será posible publicar.");
									System.out.println("Sin product neim, no será posible publicar.");
									reqPublishMessage
											.getJSONArray("rows").put(
													new org.json.JSONObject()
															.put("object",
																	new org.json.JSONObject().put("id",
																			"'" + proposalId + "'@1"))
															.put("values", new org.json.JSONArray().put("Sin ProductName")));
									continue;
								}
							}
						}
		
						if (pt == null) {
							lst = dataMap.get("AlmacenamientoAtt");
							if (lst != null) {
								almacenamientoAtt = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0)
										.getJSONArray("values").getJSONObject(0).getString("_code");
							}
							lst = dataMap.get("MTART_S4H");
							if (lst != null) {
								mtart = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
										.getJSONObject(0).getString("_code");
							}
							lst = dataMap.get("SkuType");
							if (lst != null) {
								skuType = lst.getFirst().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
										.getJSONObject(0).getString("_code");
							}
							if ("SERV".equals(skuType) && "0001".equals(almacenamientoAtt)) {
								pt = "6";
								ptl = "Digital";
								appendPlainElementValue("Digital", pt, "ProductType", attributeValues, attributes, doc,
										propiedadesCaracteristicas, atgGroups);
							} else if ("DIEN".equals(mtart) && "SB87516".equals(itemGroupS4H)) {
								pt = "6";
								ptl = "Digital";
								appendPlainElementValue("Digital", pt, "ProductType", attributeValues, attributes, doc,
										propiedadesCaracteristicas, atgGroups);
		
							} else {
								pt = "1";
								ptl = "Soft line";
								appendPlainElementValue("Soft line", pt, "ProductType", attributeValues, attributes, doc,
										propiedadesCaracteristicas, atgGroups);
							}
						}
						if (unosQueQuiero.contains("ProductType") && !heredables.containsKey("ProductType")) {
							heredables.put("ProductType",
									new org.json.JSONObject().put("_datatype", "LOOKUP")
											.put("_qualification",
													new org.json.JSONObject().put(
															"characteristic",
															new org.json.JSONObject().put("_code", "ProductType")))
											.put("_recordLang", new org.json.JSONArray()
													.put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(
															new org.json.JSONObject().put("_code", pt).put("_label", ptl))))));
						}
						if (unosQueQuiero.contains("SupplierPartNumber") && !heredables.containsKey("SupplierPartNumber")
								&& !"".equals(supplierPartNumber) && supplierPartNumber != null) {
							heredables.put("SupplierPartNumber",
									new org.json.JSONObject().put("_datatype", "TEXT")
											.put("_qualification",
													new org.json.JSONObject().put("characteristic",
															new org.json.JSONObject().put("_code", "ProductType")))
											.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject()
													.put("values", new org.json.JSONArray().put(supplierPartNumber)))));
						}
		
						if ("SalesItem".equals(productType) && tamanoUnico != null && !"".equals(tamanoUnico)) {
							talla(clothingSize == null ? sizeVaD : clothingSize, tamanoUnico, business, itemGroup, template, direccion, brandCode, attributeValues, attributes,
									doc, propiedadesCaracteristicas, atgGroups);
							appendPlainElementValue(tamanoUnico, null, "TamanoUnico", attributeValues, attributes, doc,
									propiedadesCaracteristicas, atgGroups);
						}
		
						if (productType.startsWith("SalesItemFamily")) {
							org.json.JSONObject resp = null;
							Element subProduct = null;
							Element subAttributeValues = null;
							Element varName = null;
							String childSAPObjectType = null;
							String childSAPObjectTypeLabel = null;
							java.util.LinkedList<java.util.LinkedList<String[]>> losdetalles = new java.util.LinkedList<>();
							java.util.LinkedList<java.util.LinkedList<String[]>> losesmoshes = new java.util.LinkedList<>();
							java.util.LinkedList<java.util.LinkedList<String[]>> lasilustraciones = new java.util.LinkedList<>();
							boolean theFirstTime = true;
							org.json.JSONObject characteristicObject = null;
							for (int a = 0; a < upperRows.length(); a++) {
								tallaNormalizada = null;
								tamanoUnico = null;
								color = null;
								codigoColor = null;
								childSAPObjectType = null;
								childSAPObjectTypeLabel = null;
								try {
									firstVariant = upperRows.getJSONObject(a).getJSONArray("values").getString(0);
								} catch (org.json.JSONException e) {
								}
								procede = true;
								subProduct = doc.createElement("Product");
								subProduct.setAttribute("ID", firstVariant);
								subProduct.setAttribute("UserTypeID",
										"SalesItemFamily".equals(productType) ? "SalesItemVariant" : "SalesItem");
								subProduct.setAttribute("ParentID", productsToTestWith[0]);
								subProduct.setAttribute("Changed", "true");
								subAttributeValues = doc.createElement("Values");
								varName = doc.createElement("Name");
								subProduct.appendChild(varName);
								subProduct.appendChild(subAttributeValues);
								if ("MKP".equals(business)) {
									appendPlainElementValue("true", "1", "isMarketPlace", subAttributeValues, attributes, doc,
											propiedadesCaracteristicas, atgGroups);
								}
		
								details = new java.util.LinkedList<>();
								smoshes = new java.util.LinkedList<>();
								illustrations = new java.util.LinkedList<>();
								try {
									raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant
											+ "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData",
											null);
									resp = new org.json.JSONObject(raw);
									resp = resp.getJSONObject("_data");
									if (!resp.has("_characteristicRecords")) {
										log("Variante no tenía características");
										System.out.println("Variante no tenía características");
										continue;
									}
									characteristicRecords = resp.getJSONArray("_characteristicRecords");
									org.json.JSONArray children = null;
									String[] chunk = null;
									String sku0 = resp.has("sku") ? String.valueOf(resp.getLong("sku")) : null;
									String ean0 = resp.has("gtin") ? resp.getString("gtin") : null;
									codigoColor = resp.has("articleExtraData")
											&& resp.getJSONArray("articleExtraData").getJSONObject(0).has("coloursLiverpoolAtt")
													? resp.getJSONArray("articleExtraData").getJSONObject(0)
															.getJSONObject("coloursLiverpoolAtt").getString("_code")
													: null;
									color = resp.has("articleExtraData")
											&& resp.getJSONArray("articleExtraData").getJSONObject(0).has("coloursLiverpoolAtt")
													? resp.getJSONArray("articleExtraData").getJSONObject(0)
															.getJSONObject("coloursLiverpoolAtt").getString("_label")
													: null;
									tamanoUnico = resp.has("articleExtraData")
											&& resp.getJSONArray("articleExtraData").getJSONObject(0).has("tamanoUnico")
													? resp.getJSONArray("articleExtraData").getJSONObject(0)
															.getJSONObject("tamanoUnico").getString("_code")
													: null;
									log("Came to here, color: " + color + ", tamanoUnico: " + tamanoUnico);
									java.util.List<String> misaidis = new java.util.ArrayList<>();
									for (int b = 0; b < characteristicRecords.length(); b++) {
										characteristicObject = characteristicRecords.getJSONObject(b);
										charId = characteristicObject.getJSONObject("_qualification")
												.getJSONObject("characteristic").getString("_code");
										misaidis.add(charId);
										if("clothingSize".equals(charId)) {
											clothingSize = characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}else if("sizeVaD".equals(charId)) {
											sizeVaD = characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
										}
										if ("MainBarCode".equals(charId) || "MainBarCodeS4H".equals(charId)) {
											ean0 = ean0 == null || "".equals(ean0)
													? characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
															.getJSONArray("values").getString(0)
													: ean0;
										} else if ("SKU".equals(charId)) {
											sku0 = sku0 == null || "".equals(sku0)
													? characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
															.getJSONArray("values").getString(0)
													: sku0;
										} else if (("ProductImage" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											if (characteristicObject.has("_children")) {
												children = characteristicObject.getJSONArray("_children");
												piKey = characteristicObject.getJSONObject("_qualification")
														.getString("recordKey");
												for (int c = 0; c < children.length(); c++) {
													charId = children.getJSONObject(c).getJSONObject("_qualification")
															.getJSONObject("characteristic").getString("_code");
													if (("ProductImage_Name" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														piName = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													} else if (("ProductImage_URL" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														piUrl = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													}
												}
											}
										} else if (("ProductImageDetail" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											if (characteristicObject.has("_children")) {
												children = characteristicObject.getJSONArray("_children");
												chunk = new String[4];
												chunk[2] = characteristicObject.getJSONObject("_qualification")
														.getString("recordKey");
												for (int c = 0; c < children.length(); c++) {
													charId = children.getJSONObject(c).getJSONObject("_qualification")
															.getJSONObject("characteristic").getString("_code");
													if (("ProductImageDetail_Name" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													} else if (("ProductImageDetail_URL" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													}
												}
												chunk[3] = firstVariant;
												details.addLast(chunk);
												if (theFirstTime)
													losdetalles.addLast(details);
											}
										} else if (("ProductImageSmosh" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											if (characteristicObject.has("_children")) {
												children = characteristicObject.getJSONArray("_children");
												chunk = new String[4];
												chunk[2] = characteristicObject.getJSONObject("_qualification")
														.getString("recordKey");
												for (int c = 0; c < children.length(); c++) {
													charId = children.getJSONObject(c).getJSONObject("_qualification")
															.getJSONObject("characteristic").getString("_code");
													if (("ProductImageSmosh_Name" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													} else if (("ProductImageSmosh_URL" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													}
												}
												chunk[3] = firstVariant;
												smoshes.addLast(chunk);
												if (theFirstTime)
													losesmoshes.addLast(smoshes);
											}
										} else if (("Illustration" + (frozenImagesExists ? "2" : "")).equals(charId)) {
											if (characteristicObject.has("_children")) {
												children = characteristicObject.getJSONArray("_children");
												chunk = new String[4];
												chunk[2] = characteristicObject.getJSONObject("_qualification")
														.getString("recordKey");
												for (int c = 0; c < children.length(); c++) {
													charId = children.getJSONObject(c).getJSONObject("_qualification")
															.getJSONObject("characteristic").getString("_code");
													if (("Illustration_Name" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														chunk[0] = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													} else if (("Illustration_URL" + (frozenImagesExists ? "2" : ""))
															.equals(charId)) {
														chunk[1] = children.getJSONObject(c).getJSONArray("_recordLang")
																.getJSONObject(0).getJSONArray("values").getString(0);
													}
												}
												chunk[3] = firstVariant;
												illustrations.addLast(chunk);
												if (theFirstTime)
													lasilustraciones.addLast(illustrations);
											}
										} else if ("TamanoUnicoSTD".equals(charId)) {
											tallaNormalizada = characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getString(0);
										} else if ("TamanoUnico".equals(charId)) {
											tamanoUnico = tamanoUnico != null ? tamanoUnico
													: characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
															.getJSONArray("values").getJSONObject(0).getString("_label");
										} else if ("ColoursLiverpoolAtt".equals(charId)) {
											color = color != null ? color
													: characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
															.getJSONArray("values").getJSONObject(0).getString("_label");
											codigoColor = codigoColor != null ? codigoColor
													: characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
															.getJSONArray("values").getJSONObject(0).getString("_code");
										} else if ("SAPObjectType".equals(charId)) {
											childSAPObjectTypeLabel = characteristicObject.getJSONArray("_recordLang")
													.getJSONObject(0).getJSONArray("values").getJSONObject(0)
													.getString("_label");
											childSAPObjectType = characteristicObject.getJSONArray("_recordLang")
													.getJSONObject(0).getJSONArray("values").getJSONObject(0)
													.getString("_code");
										} else if ("ProcedeNoProcede".equals(charId)) {
											procede = characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
													.getJSONArray("values").getBoolean(0);
										} else {
											if (atributosGeneralesQueSi.contains(charId)) {
												if ("LOOKUP".equals(characteristicObject.getString("_datatype"))) {
													appendPlainElementValue(
															characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
																	.getJSONArray("values").getJSONObject(0)
																	.getString("_label"),
															characteristicObject.getJSONArray("_recordLang").getJSONObject(0)
																	.getJSONArray("values").getJSONObject(0).getString("_code"),
															charId, subAttributeValues, attributes, doc,
															propiedadesCaracteristicas, atgGroups);
												} else if (!"NONE".equals(characteristicObject.getString("_datatype"))) {
													java.util.LinkedList<String> vals = new java.util.LinkedList<>();
													for (int m = 0; m < characteristicObject.getJSONArray("_recordLang")
															.getJSONObject(0).getJSONArray("values").length(); m++) {
														vals.addLast(String.valueOf(parseDateForSpecificDateFields(
																characteristicObject.getJSONArray("_recordLang")
																		.getJSONObject(0).getJSONArray("values").get(m),
																charId)));
													}
													appendPlainElementValue(String.join(",", vals), null, charId,
															subAttributeValues, attributes, doc, propiedadesCaracteristicas,
															atgGroups);
												}
											}
										}
									}
									System.out.println("VS " + sku0 + " VS " + sku);
									if (sku0 == null || "".equals(sku0)) {
										sku0 = sku;
									}
									if (sku0 != null && !"".equals(sku0)) {
										if (sku == null || "".equals(sku)) {
											sku = sku0;
										}
										keyValueSKU = doc.createElement("KeyValue");
										keyValueSKU.setAttribute("KeyID", "SKUID");
										keyValueSKU.setTextContent(sku0);
										subProduct.appendChild(keyValueSKU);
										artToSKU.put(firstVariant, sku0);
										appendPlainElementValue(sku0, null, "SKU", subAttributeValues, attributes, doc,
												propiedadesCaracteristicas, atgGroups);
									}
									System.out.println("VS 2 " + sku0 + " VS 2 " + sku);
									if (ean0 == null || "".equals(ean0)) {
										ean0 = mainBarCode;
									}
									if (ean0 != null && !"".equals(ean0)) {
										if (mainBarCode == null || "".equals(mainBarCode)) {
											mainBarCode = ean0;
										}
										keyValueEAN = doc.createElement("KeyValue");
										keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
										keyValueEAN.setTextContent(ean0);
										subProduct.appendChild(keyValueEAN);
										appendPlainElementValue(ean0, null,
												"SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode", subAttributeValues,
												attributes, doc, propiedadesCaracteristicas, atgGroups);
									}
									if (!procede) {
										procede = resp.has("procedeNoProcede") && resp.getBoolean("procedeNoProcede");
									}
									log("El procede: " + procede);
									if (!procede) {
										continue;
									}
									if (!misaidis.contains("ProductName") && "".equals(name.getTextContent())) {
										reqPublishMessage
												.getJSONArray("rows").put(
														new org.json.JSONObject()
																.put("object",
																		new org.json.JSONObject().put("id",
																				"'" + proposalId + "'@1"))
																.put("values",
																		new org.json.JSONArray().put("Sin ProductName")));
										System.out.println("Sin product name (" + proposalId + ")");
										continue;
									}
									articulosEnviados.add(firstVariant);
									req.getJSONArray("rows").put(new org.json.JSONObject()
											.put("object", new org.json.JSONObject().put("id", "'" + firstVariant + "'@1"))
											.put("values", new org.json.JSONArray().put(
													new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(reqDate))));
									product.appendChild(subProduct);
									varName.setTextContent(name.getTextContent() + ", " + tamanoUnico + ", " + color);
									for (java.util.Map.Entry<String, org.json.JSONObject> entr : heredables.entrySet()) {
										if (!misaidis.contains(entr.getKey())) {
											charId = entr.getKey();
											characteristic = entr.getValue();
											if ("LOOKUP".equals(characteristic.getString("_datatype"))) {
												if (!characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").getJSONObject(0).has("_label")) {
													System.out.println("\n\t--->" + characteristic + "<---\n");
													System.exit(0);
												}
												appendPlainElementValue(
														characteristic.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_label"),
														characteristic.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_code"),
														charId, subAttributeValues, attributes, doc, propiedadesCaracteristicas,
														atgGroups);
											} else if (!"NONE".equals(characteristic.getString("_datatype"))) {
												java.util.LinkedList<String> vals = new java.util.LinkedList<>();
												for (int m = 0; m < characteristic.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").length(); m++) {
													vals.addLast(String.valueOf(parseDateForSpecificDateFields(
															characteristic.getJSONArray("_recordLang").getJSONObject(0)
																	.getJSONArray("values").get(m),
															charId)));
												}
												appendPlainElementValue(String.join(",", vals), null, charId,
														subAttributeValues, attributes, doc, propiedadesCaracteristicas,
														atgGroups);
											}
										}
									}
		
									if (productName != null) {
										name.setTextContent(productName);
										appendPlainElementValue(productName, null, "ProductName", subAttributeValues,
												attributes, doc, propiedadesCaracteristicas, atgGroups);
									} else {
										if (charactName != null && !"".equals(charactName)) {
											name.setTextContent(charactName);
											appendPlainElementValue(charactName, null, "ProductName", subAttributeValues,
													attributes, doc, propiedadesCaracteristicas, atgGroups);
										} else {
											if (nameLang != null && !"".equals(nameLang)) {
												name.setTextContent(nameLang);
												appendPlainElementValue(nameLang, null, "ProductName", subAttributeValues,
														attributes, doc, propiedadesCaracteristicas, atgGroups);
											} else {
												log("Sin product neim, no será posible publicar.");
												System.out.println("Sin product neim, no será posible publicar.");
												reqPublishMessage.getJSONArray("rows")
														.put(new org.json.JSONObject()
																.put("object",
																		new org.json.JSONObject().put("id",
																				"'" + proposalId + "'@1"))
																.put("values",
																		new org.json.JSONArray().put("Sin ProductName")));
												continue;
											}
										}
									}
									if (descLong != null) {
										appendPlainElementValue(descLong, null, "DescriptionLong", subAttributeValues,
												attributes, doc, propiedadesCaracteristicas, atgGroups);
									}
									appendPlainElementValue(sku, null, "ParentSKU", subAttributeValues, attributes, doc,
											propiedadesCaracteristicas, atgGroups);
									if (childSAPObjectType != null && !"".equals(childSAPObjectType)) {
										appendPlainElementValue(childSAPObjectTypeLabel, childSAPObjectType, "SAPObjectType",
												subAttributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
									}
									if (tamanoUnico != null && !"".equals(tamanoUnico)) {
										appendPlainElementValue(tamanoUnico, null, "TamanoUnico", subAttributeValues,
												attributes, doc, propiedadesCaracteristicas, atgGroups);
										talla(clothingSize == null ? sizeVaD : clothingSize, tamanoUnico, business, itemGroup, template, direccion, brandCode,
												subAttributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
									}
									if (tallaNormalizada != null && !"".equals(tallaNormalizada)) {
										appendPlainElementValue(tallaNormalizada, null, "TC-NormalizedSize", subAttributeValues,
												attributes, doc, propiedadesCaracteristicas, atgGroups);
									}
									if (color != null && !"".equals(color)) {
										appendPlainElementValue(color, codigoColor, "ColoursLiverpoolAtt", subAttributeValues,
												attributes, doc, propiedadesCaracteristicas, atgGroups);
									}
									appendPlainElementValue("".equals(itemGroupLabel) ? itemGroupS4HLabel : itemGroupLabel,
											"".equals(itemGroup) ? itemGroupS4H : itemGroup, "ItemGroup2", subAttributeValues,
											attributes, doc, propiedadesCaracteristicas, atgGroups);
									if (piName != null && piUrl != null && piKey != null) {
										appendMediaAsset(piName, piUrl, "PrimaryProductImage", // String assetType,
												piKey, "Imagen Producto", // String assetValueTextContent,
												"ImageURL", // String assetValueAttributeId,
												"ProductImage", // String assetUserTypeId,
												"ProductImage", // String assetKeyPrefix,
												itemId, characteristic, "ProductImage", // String baseAssetTypeName,
												assetMap, assetReferencesMap, subProduct, assets, doc, firstVariant);
									}
									if (details != null && !details.isEmpty()) {
										for (String[] dt : details) {
											appendMediaAsset(dt[0], dt[1], "ProductImage", // String assetType,
													dt[2], "Imagen Detalle Producto", // String assetValueTextContent,
													"ImageURL", // String assetValueAttributeId,
													"ProductImageDetail", // String assetUserTypeId,
													"ProductImageDetail", // String assetKeyPrefix,
													itemId, characteristic, "ProductImageDetail", // String baseAssetTypeName,
													assetMap, assetReferencesMap, subProduct, assets, doc, dt[3]);
										}
									}
									if (smoshes != null && !smoshes.isEmpty()) {
										for (String[] dt : smoshes) {
											appendMediaAsset(dt[0], dt[1], "ProductImageSmosh", // String assetType,
													dt[2], "Imagen Smosh Producto", // String assetValueTextContent,
													"ImageURL", // String assetValueAttributeId,
													"ProductImageSmosh", // String assetUserTypeId,
													"SmoshImg", // String assetKeyPrefix,
													itemId, characteristic, "ProductImageSmosh", // String baseAssetTypeName,
													assetMap, assetReferencesMap, subProduct, assets, doc, firstVariant);
										}
									}
									if (illustrations != null && !illustrations.isEmpty()) {
										for (String[] dt : illustrations) {
											appendMediaAsset(dt[0], dt[1], "Illustration", // String assetType,
													dt[2], "Imagen Isométrica del Producto", // String assetValueTextContent,
													"ImageURL", // String assetValueAttributeId,
													"Illustration", // String assetUserTypeId,
													"Illustration", // String assetKeyPrefix,
													itemId, characteristic, "Illustration", // String baseAssetTypeName,
													assetMap, assetReferencesMap, subProduct, assets, doc, firstVariant);
										}
									}
		
								} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException
										| IOException e) {
									logE(e);
									log("Exception on processing child. " + e.getMessage());
								}
								theFirstTime = false;
							}
							System.out.println("Alojka hahajaka " + sku);
							if (sku != null && !"".equals(sku)) {
								keyValueSKU = doc.createElement("KeyValue");
								keyValueSKU.setAttribute("KeyID", "SKUID");
								String skuval = sku;
								keyValueSKU.setTextContent(skuval);
								product.appendChild(keyValueSKU);
								prodToSKU.put(proposalId, skuval);
								appendPlainElementValue(skuval, null, "SKU", attributeValues, attributes, doc,
										propiedadesCaracteristicas, atgGroups);
							}
							if (mainBarCode != null && !"".equals(mainBarCode)) {
								keyValueEAN = doc.createElement("KeyValue");
								keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
								keyValueEAN.setTextContent(mainBarCode);
								product.appendChild(keyValueEAN);
								appendPlainElementValue(mainBarCode, null,
										"SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode", attributeValues, attributes,
										doc, propiedadesCaracteristicas, atgGroups);
							}
							if (piName != null && piUrl != null && piKey != null) {
								appendMediaAsset(piName, piUrl, "PrimaryProductImage", // String assetType,
										piKey, "Imagen Producto", // String assetValueTextContent,
										"ImageURL", // String assetValueAttributeId,
										"ProductImage", // String assetUserTypeId,
										"ProductImage", // String assetKeyPrefix,
										itemId, characteristic, "ProductImage", // String baseAssetTypeName,
										assetMap, assetReferencesMap, product, assets, doc, proposalId);
							}
							for (java.util.LinkedList<String[]> eldetalle : losdetalles) {
								for (String[] dt : eldetalle) {
									appendMediaAsset(dt[0], dt[1], "ProductImage", // String assetType,
											dt[2], "Imagen Detalle Producto", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"ProductImageDetail", // String assetUserTypeId,
											"ProductImageDetail", // String assetKeyPrefix,
											itemId, characteristic, "ProductImageDetail", // String baseAssetTypeName,
											assetMap, assetReferencesMap, product, assets, doc, dt[3]);
								}
							}
							for (java.util.LinkedList<String[]> elesmoshes : losesmoshes) {
								for (String[] dt : elesmoshes) {
									appendMediaAsset(dt[0], dt[1], "ProductImageSmosh", // String assetType,
											dt[2], "Imagen Smosh Producto", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"ProductImageSmosh", // String assetUserTypeId,
											"SmoshImg", // String assetKeyPrefix,
											itemId, characteristic, "ProductImageSmosh", // String baseAssetTypeName,
											assetMap, assetReferencesMap, product, assets, doc, firstVariant);
								}
							}
							for (java.util.LinkedList<String[]> lailustracion : lasilustraciones) {
								for (String[] dt : lailustracion) {
									appendMediaAsset(dt[0], dt[1], "Illustration", // String assetType,
											dt[2], "Imagen Isométrica del Producto", // String assetValueTextContent,
											"ImageURL", // String assetValueAttributeId,
											"Illustration", // String assetUserTypeId,
											"Illustration", // String assetKeyPrefix,
											itemId, characteristic, "Illustration", // String baseAssetTypeName,
											assetMap, assetReferencesMap, product, assets, doc, firstVariant);
								}
							}
						} else {
							try {
								raw = rw.makeRequest("GET", "/object/Article/'" + firstVariant
										+ "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData",
										null);
								org.json.JSONObject resp = new org.json.JSONObject(raw);
								resp = resp.getJSONObject("_data");
								if (!resp.has("_characteristicRecords")) {
									System.out.println("No characteristic records.");
									continue;
								}
								if (resp.has("procedeNoProcede")) {
									procede = resp.getBoolean("procedeNoProcede");
								}
								characteristicRecords = resp.getJSONArray("_characteristicRecords");
								String sku0 = null;
								String ean0 = null;
								for (int b = 0; b < characteristicRecords.length(); b++) {
									imageObject = characteristicRecords.getJSONObject(b);
									charId = imageObject.getJSONObject("_qualification").getJSONObject("characteristic")
											.getString("_code");
									if (("ProductImage" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									} else if (("ProductImageDetail" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									} else if (("ProductImageSmosh" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									} else if (("Illustration" + (frozenImagesExists ? "2" : "")).equals(charId)) {
									} else if ("ProductImage".equals(charId)) {
									} else if ("ProductImageDetail".equals(charId)) {
									} else if ("ProductImageSmosh".equals(charId)) {
									} else if ("Illustration".equals(charId)) {
									} else if ("TamanoUnicoSTD".equals(charId)) {
									} else if ("TamanoUnico".equals(charId)) {
									} else if ("ColoursLiverpoolAtt".equals(charId)) {
									} else if ("SKU".equals(charId)) {
										sku0 = treatment(imageObject.getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getString(0));
									} else if ("MainBarCode".equals(charId)) {
										ean0 = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
												.getString(0);
									} else if ("MainBarCodeS4H".equals(charId)) {
										ean0 = imageObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values")
												.getString(0);
									} else if ("SAPObjectType".equals(charId)) {
									} else if ("ProcedeNoProcede".equals(charId)) {
										procede = imageObject.getJSONArray("_recordLang").getJSONObject(0)
												.getJSONArray("values").getBoolean(0);
									} else {
										if (atributosGeneralesQueSi.contains(charId)) {
											if ("LOOKUP".equals(imageObject.getString("_datatype"))) {
												appendPlainElementValue(
														imageObject.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_label"),
														imageObject.getJSONArray("_recordLang").getJSONObject(0)
																.getJSONArray("values").getJSONObject(0).getString("_code"),
														charId, attributeValues, attributes, doc, propiedadesCaracteristicas,
														atgGroups);
											} else if (!"NONE".equals(imageObject.getString("_datatype"))) {
												java.util.LinkedList<String> vals = new java.util.LinkedList<>();
												for (int m = 0; m < imageObject.getJSONArray("_recordLang").getJSONObject(0)
														.getJSONArray("values").length(); m++) {
													vals.addLast(String.valueOf(parseDateForSpecificDateFields(
															imageObject.getJSONArray("_recordLang").getJSONObject(0)
																	.getJSONArray("values").get(m),
															charId)));
												}
												appendPlainElementValue(String.join(",", vals), null, charId, attributeValues,
														attributes, doc, propiedadesCaracteristicas, atgGroups);
											}
										}
									}
								}
		
								if (sku0 == null || "".equals(sku0)) {
									sku0 = sku;
								} else {
									if (sku == null || "".equals(sku)) {
										sku = sku0;
									}
								}
								keyValueSKU = doc.createElement("KeyValue");
								keyValueSKU.setAttribute("KeyID", "SKUID");
								String skuval = sku0;
								keyValueSKU.setTextContent(skuval);
								product.appendChild(keyValueSKU);
								prodToSKU.put(proposalId, skuval);
								appendPlainElementValue(skuval, null, "SKU", attributeValues, attributes, doc,
										propiedadesCaracteristicas, atgGroups);
								if (ean0 == null || "".equals(ean0)) {
									ean0 = mainBarCode;
								} else {
									if (mainBarCode == null || "".equals(mainBarCode)) {
										mainBarCode = ean0;
									}
								}
								keyValueEAN = doc.createElement("KeyValue");
								keyValueEAN.setAttribute("KeyID", "SBB".equals(business) ? "EANS4HKey" : "EANKey");
								keyValueEAN.setTextContent(ean0);
								product.appendChild(keyValueEAN);
								appendPlainElementValue(ean0, null, "SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode",
										attributeValues, attributes, doc, propiedadesCaracteristicas, atgGroups);
								if (!procede) {
									procede = resp.has("procedeNoProcede") && resp.getBoolean("procedeNoProcede");
								}
								if (procede) {
									articulosEnviados.add(firstVariant);
									req.getJSONArray("rows").put(new org.json.JSONObject()
											.put("object", new org.json.JSONObject().put("id", "'" + firstVariant + "'@1"))
											.put("values", new org.json.JSONArray().put(
													new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(reqDate))));
								} else {
		
								}
							} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
								logE(e);
							}
		
						}
						if (rw.getXmm().listImmediateChildElements(product).get("Product") != null
								|| ("SalesItem".equals(productType) && procede)) {
							products.appendChild(product);
							System.out.println("Added. (" + proposalId + ")");
							log("Added. (" + proposalId + ")");
						} else {
							log("Not added (" + proposalId + ") - procede: " + procede);
							System.out.println("Not added (" + proposalId + ") - procede: " + procede);
						}
					} finally {
						if (hasProducts(current)) {
							long candidateBytes = measureMergedSize(batch, current);
							if (hasProducts(batch) && candidateBytes > MAX_BATCH_BYTES) {
								appendBatchResult(aggregatedMessage,
										finishBatch(batch, sendIt, execId, envioAtgExecId, jdbcConfig, batchNumber));
								batchNumber++;
								batch = createExportContext();
							}
	
							mergeContext(batch, current, true);
							long batchBytes = serializedAtgSize(batch.doc);
							log("Batch " + batchNumber + ": " + batch.proposalIds.size() + " propuestas, "
									+ batchBytes + " bytes");
	
							if (batchBytes > MAX_BATCH_BYTES && batch.proposalIds.size() == 1) {
								log("WARNING: La propuesta " + current.currentProposalId
										+ " supera por sí sola el límite de " + MAX_BATCH_BYTES + " bytes.");
								appendBatchResult(aggregatedMessage,
										finishBatch(batch, sendIt, execId, envioAtgExecId, jdbcConfig, batchNumber));
								batchNumber++;
								batch = createExportContext();
							}
						} else {
							mergeContext(batch, current, false);
						}
					}
				}
	
				if (hasPendingWork(batch)) {
					appendBatchResult(aggregatedMessage,
							finishBatch(batch, sendIt, execId, envioAtgExecId, jdbcConfig, batchNumber));
				}
				return aggregatedMessage.toString();
			} catch (TransformerException e) {
				e.printStackTrace();
				logE(e);
				markExecutionFailed(jdbcConfig, envioAtgExecId, e);
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				logE(e);
				markExecutionFailed(jdbcConfig, envioAtgExecId, e);
			} catch (IOException e) {
				e.printStackTrace();
				logE(e);
				markExecutionFailed(jdbcConfig, envioAtgExecId, e);
			}
		}
		return null;
	}
	

	private static final class ExportContext {
		private Document doc;
		private Element spim;
		private Element attributes;
		private Element assets;
		private Element products;
		private Element classifications;
		private Element webHierarchyRoot;
		private Element webHierarchyRootName;
		private String currentProposalId;

		private final java.util.Map<String, Element> assetMap = new java.util.TreeMap<String, Element>();
		private final java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap = new java.util.TreeMap<String, java.util.LinkedList<String>>();
		private final java.util.Map<String, String> prodToSKU = new java.util.HashMap<String, String>();
		private final java.util.Map<String, String> artToSKU = new java.util.HashMap<String, String>();
		private final java.util.List<String> productos = new java.util.ArrayList<String>();
		private final java.util.List<String> proposalIds = new java.util.ArrayList<String>();
		private final java.util.Set<String> articulosEnviados = new java.util.TreeSet<String>();
		private final java.util.LinkedList<org.json.JSONObject> rescataLaRaiz = new java.util.LinkedList<org.json.JSONObject>();
		private final java.util.Map<String, java.util.Map<String, org.json.JSONObject>> globalMap = new java.util.TreeMap<String, java.util.Map<String, org.json.JSONObject>>();

		private final org.json.JSONObject reqPublishMessage = newRequest(
				"Product2GCharacteristicValueLang.Value('PublishMessage',root,\"0000.0000.RK\",'PublishMessage',-1)");
		private final org.json.JSONObject req2 = newRequest(
				"Product2GCharacteristicValueLang.Value('FechaUltimaPublicacion',root,\"0000.0000.RK\",'FechaUltimaPublicacion',-1)");
		private final org.json.JSONObject reqLastApprovedCategories = newRequest(
				"Product2GExtraData.LastApprovedCategories(MX)");
		private final org.json.JSONObject req = newRequest(
				"ArticleCharacteristicValueLang.Value('FechaYHoraDePublicacion',root,\"0000.0000.RK\",'FechaYHoraDePublicacion',-1)");
		private final org.json.JSONObject reqAPublishMessage = newRequest(
				"ArticleCharacteristicValueLang.Value('PublishMessage',root,\"0000.0000.RK\",'PublishMessage',-1)");
	}

	private static org.json.JSONObject newRequest(String identifier) {
		return new org.json.JSONObject()
				.put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", identifier)))
				.put("rows", new org.json.JSONArray());
	}

	private ExportContext createExportContext() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		ExportContext context = new ExportContext();
		context.doc = builder.newDocument();

		context.spim = context.doc.createElement("STEP-ProductInformation");
		context.spim.setAttribute("ExportTime",
				new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
		context.spim.setAttribute("ExportContext", "Context2");
		context.spim.setAttribute("ContextID", "Context2");
		context.spim.setAttribute("WorkspaceID", "Approved");
		context.spim.setAttribute("UseContextLocale", "false");
		context.doc.appendChild(context.spim);

		context.attributes = context.doc.createElement("AttributeList");
		context.assets = context.doc.createElement("Assets");
		context.products = context.doc.createElement("Products");
		context.classifications = context.doc.createElement("Classifications");
		context.spim.appendChild(context.attributes);
		context.spim.appendChild(context.assets);
		context.spim.appendChild(context.products);
		context.spim.appendChild(context.classifications);

		Element raizClassification = context.doc.createElement("Classification");
		raizClassification.setAttribute("ID", "Classification 1 root");
		raizClassification.setAttribute("UserTypeID", "Classification 1 user-type root");
		raizClassification.setAttribute("Selected", "false");

		Element raizClassificationName = context.doc.createElement("Name");
		raizClassificationName.setTextContent("Clasificaciones");
		raizClassification.appendChild(raizClassificationName);

		context.webHierarchyRoot = context.doc.createElement("Classification");
		context.webHierarchyRoot.setAttribute("ID", "WebHierarchyRoot");
		context.webHierarchyRoot.setAttribute("UserTypeID", "WebHierarchyRoot");
		context.webHierarchyRoot.setAttribute("Selected", "false");

		context.webHierarchyRootName = context.doc.createElement("Name");
		context.webHierarchyRootName.setTextContent("Sitios Web");
		context.webHierarchyRoot.appendChild(context.webHierarchyRootName);
		raizClassification.appendChild(context.webHierarchyRoot);
		context.classifications.appendChild(raizClassification);

		return context;
	}

	private boolean hasProducts(ExportContext context) {
		return hasDirectElementChild(context.products, "Product");
	}

	private boolean hasPendingWork(ExportContext context) {
		return hasProducts(context)
				|| hasRows(context.reqPublishMessage)
				|| hasRows(context.reqAPublishMessage)
				|| hasRows(context.reqLastApprovedCategories)
				|| hasRows(context.req)
				|| hasRows(context.req2);
	}

	private static boolean hasRows(org.json.JSONObject request) {
		return request != null && request.getJSONArray("rows").length() > 0;
	}

	private static boolean hasDirectElementChild(Element parent, String tagName) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
				return true;
			}
		}
		return false;
	}

	private long measureMergedSize(ExportContext batch, ExportContext current)
			throws ParserConfigurationException, TransformerException {
		Document candidate = cloneDocument(batch.doc);
		mergeXml(candidate, current);
		return serializedAtgSize(candidate);
	}

	private void mergeContext(ExportContext destination, ExportContext source, boolean includeXmlAndProductData) {
		if (includeXmlAndProductData) {
			mergeXml(destination.doc, source);
			destination.assetMap.putAll(source.assetMap);
			destination.assetReferencesMap.putAll(source.assetReferencesMap);
			destination.prodToSKU.putAll(source.prodToSKU);
			destination.artToSKU.putAll(source.artToSKU);
			destination.productos.addAll(source.productos);
			destination.articulosEnviados.addAll(source.articulosEnviados);
			if (source.currentProposalId != null) {
				destination.proposalIds.add(source.currentProposalId);
			}
		}

		appendRows(destination.reqPublishMessage, source.reqPublishMessage);
		appendRows(destination.reqAPublishMessage, source.reqAPublishMessage);
		appendRows(destination.reqLastApprovedCategories, source.reqLastApprovedCategories);
		appendRows(destination.req, source.req);
		appendRows(destination.req2, source.req2);
	}

	private void mergeXml(Document destinationDocument, ExportContext source) {
		Element destinationRoot = destinationDocument.getDocumentElement();
		Element destinationAttributes = directChild(destinationRoot, "AttributeList");
		Element destinationAssets = directChild(destinationRoot, "Assets");
		Element destinationProducts = directChild(destinationRoot, "Products");
		Element destinationClassifications = directChild(destinationRoot, "Classifications");

		appendUniqueDirectChildren(destinationDocument, destinationAttributes, source.attributes, "ID");
		appendUniqueDirectChildren(destinationDocument, destinationAssets, source.assets, "ID");
		appendUniqueDirectChildren(destinationDocument, destinationProducts, source.products, "ID");

		Element destinationWebRoot = findElementById(destinationClassifications, "WebHierarchyRoot");
		Element sourceWebRoot = findElementById(source.classifications, "WebHierarchyRoot");
		if (destinationWebRoot != null && sourceWebRoot != null) {
			mergeClassificationChildren(destinationDocument, destinationWebRoot, sourceWebRoot);
		}
	}

	private static Element directChild(Element parent, String tagName) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
				return (Element) child;
			}
		}
		throw new IllegalStateException("No se encontró el elemento " + tagName);
	}

	private static void appendUniqueDirectChildren(Document destinationDocument, Element destinationParent,
			Element sourceParent, String idAttribute) {
		for (Node child = sourceParent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!(child instanceof Element)) {
				continue;
			}
			Element sourceElement = (Element) child;
			String id = sourceElement.getAttribute(idAttribute);
			if (id == null || id.isEmpty()
					|| findDirectChildById(destinationParent, sourceElement.getTagName(), id) == null) {
				destinationParent.appendChild(destinationDocument.importNode(sourceElement, true));
			}
		}
	}

	private static void mergeClassificationChildren(Document destinationDocument, Element destinationParent,
			Element sourceParent) {
		for (Node child = sourceParent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!(child instanceof Element)) {
				continue;
			}
			Element sourceElement = (Element) child;
			if (!"Classification".equals(sourceElement.getTagName())) {
				continue;
			}
			String id = sourceElement.getAttribute("ID");
			Element existing = id == null || id.isEmpty() ? null
					: findDirectChildById(destinationParent, sourceElement.getTagName(), id);
			if (existing == null) {
				destinationParent.appendChild(destinationDocument.importNode(sourceElement, true));
			} else {
				mergeClassificationChildren(destinationDocument, existing, sourceElement);
			}
		}
	}

	private static Element findDirectChildById(Element parent, String tagName, String id) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				Element element = (Element) child;
				if (tagName.equals(element.getTagName()) && id.equals(element.getAttribute("ID"))) {
					return element;
				}
			}
		}
		return null;
	}

	private static Element findElementById(Element root, String id) {
		if (id.equals(root.getAttribute("ID"))) {
			return root;
		}
		for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element) {
				Element found = findElementById((Element) child, id);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static void appendRows(org.json.JSONObject destination, org.json.JSONObject source) {
		org.json.JSONArray destinationRows = destination.getJSONArray("rows");
		org.json.JSONArray sourceRows = source.getJSONArray("rows");
		for (int i = 0; i < sourceRows.length(); i++) {
			destinationRows.put(sourceRows.get(i));
		}
	}

	private static void clearRows(org.json.JSONObject request) {
		request.put("rows", new org.json.JSONArray());
	}

	private Document cloneDocument(Document source) throws ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document clone = builder.newDocument();
		clone.appendChild(clone.importNode(source.getDocumentElement(), true));
		return clone;
	}

	private String serializeAtgXml(Document doc, boolean indent) throws TransformerException {
		return new String(serializeXml(doc, indent), StandardCharsets.UTF_8)
				.replace("&lt;CRLF&gt;", "&#13;&#10;")
				.replace("<CRLF>", "&#13;&#10;");
	}

	private long serializedAtgSize(Document doc) throws TransformerException {
		return serializeAtgXml(doc, false).getBytes(StandardCharsets.UTF_8).length;
	}

	private String finishBatch(ExportContext batch, boolean sendIt, String execId, long envioAtgExecId,
			JdbcConfig jdbcConfig, int batchNumber)
			throws TransformerException, ParserConfigurationException, IOException {
		sendRequestRows("Product2G", batch.reqPublishMessage, System.out::println);
		sendRequestRows("Article", batch.reqAPublishMessage, this::log);

		if (!hasProducts(batch)) {
			return "";
		}

		String xmlOutput = serializeAtgXml(batch.doc, false);
		String xmlOutputIndented = serializeAtgXml(batch.doc, true);
		long xmlBytes = xmlOutput.getBytes(StandardCharsets.UTF_8).length;
		long ctm = System.currentTimeMillis();
		String suffix = String.format("%03d", batchNumber) + "_" + ctm + ".xml";
		String fn = java.nio.file.Paths.get(fileSystemPrefix.toString(), "pepele" + suffix).toString();
		String fnBad = java.nio.file.Paths.get(fileSystemPrefix.toString(), "badgg" + suffix).toString();
		writeUtf8File(fn, xmlOutputIndented);
		generatedAtgFiles.add(fn);

		Document omsDocument = cloneDocument(batch.doc);
		Element omsRoot = omsDocument.getDocumentElement();
		Element omsProducts = directChild(omsRoot, "Products");
		java.util.List<Element> omsProductElements = directElementChildren(omsProducts, "Product");
		for (Element product : omsProductElements) {
			if ("SalesItemFamilyMkt".equals(product.getAttribute("UserTypeID"))) {
				product.setAttribute("UserTypeID", "SalesItemFamily");
			}
		}
		removeDirectChildIfPresent(omsRoot, "Classifications");
		removeDirectChildIfPresent(omsRoot, "AttributeList");
		String xmlOutputOms = new String(serializeXml(omsDocument, false), StandardCharsets.UTF_8);
		String fnOms = java.nio.file.Paths.get(fileSystemPrefixOMS.toString(), "pepele" + suffix).toString();
		String fnOmsBad = java.nio.file.Paths.get(fileSystemPrefixOMS.toString(), "bad" + suffix).toString();
		writeUtf8File(fnOms, xmlOutputOms);

		java.util.List<Element> productElements = directElementChildren(batch.products, "Product");
		log("Batch " + batchNumber + " terminado: " + productElements.size() + " propuestas, " + xmlBytes + " bytes, archivo " + fn);

		if (toDwhOnly && !sendIt && Boolean.parseBoolean(PropertiesManager.get("p360.contingency.dwh.enabled", "true"))) {
			sendBatchToDwh(xmlOutputIndented, batchNumber);
		}
		if (!sendIt) {
			return xmlOutputIndented;
		}

		StringBuilder result = new StringBuilder();
		RestClient batchClient = new RestClient("Content-Type: application/xml", "Accept: application/xml");
		String atgResponse = null;
		try {
			atgResponse = batchClient.getRequest("POST", urlDeATG, xmlOutput);
			log("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] (ATG) Batch " + batchNumber + " request sent for " + batch.proposalIds + ": " + atgResponse);
			result.append(fn).append("<::>").append(atgResponse);

			if (atgResponse != null && atgResponse.contains("Se proceso correctamente")) {
				handleSuccessfulAtgBatch(batch, xmlOutput, fn, execId, envioAtgExecId, jdbcConfig, ctm);
			} else {
				writeBadUtf8File(fnBad, xmlOutputIndented);
				atgBrokerFailure = true;
				markExecutionFailed(jdbcConfig, envioAtgExecId,
						new IllegalStateException(String.valueOf(atgResponse)));
			}
			atgBrokerResponses.add(atgResponse == null ? "" : atgResponse);

			sendRequestRows("Product2G", batch.reqPublishMessage, this::log);
			sendRequestRows("Product2G", batch.req2, this::log);
			sendRequestRows("Article", batch.reqAPublishMessage, this::log);
		} catch (IOException e) {
			writeBadUtf8File(fnBad, xmlOutputIndented);
			atgBrokerFailure = true;
			atgBrokerResponses.add("IOException: " + e.getMessage());
			logE(e);
			markExecutionFailed(jdbcConfig, envioAtgExecId, e);
		}

		try {
			String omsResponse = batchClient.getRequest("POST", urlDeOMS, xmlOutputOms);
			log("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] (OMS) Batch " + batchNumber + " request sent for " + batch.proposalIds + ": " + omsResponse);
			result.append("<;;>").append(fnOms).append("<::>").append(omsResponse);
			if(omsResponse == null || "".equals(omsResponse)) {
				writeBadUtf8File(fnOmsBad, xmlOutputOms);
			}
		} catch (IOException e) {
			writeBadUtf8File(fnOmsBad, xmlOutputOms);
			logE(e);
		}
		return result.toString();
	}
	
	public static ExportRunResult runForProductIdsWithResult(
			String[] proposalIds,
			boolean send)
			throws ServiceUnavailableException, IOException {

		String[] data = cleanIds(proposalIds);

		RealExportProducts o = new RealExportProducts();

		o.frozenImagesExists = Boolean.parseBoolean(
				PropertiesManager.get(
						"p360.contingency.useFrozenImages",
						"true"));

		o.exploitLayerExists = Boolean.parseBoolean(
				PropertiesManager.get(
						"p360.contingency.useExploitLayer",
						"true"));

		String rawResult =
				o.doIt(data, send, baseUrlDEV);

		return new ExportRunResult(
				rawResult,
				o.generatedAtgFiles,
				o.atgBrokerResponses,
				send
					&& !o.generatedAtgFiles.isEmpty()
					&& !o.atgBrokerFailure);
	}

	public static final class ExportRunResult {
		private final String rawResult;
		private final java.util.List<String> payloadFiles;
		private final java.util.List<String> brokerResponses;
		private final boolean successful;

		private ExportRunResult(
				String rawResult,
				java.util.List<String> payloadFiles,
				java.util.List<String> brokerResponses,
				boolean successful) {
			this.rawResult = rawResult;
			this.payloadFiles = java.util.Collections.unmodifiableList(
					new java.util.ArrayList<>(payloadFiles));
			this.brokerResponses = java.util.Collections.unmodifiableList(
					new java.util.ArrayList<>(brokerResponses));
			this.successful = successful;
		}

		public String getRawResult() {
			return rawResult;
		}

		public java.util.List<String> getPayloadFiles() {
			return payloadFiles;
		}

		public java.util.List<String> getBrokerResponses() {
			return brokerResponses;
		}

		public boolean isSuccessful() {
			return successful;
		}
	}
	
	private void handleSuccessfulAtgBatch(ExportContext batch, String xmlOutput, String fn, String execId,
			long envioAtgExecId, JdbcConfig jdbcConfig, long ctm) {
		java.time.format.DateTimeFormatter fm = java.time.format.DateTimeFormatter
				.ofPattern("dd/MM/yyyy HH:mm:ss.SSS").withZone(java.time.ZoneId.systemDefault());
		java.time.format.DateTimeFormatter fmP = java.time.format.DateTimeFormatter
				.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
		java.util.List<EnvioAtgDao.EnvioAtgRow> rows = new java.util.ArrayList<EnvioAtgDao.EnvioAtgRow>();

		for (Element product : directElementChildren(batch.products, "Product")) {
			String productId = product.getAttribute("ID");
			batch.reqPublishMessage.getJSONArray("rows").put(new org.json.JSONObject()
					.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
					.put("values", new org.json.JSONArray().put(
							"Enviado a publicación el " + fm.format(java.time.Instant.ofEpochMilli(ctm)))));
			batch.req2.getJSONArray("rows").put(new org.json.JSONObject()
					.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
					.put("values", new org.json.JSONArray().put(fmP.format(java.time.Instant.ofEpochMilli(ctm)))));

			java.util.List<Element> childProducts = directElementChildren(product, "Product");
			if (!childProducts.isEmpty()) {
				for (Element article : childProducts) {
					String articleId = article.getAttribute("ID");
					rows.add(new EnvioAtgRow(productId, Long.parseLong(batch.prodToSKU.get(productId)), articleId,
							Long.parseLong(batch.artToSKU.get(articleId))));
					logEsp("[Esta cala] ProductID: " + productId + ", ArticleID: " + articleId + ", ProductSKU: "
							+ batch.prodToSKU.get(productId) + ", ArticleSKU: " + batch.artToSKU.get(articleId)
							+ " (" + fn + ", " + execId + ")");
					batch.reqAPublishMessage.getJSONArray("rows").put(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + articleId + "'@1"))
							.put("values", new org.json.JSONArray().put(
									"Enviado a publicación el: " + fm.format(java.time.Instant.ofEpochMilli(ctm)))));
					batch.req.getJSONArray("rows").put(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + articleId + "'@1"))
							.put("values", new org.json.JSONArray().put(fmP.format(java.time.Instant.ofEpochMilli(ctm)))));
				}
			} else {
				long productSku = Long.parseLong(batch.prodToSKU.get(productId));
				rows.add(new EnvioAtgRow(productId, productSku, null, productSku));
				logEsp("[Esta cala] ProductID: " + productId + ", ArticleID: , ProductSKU: "
						+ batch.prodToSKU.get(productId) + ", ArticleSKU:  (" + fn + ", " + execId + ")");
			}
		}

		sendRequestRows("Product2G", batch.reqLastApprovedCategories, this::log);
		sendRequestRows("Article", batch.req, System.out::println);

		if (envioAtgExecId > -1 && exploitLayerExists && jdbcConfig != null) {
			try (java.sql.Connection con = openConnection(jdbcConfig, false)) {
				try {
					long envioAtgXmlId = EnvioAtgDao.insertarXml(con, envioAtgExecId, xmlOutput);
					EnvioAtgDao.insertarDetalleBatch(con, envioAtgXmlId, rows, 1000);
					EnvioAtgDao.actualizarExec(con, envioAtgExecId, "SUCCESS", "Proceso terminado correctamente");
					con.commit();
					log("[ATG DB] envioAtgExecId=" + envioAtgExecId + ", envioAtgXmlId=" + envioAtgXmlId
							+ ", rows=" + rows.size());
				} catch (java.sql.SQLException e) {
					con.rollback();
					throw e;
				}
			} catch (ClassNotFoundException | java.sql.SQLException e) {
				logE(e);
				markExecutionFailed(jdbcConfig, envioAtgExecId, e);
			}
		}
	}

	private void sendRequestRows(String entity, org.json.JSONObject request,
			java.util.function.Consumer<String> consumer) {
		if (!hasRows(request)) {
			return;
		}
		java.util.Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("includeObjectsInProtocol", "false");
		wrapper.writeData("list", entity, null, params, request, consumer::accept);
		clearRows(request);
	}

	private void sendBatchToDwh(String xml, int batchNumber) {
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		try (ClientSession session = client.connect(user, host, port).verify(10, TimeUnit.SECONDS).getSession()) {
			FileKeyPairProvider keyProvider = new FileKeyPairProvider(privateKeyPath);
			keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
			keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
			session.auth().verify(10, TimeUnit.SECONDS);
			try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
				writeToSftp(sftp, xml, PropertiesManager.get("p360.contingency.dwh.remote_directory_base"),
						"eilstep_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS")
								.format(new java.util.Date()) + ".xml");
			}
			log("DWH sent.");
		} catch (java.io.IOException e) {
			log("Could not send request to dwh: " + e.getMessage());
			logE(e);
		} finally {
			client.stop();
		}
	}

	private static java.util.List<Element> directElementChildren(Element parent, String tagName) {
		java.util.List<Element> result = new java.util.ArrayList<Element>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
				result.add((Element) child);
			}
		}
		return result;
	}

	private static void removeDirectChildIfPresent(Element parent, String tagName) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
				parent.removeChild(child);
				return;
			}
		}
	}

	private void writeUtf8File(String fileName, String content) {
		try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(fileName), StandardCharsets.UTF_8))) {
			writer.print(content);
		} catch (java.io.IOException e) {
			logE(e);
		}
	}

	private void writeBadUtf8File(String fileName, String content) {
		try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(fileName), StandardCharsets.UTF_8))) {
			writer.print(content);
		} catch (java.io.IOException e) {
			logE(e);
		}
	}

	private void appendBatchResult(StringBuilder aggregatedMessage, String batchResult) {
		if (batchResult == null || batchResult.isEmpty()) {
			return;
		}
		if (aggregatedMessage.length() > 0) {
			aggregatedMessage.append("<;;;>");
		}
		aggregatedMessage.append(batchResult);
	}

	private void markExecutionFailed(JdbcConfig jdbcConfig, long envioAtgExecId, Throwable error) {
		if (!exploitLayerExists || jdbcConfig == null || envioAtgExecId < 0) {
			return;
		}
		try (java.sql.Connection con = openConnection(jdbcConfig, true)) {
			EnvioAtgDao.actualizarExec(con, envioAtgExecId, "FAILED", stackTraceToString(error));
		} catch (Exception updateError) {
			logE(updateError);
		}
	}

	private byte[] serializeXml(Document doc, boolean indent) throws TransformerException {
		removeInvalidXml10ControlCharacters(doc);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();

		transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");

		if (indent) {
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount",
					"4"
			);
		}

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(1024 * 1024);
		transformer.transform(new DOMSource(doc), new StreamResult(out));

		return out.toByteArray();
	}
	
	private static void removeInvalidXml10ControlCharacters(Node node) {
	    if (node == null) {
	        return;
	    }

	    NamedNodeMap attributes = node.getAttributes();

	    if (attributes != null) {
	        for (int i = 0; i < attributes.getLength(); i++) {
	            Node attribute = attributes.item(i);
	            attribute.setNodeValue(
	                    removeInvalidXml10ControlCharacters(attribute.getNodeValue())
	            );
	        }
	    }

	    switch (node.getNodeType()) {
	        case Node.TEXT_NODE:
	        case Node.CDATA_SECTION_NODE:
	        case Node.COMMENT_NODE:
	        case Node.PROCESSING_INSTRUCTION_NODE:
	            node.setNodeValue(
	                    removeInvalidXml10ControlCharacters(node.getNodeValue())
	            );
	            break;

	        default:
	            break;
	    }

	    NodeList children = node.getChildNodes();

	    for (int i = 0; i < children.getLength(); i++) {
	        removeInvalidXml10ControlCharacters(children.item(i));
	    }
	}

	private static String removeInvalidXml10ControlCharacters(String value) {
	    if (value == null || value.isEmpty()) {
	        return value;
	    }

	    StringBuilder result = new StringBuilder(value.length());

	    for (int offset = 0; offset < value.length();) {
	        int codePoint = value.codePointAt(offset);

	        if (codePoint >= 0x20
	                || codePoint == '\t'
	                || codePoint == '\n'
	                || codePoint == '\r') {
	            result.appendCodePoint(codePoint);
	        }

	        offset += Character.charCount(codePoint);
	    }

	    return result.toString();
	}

	public static String stackTraceToString(Throwable t) {
		if (t == null) {
			return null;
		}
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();

		return sw.toString();
	}

	private void writeToSftp(SftpClient sftp, String content, String remoteBasePath, String fileName)
			throws IOException {
		log("DWH sending...");
		String fullPath = null;
		fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName
				: remoteBasePath + "/" + fileName + (fileName.endsWith(".xml") ? "" : ".xml");
		log("Sending: " + fileName + " to " + fullPath);
		try (OutputStream os = sftp.write(fullPath)) {
			os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
		log("sent to DWH");

	}

	private void addCharacteristicData(java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas) {
		for (java.util.Map.Entry<Integer, org.json.JSONObject> entry : characteristicMetadataByID.entrySet()) {
			Integer characteristicID = entry.getKey();
			String identifier = characteristicIdentifierByID.get( characteristicID );
			if (identifier == null || identifier.isBlank() || propiedadesCaracteristicas.containsKey( identifier )) {
				continue;
			}
			propiedadesCaracteristicas.put( identifier, copyCharacteristicMetadata( entry.getValue() ));
		}
	}

	private org.json.JSONObject getOrCreateCharacteristicProperties( int characteristicID, java.util.Map<String, org.json.JSONObject> target ) {
		org.json.JSONObject metadata = characteristicMetadataByID.get(characteristicID);
		String identifier = characteristicIdentifierByID.get(characteristicID);
		if (metadata == null || identifier == null || identifier.isBlank()) {
			return null;
		}
		org.json.JSONObject properties = target.get(identifier);
		if (properties == null) {
			properties = copyCharacteristicMetadata(metadata);
			target.put(identifier, properties);
		}
		return properties;
	}

	private void loadCharacteristicMetadata() {
		java.util.Map<Integer, String> purposeCodes = dastub.getLookupValueCodeMap(2);
		java.util.List<org.json.JSONObject> rows = dastub.getCharacteristicMetadataRows(10);
		for (org.json.JSONObject row : rows) {
			int characteristicID = row.getInt("characteristicID");
			String identifier = row.optString("identifier", "");
			if (identifier.isBlank()) {
				continue;
			}
			org.json.JSONObject metadata =
					new org.json.JSONObject()
						.put( "name", row.optString("name", ""))
						.put( "description", row.optString("description", ""))
						.put( "dataType", row.optString("dataType", ""))
						.put( "lookup", row.optString("lookup", ""))
						.put( "isMultiValue", row.optString("isMultiValue", ""))
						.put( "purposes", rutils.resolvePurposeCodes( row.optString( "purposesRaw", ""), purposeCodes))
						.put( "order", row.optString("order", ""));
			characteristicMetadataByID.put( characteristicID, metadata );
			characteristicIdentifierByID.put( characteristicID, identifier );
			if (row.isNull("parentCharacteristicID")) {
				rootCharacteristicIDs.add( characteristicID );
			}
		}
	}

	private org.json.JSONObject copyCharacteristicMetadata( org.json.JSONObject source ) {
		org.json.JSONArray purposes = new org.json.JSONArray();
		org.json.JSONArray sourcePurposes = source.optJSONArray("purposes");
		if (sourcePurposes != null) {
			for (int i = 0; i < sourcePurposes.length(); i++) {
				purposes.put(sourcePurposes.get(i));
			}
		}
		return new org.json.JSONObject()
				.put( "name", source.optString("name", "") )
				.put( "description", source.optString("description", "") )
				.put( "dataType", source.optString("dataType", "") )
				.put( "lookup", source.optString("lookup", "") )
				.put( "isMultiValue", source.optString("isMultiValue", "") )
				.put( "purposes", purposes )
				.put( "order", source.optString("order", "") );
	}

	private java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> buildDataMap(org.json.JSONArray cr) {
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> data = new java.util.TreeMap<>();
		String id;
		org.json.JSONObject obj = null;
		java.util.LinkedList<org.json.JSONObject> lst = null;
		for (int i = 0; i < cr.length(); i++) {
			obj = cr.getJSONObject(i);
			id = obj.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = data.get(id);
			if (lst == null) {
				lst = new java.util.LinkedList<>();
				data.put(id, lst);
			}
			lst.addLast(obj);
		}
		return data;
	}

	private String getSAPObjectType(org.json.JSONArray cr) {
		String id;
		org.json.JSONObject c;
		for (int i = 0; i < cr.length(); i++) {
			c = cr.getJSONObject(i);
			id = c.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			if ("SAPObjectType".equals(id)) {
				return c.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0)
						.getString("_code");
			}
		}
		return null;
	}

	private Object parseDateForSpecificDateFields(Object value, String charId) {
		if (value == null)
			return null;
		String formato = mapaDeAtributosFechas.get(charId);
		if (formato != null) {
			try {
				log(value + " AGAINST " + formato + " FOR " + charId + ":  "
						+ new java.text.SimpleDateFormat(formato)
								.format(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
										.parse(((String) value).replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1."))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (formato != null) {
			try {
				return new java.text.SimpleDateFormat(formato)
						.format(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
								.parse(((String) value).replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.")));
			} catch (java.text.ParseException e) {

			}
		}
		return value;
	}

	private void addGlobalData(java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas, java.util.Set<String> losQueSi) {
		java.util.List<org.json.JSONObject> rows = dastub.getStandardizationValueCharacteristicRows( "GlobalTemplateAttributeConfiguration" );
		for (org.json.JSONObject row : rows) {
			int characteristicID = row.getInt("characteristicID");
			org.json.JSONObject detail = getOrCreateCharacteristicProperties( characteristicID, propiedadesCaracteristicas );
			if (detail == null) {
				continue;
			}
			String property = row.optString("property", "");
			if (!property.isBlank()) {
				detail.put( property, row.optString("propertyValue", ""));
			}
		}

		for (java.util.Map.Entry<String, org.json.JSONObject> entry : propiedadesCaracteristicas.entrySet()) {
			if ("Y".equals( entry.getValue().optString( "RelevantForATG", "" ))) {
				losQueSi.add(entry.getKey());
			}
		}
	}

	public void talla(String latallaFromCharacteristic, String latalla, String business, String itemGroup, String template, String direccion,
			String brand, Element attributeValues, Element attributes, Document doc,
			java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas,
			java.util.Map<String, String> atgGroups) throws ServiceUnavailableException {
		String elcampoLatalla = null;
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		if (elcampoLatalla == null) {
			log("Bad combination to determine laTalla, itemGroup: " + itemGroup + ", business: " + business);
			return;
		}
		log("Looking for: " + itemGroup + " and " + business + " in laTalla, got: " + elcampoLatalla
				+ ", latalla es un diccionario: " + latalla);
		String stdDictionary = mapaDeDirecciones.get(elcampoLatalla);
		String lanuevatalla = queryDictionary(latalla, stdDictionary);
		log("RA: Latalla: " + latalla + ", eldiccionarioSTD: " + stdDictionary + ", lanuevatalla: " + lanuevatalla);
		lanuevatalla = lanuevatalla == null ? latalla : lanuevatalla;
		String tallaWeb = mapaDeDireccionesAtributoTallaWeb.get(elcampoLatalla);
		log("Latalla: " + tallaWeb + ", elcampolatalla: " + elcampoLatalla + ", querying dictionary for latalla: "
				+ direccion);
		log("Latalla: " + latalla + ", eldiccionarioSTD: " + stdDictionary + ", lanuevatalla: " + lanuevatalla);
		String reqTransf = queryDictionary(direccion, "ValidDirection");
		if ("S".equals(reqTransf)) {
			String lallave = itemGroup + brand + latalla;
			log("Querying a dictionary as lallave: " + lallave);
			String clothingSize = queryDictionary(lallave, "TallasInfantilesVsMarca");
			if (clothingSize != null) {
				lanuevatalla = clothingSize;
			}
		}
		log("INNNN FORM ***** : " + tallaWeb + ", " + latallaFromCharacteristic + " || " + lanuevatalla);
		System.out.println("INNNN FORM ***** : " + tallaWeb + ", " + latallaFromCharacteristic + " || " + lanuevatalla);
		if (tallaWeb != null && (latallaFromCharacteristic == null || "".equals(latallaFromCharacteristic))) {
			System.out.println("STAR***** came here " + tallaWeb + ", " + latallaFromCharacteristic + " || " + lanuevatalla);
			appendPlainElementValue(lanuevatalla, null, tallaWeb, attributeValues, attributes, doc,
					propiedadesCaracteristicas, atgGroups);
		}
		String sequence = getTheVariantSequence(latalla, template);
		if (sequence != null && !"".equals(sequence))
			appendPlainElementValue(sequence, null, "variantOrder", attributeValues, attributes, doc,
					propiedadesCaracteristicas, atgGroups);
	}

	private String getAtributoSapLatalla(String itemGroup, String business) {
		String dp = "SBB".equals(business) ? "TallaUnicavsTallaS4H" : "TallaUnicavsTallaERP";
		String value = queryDictionary( itemGroup, dp );
		if (value == null || ("".equals(value) && !"SBB".equals(business))) {
			value = queryDictionary( itemGroup, "ItemGroupSAPSizeAttribute" );
		}
		return value;
	}

	private String queryDictionary(String key, String dictionary) {
		return dastub.queryDictionary(key, dictionary);
	}

	private String getPrimaryProductTaxonomyTemplate(org.json.JSONArray classifications) {
		org.json.JSONObject classification = null;
		String externalId = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("'(EU4\\-[0-9]+)'");
		java.util.regex.Matcher m = null;
		for (int i = 0; i < classifications.length(); i++) {
			classification = classifications.getJSONObject(i);
			externalId = classification.getJSONObject("_qualification").getJSONObject("structureGroup")
					.getString("_externalId");
			if (externalId.endsWith("'PrimaryProductTaxonomy'")) {
				m = p.matcher(externalId);
				if (m.find()) {
					return m.group(1);
				} else {
					log("Could not find a match in: " + externalId);
					return null;
				}
			}
		}
		return null;
	}

	private String[] getWebCategory(org.json.JSONArray classifications) {
		java.util.LinkedList<String> webs = new java.util.LinkedList<>();
		org.json.JSONObject classification = null;
		String externalId = null;
		for (int i = 0; i < classifications.length(); i++) {
			classification = classifications.getJSONObject(i);
			externalId = classification.getJSONObject("_qualification").getJSONObject("structureGroup")
					.getString("_externalId");
			if (externalId.endsWith("'Sitios Web'")) {
				webs.addLast(externalId.replaceAll("(^')|(('@'Sitios Web')$)", ""));
			}
		}
		return webs.toArray(new String[] {});
	}

	private String treatment(String val) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (val.charAt(i) == '0') {
			i++;
		}
		while (i < val.length()) {
			sb.append(val.charAt(i));
			i++;
		}
		return sb.toString();
	}

	private void appendMediaAsset(String name, String url, String assetType, String assetKey,
			String assetValueTextContent, String assetValueAttributeId, String assetUserTypeId, String assetKeyPrefix,
			String itemId, org.json.JSONObject characteristic, String baseAssetTypeName,
			java.util.Map<String, Element> assetMap,
			java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap, Element product, Element assets,
			Document doc, String seedId) {
		Element assetCrossReference = doc.createElement("AssetCrossReference");
		org.json.JSONObject cc = null;
		String assetId = assetKeyPrefix + "-" + seedId
				+ (assetKey != null ? assetKey : characteristic.getJSONObject("_qualification").getString("recordKey"));
		java.util.LinkedList<String> ids = assetReferencesMap.get(assetId);
		if (ids == null || !ids.contains(assetId)) {
			if (name != null) {
				assetCrossReference.setAttribute("AssetID", assetId);
				assetCrossReference.setAttribute("Type", assetType);
				assetCrossReference.setAttribute("Changed", "true");
				product.appendChild(assetCrossReference);
			} else {
				cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_Name");
				if (cc != null) {
					assetCrossReference.setAttribute("AssetID", assetId);
					assetCrossReference.setAttribute("Type", assetType);
					assetCrossReference.setAttribute("Changed", "true");
					product.appendChild(assetCrossReference);
				}
			}
		}
		Element asset = assetMap.get(assetId);
		Element assetName = null;
		Element assetValues = null;
		Element assetValue = null;
		java.util.LinkedList<String> referencesList = null;
		if (asset == null) {
			asset = doc.createElement("Asset");
			assetMap.put(assetId, asset);
			asset.setAttribute("ID", assetId);
			asset.setAttribute("UserTypeID", assetUserTypeId /* "Video" */);
			asset.setAttribute("Selected", "false");
			asset.setAttribute("Referenced", "true");
			if (name != null) {
				assetName = doc.createElement("Name");
				assetName.setTextContent(name);
				asset.appendChild(assetName);
			} else {
				if (cc != null) {
					assetName = doc.createElement("Name");
					assetName.setTextContent(
							cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
					asset.appendChild(assetName);
				} else {
					cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_Name");
					if (cc != null) {
						assetName = doc.createElement("Name");
						assetName.setTextContent(
								cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
						asset.appendChild(assetName);
					}
				}
			}
			assetValues = doc.createElement("Values");
			asset.appendChild(assetValues);
			assetValue = doc.createElement("Value");
			if (!"ProductVideo".equals(assetUserTypeId)) {
				assetValues.appendChild(assetValue);
			}
			assetValue.setAttribute("AttributeID", "getObjectType");
			assetValue.setTextContent(assetValueTextContent /* "Video Producto" */);
			assetValue = doc.createElement("Value");
			assetValue.setAttribute("AttributeID", assetValueAttributeId /* "VideoURL" */);
			if (url != null) {
				if ("ProductImage".equals(assetUserTypeId)) {
					assetValue.setTextContent("largeImage=" + url + ",smallImage=" + url + ",thumbnail=" + url);
				} else {
					assetValue.setTextContent(url);
				}
				assetValues.appendChild(assetValue);
			} else {
				cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_URL");
				if (cc != null) {
					if ("ProductImage".equals(assetUserTypeId)) {
						url = cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						assetValue.setTextContent("largeImage=" + url + ",smallImage=" + url + ",thumbnail=" + url);
					} else {
						assetValue.setTextContent(url);
					}
					assetValues.appendChild(assetValue);
				} else {
					return;
				}
			}
			if ("ProductImage".equals(assetUserTypeId)) {
				assetValue = doc.createElement("Value");
				assetValues.appendChild(assetValue);
				assetValue.setAttribute("AttributeID", "ImageKey");
				assetValue.setTextContent("sm-Imagen Producto,lg-Imagen Producto,xl-Imagen Producto");
			}
			referencesList = new java.util.LinkedList<>();
			referencesList.addLast(itemId);
			assetReferencesMap.put(assetId, referencesList);
			assets.appendChild(asset);
		} else {
			referencesList = assetReferencesMap.get(assetId);
			if (referencesList == null) {
				referencesList = new java.util.LinkedList<>();
				assetReferencesMap.put(assetId, referencesList);
			}
			if (!referencesList.contains(assetId)) {
				referencesList.addLast(assetId);
			}
		}
	}

	private java.util.Map<String, String> loadLookupGroups() {
		return dastub.getLookupValueCodeNameMap("ATGAttributeGroups", 10, true);
	}

	private void appendPlainElementValue(String textValue, String code, String attributeId, Element attributeValues,
			Element attributes, Document doc, java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas,
			java.util.Map<String, String> atgGroups) throws ServiceUnavailableException {
		org.json.JSONObject prop = propiedadesCaracteristicas.get(attributeId);
		String stdDict = null;
		String nv = null;
		boolean isMultiValue = prop != null
		        && prop.has("IsMultiselect")
		        && "1".equals(prop.getString("IsMultiselect"));
		if (textValue != null) {
		    stdDict = mapaDeDirecciones.get(attributeId);
		    if (stdDict != null) {
		        nv = queryDictionary(textValue, stdDict);
		        if (nv != null) {
		            textValue = nv;
		        }
		    }
		}
		if (isMultiValue) {
		    Element multiValue = findMultiValue(attributeValues, attributeId);
		    if (multiValue == null) {
		        multiValue = doc.createElement("MultiValue");
		        multiValue.setAttribute("AttributeID", attributeId);
		        attributeValues.appendChild(multiValue);
		    }
		    Element value = doc.createElement("Value");
		    value.setTextContent(textValue);
		    multiValue.appendChild(value);
		} else {
		    Element attributeValue = doc.createElement("Value");
		    attributeValue.setAttribute("AttributeID", attributeId);
		    if (code != null) {
		        attributeValue.setAttribute("ID", code);
		    }
		    attributeValue.setTextContent(textValue);
		    attributeValues.appendChild(attributeValue);
		}
		Element metaData = doc.createElement("MetaData");
		Element valueElement = null;
		Element metaDataMultiValue = null;
		String groupLabel = null;
		java.util.LinkedList<String> grupos = null;
		Element attribute = doc.createElement("Attribute");
		attribute.setAttribute("ID", attributeId);
		Element metadataAttribute = doc.createElement("Name");
		metadataAttribute.setTextContent(
				prop != null && !prop.has("name") ? attributeId : prop != null ? prop.getString("name") : attributeId);
		attribute.appendChild(metadataAttribute);
		if (prop != null) {
			attribute.setAttribute("MultiValued",
					prop.has("IsMultiselect") ? "1".equals(prop.getString("IsMultiselect")) ? "true" : "false"
							: "false");
			attribute.setAttribute("Mandatory",
					prop.has("IsMandatory") ? "1".equals(prop.getString("IsMandatory")) ? "true" : "false" : "false");
			if (prop.has("order")) {
				metadataAttribute = doc.createElement("Value");
				metadataAttribute.setAttribute("AttributeID", "DisplaySequence");
				metadataAttribute.setTextContent(prop.getString("order"));
				metaData.appendChild(metadataAttribute);
			}
			if (prop.has("name")) {
				metadataAttribute = doc.createElement("Value");
				metadataAttribute.setAttribute("AttributeID", "DisplayName");
				metadataAttribute.setTextContent(prop.getString("name"));
				metaData.appendChild(metadataAttribute);
			}
			if (prop.has("description")) {
				metadataAttribute = doc.createElement("Value");
				metadataAttribute.setAttribute("AttributeID", "AttributeHelpText");
				metadataAttribute.setTextContent(prop.getString("description"));
				metaData.appendChild(metadataAttribute);
			}
			if (prop.has("isConfigurable")) {
				metadataAttribute = doc.createElement("Value");
				metadataAttribute.setAttribute("AttributeID", "isConfigurable");
				metadataAttribute.setTextContent(prop.getString("isConfigurable"));
				metaData.appendChild(metadataAttribute);
			}
			if (prop.has("purposes")) {
				org.json.JSONArray purposes = prop.getJSONArray("purposes");
				grupos = new java.util.LinkedList<>();
				for (int i = 0; i < purposes.length(); i++) {
					if ("CreationModificationAtributesIIEP".equals(purposes.getString(i))) {
						valueElement = doc.createElement("Value");
						valueElement.setTextContent("true");
						valueElement.setAttribute("ID", "Y");
						valueElement.setAttribute("AttributeID", purposes.getString(i));
						metaData.appendChild(valueElement);
					} else if ("isFaceted".equals(purposes.getString(i))) {
						valueElement = doc.createElement("Value");
						valueElement.setTextContent("true");
						valueElement.setAttribute("ID", "Y");
						valueElement.setAttribute("AttributeID", purposes.getString(i));
						metaData.appendChild(valueElement);
					} else if ("isConfigurable".equals(purposes.getString(i))) {
						valueElement = doc.createElement("Value");
						valueElement.setTextContent("true");
						valueElement.setAttribute("ID", "Y");
						valueElement.setAttribute("AttributeID", purposes.getString(i));
						metaData.appendChild(valueElement);
					} else {
						if (purposes.getString(i).endsWith("GPO")) {
							grupos.addLast(purposes.getString(i));
						}
					}
				}
				if (!grupos.isEmpty()) {
					metaDataMultiValue = doc.createElement("MultiValue");
					for (String grupo : grupos) {
						groupLabel = atgGroups.get(grupo);
						if (groupLabel != null) {
							valueElement = doc.createElement("Value");
							valueElement.setTextContent(groupLabel);
							valueElement.setAttribute("ID", grupo);
							metaDataMultiValue.appendChild(valueElement);
						}
					}
					if (metaDataMultiValue.getChildNodes().getLength() > 0) {
						metaDataMultiValue.setAttribute("AttributeID", "isAttInGroupAtt");
						metaData.appendChild(metaDataMultiValue);
					}
				}
			}
		} else {
			// PANIC
			log("PANIC: No property was found for characteristic: " + attributeId);
		}
		attribute.setAttribute("FullTextIndexed", "false");
		attribute.setAttribute("ProductMode", "Normal");
		attribute.setAttribute("ExternallyMaintained", "true");
		attribute.setAttribute("Derived", "false");
		attribute.setAttribute("HierarchicalFiltering", "false");
		attribute.setAttribute("ClassificationHierarchicalFiltering", "false");
		attribute.setAttribute("Referenced", "true");
		attributes.appendChild(attribute);
		attribute.appendChild(metaData);
		if (prop != null && prop.has("VendorCenterSectionSequence")) {
			Element attributeMetaDataValue = doc.createElement("Value");
			attributeMetaDataValue.setAttribute("AttributeID", "DisplaySequence");
			attributeMetaDataValue.setTextContent(prop.getString("VendorCenterSectionSequence"));
			metaData.appendChild(attributeMetaDataValue);
		}
		Element attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "AtributoCalculadoObjetos");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("Ultimo Usuario: N/A |  Fecha: N/A");
		metaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttVaDySAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("0");
		metaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttSAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("N/A");
		metaData.appendChild(attributeMetaDataValue);
	}
	
	private Element findMultiValue(Element attributeValues, String attributeId) {
	    org.w3c.dom.Node node = attributeValues.getFirstChild();
	    while (node != null) {
	        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
	            Element element = (Element) node;
	            if ("MultiValue".equals(element.getTagName())
	                    && attributeId.equals(element.getAttribute("AttributeID"))) {
	                return element;
	            }
	        }
	        node = node.getNextSibling();
	    }
	    return null;
	}

	private Element pacheleWeb(JSONObject node, Document doc) {
		Element metaData = null;
		Element value = null;
		String aux = null;
		if (node != null) {
			metaData = doc.createElement("MetaData");
			Element classificationElement = doc.createElement("Classification");
			classificationElement.setAttribute("Selected", "true");
			classificationElement.appendChild(metaData);
			if (node.has("name_es")) {
				value = doc.createElement("Value");
				value.setAttribute("AttributeID", "DisplayName");
				value.setTextContent(node.getString("name_es"));
				metaData.appendChild(value);
			}
			if (node.has("identifier")) {
				classificationElement.setAttribute("ID", node.optString("identifier", ""));
			}
			if (node.has("level")) {
				aux = String.valueOf(node.get("level"));
				classificationElement.setAttribute("UserTypeID", "0".equals(aux) ? "WebsiteRoot" : "WebLevel" + aux);
			}
			if (node.has("parentIdentifier") && node.getString("parentIdentifier").startsWith("cat")) {
				value = doc.createElement("Value");
				value.setAttribute("AttributeID", "parentCategoryID");
				value.setTextContent(node.getString("parentIdentifier"));
				metaData.appendChild(value);
			}
			return classificationElement;
		}
		return null;
	}

	private org.json.JSONObject getMeAssetChildValue(org.json.JSONObject hola, String childCharacteristic) {
		if (hola == null || (!hola.has("_children"))) {
			return null;
		}
		org.json.JSONArray children = hola.getJSONArray("_children");
		for (int i = 0; i < children.length(); i++) {
			if (children.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic")
					.getString("_code").equals(childCharacteristic)) {
				return children.getJSONObject(i);
			}
		}
		return null;
	}

	private String getTheVariantSequence(String latalla, String template) {
		String rawMap = queryVariantOrder(template);
		if (rawMap != null) {
			String[] pieces = rawMap.split(",");
			String[] smallPieces = null;
			for (int i = 0; i < pieces.length; i++) {
				smallPieces = pieces[i].split("\\=");
				if (smallPieces[0].equals(latalla)) {
					return smallPieces[1];
				}
			}
		}
		return null;
	}

	private String queryVariantOrder(String key) {
		return dastub.queryVariantOrder(key);
	}

	private static final Logger LOGGER = Logger.getLogger(RealExportProducts.class.getName());

	static {
		try {
			LOGGER.setUseParentHandlers(false);

			FileHandler fileHandler = new FileHandler("../logs/real_export_products_atg-%g.log", 5 * 1024 * 1024, 5,
					true);
			fileHandler.setEncoding(StandardCharsets.UTF_8.name());
			fileHandler.setLevel(Level.ALL);

			fileHandler.setFormatter(new Formatter() {
				@Override
				public String format(LogRecord record) {
					java.time.LocalDateTime dateTime = java.time.Instant.ofEpochMilli(record.getMillis())
							.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

					String timestamp = dateTime
							.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

					return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record)
							+ System.lineSeparator();
				}
			});

			LOGGER.addHandler(fileHandler);
			LOGGER.setLevel(Level.ALL);

		} catch (IOException e) {
			throw new RuntimeException("No se pudo inicializar el logger", e);
		}
	}

	private void logEsp(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths
						.get("..", "logs", "toATG", "real_export_products_atg_los_mandados.log").toString(), true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void log(String message) {
		LOGGER.info(message);
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.OutputStreamWriter(new java.io.FileOutputStream(
						java.nio.file.Paths.get("..", "logs", "real_export_products_atg.log").toString(), true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] BEGIN EXCEPT ");
			ex.printStackTrace(pw);
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] END EXCEPT. ");
		} catch (java.io.IOException e) {
		}
	}

	private java.util.Set<String> YEA; 


	public java.util.Map<String, String> mapaDeDirecciones; // = new java.util.TreeMap<>();
	public java.util.Map<String, String> mapaDeDireccionesAtributoTallaWeb; // = new java.util.TreeMap<>();
	public java.util.Map<String, String> mapaDeAtributosFechas; // = new java.util.TreeMap<>();

	private java.util.Map<String, String> loadFieldDictionaries() throws ServiceUnavailableException {
		return dastub.getDictionaryValueAlternativeValueMap("RelAttribSTDATG");
	}

	private java.util.Map<String, String> loadFieldTallaATG() {
		System.out.println("Loading...");
		java.util.Map<String, String> mapaDeDirecciones = dastub.getDictionaryValueAlternativeValueMap( "RelAttribTallaATG");
		System.out.println( "Loaded... " + mapaDeDirecciones.size());
		return mapaDeDirecciones;
	}

	private java.util.Map<String, String> loadAtributosFecha() {
		return dastub.getDictionaryCharacteristicAlternativeValueMap("ConversionFechaATG");
	}

	private java.util.Set<String> loadInheritedFields() {
		java.util.Map<String, String> rows = dastub.getDictionaryCharacteristicAlternativeValueMap("CaracteristicasHeredables");
		return new java.util.TreeSet<>(rows.keySet());
	}


	private void loadDatabaseDictionaries() {
		mapaDeDirecciones = loadFieldDictionaries();
		mapaDeDireccionesAtributoTallaWeb = loadFieldTallaATG();
		mapaDeAtributosFechas = loadAtributosFecha();
		YEA = loadInheritedFields();
	}

	private final class JdbcConfig {
		private String jdbcDriver;
		private String jdbcUrl;
		private String user;
		private String password;
	}

}
