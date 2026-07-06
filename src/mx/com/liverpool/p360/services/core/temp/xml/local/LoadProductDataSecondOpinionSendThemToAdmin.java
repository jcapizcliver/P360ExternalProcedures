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

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class LoadProductDataSecondOpinionSendThemToAdmin {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("../logs/loadProductDataSendToAdmin.log");
	

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
    
    private class AssetCrossReference{
    	
    	private String id = null;
    	private String type = null;
    	
    	public AssetCrossReference(String id, String type) {
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
    	private java.util.LinkedList<AssetCrossReference> assetCrossReferences = new java.util.LinkedList<>();
    	private java.util.LinkedList<Classification> classifications = new java.util.LinkedList<>();
    	private Classification workingClassification = null;
    	private AssetCrossReference workingAssetCrossReference = null;
    	
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
		
		public java.util.LinkedList<AssetCrossReference> getAssetCrossReferences(){
			return assetCrossReferences;
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
		
		public AssetCrossReference getAssetCrossReference() {
			return this.workingAssetCrossReference;
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
		
		public void prepareAssetCrossReference(AssetCrossReference assetCrossReference) {
			if(this.workingAssetCrossReference != null) {
				addAssetCrossReference();
			}
			this.workingAssetCrossReference = assetCrossReference;
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
		
		public void addAssetCrossReference() {
			if(workingAssetCrossReference != null) {
				this.assetCrossReferences.addLast(this.workingAssetCrossReference);
				this.workingAssetCrossReference = null;
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
	                } else if("AssetCrossReference".equals(name)) {
	                	String assetId = attributes.getValue("AssetID");
	                	String type = attributes.getValue("Type");
	                	AssetCrossReference assetCrossReference = new AssetCrossReference(assetId, type);
	                	product.prepareAssetCrossReference(assetCrossReference);
	                }  else if("Name".equals(name)) {
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
                }else if("AssetCrossReference".equals(name)) {
                	product.addAssetCrossReference();
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
    	LoadProductDataSecondOpinionSendThemToAdmin an = new LoadProductDataSecondOpinionSendThemToAdmin();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.procesaContent(content, parser);
        if(an.itemsP.length() > 0) {
        	an.log("From sending product data: " + an.dr.putProductData(an.itemsP) );
        	while(an.itemsP.length() > 0) {
        		an.itemsP.remove(0);
        	}
        }
        if(an.itemsV.length() > 0) {
        	an.log("From sending items data: " + an.dr.putArticleData(an.itemsV) );
        	while(an.itemsV.length() > 0) {
        		an.itemsV.remove(0);
        	}
        }
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	LoadProductDataSecondOpinionSendThemToAdmin an = new LoadProductDataSecondOpinionSendThemToAdmin();
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	an.normalLogFilePath = java.nio.file.Paths.get(
    											  ".."
    											, "logs"
    											, args.length == 1 ? "" : args[1]
    											, "loadProductDataSendToAdmin.log"
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
        an.procesaDirectorio(args[0], parser);
        if(an.itemsP.length() > 0) {
        	an.log( an.dr.putProductData(an.itemsP) );
        	while(an.itemsP.length() > 0) {
        		an.itemsP.remove(0);
        	}
        }
        if(an.itemsV.length() > 0) {
        	an.log( an.dr.putArticleData(an.itemsV) );
        	while(an.itemsV.length() > 0) {
        		an.itemsV.remove(0);
        	}
        }
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private String[] ofInterest;
    
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
	        	processProduct(p, handler.getAssetMap());
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
			        	processProduct(p, handler.getAssetMap());
			        }
		        }catch(org.xml.sax.SAXParseException e) {
		        	log("Problem processing following file: " + input.getAbsolutePath());
		        }
        	}
        }
        log("Parsing files took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
        return refProductsCount;
    }

    private org.json.JSONArray itemsP = new org.json.JSONArray();
    private org.json.JSONArray itemsV = new org.json.JSONArray();
    private DataRequestor dr = new DataRequestor();
    private int lacuenta = 0;
    private int lacuentaVars = 0;
    private final java.util.Set<String> losEncontrados = new java.util.TreeSet<>();

    private final java.util.Map<String, String> internalToExternalStatusMap = loadExternalStatusMap();

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
    
    private void processProduct(Product product, java.util.Map<String, Asset> assetMap) {
    	if(product.getParentId().matches("^(S?[0-9]+)")) {
    		processChild(product, "", "", "", assetMap);
    		return;
    	}
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	String business = null;
    	String calculatedWFAtt = null;
    	String firstDateApprove = null;
    	String[] bundle = null;
    	String prevStatus = null;
    	String currentStatus = null;
    	String externalStatus = null;
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
    		Value calculatedWFAttVal = valMap.get("CalculatedWF_Att");
    		Value statusSKUVal = valMap.get("FirstDateApprove");
    		
    		calculatedWFAtt = calculatedWFAttVal == null ? "" : calculatedWFAttVal.getId() == null ? calculatedWFAttVal.getText() == null ? "" : calculatedWFAttVal.getText() : calculatedWFAttVal.getId();
    		firstDateApprove = statusSKUVal == null ? "" : statusSKUVal.getText() == null ? statusSKUVal.getId() == null ? "" : statusSKUVal.getId() : statusSKUVal.getText() ;
    		String section = dataFromMap( valMap.get("Section") );
    		String itemGroup = dataFromMap( valMap.get("ItemGroup") );
    		String itemGroupS4H = dataFromMap( valMap.get("ItemGroupS4H") );
    		String itemGroup2 = dataFromMap( valMap.get("ItemGroup2") );
    		String brandName = dataFromMap( valMap.get("BrandName") );
    		String brandIdS4H = dataFromMap( valMap.get("BRAND_ID_S4H") );
    		String sku = dataFromMap( valMap.get("SKU") );
    		String supplierID = dataFromMap( valMap.get("SupplierID") );
    		String assignTakeNoTake = dataFromMap( valMap.get("AssignTakeNoTake") );
    		String sapObjectType = dataFromMap( valMap.get("SAPObjectType") );
    		String fotoTomadaLiverpool = dataFromMap( valMap.get("FotoTomadaLiverpool") );
    		String mainBarCode = dataFromMap( valMap.get("MainBarCode") );
    		String mainBarCodeS4H = dataFromMap( valMap.get("MainBarCodeS4H") );
    		String supplierPartNumber = dataFromMap( valMap.get("SupplierPartNomber") );
    		/*
    		.put("product", items.getString(i))
			.put("Section", values[0])
			.put("ItemGroup", values[1])
			.put("ItemGroupS4H", values[2])
			.put("BrandName", values[3])
			.put("BRAND_ID_S4H", values[4])
			.put("Business", values[5])
			.put("SKU", values[6])
			.put("SupplierID", values[7])
			.put("Template", values[8])
			.put("CurrentStatus", values[9])
			.put("AssignTakeNoTake", values[10])
			.put("SAPObjectType", values.length > 11 ? values[11] : "")
			.put("FotoTomadaLiverpool", values.length > 12 ? values[12] : "")
			.put("MainBarCode", values.length > 13 ? values[13] : "")
			.put("MainBarCodeS4H", values.length > 14 ? values[14] : "")
			.put("SupplierPartNumber", values.length > 15 ? values[15] : "") 
    		  */
    		Value extwgS4h = valMap.get("EXTWG_S4H");
    		Value negocio = valMap.get("Negocio");
    		business = determineBusiness(negocio == null || negocio.getText() == null ? "" : negocio.getText(), extwgS4h == null || extwgS4h.getText() == null ? "" : extwgS4h.getText());
    		bundle = !"".equals(calculatedWFAtt) ? computeStatus(calculatedWFAtt, !"".equals(firstDateApprove) && firstDateApprove != null ? "Aprobado" : "", fotoTomadaLiverpool) : new String[] { null, null, null };
    		currentStatus = bundle[0];
    		prevStatus = bundle[1];
    		externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
    		org.json.JSONObject jo = new org.json.JSONObject();
    		jo
	    		.put("product", product.getId())
				.put("Section", section)
				.put("ItemGroup", children.isEmpty() || "SalesItem".equals(product.getUserTypeId()) ? "" :  ("".equals(itemGroup) ? itemGroup2 : itemGroup ))
				.put("ItemGroupS4H", children.isEmpty() || "SalesItem".equals(product.getUserTypeId()) ? "" : itemGroupS4H)
				.put("BrandName", brandName)
				.put("BRAND_ID_S4H", brandIdS4H)
				.put("Business", business)
				.put("SKU", children.isEmpty() || "SalesItem".equals(product.getUserTypeId()) ? "" : sku)
				.put("SupplierID", supplierID)
				.put("Template", product.getParentId())
				.put("CurrentStatus", currentStatus)
				.put("AssignTakeNoTake", assignTakeNoTake)
				.put("SAPObjectType", sapObjectType)
				.put("FotoTomadaLiverpool", fotoTomadaLiverpool)
				.put("MainBarCode", "".equals(mainBarCode) ? mainBarCodeS4H : mainBarCode)
				.put("MainBarCodeS4H", mainBarCodeS4H)
				.put("SupplierPartNumber", supplierPartNumber)
			;
    		itemsP.put(jo);
    		if(itemsP.length() == 5000) {
    			log("From sending products data: " +  dr.putProductData(itemsP) );
    			while(itemsP.length() > 0) {
    				itemsP.remove(0);
    			}
    		}
    	}
    	if(children != null && !children.isEmpty()) {
    		for(Product child : children) {
    			processChild(child, currentStatus, prevStatus, externalStatus, assetMap);
    		}
    	}else {
    		processChild(product, currentStatus, prevStatus, externalStatus, assetMap);
    	}
    	lacuenta++;
    	if(lacuenta % 10000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
    		}
    	}
    }
    
    private String dataFromMap(Value v) {
    	return v == null ? "" : v.getId() != null ? v.getId() : v.getText() == null ? "" : v.getText() ;
    }
    
    private String dataFromMapV(Value v) {
    	return v == null ? "" : v.getText() != null ? v.getText() : v.getId() == null ? "" : v.getId() ;
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
	
    public String[] computeStatus(String calculatedWFAtt, String stateSKU, String fotoTomadaLiverpool) {
		String[] res = new String[3];
		res[0] = null;
		res[1] = null;
		res[2] = "false";
		String flujoActual = getFlujoActual(calculatedWFAtt);
		String estado = getEstadoEnElWF(calculatedWFAtt);
		if("N/A".equals(flujoActual) && "N/A".equals(estado) && "".equals(stateSKU) && "".equals(fotoTomadaLiverpool)) {
			res[0] = "1002"; // Pendiente Inicio Enriquecimiento
			res[1] = "1020";
		} else if("N/A".equals(flujoActual) && "N/A".equals(estado) && !"Aprobado".equals(stateSKU) && "Y".equals(fotoTomadaLiverpool)) {
			res[0] = "1002"; // Pendiente Inicio Enriquecimiento
			res[1] = "1020";
		} else if("N/A".equals(flujoActual) && "N/A".equals(estado) && !"Aprobado".equals(stateSKU) && "N".equals(fotoTomadaLiverpool)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("N/A".equals(flujoActual) && "N/A".equals(estado) && "Aprobado".equals(stateSKU)) {
			res[0] = "1007";
			res[1] = "1023";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "BuyerReview".equals(estado)) {
			res[0] = "1003";  // Revisión Compras
			res[1] = "10031"; // Borrador
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierReviewChange".equals(estado)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierModification".equals(estado) && "Aprobado".equals(stateSKU)) {
			res[0] = "1007"; // Aprobado
			res[1] = "1023"; // Category
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierModification".equals(estado) && !"Aprobado".equals(stateSKU)) {
			res[0] = "1004"; // Carga de Imágen
			res[1] = "1020"; // Creación de SKU
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && ("DataGovermentInitiate".equals(estado) || "ErrorRevision".equals(estado) || "DigitalAssetsReview".equals(estado))) {
			res[0] = "1021"; // Gobierno de Datos 
			res[1] = "1020";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "QAReview".equals(estado)) {
			res[0] = "1022";
			res[1] = "1020";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "CategoryManager".equals(estado)) {
			res[0] = "1023";
			res[1] = "1022";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Categorizacion".equals(estado)) {
			res[0] = "1021";
			res[1] = "1026"; // En proceso foro
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Aseguramiento_de_Calidad".equals(estado)) {
			res[0] = "1022";
			res[1] = "1026";
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Category_Manager".equals(estado)) {
			res[0] = "1023";
			res[1] = "1022";
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Rechazos".equals(estado)) {
			res[0] = "1005";
			res[1] = "1022";
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Revision_Categorizacion".equals(estado)) {
			res[0] = "1026";
			res[1] = "1002";
		} else if("SupplierCreationWF".equals(flujoActual) && "BuyerReview".equals(estado)) {
			res[0] = "1003";
			res[1] = "1001";
		} else if("SupplierCreationWF".equals(flujoActual) && "AssetReviewAndUpload".equals(estado)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && "SupplierRevision".equals(estado)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && ("DigitalAssetsReview".equals(estado) || "ErrorRevision".equals(estado))) {
			res[0] = "1021";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && "QAReview".equals(estado)) {
			res[0] = "1022";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && "CategoryManager".equals(estado)) {
			res[0] = "1023";
			res[1] = "1022";
		}else {
//			log("No rule found. (" + productId + ")");
//			log(stateSKU + " - " + fotoTomadaLiverpool + " - " + calculatedWFAtt + " (" + productId + ")");
		}
		return res;
	}
    
    private void processChild(Product child, String currentStatus, String prevStatus, String externalStatus, java.util.Map<String, Asset> assetMap) {
    	java.util.List<Value> values = child.getValues();
    	java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
		for(Value value : values) {
			valMap.put(value.getAttributeId(), value);
		}
		org.json.JSONObject jo = new org.json.JSONObject();
		/*
		    .put("variant", items.getString(i))
			.put("ProductNo", parent == null ? "" : parent )
			.put("ColoursLiverpoolAtt", values[0])
			.put("TamanoUnico", values[1])
			.put("ProductImage", values[2])
			.put("AssignTakeNoTake", values.length > 3 ? values[3] : "")
			.put("SKU", values.length > 4 ? values[4] : "")
			.put("MainBarCode", values.length > 5 ? values[5] : "")
			.put("MainBarCodeS4H", values.length > 6 ? values[6] : "")
			.put("SupplierPartNumber", values.length > 7 ? values[7] : "")
		 */
		Value coloursLiverpoolAtt = valMap.get("ColoursLiverpoolAtt");
		Value mainbarcode = valMap.get("MainBarCode");
		Value mainbarcodes4h = valMap.get("MainBarCodeS4H");
		Value sku = valMap.get("SKU");
		Value supplierpartnumber = valMap.get("SupplierPartNumber");
		Value tamanounico = valMap.get("TamanoUnico");
		
		String coloursLiverpoolAttStr = coloursLiverpoolAtt == null ? "" : coloursLiverpoolAtt.getId() != null ? coloursLiverpoolAtt.getId() : coloursLiverpoolAtt.getText() == null ? "" : coloursLiverpoolAtt.getText() ;
		String mainbarcodeStr = mainbarcode == null ? "" : mainbarcode.getId() != null ? mainbarcode.getId() : mainbarcode.getText() == null ? "" : mainbarcode.getText() ;
		String mainbarcodes4hStr = mainbarcodes4h == null ? "" : mainbarcodes4h.getId() != null ? mainbarcodes4h.getId() : mainbarcodes4h.getText() == null ? "" : mainbarcodes4h.getText() ;
		String skuStr = sku == null ? "" : sku.getId() != null ? sku.getId() : sku.getText() == null ? "" : sku.getText() ;
		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
		String tamanounicoStr = tamanounico == null ? "" : tamanounico.getId() != null ? tamanounico.getId() : tamanounico.getText() == null ? "" : tamanounico.getText() ;
		String productImageUrl = "";
		java.util.List<AssetCrossReference> assetCrossReferences = child.getAssetCrossReferences();
		if(assetCrossReferences != null) {
			for(AssetCrossReference acr : assetCrossReferences) {
				if("PrimaryProductImage".equals(acr.type)){
					Asset a = assetMap.get(acr.getId());
					java.util.LinkedList<Value> avs = a.getValues();
					for(Value av : avs) {
						if( "ImageURL".equals( av.attributeId )) {
							productImageUrl = getImageURL(av.getText());
						}
					}
				}
			}
		}
		jo
			.put("variant", child.getId())
			.put("ProductNo", !child.getParentId().matches("^(S?[0-9]+)") ? child.getId() : child.getParentId() )
			.put("ColoursLiverpoolAtt", coloursLiverpoolAttStr)
			.put("TamanoUnico", tamanounicoStr)
			.put("ProductImage", productImageUrl)
			.put("AssignTakeNoTake",  "Take".equals( dataFromMap( valMap.get("AssignTakeNoTake") ) ) ? "TOMAR" : "NO TOMAR")
			.put("SKU", skuStr)
			.put("MainBarCode", mainbarcodeStr)
			.put("MainBarCodeS4H", mainbarcodes4hStr)
			.put("SupplierPartNumber", supplierpartnumberStr)
		;
		itemsV.put(jo);
		if(itemsV.length() == 5000) {
			log("From sending items data: " +  dr.putArticleData(itemsV) );
			while(itemsV.length() > 0) {
				itemsV.remove(0);
			}
		}
		lacuentaVars++;
    }
	
	private String getImageURL(String url) {
		if(url != null) {
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\=([^,\\=]+).+").matcher(url.trim());
			if(m.find()) {
				return m.group(1);
			}
		}
		return url;
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
