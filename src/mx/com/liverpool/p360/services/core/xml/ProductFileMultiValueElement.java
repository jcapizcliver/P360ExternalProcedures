package mx.com.liverpool.p360.services.core.xml;

public class ProductFileMultiValueElement {

	
	private String attributeId;
	private java.util.LinkedList<ProductFileValueElement> values;
	
	public ProductFileMultiValueElement(String attributeId) {
		this.attributeId = attributeId;
		this.values = new java.util.LinkedList<>();
	}

	public String getAttributeId() {
		return attributeId;
	}
	
	public java.util.LinkedList<ProductFileValueElement> getValues(){
		return this.values;
	}
	
	public void addValue(ProductFileValueElement value) {
		this.values.addLast(value);
	}

}
