package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class LoadProductDataSecondOpinion {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_second_opinion2.log");
	

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
    
    public static int processContent(String content) throws Exception {
    	long init = System.currentTimeMillis();
    	LoadProductDataSecondOpinion an = new LoadProductDataSecondOpinion();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.loadItemGroups();
        an.loadOfInterest();
        an.qp.put("includeObjectsInProtocol", "false");
        int cantidad = an.procesaContenido(content, parser);
        if(an.products.length() > 0) {
			org.json.JSONObject req = new org.json.JSONObject();
			req.put("products", an.products);
			an.log( an.pub.publishMessage(
					 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
					 PropertiesManager.get( "p360.contingency.gcp.idmc_put_products" ), 
					 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
					 req.toString()
					) );
			while(an.products.length() > 0) {
				an.products.remove(0);
			}
		}
        if(an.rows.length() > 0) {
        	an.rw.writeData("list", "Product2G", null, an.qp, an.request, an::log);
        	while(an.rows.length() > 0) {
        		an.rows.remove(0);
        	}
        }
        if(an.rowsArticle.length() > 0) {
        	an.rw.writeData("list", "Article", null, an.qp, an.requestArticle, an::log);
        	while(an.rowsArticle.length() > 0) {
        		an.rowsArticle.remove(0);
        	}
        }
        org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
        org.json.JSONArray rows = new org.json.JSONArray();
        org.json.JSONObject request = new org.json.JSONObject();
        request.put("columns", columns);
        request.put("rows", rows);
        DataRequestor dr = new DataRequestor();
        String r = null;
        for(java.util.Map.Entry<String, String> entry : an.childParent.entrySet()) {
        	r = dr.getArticleData(new org.json.JSONArray().put(entry.getKey()));
        	if(r != null) {
        		org.json.JSONObject jo = new org.json.JSONObject(r);
        		org.json.JSONArray itms = jo.getJSONArray("items");
        		for(int i=0; i<itms.length(); i++) {
        			jo = itms.getJSONObject(i);
        			jo.put("ProductNo", entry.getValue());
        			dr.putArticleData(itms);
        		}
        	}
        	rows.put(
        			new org.json.JSONObject()
        				.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
        				.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue()))
        				.put("values", new org.json.JSONArray().put(entry.getValue())));
        	if(rows.length() == 5000) {
        		an.rw.writeData("list", "Article", "ProductReference", an.qp, request, System.out::println);
        		while(rows.length() > 0) {
        			rows.remove(0);
        		}
        	}
        }
        if(rows.length() > 0) {
        	an.rw.writeData("list", "Article", "ProductReference", an.qp, request, System.out::println);
    		while(rows.length() > 0) {
    			rows.remove(0);
    		}
        }
		an.rw.writeData("list", "Product2G", "Product2GStructureMap", an.qp, an.requestStructureGroup, an::log);
		while(an.rowsStructureGroupMap.length() > 0) {
			an.rowsStructureGroupMap.remove(0);
		}
		an.log("Ahora los que faltaron:");
		for(int a = 0; a<an.ofInterest.length; a++) {
			if(!an.losEncontrados.contains(an.ofInterest[a])) {
			}
		}
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
        return cantidad;
    }
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	LoadProductDataSecondOpinion an = new LoadProductDataSecondOpinion();
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	an.normalLogFilePath = java.nio.file.Paths.get(
    											  ".."
    											, "logs"
    											, args.length == 1 ? "" : args[1]
    											, "list_api_load_from_step_second_opinion2.log"
    										);
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
        an.loadItemGroups();
        an.loadOfInterest();
        an.qp.put("includeObjectsInProtocol", "false");
        an.procesaDirectorio(args[0], parser);
        if(an.products.length() > 0) {
			org.json.JSONObject req = new org.json.JSONObject();
			req.put("products", an.products);
			an.log( an.pub.publishMessage(
					 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
					 PropertiesManager.get( "p360.contingency.gcp.idmc_put_products" ), 
					 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
					 req.toString()
					) );
			while(an.products.length() > 0) {
				an.products.remove(0);
			}
		}
        if(an.rows.length() > 0) {
        	an.rw.writeData("list", "Product2G", null, an.qp, an.request, an::log);
        	while(an.rows.length() > 0) {
        		an.rows.remove(0);
        	}
        }
        if(an.rowsArticle.length() > 0) {
        	an.rw.writeData("list", "Article", null, an.qp, an.requestArticle, an::log);
        	while(an.rowsArticle.length() > 0) {
        		an.rowsArticle.remove(0);
        	}
        }
        org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
        org.json.JSONArray rows = new org.json.JSONArray();
        org.json.JSONObject request = new org.json.JSONObject();
        request.put("columns", columns);
        request.put("rows", rows);
        DataRequestor dr = new DataRequestor();
        String r = null;
        for(java.util.Map.Entry<String, String> entry : an.childParent.entrySet()) {
        	r = dr.getArticleData(new org.json.JSONArray().put(entry.getKey()));
        	if(r != null) {
        		org.json.JSONObject jo = new org.json.JSONObject(r);
        		org.json.JSONArray itms = jo.getJSONArray("items");
        		for(int i=0; i<itms.length(); i++) {
        			jo = itms.getJSONObject(i);
        			jo.put("ProductNo", entry.getValue());
        			dr.putArticleData(itms);
        		}
        	}
        	rows.put(
        			new org.json.JSONObject()
        				.put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1"))
        				.put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue()))
        				.put("values", new org.json.JSONArray().put(entry.getValue())));
        	if(rows.length() == 5000) {
        		an.rw.writeData("list", "Article", "ProductReference", an.qp, request, System.out::println);
        		while(rows.length() > 0) {
        			rows.remove(0);
        		}
        	}
        }
        if(rows.length() > 0) {
        	an.rw.writeData("list", "Article", "ProductReference", an.qp, request, System.out::println);
    		while(rows.length() > 0) {
    			rows.remove(0);
    		}
        }
		an.rw.writeData("list", "Product2G", "Product2GStructureMap", an.qp, an.requestStructureGroup, an::log);
		while(an.rowsStructureGroupMap.length() > 0) {
			an.rowsStructureGroupMap.remove(0);
		}
		an.log("Ahora los que faltaron:");
		for(int a = 0; a<an.ofInterest.length; a++) {
			if(!an.losEncontrados.contains(an.ofInterest[a])) {
//				an.log("Este no estuvo: " + an.ofInterest[a]);
			}
		}
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private String[] ofInterest;
    
    private void loadOfInterest() {
    	String[] lst = new String[5000000];
    	int a = 0;
    	try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("/", "u01", "workshop", "java", "ProductNo.txt").toFile())))){
    		String line = null;
    		while((line = br.readLine()) != null) {
    			lst[a] = line;
    			a++;
    		}
    	}catch(java.io.IOException e) {
    		logE(e);
    	}
    	ofInterest = java.util.Arrays.copyOf(lst, a);
    	java.util.Arrays.sort(ofInterest);
    }
    
    private Integer procesaContenido(String content, SAXParser parser) throws SAXException, java.io.IOException {
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        java.util.List<Product> finished = null;
        Handler handler = new Handler();
        try {
        	parser.parse( new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)) , handler);
        	finished = handler.getFinished();
	        refProductsCount += handler.getPrductsCounter();
	        for(Product p : finished) {
	        	processProduct(p);
	        }
        }catch(org.xml.sax.SAXParseException e) {
        	log("Problem processing content");
        }
        log("Parsing files took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
        return refProductsCount;
    }
    
    private Integer procesaDirectorio(String dir, SAXParser parser) throws SAXException, java.io.IOException {
    	log("Checking: " + dir);
    	java.io.File[] files = new java.io.File(dir).listFiles();
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        log("Now reading files... " + files.length);
        java.util.List<Product> finished = null;
        for(File input : files) {
        	if(input.isDirectory()) {
        		refProductsCount += procesaDirectorio(input.getAbsolutePath(), parser);
        	}else {
		        Handler handler = new Handler();
		        try {
		        	parser.parse(input, handler);
		        	finished = handler.getFinished();
			        refProductsCount += handler.getPrductsCounter();
			        for(Product p : finished) {
			        	processProduct(p);
			        }
		        }catch(org.xml.sax.SAXParseException e) {
		        	log("Problem processing following file: " + input.getAbsolutePath());
		        }
        	}
        }
        log("Parsing files took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
        return refProductsCount;
    }
    
    private PruebaEnvioPubSubMediaAssets elese = new PruebaEnvioPubSubMediaAssets();
    private int lacuenta = 0;
    private final org.json.JSONArray rows = new org.json.JSONArray();
    private final org.json.JSONArray columns = new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus"))
				.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('PrimaryProductTaxonomy')"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnriquecidoEnForo',root,\"0000.0000.RK\",'EnriquecidoEnForo',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('AR',root,\"0000.0000.RK\",'AR',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ARURL',root,\"0000.0000.RK\",'ARURL',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('AnoEstacion',root,\"0000.0000.RK\",'AnoEstacion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ApprovedDateCalc',root,\"0000.0000.RK\",'ApprovedDateCalc',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ArgumentoDeVenta',root,\"0000.0000.RK\",'ArgumentoDeVenta',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('AssetRejectionMessage',root,\"0000.0000.RK\",'AssetRejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BWSCL',root,\"0000.0000.RK\",'BWSCL',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BWVOR',root,\"0000.0000.RK\",'BWVOR',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BlueCategorizeRejectionMessage',root,\"0000.0000.RK\",'BlueCategorizeRejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BrandIDATG',root,\"0000.0000.RK\",'BrandIDATG',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BrandName',root,\"0000.0000.RK\",'BrandName',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BrandNameATG',root,\"0000.0000.RK\",'BrandNameATG',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BrandOwner',root,\"0000.0000.RK\",'BrandOwner',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Business',root,\"0000.0000.RK\",'Business',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('BuyerRejectionMessage',root,\"0000.0000.RK\",'BuyerRejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CSTFLD03',root,\"0000.0000.RK\",'CSTFLD03',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CUBISCAN',root,\"0000.0000.RK\",'CUBISCAN',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CalculatedWF_Att',root,\"0000.0000.RK\",'CalculatedWF_Att',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Calculated_inStateWF',root,\"0000.0000.RK\",'Calculated_inStateWF',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CategoryManagerRejectionMessage',root,\"0000.0000.RK\",'CategoryManagerRejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CertificadoSostenible',root,\"0000.0000.RK\",'CertificadoSostenible',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Coleccion',root,\"0000.0000.RK\",'Coleccion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CollectionDescriptionSAP',root,\"0000.0000.RK\",'CollectionDescriptionSAP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ComentarioProyectosComerciales',root,\"0000.0000.RK\",'ComentarioProyectosComerciales',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Consignacion',root,\"0000.0000.RK\",'Consignacion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CoordinadoIDSAP',root,\"0000.0000.RK\",'CoordinadoIDSAP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CostoEnMonedaExtranjera',root,\"0000.0000.RK\",'CostoEnMonedaExtranjera',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CostoNetoSinIVA',root,\"0000.0000.RK\",'CostoNetoSinIVA',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CostobrutoSinIVA',root,\"0000.0000.RK\",'CostobrutoSinIVA',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('CountryOfOrigin',root,\"0000.0000.RK\",'CountryOfOrigin',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Currency',root,\"0000.0000.RK\",'Currency',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('DeleteCheck',root,\"0000.0000.RK\",'DeleteCheck',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionLong(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionLong2(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('DescriptionTable',root,\"0000.0000.RK\",'DescriptionTable',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('DescriptionWeb',root,\"0000.0000.RK\",'DescriptionWeb',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Direction',root,\"0000.0000.RK\",'Direction',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ENV_ATG',root,\"0000.0000.RK\",'ENV_ATG',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EXTWG_S4H',root,\"0000.0000.RK\",'EXTWG_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EmbedCodeWAP',root,\"0000.0000.RK\",'EmbedCodeWAP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EmbedCodeWEB',root,\"0000.0000.RK\",'EmbedCodeWEB',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnrichmentRejectionMessage',root,\"0000.0000.RK\",'EnrichmentRejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnvioImagen',root,\"0000.0000.RK\",'EnvioImagen',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnvioInternacional',root,\"0000.0000.RK\",'EnvioInternacional',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnvioMirakl',root,\"0000.0000.RK\",'EnvioMirakl',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ErroresDeSistema',root,\"0000.0000.RK\",'ErroresDeSistema',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EsSostenible',root,\"0000.0000.RK\",'EsSostenible',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EsSostenibleVAD',root,\"0000.0000.RK\",'EsSostenibleVAD',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EstatusPropuesta',root,\"0000.0000.RK\",'EstatusPropuesta',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusivePackage',root,\"0000.0000.RK\",'ExclusivePackage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusivePackageStartDate',root,\"0000.0000.RK\",'ExclusivePackageStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Evento',root,\"0000.0000.RK\",'Evento',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Exclusive',root,\"0000.0000.RK\",'Exclusive',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusiveEndDate',root,\"0000.0000.RK\",'ExclusiveEndDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusivePromotionEndDate',root,\"0000.0000.RK\",'ExclusivePromotionEndDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusiveStartDate',root,\"0000.0000.RK\",'ExclusiveStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusivePromotionStartDate',root,\"0000.0000.RK\",'ExclusivePromotionStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusivePromotion',root,\"0000.0000.RK\",'ExclusivePromotion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExclusivePackageEndDate',root,\"0000.0000.RK\",'ExclusivePackageEndDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExpressDelivery',root,\"0000.0000.RK\",'ExpressDelivery',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExpressDeliveryEndDate',root,\"0000.0000.RK\",'ExpressDeliveryEndDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ExpressDeliveryStartDate',root,\"0000.0000.RK\",'ExpressDeliveryStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE1',root,\"0000.0000.RK\",'FIBER_CODE1',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE2',root,\"0000.0000.RK\",'FIBER_CODE2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE3',root,\"0000.0000.RK\",'FIBER_CODE3',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE4',root,\"0000.0000.RK\",'FIBER_CODE4',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE5',root,\"0000.0000.RK\",'FIBER_CODE5',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE_DESCR1',root,\"0000.0000.RK\",'FIBER_CODE_DESCR1',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE_DESCR2',root,\"0000.0000.RK\",'FIBER_CODE_DESCR2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE_DESCR3',root,\"0000.0000.RK\",'FIBER_CODE_DESCR3',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE_DESCR4',root,\"0000.0000.RK\",'FIBER_CODE_DESCR4',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_CODE_DESCR5',root,\"0000.0000.RK\",'FIBER_CODE_DESCR5',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART1',root,\"0000.0000.RK\",'FIBER_PART1',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART2',root,\"0000.0000.RK\",'FIBER_PART2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART3',root,\"0000.0000.RK\",'FIBER_PART3',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART4',root,\"0000.0000.RK\",'FIBER_PART4',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART5',root,\"0000.0000.RK\",'FIBER_PART5',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FSH_ID',root,\"0000.0000.RK\",'FSH_ID',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FSH_COLLECTION',root,\"0000.0000.RK\",'FSH_COLLECTION',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FSH_SEASON',root,\"0000.0000.RK\",'FSH_SEASON',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FSH_SEASONS',root,\"0000.0000.RK\",'FSH_SEASONS',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FSH_SEASON_YEAR',root,\"0000.0000.RK\",'FSH_SEASON_YEAR',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FSH_THEME',root,\"0000.0000.RK\",'FSH_THEME',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FamilyDescription',root,\"0000.0000.RK\",'FamilyDescription',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FeatureBullet1',root,\"0000.0000.RK\",'FeatureBullet1',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FeatureBullet2',root,\"0000.0000.RK\",'FeatureBullet2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaInicioVigenciaCostoImportacion',root,\"0000.0000.RK\",'FechaInicioVigenciaCostoImportacion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaInicioVigenciaCostoNeto',root,\"0000.0000.RK\",'FechaInicioVigenciaCostoNeto',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaInicioVigenciaPrecioVenta',root,\"0000.0000.RK\",'FechaInicioVigenciaPrecioVenta',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaVencimiento',root,\"0000.0000.RK\",'FechaVencimiento',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FlagImportBySS',root,\"0000.0000.RK\",'FlagImportBySS',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Footnote',root,\"0000.0000.RK\",'Footnote',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('GROES',root,\"0000.0000.RK\",'GROES',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('GiftWithPurchase',root,\"0000.0000.RK\",'GiftWithPurchase',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('GiftWithPurchaseEndDate',root,\"0000.0000.RK\",'GiftWithPurchaseEndDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('GiftWithPurchaseStartDate',root,\"0000.0000.RK\",'GiftWithPurchaseStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('GradoDemoda',root,\"0000.0000.RK\",'GradoDemoda',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IDLastParent',root,\"0000.0000.RK\",'IDLastParent',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IDTallaERP',root,\"0000.0000.RK\",'IDTallaERP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IEPS',root,\"0000.0000.RK\",'IEPS',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IdentificaNegocio',root,\"0000.0000.RK\",'IdentificaNegocio',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ImpuestoALaVenta',root,\"0000.0000.RK\",'ImpuestoALaVenta',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IncidenciasCategorizacion',root,\"0000.0000.RK\",'IncidenciasCategorizacion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IncidenciasCompras',root,\"0000.0000.RK\",'IncidenciasCompras',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IndicadordeImpuesto',root,\"0000.0000.RK\",'IndicadordeImpuesto',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ItemGroup',root,\"0000.0000.RK\",'ItemGroup',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ItemGroup2',root,\"0000.0000.RK\",'ItemGroup2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('KLASE_CTE',root,\"0000.0000.RK\",'KLASE_CTE',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('LABOR',root,\"0000.0000.RK\",'LABOR',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('LABOR_S4H',root,\"0000.0000.RK\",'LABOR_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('LatestParts',root,\"0000.0000.RK\",'LatestParts',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('LatestPartsEndDate',root,\"0000.0000.RK\",'LatestPartsEndDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('LatestPartsStartDate',root,\"0000.0000.RK\",'LatestPartsStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('LicenseDescription',root,\"0000.0000.RK\",'LicenseDescription',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MAX_STACK',root,\"0000.0000.RK\",'MAX_STACK',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MTART_S4H',root,\"0000.0000.RK\",'MTART_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MVGR2',root,\"0000.0000.RK\",'MVGR2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MVGR5',root,\"0000.0000.RK\",'MVGR5',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Margen',root,\"0000.0000.RK\",'Margen',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MargenS4H',root,\"0000.0000.RK\",'MargenS4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MensajeCreacionSKU',root,\"0000.0000.RK\",'MensajeCreacionSKU',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MesdeEntregadeMercancIa',root,\"0000.0000.RK\",'MesdeEntregadeMercancIa',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MiraklSalesItemFamilyID',root,\"0000.0000.RK\",'MiraklSalesItemFamilyID',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NORMT',root,\"0000.0000.RK\",'NORMT',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NUMTP_S4H',root,\"0000.0000.RK\",'NUMTP_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Name',root,\"0000.0000.RK\",'Name',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NameExceptions',root,\"0000.0000.RK\",'NameExceptions',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NameGuide',root,\"0000.0000.RK\",'NameGuide',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Negocio',root,\"0000.0000.RK\",'Negocio',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NumberOfDetailImages',root,\"0000.0000.RK\",'NumberOfDetailImages',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NumberOfIllustrationImages',root,\"0000.0000.RK\",'NumberOfIllustrationImages',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NumberOfLiverpoolManuals',root,\"0000.0000.RK\",'NumberOfLiverpoolManuals',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NumberOfNoms',root,\"0000.0000.RK\",'NumberOfNoms',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('NumberOfSmoshImages',root,\"0000.0000.RK\",'NumberOfSmoshImages',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('OrigenSAP_STEP',root,\"0000.0000.RK\",'OrigenSAP_STEP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PLGTP',root,\"0000.0000.RK\",'PLGTP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Parent',root,\"0000.0000.RK\",'Parent',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ParentID',root,\"0000.0000.RK\",'ParentID',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PerfilDeRedondeo',root,\"0000.0000.RK\",'PerfilDeRedondeo',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PreSale',root,\"0000.0000.RK\",'PreSale',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PreSaleDateStart',root,\"0000.0000.RK\",'PreSaleDateStart',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PrecioSugeridocIVA',root,\"0000.0000.RK\",'PrecioSugeridocIVA',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PreSaleDateEnd',root,\"0000.0000.RK\",'PreSaleDateEnd',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProceedBuyerToQA',root,\"0000.0000.RK\",'ProceedBuyerToQA',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductName',root,\"0000.0000.RK\",'ProductName',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductType',root,\"0000.0000.RK\",'ProductType',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductTypeSAP2',root,\"0000.0000.RK\",'ProductTypeSAP2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductTypeSAPTEMP',root,\"0000.0000.RK\",'ProductTypeSAPTEMP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductTypeSAPTEMPSBB',root,\"0000.0000.RK\",'ProductTypeSAPTEMPSBB',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Producto',root,\"0000.0000.RK\",'Producto',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('PublicarEnATG',root,\"0000.0000.RK\",'PublicarEnATG',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('QARejectionMessage',root,\"0000.0000.RK\",'QARejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('RegionTEMP',root,\"0000.0000.RK\",'RegionTEMP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAPFeedErrors',root,\"0000.0000.RK\",'SAPFeedErrors',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAPSpart',root,\"0000.0000.RK\",'SAPSpart',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAP_BEHVO',root,\"0000.0000.RK\",'SAP_BEHVO',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SAP_ZZCOMA',root,\"0000.0000.RK\",'SAP_ZZCOMA',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SB_0002',root,\"0000.0000.RK\",'SB_0002',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SB_COLORES',root,\"0000.0000.RK\",'SB_COLORES',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SB_T_HARDLINE',root,\"0000.0000.RK\",'SB_T_HARDLINE',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SERVV',root,\"0000.0000.RK\",'SERVV',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKUCreationDate',root,\"0000.0000.RK\",'SKUCreationDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SalesItemShortDescription',root,\"0000.0000.RK\",'SalesItemShortDescription',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Section',root,\"0000.0000.RK\",'Section',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ShipPleasant',root,\"0000.0000.RK\",'ShipPleasant',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ShipPleasantStartDate',root,\"0000.0000.RK\",'ShipPleasantStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SistemaOrigen',root,\"0000.0000.RK\",'SistemaOrigen',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SkuType',root,\"0000.0000.RK\",'SkuType',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SortPrice',root,\"0000.0000.RK\",'SortPrice',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('StateSKU',root,\"0000.0000.RK\",'StateSKU',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('StatusOutWF',root,\"0000.0000.RK\",'StatusOutWF',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SupplierComplete',root,\"0000.0000.RK\",'SupplierComplete',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SupplierName',root,\"0000.0000.RK\",'SupplierName',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SupplierRejectionMessage',root,\"0000.0000.RK\",'SupplierRejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXESS4H',root,\"0000.0000.RK\",'TAXESS4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXESSAP',root,\"0000.0000.RK\",'TAXESSAP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXKM1_S4H',root,\"0000.0000.RK\",'TAXKM1_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXKM2_S4H',root,\"0000.0000.RK\",'TAXKM2_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXM3_S4H',root,\"0000.0000.RK\",'TAXM3_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TIPOOPERACION',root,\"0000.0000.RK\",'TIPOOPERACION',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TImportacion',root,\"0000.0000.RK\",'TImportacion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Temporada',root,\"0000.0000.RK\",'Temporada',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TerminosYCondicionesCR',root,\"0000.0000.RK\",'TerminosYCondicionesCR',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TextoAdicional',root,\"0000.0000.RK\",'TextoAdicional',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TipoDeEtiqueta',root,\"0000.0000.RK\",'TipoDeEtiqueta',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TipoDeTomaForo',root,\"0000.0000.RK\",'TipoDeTomaForo',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TypeMainBarCode',root,\"0000.0000.RK\",'TypeMainBarCode',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('VOLUMAttCalculado',root,\"0000.0000.RK\",'VOLUMAttCalculado',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Video',root,\"0000.0000.RK\",'Video',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ViewIncidenciasCategorizacion',root,\"0000.0000.RK\",'ViewIncidenciasCategorizacion',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('VolumetryCheck',root,\"0000.0000.RK\",'VolumetryCheck',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('WESCH',root,\"0000.0000.RK\",'WESCH',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('WF_Date_created_rejected',root,\"0000.0000.RK\",'WF_Date_created_rejected',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('WHERL',root,\"0000.0000.RK\",'WHERL',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('WebCategorizeRejectionMessage',root,\"0000.0000.RK\",'WebCategorizeRejectionMessage',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZGEWCJ',root,\"0000.0000.RK\",'ZGEWCJ',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZNUMV',root,\"0000.0000.RK\",'ZNUMV',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZOMSUDDateStart',root,\"0000.0000.RK\",'ZOMSUDDateStart',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZNUMVMKP',root,\"0000.0000.RK\",'ZNUMVMKP',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZOMSUDDateEnd',root,\"0000.0000.RK\",'ZOMSUDDateEnd',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZOMSUD',root,\"0000.0000.RK\",'ZOMSUD',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZZFEEM',root,\"0000.0000.RK\",'ZZFEEM',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZVOLCJcalculado',root,\"0000.0000.RK\",'ZVOLCJcalculado',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZZLIC_S4H',root,\"0000.0000.RK\",'ZZLIC_S4H',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('deliveryMethod',root,\"0000.0000.RK\",'deliveryMethod',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('esSuburbia',root,\"0000.0000.RK\",'esSuburbia',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('exclusiveDiscount',root,\"0000.0000.RK\",'exclusiveDiscount',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('exclusiveDiscountStartDate',root,\"0000.0000.RK\",'exclusiveDiscountStartDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('exclusiveDiscountEndDate',root,\"0000.0000.RK\",'exclusiveDiscountEndDate',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('isDuttyFree',root,\"0000.0000.RK\",'isDuttyFree',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('isMarketPlace',root,\"0000.0000.RK\",'isMarketPlace',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image1',root,\"0000.0000.RK\",'mirakl-Image1',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image10',root,\"0000.0000.RK\",'mirakl-Image10',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image11',root,\"0000.0000.RK\",'mirakl-Image11',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image12',root,\"0000.0000.RK\",'mirakl-Image12',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image13',root,\"0000.0000.RK\",'mirakl-Image13',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image14',root,\"0000.0000.RK\",'mirakl-Image14',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image15',root,\"0000.0000.RK\",'mirakl-Image15',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image2',root,\"0000.0000.RK\",'mirakl-Image2',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image3',root,\"0000.0000.RK\",'mirakl-Image3',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image4',root,\"0000.0000.RK\",'mirakl-Image4',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image5',root,\"0000.0000.RK\",'mirakl-Image5',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image6',root,\"0000.0000.RK\",'mirakl-Image6',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image7',root,\"0000.0000.RK\",'mirakl-Image7',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image8',root,\"0000.0000.RK\",'mirakl-Image8',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-Image9',root,\"0000.0000.RK\",'mirakl-Image9',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-acceptance-status',root,\"0000.0000.RK\",'mirakl-acceptance-status',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-integration-code',root,\"0000.0000.RK\",'mirakl-integration-code',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-integration-message',root,\"0000.0000.RK\",'mirakl-integration-message',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-product-id',root,\"0000.0000.RK\",'mirakl-product-id',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-rejection-message',root,\"0000.0000.RK\",'mirakl-rejection-message',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-rejection-reason',root,\"0000.0000.RK\",'mirakl-rejection-reason',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-validation-status',root,\"0000.0000.RK\",'mirakl-validation-status',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('mirakl-variant-group-id',root,\"0000.0000.RK\",'mirakl-variant-group-id',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('refundPolicy',root,\"0000.0000.RK\",'refundPolicy',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('specification',root,\"0000.0000.RK\",'specification',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('supplierShopId',root,\"0000.0000.RK\",'supplierShopId',-1)"))
				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('DutyFreeKey',root,\"0000.0000.RK\",'DutyFreeKey',-1)"))
			;
    private final org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows", rows);
    private final org.json.JSONArray columnsArticle = new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))
				.put(new org.json.JSONObject().put("identifier", "Article.PrevStatus"))
				.put(new org.json.JSONObject().put("identifier", "Article.ExternalStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('BrandName',root,\"0000.0000.RK\",'BrandName',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('CUBISCAN',root,\"0000.0000.RK\",'CUBISCAN',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('CostoEnMonedaExtranjera',root,\"0000.0000.RK\",'CostoEnMonedaExtranjera',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('CostoNetoSinIVA',root,\"0000.0000.RK\",'CostoNetoSinIVA',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('CostobrutoSinIVA',root,\"0000.0000.RK\",'CostobrutoSinIVA',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('CountryOfOrigin',root,\"0000.0000.RK\",'CountryOfOrigin',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Currency',root,\"0000.0000.RK\",'Currency',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento1',root,\"0000.0000.RK\",'Descuento1',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Descuento2',root,\"0000.0000.RK\",'Descuento2',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('EnvioMirakl',root,\"0000.0000.RK\",'EnvioMirakl',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ErroresDeSistema',root,\"0000.0000.RK\",'ErroresDeSistema',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('EstatusPropuesta',root,\"0000.0000.RK\",'EstatusPropuesta',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('FechaInicioVigenciaCostoImportacion',root,\"0000.0000.RK\",'FechaInicioVigenciaCostoImportacion',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('FechaInicioVigenciaCostoNeto',root,\"0000.0000.RK\",'FechaInicioVigenciaCostoNeto',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('FechaInicioVigenciaPrecioVenta',root,\"0000.0000.RK\",'FechaInicioVigenciaPrecioVenta',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Footnote',root,\"0000.0000.RK\",'Footnote',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('IDLastParent',root,\"0000.0000.RK\",'IDLastParent',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('IDTallaERP',root,\"0000.0000.RK\",'IDTallaERP',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('IdentificaNegocio',root,\"0000.0000.RK\",'IdentificaNegocio',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('IndicadordeImpuesto',root,\"0000.0000.RK\",'IndicadordeImpuesto',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ItemGroup',root,\"0000.0000.RK\",'ItemGroup',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MensajeCreacionSKU',root,\"0000.0000.RK\",'MensajeCreacionSKU',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('NUMTP_S4H',root,\"0000.0000.RK\",'NUMTP_S4H',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Name',root,\"0000.0000.RK\",'Name',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('NameGuide',root,\"0000.0000.RK\",'NameGuide',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('Parent',root,\"0000.0000.RK\",'Parent',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ParentID',root,\"0000.0000.RK\",'ParentID',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('PrecioSugeridocIVA',root,\"0000.0000.RK\",'PrecioSugeridocIVA',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ProductType',root,\"0000.0000.RK\",'ProductType',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ProductTypeSAP2',root,\"0000.0000.RK\",'ProductTypeSAP2',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ProductTypeSAPTEMP',root,\"0000.0000.RK\",'ProductTypeSAPTEMP',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('PublicarEnATG',root,\"0000.0000.RK\",'PublicarEnATG',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SAPSpart',root,\"0000.0000.RK\",'SAPSpart',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SAP_BEHVO',root,\"0000.0000.RK\",'SAP_BEHVO',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SB_0002',root,\"0000.0000.RK\",'SB_0002',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SB_COLORES',root,\"0000.0000.RK\",'SB_COLORES',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SKUCreationDate',root,\"0000.0000.RK\",'SKUCreationDate',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SistemaOrigen',root,\"0000.0000.RK\",'SistemaOrigen',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SkuType',root,\"0000.0000.RK\",'SkuType',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('StateSKU',root,\"0000.0000.RK\",'StateSKU',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('TamanoUnicoT',root,\"0000.0000.RK\",'TamanoUnicoT',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('TerminosYCondicionesCR',root,\"0000.0000.RK\",'TerminosYCondicionesCR',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('TipoDeEtiqueta',root,\"0000.0000.RK\",'TipoDeEtiqueta',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('TypeMainBarCode',root,\"0000.0000.RK\",'TypeMainBarCode',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('WF_Date_created_rejected',root,\"0000.0000.RK\",'WF_Date_created_rejected',-1)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('isMarketPlace',root,\"0000.0000.RK\",'isMarketPlace',-1)"))
    		;
    private final org.json.JSONArray rowsArticle = new org.json.JSONArray();
    private final org.json.JSONObject requestArticle = new org.json.JSONObject().put("columns", columnsArticle).put("rows", rowsArticle);
    private final java.util.Map<String, String> childParent = new java.util.HashMap<>();
    private int lacuentaVars = 0;
    private final java.util.Set<String> losEncontrados = new java.util.TreeSet<>();
    private final org.json.JSONArray products = new org.json.JSONArray();
    private final java.util.Map<String, String> internalToExternalStatusMap = loadExternalStatusMap();
    private final java.util.Map<String, String> externalStatusLabels = loadExternalStatusLabelsMap();
    
    private final PubSubGCP pub = new PubSubGCP();
    
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

    private java.util.Map<String, String> loadExternalStatusLabelsMap() {
    	java.util.Map<String, String> internalToExternalStatusMap = new java.util.HashMap<>();
    	try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "global_lookups", "ExternalStatus").toFile())))){
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
    
    private void processProduct(Product product) {
    	if(product.getParentId().matches("^(S?[0-9]+)")) {
    		processChild(product, "", "", "");
    		return;
    	}
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	String calculatedWFAtt = null;
    	String statusSKU = null;
    	String fotoTomadaLiverpool = null;
    	String business = null;
    	String[] bundle = null;
    	String prevStatus = null;
    	String currentStatus = null;
    	String externalStatus = null;
    	String enriquecidoEnForo = null;
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
    		Value calculatedWFAttVal = valMap.get("CalculatedWF_Att");
    		Value fotoTomadaLiverpoolVal = valMap.get("FotoTomadaLiverpool");
    		Value statusSKUVal = valMap.get("StateSKU");
    		
    		calculatedWFAtt = calculatedWFAttVal == null ? "" : calculatedWFAttVal.getId() == null ? calculatedWFAttVal.getText() == null ? "" : calculatedWFAttVal.getText() : calculatedWFAttVal.getId();
    		fotoTomadaLiverpool = fotoTomadaLiverpoolVal == null ? "" : fotoTomadaLiverpoolVal.getId() == null ? fotoTomadaLiverpoolVal.getText() == null ? "" : fotoTomadaLiverpoolVal.getText() : fotoTomadaLiverpoolVal.getId();
    		statusSKU = statusSKUVal == null ? "" : statusSKUVal.getText() == null ? statusSKUVal.getId() == null ? "" : statusSKUVal.getId() : statusSKUVal.getText() ;
    		org.json.JSONArray vals = new org.json.JSONArray();
    		Value ar = valMap.get("AR");
    		Value arurl = valMap.get("ARURL");
    		Value anoestacion = valMap.get("AnoEstacion");
    		Value approveddatecalc = valMap.get("ApprovedDateCalc");
    		Value argumentodeventa = valMap.get("ArgumentoDeVenta");
    		Value assetrejectionmessage = valMap.get("AssetRejectionMessage");
    		Value brandIdS4h = valMap.get("BRAND_ID_S4H");
    		Value bwscl = valMap.get("BWSCL");
    		Value bwvor = valMap.get("BWVOR");
    		Value bluecategorizerejectionmessage = valMap.get("BlueCategorizeRejectionMessage");
    		Value brandidatg = valMap.get("BrandIDATG");
    		Value brandname = valMap.get("BrandName");
    		Value brandnameatg = valMap.get("BrandNameATG");
    		Value brandowner = valMap.get("BrandOwner");
    		Value buyerrejectionmessage = valMap.get("BuyerRejectionMessage");
    		Value cstfld03 = valMap.get("CSTFLD03");
    		Value cubiscan = valMap.get("CUBISCAN");
    		Value calculatedwfAtt = valMap.get("CalculatedWF_Att");
    		Value calculatedInstatewf = valMap.get("Calculated_inStateWF");
    		Value categorymanagerrejectionmessage = valMap.get("CategoryManagerRejectionMessage");
    		Value certificadosostenible = valMap.get("CertificadoSostenible");
    		Value coleccion = valMap.get("Coleccion");
    		Value collectiondescriptionsap = valMap.get("CollectionDescriptionSAP");
    		Value comentarioproyectoscomerciales = valMap.get("ComentarioProyectosComerciales");
    		Value consignacion = valMap.get("Consignacion");
    		Value coordinadoidsap = valMap.get("CoordinadoIDSAP");
    		Value costoenmonedaextranjera = valMap.get("CostoEnMonedaExtranjera");
    		Value costonetosiniva = valMap.get("CostoNetoSinIVA");
    		Value costobrutosiniva = valMap.get("CostobrutoSinIVA");
    		Value countryoforigin = valMap.get("CountryOfOrigin");
    		Value currency = valMap.get("Currency");
    		Value deletecheck = valMap.get("DeleteCheck");
    		Value descriptionlong = valMap.get("DescriptionLong");
    		Value descriptionlong2 = valMap.get("DescriptionLong2");
    		Value descriptiontable = valMap.get("DescriptionTable");
    		Value descriptionweb = valMap.get("DescriptionWeb");
    		Value descuento1 = valMap.get("Descuento1");
    		Value descuento2 = valMap.get("Descuento2");
    		Value direction = valMap.get("Direction");
    		Value envAtg = valMap.get("ENV_ATG");
    		Value extwgS4h = valMap.get("EXTWG_S4H");
    		Value embedcodewap = valMap.get("EmbedCodeWAP");
    		Value embedcodeweb = valMap.get("EmbedCodeWEB");
    		Value enrichmentrejectionmessage = valMap.get("EnrichmentRejectionMessage");
    		Value envioimagen = valMap.get("EnvioImagen");
    		Value enviointernacional = valMap.get("EnvioInternacional");
    		Value enviomirakl = valMap.get("EnvioMirakl");
    		Value erroresdesistema = valMap.get("ErroresDeSistema");
    		Value essostenible = valMap.get("EsSostenible");
    		Value essosteniblevad = valMap.get("EsSostenibleVAD");
    		Value estatuspropuesta = valMap.get("EstatusPropuesta");
    		Value exclusivepackage = valMap.get("ExclusivePackage");
    		Value exclusivepackagestartdate = valMap.get("ExclusivePackageStartDate");
    		Value evento = valMap.get("Evento");
    		Value exclusive = valMap.get("Exclusive");
    		Value exclusiveenddate = valMap.get("ExclusiveEndDate");
    		Value exclusivepromotionenddate = valMap.get("ExclusivePromotionEndDate");
    		Value exclusivestartdate = valMap.get("ExclusiveStartDate");
    		Value exclusivepromotionstartdate = valMap.get("ExclusivePromotionStartDate");
    		Value exclusivepromotion = valMap.get("ExclusivePromotion");
    		Value exclusivepackageenddate = valMap.get("ExclusivePackageEndDate");
    		Value expressdelivery = valMap.get("ExpressDelivery");
    		Value expressdeliveryenddate = valMap.get("ExpressDeliveryEndDate");
    		Value expressdeliverystartdate = valMap.get("ExpressDeliveryStartDate");
    		Value fiberCode1 = valMap.get("FIBER_CODE1");
    		Value fiberCode2 = valMap.get("FIBER_CODE2");
    		Value fiberCode3 = valMap.get("FIBER_CODE3");
    		Value fiberCode4 = valMap.get("FIBER_CODE4");
    		Value fiberCode5 = valMap.get("FIBER_CODE5");
    		Value fiberCodeDescr1 = valMap.get("FIBER_CODE_DESCR1");
    		Value fiberCodeDescr2 = valMap.get("FIBER_CODE_DESCR2");
    		Value fiberCodeDescr3 = valMap.get("FIBER_CODE_DESCR3");
    		Value fiberCodeDescr4 = valMap.get("FIBER_CODE_DESCR4");
    		Value fiberCodeDescr5 = valMap.get("FIBER_CODE_DESCR5");
    		Value fiberPart1 = valMap.get("FIBER_PART1");
    		Value fiberPart2 = valMap.get("FIBER_PART2");
    		Value fiberPart3 = valMap.get("FIBER_PART3");
    		Value fiberPart4 = valMap.get("FIBER_PART4");
    		Value fiberPart5 = valMap.get("FIBER_PART5");
    		Value fshId = valMap.get("FSH_ID");
    		Value fshCollection = valMap.get("FSH_COLLECTION");
    		Value fshSeason = valMap.get("FSH_SEASON");
    		Value fshSeasons = valMap.get("FSH_SEASONS");
    		Value fshSeasonYear = valMap.get("FSH_SEASON_YEAR");
    		Value fshTheme = valMap.get("FSH_THEME");
    		Value familydescription = valMap.get("FamilyDescription");
    		Value firstDateApprove = valMap.get("FirstDateApprove");
    		Value featurebullet1 = valMap.get("FeatureBullet1");
    		Value featurebullet2 = valMap.get("FeatureBullet2");
    		Value fechainiciovigenciacostoimportacion = valMap.get("FechaInicioVigenciaCostoImportacion");
    		Value fechainiciovigenciacostoneto = valMap.get("FechaInicioVigenciaCostoNeto");
    		Value fechainiciovigenciaprecioventa = valMap.get("FechaInicioVigenciaPrecioVenta");
    		Value fechavencimiento = valMap.get("FechaVencimiento");
    		Value flagimportbyss = valMap.get("FlagImportBySS");
    		Value footnote = valMap.get("Footnote");
    		Value fototomadaliverpool = valMap.get("FotoTomadaLiverpool");
    		Value groes = valMap.get("GROES");
    		Value giftwithpurchase = valMap.get("GiftWithPurchase");
    		Value giftwithpurchaseenddate = valMap.get("GiftWithPurchaseEndDate");
    		Value giftwithpurchasestartdate = valMap.get("GiftWithPurchaseStartDate");
    		Value gradodemoda = valMap.get("GradoDemoda");
    		Value idlastparent = valMap.get("IDLastParent");
    		Value idtallaerp = valMap.get("IDTallaERP");
    		Value ieps = valMap.get("IEPS");
    		Value identificanegocio = valMap.get("IdentificaNegocio");
    		Value impuestoalaventa = valMap.get("ImpuestoALaVenta");
    		Value incidenciascategorizacion = valMap.get("IncidenciasCategorizacion");
    		Value incidenciascompras = valMap.get("IncidenciasCompras");
    		Value indicadordeimpuesto = valMap.get("IndicadordeImpuesto");
    		Value itemgroup = valMap.get("ItemGroup");
    		Value itemgroup2 = valMap.get("ItemGroup2");
    		Value itemgroups4h = valMap.get("ItemGroupS4H");
    		Value klaseCte = valMap.get("KLASE_CTE");
    		Value labor = valMap.get("LABOR");
    		Value laborS4h = valMap.get("LABOR_S4H");
    		Value latestparts = valMap.get("LatestParts");
    		Value latestpartsenddate = valMap.get("LatestPartsEndDate");
    		Value latestpartsstartdate = valMap.get("LatestPartsStartDate");
    		Value licensedescription = valMap.get("LicenseDescription");
    		Value maxStack = valMap.get("MAX_STACK");
    		Value mtartS4h = valMap.get("MTART_S4H");
    		Value mvgr2 = valMap.get("MVGR2");
    		Value mvgr5 = valMap.get("MVGR5");
    		Value mainbarcode = valMap.get("MainBarCode");
    		Value mainbarcodes4h = valMap.get("MainBarCodeS4H");
    		Value margen = valMap.get("Margen");
    		Value margenS4h = valMap.get("MargenS4H");
    		Value mensajecreacionsku = valMap.get("MensajeCreacionSKU");
    		Value mesdeentregademercancia = valMap.get("MesdeEntregadeMercancIa");
    		Value miraklsalesitemfamilyid = valMap.get("MiraklSalesItemFamilyID");
    		Value normt = valMap.get("NORMT");
    		Value numtpS4h = valMap.get("NUMTP_S4H");
    		Value name = valMap.get("Name");
    		Value nameexceptions = valMap.get("NameExceptions");
    		Value nameguide = valMap.get("NameGuide");
    		Value negocio = valMap.get("Negocio");
    		Value numberofdetailimages = valMap.get("NumberOfDetailImages");
    		Value numberofillustrationimages = valMap.get("NumberOfIllustrationImages");
    		Value numberofliverpoolmanuals = valMap.get("NumberOfLiverpoolManuals");
    		Value numberofnoms = valMap.get("NumberOfNoms");
    		Value numberofsmoshimages = valMap.get("NumberOfSmoshImages");
    		Value origensapStep = valMap.get("OrigenSAP_STEP");
    		Value plgtp = valMap.get("PLGTP");
    		Value parent = valMap.get("Parent");
    		Value parentid = valMap.get("ParentID");
    		Value perfilderedondeo = valMap.get("PerfilDeRedondeo");
    		Value presale = valMap.get("PreSale");
    		Value presaledatestart = valMap.get("PreSaleDateStart");
    		Value preciosugeridociva = valMap.get("PrecioSugeridocIVA");
    		Value presaledateend = valMap.get("PreSaleDateEnd");
    		Value proceedbuyertoqa = valMap.get("ProceedBuyerToQA");
    		Value productname = valMap.get("ProductName");
    		Value producttype = valMap.get("ProductType");
    		Value producttypesap = valMap.get("ProductTypeSAP");
    		Value producttypesap2 = valMap.get("ProductTypeSAP2");
    		Value producttypesaptemp = valMap.get("ProductTypeSAPTEMP");
    		Value producttypesaptempsbb = valMap.get("ProductTypeSAPTEMPSBB");
    		Value producto = valMap.get("Producto");
    		Value publicarenatg = valMap.get("PublicarEnATG");
    		Value qarejectionmessage = valMap.get("QARejectionMessage");
    		Value regiontemp = valMap.get("RegionTEMP");
    		Value sapfeederrors = valMap.get("SAPFeedErrors");
    		Value sapobjecttype = valMap.get("SAPObjectType");
    		Value sapspart = valMap.get("SAPSpart");
    		Value sapBehvo = valMap.get("SAP_BEHVO");
    		Value sapZzcoma = valMap.get("SAP_ZZCOMA");
    		Value sb0002 = valMap.get("SB_0002");
    		Value sbColores = valMap.get("SB_COLORES");
    		Value sbTHardline = valMap.get("SB_T_HARDLINE");
    		Value servv = valMap.get("SERVV");
    		Value sku = valMap.get("SKU");
    		Value skucreationdate = valMap.get("SKUCreationDate");
    		Value salesitemshortdescription = valMap.get("SalesItemShortDescription");
    		Value section = valMap.get("Section");
    		Value shippleasant = valMap.get("ShipPleasant");
    		Value shippleasantstartdate = valMap.get("ShipPleasantStartDate");
    		Value sistemaorigen = valMap.get("SistemaOrigen");
    		Value skutype = valMap.get("SkuType");
    		Value sortprice = valMap.get("SortPrice");
    		Value statesku = valMap.get("StateSKU");
    		Value statusoutwf = valMap.get("StatusOutWF");
    		Value suppliercomplete = valMap.get("SupplierComplete");
    		Value supplierid = valMap.get("SupplierID");
    		Value suppliername = valMap.get("SupplierName");
    		Value supplierpartnumber = valMap.get("SupplierPartNumber");
    		Value supplierrejectionmessage = valMap.get("SupplierRejectionMessage");
    		Value taxess4h = valMap.get("TAXESS4H");
    		Value taxessap = valMap.get("TAXESSAP");
    		Value taxkm1S4h = valMap.get("TAXKM1_S4H");
    		Value taxkm2S4h = valMap.get("TAXKM2_S4H");
    		Value taxm3S4h = valMap.get("TAXM3_S4H");
    		Value tipooperacion = valMap.get("TIPOOPERACION");
    		Value timportacion = valMap.get("TImportacion");
    		Value temporada = valMap.get("Temporada");
    		Value terminosycondicionescr = valMap.get("TerminosYCondicionesCR");
    		Value textoadicional = valMap.get("TextoAdicional");
    		Value tipodeetiqueta = valMap.get("TipoDeEtiqueta");
    		Value tipodetomaforo = valMap.get("TipoDeTomaForo");
    		Value typemainbarcode = valMap.get("TypeMainBarCode");
    		Value volumattcalculado = valMap.get("VOLUMAttCalculado");
    		Value video = valMap.get("Video");
    		Value viewincidenciascategorizacion = valMap.get("ViewIncidenciasCategorizacion");
    		Value volumetrycheck = valMap.get("VolumetryCheck");
    		Value wesch = valMap.get("WESCH");
    		Value wfDateCreatedRejected = valMap.get("WF_Date_created_rejected");
    		Value wherl = valMap.get("WHERL");
    		Value webcategorizerejectionmessage = valMap.get("WebCategorizeRejectionMessage");
    		Value zgewcj = valMap.get("ZGEWCJ");
    		Value znumv = valMap.get("ZNUMV");
    		Value zomsuddatestart = valMap.get("ZOMSUDDateStart");
    		Value znumvmkp = valMap.get("ZNUMVMKP");
    		Value zomsuddateend = valMap.get("ZOMSUDDateEnd");
    		Value zomsud = valMap.get("ZOMSUD");
    		Value zzfeem = valMap.get("ZZFEEM");
    		Value zvolcjcalculado = valMap.get("ZVOLCJcalculado");
    		Value zzlicS4h = valMap.get("ZZLIC_S4H");
    		Value deliverymethod = valMap.get("deliveryMethod");
    		Value essuburbia = valMap.get("esSuburbia");
    		Value exclusivediscount = valMap.get("exclusiveDiscount");
    		Value exclusivediscountstartdate = valMap.get("exclusiveDiscountStartDate");
    		Value exclusivediscountenddate = valMap.get("exclusiveDiscountEndDate");
    		Value isduttyfree = valMap.get("isDuttyFree");
    		Value ismarketplace = valMap.get("isMarketPlace");
    		Value miraklImage1 = valMap.get("mirakl-Image1");
    		Value miraklImage10 = valMap.get("mirakl-Image10");
    		Value miraklImage11 = valMap.get("mirakl-Image11");
    		Value miraklImage12 = valMap.get("mirakl-Image12");
    		Value miraklImage13 = valMap.get("mirakl-Image13");
    		Value miraklImage14 = valMap.get("mirakl-Image14");
    		Value miraklImage15 = valMap.get("mirakl-Image15");
    		Value miraklImage2 = valMap.get("mirakl-Image2");
    		Value miraklImage3 = valMap.get("mirakl-Image3");
    		Value miraklImage4 = valMap.get("mirakl-Image4");
    		Value miraklImage5 = valMap.get("mirakl-Image5");
    		Value miraklImage6 = valMap.get("mirakl-Image6");
    		Value miraklImage7 = valMap.get("mirakl-Image7");
    		Value miraklImage8 = valMap.get("mirakl-Image8");
    		Value miraklImage9 = valMap.get("mirakl-Image9");
    		Value miraklAcceptanceStatus = valMap.get("mirakl-acceptance-status");
    		Value miraklIntegrationCode = valMap.get("mirakl-integration-code");
    		Value miraklIntegrationMessage = valMap.get("mirakl-integration-message");
    		Value miraklProductId = valMap.get("mirakl-product-id");
    		Value miraklRejectionMessage = valMap.get("mirakl-rejection-message");
    		Value miraklRejectionReason = valMap.get("mirakl-rejection-reason");
    		Value miraklValidationStatus = valMap.get("mirakl-validation-status");
    		Value miraklVariantGroupId = valMap.get("mirakl-variant-group-id");
    		Value refundpolicy = valMap.get("refundPolicy");
    		Value specification = valMap.get("specification");
    		Value suppliershopid = valMap.get("supplierShopId");
    		Value dutyfreekey = valMap.get("DutyFreeKey");
    		String arStr = ar == null ? "" : ar.getId() != null ? ar.getId() : ar.getText() == null ? "" : ar.getText() ;
    		String arurlStr = arurl == null ? "" : arurl.getId() != null ? arurl.getId() : arurl.getText() == null ? "" : arurl.getText() ;
    		String anoestacionStr = anoestacion == null ? "" : anoestacion.getId() != null ? anoestacion.getId() : anoestacion.getText() == null ? "" : anoestacion.getText() ;
    		String approveddatecalcStr = approveddatecalc == null ? "" : approveddatecalc.getId() != null ? approveddatecalc.getId() : approveddatecalc.getText() == null ? "" : approveddatecalc.getText() ;
    		String argumentodeventaStr = argumentodeventa == null ? "" : argumentodeventa.getId() != null ? argumentodeventa.getId() : argumentodeventa.getText() == null ? "" : argumentodeventa.getText() ;
    		String assetrejectionmessageStr = assetrejectionmessage == null ? "" : assetrejectionmessage.getId() != null ? assetrejectionmessage.getId() : assetrejectionmessage.getText() == null ? "" : assetrejectionmessage.getText() ;
    		String brandIdS4hStr = brandIdS4h == null ? "" : brandIdS4h.getId() != null ? brandIdS4h.getId() : brandIdS4h.getText() == null ? "" : brandIdS4h.getText() ;
    		String bwsclStr = bwscl == null ? "" : bwscl.getId() != null ? bwscl.getId() : bwscl.getText() == null ? "" : bwscl.getText() ;
    		String bwvorStr = bwvor == null ? "" : bwvor.getId() != null ? bwvor.getId() : bwvor.getText() == null ? "" : bwvor.getText() ;
    		String bluecategorizerejectionmessageStr = bluecategorizerejectionmessage == null ? "" : bluecategorizerejectionmessage.getId() != null ? bluecategorizerejectionmessage.getId() : bluecategorizerejectionmessage.getText() == null ? "" : bluecategorizerejectionmessage.getText() ;
    		String brandidatgStr = brandidatg == null ? "" : brandidatg.getId() != null ? brandidatg.getId() : brandidatg.getText() == null ? "" : brandidatg.getText() ;
    		String brandnameStr = brandname == null ? "" : brandname.getId() != null ? brandname.getId() : brandname.getText() == null ? "" : brandname.getText() ;
    		String brandnameatgStr = brandnameatg == null ? "" : brandnameatg.getId() != null ? brandnameatg.getId() : brandnameatg.getText() == null ? "" : brandnameatg.getText() ;
    		String brandownerStr = brandowner == null ? "" : brandowner.getId() != null ? brandowner.getId() : brandowner.getText() == null ? "" : brandowner.getText() ;
    		String buyerrejectionmessageStr = buyerrejectionmessage == null ? "" : buyerrejectionmessage.getId() != null ? buyerrejectionmessage.getId() : buyerrejectionmessage.getText() == null ? "" : buyerrejectionmessage.getText() ;
    		String cstfld03Str = cstfld03 == null ? "" : cstfld03.getId() != null ? cstfld03.getId() : cstfld03.getText() == null ? "" : cstfld03.getText() ;
    		String cubiscanStr = cubiscan == null ? "" : cubiscan.getId() != null ? cubiscan.getId() : cubiscan.getText() == null ? "" : cubiscan.getText() ;
    		String calculatedwfAttStr = calculatedwfAtt == null ? "" : calculatedwfAtt.getId() != null ? calculatedwfAtt.getId() : calculatedwfAtt.getText() == null ? "" : calculatedwfAtt.getText() ;
    		String calculatedInstatewfStr = calculatedInstatewf == null ? "" : calculatedInstatewf.getId() != null ? calculatedInstatewf.getId() : calculatedInstatewf.getText() == null ? "" : calculatedInstatewf.getText() ;
    		String categorymanagerrejectionmessageStr = categorymanagerrejectionmessage == null ? "" : categorymanagerrejectionmessage.getId() != null ? categorymanagerrejectionmessage.getId() : categorymanagerrejectionmessage.getText() == null ? "" : categorymanagerrejectionmessage.getText() ;
    		String certificadosostenibleStr = certificadosostenible == null ? "" : certificadosostenible.getId() != null ? certificadosostenible.getId() : certificadosostenible.getText() == null ? "" : certificadosostenible.getText() ;
    		String coleccionStr = coleccion == null ? "" : coleccion.getId() != null ? coleccion.getId() : coleccion.getText() == null ? "" : coleccion.getText() ;
    		String collectiondescriptionsapStr = collectiondescriptionsap == null ? "" : collectiondescriptionsap.getId() != null ? collectiondescriptionsap.getId() : collectiondescriptionsap.getText() == null ? "" : collectiondescriptionsap.getText() ;
    		String comentarioproyectoscomercialesStr = comentarioproyectoscomerciales == null ? "" : comentarioproyectoscomerciales.getId() != null ? comentarioproyectoscomerciales.getId() : comentarioproyectoscomerciales.getText() == null ? "" : comentarioproyectoscomerciales.getText() ;
    		String consignacionStr = consignacion == null ? "" : consignacion.getId() != null ? consignacion.getId() : consignacion.getText() == null ? "" : consignacion.getText() ;
    		String coordinadoidsapStr = coordinadoidsap == null ? "" : coordinadoidsap.getId() != null ? coordinadoidsap.getId() : coordinadoidsap.getText() == null ? "" : coordinadoidsap.getText() ;
    		String costoenmonedaextranjeraStr = costoenmonedaextranjera == null ? "" : costoenmonedaextranjera.getId() != null ? costoenmonedaextranjera.getId() : costoenmonedaextranjera.getText() == null ? "" : costoenmonedaextranjera.getText() ;
    		String costonetosinivaStr = costonetosiniva == null ? "" : costonetosiniva.getId() != null ? costonetosiniva.getId() : costonetosiniva.getText() == null ? "" : costonetosiniva.getText() ;
    		String costobrutosinivaStr = costobrutosiniva == null ? "" : costobrutosiniva.getId() != null ? costobrutosiniva.getId() : costobrutosiniva.getText() == null ? "" : costobrutosiniva.getText() ;
    		String countryoforiginStr = countryoforigin == null ? "" : countryoforigin.getId() != null ? countryoforigin.getId() : countryoforigin.getText() == null ? "" : countryoforigin.getText() ;
    		String currencyStr = currency == null ? "" : currency.getId() != null ? currency.getId() : currency.getText() == null ? "" : currency.getText() ;
    		String deletecheckStr = deletecheck == null ? "" : deletecheck.getId() != null ? deletecheck.getId() : deletecheck.getText() == null ? "" : deletecheck.getText() ;
    		String descriptionlongStr = descriptionlong == null ? "" : descriptionlong.getId() != null ? descriptionlong.getId() : descriptionlong.getText() == null ? "" : descriptionlong.getText() ;
    		String descriptionlong2Str = descriptionlong2 == null ? "" : descriptionlong2.getId() != null ? descriptionlong2.getId() : descriptionlong2.getText() == null ? "" : descriptionlong2.getText() ;
    		String descriptiontableStr = descriptiontable == null ? "" : descriptiontable.getId() != null ? descriptiontable.getId() : descriptiontable.getText() == null ? "" : descriptiontable.getText() ;
    		String descriptionwebStr = descriptionweb == null ? "" : descriptionweb.getId() != null ? descriptionweb.getId() : descriptionweb.getText() == null ? "" : descriptionweb.getText() ;
    		String descuento1Str = descuento1 == null ? "" : descuento1.getId() != null ? descuento1.getId() : descuento1.getText() == null ? "" : descuento1.getText() ;
    		String descuento2Str = descuento2 == null ? "" : descuento2.getId() != null ? descuento2.getId() : descuento2.getText() == null ? "" : descuento2.getText() ;
    		String directionStr = direction == null ? "" : direction.getId() != null ? direction.getId() : direction.getText() == null ? "" : direction.getText() ;
    		String envAtgStr = envAtg == null ? "" : envAtg.getId() != null ? envAtg.getId() : envAtg.getText() == null ? "" : envAtg.getText() ;
    		String extwgS4hStr = extwgS4h == null ? "" : extwgS4h.getId() != null ? extwgS4h.getId() : extwgS4h.getText() == null ? "" : extwgS4h.getText() ;
    		String embedcodewapStr = embedcodewap == null ? "" : embedcodewap.getId() != null ? embedcodewap.getId() : embedcodewap.getText() == null ? "" : embedcodewap.getText() ;
    		String embedcodewebStr = embedcodeweb == null ? "" : embedcodeweb.getId() != null ? embedcodeweb.getId() : embedcodeweb.getText() == null ? "" : embedcodeweb.getText() ;
    		String enrichmentrejectionmessageStr = enrichmentrejectionmessage == null ? "" : enrichmentrejectionmessage.getId() != null ? enrichmentrejectionmessage.getId() : enrichmentrejectionmessage.getText() == null ? "" : enrichmentrejectionmessage.getText() ;
    		String envioimagenStr = envioimagen == null ? "" : envioimagen.getId() != null ? envioimagen.getId() : envioimagen.getText() == null ? "" : envioimagen.getText() ;
    		String enviointernacionalStr = enviointernacional == null ? "" : enviointernacional.getId() != null ? enviointernacional.getId() : enviointernacional.getText() == null ? "" : enviointernacional.getText() ;
    		String enviomiraklStr = enviomirakl == null ? "" : enviomirakl.getId() != null ? enviomirakl.getId() : enviomirakl.getText() == null ? "" : enviomirakl.getText() ;
    		String erroresdesistemaStr = erroresdesistema == null ? "" : erroresdesistema.getId() != null ? erroresdesistema.getId() : erroresdesistema.getText() == null ? "" : erroresdesistema.getText() ;
    		String essostenibleStr = essostenible == null ? "" : essostenible.getId() != null ? essostenible.getId() : essostenible.getText() == null ? "" : essostenible.getText() ;
    		String essosteniblevadStr = essosteniblevad == null ? "" : essosteniblevad.getId() != null ? essosteniblevad.getId() : essosteniblevad.getText() == null ? "" : essosteniblevad.getText() ;
    		String estatuspropuestaStr = estatuspropuesta == null ? "" : estatuspropuesta.getId() != null ? estatuspropuesta.getId() : estatuspropuesta.getText() == null ? "" : estatuspropuesta.getText() ;
    		String exclusivepackageStr = exclusivepackage == null ? "" : exclusivepackage.getId() != null ? exclusivepackage.getId() : exclusivepackage.getText() == null ? "" : exclusivepackage.getText() ;
    		String exclusivepackagestartdateStr = exclusivepackagestartdate == null ? "" : exclusivepackagestartdate.getId() != null ? exclusivepackagestartdate.getId() : exclusivepackagestartdate.getText() == null ? "" : exclusivepackagestartdate.getText() ;
    		String eventoStr = evento == null ? "" : evento.getId() != null ? evento.getId() : evento.getText() == null ? "" : evento.getText() ;
    		String exclusiveStr = exclusive == null ? "" : exclusive.getId() != null ? exclusive.getId() : exclusive.getText() == null ? "" : exclusive.getText() ;
    		String exclusiveenddateStr = exclusiveenddate == null ? "" : exclusiveenddate.getId() != null ? exclusiveenddate.getId() : exclusiveenddate.getText() == null ? "" : exclusiveenddate.getText() ;
    		String exclusivepromotionenddateStr = exclusivepromotionenddate == null ? "" : exclusivepromotionenddate.getId() != null ? exclusivepromotionenddate.getId() : exclusivepromotionenddate.getText() == null ? "" : exclusivepromotionenddate.getText() ;
    		String exclusivestartdateStr = exclusivestartdate == null ? "" : exclusivestartdate.getId() != null ? exclusivestartdate.getId() : exclusivestartdate.getText() == null ? "" : exclusivestartdate.getText() ;
    		String exclusivepromotionstartdateStr = exclusivepromotionstartdate == null ? "" : exclusivepromotionstartdate.getId() != null ? exclusivepromotionstartdate.getId() : exclusivepromotionstartdate.getText() == null ? "" : exclusivepromotionstartdate.getText() ;
    		String exclusivepromotionStr = exclusivepromotion == null ? "" : exclusivepromotion.getId() != null ? exclusivepromotion.getId() : exclusivepromotion.getText() == null ? "" : exclusivepromotion.getText() ;
    		String exclusivepackageenddateStr = exclusivepackageenddate == null ? "" : exclusivepackageenddate.getId() != null ? exclusivepackageenddate.getId() : exclusivepackageenddate.getText() == null ? "" : exclusivepackageenddate.getText() ;
    		String expressdeliveryStr = expressdelivery == null ? "" : expressdelivery.getId() != null ? expressdelivery.getId() : expressdelivery.getText() == null ? "" : expressdelivery.getText() ;
    		String expressdeliveryenddateStr = expressdeliveryenddate == null ? "" : expressdeliveryenddate.getId() != null ? expressdeliveryenddate.getId() : expressdeliveryenddate.getText() == null ? "" : expressdeliveryenddate.getText() ;
    		String expressdeliverystartdateStr = expressdeliverystartdate == null ? "" : expressdeliverystartdate.getId() != null ? expressdeliverystartdate.getId() : expressdeliverystartdate.getText() == null ? "" : expressdeliverystartdate.getText() ;
    		String fiberCode1Str = fiberCode1 == null ? "" : fiberCode1.getId() != null ? fiberCode1.getId() : fiberCode1.getText() == null ? "" : fiberCode1.getText() ;
    		String fiberCode2Str = fiberCode2 == null ? "" : fiberCode2.getId() != null ? fiberCode2.getId() : fiberCode2.getText() == null ? "" : fiberCode2.getText() ;
    		String fiberCode3Str = fiberCode3 == null ? "" : fiberCode3.getId() != null ? fiberCode3.getId() : fiberCode3.getText() == null ? "" : fiberCode3.getText() ;
    		String fiberCode4Str = fiberCode4 == null ? "" : fiberCode4.getId() != null ? fiberCode4.getId() : fiberCode4.getText() == null ? "" : fiberCode4.getText() ;
    		String fiberCode5Str = fiberCode5 == null ? "" : fiberCode5.getId() != null ? fiberCode5.getId() : fiberCode5.getText() == null ? "" : fiberCode5.getText() ;
    		String fiberCodeDescr1Str = fiberCodeDescr1 == null ? "" : fiberCodeDescr1.getId() != null ? fiberCodeDescr1.getId() : fiberCodeDescr1.getText() == null ? "" : fiberCodeDescr1.getText() ;
    		String fiberCodeDescr2Str = fiberCodeDescr2 == null ? "" : fiberCodeDescr2.getId() != null ? fiberCodeDescr2.getId() : fiberCodeDescr2.getText() == null ? "" : fiberCodeDescr2.getText() ;
    		String fiberCodeDescr3Str = fiberCodeDescr3 == null ? "" : fiberCodeDescr3.getId() != null ? fiberCodeDescr3.getId() : fiberCodeDescr3.getText() == null ? "" : fiberCodeDescr3.getText() ;
    		String fiberCodeDescr4Str = fiberCodeDescr4 == null ? "" : fiberCodeDescr4.getId() != null ? fiberCodeDescr4.getId() : fiberCodeDescr4.getText() == null ? "" : fiberCodeDescr4.getText() ;
    		String fiberCodeDescr5Str = fiberCodeDescr5 == null ? "" : fiberCodeDescr5.getId() != null ? fiberCodeDescr5.getId() : fiberCodeDescr5.getText() == null ? "" : fiberCodeDescr5.getText() ;
    		String fiberPart1Str = fiberPart1 == null ? "" : fiberPart1.getId() != null ? fiberPart1.getId() : fiberPart1.getText() == null ? "" : fiberPart1.getText() ;
    		String fiberPart2Str = fiberPart2 == null ? "" : fiberPart2.getId() != null ? fiberPart2.getId() : fiberPart2.getText() == null ? "" : fiberPart2.getText() ;
    		String fiberPart3Str = fiberPart3 == null ? "" : fiberPart3.getId() != null ? fiberPart3.getId() : fiberPart3.getText() == null ? "" : fiberPart3.getText() ;
    		String fiberPart4Str = fiberPart4 == null ? "" : fiberPart4.getId() != null ? fiberPart4.getId() : fiberPart4.getText() == null ? "" : fiberPart4.getText() ;
    		String fiberPart5Str = fiberPart5 == null ? "" : fiberPart5.getId() != null ? fiberPart5.getId() : fiberPart5.getText() == null ? "" : fiberPart5.getText() ;
    		String fshIdStr = fshId == null ? "" : fshId.getId() != null ? fshId.getId() : fshId.getText() == null ? "" : fshId.getText() ;
    		String fshCollectionStr = fshCollection == null ? "" : fshCollection.getId() != null ? fshCollection.getId() : fshCollection.getText() == null ? "" : fshCollection.getText() ;
    		String fshSeasonStr = fshSeason == null ? "" : fshSeason.getId() != null ? fshSeason.getId() : fshSeason.getText() == null ? "" : fshSeason.getText() ;
    		String fshSeasonsStr = fshSeasons == null ? "" : fshSeasons.getId() != null ? fshSeasons.getId() : fshSeasons.getText() == null ? "" : fshSeasons.getText() ;
    		String fshSeasonYearStr = fshSeasonYear == null ? "" : fshSeasonYear.getId() != null ? fshSeasonYear.getId() : fshSeasonYear.getText() == null ? "" : fshSeasonYear.getText() ;
    		String fshThemeStr = fshTheme == null ? "" : fshTheme.getId() != null ? fshTheme.getId() : fshTheme.getText() == null ? "" : fshTheme.getText() ;
    		String familydescriptionStr = familydescription == null ? "" : familydescription.getId() != null ? familydescription.getId() : familydescription.getText() == null ? "" : familydescription.getText() ;
    		String featurebullet1Str = featurebullet1 == null ? "" : featurebullet1.getId() != null ? featurebullet1.getId() : featurebullet1.getText() == null ? "" : featurebullet1.getText() ;
    		String featurebullet2Str = featurebullet2 == null ? "" : featurebullet2.getId() != null ? featurebullet2.getId() : featurebullet2.getText() == null ? "" : featurebullet2.getText() ;
    		String fechainiciovigenciacostoimportacionStr = fechainiciovigenciacostoimportacion == null ? "" : fechainiciovigenciacostoimportacion.getId() != null ? fechainiciovigenciacostoimportacion.getId() : fechainiciovigenciacostoimportacion.getText() == null ? "" : fechainiciovigenciacostoimportacion.getText() ;
    		String fechainiciovigenciacostonetoStr = fechainiciovigenciacostoneto == null ? "" : fechainiciovigenciacostoneto.getId() != null ? fechainiciovigenciacostoneto.getId() : fechainiciovigenciacostoneto.getText() == null ? "" : fechainiciovigenciacostoneto.getText() ;
    		String fechainiciovigenciaprecioventaStr = fechainiciovigenciaprecioventa == null ? "" : fechainiciovigenciaprecioventa.getId() != null ? fechainiciovigenciaprecioventa.getId() : fechainiciovigenciaprecioventa.getText() == null ? "" : fechainiciovigenciaprecioventa.getText() ;
    		String fechavencimientoStr = fechavencimiento == null ? "" : fechavencimiento.getId() != null ? fechavencimiento.getId() : fechavencimiento.getText() == null ? "" : fechavencimiento.getText() ;
    		String firstdateapproveStr = firstDateApprove == null ? "" : firstDateApprove.getId() != null ? firstDateApprove.getId() : firstDateApprove.getText() == null ? "" : firstDateApprove.getText() ;
    		String flagimportbyssStr = flagimportbyss == null ? "" : flagimportbyss.getId() != null ? flagimportbyss.getId() : flagimportbyss.getText() == null ? "" : flagimportbyss.getText() ;
    		String footnoteStr = footnote == null ? "" : footnote.getId() != null ? footnote.getId() : footnote.getText() == null ? "" : footnote.getText() ;
    		String fototomadaliverpoolStr = fototomadaliverpool == null ? "" : fototomadaliverpool.getId() != null ? fototomadaliverpool.getId() : fototomadaliverpool.getText() == null ? "" : fototomadaliverpool.getText() ;
    		String groesStr = groes == null ? "" : groes.getId() != null ? groes.getId() : groes.getText() == null ? "" : groes.getText() ;
    		String giftwithpurchaseStr = giftwithpurchase == null ? "" : giftwithpurchase.getId() != null ? giftwithpurchase.getId() : giftwithpurchase.getText() == null ? "" : giftwithpurchase.getText() ;
    		String giftwithpurchaseenddateStr = giftwithpurchaseenddate == null ? "" : giftwithpurchaseenddate.getId() != null ? giftwithpurchaseenddate.getId() : giftwithpurchaseenddate.getText() == null ? "" : giftwithpurchaseenddate.getText() ;
    		String giftwithpurchasestartdateStr = giftwithpurchasestartdate == null ? "" : giftwithpurchasestartdate.getId() != null ? giftwithpurchasestartdate.getId() : giftwithpurchasestartdate.getText() == null ? "" : giftwithpurchasestartdate.getText() ;
    		String gradodemodaStr = gradodemoda == null ? "" : gradodemoda.getId() != null ? gradodemoda.getId() : gradodemoda.getText() == null ? "" : gradodemoda.getText() ;
    		String idlastparentStr = idlastparent == null ? "" : idlastparent.getId() != null ? idlastparent.getId() : idlastparent.getText() == null ? "" : idlastparent.getText() ;
    		String idtallaerpStr = idtallaerp == null ? "" : idtallaerp.getId() != null ? idtallaerp.getId() : idtallaerp.getText() == null ? "" : idtallaerp.getText() ;
    		String iepsStr = ieps == null ? "" : ieps.getId() != null ? ieps.getId() : ieps.getText() == null ? "" : ieps.getText() ;
    		String identificanegocioStr = identificanegocio == null ? "" : identificanegocio.getId() != null ? identificanegocio.getId() : identificanegocio.getText() == null ? "" : identificanegocio.getText() ;
    		String impuestoalaventaStr = impuestoalaventa == null ? "" : impuestoalaventa.getId() != null ? impuestoalaventa.getId() : impuestoalaventa.getText() == null ? "" : impuestoalaventa.getText() ;
    		String incidenciascategorizacionStr = incidenciascategorizacion == null ? "" : incidenciascategorizacion.getId() != null ? incidenciascategorizacion.getId() : incidenciascategorizacion.getText() == null ? "" : incidenciascategorizacion.getText() ;
    		String incidenciascomprasStr = incidenciascompras == null ? "" : incidenciascompras.getId() != null ? incidenciascompras.getId() : incidenciascompras.getText() == null ? "" : incidenciascompras.getText() ;
    		String indicadordeimpuestoStr = indicadordeimpuesto == null ? "" : indicadordeimpuesto.getId() != null ? indicadordeimpuesto.getId() : indicadordeimpuesto.getText() == null ? "" : indicadordeimpuesto.getText() ;
    		String itemgroupStr = itemgroup == null ? "" : itemgroup.getId() != null ? itemgroup.getId() : itemgroup.getText() == null ? "" : itemgroup.getText() ;
    		String itemgroup2Str = itemgroup2 == null ? "" : itemgroup2.getId() != null ? itemgroup2.getId() : itemgroup2.getText() == null ? "" : itemgroup2.getText() ;
    		String itemgroups4hStr = itemgroups4h == null ? "" : itemgroups4h.getId() != null ? itemgroups4h.getId() : itemgroups4h.getText() == null ? "" : itemgroups4h.getText() ;
    		String klaseCteStr = klaseCte == null ? "" : klaseCte.getId() != null ? klaseCte.getId() : klaseCte.getText() == null ? "" : klaseCte.getText() ;
    		String laborStr = labor == null ? "" : labor.getId() != null ? labor.getId() : labor.getText() == null ? "" : labor.getText() ;
    		String laborS4hStr = laborS4h == null ? "" : laborS4h.getId() != null ? laborS4h.getId() : laborS4h.getText() == null ? "" : laborS4h.getText() ;
    		String latestpartsStr = latestparts == null ? "" : latestparts.getId() != null ? latestparts.getId() : latestparts.getText() == null ? "" : latestparts.getText() ;
    		String latestpartsenddateStr = latestpartsenddate == null ? "" : latestpartsenddate.getId() != null ? latestpartsenddate.getId() : latestpartsenddate.getText() == null ? "" : latestpartsenddate.getText() ;
    		String latestpartsstartdateStr = latestpartsstartdate == null ? "" : latestpartsstartdate.getId() != null ? latestpartsstartdate.getId() : latestpartsstartdate.getText() == null ? "" : latestpartsstartdate.getText() ;
    		String licensedescriptionStr = licensedescription == null ? "" : licensedescription.getId() != null ? licensedescription.getId() : licensedescription.getText() == null ? "" : licensedescription.getText() ;
    		String maxStackStr = maxStack == null ? "" : maxStack.getId() != null ? maxStack.getId() : maxStack.getText() == null ? "" : maxStack.getText() ;
    		String mtartS4hStr = mtartS4h == null ? "" : mtartS4h.getId() != null ? mtartS4h.getId() : mtartS4h.getText() == null ? "" : mtartS4h.getText() ;
    		String mvgr2Str = mvgr2 == null ? "" : mvgr2.getId() != null ? mvgr2.getId() : mvgr2.getText() == null ? "" : mvgr2.getText() ;
    		String mvgr5Str = mvgr5 == null ? "" : mvgr5.getId() != null ? mvgr5.getId() : mvgr5.getText() == null ? "" : mvgr5.getText() ;
    		String mainbarcodeStr = mainbarcode == null ? "" : mainbarcode.getId() != null ? mainbarcode.getId() : mainbarcode.getText() == null ? "" : mainbarcode.getText() ;
    		String mainbarcodes4hStr = mainbarcodes4h == null ? "" : mainbarcodes4h.getId() != null ? mainbarcodes4h.getId() : mainbarcodes4h.getText() == null ? "" : mainbarcodes4h.getText() ;
    		String margenStr = margen == null ? "" : margen.getId() != null ? margen.getId() : margen.getText() == null ? "" : margen.getText() ;
    		String margenS4hStr = margenS4h == null ? "" : margenS4h.getId() != null ? margenS4h.getId() : margenS4h.getText() == null ? "" : margenS4h.getText() ;
    		try {
    			Integer.parseInt(margenStr);
    		}catch(NumberFormatException e) {
    			margenStr = "";
    		}
    		try {
    			Integer.parseInt(margenS4hStr);
    		}catch(NumberFormatException e) {
    			margenS4hStr = "";
    		}
    		String mensajecreacionskuStr = mensajecreacionsku == null ? "" : mensajecreacionsku.getId() != null ? mensajecreacionsku.getId() : mensajecreacionsku.getText() == null ? "" : mensajecreacionsku.getText() ;
    		String mesdeentregademercanciaStr = mesdeentregademercancia == null ? "" : mesdeentregademercancia.getId() != null ? mesdeentregademercancia.getId() : mesdeentregademercancia.getText() == null ? "" : mesdeentregademercancia.getText() ;
    		String miraklsalesitemfamilyidStr = miraklsalesitemfamilyid == null ? "" : miraklsalesitemfamilyid.getId() != null ? miraklsalesitemfamilyid.getId() : miraklsalesitemfamilyid.getText() == null ? "" : miraklsalesitemfamilyid.getText() ;
    		String normtStr = normt == null ? "" : normt.getId() != null ? normt.getId() : normt.getText() == null ? "" : normt.getText() ;
    		String numtpS4hStr = numtpS4h == null ? "" : numtpS4h.getId() != null ? numtpS4h.getId() : numtpS4h.getText() == null ? "" : numtpS4h.getText() ;
    		String nameStr = name == null ? "" : name.getId() != null ? name.getId() : name.getText() == null ? "" : name.getText() ;
    		String nameexceptionsStr = nameexceptions == null ? "" : nameexceptions.getId() != null ? nameexceptions.getId() : nameexceptions.getText() == null ? "" : nameexceptions.getText() ;
    		String nameguideStr = nameguide == null ? "" : nameguide.getId() != null ? nameguide.getId() : nameguide.getText() == null ? "" : nameguide.getText() ;
    		String negocioStr = negocio == null ? "" : negocio.getId() != null ? negocio.getId() : negocio.getText() == null ? "" : negocio.getText() ;
    		String numberofdetailimagesStr = numberofdetailimages == null ? "" : numberofdetailimages.getId() != null ? numberofdetailimages.getId() : numberofdetailimages.getText() == null ? "" : numberofdetailimages.getText() ;
    		String numberofillustrationimagesStr = numberofillustrationimages == null ? "" : numberofillustrationimages.getId() != null ? numberofillustrationimages.getId() : numberofillustrationimages.getText() == null ? "" : numberofillustrationimages.getText() ;
    		String numberofliverpoolmanualsStr = numberofliverpoolmanuals == null ? "" : numberofliverpoolmanuals.getId() != null ? numberofliverpoolmanuals.getId() : numberofliverpoolmanuals.getText() == null ? "" : numberofliverpoolmanuals.getText() ;
    		String numberofnomsStr = numberofnoms == null ? "" : numberofnoms.getId() != null ? numberofnoms.getId() : numberofnoms.getText() == null ? "" : numberofnoms.getText() ;
    		String numberofsmoshimagesStr = numberofsmoshimages == null ? "" : numberofsmoshimages.getId() != null ? numberofsmoshimages.getId() : numberofsmoshimages.getText() == null ? "" : numberofsmoshimages.getText() ;
    		String origensapStepStr = origensapStep == null ? "" : origensapStep.getId() != null ? origensapStep.getId() : origensapStep.getText() == null ? "" : origensapStep.getText() ;
    		String plgtpStr = plgtp == null ? "" : plgtp.getId() != null ? plgtp.getId() : plgtp.getText() == null ? "" : plgtp.getText() ;
    		String parentStr = parent == null ? "" : parent.getId() != null ? parent.getId() : parent.getText() == null ? "" : parent.getText() ;
    		String parentidStr = parentid == null ? "" : parentid.getId() != null ? parentid.getId() : parentid.getText() == null ? "" : parentid.getText() ;
    		String perfilderedondeoStr = perfilderedondeo == null ? "" : perfilderedondeo.getId() != null ? perfilderedondeo.getId() : perfilderedondeo.getText() == null ? "" : perfilderedondeo.getText() ;
    		String presaleStr = presale == null ? "" : presale.getId() != null ? presale.getId() : presale.getText() == null ? "" : presale.getText() ;
    		String presaledatestartStr = presaledatestart == null ? "" : presaledatestart.getId() != null ? presaledatestart.getId() : presaledatestart.getText() == null ? "" : presaledatestart.getText() ;
    		String preciosugeridocivaStr = preciosugeridociva == null ? "" : preciosugeridociva.getId() != null ? preciosugeridociva.getId() : preciosugeridociva.getText() == null ? "" : preciosugeridociva.getText() ;
    		String presaledateendStr = presaledateend == null ? "" : presaledateend.getId() != null ? presaledateend.getId() : presaledateend.getText() == null ? "" : presaledateend.getText() ;
    		String proceedbuyertoqaStr = proceedbuyertoqa == null ? "" : proceedbuyertoqa.getId() != null ? proceedbuyertoqa.getId() : proceedbuyertoqa.getText() == null ? "" : proceedbuyertoqa.getText() ;
    		String productnameStr = productname == null ? "" : productname.getId() != null ? productname.getId() : productname.getText() == null ? "" : productname.getText() ;
    		String producttypeStr = producttype == null ? "" : producttype.getId() != null ? producttype.getId() : producttype.getText() == null ? "" : producttype.getText() ;
    		String producttypesapStr = producttypesap == null ? "" : producttypesap.getId() != null ? producttypesap.getId() : producttypesap.getText() == null ? "" : producttypesap.getText() ;
    		String producttypesap2Str = producttypesap2 == null ? "" : producttypesap2.getId() != null ? producttypesap2.getId() : producttypesap2.getText() == null ? "" : producttypesap2.getText() ;
    		String producttypesaptempStr = producttypesaptemp == null ? "" : producttypesaptemp.getId() != null ? producttypesaptemp.getId() : producttypesaptemp.getText() == null ? "" : producttypesaptemp.getText() ;
    		String producttypesaptempsbbStr = producttypesaptempsbb == null ? "" : producttypesaptempsbb.getId() != null ? producttypesaptempsbb.getId() : producttypesaptempsbb.getText() == null ? "" : producttypesaptempsbb.getText() ;
    		String productoStr = producto == null ? "" : producto.getId() != null ? producto.getId() : producto.getText() == null ? "" : producto.getText() ;
    		String publicarenatgStr = publicarenatg == null ? "" : publicarenatg.getId() != null ? publicarenatg.getId() : publicarenatg.getText() == null ? "" : publicarenatg.getText() ;
    		String qarejectionmessageStr = qarejectionmessage == null ? "" : qarejectionmessage.getId() != null ? qarejectionmessage.getId() : qarejectionmessage.getText() == null ? "" : qarejectionmessage.getText() ;
    		String regiontempStr = regiontemp == null ? "" : regiontemp.getId() != null ? regiontemp.getId() : regiontemp.getText() == null ? "" : regiontemp.getText() ;
    		String sapfeederrorsStr = sapfeederrors == null ? "" : sapfeederrors.getId() != null ? sapfeederrors.getId() : sapfeederrors.getText() == null ? "" : sapfeederrors.getText() ;
    		String sapobjecttypeStr = sapobjecttype == null ? "" : sapobjecttype.getId() != null ? sapobjecttype.getId() : sapobjecttype.getText() == null ? "" : sapobjecttype.getText() ;
    		String sapspartStr = sapspart == null ? "" : sapspart.getId() != null ? sapspart.getId() : sapspart.getText() == null ? "" : sapspart.getText() ;
    		String sapBehvoStr = sapBehvo == null ? "" : sapBehvo.getId() != null ? sapBehvo.getId() : sapBehvo.getText() == null ? "" : sapBehvo.getText() ;
    		String sapZzcomaStr = sapZzcoma == null ? "" : sapZzcoma.getId() != null ? sapZzcoma.getId() : sapZzcoma.getText() == null ? "" : sapZzcoma.getText() ;
    		String sb0002Str = sb0002 == null ? "" : sb0002.getId() != null ? sb0002.getId() : sb0002.getText() == null ? "" : sb0002.getText() ;
    		String sbColoresStr = sbColores == null ? "" : sbColores.getId() != null ? sbColores.getId() : sbColores.getText() == null ? "" : sbColores.getText() ;
    		String sbTHardlineStr = sbTHardline == null ? "" : sbTHardline.getId() != null ? sbTHardline.getId() : sbTHardline.getText() == null ? "" : sbTHardline.getText() ;
    		String servvStr = servv == null ? "" : servv.getId() != null ? servv.getId() : servv.getText() == null ? "" : servv.getText() ;
    		String skuStr = sku == null ? "" : sku.getId() != null ? sku.getId() : sku.getText() == null ? "" : sku.getText() ;
    		String skucreationdateStr = skucreationdate == null ? "" : skucreationdate.getId() != null ? skucreationdate.getId() : skucreationdate.getText() == null ? "" : skucreationdate.getText() ;
    		String salesitemshortdescriptionStr = salesitemshortdescription == null ? "" : salesitemshortdescription.getId() != null ? salesitemshortdescription.getId() : salesitemshortdescription.getText() == null ? "" : salesitemshortdescription.getText() ;
    		String sectionStr = section == null ? "" : section.getId() != null ? section.getId() : section.getText() == null ? "" : section.getText() ;
    		String shippleasantStr = shippleasant == null ? "" : shippleasant.getId() != null ? shippleasant.getId() : shippleasant.getText() == null ? "" : shippleasant.getText() ;
    		String shippleasantstartdateStr = shippleasantstartdate == null ? "" : shippleasantstartdate.getId() != null ? shippleasantstartdate.getId() : shippleasantstartdate.getText() == null ? "" : shippleasantstartdate.getText() ;
    		String sistemaorigenStr = sistemaorigen == null ? "" : sistemaorigen.getId() != null ? sistemaorigen.getId() : sistemaorigen.getText() == null ? "" : sistemaorigen.getText() ;
    		String skutypeStr = skutype == null ? "" : skutype.getId() != null ? skutype.getId() : skutype.getText() == null ? "" : skutype.getText() ;
    		String sortpriceStr = sortprice == null ? "" : sortprice.getId() != null ? sortprice.getId() : sortprice.getText() == null ? "" : sortprice.getText() ;
    		String stateskuStr = statesku == null ? "" : statesku.getId() != null ? statesku.getId() : statesku.getText() == null ? "" : statesku.getText() ;
    		String statusoutwfStr = statusoutwf == null ? "" : statusoutwf.getId() != null ? statusoutwf.getId() : statusoutwf.getText() == null ? "" : statusoutwf.getText() ;
    		String suppliercompleteStr = suppliercomplete == null ? "" : suppliercomplete.getId() != null ? suppliercomplete.getId() : suppliercomplete.getText() == null ? "" : suppliercomplete.getText() ;
    		String supplieridStr = supplierid == null ? "" : supplierid.getId() != null ? supplierid.getId() : supplierid.getText() == null ? "" : supplierid.getText() ;
    		String suppliernameStr = suppliername == null ? "" : suppliername.getId() != null ? suppliername.getId() : suppliername.getText() == null ? "" : suppliername.getText() ;
    		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
    		String supplierrejectionmessageStr = supplierrejectionmessage == null ? "" : supplierrejectionmessage.getId() != null ? supplierrejectionmessage.getId() : supplierrejectionmessage.getText() == null ? "" : supplierrejectionmessage.getText() ;
    		String taxess4hStr = taxess4h == null ? "" : taxess4h.getId() != null ? taxess4h.getId() : taxess4h.getText() == null ? "" : taxess4h.getText() ;
    		String taxessapStr = taxessap == null ? "" : taxessap.getId() != null ? taxessap.getId() : taxessap.getText() == null ? "" : taxessap.getText() ;
    		String taxkm1S4hStr = taxkm1S4h == null ? "" : taxkm1S4h.getId() != null ? taxkm1S4h.getId() : taxkm1S4h.getText() == null ? "" : taxkm1S4h.getText() ;
    		String taxkm2S4hStr = taxkm2S4h == null ? "" : taxkm2S4h.getId() != null ? taxkm2S4h.getId() : taxkm2S4h.getText() == null ? "" : taxkm2S4h.getText() ;
    		String taxm3S4hStr = taxm3S4h == null ? "" : taxm3S4h.getId() != null ? taxm3S4h.getId() : taxm3S4h.getText() == null ? "" : taxm3S4h.getText() ;
    		String tipooperacionStr = tipooperacion == null ? "" : tipooperacion.getId() != null ? tipooperacion.getId() : tipooperacion.getText() == null ? "" : tipooperacion.getText() ;
    		String timportacionStr = timportacion == null ? "" : timportacion.getId() != null ? timportacion.getId() : timportacion.getText() == null ? "" : timportacion.getText() ;
    		String temporadaStr = temporada == null ? "" : temporada.getId() != null ? temporada.getId() : temporada.getText() == null ? "" : temporada.getText() ;
    		String terminosycondicionescrStr = terminosycondicionescr == null ? "" : terminosycondicionescr.getId() != null ? terminosycondicionescr.getId() : terminosycondicionescr.getText() == null ? "" : terminosycondicionescr.getText() ;
    		String textoadicionalStr = textoadicional == null ? "" : textoadicional.getId() != null ? textoadicional.getId() : textoadicional.getText() == null ? "" : textoadicional.getText() ;
    		String tipodeetiquetaStr = tipodeetiqueta == null ? "" : tipodeetiqueta.getId() != null ? tipodeetiqueta.getId() : tipodeetiqueta.getText() == null ? "" : tipodeetiqueta.getText() ;
    		String tipodetomaforoStr = tipodetomaforo == null ? "" : tipodetomaforo.getId() != null ? tipodetomaforo.getId() : tipodetomaforo.getText() == null ? "" : tipodetomaforo.getText() ;
    		String typemainbarcodeStr = typemainbarcode == null ? "" : typemainbarcode.getId() != null ? typemainbarcode.getId() : typemainbarcode.getText() == null ? "" : typemainbarcode.getText() ;
    		String volumattcalculadoStr = volumattcalculado == null ? "" : volumattcalculado.getId() != null ? volumattcalculado.getId() : volumattcalculado.getText() == null ? "" : volumattcalculado.getText() ;
    		String videoStr = video == null ? "" : video.getId() != null ? video.getId() : video.getText() == null ? "" : video.getText() ;
    		String viewincidenciascategorizacionStr = viewincidenciascategorizacion == null ? "" : viewincidenciascategorizacion.getId() != null ? viewincidenciascategorizacion.getId() : viewincidenciascategorizacion.getText() == null ? "" : viewincidenciascategorizacion.getText() ;
    		String volumetrycheckStr = volumetrycheck == null ? "" : volumetrycheck.getId() != null ? volumetrycheck.getId() : volumetrycheck.getText() == null ? "" : volumetrycheck.getText() ;
    		String weschStr = wesch == null ? "" : wesch.getId() != null ? wesch.getId() : wesch.getText() == null ? "" : wesch.getText() ;
    		String wfDateCreatedRejectedStr = wfDateCreatedRejected == null ? "" : wfDateCreatedRejected.getId() != null ? wfDateCreatedRejected.getId() : wfDateCreatedRejected.getText() == null ? "" : wfDateCreatedRejected.getText() ;
    		String wherlStr = wherl == null ? "" : wherl.getId() != null ? wherl.getId() : wherl.getText() == null ? "" : wherl.getText() ;
    		String webcategorizerejectionmessageStr = webcategorizerejectionmessage == null ? "" : webcategorizerejectionmessage.getId() != null ? webcategorizerejectionmessage.getId() : webcategorizerejectionmessage.getText() == null ? "" : webcategorizerejectionmessage.getText() ;
    		String zgewcjStr = zgewcj == null ? "" : zgewcj.getId() != null ? zgewcj.getId() : zgewcj.getText() == null ? "" : zgewcj.getText() ;
    		String znumvStr = znumv == null ? "" : znumv.getId() != null ? znumv.getId() : znumv.getText() == null ? "" : znumv.getText() ;
    		String zomsuddatestartStr = zomsuddatestart == null ? "" : zomsuddatestart.getId() != null ? zomsuddatestart.getId() : zomsuddatestart.getText() == null ? "" : zomsuddatestart.getText() ;
    		String znumvmkpStr = znumvmkp == null ? "" : znumvmkp.getId() != null ? znumvmkp.getId() : znumvmkp.getText() == null ? "" : znumvmkp.getText() ;
    		String zomsuddateendStr = zomsuddateend == null ? "" : zomsuddateend.getId() != null ? zomsuddateend.getId() : zomsuddateend.getText() == null ? "" : zomsuddateend.getText() ;
    		String zomsudStr = zomsud == null ? "" : zomsud.getId() != null ? zomsud.getId() : zomsud.getText() == null ? "" : zomsud.getText() ;
    		String zzfeemStr = zzfeem == null ? "" : zzfeem.getId() != null ? zzfeem.getId() : zzfeem.getText() == null ? "" : zzfeem.getText() ;
    		String zvolcjcalculadoStr = zvolcjcalculado == null ? "" : zvolcjcalculado.getId() != null ? zvolcjcalculado.getId() : zvolcjcalculado.getText() == null ? "" : zvolcjcalculado.getText() ;
    		String zzlicS4hStr = zzlicS4h == null ? "" : zzlicS4h.getId() != null ? zzlicS4h.getId() : zzlicS4h.getText() == null ? "" : zzlicS4h.getText() ;
    		String deliverymethodStr = deliverymethod == null ? "" : deliverymethod.getId() != null ? deliverymethod.getId() : deliverymethod.getText() == null ? "" : deliverymethod.getText() ;
    		String essuburbiaStr = essuburbia == null ? "" : essuburbia.getId() != null ? essuburbia.getId() : essuburbia.getText() == null ? "" : essuburbia.getText() ;
    		String exclusivediscountStr = exclusivediscount == null ? "" : exclusivediscount.getId() != null ? exclusivediscount.getId() : exclusivediscount.getText() == null ? "" : exclusivediscount.getText() ;
    		String exclusivediscountstartdateStr = exclusivediscountstartdate == null ? "" : exclusivediscountstartdate.getId() != null ? exclusivediscountstartdate.getId() : exclusivediscountstartdate.getText() == null ? "" : exclusivediscountstartdate.getText() ;
    		String exclusivediscountenddateStr = exclusivediscountenddate == null ? "" : exclusivediscountenddate.getId() != null ? exclusivediscountenddate.getId() : exclusivediscountenddate.getText() == null ? "" : exclusivediscountenddate.getText() ;
    		String isduttyfreeStr = isduttyfree == null ? "" : isduttyfree.getId() != null ? isduttyfree.getId() : isduttyfree.getText() == null ? "" : isduttyfree.getText() ;
    		String ismarketplaceStr = ismarketplace == null ? "" : ismarketplace.getId() != null ? ismarketplace.getId() : ismarketplace.getText() == null ? "" : ismarketplace.getText() ;
    		String miraklImage1Str = miraklImage1 == null ? "" : miraklImage1.getId() != null ? miraklImage1.getId() : miraklImage1.getText() == null ? "" : miraklImage1.getText() ;
    		String miraklImage10Str = miraklImage10 == null ? "" : miraklImage10.getId() != null ? miraklImage10.getId() : miraklImage10.getText() == null ? "" : miraklImage10.getText() ;
    		String miraklImage11Str = miraklImage11 == null ? "" : miraklImage11.getId() != null ? miraklImage11.getId() : miraklImage11.getText() == null ? "" : miraklImage11.getText() ;
    		String miraklImage12Str = miraklImage12 == null ? "" : miraklImage12.getId() != null ? miraklImage12.getId() : miraklImage12.getText() == null ? "" : miraklImage12.getText() ;
    		String miraklImage13Str = miraklImage13 == null ? "" : miraklImage13.getId() != null ? miraklImage13.getId() : miraklImage13.getText() == null ? "" : miraklImage13.getText() ;
    		String miraklImage14Str = miraklImage14 == null ? "" : miraklImage14.getId() != null ? miraklImage14.getId() : miraklImage14.getText() == null ? "" : miraklImage14.getText() ;
    		String miraklImage15Str = miraklImage15 == null ? "" : miraklImage15.getId() != null ? miraklImage15.getId() : miraklImage15.getText() == null ? "" : miraklImage15.getText() ;
    		String miraklImage2Str = miraklImage2 == null ? "" : miraklImage2.getId() != null ? miraklImage2.getId() : miraklImage2.getText() == null ? "" : miraklImage2.getText() ;
    		String miraklImage3Str = miraklImage3 == null ? "" : miraklImage3.getId() != null ? miraklImage3.getId() : miraklImage3.getText() == null ? "" : miraklImage3.getText() ;
    		String miraklImage4Str = miraklImage4 == null ? "" : miraklImage4.getId() != null ? miraklImage4.getId() : miraklImage4.getText() == null ? "" : miraklImage4.getText() ;
    		String miraklImage5Str = miraklImage5 == null ? "" : miraklImage5.getId() != null ? miraklImage5.getId() : miraklImage5.getText() == null ? "" : miraklImage5.getText() ;
    		String miraklImage6Str = miraklImage6 == null ? "" : miraklImage6.getId() != null ? miraklImage6.getId() : miraklImage6.getText() == null ? "" : miraklImage6.getText() ;
    		String miraklImage7Str = miraklImage7 == null ? "" : miraklImage7.getId() != null ? miraklImage7.getId() : miraklImage7.getText() == null ? "" : miraklImage7.getText() ;
    		String miraklImage8Str = miraklImage8 == null ? "" : miraklImage8.getId() != null ? miraklImage8.getId() : miraklImage8.getText() == null ? "" : miraklImage8.getText() ;
    		String miraklImage9Str = miraklImage9 == null ? "" : miraklImage9.getId() != null ? miraklImage9.getId() : miraklImage9.getText() == null ? "" : miraklImage9.getText() ;
    		String miraklAcceptanceStatusStr = miraklAcceptanceStatus == null ? "" : miraklAcceptanceStatus.getId() != null ? miraklAcceptanceStatus.getId() : miraklAcceptanceStatus.getText() == null ? "" : miraklAcceptanceStatus.getText() ;
    		String miraklIntegrationCodeStr = miraklIntegrationCode == null ? "" : miraklIntegrationCode.getId() != null ? miraklIntegrationCode.getId() : miraklIntegrationCode.getText() == null ? "" : miraklIntegrationCode.getText() ;
    		String miraklIntegrationMessageStr = miraklIntegrationMessage == null ? "" : miraklIntegrationMessage.getId() != null ? miraklIntegrationMessage.getId() : miraklIntegrationMessage.getText() == null ? "" : miraklIntegrationMessage.getText() ;
    		String miraklProductIdStr = miraklProductId == null ? "" : miraklProductId.getId() != null ? miraklProductId.getId() : miraklProductId.getText() == null ? "" : miraklProductId.getText() ;
    		String miraklRejectionMessageStr = miraklRejectionMessage == null ? "" : miraklRejectionMessage.getId() != null ? miraklRejectionMessage.getId() : miraklRejectionMessage.getText() == null ? "" : miraklRejectionMessage.getText() ;
    		String miraklRejectionReasonStr = miraklRejectionReason == null ? "" : miraklRejectionReason.getId() != null ? miraklRejectionReason.getId() : miraklRejectionReason.getText() == null ? "" : miraklRejectionReason.getText() ;
    		String miraklValidationStatusStr = miraklValidationStatus == null ? "" : miraklValidationStatus.getId() != null ? miraklValidationStatus.getId() : miraklValidationStatus.getText() == null ? "" : miraklValidationStatus.getText() ;
    		String miraklVariantGroupIdStr = miraklVariantGroupId == null ? "" : miraklVariantGroupId.getId() != null ? miraklVariantGroupId.getId() : miraklVariantGroupId.getText() == null ? "" : miraklVariantGroupId.getText() ;
    		String refundpolicyStr = refundpolicy == null ? "" : refundpolicy.getId() != null ? refundpolicy.getId() : refundpolicy.getText() == null ? "" : refundpolicy.getText() ;
    		String specificationStr = specification == null ? "" : specification.getId() != null ? specification.getId() : specification.getText() == null ? "" : specification.getText() ;
    		String suppliershopidStr = suppliershopid == null ? "" : suppliershopid.getId() != null ? suppliershopid.getId() : suppliershopid.getText() == null ? "" : suppliershopid.getText() ;
    		String dutyfreekeyStr = dutyfreekey == null ? "" : dutyfreekey.getId() != null ? dutyfreekey.getId() : dutyfreekey.getText() == null ? "" : dutyfreekey.getText() ;
    		business = determineBusiness(negocio == null || negocio.getText() == null ? "" : negocio.getText(), extwgS4h == null || extwgS4h.getText() == null ? "" : extwgS4h.getText());
    		nameStr = product.getName();
    		
    		bundle = elese.computeStatus(calculatedWFAtt, !"".equals(firstdateapproveStr) ? "Aprobado" : "", fotoTomadaLiverpool, product.getId());
    		currentStatus = bundle[0];
    		prevStatus = bundle[1];
    		enriquecidoEnForo = bundle[2];
    		externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
    		
    		products.put(new org.json.JSONObject().put("externalStatus", externalStatusLabels.get( externalStatus )).put("proposalId", product.getId()).put("entityType", "Generic"));
    		if(products.length() == 1000) {
    			org.json.JSONObject req = new org.json.JSONObject();
    			req.put("products", products);
    			log( pub.publishMessage( 
						 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
						 PropertiesManager.get( "p360.contingency.gcp.idmc_put_products" ), 
						 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
						 req.toString()
						) );
    			while(products.length() > 0) {	
    				products.remove(0);
    			}
    		}
    		
    		vals.put( currentStatus == null || "".equals(currentStatus) ? "" : Integer.parseInt(currentStatus) );
    		vals.put( prevStatus == null || "".equals(prevStatus) ? "" : Integer.parseInt(prevStatus) );
    		vals.put( externalStatus );
    		vals.put( new org.json.JSONArray().put( product.getParentId() ));
    		vals.put(enriquecidoEnForo);
    		vals.put(arStr);
    		vals.put(arurlStr);
    		vals.put(anoestacionStr);
    		vals.put(approveddatecalcStr);
    		vals.put(argumentodeventaStr);
    		vals.put(assetrejectionmessageStr);
    		vals.put(brandIdS4hStr);
    		vals.put(bwsclStr);
    		vals.put(bwvorStr);
    		vals.put(bluecategorizerejectionmessageStr);
    		vals.put(brandidatgStr);
    		vals.put(brandnameStr);
    		vals.put(brandnameatgStr);
    		vals.put(brandownerStr);
    		vals.put(business);
    		vals.put(buyerrejectionmessageStr);
    		vals.put(cstfld03Str);
    		vals.put(cubiscanStr);
    		vals.put(calculatedwfAttStr);
    		vals.put(calculatedInstatewfStr);
    		vals.put(categorymanagerrejectionmessageStr);
    		vals.put(certificadosostenibleStr);
    		vals.put(coleccionStr);
    		vals.put(collectiondescriptionsapStr);
    		vals.put(comentarioproyectoscomercialesStr);
    		vals.put(consignacionStr);
    		vals.put(coordinadoidsapStr);
    		vals.put(costoenmonedaextranjeraStr);
    		vals.put(costonetosinivaStr);
    		vals.put(costobrutosinivaStr);
    		vals.put(countryoforiginStr);
    		vals.put(currencyStr);
    		vals.put(deletecheckStr);
    		vals.put(descriptionlongStr);
    		vals.put(descriptionlong2Str);
    		vals.put(descriptiontableStr);
    		vals.put(descriptionwebStr);
    		vals.put(descuento1Str);
    		vals.put(descuento2Str);
    		vals.put(directionStr);
    		vals.put(envAtgStr);
    		vals.put(extwgS4hStr);
    		vals.put(embedcodewapStr);
    		vals.put(embedcodewebStr);
    		vals.put(enrichmentrejectionmessageStr);
    		vals.put(envioimagenStr);
    		vals.put(enviointernacionalStr);
    		vals.put(enviomiraklStr);
    		vals.put(erroresdesistemaStr);
    		vals.put(essostenibleStr);
    		vals.put(essosteniblevadStr);
    		vals.put(estatuspropuestaStr);
    		vals.put(exclusivepackageStr);
    		vals.put(exclusivepackagestartdateStr);
    		vals.put(eventoStr);
    		vals.put(exclusiveStr);
    		vals.put(exclusiveenddateStr);
    		vals.put(exclusivepromotionenddateStr);
    		vals.put(exclusivestartdateStr);
    		vals.put(exclusivepromotionstartdateStr);
    		vals.put(exclusivepromotionStr);
    		vals.put(exclusivepackageenddateStr);
    		vals.put(expressdeliveryStr);
    		vals.put(expressdeliveryenddateStr);
    		vals.put(expressdeliverystartdateStr);
    		vals.put(fiberCode1Str);
    		vals.put(fiberCode2Str);
    		vals.put(fiberCode3Str);
    		vals.put(fiberCode4Str);
    		vals.put(fiberCode5Str);
    		vals.put(fiberCodeDescr1Str);
    		vals.put(fiberCodeDescr2Str);
    		vals.put(fiberCodeDescr3Str);
    		vals.put(fiberCodeDescr4Str);
    		vals.put(fiberCodeDescr5Str);
    		vals.put(fiberPart1Str);
    		vals.put(fiberPart2Str);
    		vals.put(fiberPart3Str);
    		vals.put(fiberPart4Str);
    		vals.put(fiberPart5Str);
    		vals.put(fshIdStr);
    		vals.put(fshCollectionStr);
    		vals.put(fshSeasonStr);
    		vals.put(fshSeasonsStr);
    		vals.put(fshSeasonYearStr);
    		vals.put(fshThemeStr);
    		vals.put(familydescriptionStr);
    		vals.put(featurebullet1Str);
    		vals.put(featurebullet2Str);
    		vals.put(fechainiciovigenciacostoimportacionStr);
    		vals.put(fechainiciovigenciacostonetoStr);
    		vals.put(fechainiciovigenciaprecioventaStr);
    		vals.put(fechavencimientoStr);
    		vals.put(flagimportbyssStr);
    		vals.put(footnoteStr);
    		vals.put(fototomadaliverpoolStr);
    		vals.put(groesStr);
    		vals.put(giftwithpurchaseStr);
    		vals.put(giftwithpurchaseenddateStr);
    		vals.put(giftwithpurchasestartdateStr);
    		vals.put(gradodemodaStr);
    		vals.put(idlastparentStr);
    		vals.put(idtallaerpStr);
    		vals.put(iepsStr);
    		vals.put(identificanegocioStr);
    		vals.put(impuestoalaventaStr);
    		vals.put(incidenciascategorizacionStr);
    		vals.put(incidenciascomprasStr);
    		vals.put(indicadordeimpuestoStr);
    		vals.put(itemgroupStr);
    		vals.put(itemgroup2Str);
    		vals.put(itemgroups4hStr);
    		vals.put(klaseCteStr);
    		vals.put(laborStr);
    		vals.put(laborS4hStr);
    		vals.put(latestpartsStr);
    		vals.put(latestpartsenddateStr);
    		vals.put(latestpartsstartdateStr);
    		vals.put(licensedescriptionStr);
    		vals.put(maxStackStr);
    		vals.put(mtartS4hStr);
    		vals.put(mvgr2Str);
    		vals.put(mvgr5Str);
    		vals.put("00".equals(sapobjecttypeStr) ? "" : mainbarcodeStr);
    		vals.put("00".equals(sapobjecttypeStr) ? "" : mainbarcodes4hStr);
    		vals.put(margenStr);
    		vals.put(margenS4hStr);
    		vals.put(mensajecreacionskuStr);
    		vals.put(mesdeentregademercanciaStr);
    		vals.put(miraklsalesitemfamilyidStr);
    		vals.put(normtStr);
    		vals.put(numtpS4hStr);
    		vals.put(nameStr);
    		vals.put(nameexceptionsStr);
    		vals.put(nameguideStr);
    		vals.put(negocioStr);
    		vals.put(numberofdetailimagesStr);
    		vals.put(numberofillustrationimagesStr);
    		vals.put(numberofliverpoolmanualsStr);
    		vals.put(numberofnomsStr);
    		vals.put(numberofsmoshimagesStr);
    		vals.put(origensapStepStr);
    		vals.put(plgtpStr);
    		vals.put(parentStr);
    		vals.put(parentidStr);
    		vals.put(perfilderedondeoStr);
    		vals.put(presaleStr);
    		vals.put(presaledatestartStr);
    		vals.put(preciosugeridocivaStr);
    		vals.put(presaledateendStr);
    		vals.put(proceedbuyertoqaStr);
    		vals.put(productnameStr);
    		vals.put(producttypeStr);
    		vals.put(producttypesapStr);
    		vals.put(producttypesap2Str);
    		vals.put(producttypesaptempStr);
    		vals.put(producttypesaptempsbbStr);
    		vals.put(productoStr);
    		vals.put(publicarenatgStr);
    		vals.put(qarejectionmessageStr);
    		vals.put(regiontempStr);
    		vals.put(sapfeederrorsStr);
    		vals.put(sapobjecttypeStr);
    		vals.put(sapspartStr);
    		vals.put(sapBehvoStr);
    		vals.put(sapZzcomaStr);
    		vals.put(sb0002Str);
    		vals.put(sbColoresStr);
    		vals.put(sbTHardlineStr);
    		vals.put(servvStr);
    		vals.put("00".equals( sapobjecttypeStr ) ? "" : skuStr);
    		vals.put(skucreationdateStr);
    		vals.put(salesitemshortdescriptionStr);
    		vals.put(sectionStr);
    		vals.put(shippleasantStr);
    		vals.put(shippleasantstartdateStr);
    		vals.put(sistemaorigenStr);
    		vals.put(skutypeStr);
    		vals.put(sortpriceStr);
    		vals.put(stateskuStr);
    		vals.put(statusoutwfStr);
    		vals.put(suppliercompleteStr);
    		vals.put(supplieridStr);
    		vals.put(suppliernameStr);
    		vals.put(supplierpartnumberStr);
    		vals.put(supplierrejectionmessageStr);
    		vals.put(taxess4hStr);
    		vals.put(taxessapStr);
    		vals.put(taxkm1S4hStr);
    		vals.put(taxkm2S4hStr);
    		vals.put(taxm3S4hStr);
    		vals.put(tipooperacionStr);
    		vals.put(timportacionStr);
    		vals.put(temporadaStr);
    		vals.put(terminosycondicionescrStr);
    		vals.put(textoadicionalStr);
    		vals.put(tipodeetiquetaStr);
    		vals.put(tipodetomaforoStr);
    		vals.put(typemainbarcodeStr);
    		vals.put(volumattcalculadoStr);
    		vals.put(videoStr);
    		vals.put(viewincidenciascategorizacionStr);
    		vals.put(volumetrycheckStr);
    		vals.put(weschStr);
    		vals.put(wfDateCreatedRejectedStr);
    		vals.put(wherlStr);
    		vals.put(webcategorizerejectionmessageStr);
    		vals.put(zgewcjStr);
    		vals.put(znumvStr);
    		vals.put(zomsuddatestartStr);
    		vals.put(znumvmkpStr);
    		vals.put(zomsuddateendStr);
    		vals.put(zomsudStr);
    		vals.put(zzfeemStr);
    		vals.put(zvolcjcalculadoStr);
    		vals.put(zzlicS4hStr);
    		vals.put(deliverymethodStr);
    		vals.put(essuburbiaStr);
    		vals.put(exclusivediscountStr);
    		vals.put(exclusivediscountstartdateStr);
    		vals.put(exclusivediscountenddateStr);
    		vals.put(isduttyfreeStr);
    		vals.put(ismarketplaceStr);
    		vals.put(miraklImage1Str);
    		vals.put(miraklImage10Str);
    		vals.put(miraklImage11Str);
    		vals.put(miraklImage12Str);
    		vals.put(miraklImage13Str);
    		vals.put(miraklImage14Str);
    		vals.put(miraklImage15Str);
    		vals.put(miraklImage2Str);
    		vals.put(miraklImage3Str);
    		vals.put(miraklImage4Str);
    		vals.put(miraklImage5Str);
    		vals.put(miraklImage6Str);
    		vals.put(miraklImage7Str);
    		vals.put(miraklImage8Str);
    		vals.put(miraklImage9Str);
    		vals.put(miraklAcceptanceStatusStr);
    		vals.put(miraklIntegrationCodeStr);
    		vals.put(miraklIntegrationMessageStr);
    		vals.put(miraklProductIdStr);
    		vals.put(miraklRejectionMessageStr);
    		vals.put(miraklRejectionReasonStr);
    		vals.put(miraklValidationStatusStr);
    		vals.put(miraklVariantGroupIdStr);
    		vals.put(refundpolicyStr);
    		vals.put(specificationStr);
    		vals.put(suppliershopidStr);
    		vals.put(dutyfreekeyStr);
    		collectClassifications(product.getClassifications(), product.getId());
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + product.getId() + "'@1")).put("values", vals));
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
        	java.nio.file.Path p = java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.migration.to_skip_directory"), product.getId() );
        	try {
        		java.nio.file.Files.createFile(p);
        	}catch(java.io.IOException ignore) { logE(ignore); }
    	}
    	if(children != null && !children.isEmpty()) {
    		for(Product child : children) {
    			processChild(child, currentStatus, prevStatus, externalStatus);
//    	    	lacuenta++;
//    	    	if(lacuenta % 10000 == 0) {
//    	    		System.out.print(".");
//    	    		if(lacuenta % 1000000 == 0) {
//    	    			System.out.println("" + lacuenta);
//    	    		}
//    	    	}
    		}
    	}else {
    		processChild(product, currentStatus, prevStatus, externalStatus);
    	}
    	lacuenta++;
    	if(lacuenta % 10000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
    		}
    	}
    	/*
    	System.out.println(request);
    	System.out.println(requestArticle);
    	System.out.println(requestStructureGroup);
    	
    	rw.writeData("list", "Product2G", null, qp, request, System.out::println);
    	rw.writeData("list", "Article", null, qp, requestArticle, System.out::println);
    	rw.writeData("list", "Product2G", "Product2GStructureMap", qp, requestStructureGroup, System.out::println);
    	org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
        org.json.JSONArray rows = new org.json.JSONArray();
        org.json.JSONObject request = new org.json.JSONObject();
        request.put("columns", columns);
        request.put("rows", rows);
        System.out.println("Now child parent...");
        for(java.util.Map.Entry<String, String> entry : childParent.entrySet()) {
        	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", entry.getValue())).put("values", new org.json.JSONArray().put( new org.json.JSONArray().put( entry.getValue() ))));
        	if(rows.length() == 50000) {
        		rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
        		while(rows.length() > 0) {
        			rows.remove(0);
        		}
        	}
        }
        System.out.println("obj: " + request);
        if(rows.length() > 0) {
        	rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
    		while(rows.length() > 0) {
    			rows.remove(0);
    		}
        }
    	System.exit(0);
    	*/
    }
    
//	private final java.util.Map<String, String> internalToExternalStatusMap = loadInternalToExternalStatusMap();
//	
//	private static java.util.Map<String, String> loadInternalToExternalStatusMap(){
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
//		qp.put("dictionary", "ExternalStatus");
//		java.util.Map<String, String> data = new java.util.HashMap<>();
//		RESTWrapper rw = new RESTWrapper();
//		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> data.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)));
//		return data;
//	}
    
    private void processChild(Product child, String currentStatus, String prevStatus, String externalStatus) {
    	
    	java.util.List<Value> values = child.getValues();
    	java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
		for(Value value : values) {
			valMap.put(value.getAttributeId(), value);
		}
		org.json.JSONArray vals = new org.json.JSONArray();
		Value brandIdS4h = valMap.get("BRAND_ID_S4H");
		Value brandname = valMap.get("BrandName");
		Value coloursLiverpoolAtt = valMap.get("ColoursLiverpoolAtt");
		Value cubiscan = valMap.get("CUBISCAN");
		Value costoenmonedaextranjera = valMap.get("CostoEnMonedaExtranjera");
		Value costonetosiniva = valMap.get("CostoNetoSinIVA");
		Value costobrutosiniva = valMap.get("CostobrutoSinIVA");
		Value countryoforigin = valMap.get("CountryOfOrigin");
		Value currency = valMap.get("Currency");
		Value descuento1 = valMap.get("Descuento1");
		Value descuento2 = valMap.get("Descuento2");
		Value enviomirakl = valMap.get("EnvioMirakl");
		Value erroresdesistema = valMap.get("ErroresDeSistema");
		Value estatuspropuesta = valMap.get("EstatusPropuesta");
		Value fechainiciovigenciacostoimportacion = valMap.get("FechaInicioVigenciaCostoImportacion");
		Value fechainiciovigenciacostoneto = valMap.get("FechaInicioVigenciaCostoNeto");
		Value fechainiciovigenciaprecioventa = valMap.get("FechaInicioVigenciaPrecioVenta");
		Value footnote = valMap.get("Footnote");
		Value fototomadaliverpool = valMap.get("FotoTomadaLiverpool");
		Value idlastparent = valMap.get("IDLastParent");
		Value idtallaerp = valMap.get("IDTallaERP");
		Value identificanegocio = valMap.get("IdentificaNegocio");
		Value indicadordeimpuesto = valMap.get("IndicadordeImpuesto");
		Value itemgroup = valMap.get("ItemGroup");
		Value itemgroups4h = valMap.get("ItemGroupS4H");
		Value mainbarcode = valMap.get("MainBarCode");
		Value mainbarcodes4h = valMap.get("MainBarCodeS4H");
		Value mensajecreacionsku = valMap.get("MensajeCreacionSKU");
		Value numtpS4h = valMap.get("NUMTP_S4H");
		Value name = valMap.get("Name");
		Value nameguide = valMap.get("NameGuide");
		Value parent = valMap.get("Parent");
		Value parentid = valMap.get("ParentID");
		Value preciosugeridociva = valMap.get("PrecioSugeridocIVA");
		Value producttype = valMap.get("ProductType");
		Value producttypesap = valMap.get("ProductTypeSAP");
		Value producttypesap2 = valMap.get("ProductTypeSAP2");
		Value producttypesaptemp = valMap.get("ProductTypeSAPTEMP");
		Value publicarenatg = valMap.get("PublicarEnATG");
		Value sapobjecttype = valMap.get("SAPObjectType");
		Value sapspart = valMap.get("SAPSpart");
		Value sapBehvo = valMap.get("SAP_BEHVO");
		Value sb0002 = valMap.get("SB_0002");
		Value sbColores = valMap.get("SB_COLORES");
		Value sku = valMap.get("SKU");
		Value skucreationdate = valMap.get("SKUCreationDate");
		Value sistemaorigen = valMap.get("SistemaOrigen");
		Value skutype = valMap.get("SkuType");
		Value statesku = valMap.get("StateSKU");
		Value supplierid = valMap.get("SupplierID");
		Value supplierpartnumber = valMap.get("SupplierPartNumber");
		Value tamanounico = valMap.get("TamanoUnico");
		Value tamanounicot = valMap.get("TamanoUnicoT");
		Value terminosycondicionescr = valMap.get("TerminosYCondicionesCR");
		Value tipodeetiqueta = valMap.get("TipoDeEtiqueta");
		Value typemainbarcode = valMap.get("TypeMainBarCode");
		Value wfDateCreatedRejected = valMap.get("WF_Date_created_rejected");
		Value ismarketplace = valMap.get("isMarketPlace");
		
		String brandIdS4hStr = brandIdS4h == null ? "" : brandIdS4h.getId() != null ? brandIdS4h.getId() : brandIdS4h.getText() == null ? "" : brandIdS4h.getText() ;
		String brandnameStr = brandname == null ? "" : brandname.getId() != null ? brandname.getId() : brandname.getText() == null ? "" : brandname.getText() ;
		String coloursLiverpoolAttStr = coloursLiverpoolAtt == null ? "" : coloursLiverpoolAtt.getId() != null ? coloursLiverpoolAtt.getId() : coloursLiverpoolAtt.getText() == null ? "" : coloursLiverpoolAtt.getText() ;
		String cubiscanStr = cubiscan == null ? "" : cubiscan.getId() != null ? cubiscan.getId() : cubiscan.getText() == null ? "" : cubiscan.getText() ;
		String costoenmonedaextranjeraStr = costoenmonedaextranjera == null ? "" : costoenmonedaextranjera.getId() != null ? costoenmonedaextranjera.getId() : costoenmonedaextranjera.getText() == null ? "" : costoenmonedaextranjera.getText() ;
		String costonetosinivaStr = costonetosiniva == null ? "" : costonetosiniva.getId() != null ? costonetosiniva.getId() : costonetosiniva.getText() == null ? "" : costonetosiniva.getText() ;
		String costobrutosinivaStr = costobrutosiniva == null ? "" : costobrutosiniva.getId() != null ? costobrutosiniva.getId() : costobrutosiniva.getText() == null ? "" : costobrutosiniva.getText() ;
		String countryoforiginStr = countryoforigin == null ? "" : countryoforigin.getId() != null ? countryoforigin.getId() : countryoforigin.getText() == null ? "" : countryoforigin.getText() ;
		String currencyStr = currency == null ? "" : currency.getId() != null ? currency.getId() : currency.getText() == null ? "" : currency.getText() ;
		String descuento1Str = descuento1 == null ? "" : descuento1.getId() != null ? descuento1.getId() : descuento1.getText() == null ? "" : descuento1.getText() ;
		String descuento2Str = descuento2 == null ? "" : descuento2.getId() != null ? descuento2.getId() : descuento2.getText() == null ? "" : descuento2.getText() ;
		String enviomiraklStr = enviomirakl == null ? "" : enviomirakl.getId() != null ? enviomirakl.getId() : enviomirakl.getText() == null ? "" : enviomirakl.getText() ;
		String erroresdesistemaStr = erroresdesistema == null ? "" : erroresdesistema.getId() != null ? erroresdesistema.getId() : erroresdesistema.getText() == null ? "" : erroresdesistema.getText() ;
		String estatuspropuestaStr = estatuspropuesta == null ? "" : estatuspropuesta.getId() != null ? estatuspropuesta.getId() : estatuspropuesta.getText() == null ? "" : estatuspropuesta.getText() ;
		String fechainiciovigenciacostoimportacionStr = fechainiciovigenciacostoimportacion == null ? "" : fechainiciovigenciacostoimportacion.getId() != null ? fechainiciovigenciacostoimportacion.getId() : fechainiciovigenciacostoimportacion.getText() == null ? "" : fechainiciovigenciacostoimportacion.getText() ;
		String fechainiciovigenciacostonetoStr = fechainiciovigenciacostoneto == null ? "" : fechainiciovigenciacostoneto.getId() != null ? fechainiciovigenciacostoneto.getId() : fechainiciovigenciacostoneto.getText() == null ? "" : fechainiciovigenciacostoneto.getText() ;
		String fechainiciovigenciaprecioventaStr = fechainiciovigenciaprecioventa == null ? "" : fechainiciovigenciaprecioventa.getId() != null ? fechainiciovigenciaprecioventa.getId() : fechainiciovigenciaprecioventa.getText() == null ? "" : fechainiciovigenciaprecioventa.getText() ;
		String footnoteStr = footnote == null ? "" : footnote.getId() != null ? footnote.getId() : footnote.getText() == null ? "" : footnote.getText() ;
		String fototomadaliverpoolStr = fototomadaliverpool == null ? "" : fototomadaliverpool.getId() != null ? fototomadaliverpool.getId() : fototomadaliverpool.getText() == null ? "" : fototomadaliverpool.getText() ;
		String idlastparentStr = idlastparent == null ? "" : idlastparent.getId() != null ? idlastparent.getId() : idlastparent.getText() == null ? "" : idlastparent.getText() ;
		String idtallaerpStr = idtallaerp == null ? "" : idtallaerp.getId() != null ? idtallaerp.getId() : idtallaerp.getText() == null ? "" : idtallaerp.getText() ;
		String identificanegocioStr = identificanegocio == null ? "" : identificanegocio.getId() != null ? identificanegocio.getId() : identificanegocio.getText() == null ? "" : identificanegocio.getText() ;
		String indicadordeimpuestoStr = indicadordeimpuesto == null ? "" : indicadordeimpuesto.getId() != null ? indicadordeimpuesto.getId() : indicadordeimpuesto.getText() == null ? "" : indicadordeimpuesto.getText() ;
		String itemgroupStr = itemgroup == null ? "" : itemgroup.getId() != null ? itemgroup.getId() : itemgroup.getText() == null ? "" : itemgroup.getText() ;
		String itemgroups4hStr = itemgroups4h == null ? "" : itemgroups4h.getId() != null ? itemgroups4h.getId() : itemgroups4h.getText() == null ? "" : itemgroups4h.getText() ;
		String mainbarcodeStr = mainbarcode == null ? "" : mainbarcode.getId() != null ? mainbarcode.getId() : mainbarcode.getText() == null ? "" : mainbarcode.getText() ;
		String mainbarcodes4hStr = mainbarcodes4h == null ? "" : mainbarcodes4h.getId() != null ? mainbarcodes4h.getId() : mainbarcodes4h.getText() == null ? "" : mainbarcodes4h.getText() ;
		String mensajecreacionskuStr = mensajecreacionsku == null ? "" : mensajecreacionsku.getId() != null ? mensajecreacionsku.getId() : mensajecreacionsku.getText() == null ? "" : mensajecreacionsku.getText() ;
		String numtpS4hStr = numtpS4h == null ? "" : numtpS4h.getId() != null ? numtpS4h.getId() : numtpS4h.getText() == null ? "" : numtpS4h.getText() ;
		String nameStr = name == null ? "" : name.getId() != null ? name.getId() : name.getText() == null ? "" : name.getText() ;
		String nameguideStr = nameguide == null ? "" : nameguide.getId() != null ? nameguide.getId() : nameguide.getText() == null ? "" : nameguide.getText() ;
		String parentStr = parent == null ? "" : parent.getId() != null ? parent.getId() : parent.getText() == null ? "" : parent.getText() ;
		String parentidStr = parentid == null ? "" : parentid.getId() != null ? parentid.getId() : parentid.getText() == null ? "" : parentid.getText() ;
		String preciosugeridocivaStr = preciosugeridociva == null ? "" : preciosugeridociva.getId() != null ? preciosugeridociva.getId() : preciosugeridociva.getText() == null ? "" : preciosugeridociva.getText() ;
		String producttypeStr = producttype == null ? "" : producttype.getId() != null ? producttype.getId() : producttype.getText() == null ? "" : producttype.getText() ;
		String producttypesapStr = producttypesap == null ? "" : producttypesap.getId() != null ? producttypesap.getId() : producttypesap.getText() == null ? "" : producttypesap.getText() ;
		String producttypesap2Str = producttypesap2 == null ? "" : producttypesap2.getId() != null ? producttypesap2.getId() : producttypesap2.getText() == null ? "" : producttypesap2.getText() ;
		String producttypesaptempStr = producttypesaptemp == null ? "" : producttypesaptemp.getId() != null ? producttypesaptemp.getId() : producttypesaptemp.getText() == null ? "" : producttypesaptemp.getText() ;
		String publicarenatgStr = publicarenatg == null ? "" : publicarenatg.getId() != null ? publicarenatg.getId() : publicarenatg.getText() == null ? "" : publicarenatg.getText() ;
		String sapobjecttypeStr = sapobjecttype == null ? "" : sapobjecttype.getId() != null ? sapobjecttype.getId() : sapobjecttype.getText() == null ? "" : sapobjecttype.getText() ;
		String sapspartStr = sapspart == null ? "" : sapspart.getId() != null ? sapspart.getId() : sapspart.getText() == null ? "" : sapspart.getText() ;
		String sapBehvoStr = sapBehvo == null ? "" : sapBehvo.getId() != null ? sapBehvo.getId() : sapBehvo.getText() == null ? "" : sapBehvo.getText() ;
		String sb0002Str = sb0002 == null ? "" : sb0002.getId() != null ? sb0002.getId() : sb0002.getText() == null ? "" : sb0002.getText() ;
		String sbColoresStr = sbColores == null ? "" : sbColores.getId() != null ? sbColores.getId() : sbColores.getText() == null ? "" : sbColores.getText() ;
		String skuStr = sku == null ? "" : sku.getId() != null ? sku.getId() : sku.getText() == null ? "" : sku.getText() ;
		String skucreationdateStr = skucreationdate == null ? "" : skucreationdate.getId() != null ? skucreationdate.getId() : skucreationdate.getText() == null ? "" : skucreationdate.getText() ;
		String sistemaorigenStr = sistemaorigen == null ? "" : sistemaorigen.getId() != null ? sistemaorigen.getId() : sistemaorigen.getText() == null ? "" : sistemaorigen.getText() ;
		String skutypeStr = skutype == null ? "" : skutype.getId() != null ? skutype.getId() : skutype.getText() == null ? "" : skutype.getText() ;
		String stateskuStr = statesku == null ? "" : statesku.getId() != null ? statesku.getId() : statesku.getText() == null ? "" : statesku.getText() ;
		String supplieridStr = supplierid == null ? "" : supplierid.getId() != null ? supplierid.getId() : supplierid.getText() == null ? "" : supplierid.getText() ;
		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
		String tamanounicoStr = tamanounico == null ? "" : tamanounico.getId() != null ? tamanounico.getId() : tamanounico.getText() == null ? "" : tamanounico.getText() ;
		String tamanounicotStr = tamanounicot == null ? "" : tamanounicot.getId() != null ? tamanounicot.getId() : tamanounicot.getText() == null ? "" : tamanounicot.getText() ;
		String terminosycondicionescrStr = terminosycondicionescr == null ? "" : terminosycondicionescr.getId() != null ? terminosycondicionescr.getId() : terminosycondicionescr.getText() == null ? "" : terminosycondicionescr.getText() ;
		String tipodeetiquetaStr = tipodeetiqueta == null ? "" : tipodeetiqueta.getId() != null ? tipodeetiqueta.getId() : tipodeetiqueta.getText() == null ? "" : tipodeetiqueta.getText() ;
		String typemainbarcodeStr = typemainbarcode == null ? "" : typemainbarcode.getId() != null ? typemainbarcode.getId() : typemainbarcode.getText() == null ? "" : typemainbarcode.getText() ;
		String wfDateCreatedRejectedStr = wfDateCreatedRejected == null ? "" : wfDateCreatedRejected.getId() != null ? wfDateCreatedRejected.getId() : wfDateCreatedRejected.getText() == null ? "" : wfDateCreatedRejected.getText() ;
		String ismarketplaceStr = ismarketplace == null ? "" : ismarketplace.getId() != null ? ismarketplace.getId() : ismarketplace.getText() == null ? "" : ismarketplace.getText() ;

		vals.put( currentStatus == null || "".equals(currentStatus) ? "" : Integer.parseInt(currentStatus) );
		vals.put( prevStatus == null || "".equals(prevStatus) ? "" : Integer.parseInt(prevStatus) );
		vals.put( externalStatus );
		vals.put(brandIdS4hStr);
		vals.put(brandnameStr);
		vals.put(coloursLiverpoolAttStr);
		vals.put(cubiscanStr);
		vals.put(costoenmonedaextranjeraStr);
		vals.put(costonetosinivaStr);
		vals.put(costobrutosinivaStr);
		vals.put(countryoforiginStr);
		vals.put(currencyStr);
		vals.put(descuento1Str);
		vals.put(descuento2Str);
		vals.put(enviomiraklStr);
		vals.put(erroresdesistemaStr);
		vals.put(estatuspropuestaStr);
		vals.put(fechainiciovigenciacostoimportacionStr);
		vals.put(fechainiciovigenciacostonetoStr);
		vals.put(fechainiciovigenciaprecioventaStr);
		vals.put(footnoteStr);
		vals.put(fototomadaliverpoolStr);
		vals.put(idlastparentStr);
		vals.put(idtallaerpStr);
		vals.put(identificanegocioStr);
		vals.put(indicadordeimpuestoStr);
		vals.put(itemgroupStr);
		vals.put(itemgroups4hStr);
		vals.put("00".equals(sapobjecttypeStr) ? "" : mainbarcodeStr);
		vals.put("00".equals(sapobjecttypeStr) ? "" : mainbarcodes4hStr);
		vals.put(mensajecreacionskuStr);
		vals.put(numtpS4hStr);
		vals.put(nameStr);
		vals.put(nameguideStr);
		vals.put(parentStr);
		vals.put(parentidStr);
		vals.put(preciosugeridocivaStr);
		vals.put(producttypeStr);
		vals.put(producttypesapStr);
		vals.put(producttypesap2Str);
		vals.put(producttypesaptempStr);
		vals.put(publicarenatgStr);
		vals.put(sapobjecttypeStr);
		vals.put(sapspartStr);
		vals.put(sapBehvoStr);
		vals.put(sb0002Str);
		vals.put(sbColoresStr);
		vals.put(skuStr);
		vals.put(skucreationdateStr);
		vals.put(sistemaorigenStr);
		vals.put(skutypeStr);
		vals.put(stateskuStr);
		vals.put(supplieridStr);
		vals.put(supplierpartnumberStr);
		vals.put(tamanounicoStr);
		vals.put(tamanounicotStr);
		vals.put(terminosycondicionescrStr);
		vals.put(tipodeetiquetaStr);
		vals.put(typemainbarcodeStr);
		vals.put(wfDateCreatedRejectedStr);
		vals.put(ismarketplaceStr);
		rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + child.getId() + "'@1")).put("values", vals));
		if(rowsArticle.length() == 5000) {
			rw.writeData("list", "Article", null, qp, requestArticle, this::log);
			while(rowsArticle.length() > 0) {
				rowsArticle.remove(0);
			}
		}
		
		childParent.put(child.getId(), !child.getParentId().matches("^(S?[0-9]+)") ? child.getId() : child.getParentId());
		lacuentaVars++;
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
