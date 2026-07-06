package mx.com.liverpool.p360.services.core.sftp.handlers;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ECC122AttributesHandler extends DefaultHandler {
	
	public class Product {

		private java.util.List<Value> attributes = new java.util.ArrayList<>();
		private java.util.List<Value> values = new java.util.ArrayList<>();
		private boolean isAttributes = false;
		
		public boolean isAttributes() {
			return isAttributes;
		}
		
		public void setIsAttributes(boolean isAttributes) {
			this.isAttributes = isAttributes;
		}
		
		public java.util.List<Value> getValues() {
			return values;
		}
		
		public void addValue(Value value) {
			this.values.add( value );
		}
		
		public java.util.List<Value> getAttributes(){
			return attributes;
		}
		
		public void addAttribute(Value value) {
			this.attributes.add(value);
		}
		
	}

	private java.util.LinkedList<Product> collected = new java.util.LinkedList<>();
	private java.util.LinkedList<Product> productStack = new java.util.LinkedList<>();
	private boolean valueOpen = false;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Product".equals(name)) {
			Product p = new Product();
			productStack.addLast(p);
		}else if("Value".equals(name)) {
			if(!productStack.isEmpty()) {
				Product p = productStack.getLast();
				Value v = new Value();
				v.setAttributeId(attributes.getValue("AttributeID"));
				if(p.isAttributes()) {
					p.addAttribute(v);
				}else {
					p.addValue(v);
				}
				p.getValues().add(v);
				valueOpen = true;
			}
		}else if("Attributes".equals(name)) {
			if(!productStack.isEmpty()) {
				Product p = productStack.getLast();
				p.setIsAttributes(true);
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) {
		if(valueOpen && !productStack.isEmpty()) {
			Product p = productStack.getLast();
			if(!p.getValues().isEmpty()) {
				Value v = (p.isAttributes() ? p.getAttributes() : p.getValues()).get((p.isAttributes() ? p.getAttributes() : p.getValues()).size() - 1);
				StringBuilder sb = new StringBuilder();
				sb.append(v.getText() == null ? "" : v.getText());
				sb.append(ch, start, length);
				v.setText(sb.toString());
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Product".equals(name)) {
			collected.addLast(productStack.removeLast());
		}else if("Value".equals(name)) {
			valueOpen = false;
		}else if("Attributes".equals(name)) {
			if(!productStack.isEmpty()) {
				Product p = productStack.getLast();
				p.setIsAttributes(false);
			}
		}
	}
	
	public java.util.LinkedList<Product> getCollected(){
		return collected;
	}
	
}
