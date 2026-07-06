package mx.com.liverpool.p360.services.core.amqp.run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import javax.xml.parsers.ParserConfigurationException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.xml.sax.SAXException;

public class ProductArticleChangeProcessor {

	private boolean running = true;
	
//	private final RESTWrapper rw;
//	private final RESTWorkshop workshop;

	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;
	
	public ProductArticleChangeProcessor() {
//		rw = new RESTWrapper();
//		workshop = rw.getRw();
	}

	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException {

//		org.json.JSONObject response = null;
//		org.json.JSONObject data = null;
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();

//		String entity = null;
//		String internalId = null;
//		String changeSummary = null;

//		org.json.JSONArray changedField = null;

//		java.util.Set<String> changedFieldSet = new java.util.TreeSet<>();
		org.json.JSONObject json = new org.json.JSONObject(message);

//    	String identifier = null;
    	
		if(json.has("entityItemChange")) {
//	     	entity = json.getJSONObject("entityItemChange").getString("_entity");
//	     	internalId = json.getJSONObject("entityItemChange").getJSONObject("_entityItem").getString("_internalId");
//	     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
//	     	changedField = json.getJSONObject("entityItemChange").has("_changedField") ? json.getJSONObject("entityItemChange").getJSONArray("_changedField") : new org.json.JSONArray();
			log("A message body: " + json);
//			for(int i=0; i<changedField.length(); i++) {
//				changedFieldSet.add(changedField.getString(i));
//			}

//			log("changeFields: " + changedField);
//
//	     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//	    	DocumentBuilder builder = factory.newDocumentBuilder();
//	    	Document doc;
//
//	    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
//	    		doc = builder.parse( baos );
//	    		doc.getDocumentElement().normalize();
//	    	}
//	    	
//			if(doc != null) {
//
//				if("Product2G".equals(entity) || "Article".equals(entity)) {
//					qp.put("entityFilter", entity +"," + entity + "Log");
//					qp.put("includeIds", "true");
//					qp.put("includeLabels", "true");
//					log("requesting: " + workshop.getBaseUrl() + "/object/" + entity + "/" + internalId);
//					response = workshop.makeRequest("GET", "/object/" + entity + "/" + internalId , qp, null);
//					if(response != null) {
//						data = response.getJSONObject("_data");
//						identifier = data.getString("identifier");
//						try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.history_log") ).toFile(), true), java.nio.charset.StandardCharsets.UTF_8))){
//							pw.println( workshop.serializeChunk( new Object[] {identifier, changeSummary, data} ) );
//						}catch(java.io.IOException e) {
//							logE(e);
//						}
//					}
//				}
//			}
		}else if(json.has("entityItemsDeleted")){
			log("A message body: " + json);
		}
	}

	public void connect(String host, int port, String qName) {
		try{
			connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port + "?wireFormat.maxInactivityDuration=60000&keepAlive=true");
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        responseQueue = session.createQueue(qName);
	        consumer = session.createConsumer(responseQueue);
		}catch(JMSException e){
			e.printStackTrace();
		}
	}
	
	public void process() {
		try{
			log("Start listening for messages...");
			while(running){
				responseMessage = consumer.receive(30);
			    if (responseMessage != null && responseMessage instanceof TextMessage) {
			     	try{
			     		messageProcessor(((TextMessage) responseMessage).getText());
			     	}catch(org.json.JSONException e) {
			     		logE(e);
			     	}
			     	log("Doney");
				}
			}
		}catch(ParserConfigurationException | SAXException | java.io.IOException e){
			logE(e);
		}catch(org.json.JSONException e){
    		logE(e);
    	} catch (JMSException e) {
    		logE(e);
		}finally {
			disconnect();
		}
	}
	
	public void setRunning(boolean running) {
		this.running = running;
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

	private static final Logger LOGGER = Logger.getLogger(ProductArticleChangeProcessor.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp/productArticleChange/PACactiveMQListener-%g.log", 25 * 1024 * 1024, 10, true);
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
//		LOGGER.info(message);
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/PACactiveMQListener.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/PACactiveMQListener.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

}
