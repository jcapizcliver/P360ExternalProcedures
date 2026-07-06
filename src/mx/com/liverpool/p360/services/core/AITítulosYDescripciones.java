package mx.com.liverpool.p360.services.core;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AITítulosYDescripciones {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	private class Item{
		
		private String id;
		private String productName;
		private String descriptionLong;
		private String flags;
		
	}
	
	public class Handler extends DefaultHandler {
    	
        private java.util.LinkedList<Item> stack = new java.util.LinkedList<>();
        private java.util.List<Item> finished = new java.util.ArrayList<>();
        
        private boolean isID = false;
        private boolean isProductName = false;
        private boolean isDescriptionLong = false;
        private boolean isFlag = false;
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;
            if("Item".equals(name)) {
            	Item item = new Item();
            	stack.addLast(item);
            }else if("ID".equals(name)) {
            	if(!stack.isEmpty()) {
            		isID = true;
            	}
            }else if("ProductName".equals(name)) {
            	if(!stack.isEmpty()) {
            		isProductName = false;
            	}
            }else if("DescriptionLong".equals(name)) {
            	if(!stack.isEmpty()) {
            		isDescriptionLong = true;
            	}
            }else if("IAFlagTitleDescriptionArticleGroup".equals(name)) {
            	if(!stack.isEmpty()) {
            		isFlag = true;
            	}
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
        	StringBuilder sb = new StringBuilder();
        	if(!stack.isEmpty()) {
        		Item item = stack.getLast();
	        	if(isID) {
	        		sb.append(item.id == null ? "" : item.id);
	        		sb.append(ch, start, length);
	        		item.id = sb.toString();
	        	}else if(isProductName) {
	        		sb.append(item.productName == null ? "" : item.productName);
	        		sb.append(ch, start, length);
	        		item.productName = sb.toString();
	        	}else if(isDescriptionLong) {
	        		sb.append(item.descriptionLong == null ? "" : item.descriptionLong);
	        		sb.append(ch, start, length);
	        		item.descriptionLong = sb.toString();
	        	}else if(isFlag) {
	        		sb.append(item.flags == null ? "" : item.flags);
	        		sb.append(ch, start, length);
	        		item.flags = sb.toString();
	        	}
        	}
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;
            if("Item".equals(name)) {
            	finished.add( stack.removeLast() );
            }else if("ID".equals(name)) {
            	if(!stack.isEmpty()) {
            		isID = false;
            	}
            }else if("ProductName".equals(name)) {
            	if(!stack.isEmpty()) {
            		isProductName = false;
            	}
            }else if("DescriptionLong".equals(name)) {
            	if(!stack.isEmpty()) {
            		isDescriptionLong = false;
            	}
            }else if("IAFlagTitleDescriptionArticleGroup".equals(name)) {
            	if(!stack.isEmpty()) {
            		isFlag = false;
            	}
            }
        }
    }
	
	public void processData(java.io.ByteArrayInputStream bais) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        Handler handler = new Handler();
        parser.parse(bais, handler);
        java.util.Map<String, String> qp = new java.util.HashMap<>();
        qp.put("includeObjectsInProtocol", "false");
        java.util.List<Item> items = handler.finished;
        org.json.JSONObject request = new org.json.JSONObject();
        org.json.JSONArray columns = new org.json.JSONArray();
        org.json.JSONArray rows = new org.json.JSONArray();
        request.put("columns", columns);
        request.put("rows", rows);
        columns.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionLong(es)"));
        org.json.JSONObject request2 = new org.json.JSONObject();
        org.json.JSONArray columns2 = new org.json.JSONArray();
        org.json.JSONArray rows2 = new org.json.JSONArray();
        request2.put("columns", columns2);
        request2.put("rows", rows2);
        columns2.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductName',root,\"0000.0000.RK\",'ProductName',-1)"));
        String[] flags = null;
        for(Item item : items) {
        	flags = item.flags == null ? null : item.flags.split(";");
        	if(flags != null) {
        		for(int i=0; i<flags.length; i++) {
        			if("001".equals(flags[i])) {
        				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item.id + "'@1")).put("values", new org.json.JSONArray().put(item.descriptionLong)));
        				if(rows.length() == 2000) {
        					rw.writeData("list", "Product2G", null, qp, request, System.out::println);
        					while(rows.length() > 0) {
        						rows.remove(0);
        					}
        				}
        			}else if("002".equals(flags[i])) {
        				rows2.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item.id + "'@1")).put("values", new org.json.JSONArray().put(item.productName)));
        				if(rows2.length() == 2000) {
        					rw.writeData("list", "Product2G", null, qp, request2, System.out::println);
        					while(rows2.length() > 0) {
        						rows2.remove(0);
        					}
        				}
        			}
        		}
        	}
        }
        if(rows.length() > 0) {
        	rw.writeData("list", "Product2G", null, qp, request, System.out::println);
			while(rows.length() > 0) {
				rows.remove(0);
			}
        }
        if(rows2.length() > 0) {
        	rw.writeData("list", "Product2G", null, qp, request2, System.out::println);
			while(rows2.length() > 0) {
				rows2.remove(0);
			}
        }
	}
	
}
