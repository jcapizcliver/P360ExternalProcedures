package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ColocaPrimeraFechaDeAprobacion {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = null;
	

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
    	ColocaPrimeraFechaDeAprobacion an = new ColocaPrimeraFechaDeAprobacion();
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	an.normalLogFilePath = java.nio.file.Paths.get(
    											  ".."
    											, "logs"
    											, args.length == 1 ? "" : args[1]
    											, "coloca_primera_fecha_de_aprobacion.log"
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
        java.util.Map<String, java.util.Set<Long>> refs = new java.util.HashMap<>();
        an.procesaDirectorio(args[0], refs, parser);
        StringBuilder sb = new StringBuilder();
        int m = 0;
        an.qp.put("pageSize", "2000");
        an.qp.put("fields", "Article.SupplierAID,Article.FirstDateApproved,Article.LastDateApproved");
        java.util.List<String> toOp = new java.util.ArrayList<>();
        java.util.List<String> toOp2 = new java.util.ArrayList<>();
        for(String varId : an.varIds) {
        	sb.append(sb.length() == 0 ? "" : ",").append("'").append(varId).append("'@1");
        	m++;
        	if("S93653049".equals(varId)) {
        		System.out.println("Added for request.");
        	}
        	if(m % 1000 == 0) {
        		an.qp.put("items", sb.toString());
        		an.rw.collectData("list", "Article", null, "byItems", an.qp, row -> {
        			org.json.JSONArray values = row.getJSONArray("values");
        			if("S93653049".equals(values.getString(0)))
        				System.out.println(values);
        			if("".equals(values.getString(1))) {
        				toOp.add(values.getString(0));
        			}
        			if("".equals(values.getString(2))) {
        				toOp2.add(values.getString(0));
        			}
        		});
        		sb.setLength(0);
        	}
        }
        if(sb.length() > 0) {
        	an.qp.put("items", sb.toString());
    		an.rw.collectData("list", "Article", null, "byItems", an.qp, row -> {
    			org.json.JSONArray values = row.getJSONArray("values");
    			if("S93653049".equals(values.getString(0)))
    				System.out.println(values + "\tOn2");
    			if("".equals(values.getString(1))) {
    				toOp.add(values.getString(0));
    			}
    			if("".equals(values.getString(2))) {
    				toOp2.add(values.getString(0));
    			}
    		});
    		sb.setLength(0);
        }
        System.out.println("------>>>> " + toOp.size());
        java.util.Set<Long> times = null;
        java.util.Map<String, String> qp0 = new java.util.HashMap<>();
        qp0.put("includeObjectsInProtocol", "false");
        System.out.println("--- " + toOp.contains("S93653049"));
        for(String op : toOp) {
        	times = refs.get(op);
        	if(times != null) {
        		java.util.List<Long> l = new java.util.ArrayList<>( times );
        		an.rowsArticle.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + op + "'@1")).put("values", new org.json.JSONArray().put( asDateString( l.get(0) ) )));
        		if(an.rowsArticle.length() == 1000) {
        			an.rw.writeData("list", "Article", null, qp0, an.requestArticle, System.out::println);
        			while(an.rowsArticle.length() > 0) {
        				an.rowsArticle.remove(0);
        			}
        		}
        	}else {
        		System.out.println("Andamos pidiendo cosas que no tenemos... " + op);
        	}
        }
        if(an.rowsArticle.length() > 0) {
        	an.rw.writeData("list", "Article", null, an.qp, an.requestArticle, an::log);
        	while(an.rowsArticle.length() > 0) {
        		an.rowsArticle.remove(0);
        	}
        }
        for(String op : toOp2) {
        	times = refs.get(op);
        	if(times != null) {
        		java.util.List<Long> l = new java.util.ArrayList<>( times );
        		an.rowsArticleL.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + op + "'@1")).put("values", new org.json.JSONArray().put( asDateString( l.get(l.size() - 1) ) )));
        		if(an.rowsArticleL.length() == 1000) {
        			an.rw.writeData("list", "Article", null, qp0, an.requestArticleL, System.out::println);
        			while(an.rowsArticleL.length() > 0) {
        				an.rowsArticleL.remove(0);
        			}
        		}
        	}else {
        		System.out.println("Andamos pidiendo cosas que no tenemos... " + op);
        	}
        }
        if(an.rowsArticleL.length() > 0) {
        	an.rw.writeData("list", "Article", null, an.qp, an.requestArticleL, an::log);
        	while(an.rowsArticleL.length() > 0) {
        		an.rowsArticleL.remove(0);
        	}
        }
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private static String asDateString(Long unixTimestamp) {
    	return java.time.Instant.ofEpochMilli(unixTimestamp).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }
    
    private Integer procesaDirectorio(String dir, java.util.Map<String, java.util.Set<Long>> refs, SAXParser parser) throws SAXException, java.io.IOException {
    	log("Checking: " + dir);
    	java.io.File[] files = new java.io.File(dir).listFiles();
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        log("Now reading files... " + files.length);
        java.util.List<Product> finished = null;
        for(File input : files) {
        	if(input.isDirectory()) {
        		refProductsCount += procesaDirectorio(input.getAbsolutePath(), refs, parser);
        	}else {
		        Handler handler = new Handler();
		        try {
		        	parser.parse(input, handler);
		        	finished = handler.getFinished();
			        refProductsCount += handler.getPrductsCounter();
			        java.util.regex.Matcher m = p.matcher(input.getName());
			        if(!m.find()) {
			        	System.out.println("---->" + input.getAbsolutePath());
			        }else {
				        for(Product p : finished) {
				        	processProduct(p, Long.parseLong(m.group(1)) , refs);
				        }
			        }
		        }catch(org.xml.sax.SAXParseException e) {
		        	log("Problem processing following file: " + input.getAbsolutePath());
		        }
        	}
        }
        log("Parsing files took: " + rw.getRw().formatTime(System.currentTimeMillis() - in));
        return refProductsCount;
    }
    
    private final java.util.regex.Pattern p = java.util.regex.Pattern.compile("([0-9]+)(?=\\.xml)");
    private final java.util.Set<String> varIds = new java.util.TreeSet<>();

    private int lacuenta = 0;
    private final org.json.JSONArray columnsArticle = new org.json.JSONArray()
	    		.put(new org.json.JSONObject().put("identifier", "Article.FirstDateApproved"))
    		;
    private final org.json.JSONArray rowsArticle = new org.json.JSONArray();
    private final org.json.JSONObject requestArticle = new org.json.JSONObject().put("columns", columnsArticle).put("rows", rowsArticle);

    private final org.json.JSONArray columnsArticleL = new org.json.JSONArray()
    		.put(new org.json.JSONObject().put("identifier", "Article.LastDateApproved"))
    		;
    private final org.json.JSONArray rowsArticleL = new org.json.JSONArray();
    private final org.json.JSONObject requestArticleL = new org.json.JSONObject().put("columns", columnsArticleL).put("rows", rowsArticleL);
    private int lacuentaVars = 0;

    private void processProduct(Product product, Long timestamp, java.util.Map<String, java.util.Set<Long>> refs) {
    	java.util.LinkedList<Product> children = null;
    	children = product.getProducts();
    	if(children != null && !children.isEmpty()) {
    		for(Product child : children) {
    			if("S93653049".equals(child.getId())) {
    				System.out.println("Found you " + child.getId() + " - " + timestamp);
    			}
    			varIds.add(child.getId());
    			java.util.Set<Long> times = refs.get(child.getId());
    			if(times == null) {
    				times = new java.util.TreeSet<>();
    				refs.put(child.getId(), times);
    			}
    			times.add(timestamp);
    		}
    	}else if(product.getId().startsWith("S")) {
    		varIds.add(product.getId());
    		java.util.Set<Long> times = refs.get(product.getId());
    		if(times == null) {
    			times = new java.util.TreeSet<>();
    			refs.put(product.getId(), times);
    		}
    		times.add(timestamp);
    	}
    	lacuenta++;
    	if(lacuenta % 10000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
    		}
    	}
    }
    
    public static String convertirFecha(String fechaEntrada) {
        java.time.format.DateTimeFormatter out = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        try {
            return java.time.ZonedDateTime.parse(fechaEntrada, java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", java.util.Locale.ENGLISH)).format(out);
        } catch (java.time.format.DateTimeParseException e) {
            try{
            	return java.time.LocalDateTime.parse(fechaEntrada, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")).format(out);
            }catch(java.time.format.DateTimeParseException ex) {
            	return fechaEntrada;
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
