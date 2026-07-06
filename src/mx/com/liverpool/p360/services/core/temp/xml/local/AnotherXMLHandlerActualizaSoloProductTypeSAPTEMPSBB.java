package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnotherXMLHandlerActualizaSoloProductTypeSAPTEMPSBB {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	private java.util.regex.Pattern attributeIDPattern = java.util.regex.Pattern.compile("(?<=')(.+)(?=',root)");
	private java.util.Map<String, String> qp = new java.util.TreeMap<>();
    private java.util.Map<String, String[]> characteristics = new java.util.TreeMap<>();
	private int bs = 2000;
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private final java.util.Map<String, java.util.Map<String, String>> lkpData = new java.util.TreeMap<>();
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_ptsts.log");
	private java.nio.file.Path correctIDsFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_proposals_correct_ptsts.log");
	private java.nio.file.Path errorIDsFilePath = java.nio.file.Paths.get("..", "logs", "list_api_load_from_step_proposals_wrong_ptsts.log");
	private final java.util.regex.Pattern valuePattern = java.util.regex.Pattern.compile("^'(.+)'@'.+'$");
	private final java.util.Set<String> aEnviar = new java.util.TreeSet<>();
	private final java.util.Set<String> aEnviarV = new java.util.TreeSet<>();
	private final java.util.LinkedList<String> ids = new java.util.LinkedList<>();
	private final java.util.LinkedList<String> idsV = new java.util.LinkedList<>();
	private final boolean specific = true;
	
	private java.util.LinkedList<String[]> datas = new java.util.LinkedList<>();
	

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
                Value wv = product.getWorkingValue();
                if(wv != null) {
            		StringBuilder sb = new StringBuilder();
            		sb.append(wv.getText() == null ? "" : wv.getText());
            		sb.append(ch, start, length);
                	wv.setText( sb.toString() );
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
                		StringBuilder sb = new StringBuilder();
                		sb.append(a.getName() == null ? "" : a.getName());
                		sb.append(ch, start, length);
            			a.setName( sb.toString() );
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

    // Entry point
    public static void main(String[] args) throws Exception {
    	long init = System.currentTimeMillis();
    	AnotherXMLHandlerActualizaSoloProductTypeSAPTEMPSBB an = new AnotherXMLHandlerActualizaSoloProductTypeSAPTEMPSBB();

    	java.nio.file.Files.createDirectories( java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1]) );
    	
    	an.normalLogFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_ptsts.log");
    	an.correctIDsFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_proposals_correct_ptsts.log");
    	an.errorIDsFilePath = java.nio.file.Paths.get("..", "logs", args.length == 1 ? "" : args[1], "list_api_load_from_step_proposals_wrong_ptsts.log");
        
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
        an.qp.put("includeObjectsInProtocol", "false");
        SAXParser parser = factory.newSAXParser();
        org.json.JSONObject requestProduct = new org.json.JSONObject();
        org.json.JSONArray columnsProduct = new org.json.JSONArray();
        org.json.JSONArray rowsProduct = new org.json.JSONArray();
        columnsProduct.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductTypeSAPTEMPSBB',root,\"0000.0000.RK\",'ProductTypeSAPTEMPSBB',-1)"));
        requestProduct.put("columns", columnsProduct);
        requestProduct.put("rows", rowsProduct);
        long refProductsCount = 0l;
        long in = System.currentTimeMillis();
        an.log("Now reading files...");
        an.processDirectory(args[0], requestProduct, parser);
        an.log("Parsing files took: " + an.rw.getRw().formatTime(System.currentTimeMillis() - in));
        an.log("Total products found: " + refProductsCount);
        an.log("*** Total AttributeID ***");
        long current = 0l;
        an.log("Done, found: " + current + " products.");
        if(rowsProduct.length() > 0) {
        	an.sendData("Product2G", requestProduct);
        }
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
    }
    
    private void processDirectory(String filePath, org.json.JSONObject requestProduct, SAXParser parser) {
    	 java.io.File[] files = new java.io.File(filePath).listFiles();
    	for(File input : files) {
    		if(input.isDirectory()) {
    			processDirectory(input.getAbsolutePath(), requestProduct, parser);
    		}else {
		        Handler handler = new Handler();
		        try {
		        	parser.parse(input, handler);
		        	List<Product> fp = handler.getFinished();
		        	log("-->" + fp.size());
		        	for(Product p : handler.getFinished()) {
		        		processProduct(p, requestProduct);
		        	}
		        }catch(org.xml.sax.SAXParseException e) {
		        	log("Problem processing following file: " + input.getName());
		        } catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
        }
    }
    
    private void processProduct(Product product, org.json.JSONObject requestProduct) {
    	java.util.Map<String, String> lkpCont = null;
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	if(product.getUserTypeId().startsWith("SalesItemFamily") ){
    		ids.add(product.getId());
    	}else if(product.getUserTypeId().equals("SalesItemVariant")) {
    		idsV.add(product.getId());
    	}else if("SalesItem".equals(product.getUserTypeId())) {
    		if(product.getParentId().startsWith("EU") || product.getParentId().startsWith("UnCatLevel")) {
    			ids.add(product.getId());
    		}
    		idsV.add(product.getId());
    	}else {
    		System.out.println("Not a known userTypeID: " + product.getUserTypeId());
    	}
    	if(values != null) {
    		String[] data = null;
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    			if(sendLkpValues) {
    				data = characteristics.get(value.attributeId);
    				if(data != null) {
    					if("LOOKUP".equals(data[1])) {
    						lkpCont = lkpData.get(data[0]);
    						if(lkpCont == null) {
    							lkpCont = new java.util.TreeMap<>();
    							lkpData.put(data[0], lkpCont);
    						}
    						lkpCont.put(value.getId() == null ? value.getText() : value.getId(), value.getText());
    					}
    				}
    			}
    		}
    		org.json.JSONArray rowsP = requestProduct.getJSONArray("rows");
			if( "SalesItemVariant".equals(product.getUserTypeId()) ) {
			}else if("SalesItem".equals(product.getUserTypeId())) {
				Value skuValue = valMap.get("ProductTypeSAPTEMPSBB");
				if(skuValue != null){
					addRow("Product2G", product.getId(), skuValue.getId(), requestProduct);
	    			if(rowsP.length() == bs) {
	    				sendData("Product2G", requestProduct);
	    			}
				}else {
//					log("SalesItem with no propper ParentID: " + product.getId());
				}
			}else if(product.getUserTypeId().startsWith("SalesItemFamily")) {
				Value skuValue = valMap.get("SKU");
				if(skuValue != null){
					addRow("Product2G", product.getId(), skuValue.getId(), requestProduct);
	    			if(rowsP.length() == bs) {
	    				sendData("Product2G", requestProduct);
	    			}
				}else {
//					log("SalesItem with no propper ParentID: " + product.getId());
				}
			}else {
				log("PANIC: no product type found: " + product.getUserTypeId());
			}
    	}
    	
    }
    
    private void addRow(String entity, String id, String sku, org.json.JSONObject requestProduct) {
    	org.json.JSONArray rowsP = requestProduct.getJSONArray("rows");
    	org.json.JSONArray rowValues = new org.json.JSONArray();
		org.json.JSONObject row = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", rowValues);
		rowsP.put(row);
		rowValues.put( sku );
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
        		if(!rowsWithErrors.contains(i)) {
        			pw.println( objects.getJSONObject(i).getJSONObject("object").getString("id") );
        		}
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
			sendData( entity , new org.json.JSONObject().put("columns", columns).put("rows", new org.json.JSONArray().put(row)) );
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
