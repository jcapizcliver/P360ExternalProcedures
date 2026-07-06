package mx.com.liverpool.p360.services.core.temp.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandlerFindElementsBySKU {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private static final java.util.regex.Pattern attributeIDPattern = java.util.regex.Pattern.compile("(?<=')(.+)(?=',root)");
	private static final java.util.Map<String, String> qp = new java.util.TreeMap<>();
    private static final java.util.Map<String, String[]> characteristics = new java.util.TreeMap<>();
	private static final java.util.Map<String, java.util.Map<String, String>> lkpData = new java.util.TreeMap<>();
	private static final java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "reprocess_list_api_load_from_step.log");
	private static final java.nio.file.Path correctIDsFilePath = java.nio.file.Paths.get("..", "logs", "reprocess_list_api_load_from_step_proposals_correct.log");
	private static final java.nio.file.Path errorIDsFilePath = java.nio.file.Paths.get("..", "logs", "reprocess_list_api_load_from_step_proposals_wrong.log");
	private static final java.util.Set<String> skusGen = new java.util.TreeSet<>();
	private static final java.util.Set<String> skusVar = new java.util.TreeSet<>();
	private static final java.util.Set<String> parentIdFromMissingVariants = new java.util.TreeSet<>();
	private static final java.util.Set<String> asíTeQueríaAgarrarPrko = new java.util.TreeSet<>();
	private static final java.util.Set<String> missingVariantSKU = new java.util.TreeSet<>();
	private static final java.util.Set<String> missingProductSKU = new java.util.TreeSet<>();
	private static final java.util.regex.Pattern valuePattern = java.util.regex.Pattern.compile("^'(.+)'@'.+'$");
	
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

    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandlerFindElementsBySKU an = new AnotherXMLHandlerFindElementsBySKU();
        if (args.length == 0) {
            System.err.println("Usage: java ProductValuesSaxParser <file.xml>");
            System.exit(1);
        }
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        qp.put("includeObjectsInProtocol", "false");
        SAXParser parser = factory.newSAXParser();
        java.io.File[] files = new java.io.File(args[0]).listFiles(ff -> ff.getName().endsWith("xml"));
        an.collectCharacteristics(characteristics);
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
        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUMigraciónWorkshop", "real_faltantes_gen"))){
        	lns.forEach(skusGen::add);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUMigraciónWorkshop", "real_faltantes_var"))){
        	lns.forEach(skusVar::add);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
        String[] data = null;
        for(String attributeId : attributeIDs) {
        	data = characteristics.get(attributeId);
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
        for(File input : files) {
	        Handler handler = an.new Handler();
	        parser.parse(input, handler);
	        for (Product product : handler.getFinished()) {
        		an.processProduct(product, requestProduct, requestArticle);
	        }
	        current += handler.getPrductsCounter();
        }
        an.log("Done, found: " + asíTeQueríaAgarrarPrko.size() + " puercos.");
        an.log("Done, found: " + parentIdFromMissingVariants.size() + " parentIds from missing variant sku.");
        java.util.LinkedList<String> stillMissingGenSKUs = new java.util.LinkedList<>();
        java.util.LinkedList<String> stillMissingVarSKUs = new java.util.LinkedList<>();
        skusGen.forEach(s -> {
        	if(!missingProductSKU.contains(s)) {
        		stillMissingGenSKUs.addLast(s);
        	}
        });
        skusVar.forEach(s -> {
        	if(!missingVariantSKU.contains(s)) {
        		stillMissingVarSKUs.addLast(s);
        	}else {
        		System.out.println("Esta kala sí la enkantré: " + s);
        	}
        });
        System.out.println("Still missing generic SKUs: ");
        for(String s : stillMissingGenSKUs) {
        	System.out.println(s);
        }
        System.out.println("Still missing variant SKUs: ");
        for(String s : stillMissingVarSKUs) {
        	System.out.println(s);
        }
        System.out.println("Padres faltantes: ");
        for(String pid : parentIdFromMissingVariants) {
        	System.out.println(pid);
        }
        an.log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private void processProduct(Product product, org.json.JSONObject requestProduct, org.json.JSONObject requestArticle) {
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	children = product.getProducts();
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
			Value sku = valMap.get("SKU");
			String parentId = product.getParentId();
			if(sku != null) {
				if(skusVar.contains(sku.getText())) {
					parentIdFromMissingVariants.add(parentId);
					missingVariantSKU.add(sku.getText());
				}else if(skusGen.contains(sku.getText())) {
					asíTeQueríaAgarrarPrko.add(product.getId());
					missingProductSKU.add(sku.getText());
				}
			}
    	}
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, requestProduct, requestArticle);
    		}
    	}
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
