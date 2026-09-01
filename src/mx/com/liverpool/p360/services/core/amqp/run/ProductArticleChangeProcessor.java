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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ProductArticleChangeProcessor {

	private boolean running = true;
	
	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;
	
	public ProductArticleChangeProcessor() {
	}
	
	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException {

		String entity = null;
		String changeSummary = null;

		org.json.JSONObject json = new org.json.JSONObject(message);
		String externalId = null;
    	
		if(json.has("entityItemChange")) {
	     	entity = json.getJSONObject("entityItemChange").getString("_entity");
	     	externalId = json.getJSONObject("entityItemChange").getString("_identifier");
	     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
			log("A message body: " + json);
	     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	Document doc;
	    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
	    		doc = builder.parse( baos );
	    		doc.getDocumentElement().normalize();
	    	}
			if(doc != null) {
				if( "Product2G".equals(entity) || "Article".equals(entity) ) {
					Element rootElement = doc.getDocumentElement();
					XMLMisc xmm = new XMLMisc();
					try{
						Node _n1 = xmm.byName(rootElement, "Product2G".equals(entity) ? "product" : "article");
						if(_n1 != null) {
							Node _n2 = xmm.byName( _n1, "gtin");
							if(_n2 != null) {
								Node _oldGTINNode     = xmm.byName( _n2, "_old");
								Node _currentGTINNode = xmm.byName( _n2, "_current");
								if(_currentGTINNode == null && _oldGTINNode != null) {
									RESTWrapper rw0 = new RESTWrapper();
									rw0.getRw().setBaseUrl("https://pro-api.liverpool.com.mx/api/cataloging/productmanagement/proposals");
									java.util.Map<String, String> qp = new java.util.HashMap<>();
									rw0.getRw().addHeader("Content-Type", "application/json");
									rw0.getRw().addHeader("apikey", "66a831ee-d57d-49fa-a830-fa185323cb8f");
									rw0.getRw().removeHeader("Authorization");
									rw0.getRw().removeHeader("Accept");
									log("Resp from del method (dropping ean: " + _oldGTINNode.getTextContent() + ", from PID: " + externalId + "): " + rw0.getRw().makeRequest("DELETE", "/upc-eans", qp, new org.json.JSONObject().put("upcEans", new org.json.JSONArray().put(_oldGTINNode.getTextContent())).toString()));
								}
							}
						}
					}catch(NullPointerException e) {
					}
				}
			}
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
                new java.io.FileOutputStream("../logs/PACactiveMQListener.err", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

}
