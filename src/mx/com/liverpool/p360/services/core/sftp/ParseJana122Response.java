package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.SimpleLog;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.sftp.xml.Jana122Handler;
import mx.com.liverpool.p360.services.core.sftp.xml.Jana122Handler.Product;
import mx.com.liverpool.p360.services.core.sftp.xml.Jana122Handler.Value;
import mx.com.liverpool.p360.services.core.temp.exports.RealExportProductsExpressOMS;

public class ParseJana122Response implements SimpleLog {

	private final DBAccessDataStub dastub = new DBAccessDataStub(new ELog() {
		@Override
		public void logE(Exception e) {
			ParseJana122Response.this.logE(e);
		}

		@Override
		public void log(String message) {
			ParseJana122Response.this.log(message);
		}
	});

	private final DataRequestor dr = new DataRequestor(dastub);

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	private static final String BASE_URL = workshop.getBaseUrl();

	private static final String HOST = PropertiesManager.get("p360.contingency.s4h.host"); // SFTP server address:
																							// 172.18.184.26
	private static final int PORT = Integer.parseInt(PropertiesManager.get("p360.contingency.s4h.port"));// SFTP server
																											// port: 22
	private static final String USER = PropertiesManager.get("p360.contingency.s4h.userp360");// SFTP username: userp360
	private static final Path PRIVATE_KEY_PATH = Paths
			.get(PropertiesManager.get("p360.contingency.s4h.private_key_path"));// Path to private key:
																					// /home/P360admin/.ssh/id_rsa
	private static final String REMOTE_DIR = PropertiesManager.get("p360.contingency.s4h.remote_directory_122");// Remote
																												// directory
																												// to
																												// monitor:
																												// /interfase/mer/out/step/P360/zrtuab122
	private static final Path LOCAL_PROCESSED_DIR = Paths
			.get(PropertiesManager.get("p360.contingency.s4h.local_processed_dir_122"));// Path:
																						// /u01/stage/SBB_122/processed
	private static final Path STATE_FILE = Paths.get(PropertiesManager.get("p360.contingency.s4h.state_file_122"));// File:
																													// processed_s4h.122.properties
	private static boolean USE_CACHE = Boolean.parseBoolean(PropertiesManager.get("p360.contingency.s4h.use_cache"));// false;

	private final java.util.LinkedList<String> product2GCharacteristics = new java.util.LinkedList<>();
	private final java.util.LinkedList<String> articleCharacteristics = new java.util.LinkedList<>();

	private final org.json.JSONObject reqCurrentStatus = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqPrevStatus = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqSKU = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.SKU"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqEAN = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.EAN"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqBusiness = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.Business"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqDS = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionShort(es)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqDir = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Direccion(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqSec = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Section(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqIG = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.ItemGroupS4H(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqBN = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.BRAND_ID_S4H(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqST = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SAPObjectType(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqSupplier = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierID(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqSPN = new org.json.JSONObject().put("columns",new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierPartNumber(MX)"))).put("rows", new org.json.JSONArray());

	private final org.json.JSONObject reqSKUA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SKU"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqEANA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.EAN"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqBNA = new org.json.JSONObject().put("columns",new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.Business"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqDSA = new org.json.JSONObject().put("columns",new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleLang.DescriptionShort(es)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqSPNA = new org.json.JSONObject().put("columns",new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SupplierPartNumber(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqSTA = new org.json.JSONObject().put("columns",new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject requestCommercialS4H = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('CommercialS4H')"))).put("rows", new org.json.JSONArray());

	private final java.util.Map<String, String> dataTypes = new java.util.TreeMap<>();
	private final java.util.Map<String, String> lkps = new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Map<String, String>> map = new java.util.TreeMap<>();
	private final java.util.Map<String, java.util.Map<String, String>> mapB = new java.util.TreeMap<>();

	private static final java.util.Map<String, String> charIDToS4H = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> s4hToCharID = new java.util.TreeMap<>();

	private final java.util.Map<String, String> articleHigherLevelProduct = new java.util.TreeMap<>();

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

	private final java.util.List<String> pids = new java.util.ArrayList<>();
	private final java.util.Map<String, String> qp = new java.util.HashMap<>();
	private static final java.util.Map<String, String> qp0 = new java.util.HashMap<>();

	private void addSKU(String sku, String id, String entity) {
		if ("Product2G".equals(entity)) {
			org.json.JSONArray rows = reqSKU.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(sku)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
			}
		} else if ("Article".equals(entity)) {
			org.json.JSONArray rows = reqSKUA.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(sku)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Article", null, qp0, reqSKUA, this::log);
			}
		}
	}

	private void addEAN(String ean, String id, String entity) {
		if ("Product2G".equals(entity)) {
			org.json.JSONArray rows = reqEAN.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(ean)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Product2G", null, qp0, reqEAN, this::log);
			}
		} else if ("Article".equals(entity)) {
			org.json.JSONArray rows = reqEANA.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(ean)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Article", null, qp0, reqEANA, this::log);
			}
		}
	}

	private void addSupplierPartNumber(String supplierPartNumber, String id, String entity) {
		if ("Product2G".equals(entity)) {
			org.json.JSONArray rows = reqSPN.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(supplierPartNumber)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Product2G", null, qp0, reqSPN, this::log);
			}
		} else if ("Article".equals(entity)) {
			org.json.JSONArray rows = reqSPNA.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(supplierPartNumber)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Article", null, qp0, reqSPNA, this::log);
			}
		}
	}

	private void addSAPObjectType(String sapObjectType, String id, String entity) {
		if ("Product2G".equals(entity)) {
			org.json.JSONArray rows = reqST.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(sapObjectType)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Product2G", null, qp0, reqST, this::log);
			}
		} else if ("Article".equals(entity)) {
			org.json.JSONArray rows = reqSTA.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(sapObjectType)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Article", null, qp0, reqSTA, this::log);
			}
		}
	}

	private void addBusiness(String business, String id, String entity) {
		if ("Product2G".equals(entity)) {
			org.json.JSONArray rows = reqBusiness.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(business)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Product2G", null, qp0, reqBusiness, this::log);
			}
		} else if ("Article".equals(entity)) {
			org.json.JSONArray rows = reqBNA.getJSONArray("rows");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
					.put("values", new org.json.JSONArray().put(business)));
			if (rows.length() == 1000) {
				rw.writeData("list", "Article", null, qp0, reqBNA, this::log);
			}
		}
	}

	private void addShortDescription(String id, String shortDescription) {
		org.json.JSONArray rows = reqDS.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
				.put("values", new org.json.JSONArray().put(shortDescription)));
		if (rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqDS, this::log);
		}
	}

	private void addSupplier(String id, String supplier) {
		org.json.JSONArray rows = reqSupplier.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
				.put("values", new org.json.JSONArray().put(supplier)));
		if (rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqSupplier, this::log);
		}
	}

	private void addShortDescriptionA(String id, String shortDescription) {
		org.json.JSONArray rows = reqDSA.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
				.put("values", new org.json.JSONArray().put(shortDescription)));
		if (rows.length() == 1000) {
			rw.writeData("list", "Article", null, qp0, reqDSA, this::log);
		}
	}

	private void addDirection(String id, String direction) {
		org.json.JSONArray rows = reqDir.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
				.put("values", new org.json.JSONArray().put(direction)));
		if (rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqDir, this::log);
		}
	}

	private void addSection(String id, String section) {
		org.json.JSONArray rows = reqSec.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
				.put("values", new org.json.JSONArray().put(section)));
		if (rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqSec, this::log);
		}
	}

	private void addItemGroup(String id, String itemGroup) {
		org.json.JSONArray rows = reqIG.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
				.put("values", new org.json.JSONArray().put(itemGroup)));
		if (rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqIG, this::log);
		}
	}

	private void addBrandName(String id, String brandName) {
		org.json.JSONArray rows = reqBN.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1"))
				.put("values", new org.json.JSONArray().put(brandName)));
		if (rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqBN, this::log);
		}
	}

	private void sendData() {
		org.json.JSONArray rows = reqCurrentStatus.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqCurrentStatus, this::log);
		}
		rows = reqPrevStatus.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqPrevStatus, this::log);
		}
		rows = reqEAN.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqEAN, this::log);
		}
		rows = reqEANA.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Article", null, qp0, reqEANA, this::log);
		}
		rows = reqSKU.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
		}
		rows = reqSKUA.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Article", null, qp0, reqSKUA, this::log);
		}
		rows = reqBusiness.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqBusiness, this::log);
		}
		rows = reqDir.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqDir, this::log);
		}
		rows = reqSec.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqSec, this::log);
		}
		rows = reqIG.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqIG, this::log);
		}
		rows = reqBN.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqBN, this::log);
		}
		rows = reqST.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqST, this::log);
		}
		rows = reqSupplier.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqSupplier, this::log);
		}
		rows = reqSPN.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqSPN, this::log);
		}
		rows = reqBNA.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Article", null, qp0, reqBNA, this::log);
		}
		rows = reqDSA.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Article", null, qp0, reqDSA, this::log);
		}
		rows = reqSPNA.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Article", null, qp0, reqSPNA, this::log);
		}
		rows = reqSTA.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Article", null, qp0, reqSTA, this::log);
		}
		RealExportProductsExpressOMS repO = new RealExportProductsExpressOMS();
		repO.doIt(pids.toArray(new String[] {}), true);
		pids.clear();
	}

	private final ParsersTools tools = new ParsersTools(this, dr);

	private boolean running = true;

	static {
		qp0.put("includeObjectsInProtocol", "false");
		if (java.nio.file.Files
				.notExists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")))) {
			try {
				java.nio.file.Files.createDirectories(
						java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")));
			} catch (java.io.IOException e) {

			}
		}
		if (java.nio.file.Files.notExists(
				java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"))) {
			try {
				java.nio.file.Files.createDirectories(
						java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"));
			} catch (java.io.IOException e) {

			}
		}
	}
	
	private void cargaLasCosas() {
		long init = System.currentTimeMillis();
		collectLookupCharacteristics(dataTypes, lkps);
		collectCharacteristicsByEntity(product2GCharacteristics, articleCharacteristics);
		for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
			tools.collectLookupValues(lkps.get( entry.getKey() ), map, mapB, entry.getValue());
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pcs = null;
			while((line = br.readLine()) != null) {
				pcs = workshop.parseLine(line, "\"", ";", "\\");
				if(pcs.length > 2) {
					charIDToS4H.put(pcs[0], pcs[2]);
					s4hToCharID.put(pcs[2], pcs[0]);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		articleHigherLevelProduct.clear();
		log("Refreshed took: " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}

	private void launchListenerThread() {
		Thread t = new Thread(() -> {
			while (running) {
				try (java.net.ServerSocket server = new java.net.ServerSocket(23545);
						java.net.Socket cli = server.accept();
						java.io.BufferedReader br = new java.io.BufferedReader(
								new java.io.InputStreamReader(cli.getInputStream()));
						java.io.PrintWriter pw = new java.io.PrintWriter(
								new java.io.OutputStreamWriter(cli.getOutputStream()))) {
					try {
						org.json.JSONObject req = new org.json.JSONObject(br.readLine());
						String action = req.getString("action");
						if ("finish".equals(action.toLowerCase())) {
							this.running = false;
						}
					} catch (org.json.JSONException e) {
						logE(e);
					}
				} catch (java.io.IOException e) {
					logE(e);
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logE(e);
				}
			}
			log("Finishing...");
		});
		t.start();
	}

	public static void main(String[] args) throws ServiceUnavailableException {
		ParseJana122Response object = new ParseJana122Response();
		object.launchListenerThread();
		while (object.running) {
			object.runOnSftp(args);
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				object.logE(e);
			}
		}
		object.log("Terminé.");
	}

	public void prepareLocalReplay() {
	}

	public void runOnSftp(String[] args) throws ServiceUnavailableException {
		qp.put("includeObjectsInProtocol", "false");
		workshop.setBaseUrl(BASE_URL);
		if (args.length > 0) {
			USE_CACHE = Boolean.parseBoolean(args[0]);
		} else {
			USE_CACHE = true;
		}
		boolean ft = true;
		try (SshClient client = SshClient.setUpDefaultClient()) {
			client.start();
			java.nio.file.Files.createDirectories(LOCAL_PROCESSED_DIR);

			java.util.Properties processedState = new java.util.Properties();
			if (USE_CACHE && java.nio.file.Files.exists(STATE_FILE)) {
				try (InputStream in = java.nio.file.Files.newInputStream(STATE_FILE)) {
					processedState.load(in);
				}
			}

			try (ClientSession session = client.connect(USER, HOST, PORT).verify(10, TimeUnit.SECONDS).getSession()) {

				FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
				keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
				keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);

				session.auth().verify(10, TimeUnit.SECONDS);

				try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
					while (running) {
						ft = true;
						Iterable<DirEntry> entries = sftp.readDir(REMOTE_DIR);
						for (DirEntry entry : entries) {
							String name = entry.getFilename();
							if (name.equals(".") || name.equals("..")) {
								continue;
							}

							long remoteModified = entry.getAttributes().getModifyTime().toMillis();
							String previousTimestamp = processedState.getProperty(name);

							if (USE_CACHE && previousTimestamp != null
									&& Long.parseLong(previousTimestamp) == remoteModified) {
								continue;
							}

							String filePath = REMOTE_DIR + "/" + name;
							log("Processing: " + name);
							try (InputStream input = sftp.read(filePath);
									ByteArrayOutputStream out = new ByteArrayOutputStream()) {
								copyStream(input, out);

								Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
								java.nio.file.Files.write(localCopy, out.toByteArray());

								if (!name.startsWith("GenericXMLproducts")) {
									log("Skipping " + name);
									processedState.setProperty(name, String.valueOf(remoteModified));
									if (USE_CACHE) {
										try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
											processedState.store(stateOut, null);
										}
									}
									continue;
								}

								boolean processedOk = false;
								try {
									if(ft) {
										cargaLasCosas();
										ft = false;
									}
									processFile(null, out, sftp);
									sftp.remove(filePath);
									processedOk = true;
								} catch (ParserConfigurationException | SAXException | IOException e) {
									log("No se marca como procesado; se reintentará: " + name);
									logE(e);
								}

								if (processedOk) {
									processedState.setProperty(name, String.valueOf(remoteModified));
									if (USE_CACHE) {
										try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
											processedState.store(stateOut, null);
										}
									}
								}
								if (!running)
									break;
							} catch (java.io.IOException e) {
								log("Problem reading file: " + filePath);
								logE(e);

								log("El archivo queda pendiente para reintento: " + name);
								if (!running)
									break;
							}
						}
						Thread.sleep(1_000);
						sendData();
					}
				}
			} finally {
				client.close();
			}
		} catch (IOException | InterruptedException e) {
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

	public void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos, SftpClient sftp)
			throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {
		articleHigherLevelProduct.clear();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.ResDatetime"));
		request.put("columns", columns);
		request.put("rows", rows);

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (Exception ignored) {
		}
		SAXParser parser = factory.newSAXParser();
		Jana122Handler handler = new Jana122Handler();
		try {
			if (path != null) {
				parser.parse(path.toFile(), handler);
			} else {
				parser.parse(new java.io.ByteArrayInputStream(baos.toByteArray()), handler);
			}
		} catch (NullPointerException e) {
			return;
		}

		java.util.LinkedList<Product> products = handler.getProducts();
		java.util.LinkedList<Value> values = null;

		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> attributeValues = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> newAttributeValues = new java.util.TreeMap<>();
		java.util.Map<String, String> unidades = new java.util.TreeMap<>();
		java.util.Map<String, String> articleHigherLevelProductNotReadyYet = new java.util.TreeMap<>();
		String znprst = null;
		String negocio = null;
		String sb0002 = null;
		String sku = null;
		String satnr = null;
		String attyp = null;
		String sapBehvo = null;
		String fshId = null;
		String mstae = null;
		String sistemaorigen = null;
		String itemId = null;
		String[] info = null;
		java.util.Map<String, String> skuIds = new java.util.TreeMap<>();
		java.util.Map<String, String> articleSupplierAIDToSKU = new java.util.TreeMap<>();
		java.util.Map<String, String> skuToArticleSupplierAID = new java.util.TreeMap<>();
		if (products != null) {
			for (Product n : products) {
				values = n.getValues();
				for (Value v : values) {
					if (!"".equals(v.getText())) {
						sb.append(sb.length() > 0 ? "," : "").append(v.getAttributeId());
						if (!excluyeAtributo(v)) {
							attributeValues.put(v.getAttributeId(), "MATNR".equals(v.getAttributeId())
									|| "LIFNR".equals(v.getAttributeId()) || "WESCH".equals(v.getAttributeId())
											? (v.getText() != null ? v.getText().replaceAll("^0+", "").trim() : "")
											: v.getText() == null ? "" : v.getText());
							if (v.getText() != null) {
								if (v.getText().matches("^(unece\\.unit\\.)[A-Z0-9]+$")) {
									unidades.put(v.getAttributeId(), v.getText());
								} else {
								}
							} else {
								log("Element with null text: " + v.getAttributeId());
							}
						}
					}
				}
				sb.setLength(0);
				
				if (!lkps.containsKey("EXTWG_S4H")) {
					log("Un caso donde no hay EXTWG_S4H. " + attributeValues.get("EXTWG"));
				}
				negocio = attributeValues.get("EXTWG");
				log("Negocio: " + negocio + " || " + attributeValues.get("EXTWG"));
				sku = attributeValues.get("MATNR").replaceAll("^0+", "");
				sb0002 = attributeValues.get("SB_0002");
				itemId = null;
				satnr = attributeValues.containsKey("SATNR")
						? attributeValues.get("SATNR") != null ? attributeValues.get("SATNR").replaceAll("^0+", "") : ""
						: "";
				attyp = attributeValues.get("ATTYP"); // SAPObjectType
				sapBehvo = attributeValues.get("BEHVO");
				fshId = attributeValues.get("FSH_ID");
				mstae = attributeValues.get("MSTAE");
				String lifnr = attributeValues.get("LIFNR"); // Supplier
				String brandId = attributeValues.get("BRAND_ID"); // BrandID
				String idnlf = attributeValues.get("IDNLF"); // Modelo
				String matkl = attributeValues.get("MATKL"); // ItemGroup
				String zsec = attributeValues.get("ZSEC"); // Sección
				String zdir = attributeValues.get("ZDIR"); // Dirección
				String maktx = attributeValues.get("MAKTX"); // description short
				String ean = attributeValues.get("EAN11"); // EAN
				String mtart = attributeValues.get("MTART");

				lifnr = lifnr == null ? "" : lifnr;
				brandId = brandId == null ? "" : brandId;
				idnlf = idnlf == null ? "" : idnlf;
				matkl = matkl == null ? "" : matkl;
				zsec = zsec == null ? "" : zsec;
				zdir = zdir == null ? "" : zdir;
				maktx = maktx == null ? "" : maktx;

				sistemaorigen = attributeValues.get("SISTEMAORIGEN");
				String externalId = null;
				znprst = attributeValues.get("PRODUCT_ID");
				log("Processing following SKU: " + sku);
				log("znprst: " + znprst);
				log("attyp: " + attyp);
				log("negocio: " + negocio);
				if (znprst != null && !"".equals(znprst)) {
					if (znprst.length() == 15 && !znprst.startsWith("S")) {
						znprst = "1" + znprst;
					}
					if (znprst != null && sku != null && !"".equals(sku)) {
						articleSupplierAIDToSKU.put(znprst, sku);
						skuToArticleSupplierAID.put(sku, znprst);
					}
					externalId = znprst;
					skuIds.put(sku, znprst);
					info = tools.checkProduct(znprst);
					if (info == null) {
						info = tools.checkArticle(znprst);
						if (info != null) {
							if ("00".equals(info[1])) {
								addValue(
										 "MensajeCreacionSKU"
										, "Article"
										, externalId
										, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
								sendWriteRequest("Product2G", info[0], null, info[3], info[4], sku);
							}
							addValue(
									  "MensajeCreacionSKU"
									, "Product2G"
									, externalId
									, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
							sendWriteRequest("Article", znprst, null, null, null, sku);
							log("Was Article (" + znprst + "), " + java.util.Arrays.asList(info));
						} else {
							log("Brand new SKU for P360: " + sku + " (" + znprst + ") Negocio: " + negocio);
							if ("00".equals(attyp)) {
								String originalznprst = znprst;
								String productZNPRST = null;
								znprst = chooseProperProductZNPRST(sku, znprst);
								productZNPRST = znprst;

								addValue("SAPObjectType", "Product2G", externalId, "00");
								addValue("Business", "Product2G", externalId, "SBB");
								znprst = chooseProperArticleZNPRST(sku, originalznprst);
								addValue("SAPObjectType", "Article", externalId, "00");
								sendWriteRequest("Article", externalId, null, null, null, sku);
								sendWriteRequestProduct(znprst, matkl, sb0002, negocio);
								articleHigherLevelProduct.put(znprst, znprst);
								org.json.JSONArray items = new org.json.JSONArray();
								if (!"".equals(sku)) {
									items.put(
											new org.json.JSONObject().put("productNo", productZNPRST).put("sku", sku));
									log("From registering to admin 00: " + dr.skuProductNo(items));
									items.remove(0);
									items.put(new org.json.JSONObject().put("productNo", productZNPRST).put("sku", sku)
											.put("supplierAID", znprst));
									log("From registering to admin 00: " + dr.putSkuSupplierAID(items));
								}
							} else if ("01".equals(attyp)) {
								znprst = chooseProperProductZNPRST(sku, znprst);
								addValue("SAPObjectType", "Product2G", externalId, "01");
								addValue("Business", "Product2G", externalId, "SBB");
								sendWriteRequestProduct(znprst, matkl, sb0002, negocio);
							} else {
								addValue("SAPObjectType", "Article", externalId, "02");
								sendWriteRequest("Article", znprst, null, null, null, sku);
								conciliaRelacionArticuloProducto(znprst, sku, null, satnr, articleHigherLevelProductNotReadyYet, dr);
							}
						}
					} else {
						if ("02".equals(attyp)) {
							log("Came to see that the product is now a variant.");
							conciliaRelacionArticuloProducto(znprst, sku, null, satnr, articleHigherLevelProductNotReadyYet, dr);
						} else {
							info = tools.checkProduct(znprst);
							log("Was Product (" + znprst + "), " + java.util.Arrays.asList(info));
							addValue(
									  "MensajeCreacionSKU"
									, "Product2G"
									, externalId
									, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
							if ("00".equals(info[0])) {
								itemId = getArticleIdFromProduct(znprst);
								log("Article.SupplierAID: " + itemId);
								addValue(
										  "MensajeCreacionSKU"
										, "Article"
										, externalId
										, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
								sendWriteRequest("Article", itemId, null, null, null, sku);
							}
							sendWriteRequest("Product2G", znprst, null, info[2], info[3], sku);
						}
					}
				} else {
					externalId = "SBB" + sku;
					log("No znprst found " + attyp);
					if (sku != null && !"".equals(sku)) {
						info = tools.checkProductBySKU(sku);
						if (info != null) {
							externalId = info[0];
							log("Found SKU in product");
							if ("02".equals(attyp)) {
								log("Came to see that the product result to be variant now.");
								addValue(
										  "MensajeCreacionSKU"
										, "Article"
										, externalId
										, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
								sendWriteRequest("Article", itemId, null, null, null, sku);
							} else {
								znprst = externalId;
								addValue(
										  "MensajeCreacionSKU"
										, "Product2G"
										, externalId
										, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
								sendWriteRequest("Product2G", externalId, null, info[3] == null || "".equals(info[3]) ? "Y" : info[3], info[4] == null || "".equals( info[4] ) ? "1020" : info[4], sku);
								if ("00".equals(info[1])) {
									addValue(
											  "MensajeCreacionSKU"
											, "Article"
											, externalId
											, "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()));
									sendWriteRequest("Article", externalId, null, null, null, sku);
									itemId = tools.checkArticleBySKU(sku);
									if (itemId != null && !"".equals(itemId))
										articleHigherLevelProduct.put(itemId, externalId);
								}
							}
						} else {
							itemId = tools.checkArticleBySKU(sku);
							if (itemId != null) {
								znprst = itemId;
								log("Found SKU in article. #" + itemId + "#");
								if ("00".equals(attyp)) {
									String pid = externalId;
									String rr = dr.getProductByVariant(new org.json.JSONArray().put(itemId));
									if (rr != null) {
										try {
											org.json.JSONObject j = new org.json.JSONObject(rr);
											org.json.JSONArray itms = j.getJSONArray("items");
											if (!"".equals(itms.getString(0))) {
												pid = itms.getString(0);
												rr = dr.getProductData(new org.json.JSONArray().put(pid));
												if (rr != null) {
													j = new org.json.JSONObject(rr);
													itms = j.getJSONArray("items");
													org.json.JSONObject itm = itms.getJSONObject(0);
													log("Individual a partir de SKU plano. Encontramos sku en artículo. " + itm);
													addValue("MensajeCreacionSKU", "Product2G", itm.getString("product"), "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ") .format(new java.util.Date()));
													sendWriteRequest("Product2G", itm.getString("product"), null, itm.getString("FotoTomadaLiverpool"), itm.getString("CurrentStatus"), sku);
												}
											}
										} catch (org.json.JSONException e) {
											log("Couldn't parse json response from pvia.");
										}
									} else {
										articleHigherLevelProduct.put(externalId, pid);
										sendWriteRequest("Product2G", pid, null, "Y", "1020", sku);
									}

									sendWriteRequestProduct(externalId, matkl, sb0002, negocio);

									org.json.JSONArray items = new org.json.JSONArray();
									if (!"".equals(sku)) {
										items.put(new org.json.JSONObject().put("productNo", pid).put("sku", sku));
										log("From registering to admin 00: " + dr.skuProductNo(items));
									}
								} else if ("02".equals(attyp)) {
									conciliaRelacionArticuloProducto(externalId, sku, null, satnr, articleHigherLevelProductNotReadyYet, dr);
								}
							} else {
								log("Brand new SKU for P360: " + sku + " Negocio: " + negocio);
								znprst = "SBB" + sku;
								externalId = znprst;
								if ("00".equals(attyp)) {
									info = tools.checkProductBySKU(satnr.replaceFirst("^0+", ""));
									addValue("SAPObjectType", "Product2G", externalId, "00");
									addValue("Business", "Product2G", externalId, "SBB");
									addValue("SAPObjectType", "Article", externalId, "00");
									sendWriteRequest("Article", "SBB" + sku, null, null, null, sku);
									sendWriteRequestProduct("SBB" + sku, matkl, sb0002, negocio);
									articleHigherLevelProduct.put("SBB" + sku, info != null && info.length > 0 ? info[0] : "SBB" + sku);
								} else if ("01".equals(attyp)) {
									addValue("SAPObjectType", "Product2G", externalId, "01");
									addValue("Business", "Product2G", externalId, "SBB");
									sendWriteRequestProduct("SBB" + sku, matkl, sb0002, negocio);
								} else {
									resolveVariantParent(externalId, itemId, satnr);
									addValue("SAPObjectType", "Article", externalId, "02");
									sendWriteRequest("Article", "SBB" + sku, null, null, null, sku);
									znprst = chooseProperArticleZNPRST(sku, "SBB" + sku);
									conciliaRelacionArticuloProducto(znprst, sku, null, satnr, articleHigherLevelProductNotReadyYet, dr);
								}
							}
						}
					} else {
						log("No SKU found either!");
					}
				}
				
				String entity = null;
				if ("01".equals(attyp)) {
					entity = "Product2G";
				}else if("02".equals(attyp)) {
					entity = "Article";
				}else if("00".equals(attyp)) {
					entity = "Individual";
				}
				String productId = externalId;
				if("Individual".equals(entity)) {
					String ya = dr.getArticleData(new org.json.JSONArray().put(externalId));
					if(ya != null) {
						try {
							org.json.JSONObject jya = new org.json.JSONObject(ya);
							org.json.JSONArray itms = jya.getJSONArray("items");
							org.json.JSONObject itm = itms.getJSONObject(0);
							if(itm.has("ProductNo")  &&  !"".equals(itm.getString("ProductNo"))) {
								productId = itm.getString("ProductNo");
							}
						}catch(org.json.JSONException e) {
							logE(e);
						}
					}
				}
				try {
					calculaProductType(sapBehvo, matkl, fshId, "Suburbia", null, mtart, mtart, productId, workshop);
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					e.printStackTrace();
				}
				String charId = null;
				for(java.util.Map.Entry<String, String> entry : attributeValues.entrySet()) {
					charId = s4hToCharID.get(entry.getKey());
					if(charId != null) {
						try{
							if("Product2G".equals(entity)) {
								if(!unidades.containsKey(entry.getKey()) && product2GCharacteristics.contains(charId) ) {
									addValue(charId, "Product2G", externalId, entry.getValue() );
								}
							}else if("Article".equals(entity)) {
								if(!unidades.containsKey(entry.getKey()) && articleCharacteristics.contains(charId) ) {
									addValue(charId, "Article", externalId, entry.getValue() );
								}
							}else if("Individual".equals(entity)) {
								if(!unidades.containsKey(entry.getKey()) && product2GCharacteristics.contains(charId) ) {
									addValue(charId, "Product2G", productId, entry.getValue() );
								} else if(!unidades.containsKey(entry.getKey()) && articleCharacteristics.contains(charId) ) {
									addValue(charId, "Article", externalId, entry.getValue() );
								}
							}
						}catch(IllegalArgumentException e) {
							logE(e);
						}
					}
				}
				agregaUnidadesDeMedida(unidades, charIDToS4H, externalId);
				
				if ("01".equals(attyp)) {
					addSKU(sku, znprst, "Product2G");
					addEAN(ean, znprst, "Product2G");
					addSAPObjectType(attyp, znprst, "Product2G");
					addBusiness("SBB", znprst, "Product2G");
					addSupplierPartNumber(idnlf, znprst, "Product2G");
					addShortDescription(znprst, maktx);
					addDirection(znprst, zdir);
					addSection(znprst, zsec);
					addItemGroup(znprst, matkl);
					addBrandName(znprst, brandId);
					addSupplier(znprst, lifnr);
				} else if ("02".equals(attyp)) {
					addSKU(sku, znprst, "Article");
					addEAN(ean, znprst, "Article");
					addSAPObjectType(attyp, znprst, "Article");
					addBusiness("SBB", znprst, "Article");
					addSupplierPartNumber(idnlf, znprst, "Article");
					addShortDescriptionA(znprst, maktx);
				} else if ("00".equals(attyp)) {
					addSKU(sku, znprst, "Product2G");
					addSAPObjectType(attyp, znprst, "Product2G");
					addBusiness("SBB", znprst, "Product2G");
					addSupplierPartNumber(idnlf, znprst, "Product2G");
					addShortDescription(znprst, maktx);
					addDirection(znprst, zdir);
					addSection(znprst, zsec);
					addItemGroup(znprst, matkl);
					addBrandName(znprst, brandId);
					addSupplier(znprst, lifnr);

					addSKU(sku, znprst, "Article");
					addEAN(ean, znprst, "Article");
					addSAPObjectType(attyp, znprst, "Article");
					addBusiness("SBB", znprst, "Article");
					addSupplierPartNumber(idnlf, znprst, "Article");
					addShortDescriptionA(znprst, maktx);
				}
				negocio = null;
				sku = null;
				znprst = null;
				satnr = null;
				attyp = null;
				itemId = null;
				info = null;
				unidades.clear();
				attributeValues.clear();

				if (!running)
					break;
			}
			log("\t\tNow placing relationships...");
			org.json.JSONArray items = new org.json.JSONArray();
			org.json.JSONObject item = null;
			org.json.JSONArray columns00 = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
			org.json.JSONArray rows00 = new org.json.JSONArray();
			org.json.JSONObject req = new org.json.JSONObject();
			req.put("columns", columns00);
			req.put("rows", rows00);
			for (java.util.Map.Entry<String, String> entry : articleHigherLevelProduct.entrySet()) {
				item = new org.json.JSONObject();
				item.put("supplierAID", entry.getKey());
				item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
				item.put("productNo", entry.getValue());
				items.put(item);
				rows00.put(new org.json.JSONObject()
						.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
						.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue()))
						.put("values", new org.json.JSONArray().put(entry.getValue())));
				if (rows00.length() == 1000) {
					rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
				}
			}
			if (rows00.length() > 0) {
				rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
			}
			log("HLPs: " + articleHigherLevelProduct);
			String parentId = null;
			for (java.util.Map.Entry<String, String> entry : articleHigherLevelProductNotReadyYet.entrySet()) {
				parentId = skuToArticleSupplierAID.get(entry.getValue());
				if (parentId == null) {
					log("En el archivo no estaba el padre referenciado por el valor de SKU: " + entry.getValue()
							+ " para la variante con id de sistema: " + entry.getKey());
					String response = dr.productBySKU(new org.json.JSONArray().put(entry.getValue()));
					try {
						org.json.JSONObject jr = new org.json.JSONObject(response);
						org.json.JSONArray ir = jr.getJSONArray("items");
						parentId = ir.getString(0);
						log("Recuperamos el padre gracias al admin: " + parentId + " para SKU: " + entry.getValue()
								+ ", de la propuesta variante: " + entry.getKey());
					} catch (org.json.JSONException e) {
						logE(e);
					}
				}
				if (parentId != null) {
					item = new org.json.JSONObject();
					item.put("supplierAID", entry.getKey());
					item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
					item.put("productNo", parentId);
					items.put(item);
					rows00.put(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
							.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue()))
							.put("values", new org.json.JSONArray().put(entry.getValue())));
					if (rows00.length() == 1000) {
						rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
					}
				}
			}
			if (rows00.length() > 0) {
				rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
			}
			dr.putSkuSupplierAID(items);
			arremangalos(newAttributeValues);
		} else {
			log("Malformed file content...");
		}
	}

	private String conciliaRelacionArticuloProducto(String articleId, String sku, String currentParentId, String satnr,
			java.util.Map<String, String> articleHigherLevelProductNotReadyYet, DataRequestor dr) {
		if (articleId == null || "".equals(articleId) || "null".equals(articleId)) {
			return null;
		}

		if (currentParentId == null || "".equals(currentParentId) || "null".equals(currentParentId)) {
			String response = dr.getProductByVariant(new org.json.JSONArray().put(articleId));
			if (response != null) {
				try {
					org.json.JSONObject jr = new org.json.JSONObject(response);
					org.json.JSONArray items = jr.getJSONArray("items");
					if (items.length() > 0 && !"".equals(items.getString(0))) {
						currentParentId = items.getString(0);
					}
				} catch (org.json.JSONException e) {
					logE(e);
				}
			}
		}

		/* Si ya tenía padre, ese padre manda. SATNR no re-parenta. */
		if (currentParentId != null && !"".equals(currentParentId) && !"null".equals(currentParentId)) {
			if (satnr != null && !"".equals(satnr)) {
				String targetFromSatnr = resolveProductIdBySku(satnr, dr);
				if (targetFromSatnr != null && !currentParentId.equals(targetFromSatnr)) {
					log("Se conserva relación existente. articleId=" + articleId + ", sku=" + sku
							+ ", currentParent=" + currentParentId + ", SATNR resolvería a=" + targetFromSatnr);
				}
			}
			articleHigherLevelProduct.put(articleId, currentParentId);
			registerArticleParentInAdmin(articleId, sku, currentParentId, dr);
			return currentParentId;
		}

		if (satnr == null || "".equals(satnr)) {
			log("Artículo sin padre y sin SATNR. articleId=" + articleId + ", sku=" + sku);
			return null;
		}

		String targetParentId = resolveProductIdBySku(satnr, dr);
		if (targetParentId == null || "".equals(targetParentId)) {
			articleHigherLevelProductNotReadyYet.put(articleId, satnr);
			log("Padre pendiente. articleId=" + articleId + ", sku=" + sku + ", satnr=" + satnr);
			return null;
		}

		articleHigherLevelProduct.put(articleId, targetParentId);
		registerArticleParentInAdmin(articleId, sku, targetParentId, dr);
		return targetParentId;
	}

	private void registerArticleParentInAdmin(String articleId, String sku, String productNo, DataRequestor dr) {
		org.json.JSONArray items = new org.json.JSONArray();
		items.put(new org.json.JSONObject()
				.put("supplierAID", articleId)
				.put("sku", sku)
				.put("productNo", productNo));
		log("From conciliación conservadora: " + dr.putSkuSupplierAID(items));
	}

	private boolean excluyeAtributo(Value v) {

//		+ ",Product2GCharacteristicValueLang.Value('ZLAEPQ',root,\"0000.0000.RK\",'ZLAEPQ',-1)"
//		+ ",Product2GCharacteristicValueLang.Value('ZBREPQ',root,\"0000.0000.RK\",'ZBREPQ',-1)"
//		+ ",Product2GCharacteristicValueLang.Value('ZHOEPQ',root,\"0000.0000.RK\",'ZHOEPQ',-1)"
//		+ ",Product2GCharacteristicValueLang.Value('ZVOLPQ',root,\"0000.0000.RK\",'ZVOLPQ',-1)"
//		+ ",Product2GCharacteristicValueLang.Value('ZBRGPQ',root,\"0000.0000.RK\",'ZBRGPQ',-1)"
//		+ ",Product2GCharacteristicValueLang.Value('ZNTGPQ',root,\"0000.0000.RK\",'ZNTGPQ',-1)"
		java.util.List<String> mPack = new java.util.ArrayList<>();
		mPack.add("ZLAEPQ");
		mPack.add("ZBREPQ");
		mPack.add("ZHOEPQ");
		mPack.add("ZVOLPQ");
		mPack.add("ZBRGPQ");
		mPack.add("ZNTGPQ");
		mPack.add("FIBER_PART1");
		mPack.add("FIBER_PART2");
		mPack.add("FIBER_PART3");
		mPack.add("FIBER_PART4");
		mPack.add("FIBER_PART5");
		if (mPack.contains(v.getAttributeId())) {
			try {
				float val = Float.parseFloat(v.getText());
				return val == 0f;
			} catch (NumberFormatException | NullPointerException e) {

			}
		}
		return false;
	}

	/*************************************************************
	 * Helpers homologados desde ParseECC122Response.
	 *************************************************************/

	private String conciliaRelacionArticuloProducto(String articleId, String sku, String currentParentId, String satnr,
			java.util.Map<String, String> articleHigherLevelProduct,
			java.util.Map<String, String> articleHigherLevelProductNotReadyYet, DataRequestor dr) {
		String parentId = conciliaRelacionArticuloProducto(
				articleId, sku, currentParentId, satnr, articleHigherLevelProductNotReadyYet, dr);
		if (parentId != null && articleHigherLevelProduct != null) {
			articleHigherLevelProduct.put(articleId, parentId);
		}
		return parentId;
	}

	private String resolveProductIdBySku(String sku, DataRequestor dr) {
		if (sku == null || "".equals(sku)) {
			return null;
		}

		String response = dr.productBySKU(new org.json.JSONArray().put(sku));

		if (response != null) {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(response);
				org.json.JSONArray items = jr.getJSONArray("items");

				if (items.length() > 0 && !"".equals(items.getString(0))) {
					return items.getString(0);
				}
			} catch (org.json.JSONException e) {
				logE(e);
			}
		}

		String[] p360Parent = tools.checkProductBySKUOnP360(sku);

		if (p360Parent != null) {
			return p360Parent[0];
		}

		return null;
	}

	private void resuelveCombinación(String id1, String id2) {
		if (id1 != null && id2 != null && !id1.equals(id2)) {
			org.json.JSONObject response1 = rw.getRw().makeRequest("GET",
					"/object/Product2G/'" + rw.getRw().encode(id1) + "'@1?includeIds=true&includeLabels=true");
			org.json.JSONObject response2 = rw.getRw().makeRequest("GET",
					"/object/Product2G/'" + rw.getRw().encode(id2) + "'@1?includeIds=true&includeLabels=true");

			org.json.JSONObject data1 = response1 != null && response1.has("_data") ? response1.getJSONObject("_data")
					: null;
			org.json.JSONObject data2 = response2 != null && response2.has("_data") ? response2.getJSONObject("_data")
					: null;

			if (data1 != null && data2 != null) {

				/**********************************************************
				 * 
				 * Hacer la lógica de comparación y merge y todo en data1
				 * 
				 ***************************************************************/

				java.util.List<String> itemsOfProduct1 = collectArticleObjectIdsByProduct(id1);
				java.util.List<String> itemsOfProduct2 = collectArticleObjectIdsByProduct(id2);

				java.util.List<VariantInfo> variants1 = loadVariantInfos(itemsOfProduct1, "id1");
				java.util.List<VariantInfo> variants2 = loadVariantInfos(itemsOfProduct2, "id2");

				java.util.Map<String, java.util.List<VariantInfo>> signatureIndex = buildSignatureIndex(variants1,
						variants2);

				VariantMergeDecision decision = decideVariantMovement(variants2, signatureIndex);

				deleteProductReferencesFromArticles(itemsOfProduct2);

				createProductReferencesToId1(toArticleObjectIds(decision.articlesToMoveToProduct1), id1);

				clearSkuAndEanFromArticles(decision.duplicatedArticlesToDetachAndClean);

				mergeMissingProductData(data1, data2);
				clearProductSkuAndEan(data2);

				java.util.Map<String, String> qp = new java.util.HashMap<>();

				org.json.JSONObject write1 = rw.getRw().makeRequest("PUT",
						"/object/Product2G/'" + rw.getRw().encode(id1) + "'@1", qp, data1.toString());

				if (write1 == null) {
					log("PANIC: from id1=" + id1 + ". rawResponse=" + rw.getRw().getRawResponse());
					return;
				}

				qp.clear();

				org.json.JSONObject write2 = rw.getRw().makeRequest("PUT",
						"/object/Product2G/'" + rw.getRw().encode(id2) + "'@1", qp, data2.toString());

				if (write2 == null) {
					log("PANIC: from id2=" + id2 + ". rawResponse=" + rw.getRw().getRawResponse());
					return;
				}

				log("Combinación finished. id1=" + id1 + ", id2=" + id2 + ", moveToId1="
						+ decision.articlesToMoveToProduct1.size() + ", cleanDuplicated="
						+ decision.duplicatedArticlesToDetachAndClean.size());

			}
		}
	}

	private java.util.List<String> toArticleObjectIds(java.util.List<VariantInfo> variants) {
		java.util.List<String> ids = new java.util.ArrayList<>();

		for (VariantInfo variant : variants) {
			ids.add(variant.articleObjectId);
		}

		return ids;
	}

	private void mergeMissingProductData(org.json.JSONObject data1, org.json.JSONObject data2) {
		java.util.Set<String> excluded = new java.util.HashSet<>();

		excluded.add("identifier");
		excluded.add("sku");
		excluded.add("gtin");
		excluded.add("statusModification");
		excluded.add("log");
		excluded.add("ownLog");

		mergeObjectMissing(data1, data2, excluded);
	}

	private void clearProductSkuAndEan(org.json.JSONObject data) {
		data.put("sku", org.json.JSONObject.NULL);
		data.put("gtin", org.json.JSONObject.NULL);
		clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);
	}

	private java.util.List<VariantInfo> loadVariantInfos(java.util.List<String> articleObjectIds, String productOwner) {
		java.util.List<VariantInfo> result = new java.util.ArrayList<>();

		for (String articleObjectId : articleObjectIds) {
			org.json.JSONObject response = rw.getRw().makeRequest("GET",
					"/object/Article/" + articleObjectId + "?includeIds=true&includeLabels=true");

			org.json.JSONObject data = response != null && response.has("_data") ? response.getJSONObject("_data")
					: null;

			if (data == null) {
				log("No se pudo leer Article. owner=" + productOwner + ", article=" + articleObjectId);
				continue;
			}

			java.util.Set<String> signatures = buildVariantSignatures(data);

			if (signatures.isEmpty()) {
				log("Article sin firma útil. owner=" + productOwner + ", article=" + articleObjectId);
			}

			result.add(new VariantInfo(articleObjectId, productOwner, data, signatures));
		}

		return result;
	}

	private java.util.Set<String> buildVariantSignatures(org.json.JSONObject data) {
		java.util.Set<String> signatures = new java.util.LinkedHashSet<>();

		String sku = stringValue(data.opt("sku"));
		String gtin = stringValue(data.opt("gtin"));

		if (!isBlank(sku)) {
			signatures.add("SKU|" + normalize(sku));
		}

		if (!isBlank(gtin)) {
			signatures.add("EAN|" + normalize(gtin));
		}

		java.util.Map<String, String> characteristicValues = extractCharacteristicValues(data);

		addSignatureIfPresent(signatures, "SKU", characteristicValues.get("SKU"));
		addSignatureIfPresent(signatures, "MainBarCode", characteristicValues.get("MainBarCode"));
		addSignatureIfPresent(signatures, "MainBarCodeS4H", characteristicValues.get("MainBarCodeS4H"));

		String color = firstNotBlank(extractMxExtraDataValue(data, "coloursLiverpoolAtt"),
				characteristicValues.get("ColoursLiverpoolAtt"));

		String size = firstNotBlank(extractMxExtraDataValue(data, "tamanoUnico"),
				characteristicValues.get("TamanoUnico"));

		String supplierPartNumber = firstNotBlank(extractMxExtraDataValue(data, "supplierPartNumber"),
				characteristicValues.get("SupplierPartNumber"));

		if (!isBlank(color) && !isBlank(size) && !isBlank(supplierPartNumber)) {
			signatures.add("COLOR_SIZE_MODEL|" + normalize(color) + "|" + normalize(size) + "|"
					+ normalize(supplierPartNumber));
		}

		return signatures;
	}

	private java.util.Map<String, String> extractCharacteristicValues(org.json.JSONObject data) {
		java.util.Map<String, String> valuesByCode = new java.util.HashMap<>();

		org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

		if (records == null) {
			return valuesByCode;
		}

		for (int i = 0; i < records.length(); i++) {
			org.json.JSONObject record = records.optJSONObject(i);

			if (record == null) {
				continue;
			}

			String code = nestedValue(record, "_qualification.characteristic._code");

			if (isBlank(code)) {
				continue;
			}

			if (!VARIANT_MATCH_CHARACTERISTICS.contains(code)) {
				continue;
			}

			String value = firstRecordValue(record);

			if (!isBlank(value)) {
				valuesByCode.put(code, value);
			}
		}

		return valuesByCode;
	}

	private String firstRecordValue(org.json.JSONObject record) {
		org.json.JSONArray recordLang = record.optJSONArray("_recordLang");

		if (recordLang == null) {
			return "";
		}

		for (int i = 0; i < recordLang.length(); i++) {
			org.json.JSONObject lang = recordLang.optJSONObject(i);

			if (lang == null) {
				continue;
			}

			org.json.JSONArray values = lang.optJSONArray("values");

			if (values == null || values.length() == 0) {
				continue;
			}

			Object value = values.opt(0);

			if (value == null || value == org.json.JSONObject.NULL) {
				continue;
			}

			if (value instanceof org.json.JSONObject) {
				org.json.JSONObject valueObject = (org.json.JSONObject) value;

				String code = valueObject.optString("_code", "");
				if (!isBlank(code)) {
					return code;
				}

				String label = valueObject.optString("_label", "");
				if (!isBlank(label)) {
					return label;
				}

				org.json.JSONObject key = valueObject.optJSONObject("_key");
				if (key != null) {
					String externalId = key.optString("_externalId", "");
					if (!isBlank(externalId)) {
						return externalId;
					}

					String internalId = key.optString("_internalId", "");
					if (!isBlank(internalId)) {
						return internalId;
					}
				}

				return valueObject.toString();
			}

			return String.valueOf(value).trim();
		}

		return "";
	}

	private String extractMxExtraDataValue(org.json.JSONObject data, String fieldName) {
		String value = extractMxExtraDataValueFromArray(data.optJSONArray("extraData"), fieldName);

		if (!isBlank(value)) {
			return value;
		}

		return extractMxExtraDataValueFromArray(data.optJSONArray("productExtraData"), fieldName);
	}

	private String extractMxExtraDataValueFromArray(org.json.JSONArray array, String fieldName) {
		if (array == null) {
			return "";
		}

		for (int i = 0; i < array.length(); i++) {
			org.json.JSONObject item = array.optJSONObject(i);

			if (item == null) {
				continue;
			}

			String targetMarket = firstNotBlank(nestedValue(item, "_qualification.targetMarket._code"),
					nestedValue(item, "_qualification.targetMarket._key"),
					nestedValue(item, "_qualification.targetMarket._label"));

			if (!"MX".equalsIgnoreCase(targetMarket) && !"Mexico".equalsIgnoreCase(targetMarket)) {
				continue;
			}

			Object rawValue = item.opt(fieldName);

			if (rawValue == null || rawValue == org.json.JSONObject.NULL) {
				continue;
			}

			if (rawValue instanceof org.json.JSONObject) {
				org.json.JSONObject object = (org.json.JSONObject) rawValue;

				String code = object.optString("_code", "");
				if (!isBlank(code)) {
					return code;
				}

				String label = object.optString("_label", "");
				if (!isBlank(label)) {
					return label;
				}

				org.json.JSONObject key = object.optJSONObject("_key");
				if (key != null) {
					String externalId = key.optString("_externalId", "");
					if (!isBlank(externalId)) {
						return externalId;
					}

					String internalId = key.optString("_internalId", "");
					if (!isBlank(internalId)) {
						return internalId;
					}
				}

				return object.toString();
			}

			return String.valueOf(rawValue).trim();
		}

		return "";
	}

	private void addSignatureIfPresent(java.util.Set<String> signatures, String name, String value) {
		if (!isBlank(value)) {
			signatures.add(name + "|" + normalize(value));
		}
	}

	private String stringValue(Object value) {
		if (value == null || value == org.json.JSONObject.NULL) {
			return "";
		}

		return String.valueOf(value).trim();
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}

		return value.trim().toUpperCase(java.util.Locale.ROOT);
	}

	private java.util.Map<String, java.util.List<VariantInfo>> buildSignatureIndex(
			java.util.List<VariantInfo> variants1, java.util.List<VariantInfo> variants2) {

		java.util.Map<String, java.util.List<VariantInfo>> index = new java.util.LinkedHashMap<>();

		addToSignatureIndex(index, variants1);
		addToSignatureIndex(index, variants2);

		return index;
	}

	private void addToSignatureIndex(java.util.Map<String, java.util.List<VariantInfo>> index,
			java.util.List<VariantInfo> variants) {

		for (VariantInfo variant : variants) {
			for (String signature : variant.signatures) {
				index.computeIfAbsent(signature, k -> new java.util.ArrayList<>()).add(variant);
			}
		}
	}

	private void deleteProductReferencesFromArticles(java.util.List<String> itemsOfTheProduct) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		StringBuilder sb = new StringBuilder();
		int a = 0;

		for (String internalArticleId : itemsOfTheProduct) {
			sb.append(sb.length() == 0 ? "" : ",").append(internalArticleId);
			a++;

			if (a % 1000 == 0) {
				qp.put("items", sb.toString());
				rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
				sb.setLength(0);
				qp.clear();
			}
		}

		if (sb.length() > 0) {
			qp.put("items", sb.toString());
			rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
			sb.setLength(0);
			qp.clear();
		}
	}

	private void createProductReferencesToId1(java.util.List<String> itemsOfTheProduct, String id1) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");

		RequestHandler rh = new RequestHandler(
				new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")),
				1000, request -> rw.writeData("list", "Article", "ProductReference", qp, request, this::log));

		for (String internalId : itemsOfTheProduct) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId))
					.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", id1))
					.put("values", new org.json.JSONArray().put(id1)));
		}

		rh.sendData();
	}

	private void mergeObjectMissing(org.json.JSONObject target, org.json.JSONObject source,
			java.util.Set<String> excludedKeys) {
		for (Object keyObject : source.keySet()) {
			String key = String.valueOf(keyObject);

			if (excludedKeys != null && excludedKeys.contains(key)) {
				continue;
			}

			Object sourceValue = source.opt(key);

			if (isEmptyJsonValue(sourceValue)) {
				continue;
			}

			Object targetValue = target.opt(key);

			if (isEmptyJsonValue(targetValue)) {
				target.put(key, cloneJsonValue(sourceValue));
				continue;
			}

			if (sourceValue instanceof org.json.JSONObject && targetValue instanceof org.json.JSONObject) {
				mergeObjectMissing((org.json.JSONObject) targetValue, (org.json.JSONObject) sourceValue, null);
				continue;
			}

			if (sourceValue instanceof org.json.JSONArray && targetValue instanceof org.json.JSONArray) {
				mergeArrayMissing(key, (org.json.JSONArray) targetValue, (org.json.JSONArray) sourceValue);
			}
		}
	}

	private java.util.List<String> collectArticleObjectIdsByProduct(String productIdentifier) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "2000");
		qp.put("products", "'" + productIdentifier + "'@1");

		java.util.List<String> items = new java.util.ArrayList<>();

		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			items.add(row.getJSONObject("object").getString("id"));
		});

		return items;
	}

	private void mergeArrayMissing(String sectionName, org.json.JSONArray targetArray, org.json.JSONArray sourceArray) {
		java.util.Map<String, org.json.JSONObject> targetByKey = new java.util.LinkedHashMap<>();

		for (int i = 0; i < targetArray.length(); i++) {
			Object value = targetArray.opt(i);

			if (value instanceof org.json.JSONObject) {
				org.json.JSONObject object = (org.json.JSONObject) value;
				targetByKey.put(buildArrayItemKey(sectionName, object), object);
			}
		}

		for (int i = 0; i < sourceArray.length(); i++) {
			Object sourceValue = sourceArray.opt(i);

			if (!(sourceValue instanceof org.json.JSONObject)) {
				if (!arrayContainsEquivalentValue(targetArray, sourceValue)) {
					targetArray.put(cloneJsonValue(sourceValue));
				}
				continue;
			}

			org.json.JSONObject sourceObject = (org.json.JSONObject) sourceValue;
			String sourceKey = buildArrayItemKey(sectionName, sourceObject);
			org.json.JSONObject targetObject = targetByKey.get(sourceKey);

			if (targetObject == null) {
				targetArray.put(new org.json.JSONObject(sourceObject.toString()));
			} else {
				mergeObjectMissing(targetObject, sourceObject, null);
			}
		}
	}

	private String buildArrayItemKey(String sectionName, org.json.JSONObject object) {
		if ("lang".equals(sectionName)) {
			return "lang|" + nestedValue(object, "_qualification.language._key");
		}

		if ("structureGroupMap".equals(sectionName)) {
			return "structureGroupMap|" + objectKey(object.optJSONObject("_qualification"), "structureGroup");
		}

		if ("attribute".equals(sectionName)) {
			String identifier = object.optString("identifier", "");
			if (!isBlank(identifier)) {
				return "attribute|" + identifier;
			}

			return "attribute|" + nestedValue(object, "_qualification.nameInKeyLang");
		}

		if ("_characteristicRecords".equals(sectionName)) {
			String characteristic = objectKey(object.optJSONObject("_qualification"), "characteristic");
			String recordKey = nestedValue(object, "_qualification.recordKey");
			String parentRecordKey = nestedValue(object, "_qualification.parentRecordKey");
			return "_characteristicRecords|" + characteristic + "|" + recordKey + "|" + parentRecordKey;
		}

		if ("productExtraData".equals(sectionName)) {
			return "productExtraData|" + objectKey(object.optJSONObject("_qualification"), "targetMarket");
		}

		if ("value".equals(sectionName)) {
			String lang = nestedValue(object, "_qualification.language._key");
			String identifier = nestedValue(object, "_qualification.identifier");
			return "value|" + lang + "|" + identifier;
		}

		if ("_recordLang".equals(sectionName)) {
			return "_recordLang|" + nestedValue(object, "_qualification.language._key");
		}

		return sectionName + "|" + object.toString();
	}

	private String objectKey(org.json.JSONObject parent, String childName) {
		if (parent == null) {
			return "";
		}

		org.json.JSONObject child = parent.optJSONObject(childName);

		if (child == null) {
			return "";
		}

		org.json.JSONObject key = child.optJSONObject("_key");

		if (key != null) {
			String externalId = key.optString("_externalId", "");
			String internalId = key.optString("_internalId", "");
			String entityId = String.valueOf(key.opt("_entityId"));
			return firstNotBlank(externalId, internalId, entityId);
		}

		String externalId = child.optString("_externalId", "");
		String internalId = child.optString("_internalId", "");
		String code = child.optString("_code", "");
		String keyValue = String.valueOf(child.opt("_key"));

		return firstNotBlank(externalId, internalId, code, keyValue);
	}

	private String nestedValue(org.json.JSONObject object, String path) {
		if (object == null || isBlank(path)) {
			return "";
		}

		String[] parts = path.split("\\.");
		Object current = object;

		for (String part : parts) {
			if (!(current instanceof org.json.JSONObject)) {
				return "";
			}

			current = ((org.json.JSONObject) current).opt(part);

			if (current == null || current == org.json.JSONObject.NULL) {
				return "";
			}
		}

		return String.valueOf(current);
	}

	private void clearSkuAndEanFromArticles(java.util.List<VariantInfo> variants) {
		for (VariantInfo variant : variants) {
			org.json.JSONObject data = variant.data;

			data.put("sku", org.json.JSONObject.NULL);
			data.put("gtin", org.json.JSONObject.NULL);

			clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);

			org.json.JSONObject writeResponse = rw.getRw().makeRequest("PUT",
					"/object/Article/" + variant.articleObjectId, new java.util.HashMap<>(), data.toString());

			if (writeResponse == null) {
				log("PANIC: fallo PUT Article " + variant.articleObjectId + ". rawResponse="
						+ rw.getRw().getRawResponse());
				return;
			}
		}
	}

	private void clearCharacteristicRecords(org.json.JSONObject data,
			java.util.Set<String> characteristicCodesToClear) {
		org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

		if (records == null) {
			return;
		}

		org.json.JSONArray kept = new org.json.JSONArray();

		for (int i = 0; i < records.length(); i++) {
			org.json.JSONObject record = records.optJSONObject(i);

			if (record == null) {
				kept.put(records.opt(i));
				continue;
			}

			String code = nestedValue(record, "_qualification.characteristic._code");

			if (characteristicCodesToClear.contains(code)) {
				log("Quitando characteristicRecord de identidad: " + code);
				continue;
			}

			kept.put(record);
		}

		data.put("_characteristicRecords", kept);
	}

	private VariantMergeDecision decideVariantMovement(java.util.List<VariantInfo> variants2,
			java.util.Map<String, java.util.List<VariantInfo>> signatureIndex) {

		java.util.List<VariantInfo> articlesToMoveToProduct1 = new java.util.ArrayList<>();
		java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean = new java.util.ArrayList<>();

		for (VariantInfo variant2 : variants2) {
			if (variant2.signatures.isEmpty()) {
				log("Article de id2 sin firma útil; se mueve por conservación. article=" + variant2.articleObjectId);
				articlesToMoveToProduct1.add(variant2);
				continue;
			}

			boolean matchedAgainstId1 = false;
			java.util.Set<String> matchedRefsForLog = new java.util.LinkedHashSet<>();

			for (String signature : variant2.signatures) {
				java.util.List<VariantInfo> refs = signatureIndex.get(signature);

				if (refs == null || refs.isEmpty()) {
					continue;
				}

				for (VariantInfo ref : refs) {
					matchedRefsForLog.add(signature + " -> " + ref.toString());

					if (ref.belongsToProduct1()) {
						matchedAgainstId1 = true;
					}
				}
			}

			if (matchedAgainstId1) {
				duplicatedArticlesToDetachAndClean.add(variant2);
				log("Article duplicado contra id1; se despoja SKU/EAN. article=" + variant2.articleObjectId
						+ ", matches=" + matchedRefsForLog);
			} else {
				articlesToMoveToProduct1.add(variant2);
				log("Article de id2 sin coincidencia contra id1; se mueve a id1. article=" + variant2.articleObjectId
						+ ", matches=" + matchedRefsForLog);
			}
		}

		return new VariantMergeDecision(articlesToMoveToProduct1, duplicatedArticlesToDetachAndClean);
	}

	private static class VariantMergeDecision {
		final java.util.List<VariantInfo> articlesToMoveToProduct1;
		final java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean;

		VariantMergeDecision(java.util.List<VariantInfo> articlesToMoveToProduct1,
				java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean) {

			this.articlesToMoveToProduct1 = articlesToMoveToProduct1;
			this.duplicatedArticlesToDetachAndClean = duplicatedArticlesToDetachAndClean;
		}
	}

	private static class VariantInfo {
		final String articleObjectId;
		final String productOwner;
		final org.json.JSONObject data;
		final java.util.Set<String> signatures;

		VariantInfo(String articleObjectId, String productOwner, org.json.JSONObject data,
				java.util.Set<String> signatures) {
			this.articleObjectId = articleObjectId;
			this.productOwner = productOwner;
			this.data = data;
			this.signatures = signatures;
		}

		boolean belongsToProduct1() {
			return "id1".equals(productOwner);
		}

		@Override
		public String toString() {
			return productOwner + ":" + articleObjectId;
		}
	}

	private boolean arrayContainsEquivalentValue(org.json.JSONArray array, Object value) {
		String valueString = String.valueOf(value);

		for (int i = 0; i < array.length(); i++) {
			Object current = array.opt(i);

			if (String.valueOf(current).equals(valueString)) {
				return true;
			}
		}

		return false;
	}

	private Object cloneJsonValue(Object value) {
		if (value instanceof org.json.JSONObject) {
			return new org.json.JSONObject(((org.json.JSONObject) value).toString());
		}

		if (value instanceof org.json.JSONArray) {
			return new org.json.JSONArray(((org.json.JSONArray) value).toString());
		}

		return value;
	}

	private boolean isEmptyJsonValue(Object value) {
		if (value == null || value == org.json.JSONObject.NULL) {
			return true;
		}

		if (value instanceof String) {
			return ((String) value).trim().isEmpty();
		}

		if (value instanceof org.json.JSONArray) {
			return ((org.json.JSONArray) value).length() == 0;
		}

		if (value instanceof org.json.JSONObject) {
			return ((org.json.JSONObject) value).length() == 0;
		}

		return false;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String firstNotBlank(String... values) {
		if (values == null) {
			return "";
		}

		for (String value : values) {
			if (!isBlank(value) && !"null".equalsIgnoreCase(value)) {
				return value;
			}
		}

		return "";
	}

	private void resolveVariantParent(String znprst, String pid, String satnr) {
		/*
		 * No se borra ProductReference aquí. La conciliación conservadora posterior
		 * conserva el padre actual y sólo usa SATNR cuando el Article está huérfano.
		 */
		log("resolveVariantParent conservador: no se elimina relación existente. articleId="
				+ znprst + ", currentParent=" + pid + ", satnr=" + satnr);
	}

	private String chooseProperProductZNPRST(String sku, String znprst) {
		String chosenOne = znprst;
		String[] info = tools.checkProductBySKU(sku);
		String externalId = null;
		if (info != null && info.length > 0 && znprst != null) {
			externalId = info[0];
			if (!znprst.equals(externalId)) {
				return externalId;
			}
		}
		return chosenOne;
	}

	private String chooseProperArticleZNPRST(String sku, String znprst) {
		String chosenOne = znprst;
		String externalId = tools.checkArticleBySKU(sku);
		if (externalId != null && !znprst.equals(externalId)) {
			return externalId;
		}
		return chosenOne;
	}

	private ProductMergeDecision resuelveEmpateDeProducto(String sku, String znprst) {
		ProductMergeDecision decision = new ProductMergeDecision();
		decision.productIdToUse = znprst;
		decision.shouldMerge = false;
		decision.manualReview = false;

		String[] info = tools.checkProductBySKU(sku);

		if (info == null) {
			decision.reason = "No existing Product2G by SKU";
			return decision;
		}

		String externalId = info[0];

		if (externalId == null || "".equals(externalId) || znprst.equals(externalId)) {
			decision.productIdToUse = znprst;
			decision.reason = "Same Product2G or empty existing";
			return decision;
		}

		String winner = decideProductWinner(znprst, externalId);
		String loser = winner.equals(znprst) ? externalId : znprst;

		decision.productIdToUse = winner;
		decision.id1 = winner;
		decision.id2 = loser;

		if (winner == null || loser == null) {
			decision.manualReview = true;
			decision.reason = "Could not decide winner";
			return decision;
		}

		if (isSameOriginUnsafe(znprst, externalId)) {
			decision.manualReview = true;
			decision.shouldMerge = false;
			decision.reason = "Same-origin duplicate Product2G. current=" + znprst + ", existing=" + externalId;

			log("PANIC, necesita generarse una tarea de resolución de duplicados. (current: " + znprst + " | existing: "
					+ externalId + ")");

			return decision;
		}

		decision.shouldMerge = false;
		decision.reason = "Logical unification only; destructive merge disabled. winner=" + winner + ", loser=" + loser;

		return decision;
	}

	private String decideProductWinner(String currentId, String existingId) {
		int currentRank = originRank(currentId);
		int existingRank = originRank(existingId);

		if (currentRank > existingRank) {
			return currentId;
		}

		/* En empate o si el incoming es peor, conserva el ID que ya tenía el MATNR. */
		return existingId;
	}

	private int originRank(String id) {
		if (id == null || "".equals(id)) {
			return 0;
		}

		if (id.length() == 16) {
			return 3; // P360
		}

		if (id.startsWith("LVP") || id.startsWith("SBB")) {
			return 1; // prototipo/local fallback
		}

		if (id.startsWith("S")) {
			return 2; // STEP
		}

		return 0;
	}

	private boolean isSameOriginUnsafe(String currentId, String existingId) {
		return originRank(currentId) == originRank(existingId);
	}

	private void resuelveEmpateDeArticulo(String sku, String znprst) {

		/***********************************************/

		String externalId = tools.checkArticleBySKU(sku);
		if (externalId != null) {
			if (!znprst.equals(externalId)) {
				if (znprst.length() == 16 && externalId.length() == 16) {
					// PANIC, necesita generarse una tarea de resolución de duplicados
					log("PANIC, necesita generarse una tarea de resolución de duplicados. (current: " + znprst
							+ " | existing: " + externalId + ")");
					java.util.Map<String, String> qp = new java.util.HashMap<>();
					qp.put("includeObjectsInProtocol", "false");
					RequestHandler rh = new RequestHandler(
							new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "ArticleLog.Remarks(es)")),
							1000, request -> rw.writeData("list", "Article", null, qp, request, this::log));
					rh.addRow(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1"))
							.put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + znprst)));
					rh.addRow(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + znprst + "'@1"))
							.put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + externalId)));
					rh.sendData();
				} else if (znprst.startsWith("S") && externalId.startsWith("S")) {
					// PANIC, necesita generarse una tarea de resolución de duplicados
					log("PANIC, necesita generarse una tarea de resolución de duplicados. (current: " + znprst
							+ " | existing: " + externalId + ")");
					java.util.Map<String, String> qp = new java.util.HashMap<>();
					qp.put("includeObjectsInProtocol", "false");
					RequestHandler rh = new RequestHandler(
							new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "ArticleLog.Remarks(es)")),
							1000, request -> rw.writeData("list", "Article", null, qp, request, this::log));
					rh.addRow(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1"))
							.put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + znprst)));
					rh.addRow(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + znprst + "'@1"))
							.put("values", new org.json.JSONArray().put("Otro artículo con mismo SKU: " + externalId)));
					rh.sendData();
				} else if (externalId.startsWith("SBB")) {
					java.util.Map<String, String> qp = new java.util.HashMap<>();
					qp.put("includeObjectsInProtocol", "false");
					RequestHandler rh = new RequestHandler(new org.json.JSONArray()
							.put(new org.json.JSONObject().put("identifier", "Article.SKU"))
							.put(new org.json.JSONObject().put("identifier",
									"ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
							.put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))
							.put(new org.json.JSONObject().put("identifier", "ArticleLog.Remarks(es)")), 1000,
							request -> rw.writeData("list", "Article", null, qp, request, this::log));
					rh.addRow(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values",
									new org.json.JSONArray().put("").put("").put("Eliminada")
											.put("Eliminado por arribo de producto con ID establecido por sistema PIM ("
													+ (znprst.length() == 16 ? "P360" : "STEP") + ")")));
					rh.sendData();
				}
			}
		}

		/***********************************************/

	}

	public void flushPendingWrites() {
		sendData();
	}

	private String determineBusiness(String negocio) {
		return "".equals(negocio) ? null : "MARKETPLACE".equals(negocio) ? "Marketplace" : "Liverpool";
	}

	private java.util.Map<String, String> readFieldSections() {
		try {
			java.util.Map<String, String> data = new java.util.TreeMap<>();
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
					new java.io.FileInputStream(
							java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache",
									"characteristic_vendor_center_sections").toFile()),
					java.nio.charset.StandardCharsets.UTF_8))) {
				String line = null;
				String[] pieces = null;
				while ((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					data.put(pieces[0], pieces[1]);
				}
			}
			return data;
		} catch (IOException e) {
			logE(e);
		}
		return null;
	}

	private java.util.Map<String, String> readECCFieldMappings() {
		try {
			java.util.Map<String, String> data = new java.util.TreeMap<>();
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
					new java.io.FileInputStream(
							java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache",
									"characteristic_ecc_mapping").toFile()),
					java.nio.charset.StandardCharsets.UTF_8))) {
				String line = null;
				String[] pieces = null;
				while ((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					data.put(pieces[0], pieces[1]);
				}
			}
			return data;
		} catch (IOException e) {
			logE(e);
		}
		return null;
	}

	private void arremangalos(java.util.Map<String, java.util.Map<String, String>> newAttributeValues) {
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Authorization", ParseJana122Response.workshop.getRc().getHeader().get("Authorization"));
		rw.setBaseUrl(PropertiesManager.get("p360.contingency.servlets_url"));
		rw.getRc().getHeader().remove("Authorization");
		java.util.Map<String, String> fieldVendorCenterSections = readFieldSections();
		java.util.Map<String, String> eccFieldMapping = readECCFieldMappings();
		String charId = null;
		String vendorCenterSection = null;
		String vendorCenterSectionKey = null;
		org.json.JSONObject msgBody = null;
		org.json.JSONObject section = null;
		java.util.Map<String, String> equivalenciaVCS = cargaMapaDeEquivalenciaDeSeccionDeVendorCenter();
		org.json.JSONArray productos = new org.json.JSONArray();
		String supplier = null;
		java.util.Set<String> proveedoresMigrados = cargaProveedoresMigrados();
		java.util.Map<String, String> equivExt = cargaEquivalenciaEstatusExterno();
		java.util.Map<String, String> extStatus = cargaEstatusExterno();
		String statusCode = null;
		String extwg = null;
		String negocio = null;
		String status = null;
		for (java.util.Map.Entry<String, java.util.Map<String, String>> entry : newAttributeValues.entrySet()) {
			msgBody = new org.json.JSONObject();
			msgBody.put("proposalId", entry.getKey());
			for (java.util.Map.Entry<String, String> entry0 : entry.getValue().entrySet()) {
				charId = eccFieldMapping.get(entry0.getKey());
				if (charId != null) {
					vendorCenterSection = fieldVendorCenterSections.get(charId);
					if (vendorCenterSection != null) {
						vendorCenterSectionKey = equivalenciaVCS.get(vendorCenterSection);
						if (vendorCenterSectionKey != null) {
							section = msgBody.has(vendorCenterSectionKey)
									? msgBody.getJSONObject(vendorCenterSectionKey)
									: null;
							if (section == null) {
								section = new org.json.JSONObject();
								msgBody.put(vendorCenterSectionKey, section);
							}
							section.put(charId, entry0.getValue());
						}
					}
				}
			}
			extwg = entry.getValue().get("EXTWG");
			status = "SFERA".equals(extwg) ? "Gobierno de Datos"
					: "DUTY FREE".equals(extwg) ? "Propuesta Generada"
							: "MARCAS PROPIAS".equals(extwg) ? "Pendiente Inicio Enriquecimiento"
									: "REGULAR".equals(extwg) ? "Pendiente Inicio Enriquecimiento"
											: "SERVICIOS".equals(extwg) ? "Pendiente Inicio Enriquecimiento"
													: "Propuesta Generada";
			statusCode = String.valueOf("SFERA".equals(negocio) ? 1021
					: "DUTY FREE".equals(negocio) ? 1001
							: "MARCAS PROPIAS".equals(negocio) ? 1002
									: "REGULAR".equals(negocio) ? 1002 : "SERVICIOS".equals(negocio) ? 1002 : 1001);
			negocio = determineBusiness(extwg);
			if (negocio != null && !"".equals(negocio)) {
				msgBody.put("Business", negocio);
			}
			if (status != null && !"".equals(status) && statusCode != null && !"".equals(statusCode)) {
				msgBody.put("currentStatus", status);
				status = equivExt.get(statusCode);
				status = extStatus.get(status);
				msgBody.put("externalStatus", status);
				msgBody.put("previousStatus", "1020");
			}
			supplier = entry.getValue().get("LIFNR");
			if (supplier != null) {
				msgBody.put("supplier", supplier);
				if (proveedoresMigrados.contains(supplier)) {
					msgBody.put("owner", "P360");
				} else {
					msgBody.put("owner", "STEP");
				}
			} else {
				if (proveedoresMigrados.isEmpty()) {
					msgBody.put("owner", "STEP");
				} else {
					msgBody.put("owner", "");
				}
			}
			productos.put(msgBody);
		}
		new PubSubGCP().publishMessage(PropertiesManager.get("p360.contingency.gcp.project_back"),
				PropertiesManager.get("p360.contingency.gcp.post_products_topic"),
				PropertiesManager.get("p360.contingency.gcp.service_account_back"),
				new org.json.JSONObject().put("products", productos).toString());
		log("Sent.");
	}

	private void collectNumberOfImages(String productId, String fotosTomaLiverpool) {
		int lacuenta = 0;
		String rsp = dr.getProductData( new org.json.JSONArray().put( productId ));
		org.json.JSONObject jr = new org.json.JSONObject(rsp);
		org.json.JSONArray items = jr.getJSONArray("items");
		org.json.JSONObject j0 = items.getJSONObject(0);
		String business = j0.getString("Business");
		java.util.Set<String> variants = dr.getVariants(productId);
		org.json.JSONArray itms = new org.json.JSONArray();
		variants.forEach(itms::put);
		String rp = dr.getArticleData(itms);
		try {
			jr = new org.json.JSONObject(rp);
			items = jr.getJSONArray("items");
			for(int i=0; i<items.length(); i++) {
				if(!"".equals(items.getJSONObject(i).getString("ProductImage"))) {
					lacuenta++;
				}
			}
		}catch(org.json.JSONException e) {
			logE(e);
		}
		qp.put("includeObjectsInProtocol", "false");
		log("Las imágenes...");
		if("MKP".equals(business) || lacuenta > 0 || ( "Corregido".equals(fotosTomaLiverpool) || (("N".equals(fotosTomaLiverpool) || "".equals(fotosTomaLiverpool)) && lacuenta > 0 ) )) {
			log("1 " + productId);
			reqCurrentStatus.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put(1022)));
			reqPrevStatus.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put(1020)));
//			requestStatus.getJSONArray("rows")
//				.put(new org.json.JSONObject()
//						.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
//						.put("values", new org.json.JSONArray().put(1020).put(1022).put("EnProcesoLiverpool")));
		}else if("Y".equals(fotosTomaLiverpool)) {
			log("2 " + productId);
			reqCurrentStatus.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put(1002)));
			reqPrevStatus.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put(1020)));
//			requestStatus.getJSONArray("rows")
//			.put(new org.json.JSONObject()
//					.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
//					.put("values", new org.json.JSONArray().put(1020).put(1002).put("EnProcesoLiverpool")));
		}else {
			log("3 " + productId);
			reqCurrentStatus.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put(1004)));
			reqPrevStatus.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put(1020)));
//			requestStatus.getJSONArray("rows")
//			.put(new org.json.JSONObject()
//					.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
//					.put("values", new org.json.JSONArray().put(1020).put(1004).put("CargaDeImagen")));
		}
		if(reqCurrentStatus.getJSONArray("rows").length() == 100) {
			rw.writeData("list", "Product2G", null, qp, reqSKU, this::log);
			log("sending bloc (Product2G status, " + reqCurrentStatus.getJSONArray("rows").length() + ")");
			rw.writeData("list", "Product2G", null, qp, reqCurrentStatus, this::log);
		}
		log("Done with pictures and status stuff...");
//		int lacuenta = 0;
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "Article.SupplierAID" + ",ProductReference.ReferencedSupplierAid(\"" + productId + "\")"
//				+ ",ArticleCharacteristicValueLang.Value(ProductImageDetail,\"0000.0000.RK\",\"0000.0000.RK\",ProductImageDetail_URL,-1)"
//				+ ",ArticleCharacteristicValueLang.Value(ProductImage,\"0000.0000.RK\",\"0000.0000.RK\",ProductImage_URL,-1)");
//		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
//		org.json.JSONObject response = null;
//		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
//		if (response != null) {
//			org.json.JSONArray rows = response.getJSONArray("rows");
//			org.json.JSONArray values = null;
//			org.json.JSONArray details = null;
//			org.json.JSONArray principal = null;
//			for (int i = 0; i < rows.length(); i++) {
//				values = rows.getJSONObject(i).getJSONArray("values");
//				details = values.getJSONArray(2);
//				principal = values.getJSONArray(3);
//				for (int j = 0; j < details.length(); j++) {
//					if (!"".equals(details.getString(j)))
//						lacuenta++;
//				}
//				for (int j = 0; j < principal.length(); j++) {
//					if (!"".equals(principal.getString(j)))
//						lacuenta++;
//				}
//			}
//			if ("Corregido".equals(fotosTomaLiverpool) || ("N".equals(fotosTomaLiverpool) && lacuenta > 0)) {
//				data.put("currentStatus", new org.json.JSONObject().put("_key", 1022));
//				data.put("externalStatus", new org.json.JSONObject().put("_code", "EnProcesoLiverpool"));
//			} else if ("Y".equals(fotosTomaLiverpool)) {
//				data.put("currentStatus", new org.json.JSONObject().put("_key", 1002));
//				data.put("externalStatus", new org.json.JSONObject().put("_code", "EnProcesoLiverpool"));
//			} else {
//				data.put("currentStatus", new org.json.JSONObject().put("_key", 1004));
//				data.put("externalStatus", new org.json.JSONObject().put("_code", "CargaDeImagen"));
//			}
//			data.put("previousStatus", new org.json.JSONObject().put("_key", 1020));
//		} else {
//			log("ERROR: " + workshop.getRawResponse());
//		}
	}

	private void agregaUnidadesDeMedida(
			  java.util.Map<String, String> unidades
			, java.util.Map<String, String> eccFieldMapping
			, String externalId
	) {
		java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
		unidadesVolumen.put("unece.unit.FTQ", "PI3");
		unidadesVolumen.put("unece.unit.MTQ", "M3");
		unidadesVolumen.put("unece.unit.GRM", "G");
		String unidadDeMedidaLongitud = null;
		String unidadDeMedidaVolumen = null;
		String unidadDeMedidaPeso = null;
		String[] atributosLongitud = new String[] { "MEABM", "ZMEACJ", "ZMEAPQ" };
		String[] atributosVolumen = new String[] { "VOLEH", "ZVOLEH", "ZVOLEHPQ" };
		String[] atributosPeso = new String[] { "GEWEI", "ZGEWCJ", "ZGEWPQ" };
		String unidadId = null;
		for(String a : atributosLongitud) {
			unidadId = unidades.get( a );
			if(unidadId != null) {
				unidadDeMedidaLongitud = unidadesLongitud.get(unidadId);
				break;
			}
		}
		for(String a : atributosVolumen) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaVolumen = unidadesVolumen.get( unidadId );
				break;
			}
		}
		for(String a : atributosPeso) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaPeso = unidadesPeso.get( unidadId );
				break;
			}
		}
		if(unidadDeMedidaLongitud == null) {
		} else {
			log("Adding la iunit (long): " + unidadDeMedidaLongitud);
			addValue("UnidadDeMedidaLongitud", "Product2G", externalId, unidadDeMedidaLongitud);
		}
		if(unidadDeMedidaPeso == null) {
		}else {
			log("Adding la iunit (peso): " + unidadDeMedidaPeso);
			addValue("UnidadDeMedidaPeso", "Product2G", externalId, unidadDeMedidaPeso);
		}
		if(unidadDeMedidaVolumen == null) {
		}else {
			log("Adding la iunit (vol): " + unidadDeMedidaVolumen);
			addValue("UnidadDeMedidaVolumen", "Product2G", externalId, unidadDeMedidaVolumen);
		}
	}

	private String getArticleIdFromProduct(String productId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if (response != null) {
			if (!response.has("rows")) {
				log("ERROR: " + response);
				return null;
			}
			rows = response.getJSONArray("rows");
			if (rows.length() == 1) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			}
		} else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}

	private java.util.Map<String, String> cargaEstatusExterno() {
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
				new java.io.FileInputStream(
						java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"),
								"global_lookups", "ExternalStatus").toFile()),
				java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Map<String, String> map = new java.util.HashMap<>();
			String line = null;
			String[] pieces = null;
			while ((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				map.put(pieces[0], pieces.length == 1 ? "" : pieces[1]);
			}
			return map;
		} catch (java.io.IOException e) {
			logE(e);
		}
		return null;
	}

	private java.util.Map<String, String> cargaEquivalenciaEstatusExterno() {
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
				new java.io.FileInputStream(
						java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"),
								"dictionaries", "ExternalStatus").toFile()),
				java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Map<String, String> map = new java.util.HashMap<>();
			String line = null;
			String[] pieces = null;
			while ((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				map.put(pieces[0], pieces.length == 1 ? "" : pieces[1]);
			}
			return map;
		} catch (java.io.IOException e) {
			logE(e);
		}
		return null;
	}

	private java.util.Set<String> cargaProveedoresMigrados() {
		try (java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.InputStreamReader(
						new java.io.FileInputStream(
								java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"),
										"cache", "proveedores_migrados").toFile()),
						java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Set<String> st = new java.util.TreeSet<>();
			String line = null;
			while ((line = br.readLine()) != null) {
				st.add(line);
			}
			return st;
		} catch (java.io.IOException e) {
			logE(e);
		}
		return null;
	}

	private java.util.Map<String, String> cargaMapaDeEquivalenciaDeSeccionDeVendorCenter() {
		java.util.Map<String, String> translation = new java.util.TreeMap<>();
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(
				new java.io.FileInputStream(
						java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"),
								"dictionaries", "SeccionesEntradaUnicaCatalogacion").toFile()),
				java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Map<String, String> map = new java.util.HashMap<>();
			String line = null;
			String[] pieces = null;
			while ((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				map.put(pieces[0], pieces.length == 1 ? "" : pieces[1]);
			}
			return map;
		} catch (java.io.IOException e) {
			logE(e);
		}
		return translation;
	}

	private void agregaClasificacion(String itemGroup, String product, String id) throws java.io.IOException {
		if(itemGroup == null || "".equals(itemGroup)) {
			return;
		}
		int internalId = forbiddenChalice(itemGroup, product);
		requestCommercialS4H.getJSONArray("rows")
			.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(internalId + "@10001")))
		;
		if(requestCommercialS4H.getJSONArray("rows").length() == 10000) {
			rw.writeData("list", "Product2G", null, qp, requestCommercialS4H, this::log);
		}
	}
	
	private void sendWriteRequestProduct(String id, String itemGroup, String product, String negocio) {
//		if ("MARCAS PROPIAS".equals(negocio) || "REGULAR".equals(negocio) || "SERVICIOS".equals(negocio)) {
			addValue("EnriquecidoEnForo", "Product2G", id, true);
//		}
		try {
			agregaClasificacion(itemGroup, product, id);
		} catch (IOException e) {
			logE(e);
		}
		reqCurrentStatus.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(1002)));
		if(reqCurrentStatus.getJSONArray("rows").length() == 100) {
			rw.writeData("list", "Product2G", null, qp, reqCurrentStatus, this::log);
		}
		org.json.JSONArray rows = reqSKU.getJSONArray("rows");
		if (rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
		}
//		response = workshop.makeRequest("PUT", "/object/Product2G/'" + id + "'@'MASTER'", qp, request.toString());
//		if (response != null) {
//			log("\tWriting: " + request + "\nNot really an error from writing id: " + id + ": " + response);
//		} else {
//			log("ERR: " + workshop.getRawResponse());
//		}
	}

	private void sendWriteRequest(
			  String entity
			, String id
			, org.json.JSONArray characteristicRecords
			, String fotoTomadaLiverpool
			, String currentStatus
			, String sku
	) {
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		org.json.JSONObject request = new org.json.JSONObject();
//		request.put("_characteristicRecords", characteristicRecords);
		log("Sending write request with; entity: " + entity + ", id: " + id + ", fotoTomadaLiverpool: " + fotoTomadaLiverpool + ", currentStatus: " + currentStatus + ", sku: " + sku);
		if ("Product2G".equals(entity) && currentStatus != null && "1020".equals(currentStatus)) {
			collectNumberOfImages(id, fotoTomadaLiverpool);
		}
		if(sku != null && !"".equals(sku)) {
			if ("Product2G".equals(entity)) {
				org.json.JSONArray rows = reqSKU.getJSONArray("rows");
				if (rows.length() > 0) {
					rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
				}
			} else {
				org.json.JSONArray rows = reqSKUA.getJSONArray("rows");
				if (rows.length() > 0) {
					rw.writeData("list", "Article", null, qp0, reqSKUA, this::log);
				}
			}
		}
//		org.json.JSONObject response = null;
//		log("Blerg: /object/" + entity + "/'" + id + "'@'MASTER'");
//		response = workshop.makeRequest("PUT", "/object/" + entity + "/'" + id + "'@'MASTER'", qp, request.toString());
//		if (response != null) {
//			log("\tWriting: " + request + "\nNot really an error from writing id: " + id + ": " + response);
//		} else {
//			log("ERR: " + workshop.getRawResponse());
//		}
	}

	private void collectCharacteristicsByEntity(java.util.LinkedList<String> product2G,
			java.util.LinkedList<String> article) {
		try (java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths
				.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_entities"))) {
			lns.forEach(s -> {
				String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
				String[] entities = workshop.parseLine(pieces[1], "\"", ",", "\\");
				for (int i = 0; i < entities.length; i++) {
					if ("Product2G".equals(entities[i])) {
						product2G.addLast(pieces[0]);
					} else if ("Article".equals(entities[i])) {
						article.addLast(pieces[0]);
					}
				}
			});
		} catch (java.io.IOException e) {
			logE(e);
		}
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
		if(rows.length() == 100) {
			rw.writeData("list", entity, null, qp, request, this::log);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}

	private void addValue(String name, String entity, String externalId, Object value) {
		if(value == null)
			return;
		creaPeticion(entity, externalId, name, value);
	}

//	private void addValue(String name, Object value, org.json.JSONArray values) {
//		if (value == null)
//			return;
//		values.put(
//				new org.json.JSONObject()
//						.put("_qualification",
//								new org.json.JSONObject().put("characteristic",
//										new org.json.JSONObject().put("_code", name)))
//						.put("_recordLang", new org.json.JSONArray()
//								.put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)))));
//	}
	
	private void collectLookupCharacteristics(java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> lkps){
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] erg = null;
			while((line = br.readLine()) != null) {
				erg = workshop.parseLine(line, "\"", ";", "\\");
				if(erg != null && erg.length == 6) {
					characteristicsInfo.put(erg[0], erg[1]);
					lkps.put(erg[0], erg[5]);
				}else {
					log("Pieza malformada: --->" + line + "<---");
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
	}

//	private void collectLookupCharacteristics(String names, java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> s4hMapping) {
//		java.util.Set<String> pieces = new java.util.TreeSet<>(java.util.Arrays.asList(names.split(",")));
//		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
//		try (java.io.BufferedReader br = new java.io.BufferedReader(
//				new java.io.InputStreamReader(
//						new java.io.FileInputStream(
//								java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"),
//										"cache", "characteristics").toFile()),
//						java.nio.charset.StandardCharsets.UTF_8))) {
//			String line = null;
//			String[] pcs = null;
//			while ((line = br.readLine()) != null) {
//				pcs = workshop.parseLine(line, "\"", ";", "\\");
//				if ("SKU".equals(pcs[0])) {
//					log("\n\t" + pcs.length + " - " + java.util.Arrays.asList(pcs) + "\n");
//				}
//				if (pcs.length == 6 && !"".equals(pcs[3]))
//					data.put(pcs[3], pcs);
//			}
//		} catch (java.io.IOException e) {
//			logE(e);
//		}
//		String[] d;
//		for (String piece : pieces) {
//			d = data.get(piece);
//			if (d != null) {
//				if ("MATNR".equals(piece)) {
//					log("MATNR >--< " + java.util.Arrays.asList(d));
//				}
//				characteristicsInfo.put(d[0], d[1]);
//				s4hMapping.put(d[0], d[5]);
//			} else {
//			}
//		}
//	}
	
	private void calculaProductType(String sapBehvo1, String itemGroup, String fshId, String negocio, String almacenamientoAtt, String skuType, String mtart, String externalId, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ServiceUnavailableException {
		String sapBehvo = null;
		if("Liverpool".equals(negocio) || "Marketplace".equals(negocio)) {
			int month = Integer.parseInt( new java.text.SimpleDateFormat("MM").format(new java.util.Date()) );
			int year = Integer.parseInt( new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) ) + (month < 11 ? 0 : 1);
			addValue("AnoEstacion", "Product2G", externalId, String.valueOf(year));
			addValue("Temporada", "Product2G", externalId, "0003");
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase", rw);
		}else if("Suburbia".equals(negocio)) {
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase_S4H", rw);
			if(fshId != null && fshId.length() >= 4) {
				addValue("FSH_SEASON_YEAR", "Product2G", externalId, fshId.subSequence(0, 4));
			}
		}
		if(sapBehvo == null || "".equals(sapBehvo)) {
			return;
		}
		sapBehvo = sapBehvo.substring(0,2);
		if(sapBehvo1 == null || "".equals(sapBehvo1)) {
			addValue("SAP_BEHVO", "Product2G", externalId, sapBehvo.substring(0,2));
		}
		if(sapBehvo != null && !"".equals(sapBehvo)) {
			String thevalue = "1";
			try{
				org.json.JSONArray rws = new org.json.JSONObject( rw.getRc().getRequest("GET", rw.getBaseUrl() + "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \"" + sapBehvo.substring(0,2) + "\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8")
						, null) ).getJSONArray("rows");
				if(rws.length() > 0) {
					thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
			addValue("ProductType", "Product2G", externalId, thevalue);
		} else {
			if(almacenamientoAtt != null && !"".equals(almacenamientoAtt) && "0001".equals(almacenamientoAtt) && "SERV".equals(skuType)) {
				addValue("ProductType", "Product2G", externalId, "6");
			}else if("DIEN".equals(mtart) && "SB87516".equals(itemGroup)){
				addValue("ProductType", "Product2G", externalId, "6");
			}else {
				addValue("ProductType", "Product2G", externalId, "1");
			}
		}
	}
	
	private String lookupValue(String value, String standardizationDictionary, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException {
		String container = standardizationDictionary.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				if(value.equals(pieces[0]))
					return pieces[1];
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return null;
	}
	
//	private void calculaProductType(String sapBehvo, String fshId, org.json.JSONArray newCharacteristicRecords,
//			RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException,
//			URISyntaxException, IOException, ServiceUnavailableException {
//		if (sapBehvo != null && !"".equals(sapBehvo)) {
//			newCharacteristicRecords.put(createCharacteristicValueObject("SAP_BEHVO",
//					new org.json.JSONObject().put("_code", sapBehvo.substring(0, 2))));
//			System.out.println("Got " + sapBehvo + " for SAP_BEHVO.");
//			String thevalue = "1";
//			try {
//				org.json.JSONArray rws = new org.json.JSONObject(rw.getRc().getRequest("GET", rw.getBaseUrl()
//						+ "/list/StandardizationValue/bySearch" + "?dictionaryProxy="
//						+ java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8") + "&query="
//						+ java.net.URLEncoder.encode(
//								"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \""
//										+ sapBehvo.substring(0, 2) + "\"",
//								"UTF-8")
//						+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8"),
//						null)).getJSONArray("rows");
//				System.out.println("Checking sapBehvo: " + sapBehvo);
//				if (rws.length() > 0) {
//					thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
//				}
//			} catch (org.json.JSONException e) {
//				e.printStackTrace();
//			}
//			newCharacteristicRecords.put(
//					createCharacteristicValueObject("ProductType", new org.json.JSONObject().put("_code", thevalue)));
//			System.out.println("Placing value: " + thevalue + " for ProductType");
//		} else {
//			System.out.println("No SAP_BEHVO found, placing value 1.");
//			newCharacteristicRecords
//					.put(createCharacteristicValueObject("ProductType", new org.json.JSONObject().put("_code", "1")));
//		}
//	}

	private static final java.util.Set<String> IDENTITY_CHARACTERISTICS = new java.util.HashSet<>(
			java.util.Arrays.asList("SKU", "MainBarCode", "MainBarCodeS4H"));

	private static final java.util.Set<String> VARIANT_MATCH_CHARACTERISTICS = new java.util.HashSet<>(
			java.util.Arrays.asList("SKU", "MainBarCode", "MainBarCodeS4H", "ColoursLiverpoolAtt", "TamanoUnico",
					"SupplierPartNumber"));

	private JdbcConfig initJdbcConfig() throws IOException {
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
	
	private int forbiddenChalice(String itemGroup, String product) {
		if (itemGroup == null || itemGroup.isBlank()) {
			return -1;
		}
		if (product != null && !product.isBlank()) {
			Integer id =
					dastub.getLeafStructureGroupId(
							10002,
							itemGroup + "-L4SH",
							product + "-L5SH",
							itemGroup + product + "-L5SH");

			if (id != null) {
				return id;
			}
			return processMissingPair(
					itemGroup,
					itemGroup + product);
		}
		Integer id =
				dastub.getLeafStructureGroupId(
						10002,
						null,
						itemGroup + "-L4SH",
						null);

		return id == null ? -1 : id;
	}
	
	private int processMissingPair(String itemGroup, String product) {
		org.json.JSONObject req = new org.json.JSONObject();
		req.put("level", 5);
		req.put("nodeType", "leaf");
		req.put("parent", new org.json.JSONObject().put("_externalId", "'" + itemGroup + "'@10002"));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		org.json.JSONObject resp = rw.getRw().makeRequest("PUT", "/object/StructureGroup/'" + product + "-L5SH'@10002", qp, req.toString());
		if(resp != null && resp.has("_entityItem")) {
			return Integer.parseInt( resp.getJSONObject("_entityItem").getString("_internalId").split("@")[0] );
		}
		return -1;
	}

	private java.sql.Connection openConnection(JdbcConfig jdbcConfig, boolean autoCommit)
			throws java.sql.SQLException, ClassNotFoundException {
		Class.forName(jdbcConfig.jdbcDriver);

		java.sql.Connection connection = java.sql.DriverManager.getConnection(jdbcConfig.jdbcUrl, jdbcConfig.user,
				jdbcConfig.password);

		connection.setAutoCommit(autoCommit);

		return connection;
	}

	private final class JdbcConfig {
		private String jdbcDriver;
		private String jdbcUrl;
		private String user;
		private String password;
	}

	private static class ProductMergeDecision {
		String productIdToUse;
		String id1;
		String id2;
		boolean shouldMerge;
		boolean manualReview;
		String reason;
	}

	private static final Logger LOGGER = Logger.getLogger(ParseJana122Response.class.getName());

	static {
		try {
			LOGGER.setUseParentHandlers(false);

			FileHandler fileHandler = new FileHandler("../logs/sftp/s4h/parseJana122Response-%g.log", 25 * 1024 * 1024,
					10, true);
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

	@Override
	public final void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJana122Response.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	@Override
	public final void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.OutputStreamWriter(new java.io.FileOutputStream(
						java.nio.file.Paths.get("..", "logs", "parseJana122Response.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

	private static final java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();

	static {
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesPeso.put("unece.unit.GRM", "G");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
	}
}
