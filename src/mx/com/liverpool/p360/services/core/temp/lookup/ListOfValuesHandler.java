package mx.com.liverpool.p360.services.core.temp.lookup;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ListOfValuesHandler extends org.xml.sax.helpers.DefaultHandler {

	private class Value {
		
		private String attributeId;
		private String id;
		private String text;
		
		public Value(String attributeId, String id) {
			this.attributeId = attributeId;
			this.id = id;
		}
		
		public void setText(String text) {
			this.text = text;
		}
		
		public String getAttributeId() {
			return attributeId;
		}
		
		public String getId() {
			return id;
		}
		
		public String getText() {
			return text;
		}
		
	}
	
	private class ListOfValue {
		
		private String id;
		private String name;
		private Value currentValue = null;
		private java.util.LinkedList<Value> values = new java.util.LinkedList<>();
		
		public Value getCurrentValue() {
			return currentValue;
		}
		
		public void setCurrentValue(Value value) {
			currentValue = value;
		}
		
		public void addValue() {
			if(currentValue != null) {
				values.addLast(currentValue);
				currentValue = null;
			}
		}
		
		public ListOfValue(String id) {
			this.id = id;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getId() {
			return this.id;
		}
		
		public String getName() {
			return this.name;
		}
		
		public java.util.LinkedList<Value> getValues(){
			return values;
		}
	}
	
	private java.util.LinkedList<ListOfValue> stack = new java.util.LinkedList<>();
	private java.util.LinkedList<ListOfValue> finished = new java.util.LinkedList<>();
	
	private boolean isName = false;
	private boolean isMetaData = false;
	private java.util.Map<String, String> qp = getQueryParameters();
	private RESTWrapper rw = new RESTWrapper();
	private RequestHandler createLookups = new RequestHandler(
			new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)"))
			, 100, request -> rw.writeData("list", "Lookup", null, qp, request, System.out::println) );
	private RequestHandler createLookupValues = new RequestHandler(
			new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
			.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
			, 100, request ->{ 
				createLookups.sendData();
				rw.writeData("list", "LookupValue", null, qp, request, System.out::println); 
			} );
	
	private java.util.Map<String, String> getQueryParameters(){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		return qp;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("ListOfValue".equals(name)) {
			ListOfValue attribute = new ListOfValue( attributes.getValue("ID") );
			stack.addLast(attribute);
		}else if("Name".equals(name)) {
			isName = !stack.isEmpty();
		}else if("Value".equals(name)) {
			if(!stack.isEmpty() && !isMetaData) {
				ListOfValue a = stack.getLast();
				Value value = new Value(attributes.getValue("AttributeID"), attributes.getValue("ID"));
				a.setCurrentValue(value);
			}
		}else if("MetaData".equals(name)) {
			isMetaData = !stack.isEmpty();
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		StringBuilder sb = new StringBuilder();
		if(isName) {
			if(!stack.isEmpty()) {
				ListOfValue a = stack.getLast();
				String name = a.getName();
				sb.append(name == null ? "" : name);
				sb.append(ch, start, length);
				a.setName(sb.toString());
			}
		}else {
			if(!stack.isEmpty()) {
				ListOfValue a = stack.getLast();
				Value cv = a.getCurrentValue();
				if( cv != null ) {
					String text = cv.getText();
					sb.append(text == null ? "" : text);
					sb.append(ch, start, length);
					cv.setText(sb.toString());
				}
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("ListOfValue".equals(name)) {
			finished.addLast(stack.removeLast());
		}else if("Name".equals(name)) {
			if(isName) {
				isName = false;
			}
		}else if("MetaData".equals(name)) {
			isMetaData = false;
		}else if("Value".equals(name)) {
			if(!stack.isEmpty() && !isMetaData) {
				ListOfValue a = stack.getLast();
				a.addValue();
			}
		}
	}
	
	public java.util.LinkedList<ListOfValue> getFinished(){
		return this.finished;
	}
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		ListOfValuesHandler handler = new ListOfValuesHandler();
		javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        javax.xml.parsers.SAXParser parser = factory.newSAXParser();
        parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "atributos", "step-11733140395690319939-exported.xml").toFile(), handler);
//        parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "atributos", "step-18375348095873831113-exported.xml").toFile(), handler);
        java.util.LinkedList<ListOfValue> lovs = handler.getFinished();
        System.out.println("HH: " + lovs.size());
        java.util.Set<String> ofInterest = new java.util.TreeSet<>(java.util.Arrays.asList(("ConfundaprotectoraLOV"
//        		+ "MecanismodebloqueoLOV\r\n"
//        		+ "CompatibilidadconasadoresLOV\r\n"
//        		+ "ElevaciondelsueloLOV\r\n"
//        		+ "DistribuciondenutrientesLOV\r\n"
//        		+ "MetododerizadoLOV\r\n"
//        		+ "RemovibleconacetonaLOV\r\n"
        		).split("\r\n")));
        for(ListOfValue lov : lovs) {
//        	if(ofInterest.contains(lov.getId())) {
        		handler.processListOfValues(lov);
//        	}
        }
        handler.createLookupValues.sendData();
//        attributes.forEach(handler::printAttribute);
//        handler.printAttribute(attributes.getFirst());
//        System.out.println(attributes.size());
	}
	
	private void processListOfValues(ListOfValue lov) {
		createLookups.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + lov.getId() + "'")).put("values", new org.json.JSONArray().put(lov.getName())));
		java.util.LinkedList<Value> values = lov.getValues();
		for(Value v : values) {
			createLookupValues.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + (v.getId() != null ? v.getId().replaceAll("'", "\\\\'")  : v.getText().replaceAll("'", "\\\\'")) + "'@'" + lov.getId() + "'")).put("values", new org.json.JSONArray().put(v.getText()).put(true)));
		}
	}

//	private void printAttribute(ListOfValue attribute) {
//		System.out.println(attribute.getId() + ", Name: " + attribute.getName());
//		java.util.LinkedList<Value> metaDataValues = attribute.getValues();
//		System.out.println("*** Values ***");
//		metaDataValues.forEach(this::printValue);
//		System.out.println("~~~\n");
//	}
	
//	private void printValue(Value value) {
//		System.out.println((value.getId() != null ? "ID: " + value.getId() + ", " : "") + (value.getAttributeId() != null ? "AttributeID: " + value.getAttributeId() + ", " : "") + (value.getText() != null ? "Text: " + value.getText() : ""));
//	}
}
