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

public class LoadProductDataSecondOpinionForFlat {

	private RESTWrapper rw = new RESTWrapper();
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	public static boolean sendProduct = true;
	public static boolean sendLkpValues = false;
	private java.nio.file.Path normalLogFilePath = java.nio.file.Paths.get("..", "logs", "list_for_flat.log");
	
	java.util.zip.ZipOutputStream zos = null;
    private java.io.BufferedWriter bw = null;
    
    public LoadProductDataSecondOpinionForFlat(String baseFile) throws IOException {
    	java.nio.file.Path zipPath = java.nio.file.Paths.get( baseFile );
    	java.io.FileOutputStream fos = new java.io.FileOutputStream(zipPath.toFile());
        java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos);
        zos = new java.util.zip.ZipOutputStream(bos);
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("RobustSTEPDataFlattened.csv");
        zos.putNextEntry(entry);
        bw = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(zos, java.nio.charset.StandardCharsets.UTF_8)
        	    );
        bw.write(rw.getRw().serializeChunk(header));
        bw.write("\r\n");
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
    	LoadProductDataSecondOpinionForFlat an = new LoadProductDataSecondOpinionForFlat( args.length == 1 ? "RobustSTEPDataFlattened.zip" : args[1] );
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        an.loadItemGroups();
        an.procesaDirectorio(args[0], parser);
        
        System.out.println("lacuenta: " + an.lacuenta + ") || " + an.ids.size() + " (" + new java.util.TreeSet<>(an.ids).size() + ")");
        try {

            an.bw.flush();
            an.zos.closeEntry();
            an.bw.close();
        } catch (java.io.IOException e) {
            an.logE(e);
        }
        for(String id : an.ids) {
        	an.log(id);
        }
        an.log("Total products found: " + an.lacuenta);
        an.log("Total vars found: " + an.lacuentaVars);
        an.log("Done. " + an.rw.getRw().formatTime(System.currentTimeMillis() - init) );
        an.eleseFinish();
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
    
    private PruebaEnvioPubSubMediaAssets elese = new PruebaEnvioPubSubMediaAssets();
    private int lacuenta = 0;
    private int lacuentaVars = 0;
    private final java.util.Map<String, String> internalToExternalStatusMap = loadExternalStatusMap();
    private final java.util.List<String> ids = new java.util.ArrayList<>();
    
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

    private void processProduct(Product product) throws IOException {
    	if(product.getParentId().matches("^(S?[0-9]+)$")) {
    		return;
    	}
    	ids.add(product.getId());
        java.util.LinkedList<Value> values = null;
    	values = product.getValues();
    	String calculatedWFAtt = null;
    	String fotoTomadaLiverpool = null;
    	String business = null;
    	String[] bundle = null;
    	String prevStatus = null;
    	String currentStatus = null;
    	String externalStatus = null;
    	String enriquecidoEnForo = null;
    	if(values != null) {
    		java.util.Map<String, Value> valMap = new java.util.TreeMap<>();
    		for(Value value : values) {
    			valMap.put(value.getAttributeId(), value);
    		}
    		Value calculatedWFAttVal = valMap.get("CalculatedWF_Att");
    		Value fotoTomadaLiverpoolVal = valMap.get("FotoTomadaLiverpool");
    		
    		calculatedWFAtt = calculatedWFAttVal == null ? "" : calculatedWFAttVal.getId() == null ? calculatedWFAttVal.getText() == null ? "" : calculatedWFAttVal.getText() : calculatedWFAttVal.getId();
    		fotoTomadaLiverpool = fotoTomadaLiverpoolVal == null ? "" : fotoTomadaLiverpoolVal.getId() == null ? fotoTomadaLiverpoolVal.getText() == null ? "" : fotoTomadaLiverpoolVal.getText() : fotoTomadaLiverpoolVal.getId();
    		java.util.List<String> vals = new java.util.ArrayList<>();
    		Value approveddatecalc = valMap.get("ApprovedDateCalc");
    		Value brandIdS4h = valMap.get("BRAND_ID_S4H");
    		Value brandidatg = valMap.get("BrandIDATG");
    		Value brandname = valMap.get("BrandName");
    		Value brandNameATG = valMap.get("BrandNameATG");
    		Value brandowner = valMap.get("BrandOwner");
    		Value calculatedwfAtt = valMap.get("CalculatedWF_Att");
    		Value calculatedInstatewf = valMap.get("Calculated_inStateWF");
    		Value direction = valMap.get("Direction");
    		Value extwgS4h = valMap.get("EXTWG_S4H");
    		Value fiberCode1 = valMap.get("FIBER_CODE1");
    		Value fiberCode2 = valMap.get("FIBER_CODE2");
    		Value fiberCode3 = valMap.get("FIBER_CODE3");
    		Value fiberCode4 = valMap.get("FIBER_CODE4");
    		Value fiberCode5 = valMap.get("FIBER_CODE5");
    		Value fiberCodeDescr1 = valMap.get("FIBER_CODE_DESCR1");
    		Value fiberCodeDescr2 = valMap.get("FIBER_CODE_DESCR2");
    		Value fiberCodeDescr3 = valMap.get("FIBER_CODE_DESCR3");
    		Value fiberCodeDescr4 = valMap.get("FIBER_CODE_DESCR4");
    		Value fiberCodeDescr5 = valMap.get("FIBER_CODE_DESCR5");
    		Value fiberPart1 = valMap.get("FIBER_PART1");
    		Value fiberPart2 = valMap.get("FIBER_PART2");
    		Value fiberPart3 = valMap.get("FIBER_PART3");
    		Value fiberPart4 = valMap.get("FIBER_PART4");
    		Value fiberPart5 = valMap.get("FIBER_PART5");
    		Value fshId = valMap.get("FSH_ID");
    		Value firstDateApprove = valMap.get("FirstDateApprove");
    		Value fototomadaliverpool = valMap.get("FotoTomadaLiverpool");
    		Value identificanegocio = valMap.get("IdentificaNegocio");
    		Value itemgroup = valMap.get("ItemGroup");
    		Value itemgroup2 = valMap.get("ItemGroup2");
    		Value itemgroups4h = valMap.get("ItemGroupS4H");
    		Value maxStack = valMap.get("MAX_STACK");
    		Value mtartS4h = valMap.get("MTART_S4H");
    		Value mvgr2 = valMap.get("MVGR2");
    		Value mvgr5 = valMap.get("MVGR5");
    		Value mainbarcode = valMap.get("MainBarCode");
    		Value mainbarcodes4h = valMap.get("MainBarCodeS4H");
    		Value margen = valMap.get("Margen");
    		Value margenS4h = valMap.get("MargenS4H");
    		Value materialAtt = valMap.get("MaterialAtt");
    		Value mesdeentregademercancia = valMap.get("MesdeEntregadeMercancIa");
    		Value name = valMap.get("Name");
    		Value negocio = valMap.get("Negocio");
    		Value parent = valMap.get("Parent");
    		Value parentid = valMap.get("ParentID");
    		Value producttype = valMap.get("ProductType");
    		Value producttypesap = valMap.get("ProductTypeSAP");
    		Value producttypesap2 = valMap.get("ProductTypeSAP2");
    		Value producttypesaptemp = valMap.get("ProductTypeSAPTEMP");
    		Value producttypesaptempsbb = valMap.get("ProductTypeSAPTEMPSBB");
    		Value producto = valMap.get("Producto");
    		Value sapobjecttype = valMap.get("SAPObjectType");
    		Value sapspart = valMap.get("SAPSpart");
    		Value sapBehvo = valMap.get("SAP_BEHVO");
    		Value sb0002 = valMap.get("SB_0002");
    		Value sbColores = valMap.get("SB_COLORES");
    		Value sbTHardline = valMap.get("SB_T_HARDLINE");
    		Value sku = valMap.get("SKU");
    		Value skucreationdate = valMap.get("SKUCreationDate");
    		Value section = valMap.get("Section");
    		Value skutype = valMap.get("SkuType");
    		Value statesku = valMap.get("StateSKU");
    		Value statusoutwf = valMap.get("StatusOutWF");
    		Value supplierid = valMap.get("SupplierID");
    		Value suppliername = valMap.get("SupplierName");
    		Value supplierpartnumber = valMap.get("SupplierPartNumber");
    		Value video = valMap.get("Video");
    		Value suppliershopid = valMap.get("supplierShopId");
    		Value acabadosVaD = valMap.get("AcabadosVaD");
    		Value altoVaD = valMap.get("AltoVaD");
    		Value anchoVaD = valMap.get("AnchoVaD");

    		Value status = valMap.get("Status");
    		Value productName = valMap.get("ProductName");
    		Value isMarketPlace = valMap.get("isMarketPlace");
    		Value typeMainBarCode = valMap.get("TypeMainBarCode");
    		Value parentSKU = valMap.get("ParentSKU");
    		Value baseUnitOfMeasure = valMap.get("BaseUnitOfMeasure");
    		
    		Value lastDateApprove = valMap.get("LastDateApprove");
    		Value creationDateCalc = valMap.get("CreationDateCalc");
    		
    		Value dutyfreekey = valMap.get("DutyFreeKey");
    		
    		String approveddatecalcStr = approveddatecalc == null ? "" : approveddatecalc.getId() != null ? approveddatecalc.getId() : approveddatecalc.getText() == null ? "" : approveddatecalc.getText() ;
    		String brandIdS4hStr = brandIdS4h == null ? "" : brandIdS4h.getId() != null ? brandIdS4h.getId() : brandIdS4h.getText() == null ? "" : brandIdS4h.getText() ;
    		String brandidatgStr = brandidatg == null ? "" : brandidatg.getId() != null ? brandidatg.getId() : brandidatg.getText() == null ? "" : brandidatg.getText() ;
    		String brandnameStr = brandname == null ? "" : brandname.getId() != null ? brandname.getId() : brandname.getText() == null ? "" : brandname.getText() ;
    		String brandownerStr = brandowner == null ? "" : brandowner.getId() != null ? brandowner.getId() : brandowner.getText() == null ? "" : brandowner.getText() ;
    		String calculatedwfAttStr = calculatedwfAtt == null ? "" : calculatedwfAtt.getId() != null ? calculatedwfAtt.getId() : calculatedwfAtt.getText() == null ? "" : calculatedwfAtt.getText() ;
    		String calculatedInstatewfStr = calculatedInstatewf == null ? "" : calculatedInstatewf.getId() != null ? calculatedInstatewf.getId() : calculatedInstatewf.getText() == null ? "" : calculatedInstatewf.getText() ;
    		String directionStr = direction == null ? "" : direction.getId() != null ? direction.getId() : direction.getText() == null ? "" : direction.getText() ;
    		String extwgS4hStr = extwgS4h == null ? "" : extwgS4h.getId() != null ? extwgS4h.getId() : extwgS4h.getText() == null ? "" : extwgS4h.getText() ;
    		String fiberCode1Str = fiberCode1 == null ? "" : fiberCode1.getId() != null ? fiberCode1.getId() : fiberCode1.getText() == null ? "" : fiberCode1.getText() ;
    		String fiberCode2Str = fiberCode2 == null ? "" : fiberCode2.getId() != null ? fiberCode2.getId() : fiberCode2.getText() == null ? "" : fiberCode2.getText() ;
    		String fiberCode3Str = fiberCode3 == null ? "" : fiberCode3.getId() != null ? fiberCode3.getId() : fiberCode3.getText() == null ? "" : fiberCode3.getText() ;
    		String fiberCode4Str = fiberCode4 == null ? "" : fiberCode4.getId() != null ? fiberCode4.getId() : fiberCode4.getText() == null ? "" : fiberCode4.getText() ;
    		String fiberCode5Str = fiberCode5 == null ? "" : fiberCode5.getId() != null ? fiberCode5.getId() : fiberCode5.getText() == null ? "" : fiberCode5.getText() ;
    		String fiberCodeDescr1Str = fiberCodeDescr1 == null ? "" : fiberCodeDescr1.getId() != null ? fiberCodeDescr1.getId() : fiberCodeDescr1.getText() == null ? "" : fiberCodeDescr1.getText() ;
    		String fiberCodeDescr2Str = fiberCodeDescr2 == null ? "" : fiberCodeDescr2.getId() != null ? fiberCodeDescr2.getId() : fiberCodeDescr2.getText() == null ? "" : fiberCodeDescr2.getText() ;
    		String fiberCodeDescr3Str = fiberCodeDescr3 == null ? "" : fiberCodeDescr3.getId() != null ? fiberCodeDescr3.getId() : fiberCodeDescr3.getText() == null ? "" : fiberCodeDescr3.getText() ;
    		String fiberCodeDescr4Str = fiberCodeDescr4 == null ? "" : fiberCodeDescr4.getId() != null ? fiberCodeDescr4.getId() : fiberCodeDescr4.getText() == null ? "" : fiberCodeDescr4.getText() ;
    		String fiberCodeDescr5Str = fiberCodeDescr5 == null ? "" : fiberCodeDescr5.getId() != null ? fiberCodeDescr5.getId() : fiberCodeDescr5.getText() == null ? "" : fiberCodeDescr5.getText() ;
    		String fiberPart1Str = fiberPart1 == null ? "" : fiberPart1.getId() != null ? fiberPart1.getId() : fiberPart1.getText() == null ? "" : fiberPart1.getText() ;
    		String fiberPart2Str = fiberPart2 == null ? "" : fiberPart2.getId() != null ? fiberPart2.getId() : fiberPart2.getText() == null ? "" : fiberPart2.getText() ;
    		String fiberPart3Str = fiberPart3 == null ? "" : fiberPart3.getId() != null ? fiberPart3.getId() : fiberPart3.getText() == null ? "" : fiberPart3.getText() ;
    		String fiberPart4Str = fiberPart4 == null ? "" : fiberPart4.getId() != null ? fiberPart4.getId() : fiberPart4.getText() == null ? "" : fiberPart4.getText() ;
    		String fiberPart5Str = fiberPart5 == null ? "" : fiberPart5.getId() != null ? fiberPart5.getId() : fiberPart5.getText() == null ? "" : fiberPart5.getText() ;
    		String fshIdStr = fshId == null ? "" : fshId.getId() != null ? fshId.getId() : fshId.getText() == null ? "" : fshId.getText() ;
    		String firstdateapproveStr = firstDateApprove == null ? "" : firstDateApprove.getId() != null ? firstDateApprove.getId() : firstDateApprove.getText() == null ? "" : firstDateApprove.getText() ;
    		String fototomadaliverpoolStr = fototomadaliverpool == null ? "" : fototomadaliverpool.getId() != null ? fototomadaliverpool.getId() : fototomadaliverpool.getText() == null ? "" : fototomadaliverpool.getText() ;
    		String identificanegocioStr = identificanegocio == null ? "" : identificanegocio.getId() != null ? identificanegocio.getId() : identificanegocio.getText() == null ? "" : identificanegocio.getText() ;
    		String itemgroupStr = itemgroup == null ? "" : itemgroup.getId() != null ? itemgroup.getId() : itemgroup.getText() == null ? "" : itemgroup.getText() ;
    		String itemgroup2Str = itemgroup2 == null ? "" : itemgroup2.getId() != null ? itemgroup2.getId() : itemgroup2.getText() == null ? "" : itemgroup2.getText() ;
    		String itemgroups4hStr = itemgroups4h == null ? "" : itemgroups4h.getId() != null ? itemgroups4h.getId() : itemgroups4h.getText() == null ? "" : itemgroups4h.getText() ;
    		String materialAttStr = materialAtt == null ? "" : materialAtt.getId() != null ? materialAtt.getId() : materialAtt.getText() == null ? "" : materialAtt.getText() ;
    		String maxStackStr = maxStack == null ? "" : maxStack.getId() != null ? maxStack.getId() : maxStack.getText() == null ? "" : maxStack.getText() ;
    		String mtartS4hStr = mtartS4h == null ? "" : mtartS4h.getId() != null ? mtartS4h.getId() : mtartS4h.getText() == null ? "" : mtartS4h.getText() ;
    		String mvgr2Str = mvgr2 == null ? "" : mvgr2.getId() != null ? mvgr2.getId() : mvgr2.getText() == null ? "" : mvgr2.getText() ;
    		String mvgr5Str = mvgr5 == null ? "" : mvgr5.getId() != null ? mvgr5.getId() : mvgr5.getText() == null ? "" : mvgr5.getText() ;
    		String mainbarcodeStr = mainbarcode == null ? "" : mainbarcode.getId() != null ? mainbarcode.getId() : mainbarcode.getText() == null ? "" : mainbarcode.getText() ;
    		String mainbarcodes4hStr = mainbarcodes4h == null ? "" : mainbarcodes4h.getId() != null ? mainbarcodes4h.getId() : mainbarcodes4h.getText() == null ? "" : mainbarcodes4h.getText() ;
    		String margenStr = margen == null ? "" : margen.getId() != null ? margen.getId() : margen.getText() == null ? "" : margen.getText() ;
    		String margenS4hStr = margenS4h == null ? "" : margenS4h.getId() != null ? margenS4h.getId() : margenS4h.getText() == null ? "" : margenS4h.getText() ;
    		try {
    			Integer.parseInt(margenStr);
    		}catch(NumberFormatException e) {
    			margenStr = "";
    		}
    		try {
    			Integer.parseInt(margenS4hStr);
    		}catch(NumberFormatException e) {
    			margenS4hStr = "";
    		}
    		String mesdeentregademercanciaStr = mesdeentregademercancia == null ? "" : mesdeentregademercancia.getId() != null ? mesdeentregademercancia.getId() : mesdeentregademercancia.getText() == null ? "" : mesdeentregademercancia.getText() ;
    		String nameStr = name == null ? "" : name.getId() != null ? name.getId() : name.getText() == null ? "" : name.getText() ;
    		String negocioStr = negocio == null ? "" : negocio.getId() != null ? negocio.getId() : negocio.getText() == null ? "" : negocio.getText() ;
    		String parentStr = parent == null ? "" : parent.getId() != null ? parent.getId() : parent.getText() == null ? "" : parent.getText() ;
    		String parentidStr = parentid == null ? "" : parentid.getId() != null ? parentid.getId() : parentid.getText() == null ? "" : parentid.getText() ;
    		String producttypeStr = producttype == null ? "" : producttype.getId() != null ? producttype.getId() : producttype.getText() == null ? "" : producttype.getText() ;
    		String producttypesapStr = producttypesap == null ? "" : producttypesap.getId() != null ? producttypesap.getId() : producttypesap.getText() == null ? "" : producttypesap.getText() ;
    		String producttypesap2Str = producttypesap2 == null ? "" : producttypesap2.getId() != null ? producttypesap2.getId() : producttypesap2.getText() == null ? "" : producttypesap2.getText() ;
    		String producttypesaptempStr = producttypesaptemp == null ? "" : producttypesaptemp.getId() != null ? producttypesaptemp.getId() : producttypesaptemp.getText() == null ? "" : producttypesaptemp.getText() ;
    		String producttypesaptempsbbStr = producttypesaptempsbb == null ? "" : producttypesaptempsbb.getId() != null ? producttypesaptempsbb.getId() : producttypesaptempsbb.getText() == null ? "" : producttypesaptempsbb.getText() ;
    		String productoStr = producto == null ? "" : producto.getId() != null ? producto.getId() : producto.getText() == null ? "" : producto.getText() ;
    		String sapobjecttypeStr = sapobjecttype == null ? "" : sapobjecttype.getId() != null ? sapobjecttype.getId() : sapobjecttype.getText() == null ? "" : sapobjecttype.getText() ;
    		String sapspartStr = sapspart == null ? "" : sapspart.getId() != null ? sapspart.getId() : sapspart.getText() == null ? "" : sapspart.getText() ;
    		String sapBehvoStr = sapBehvo == null ? "" : sapBehvo.getId() != null ? sapBehvo.getId() : sapBehvo.getText() == null ? "" : sapBehvo.getText() ;
    		String sb0002Str = sb0002 == null ? "" : sb0002.getId() != null ? sb0002.getId() : sb0002.getText() == null ? "" : sb0002.getText() ;
    		String sbColoresStr = sbColores == null ? "" : sbColores.getId() != null ? sbColores.getId() : sbColores.getText() == null ? "" : sbColores.getText() ;
    		String sbTHardlineStr = sbTHardline == null ? "" : sbTHardline.getId() != null ? sbTHardline.getId() : sbTHardline.getText() == null ? "" : sbTHardline.getText() ;
    		String skuStr = sku == null ? "" : sku.getText() == null ? "" : sku.getText() ;
    		String skucreationdateStr = skucreationdate == null ? "" : skucreationdate.getId() != null ? skucreationdate.getId() : skucreationdate.getText() == null ? "" : skucreationdate.getText() ;
    		String sectionStr = section == null ? "" : section.getId() != null ? section.getId() : section.getText() == null ? "" : section.getText() ;
    		String skutypeStr = skutype == null ? "" : skutype.getId() != null ? skutype.getId() : skutype.getText() == null ? "" : skutype.getText() ;
    		String stateskuStr = statesku == null ? "" : statesku.getText() == null ? "" : statesku.getText() ;
    		String statusoutwfStr = statusoutwf == null ? "" : statusoutwf.getId() != null ? statusoutwf.getId() : statusoutwf.getText() == null ? "" : statusoutwf.getText() ;
    		String supplieridStr = supplierid == null ? "" : supplierid.getId() != null ? supplierid.getId() : supplierid.getText() == null ? "" : supplierid.getText() ;
    		String suppliernameStr = suppliername == null ? "" : suppliername.getId() != null ? suppliername.getId() : suppliername.getText() == null ? "" : suppliername.getText() ;
    		String supplierpartnumberStr = supplierpartnumber == null ? "" : supplierpartnumber.getId() != null ? supplierpartnumber.getId() : supplierpartnumber.getText() == null ? "" : supplierpartnumber.getText() ;
    		String videoStr = video == null ? "" : video.getId() != null ? video.getId() : video.getText() == null ? "" : video.getText() ;
    		String suppliershopidStr = suppliershopid == null ? "" : suppliershopid.getId() != null ? suppliershopid.getId() : suppliershopid.getText() == null ? "" : suppliershopid.getText() ;
    		String acabadosStr = acabadosVaD == null ? "" : acabadosVaD.getId() != null ? acabadosVaD.getId() : acabadosVaD.getText() == null ? "" : acabadosVaD.getText() ;
    		String altoVaDStr = altoVaD == null ? "" : altoVaD.getId() != null ? altoVaD.getId() : altoVaD.getText() == null ? "" : altoVaD.getText() ;
    		String anchoVaDStr = anchoVaD == null ? "" : anchoVaD.getId() != null ? anchoVaD.getId() : anchoVaD.getText() == null ? "" : anchoVaD.getText() ;
    		String dutyfreekeyStr = dutyfreekey == null ? "" : dutyfreekey.getId() != null ? dutyfreekey.getId() : dutyfreekey.getText() == null ? "" : dutyfreekey.getText() ;
    		business = determineBusiness(negocio == null || negocio.getText() == null ? "" : negocio.getText(), extwgS4h == null || extwgS4h.getText() == null ? "" : extwgS4h.getText());
    		nameStr = product.getName();
    		
    		bundle = elese.computeStatus(calculatedWFAtt, !"".equals(firstdateapproveStr) ? "Aprobado" : stateskuStr, fotoTomadaLiverpool, product.getId());
    		currentStatus = getStatusLabel( bundle[0] );
    		prevStatus = getStatusLabel( bundle[1] );
    		enriquecidoEnForo = bundle[2];
    		externalStatus = bundle[0] == null || "".equals(bundle[0]) ? "" : internalToExternalStatusMap.get(bundle[0]);
    		
    		vals.add( product.getId() );
    		vals.add( currentStatus == null || "".equals(currentStatus) ? "" : currentStatus );
    		vals.add( prevStatus == null || "".equals(prevStatus) ? "" : prevStatus );
    		vals.add( externalStatus );
    		vals.add(  product.getParentId() );
    		vals.add(enriquecidoEnForo);
    		vals.add(approveddatecalcStr);
    		vals.add( flattenValue( brandIdS4hStr ));
    		vals.add( flattenValue( brandidatgStr ));
    		vals.add( flattenValue( brandnameStr ));
    		vals.add( flattenValue( brandNameATG == null ? "" : brandNameATG.getText() ));
    		vals.add( flattenValue( brandownerStr ));
    		vals.add( flattenValue( business ));
    		vals.add( flattenValue( calculatedwfAttStr ));
    		vals.add( flattenValue( calculatedInstatewfStr ));
    		vals.add( flattenValue( directionStr ));
    		vals.add( flattenValue( extwgS4hStr ));
    		vals.add( flattenValue( fiberCode1Str ));
    		vals.add( flattenValue( fiberCode2Str ));
    		vals.add( flattenValue( fiberCode3Str ));
    		vals.add( flattenValue( fiberCode4Str ));
    		vals.add( flattenValue( fiberCode5Str ));
    		vals.add( flattenValue( fiberCodeDescr1Str ));
    		vals.add( flattenValue( fiberCodeDescr2Str ));
    		vals.add( flattenValue( fiberCodeDescr3Str ));
    		vals.add( flattenValue( fiberCodeDescr4Str ));
    		vals.add( flattenValue( fiberCodeDescr5Str ));
    		vals.add( flattenValue( fiberPart1Str ));
    		vals.add( flattenValue( fiberPart2Str ));
    		vals.add( flattenValue( fiberPart3Str ));
    		vals.add( flattenValue( fiberPart4Str ));
    		vals.add( flattenValue( fiberPart5Str ));
    		vals.add( flattenValue( fshIdStr ));
    		vals.add( flattenValue( firstdateapproveStr ));
    		vals.add( flattenValue( fototomadaliverpoolStr ));
    		vals.add( flattenValue( identificanegocioStr ));
    		vals.add( flattenValue( itemgroupStr ));
    		vals.add( flattenValue( itemgroup2Str ));
    		vals.add( flattenValue( itemgroups4hStr ));
    		vals.add( flattenValue( maxStackStr ));
    		vals.add( flattenValue( mtartS4hStr ));
    		vals.add( flattenValue( mvgr2Str ));
    		vals.add( flattenValue( mvgr5Str ));
    		vals.add( flattenValue( mainbarcodeStr ));
    		vals.add( flattenValue( mainbarcodes4hStr ));
    		vals.add( flattenValue( materialAttStr ));
    		vals.add( flattenValue( mesdeentregademercanciaStr ));
    		vals.add( flattenValue( nameStr ));
    		vals.add( flattenValue( negocioStr ));
    		vals.add( flattenValue( parentidStr ));
    		vals.add( flattenValue( producttypeStr ));
    		vals.add( flattenValue( producttypesapStr ));
    		vals.add( flattenValue( producttypesap2Str ));
    		vals.add( flattenValue( producttypesaptempStr ));
    		vals.add( flattenValue( producttypesaptempsbbStr ));
    		vals.add( flattenValue( sapobjecttypeStr ));
    		vals.add( flattenValue( sapspartStr ));
    		vals.add( flattenValue( sapBehvoStr ));
    		vals.add( flattenValue( sb0002Str ));
    		vals.add( flattenValue( sbColoresStr ));
    		vals.add( flattenValue( sbTHardlineStr ));
    		vals.add( flattenValue( skuStr ));
    		vals.add( flattenValue( skucreationdateStr ));
    		vals.add( flattenValue( sectionStr ));
    		vals.add( flattenValue( skutypeStr ));
    		vals.add( flattenValue( stateskuStr ));
    		vals.add( flattenValue( statusoutwfStr ));
    		vals.add( flattenValue( supplieridStr ));
    		vals.add( flattenValue( suppliernameStr ));
    		vals.add( flattenValue( supplierpartnumberStr ));
    		vals.add( flattenValue( videoStr ));
    		vals.add( flattenValue( suppliershopidStr ));
    		vals.add( flattenValue( dutyfreekeyStr ));
    		vals.add( flattenValue( acabadosStr ));
    		vals.add( flattenValue( altoVaDStr ));
    		vals.add( flattenValue( anchoVaDStr ));

    		vals.add( flattenValue( product.getUserTypeId() ));
			vals.add( flattenValue( status == null ? "" : status.getId() == null ? status.getText() : status.getId() ));
			vals.add( flattenValue( productName == null ? "" : productName.getText() ));
			vals.add( flattenValue( isMarketPlace == null ? "" : isMarketPlace.getId() ));
			vals.add( flattenValue( typeMainBarCode == null ? "" : typeMainBarCode.getId() ));
			vals.add( flattenValue( parentSKU == null ? "" : parentSKU.getText() ));
			vals.add( flattenValue( baseUnitOfMeasure == null ? "" : baseUnitOfMeasure.getText() ));
			
			vals.add( flattenValue( lastDateApprove == null ? "" : lastDateApprove.getText() ));
			vals.add( flattenValue( creationDateCalc == null ? "" : creationDateCalc.getText() ));
			
    		vals.add( collectClassifications(product.getClassifications(), product.getId()) );
    		vals.add( collectImageReferences(product.getImageReference()) );
    		vals.add( String.valueOf( product.getProducts() == null ? 0 : product.getProducts().size() ) );
    		
//    		ladata.put(product.getId(), vals.toArray(new String[] {}));
    		 bw.write(rw.getRw().serializeChunk(vals.toArray(new String[] {})));
             bw.write( "\r\n" );
    	}else {
    		log("MEP (" + product.getId() + ")");
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
    
    private String flattenValue(String value){
        return value == null ? value : value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\r\\n");
    }
    
    private String collectClassifications(java.util.List<Classification> classifications, String productId) {
    	String structureId = null;
    	String type = null;
    	int index = -1;
    	StringBuilder sb = new StringBuilder();
    	for(Classification classification : classifications) {
    		type = classification.getType();
    		if("WebsiteLink".equals(type) /* || type.startsWith("GALink") */) {
    			structureId = classification.getId();
    			if("GALink".equals(type)) {
    				index = java.util.Arrays.binarySearch(eccItemGroups, structureId);
    			}else if("GALink_S4H".equals(type)) {
    				index = java.util.Arrays.binarySearch(s4hItemGroups, structureId);
    			}else {
    				index = java.util.Arrays.binarySearch(webSites, structureId);
    			}
    			if(index > -1) {
    				sb.append(sb.length() == 0 ? "" : ",").append(classification.getId());
    			}
    		}
    	}
    	return sb.toString();
    }

    private String collectImageReferences(java.util.List<AssetCrossRef> imageReferences) {
    	StringBuilder sb = new StringBuilder();
    	for(AssetCrossRef cr : imageReferences) {
    		sb.append( sb.length() == 0 ? "" : "," ).append(cr.type).append(";").append(cr.id);
    	}
    	return sb.toString();
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
	
	private static final String[] header = new String[] {
			 "Identifier"
			,"currentStatus"
			,"prevStatus"
			,"externalStatus "
			,"parentId"
			,"enriquecidoEnForo"
			,"approveddatecalc"
			,"brandIdS4h"
			,"brandidatg"
			,"brandname"
			,"brandnameatg"
			,"brandowner"
			,"business"
			,"calculatedwfAtt"
			,"calculatedInstatewf"
			,"direction"
			,"extwgS4h"
			,"fiberCode1"
			,"fiberCode2"
			,"fiberCode3"
			,"fiberCode4"
			,"fiberCode5"
			,"fiberCodeDescr1"
			,"fiberCodeDescr2"
			,"fiberCodeDescr3"
			,"fiberCodeDescr4"
			,"fiberCodeDescr5"
			,"fiberPart1"
			,"fiberPart2"
			,"fiberPart3"
			,"fiberPart4"
			,"fiberPart5"
			,"fshId"
			,"firstDateApprove"
			,"fototomadaliverpool"
			,"identificanegocio"
			,"itemgroup"
			,"itemgroup2"
			,"itemgroups4h"
			,"maxStack"
			,"mtartS4h"
			,"mvgr2"
			,"mvgr5"
			,"mainbarcode"
			,"mainbarcodes4h"
			,"materialatt"
			,"mesdeentregademercancia"
			,"name"
			,"negocio"
			,"parentid"
			,"producttype"
			,"producttypesap"
			,"producttypesap2"
			,"producttypesaptemp"
			,"producttypesaptempsbb"
			,"sapobjecttype"
			,"sapspart"
			,"sapBehvo"
			,"sb0002"
			,"sbColores"
			,"sbTHardline"
			,"sku"
			,"skucreationdate"
			,"section"
			,"skutype"
			,"statesku"
			,"statusoutwf"
			,"supplierid"
			,"suppliername"
			,"supplierpartnumber"
			,"video"
			,"suppliershopid"
			,"dutyfreekey"
			,"AcabadosVaD"
			,"AltoVaD"
			,"AnchoVaD"
			
			,"objectType"
			,"status"
			,"productName"
			,"isMarketPlace"
			,"typeMainBarCode"
			,"parentSKU"
			,"baseUnitOfMeasure"
			,"lastDateApprove"
			,"creationDateCalc"

			,"catIDs"
			,"imageReferences"
			,"numChilds"
	};
}
