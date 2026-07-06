package mx.com.liverpool.p360.services.core.temp.xml.local.precise;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class LoadProductDataSecondOpinionForFlatChildProducts {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "list_for_flat_child_products.log");
	
	public LoadProductDataSecondOpinionForFlatChildProducts(String baseOutputFileName) throws IOException {
		initLekungas(baseOutputFileName);
	}
	
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

    private class AssetCrossRef {
    	
    	private String id = null;
    	private String type = null;
    	
    	public AssetCrossRef(String id, String type) {
    		this.id = id;
    		this.type = type;
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
    	private java.util.List<AssetCrossRef> imageReference = new java.util.ArrayList<>();
    	
    	public Product(String id, String parentId, String userTypeId) {
    		this.id = id;
    		this.parentId = parentId;
    		this.userTypeId = userTypeId;
    	}
    	
    	public java.util.List<AssetCrossRef> getImageReference(){
    		return this.imageReference;
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
	                } else if("AssetCrossReference".equals(name)) {
	                	String assetId = attributes.getValue("AssetID");
	                	String type = attributes.getValue("type");
	                	product.getImageReference().add(new AssetCrossRef(assetId, type));
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
    	if (args.length == 0) {
            System.err.println("Usage: java CuentaSKUsConNegocios <directory with xml files>");
            System.exit(1);
        }
    	String baseOutputFileName = args.length > 1 ? args[1] : "MigratedRelevantVariantData";
    	LoadProductDataSecondOpinionForFlatChildProducts an = new LoadProductDataSecondOpinionForFlatChildProducts(baseOutputFileName);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.procesaDirectorio(args[0], parser);
        an.resolveMissingChilds();
//        java.nio.file.Path zipPath = java.nio.file.Paths.get( baseOutputFileName + ".zip" );
//        System.out.println("Ladata: " + an.ladata.size() + " (lacuenta: " + an.lacuenta + ", " + an.lacuentaVars + ")");
        try {
            an.bw.flush();
            an.zos.closeEntry();
            an.bw.close();
        } catch (java.io.IOException e) {
            an.logE(e);
        }
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Total familias sin variantes: " + an.lacuentaPadresSinVariantes);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
        an.eleseFinish();
    }
    
    private java.io.BufferedWriter bw;
    private java.util.zip.ZipOutputStream zos = null;
    
    private void initLekungas(String baseOutputFileName) throws IOException {
//    	String baseOutputFileName = "MigratedRelevantVariantData";
        java.nio.file.Path zipPath = java.nio.file.Paths.get( baseOutputFileName + ".zip" );
        java.io.FileOutputStream fos = new java.io.FileOutputStream(zipPath.toFile());
        java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos);
        zos = new java.util.zip.ZipOutputStream(bos);
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry( baseOutputFileName + ".csv");
        zos.putNextEntry(entry);
        bw = new java.io.BufferedWriter(
        		new java.io.OutputStreamWriter(zos, java.nio.charset.StandardCharsets.UTF_8)
        		); 
        bw.write(rw.getRw().serializeChunk(header));
        bw.write("\r\n");
    }
    
    private void eleseFinish() {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
		qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
		qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
		log( "" + rw.getRw().makeRequest("POST", "/AAAAvwSYdXo/messages", qp, 
				new org.json.JSONObject().put("text", 
						"Finaliza aplanamiento de data desde XMLs recibidos de STEP: " + lacuenta + " gen/ind (" + lacuentaVars + " vars) 😁.").toString()) );
    }
    
    
    private Integer procesaDirectorio(String dir, SAXParser parser) throws SAXException, java.io.IOException {
    	log("Checking: " + dir);
    	java.io.File[] files = new java.io.File(dir).listFiles();
        Integer refProductsCount = 0;
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
        return refProductsCount;
    }
    
    private int lacuenta = 0;
    private int lacuentaVars = 0;
//    private final java.util.Map<String, String[]> ladata = new java.util.HashMap<>();
    private int lacuentaPadresSinVariantes = 0;
    private java.util.Map<String, String[]> gens = new java.util.HashMap<>();
    private java.util.List<Product> sueltos = new java.util.ArrayList<>();
    private PruebaEnvioPubSubMediaAssets elese = new PruebaEnvioPubSubMediaAssets();
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
    
    private void resolveMissingChilds() throws IOException {
    	String[] bundle = null;
    	for(Product p : sueltos) {
    		bundle = gens.get(p.getParentId());
    		if(bundle != null) {
    			processVariantData(p, bundle);
    		}
    	}
    }
    
    private void processProduct(Product product) throws IOException {
    	lacuenta++;
    	if(lacuenta % 10000 == 0) {
    		System.out.print(".");
    		if(lacuenta % 1000000 == 0) {
    			System.out.println("" + lacuenta);
    		}
    	}
    	if(!product.getParentId().matches("^(S?[0-9]+)$")) {
    		java.util.List<Product> childProducts = product.getProducts();
    		String calculatedWFAtt = null;
        	String fotoTomadaLiverpool = null;
        	String business = null;
        	String[] bundle = null;
        	String prevStatus = null;
        	String currentStatus = null;
        	String externalStatus = null;
        	java.util.List<Value> values = product.getValues();
        	java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
    		Value calculatedWFAttVal = valMap.get("CalculatedWF_Att");
    		Value fotoTomadaLiverpoolVal = valMap.get("FotoTomadaLiverpool");
    		Value negocio = valMap.get("Negocio");
    		Value extwgS4h = valMap.get("EXTWG_S4H");
    		Value firstDateApprove = valMap.get("FirstDateApprove");
    		Value supplierPartNumber = valMap.get("SupplierPartNumber");
    		Value stateSKU = valMap.get("StateSKU");
    		
    		String firstdateapproveStr = firstDateApprove == null ? "" : firstDateApprove.getId() != null ? firstDateApprove.getId() : firstDateApprove.getText() == null ? "" : firstDateApprove.getText() ;
    		    		
    		calculatedWFAtt = calculatedWFAttVal == null ? "" : calculatedWFAttVal.getId() == null ? calculatedWFAttVal.getText() == null ? "" : calculatedWFAttVal.getText() : calculatedWFAttVal.getId();
    		fotoTomadaLiverpool = fotoTomadaLiverpoolVal == null ? "" : fotoTomadaLiverpoolVal.getId() == null ? fotoTomadaLiverpoolVal.getText() == null ? "" : fotoTomadaLiverpoolVal.getText() : fotoTomadaLiverpoolVal.getId();
    		bundle = elese.computeStatus(calculatedWFAtt, !"".equals(firstdateapproveStr) ? "Aprobado" : nvl(stateSKU), fotoTomadaLiverpool, product.getId());
    		currentStatus = getStatusLabel( bundle[0] );
    		prevStatus = getStatusLabel( bundle[1] );
    		externalStatus = bundle[0] == null || "".equals(bundle[0]) ? "" : internalToExternalStatusMap.get(bundle[0]);
    		business = determineBusiness(negocio == null || negocio.getText() == null ? "" : negocio.getText(), extwgS4h == null || extwgS4h.getText() == null ? "" : extwgS4h.getText());
    		String[] bundle2 = new String[] { nvlById(negocio), nvlById(extwgS4h), nvl(supplierPartNumber), prevStatus, currentStatus, externalStatus, business };
    		gens.put(product.getId(), bundle2);
    		if(childProducts != null && !childProducts.isEmpty()) {
	    		for(Product cp : childProducts) {
	    			processVariantData(cp, bundle2);
	    		}
    		}else {
    			if(product.getUserTypeId().startsWith("SalesItemFamily")) {
    				lacuentaPadresSinVariantes++;
    			}else {
    				processVariantData(product, bundle2);
    			}
    		}
    		return;
    	}
//    	processVariantData(product);
    	sueltos.add(product);
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
    
    private String nvl(Value v) {
    	return v == null ? "" : v.getText();
    }
    
    private String nvlById(Value v) {
    	return v == null ? "" : v.getId() == null ? v.getText() == null ? "" : v.getText() : v.getId();
    }
    
	private String determineBusiness(String negocio, String extwgS4h) {
		return     "".equals(negocio) 
				&& "".equals(extwgS4h) ? null : 
					("".equals(negocio) && !"".equals(extwgS4h) ? "SBB": "ART. MARKETPLACE".equals(negocio) ? "MKP" : "LVP" );
	}
    
    private void processVariantData(Product product, String[] bundle) throws IOException {
    	String negocio = bundle[0];
    	String extwgs4h = bundle[1];
    	String supplierPartNumberN1 = bundle[2];
    	String prevStatus = bundle[3];
    	String currentStatus = bundle[4];
    	String externalStatus = bundle[5];
    	String business = bundle[6];
    	java.util.LinkedList<Value> values = null;
     	values = product.getValues();
     	if(values != null) {
     		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
     		for(Value value : values) {
     			valMap.put(value.getAttributeId(), value);
     		}
     		java.util.List<String> vals = new java.util.ArrayList<>();
     		Value mainbarcode = valMap.get("MainBarCode");
     		Value mainbarcodes4h = valMap.get("MainBarCodeS4H");
     		Value name = valMap.get("Name");
     		Value sku = valMap.get("SKU");
     		Value skucreationdate = valMap.get("SKUCreationDate");
     		Value supplierpartnumber = valMap.get("SupplierPartNumber");

     		Value productName = valMap.get("ProductName");
     		Value typeMainBarCode = valMap.get("TypeMainBarCode");
     		Value parentSKU = valMap.get("ParentSKU");

     		Value coloursLiverpoolAtt = valMap.get("ColoursLiverpoolAtt");
     		Value tamanoUnico = valMap.get("TamanoUnico");
     		Value sizeVaD = valMap.get("SizeVaD");
     		Value clothingSize = valMap.get("clothingSize");
     		Value variantSequence = valMap.get("variantSequence");
     		
     		String mainbarcodeStr = mainbarcode == null ? "" : mainbarcode.getId() != null ? mainbarcode.getId() : mainbarcode.getText() == null ? "" : mainbarcode.getText() ;
     		String mainbarcodes4hStr = mainbarcodes4h == null ? "" : mainbarcodes4h.getId() != null ? mainbarcodes4h.getId() : mainbarcodes4h.getText() == null ? "" : mainbarcodes4h.getText() ;
     		String nameStr = name == null ? "" : name.getId() != null ? name.getId() : name.getText() == null ? "" : name.getText() ;
     		String skuStr = sku == null ? "" : sku.getId() != null ? sku.getId() : sku.getText() == null ? "" : sku.getText() ;
     		String skucreationdateStr = skucreationdate == null ? "" : skucreationdate.getId() != null ? skucreationdate.getId() : skucreationdate.getText() == null ? "" : skucreationdate.getText() ;
     		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
     		nameStr = product.getName();
     		
     		vals.add( product.getId() );
     		vals.add( product.getParentId() );
     		
     		vals.add( flattenValue( coloursLiverpoolAtt == null ? "" : coloursLiverpoolAtt.getId() ));
     		vals.add( flattenValue( tamanoUnico == null ? "" : tamanoUnico.getId() ));
     		vals.add( flattenValue( sizeVaD == null ? "" : sizeVaD.getText() ));
     		vals.add( flattenValue( clothingSize == null ? "" : clothingSize.getText() ));
     		vals.add( flattenValue( variantSequence == null ? "" : variantSequence.getText() ));
     		vals.add( flattenValue( supplierpartnumberStr ));
     		vals.add( flattenValue( typeMainBarCode == null ? "" : typeMainBarCode.getId() ));
     		vals.add( flattenValue( mainbarcodeStr ));
     		vals.add( flattenValue( mainbarcodes4hStr ));
     		vals.add( flattenValue( nameStr ));
     		vals.add( flattenValue( skuStr ));
     		vals.add( flattenValue( skucreationdateStr ));
     		vals.add( flattenValue( product.getUserTypeId() ));
 			vals.add( flattenValue( productName == null ? "" : productName.getText() ));
 			vals.add( flattenValue( parentSKU == null ? "" : parentSKU.getText() ));
 			
 			vals.add( flattenValue( negocio ));
 			vals.add( flattenValue( extwgs4h ));
 			vals.add( flattenValue( supplierPartNumberN1 ));

 			vals.add( flattenValue( prevStatus ));
 			vals.add( flattenValue( currentStatus ));
 			vals.add( flattenValue( externalStatus ));
 			vals.add( flattenValue( business ));
     		
//     		ladata.put(product.getId(), vals.toArray(new String[] {}));
 			 bw.write(rw.getRw().serializeChunk(vals.toArray(new String[] {})));
             bw.write( "\r\n" );
     		lacuentaVars++;
     	}else {
     		log("MEP (" + product.getId() + ")");
     	}
    }
    
    private String flattenValue(String value){
        return value == null ? value : value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\r\\n");
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
	
	private static final String[] header = new String[] {
			 "Identifier"
			,"parentId"
			,"coloursLiverpoolAtt"
			,"tamanoUnico"
			,"sizeVaD"
			,"clothingSize"
			,"variantSequence"
			,"supplierPartNumber"
			,"typeMainBarcode"
			,"mainbarcode"
			,"mainbarcodes4h"
			,"name"
			,"sku"
			,"skucreationdate"
			,"objectType"
			,"productName"
			,"parentSKU"
			,"extwgs4h"
			,"negocio"
			,"supplierPartNumberN1"
			,"previousStatus"
			,"currentStatus"
			,"externalStatus"
			,"business"
	};
}
