package mx.com.liverpool.p360.services.core.temp.characteristic;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class AttributeListHandler extends org.xml.sax.helpers.DefaultHandler {

	public class AttributeGroupLink {
		
		private String attributeGroupId;
		
		public AttributeGroupLink(String attributeGroupId) {
			this.attributeGroupId = attributeGroupId;
		}
		
		public String getAttributeGroupId() {
			return this.attributeGroupId;
		}
		
	}
	
	public class Value {
		
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
	
	public class MultiValue{
		
		private String attributeId;
		private java.util.LinkedList<Value> values = new java.util.LinkedList<>();
		private Value currentValue = null;
		
		public MultiValue(String attributeId) {
			this.attributeId = attributeId;
		}
		
		public Value getCurrentValue() {
			return this.currentValue;
		}
		
		public void setCurrentValue(Value currentValue) {
			addValue();
			this.currentValue = currentValue;
		}
		
		public void addValue() {
			if(currentValue != null) {
				this.values.addLast(currentValue);
				this.currentValue = null;
			}
		}
		
		public String getAttributeId() {
			return attributeId;
		}
		
		public java.util.LinkedList<Value> getValues(){
			return this.values;
		}
		
	}
	
	public class MetaData{
		
		private java.util.LinkedList<Value> values = new java.util.LinkedList<>();
		private java.util.LinkedList<MultiValue> multiValues = new java.util.LinkedList<>();
		
		private Value currentValue = null;
		private MultiValue currentMultiValue = null;
		
		public void setCurrentValue(Value currentValue) {
			addValue();
			this.currentValue = currentValue;
		}
		
		public void setCurrentMultiValue(MultiValue currentMultiValue) {
			addMultiValue();
			this.currentMultiValue = currentMultiValue;
		}
		
		public Value getCurrentValue() {
			return this.currentValue;
		}
		
		public MultiValue getCurrentMultiValue() {
			return this.currentMultiValue;
		}
		
		public void addValue() {
			if(this.currentValue != null) {
				this.values.addLast(this.currentValue);
				this.currentValue = null;
			}
		}
		
		public void addMultiValue() {
			if(this.currentMultiValue != null) {
				this.multiValues.addLast(this.currentMultiValue);
				this.currentMultiValue = null;
			}
		}
		
		public java.util.LinkedList<Value> getValues(){
			return this.values;
		}
		
		public java.util.LinkedList<MultiValue> getMultiValues(){
			return this.multiValues;
		}
		
	}
	
	public class Attribute {
		
		private String id;
		private boolean mandatory;
		private String name;
		private String listOfValues;
		private MetaData metaData;
		private java.util.LinkedList<AttributeGroupLink> attributeGroupLinks = new java.util.LinkedList<>();
		private AttributeGroupLink currentAttributeGoupLink = null;
		
		public Attribute(String id, boolean mandatory) {
			this.id = id;
			this.mandatory = mandatory;
			this.metaData = new MetaData();
		}
		
		public void addAttributeGroupLink() {
			if(this.currentAttributeGoupLink != null) {
				this.attributeGroupLinks.addLast(this.currentAttributeGoupLink);
				this.currentAttributeGoupLink = null;
			}
		}
		
		public void setCurrentAttributeGroupLink(AttributeGroupLink currentAttributeGroupLink) {
			this.currentAttributeGoupLink = currentAttributeGroupLink;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setListOfValues(String listOfValues) {
			this.listOfValues = listOfValues;
		}
		
		public String getId() {
			return this.id;
		}
		
		public boolean isMandatory() {
			return this.mandatory;
		}
		
		public String getName() {
			return this.name;
		}
		
		public String getListOfValues() {
			return this.listOfValues;
		}
		
		public MetaData getMetaData() {
			return metaData;
		}
		
		public java.util.LinkedList<AttributeGroupLink> getAttributeGroupLinks(){
			return this.attributeGroupLinks;
		}
		
	}
	
	private java.util.LinkedList<Attribute> stack = new java.util.LinkedList<>();
	private java.util.LinkedList<Attribute> finished = new java.util.LinkedList<>();
	
	private boolean isName = false;
	private boolean isListOfValues = false;
	private boolean isMetaData = false;
	private java.util.Map<String, String> qp = getQueryParameters();
	private RESTWrapper rw = new RESTWrapper();
	private RequestHandler createCategories = new RequestHandler(
			new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
			.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
			, 100, request -> rw.writeData("list", "LookupValue", null, qp, request, System.out::println) );
	private RequestHandler createCharacteristicLookupValue = new RequestHandler(
			new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
			.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
			.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(AttributeGroup)"))
			, 100, request -> rw.writeData("list", "LookupValue", null, qp, request, System.out::println) );
	private RequestHandler createCharacteristics = new RequestHandler(
				new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)"))
					.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Description(es)"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Category"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Entities"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Order"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
			, 100, request -> {
					createCategories.sendData();
					rw.writeData("list", "Characteristic", null, qp, request, System.out::println); 
				} );
	private RequestHandler createCharacteristicsRechazo = new RequestHandler(
			new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
			, 100, request -> {
				createCategories.sendData();
				createCharacteristics.sendData();
				rw.writeData("list", "Characteristic", null, qp, request, System.out::println); 
			} );
	
	private java.util.Map<String, String> getQueryParameters(){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		return qp;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Attribute".equals(name)) {
			Attribute attribute = new Attribute( attributes.getValue("ID"), Boolean.parseBoolean(attributes.getValue("Mandatory")) );
			stack.addLast(attribute);
		}else if("Name".equals(name)) {
			isName = !stack.isEmpty();
		}else if("AttributeGroupLink".equals(name)) {
			AttributeGroupLink agl = new AttributeGroupLink(attributes.getValue("AttributeGroupID"));
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				a.setCurrentAttributeGroupLink(agl);
			}
		}else if("ListOfValueLink".equals(name)) {
			isListOfValues = !stack.isEmpty();
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				a.setListOfValues(attributes.getValue("ListOfValueID"));
			}
		}else if("Value".equals(name)) {
			if(isMetaData) {
				if(!stack.isEmpty()) {
					Attribute a = stack.getLast();
					MetaData md = a.getMetaData();
					Value value = new Value(attributes.getValue("AttributeID"), attributes.getValue("ID"));
					MultiValue mv = md.getCurrentMultiValue();
					if(mv != null) {
						mv.setCurrentValue(value);
					} else {
						md.setCurrentValue(value);
					}
				}
			}
		}else if("MetaData".equals(name)) {
			isMetaData = !stack.isEmpty();
		}else if("MultiValue".equals(name)){
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				MetaData md = a.getMetaData();
				MultiValue mv = new MultiValue(attributes.getValue("AttributeID"));
				md.setCurrentMultiValue(mv);
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		StringBuilder sb = new StringBuilder();
		if(isName) {
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				String name = a.getName();
				sb.append(name == null ? "" : name);
				sb.append(ch, start, length);
				a.setName(sb.toString());
			}
		}else if(isListOfValues) {
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				String lov = a.getListOfValues();
				sb.append(lov == null ? "" : lov);
				sb.append(ch, start, length);
				a.setListOfValues(sb.toString());
			}
		}else {
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				MetaData md = a.getMetaData();
				MultiValue mv = md.getCurrentMultiValue();
				if( mv != null ) {
					Value cv = mv.getCurrentValue();
					if(cv != null) {
						String text = cv.getText();
						sb.append(text == null ? "" : text);
						sb.append(ch, start, length);
						cv.setText(sb.toString());
					}
				}else {
					Value cv = md.getCurrentValue();
					if( cv != null ) {
						String text = cv.getText();
						sb.append(text == null ? "" : text);
						sb.append(ch, start, length);
						cv.setText(sb.toString());
					}
				}
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Attribute".equals(name)) {
			finished.addLast(stack.removeLast());
		}else if("Name".equals(name)) {
			if(isName) {
				isName = false;
			}
		}else if("AttributeGroupLink".equals(name)) {
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				a.addAttributeGroupLink();
			}
		}else if("ListOfValueLink".equals(name)) {
			if(isListOfValues) {
				isListOfValues = false;
			}
		}else if("MetaData".equals(name)) {
			isMetaData = false;
		}else if("Value".equals(name)) {
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				MetaData md = a.getMetaData();
				MultiValue mv = md.getCurrentMultiValue();
				if(mv != null) {
					mv.addValue();
				}else {
					if(md != null) {
						md.addValue();
					}
				}
			}
		}else if("MultiValue".equals(name)) {
			if(!stack.isEmpty()) {
				Attribute a = stack.getLast();
				MetaData md = a.getMetaData();
				if(md != null) {
					md.addMultiValue();
				}
			}
		}
	}
	
	public java.util.LinkedList<Attribute> getFinished(){
		return this.finished;
	}
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		AttributeListHandler handler = new AttributeListHandler();
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
        java.util.LinkedList<Attribute> attributes = handler.getFinished();
        attributes.forEach(handler::printAttribute);
        System.out.println(attributes.size());
        RESTWrapper rw = new RESTWrapper();
        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("fields", "Characteristic.Identifier,Characteristic.Lookup->Lookup.Identifier");
        qp.put("query", 
        		"not Characteristic.DataType = \"NONE\" and Characteristic.ParentCharacteristic is empty"
        	);
        qp.put("pageSize", "5000");
        java.util.Map<String, String> characteristics = new java.util.TreeMap<>();
        java.util.LinkedList<String> vals = new java.util.LinkedList<>();
        rw.collectData("list", "Characteristic", null, "bySearch", qp, row ->{ vals.addLast(row.getJSONArray("values").getString(0)); characteristics.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)); }, System.out::println);
        System.out.println("Collected: " + characteristics.size() + " characteristics.");
        System.out.println(vals.size());
        System.out.println("***");
        int existing = 0;
        String lkp = null;
        java.util.LinkedList<Attribute> nonExisting = new java.util.LinkedList<>();
        for(Attribute attribute : attributes) {
        	lkp = characteristics.get(attribute.getId());
        	if(lkp != null && attribute.getListOfValues() != null && attribute.getListOfValues().equals(lkp)) {
        		existing++;
        	}else if(lkp != null) {
        		System.out.println("This exists with a different lkp: " + attribute.getId() + ", Current: " + lkp + " vs " + attribute.getListOfValues());
        	}else {
        		nonExisting.addLast(attribute);
        	}
        }
        System.out.println("Existing: " + existing + ". Remaining unknown: " + (attributes.size() - existing) + ". Still non existing: " + nonExisting.size());
        
        nonExisting.forEach(handler::handleNewCharacteristic);
        handler.createCategories.sendData();
        handler.createCharacteristicLookupValue.sendData();
        handler.createCharacteristics.sendData();
        handler.createCharacteristicsRechazo.sendData();
	}
	
	private void handleNewCharacteristic(Attribute attribute) {
		MetaData md = attribute.getMetaData();
		java.util.LinkedList<MultiValue> multiValues = md.getMultiValues();
		java.util.LinkedList<Value> metaDataValues = md.getValues();
		java.util.LinkedList<AttributeGroupLink> glinks = attribute.getAttributeGroupLinks();
		Value attributeHelpText = getValueByAttributeID(metaDataValues, "AttributeHelpText");
		Value displaySequence = getValueByAttributeID(metaDataValues, "DisplaySequence");
		Value isConfigurable = getValueByAttributeID(metaDataValues, "isConfigurable");
		Value creationModificationAtributesIIEP = getValueByAttributeID(metaDataValues, "CreationModificationAtributesIIEP");
		Value isFaceted = getValueByAttributeID(metaDataValues, "isFaceted");
		org.json.JSONArray attributeGroups = new org.json.JSONArray();
		org.json.JSONArray purposes = new org.json.JSONArray();
		if(isConfigurable != null && Boolean.parseBoolean(isConfigurable.getText())) {
			purposes.put("isConfigurable");
		}
		if(isFaceted != null && Boolean.parseBoolean(isFaceted.getText())) {
			purposes.put("isFaceted");
		}
		if(creationModificationAtributesIIEP != null && Boolean.parseBoolean(creationModificationAtributesIIEP.getText())) {
			purposes.put("CreationModificationAtributesIIEP");
		}
		for(MultiValue mv : multiValues) {
			if("isAttInGroupAtt".equals(mv.getAttributeId())) {
				java.util.LinkedList<Value> vls = mv.getValues();
				for(Value v : vls) {
					purposes.put(v.getId());
				}
			}
		}
		for(AttributeGroupLink agl : glinks) {
			attributeGroups.put(agl.getAttributeGroupId());
		}
		createCategories.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + attribute.getId() + "'@'CharacteristicCategories'")).put("values", new org.json.JSONArray().put(attribute.getName()).put(true)));
		createCharacteristicLookupValue.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + attribute.getId() + "'@'Characteristics'")).put("values", new org.json.JSONArray().put(attribute.getName()).put(true).put(attributeGroups)));
		createCharacteristics.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + attribute.getId() + "'")).put("values", new org.json.JSONArray().put(attribute.getName()).put(attributeHelpText == null ? "" : attributeHelpText).put(attribute.getId()).put(new org.json.JSONArray().put("Product2G")).put("LOOKUP").put(attribute.getListOfValues()).put(displaySequence == null ? "64535" : displaySequence.getText()).put(purposes).put(true).put("")));
		addRejection(attribute);
//		printAttribute(attribute);
	}
	
	private void addRejection(Attribute attribute) {
		String id = attribute.getId() + "_Rechazo";
		createCharacteristics.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'"))
				.put("values", 
					new org.json.JSONArray()
						.put(attribute.getName() + " (Rechazo)")
						.put("")
						.put(attribute.getId())
						.put(new org.json.JSONArray().put("Product2G"))
						.put("NONE")
						.put("")
						.put("64535")
						.put(new org.json.JSONArray())
						.put(true)
						.put("")
				)
			);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "mdr_" + attribute.getId() + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Motivo (" + attribute.getName() + ")")
						.put("LOOKUP")
						.put("RejectReazonType")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "msj_" + attribute.getId() + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Mensaje (" + attribute.getName() + ")")
						.put("TEXT")
						.put("")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rem_" + attribute.getId() + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Estatus del Rechazo (" + attribute.getName() + ")")
						.put("LOOKUP")
						.put("CommentStatus")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rma_" + attribute.getId() + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Acción Requerida (" + attribute.getName() + ")")
						.put("LOOKUP")
						.put("RechazoMensajeAccion")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rmum_" + attribute.getId() + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Estampa de Tiempo (" + attribute.getName() + ")")
						.put("DATETIME")
						.put("")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rrd_" + attribute.getId() + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Rol Destino (" + attribute.getName() + ")")
						.put("LOOKUP")
						.put("TargetRole")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rre_" + attribute.getId() + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Rol Emisor (" + attribute.getName() + ")")
						.put("LOOKUP")
						.put("TargetRole")
						.put(true)
						.put(id)
						)
				);
		
	}
	
	private Value getValueByAttributeID(java.util.LinkedList<Value> values, String attributeId) {
		for(Value value : values) {
			if(attributeId.equals(value.getAttributeId())) {
				return value;
			}
		}
		return null;
	}
	
	public void printAttribute(Attribute attribute) {
		System.out.println(attribute.getId() + ", IsMandatory: " + attribute.isMandatory() + ", Name: " + attribute.getName() + ", LoV: " + attribute.getListOfValues());
		MetaData md = attribute.getMetaData();
		java.util.LinkedList<MultiValue> multiValues = md.getMultiValues();
		java.util.LinkedList<Value> metaDataValues = md.getValues();
		java.util.LinkedList<AttributeGroupLink> glinks = attribute.getAttributeGroupLinks();
//		System.out.println("*** MultiValues ***");
//		multiValues.forEach(mv -> {
//			System.out.println("\t" + mv.getAttributeId());
//			java.util.LinkedList<Value> values = mv.getValues();
//			values.forEach(this::printValue);
//		});
//		System.out.println("*** Values ***");
//		metaDataValues.forEach(this::printValue);
//		System.out.println("*** AttributeGroupLink ***");
//		glinks.forEach(gl -> System.out.println(gl.getAttributeGroupId()));
//		System.out.println("~~~\n");
	}
	
	private void printValue(Value value) {
		System.out.println((value.getId() != null ? "ID: " + value.getId() + ", " : "") + (value.getAttributeId() != null ? "AttributeID: " + value.getAttributeId() + ", " : "") + (value.getText() != null ? "Text: " + value.getText() : ""));
	}
}
