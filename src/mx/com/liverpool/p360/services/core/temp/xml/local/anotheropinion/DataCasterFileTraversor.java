package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class DataCasterFileTraversor extends DefaultHandler {
	
	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	

	public class Asset{
		
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

    public class MultiValue{
    	
    	private java.util.LinkedList<Value> values;
    	
    	public MultiValue(String attributeId) {
    		this.values = new java.util.LinkedList<>();
    	}

		public void addValue(Value value) {
			this.values.addLast(value);
		}

    }
    
    public class Classification{
    	
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
    
    public void setProductsCounter(int productsCounter) {
    	this.productsCounter = 0;
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
    
    public void clear() {
    	finished.clear();
    	assetMap.clear();
    	attributeIDs.clear();
    	productStack.clear();
    	assetStack.clear();
    	productsCounter = 0;
    }
}
