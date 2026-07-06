package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandlerIDsCollector {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath  = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_to_collect_ids.log");
	private final java.util.Set<String> ids = new java.util.TreeSet<>();
	private final java.util.Set<String> idsV = new java.util.TreeSet<>();

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
    	private long productsCounter = 0l;
    	private boolean assetName = false;

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
	                		attributeIDs.add(attributeId);
	                	}
	                	String valueId = attributes.getValue("ID");
	                	String unidadId = attributes.getValue("UnitID");
	                	Value value = new Value(attributeId, valueId, unidadId);
	                	product.prepareValue(value);
	                } else if( "MultiValue".equals(name) && product.getMultiValues() != null ) {
	                	String attributeId = attributes.getValue("AttributeID");
	                	if(attributeId != null) {
	                		attributeIDs.add(attributeId);
	                	}
	                	MultiValue multiValueList = new MultiValue(attributeId);
	                	product.prepareMultiValue(multiValueList);
	                } else if("ClassificationReference".equals(name)) {
	                	String classificationId = attributes.getValue("ClassificationID");
	                	String type = attributes.getValue("Type");
	                	Classification classification = new Classification(classificationId, type);
	                	product.prepareClassification(classification);
	                }
	            }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (!productStack.isEmpty()) {
                Product product = productStack.getLast();
                Value workingValue = product.getWorkingValue();
                if(workingValue != null) {
                	workingValue.setText( new StringBuilder().append(ch, start, length).toString() );
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
                } else if ("Product".equals(name)) {
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
        
        public long getPrductsCounter() {
        	return productsCounter;
        }

        public List<Product> getFinished() {
            return finished;
        }
        
        public java.util.Map<String, Asset> getAssetMap(){
        	return assetMap;
        }
    }
    
    // Entry point
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandlerIDsCollector an = new AnotherXMLHandlerIDsCollector();
    	java.util.Set<String> currentProducts = new java.util.TreeSet<>();
    	if (args.length == 0) {
            System.err.println("Usage: java AnotherXMLHandlerIDsCollector <file.xml>");
            System.exit(1);
        }
        long in = System.currentTimeMillis();
        an.log("Now reading files...");
        int total = an.processDirContent(args[0], currentProducts);
        java.util.Set<String> padresQueFaltan = new java.util.TreeSet<>();
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("DataRecibida.csv").toFile())))){
	        pw.println( an.rw.getRw().serializeChunk(new Object[] { "ID", "Padre", "STEPUserType" }) );
        	for( java.util.Map.Entry<String, String> entry : an.childParentRel.entrySet() ) {
        		pw.println( an.rw.getRw().serializeChunk( new Object[] { entry.getKey(), entry.getValue(), an.idConUserType.get(entry.getKey()) } ) );
	        	if(!currentProducts.contains(entry.getValue())) {
	        		if(!entry.getValue().startsWith("EU") && !entry.getValue().startsWith("UnCatLevel") && !entry.getValue().startsWith("Rejected") && !entry.getValue().startsWith("L") && !entry.getValue().startsWith("M")) {
	        			padresQueFaltan.add(entry.getValue());
	        		}
	        	}
	        }
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("PadresQueNoEstaban.csv").toFile())))){
        	padresQueFaltan.forEach(pw::println);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        an.log("Parsing files took: " + an.rw.getRw().formatTime(System.currentTimeMillis() - in));
        an.log("Total products found: " + an.ids.size());
        an.log("Total variants found: " + an.idsV.size());
        an.log("Done, found: " + total + " products.");
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private int processDirContent(String dir, java.util.Set<String> alreadyProcessed) throws ParserConfigurationException, SAXException, FileNotFoundException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
    	java.io.File[] files = new java.io.File(dir).listFiles();
    	int processedProducts = 0;
    	log("Going on " + dir + " (" + files.length + ")");
    	for(java.io.File f : files) {
    		if(f.isDirectory()) {
    			processDirContent(f.getAbsolutePath(), alreadyProcessed);
    		}else {
    			Handler handler = new Handler();
    	        try {
    		        parser.parse( new java.io.FileInputStream(f), handler);
    		        for (Product product : handler.getFinished()) {
    	        		processProduct(product, alreadyProcessed);
    		        }
    		        processedProducts += handler.getPrductsCounter();
    	        }catch(org.xml.sax.SAXParseException e) {
    	        	log("Problem processing following file: " + f.getName());
    	        }
    		}
    	}
    	return processedProducts;
    }
    
    private final java.util.Map<String, String> childParentRel = new java.util.HashMap<>();
    private final java.util.Map<String, String> idConUserType = new java.util.HashMap<>();
    
    private void processProduct(Product product, java.util.Set<String> alreadyInSystem) {
//    	if(alreadyInSystem.contains(product.getId())) {
//    		return;
//    	}
    	alreadyInSystem.add(product.getId());
    	java.util.LinkedList<Product> children = null;
    	children = product.getProducts();
    	if(product.getUserTypeId().startsWith("SalesItemFamily") ){
    		ids.add(product.getId());
    	}else if(product.getUserTypeId().equals("SalesItemVariant")) {
    		idsV.add(product.getId());
    	}else if("SalesItem".equals(product.getUserTypeId())) {
    		if(product.getParentId().startsWith("EU") || product.getParentId().startsWith("UnCatLevel")) {
    			ids.add(product.getId());
    		}else {
    		}
    		idsV.add(product.getId());
    	}else {
    		System.out.println("Not a known userTypeID: " + product.getUserTypeId());
    	}
//    	if(!product.getParentId().startsWith("EU") && !product.getParentId().startsWith("UnCatLevel") && !product.getParentId().startsWith("Rejected") && !product.getParentId().startsWith("L") && !product.getParentId().startsWith("M")) {
		childParentRel.put(product.getId(), product.getParentId());
//    	}
    	idConUserType.put(product.getId(), product.getUserTypeId());
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, alreadyInSystem);
    		}
    	}
    }
    
	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(normalLogFilePath.toString(), true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "] " + message);
        } catch (java.io.IOException e) {
        }
    }
}
