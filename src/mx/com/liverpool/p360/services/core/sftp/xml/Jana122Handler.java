package mx.com.liverpool.p360.services.core.sftp.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Jana122Handler extends org.xml.sax.helpers.DefaultHandler {

	public class Value{
		
		private String attributeId;
		private String text;
		
		private Value(String attributeId){
			this.attributeId = attributeId;
		}
		
		public String getAttributeId() {
			return attributeId;
		}
		
		public String getText() {
			return text;
		}
		
		public void setText(String text) {
			this.text = text;
		}
		
	}
	
	public class Product {
		
		private String ean11;
		private java.util.LinkedList<Value> values = new java.util.LinkedList<>();
	
		private Value currentValue = null;
		
		private Product(String ean11) {
			this.ean11 = ean11;
		}
		
		public void setCurrentValue(Value currentValue) {
			this.currentValue = currentValue;
		}
		
		public Value getCurrentValue() {
			return currentValue;
		}
		
		public void addValue() {
			if(currentValue != null) {
				values.addLast(currentValue);
				currentValue = null;
			}
		}
		
		public String getEan11() {
			return ean11;
		}
		
		public java.util.LinkedList<Value> getValues(){
			return values;
		}
	
		
	}
	
	private final java.util.LinkedList<Product> stack = new java.util.LinkedList<>();
	private final java.util.LinkedList<Product> complete = new java.util.LinkedList<>();
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Product".equals(name)) {
			Product p = new Product(attributes.getValue("EAN11_EAN"));
			stack.addLast(p);
		}else if("Value".equals(name)) {
			if(!stack.isEmpty()) {
				Product p = stack.getLast();
				Value v = new Value(attributes.getValue("AttributeID"));
				p.setCurrentValue(v);
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(!stack.isEmpty()) {
			Product p = stack.getLast();
			StringBuilder sb = new StringBuilder();
			if(p.getCurrentValue() != null) {
				sb.append( p.getCurrentValue().getText() == null ? "" : p.getCurrentValue().getText() );
				sb.append(ch, start, length);
				p.getCurrentValue().setText(sb.toString());
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Product".equals(name)) {
			if(!stack.isEmpty()) {
				complete.addLast(stack.removeLast());
			}
		}else if("Value".equals(name)) {
			if(!stack.isEmpty()) {
				Product p = stack.getLast();
				p.addValue();
			}
		}
	}
	
	public java.util.LinkedList<Product> getProducts(){
		return complete;
	}
	
}
