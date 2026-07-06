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

public class LoadProductDataSecondOpinionFTW {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("../logs/loadProductDataPipeline.log");
	

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
    
    public static void processContent(String content) throws ParserConfigurationException, SAXException, IOException {
    	long init = System.currentTimeMillis();
    	LoadProductDataSecondOpinionFTW an = new LoadProductDataSecondOpinionFTW();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.loadOfInterest();
        an.qp.put("includeObjectsInProtocol", "false");
        an.procesaContent(content, parser);
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
		an.log("Ahora los que faltaron:");
		for(int a = 0; a<an.ofInterest.length; a++) {
			if(!an.losEncontrados.contains(an.ofInterest[a])) {
			}
		}
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	LoadProductDataSecondOpinionFTW an = new LoadProductDataSecondOpinionFTW();
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	an.normalLogFilePath = java.nio.file.Paths.get(
    											  ".."
    											, "logs"
    											, args.length == 1 ? "" : args[1]
    											, "list_api_load_from_step_second_opinion_ftw.log"
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
        an.loadOfInterest();
        an.qp.put("includeObjectsInProtocol", "false");
        an.procesaDirectorio(args[0], parser);
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
		an.log("Ahora los que faltaron:");
		for(int a = 0; a<an.ofInterest.length; a++) {
			if(!an.losEncontrados.contains(an.ofInterest[a])) {
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

    private int lacuenta = 0;
    private final org.json.JSONArray rows = new org.json.JSONArray();
    private final org.json.JSONArray columns = new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.SKU"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.EAN"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.Business"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2GLang.ProductName(es)"))
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
	    		.put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.PrevStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.ExternalStatus"))
    			.put(new org.json.JSONObject().put("identifier", "Article.SKU"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.EAN"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleLang.DescriptionShort(es)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.TamanoUnico(MX)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SupplierPartNumber(MX)"))
	    		.put(new org.json.JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)"))
    		;
    private final org.json.JSONArray rowsArticle = new org.json.JSONArray();
    private final org.json.JSONObject requestArticle = new org.json.JSONObject().put("columns", columnsArticle).put("rows", rowsArticle);
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
    
    private void processProduct(Product product) {
//    	if(java.util.Arrays.binarySearch(ofInterest, product.getId()) < 0){
//			return;
//		}else {
//			losEncontrados.add(product.getId());
//		}
    	if(product.getParentId().matches("^S?[0-9]+")) {
    		processChild(product, "", "", "");
    		return;
    	}
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	String business = null;
    	String calculatedWFAtt = null;
    	String firstDateApprove = null;
    	String fotoTomadaLiverpool = null;
    	String[] bundle = null;
    	String prevStatus = null;
    	String currentStatus = null;
    	String externalStatus = null;
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
    		org.json.JSONArray vals = new org.json.JSONArray();
    		Value calculatedWFAttVal = valMap.get("CalculatedWF_Att");
    		Value fotoTomadaLiverpoolVal = valMap.get("FotoTomadaLiverpool");
    		Value statusSKUVal = valMap.get("FirstDateApprove");
    		
    		calculatedWFAtt = calculatedWFAttVal == null ? "" : calculatedWFAttVal.getId() == null ? calculatedWFAttVal.getText() == null ? "" : calculatedWFAttVal.getText() : calculatedWFAttVal.getId();
    		fotoTomadaLiverpool = fotoTomadaLiverpoolVal == null ? "" : fotoTomadaLiverpoolVal.getId() == null ? fotoTomadaLiverpoolVal.getText() == null ? "" : fotoTomadaLiverpoolVal.getText() : fotoTomadaLiverpoolVal.getId();
    		firstDateApprove = statusSKUVal == null ? "" : statusSKUVal.getText() == null ? statusSKUVal.getId() == null ? "" : statusSKUVal.getId() : statusSKUVal.getText() ;
    		Value brandIdS4h = valMap.get("BRAND_ID_S4H");
    		Value brandname = valMap.get("BrandName");
    		Value direction = valMap.get("Direction");
    		Value extwgS4h = valMap.get("EXTWG_S4H");
    		Value itemgroup = valMap.get("ItemGroup");
    		Value itemgroups4h = valMap.get("ItemGroupS4H");
    		Value mainbarcode = valMap.get("MainBarCode");
    		Value mainbarcodes4h = valMap.get("MainBarCodeS4H");
    		Value negocio = valMap.get("Negocio");
    		Value productName = valMap.get("ProductName");
    		Value sapobjecttype = valMap.get("SAPObjectType");
    		Value sku = valMap.get("SKU");
    		Value section = valMap.get("Section");
    		Value supplierid = valMap.get("SupplierID");
    		Value supplierpartnumber = valMap.get("SupplierPartNumber");
    		String brandIdS4hStr = brandIdS4h == null ? "" : brandIdS4h.getId() != null ? brandIdS4h.getId() : brandIdS4h.getText() == null ? "" : brandIdS4h.getText() ;
    		String brandnameStr = brandname == null ? "" : brandname.getId() != null ? brandname.getId() : brandname.getText() == null ? "" : brandname.getText() ;
    		String directionStr = direction == null ? "" : direction.getId() != null ? direction.getId() : direction.getText() == null ? "" : direction.getText() ;
    		String itemgroupStr = itemgroup == null ? "" : itemgroup.getId() != null ? itemgroup.getId() : itemgroup.getText() == null ? "" : itemgroup.getText() ;
    		String itemgroups4hStr = itemgroups4h == null ? "" : itemgroups4h.getId() != null ? itemgroups4h.getId() : itemgroups4h.getText() == null ? "" : itemgroups4h.getText() ;
    		String mainbarcodeStr = mainbarcode == null ? "" : mainbarcode.getId() != null ? mainbarcode.getId() : mainbarcode.getText() == null ? "" : mainbarcode.getText() ;
    		String mainbarcodes4hStr = mainbarcodes4h == null ? "" : mainbarcodes4h.getId() != null ? mainbarcodes4h.getId() : mainbarcodes4h.getText() == null ? "" : mainbarcodes4h.getText() ;
    		String negocioStr = negocio == null ? "" : negocio.getId() != null ? negocio.getId() : negocio.getText() == null ? "" : negocio.getText() ;
    		String productnameStr = productName == null ? "" : productName.getText() == null ? "" : productName.getText() ;
    		String sapobjecttypeStr = sapobjecttype == null ? "" : sapobjecttype.getId() != null ? sapobjecttype.getId() : sapobjecttype.getText() == null ? "" : sapobjecttype.getText() ;
    		String skuStr = sku == null ? "" : sku.getId() != null ? sku.getId() : sku.getText() == null ? "" : sku.getText() ;
    		String sectionStr = section == null ? "" : section.getId() != null ? section.getId() : section.getText() == null ? "" : section.getText() ;
    		String supplieridStr = supplierid == null ? "" : supplierid.getId() != null ? supplierid.getId() : supplierid.getText() == null ? "" : supplierid.getText() ;
    		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
    		productnameStr = productnameStr.replaceAll("\s{2,}", " ").trim();
    		business = determineBusiness(negocio == null || negocio.getText() == null ? "" : negocio.getText(), extwgS4h == null || extwgS4h.getText() == null ? "" : extwgS4h.getText());
    		bundle = !"".equals(calculatedWFAtt) ? computeStatus(calculatedWFAtt, !"".equals(firstDateApprove) && firstDateApprove != null ? "Aprobado" : "", fotoTomadaLiverpool) : new String[] { null, null, null };
    		currentStatus = bundle[0];
    		prevStatus = bundle[1];
    		externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
    		log(product.getId() + " ---> " + calculatedWFAtt + ", " + firstDateApprove + ", " + bundle[0] + ", " + bundle[1]);
    		vals.put(currentStatus);
    		vals.put(prevStatus);
    		vals.put(externalStatus);
    		vals.put("00".equals(sapobjecttypeStr) ? "" : skuStr);
    		vals.put("00".equals(sapobjecttypeStr) ? "" : (mainbarcodeStr == null || "".equals(mainbarcodeStr) ? mainbarcodes4hStr : mainbarcodeStr));
    		vals.put(business);
    		vals.put(productnameStr.substring(0, Integer.min(productnameStr.length(), 250)));
    		if(productnameStr.length() > 250)
    			log("MB <:>" + productnameStr + "<:>");
    		vals.put(directionStr);
    		vals.put(sectionStr);
    		vals.put(itemgroupStr);
    		vals.put(itemgroups4hStr);
    		vals.put(brandnameStr);
    		vals.put(brandIdS4hStr);
    		vals.put(negocioStr);
    		vals.put(sapobjecttypeStr);
    		vals.put(supplieridStr);
    		vals.put(supplierpartnumberStr);
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + product.getId() + "'@1")).put("values", vals));
    		if(rows.length() == 1000) {
    			rw.writeData("list", "Product2G", null, qp, request, this::log);
    			while(rows.length() > 0) {
    				rows.remove(0);
    			}
    		}
        	java.nio.file.Path p = java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.migration.to_skip_directory"), product.getId() );
        	try {
        		java.nio.file.Files.createFile(p);
        	}catch(java.io.IOException ignore) {}
    	}
    	if(children != null && !children.isEmpty()) {
    		for(Product child : children) {
    			processChild(child, currentStatus, prevStatus, externalStatus);
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
    
    private void processChild(Product child, String currentStatus, String prevStatus, String externalStatus) {
    	
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
		String nameStr = name == null ? "" : name.getText() == null ? "" : name.getText() ;
		String sapobjecttypeStr = sapobjecttype == null ? "" : sapobjecttype.getId() != null ? sapobjecttype.getId() : sapobjecttype.getText() == null ? "" : sapobjecttype.getText() ;
		String skuStr = sku == null ? "" : sku.getId() != null ? sku.getId() : sku.getText() == null ? "" : sku.getText() ;
		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
		String tamanounicoStr = tamanounico == null ? "" : tamanounico.getId() != null ? tamanounico.getId() : tamanounico.getText() == null ? "" : tamanounico.getText() ;

		vals.put(currentStatus);
		vals.put(prevStatus);
		vals.put(externalStatus);
		vals.put(skuStr);
		vals.put("".equals( mainbarcodeStr ) ? mainbarcodes4hStr : mainbarcodeStr);
		vals.put(nameStr);
		vals.put(coloursLiverpoolAttStr);
		vals.put(tamanounicoStr);
		vals.put(supplierpartnumberStr);
		vals.put(sapobjecttypeStr);
		rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + child.getId() + "'@1")).put("values", vals));
		if(rowsArticle.length() == 5000) {
			rw.writeData("list", "Article", null, qp, requestArticle, this::log);
			while(rowsArticle.length() > 0) {
				rowsArticle.remove(0);
			}
		}
		lacuentaVars++;
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
