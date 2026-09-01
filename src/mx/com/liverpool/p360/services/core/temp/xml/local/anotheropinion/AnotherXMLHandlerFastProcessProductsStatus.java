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

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandlerFastProcessProductsStatus {

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
    
    private static final java.util.Map<String, String[]> data = new java.util.HashMap<>();
    
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandlerFastProcessProductsStatus an = new AnotherXMLHandlerFastProcessProductsStatus();
    	an.qp.put("includeObjectsInProtocol", "false");
    	an.rw.getRw().setBaseUrl("https://172.18.251.3:1512/rest/V2.0");
    	if(args.length < 1) {
    		System.out.println("Need to pass as first argument a directory that contains xml files to send.");
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
    					}
    				}else {
    					System.out.println("Not found.");
    				}
    			}
    		}
			if(an.rows.length() > 0) {
				an.rw.writeData("list", "Product2G", null, an.qp, an.request, an::log);
			}
			an.eleseFinish();
	        an.log("Total products found: " + an.lacuenta);
	        an.log("Total vars found: " + an.lacuentaVars);
	        an.log("Total lossmissings: " + an.perdidas);
    	}
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
//    private void fillInNonStatusIdentifiers() {
//    	
//    }
    
//    private void runOverDirectories(String startDirectory) throws ParserConfigurationException, SAXException, IOException {
//    	log("Running over... " + startDirectory);
//    	java.io.File[] files = new java.io.File(startDirectory).listFiles();
//    	for(java.io.File f : files) {
//    		if(f.isDirectory()) {
//    			runOverDirectories(f.getAbsolutePath());
//    		}else {
//    			log("\tNow processing " + f.getAbsolutePath());
//		    	procesaArchivoYProducto(f.getAbsolutePath());
//    		}
//    	}
//    }
    
    private void eleseFinish() {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		//https://chat.googleapis.com/v1/spaces/AAAAZpaMbww/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=u-P1Me5vwfb04AoqTpZI0QCmPNR4fELWlqPgmupabSY
		rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
		qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
		qp.put("token", "u-P1Me5vwfb04AoqTpZI0QCmPNR4fELWlqPgmupabSY");
		log( "" + rw.getRw().makeRequest("POST", "/AAAAZpaMbww/messages", qp, 
				new org.json.JSONObject().put("text", 
						"Envio Express de Estatus de Productos Finalizado. Productos procesados: " + lacuenta + " gen/ind (" + lacuentaVars + " vars, " + perdidas + " variantes sueltas) 😁.").toString()) );
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
    
    private void procesaArchivoYProducto(String file, String pid) throws ParserConfigurationException, SAXException {
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
	        	if(pid.equals(p.getId()))
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
			;
    private final org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows", rows);
    private int lacuentaVars = 0;
    private int perdidas = 0;
    private final java.util.Map<String, String> internalToExternalStatusMap = loadExternalStatusMap();
    private final PruebaEnvioPubSubMediaAssets elese = new PruebaEnvioPubSubMediaAssets();
    
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
    	if(product.getParentId().matches("^(S?[0-9]+)")) {
    		perdidas++;
    		return;
    	}
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	String currentStatus = null;
    	String prevStatus = null;
    	String externalStatus = null;
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
    		org.json.JSONArray vals = new org.json.JSONArray();
    		Value firstDateApprove = valMap.get("FirstDateApprove");
    		Value calculatedWFAtt = valMap.get("CalculatedWF_Att");
    		Value fotoTomadaLiverpool = valMap.get("FotoTomadaLiverpool");
    		Value stateSKU = valMap.get("StateSKU");
    		String firstdateapproveStr = firstDateApprove == null ? "" : firstDateApprove.getId() != null ? firstDateApprove.getId() : firstDateApprove.getText() == null ? "" : firstDateApprove.getText() ;
    		String[] bundle = elese.computeStatus(calculatedWFAtt == null ? "" : calculatedWFAtt.getText(), !"".equals(firstdateapproveStr) ? "Aprobado" : (stateSKU == null ? "" : stateSKU.getText()), fotoTomadaLiverpool == null ? "" : fotoTomadaLiverpool.getText(), product.getId());
    		currentStatus = bundle[0];
    		prevStatus = bundle[1];
    		externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
    		vals.put( currentStatus );
    		vals.put( prevStatus );
    		vals.put( externalStatus );
    		if("1007".equals(currentStatus)) {
	    		java.nio.file.Path p = java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.migration.to_skip_directory"), product.getId() );
	        	try {
	        		java.nio.file.Files.createFile(p);
	        	}catch(java.io.IOException ignore) {}
    		}
    		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + product.getId() + "'@1")).put("values", vals));
    		if(rows.length() == 200) {
    			rw.writeData("list", "Product2G", null, qp, request, System.out::println);
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
