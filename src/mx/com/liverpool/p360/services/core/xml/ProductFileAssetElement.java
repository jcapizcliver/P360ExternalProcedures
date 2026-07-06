package mx.com.liverpool.p360.services.core.xml;

public class ProductFileAssetElement {
	
	private String id;
	private String name;
	private String userTypeId;
	
	private ProductFileValueElement currentValue = null;
	private java.util.Map<String, ProductFileValueElement> values = new java.util.HashMap<>();
	
	public ProductFileAssetElement(String id, String userTypeId) {
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

	public ProductFileValueElement getCurrentValue() {
		return currentValue;
	}

	public java.util.Map<String, ProductFileValueElement> getValues() {
		return values;
	}
	
	public void addValue() {
		if(currentValue != null) {
			values.put(currentValue.getAttributeId(), currentValue);
			currentValue = null;
		}
	}
	
	public void setCurrentValue(ProductFileValueElement currentValue) {
		this.currentValue = currentValue;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
}
