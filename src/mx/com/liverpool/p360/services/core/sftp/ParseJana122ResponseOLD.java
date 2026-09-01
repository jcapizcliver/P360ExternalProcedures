package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.SimpleLog;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.sftp.xml.Jana122Handler;
import mx.com.liverpool.p360.services.core.sftp.xml.Jana122Handler.Product;
import mx.com.liverpool.p360.services.core.sftp.xml.Jana122Handler.Value;

public class ParseJana122ResponseOLD implements SimpleLog, Closeable {
	

	private DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
		
		@Override
		public void logE(Exception e) {
			ParseJana122ResponseOLD.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			ParseJana122ResponseOLD.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);

	@Override
	public void close() {
		dastub.close();
	}

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();

    private static final String HOST = PropertiesManager.get( "p360.contingency.s4h.host" ); //SFTP server address: 172.18.184.26
    private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.s4h.port" ));//SFTP server port: 22
    private static final String USER = PropertiesManager.get( "p360.contingency.s4h.userp360" );// SFTP username: userp360
    private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.s4h.private_key_path" ));//Path to private key: /home/P360admin/.ssh/id_rsa
    private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.s4h.remote_directory_122" );//Remote directory to monitor: /interfase/mer/out/step/P360/zrtuab122
    private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.s4h.local_processed_dir_122" ));//Path: /u01/stage/SBB_122/processed
    private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.s4h.state_file_122" ));//File: processed_s4h.122.properties
    private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.s4h.use_cache" ));//false;

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
    private final org.json.JSONObject reqSPN = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierPartNumber(MX)"))).put("rows", new org.json.JSONArray());
    
    private final org.json.JSONObject reqSKUA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SKU"))).put("rows", new org.json.JSONArray());
    private final org.json.JSONObject reqEANA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.EAN"))).put("rows", new org.json.JSONArray());
    private final org.json.JSONObject reqBNA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.Business"))).put("rows", new org.json.JSONArray());
    private final org.json.JSONObject reqDSA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleLang.DescriptionShort(es)"))).put("rows", new org.json.JSONArray());
    private final org.json.JSONObject reqSPNA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SupplierPartNumber(MX)"))).put("rows", new org.json.JSONArray());
    private final org.json.JSONObject reqSTA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)"))).put("rows", new org.json.JSONArray());
    
    private final java.util.Map<String, String> qp = new java.util.HashMap<>();
    
    private static final java.util.Map<String, String> qp0 = new java.util.HashMap<>();
    
    private void addSKU(String sku, String id, String entity) {
    	if("Product2G".equals(entity)) {
    		org.json.JSONArray rows = reqSKU.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(sku)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
    		}
    	}else if("Article".equals(entity)) {
    		org.json.JSONArray rows = reqSKUA.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(sku)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Article", null, qp0, reqSKUA, this::log);
    		}
    	}
    }
    
    private void addEAN(String ean, String id, String entity) {
    	if("Product2G".equals(entity)) {
    		org.json.JSONArray rows = reqEAN.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(ean)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Product2G", null, qp0, reqEAN, this::log);
    		}
    	}else if("Article".equals(entity)) {
    		org.json.JSONArray rows = reqEANA.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(ean)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Article", null, qp0, reqEANA, this::log);
    		}
    	}
    }
    
    private void addSupplierPartNumber(String supplierPartNumber, String id, String entity) {
    	if("Product2G".equals(entity)) {
    		org.json.JSONArray rows = reqSPN.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(supplierPartNumber)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Product2G", null, qp0, reqSPN, this::log);
    		}
    	}else if("Article".equals(entity)) {
    		org.json.JSONArray rows = reqSPNA.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(supplierPartNumber)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Article", null, qp0, reqSPNA, this::log);
    		}
    	}
    }
    
    private void addSAPObjectType(String sapObjectType, String id, String entity) {
    	if("Product2G".equals(entity)) {
    		org.json.JSONArray rows = reqST.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(sapObjectType)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Product2G", null, qp0, reqST, this::log);
    		}
    	}else if("Article".equals(entity)) {
    		org.json.JSONArray rows = reqSTA.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(sapObjectType)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Article", null, qp0, reqSTA, this::log);
    		}
    	}
    }
    
    private void addBusiness(String business, String id, String entity) {
    	if("Product2G".equals(entity)) {
    		org.json.JSONArray rows = reqBN.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(business)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Product2G", null, qp0, reqBN, this::log);
    		}
    	}else if("Article".equals(entity)) {
    		org.json.JSONArray rows = reqBNA.getJSONArray("rows");
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(business)));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Article", null, qp0, reqBNA, this::log);
    		}
    	}
    }
    
    private void addShortDescription(String id, String shortDescription) {
		org.json.JSONArray rows = reqDS.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(shortDescription)));
		if(rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqDS, this::log);
		}
    }
    
    private void addSupplier(String id, String supplier) {
    	org.json.JSONArray rows = reqSupplier.getJSONArray("rows");
    	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(supplier)));
    	if(rows.length() == 1000) {
    		rw.writeData("list", "Product2G", null, qp0, reqSupplier, this::log);
    	}
    }
    
    private void addShortDescriptionA(String id, String shortDescription) {
    	org.json.JSONArray rows = reqDSA.getJSONArray("rows");
    	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(shortDescription)));
    	if(rows.length() == 1000) {
    		rw.writeData("list", "Article", null, qp0, reqDSA, this::log);
    	}
    }
    
    private void addDirection(String id, String direction) {
		org.json.JSONArray rows = reqDir.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(direction)));
		if(rows.length() == 1000) {
			rw.writeData("list", "Product2G", null, qp0, reqDir, this::log);
		}
    }
    
    private void addSection(String id, String section) {
    	org.json.JSONArray rows = reqSec.getJSONArray("rows");
    	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(section)));
    	if(rows.length() == 1000) {
    		rw.writeData("list", "Product2G", null, qp0, reqSec, this::log);
    	}
    }
    
    private void addItemGroup(String id, String itemGroup) {
    	org.json.JSONArray rows = reqIG.getJSONArray("rows");
    	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(itemGroup)));
    	if(rows.length() == 1000) {
    		rw.writeData("list", "Product2G", null, qp0, reqIG, this::log);
    	}
    }
    
    private void addBrandName(String id, String brandName) {
    	org.json.JSONArray rows = reqBN.getJSONArray("rows");
    	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(brandName)));
    	if(rows.length() == 1000) {
    		rw.writeData("list", "Product2G", null, qp0, reqBN, this::log);
    	}
    }
    
    
    private void sendData() {
    	org.json.JSONArray rows = reqEAN.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqEAN, this::log);
    	}
    	rows = reqEANA.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Article", null, qp0, reqEANA, this::log);
    	}
    	rows = reqSKU.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
    	}
    	rows = reqSKUA.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Article", null, qp0, reqSKUA, this::log);
    	}
    	rows = reqBusiness.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqBusiness, this::log);
    	}
    	rows = reqDir.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqDir, this::log);
    	}
    	rows = reqSec.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqSec, this::log);
    	}
    	rows = reqIG.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqIG, this::log);
    	}
    	rows = reqBN.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqBN, this::log);
    	}
    	rows = reqST.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqST, this::log);
    	}
    	rows = reqSupplier.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqSupplier, this::log);
    	}
    	rows = reqSPN.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqSPN, this::log);
    	}
    	rows = reqBNA.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Article", null, qp0, reqBNA, this::log);
    	}
    	rows = reqDSA.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Article", null, qp0, reqDSA, this::log);
    	}
    	rows = reqSPNA.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Article", null, qp0, reqSPNA, this::log);
    	}
    	rows = reqSTA.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Article", null, qp0, reqSTA, this::log);
    	}
    	
        
    }
    
	private static final java.util.Map<String, String> s4hFieldMapping = new java.util.TreeMap<>();
	private final ParsersTools tools = new ParsersTools(this, dr);

    private boolean running = true;

	static {
		qp0.put("includeObjectsInProtocol", "false");
		if(java.nio.file.Files.notExists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")))) {
			try{
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory")));
			}catch(java.io.IOException e) {
				
			}
		}
		if(java.nio.file.Files.notExists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"))) {
			try{
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache"));
			}catch(java.io.IOException e) {
				
			}
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Characteristic.Identifier"
				+ ",Characteristic.DataType"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(ECC)"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(ATG)"
				+ ",Characteristic.Lookup->Lookup.Identifier"
			);
		qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "1500");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toString())))){
			rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> pw.println( workshop.serializeChunk(new Object[] { 
					 row.getJSONArray("values").getString(0)
					,row.getJSONArray("values").getString(1)
					,row.getJSONArray("values").getString(2)
					,row.getJSONArray("values").getString(3)
					,row.getJSONArray("values").getString(4)
					,row.getJSONArray("values").getString(5)
				}, "\"", ";", "\\") ), System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pcs = null;
			while((line = br.readLine()) != null) {
				pcs = workshop.parseLine(line, "\"", ";", "\\");
				if(pcs.length == 6 && !"".equals(pcs[3]))
					s4hFieldMapping.put(pcs[0], pcs[3]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		/*
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics"))){
			lns.map(s -> workshop.parseLine(s, "\"", ";", "\\")).collect(java.util.stream.Collectors.toMap(arr -> arr[0], arr -> java.util.Arrays.copyOfRange(arr, 1, arr.length))).entrySet().forEach(entry -> {
				s4hFieldMapping.put(entry.getKey(), entry.getValue()[3]);
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		*/
		qp.clear();
		qp.put("fields", "Characteristic.Identifier,Characteristic.Entities");
		qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "10000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_entities").toFile())))){
			rw.collectData("list", "Characteristic", null, "bySearch", qp, row->{
				org.json.JSONArray entities = row.getJSONArray("values").getJSONArray(1);
				java.util.LinkedList<String> entitiesList = new java.util.LinkedList<>();
				for(int i=0; i<entities.length(); i++) {
					entitiesList.addLast(entities.getString(i));
				}
				pw.println( rw.getRw().serializeChunk( new String[] { row.getJSONArray("values").getString(0), rw.getRw().serializeChunk( entitiesList.toArray(new String[] {}) ) }, "\"", ";", "\\" ) );
			}, System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}
	
    private void launchListenerThread() {
		Thread t = new Thread(()->{
			while(running) {
				try(
					java.net.ServerSocket server = new java.net.ServerSocket(23545);
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
    
    public static void main(String[] args) throws ServiceUnavailableException {
    	try(ParseJana122ResponseOLD object = new ParseJana122ResponseOLD()){
	    	object.launchListenerThread();
	    	while(object.running) {
	    		object.runOnSftp(args);
	    		try {
	    			Thread.sleep(60000);
	    		}catch(InterruptedException e) {
	    			object.logE(e);
	    		}
	    	}
	    	object.log("Terminé.");
    	}
    }
    
	public void runOnSftp(String[] args) throws ServiceUnavailableException {
		qp.put("includeObjectsInProtocol", "false");
		if(args.length > 0) {
    		USE_CACHE = Boolean.parseBoolean(args[0]);
    	}else {
    		USE_CACHE = true;
    	}

		try (SshClient client = SshClient.setUpDefaultClient()){
			client.start();
	        java.nio.file.Files.createDirectories(LOCAL_PROCESSED_DIR);
	
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
	                while (running) {
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
	
	                            Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
	                            java.nio.file.Files.write(localCopy, out.toByteArray());
	
	                            processedState.setProperty(name, String.valueOf(remoteModified));
	                            if (USE_CACHE) {
	                                try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(STATE_FILE)) {
	                                    processedState.store(stateOut, null);
	                                }
	                            }
	
	                            if(!name.startsWith("GenericXMLproducts")) {
	                        		log("Skipping " + name);
	                            	continue;
	                            }
	                            try {
	                            	processFile(null, out, sftp);
	                            	sftp.remove(filePath);
	                    		} catch (ParserConfigurationException | SAXException | IOException e) {
	                    			e.printStackTrace();
	                    		}
	                            if(!running)
	                            	break;
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
	                    Thread.sleep(1_000);
	                    sendData();
	                }
	            }
	        } finally {
	        	client.close();
	        }
		}catch(IOException | InterruptedException e) {
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

	public void processFile(java.nio.file.Path path, java.io.ByteArrayOutputStream baos, SftpClient sftp) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {
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
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        Jana122Handler handler = new Jana122Handler();
        try {
	        if(path != null) {
	        	parser.parse(path.toFile(), handler);
	        }else {
	        	parser.parse(new java.io.ByteArrayInputStream( baos.toByteArray() ), handler);
	        }
        }catch(NullPointerException e) {
        	return ;
        }
        
        java.util.LinkedList<Product> products = handler.getProducts();
        java.util.LinkedList<Value> values = null;
		
		org.json.JSONArray product2GCharacteristicRecords = new org.json.JSONArray();
		org.json.JSONArray articleCharacteristicRecords = new org.json.JSONArray();
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> attributeValues = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> newAttributeValues = new java.util.TreeMap<>();
		java.util.Map<String, String> dataTypes = new java.util.TreeMap<>();
		java.util.LinkedList<String> product2GCharacteristics = new java.util.LinkedList<>();
		java.util.LinkedList<String> articleCharacteristics = new java.util.LinkedList<>();
		java.util.Map<String, String> unidades = new java.util.TreeMap<>();
		java.util.Map<String, String> articleHigherLevelProductNotReadyYet = new java.util.TreeMap<>();
		java.util.Map<String, String> articleHigherLevelProduct = new java.util.TreeMap<>();
		java.util.Map<String, String> articleBusiness = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> map = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> mapB = new java.util.TreeMap<>();
		java.util.Map<String, String> lkps = new java.util.TreeMap<>();
		String znprst = null;
		String negocio = null;
		String sb0002 = null;
		String sku = null;
		String satnr = null;
		String attyp = null;
		String sapBehvo = null;
		String fshId = null;
		String productId = null;
		String mstae = null;
		String sistemaorigen = null;
		String itemId = null;
		String[] info = null;
		org.json.JSONObject data = null;
		java.util.LinkedList<String> cosasNuevas = new java.util.LinkedList<>();
		java.util.Map<String, String> skuIds = new java.util.TreeMap<>();
		java.util.Map<String, String> articleSupplierAIDToSKU = new java.util.TreeMap<>();
		java.util.Map<String, String> skuToArticleSupplierAID = new java.util.TreeMap<>();
		collectCharacteristicsByEntity(product2GCharacteristics, articleCharacteristics);
		if(products != null) {
			for(Product n : products) {
				values = n.getValues();
				for(Value v : values) {
					if(!"".equals(v.getText())){
						sb.append(sb.length() > 0 ? "," : "")
							.append(v.getAttributeId())
						;
						if(!excluyeAtributo(v)) {
							attributeValues.put(v.getAttributeId(), "MATNR".equals(v.getAttributeId()) || "LIFNR".equals(v.getAttributeId()) || "WESCH".equals(v.getAttributeId()) ? (v.getText() != null ? v.getText().replaceAll("^0+", "").trim() : "") : v.getText() == null ? "" : v.getText());
							if(v.getText() != null) {
								if(v.getText().matches("^(unece\\.unit\\.)[A-Z0-9]+$")) {
									unidades.put(v.getAttributeId(), v.getText());
								}else {
								}
							}else {
								log("Element with null text: " + v.getAttributeId());
							}
						}
					}
				}
				collectLookupCharacteristics(sb.toString(), dataTypes, lkps);
				sb.setLength(0);
				for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
					collectLookupValues(lkps.get( entry.getKey() ), map, mapB, entry.getValue());
				}
				for(java.util.Map.Entry<String, String> entry : dataTypes.entrySet()) {
					try{
						if(!unidades.containsKey(entry.getKey()) && product2GCharacteristics.contains(entry.getKey()) ) {
							addValue(entry.getKey(), tools.resolveDataType(entry.getKey(), entry.getValue(), lkps.get(entry.getKey()), attributeValues.get(s4hFieldMapping.get(entry.getKey())), map, mapB), product2GCharacteristicRecords );
						}
						if(!unidades.containsKey(entry.getKey()) && articleCharacteristics.contains(entry.getKey()) ) {
							addValue(entry.getKey(), tools.resolveDataType(entry.getKey(), entry.getValue(), lkps.get(entry.getKey()), attributeValues.get(s4hFieldMapping.get(entry.getKey())), map, mapB), articleCharacteristicRecords );
						}
					}catch(IllegalArgumentException e) {
						logE(e);
					}
				}
				agregaUnidadesDeMedida(unidades, product2GCharacteristicRecords, lkps);
				if(!lkps.containsKey("EXTWG_S4H")) {
					log("Un caso donde no hay EXTWG_S4H. " + attributeValues.get("EXTWG"));
				}
				negocio = !lkps.containsKey("EXTWG_S4H") ? attributeValues.get("EXTWG") : attributeValues.get( lkps.get("EXTWG_S4H") );
				sku = attributeValues.get( "MATNR" ).replaceAll("^0+", "");
				sb0002 = attributeValues.get("SB_0002");
				productId = null;
				itemId = null;
				satnr = attributeValues.containsKey("SATNR") ? attributeValues.get( "SATNR" ) != null ? attributeValues.get( "SATNR" ).replaceAll("^0+", "") : "" : "";
				attyp = attributeValues.get( "ATTYP" ); // SAPObjectType
				sapBehvo = attributeValues.get( "SAP_BEHVO" );
				fshId = attributeValues.get( "FSH_ID" );
				mstae = attributeValues.get( "MSTAE" );
				String lifnr = attributeValues.get( "LIFNR" ); // Supplier
				String brandId = attributeValues.get( "BRAND_ID" ); // BrandID
				String idnlf = attributeValues.get( "IDNLF" ); // Modelo
				String matkl = attributeValues.get( "MATKL" ); // ItemGroup
				String zsec = attributeValues.get( "ZSEC" ); // Sección
				String zdir = attributeValues.get( "ZDIR" ); // Dirección
				String maktx = attributeValues.get( "MAKTX" ); // description short 
				String ean = attributeValues.get( "EAN11" ); // EAN
				
				lifnr = lifnr == null ? "" : lifnr;
				brandId = brandId == null ? "" : brandId;
				idnlf = idnlf == null ? "" : idnlf;
				matkl = matkl == null ? "" : matkl;
				zsec = zsec == null ? "" : zsec;
				zdir = zdir == null ? "" : zdir;
				maktx = maktx == null ? "" : maktx;
				
				sistemaorigen = attributeValues.get( "SISTEMAORIGEN" );
				try {
					calculaProductType(sapBehvo, fshId, articleCharacteristicRecords, workshop);
				} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
					e.printStackTrace();
				}
				znprst = attributeValues.get("PRODUCT_ID");
				if(znprst != null && !"".equals(znprst)) {
					if(znprst.length() == 15 && !znprst.startsWith("S")) {
						znprst = "1" + znprst;
					}
					if(znprst != null && sku != null && !"".equals(sku)) {
						articleSupplierAIDToSKU.put(sku, znprst);
						skuToArticleSupplierAID.put(znprst, sku);
					}
					skuIds.put(sku, znprst);
					info = tools.checkProduct(znprst);
					if( info == null ) {
						info = tools.checkArticle(znprst);
						if( info != null ) {
//							if(znprst.length() == 16)
//								enviaForo40(sftp, sku, info[1], info[2], mstae, sistemaorigen);
							if("00".equals(info[1])) {
								addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), product2GCharacteristicRecords );
								sendWriteRequest("Product2G", info[0], product2GCharacteristicRecords, info[3], info[4], sku);
							}
							addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
							sendWriteRequest("Article", znprst, articleCharacteristicRecords, null, null, sku);
							log("Was Article (" + znprst + "), " + java.util.Arrays.asList(info));
						}else {
							log("Not a known product (" + znprst + ")");
							if(sku != null && !"".equals(sku)) {
								info = tools.checkProductBySKU(sku);
								if(info != null) {
									log("Found SKU in product ¿...?");
									productId = info[0];
									addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), product2GCharacteristicRecords );
									sendWriteRequest("Product2G", productId, product2GCharacteristicRecords, info[3], info[4], sku);
									if("00".equals(info[1])) {
										addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
										sendWriteRequest("Article", productId, articleCharacteristicRecords, null, null, sku);
									}
								} else {
									itemId = tools.checkArticleBySKU(sku);
									if(itemId != null) {
										log("Found SKU in article. #" + itemId + "#");
										if(!"MARKETPLACE".equals(negocio)) {
											addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
											sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null, sku);
										}
									} else {
										log("Brand new SKU for P360: " + sku + " (" + znprst + ") Negocio: " + negocio);
										if("00".equals(attyp)) {
											addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), product2GCharacteristicRecords);
											addValue("Business", new org.json.JSONObject().put("_code", "SBB" ), product2GCharacteristicRecords);
											addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), articleCharacteristicRecords);
											sendWriteRequest("Article",   "SBB" + sku, articleCharacteristicRecords, null, null, sku);
											sendWriteRequestProduct(znprst, sb0002, negocio, product2GCharacteristicRecords);
											cosasNuevas.addLast(znprst);
											articleBusiness.put(znprst, negocio);
											articleHigherLevelProduct.put(znprst, znprst);
										}else if("01".equals(attyp)) {
											addValue("SAPObjectType", new org.json.JSONObject().put("_code", "01"), product2GCharacteristicRecords);
											addValue("Business", new org.json.JSONObject().put("_code", "SBB" ), product2GCharacteristicRecords);
											sendWriteRequestProduct(znprst, sb0002, negocio, product2GCharacteristicRecords);
										}else {
											addValue("SAPObjectType", new org.json.JSONObject().put("_code", "02" ), articleCharacteristicRecords);
											sendWriteRequest("Article", znprst, articleCharacteristicRecords, null, null, sku);
											cosasNuevas.addLast(znprst);
											articleHigherLevelProduct.put(znprst, skuIds.get(satnr));
											articleHigherLevelProductNotReadyYet.put(znprst, satnr);
											articleBusiness.put(znprst, negocio);
										}
									}
								}
							}else {
								log("No SKU found either!");
							}
						}
					}else {
						log("Was Product (" + znprst + "), " + java.util.Arrays.asList(info));
						addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), product2GCharacteristicRecords );
						if("00".equals(info[0])) {
							itemId = getArticleIdFromProduct(znprst);
							log("Article.SupplierAID: " + itemId);
							addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
							sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null, sku);
						}
						sendWriteRequest("Product2G", znprst, product2GCharacteristicRecords, info[2], info[3], sku);
					}
				}else {
					log("No znprst found");
					if(sku != null && !"".equals(sku)) {
						info = tools.checkProductBySKU(sku);
						if(info != null) {
							log("Found SKU in product");
							productId = info[0];
							znprst = productId;
							addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), product2GCharacteristicRecords );
							sendWriteRequest("Product2G", productId, product2GCharacteristicRecords, info[3], info[4], sku);
							if("00".equals(info[1])) {
								addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
								sendWriteRequest("Article", productId, articleCharacteristicRecords, null, null, sku);
								itemId = tools.checkArticleBySKU(sku);
								if(itemId != null && !"".equals(itemId))
									articleHigherLevelProduct.put(itemId, productId);
							}
						} else {
							itemId = tools.checkArticleBySKU(sku);
							znprst = itemId;
							if(itemId != null) {
								log("Found SKU in attribute. #" + itemId + "#");
								if(!"MARKETPLACE".equals(negocio)) {
									info = tools.checkProductBySKU(satnr.replaceFirst("^0+", ""));
									articleHigherLevelProduct.put("SBB" + sku, info != null && info.length > 0 ? info[0]: "SBB" + satnr.replaceFirst("^0+", ""));
									addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), articleCharacteristicRecords );
									sendWriteRequest("Article", itemId, articleCharacteristicRecords, null, null, sku);
								}else {
									// ¿Qué hace un Marketplace dentro de SBB?
								}
							} else {
								log("Brand new SKU for P360: " + sku + " Negocio: " + negocio);
								znprst = "SBB" + sku;
								if("00".equals(attyp)) {
									info = tools.checkProductBySKU(satnr.replaceFirst("^0+", ""));
									addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), product2GCharacteristicRecords);
									addValue("Business", new org.json.JSONObject().put("_code", "SBB" ), product2GCharacteristicRecords);
									addValue("SAPObjectType", new org.json.JSONObject().put("_code", "00"), articleCharacteristicRecords);
									sendWriteRequest("Article",   "SBB" + sku, articleCharacteristicRecords, null, null, sku);
									sendWriteRequestProduct("SBB" + sku, sb0002, negocio, product2GCharacteristicRecords);
									cosasNuevas.addLast("SBB" + sku);
									articleBusiness.put("SBB" + sku, negocio);
									articleHigherLevelProduct.put("SBB" + sku, info != null && info.length > 0 ? info[0]: "SBB" + sku);
								}else if("01".equals(attyp)) {
									addValue("SAPObjectType", new org.json.JSONObject().put("_code", "01"), product2GCharacteristicRecords);
									addValue("Business", new org.json.JSONObject().put("_code", "SBB" ), product2GCharacteristicRecords);
									sendWriteRequestProduct("SBB" + sku, sb0002, negocio, product2GCharacteristicRecords);
								}else {
									info = tools.checkProductBySKU(satnr.replaceFirst("^0+", ""));
									addValue("SAPObjectType", new org.json.JSONObject().put("_code", "02" ), articleCharacteristicRecords);
									sendWriteRequest("Article", "SBB" + sku, articleCharacteristicRecords, null, null, sku);
									cosasNuevas.addLast("SBB" + sku);
									articleHigherLevelProduct.put("SBB" + sku, info != null && info.length > 0 ? info[0]: "SBB" + satnr);
									articleBusiness.put("SBB" + sku, negocio);
								}
							}
						}
					}else {
						log("No SKU found either!");
					}
				}
				if("01".equals(attyp)) {
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
				}else if("02".equals(attyp)) {
					addSKU(sku, znprst, "Article");
					addEAN(ean, znprst, "Article");
					addSAPObjectType(attyp, znprst, "Article");
					addBusiness("SBB", znprst, "Article");
					addSupplierPartNumber(idnlf, znprst, "Article");
					addShortDescriptionA(znprst, maktx);
				}else if("00".equals(attyp)) {
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
				product2GCharacteristicRecords = new org.json.JSONArray();
				articleCharacteristicRecords = new org.json.JSONArray();
				articleBusiness.clear();
				unidades.clear();
				attributeValues.clear();
				
				if(!running)
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
			for( java.util.Map.Entry<String, String> entry : articleHigherLevelProduct.entrySet() ) {
				item = new org.json.JSONObject();
				item.put("supplierAID", entry.getKey());
				item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
				item.put("productNo", entry.getValue());
				items.put(item);
				rows00.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue())).put("values", new org.json.JSONArray().put(entry.getValue())));
				if(rows00.length() == 5000) {
					rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
				}
			}
			if(rows00.length() > 0) {
				rw.writeData("list", "Article", "ProductReference", qp, req, this::log);
			}
//			for( java.util.Map.Entry<String, String> entry : articleHigherLevelProduct.entrySet() ) {
//				data = new org.json.JSONObject();
//				data.put("higherLevelProduct", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("referencedIdentifier", entry.getValue()))));
//				updateArticleHigherLevelProduct(entry.getKey(), data);
//			}
			log( "HLPs: " + articleHigherLevelProduct);
			String parentId = null;
			for( java.util.Map.Entry<String, String> entry : articleHigherLevelProductNotReadyYet.entrySet() ) {
				parentId = skuToArticleSupplierAID.get(entry.getValue());
				if(parentId == null) {
					log("En el archivo no estaba el padre referenciado por el valor de SKU: " + entry.getValue() + " para la variante con id de sistema: " + entry.getKey());
					String response = dr.productBySKU( new org.json.JSONArray().put(entry.getValue()) );
					try {
						org.json.JSONObject jr = new org.json.JSONObject(response);
						org.json.JSONArray ir = jr.getJSONArray("items");
						parentId = ir.getString(0);
						log("Recuperamos el padre gracias al admin: " + parentId + " para SKU: " + entry.getValue() + ", de la propuesta variante: " + entry.getKey());
					}catch(org.json.JSONException e) {
						logE(e);
					}
				}
				if(parentId != null) {
					item = new org.json.JSONObject();
					item.put("supplierAID", entry.getKey());
					item.put("sku", articleSupplierAIDToSKU.get(entry.getKey()));
					item.put("productNo", parentId);
					items.put(item);
				}
			}
			dr.putSkuSupplierAID(items);
			arremangalos(newAttributeValues);
		}else {
			log("Malformed file content...");
		}
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
		if(mPack.contains(v.getAttributeId())) {
			try{
				float val = Float.parseFloat(v.getText());
				return val == 0f;
			}catch(NumberFormatException | NullPointerException e) {
				
			}
		}
		return false;
	}
	
	public void flushPendingWrites() {
	    sendData();
	}

	private String determineBusiness(String negocio) {
		return "".equals(negocio) ? null : "MARKETPLACE".equals(negocio) ? "Marketplace" : "Liverpool" ;
	}
	
	private java.util.Map<String, String> readFieldSections() {
		try {
			java.util.Map<String, String> data = new java.util.TreeMap<>();
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_vendor_center_sections").toFile() ), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] pieces = null;
				while((line = br.readLine()) != null) {
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
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping" ).toFile() ), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] pieces = null;
				while((line = br.readLine()) != null) {
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
		rw.addHeader("Authorization", ParseJana122ResponseOLD.workshop.getRc().getHeader().get("Authorization"));
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
		for(java.util.Map.Entry<String, java.util.Map<String, String>> entry : newAttributeValues.entrySet()) {
			msgBody = new org.json.JSONObject();
			msgBody.put("proposalId", entry.getKey());
			for(java.util.Map.Entry<String, String> entry0 : entry.getValue().entrySet() ) {
				charId = eccFieldMapping.get(entry0.getKey());
				if(charId != null) {
					vendorCenterSection = fieldVendorCenterSections.get(charId);
					if(vendorCenterSection != null) {
						vendorCenterSectionKey = equivalenciaVCS.get(vendorCenterSection);
						if(vendorCenterSectionKey != null) {
							section = msgBody.has(vendorCenterSectionKey) ? msgBody.getJSONObject(vendorCenterSectionKey) : null;
							if(section == null) {
								section = new org.json.JSONObject();
								msgBody.put(vendorCenterSectionKey, section);
							}
							section.put(charId, entry0.getValue());
						}
					}
				}
			}
			extwg = entry.getValue().get("EXTWG");
			status = "SFERA".equals(extwg) ? "Gobierno de Datos" : "DUTY FREE".equals(extwg) ? "Propuesta Generada" : "MARCAS PROPIAS".equals(extwg) ? "Pendiente Inicio Enriquecimiento" : "REGULAR".equals(extwg) ? "Pendiente Inicio Enriquecimiento" : "SERVICIOS".equals(extwg) ? "Pendiente Inicio Enriquecimiento" : "Propuesta Generada" ;
			statusCode = String.valueOf( "SFERA".equals(negocio) ? 1021 : "DUTY FREE".equals(negocio) ? 1001 : "MARCAS PROPIAS".equals(negocio) ? 1002 : "REGULAR".equals(negocio) ? 1002 : "SERVICIOS".equals(negocio) ? 1002 : 1001);
			negocio = determineBusiness(extwg);
			if(negocio != null && !"".equals(negocio)) {
				msgBody.put("Business", negocio);
			}
			if(status != null && !"".equals(status) && statusCode != null && !"".equals(statusCode)) {
				msgBody.put("currentStatus", status);
				status = equivExt.get(statusCode);
				status = extStatus.get(status);
				msgBody.put("externalStatus", status);
				msgBody.put("previousStatus", "1020");
			}
			supplier = entry.getValue().get("LIFNR");
			if(supplier != null) {
				msgBody.put("supplier", supplier);
				if(proveedoresMigrados.contains(supplier)) {
					msgBody.put("owner", "P360");
				}else {
					msgBody.put("owner", "STEP");
				}
			}else {
				if(proveedoresMigrados.isEmpty()) {
					msgBody.put("owner", "STEP");
				}else {
					msgBody.put("owner", "");
				}
			}
			productos.put(msgBody);
		}
		new PubSubGCP().publishMessage(
				  PropertiesManager.get("p360.contingency.gcp.project_back")
				, PropertiesManager.get("p360.contingency.gcp.post_products_topic")
				, PropertiesManager.get("p360.contingency.gcp.service_account_back")
				, new org.json.JSONObject().put("products", productos).toString());
		log( "Sent." );
	}
	
	private void updateArticleHigherLevelProduct(String articleId, org.json.JSONObject data) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = data;
		org.json.JSONObject response = null;
		log("/object/Article/'" + articleId + "'@'MASTER'");
		response = workshop.makeRequest("PUT", "/object/Article/'" + articleId + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			log("On writing Article Id (relationship): " + articleId + ": " + response);
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
	}
	
	private void collectNumberOfImages(String productId, String fotosTomaLiverpool, org.json.JSONObject data) {
		int lacuenta = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"Article.SupplierAID"
				+ ",ProductReference.ReferencedSupplierAid(\"" + productId + "\")"
				+ ",ArticleCharacteristicValueLang.Value(ProductImageDetail,\"0000.0000.RK\",\"0000.0000.RK\",ProductImageDetail_URL,-1)"
				+ ",ArticleCharacteristicValueLang.Value(ProductImage,\"0000.0000.RK\",\"0000.0000.RK\",ProductImage_URL,-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null) {
			org.json.JSONArray rows = response.getJSONArray("rows");
			org.json.JSONArray values = null;
			org.json.JSONArray details = null;
			org.json.JSONArray principal = null;
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				details = values.getJSONArray(2);
				principal = values.getJSONArray(3);
				for(int j=0; j<details.length(); j++) {
					if(!"".equals(details.getString(j)))
						lacuenta++;
				}
				for(int j=0; j<principal.length(); j++){
					if(!"".equals(principal.getString(j)))
						lacuenta++;
				}
			}
			if("Corregido".equals(fotosTomaLiverpool) || ("N".equals(fotosTomaLiverpool) && lacuenta > 0 )) {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1022));
				data.put("externalStatus", new org.json.JSONObject().put("_code", "EnProcesoLiverpool"));
			}else if("Y".equals(fotosTomaLiverpool)) {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1002));
				data.put("externalStatus", new org.json.JSONObject().put("_code", "EnProcesoLiverpool"));
			}else {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1004));
				data.put("externalStatus", new org.json.JSONObject().put("_code", "CargaDeImagen"));
			}
			data.put("previousStatus", new org.json.JSONObject().put("_key", 1020));
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
	}
	
	private void agregaUnidadesDeMedida(java.util.Map<String, String> unidades, org.json.JSONArray characteristicRecords, java.util.Map<String, String> s4hFieldMapping) {
		java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
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
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaLongitud", new org.json.JSONObject().put("_code", unidadDeMedidaLongitud) ) );
		}
		if(unidadDeMedidaPeso == null) {
		}else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaPeso", new org.json.JSONObject().put("_code", unidadDeMedidaPeso) ) );
		}
		if(unidadDeMedidaVolumen == null) {
		}else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaVolumen", new org.json.JSONObject().put("_code", unidadDeMedidaVolumen) ) );
		}
	}

	private static org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}
	
	private String getArticleIdFromProduct(String productId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null) {
			if(!response.has("rows")) {
				log("ERROR: " + response);
				return null;
			}
			rows = response.getJSONArray("rows");
			if(rows.length() == 1) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			}
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
	}
	
	private java.util.Map<String, String> cargaEstatusExterno(){
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", "ExternalStatus").toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Map<String, String> map = new java.util.HashMap<>();
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				map.put(pieces[0], pieces.length == 1 ? "" : pieces[1]);
			}
			return map;
		}catch(java.io.IOException e) {
			logE(e);
		}
		return null;
	}
	
	private java.util.Map<String, String> cargaEquivalenciaEstatusExterno(){
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", "ExternalStatus").toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Map<String, String> map = new java.util.HashMap<>();
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				map.put(pieces[0], pieces.length == 1 ? "" : pieces[1]);
			}
			return map;
		}catch(java.io.IOException e) {
			logE(e);
		}
		return null;
	}
	
	private java.util.Set<String> cargaProveedoresMigrados(){
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "proveedores_migrados").toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Set<String> st = new java.util.TreeSet<>();
			String line = null;
			while((line = br.readLine()) != null) {
				st.add(line);
			}
			return st;
		}catch(java.io.IOException e) {
			logE(e);
		}
		return null;
	}
	
	private java.util.Map<String, String> cargaMapaDeEquivalenciaDeSeccionDeVendorCenter(){
		java.util.Map<String, String> translation = new java.util.TreeMap<>();
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", "SeccionesEntradaUnicaCatalogacion").toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
			java.util.Map<String, String> map = new java.util.HashMap<>();
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				map.put(pieces[0], pieces.length == 1 ? "" : pieces[1]);
			}
			return map;
		}catch(java.io.IOException e) {
			logE(e);
		}
		return translation;
	}
	
	private void agregaClasificacion(String itemGroup, org.json.JSONObject data) {
		org.json.JSONArray structureGroupMap = null; //		
		if(data.has("structureGroupMap")) {
			structureGroupMap = data.getJSONArray("structureGroupMap") ;
		}else {
			structureGroupMap = new org.json.JSONArray();
			data.put("structureGroupMap", structureGroupMap);
		}
		for(int i=0; i<structureGroupMap.length(); i++) {
			if(structureGroupMap.getJSONObject(i).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").endsWith("'@'CommercialECC'")) {
				return;
			}
		}
		structureGroupMap.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + itemGroup + "-L5SH'@'CommercialECC'"))));
	}
	
	private void sendWriteRequestProduct(String id, String itemGroup, String negocio, org.json.JSONArray characteristicRecords) {
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("_characteristicRecords", characteristicRecords);
		if( "MARCAS PROPIAS".equals(negocio) || "REGULAR".equals(negocio) || "SERVICIOS".equals(negocio) ) {
			addValue("EnriquecidoEnForo", true, characteristicRecords );
		}
		agregaClasificacion(itemGroup, request);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		request.put("_characteristicRecords", characteristicRecords);
		request.put("currentStatus", new org.json.JSONObject().put("_key", "SFERA".equals(negocio) ? 1021 : "DUTY FREE".equals(negocio) ? 1001 : "MARCAS PROPIAS".equals(negocio) ? 1002 : "REGULAR".equals(negocio) ? 1002 : 1001 ));
		org.json.JSONObject response = null;
		org.json.JSONArray rows = reqSKU.getJSONArray("rows");
    	if(rows.length() > 0) {
    		rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
    	}
		response = workshop.makeRequest("PUT", "/object/Product2G/'" + id + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			log("\tWriting: " + request + "\nNot really an error from writing id: " + id + ": " + response);
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
	}
	
	private void sendWriteRequest(String entity, String id, org.json.JSONArray characteristicRecords, String fotoTomadaLiverpool, String currentStatus, String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("_characteristicRecords", characteristicRecords);
		if("Product2G".equals(entity) && currentStatus != null && "1020".equals(currentStatus)) {
			collectNumberOfImages(id, fotoTomadaLiverpool, request);
		}
		org.json.JSONObject response = null;
		if("Product2G".equals(entity)) {
			org.json.JSONArray rows = reqSKU.getJSONArray("rows");
	    	if(rows.length() > 0) {
	    		rw.writeData("list", "Product2G", null, qp0, reqSKU, this::log);
	    	}
		}else {
	    	org.json.JSONArray rows = reqSKUA.getJSONArray("rows");
	    	if(rows.length() > 0) {
	    		rw.writeData("list", "Article", null, qp0, reqSKUA, this::log);
	    	}
		}
		log("Blerg: /object/" + entity + "/'" + id + "'@'MASTER'");
		response = workshop.makeRequest("PUT", "/object/" + entity + "/'" + id + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			if(sku != null && !"".equals(sku) && "Product2G".equals(entity)) {
				org.json.JSONArray items = new org.json.JSONArray();
				items.put(new org.json.JSONObject().put("productNo", id).put("sku", sku));
				dr.skuProductNo( items );
			}
			log("\tWriting: " + request + "\nNot really an error from writing id: " + id + ": " + response);
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
	}
	
	private void collectCharacteristicsByEntity(java.util.LinkedList<String> product2G, java.util.LinkedList<String> article) {
		try( java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_entities")) ){
			lns.forEach(s -> {
				String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
				String[] entities = workshop.parseLine(pieces[1], "\"", ",", "\\");
				for(int i=0; i<entities.length; i++) {
					if("Product2G".equals(entities[i])) {
						product2G.addLast(pieces[0]);
					}else if("Article".equals(entities[i])) {
						article.addLast(pieces[0]);
					}
				}
			});
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	private void collectLookupValues(String lkpId, java.util.Map<String, java.util.Map<String, String>> map, java.util.Map<String, java.util.Map<String, String>> mapB, String dataType){
		if("LOOKUP".equals(dataType) && lkpId != null) {
			java.util.Map<String, String> data = null;
			data = getData(lkpId);
			map.put(lkpId, data);
			data = getDataB(lkpId);
			mapB.put(lkpId, data);
		}
	}
	
	private java.util.Map<String, String> getData(String lkpId){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		if(lkpId != null) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")))){
				lns.forEach(s -> {
					String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
					data.put(pieces[0], pieces[1]);
				});
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		return data;
	}
	
	private java.util.Map<String, String> getDataB(String lkpId){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		if(lkpId != null) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", lkpId.replaceAll("/", "<::>")))){
				lns.forEach(s -> {
					String[] pieces = workshop.parseLine(s, "\"", ";", "\\");
					data.put(pieces[1], pieces[0]);
				});
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		return data;
	}
	
	private void addValue(String name, Object value, org.json.JSONArray values) {
		if(value == null)
			return;
		values.put( new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", name))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put( value )))) );
	}
	
	private void collectLookupCharacteristics(String names, java.util.Map<String, String> characteristicsInfo, java.util.Map<String, String> s4hMapping){
		java.util.Set<String> pieces = new java.util.TreeSet<>(java.util.Arrays.asList( names.split(",") ) );
		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristics").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pcs = null;
			while((line = br.readLine()) != null) {
				pcs = workshop.parseLine(line, "\"", ";", "\\");
				if("SKU".equals( pcs[0]) ) {
					log("\n\t" + pcs.length + " - " + java.util.Arrays.asList(pcs) + "\n");
				}
				if(pcs.length == 6 && !"".equals(pcs[3]))
					data.put(pcs[3], pcs);
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		String[] d ;
		for(String piece : pieces) {
			d = data.get(piece);
			if(d != null) {
				if("MATNR".equals(piece)) {
					log("MATNR >--< " + java.util.Arrays.asList(d));
				}
				characteristicsInfo.put(d[0], d[1]);
				s4hMapping.put(d[0], d[5]);
			} else {
			}
		}
	}

	
	private void calculaProductType(String sapBehvo, String fshId, org.json.JSONArray newCharacteristicRecords, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ServiceUnavailableException {
		if(sapBehvo != null && !"".equals(sapBehvo)) {
			newCharacteristicRecords.put( createCharacteristicValueObject("SAP_BEHVO", new org.json.JSONObject().put("_code", sapBehvo.substring(0,2) )) );
			System.out.println("Got " + sapBehvo + " for SAP_BEHVO.");
			String thevalue = "1";
			try{
				org.json.JSONArray rws = new org.json.JSONObject( rw.getRc().getRequest("GET", rw.getBaseUrl() + "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \"" + sapBehvo.substring(0,2) + "\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8")
						, null) ).getJSONArray("rows");
				System.out.println("Checking sapBehvo: " + sapBehvo);
				if(rws.length() > 0) {
					thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}catch(org.json.JSONException e) {
				e.printStackTrace();
			}
			newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", thevalue) ) );
			System.out.println("Placing value: " + thevalue + " for ProductType");
		}else {
			System.out.println("No SAP_BEHVO found, placing value 1.");
			newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "1") ) );
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ParseJana122ResponseOLD.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/sftp/s4h/parseJana122Response-%g.log", 25 * 1024 * 1024, 10, true);
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
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJana122Response.log").toString(), true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	@Override
	public final void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJana122Response.log").toString(), true)))) {
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
