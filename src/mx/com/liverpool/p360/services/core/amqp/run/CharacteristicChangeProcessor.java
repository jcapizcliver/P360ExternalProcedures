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

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class CharacteristicChangeProcessor {

	private boolean running = true;

	private final RESTWrapper rw = new RESTWrapper();
	private final XMLMisc xmm = rw.getRw().getXmm();
	
	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;
	
	public CharacteristicChangeProcessor() {
	}

	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException {

		String entity = null;
		String changeSummary = null;
		String identifier = null;

		org.json.JSONArray changedField = null;

		java.util.Set<String> changedFieldSet = new java.util.TreeSet<>();
		org.json.JSONObject json = new org.json.JSONObject(message);

		if(json.has("entityItemChange")) {
	     	entity = json.getJSONObject("entityItemChange").getString("_entity");
	     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
	     	identifier = json.getJSONObject("entityItemChange").getString("_identifier");
	     	changedField = json.getJSONObject("entityItemChange").has("_changedField") ? json.getJSONObject("entityItemChange").getJSONArray("_changedField") : new org.json.JSONArray();
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
				if("Characteristic".equals(entity)){
					Element rootElement = doc.getDocumentElement();
					Element characteristicElement = (Element) xmm.byName(rootElement, "characteristic");
					if(characteristicElement != null) {
						Element dataType = (Element) xmm.byName(characteristicElement, "dataType");
						Element alternativeIdentifierElement = (Element) xmm.byName(characteristicElement, "alternativeIdentifier");
						Element lookup = (Element) xmm.byName(characteristicElement, "lookup");
						
						if(dataType != null) {
							Element old = (Element) xmm.byName(dataType, "_old");
							Element current = (Element) xmm.byName(dataType, "_current");
							DataRequestor dr = new DataRequestor();
							String drr = dr.getCharacteristicData(new org.json.JSONArray().put(identifier));
							if(drr != null && current != null) {
								org.json.JSONObject jr = new org.json.JSONObject(drr);
								org.json.JSONObject item = jr.getJSONArray("items").getJSONObject(0);
								item.put("dataType", ((Element) xmm.byName(current, "_key")).getTextContent());
								if("LOOKUP".equals(((Element) xmm.byName(old, "_key")).getTextContent()))
									item.put("lookup", "");
								log( dr.addCharacteristicData(new org.json.JSONArray().put(item)) );
							}else if(drr != null && current == null) {
								
							}
						}
						
						if(lookup != null) {
							Element old = (Element) xmm.byName(lookup, "_old");
							Element current = (Element) xmm.byName(lookup, "_current");
							DataRequestor dr = new DataRequestor();
							String drr = dr.getCharacteristicData(new org.json.JSONArray().put(identifier));
							if(drr != null && current != null) {
								org.json.JSONObject jr = new org.json.JSONObject(drr);
								org.json.JSONObject item = jr.getJSONArray("items").getJSONObject(0);
								item.put("lookup", ((Element) xmm.byName(current, "_code")).getTextContent());
								dr.addCharacteristicData(new org.json.JSONArray().put(item));
							}else if(drr != null && current == null) {
								org.json.JSONObject jr = new org.json.JSONObject(drr);
								org.json.JSONObject item = jr.getJSONArray("items").getJSONObject(0);
								item.put("lookup", "");
								dr.addCharacteristicData(new org.json.JSONArray().put(item));
							}
							String container = "characteristic_and_lookups";
							java.util.Map<String, String> dictionaryData = readLkpValues(container.replaceAll("/", "<::>"));
							if(old != null) {
								dictionaryData.remove(identifier);
							}
							if(current != null) {
								dictionaryData.put(identifier, ((Element)xmm.byName(current, "_code")).getTextContent());
							}
							keepLkpValues(container.replaceAll("/", "<::>"), dictionaryData);
						}
						if(alternativeIdentifierElement != null) {
							try {
								String systemCode = xmm.byName( xmm.byName( xmm.byName(alternativeIdentifierElement, "_qualification"), "system"), "_code").getTextContent();
								if("ECC".equals(systemCode) || "S4HANA".equals(systemCode)) {
									String currentAlternativeIdentifier = null;
									String oldAlternativeIdentifier = null;
									Element oldAlternativeIdentifierElement = null;
									Element currentAlternativeIdentifierElement = null;
									currentAlternativeIdentifierElement = (Element) xmm.byName( xmm.byName(alternativeIdentifierElement, "alternativeIdentifier"), "_current" );
									oldAlternativeIdentifierElement = (Element) xmm.byName( xmm.byName(alternativeIdentifierElement, "alternativeIdentifier"), "_old" );
									if(currentAlternativeIdentifierElement != null) {
										currentAlternativeIdentifier = currentAlternativeIdentifierElement.getTextContent();
									}
									if(oldAlternativeIdentifierElement != null) {
										oldAlternativeIdentifier = oldAlternativeIdentifierElement.getTextContent();
									}
									if(oldAlternativeIdentifier == null && currentAlternativeIdentifier != null) {
										try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping" ).toFile(), true), java.nio.charset.StandardCharsets.UTF_8))){
											pw.println( rw.getRw().serializeChunk(new Object[] { currentAlternativeIdentifier, identifier }));
										}catch(java.io.IOException e) {
											e.printStackTrace();
										}
									}else if(oldAlternativeIdentifier != null && currentAlternativeIdentifier != null) {
										try {
											java.util.Map<String, String> eccFieldMapping = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping")).parallelStream().map(rw.getRw()::parseLine).collect(java.util.stream.Collectors.toConcurrentMap(a -> a[0], a -> a[1]));
											eccFieldMapping.remove(oldAlternativeIdentifier);
											eccFieldMapping.put(currentAlternativeIdentifier, identifier);
											try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
												eccFieldMapping.entrySet().forEach(entry -> pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
											}catch(java.io.IOException e) {
												e.printStackTrace();
											}
										}catch(java.io.IOException e) {
											logE(e);
										}
									}else if(oldAlternativeIdentifier != null && currentAlternativeIdentifier == null) {
										java.util.Map<String, String> eccFieldMapping = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping")).parallelStream().map(rw.getRw()::parseLine).collect(java.util.stream.Collectors.toConcurrentMap(a -> a[0], a -> a[1]));
										eccFieldMapping.remove(oldAlternativeIdentifier);
										try(java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.OutputStreamWriter( new java.io.FileOutputStream( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.base_directory"), "cache", "characteristic_ecc_mapping" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
											eccFieldMapping.entrySet().forEach(entry -> pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
										}catch(java.io.IOException e) {
											e.printStackTrace();
										}
									}
								}
							}catch(NullPointerException e) {
								logE(e);
							}
						}
						
					}
				}
			}
		}else if(json.has("entityItemsDeleted")){
			log("A message body: " + json);
		}
	}
	
	private void keepLkpValues(String container, java.util.Map<String, String> data){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), container).toString())))){
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			for(java.util.Map.Entry<String, String> entry : data.entrySet() ) {
				pw.println(rw.getRw().serializeChunk(new String[] { entry.getKey(), entry.getValue() }, delim, sep, escp));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private java.util.Map<String, String> readLkpValues(String container){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, delim, sep, escp);
				if(pieces.length > 1)
					data.put(pieces[0], pieces[1]);
				else
					log("Malformed line -->" + line + "<--" );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return data;
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

	private static final Logger LOGGER = Logger.getLogger(CharacteristicChangeProcessor.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp/characteristicChange/CharacteristicChangeActiveMQListener-%g.log", 25 * 1024 * 1024, 10, true);
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
//                new java.io.FileOutputStream("../logs/CharacteristicChangeActiveMQListener.log", true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "]  " + message);
//        } catch (java.io.IOException e) {
//        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/CharacteristicChangeActiveMQListener.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

}
