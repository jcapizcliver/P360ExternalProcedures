package mx.com.liverpool.p360.services.core.amqp;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.PublicationExceptions;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.amqp.run.CharacteristicChangeProcessor;
import mx.com.liverpool.p360.services.core.amqp.run.LookupsAndDictionariesProcessor;
import mx.com.liverpool.p360.services.core.amqp.run.ProductArticleChangeProcessor;
import mx.com.liverpool.p360.services.core.amqp.run.ProductArticleCharacteristicValueChangeProcessor;
import mx.com.liverpool.p360.services.core.amqp.run.StructureGroupChangeProcessor;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.temp.exports.RealExportProducts;
import mx.com.liverpool.p360.services.core.temp.exports.RealExportProducts2Mirakl;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class P360ActiveMQBPMStage extends Thread implements Closeable {

	private RESTWrapper rw = new RESTWrapper();
	private RESTWorkshop workshop = rw.getRw();
	private XMLMisc xmm = workshop.getXmm();

	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;

	private final CharacteristicChangeProcessor ccp;    					// Cambios en las características como tal
	private final ProductArticleChangeProcessor pacp;   					// Este es el del histórico
	private final LookupsAndDictionariesProcessor ladp; 					// Este es el de los cambios en valores de lkp y dct.
	private final ProductArticleCharacteristicValueChangeProcessor pacvcp;  // Cambios específicos en valores de características asociadas directamente a Article o Product2G
	private final StructureGroupChangeProcessor sgcp;						// Cambios en StructureGroups.
	
	private Thread pacvcpT;
	private Thread pacpT;
	private Thread ladpT;
	private Thread ccpT;
	private Thread sgcpT;
	
	private boolean connected = false;
	private boolean failed = false;
	
	private boolean running = true;
	
	private final int bs = 2000;
	private DBAccessDataStub dastub = new DBAccessDataStub( (ELog) new ELog() {
		
		@Override
		public void logE(Exception e) {
			P360ActiveMQBPMStage.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			P360ActiveMQBPMStage.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);
	
	private static final String USER_ECC = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final String HOST_ECC = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT_ECC = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final Path PRIVATE_KEY_PATH_ECC = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR_ECC = PropertiesManager.get( "p360.contingency.ecc.remote_directory_f40" );//Remote directory to monitor: /interfase/mer/in/step/P360/zrtuab122
	
	private static final String USER_S4H = PropertiesManager.get( "p360.contingency.s4h.userp360" ); 
	private static final String HOST_S4H = PropertiesManager.get( "p360.contingency.s4h.host" );
	private static final int PORT_S4H = Integer.parseInt(PropertiesManager.get( "p360.contingency.s4h.port" ));
	private static final Path PRIVATE_KEY_PATH_S4H = Paths.get(PropertiesManager.get( "p360.contingency.s4h.private_key_path" )); 
	private static final String REMOTE_DIR_S4H = PropertiesManager.get( "p360.contingency.s4h.remote_directory_f40" );

	private final org.json.JSONObject reqProcedeNoProcede = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ProcedeNoProcede',root,\"0000.0000.RK\",'ProcedeNoProcede',-1)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqEnriquecidoEnForo = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnriquecidoEnForo',root,\"0000.0000.RK\",'EnriquecidoEnForo',-1)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqCurrentStatus = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqExternalStatus = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqPrevStatus = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))).put("rows", new org.json.JSONArray());

	private final org.json.JSONObject reqCurrentStatusA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqExternalStatusA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.ExternalStatus"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqPrevStatusA = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.PrevStatus"))).put("rows", new org.json.JSONArray());

	private final org.json.JSONObject reqLastSentToMarketplace = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('LastSentToMarketplace',root,\"0000.0000.RK\",'LastSentToMarketplace',-1)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqPublishMktMessage = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PublishMktMessage',root,\"0000.0000.RK\",'PublishMktMessage',-1)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqFechaUltimaAprobacion = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaUltimaAprobacion',root,\"0000.0000.RK\",'FechaUltimaAprobacion',-1)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqFechaUltimaPublicacion = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaUltimaPublicacion',root,\"0000.0000.RK\",'FechaUltimaPublicacion',-1)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqFirstApprovedDate = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FirstApprovedDate',root,\"0000.0000.RK\",'FirstApprovedDate',-1)"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqFirstDateApproved = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.FirstDateApproved"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqLastDateApproved = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.LastDateApproved"))).put("rows", new org.json.JSONArray());
	private final org.json.JSONObject reqPublishMessage = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PublishMessage',root,\"0000.0000.RK\",'PublishMessage',-1)"))).put("rows", new org.json.JSONArray());
	
	private final RealExportProducts rep = new RealExportProducts();
	private final RealExportProducts2Mirakl rep2m = new RealExportProducts2Mirakl();

	private final PubSubGCP pubIdmcPutProducts = new PubSubGCP(
		    PropertiesManager.get("p360.contingency.gcp.service_account_back"),
		    PropertiesManager.get("p360.contingency.gcp.project_back"),
		    PropertiesManager.get("p360.contingency.gcp.idmc_put_products")
		);

//		private final PubSubGCP pubPostProducts = new PubSubGCP(
//		    PropertiesManager.get("p360.contingency.gcp.service_account_back"),
//		    PropertiesManager.get("p360.contingency.gcp.project_back"),
//		    PropertiesManager.get("p360.contingency.gcp.post_products_topic")
//		);
	
	private final java.util.List<String> toPublish = new java.util.ArrayList<>();
	private final java.util.List<String> toMkt = new java.util.ArrayList<>();
	
    public P360ActiveMQBPMStage() throws ServiceUnavailableException {
    	ccp = new CharacteristicChangeProcessor();
    	pacp = new ProductArticleChangeProcessor();
		ladp = new LookupsAndDictionariesProcessor();
		pacvcp = new ProductArticleCharacteristicValueChangeProcessor();
		sgcp = new StructureGroupChangeProcessor();
    }
    
    public P360ActiveMQBPMStage(String baseUrl) throws ServiceUnavailableException {
    	ccp = new CharacteristicChangeProcessor();
    	pacp = new ProductArticleChangeProcessor();
		ladp = new LookupsAndDictionariesProcessor();
		pacvcp = new ProductArticleCharacteristicValueChangeProcessor();
		sgcp = new StructureGroupChangeProcessor();
		
    }
    
    public P360ActiveMQBPMStage(String baseUrl, String baseCacheDirectory) throws ServiceUnavailableException {
    	ccp = new CharacteristicChangeProcessor();
    	pacp = new ProductArticleChangeProcessor();
		ladp = new LookupsAndDictionariesProcessor();
		pacvcp = new ProductArticleCharacteristicValueChangeProcessor();
		sgcp = new StructureGroupChangeProcessor();
		
    }

	private void connect(String host, String port, String qName){
		try{
			connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port + "?wireFormat.maxInactivityDuration=60000&keepAlive=true");
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        responseQueue = session.createQueue(qName);
	        consumer = session.createConsumer(responseQueue);
			connected = true;
		}catch(JMSException e){
			e.printStackTrace();
			failed = true;
		}
	}
	
	private void setRunning(boolean running) {
		this.running = false;
		this.pacvcp.setRunning(false);
		this.ccp.setRunning(false);
		this.pacp.setRunning(false);
		this.ladp.setRunning(false);
		this.sgcp.setRunning(false);
	}

	@Override
	public void run() {
		setRunning(false);
	}
	
	private void launchTimeSenderThread() {
		Thread t = new Thread(()->{
			log("Running sender watcher...");
			while(running) {
				try {
					sendData("Product2G", reqEnriquecidoEnForo);
					sendData("Product2G", reqLastSentToMarketplace);
					sendData("Product2G", reqPublishMktMessage);
					sendData("Product2G", reqFechaUltimaAprobacion);
					sendData("Product2G", reqFechaUltimaPublicacion);
					sendData("Product2G", reqFirstApprovedDate);
					sendData("Product2G", reqFirstDateApproved);
					sendData("Product2G", reqPublishMessage);
					sendData("Product2G", reqCurrentStatus);
					sendData("Product2G", reqExternalStatus);
					sendData("Product2G", reqPrevStatus);

					sendData("Article", reqProcedeNoProcede);
					sendData("Article", reqCurrentStatusA);
					sendData("Article", reqExternalStatusA);
					sendData("Article", reqPrevStatusA);

					// Los buffers de PACVCP se vacían una sola vez por ciclo, no una vez por cada familia.
					pacvcp.sendData();
				} catch (RuntimeException e) {
					// Una caída transitoria del REST local no debe matar el watcher.
					logE(e);
				}

				try{
					Thread.sleep(10000);
				}catch(InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			this.pacvcp.setRunning(false);
			this.ccp.setRunning(false);
			this.pacp.setRunning(false);
			this.ladp.setRunning(false);
			this.sgcp.setRunning(false);
			log("Watcher end");
		});
		t.setDaemon(true);
		t.start();
		t = new Thread(()->{
			log("Running sender to outer systems watcher...");
			while(running) {

				sendToPublication();
				sendToMkt();
				
				try{
					Thread.sleep(60000);
				}catch(InterruptedException e) {
					logE(e);
				}
			}
			log("Watcher end");
		});
		t.setDaemon(true);
		t.start();
	}
	
	private void launchListenerThread() {
		Thread t = new Thread(()->{
			log("Running... " + running);
			try(java.net.ServerSocket server = new java.net.ServerSocket(23543)){
				while(running) {
					try(
						java.net.Socket cli = server.accept();
						java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(cli.getInputStream()));
						java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(cli.getOutputStream()))
					){
						try{
							org.json.JSONObject req = new org.json.JSONObject(br.readLine());
							String action = req.getString("action");
							if("finish".equals(action.toLowerCase())) {
								this.pacvcp.setRunning(false);
								this.ccp.setRunning(false);
								this.pacp.setRunning(false);
								this.ladp.setRunning(false);
								this.sgcp.setRunning(false);
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
			}catch(java.io.IOException e) {
				logE(e);
			}
			log("Finishing...");
		});
		t.start();
	}
	
	private void sendToMkt() {
		if(!toMkt.isEmpty()) {
			java.util.Date currentDate = new java.util.Date();
			String sent2 = rep2m.doIt( toMkt.toArray(new String[] {}), true, workshop.getBaseUrl() );
			for(String externalId : toMkt) {
				addLastSentToMarketplace(externalId, new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(currentDate));
				addPublishMktMessage(externalId, sent2 == null ? "Problema enviando a MKT." : sent2);
				log("Result from sending to mkt: " + sent2);
			}
			toMkt.clear();
		}
		
	}

	private void sendToPublication() {
		if(!toPublish.isEmpty()) {
			java.util.Date currentDate = new java.util.Date();
			String sent = rep.doIt(toPublish.toArray(new String[] {}), true, workshop.getBaseUrl());
			log("Result from sending to mkt,oms: " + sent);
			if(sent != null && sent.contains("Se proceso correctamente") && sent.contains("pépele")) {
				java.util.Map<String, String> qp = new java.util.HashMap<>();
				qp.put("fields", 
						   "Product2G.ProductNo"
						+ ",Product2G.FirstDateApproved"
						+ ",Product2G.StatusModification"
						+ ",Product2G.LastDateApproved"
					);
				qp.put("pageSize", "2000" );
				java.util.Map<String, String> qp1 = new java.util.HashMap<>();
				qp1.put("fields", 
								   "Article.SupplierAID"
								+ ",Article.FirstDateApproved"
								+ ",Article.ProductImageURL"
						);
				qp1.put("pageSize", "2000" );
				org.json.JSONArray ir = new org.json.JSONArray();
				org.json.JSONObject jsonResponse = new org.json.JSONObject();
				org.json.JSONObject jsonResponsePO = new org.json.JSONObject();
				int a = 0;
				StringBuilder sb = new StringBuilder();
				java.util.Map<String, String> qp0 = new java.util.HashMap<>();
				qp0.put("includeObjectsInProtocol", "false");
				RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.FirstDateApproved")), 2000, request -> rw.writeData("list", "Article", null, qp0, request, this::log) );
				RequestHandler rh2 = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.LastDateApproved")),  2000, request -> rw.writeData("list", "Article", null, qp0, request, this::log) );
				String currentDateStr = currentDate.toInstant().atZone(java.time.ZoneId.systemDefault()).format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") );
				org.json.JSONArray items = new org.json.JSONArray();
				jsonResponse.put("products", items);
				org.json.JSONArray itemsPO = new org.json.JSONArray();
				jsonResponsePO.put("products", itemsPO);
				for(String externalId : toPublish) {
					addFechaUltimaPublicacion(externalId, currentDateStr);
					addLastDateApprove(externalId, currentDateStr);
					addPublishMessage(externalId, sent == null ? "Problema enviando a ATG." : sent);
					ir.put(externalId);
					sb.append(sb.length() == 0 ? "" : ",").append("'").append(externalId).append("'@1");
					a++;
					if(a % 1000 == 0) {
						qp.put("items", sb.toString());
						rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
							org.json.JSONArray values = row.getJSONArray("values");
							String lastDateApproved = null;
							if("".equals(values.getString(1))) {
								addFirstDateApproved(values.getString(0), currentDateStr);
								addFirstApprovedDate(values.getString(0), currentDateStr);
								lastDateApproved = currentDateStr;
							}else {
								lastDateApproved = values.getString(3);
							}
							boolean wereYouInForo = wereYouInForo(lastDateApproved, values.getString(2));
	                        addEnriquecidoEnForo(values.getString(0), wereYouInForo);
	                        org.json.JSONObject item = new org.json.JSONObject();
							item.put( "enrichmentOriginForo", wereYouInForo);
							item.put( "proposalId", values.getString(0));
							item.put( "entityType", "Generic" );
							items.put(item);
							org.json.JSONObject itemPO = new org.json.JSONObject();
							itemPO.put( "enrichmentOriginForo", wereYouInForo);
							itemPO.put( "proposalId", values.getString(0));
							itemsPO.put(itemPO);
						});
						qp1.put("products", sb.toString());
						rw.collectData("list", "Article", null, "byProducts", qp1, row -> {
							org.json.JSONArray values = row.getJSONArray("values");
							if(!"".equals(values.getString(2))) {
								if("".equals(values.getString(1)))
									rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(currentDateStr)));
								rh2.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(currentDateStr)));
							}
						});
						sb.setLength(0);
					}
				}
				if(sb.length() > 0) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						String lastDateApproved = null;
						if("".equals(values.getString(1))) {
							addFirstDateApproved(values.getString(0), currentDateStr);
							addFirstApprovedDate(values.getString(0), currentDateStr);
							lastDateApproved = currentDateStr;
						}else {
							lastDateApproved = values.getString(3);
						}
						boolean wereYouInForo = wereYouInForo(lastDateApproved, values.getString(2));
	                    addEnriquecidoEnForo(values.getString(0), wereYouInForo);
	                    org.json.JSONObject item = new org.json.JSONObject();
						item.put( "enrichmentOriginForo", wereYouInForo);
						item.put( "proposalId", values.getString(0));
						item.put("entityType", "Generic" );
						items.put(item);
						org.json.JSONObject itemPO = new org.json.JSONObject();
						itemPO.put( "enrichmentOriginForo", wereYouInForo);
						itemPO.put( "proposalId", values.getString(0));
						itemsPO.put(itemPO);
					});
					rw.collectData("list", "Article", null, "byProducts", qp1, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						if(!"".equals(values.getString(2))) {
							if("".equals(values.getString(1)))
								rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(currentDateStr)));
							rh2.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(currentDateStr)));
						}
					});
					sb.setLength(0);
				}
				rh.sendData();
				rh2.sendData();
				pubIdmcPutProducts.publishMessage(jsonResponse.toString());
//				pubPostProducts.publishMessage(jsonResponsePO.toString());
				toPublish.clear();
			}
		}
	}
	
	private void process(String host, String port, String qName) throws ServiceUnavailableException {
		if(!failed){
			if(!connected){
				connect(host, port, qName);
				log("Starting ccp...");
				Thread t = new Thread(()->{
					ccp.connect(host, Integer.parseInt(port), "CHARACTERISTIC_UPD_SYNC");
					ccp.process();
				});
				t.setPriority(Thread.currentThread().getPriority() - 1);
				t.setDaemon(false);
				t.start();
				log("Started ccp... " + (Thread.currentThread().getPriority() - 1));
				ccpT = t;
				log("Starting pvcvp...");
				t = new Thread(()->{
					pacp.connect(host, Integer.parseInt(port), "ProductVariantCharacteristicValue_CHANGE_HISTORY");
					pacp.process();
				});
				t.setPriority(Thread.currentThread().getPriority() - 1);
				t.setDaemon(false);
				t.start();
				log("Started pvcvp... " + (Thread.currentThread().getPriority() - 1));
				pacpT = t;
				log("Starting ladp...");
				Thread t1 = new Thread(()->{
					ladp.connect(host, Integer.parseInt(port), "LKP_DCT_VAL_C");
					ladp.process();
				});
				t1.setPriority(Thread.currentThread().getPriority() - 1);
				t1.setDaemon(false);
				t1.start();
				log("Started ladp... " + (Thread.currentThread().getPriority() - 1));
				ladpT = t1;
				Thread t2 = new Thread(()->{
					pacvcp.connect(host, Integer.parseInt(port), "GENERAL_CHARACTERISTIC_VALUE_CHANGE");
					try {
						pacvcp.process();
					} catch (ServiceUnavailableException e) {
						logE(e);
					}
				});
				t2.setPriority(Thread.currentThread().getPriority() - 1);
				t2.setDaemon(false);
				t2.start();
				log("Started Characteristics... " + (Thread.currentThread().getPriority() - 1));
				pacvcpT = t2;
				Thread t3 = new Thread(()->{
					sgcp.connect(host, Integer.parseInt(port), "STRUCTURE_GROUP_UPD_SYNC");
					sgcp.process();
				});
				t3.setPriority(Thread.currentThread().getPriority() - 1);
				t3.setDaemon(false);
				t3.start();
				sgcpT = t3;
				log("Started Characteristics... " + (Thread.currentThread().getPriority() - 1));
			}
		}
		try{
			log("Now running...");
			while(running){
				try {
					responseMessage = consumer.receive(30);
					if (responseMessage != null && responseMessage instanceof TextMessage) {
						try{
							log("Now processing message...");
							messageProcessor(((TextMessage) responseMessage).getText());
						}catch(Exception e) {
							// Un mensaje defectuoso o una indisponibilidad transitoria no debe matar el consumer principal.
							logE(e);
						}
						log("Doney");
					}
				}catch(JMSException e) {
					logE(e);
					break;
				}
			}
		}finally {
			disconnect();
		}
		try {
			this.ccp.setRunning(false);
			this.pacp.setRunning(false);
			this.ladp.setRunning(false);
			this.pacvcp.setRunning(false);
			this.sgcp.setRunning(false);
			pacpT.join();
			ladpT.join();
			ccpT.join();
			pacvcpT.join();
			sgcpT.join();
			this.ccp.close();
			this.ladp.close();
			this.pacvcp.close();
//			this.pacp.setRunning(false);
//			this.sgcp.setRunning(false);
		}catch(InterruptedException e) {
			e.printStackTrace();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException, ServiceUnavailableException {

		org.json.JSONObject response = null;
		org.json.JSONArray characteristicRecords = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		String entity = null;
		String internalId = null;
		String externalId = null;
		String changeSummary = null;

		String firstDateApproved = null;
		String externalStatusCode = null;
		String externalStatus = null;
		String nes = null;
		String currentStatusOld = null;
		String currentStatusNew = null;
		String rejectionInfo = null;
		org.json.JSONObject jsonResponse = null;
		org.json.JSONObject jsonResponse4PO = null;
		org.json.JSONArray rechazos = new org.json.JSONArray();
		org.json.JSONArray changedField = null;

		java.util.Set<String> changedFieldSet = new java.util.TreeSet<>();
		org.json.JSONObject json = new org.json.JSONObject(message);

    	String identifier = null;

		if(json.has("entityItemChange")) {
	     	entity = json.getJSONObject("entityItemChange").getString("_entity");
	     	internalId = json.getJSONObject("entityItemChange").getJSONObject("_entityItem").getString("_internalId");
	     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
	     	changedField = json.getJSONObject("entityItemChange").has("_changedField") ? json.getJSONObject("entityItemChange").getJSONArray("_changedField") : new org.json.JSONArray();
			log("A message body: " + json);
			for(int i=0; i<changedField.length(); i++) {
				changedFieldSet.add(changedField.getString(i));
			}

	     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	Document doc;

	    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
	    		doc = builder.parse( baos );
	    		doc.getDocumentElement().normalize();
	    	}
	    	
			if(doc != null) {
				Element rootElement = doc.getDocumentElement();
				externalId = json.getJSONObject("entityItemChange").getString("_identifier");

				if("Product2G".equals(entity) || "Article".equals(entity)) {
					
					if(changedFieldSet.contains("Product2G.CurrentStatus")) {
						try{
							currentStatusOld = xmm.byName( xmm.byName( xmm.byName( xmm.byName(rootElement, "product"), "currentStatus"), "_old"), "_key") .getTextContent();
						}catch(NullPointerException e) {
							currentStatusOld = "";
						}
						try{
							currentStatusNew = xmm.byName( xmm.byName( xmm.byName( xmm.byName(rootElement, "product"), "currentStatus"), "_current"), "_key" ).getTextContent();
						}catch(NullPointerException e) {
							currentStatusNew = "";
						}
					}
					
						identifier = externalId;
						if(changedFieldSet.contains("Product2G.CurrentStatus")) {
							String business = null;
							try{
								currentStatusOld = xmm.byName( xmm.byName( xmm.byName( xmm.byName(rootElement, "product"), "currentStatus"), "_old"), "_key") .getTextContent();
							}catch(NullPointerException e) {
								currentStatusOld = "";
							}
							try{
								currentStatusNew = xmm.byName( xmm.byName( xmm.byName( xmm.byName(rootElement, "product"), "currentStatus"), "_current"), "_key" ).getTextContent();
								externalStatusCode = getExternalStatus(currentStatusNew);
								externalStatus = getExternalStatusCode(currentStatusNew);
								log("Got NES: " + nes + " | " + currentStatusNew + " | " + externalStatusCode + " | " + currentStatusOld);
							}catch(NullPointerException e) {
								currentStatusNew = "";
							}
							String rsp = dr.getProductData(new org.json.JSONArray().put(externalId));
							String sku = "";
							if(rsp != null)
							try {
								org.json.JSONObject jr = new org.json.JSONObject(rsp);
								org.json.JSONArray items = jr.getJSONArray("items");
								org.json.JSONObject j0 = items.getJSONObject(0);
								business = j0.getString("Business");
								j0.put("CurrentStatus", currentStatusNew);
								sku = j0.getString("SKU");
								log("Sent to admin: " + dr.putProductData(items) );
							}catch(org.json.JSONException e) {
								logE(e);
							}
							jsonResponse = new org.json.JSONObject()
									.put("products", new org.json.JSONArray()
											.put(new org.json.JSONObject()
													.put("proposalId", externalId)
													.put("internalStatus", getStatusLabel(currentStatusNew))
													.put("externalStatus", externalStatusCode == null ? "" : externalStatusCode)
													.put("previousStatus", getStatusLabel(currentStatusOld))
													.put("sku", sku)
													.put("entityType", "Product2G".equals(entity) ? "Generic" : "Variant")
											));
							if("Product2G".equals(entity)) {
								jsonResponse4PO = new org.json.JSONObject()
										.put("products", new org.json.JSONArray()
												.put(new org.json.JSONObject()
														.put("proposalId", externalId)
														.put("internalStatus", getStatusLabel(currentStatusNew))
														.put("externalStatus", externalStatusCode == null ? "" : externalStatusCode)
														.put("previousStatus", getStatusLabel(currentStatusOld))
														.put("sku", sku)
														));
							}else {
								String rr = dr.getArticleData(new org.json.JSONArray().put(externalId));
								if(rr != null) {
									org.json.JSONObject jr = new org.json.JSONObject(rr);
									org.json.JSONArray items = jr.getJSONArray("items");
									org.json.JSONObject item = items.getJSONObject(0);
									if(item.has("ProductNo") && !"".equals(item.getString("ProductNo"))) {
										jsonResponse4PO = new org.json.JSONObject()
												.put("products", new org.json.JSONArray()
														.put(new org.json.JSONObject()
																.put("proposalId", item.getString("ProductNo"))
																.put("variants", new org.json.JSONArray()
																			.put(new org.json.JSONObject()
																					.put("internalStatus", getStatusLabel(currentStatusNew))
																					.put("externalStatus", externalStatusCode == null ? "" : externalStatusCode)
																					.put("previousStatus", getStatusLabel(currentStatusOld))
																					.put("sku", sku)
																				)
																	)
															));
									}
								}
							}
							
							log("Old current status: " + currentStatusOld);
							log("Current current status: " + currentStatusNew);
							log("external status: " + externalStatusCode);

							if("1020".equals(currentStatusOld)) {
								new PublicationExceptions().isException(workshop, "'" + externalId + "'@1");
							}
							if("1020".equals(currentStatusOld) || "1007".equals(currentStatusNew) || ("".equals(currentStatusOld) && "1002".equals(currentStatusNew))){
								log("(tf40) " + externalId + " Estado anterior: " + currentStatusOld);
								log("(tf40) " + externalId + " Estado actual: " + currentStatusNew);
								if("1002".equals(currentStatusNew) || "1007".equals(currentStatusNew)) {
									log("(tf40) entramos a \"nos vamos a foro\"");
									org.json.JSONObject jr = new org.json.JSONObject(rsp);
									org.json.JSONArray items = jr.getJSONArray("items");
									org.json.JSONObject j0 = items.getJSONObject(0);
									business = j0.getString("Business");
									String fotoTomadaLiverpool = j0.getString("FotoTomadaLiverpool");
									log("(tf40) negocio: " + business);
									java.util.List<String> vars = java.util.Arrays.asList( dr.getVariants(externalId).toArray(new String[] {}) );
									if("LVP".equals(business)) {
										if("00".equals(j0.getString("SAPObjectType"))) {
											sku = j0.getString("SKU");
											if("".equals(sku)) {
												rsp = dr.getArticleData(new org.json.JSONArray().put(vars.get(0)));
												if(rsp != null) {
													jr = new org.json.JSONObject(rsp);
													items = jr.getJSONArray("items");
													j0 = items.getJSONObject(0);
													sku = j0.getString("SKU");
												}
											}
											log("(tf40) Soy individual.");
											if(!"".equals(sku)) {
												java.util.Map<String, String> qp00 = new java.util.HashMap<>();
												qp00.put("includeLabels", "true");
												qp00.put("includeIds", "true");
												qp00.put("entityFilter", "Product2GCharacteristicValue");
												qp00.put("qualificationFilter", "characteristic(SistemaOrigen)");
												org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
												org.json.JSONObject jd = jResp.getJSONObject("_data");
												String sistemaOrigen = "1"; //jd.has("_characteristicRecords") ? jd.getJSONArray("_characteristicRecords").getJSONObject(0).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : "1";
												org.json.JSONArray cs = jd.has("_characteristicRecords") ? jr.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
												String cid = null;
												log("On 1007 for product (" + externalId + "): " + jr);
												for( int idx = 0; idx < cs.length(); idx++ ) {
													cid = cs.getJSONObject(idx).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
													if("SistemaOrigen".equals(cid)) {
														sistemaOrigen = cs.getJSONObject(idx).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
													}
												}
												if(!"N".equals(fotoTomadaLiverpool)) {
													log("(tf40) SistemaOrigen: " + sistemaOrigen);
													Object[] objs = getClientToECC();
													try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
														writeToSftp(sftpCli, "Código SKU|ESTADO|ORIGEN\n" + sku + "|" + ( "1007".equals(currentStatusNew) ? 1 : 3) + "|" + (sistemaOrigen == null || "".equals(sistemaOrigen) ? "1" : sistemaOrigen), REMOTE_DIR_ECC);
													}catch(java.io.IOException e) {
											        	log("No fue posible escribir a foro 40 " + externalId);
											        }
												}
											}else {
												log("(tf40) Estaba vacío, por eso no se fue a foro 40");
											}
										}else {
											log("(tf40) Soy " + j0.getString("SAPObjectType"));
											sku = j0.getString("SKU");
											if(!"".equals(sku)) {
												java.util.Map<String, String> qp00 = new java.util.HashMap<>();
												qp00.put("includeLabels", "true");
												qp00.put("includeIds", "true");
												qp00.put("entityFilter", "Product2GCharacteristicValue");
												qp00.put("qualificationFilter", "characteristic(SistemaOrigen)");
												org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
												if(jResp == null) {
													log("Problem querying server: " + rw.getRw().getRawResponse());
												}else {
													org.json.JSONObject jd = jResp.getJSONObject("_data");
													String sistemaOrigen = "1"; //jd.has("_characteristicRecords") ? jd.getJSONArray("_characteristicRecords").getJSONObject(0).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : "1";
													org.json.JSONArray cs = jd.has("_characteristicRecords") ? jr.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
													String cid = null;
													log("On 1007 for product (" + externalId + "): " + jr);
													for( int idx = 0; idx < cs.length(); idx++ ) {
														cid = cs.getJSONObject(idx).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
														if("SistemaOrigen".equals(cid)) {
															sistemaOrigen = cs.getJSONObject(idx).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
														}
													}
													log("(tf40) " + externalId + " SistemaOrigen: " + sistemaOrigen + "|" + fotoTomadaLiverpool);
													if(!"N".equals(fotoTomadaLiverpool)) {
														org.json.JSONArray jvars = new org.json.JSONArray();
														for(int i=0; i<vars.size(); i++) {
															jvars.put(vars.get(i));
														}
														rsp = dr.getArticleData(jvars);
														if(rsp != null) {
															jr = new org.json.JSONObject(rsp);
															items = jr.getJSONArray("items");
															Object[] objs = getClientToECC();
															try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
																StringBuilder ssb = new StringBuilder();
																for(int i=0; i<items.length(); i++) {
																	j0 = items.getJSONObject(i);
																	sku = j0.getString("SKU");
																	ssb.append(ssb.length() == 0 ? "" : "\n").append( sku + "|" + ( "1007".equals(currentStatusNew) ? 1 : 3) + "|" + (sistemaOrigen == null || "".equals(sistemaOrigen) ? "1" : sistemaOrigen) );
																}
																writeToSftp(sftpCli, "Código SKU|ESTADO|ORIGEN\n" + ssb.toString(), REMOTE_DIR_ECC);
															}catch(java.io.IOException e) {
													        	log("No fue posible escribir a foro 40 " + externalId);
													        }
														}
													}else { log("Skipped due to Foto No Tomada Liverpool: " + externalId + ":" + fotoTomadaLiverpool); }
												}
											}else {
												log("(tf40) Estaba vacío, por eso no se fue a foro 40");
											}
										}
									}else if("SBB".equals(business)) {
										log("(tf40) SBB: " + j0);
										if(vars.isEmpty()) {
											log("No variants found for " + externalId);
										}else {
											if("00".equals(j0.getString("SAPObjectType"))) {
												sku = j0.getString("SKU");
												if("".equals(sku)) {
													rsp = dr.getArticleData(new org.json.JSONArray().put(vars.get(0)));
													if(rsp != null) {
														jr = new org.json.JSONObject(rsp);
														items = jr.getJSONArray("items");
														j0 = items.getJSONObject(0);
														sku = j0.getString("SKU");
													}
												}
												if(!"".equals(sku)) {
													java.util.Map<String, String> qp00 = new java.util.HashMap<>();
													qp00.put("includeLabels", "true");
													qp00.put("includeIds", "true");
													qp00.put("entityFilter", "Product2G");
													org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
													org.json.JSONObject jd = jResp.getJSONObject("_data");
													String fda = jd.has("firstDateApproved") ? jd.getString("firstDateApproved").replace("T", " ") : "1007".equals(currentStatusNew) ? new java.util.Date().toInstant().atZone(java.time.ZoneId.systemDefault()).format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") ) : "" ;
													Object[] objs = getClientToS4H();
													try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
														writeToSftp(sftpCli, "Código SKU,Estado,Responsabilidad de fotos,Fecha Primera Aprobación\n" +  sku + "," + ("1007".equals(currentStatusNew) ? 1 : 3) + "," + fotoTomadaLiverpool + "," + fda, REMOTE_DIR_S4H);
													}catch(java.io.IOException e) {
														log("No fue posible escribir a foro 40 " + externalId);
													}
												}else {
													log("(tf40) Estaba vacío, por eso no se fue a foro 40");
												}
											}else {
												sku = j0.getString("SKU");
												if(!"".equals(sku)) {
													java.util.Map<String, String> qp00 = new java.util.HashMap<>();
													qp00.put("includeLabels", "true");
													qp00.put("includeIds", "true");
													qp00.put("entityFilter", "Product2G");
													org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
													org.json.JSONObject jd = jResp.getJSONObject("_data");
													String fda = jd.has("firstDateApproved") ? jd.getString("firstDateApproved").replace("T", " ") : "1007".equals(currentStatusNew) ? new java.util.Date().toInstant().atZone(java.time.ZoneId.systemDefault()).format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") ) : "";
													org.json.JSONArray jvars = new org.json.JSONArray();
													for(int i=0; i<vars.size(); i++) {
														jvars.put(vars.get(i));
													}
													rsp = dr.getArticleData(jvars);
													if(rsp != null) {
														jr = new org.json.JSONObject(rsp);
														items = jr.getJSONArray("items");
														log("(tf40) About to send to sbb");
														Object[] objs = getClientToS4H();
														StringBuilder sb = new StringBuilder();
														sb.append("Código SKU,Estado,Responsabilidad de fotos,Fecha Primera Aprobación\n");
														try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
															for(int i=0; i<items.length(); i++) {
																j0 = items.getJSONObject(i);
																sku = j0.getString("SKU");
																if("1007".equals(currentStatusNew)) {
																	sb.append(sku + "," + 1 + "," + fotoTomadaLiverpool + "," + fda).append("\n");
																}else {
																	if("Y".equals(fotoTomadaLiverpool)) {
																		sb.append(sku + "," + 3 + ",Y," + fda).append("\n");
																	}else {
																		sb.append(sku + "," + 2 + "," + "N" + "," + fda);
																	}
																}
															}
															writeToSftp(sftpCli, sb.toString(), REMOTE_DIR_S4H);
														}catch(java.io.IOException e) {
															log("No fue posible escribir a foro 40 " + externalId);
															logE(e);
															e.printStackTrace();
														}
													}
												}else {
													log("Estaba vacío, por eso no se fue a foro 40");
												}
											}
										}
										
									}
								}
							}
							if(
									("1028".equals(currentStatusNew) && "1022".equals(currentStatusOld)) ||
									("1029".equals(currentStatusNew) && "1021".equals(currentStatusOld)) ||
									("1030".equals(currentStatusNew) && "1023".equals(currentStatusOld)) ||
									("1006".equals(currentStatusNew) && "1031".equals(currentStatusOld)) ||
									("1022".equals(currentStatusNew) && "1023".equals(currentStatusOld))
							){
								if("1028".equals(currentStatusNew) && "1022".equals(currentStatusOld)) {

								}

								qp.put("entityFilter", entity + "," + entity + "CharacteristicValue" );
								qp.put("includeIds", "true");
								qp.put("includeLabels", "true");
								log("requesting: " + workshop.getBaseUrl() + "/object/" + entity + "/" + internalId);
								response = workshop.makeRequest("GET", "/object/" + entity + "/" + internalId, qp, null);
								if(response == null || !response.has("_data")) {
									log("No fue posible leer el objeto para complementar rechazos. entity=" + entity + ", internalId=" + internalId + ", rawResponse=" + workshop.getRawResponse());
								} else {
									org.json.JSONObject data = response.getJSONObject("_data");
									rejectionInfo  = data.optString("rejectionInfo", null);
									characteristicRecords = data.has("_characteristicRecords") ? data.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
									java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap = new java.util.TreeMap<>();
									characteristicsToMap(characteristicRecords, characteristicRecordsMap, rechazos);
									complementaRechazos(rechazos, internalId, rejectionInfo, "Product2G", externalId);
								}
							}else if("1023".equals(currentStatusOld) && "1007".equals(currentStatusNew)) {
//								log("Elm: " + workshop.getRawResponse());
							}
							if("1022".equals(currentStatusOld)) {
								dejaWorkflow(internalId, "23540", "QARevision","Revisión QA");
							}else if("1023".equals(currentStatusOld)) { log("Leaging Category");
								dejaWorkflow(internalId, "23541", "CategoryRevision", "Revisión Category");
							}else if("1031".equals(currentStatusOld)) {
								dejaWorkflow(internalId, "23542", "RepopulationRevision", "Revisión Repoblamiento");
							}else if("1021".equals(currentStatusOld)) {
								dejaWorkflow(internalId, "1234567", "Sfera", "Nuevo producto Sfera");
							}
							if("1022".equals(currentStatusNew)) {
								qp.put("fields",
										"Product2G".equals(entity) 
										?
											   "Product2G.LastDateApproved"
											+ ",Product2G.StatusModification"
										: 
											   "Article.LastDateApproved"
											+ ",Article.StatusModification"
										);
								qp.put("items", internalId);
								log("requesting: " + workshop.getBaseUrl() + "/list/" + entity + "/byItems");
								response = workshop.makeRequest("GET", "/list/" + entity + "/byItems", qp, null);
								if(response == null) {
									log("Error, got null response from requested url -->" + workshop.getRawResponse() + "<--");
									log("Message skipped without stopping the consumer: " + json);
									return;
								}
								org.json.JSONArray rws = response.has("rows") ? response.getJSONArray("rows") : null;
								if(rws != null && rws.length() > 0) {
									org.json.JSONArray values = rws.getJSONObject(0).getJSONArray("values");
									String lastDateApprove =  values.getString(0);// data.has("lastDateApproved") ? data.getString("lastDateApproved") : "";
									boolean wereYouInForo = wereYouInForo(lastDateApprove, values.getString(1));// data.getString("statusModification"));
									if("1026".equals(currentStatusOld)) {
										wereYouInForo = true;
									}
									log("EnriquecidoForo: " + wereYouInForo + " (firstDateApproved: " + firstDateApproved + ", sm: " + values.getString(1) + ")");// data.getString("statusModification") + ")");
									jsonResponse.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", wereYouInForo);
									jsonResponse4PO.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", wereYouInForo);
//								addCharacteristicValue(characteristicRecordsForUpdate, "EnriquecidoEnForo", String.valueOf(wereYouInForo), false, false);
									addEnriquecidoEnForo(externalId, wereYouInForo);
//								sendUpdateObjectAPI(externalId, new org.json.JSONObject().put("_characteristicRecords", characteristicRecordsForUpdate));
									log("Setting \"ProcedeNoProcede\"");
									setProcedeNoProcede(externalId);
									ingresaWorkflow(internalId, "23540", "QARevision", "Revisión QA");
								}else {
									log("Problems (qd): " + workshop.getRawResponse());
								}
							}else if("1023".equals(currentStatusNew)){
								ingresaWorkflow(internalId, "23541", "CategoryRevision", "Revisión Category");
							}else if("1031".equals(currentStatusNew)) {
								ingresaWorkflow(internalId, "23542", "RepopulationRevision", "Revisión Repoblamiento");
							}else if("1021".equals(currentStatusNew)) {
							}else if("1006".equals(currentStatusNew)) {
								qp.put("entityFilter", entity + "," + entity + "CharacteristicValue" );
								qp.put("includeIds", "true");
								qp.put("includeLabels", "true");
								log("requesting: " + workshop.getBaseUrl() + "/object/" + entity + "/" + internalId);
								response = workshop.makeRequest("GET", "/object/" + entity + "/" + internalId, qp, null);
								if(response == null || !response.has("_data")) {
									log("No fue posible leer el objeto para complementar rechazos. entity=" + entity + ", internalId=" + internalId + ", rawResponse=" + workshop.getRawResponse());
								} else {
									org.json.JSONObject data = response.getJSONObject("_data");
									rejectionInfo  = data.optString("rejectionInfo", null);
									characteristicRecords = data.has("_characteristicRecords") ? data.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
									java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap = new java.util.TreeMap<>();
									characteristicsToMap(characteristicRecords, characteristicRecordsMap, rechazos);
									complementaRechazos(rechazos, internalId, rejectionInfo, "Product2G", externalId);
								}
							}else if("1007".equals(currentStatusNew)) {
								log("About to send the request to ATG,OMS,MKT: " + externalId + " (" + workshop.getBaseUrl() + ")");
								try {
									if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.send_to_atg"))) {
//										toPublish.add(externalId);
//										if(toPublish.size() == 10) {
//											sendToPublication();
											appendAtgPendingId(externalId);
//										}
									}
									if(!"SBB".equals(business)) {
										if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.send_to_mkt"))) {
//											toMkt.add(externalId);
//											if(toMkt.size() == 10) {
//												sendToMkt();
												appendMktPendingId(externalId);
//											}
										}
									}
								}catch(Exception e) {
									logE(e);
								}
							}else if("1001".equals(currentStatusNew)) {
								
							}else if("1009".equals(currentStatusNew)) {
								
							}else if("1002".equals(currentStatusNew)) {
								jsonResponse.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", true);
								jsonResponse4PO.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", true);
//								addCharacteristicValue(characteristicRecordsForUpdate, "EnriquecidoEnForo", "true", false, false);
								addEnriquecidoEnForo(externalId, true);
							} else if("1026".equals(currentStatusNew)) {
								jsonResponse.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", true);
								jsonResponse4PO.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", true);
//								addCharacteristicValue(characteristicRecordsForUpdate, "EnriquecidoEnForo", "true", false, false);
								addEnriquecidoEnForo(externalId, true);
							}else if("1004".equals(currentStatusNew)) {
								if("1002".equals(currentStatusOld) || "1026".equals(currentStatusOld)) {
									org.json.JSONArray characteristicRecordsForUpdate = new org.json.JSONArray();
//									addCharacteristicValue(characteristicRecordsForUpdate, "EnriquecidoEnForo", "false", false, false);
									addEnriquecidoEnForo(externalId, false);
									jsonResponse.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", false);
									jsonResponse4PO.getJSONArray("products").getJSONObject(0).put("enrichmentOriginForo", false);
									org.json.JSONObject dataForUpdate = new org.json.JSONObject();
									dataForUpdate.put("_characteristicRecords", characteristicRecordsForUpdate);
									revisaTieneImagen(externalId, dataForUpdate);
								}
							}
							if(currentStatusNew != null) {
								log("Calculating new statuses...");
								setExternalStatus(identifier, currentStatusNew, externalStatus, currentStatusOld );
							}
							log("JSONResponse (from product status change): " + jsonResponse);
							pubIdmcPutProducts.publishMessage(jsonResponse.toString());
//							pubPostProducts.publishMessage(jsonResponse4PO.toString());
						}else if(changedFieldSet.contains("Article.CurrentStatus")) {
						}
				}else if("StructureGroup".equals(entity)) {
				}else if("StandardizationValue".equals(entity)) {
				}else if("Characteristic".equals(entity)) {
				}else if("LookupValue".equals(entity)) {
				}
			}
		}else if(json.has("entityItemsDeleted")){}else {
			log("Unknown\n" + json.toString());
		}
	}
	
	private String filePrefix = "STEP_SKU";

    private String writeToSftp(SftpClient sftp, String content, String remoteBasePath) throws IOException {

    	LocalDateTime now = LocalDateTime.now();
        String dateKey = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); // yyyyMMdd
        String fileName = null;
        String fullPath = null;
        log("(tf40) Now generating files... -->" + filePrefix + "<--");
        fileName = String.format( filePrefix +  "%s.txt", dateKey);
        log("(tf40) First path: " + fileName);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        log("(tf40) Writing: " + fullPath);
        OutputStream os = sftp.write(fullPath);
    	log("(tf40) Writing out: " + fullPath);
        os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        os.flush();
        os.close();
        log("(tf40) LOG:: WROTE.");
        log("(tf40) content: " + content + "\n./Content.");

        return fullPath;
    }
	
	private Object[] getClientToECC() throws IOException {
		SshClient client = SshClient.setUpDefaultClient();
        client.setKeyIdentityProvider(new FileKeyPairProvider(PRIVATE_KEY_PATH_ECC));
        client.start();
        ClientSession session = client.connect(USER_ECC, HOST_ECC, PORT_ECC)
            .verify(15, TimeUnit.SECONDS)
            .getSession();
        session.auth().verify(15, TimeUnit.SECONDS);
        SftpClient sftp = SftpClientFactory.instance().createSftpClient(session);
        return new Object[] {client, sftp};
	}
	
	private Object[] getClientToS4H() throws IOException {
		SshClient client = SshClient.setUpDefaultClient();
		client.setKeyIdentityProvider(new FileKeyPairProvider(PRIVATE_KEY_PATH_S4H));
		client.start();
		ClientSession session = client.connect(USER_S4H, HOST_S4H, PORT_S4H)
				.verify(15, TimeUnit.SECONDS)
				.getSession();
		session.auth().verify(15, TimeUnit.SECONDS);
		SftpClient sftp = SftpClientFactory.instance().createSftpClient(session);
		return new Object[] { client, sftp };
	}
	
	private String getStatusLabel(String key) {
		return 
			  "1001".equals(key) ? "Propuesta Generada"
			: "1002".equals(key) ? "Pendiente Inicio Enriquecimiento"
			: "1003".equals(key) ? "Revisi\u00f3n Compras"
			: "1004".equals(key) ? "Carga de Imagen"
			: "1005".equals(key) ? "Rechazada"
			: "1006".equals(key) ? "Por Actualizar "
			: "1007".equals(key) ? "Aprobada"
			: "1008".equals(key) ? "Modificaci\u00f3n "
			: "1009".equals(key) ? "Cancelado"
			: "1010".equals(key) ? "En Proceso Liverpool"
			: "1011".equals(key) ? "En Proceso de Env\u00edo"
			: "1020".equals(key) ? "Creaci\u00f3n de SKU"
			: "1021".equals(key) ? "Gobierno de Datos"
			: "1022".equals(key) ? "Revisi\u00f3n QA"
			: "1023".equals(key) ? "Category"
			: "1024".equals(key) ? "Rechazo Publicaci\u00f3n"
			: "1025".equals(key) ? "Eliminada"
			: "1026".equals(key) ? "En Proceso Foro"
			: "1027".equals(key) ? "Rechazo Compras"
			: "1028".equals(key) ? "Rechazo QA"
			: "1029".equals(key) ? "Rechazo Gobierno"
			: "1030".equals(key) ? "Rechazo Category"
			: "1031".equals(key) ? "Repoblamiento"
			: "1032".equals(key) ? "Excepci\u00f3n de Catalogaci\u00f3n"
			: "";
	}
	
	private void sendData(String entity, org.json.JSONObject req) {
		org.json.JSONArray rows = req.getJSONArray("rows");
		if(rows.length() > 0) {
			if(req.equals(reqEnriquecidoEnForo)) {
				log("Sending list of values for enriquecidoEnForo (" + rows.length() + " elements)");
			}
			writeListDataKeepingRowsOnFailure(entity, req);
		}
	}

	private boolean writeListDataKeepingRowsOnFailure(String entity, org.json.JSONObject request) {
		org.json.JSONArray rows = request.getJSONArray("rows");
		if(rows.length() == 0) {
			return true;
		}
		int rowCount = rows.length();
		String url = workshop.getBaseUrl() + "/list/" + entity + "?includeObjectsInProtocol=false";
		try {
			String rawResponse = workshop.getRc().getRequest("POST", url, request.toString());
			log(rawResponse);
			while(rows.length() > 0) {
				rows.remove(0);
			}
			return true;
		} catch(Exception e) {
			// Importante: NO vaciar rows si P360/localhost:1512 no confirmó la escritura.
			log("Write failed; keeping " + rowCount + " pending rows for " + entity + " to retry on the next watcher cycle.");
			logE(e);
			return false;
		}
	}
	
	private void addCurrentStatus(String externalId, String currentStatus) {
		org.json.JSONArray rows = reqCurrentStatus.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(currentStatus)));
		if(rows.length() == bs) {
			sendData("Product2G", reqCurrentStatus);
		}
	}
	
	private void addPrevStatus(String externalId, String prevStatus) {
		org.json.JSONArray rows = reqPrevStatus.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(prevStatus)));
		if(rows.length() == bs) {
			sendData("Product2G", reqPrevStatus);
		}
	}

	private void addExternalStatus(String externalId, String externalStatus) {
		org.json.JSONArray rows = reqExternalStatus.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(externalStatus)));
		if(rows.length() == bs) {
			sendData("Product2G", reqExternalStatus);
		}
	}

	private void addCurrentStatusA(String externalId, String currentStatus) {
		org.json.JSONArray rows = reqCurrentStatusA.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(currentStatus)));
		if(rows.length() == bs) {
			sendData("Article", reqCurrentStatusA);
		}
	}
	
	private void addPrevStatusA(String externalId, String prevStatus) {
		org.json.JSONArray rows = reqPrevStatusA.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(prevStatus)));
		if(rows.length() == bs) {
			sendData("Article", reqPrevStatusA);
		}
	}
	
	private void addExternalStatusA(String externalId, String externalStatus) {
		org.json.JSONArray rows = reqExternalStatusA.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(externalStatus)));
		if(rows.length() == bs) {
			sendData("Article", reqExternalStatusA);
		}
	}
	
	private void addProcedeNoProcede(String externalId, Boolean procedeNoProcede) {
		org.json.JSONArray rows = reqProcedeNoProcede.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(procedeNoProcede)));
		if(rows.length() == bs) {
			sendData("Article", reqProcedeNoProcede);
		}
	}
	
	private void addEnriquecidoEnForo(String externalId, Boolean enriquecidoEnForo) {
		org.json.JSONArray rows = reqEnriquecidoEnForo.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(enriquecidoEnForo)));
		log("Added a value on enriquecidoEnForo for " + externalId + " (" + enriquecidoEnForo + ")");
		if(rows.length() == bs) {
			sendData("Product2G", reqEnriquecidoEnForo);
		}
	}
	
	private void addLastSentToMarketplace(String externalId, String lastSentToMarketplace) {
		org.json.JSONArray rows = reqLastSentToMarketplace.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(lastSentToMarketplace)));
		if(rows.length() == bs) {
			sendData("Product2G", reqLastSentToMarketplace);
		}
	}
	
	private void addPublishMktMessage(String externalId, String publishMktMessage) {
		org.json.JSONArray rows = reqPublishMktMessage.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(publishMktMessage)));
		if(rows.length() == bs) {
			sendData("Product2G", reqPublishMktMessage);
		}
	}
	
	private void addFechaUltimaPublicacion(String externalId, String fechaUltimaPublicacion) {
		org.json.JSONArray rows = reqFechaUltimaPublicacion.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(fechaUltimaPublicacion)));
		if(rows.length() == bs) {
			sendData("Product2G", reqFechaUltimaPublicacion);
		}
	}
	
	private void addFirstApprovedDate(String externalId, String firstApprovedDate) {
		org.json.JSONArray rows = reqFirstApprovedDate.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(firstApprovedDate)));
		if(rows.length() == bs) {
			sendData("Product2G", reqFirstApprovedDate);
		}
	}
	
	private void addFirstDateApproved(String externalId, String firstDateApproved) {
		org.json.JSONArray rows = reqFirstDateApproved.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(firstDateApproved)));
		if(rows.length() == bs) {
			sendData("Product2G", reqFirstDateApproved);
		}
	}
	
	private void addLastDateApprove(String externalId, String lastDateApprove) {
		org.json.JSONArray rows = reqLastDateApproved.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(lastDateApprove)));
		if(rows.length() == bs) {
			sendData("Product2G", reqLastDateApproved);
		}
	}
	
	private void addPublishMessage(String externalId, String publishMessage) {
		org.json.JSONArray rows = reqPublishMessage.getJSONArray("rows");
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(publishMessage)));
		if(rows.length() == bs) {
			sendData("Product2G", reqPublishMessage);
		}
	}
	
	private void revisaTieneImagen(String externalId, org.json.JSONObject data){
		String resp = dr.getArticleData(new org.json.JSONArray().put(externalId));
		if(resp != null)
		try {
			org.json.JSONObject jr = new org.json.JSONObject(resp);
			org.json.JSONArray items = jr.getJSONArray("items");
			org.json.JSONObject ad = items.getJSONObject(0);
			if(!"".equals(ad.getString("ProductImage"))) {
				addCurrentStatus(externalId, "1022");
			}
		}catch(org.json.JSONException e) {
			logE(e);
		}
		/*
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
		qp.put("products", "'" + externalId + "'@1");
		qp.put("pageSize", "1000");
		boolean[] anyImage = new boolean[1];
		anyImage[0] = false;
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			if(!"".equals(row.getJSONArray("values").getJSONArray(0).getString(0))) {
				anyImage[0] = true;
			}
		});
		if(anyImage[0]) {
			data.put("currentStatus", "Revisión QA");
		}
		*/
	}
	
	private java.util.LinkedList<String> variantsOfTheProduct(String externalId) {
		java.util.LinkedList<String> hol = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "Article.SupplierAID"
			);
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + externalId + "\") = \"" + externalId + "\"");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
			if(response == null) {
				log("Error on getting variants of the product (" + externalId + "): " + workshop.getRawResponse());
			}else {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int iu=0; iu < rows.length(); iu++) {
					values = rows.getJSONObject(iu).getJSONArray("values");
					hol.addLast(values.getString(0));
				}
				a += response.getInt("pageSize");
			}
		}while(a < b);
		a = 0;
		return hol;
	}
	
	
	private boolean wereYouInForo(String lastDateApproved, String lastStatusChangeRaw) {
		if(lastStatusChangeRaw == null || "".equals(lastStatusChangeRaw))
			return false;
		String[] records = lastStatusChangeRaw.split("\\r\\n");
		if(lastDateApproved == null || "".equals(lastDateApproved)) {
			java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
			java.util.regex.Matcher m = null;
			String datePart = null;
			java.util.Date currentLogDate = null;
			String prevStatus = null;
			String currentStatus = null;
			java.util.regex.Pattern statusPattern = java.util.regex.Pattern.compile("(?<=\")(.+)(?=\")");
			java.util.regex.Matcher mSP = null;
			String[][] tuplas = collectStatusInformation();
			java.util.Map<String, String> espMap = fromTuples(tuplas, 1);
			java.util.Map<String, String> engMap = fromTuples(tuplas, 2);
			log("Spanish map: " + espMap);
			log("English map: " + engMap);
			for(int i=0; i<records.length; i++) {
				 m = datePattern.matcher(records[i]);
				 if(m.find()) {
					 try {
						 datePart = m.group();
						 if(records[i].startsWith("El usuario")) {
							 currentLogDate = new java.text.SimpleDateFormat("dd/MM/yyyy").parse(datePart);
							 log("Parsed date from Spanish message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else if(records[i].startsWith("The user")) {
							 currentLogDate = new java.text.SimpleDateFormat("MM/dd/yyyy").parse(datePart);
							 log("Parsed date from English message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else {
							 currentLogDate = null;
						 }
						 if(currentLogDate != null) {
							 mSP = statusPattern.matcher(records[i]);
							 if(mSP.find()) {
								 if(records[i].startsWith("El usuario")) {
									 currentStatus = espMap.get(mSP.group());
									 log("Parsed status from Spanish message: " + currentStatus + " ( from: " + mSP.group() + ")" );
								 }else if(records[i].startsWith("The user")) {
									 currentStatus = engMap.get(mSP.group());
									 log("Parsed status from English message: " + currentStatus + " ( from: " + mSP.group() + ")" );
								 }else {
									 currentStatus = null;
								 }
								 if(currentStatus != null) {
									 if(prevStatus != null && "1022".equals(prevStatus) && ("1026".equals(currentStatus) /* || "1002".equals(currentStatus) */)) {
										 log("Found a transition from Foro Process to QA within last approved time frame.");
										 return true;
									 }
									 prevStatus = currentStatus;
								 }
							 }
						 }
					 }catch(java.text.ParseException e) {
						 logE(e);
					 }
				 }
			 }
		}else {
			java.util.Date lastDateApprovedDate = null;
			java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
			java.util.regex.Matcher m = null;
			String datePart = null;
			java.util.Date currentLogDate = null;
			String prevStatus = null;
			String currentStatus = null;
			java.util.regex.Pattern statusPattern = java.util.regex.Pattern.compile("(?<=\")(.+)(?=\")");
			java.util.regex.Matcher mSP = null;
			String[][] tuplas = collectStatusInformation();
			java.util.Map<String, String> espMap = fromTuples(tuplas, 1);
			java.util.Map<String, String> engMap = fromTuples(tuplas, 2);
			try{
				 lastDateApprovedDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse( lastDateApproved.replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.") );
				 log("Got last date approved: " + lastDateApproved + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(lastDateApprovedDate) + ")");
				 for(int i=0; i<records.length; i++) {
					 m = datePattern.matcher(records[i]);
					 if(m.find()) {
						 datePart = m.group();
						 if(records[i].startsWith("El usuario")) {
							 currentLogDate = new java.text.SimpleDateFormat("dd/MM/yyyy").parse(datePart);
							 log("Parsed date from Spanish message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else if(records[i].startsWith("The user")) {
							 currentLogDate = new java.text.SimpleDateFormat("MM/dd/yyyy").parse(datePart);
							 log("Parsed date from English message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else {
							 currentLogDate = null;
						 }
						 if(currentLogDate != null) {
							 log("Comparing dates (currentLogDate vs lastDateApproved, " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + " vs " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(lastDateApprovedDate) + "): " + currentLogDate.compareTo(lastDateApprovedDate));
							 if(currentLogDate.compareTo(lastDateApprovedDate) < 0) {
								 break;
							 }else {
								 mSP = statusPattern.matcher(records[i]);
								 if(mSP.find()) {
									 if(records[i].startsWith("El usuario")) {
										 currentStatus = espMap.get(mSP.group());
										 log("Parsed status from Spanish message: " + currentStatus + " ( from: " + mSP.group() + ")" );
									 }else if(records[i].startsWith("The user")) {
										 currentStatus = engMap.get(mSP.group());
										 log("Parsed status from English message: " + currentStatus + " ( from: " + mSP.group() + ")" );
									 }else {
										 currentStatus = null;
									 }
									 if(currentStatus != null) {
										 if(prevStatus != null && "1022".equals(prevStatus) && "1026".equals(currentStatus)) {
											 log("Found a transition from Foro Process to QA within last approved time frame.");
											 return true;
										 }
										 prevStatus = currentStatus;
									 }
								 }
							 }
						 }
					 }
				 }
			}catch(java.text.ParseException e) {
				logE(e);
			}
		}
		return false;
	}
	
	private java.util.Map<String, String> fromTuples(String[][] tuplas, int index){
		java.util.Map<String, String> map = new java.util.TreeMap<>();
		if(tuplas != null && tuplas.length > 0) {
			if(index > 0 && index < tuplas[0].length) {
				for(int i=0; i<tuplas.length; i++) {
					map.put(tuplas[i][index], tuplas[i][0]);
				}
			}
		}
		return map;
	}
	
	private String[][] collectStatusInformation(){
		RESTWorkshop rw = workshop;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("GET", "/enum/Enum.ProductStatus", qp, null);
		java.util.Map<String, String> esp = new java.util.TreeMap<>();
		if(response != null) {
			org.json.JSONArray entries = response.getJSONArray("entries");
			for(int i=0; i<entries.length(); i++) {
				esp.put(entries.getJSONObject(i).getString("key"), entries.getJSONObject(i).getString("label"));
			}
		}
		rw.getRc().getHeader().put("Accept-Language", "en");
		response = rw.makeRequest("GET", "/enum/Enum.ProductStatus", qp, null);
		java.util.Map<String, String> eng = new java.util.TreeMap<>();
		if(response != null) {
			org.json.JSONArray entries = response.getJSONArray("entries");
			for(int i=0; i<entries.length(); i++) {
				eng.put(entries.getJSONObject(i).getString("key"), entries.getJSONObject(i).getString("label"));
			}
		}
		java.util.LinkedList<String[]> tuplas = new java.util.LinkedList<>();
		for(java.util.Map.Entry<String, String> entry : esp.entrySet()) {
			tuplas.addLast(new String[] {entry.getKey(), entry.getValue(), eng.get(entry.getKey())});
		}
		return tuplas.toArray(new String[][] {});
	}
	
	private void dejaWorkflow(String internalId, String processId, String workflowId, String status) {
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONArray itemIds = new org.json.JSONArray();
		org.json.JSONObject response = null;
		itemIds.put(internalId);
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("POST", "/manage/workflow/status/leave", qp, rb.toString());
		log("From leaving wf (" + workflowId + "): " + (response == null ? "ERR: " + workshop.getRawResponse() : response.toString() ));
	}

	private void ingresaWorkflow(String internalId, String processId, String workflowId, String status) {
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONArray itemIds = new org.json.JSONArray();
		org.json.JSONObject response = null;
		itemIds.put(internalId);
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("POST", "/manage/workflow/status/enter", qp, rb.toString());
		log(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	}

	private void setExternalStatus(
			String externalId, 
			String currentStatus, 
			String externalStatus, 
			String currentStatusOld
	) {
		if(externalStatus == null) {
			return;
		}
		log("Checking to update---");
		if( /* java.util.Arrays.binarySearch(losIDs, externalId) < 0 */ !java.nio.file.Files.exists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.migration.to_skip_directory"), externalId)) ) {
			addExternalStatus(externalId, externalStatus);
			addPrevStatus(externalId, currentStatusOld);
		}
		log("Writing to file for further variant updates... " + rw.getRw().serializeChunk( new Object[] {externalId, currentStatus, externalStatus, currentStatusOld}) );
		log("Completed.");
		replicateToVariants(externalId, currentStatus, externalStatus, currentStatusOld);
		
	}

	private void replicateToVariants(
			String externalId, 
			String currentStatus, 
			String externalStatus,
			String currentStatusOld
	) {
		java.util.Set<String> variantIds = dr.getVariants(externalId);
		for(String variantId : variantIds) {
			addCurrentStatusA(variantId, currentStatus);
			addExternalStatusA(variantId, externalStatus);
			addPrevStatusA(variantId, currentStatusOld);
		}

	}

	private String getExternalStatus(String currentStatus) {
		String externalStatusCode = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", "ExternalStatus").toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(currentStatus.equals(pieces[0])) {
					externalStatusCode = pieces[1];
					break;
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		
		if(externalStatusCode == null) {
			log("Problem when querying ExternalStatus: " + workshop.getRawResponse());
		}else {
			log( "Returning: " + externalStatusCode );
			String lbl = getLookupCodeName(externalStatusCode, "ExternalStatus");
			log("Now got: " + lbl);
			return lbl;
		}
		return null;
	}
	
	private String getExternalStatusCode(String currentStatus) {
		String externalStatusCode = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", "ExternalStatus").toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(currentStatus.equals(pieces[0])) {
					externalStatusCode = pieces[1];
					break;
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		
		if(externalStatusCode == null) {
			log("Problem when querying ExternalStatus: " + workshop.getRawResponse());
		}else {
			log( "Returning: " + externalStatusCode );
			return externalStatusCode;
		}
		return null;
	}

	private String getLookupCodeName(String code, String lookup) {

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "global_lookups", lookup).toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(code.equals(pieces[0])) {
					return pieces[1];
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return null;
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("lookup", lookup);
//		qp.put("fields", "LookupValueLang.Name(es)");
//		qp.put("query", "LookupValue.IsActive = true and LookupValue.Code equals \"" + code + "\"");
//
//		org.json.JSONObject response = null;
//
//		response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//		if(response == null) {
//			log("Problem when querying ExternalStatus: " + workshop.getRawResponse());
//		}else {
//			log( "Returning: " + (
//					response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : "" ) );
//			return response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : "";
//		}
//		return null;

	}

	public void setProcedeNoProcede(String externalId) {
		org.json.JSONArray variants = new org.json.JSONArray();
		dr.getVariants(externalId).forEach(variants::put);
		String resp = dr.getArticleData(variants);
		if(resp != null) {
			try {
				org.json.JSONObject jr = new org.json.JSONObject(resp);
				org.json.JSONArray items = jr.getJSONArray("items");
				org.json.JSONObject item = null;
				for(int i=0; i<items.length(); i++) {
					item = items.getJSONObject(i);
					addProcedeNoProcede(item.getString("variant"), !"".equals(item.getString("ProductImage")));
				}
			}catch(org.json.JSONException e) {
				logE(e);
			}
		}
//		java.util.Map<String, String> empty = new java.util.TreeMap<>();
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields",
//				"Article.SupplierAID"
//				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
//		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + externalId + "\") = \"" + externalId + "\"");
//		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		org.json.JSONArray charValue = null;
//		String articleId = null;
//		if(response != null) {
//			rows = response.getJSONArray("rows");
//			for(int i=0; i<rows.length(); i++) {
//				values = rows.getJSONObject(i).getJSONArray("values");
//				charValue = values.getJSONArray(1);
//				articleId = values.getString(0);
//				response = workshop.makeRequest("PUT", "/object/Article/'" + articleId + "'@'MASTER'", empty, new org.json.JSONObject()
//						.put("_characteristicRecords", new org.json.JSONArray()
//								.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", "ProcedeNoProcede")))
//										.put("_recordLang", new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
//												.put("values", new org.json.JSONArray().put(!"".equals(charValue.getString(0))))))).toString());
//				if(response != null) {
//					log("Put ProcedeNoProcede for: " + articleId);
//				}else {
//					log("Error while updating \"ProcedeNoProcede\" value. " + workshop.getRawResponse());
//				}
//			}
//		}
	}

	public void setProcedeNoProcedeArticle(String externalId) {
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)");
		qp.put("query", "Article.SupplierAID equals \"" + externalId + "\"");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray charValue = null;
		String articleId = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				charValue = values.getJSONArray(1);
				articleId = values.getString(0);
				response = workshop.makeRequest("PUT", "/object/Article/'" + articleId + "'@'MASTER'", empty, new org.json.JSONObject()
						.put("_characteristicRecords", new org.json.JSONArray()
								.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", "ProcedeNoProcede")))
										.put("_recordLang", new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
												.put("values", new org.json.JSONArray().put(!"".equals(charValue.getString(0))))))).toString());
				if(response != null) {
					log("Put ProcedeNoProcede for: " + articleId);
				}else {
					log("Error while updating \"ProcedeNoProcede\" value. " + workshop.getRawResponse());
				}
			}
		}
	}
	
//	private void enviaForo40(SftpClient sftpCli, String sku, String business, String fotoTomadaLiverpool, String saity, String sistemaorigen) {
//		if(sftpCli != null) {
//			log("Evaluating: " + sku + ", " + business + ", " + fotoTomadaLiverpool + ", " + saity + ", " + sistemaorigen);
//			if("LVP".equals(business) && "Y".equals(fotoTomadaLiverpool))
//				try {
//					writeToSftp(sku, sftpCli,  "Código SKU|ESTADO|ORIGEN\n" + sku + "|" + 3 + "|" + sistemaorigen, "/interfase/mer/in/pedforo40");
//				} catch (java.io.IOException e) {
//					logE(e);
//				}
//		}
//	}

	private void complementaRechazos(org.json.JSONArray rechazos, String internalId, String rejectionInfo, String entity, String externalId) {
		String characteristicIdentifier = null;
		String recordKey = null;
		org.json.JSONObject rechazo = null;
		org.json.JSONArray children = null;
		String aux = null;
		for(int i=0; i<rechazos.length(); i++) {
			rechazo = rechazos.getJSONObject(i);
			characteristicIdentifier = rechazo.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code")
					.replaceAll("(_Rechazo)$", "")
					.replaceAll("(_Rejection)$", "")
					;
			if(
				characteristicIdentifier.equals("ProductImage") ||
				characteristicIdentifier.equals("ProductImageDetail") ||
				characteristicIdentifier.equals("ProductImageSmosh") ||
				characteristicIdentifier.equals("Illustration")
			) {
				recordKey = rechazo.getJSONObject("_qualification").getString("recordKey");
				org.json.JSONArray children0 = rechazo.has("_children") ? 
						rechazo.getJSONArray("_children") : 
							null;
				if(children0 != null) {
					for (int k=0; k<children0.length(); k++) {
						if(children0.getJSONObject(k).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").endsWith("_Rejection")) {
							recordKey = children0.getJSONObject(k).getJSONObject("_qualification").getString("recordKey");
							children = children0.getJSONObject(k).has("_children") ? 
									children0.getJSONObject(k).getJSONArray("_children") : 
										null;
							if(children != null) {
								children.put(new org.json.JSONObject()
										.put("_qualification", new org.json.JSONObject()
												.put("recordKey", recordKey)
												.put("characteristic", new org.json.JSONObject()
														.put("_code", "rem_" + characteristicIdentifier)))
										.put("_recordLang", new org.json.JSONArray()
												.put(new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject()
																.put("language", new org.json.JSONObject()
																		.put("_code", "zxx")))
														.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "CS01")))))
										);
								children.put(new org.json.JSONObject()
										.put("_qualification", new org.json.JSONObject()
												.put("recordKey", recordKey)
												.put("characteristic", new org.json.JSONObject()
														.put("_code", "rmum_" + characteristicIdentifier)))
										.put("_recordLang", new org.json.JSONArray()
												.put(new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject()
																.put("language", new org.json.JSONObject()
																		.put("_code", "zxx")))
														.put("values", new org.json.JSONArray().put(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date())))))
										);
								aux = "QAToPurchase".equals(rejectionInfo) ? "TG01" : 
									  "QAToProvider".equals(rejectionInfo) ? "TG02" : 
									  "QAToForo".equals(rejectionInfo) ? "TG09" : 
									  "";
								log("RejectionInfo: " + rejectionInfo + " - " + aux + " (For Rol Destino)");
								if(!"".equals(aux)) {
									children.put(new org.json.JSONObject()
											.put("_qualification", new org.json.JSONObject()
													.put("recordKey", recordKey)
													.put("characteristic", new org.json.JSONObject()
															.put("_code", "rrd_" + characteristicIdentifier)))
											.put("_recordLang", new org.json.JSONArray()
													.put(new org.json.JSONObject()
															.put("_qualification", new org.json.JSONObject()
																	.put("language", new org.json.JSONObject()
																			.put("_code", "zxx")))
															.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", aux)))))
											);
								}
								aux = "QAToPurchase".equals(rejectionInfo) ? "TG03" : 
									  "QAToProvider".equals(rejectionInfo) ? "TG03" : 
									  "GDToPurchase".equals(rejectionInfo) ? "TG04" : 
									  "QAToForo".equals(rejectionInfo) ? "TG03" : 
									  ""
									;
								log("RejectionInfo: " + rejectionInfo + " - " + aux + " (For Rol Emisor)");
								if(!"".equals(aux)) {
									children.put(new org.json.JSONObject()
											.put("_qualification", new org.json.JSONObject()
													.put("recordKey", recordKey)
													.put("characteristic", new org.json.JSONObject()
															.put("_code", "rre_" + characteristicIdentifier)))
											.put("_recordLang", new org.json.JSONArray()
													.put(new org.json.JSONObject()
															.put("_qualification", new org.json.JSONObject()
																	.put("language", new org.json.JSONObject()
																			.put("_code", "zxx")))
															.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", aux)))))
											);
								} 
								
								if(!"Comentario".equals(characteristicIdentifier)) {
										children.put(new org.json.JSONObject()
											.put("_qualification", new org.json.JSONObject()
													.put("recordKey", recordKey)
													.put("characteristic", new org.json.JSONObject()
															.put("_code", "rma_" + characteristicIdentifier)))
											.put("_recordLang", new org.json.JSONArray()
													.put(new org.json.JSONObject()
															.put("_qualification", new org.json.JSONObject()
																	.put("language", new org.json.JSONObject()
																			.put("_code", "zxx")))
															.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "Campo rechazado")))))
											);
								}
							}
						}
					}
				}else {
					log("No children.");
				}
			}else {
				recordKey = rechazo.getJSONObject("_qualification").getString("recordKey");
				children = rechazo.has("_children") ? 
						rechazo.getJSONArray("_children") : 
							null;
				if(children != null) {
					children.put(new org.json.JSONObject()
							.put("_qualification", new org.json.JSONObject()
									.put("recordKey", recordKey)
									.put("characteristic", new org.json.JSONObject()
											.put("_code", "rem_" + characteristicIdentifier)))
							.put("_recordLang", new org.json.JSONArray()
									.put(new org.json.JSONObject()
											.put("_qualification", new org.json.JSONObject()
													.put("language", new org.json.JSONObject()
															.put("_code", "zxx")))
											.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "CS01")))))
							);
					children.put(new org.json.JSONObject()
							.put("_qualification", new org.json.JSONObject()
									.put("recordKey", recordKey)
									.put("characteristic", new org.json.JSONObject()
											.put("_code", "rmum_" + characteristicIdentifier)))
							.put("_recordLang", new org.json.JSONArray()
									.put(new org.json.JSONObject()
											.put("_qualification", new org.json.JSONObject()
													.put("language", new org.json.JSONObject()
															.put("_code", "zxx")))
											.put("values", new org.json.JSONArray().put(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date())))))
							);
					aux = "QAToPurchase".equals(rejectionInfo) ? "TG01" : 
						  "QAToProvider".equals(rejectionInfo) ? "TG02" : 
						  "QAToForo".equals(rejectionInfo) ? "TG09" : 
						  "";
					log("RejectionInfo: " + rejectionInfo + " - " + aux + " (For Rol Destino)");
					if(!"".equals(aux)) {
						children.put(new org.json.JSONObject()
								.put("_qualification", new org.json.JSONObject()
										.put("recordKey", recordKey)
										.put("characteristic", new org.json.JSONObject()
												.put("_code", "rrd_" + characteristicIdentifier)))
								.put("_recordLang", new org.json.JSONArray()
										.put(new org.json.JSONObject()
												.put("_qualification", new org.json.JSONObject()
														.put("language", new org.json.JSONObject()
																.put("_code", "zxx")))
												.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", aux)))))
								);
					}
					aux = "QAToPurchase".equals(rejectionInfo) ? "TG03" : 
						  "QAToProvider".equals(rejectionInfo) ? "TG03" : 
						  "GDToPurchase".equals(rejectionInfo) ? "TG04" : 
						  "QAToForo".equals(rejectionInfo) ? "TG03" : 
						  ""
						;
					log("RejectionInfo: " + rejectionInfo + " - " + aux + " (For Rol Emisor)");
					if(!"".equals(aux)) {
						children.put(new org.json.JSONObject()
								.put("_qualification", new org.json.JSONObject()
										.put("recordKey", recordKey)
										.put("characteristic", new org.json.JSONObject()
												.put("_code", "rre_" + characteristicIdentifier)))
								.put("_recordLang", new org.json.JSONArray()
										.put(new org.json.JSONObject()
												.put("_qualification", new org.json.JSONObject()
														.put("language", new org.json.JSONObject()
																.put("_code", "zxx")))
												.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", aux)))))
								);
					} 
					log("Loschildren: " + children);
					if(!"Comentario".equals(characteristicIdentifier)) {
							children.put(new org.json.JSONObject()
								.put("_qualification", new org.json.JSONObject()
										.put("recordKey", recordKey)
										.put("characteristic", new org.json.JSONObject()
												.put("_code", "rma_" + characteristicIdentifier)))
								.put("_recordLang", new org.json.JSONArray()
										.put(new org.json.JSONObject()
												.put("_qualification", new org.json.JSONObject()
														.put("language", new org.json.JSONObject()
																.put("_code", "zxx")))
												.put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "Campo rechazado")))))
								);
					}
				}else {
					log("No children.");
				}
			}
		}
		log("Loschildren: " + children);
		org.json.JSONObject request = new org.json.JSONObject().put("_characteristicRecords", rechazos).put("rejectionInfo", "");
		log("Sending this payload: " + request);
//		org.json.JSONObject response = 
				workshop.makeRequest("PUT", "/object/" + entity + "/" + ( internalId != null ? internalId : "'" + externalId + "'@1" ), 
						new java.util.TreeMap<>(), request.toString());
		log("Completado el complemento a rechazos. " + workshop.getRawResponse());
		if(workshop.getException() != null) {
			logE(workshop.getException());
		}
		if("Product2G".equals(entity) && externalId != null) {
			java.util.LinkedList<String> variantes = variantsOfTheProduct(externalId);
			log("Variantes de la propuesta: " + variantes.size());
			org.json.JSONObject data = null;
			org.json.JSONArray characteristicRecords = null;
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONArray rechs = null;
			for(String variant : variantes) {
				data = workshop.makeRequest("GET", "/object/Article/'" + variant + "'@1", qp, null);
				if(data != null && data.has("_data")) {
					data = data.getJSONObject("_data");
					characteristicRecords = data.has("_characteristicRecords") ? data.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
					rechs = new org.json.JSONArray();
					log("data id: " + characteristicRecords);
					characteristicsToMap(characteristicRecords, new java.util.TreeMap<>(), rechs);
					log("Le vamos a echar los rechazos a la variante: " + variant + " del producto: " + internalId);
					complementaRechazos(rechs, null, rejectionInfo, "Article", variant);
				}
			}
		}
	}

	private void characteristicsToMap(org.json.JSONArray characteristicRecords, 
			java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap, 
			org.json.JSONArray rechazos){
		java.util.LinkedList<org.json.JSONObject> lst = null;
		org.json.JSONObject characteristicRecord = null;
		String characteristicIdentifier = null;
		org.json.JSONArray children = null;
		org.json.JSONObject child = null;
		org.json.JSONArray losChildren = null;
		org.json.JSONObject losChild = null;
		boolean notEnough = true;
		boolean losNotEnough = true;
		for(int i=0; i<characteristicRecords.length(); i++) {
			characteristicRecord = characteristicRecords.getJSONObject(i);
			characteristicIdentifier = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = characteristicRecordsMap.get(characteristicIdentifier);
			if(lst == null) {
				lst = new java.util.LinkedList<>();
				characteristicRecordsMap.put(characteristicIdentifier, lst);
			}
			lst.addLast(characteristicRecord);
			if(
				characteristicIdentifier.endsWith("_Rechazo") || 
				characteristicIdentifier.equals("Comentario") ||
				characteristicIdentifier.equals("ProductImage") ||
				characteristicIdentifier.equals("ProductImageDetail") ||
				characteristicIdentifier.equals("ProductImageSmosh") ||
				characteristicIdentifier.equals("Illustration")
			) {
				log("Entering to: " + characteristicIdentifier);
				children = characteristicRecord.has("_children") ? characteristicRecord.getJSONArray("_children") : null;
				if(children != null) {
					for(int j=0; j<children.length(); j++) {
						child = children.getJSONObject(j);
						log("\tVisiting: " + child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
						if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").startsWith("rre_")) {
							notEnough = false;
						}else {
							if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").endsWith("_Rejection")) {
								if(child.has("_children")) {
									losChildren = child.getJSONArray("_children");
									for(int k=0; k<losChildren.length(); k++) {
										losChild = losChildren.getJSONObject(k);
										if(losChild.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").startsWith("rre_")) {
											losNotEnough = false;
										}
									}
								}else {
									rechazos.put(characteristicRecord);
									log("Agregamos un kissi: " + characteristicIdentifier);
									log("El kissi: " + child);
								}
								if(losNotEnough) {
									rechazos.put(characteristicRecord);
									log("2Agregamos un kissi: " + characteristicIdentifier);
									log("2El kissi: " + child);
								}
								losNotEnough = true;
								continue;
							}
						}
					}
				}else {
					if(characteristicIdentifier.endsWith("_Rechazo"))
						rechazos.put(characteristicRecord);
				}
				if(notEnough && characteristicIdentifier.endsWith("_Rechazo")) {
					rechazos.put(characteristicRecord);
				}
				if(characteristicIdentifier.equals("Comentario"))
					rechazos.put(characteristicRecord);
				log("R: " + rechazos);
				notEnough = true;
			}
		}
	}

	private void disconnect() {
		if(connection != null){
			try{
				connection.close();
			}catch(JMSException e){
				e.printStackTrace();
			}
		}
	}
	

	private static final java.nio.file.Path ATG_PENDING_IDS_FILE =
			java.nio.file.Paths.get("../logs/amqp/productArticleCharactChange/P360AMQ_ATG_PENDING_IDS.txt");

	private static synchronized void appendAtgPendingId(String externalId) {
		try {
			java.nio.file.Files.createDirectories(ATG_PENDING_IDS_FILE.getParent());

			java.nio.file.Files.write(
					ATG_PENDING_IDS_FILE,
					(externalId + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (java.io.IOException e) {
			LOGGER.log(Level.SEVERE, "No pude escribir ID pendiente de ATG: " + externalId, e);
		}
	}
	
	private static final java.nio.file.Path MKT_PENDING_IDS_FILE =
			java.nio.file.Paths.get("../logs/amqp/productArticleCharactChange/P360AMQ_MKT_PENDING_IDS.txt");

	private static synchronized void appendMktPendingId(String externalId) {
		try {
			java.nio.file.Files.createDirectories(MKT_PENDING_IDS_FILE.getParent());

			java.nio.file.Files.write(
					MKT_PENDING_IDS_FILE,
					(externalId + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (java.io.IOException e) {
			LOGGER.log(Level.SEVERE, "No pude escribir ID pendiente de MKT: " + externalId, e);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(P360ActiveMQBPMStage.class.getName());
	
    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp/main/activeMQListener-%g.log", 25 * 1024 * 1024, 10, true);
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
//        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//                new java.io.FileOutputStream("../logs/activeMQListener.log", true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "]  " + message);
//        } catch (java.io.IOException e) {
//        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/activeMQListener.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

	public static void main(String[] args) throws ServiceUnavailableException {
		try(P360ActiveMQBPMStage o = new P360ActiveMQBPMStage(PropertiesManager.get("p360.contingency.base_url", "http://172.18.237.162:1512/rest/V2.0"))){
			o.launchListenerThread();
			o.launchTimeSenderThread();
			Runtime.getRuntime().addShutdownHook(o);
			o.process(args[0], args[1], args[2]);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		dastub.close();
		this.ccp.close();
		this.ladp.close();
		this.pacvcp.close();
	}

}
