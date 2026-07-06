package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandler2 {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.regex.Pattern attributeIDPattern = java.util.regex.Pattern.compile("(?<=')(.+)(?=',root)");
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
    private java.util.Map<String, String[]> characteristics = new java.util.TreeMap<>();
	private int bs = 110;
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private final java.util.Map<String, java.util.Map<String, String>> lkpData = new java.util.TreeMap<>();
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step.log");
	private java.nio.file.Path correctIDsFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_proposals_correct.log");
	private java.nio.file.Path errorIDsFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_proposals_wrong.log");
	private final java.util.regex.Pattern valuePattern = java.util.regex.Pattern.compile("^'(.+)'@'.+'$");
	private final java.util.Set<String> aEnviar = new java.util.TreeSet<>();
	private final java.util.Set<String> aEnviarV = new java.util.TreeSet<>();
	private final java.util.LinkedList<String> ids = new java.util.LinkedList<>();
	private final java.util.LinkedList<String> idsV = new java.util.LinkedList<>();
	private final boolean specific = false;
	

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
    
    private void collectCharacteristics(java.util.Map<String, String[]> data) {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
    	qp.put("fields", 
    			  "Characteristic.Identifier"
    			+ ",Characteristic.Lookup->Lookup.Identifier"
    			+ ",Characteristic.DataType"
    			+ ",Characteristic.Entities"
    		);
    	qp.put("query", "Characteristic.ParentCharacteristic is empty and Characteristic.IsActive = true and not Characteristic.DataType = NONE");
    	qp.put("pageSize", "1200");
    	rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> data.put(row.getJSONArray("values").getString(0), new String[] { 
    			  row.getJSONArray("values").getString(1)
    			, row.getJSONArray("values").getString(2) 
    			, row.getJSONArray("values").getJSONArray(3).join(",") 
    			}), System.out::println);
    }

    // Entry point
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandler2 an = new AnotherXMLHandler2();

    	java.util.List<String> currentProducts = new java.util.ArrayList<>(sendProduct ? 2000000 : 10);
//    	if(sendProduct) {
    	an.log("Now collecting current known products");
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "100000");
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> currentProducts.add(row.getJSONArray("values").getString(0)), an::log);
		qp.put("fields", "Article.SupplierAID");
		rw.collectData("list", "Article", null, "byCatalog", qp, row -> currentProducts.add(row.getJSONArray("values").getString(0)), an::log);
		an.log("Collected: " + currentProducts.size());
//    	}
    	
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	
    	an.normalLogFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step.log");
    	an.correctIDsFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_proposals_correct.log");
    	an.errorIDsFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_proposals_wrong.log");
        
    	if (args.length == 0) {
            System.err.println("Usage: java AnotherXMLHandler2 <file.xml>");
            System.exit(1);
        }

//        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "faltantes_blocke_en_flujo"))){
//        	lns.forEach(an.aEnviar::add);
//        }catch(java.io.IOException e) {
//        	an.logE(e);
//        }
//        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "faltantes_blocke_en_flujo_otros"))){
//        	lns.forEach(an.aEnviar::add);
//        }catch(java.io.IOException e) {
//        	an.logE(e);
//        }
//        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "faltantes_blocke_en_flujo_variantes"))){
//        	lns.forEach(an.aEnviarV::add);
//        }catch(java.io.IOException e) {
//        	an.logE(e);
//        }
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.qp.put("includeObjectsInProtocol", "false");
        org.json.JSONObject requestProduct = new org.json.JSONObject();
        org.json.JSONObject requestArticle = new org.json.JSONObject();
        org.json.JSONArray columnsProduct = new org.json.JSONArray();
        org.json.JSONArray columnsArticle = new org.json.JSONArray();
        org.json.JSONArray rowsProduct = new org.json.JSONArray();
        org.json.JSONArray rowsArticle = new org.json.JSONArray();
        requestProduct.put("columns", columnsProduct);
        requestArticle.put("columns", columnsArticle);
        requestProduct.put("rows", rowsProduct);
        requestArticle.put("rows", rowsArticle);
        java.io.File[] files = new java.io.File(args[0]).listFiles(ff -> ff.getName().endsWith("xml"));
        long refProductsCount = 0l;
        long in = System.currentTimeMillis();
        an.log("Now reading files...");
        for(File input : files) {
	        Handler handler = an.new Handler();
	        try {
	        	parser.parse(input, handler);
		        refProductsCount += handler.getPrductsCounter();
	        }catch(org.xml.sax.SAXParseException e) {
	        	an.log("Problem processing following file: " + input.getName());
	        }
        }
        an.log("Parsing files took: " + an.rw.getRw().formatTime(System.currentTimeMillis() - in));
        an.log("Total products found: " + refProductsCount);
        an.log("*** Total AttributeID ***");
        an.collectCharacteristics(an.characteristics);
        String[] data = null;
        for(String attributeId : an.attributeIDs) {
        	data = an.characteristics.get(attributeId);
        	if(data != null) {
        		if(data[2].contains("Product2G")) {
        			columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('" + attributeId + "',root,\"0000.0000.RK\",'" + attributeId + "',-1)"));
        		}
        		if(data[2].contains("Article")) {
        			columnsArticle.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('" + attributeId + "',root,\"0000.0000.RK\",'" + attributeId + "',-1)"));
        		}
        	}else {
        		System.out.println("Missing this one: " + attributeId);
        	}
        }
        columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('UnidadDeMedidaLongitud',root,\"0000.0000.RK\",'UnidadDeMedidaLongitud',-1)"));
        columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('UnidadDeMedidaPeso',root,\"0000.0000.RK\",'UnidadDeMedidaPeso',-1)"));
        columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('UnidadDeMedidaVolumen',root,\"0000.0000.RK\",'UnidadDeMedidaVolumen',-1)"));
        columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionLong(es)"));
        columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('PrimaryProductTaxonomy')"));
        columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('Sitios Web')"));
        long current = 0l;
        for(File input : files) {
	        Handler handler = an.new Handler();
	        try {
		        parser.parse(input, handler);
		        for (Product product : handler.getFinished()) {
	        		an.processProduct(product, requestProduct, requestArticle, currentProducts);
		        }
		        current += handler.getPrductsCounter();
	        }catch(org.xml.sax.SAXParseException e) {
	        	an.log("Problem processing following file: " + input.getName());
	        }
        }
        an.log("Done, found: " + current + " products.");
        if(rowsProduct.length() > 0) {
        	an.sendData("Product2G", requestProduct);
        }
        if(rowsArticle.length() > 0) {
        	an.sendData("Article", requestArticle);
        }
        if(AnotherXMLHandler2.sendLkpValues) {
        	an.processLkpContent();
        }
        /*
        RESTWrapper rw = new RESTWrapper();
        java.util.Set<String> nids = new java.util.TreeSet<>(an.ids);
        System.out.println(an.ids.size() + "/" + nids.size());
        java.util.Set<String> nidsV = new java.util.TreeSet<>(an.idsV);
        System.out.println("For variants: " + an.idsV.size() + "/" + nidsV.size());
        int a = 0;
        StringBuilder sb = new StringBuilder();
        java.util.Map<String, String> qpc = new java.util.TreeMap<>();
        qpc.put("fields", "Product2G.ProductNo");
        qpc.put("pageSize", "1000");
        java.util.Set<String> currentSet = new java.util.TreeSet<>();
        java.util.LinkedList<String> notFound = new java.util.LinkedList<>();
        java.util.LinkedList<String> notFoundV = new java.util.LinkedList<>();
        java.util.Set<String> collected = new java.util.TreeSet<>();
        for(String id : nids) {
        	sb.append(a == 0 || a%1000 == 0 ? "" : ",");
        	sb.append("'");
        	sb.append(id);
        	sb.append("'@1");
        	currentSet.add(id);
        	a++;
        	if(a % 1000 == 0) {
        		qpc.put("items", sb.toString());
        		System.out.println("Searching for: " + sb.toString());
        		rw.collectData("list", "Product2G", null, "byItems", qpc, row -> collected.add(row.getJSONArray("values").getString(0)), System.out::println);
        		for(String currentId : currentSet) {
        			if(!collected.contains(currentId)) {
        				notFound.addLast(currentId);
        			}
        		}
        		sb.setLength(0);
        		collected.clear();
        		currentSet.clear();
        		System.out.println(a + "/" + nids.size());
        	}
        }
        if(a % 1000 != 0) {
        	qpc.put("items", sb.toString());
    		rw.collectData("list", "Product2G", null, "byItems", qpc, row -> collected.add(row.getJSONArray("values").getString(0)), System.out::println);
    		for(String currentId : currentSet) {
    			if(!collected.contains(currentId)) {
    				notFound.addLast(currentId);
    			}
    		}
    		collected.clear();
    		currentSet.clear();
    		System.out.println(a + "/" + nids.size());
        }
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "faltantes_blocke_en_flujo").toFile())))){
        	notFound.forEach(pw::println);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        sb.setLength(0);
        a = 0;
        qpc.put("fields", "Article.SupplierAID");
        for(String id : nidsV) {
        	sb.append(a == 0 || a%1000 == 0 ? "" : ",");
        	sb.append("'");
        	sb.append(id);
        	sb.append("'@1");
        	currentSet.add(id);
        	a++;
        	if(a % 1000 == 0) {
        		qpc.put("items", sb.toString());
        		System.out.println("Searching for variants: " + sb.toString());
        		rw.collectData("list", "Article", null, "byItems", qpc, row -> collected.add(row.getJSONArray("values").getString(0)), System.out::println);
        		for(String currentId : currentSet) {
        			if(!collected.contains(currentId)) {
        				notFoundV.addLast(currentId);
        			}
        		}
        		sb.setLength(0);
        		collected.clear();
        		currentSet.clear();
        		System.out.println(a + "/" + nidsV.size());
        	}
        }
        if(a % 1000 != 0) {
        	qpc.put("items", sb.toString());
    		rw.collectData("list", "Article", null, "byItems", qpc, row -> collected.add(row.getJSONArray("values").getString(0)), System.out::println);
    		for(String currentId : currentSet) {
    			if(!collected.contains(currentId)) {
    				notFoundV.addLast(currentId);
    			}
    		}
    		collected.clear();
    		currentSet.clear();
    		System.out.println(a + "/" + nidsV.size());
        }
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "faltantes_blocke_en_flujo_variantes").toFile())))){
        	notFoundV.forEach(pw::println);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        */
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private void processLkpContent() {
    	System.out.println("Working on: " + lkpData.size() + " lookups");
    	java.util.Map<String, java.util.Map<String, String>> masterDictionary = new java.util.TreeMap<>();
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
    	qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
    	qp.put("pageSize", "2000");
    	for(java.util.Map.Entry<String, java.util.Map<String, String>> entry : lkpData.entrySet()) {
    		System.out.println("Collecting: " + entry.getKey());
    		qp.put("lookup", "'" + entry.getKey() + "'");
    		java.util.Map<String, String> data = new java.util.TreeMap<>();
    		rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> {
    			data.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
    		}, this::log);
    		masterDictionary.put(entry.getKey(), data);
    	}
    	org.json.JSONObject request = new org.json.JSONObject();
    	org.json.JSONArray columns = new org.json.JSONArray();
    	org.json.JSONArray rows = new org.json.JSONArray();
    	request.put("columns", columns);
    	request.put("rows", rows);
    	columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
    	columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
    	java.util.Map<String, String> data = null;
    	String value = null;
    	for(java.util.Map.Entry<String, java.util.Map<String, String>> entry : lkpData.entrySet()) {
    		data = masterDictionary.get(entry.getKey());
    		for(java.util.Map.Entry<String, String> subEntry : entry.getValue().entrySet()) {
    			value = data.get(subEntry.getKey());
    			if(value == null || !value.equals(subEntry.getValue())){
    				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + subEntry.getKey() + "'@'" + entry.getKey() + "'")).put("values", new org.json.JSONArray().put(subEntry.getValue()).put(true)));
    				if(rows.length() == 500) {
    					sendData("LookupValue", request);
    				}
    			}
    		}
    	}
    	if(rows.length() > 0) {
    		sendData("LookupValue", request);
    	}
    }
    
    private void processProduct(Product product, org.json.JSONObject requestProduct, org.json.JSONObject requestArticle, java.util.List<String> alreadyInSystem) {
    	if(alreadyInSystem.contains(product.getId())) {
    		return;
    	}
    	java.util.Map<String, String> lkpCont = null;
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
        java.util.LinkedList<Classification> classifications = null;
    	values = product.getValues();
    	children = product.getProducts();
    	classifications = product.getClassifications();
    	if(product.getUserTypeId().startsWith("SalesItemFamily") ){
    		ids.add(product.getId());
    	}else if(product.getUserTypeId().equals("SalesItemVariant")) {
    		idsV.add(product.getId());
    	}else if("SalesItem".equals(product.getUserTypeId())) {
    		if(product.getParentId().startsWith("EU") || product.getParentId().startsWith("UnCatLevel")) {
    			ids.add(product.getId());
    		}
    		idsV.add(product.getId());
    	}else {
    		System.out.println("Not a known userTypeID: " + product.getUserTypeId());
    	}
    	if(values != null) {
    		String[] data = null;
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    			if(sendLkpValues) {
    				data = characteristics.get(value.attributeId);
    				if(data != null) {
    					if("LOOKUP".equals(data[1])) {
    						lkpCont = lkpData.get(data[0]);
    						if(lkpCont == null) {
    							lkpCont = new java.util.TreeMap<>();
    							lkpData.put(data[0], lkpCont);
    						}
    						lkpCont.put(value.getId() == null ? value.getText() : value.getId(), value.getText());
    					}
    				}
    			}
    		}
    		if( (sendProduct && !specific) || 
    				( sendProduct && specific && (aEnviar.contains(product.getId()) || aEnviarV.contains(product.getId() )) )
    		) {
	    		addUnidadesDeMedida(valMap);
	    		org.json.JSONArray columnsP = requestProduct.getJSONArray("columns");
	    		org.json.JSONArray columnsA = requestArticle.getJSONArray("columns");
	    		org.json.JSONArray rowsP = requestProduct.getJSONArray("rows");
	    		org.json.JSONArray rowsA = requestArticle.getJSONArray("rows");
    			if( "SalesItemVariant".equals(product.getUserTypeId()) ) {
    				String parentId = product.getParentId();
    				Value parentIdValue = valMap.get("ParentID");
    				parentId = parentId == null ? parentIdValue != null ? parentIdValue.getText() : null : parentId;
    				if(parentId != null) {
    					addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
		    			if(rowsA.length() == bs) {
		    				sendData("Article", requestArticle);
		    			}
    				}else {
    					log("SalesItemVariant with no ParentID: " + product.getId());
    				}
    			}else if("SalesItem".equals(product.getUserTypeId())) {
    				String parentId = product.getParentId();
    				Value parentIdValue = valMap.get("ParentID");
    				parentId = parentId == null ? parentIdValue != null ? parentIdValue.getText() : null : parentId;
    				if(parentId != null && !parentId.startsWith("EU") && !parentId.startsWith("UnCatLevel")) {
    					addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
		    			if(rowsA.length() == bs) {
		    				sendData("Article", requestArticle);
		    			}
    				}else if(parentId != null && (parentId.startsWith("EU") || parentId.startsWith("UnCatLevel") )){
    					addRow("Product2G", product.getId(), product.getParentId(), requestProduct, columnsP, valMap, classifications);
		    			if(rowsP.length() == bs) {
		    				sendData("Product2G", requestProduct);
		    			}
		    			addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
		    			if(rowsA.length() == bs) {
		    				sendData("Article", requestArticle);
		    			}
    				}else {
    					log("SalesItem with no propper ParentID: " + product.getId());
    				}
    			}else if(product.getUserTypeId().startsWith("SalesItemFamily")) {
    				String parentId = product.getParentId();
    				Value parentIdValue = valMap.get("ParentID");
    				parentId = parentId == null ? parentIdValue != null ? parentIdValue.getText() : null : parentId;
    				if(parentId != null){
    					addRow("Product2G", product.getId(), product.getParentId(), requestProduct, columnsP, valMap, classifications);
		    			if(rowsP.length() == bs) {
		    				sendData("Product2G", requestProduct);
		    			}
    				}else {
    					log("SalesItem with no propper ParentID: " + product.getId());
    				}
    			}else {
    				log("PANIC: no product type found: " + product.getUserTypeId());
    			}
    		}
    	}
    	
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, requestProduct, requestArticle, alreadyInSystem);
    		}
    	}
    }
    
    private void addRow(String entity, String id, String parentId, org.json.JSONObject requestProduct, org.json.JSONArray columnsP, java.util.Map<String, Value> valMap, java.util.LinkedList<Classification> classifications) {
    	org.json.JSONArray rowsP = requestProduct.getJSONArray("rows");
    	org.json.JSONArray rowValues = new org.json.JSONArray();
		org.json.JSONObject row = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", rowValues);
		String column = null;
		java.util.regex.Matcher m = null;
		String attributeId = null;
		String[] data = null;
		Value value = null;
		rowsP.put(row);
		String descLong = null;
		for(int i=0; i<columnsP.length(); i++) {
			column = columnsP.getJSONObject(i).getString("identifier");
			if(column.contains("Product2GStructureMap.ManualMap")) {
				continue;
			}// Product2GCharacteristicValueLang.Value('SKU',root,"0000.0000.RK",'SKU',-1)
			m = attributeIDPattern.matcher(column);
			if(m.find()) {
				attributeId = m.group();
				data = characteristics.get(attributeId);
				value = valMap.get(attributeId);
				if("DescriptionLong".equals(attributeId)) {
					descLong = value == null ? "" : value.getText();
				}
				rowValues.put("DescriptionLong".equals(attributeId) || value == null ? "" : "LOOKUP".equals(data[1]) ? new org.json.JSONObject().put("id", "'" + (value.getId() != null ? value.getId().replaceAll("'", "\\\\'") : value.getText().replaceAll("'", "\\\\'")) + "'@'" + data[0] + "'") : formatPlainValue( attributeId, value.getText(), data[1]) );
			}else {
				log("PANIC: Not able to determine characteristic from column: " + column);
			}
		}
		if("Product2G".equals(entity)) {
			rowValues.put( descLong == null ? "" : descLong );
			rowValues.put(new org.json.JSONArray().put( parentId != null && parentId.startsWith("EU4-") || parentId.startsWith("UnCatLevel") ? parentId : "" ));
			org.json.JSONArray valSitiosWeb = new org.json.JSONArray();
			if(classifications != null) {
				if(valSitiosWeb.length() == 0) {
					valSitiosWeb.put("");
				}else {
//					for(Classification classif : classifications) {
//						if(classif.getType()) {
//							
//						}
//					}
				}
				rowValues.put(valSitiosWeb);
			}else {
				valSitiosWeb.put("");
				rowValues.put(valSitiosWeb);
			}
		}
    }
	
	private void addUnidadesDeMedida(java.util.Map<String, Value> values) {
		java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesLongitud.put("unece.unit.MTR", "M");
		unidadesLongitud.put("unece.unit.MMT", "MM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		unidadesVolumen.put("unece.unit.LTR", "L");
		unidadesVolumen.put("unece.unit.FTQ", "PI3");
		unidadesVolumen.put("unece.unit.MTQ", "M3");
		unidadesVolumen.put("unece.unit.GRM", "G");
		String unidadDeMedidaLongitud = null;
		String unidadDeMedidaVolumen = null;
		String unidadDeMedidaPeso = null;
		String[] atributosLongitud = new String[] { "ProductWidth", "ProductDepth", "ProductHeight", "ZBRECJ", "ZLAECJ", "ZHOECJ", "ZHOEPQ", "ZBREPQ", "ZLAEPQ" };
		String[] atributosVolumen = new String[] { "VOLUMAtt", "ZVOLCJ", "ZVOLPQ" };
		String[] atributosPeso = new String[] { "PesoBruto", "ProductWeight", "ZBRGCJ", "ZNTGCJ", "ZBRGPQ", "ZNTGPQ" };
		Value value = null;
		for(String a : atributosLongitud) {
			value = values.get(a);
			if(value != null) {
				unidadDeMedidaLongitud = unidadesLongitud.get(value.getUnidadId());
				break;
			}
		}
		for(String a : atributosVolumen) {
			value = values.get(a);
			if(value != null) {
				unidadDeMedidaVolumen = unidadesVolumen.get( value.getUnidadId() );
				break;
			}
		}
		for(String a : atributosPeso) {
			value = values.get(a);
			if(value != null) {
				unidadDeMedidaPeso = unidadesPeso.get( value.getUnidadId() );
				break;
			}
		}
		if(unidadDeMedidaLongitud == null) {
		} else {
			Value uml = new Value("UnidadDeMedidaLongitud", unidadDeMedidaLongitud, null);
			values.put("UnidadDeMedidaLongitud", uml);
		}
		if(unidadDeMedidaPeso == null) {
		}else {
			Value ump = new Value("UnidadDeMedidaPeso", unidadDeMedidaPeso, null);
			values.put("UnidadDeMedidaPeso", ump);
		}
		if(unidadDeMedidaVolumen == null) {
		}else {
			Value umv = new Value("UnidadDeMedidaVolumen", unidadDeMedidaVolumen, null);
			values.put("UnidadDeMedidaVolumen", umv);
		}
		
	}
	
	private Object formatPlainValue(String characteristic, String value, String dataType) {
		if("DATETIME".equals(dataType)) {
			try {
				return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format( new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy").parse(value) );
			}catch(java.text.ParseException e) {
				try {
					return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format( new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value) );
				}catch(java.text.ParseException ex) {
				}
			}
		} else if("DECIMAL".equals(dataType)) {
			try{
				return new java.math.BigDecimal(value);
			}catch(NumberFormatException e) {
				return null;
			}
		} else if("INTEGER".equals(dataType)) {
			try{
				return new java.math.BigDecimal(value).intValue();
			}catch(NumberFormatException e) {
				return null;
			}
		}
		return value;
	}
    
    private void sendData(String entity, org.json.JSONObject request) {
    	log("Sending: " + request.getJSONArray("rows").length());
    	rw.writeData("list", entity, null, qp, request, rr->{
    		try {
    			org.json.JSONObject response = new org.json.JSONObject(rr);
    			if(response.has("counters")) {
    				log(response.getJSONObject("counters").toString());
    				logCorrectlyWrittenIDs(request, response, entity);
    			}else {
    				log("Unparseable response: " + rr);
    			}
    		}catch(org.json.JSONException e) {
    			log("Got: " + rr);
    			logE(e);
    		}
    	});
//    	emptyArray(request.getJSONArray("rows"));
    }
    
    private void emptyArray(org.json.JSONArray rows) {
    	while(rows.length() > 0) {
    		rows.remove(0);
    	}
    }

	private void logCorrectlyWrittenIDs(org.json.JSONObject request, org.json.JSONObject response, String entity) {
		java.util.Set<Integer> rowsWithErrors = new java.util.TreeSet<>();
		org.json.JSONArray objects = request.getJSONArray("rows");
		org.json.JSONArray entries = response.has("entries") ? response.getJSONArray("entries") : new org.json.JSONArray();
		org.json.JSONObject entry = null;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
        		new java.io.FileOutputStream(errorIDsFilePath.toString(), true)))) {
    		for(int i=0; i<entries.length(); i++) {
				entry = entries.getJSONObject(i);
				rowsWithErrors.add(entry.getInt("row"));
				entry.put("_externalId", objects.getJSONObject(entries.getJSONObject(i).getInt("row")).getJSONObject("object").getString("id"));
				pw.println(rw.getRw().serializeChunk(
						new String[] { 
								  entry.getString("category")
								, entry.getString("logDate")
								, entry.getString("logTime")
								, entry.getString("_externalId")
								, entry.getString("message") 
								, entry.getString("severity") 
							}, "\"", ",", "\\"));
				if("The given lookup value is not contained in the lookup of the characteristic.".equals(entry.getString("message"))) {
					furtherProblemIdentification(objects.getJSONObject(entries.getJSONObject(i).getInt("row")), request.getJSONArray("columns"), entity);
				}
    		}
        } catch (java.io.IOException e) {
        	e.printStackTrace();
        }
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(correctIDsFilePath.toString(), true)))) {
        	for(int i=0; i<objects.length(); i++) {
        		if(!rowsWithErrors.contains(i)) {
        			pw.println( objects.getJSONObject(i).getJSONObject("object").getString("id") );
        		}
        	}
        } catch (java.io.IOException e) {
        	e.printStackTrace();
        }
    }
	
	private void furtherProblemIdentification(org.json.JSONObject row, org.json.JSONArray columns, String entity) {
		java.util.regex.Matcher m = null;
		String column = null;
		String[] data = null;
		String attributeId = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("pageSize", "2000");
		String value = null;
		java.util.Map<String, String> elMap = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray lookupColumns = new org.json.JSONArray();
		request.put("columns", lookupColumns);
		request.put("rows", rows);
		lookupColumns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		for(int i=0; i<columns.length(); i++) {
			column = columns.getJSONObject(i).getString("identifier");
			m = attributeIDPattern.matcher(column);
			if(m.find()) {
				attributeId = m.group();
				data = characteristics.get(attributeId);
				if("LOOKUP".equals(data[1])) {
					elMap = lkpData.get(data[0]);
					if(elMap == null) {
			    		System.out.println("Collecting: " + data[0]);
			    		qp.put("lookup", "'" + data[0] + "'");
			    		java.util.Map<String, String> data0 = new java.util.TreeMap<>();
			    		rw.collectData("list", "LookupValue", null, "byLookup", qp, row0 -> {
			    			data0.put(row0.getJSONArray("values").getString(0), row0.getJSONArray("values").getString(1));
			    		}, this::log);
			    		lkpData.put(data[0], data0);
			    		elMap = data0;
					}
					value = row.getJSONArray("values").get(i) instanceof org.json.JSONObject ? row.getJSONArray("values").getJSONObject(i).getString("id").replaceAll("\\\\'", "'") : String.valueOf( row.getJSONArray("values").get(i) ).replaceAll("\\\\'", "'");
					if(!"".equals(value)) {
						m = valuePattern.matcher(value);
						if(m.find()) {
							value = m.group(1);
							log("Checking: " + value);
							if(!elMap.containsKey(value)) {
								log("Value not found: " + value + ", in: " + data[0] + " for column: " + column);
								rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + value.replaceAll("'", "\\\\'") + "'@'" + data[0] + "'")).put("values", new org.json.JSONArray().put(true)));
							}
						}else {
							log("Problem applying pattern to value: " + value);
						}
					}
				}
			}
		}
		if(rows.length() > 0) {
			sendData( "LookupValue", request );
			log("Now retrying...");
			sendData( entity , new org.json.JSONObject().put("columns", columns).put("rows", new org.json.JSONArray().put(row)) );
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

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(normalLogFilePath.toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
