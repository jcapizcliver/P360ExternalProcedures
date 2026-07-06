package mx.com.liverpool.p360.services.core.sftp.handlers;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ECC122ResponseHandler extends DefaultHandler {

	private java.util.LinkedList<Product122> collected = new java.util.LinkedList<>();
	private java.util.LinkedList<Product122> productStack = new java.util.LinkedList<>();
	private boolean valueOpen = false;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Product".equals(name)) {
			Product122 p = new Product122();
			p.setProposalId( attributes.getValue("ZNPRST") );
			java.util.LinkedList<Value> values = new java.util.LinkedList<>();
			p.setValues(values);
			productStack.addLast(p);
		}else if("Value".equals(name)) {
			if(!productStack.isEmpty()) {
				Product122 p = productStack.getLast();
				Value v = new Value();
				v.setAttributeId(attributes.getValue("AttributeID"));
				p.getValues().addLast(v);
				valueOpen = true;
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) {
		if(valueOpen && !productStack.isEmpty()) {
			Product122 p = productStack.getLast();
			if(!p.getValues().isEmpty()) {
				Value v = p.getValues().getLast();
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
		}
	}
	
	public java.util.LinkedList<Product122> getCollected(){
		return collected;
	}
	
}
