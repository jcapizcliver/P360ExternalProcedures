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

public class TuCualesTienes {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_lacuenta.log");
	

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
    
	private java.util.List<String> currentProducts = new java.util.ArrayList<>(sendProduct ? 2000000 : 10);

    // Entry point
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	TuCualesTienes an = new TuCualesTienes();

    	
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	
    	an.normalLogFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_lacuenta.log");
        
    	if (args.length == 0) {
            System.err.println("Usage: java AnotherXMLHandler2 <file.xml>");
            System.exit(1);
        }
    	String[] losData = null;
    	an.log("Now collecting current known products");
    	java.nio.file.Path path = java.nio.file.Paths.get("..", "llaves");
		if(!java.nio.file.Files.exists(path)) {
			an.log("Querying from P360");
			RESTWrapper rw = new RESTWrapper();
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", "Product2G.ProductNo");
			qp.put("pageSize", "50000");
			rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> an.currentProducts.add(row.getJSONArray("values").getString(0)), an::log);
			qp.put("fields", "Article.SupplierAID");
			rw.collectData("list", "Article", null, "byCatalog", qp, row -> an.currentProducts.add(row.getJSONArray("values").getString(0)), an::log);
			an.log("Collected: " + an.currentProducts.size());
			losData = an.currentProducts.toArray(new String[] {});
			an.currentProducts.clear();
	    	java.util.Arrays.sort(losData);
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(path.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				for(int a = 0; a<losData.length; a++) {
					pw.println( losData[a] );
				}
				an.log("Kept data.");
			}catch(java.io.IOException e) {
				an.logE(e);
			}
		}else {
			an.log("Reading from file: " + path);
			String[] muchos = new String[10000000];
			int a = 0;
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(path.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String ln = null;
				while((ln = br.readLine()) != null) {
					muchos[a] = ln;
					a++;
				}
				losData = java.util.Arrays.copyOf(muchos, a);
			}catch(java.io.IOException e) {
				an.logE(e);
			}
			muchos = null;
		}
		
        an.qp.put("includeObjectsInProtocol", "false");
        long refProductsCount = 0l;
        long in = System.currentTimeMillis();
        an.log("Parsing files took: " + an.rw.getRw().formatTime(System.currentTimeMillis() - in));
        an.log("Total products found: " + refProductsCount);
        an.log("*** Total AttributeID ***");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        long current = 0l;
        processFile(args[0], an, losData, parser);
        String[] hola1 = new String[an.i1];
        String[] hola2 = new String[an.i2];
        int m1 = 0;
        int m2 = 0;
        String prev = null;
        for(int a = 0; a<an.i1; a++) {
        	if(prev != null && !prev.equals(an.losQueVi[a])) {
        		hola1[m1] = prev;
        		m1++;
        	}
        	prev = an.losQueVi[a];
        }
        hola1[m1] = prev;
        m1++;
        prev = null;
        for(int a = 0; a<an.i2; a++) {
        	if(prev != null && !prev.equals(an.losQueNoVi[a])) {
        		hola2[m2] = an.losQueNoVi[a];
        		m2++;
        	}
        	prev = an.losQueNoVi[a];
        }
        hola2[m2] = prev;
        m2++;
        prev = null;
        String[] losQueVi = java.util.Arrays.copyOf(hola1, m1);
        String[] losQueNoVi = java.util.Arrays.copyOf(hola2, m2);
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("..", "losQueTengoEnP360").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
        	for(int a = 0; a<m1; a++) {
        		pw.println(losQueVi[a]);
        	}
        }catch(java.io.IOException e) {
        	an.logE(e);
        }
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("..", "losQueNoTengoEnP360").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
        	for(int a = 0; a<m2; a++) {
        		pw.println(losQueNoVi[a]);
        	}
        }catch(java.io.IOException e) {
        	an.logE(e);
        }
        an.log("Done, found: " + current + " products.");
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
        an.log("Los ya lo tengo: " + m1);
        an.log("Los que sí tengo: " + m2);
        an.log("Suburbias: " + an.suburbias);
        an.log("Liverpools: " + an.liverpools);
        an.log("Marketpleis: " + an.mkt);
        an.log("Otros: " + an.otros);
        an.cuentaPorNegocio.entrySet().forEach(entry -> System.out.println( entry.getKey() + " - " + entry.getValue() ));
    }
    
    private static void processFile(String file, TuCualesTienes an, String[] losData, SAXParser parser) throws ParserConfigurationException, SAXException, IOException {
    	java.io.File[] files = new java.io.File(file).listFiles();
		for(File input : files) {
			if(input.isDirectory()) {
				processFile(input.getAbsolutePath(), an, losData, parser);
			}else {
			    try {
			    	Handler handler = an.new Handler();
			    	long in = System.currentTimeMillis();
			        parser.parse(input, handler);
			        an.log("Parsed in: " + an.rw.getRw().formatTime(System.currentTimeMillis() - in) + " -->" + input.getAbsolutePath());
			        in = System.currentTimeMillis();
			        for (Product product : handler.getFinished()) {
			    		an.processProduct(product, losData, input.getAbsolutePath());
			        }
			        an.log("Iterated in: " + an.rw.getRw().formatTime(System.currentTimeMillis() - in) + " (" + handler.getFinished().size() + " products)");
			    }catch(org.xml.sax.SAXParseException e) {
			    	an.log("Problem processing following file: " + input.getName());
			    }
			}
        }
    }
    
    private java.util.Map<String, Integer> cuentaPorNegocio = new java.util.TreeMap<>();
//    private java.util.Set<String> losQueVi = new java.util.HashSet<>();
//    private java.util.Set<String> losQueNoVi = new java.util.HashSet<>();
    private String[] losQueVi = new String[20000000];
    private String[] losQueNoVi = new String[20000000];
    private int i1 = 0;
    private int i2 = 0;
    private int suburbias = 0;
    private int liverpools = 0;
    private int mkt = 0;
    private int otros = 0;
    
    private void processProduct(Product product, String[] alreadyInSystem, String currentFile) {
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, alreadyInSystem, currentFile);
    		}
    	}
    	if(java.util.Arrays.binarySearch( alreadyInSystem, product.getId()) >= 0) {
    		losQueVi[i1] = product.getId();
    		i1++;
    		return;
    	}
    	losQueNoVi[i2] = product.getId();
    	i2++;
    	if(children != null && values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
    		Value v = null;
    		v = valMap.get("Negocio");
    		String n1 = v == null ? "" : v.getText();
    		v = valMap.get("EXTWG_S4H");
    		String n2 = v == null ? "" : v.getText();
    		n1 = n1 == null ? "" : n1;
    		n2 = n2 == null ? "" : n2;
    		String business = determineBusiness(n1, n2);
    		if(business == null) {
    			otros++;
    			otros+=children.size();
    		}else {
    			if("SBB".equals(business)) {
    				suburbias++;
    				suburbias+=children.size();
    			}else if("LVP".equals(business)) {
    				liverpools++;
    				liverpools+=children.size();
    			}else if("MKP".equals(business)) {
    				mkt++;
    				mkt+=children.size();
    			}
    		}
    	}
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
