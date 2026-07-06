package mx.com.liverpool.p360.services.core.temp.characteristic.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class AttributeHandler extends org.xml.sax.helpers.DefaultHandler {

	private java.util.LinkedList<Attribute> stack = new java.util.LinkedList<>();
	private java.util.Map<String, Attribute> finished = new java.util.TreeMap<>();
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Attribute".equals(name)) {
			Attribute a = new Attribute();
			a.setId(attributes.getValue("ID"));
			stack.addLast(a);
		}else {
			if("ListOfValueLink".equals(name)) {
				if(!stack.isEmpty()) {
					Attribute a = stack.getLast();
					a.setLookupValue(attributes.getValue("ListOfValueID"));
				}
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if(!stack.isEmpty()) {
			if("Attribute".equals(name)) {
				Attribute a = stack.removeLast();
				finished.put(a.getId(), a);
			}
		}
	}
	
	public java.util.Map<String, Attribute> getAttributes(){
		return finished;
	}
}
