package mx.com.liverpool.p360.services.core.temp.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class FlattenSTEPXMLData {

    private class Value{
    	
    	private String attributeId;
    	private String id;
    	private String text;
    	
    	public Value(String attributeId, String id) {
    		this.attributeId = attributeId;
    		this.id = id;
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
	
    public class Product {
    	
    	private String id;
    	private String parentId;
    	private java.util.LinkedList<Value> values = null;
    	private java.util.LinkedList<MultiValue> multiValues = null;
    	private Value workingValue = null;
    	private MultiValue workingMultiValue = null;
    	private java.util.LinkedList<Product> products = new java.util.LinkedList<>();
    	
    	public Product(String id, String parentId) {
    		this.id = id;
    		this.parentId = parentId;
    	}

		public String getId() {
			return id;
		}

		public String getParentId() {
			return parentId;
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
		
		public Value getWorkingValue() {
			return this.workingValue;
		}
		
		public MultiValue getWorkingMultiValue(){
			return this.workingMultiValue;
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
		
		public void addProduct(Product product) {
			this.products.addLast(product);
		}
    	
    }

    public class Handler extends DefaultHandler {
    	
        private final java.util.LinkedList<Product> productStack = new java.util.LinkedList<>();
        private final java.util.List<Product> finished = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;
            if ("Product".equals(name)) {
                String id = attributes.getValue("ID");
                String parentId = attributes.getValue("ParentID");
                if(parentId == null && !productStack.isEmpty()) {
                	parentId = productStack.getLast().getId();
                }
                productStack.addLast(new Product(id, parentId));
            }else {
	            if (!productStack.isEmpty()) {
	                Product product = productStack.getLast();
	                if ("Values".equals(name)) {
	                    product.createList();
	                    product.createMultiValueList();
	                } else if (("Value".equals(name)) && product.getValues() != null) {
	                	String attributeId = attributes.getValue("AttributeID");
	                	String valueId = attributes.getValue("ID");
	                	Value value = new Value(attributeId, valueId);
	                	product.prepareValue(value);
	                } else if( "MultiValue".equals(name) && product.getMultiValues() != null ) {
	                	String attributeId = attributes.getValue("AttributeID");
	                	MultiValue multiValueList = new MultiValue(attributeId);
	                	product.prepareMultiValue(multiValueList);
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
                }else if ("Product".equals(name)) {
                	productStack.removeLast();
                	if(!productStack.isEmpty()) {
                		productStack.getLast().addProduct(product);
                	}else {
                		finished.add(product);
                	}
                }
            }
        }

        public List<Product> getFinished() {
            return finished;
        }
    }

    // Entry point
    public static void main(String[] args) throws Exception {
    	FlattenSTEPXMLData an = new FlattenSTEPXMLData();
        if (args.length == 0) {
            System.err.println("Usage: java ProductValuesSaxParser <file.xml>");
            System.exit(1);
        }
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        // Harden parser (avoid XXE)
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}

        SAXParser parser = factory.newSAXParser();
        java.io.File[] files = new java.io.File(args[0]).listFiles(ff -> ff.getName().endsWith("xml"));
        
        for(File input : files) {
        	System.out.println("Going with: " + input.getName());
	        Handler handler = an.new Handler();
	        parser.parse(input, handler);
	        for (Product product : handler.getFinished()) {
	        	processProduct(product, "");
	        }
        }
    }
    
    private static void processProduct(Product product, String offset) {
    	java.util.LinkedList<Product> children = null;
        java.util.LinkedList<Value> values = null;
        java.util.LinkedList<MultiValue> multiValues = null;
    	values = product.getValues();
    	multiValues = product.getMultiValues();
    	children = product.getProducts();
    	if(values != null) {
    		for(Value value : values) {
    		}
    	}
    	if(multiValues != null && !multiValues.isEmpty()) {
    		for(MultiValue mvs : multiValues) {
    			System.out.println(offset + "\tMULTIVALUE: " + mvs.getAttributeId() );
        		for(Value value : mvs.getValues()) {
        		}
    		}
    	}
    	if(children != null) {
    		for(Product p : children) {
    			processProduct(p, offset + "   ");
    		}
    	}
    }
    
	private static final java.util.Set<String> ofInterest = new java.util.TreeSet<>( java.util.Arrays.asList(new String[] {
			"Direction",
			"Section",
			"ItemGroup",
			"ItemGroupS4H",
			"SKU",
			"MainBarCode",
			"MainBarCodeS4H",
			"BrandName",
			"BRAND_ID_S4H",
			"SupplierPartNumber",
			"CalculatedWF_Att",
			"Path",
			"Negocio",
			"ParentSKU",
			"SkuType",
			"Name",
			"ProductName",
			"SupplierID",
			"SupplierName",
			"StateSKU",
			"SKUCreationDate",
			"LastDateApprove",
			"FirstDateApprove",
			"SAPObjectType"
	})
			);
}
