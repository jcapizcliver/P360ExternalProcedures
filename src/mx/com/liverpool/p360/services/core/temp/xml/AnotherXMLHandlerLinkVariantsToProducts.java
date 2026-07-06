package mx.com.liverpool.p360.services.core.temp.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandlerLinkVariantsToProducts {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "link_to_products_list_api_load_from_step.log");
	private java.nio.file.Path correctIDsFilePath = java.nio.file.Paths.get("..", "logs", "link_to_products_list_api_load_from_step_proposals_correct.log");
	private java.nio.file.Path errorIDsFilePath = java.nio.file.Paths.get("..", "logs", "link_to_products_list_api_load_from_step_proposals_wrong.log");
	private java.util.Set<String> salesItems = new java.util.TreeSet<>();
	private final int bs = 2000;
	
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
	                } else if("Classification".equals(name)) {
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
                }else if("Classification".equals(name)) {
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

    // Entry point
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandlerLinkVariantsToProducts an = new AnotherXMLHandlerLinkVariantsToProducts();
        if (args.length == 0) {
            System.err.println("Usage: java ProductValuesSaxParser <file.xml>");
            System.exit(1);
        }

    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );

    	an.normalLogFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "link_to_products_list_api_load_from_step.log");
    	an.correctIDsFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "link_to_products_list_api_load_from_step_proposals_correct.log");
    	an.errorIDsFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "link_to_products_list_api_load_from_step_proposals_wrong.log");
        
        
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
        java.io.File[] files = new java.io.File(args[0]).listFiles(ff -> ff.getName().endsWith("xml"));
        org.json.JSONObject requestArticle = new org.json.JSONObject();
        org.json.JSONArray columnsArticle = new org.json.JSONArray();
        org.json.JSONArray rowsArticle = new org.json.JSONArray();
        columnsArticle.put(new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
        requestArticle.put("columns", columnsArticle);
        requestArticle.put("rows", rowsArticle);
        long refProductsCount = 0l;
        long in = System.currentTimeMillis();
        an.log("Now reading files...");
        for(File input : files) {
	        Handler handler = an.new Handler();
	        try {
		        parser.parse(input, handler);
		        refProductsCount += handler.getPrductsCounter();
	        }catch(org.xml.sax.SAXParseException e) {
	        	an.log("Problem processing following file: " + input.getName());
	        }
        }
        an.log("Parsing files took: " + an.rw.getRw().formatTime(System.currentTimeMillis() - in));
        an.log("Total products found: " + refProductsCount);
        java.math.BigDecimal bdRefCounter = new java.math.BigDecimal(refProductsCount);
        java.math.BigDecimal HUNDRED = java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.TEN);
        long current = 0l;
        for(File input : files) {
	        Handler handler = an.new Handler();
	        try {
		        parser.parse(input, handler);
		        for (Product product : handler.getFinished()) {
	        		an.processProduct(product, requestArticle);
		        }
		        current += handler.getPrductsCounter();
		        an.log(current + "/" + refProductsCount + " " + (new java.math.BigDecimal(current).multiply(HUNDRED).divide( bdRefCounter, 4, java.math.RoundingMode.HALF_UP )) + "% " + an.rw.getRw().formatTime(System.currentTimeMillis() - init));
	        }catch(org.xml.sax.SAXParseException e) {
	        	an.log("Problem processing following file: " + input.getName());
	        }
        }
        System.out.println("All sales items: " + an.salesItems.size());
//        java.util.Set<String> toDel = new java.util.TreeSet<>();
        for(String si : an.salesItems) {
        	
//        	toDel.add(si);
//        	if(toDel.size() == 1000) {
//        		an.deleteData(toDel);
//        		toDel.clear();
//        	}
        }
//        if(toDel.size() > 0) {
//        	an.deleteData(toDel);
//        	toDel.clear();
//        }
    	an.sendData(requestArticle);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }

    private void processProduct(Product product, org.json.JSONObject requestArticle) {
    	java.util.LinkedList<Product> children = null;
    	java.util.LinkedList<Value> values = null;
    	children = product.getProducts();
    	values = product.getValues();
		String parentId = product.getParentId();
		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
		for(Value value : values) {
			valMap.put(value.getAttributeId(), value);
		}
		org.json.JSONArray rowsA = requestArticle.getJSONArray("rows");
		if(parentId != null) {
			parentId = product.getParentId();
			Value parentIdValue = valMap.get("ParentID");
			parentId = parentId == null ? parentIdValue != null ? parentIdValue.getText() : null : parentId;
			if( "SalesItemVariant".equals(product.getUserTypeId()) ) {
				addRow( product.getId(), product.getParentId(), requestArticle );
    			if(rowsA.length() == bs) {
    				sendData(requestArticle);
				}
			}else if("SalesItem".equals(product.getUserTypeId())) {
				salesItems.add(product.getId());
				if(parentId.startsWith("EU") || parentId.startsWith("UnCatLevel")) {
					addRow(product.getId(), product.getId(), requestArticle);
	    			if(rowsA.length() == bs) {
	    				sendData(requestArticle);
	    			}
				}else if(!parentId.startsWith("EU") && !parentId.startsWith("UnCatLevel")){
					addRow( product.getId(), product.getParentId(), requestArticle );
	    			if(rowsA.length() == bs) {
	    				sendData(requestArticle);
					}
				}
			}
		}else {
			log("No ParentID: " + product.getId());
		}
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, requestArticle);
    		}
    	}
    }
    
    private void deleteData(java.util.Set<String> idsToDelete) {
    	StringBuilder sb = new StringBuilder();
    	int a = 0;
    	for(String id : idsToDelete) {
    		sb.append(a == 0 ? "" : ",");
    		sb.append("'");
    		sb.append(id);
    		sb.append("'@1");
    		a++;
    	}
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
    	qp.put("items", sb.toString());
    	rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
    }
    
    private void addRow(String id, String parentId, org.json.JSONObject requestProduct) {
    	org.json.JSONArray rowsP = requestProduct.getJSONArray("rows");
    	org.json.JSONArray rowValues = new org.json.JSONArray();
    	org.json.JSONObject row = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("qualification", new org.json.JSONObject().put("referencedSupplierAid", parentId)).put("values", rowValues);
		rowValues.put( parentId );
		rowsP.put(row);
    }
    
    private void sendData(org.json.JSONObject request) {
    	log("Sending: " + request.getJSONArray("rows").length());
    	rw.writeData("list", "Article", "ProductReference", qp, request, rr->{
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
//    	emptyArray(request.getJSONArray("rows"));
    }
    
    private void emptyArray(org.json.JSONArray rows) {
    	while(rows.length() > 0) {
    		rows.remove(0);
    	}
    }

	private void logCorrectlyWrittenIDs(org.json.JSONObject request, org.json.JSONObject response) {
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
