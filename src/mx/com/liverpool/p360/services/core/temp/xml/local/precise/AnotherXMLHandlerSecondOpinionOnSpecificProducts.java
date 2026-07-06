package mx.com.liverpool.p360.services.core.temp.xml.local.precise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.example.ei.forfun.logic.WildDateStandardizer;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class AnotherXMLHandlerSecondOpinionOnSpecificProducts {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "list_api_precise.log");

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
                Value workingValue = product.getWorkingValue();
                if(workingValue != null) {
                	StringBuilder sb = new StringBuilder();
            		sb.append(workingValue.getText() == null ? "" : workingValue.getText());
            		sb.append(ch, start, length);
            		workingValue.setText( sb.toString() );
                }
                if(gettingName) {
                	StringBuilder sb = new StringBuilder();
            		sb.append(product.getName() == null ? "" : product.getName());
            		sb.append(ch, start, length);
            		product.setName(sb.toString());
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
            			StringBuilder sb = new StringBuilder();
                		sb.append(a.getName() == null ? "" : a.getName());
                		sb.append(ch, start, length);
            			a.setName( sb.toString() );
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
    
    
    private static final java.util.Map<String, String[]> data = new java.util.HashMap<>();
    
    public static int processContent(String content) throws SAXException, IOException, ParserConfigurationException {
    	AnotherXMLHandlerSecondOpinionOnSpecificProducts an = new AnotherXMLHandlerSecondOpinionOnSpecificProducts();
		LoadProductDataRemainingFieldsOnSpecificProducts elp = new LoadProductDataRemainingFieldsOnSpecificProducts();
    	an.qp.put("includeObjectsInProtocol", "false");
		an.loadItemGroups();
		int a = an.procesaArchivoYProducto(content);
		elp.processContent(content);
        elp.processRemaining();
        return a;
    }
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandlerSecondOpinionOnSpecificProducts an = new AnotherXMLHandlerSecondOpinionOnSpecificProducts();
    	an.qp.put("includeObjectsInProtocol", "false");
    	if(args.length < 1) {
    		System.out.println("Need to pass as first argument a file with a list with IDs to be processed among those files.");
    	}else {
    		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("IDX")))){
    			String line = null;
    			String[] pieces = null;
    			String[] ternas = null;
    			while((line = br.readLine()) != null) {
    				pieces = an.rw.getRw().parseLine(line);
    				if(pieces.length > 1) {
    					ternas = an.rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
    					if(ternas.length > 0) {
    						data.put(pieces[0], ternas);
    					}
    				}
    			}
    		}
    		an.loadItemGroups();
    		LoadProductDataRemainingFieldsOnSpecificProducts elp = new LoadProductDataRemainingFieldsOnSpecificProducts();
    		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(args[0])))){
    			String line = null;
    			String[] ternas = null;
    			String[] pieces = null;
    			while((line = br.readLine()) != null) {
    				ternas = data.get(line);
    				System.out.println("Busqué: " + line);
    				if(ternas != null) {
    					for(int m=0; m<ternas.length; m++) {
	    					pieces = an.rw.getRw().parseLine(ternas[m], "\"", "|", "\\");
	    					an.log("Processing: " + pieces[2] + " (" + line + ")");
	    					an.procesaArchivoYProducto(pieces[2], line);
	    					elp.process(pieces[2]);
	    			        elp.processRemaining();
    					}
    				}else {
    					System.out.println("Not found.");
    				}
    			}
    		}
	    	
	        an.log("Total products found: " + an.lacuenta);
	        an.log("Total vars found: " + an.lacuentaVars);
    	}
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private int procesaArchivoYProducto(String content) throws ParserConfigurationException, SAXException {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        Handler handler = new Handler();
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        java.util.List<Product> finished = null;
        try {
        	parser.parse( new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)) , handler);
        	finished = handler.getFinished();
	        refProductsCount += handler.getPrductsCounter();
	        for(Product p : finished) {
	        	processProduct(p);
	        }
        }catch(org.xml.sax.SAXParseException e) {
        	log("Problem processing content");
        } catch (IOException e) {
        	log("Problem processing content");
        	logE(e);
		}
        log("Parsing files took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
		if(rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp, request, this::log);
		}
		rw.writeData("list", "Product2G", "Product2GStructureMap", qp, requestStructureGroup, System.out::println);
		reqPID.sendData();
		reqAID.sendData();
		reqArticleAltID.sendData();
		reqAltID.sendData();
		reqProductStatus.sendData();
		reqSuppressSKUAndEAN.sendData();
		reqArticleStatus.sendData();
		reqSuppressSKUAndEANArticle.sendData();
        org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
        org.json.JSONArray rows = new org.json.JSONArray();
        org.json.JSONObject request = new org.json.JSONObject();
        request.put("columns", columns);
        request.put("rows", rows);
        for(java.util.Map.Entry<String, String> entry : childParent.entrySet()) {
        	rows.put(
        			new org.json.JSONObject()
        				.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
        				.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue()))
        				.put("values", new org.json.JSONArray().put(entry.getValue())));
        	if(rows.length() == 5000) {
        		rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
        		while(rows.length() > 0) {
        			rows.remove(0);
        		}
        	}
        }
        if(rows.length() > 0) {
        	rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
    		while(rows.length() > 0) {
    			rows.remove(0);
    		}
        }
        return refProductsCount;
    }
    
    private void procesaArchivoYProducto(String file, String productId) throws ParserConfigurationException, SAXException {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        Handler handler = new Handler();
        Integer refProductsCount = 0;
        java.util.List<Product> finished = null;
        try {
        	parser.parse(new java.io.File(file), handler);
        	finished = handler.getFinished();
	        refProductsCount += handler.getPrductsCounter();
	        System.out.println("Products from file: " + file);
	        for(Product p : finished) {
	        	if(p.getId().equals(productId))
	        		processProduct(p);
	        }
        }catch(org.xml.sax.SAXParseException e) {
        	log("Problem processing following file: " + file);
        } catch (IOException e) {
			e.printStackTrace();
		}
		if(rows.length() > 0) {
			rw.writeData("list", "Product2G", null, qp, request, this::log);
		}
		rw.writeData("list", "Product2G", "Product2GStructureMap", qp, requestStructureGroup, System.out::println);
		reqPID.sendData();
		reqAID.sendData();
		reqArticleAltID.sendData();
		reqAltID.sendData();
		reqProductStatus.sendData();
		reqSuppressSKUAndEAN.sendData();
		reqArticleStatus.sendData();
		reqSuppressSKUAndEANArticle.sendData();
        org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
        org.json.JSONArray rows = new org.json.JSONArray();
        org.json.JSONObject request = new org.json.JSONObject();
        request.put("columns", columns);
        request.put("rows", rows);
        for(java.util.Map.Entry<String, String> entry : childParent.entrySet()) {
        	rows.put(
        			new org.json.JSONObject()
        				.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
        				.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue()))
        				.put("values", new org.json.JSONArray().put(entry.getValue())));
        	if(rows.length() == 5000) {
        		rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
        		while(rows.length() > 0) {
        			rows.remove(0);
        		}
        	}
        }
        if(rows.length() > 0) {
        	rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
    		while(rows.length() > 0) {
    			rows.remove(0);
    		}
        }
    }
    

    private java.util.Map<String, String> loadExternalStatusMap() {
    	java.util.Map<String, String> internalToExternalStatusMap = new java.util.HashMap<>();
    	try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", "ExternalStatus").toFile())))){
    		String line = null;
    		String[] pieces = null;
    		while((line = br.readLine()) != null) {
    			pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
    			internalToExternalStatusMap.put(pieces[0], pieces[1]);
    		}
    	}catch(java.io.IOException e) {
    		logE(e);
    	}
    	return internalToExternalStatusMap;
    }

    private int lacuenta = 0;

    private final PruebaEnvioPubSubMediaAssets elese = new PruebaEnvioPubSubMediaAssets();
    
    private final java.util.Map<String, String> internalToExternalStatusMap = loadExternalStatusMap();
    
    private final RequestHandler reqProductStatus = new RequestHandler( new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnriquecidoEnForo',root,\"0000.0000.RK\",'EnriquecidoEnForo',-1)"))
    		, 1000, request -> rw.writeData("list", "Product2G", null, qp, request, this::log));

    private final RequestHandler reqArticleStatus = new RequestHandler( new org.json.JSONArray()
    		.put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))
    		.put(new org.json.JSONObject().put("identifier", "Article.PrevStatus"))
    		.put(new org.json.JSONObject().put("identifier", "Article.ExternalStatus"))
    		, 1000, request -> rw.writeData("list", "Article", null, qp, request, this::log));

    private final RequestHandler reqSuppressSKUAndEAN = new RequestHandler( new org.json.JSONArray()
    			.put(new org.json.JSONObject().put("identifier", "Product2G.SKU"))
    			.put(new org.json.JSONObject().put("identifier", "Product2G.EAN"))
    			.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
    			.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
    			.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"))
    		, 1000, request -> rw.writeData("list", "Product2G", null, qp, request, this::log));
    
    private final RequestHandler reqSuppressSKUAndEANArticle = new RequestHandler( new org.json.JSONArray()
    		.put(new org.json.JSONObject().put("identifier", "Article.SKU"))
    		.put(new org.json.JSONObject().put("identifier", "Article.EAN"))
    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"))
    		, 1000, request -> rw.writeData("list", "Product2G", null, qp, request, this::log));
    
    private final RequestHandler reqAltID = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.AltProductNo")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, this::log));
    private final RequestHandler reqArticleAltID = new RequestHandler(  new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SupplierAltAID")), 1000, request -> rw.writeData("list", "Article", null, qp, request, this::log) );
    private final RequestHandler reqPID = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.ProductNo")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, this::log));
    private final RequestHandler reqAID = new RequestHandler(  new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SupplierAID")), 1000, request -> rw.writeData("list", "Article", null, qp, request, this::log) );
    
    private final org.json.JSONArray rows = new org.json.JSONArray();
    private final org.json.JSONArray columns = new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('PrimaryProductTaxonomy')"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Business',root,\"0000.0000.RK\",'Business',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.Business"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.SKU"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.EAN"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.EmbeddedCodeWAP"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.EmbeddedCodeWEB"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.RefundPolicy"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FirstDateApprove',root,\"0000.0000.RK\",'FirstDateApprove',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GLang.ProductName(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionShort(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionLong(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionLong2(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Direccion(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Section(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.ItemGroup(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.ItemGroupS4H(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.BrandName(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.BRAND_ID_S4H(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Negocio(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SAPObjectType(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierID(MX)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierPartNumber(MX)"))

			;
    private final org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows", rows);
    private final org.json.JSONArray columnsArticle = new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "Article.SKU"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.EAN"))
				.put(new org.json.JSONObject().put("identifier", "Article.Business"))
				.put(new org.json.JSONObject().put("identifier", "ArticleLang.DescriptionShort(es)"))
				
				.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.TamanoUnico(MX)"))
				.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)"))
				.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SupplierPartNumber(MX)"))
				.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)"))
    		;
    private final org.json.JSONArray rowsArticle = new org.json.JSONArray();
    private final org.json.JSONObject requestArticle = new org.json.JSONObject().put("columns", columnsArticle).put("rows", rowsArticle);
    private final java.util.Map<String, String> childParent = new java.util.HashMap<>();
    private int lacuentaVars = 0;

	
	private static final java.util.Set<String> IDENTITY_CHARACTERISTICS =
	        new java.util.HashSet<>(java.util.Arrays.asList(
	                "SKU",
	                "MainBarCode",
	                "MainBarCodeS4H"
	        ));

	private static final java.util.Set<String> VARIANT_MATCH_CHARACTERISTICS =
	        new java.util.HashSet<>(java.util.Arrays.asList(
	                "SKU",
	                "MainBarCode",
	                "MainBarCodeS4H",
	                "ColoursLiverpoolAtt",
	                "TamanoUnico",
	                "SupplierPartNumber"
	        ));

    private void processProduct(Product product) {
    	if(product.getParentId().matches("^(S?[0-9]+)")) {
    		System.out.println("Went away");
    		return;
    	}
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	String currentStatus = null;
    	String prevStatus = null;
    	String externalStatus = null;
    	String business = null;
		String externalId = product.getId();
		String internalId = null;
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    			System.out.println(value.getAttributeId() + " - " + value.getText());
    		}
    		org.json.JSONArray vals = new org.json.JSONArray();
    		Value extwgS4h = valMap.get("EXTWG_S4H");
    		Value firstDateApprove = valMap.get("FirstDateApprove");
    		Value negocio = valMap.get("Negocio");
    		Value descriptionLong = valMap.get("DescriptionLong");
    		Value descriptionLong2 = valMap.get("DescriptionLong2");
    		Value productName = valMap.get("ProductName");
    		Value sku = valMap.get("SKU");
    		Value ean = valMap.get("MainBarCode");
    		Value ean2 = valMap.get("MainBarCodeS4H");
    		Value embedCodeWAP = valMap.get("EmbedCodeWAP");
    		Value embedCodeWEB = valMap.get("EmbedCodeWEB");
    		Value refundPolicy = valMap.get("refundPolicy");
    		Value direction = valMap.get("Direction");
    		Value section = valMap.get("Section");
    		Value itemGroup = valMap.get("ItemGroup");
    		Value itemGroupS4H = valMap.get("ItemGroupS4H");
    		Value brandName = valMap.get("BrandName");
    		Value brandIdS4H = valMap.get("BRAND_ID_S4H");
    		Value sapObjectType = valMap.get("SAPObjectType");
    		Value supplierID = valMap.get("SupplierID");
    		Value supplierPartNumber = valMap.get("SupplierPartNumber");
    		String firstdateapproveStr = firstDateApprove == null ? "" : firstDateApprove.getId() != null ? firstDateApprove.getId() : firstDateApprove.getText() == null ? "" : firstDateApprove.getText() ;
    		String descriptionLongStr = descriptionLong == null ? "" : descriptionLong.getText() ;
    		String descriptionLong2Str = descriptionLong2 == null ? "" : descriptionLong2.getText() ;
    		String nameStr = product.getName();
    		String productNameStr = productName == null ? nameStr : "".equals(productName.getText()) ? nameStr : productName.getText() ;
    		business = determineBusiness(negocio == null || negocio.getText() == null ? "" : negocio.getText(), extwgS4h == null || extwgS4h.getText() == null ? "" : extwgS4h.getText());
    		System.out.println("ProductName: " + productNameStr);
    		System.out.println("ID: " + product.getId());
    		System.out.println( "--> " + product.getName() );
    		System.out.println("SKU: " + (sku == null ? "NoSKU" : sku.getText()));
    		String wdspr = WildDateStandardizer.normalize(firstdateapproveStr, java.time.ZoneId.of("America/Mexico_City"), WildDateStandardizer.AmbiguityPolicy.PREFER_DMY).orElse("");
    		
    		DataRequestor dr = new DataRequestor();
    		if(sku != null && sku.getText() != null && !"".equals(sku.getText())) {
	    		String rsp = dr.getProductBySKU(new org.json.JSONArray().put(sku.getText()));
	    		org.json.JSONObject jr = new org.json.JSONObject(rsp);
	    		org.json.JSONArray items = jr.getJSONArray("items");
	    		String item = items.getString(0);
	    		if(!"".equals(item)) {
	    			if(!item.equals(externalId)) {
	    				rsp = dr.getProductData(new org.json.JSONArray().put(externalId));
	    				boolean skip = false;
	    				if(rsp != null) {
	    					org.json.JSONObject r0 = new org.json.JSONObject(rsp);
	    					org.json.JSONArray a0 = r0.getJSONArray("items");
	    					org.json.JSONObject i0 = a0.getJSONObject(0);
	    					skip = ! (
	    							   "".equals(i0.getString("Section")) 
	    							&& "".equals(i0.getString("ItemGroup")) 
	    							&& "".equals(i0.getString("ItemGroupS4H")) 
	    							&& "".equals(i0.getString("BrandName")) 
	    							&& "".equals(i0.getString("BRAND_ID_S4H")) 
	    							&& "".equals(i0.getString("Business")) 
	    							&& "".equals(i0.getString("SKU")) 
	    							&& "".equals(i0.getString("SupplierID")) 
	    							&& "".equals(i0.getString("Template")) 
	    							&& "".equals(i0.getString("CurrentStatus")) 
	    							&& "".equals(i0.getString("AssignTakeNoTake")) 
	    							&& "".equals(i0.getString("SAPObjectType")) 
	    							&& "".equals(i0.getString("FotoTomadaLiverpool")) 
	    							&& "".equals(i0.getString("MainBarCode")) 
	    							&& "".equals(i0.getString("MainBarCodeS4H")) 
	    							&& "".equals(i0.getString("SupplierPartNumber")) 
	    						);
	    				}
	    				if(!skip) {
	    					if(item.length() < 15) {
			    				java.util.Map<String, String> qp = new java.util.HashMap<>();
			    				qp.put("items", "'" + item + "'@1");
			    				String[] data = new String[1];
			    				data[0] = null;
			    				rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			    					data[0] = row.getJSONObject("object").getString("id");
			    				});
			    				internalId = data[0];
			    				reqAltID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId)).put("values", new org.json.JSONArray().put(item)));
			    				reqPID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId)).put("values", new org.json.JSONArray().put(externalId)));
			    				dr.skuProductNo(new org.json.JSONArray().put(new org.json.JSONObject().put("productNo", externalId).put("sku", sku.getText())));
	    					}else {
	    						reqAltID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1")).put("values", new org.json.JSONArray().put(externalId)));
	    						reqAltID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(item)));
	    					}
	    				}else {
	    					if(item.length() < 15) {
	    						reqSuppressSKUAndEAN.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1" )).put("values", new org.json.JSONArray().put("").put("").put("").put("").put("")));
	    					} else {
	    						// Merge on P360 product data comming from there
	    						resuelveCombinación(item, externalId);
	    						externalId = item;
	    					}
	    				}
	    			}
	    		}
    		}
    		String rsp = dr.getProductData(new org.json.JSONArray().put(externalId));
    		if(rsp != null) {
    			org.json.JSONObject jr = new org.json.JSONObject(rsp);
    			org.json.JSONArray items = jr.getJSONArray("items");
    			org.json.JSONObject item = items.getJSONObject(0);
    			if(item.has("CurrentStatus") && "".equals(item.getString("CurrentStatus"))) {
    	    		Value calculatedWFAtt = valMap.get("CalculatedWF_Att");
    	    		Value fotoTomadaLiverpool = valMap.get("FotoTomadaLiverpool");
    	    		Value stateSKU = valMap.get("StateSKU");
    	    		String[] bundle = elese.computeStatus(calculatedWFAtt == null ? "" : calculatedWFAtt.getText(), !"".equals(firstdateapproveStr) ? "Aprobado" : (stateSKU == null ? "" : stateSKU.getText()), fotoTomadaLiverpool == null ? "" : fotoTomadaLiverpool.getText(), product.getId());
    	    		currentStatus = bundle[0];
    	    		prevStatus = bundle[1];
    	    		String enriquecidoEnForo = bundle[2];
    	    		externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
    				reqProductStatus.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id",  internalId == null ? "'" + externalId + "'@1" : internalId )).put("values", new org.json.JSONArray().put(currentStatus).put(prevStatus).put(externalStatus).put(enriquecidoEnForo)));
    			}
    			
    		}
    		
    		vals.put( new org.json.JSONArray().put( product.getParentId() ));
    		vals.put( business );
    		vals.put( business );
    		vals.put( sku == null ? "" : sku.getText() );
    		vals.put( ean != null ? ean.getText() : ean2 != null ? ean2.getText() : "" );
    		vals.put( embedCodeWAP == null ? "" : embedCodeWAP.getText() );
    		vals.put( embedCodeWEB == null ? "" : embedCodeWEB.getText() );
    		vals.put( refundPolicy == null ? "" : refundPolicy.getText() );
    		vals.put( wdspr );
    		vals.put( productNameStr == null || "".equals(productNameStr) ? nameStr : productNameStr );
    		vals.put( nameStr );
    		vals.put( descriptionLongStr );
    		vals.put( descriptionLong2Str );
    		
    		vals.put( direction == null ? "" : direction.getId() );
    		vals.put( section == null ? "" : section.getId() );
    		vals.put( itemGroup == null ? "" : itemGroup.getId() );
    		vals.put( itemGroupS4H == null ? "" : itemGroupS4H.getId() );
    		vals.put( brandName == null ? "" : brandName.getId() );
    		vals.put( brandIdS4H == null ? "" : brandIdS4H.getId() );
    		vals.put( negocio == null ? "" : negocio.getId() );
    		vals.put( sapObjectType == null ? "" : sapObjectType.getId() );
    		vals.put( supplierID == null ? "" : supplierID.getId() == null ? supplierID.getText() : supplierID.getId() );
    		vals.put( supplierPartNumber == null ? "" : supplierPartNumber.getText() );
    		System.out.println("---> " + vals);
    		collectClassifications(product.getClassifications(), product.getId());
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", ( internalId == null ?  "'" + externalId + "'@1" : internalId ))).put("values", vals));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Product2G", null, qp, request, this::log);
    			while(rows.length() > 0) {
    				rows.remove(0);
    			}
    			rw.writeData("list", "Product2G", "Product2GStructureMap", qp, requestStructureGroup, this::log);
    			while(rowsStructureGroupMap.length() > 0) {
    				rowsStructureGroupMap.remove(0);
    			}
    		}
    	}
    	if(children != null && !children.isEmpty()) {
    		for(Product child : children) {
    			processChild(child, business, currentStatus, prevStatus, externalStatus, externalId);
    		}
    	}else {
    		processChild(product, business, currentStatus, prevStatus, externalStatus, externalId);
    	}
    	lacuenta++;
    	if(lacuenta % 10000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
    		}
    	}
    }
    
    private void processChild(Product child, String business, String currentStatus, String prevStatus, String externalStatus, String parentExternalId) {
    	
    	java.util.List<Value> values = child.getValues();
    	java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
		for(Value value : values) {
			valMap.put(value.getAttributeId(), value);
		}
		org.json.JSONArray vals = new org.json.JSONArray();
		Value coloursLiverpoolAtt = valMap.get("ColoursLiverpoolAtt");
		Value mainbarcode = valMap.get("MainBarCode");
		Value mainbarcodes4h = valMap.get("MainBarCodeS4H");
		Value name = valMap.get("Name");
		Value sapobjecttype = valMap.get("SAPObjectType");
		Value sku = valMap.get("SKU");
		Value supplierpartnumber = valMap.get("SupplierPartNumber");
		Value tamanounico = valMap.get("TamanoUnico");
		
		String coloursLiverpoolAttStr = coloursLiverpoolAtt == null ? "" : coloursLiverpoolAtt.getId() != null ? coloursLiverpoolAtt.getId() : coloursLiverpoolAtt.getText() == null ? "" : coloursLiverpoolAtt.getText() ;
		String mainbarcodeStr = mainbarcode == null ? "" : mainbarcode.getId() != null ? mainbarcode.getId() : mainbarcode.getText() == null ? "" : mainbarcode.getText() ;
		String mainbarcodes4hStr = mainbarcodes4h == null ? "" : mainbarcodes4h.getId() != null ? mainbarcodes4h.getId() : mainbarcodes4h.getText() == null ? "" : mainbarcodes4h.getText() ;
		String nameStr = name == null ? "" : name.getId() != null ? name.getId() : name.getText() == null ? "" : name.getText() ;
		String sapobjecttypeStr = sapobjecttype == null ? "" : sapobjecttype.getId() != null ? sapobjecttype.getId() : sapobjecttype.getText() == null ? "" : sapobjecttype.getText() ;
		String skuStr = sku == null ? "" : sku.getId() != null ? sku.getId() : sku.getText() == null ? "" : sku.getText() ;
		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
		String tamanounicoStr = tamanounico == null ? "" : tamanounico.getId() != null ? tamanounico.getId() : tamanounico.getText() == null ? "" : tamanounico.getText() ;

		String externalId = child.getId();
		String internalId = null;
		if(sku != null && sku.getText() != null && !"".equals(sku.getText())) {
    		DataRequestor dr = new DataRequestor();
    		String rsp = dr.getProductBySKU(new org.json.JSONArray().put(sku.getText()));
    		org.json.JSONObject jr = new org.json.JSONObject(rsp);
    		org.json.JSONArray items = jr.getJSONArray("items");
    		String item = items.getString(0);
    		if(!"".equals(item)) {
    			if(!item.equals(externalId)) {
    				rsp = dr.getArticleData(new org.json.JSONArray().put(externalId));
    				boolean skip = false;
    				if(rsp != null) {
    					org.json.JSONObject r0 = new org.json.JSONObject(rsp);
    					org.json.JSONArray a0 = r0.getJSONArray("items");
    					org.json.JSONObject i0 = a0.getJSONObject(0);
    					skip = ! (
    							   "".equals(i0.getString("ProductNo")) 
    							&& "".equals(i0.getString("ColoursLiverpoolAtt")) 
    							&& "".equals(i0.getString("TamanoUnico")) 
    							&& "".equals(i0.getString("ProductImage")) 
    							&& "".equals(i0.getString("AssignTakeNoTake")) 
    							&& "".equals(i0.getString("SKU")) 
    							&& "".equals(i0.getString("MainBarCode")) 
    							&& "".equals(i0.getString("MainBarCodeS4H")) 
    							&& "".equals(i0.getString("SupplierPartNumber")) 
    						);
    				}
    				if(!skip) {
    					if(item.length() < 15) {
    						java.util.Map<String, String> qp = new java.util.HashMap<>();
    						qp.put("items", "'" + item + "'@1");
    						String[] data = new String[1];
    						data[0] = null;
    						rw.collectData("list", "Article", null, "byItems", qp, row -> {
    							data[0] = row.getJSONObject("object").getString("id");
    						});
    						internalId = data[0];
    						reqArticleAltID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId)).put("values", new org.json.JSONArray().put(item)));
    						reqAID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId)).put("values", new org.json.JSONArray().put(externalId)));
    						dr.putSkuSupplierAID(new org.json.JSONArray().put(new org.json.JSONObject().put("supplierAID", externalId).put("productNo", parentExternalId).put("sku", sku.getText())));
    					}else {
    						reqArticleAltID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1")).put("values", new org.json.JSONArray().put(externalId)));
    						reqArticleAltID.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(item)));
    					}
    				}else {
    					if(item.length() < 15) {
    						reqSuppressSKUAndEANArticle.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + item + "'@1" )).put("values", new org.json.JSONArray().put("").put("").put("").put("").put("")));
    					} 
    				}
    			}
    		}
		}
		
		if(currentStatus != null && prevStatus != null && externalStatus != null) {
			reqArticleStatus.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId == null ? "'" + externalId + "'@1" : internalId)).put("values", new org.json.JSONArray().put(currentStatus).put(prevStatus).put(externalStatus)));
		}
		
		vals.put(skuStr);
		vals.put("".equals( mainbarcodeStr ) ? mainbarcodes4hStr : mainbarcodeStr);
		vals.put(business);
		vals.put(nameStr);
		vals.put(tamanounicoStr);
		vals.put(coloursLiverpoolAttStr);
		vals.put(supplierpartnumberStr);
		vals.put(sapobjecttypeStr);
		rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internalId == null ? "'" + externalId + "'@1" : internalId)).put("values", vals));
		if(rowsArticle.length() == 5000) {
			rw.writeData("list", "Article", null, qp, requestArticle, this::log);
			while(rowsArticle.length() > 0) {
				rowsArticle.remove(0);
			}
		}
		
		childParent.put(child.getId(), !child.getParentId().matches("^(S?[0-9]+)") ? child.getId() : child.getParentId());
		lacuentaVars++;
    }
    
    private void resuelveCombinación(String id1, String id2) {
		if(id1 != null && id2 != null && !id1.equals(id2)) {
			org.json.JSONObject response1 = rw.getRw().makeRequest("GET", "/object/Product2G/'" + rw.getRw().encode(id1) + "'@1?includeIds=true&includeLabels=true");
			org.json.JSONObject response2 = rw.getRw().makeRequest("GET", "/object/Product2G/'" + rw.getRw().encode(id2) + "'@1?includeIds=true&includeLabels=true");
			
			org.json.JSONObject data1 = response1 != null && response1.has("_data") ? response1.getJSONObject("_data") : null;
			org.json.JSONObject data2 = response2 != null && response2.has("_data") ? response2.getJSONObject("_data") : null;
			
			if(data1 != null && data2 != null) {
				
				/**********************************************************
				 * 
				 * Hacer la lógica de comparación y merge y todo en data1
				 * 
				 ***************************************************************/
				
				java.util.List<String> itemsOfProduct1 = collectArticleObjectIdsByProduct(id1);
			    java.util.List<String> itemsOfProduct2 = collectArticleObjectIdsByProduct(id2);

			    java.util.List<VariantInfo> variants1 = loadVariantInfos(itemsOfProduct1, "id1");
			    java.util.List<VariantInfo> variants2 = loadVariantInfos(itemsOfProduct2, "id2");

			    java.util.Map<String, java.util.List<VariantInfo>> signatureIndex = buildSignatureIndex(variants1, variants2);

			    VariantMergeDecision decision = decideVariantMovement(variants2, signatureIndex);

			    deleteProductReferencesFromArticles(itemsOfProduct2);

			    createProductReferencesToId1(toArticleObjectIds(decision.articlesToMoveToProduct1), id1);

			    clearSkuAndEanFromArticles(decision.duplicatedArticlesToDetachAndClean);

			    mergeMissingProductData(data1, data2);
			    clearProductSkuAndEan(data2);

			    java.util.Map<String, String> qp = new java.util.HashMap<>();

			    org.json.JSONObject write1 = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + rw.getRw().encode(id1) + "'@1", qp, data1.toString());

			    if(write1 == null) {
			        log("PANIC: from id1=" + id1 + ". rawResponse=" + rw.getRw().getRawResponse());
			        return;
			    }

			    qp.clear();

			    org.json.JSONObject write2 = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + rw.getRw().encode(id2) + "'@1", qp, data2.toString());

			    if(write2 == null) {
			        log("PANIC: from id2=" + id2 + ". rawResponse=" + rw.getRw().getRawResponse());
			        return;
			    }

			    log("Combinación finished. id1=" + id1
			            + ", id2=" + id2
			            + ", moveToId1=" + decision.articlesToMoveToProduct1.size()
			            + ", cleanDuplicated=" + decision.duplicatedArticlesToDetachAndClean.size());
				
			}
		}
	}
	
	private java.util.List<String> toArticleObjectIds(java.util.List<VariantInfo> variants) {
	    java.util.List<String> ids = new java.util.ArrayList<>();

	    for(VariantInfo variant : variants) {
	        ids.add(variant.articleObjectId);
	    }

	    return ids;
	}
	
	private void mergeMissingProductData(org.json.JSONObject data1, org.json.JSONObject data2) {
	    java.util.Set<String> excluded = new java.util.HashSet<>();

	    excluded.add("identifier");
	    excluded.add("sku");
	    excluded.add("gtin");
	    excluded.add("statusModification");
	    excluded.add("log");
	    excluded.add("ownLog");

	    mergeObjectMissing(data1, data2, excluded);
	}
	
	private void clearProductSkuAndEan(org.json.JSONObject data) {
	    data.put("sku", org.json.JSONObject.NULL);
	    data.put("gtin", org.json.JSONObject.NULL);
	    clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);
	}
	
	private java.util.List<VariantInfo> loadVariantInfos(java.util.List<String> articleObjectIds, String productOwner) {
	    java.util.List<VariantInfo> result = new java.util.ArrayList<>();

	    for(String articleObjectId : articleObjectIds) {
	        org.json.JSONObject response = rw.getRw().makeRequest(
	                "GET",
	                "/object/Article/" + articleObjectId + "?includeIds=true&includeLabels=true"
	        );

	        org.json.JSONObject data = response != null && response.has("_data")
	                ? response.getJSONObject("_data")
	                : null;

	        if(data == null) {
	            log("No se pudo leer Article. owner=" + productOwner + ", article=" + articleObjectId);
	            continue;
	        }

	        java.util.Set<String> signatures = buildVariantSignatures(data);

	        if(signatures.isEmpty()) {
	            log("Article sin firma útil. owner=" + productOwner + ", article=" + articleObjectId);
	        }

	        result.add(new VariantInfo(articleObjectId, productOwner, data, signatures));
	    }

	    return result;
	}
	
	private java.util.Set<String> buildVariantSignatures(org.json.JSONObject data) {
	    java.util.Set<String> signatures = new java.util.LinkedHashSet<>();

	    String sku = stringValue(data.opt("sku"));
	    String gtin = stringValue(data.opt("gtin"));

	    if(!isBlank(sku)) {
	        signatures.add("SKU|" + normalize(sku));
	    }

	    if(!isBlank(gtin)) {
	        signatures.add("EAN|" + normalize(gtin));
	    }

	    java.util.Map<String, String> characteristicValues = extractCharacteristicValues(data);

	    addSignatureIfPresent(signatures, "SKU", characteristicValues.get("SKU"));
	    addSignatureIfPresent(signatures, "MainBarCode", characteristicValues.get("MainBarCode"));
	    addSignatureIfPresent(signatures, "MainBarCodeS4H", characteristicValues.get("MainBarCodeS4H"));

	    String color = firstNotBlank(
	            extractMxExtraDataValue(data, "coloursLiverpoolAtt"),
	            characteristicValues.get("ColoursLiverpoolAtt")
	    );

	    String size = firstNotBlank(
	            extractMxExtraDataValue(data, "tamanoUnico"),
	            characteristicValues.get("TamanoUnico")
	    );

	    String supplierPartNumber = firstNotBlank(
	            extractMxExtraDataValue(data, "supplierPartNumber"),
	            characteristicValues.get("SupplierPartNumber")
	    );

	    if(!isBlank(color) && !isBlank(size) && !isBlank(supplierPartNumber)) {
	        signatures.add("COLOR_SIZE_MODEL|" + normalize(color) + "|" + normalize(size) + "|" + normalize(supplierPartNumber));
	    }

	    return signatures;
	}
	
	private java.util.Map<String, String> extractCharacteristicValues(org.json.JSONObject data) {
	    java.util.Map<String, String> valuesByCode = new java.util.HashMap<>();

	    org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

	    if(records == null) {
	        return valuesByCode;
	    }

	    for(int i = 0; i < records.length(); i++) {
	        org.json.JSONObject record = records.optJSONObject(i);

	        if(record == null) {
	            continue;
	        }

	        String code = nestedValue(record, "_qualification.characteristic._code");

	        if(isBlank(code)) {
	            continue;
	        }

	        if(!VARIANT_MATCH_CHARACTERISTICS.contains(code)) {
	            continue;
	        }

	        String value = firstRecordValue(record);

	        if(!isBlank(value)) {
	            valuesByCode.put(code, value);
	        }
	    }

	    return valuesByCode;
	}

	private String firstRecordValue(org.json.JSONObject record) {
	    org.json.JSONArray recordLang = record.optJSONArray("_recordLang");

	    if(recordLang == null) {
	        return "";
	    }

	    for(int i = 0; i < recordLang.length(); i++) {
	        org.json.JSONObject lang = recordLang.optJSONObject(i);

	        if(lang == null) {
	            continue;
	        }

	        org.json.JSONArray values = lang.optJSONArray("values");

	        if(values == null || values.length() == 0) {
	            continue;
	        }

	        Object value = values.opt(0);

	        if(value == null || value == org.json.JSONObject.NULL) {
	            continue;
	        }

	        if(value instanceof org.json.JSONObject) {
	            org.json.JSONObject valueObject = (org.json.JSONObject) value;

	            String code = valueObject.optString("_code", "");
	            if(!isBlank(code)) {
	                return code;
	            }

	            String label = valueObject.optString("_label", "");
	            if(!isBlank(label)) {
	                return label;
	            }

	            org.json.JSONObject key = valueObject.optJSONObject("_key");
	            if(key != null) {
	                String externalId = key.optString("_externalId", "");
	                if(!isBlank(externalId)) {
	                    return externalId;
	                }

	                String internalId = key.optString("_internalId", "");
	                if(!isBlank(internalId)) {
	                    return internalId;
	                }
	            }

	            return valueObject.toString();
	        }

	        return String.valueOf(value).trim();
	    }

	    return "";
	}

	private String extractMxExtraDataValue(org.json.JSONObject data, String fieldName) {
	    String value = extractMxExtraDataValueFromArray(data.optJSONArray("extraData"), fieldName);

	    if(!isBlank(value)) {
	        return value;
	    }

	    return extractMxExtraDataValueFromArray(data.optJSONArray("productExtraData"), fieldName);
	}

	private String extractMxExtraDataValueFromArray(org.json.JSONArray array, String fieldName) {
	    if(array == null) {
	        return "";
	    }

	    for(int i = 0; i < array.length(); i++) {
	        org.json.JSONObject item = array.optJSONObject(i);

	        if(item == null) {
	            continue;
	        }

	        String targetMarket = firstNotBlank(
	                nestedValue(item, "_qualification.targetMarket._code"),
	                nestedValue(item, "_qualification.targetMarket._key"),
	                nestedValue(item, "_qualification.targetMarket._label")
	        );

	        if(!"MX".equalsIgnoreCase(targetMarket) && !"Mexico".equalsIgnoreCase(targetMarket)) {
	            continue;
	        }

	        Object rawValue = item.opt(fieldName);

	        if(rawValue == null || rawValue == org.json.JSONObject.NULL) {
	            continue;
	        }

	        if(rawValue instanceof org.json.JSONObject) {
	            org.json.JSONObject object = (org.json.JSONObject) rawValue;

	            String code = object.optString("_code", "");
	            if(!isBlank(code)) {
	                return code;
	            }

	            String label = object.optString("_label", "");
	            if(!isBlank(label)) {
	                return label;
	            }

	            org.json.JSONObject key = object.optJSONObject("_key");
	            if(key != null) {
	                String externalId = key.optString("_externalId", "");
	                if(!isBlank(externalId)) {
	                    return externalId;
	                }

	                String internalId = key.optString("_internalId", "");
	                if(!isBlank(internalId)) {
	                    return internalId;
	                }
	            }

	            return object.toString();
	        }

	        return String.valueOf(rawValue).trim();
	    }

	    return "";
	}
	
	private void addSignatureIfPresent(java.util.Set<String> signatures, String name, String value) {
	    if(!isBlank(value)) {
	        signatures.add(name + "|" + normalize(value));
	    }
	}

	private String stringValue(Object value) {
	    if(value == null || value == org.json.JSONObject.NULL) {
	        return "";
	    }

	    return String.valueOf(value).trim();
	}

	private String normalize(String value) {
	    if(value == null) {
	        return "";
	    }

	    return value.trim().toUpperCase(java.util.Locale.ROOT);
	}

	private java.util.Map<String, java.util.List<VariantInfo>> buildSignatureIndex(
	        java.util.List<VariantInfo> variants1,
	        java.util.List<VariantInfo> variants2) {

	    java.util.Map<String, java.util.List<VariantInfo>> index = new java.util.LinkedHashMap<>();

	    addToSignatureIndex(index, variants1);
	    addToSignatureIndex(index, variants2);

	    return index;
	}

	private void addToSignatureIndex(
	        java.util.Map<String, java.util.List<VariantInfo>> index,
	        java.util.List<VariantInfo> variants) {

	    for(VariantInfo variant : variants) {
	        for(String signature : variant.signatures) {
	            index.computeIfAbsent(signature, k -> new java.util.ArrayList<>()).add(variant);
	        }
	    }
	}
	
	private void deleteProductReferencesFromArticles(java.util.List<String> itemsOfTheProduct) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    StringBuilder sb = new StringBuilder();
	    int a = 0;

	    for(String internalArticleId : itemsOfTheProduct) {
	        sb.append(sb.length() == 0 ? "" : ",").append(internalArticleId);
	        a++;

	        if(a % 1000 == 0) {
	            qp.put("items", sb.toString());
	            rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
	            sb.setLength(0);
	            qp.clear();
	        }
	    }

	    if(sb.length() > 0) {
	        qp.put("items", sb.toString());
	        rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
	        sb.setLength(0);
	        qp.clear();
	    }
	}

	private void createProductReferencesToId1(java.util.List<String> itemsOfTheProduct, String id1) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    qp.put("includeObjectsInProtocol", "false");

	    RequestHandler rh = new RequestHandler(
	            new org.json.JSONArray().put(
	                    new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")
	            ),
	            1000,
	            request -> rw.writeData("list", "Article", "ProductReference", qp, request, this::log)
	    );

	    for(String internalId : itemsOfTheProduct) {
	        rh.addRow(
	                new org.json.JSONObject()
	                        .put("object", new org.json.JSONObject().put("id", internalId))
	                        .put("qualification", new org.json.JSONObject().put("referencedSupplierAid", id1))
	                        .put("values", new org.json.JSONArray().put(id1))
	        );
	    }

	    rh.sendData();
	}

	private void mergeObjectMissing(org.json.JSONObject target, org.json.JSONObject source, java.util.Set<String> excludedKeys) {
	    for(Object keyObject : source.keySet()) {
	        String key = String.valueOf(keyObject);

	        if(excludedKeys != null && excludedKeys.contains(key)) {
	            continue;
	        }

	        Object sourceValue = source.opt(key);

	        if(isEmptyJsonValue(sourceValue)) {
	            continue;
	        }

	        Object targetValue = target.opt(key);

	        if(isEmptyJsonValue(targetValue)) {
	            target.put(key, cloneJsonValue(sourceValue));
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONObject && targetValue instanceof org.json.JSONObject) {
	            mergeObjectMissing((org.json.JSONObject) targetValue, (org.json.JSONObject) sourceValue, null);
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONArray && targetValue instanceof org.json.JSONArray) {
	            mergeArrayMissing(key, (org.json.JSONArray) targetValue, (org.json.JSONArray) sourceValue);
	        }
	    }
	}
	
	private java.util.List<String> collectArticleObjectIdsByProduct(String productIdentifier) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    qp.put("pageSize", "2000");
	    qp.put("products", "'" + productIdentifier + "'@1");

	    java.util.List<String> items = new java.util.ArrayList<>();

	    rw.collectData("list", "Article", null, "byProducts", qp, row -> {
	        items.add(row.getJSONObject("object").getString("id"));
	    });

	    return items;
	}
	
	private void mergeArrayMissing(String sectionName, org.json.JSONArray targetArray, org.json.JSONArray sourceArray) {
	    java.util.Map<String, org.json.JSONObject> targetByKey = new java.util.LinkedHashMap<>();

	    for(int i = 0; i < targetArray.length(); i++) {
	        Object value = targetArray.opt(i);

	        if(value instanceof org.json.JSONObject) {
	            org.json.JSONObject object = (org.json.JSONObject) value;
	            targetByKey.put(buildArrayItemKey(sectionName, object), object);
	        }
	    }

	    for(int i = 0; i < sourceArray.length(); i++) {
	        Object sourceValue = sourceArray.opt(i);

	        if(!(sourceValue instanceof org.json.JSONObject)) {
	            if(!arrayContainsEquivalentValue(targetArray, sourceValue)) {
	                targetArray.put(cloneJsonValue(sourceValue));
	            }
	            continue;
	        }

	        org.json.JSONObject sourceObject = (org.json.JSONObject) sourceValue;
	        String sourceKey = buildArrayItemKey(sectionName, sourceObject);
	        org.json.JSONObject targetObject = targetByKey.get(sourceKey);

	        if(targetObject == null) {
	            targetArray.put(new org.json.JSONObject(sourceObject.toString()));
	        } else {
	            mergeObjectMissing(targetObject, sourceObject, null);
	        }
	    }
	}

	private String buildArrayItemKey(String sectionName, org.json.JSONObject object) {
	    if("lang".equals(sectionName)) {
	        return "lang|" + nestedValue(object, "_qualification.language._key");
	    }

	    if("structureGroupMap".equals(sectionName)) {
	        return "structureGroupMap|" + objectKey(object.optJSONObject("_qualification"), "structureGroup");
	    }

	    if("attribute".equals(sectionName)) {
	        String identifier = object.optString("identifier", "");
	        if(!isBlank(identifier)) {
	            return "attribute|" + identifier;
	        }

	        return "attribute|" + nestedValue(object, "_qualification.nameInKeyLang");
	    }

	    if("_characteristicRecords".equals(sectionName)) {
	        String characteristic = objectKey(object.optJSONObject("_qualification"), "characteristic");
	        String recordKey = nestedValue(object, "_qualification.recordKey");
	        String parentRecordKey = nestedValue(object, "_qualification.parentRecordKey");
	        return "_characteristicRecords|" + characteristic + "|" + recordKey + "|" + parentRecordKey;
	    }

	    if("productExtraData".equals(sectionName)) {
	        return "productExtraData|" + objectKey(object.optJSONObject("_qualification"), "targetMarket");
	    }

	    if("value".equals(sectionName)) {
	        String lang = nestedValue(object, "_qualification.language._key");
	        String identifier = nestedValue(object, "_qualification.identifier");
	        return "value|" + lang + "|" + identifier;
	    }

	    if("_recordLang".equals(sectionName)) {
	        return "_recordLang|" + nestedValue(object, "_qualification.language._key");
	    }

	    return sectionName + "|" + object.toString();
	}

	private String objectKey(org.json.JSONObject parent, String childName) {
	    if(parent == null) {
	        return "";
	    }

	    org.json.JSONObject child = parent.optJSONObject(childName);

	    if(child == null) {
	        return "";
	    }

	    org.json.JSONObject key = child.optJSONObject("_key");

	    if(key != null) {
	        String externalId = key.optString("_externalId", "");
	        String internalId = key.optString("_internalId", "");
	        String entityId = String.valueOf(key.opt("_entityId"));
	        return firstNotBlank(externalId, internalId, entityId);
	    }

	    String externalId = child.optString("_externalId", "");
	    String internalId = child.optString("_internalId", "");
	    String code = child.optString("_code", "");
	    String keyValue = String.valueOf(child.opt("_key"));

	    return firstNotBlank(externalId, internalId, code, keyValue);
	}

	private String nestedValue(org.json.JSONObject object, String path) {
	    if(object == null || isBlank(path)) {
	        return "";
	    }

	    String[] parts = path.split("\\.");
	    Object current = object;

	    for(String part : parts) {
	        if(!(current instanceof org.json.JSONObject)) {
	            return "";
	        }

	        current = ((org.json.JSONObject) current).opt(part);

	        if(current == null || current == org.json.JSONObject.NULL) {
	            return "";
	        }
	    }

	    return String.valueOf(current);
	}

	private void clearSkuAndEanFromArticles(java.util.List<VariantInfo> variants) {
	    for(VariantInfo variant : variants) {
	        org.json.JSONObject data = variant.data;

	        data.put("sku", org.json.JSONObject.NULL);
	        data.put("gtin", org.json.JSONObject.NULL);

	        clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);

	        org.json.JSONObject writeResponse = rw.getRw().makeRequest(
	                "PUT",
	                "/object/Article/" + variant.articleObjectId,
	                new java.util.HashMap<>(),
	                data.toString()
	        );

	        if(writeResponse == null) {
	            log("PANIC: fallo PUT Article " + variant.articleObjectId + ". rawResponse=" + rw.getRw().getRawResponse());
	            return;
	        }
	    }
	}

	private void clearCharacteristicRecords(org.json.JSONObject data, java.util.Set<String> characteristicCodesToClear) {
	    org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

	    if(records == null) {
	        return;
	    }

	    org.json.JSONArray kept = new org.json.JSONArray();

	    for(int i = 0; i < records.length(); i++) {
	        org.json.JSONObject record = records.optJSONObject(i);

	        if(record == null) {
	            kept.put(records.opt(i));
	            continue;
	        }

	        String code = nestedValue(record, "_qualification.characteristic._code");

	        if(characteristicCodesToClear.contains(code)) {
	            log("Quitando characteristicRecord de identidad: " + code);
	            continue;
	        }

	        kept.put(record);
	    }

	    data.put("_characteristicRecords", kept);
	}
	
	private VariantMergeDecision decideVariantMovement(
	        java.util.List<VariantInfo> variants2,
	        java.util.Map<String, java.util.List<VariantInfo>> signatureIndex) {

	    java.util.List<VariantInfo> articlesToMoveToProduct1 = new java.util.ArrayList<>();
	    java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean = new java.util.ArrayList<>();

	    for(VariantInfo variant2 : variants2) {
	        if(variant2.signatures.isEmpty()) {
	            log("Article de id2 sin firma útil; se mueve por conservación. article=" + variant2.articleObjectId);
	            articlesToMoveToProduct1.add(variant2);
	            continue;
	        }

	        boolean matchedAgainstId1 = false;
	        java.util.Set<String> matchedRefsForLog = new java.util.LinkedHashSet<>();

	        for(String signature : variant2.signatures) {
	            java.util.List<VariantInfo> refs = signatureIndex.get(signature);

	            if(refs == null || refs.isEmpty()) {
	                continue;
	            }

	            for(VariantInfo ref : refs) {
	                matchedRefsForLog.add(signature + " -> " + ref.toString());

	                if(ref.belongsToProduct1()) {
	                    matchedAgainstId1 = true;
	                }
	            }
	        }

	        if(matchedAgainstId1) {
	            duplicatedArticlesToDetachAndClean.add(variant2);
	            log("Article duplicado contra id1; se despoja SKU/EAN. article="
	                    + variant2.articleObjectId
	                    + ", matches="
	                    + matchedRefsForLog);
	        } else {
	            articlesToMoveToProduct1.add(variant2);
	            log("Article de id2 sin coincidencia contra id1; se mueve a id1. article="
	                    + variant2.articleObjectId
	                    + ", matches="
	                    + matchedRefsForLog);
	        }
	    }

	    return new VariantMergeDecision(articlesToMoveToProduct1, duplicatedArticlesToDetachAndClean);
	}

	private static class VariantMergeDecision {
	    final java.util.List<VariantInfo> articlesToMoveToProduct1;
	    final java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean;

	    VariantMergeDecision(
	            java.util.List<VariantInfo> articlesToMoveToProduct1,
	            java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean) {

	        this.articlesToMoveToProduct1 = articlesToMoveToProduct1;
	        this.duplicatedArticlesToDetachAndClean = duplicatedArticlesToDetachAndClean;
	    }
	}
	
	private static class VariantInfo {
	    final String articleObjectId;
	    final String productOwner;
	    final org.json.JSONObject data;
	    final java.util.Set<String> signatures;

	    VariantInfo(String articleObjectId, String productOwner, org.json.JSONObject data, java.util.Set<String> signatures) {
	        this.articleObjectId = articleObjectId;
	        this.productOwner = productOwner;
	        this.data = data;
	        this.signatures = signatures;
	    }

	    boolean belongsToProduct1() {
	        return "id1".equals(productOwner);
	    }

	    @Override
	    public String toString() {
	        return productOwner + ":" + articleObjectId;
	    }
	}

	private boolean arrayContainsEquivalentValue(org.json.JSONArray array, Object value) {
	    String valueString = String.valueOf(value);

	    for(int i = 0; i < array.length(); i++) {
	        Object current = array.opt(i);

	        if(String.valueOf(current).equals(valueString)) {
	            return true;
	        }
	    }

	    return false;
	}

	private Object cloneJsonValue(Object value) {
	    if(value instanceof org.json.JSONObject) {
	        return new org.json.JSONObject(((org.json.JSONObject) value).toString());
	    }

	    if(value instanceof org.json.JSONArray) {
	        return new org.json.JSONArray(((org.json.JSONArray) value).toString());
	    }

	    return value;
	}

	private boolean isEmptyJsonValue(Object value) {
	    if(value == null || value == org.json.JSONObject.NULL) {
	        return true;
	    }

	    if(value instanceof String) {
	        return ((String) value).trim().isEmpty();
	    }

	    if(value instanceof org.json.JSONArray) {
	        return ((org.json.JSONArray) value).length() == 0;
	    }

	    if(value instanceof org.json.JSONObject) {
	        return ((org.json.JSONObject) value).length() == 0;
	    }

	    return false;
	}

	private boolean isBlank(String value) {
	    return value == null || value.trim().isEmpty();
	}

	private String firstNotBlank(String... values) {
	    if(values == null) {
	        return "";
	    }

	    for(String value : values) {
	        if(!isBlank(value) && !"null".equalsIgnoreCase(value)) {
	            return value;
	        }
	    }

	    return "";
	}
    
    private final org.json.JSONArray columnsStructureGroupMap = new org.json.JSONArray()
    			.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap"))
    		;
    private final org.json.JSONArray rowsStructureGroupMap = new org.json.JSONArray();
    private final org.json.JSONObject requestStructureGroup = new org.json.JSONObject().put("columns", columnsStructureGroupMap).put("rows", rowsStructureGroupMap);
    
    private void collectClassifications(java.util.List<Classification> classifications, String productId) {
    	String structureId = null;
    	String type = null;
    	int index = -1;
    	for(Classification classification : classifications) {
    		type = classification.getType();
    		if("WebsiteLink".equals(type) || type.startsWith("GALink")) {
    			structureId = classification.getId();
    			if("GALink".equals(type)) {
    				index = java.util.Arrays.binarySearch(eccItemGroups, structureId);
    			}else if("GALink_S4H".equals(type)) {
    				index = java.util.Arrays.binarySearch(s4hItemGroups, structureId);
    			}else {
    				index = java.util.Arrays.binarySearch(webSites, structureId);
    			}
    			if(index > -1) {
	    			rowsStructureGroupMap.put(
	    					new org.json.JSONObject()
	    						.put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1"))
	    						.put("qualification", new org.json.JSONObject().put("structureId",  ( "GALink".equals(type) ? "CommercialECC" : "GALink_S4H".equals(type) ? "CommercialS4H" : "Sitios Web" ) ))
	    						.put("values", new org.json.JSONArray().put( classification.getId() ))
	    				);
    			}else {
//    				log("Structure not found: " + structureId + " of type: " + type);
    			}
    		}
    	}
    }
    
    private String[] webSites      = new String[60000];
    private String[] eccItemGroups = new String[60000];
    private String[] s4hItemGroups = new String[60000];
    private int eccItemGroupsCounter = 0;
    private int s4hItemGroupsCounter = 0;
    private int webSitesCounter      = 0;
    
    private final void loadItemGroups() {
    	log("Loading item groups...");
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
    	qp.put("fields", "StructureGroup.Identifier");
    	qp.put("query", "not StructureGroup.Identifier is empty");
    	qp.put("structure", "CommercialECC");
    	qp.put("pageSize", "30000");
    	rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
    		eccItemGroups[eccItemGroupsCounter] = row.getJSONArray("values").getString(0);
    		eccItemGroupsCounter++;
    	});
    	qp.put("structure", "CommercialS4H");
    	qp.put("query", "not StructureGroup.Identifier is empty");
    	rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
    		s4hItemGroups[s4hItemGroupsCounter] = row.getJSONArray("values").getString(0);
    		s4hItemGroupsCounter++;
    	});
    	qp.put("structure", "Sitios Web");
    	rw.collectData("list", "StructureGroup", null, "byStructure", qp, row -> {
    		webSites[webSitesCounter] = row.getJSONArray("values").getString(0);
    		webSitesCounter++;
    	});
    	String[] auxECC = java.util.Arrays.copyOf(eccItemGroups, eccItemGroupsCounter);
    	String[] auxS4H = java.util.Arrays.copyOf(s4hItemGroups, s4hItemGroupsCounter);
    	String[] auxWebSites = java.util.Arrays.copyOf(webSites, webSitesCounter);
    	eccItemGroups = auxECC;
    	s4hItemGroups = auxS4H;
    	webSites = auxWebSites;
    	java.util.Arrays.sort(eccItemGroups);
    	java.util.Arrays.sort(s4hItemGroups);
    	java.util.Arrays.sort(webSites);
    	log("ItemGroups loaded...");
    	log("ECC: " + eccItemGroups.length);
    	log("S4H: " + s4hItemGroups.length);
    	log("Sitios Web: " + webSites.length);
    }
    
	private String determineBusiness(String negocio, String extwgS4h) {
		return     "".equals(negocio) 
				&& "".equals(extwgS4h) ? null : 
					("".equals(negocio) && !"".equals(extwgS4h) ? "SBB": "ART. MARKETPLACE".equals(negocio) ? "MKP" : "LVP" );
	}

	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(normalLogFilePath.toString(), true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "] " + message);
        } catch (java.io.IOException e) {
        }
    }

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(normalLogFilePath.toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
