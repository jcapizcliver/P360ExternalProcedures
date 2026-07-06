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

public class CuentaSKUsConNegocios {

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
    	private Integer productsCounter = 0;
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
    	CuentaSKUsConNegocios an = new CuentaSKUsConNegocios();
    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	an.normalLogFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_lacuenta_todo.log");
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
        Integer refProductsCount = 0;
        an.procesaDirectorio(args[0], parser);
        an.log("Los bisnes:  " + an.negocios.size());
        an.log("Los con pas: " + an.conPas.size());
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "java", "xmlFilesProductIDs").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
        	an.productIDs.forEach(pw::println);
        }catch(java.io.IOException e) {
        	an.logE(e);
        }
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "java", "xmlFilesVariantIDs").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
        	an.variantIDs.forEach(pw::println);
        }catch(java.io.IOException e) {
        	an.logE(e);
        }
        String business = null;
        for(java.util.Map.Entry<String, String> entry : an.negocios.entrySet()) {
        	business = entry.getValue();
        	if("LVP".equals(business)) {
        		an.laCuenta[0]++;
			}else if("MKP".equals(business)) {
				an.laCuenta[1]++;
			}else if("SBB".equals(business)) {
				an.laCuenta[2]++;
			}else {
				an.laCuenta[3]++;
			}
        	if("MKP".equals(business)) {
    			an.mkp.add(entry.getKey());
    		}else if("LVP".equals(business)) {
    			an.lbp.add(entry.getKey());
    		}else if("SBB".equals(business)) {
    			an.sbb.add(entry.getKey());
    		}else {
    			an.nb.put(entry.getKey(), new String[] { "NoPa", "NoPa"});
    		}
        }
        business = null;
        int noTuvePa = 0;
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/","u01","workshop","global_me_faltan_pas").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
	        java.util.Set<String> pasQueNo = new java.util.TreeSet<>();
        	for(java.util.Map.Entry<String, String> child : an.conPas.entrySet()) {
	        	business = an.negocios.get(child.getValue());
	        	if(business == null) {
	        		noTuvePa++;
	        		pasQueNo.add(child.getValue());
	        	}else {
	        		if("MKP".equals(business)) {
	        			an.mkp.add(child.getKey());
	        		}else if("LVP".equals(business)) {
	        			an.lbp.add(child.getKey());
	        		}else if("SBB".equals(business)) {
	        			an.sbb.add(child.getKey());
	        		}else {
	        			an.nb.put(child.getKey(), new String[] { "NoPa", "NoPa"});
	        		}
		        	if("LVP".equals(business)) {
						an.laCuenta[0]++;
					}else if("MKP".equals(business)) {
						an.laCuenta[1]++;
					}else if("SBB".equals(business)) {
						an.laCuenta[2]++;
					}else {
						an.laCuenta[3]++;
					}
	        	}
	        }
        	pasQueNo.forEach(pw::println);
        }catch(java.io.IOException e) {
        	an.logE(e);
        }
        an.log("Total products found: " + refProductsCount);
        an.log("LVP:    " + an.laCuenta[0]);
        an.log("MKP:    " + an.laCuenta[1]);
        an.log("SBB:    " + an.laCuenta[2]);
        an.log("Otros:  " + an.laCuenta[3]);
        an.log("SinPas: " + noTuvePa);
        an.log("DD LVP: " + an.lbp.size());
        an.log("DD MKP: " + an.mkp.size());
        an.log("DD SBB: " + an.sbb.size());
        an.log("DD NB:  " + an.nb.size());
        
        an.laCuenta[0] = 0;
        an.laCuenta[1] = 0;
        an.laCuenta[2] = 0;
        an.laCuenta[3] = 0;
        an.mkp.clear();
        an.lbp.clear();
        an.sbb.clear();
        an.nb.clear();
        for(java.util.Map.Entry<String, String> entry : an.negociosSKUs.entrySet()) {
        	business = entry.getValue();
        	if("LVP".equals(business)) {
        		an.laCuenta[0]++;
			}else if("MKP".equals(business)) {
				an.laCuenta[1]++;
			}else if("SBB".equals(business)) {
				an.laCuenta[2]++;
			}else {
				an.laCuenta[3]++;
			}
        	if("MKP".equals(business)) {
    			an.mkp.add(entry.getKey());
    		}else if("LVP".equals(business)) {
    			an.lbp.add(entry.getKey());
    		}else if("SBB".equals(business)) {
    			an.sbb.add(entry.getKey());
    		}else {
    			an.nb.put(entry.getKey(), new String[] { "NoPa", "NoPa"});
    		}
        }
        business = null;
        noTuvePa = 0;
        try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/","u01","workshop","global_me_faltan_pas_SKUs").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
	        java.util.Set<String> pasQueNo = new java.util.TreeSet<>();
        	for(java.util.Map.Entry<String, String> child : an.conPasSKU.entrySet()) {
	        	business = an.negociosSKUs.get(child.getValue());
	        	if(business == null) {
	        		noTuvePa++;
	        		pasQueNo.add(child.getValue());
	        	}else {
	        		if("MKP".equals(business)) {
	        			an.mkp.add(child.getKey());
	        		}else if("LVP".equals(business)) {
	        			an.lbp.add(child.getKey());
	        		}else if("SBB".equals(business)) {
	        			an.sbb.add(child.getKey());
	        		}else {
	        			an.nb.put(child.getKey(), new String[] { "NoPa", "NoPa"});
	        		}
		        	if("LVP".equals(business)) {
						an.laCuenta[0]++;
					}else if("MKP".equals(business)) {
						an.laCuenta[1]++;
					}else if("SBB".equals(business)) {
						an.laCuenta[2]++;
					}else {
						an.laCuenta[3]++;
					}
	        	}
	        }
        	pasQueNo.forEach(pw::println);
        }catch(java.io.IOException e) {
        	an.logE(e);
        }
        

        an.log("Total products found (SKUs): " + refProductsCount);
        an.log("LVP:    " + an.laCuenta[0]);
        an.log("MKP:    " + an.laCuenta[1]);
        an.log("SBB:    " + an.laCuenta[2]);
        an.log("Otros:  " + an.laCuenta[3]);
        an.log("SinPas: " + noTuvePa);
        an.log("DD LVP: " + an.lbp.size());
        an.log("DD MKP: " + an.mkp.size());
        an.log("DD SBB: " + an.sbb.size());
        an.log("DD NB:  " + an.nb.size());
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private Integer procesaDirectorio(String dir, SAXParser parser) throws SAXException, java.io.IOException {
    	java.io.File[] files = new java.io.File(dir).listFiles();
        Integer refProductsCount = 0;
        long in = System.currentTimeMillis();
        log("Now reading files... " + files.length + " (" + dir + ")");
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
    
    private final java.util.Map<String, String> negocios = new java.util.TreeMap<>();
    private final java.util.Map<String, String> negociosSKUs = new java.util.TreeMap<>();
    private int[] laCuenta = new int[4];
    private final java.util.Map<String, String> conPas = new java.util.TreeMap<>();
    private final java.util.Map<String, String> conPasSKU = new java.util.TreeMap<>();
    private int lacuenta = 0;
    private final java.util.Set<String> mkp = new java.util.TreeSet<>();
    private final java.util.Set<String> lbp = new java.util.TreeSet<>();
    private final java.util.Set<String> sbb = new java.util.TreeSet<>();
    private final java.util.Map<String, String[]> nb = new java.util.TreeMap<>();
    
    private final java.util.Set<String> productIDs = new java.util.TreeSet<>();
    private final java.util.Set<String> variantIDs = new java.util.TreeSet<>();
    
    private void processProduct(Product product) {
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	String business = null;
    	String sku = null;
    	String childSku = null;
    	if(values != null) {
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
    		business = determineBusiness(n1, n2);
    		v = valMap.get("SKU");
    		if(v != null && v.getText() != null) {
    			sku = v.getText();
    		}else {
    			log("No SKU for: " + product.getId() + " negocio: " + n1 + ", extwg_s4h: " + n2 + " (" + product.getId() + ")");
    		}
    		if(business == null) {
    			if(!product.getParentId().contains("-")) {
	    			conPas.put(product.getId(), product.getParentId());
	    			v = valMap.get("ParentSKU");
	    			if(sku != null && v != null && v.getText() != null)
	    				conPasSKU.put(sku, v.getText());
    			}
    		}else {
    			negocios.put(product.getId(), business);
    			if(sku != null)
    				negociosSKUs.put(sku, business);
    		}
    	}
    	if(children != null) {
    		for(Product child : children) {
    			variantIDs.add(child.getId());
    			conPas.put(child.getId(), child.getParentId());
    			java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
        		for(Value value : child.getValues()) {
        			valMap.put(value.getAttributeId(), value);
        		}
    			Value v = null;
    			v = valMap.get("SKU");
    			childSku = v != null && v.getText() != null ? v.getText() : null;
    			v = valMap.get("ParentSKU");
    			if(childSku != null && v != null && v.getText() != null)
    				conPasSKU.put(childSku, v.getText());
    		}
    		productIDs.add(product.getId());
    	}else {
    		variantIDs.add(product.getId());
    	}
    	lacuenta++;
    	if(lacuenta % 100000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
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
