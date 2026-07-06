package mx.com.liverpool.p360.services.core.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class ProcessXMLFiles implements XMLFileContentProcessor {

	protected String currentWorkingFile = null;
	
	public final void processFiles(java.io.File[] files) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = null;
        try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
        if(parser != null) {
        	for(File input : files) {
        		if(input.isDirectory()) {
        			java.io.File[] fls = input.listFiles();
        			processFiles(fls);
        		}else {
        			currentWorkingFile = input.getAbsolutePath();
	    	        try {
	    	        	DefaultHandler handler = new ProductFileHandler();
	    	        	parser.parse(input, handler);
	    	        	inspectHandler(handler);
	    	        }catch(org.xml.sax.SAXParseException e) {
	    	        	System.out.println("Problem processing following file: " + input.getName());
	    	        } catch (SAXException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
        		}
            }
        }
	}
	
	public final void processFile(String content) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = null;
        try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
        if(parser != null) {
        	
	        try {
	        	DefaultHandler handler = new ProductFileHandler();
	        	parser.parse(new java.io.ByteArrayInputStream(content.getBytes( java.nio.charset.StandardCharsets.ISO_8859_1 )), handler);
	        	inspectHandler(handler);
	        }catch(org.xml.sax.SAXParseException e) {
	        	System.out.println("Problem processing content");
	        } catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}

	
}
