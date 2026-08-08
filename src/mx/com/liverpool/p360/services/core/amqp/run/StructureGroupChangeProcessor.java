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
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.temp.structuregroups.SendStructureGroupToPubSub;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class StructureGroupChangeProcessor {

	private boolean running = true;
	
	private final RESTWrapper rw;

	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;
	
	public StructureGroupChangeProcessor() throws ServiceUnavailableException {
		rw = new RESTWrapper();
	}

	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException {
		String entity = null;
		String changeSummary = null;
		org.json.JSONArray changedField = null;
		java.util.Set<String> changedFieldSet = new java.util.TreeSet<>();
		org.json.JSONObject json = new org.json.JSONObject(message);
		if(json.has("entityItemChange")) {
	     	entity = json.getJSONObject("entityItemChange").getString("_entity");
	     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
	     	changedField = json.getJSONObject("entityItemChange").has("_changedField") ? json.getJSONObject("entityItemChange").getJSONArray("_changedField") : new org.json.JSONArray();
			String structureExternalId = json.getJSONObject("entityItemChange").getJSONObject("_container").getString("_externalId");
	     	String internalId = json.getJSONObject("entityItemChange").getJSONObject("_entityItem").getString("_internalId");
	     	String externalId = json.getJSONObject("entityItemChange").getJSONObject("_entityItem").getString("_externalId");
			log("A message body: " + json);
			for(int i=0; i<changedField.length(); i++) {
				changedFieldSet.add(changedField.getString(i));
			}
			log("changeFields: " + changedField);
	     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder builder = factory.newDocumentBuilder();
	    	Document doc;
	    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
	    		doc = builder.parse( baos );
	    		doc.getDocumentElement().normalize();
	    	}
			if(doc != null) {
				Element rootElement = doc.getDocumentElement();
				if("StructureGroup".equals(entity)){ // catst
					if("'Sitios Web'".equals(structureExternalId)) {
						if(externalId.startsWith("'StructureGroup_")) {
							String nid = "catst" + externalId.replaceFirst("'StructureGroup_", "").substring(7).replaceAll("'.+", "");
							rw.writeData(
									  "list"
									, "StructureGroup"
									, null
									, new java.util.HashMap<>()
									, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.Identifier"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId)).put("values", new org.json.JSONArray().put( nid ))))
									, this::log
							);
						}
					}else if("'PrimaryProductTaxonomy'".equals(structureExternalId)) {
						SendStructureGroupToPubSub s = new SendStructureGroupToPubSub();
						s.sendDataToPubSub(json.getJSONObject("entityItemChange").getString("_identifier"), "idmc_put_template");
						log("Sent " + json.getJSONObject("entityItemChange").getString("_identifier") + " to pubSub: idmc:put_template");
						if(externalId.startsWith("'StructureGroup_")) {
							XMLMisc xmm = rw.getXmm();
							String level = null;
							try{
								level = xmm.byName( xmm.byName( xmm.byName(rootElement, "structureGroup"), "level"), "_current") .getTextContent();
							}catch(NullPointerException ignore) {}
							if(level != null) {
								int lvl = Integer.parseInt(level);
								String nid = ("EU" + (lvl + 1) + "-") + externalId.replaceFirst("'StructureGroup_", "").substring(7).replaceAll("'.+", "");
								externalId = nid;
								rw.writeData(
										  "list"
										, "StructureGroup"
										, null
										, new java.util.HashMap<>()
										, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.Identifier"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId)).put("values", new org.json.JSONArray().put( nid ))))
										, this::log
								);
							}
						}
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

	private static final Logger LOGGER = Logger.getLogger(StructureGroupChangeProcessor.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp/structureGroupChange/SGCactiveMQListener-%g.log", 25 * 1024 * 1024, 10, true);
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
//                new java.io.FileOutputStream("../logs/SGCactiveMQListener.log", true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "]  " + message);
//        } catch (java.io.IOException e) {
//        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/SGCactiveMQListener.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

}
