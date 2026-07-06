package mx.com.liverpool.p360.services.core.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ProductFileHandler extends DefaultHandler{
	
    private final java.util.LinkedList<ProductFileProductElement> productStack = new java.util.LinkedList<>();
    private final java.util.List<ProductFileProductElement> finished = new ArrayList<>();
    private final java.util.Map<String, ProductFileAssetElement> assetMap = new java.util.TreeMap<>();
    private final java.util.LinkedList<ProductFileAssetElement> assetStack = new java.util.LinkedList<>();

	private java.util.Set<String> attributeIDs = new java.util.TreeSet<>();
	
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
            productStack.addLast(new ProductFileProductElement(id, parentId, userTypeId));
        }else if("Asset".equals(name)) {
        	String id = attributes.getValue("ID");
        	String userTypeId = attributes.getValue("UserTypeID");
        	ProductFileAssetElement a = new ProductFileAssetElement(id, userTypeId);
        	assetStack.addLast(a);
        } else {
            if (!productStack.isEmpty()) {
                ProductFileProductElement product = productStack.getLast();
                if ("Values".equals(name)) {
                    product.createList();
                    product.createMultiValueList();
                } else if("AssetCrossReference".equals(name)) {
                	product.putAssetCrossReference(attributes.getValue("AssetID"), attributes.getValue("Type"));
                } else if (("Value".equals(name)) && product.getValues() != null) {
                	String attributeId = attributes.getValue("AttributeID");
                	if(attributeId != null) {
                		attributeIDs.add(attributeId);
                	}
                	String valueId = attributes.getValue("ID");
                	String unidadId = attributes.getValue("UnitID");
                	ProductFileValueElement value = new ProductFileValueElement(attributeId, valueId, unidadId);
                	product.prepareValue(value);
                } else if( "MultiValue".equals(name) && product.getMultiValues() != null ) {
                	String attributeId = attributes.getValue("AttributeID");
                	if(attributeId != null) {
                		attributeIDs.add(attributeId);
                	}
                	ProductFileMultiValueElement multiValueList = new ProductFileMultiValueElement(attributeId);
                	product.prepareMultiValue(multiValueList);
                } else if("ClassificationReference".equals(name)) {
                	String classificationId = attributes.getValue("ClassificationID");
                	String type = attributes.getValue("Type");
                	ProductFileClassificationElement classification = new ProductFileClassificationElement(classificationId, type);
                	product.prepareClassification(classification);
                }
            }else if(!assetStack.isEmpty()) {
            	if("Name".equals(name)) {
            		assetName = true;
            	}else if("Value".equals(name)) {
            		ProductFileAssetElement asset = assetStack.getLast();
            		ProductFileValueElement v = new ProductFileValueElement(attributes.getValue("AttributeID"), null, null);
            		asset.setCurrentValue(v);
            	}
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (!productStack.isEmpty()) {
            ProductFileProductElement product = productStack.getLast();
            ProductFileValueElement workingValue = product.getWorkingValue();
            if(workingValue != null) {
            	workingValue.setText( new StringBuilder().append(ch, start, length).toString() );
            }
        }else if(!assetStack.isEmpty()) {
        	ProductFileAssetElement a = assetStack.getLast();
        	ProductFileValueElement wv = a.getCurrentValue();
        	if(wv != null) {
        		StringBuilder sb = new StringBuilder();
        		sb.append(wv.getText() == null ? "" : wv.getText());
        		sb.append(ch, start, length);
        		wv.setText( sb.toString() );
        	} else {
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
            ProductFileProductElement product = productStack.getLast();
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
        	ProductFileAssetElement a = assetStack.getLast();
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

    public List<ProductFileProductElement> getFinished() {
        return finished;
    }
    
    public java.util.Map<String, ProductFileAssetElement> getAssetMap(){
    	return assetMap;
    }


}
