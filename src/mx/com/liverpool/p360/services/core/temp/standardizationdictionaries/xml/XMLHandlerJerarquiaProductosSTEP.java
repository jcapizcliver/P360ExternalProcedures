package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class XMLHandlerJerarquiaProductosSTEP extends org.xml.sax.helpers.DefaultHandler{

	private java.util.LinkedList<NodePPH> stack = new java.util.LinkedList<>();
	private java.util.LinkedList<NodePPH> finished = new java.util.LinkedList<>();
	
	private boolean isName = false;
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Product".equals(name)) {
			String parentId = null;
			if(!stack.isEmpty()) {
				NodePPH parent = stack.getLast();
				parentId = parent.getId();
			}
			String id = attributes.getValue("ID");
			String userTypeId = attributes.getValue("UserTypeID");
			NodePPH p = new NodePPH();
			p.setId(id);
			p.setUserTypeId(userTypeId);
			if(parentId != null) {
				p.setParentId(parentId);
			}
			stack.addLast(p);
		}else if("Name".equals(name)) {
			isName = true;
		}else if("AttributeLink".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH n = stack.getLast();
				AttributeLinkPPH al = new AttributeLinkPPH();
				al.setAttributeId(attributes.getValue("AttributeID"));
				al.setMandatory( attributes.getValue("Mandatory") != null ? Boolean.parseBoolean( attributes.getValue("Mandatory") ) : false );
				n.setCurrentAttributeLink(al);
			}
		} else if("ValueFilter".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH n = stack.getLast();
				AttributeLinkPPH al = n.getCurrentAttributeLink();
				if(al != null) {
					al.setValueFilter(true);
				}
			}
		} else if("MultiValue".equals(name)) {
			if(!stack.isEmpty()) {
				MultiValuePPH mv = new MultiValuePPH();
				mv.setAttributeId(attributes.getValue("AttributeID"));
				NodePPH n = stack.getLast();
				n.setCurrentMultiValue(mv);
			}
		}else if("Value".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH p = stack.getLast();
				ValuePPH value = new ValuePPH();
				value.setId(attributes.getValue("ID"));
				value.setAttributeId(attributes.getValue("AttributeID"));
				AttributeLinkPPH al = p.getCurrentAttributeLink();
				MultiValuePPH mv = p.getCurrentMultiValue();
				if(al != null) {
					if(al.isValueFilter())
						al.setCurrentValue(value);
					else if(al.isMetaData())
						al.setCurrentMetaDataValue(value);
				}else if(mv != null) {
					mv.setCurrentValue(value);
				}else {
					p.setCurrentValue(value);
				}
			}
		}else if("MetaData".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH p = stack.getLast();
				AttributeLinkPPH a = p.getCurrentAttributeLink();
				if(a != null) {
					a.setMetaData(true);
				}
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(isName && !stack.isEmpty()) {
			NodePPH p = stack.getLast();
			StringBuilder sb = new StringBuilder();
			sb.append(p.getName() == null ? "" : p.getName());
			sb.append(ch, start, length);
			p.setName(sb.toString());
		}else if(!stack.isEmpty()) {
			NodePPH n = stack.getLast();
			AttributeLinkPPH al = n.getCurrentAttributeLink();
			MultiValuePPH mv = n.getCurrentMultiValue();
			ValuePPH v = n.getCurrentValue();
			if(al != null && al.getCurrentValue() != null) {
				StringBuilder sb = new StringBuilder();
				sb.append(al.getCurrentValue().getText() == null ? "" : al.getCurrentValue().getText());
				sb.append(ch, start, length);
				if(al.isMetaData()) {
					al.getCurrentMetaDataValue().setText(sb.toString());
				}else if(al.isValueFilter()) {
					al.getCurrentValue().setText(sb.toString());
				}
			}else if(mv != null && mv.getCurrentValue() != null) {
				StringBuilder sb = new StringBuilder();
				sb.append(mv.getCurrentValue().getText() == null ? "" : mv.getCurrentValue().getText());
				sb.append(ch, start, length);
				mv.getCurrentValue().setText( sb.toString() );
			}else if(v != null) {
				StringBuilder sb = new StringBuilder();
				sb.append(v.getText() == null ? "" : v.getText());
				sb.append(ch, start, length);
				v.setText( sb.toString() );
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String name = localName != null && !localName.isEmpty() ? localName : qName;
		if("Name".equals(name)) {
			isName = false;
		}else if("Product".equals(name)) {
			NodePPH p = stack.removeLast();
			if(stack.isEmpty()) {
				finished.addLast(p);
			}else {
				NodePPH pp = stack.getLast();
				pp.getProductos().addLast(p);
			}
		}else if("ValueFilter".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH n = stack.getLast();
				AttributeLinkPPH al = n.getCurrentAttributeLink();
				if(al != null) {
					al.setValueFilter(false);
				}
			}
		}else if("Value".equals(name) && !stack.isEmpty()) {
			if(!stack.isEmpty()) {
				NodePPH n = stack.getLast();
				AttributeLinkPPH al = n.getCurrentAttributeLink();
				MultiValuePPH mv = n.getCurrentMultiValue();
				if(al != null) {
					if(al.isValueFilter())
						al.addValue();
					else if(al.isMetaData())
						al.addMetaDataValue();
				}else if(mv != null) {
					mv.addValue();
				}else {
					n.addValue();
				}
			}
		}else if("MultiValue".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH n = stack.getLast();
				n.addMultiValue();
			}
		}else if("AttributeLink".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH n = stack.getLast();
				n.addAttributeLink();
			}
		}else if("MetaData".equals(name)) {
			if(!stack.isEmpty()) {
				NodePPH n = stack.getLast();
				AttributeLinkPPH a = n.getCurrentAttributeLink();
				if(a != null) {
					if(a.isMetaData()) {
						a.setMetaData(false);
					}
				}
			}
		}
	}
	
	public java.util.LinkedList<NodePPH> getFinished(){
		return finished;
	}
	
}
