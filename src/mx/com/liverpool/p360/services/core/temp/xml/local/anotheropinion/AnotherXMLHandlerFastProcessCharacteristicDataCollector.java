package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

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
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.xml.local.precise.LoadProductDataRemainingFieldsOnSpecificProducts;

public class AnotherXMLHandlerFastProcessCharacteristicDataCollector {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "a_brand_new_xml_step_files_processor.log");
	

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
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandlerFastProcessCharacteristicDataCollector an = new AnotherXMLHandlerFastProcessCharacteristicDataCollector();
    	an.qp.put("includeObjectsInProtocol", "false");
    	if(args.length < 1) {
    		System.out.println("Need to pass as first argument a directory that contains xml files to send.");
    	}else {
    		an.loadItemGroups();
//    		LoadProductDataRemainingFieldsOnSpecificProducts elp = new LoadProductDataRemainingFieldsOnSpecificProducts();
//    		Thread t = new Thread(new Runnable(){
//    			@Override 
//    			public void run() {
//    				while(an.running) {
//    					try {
//    						Thread.sleep(1200000);
//    					} catch (InterruptedException e) {
//    						e.printStackTrace();
//    					}
//						an.eleseProgress();
//					}
//    				System.out.println("Exiting.");
//    			}
//    		});
//    		t.setDaemon(true);
//    		t.start();
			an.runOverDirectories(args[0]);
	        an.log("Total products found: " + an.lacuenta);
	        an.log("Total vars found: " + an.lacuentaVars);
	        an.log("Total lossmissings: " + an.perdidas);
    	}
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
        an.running = false;
		an.eleseFinish();
    }
    
    private boolean running = true;
    
    private void runOverDirectories(String startDirectory) throws ParserConfigurationException, SAXException, IOException {
    	log("Running over... " + startDirectory);
    	java.io.File[] files = new java.io.File(startDirectory).listFiles();
    	for(java.io.File f : files) {
    		if(f.isDirectory()) {
    			runOverDirectories(f.getAbsolutePath());
    		}else {
    			log("\tNow processing " + f.getAbsolutePath());
		    	procesaArchivoYProducto(f.getAbsolutePath());
    		}
    	}
    }
    
    private void eleseFinish() {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		//https://chat.googleapis.com/v1/spaces/AAAAZpaMbww/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=u-P1Me5vwfb04AoqTpZI0QCmPNR4fELWlqPgmupabSY
		rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
		qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
		qp.put("token", "u-P1Me5vwfb04AoqTpZI0QCmPNR4fELWlqPgmupabSY");
		log( "" + rw.getRw().makeRequest("POST", "/AAAAZpaMbww/messages", qp, 
				new org.json.JSONObject().put("text", 
						"Finalizado. Productos procesados: " + lacuenta + " gen/ind (" + lacuentaVars + " vars, " + perdidas + " variantes sueltas) 😁.").toString()) );
    }
    
    private void eleseProgress() {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
		qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
		qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
		log( "" + rw.getRw().makeRequest("POST", "/AAAAvwSYdXo/messages", qp, 
				new org.json.JSONObject().put("text", 
						"Productos procesados: " + lacuenta + " gen/ind (" + lacuentaVars + " variantes) 😁.").toString()) );
    }
    
    private void procesaArchivoYProducto(String file) throws ParserConfigurationException, SAXException {
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
	        for(Product p : finished) {
	        	processProduct(p);
	        }
        }catch(org.xml.sax.SAXParseException e) {
        	log("Problem processing following file: " + file);
        } catch (IOException e) {
			e.printStackTrace();
		}
        log("Lacuenta: " + lacuenta);
    }
    
    private int lacuenta = 0;
    private final org.json.JSONArray rows = new org.json.JSONArray();
    private final org.json.JSONArray columns = new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus"))
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

				.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnriquecidoEnForo',root,\"0000.0000.RK\",'EnriquecidoEnForo',-1)"))
			;
    private final org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows", rows);
    private final org.json.JSONArray columnsArticle = new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.PrevStatus"))
	    		.put(new org.json.JSONObject().put("identifier", "Article.ExternalStatus"))
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
    private int perdidas = 0;
    private final java.util.Map<String, String> internalToExternalStatusMap = loadExternalStatusMap();
    private final java.util.Map<String, String> externalStatusLabels = loadExternalStatusLabelsMap();
    private final PruebaEnvioPubSubMediaAssets elese = new PruebaEnvioPubSubMediaAssets();
    private final java.util.Set<String> missingParents = new java.util.TreeSet<>();
    
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
    	if(product.getParentId().matches("^(S?[0-9]+)$")) {
    		perdidas++;
    		missingParents.add(product.getParentId());
    		return;
    	}
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	String business = null;
    	String currentStatus = null;
    	String prevStatus = null;
    	String externalStatus = null;
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
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
    		Value calculatedWFAtt = valMap.get("CalculatedWF_Att");
    		Value fotoTomadaLiverpool = valMap.get("FotoTomadaLiverpool");
    		Value stateSKU = valMap.get("StateSKU");
    		String firstdateapproveStr = firstDateApprove == null ? "" : firstDateApprove.getId() != null ? firstDateApprove.getId() : firstDateApprove.getText() == null ? "" : firstDateApprove.getText() ;
    		String descriptionLongStr = descriptionLong == null ? "" : descriptionLong.getText() ;
    		String descriptionLong2Str = descriptionLong2 == null ? "" : descriptionLong2.getText() ;
    		String productNameStr = productName == null ? "" : productName.getText() ;
    		business = determineBusiness(negocio == null || negocio.getText() == null ? "" : negocio.getText(), extwgS4h == null || extwgS4h.getText() == null ? "" : extwgS4h.getText());
    		String nameStr = product.getName();
    		String wdspr = WildDateStandardizer.normalize(firstdateapproveStr, java.time.ZoneId.of("America/Mexico_City"), WildDateStandardizer.AmbiguityPolicy.PREFER_DMY).orElse("");
    		String[] bundle = elese.computeStatus(calculatedWFAtt == null ? "" : calculatedWFAtt.getText(), !"".equals(firstdateapproveStr) ? "Aprobado" : (stateSKU == null ? "" : stateSKU.getText()), fotoTomadaLiverpool == null ? "" : fotoTomadaLiverpool.getText(), product.getId());
    		currentStatus = bundle[0];
    		prevStatus = bundle[1];
    		String enriquecidoEnForo = bundle[2];
    		externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
    		vals.put( currentStatus );
    		vals.put( prevStatus );
    		vals.put( externalStatus );
    		vals.put( new org.json.JSONArray().put( product.getParentId() ));
    		vals.put( business );
    		vals.put( business );
    		vals.put( sku == null ? "" : sku.getText() );
    		vals.put( ean != null ? ean.getText() : ean2 != null ? ean2.getText() : "" );
    		vals.put( embedCodeWAP == null ? "" : embedCodeWAP.getText() );
    		vals.put( embedCodeWEB == null ? "" : embedCodeWEB.getText() );
    		vals.put( refundPolicy == null ? "" : refundPolicy.getText() );
    		vals.put( wdspr );
    		vals.put( productNameStr );
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
    		vals.put( enriquecidoEnForo == null ? "" : enriquecidoEnForo );
    		collectClassifications(product.getClassifications(), product.getId());
    		if("1007".equals(currentStatus)) {
	    		java.nio.file.Path p = java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.migration.to_skip_directory"), product.getId() );
	        	try {
	        		java.nio.file.Files.createFile(p);
	        	}catch(java.io.IOException ignore) {}
    		}
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + product.getId() + "'@1")).put("values", vals));
    		if(rows.length() == 10000) {
    			rw.writeData("list", "Product2G", null, qp, request, this::log);
    			rw.writeData("list", "Product2G", "Product2GStructureMap", qp, requestStructureGroup, this::log);

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
    	        	if(rows.length() == 10000) {
    	        		rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
    	        	}
    	        }
    	        if(rows.length() > 0) {
    	        	rw.writeData("list", "Article", "ProductReference", qp, request, System.out::println);
    	    		while(rows.length() > 0) {
    	    			rows.remove(0);
    	    		}
    	        }
    	        childParent.clear();
    		}
    		org.json.JSONObject jp = new org.json.JSONObject();
    		jp.put("proposalId", product.getId());
			jp.put("previousStatus", getStatusLabel(prevStatus) );
			jp.put("currentStatus" ,  getStatusLabel(currentStatus) );
			jp.put("externalStatus", externalStatusLabels.get( externalStatus ));
			jp.put("Business", "LVP".equals(business) ? "Liverpool" : "SBB".equals(business) ? "Suburbia" : "MKP".equals(business) ? "Marketplace" : "");
			jp.put("SKU", sku == null ? "" : sku);
			jp.put("SBB".equals(business) ? "MainBarCodeS4H" : "MainBarCode", "SBB".equals(business) ? (ean == null ? "" : ean.getText()) : (ean2 != null ? ean2.getText() : "") );
			jp.put("Direction", direction != null ? direction.getText() : "");
			jp.put("Section", section != null ? section.getText() : "");
    		if(children != null && !children.isEmpty()) {
    			org.json.JSONArray variants = new org.json.JSONArray();
				jp.put("variants", variants);
    			for(Product child : children) {
    				org.json.JSONObject variant = new org.json.JSONObject();
    				variant.put("variantId", child.getId());
    				variant.put("previousStatus", getStatusLabel(prevStatus) );
    				variant.put("currentStatus" ,  getStatusLabel(currentStatus) );
    				variant.put("externalStatus", externalStatusLabels.get( externalStatus ));
    				processChild(child, business, currentStatus, prevStatus, externalStatus, variant);
    			}
    		}else {
    			processChild(product, business, currentStatus, prevStatus, externalStatus, null);
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
    
	private String getStatusLabel(String key) {
		return 
			  "1001".equals(key) ? "Propuesta Generada"
			: "1002".equals(key) ? "Pendiente Inicio Enriquecimiento"
			: "1003".equals(key) ? "Revisi\u00f3n Compras"
			: "1004".equals(key) ? "Carga de Imagen"
			: "1005".equals(key) ? "Rechazada"
			: "1006".equals(key) ? "Por Actualizar "
			: "1007".equals(key) ? "Aprobada"
			: "1008".equals(key) ? "Modificaci\u00f3n "
			: "1009".equals(key) ? "Cancelado"
			: "1010".equals(key) ? "En Proceso Liverpool"
			: "1011".equals(key) ? "En Proceso de Env\u00edo"
			: "1020".equals(key) ? "Creaci\u00f3n de SKU"
			: "1021".equals(key) ? "Gobierno de Datos"
			: "1022".equals(key) ? "Revisi\u00f3n QA"
			: "1023".equals(key) ? "Category"
			: "1024".equals(key) ? "Rechazo Publicaci\u00f3n"
			: "1025".equals(key) ? "Eliminada"
			: "1026".equals(key) ? "En Proceso Foro"
			: "1027".equals(key) ? "Rechazo Compras"
			: "1028".equals(key) ? "Rechazo QA"
			: "1029".equals(key) ? "Rechazo Gobierno"
			: "1030".equals(key) ? "Rechazo Category"
			: "1031".equals(key) ? "Repoblamiento"
			: "1032".equals(key) ? "Excepci\u00f3n de Catalogaci\u00f3n"
			: "";
	}
    
    private void processChild(Product child, String business, String currentStatus, String prevStatus, String externalStatus, org.json.JSONObject variant) {
    	
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

		vals.put(currentStatus);
		vals.put(prevStatus);
		vals.put(externalStatus);
		vals.put(skuStr);
		vals.put("".equals( mainbarcodeStr ) ? mainbarcodes4hStr : mainbarcodeStr);
		vals.put(business);
		vals.put(nameStr);
		vals.put(tamanounicoStr);
		vals.put(coloursLiverpoolAttStr);
		vals.put(supplierpartnumberStr);
		vals.put(sapobjecttypeStr);
		rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + child.getId() + "'@1")).put("values", vals));
		if(rowsArticle.length() == 10000) {
			rw.writeData("list", "Article", null, qp, requestArticle, this::log);
		}
		if(variant != null) {
			if(!"".equals(skuStr)) {
				variant.put("SKU", skuStr);
			}
			if(!"".equals(mainbarcodeStr)) {
				variant.put("MainBarCode", mainbarcodeStr);
			}
			if(!"".equals(mainbarcodes4hStr)) {
				variant.put("MainBarCodeS4H", mainbarcodes4hStr);
			}
			if(!"".equals(supplierpartnumberStr)) {
				variant.put("SupplierPartNumber", supplierpartnumberStr);
			}
			if(!"".equals(coloursLiverpoolAttStr)) {
				variant.put("ColoursLiverpoolAtt", coloursLiverpoolAtt.getText());
			}
			if(!"".equals(tamanounicoStr)) {
				variant.put("TamanoUnico", tamanounico.getText());
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
