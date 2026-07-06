package mx.com.liverpool.p360.services.core.amqp;

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

import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class CambioEnSKU {

	private RESTWorkshop workshop = new RESTWorkshop();
	private XMLMisc xmm = workshop.getXmm();

	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;

	private boolean connected = false;
	private boolean failed = false;

	private void messageProcessor(String message) throws org.json.JSONException, ParserConfigurationException, SAXException, java.io.IOException {

		PubSubGCP pub = new PubSubGCP();

		org.json.JSONObject response = null;
		org.json.JSONObject data = null;
		org.json.JSONArray characteristicRecords = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap = new java.util.TreeMap<>();

		String entity = null;
		String internalId = null;
		String externalId = null;
		String changeSummary = null;

		String creationDate = null;
		String ean = null;
		String skuCurrent = null;
		String previousStatus = null;
		String internalStatus = null;
		String externalStatus = null;
		String rejectionInfo = null;
		org.json.JSONArray logArray = null;
		org.json.JSONArray rechazos = new org.json.JSONArray();

		org.json.JSONObject json = new org.json.JSONObject(message);
     	entity = json.getJSONObject("entityItemChange").getString("_entity");
     	internalId = json.getJSONObject("entityItemChange").getJSONObject("_entityItem").getString("_internalId");
     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");

     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;

    	String baseDirectory = "/u01/workshop/tmp";
    	String baseName = "ToPubSubEntityChange_" + System.currentTimeMillis() + ".json";
    	String fileNameToPubSub = baseDirectory + "/" + baseName;

    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
    		doc = builder.parse( baos );
    		doc.getDocumentElement().normalize();
    	}

		if(doc != null) {
			Element rootElement = doc.getDocumentElement();
			log("NGGUU");
			if("Product2G".equals(entity) || "Article".equals(entity)) {
				qp.put("entityFilter", entity +"," + entity + "CharacteristicValue");
				qp.put("includeIds", "true");
				qp.put("includeLabels", "true");
				response = workshop.makeRequest("GET", "/object/" + entity + "/" + internalId, qp, "");
				data = response.getJSONObject("_data");
				characteristicRecords = data.getJSONArray("_characteristicRecords");
				characteristicsToMap(characteristicRecords, characteristicRecordsMap, rechazos);
				logArray = data.getJSONArray("log");
				for(int i=0; i<logArray.length(); i++) {
					if("HPM".equals(logArray.getJSONObject(i).getJSONObject("_qualification").getJSONObject("channel").getString("_key"))) {
						creationDate = logArray.getJSONObject(i).getString("modificationDate");
					}
				}
				ean = getSimpleValue("MainBarCode", characteristicRecordsMap);
				rejectionInfo = data.optString("rejectionInfo", null);
				previousStatus = !data.has("previousStatus") ? "" : data.getJSONObject("previousStatus").getString("_label");
				internalStatus = !data.has("currentStatus") ? "" : data.getJSONObject("currentStatus").getString("_label");
				externalStatus = !data.has("externalStatus") ? "" : data.getJSONObject("externalStatus").getString("_label");
				try{
					skuCurrent = xmm.byName( xmm.byName( xmm.byName( xmm.byName( xmm.byName(rootElement, "product"), "_characteristicRecords"), "_recordLang"), "values" ), "_current").getTextContent();
				}catch(NullPointerException e) {
					skuCurrent = "";
				}



			}else if("StructureGroup".equals(entity)) {

			}else if("LookupValue".equals(entity)) {

			}
		}

	}

	private void complementaRechazos(org.json.JSONArray rechazos, String internalId, String rejectionInfo) {
		String characteristicIdentifier = null;
		String recordKey = null;
		org.json.JSONObject rechazo = null;
		org.json.JSONArray children = null;
		String aux = null;
		for(int i=0; i<rechazos.length(); i++) {
			rechazo = rechazos.getJSONObject(i);
			characteristicIdentifier = rechazo.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").replaceAll("(_Rechazo)$", "");
			recordKey = rechazo.getJSONObject("_qualification").getString("recordKey");
			children = rechazo.getJSONArray("_children");
			if(children != null) {
				//{ if($temp.rejectionInfo = 'QAToPurchase') then('TG03') else ( if($temp.rejectionInfo =  'QAToProvider')then('TG03')else ( if($temp.rejectionInfo = 'GDToPurchase')then('TG04')else() ))}
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
				// if($temp.rejectionInfo = 'QAToPurchase') then('TG01') else ( if($temp.rejectionInfo =  'QAToProvider')then('TG02')else ())
				aux = "QAToPurchase".equals(rejectionInfo) ? "TG01" : "QAToProvider".equals(rejectionInfo) ? "TG02" : "";
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
				aux = "QAToPurchase".equals(rejectionInfo) ? "TG03" : "QAToProvider".equals(rejectionInfo) ? "TG03" : "GDToPurchase".equals(rejectionInfo) ? "TG04" : "";
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
					log("<::::::>" + characteristicIdentifier + " while placing rma_ prefix.");
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
		org.json.JSONObject request = new org.json.JSONObject().put("_characteristicRecords", rechazos).put("rejectionInfo", "");
		log("Sending this payload: " + request);
		org.json.JSONObject response = workshop.makeRequest("PUT", "/object/Product2G/" + internalId, new java.util.TreeMap<>(), request.toString());
		log("Completado el complemento a rechazos. " + response);
	}

	private String getSimpleValue(String characteristicIdentifier, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicRecordsMap) {
		java.util.LinkedList<org.json.JSONObject> list = characteristicRecordsMap.get(characteristicIdentifier);
		if(list != null) {
			return list.getLast().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
		}
		return null;
	}

	private void characteristicsToMap(org.json.JSONArray characteristicRecords, java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap, org.json.JSONArray rechazos){
		java.util.LinkedList<org.json.JSONObject> lst = null;
		org.json.JSONObject characteristicRecord = null;
		String characteristicIdentifier = null;
		org.json.JSONArray children = null;
		org.json.JSONObject child = null;
		boolean notEnough = true;
		for(int i=0; i<characteristicRecords.length(); i++) {
			characteristicRecord = characteristicRecords.getJSONObject(i);
			characteristicIdentifier = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = characteristicRecordsMap.get(characteristicIdentifier);
			if(lst == null) {
				lst = new java.util.LinkedList<>();
				characteristicRecordsMap.put(characteristicIdentifier, lst);
			}
			lst.addLast(characteristicRecord);
			if(characteristicIdentifier.endsWith("_Rechazo") || characteristicIdentifier.equals("Comentario")) {
				children = characteristicRecord.getJSONArray("_children");
				if(children != null) {
					for(int j=0; j<children.length(); j++) {
						child = children.getJSONObject(j);
						if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").startsWith("rre_")) {
							notEnough = false;
						}
					}
				}else {
					rechazos.put(characteristicRecord);
				}
				if(notEnough) {
					rechazos.put(characteristicRecord);
				}
				notEnough = true;
			}
		}
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

	private void process(String host, String port, String qName) {
		if(!failed){
			if(!connected){
				connect(host, port, qName);
				if(failed){
				}
			}
		}else{
		}
		if(failed) {
		}
		try{
			while(true){
				responseMessage = consumer.receive(3000);
			    if (responseMessage != null && responseMessage instanceof TextMessage) {
			     	messageProcessor(((TextMessage) responseMessage).getText());
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

	private void disconnect() {
		if(connection != null){
			try{
				connection.close();
			}catch(JMSException e){
				e.printStackTrace();
			}
		}
	}

	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/activeMQListener.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private static void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/activeMQListener.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

	public static void main(String[] args) {
		CambioEnSKU o = new CambioEnSKU();
		o.process(args[0], args[1], args[2]);
	}
}
