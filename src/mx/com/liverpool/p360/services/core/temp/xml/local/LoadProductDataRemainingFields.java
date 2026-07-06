package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.io.File;
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

public class LoadProductDataRemainingFields {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get(
			  ".."
			, "logs"
			, "list_api_load_from_step_second_opinion_remaining_data.log"
		);
	

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
    
    public static void processContent(String content) throws SAXException, IOException, ParserConfigurationException {
    	long init = System.currentTimeMillis();
    	LoadProductDataRemainingFields an = new LoadProductDataRemainingFields();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.qp.put("includeObjectsInProtocol", "false");
        an.collectActiveCharacteristics();
        an.procesaContenido(content, parser);
        an.processRemaining();
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	LoadProductDataRemainingFields an = new LoadProductDataRemainingFields();
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	an.normalLogFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_second_opinion_remaining_data__trón.log");
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
        an.qp.put("includeObjectsInProtocol", "false");
        an.collectActiveCharacteristics();
        an.procesaDirectorio(args[0], parser);
        an.processRemaining();
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private Integer procesaContenido(String content, SAXParser parser) throws SAXException, java.io.IOException {
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        java.util.List<Product> finished = null;
        Handler handler = new Handler();
        try {
        	parser.parse(new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)), handler);
        	finished = handler.getFinished();
	        refProductsCount += handler.getPrductsCounter();
	        for(Product p : finished) {
	        	processProduct(p);
	        }
        }catch(org.xml.sax.SAXParseException e) {
        	log("Problem processing content");
        }
        log("Parsing content took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
        return refProductsCount;
    }
    
    private Integer procesaDirectorio(String dir, SAXParser parser) throws SAXException, java.io.IOException {
    	if("oldSTEP".equals(dir) || dir.contains("oldSTEP")) {
    		return 0;
    	}
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
    
    private int lacuenta = 0;
    private int lacuentaVars = 0;
    
    private final java.util.Map<String, org.json.JSONObject> peticiones = new java.util.HashMap<>();
    private final java.util.Map<String, org.json.JSONObject> peticionesArt = new java.util.HashMap<>();
    private org.json.JSONObject peticion = null;
    
    private void processProduct(Product product) {
    	if(product.getParentId().matches("^(S?[0-9]+)")) {
    		processChild(product);
        	lacuenta++;
        	if(lacuenta % 10000 == 0) {
        		System.out.print(".");
        		if(lacuenta % 1000000 == 0) {
        			System.out.println("" + lacuenta);
        		}
        	}
    		return;
    	}
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	if(values != null) {
    		for(Value value : values) {
    			if(value.getId() != null || value.getUnidadId() != null || value.getText() != null) {
	    			if(java.util.Collections.binarySearch(PRODUCT_FIELDS, value.getAttributeId()) < 0) {
	    				if(java.util.Collections.binarySearch(ARTICLE_FIELDS, value.getAttributeId()) < 0) {
	    					if(java.util.Arrays.binarySearch(activeCharacteristics, value.getAttributeId()) > -1) {
	    						if(!addUnidadesDeMedida(value, product.getId())) {
	    							processValue(value.getAttributeId(), product.getId(), value.getId() != null ? value.getId() : value.getText(), peticiones, "Product2G");
	    						}
	    					}
	    				}
	    			}
    			}
    		}
    	}
    	if(children != null && !children.isEmpty()) {
    		for(Product child : children) {
    			processChild(child);
    	    	lacuenta++;
    	    	if(lacuenta % 10000 == 0) {
    	    		System.out.print(".");
    	    		if(lacuenta % 1000000 == 0) {
    	    			System.out.println("" + lacuenta);
    	    		}
    	    	}
    		}
    	}else {
    		processChild(product);
    	}
    	lacuenta++;
    	if(lacuenta % 10000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
    		}
    	}
    }
    
    private void processChild(Product child) {
    	java.util.List<Value> values = child.getValues();
		for(Value value : values) {
			if(value.getId() != null || value.getUnidadId() != null || value.getText() != null) {
    			if(java.util.Collections.binarySearch(PRODUCT_FIELDS, value.getAttributeId()) < 0) {
    				if(java.util.Collections.binarySearch(ARTICLE_FIELDS, value.getAttributeId()) < 0) {
    					if(java.util.Arrays.binarySearch(activeCharacteristicsArt, value.getAttributeId()) > -1) {
    						processValue(value.getAttributeId(), child.getId(), value.getId() != null ? value.getId() : value.getText(), peticionesArt, "Article");
    					}
    				}
    			}
			}
		}
		lacuentaVars++;
    }
	
    private static final java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
    private static final java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
    private static final java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();
    private static final String[] atributosLongitud = new String[] { "ProductWidth", "ProductDepth", "ProductHeight", "ZBRECJ", "ZLAECJ", "ZHOECJ", "ZHOEPQ", "ZBREPQ", "ZLAEPQ" };
    private static final String[] atributosVolumen = new String[] { "VOLUMAtt", "ZVOLCJ", "ZVOLPQ" };
    private static final String[] atributosPeso = new String[] { "PesoBruto", "ProductWeight", "ZBRGCJ", "ZNTGCJ", "ZBRGPQ", "ZNTGPQ" };
    
	private boolean addUnidadesDeMedida(Value value, String productId) {
		String unidadDeMedidaLongitud = null;
		String unidadDeMedidaVolumen = null;
		String unidadDeMedidaPeso = null;
		if( java.util.Arrays.binarySearch( atributosLongitud, value.getAttributeId() ) > -1) {
			unidadDeMedidaLongitud = unidadesLongitud.get(value.getUnidadId());
		}
		if( java.util.Arrays.binarySearch( atributosVolumen, value.getAttributeId() ) > -1) {
			unidadDeMedidaVolumen = unidadesVolumen.get( value.getUnidadId() );
		}
		if( java.util.Arrays.binarySearch( atributosPeso, value.getAttributeId() ) > -1) {
			unidadDeMedidaPeso = unidadesPeso.get( value.getUnidadId() );
		}
		if(unidadDeMedidaLongitud == null) {
		} else {
			processValue("UnidadDeMedidaLongitud", productId, unidadDeMedidaLongitud, peticiones, "Product2G");
			return true;
		}
		if(unidadDeMedidaPeso == null) {
		}else {
			processValue("UnidadDeMedidaPeso", productId, unidadDeMedidaPeso, peticiones, "Product2G");
			return true;
		}
		if(unidadDeMedidaVolumen == null) {
		}else {
			processValue("UnidadDeMedidaVolumen", productId, unidadDeMedidaVolumen, peticiones, "Product2G");
			return true;
		}
		return false;
	}
	
	private void processRemaining() {
		org.json.JSONArray rows = null;
		for(java.util.Map.Entry<String, org.json.JSONObject> entry : peticiones.entrySet()) {
			rows = entry.getValue() != null ? entry.getValue().getJSONArray("rows") : null;
			if(rows != null && rows.length() > 0) {
				rw.writeData("list", "Product2G", null, qp, entry.getValue(), this::log);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		for(java.util.Map.Entry<String, org.json.JSONObject> entry : peticionesArt.entrySet()) {
			rows = entry.getValue() != null ? entry.getValue().getJSONArray("rows") : null;
			if(rows != null && rows.length() > 0) {
				rw.writeData("list", "Article", null, qp, entry.getValue(), this::log);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
	}
	
	private void processValue(String attributeId, String productId, String value, java.util.Map<String, org.json.JSONObject> peticiones, String entity) {
		if(value == null) {
			return;
		}else if(value.length() > 2000) {
			log("Had att larger than black: " + attributeId + " -->" + value + "<--");
			System.exit(0);
		}
		peticion = peticiones.get(attributeId);
		org.json.JSONArray rows = null;
		if(peticion == null) {
			peticion = new org.json.JSONObject();
			rows = new org.json.JSONArray();
			org.json.JSONArray columns = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", entity + "CharacteristicValueLang.Value('" + attributeId + "',root,\"0000.0000.RK\",'" + attributeId + "',-1)"));
			peticion.put("rows", rows);
			peticion.put("columns", columns);
			peticiones.put(attributeId, peticion);
		}else {
			rows = peticion.getJSONArray("rows");
		}
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + productId + "'@1")).put("values", new org.json.JSONArray().put( value )));
		if(rows.length() == 50000) {
			rw.writeData("list", entity, null, qp, peticion, this::log);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
    
    private String[] activeCharacteristics = null;
    private String[] activeCharacteristicsArt = null;
    
    private void collectActiveCharacteristics() {
    	org.json.JSONObject response = null;
    	org.json.JSONArray rows = null;
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
    	qp.put("fields", "Characteristic.Identifier");
    	qp.put("query", "Characteristic.IsActive = true and Characteristic.ParentCharacteristic is empty and not Characteristic.Identifier wildcard \"%_Rechazo\" and Characteristic.Entities contains \"Product2G\"");
    	qp.put("pageSize", "10000");
    	int a = 0;
		int b = 0;
		int c = 0;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.getRw().makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				if(activeCharacteristics == null)
					activeCharacteristics = new String[b];
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					activeCharacteristics[c] = rows.getJSONObject(i).getJSONArray("values").getString(0);
					c++;
				}
				a += response.getInt("pageSize");
			} else {
				log("Not able to get characteristics. " + rw.getRw().getRawResponse());
			}
		}while(a < b);
		a = 0;
		java.util.Arrays.sort(activeCharacteristics);
		log("Got: " + activeCharacteristics.length + " active characteristics.");
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.IsActive = true and Characteristic.ParentCharacteristic is empty and not Characteristic.Identifier wildcard \"%_Rechazo\" and Characteristic.Entities contains \"Article\"");
		qp.put("pageSize", "10000");
		a = 0;
		b = 0;
		c = 0;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.getRw().makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				if(activeCharacteristicsArt == null)
					activeCharacteristicsArt = new String[b];
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					activeCharacteristicsArt[c] = rows.getJSONObject(i).getJSONArray("values").getString(0);
					c++;
				}
				a += response.getInt("pageSize");
			} else {
				log("Not able to get characteristics. " + rw.getRw().getRawResponse());
			}
		}while(a < b);
		a = 0;
		java.util.Arrays.sort(activeCharacteristicsArt);
		log("Got: " + activeCharacteristicsArt.length + " active characteristics article.");
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

	
	private static final java.util.List<String> ARTICLE_FIELDS = java.util.Arrays.asList(new String[] {
			 "BRAND_ID_S4H"
			,"BrandName"
			,"ColoursLiverpoolAtt"
			,"CUBISCAN"
			,"CostoEnMonedaExtranjera"
			,"CostoNetoSinIVA"
			,"CostobrutoSinIVA"
			,"CountryOfOrigin"
			,"Currency"
			,"Descuento1"
			,"Descuento2"
			,"EnvioMirakl"
			,"ErroresDeSistema"
			,"EstatusPropuesta"
			,"FechaInicioVigenciaCostoImportacion"
			,"FechaInicioVigenciaCostoNeto"
			,"FechaInicioVigenciaPrecioVenta"
			,"Footnote"
			,"FotoTomadaLiverpool"
			,"IDLastParent"
			,"IDTallaERP"
			,"IdentificaNegocio"
			,"IndicadordeImpuesto"
			,"ItemGroup"
			,"ItemGroupS4H"
			,"MainBarCode"
			,"MainBarCodeS4H"
			,"MensajeCreacionSKU"
			,"NUMTP_S4H"
			,"Name"
			,"NameGuide"
			,"Parent"
			,"ParentID"
			,"PrecioSugeridocIVA"
			,"ProductType"
			,"ProductTypeSAP"
			,"ProductTypeSAP2"
			,"ProductTypeSAPTEMP"
			,"PublicarEnATG"
			,"SAPObjectType"
			,"SAPSpart"
			,"SAP_BEHVO"
			,"SB_0002"
			,"SB_COLORES"
			,"SKU"
			,"SKUCreationDate"
			,"SistemaOrigen"
			,"SkuType"
			,"StateSKU"
			,"SupplierID"
			,"SupplierPartNumber"
			,"TamanoUnico"
			,"TamanoUnicoT"
			,"TerminosYCondicionesCR"
			,"TipoDeEtiqueta"
			,"TypeMainBarCode"
			,"WF_Date_created_rejected"
			,"isMarketPlace"
	});
	
	private static final java.util.List<String> PRODUCT_FIELDS = java.util.Arrays.asList(new String[] {
			 "AR"
			,"ARURL"
			,"AnoEstacion"
			,"ApprovedDateCalc"
			,"ArgumentoDeVenta"
			,"AssetRejectionMessage"
			,"BRAND_ID_S4H"
			,"BWSCL"
			,"BWVOR"
			,"BlueCategorizeRejectionMessage"
			,"BrandIDATG"
			,"BrandName"
			,"BrandNameATG"
			,"BrandOwner"
			,"Business"
			,"BuyerRejectionMessage"
			,"CSTFLD03"
			,"CUBISCAN"
			,"CalculatedWF_Att"
			,"Calculated_inStateWF"
			,"CategoryManagerRejectionMessage"
			,"CertificadoSostenible"
			,"Coleccion"
			,"CollectionDescriptionSAP"
			,"ComentarioProyectosComerciales"
			,"Consignacion"
			,"CoordinadoIDSAP"
			,"CostoEnMonedaExtranjera"
			,"CostoNetoSinIVA"
			,"CostobrutoSinIVA"
			,"CountryOfOrigin"
			,"Currency"
			,"DeleteCheck"
			,"DescriptionLong"
			,"DescriptionLong2"
			,"DescriptionTable"
			,"DescriptionWeb"
			,"Descuento1"
			,"Descuento2"
			,"Direction"
			,"ENV_ATG"
			,"EXTWG_S4H"
			,"EmbedCodeWAP"
			,"EmbedCodeWEB"
			,"EnrichmentRejectionMessage"
			,"EnvioImagen"
			,"EnvioInternacional"
			,"EnvioMirakl"
			,"ErroresDeSistema"
			,"EsSostenible"
			,"EsSostenibleVAD"
			,"EstatusPropuesta"
			,"ExclusivePackage"
			,"ExclusivePackageStartDate"
			,"Evento"
			,"Exclusive"
			,"ExclusiveEndDate"
			,"ExclusivePromotionEndDate"
			,"ExclusiveStartDate"
			,"ExclusivePromotionStartDate"
			,"ExclusivePromotion"
			,"ExclusivePackageEndDate"
			,"ExpressDelivery"
			,"ExpressDeliveryEndDate"
			,"ExpressDeliveryStartDate"
			,"FIBER_CODE1"
			,"FIBER_CODE2"
			,"FIBER_CODE3"
			,"FIBER_CODE4"
			,"FIBER_CODE5"
			,"FIBER_CODE_DESCR1"
			,"FIBER_CODE_DESCR2"
			,"FIBER_CODE_DESCR3"
			,"FIBER_CODE_DESCR4"
			,"FIBER_CODE_DESCR5"
			,"FIBER_PART1"
			,"FIBER_PART2"
			,"FIBER_PART3"
			,"FIBER_PART4"
			,"FIBER_PART5"
			,"FSH_ID"
			,"FSH_COLLECTION"
			,"FSH_SEASON"
			,"FSH_SEASONS"
			,"FSH_SEASON_YEAR"
			,"FSH_THEME"
			,"FamilyDescription"
			,"FeatureBullet1"
			,"FeatureBullet2"
			,"FechaInicioVigenciaCostoImportacion"
			,"FechaInicioVigenciaCostoNeto"
			,"FechaInicioVigenciaPrecioVenta"
			,"FechaVencimiento"
			,"FlagImportBySS"
			,"Footnote"
			,"FotoTomadaLiverpool"
			,"GROES"
			,"GiftWithPurchase"
			,"GiftWithPurchaseEndDate"
			,"GiftWithPurchaseStartDate"
			,"GradoDemoda"
			,"IDLastParent"
			,"IDTallaERP"
			,"IEPS"
			,"IdentificaNegocio"
			,"ImpuestoALaVenta"
			,"IncidenciasCategorizacion"
			,"IncidenciasCompras"
			,"IndicadordeImpuesto"
			,"ItemGroup"
			,"ItemGroup2"
			,"ItemGroupS4H"
			,"KLASE_CTE"
			,"LABOR"
			,"LABOR_S4H"
			,"LatestParts"
			,"LatestPartsEndDate"
			,"LatestPartsStartDate"
			,"LicenseDescription"
			,"MAX_STACK"
			,"MTART_S4H"
			,"MVGR2"
			,"MVGR5"
			,"MainBarCode"
			,"MainBarCodeS4H"
			,"Margen"
			,"MargenS4H"
			,"MensajeCreacionSKU"
			,"MesdeEntregadeMercancIa"
			,"MiraklSalesItemFamilyID"
			,"NORMT"
			,"NUMTP_S4H"
			,"Name"
			,"NameExceptions"
			,"NameGuide"
			,"Negocio"
			,"NumberOfDetailImages"
			,"NumberOfIllustrationImages"
			,"NumberOfLiverpoolManuals"
			,"NumberOfNoms"
			,"NumberOfSmoshImages"
			,"OrigenSAP_STEP"
			,"PLGTP"
			,"Parent"
			,"ParentID"
			,"PerfilDeRedondeo"
			,"PreSale"
			,"PreSaleDateStart"
			,"PrecioSugeridocIVA"
			,"PreSaleDateEnd"
			,"ProceedBuyerToQA"
			,"ProductName"
			,"ProductType"
			,"ProductTypeSAP"
			,"ProductTypeSAP2"
			,"ProductTypeSAPTEMP"
			,"ProductTypeSAPTEMPSBB"
			,"Producto"
			,"PublicarEnATG"
			,"QARejectionMessage"
			,"RegionTEMP"
			,"SAPFeedErrors"
			,"SAPObjectType"
			,"SAPSpart"
			,"SAP_BEHVO"
			,"SAP_ZZCOMA"
			,"SB_0002"
			,"SB_COLORES"
			,"SB_T_HARDLINE"
			,"SERVV"
			,"SKU"
			,"SKUCreationDate"
			,"SalesItemShortDescription"
			,"Section"
			,"ShipPleasant"
			,"ShipPleasantStartDate"
			,"SistemaOrigen"
			,"SkuType"
			,"SortPrice"
			,"StateSKU"
			,"StatusOutWF"
			,"SupplierComplete"
			,"SupplierID"
			,"SupplierName"
			,"SupplierPartNumber"
			,"SupplierRejectionMessage"
			,"TAXESS4H"
			,"TAXESSAP"
			,"TAXKM1_S4H"
			,"TAXKM2_S4H"
			,"TAXM3_S4H"
			,"TIPOOPERACION"
			,"TImportacion"
			,"TamanoUnico"
			,"TamanoUnicoT"
			,"Temporada"
			,"TerminosYCondicionesCR"
			,"TextoAdicional"
			,"TipoDeEtiqueta"
			,"TipoDeTomaForo"
			,"TypeMainBarCode"
			,"VOLUMAttCalculado"
			,"Video"
			,"ViewIncidenciasCategorizacion"
			,"VolumetryCheck"
			,"WESCH"
			,"WF_Date_created_rejected"
			,"WHERL"
			,"WebCategorizeRejectionMessage"
			,"ZGEWCJ"
			,"ZNUMV"
			,"ZOMSUDDateStart"
			,"ZNUMVMKP"
			,"ZOMSUDDateEnd"
			,"ZOMSUD"
			,"ZZFEEM"
			,"ZVOLCJcalculado"
			,"ZZLIC_S4H"
			,"deliveryMethod"
			,"esSuburbia"
			,"exclusiveDiscount"
			,"exclusiveDiscountStartDate"
			,"exclusiveDiscountEndDate"
			,"isDuttyFree"
			,"isMarketPlace"
			,"mirakl-Image1"
			,"mirakl-Image10"
			,"mirakl-Image11"
			,"mirakl-Image12"
			,"mirakl-Image13"
			,"mirakl-Image14"
			,"mirakl-Image15"
			,"mirakl-Image2"
			,"mirakl-Image3"
			,"mirakl-Image4"
			,"mirakl-Image5"
			,"mirakl-Image6"
			,"mirakl-Image7"
			,"mirakl-Image8"
			,"mirakl-Image9"
			,"mirakl-acceptance-status"
			,"mirakl-integration-code"
			,"mirakl-integration-message"
			,"mirakl-product-id"
			,"mirakl-rejection-message"
			,"mirakl-rejection-reason"
			,"mirakl-validation-status"
			,"mirakl-variant-group-id"
			,"refundPolicy"
			,"specification"
			,"supplierShopId"
			,"DutyFreeKey"
	});
	
	static {
		java.util.Collections.sort(PRODUCT_FIELDS);
		java.util.Collections.sort(ARTICLE_FIELDS);

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
	}
}
