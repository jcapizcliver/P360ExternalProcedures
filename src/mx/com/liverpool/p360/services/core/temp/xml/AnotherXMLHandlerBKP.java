package mx.com.liverpool.p360.services.core.temp.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandlerBKP {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.regex.Pattern attributeIDPattern = java.util.regex.Pattern.compile("(?<=')(.+)(?=',root)");
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
    private java.util.Map<String, String[]> characteristics = new java.util.TreeMap<>();
	private int bs = 125;
	private final boolean sendProduct = false;
	private final boolean sendLkpValues = false;
	private java.util.Map<String, java.util.Map<String, String>> lkpData = new java.util.TreeMap<>();
	
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
	
    private class Value{

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
    	private java.util.LinkedList<Value> values = null;
    	private java.util.LinkedList<MultiValue> multiValues = null;
    	private Value workingValue = null;
    	private MultiValue workingMultiValue = null;
    	private java.util.LinkedList<Product> products = new java.util.LinkedList<>();
    	private java.util.LinkedList<Classification> classifications = new java.util.LinkedList<>();
    	private Classification workingClassification = null;
    	
    	public Product(String id, String parentId) {
    		this.id = id;
    		this.parentId = parentId;
    	}

		public String getId() {
			return id;
		}

		public String getParentId() {
			return parentId;
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
                if(parentId == null && !productStack.isEmpty()) {
                	parentId = productStack.getLast().getId();
                }
                productStack.addLast(new Product(id, parentId));
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
	            }else if(!assetStack.isEmpty()) {
	            	Asset a = assetStack.getLast();
	            	if("Value".equals(name)) {
	            		Value v = new Value(attributes.getValue("AttributeID"), null, null);
	            		a.setCurrentValue(v);
	            	}else if("Name".equals(name)) {
	            		assetName = true;
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
    	AnotherXMLHandlerBKP an = new AnotherXMLHandlerBKP();
//        if (args.length == 0) {
//            System.err.println("Usage: java ProductValuesSaxParser <file.xml>");
//            System.exit(1);
//        }
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        // Harden parser (avoid XXE)
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        an.qp.put("includeObjectsInProtocol", "false");
        SAXParser parser = factory.newSAXParser();
        java.io.File[] files = new java.io.File( java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "data").toString() ).listFiles(ff -> ff.getName().endsWith("xml"));
//        java.io.File[] files = new java.io.File( java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "samples").toString() ).listFiles(ff -> ff.getName().endsWith("xml"));
//        java.io.File[] files = new java.io.File(args[0]).listFiles(ff -> ff.getName().endsWith("xml"));
//        an.collectCharacteristics(an.characteristics);
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
        long refProductsCount = 0l;
        for(File input : files) {
	        Handler handler = an.new Handler();
	        parser.parse(input, handler);
	        refProductsCount += handler.getPrductsCounter();
        }
        an.log("Total products found: " + refProductsCount);
        an.log("*** Total AttributeID ***");
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
        java.math.BigDecimal bdRefCounter = new java.math.BigDecimal(refProductsCount);
        java.math.BigDecimal HUNDRED = java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.TEN);
        long current = 0l;
        for(File input : files) {
	        Handler handler = an.new Handler();
	        parser.parse(input, handler);
	        for (Product product : handler.getFinished()) {
	        	an.processProduct(product, requestProduct, requestArticle, an.characteristics);
	        }
	        current += handler.getPrductsCounter();
	        an.log(current + "/" + refProductsCount + " " + (new java.math.BigDecimal(current).multiply(HUNDRED).divide( bdRefCounter, 4, java.math.RoundingMode.HALF_UP )) + "% " + an.rw.getRw().formatTime(System.currentTimeMillis() - init));
	        handler.getAssetMap().entrySet().forEach(en ->{
	        	if(an.getValue(en.getValue().getValues(), "ImageURL") !=  null) {
	        		System.out.println(en.getKey() + ". Id: " + en.getValue().getId() + ", Name: " + en.getValue().getName() + ", ImageURL: " + an.getValue(en.getValue().getValues(), "ImageURL"));
	        	}
	        } );
        }
        if(rowsProduct.length() > 0) {
        	an.sendData("Product2G", requestProduct);
        }
        if(rowsArticle.length() > 0) {
        	an.sendData("Article", requestArticle);
        }
        if(an.sendLkpValues) {
        	an.processLkpContent();
        }
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private String getValue(java.util.LinkedList<Value> values, String attributeId) {
    	for(Value value : values) {
    		if(attributeId.equals(value.getAttributeId()))
    			return value.getText();
    	}
    	return "";
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
    				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + subEntry.getKey().replaceAll("'", "\\\\'") + "'@'" + entry.getKey() + "'")).put("values", new org.json.JSONArray().put(subEntry.getValue()).put(true)));
    				if(rows.length() == 1000) {
    					sendData("LookupValue", request);
    				}
    			}
    		}
    	}
//    	java.util.stream.Collector.of
    	if(rows.length() > 0) {
    		sendData("LookupValue", request);
    	}
    }
    
    private void sendLkpContent() {
        org.json.JSONObject request = new org.json.JSONObject();
        org.json.JSONArray columns = new org.json.JSONArray();
        org.json.JSONArray rows = new org.json.JSONArray();
        request.put("columns", columns);
        request.put("rows", rows);
        columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
        columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
        for(java.util.Map.Entry<String, java.util.Map<String, String>> entry : lkpData.entrySet()) {
        	for(java.util.Map.Entry<String, String> subEntry : entry.getValue().entrySet()) {
        		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + subEntry.getKey().replaceAll("'", "\\\\'") + "'@'" + entry.getKey() + "'"))
        				.put("values", new org.json.JSONArray().put(subEntry.getValue()).put(true)));
        		if(rows.length() == 4000) {
        			sendData("LookupValue", request);
        		}
        	}
        }
        if(rows.length() > 0) {
			sendData("LookupValue", request);
		}
    }
    
    private void processProduct(Product product, org.json.JSONObject requestProduct, org.json.JSONObject requestArticle, java.util.Map<String, String[]> characteristics) {
    	java.util.Map<String, String> lkpCont = null;
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
        java.util.LinkedList<Classification> classifications = null;
    	values = product.getValues();
    	children = product.getProducts();
    	classifications = product.getClassifications();
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
    		if(sendProduct) {
	    		addUnidadesDeMedida(valMap);
	    		Value sapObjectType = valMap.get("SAPObjectType");
	    		if(sapObjectType == null) {
	    			System.out.println("Product with no SAPObjectType: " + product.getId());
	    		}else {
		    		org.json.JSONArray columnsP = requestProduct.getJSONArray("columns");
		    		org.json.JSONArray columnsA = requestArticle.getJSONArray("columns");
		    		org.json.JSONArray rowsP = requestProduct.getJSONArray("rows");
		    		org.json.JSONArray rowsA = requestArticle.getJSONArray("rows");
		    		if("00".equals(sapObjectType.getId())) {
		    			addRow("Product2G", product.getId(), product.getParentId(), requestProduct, columnsP, characteristics, valMap, classifications);
		    			if(rowsP.length() == bs) {
		    				sendData("Product2G", requestProduct);
		    			}
		    			addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, characteristics, valMap, classifications);
		    			if(rowsA.length() == bs) {
		    				sendData("Product2G", requestProduct);
		    				sendData("Article", requestArticle);
		    			}
		    		}else if("01".equals(sapObjectType.getId())) {
		    			addRow("Product2G", product.getId(), product.getParentId(), requestProduct, columnsP, characteristics, valMap, classifications);
		    			if(rowsP.length() == bs) {
		    				sendData("Product2G", requestProduct);
		    			}
		    		}else if("02".equals(sapObjectType.getId())) {
		    			addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, characteristics, valMap, classifications);
		    			if(rowsA.length() == bs) {
		    				sendData("Product2G", requestProduct);
		    				sendData("Article", requestArticle);
		    			}
		    		}else {
		    			System.out.println("Diff SAPObjectType: " + sapObjectType.getId());
		    		}
	    		}
    		}
    	}
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, requestProduct, requestArticle, characteristics);
    		}
    	}
    }
    
    private void addRow(String entity, String id, String parentId, org.json.JSONObject requestProduct, org.json.JSONArray columnsP, java.util.Map<String, String[]> characteristics, java.util.Map<String, Value> valMap, java.util.LinkedList<Classification> classifications) {
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
			}
			m = attributeIDPattern.matcher(column);
			if(m.find()) {
				attributeId = m.group();
				data = characteristics.get(attributeId);
				value = valMap.get(attributeId);
				if("DescriptionLong".equals(attributeId)) {
					descLong = value == null ? "" : value.getText();
				}
				rowValues.put("DescriptionLong".equals(attributeId) || value == null ? "" : "LOOKUP".equals(data[1]) ? new org.json.JSONObject().put("id", "'" + (value.getId() != null ? value.getId() : value.getText()).replaceAll("'", "\\\\'") + "'@'" + data[0] + "'") : formatPlainValue( attributeId, value.getText(), data[1]) );
			}else {
				System.out.println("PANIC: no match for column pattern: " + column);
			}
		}
		if("Product2G".equals(entity)) {
			rowValues.put( descLong == null ? "" : descLong );
			rowValues.put(new org.json.JSONArray().put( parentId != null && parentId.startsWith("EU4-") ? parentId : "" ));
			org.json.JSONArray valSitiosWeb = new org.json.JSONArray();
			if(classifications != null) {
//				for(Classification classification : classifications) {
//					if("WebsiteLink".equals(classification.getType())) {
//						valSitiosWeb.put( classification.getId() );
//					}
//				}
				if(valSitiosWeb.length() == 0) {
					valSitiosWeb.put("");
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
//				log("Problem parsing a decimal data, the data: " + value);
				return null;
			}
		} else if("INTEGER".equals(dataType)) {
			try{
				return new java.math.BigDecimal(value).intValue();
			}catch(NumberFormatException e) {
//				log("Problem interpreting a number data, the data: " +  value);
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
    				logCorrectlyWrittenIDs(request, response);
    			}else {
    				log("Unparseable response: " + rr);
    			}
    		}catch(org.json.JSONException e) {
    			log("Got: " + rr);
    			logE(e);
    		}
    	});
    }

	private void logCorrectlyWrittenIDs(org.json.JSONObject request, org.json.JSONObject response) {
		java.util.Set<Integer> rowsWithErrors = new java.util.TreeSet<>();
		org.json.JSONArray objects = request.getJSONArray("rows");
		org.json.JSONArray entries = response.has("entries") ? response.getJSONArray("entries") : new org.json.JSONArray();
		org.json.JSONObject entry = null;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
        		new java.io.FileOutputStream("../logs/list_api_load_from_step_proposals_wrong.log", true)))) {
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
    		}
        } catch (java.io.IOException e) {
        	e.printStackTrace();
        }
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/list_api_load_from_step_proposals_correct.log", true)))) {
        	for(int i=0; i<objects.length(); i++) {
        		if(!rowsWithErrors.contains(i))
        			pw.println( objects.getJSONObject(i).getJSONObject("object").getString("id") );
        	}
        } catch (java.io.IOException e) {
        	e.printStackTrace();
        }
    }

	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/list_api_load_from_step.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

	private void logE(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/list_api_load_from_step_errors.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","list_api_load_from_step.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
    
	private static final java.util.Set<String> ofInterest = new java.util.TreeSet<>( java.util.Arrays.asList(new String[] {
			"Direction",
			"Section",
			"ItemGroup",
			"ItemGroupS4H",
			"SKU",
			"MainBarCode",
			"MainBarCodeS4H",
			"BrandName",
			"BRAND_ID_S4H",
			"SupplierPartNumber",
			"CalculatedWF_Att",
			"Path",
			"Negocio",
			"ParentSKU",
			"SkuType",
			"Name",
			"ProductName",
			"SupplierID",
			"SupplierName",
			"StateSKU",
			"SKUCreationDate",
			"LastDateApprove",
			"FirstDateApprove",
			"SAPObjectType"
	})
			);
}
