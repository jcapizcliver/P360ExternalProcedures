package mx.com.liverpool.p360.services.core.temp.exports.helper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.amqp.P360ActiveMQBPMStage;

public class CollectSentInfo {

	private RESTWrapper rw = new RESTWrapper();
    private final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(java.time.ZoneId.systemDefault());

	private class Asset{
		
		private String id;
		private String name;
		private String userTypeId;
		
		private Value currentValue = null;
		private java.util.LinkedList<Value> values = new java.util.LinkedList<>();
		
		public Asset(String id, String userTypeId) {
			this.id = id;
			this.userTypeId = userTypeId;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getUserTypeId() {
			return userTypeId;
		}

		public Value getCurrentValue() {
			return currentValue;
		}

		public java.util.LinkedList<Value> getValues() {
			return values;
		}
		
		public void addValue() {
			if(currentValue != null) {
				values.addLast(currentValue);
				currentValue = null;
			}
		}
		
		public void setCurrentValue(Value currentValue) {
			this.currentValue = currentValue;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
	}
	
    public class Value{

    	private String attributeId;
    	private String id;
    	private String text;
    	private String unidadId;
    	
    	public Value(String attributeId, String id, String unidadId) {
    		this.attributeId = attributeId;
    		this.id = id;
    		this.unidadId = unidadId;
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
		
		public String getUnidadId() {
			return unidadId;
		}
		
		public void setText(String text) {
			this.text = text;
		}
    	
    }

    private class MultiValue{
    	
    	private String attributeId;
    	private java.util.LinkedList<Value> values;
    	
    	public MultiValue(String attributeId) {
    		this.attributeId = attributeId;
    		this.values = new java.util.LinkedList<>();
    	}

		public String getAttributeId() {
			return attributeId;
		}
		
		public java.util.LinkedList<Value> getValues(){
			return this.values;
		}
		
		public void addValue(Value value) {
			this.values.addLast(value);
		}

    }
    
    private class Classification{
    	
    	private String id = null;
    	private String type = null;

    	public Classification(String id, String type) {
			this.id = id;
			this.type = type;
		}

		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}
    	
    }
	
    public class Product {
    	
    	private String id;
    	private String parentId;
    	private String userTypeId;
    	private String name = null;
    	
    	private java.util.LinkedList<Value> values = null;
    	private java.util.LinkedList<MultiValue> multiValues = null;
    	private Value workingValue = null;
    	private MultiValue workingMultiValue = null;
    	private java.util.LinkedList<Product> products = new java.util.LinkedList<>();
    	private java.util.LinkedList<Classification> classifications = new java.util.LinkedList<>();
    	private Classification workingClassification = null;
    	
    	public Product(String id, String parentId, String userTypeId) {
    		this.id = id;
    		this.parentId = parentId;
    		this.userTypeId = userTypeId;
    	}
    	
    	public String getName() {
    		return name;
    	}
    	
    	public void setName(String name) {
    		this.name = name;
    	}

		public String getId() {
			return id;
		}

		public String getParentId() {
			return parentId;
		}
		
		public String getUserTypeId() {
			return userTypeId;
		}

		public java.util.LinkedList<Value> getValues() {
			return values;
		}
		
		public java.util.LinkedList<Product> getProducts(){
			return products;
		}
		
		public java.util.LinkedList<MultiValue> getMultiValues(){
			return multiValues;
		}
		
		public java.util.LinkedList<Classification> getClassifications(){
			return classifications;
		}
		
		public Value getWorkingValue() {
			return this.workingValue;
		}
		
		public MultiValue getWorkingMultiValue(){
			return this.workingMultiValue;
		}
		
		public Classification getWorkingClassification() {
			return this.workingClassification;
		}
		
		public void createList() {
			values = new java.util.LinkedList<>();
		}
		
		public void createMultiValueList() {
			this.multiValues = new java.util.LinkedList<>();
		}
		
		public void prepareValue(Value value) {
			if(this.workingValue != null) {
				addValue();
			}
			this.workingValue = value;
		}
		
		public void prepareMultiValue(MultiValue multiValues) {
			if(this.workingMultiValue != null) {
				addMultiValue();
			}
			this.workingMultiValue = multiValues;
		}
		
		public void prepareClassification(Classification classification) {
			if(this.workingClassification != null) {
				addClassification();
			}
			this.workingClassification = classification;
		}
		
		public void addValue() {
			if(workingValue != null) {
				if(this.workingMultiValue != null) {
					this.workingMultiValue.addValue(this.workingValue);
				}else {
					this.values.addLast(workingValue);
				}
				this.workingValue = null;
			}
		}
		
		public void addMultiValue() {
			if(workingMultiValue != null) {
				addValue();
				this.multiValues.addLast(workingMultiValue);
				this.workingMultiValue = null;
			}
		}
		
		public void addClassification() {
			if(workingClassification != null) {
				this.classifications.addLast(this.workingClassification);
				this.workingClassification = null;
			}
		}
		
		public void addProduct(Product product) {
			this.products.addLast(product);
		}
    	
    }

    public class Handler extends DefaultHandler {
    	
        private final java.util.LinkedList<Product> productStack = new java.util.LinkedList<>();
        private final java.util.List<Product> finished = new ArrayList<>();
        
        private final java.util.Map<String, Asset> assetMap = new java.util.TreeMap<>();
        private final java.util.LinkedList<Asset> assetStack = new java.util.LinkedList<>();
    	private Integer productsCounter = 0;
    	private boolean assetName = false;
    	private boolean gettingName = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;
            if ("Product".equals(name)) {
                String id = attributes.getValue("ID");
                String parentId = attributes.getValue("ParentID");
                String userTypeId = attributes.getValue("UserTypeID");
                if(parentId == null && !productStack.isEmpty()) {
                	parentId = productStack.getLast().getId();
                }
                productStack.addLast(new Product(id, parentId, userTypeId));
            }else if("Asset".equals(name)) {
            	String id = attributes.getValue("ID");
            	String userTypeId = attributes.getValue("UserTypeID");
            	Asset a = new Asset(id, userTypeId);
            	assetStack.addLast(a);
            } else {
	            if (!productStack.isEmpty()) {
	                Product product = productStack.getLast();
	                if ("Values".equals(name)) {
	                    product.createList();
	                    product.createMultiValueList();
	                } else if (("Value".equals(name)) && product.getValues() != null) {
	                	String attributeId = attributes.getValue("AttributeID");
	                	if(attributeId != null) {
//	                		attributeIDs.add(attributeId);
	                	}
	                	String valueId = attributes.getValue("ID");
	                	String unidadId = attributes.getValue("UnitID");
	                	Value value = new Value(attributeId, valueId, unidadId);
	                	product.prepareValue(value);
	                } else if( "MultiValue".equals(name) && product.getMultiValues() != null ) {
	                	String attributeId = attributes.getValue("AttributeID");
	                	if(attributeId != null) {
//	                		attributeIDs.add(attributeId);
	                	}
	                	MultiValue multiValueList = new MultiValue(attributeId);
	                	product.prepareMultiValue(multiValueList);
	                } else if("ClassificationReference".equals(name)) {
	                	String classificationId = attributes.getValue("ClassificationID");
	                	String type = attributes.getValue("Type");
	                	Classification classification = new Classification(classificationId, type);
	                	product.prepareClassification(classification);
	                } else if("Name".equals(name)) {
	                	gettingName = true;
	                }
	            }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (!productStack.isEmpty()) {
                Product product = productStack.getLast();
                if(product.getName() == null) {
	                Value workingValue = product.getWorkingValue();
	                if(workingValue != null) {
	                	StringBuilder sb = new StringBuilder();
	            		sb.append(workingValue.getText() == null ? "" : workingValue.getText());
	            		sb.append(ch, start, length);
	            		workingValue.setText( sb.toString() );
	                }
                }else if(gettingName) {
                	StringBuilder sb = new StringBuilder();
            		sb.append(product.getName() == null ? "" : product.getName());
            		sb.append(ch, start, length);
            		product.setName(null);
                }
            }else if(!assetStack.isEmpty()) {
            	Asset a = assetStack.getLast();
            	Value wv = a.getCurrentValue();
            	if(wv != null) {
            		StringBuilder sb = new StringBuilder();
            		sb.append(wv.getText() == null ? "" : wv.getText());
            		sb.append(ch, start, length);
            		wv.setText( sb.toString() );
            	}else {
            		if(assetName) {
            			a.setName( new StringBuilder().append(ch, start, length).toString() );
            		}
            	}
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;
            if (!productStack.isEmpty()) {
                Product product = productStack.getLast();
                if ("Values".equals(name)) {
                } else if ("Value".equals(name)) {
                	product.addValue();
                }else if("MultiValue".equals(name)) {
                	product.addMultiValue();
                }else if("ClassificationReference".equals(name)) {
                	product.addClassification();
                }else if("Name".equals(name)) {
                	gettingName = false;
                } else if("Product".equals(name)) {
                	productStack.removeLast();
                	productsCounter++;
                	if(!productStack.isEmpty()) {
                		productStack.getLast().addProduct(product);
                	}else {
                		finished.add(product);
                	}
                }
            }else if(!assetStack.isEmpty()) {
            	Asset a = assetStack.getLast();
            	if("Value".equals(name)) {
            		a.addValue();
            	}else if("Asset".equals(name)) {
            		assetStack.removeLast();
            		assetMap.put(a.getId(), a);
            	} else if("Name".equals(name)) {
            		assetName = false;
            	}
            }
        }
        
        public Integer getPrductsCounter() {
        	return productsCounter;
        }

        public List<Product> getFinished() {
            return finished;
        }
        
        public java.util.Map<String, Asset> getAssetMap(){
        	return assetMap;
        }
    }
    
    public static void processContent(String content) throws ParserConfigurationException, SAXException, IOException {
    	long init = System.currentTimeMillis();
    	CollectSentInfo an = new CollectSentInfo();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.procesaContent(content, parser);
		an.log("Ahora los que faltaron:");
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	CollectSentInfo an = new CollectSentInfo();
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	if (args.length == 0) {
            System.err.println("Usage: java CuentaSKUsConNegocios <directory with xml files>");
            System.exit(1);
        }
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        java.util.List<Object[]> data = new java.util.ArrayList<>();
        java.util.Map<String, String> qp = new java.util.HashMap<>();
        qp.put("fields", "Product2G.ProductNo,Product2G.FirstDateApproved");
        qp.put("pageSize", "1000");
        StringBuilder sb = new StringBuilder();
        int m =0;
        java.util.List<String> noFirstDateApproved = new java.util.ArrayList<>();
        java.util.List<String> noFirstDateApprovedItem = new java.util.ArrayList<>();
        for(Object[] pieces : an.productTimestamp) {
        	sb.append(sb.length() == 0 ? "" : ",").append("'").append(pieces[0]).append("'@1");
        	m++;
        	if(m % 1000 == 0) {
        		qp.put("items", sb.toString());
        		an.rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
        			org.json.JSONArray values = row.getJSONArray("values");
        			if("".equals(values.getString(1))) {
        				noFirstDateApproved.add(values.getString(0));
        			}
        		});
        		sb.setLength(0);
        	}
        }
        if(sb.length() > 0) {
        	qp.put("items", sb.toString());
    		an.rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
    			org.json.JSONArray values = row.getJSONArray("values");
    			if("".equals(values.getString(1))) {
    				noFirstDateApproved.add(values.getString(0));
    			}
    		});
    		sb.setLength(0);
        }
        m = 0;
        qp.put("fields", "Article.FirstDateApproved");
        for(Object[] pieces : an.itemTimestamp) {
        	sb.append(sb.length() == 0 ? "" : ",").append("'").append(pieces[0]).append("'@1");
        	m++;
        	if(m % 1000 == 0) {
        		qp.put("items", sb.toString());
        		an.rw.collectData("list", "Article", null, "byItems", qp, row -> {
        			org.json.JSONArray values = row.getJSONArray("values");
        			if("".equals(values.getString(1))) {
        				noFirstDateApprovedItem.add(values.getString(0));
        			}
        		});
        		sb.setLength(0);
        	}
        }
        if(sb.length() > 0) {
        	qp.put("items", sb.toString());
        	an.rw.collectData("list", "Article", null, "byItems", qp, row -> {
        		org.json.JSONArray values = row.getJSONArray("values");
        		if("".equals(values.getString(1))) {
        			noFirstDateApprovedItem.add(values.getString(0));
        		}
        	});
        	sb.setLength(0);
        }
        m = 0;
        
        an.procesaDirectorio(args[0], data, parser);
        java.util.Collections.sort( an.productTimestamp, (o1, o2)-> ((Long)o1[1]).compareTo((Long)o2[1]) );
        java.util.Collections.sort( an.itemTimestamp, (o1, o2)-> ((Long)o1[1]).compareTo((Long)o2[1]) );
        java.util.Map<String, String> qp0 = new java.util.HashMap<>();
        qp0.put("includeObjectsInProtocol", "false");
        RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.LastDateApproved")), 1000, request -> an.rw.writeData("list", "Product2G", null, qp0, request, an::log) );
        for(Object[] pieces : an.productTimestamp) {
        	rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(an.formatTimestamp((Long)pieces[1]))));
        }
        rh.sendData();
        rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.LastDateApproved")), 1000, request -> an.rw.writeData("list", "Article", null, qp0, request, an::log) );
        for(Object[] pieces : an.itemTimestamp) {
        	rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(an.formatTimestamp((Long)pieces[1]))));
        }
        rh.sendData();
        
        java.util.Collections.sort( an.productTimestamp, (o1, o2)-> ((String)o1[0]).compareTo((String)o2[0]) );
        java.util.Collections.sort( an.itemTimestamp, (o1, o2)-> ((String)o1[0]).compareTo((String)o2[0]) );
        String prevId = null;
        java.util.List<Long> timestamps = new java.util.ArrayList<>();
        rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.FirstDateApproved")), 1000, request -> an.rw.writeData("list", "Product2G", null, qp0, request, an::log) );
        int timesForProduct = 0;
        for(Object[] pieces : an.productTimestamp) {
        	if(prevId != null && !prevId.equals((String)pieces[0])) {
        		if(timestamps.size() > 1) {
        			if(noFirstDateApproved.contains(prevId)) {
        				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevId + "'@1")).put("values", new org.json.JSONArray().put(an.formatTimestamp((Long)timestamps.get(0)))));
        				timesForProduct++;
        			}
        		}
        		timestamps.clear();
        	}
        	timestamps.add((Long)pieces[1]);
        	prevId = (String) pieces[0];
        }
        if(timestamps.size() > 1) {
			if(noFirstDateApproved.contains(prevId)) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevId + "'@1")).put("values", new org.json.JSONArray().put(an.formatTimestamp((Long)timestamps.get(0)))));
			}
		}
        rh.sendData();
		timestamps.clear();
        prevId = null;
        rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.FirstDateApproved")), 1000, request -> an.rw.writeData("list", "Product2G", null, qp0, request, an::log) );
        int timesForItem = 0;
        for(Object[] pieces : an.itemTimestamp) {
        	if(prevId != null && !prevId.equals((String)pieces[0])) {
        		if(timestamps.size() > 1) {
        			if(noFirstDateApprovedItem.contains(prevId)) {
        				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevId + "'@1")).put("values", new org.json.JSONArray().put(an.formatTimestamp((Long)timestamps.get(0)))));
        				timesForItem++;
        			}
        		}
        		timestamps.clear();
        	}
        	timestamps.add((Long)pieces[1]);
        	prevId = (String) pieces[0];
        }
        if(timestamps.size() > 1) {
			if(noFirstDateApprovedItem.contains(prevId)) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevId + "'@1")).put("values", new org.json.JSONArray().put(an.formatTimestamp((Long)timestamps.get(0)))));
			}
		}
        rh.sendData();
		timestamps.clear();
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Total products fda: " + timesForProduct);
        an.log("Total vars fda: " + timesForItem);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private Integer procesaContent(String contenido, SAXParser parser) throws SAXException, java.io.IOException {
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        java.util.List<Product> finished = null;
        Handler handler = new Handler();
        try {
        	parser.parse(new java.io.ByteArrayInputStream(contenido.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)), handler);
        	finished = handler.getFinished();
	        refProductsCount += handler.getPrductsCounter();
	        for(Product p : finished) {
	        	processProduct(p, in);
	        }
        }catch(org.xml.sax.SAXParseException e) {
        	log("Problem processing content");
        }
        log("Parsing files took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
        return refProductsCount;
    }
    
    private String formatTimestamp(Long timestamp) {
    	return formatter.format( java.time.Instant.ofEpochMilli(timestamp) );
    }
    
    private Integer procesaDirectorio(String dir, java.util.List<Object[]> data, SAXParser parser) throws SAXException, java.io.IOException {
    	log("Checking: " + dir);
    	java.io.File[] files = new java.io.File(dir).listFiles();
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        log("Now reading files... " + files.length);
        java.util.List<Product> finished = null;
        java.util.regex.Pattern numP = java.util.regex.Pattern.compile("[0-9]+");
        java.util.regex.Matcher m = null;
        Long timestamp = null;
        for(File input : files) {
        	if(input.isDirectory()) {
        		refProductsCount += procesaDirectorio(input.getAbsolutePath(), data, parser);
        	}else {
        		m = numP.matcher(input.getName());
        		if(m.find()) {
        			data.add(new Object[] { timestamp = Long.parseLong(m.group()), input.getAbsolutePath() });
			        Handler handler = new Handler();
			        try {
			        	parser.parse(input, handler);
			        	finished = handler.getFinished();
				        refProductsCount += handler.getPrductsCounter();
				        for(Product p : finished) {
				        	processProduct(p, timestamp);
				        }
			        }catch(org.xml.sax.SAXParseException e) {
			        	log("Problem processing following file: " + input.getAbsolutePath());
			        }
        		}
        	}
        }
        log("Parsing files took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
        return refProductsCount;
    }

    private int lacuenta = 0;
    private int lacuentaVars = 0;
    private final java.util.List<Object[]> productTimestamp = new java.util.ArrayList<>();
    private final java.util.List<Object[]> itemTimestamp = new java.util.ArrayList<>();

    private void processProduct(Product product, Long timestamp) {
    	productTimestamp.add(new Object[] { product.getId(), timestamp });
    	java.util.LinkedList<Product> children = null;
    	children = product.getProducts();
    	if(children != null && !children.isEmpty()) {
    		for(Product child : children) {
    			itemTimestamp.add(new Object[] { child.getId(), timestamp });
    			lacuentaVars++;
    		}
    	}
    	lacuenta++;
    	if(lacuenta % 10000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
    		}
    	}
    }
    
	private final java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?<=Flujo Actual: )([^|]+)");
	private final java.util.regex.Pattern p0 = java.util.regex.Pattern.compile("(?<=Estado en el WF: )([^|]+)");
	
	public String getFlujoActual(String calculatedWFAtt) {
		if(calculatedWFAtt != null) {
			java.util.regex.Matcher m = p.matcher(calculatedWFAtt);
			if(m.find()) {
				return m.group(1);
			}
		}
		return null;
	}
	
	public String getEstadoEnElWF(String calculatedWFAtt) {
		if(calculatedWFAtt != null) {
			java.util.regex.Matcher m = p0.matcher(calculatedWFAtt);
			if(m.find()) {
				return m.group(1);
			}
		}
		return null;
	}
	private static final Logger LOGGER = Logger.getLogger(P360ActiveMQBPMStage.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/collectSentTimestamps-%g.log", 5 * 1024 * 1024, 5, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    java.time.LocalDateTime dateTime =
                        java.time.Instant.ofEpochMilli(record.getMillis())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();

                    String timestamp = dateTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }
	
	
	private void log(String message) {
		LOGGER.info(message);
//        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//                new java.io.FileOutputStream(normalLogFilePath.toString(), true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "] " + message);
//        } catch (java.io.IOException e) {
//        }
    }

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/collectSentTimestamps.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
