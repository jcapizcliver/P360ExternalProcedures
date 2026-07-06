package mx.com.liverpool.p360.services.core.temp.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandlerToIdentifyIDFromSKU {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.regex.Pattern attributeIDPattern = java.util.regex.Pattern.compile("(?<=')(.+)(?=',root)");
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
    private java.util.Map<String, String[]> characteristics = new java.util.TreeMap<>();
	private int bs = 130;
	private final boolean sendProduct = true;
	private final boolean sendLkpValues = false;
	private java.util.Map<String, java.util.Map<String, String>> lkpData = new java.util.TreeMap<>();
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "reprocess_list_api_load_from_step.log");
	private java.nio.file.Path correctIDsFilePath = java.nio.file.Paths.get("..", "logs", "reprocess_list_api_load_from_step_proposals_correct.log");
	private java.nio.file.Path errorIDsFilePath = java.nio.file.Paths.get("..", "logs", "reprocess_list_api_load_from_step_proposals_wrong.log");
	
	private java.nio.file.Path normalAttemptsWithError = java.nio.file.Paths.get("/", "u01", "workshop", "java", "compare_missing_ids");
	private java.util.regex.Pattern valuePattern = java.util.regex.Pattern.compile("^'(.+)'@'.+'$");
	
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
    	private long productsCounter = 0l;

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
            }else {
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
            }
        }
        
        public long getPrductsCounter() {
        	return productsCounter;
        }

        public List<Product> getFinished() {
            return finished;
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
    	AnotherXMLHandlerToIdentifyIDFromSKU an = new AnotherXMLHandlerToIdentifyIDFromSKU();
        if (args.length == 0) {
            System.err.println("Usage: java ProductValuesSaxParser <file.xml>");
            System.exit(1);
        }
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        // Harden parser (avoid XXE)
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        java.util.Set<String> skus = new java.util.TreeSet<>();
        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("/u01/workshop/data/migración/skus_primera_faltantes", "unique"))){
        	lns.forEach(skus::add);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        an.log("Got: " + skus.size() + " to process...");
        an.qp.put("includeObjectsInProtocol", "false");
        SAXParser parser = factory.newSAXParser();
        java.io.File[] files = new java.io.File(args[0]).listFiles(ff -> ff.getName().endsWith("xml"));
        an.collectCharacteristics(an.characteristics);
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
        long current = 0l;
        String sku = null;
        for(File input : files) {
	        Handler handler = an.new Handler();
	        parser.parse(input, handler);
	        for (Product product : handler.getFinished()) {
	        	sku = an.getSKU(product.getValues());
	        	if(sku != null && !sku.isEmpty() && skus.contains(sku)) {
	        		an.processProduct(product, requestProduct, requestArticle);
	        	}else {
	        	}
	        }
	        current += handler.getPrductsCounter();
        }
        an.log("Done, found: " + current + " products.");
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
    
    private String getSKU(java.util.LinkedList<Value> values) {
    	if(values != null) {
	    	for(Value value : values) {
	    		if("SKU".equals(value.getAttributeId())) {
	    			return value.getText();
	    		}
	    	}
    	}
    	return null;
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
    				if(rows.length() == 1000) {
    					sendData("LookupValue", request);
    				}
    			}
    		}
    	}
    	if(rows.length() > 0) {
    		sendData("LookupValue", request);
    	}
    }
    
    private void readToBeReSent(java.util.List<String> todo) {
    	try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(normalAttemptsWithError)){
    		todo.addAll( lns.map(this::retrieveID).filter(s -> s != null).toList() );
    	}catch(java.io.IOException e) {
    		e.printStackTrace();
    	}
    }
    
    private String retrieveID(String line) {
//    	java.util.regex.Matcher m = idPattern.matcher(line);
//    	if(m.find()) {
//    		return m.group(1);
//    	}
//    	if(line.matches("^S\\d+$"))
    		return line;
    }
    
    private void processProduct(Product product, org.json.JSONObject requestProduct, org.json.JSONObject requestArticle) {
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
	    		Value sapObjectTypeValue = valMap.get("SAPObjectType");
	    		String sapObjectType = sapObjectTypeValue == null ? "" : sapObjectTypeValue.getId();
	    		org.json.JSONArray columnsP = requestProduct.getJSONArray("columns");
	    		org.json.JSONArray columnsA = requestArticle.getJSONArray("columns");
	    		org.json.JSONArray rowsP = requestProduct.getJSONArray("rows");
	    		org.json.JSONArray rowsA = requestArticle.getJSONArray("rows");
	    		if("00".equals(sapObjectType)) {
	    			addRow("Product2G", product.getId(), product.getParentId(), requestProduct, columnsP, valMap, classifications);
	    			if(rowsP.length() == bs) {
	    				sendData("Product2G", requestProduct);
	    			}
	    			addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
	    			if(rowsA.length() == bs) {
	    				sendData("Product2G", requestProduct);
	    				sendData("Article", requestArticle);
	    			}
	    		}else if("01".equals(sapObjectType)) {
	    			addRow("Product2G", product.getId(), product.getParentId(), requestProduct, columnsP, valMap, classifications);
	    			if(rowsP.length() == bs) {
	    				sendData("Product2G", requestProduct);
	    			}
	    		}else if("02".equals(sapObjectType)) {
	    			addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
	    			if(rowsA.length() == bs) {
	    				sendData("Product2G", requestProduct);
	    				sendData("Article", requestArticle);
	    			}
	    		}else {
	    			if( "SalesItemVariant".equals(product.getUserTypeId()) ) {
	    				String parentId = product.getParentId();
	    				Value parentIdValue = valMap.get("ParentID");
	    				parentId = parentId == null ? parentIdValue != null ? parentIdValue.getText() : null : parentId;
	    				if(parentId != null) {
	    					addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
			    			if(rowsA.length() == bs) {
			    				sendData("Product2G", requestProduct);
			    				sendData("Article", requestArticle);
			    			}
	    				}else {
	    					log("SalesItemVariant with no ParentID: " + product.getId());
	    				}
	    			}else if("SalesItem".equals(product.getUserTypeId())) {
	    				String parentId = product.getParentId();
	    				Value parentIdValue = valMap.get("ParentID");
	    				parentId = parentId == null ? parentIdValue != null ? parentIdValue.getText() : null : parentId;
	    				if(parentId != null && parentId.startsWith("S")) {
	    					addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
			    			if(rowsA.length() == bs) {
			    				sendData("Product2G", requestProduct);
			    				sendData("Article", requestArticle);
			    			}
	    				}else if(parentId != null){
	    					addRow("Product2G", product.getId(), product.getParentId(), requestProduct, columnsP, valMap, classifications);
			    			if(rowsP.length() == bs) {
			    				sendData("Product2G", requestProduct);
			    			}
			    			addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
			    			if(rowsA.length() == bs) {
			    				sendData("Product2G", requestProduct);
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
			    			addRow("Article", product.getId(), product.getParentId(), requestArticle, columnsA, valMap, classifications);
			    			if(rowsA.length() == bs) {
			    				sendData("Product2G", requestProduct);
			    				sendData("Article", requestArticle);
			    			}
	    				}else {
	    					log("SalesItem with no propper ParentID: " + product.getId());
	    				}
	    			}
	    		}
    		}
    	}
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, requestProduct, requestArticle);
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
			}
			m = attributeIDPattern.matcher(column);
			if(m.find()) {
				attributeId = m.group();
				data = characteristics.get(attributeId);
				if(data != null) {
					value = valMap.get(attributeId);
					if("DescriptionLong".equals(attributeId)) {
						descLong = value == null ? "" : value.getText();
					}
					rowValues.put("DescriptionLong".equals(attributeId) || value == null ? "" : "LOOKUP".equals(data[1]) ? new org.json.JSONObject().put("id", "'" + (value.getId() != null ? value.getId().replaceAll("'", "\\\\'") : value.getText().replaceAll("'", "\\\\'")) + "'@'" + data[0] + "'") : formatPlainValue( attributeId, value.getText(), data[1]) );
				}else {
					rowValues.put("");
				}
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
        		if(!rowsWithErrors.contains(i))
        			pw.println( objects.getJSONObject(i).getJSONObject("object").getString("id") );
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
			sendData( entity, new org.json.JSONObject().put("columns", columns).put("rows", new org.json.JSONArray().put(row)) );
		}
	}

	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(normalLogFilePath.toString(), true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
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
