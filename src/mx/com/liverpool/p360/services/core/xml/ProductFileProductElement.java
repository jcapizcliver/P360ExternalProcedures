package mx.com.liverpool.p360.services.core.xml;

public class ProductFileProductElement {
	
	private String id;
	private String parentId;
	private String userTypeId;
	
	private java.util.Map<String, ProductFileValueElement> values = null;
	private java.util.Map<String, ProductFileMultiValueElement> multiValues = null;
	private ProductFileValueElement workingValue = null;
	private ProductFileMultiValueElement workingMultiValue = null;
	private java.util.Map<String, ProductFileProductElement> products = new java.util.HashMap<>();
	private java.util.Map<String, ProductFileClassificationElement> classifications = new java.util.HashMap<>();
	private ProductFileClassificationElement workingClassification = null;
	private java.util.Map<String, String> assetCrossReference = new java.util.HashMap<>();
	
	public ProductFileProductElement(String id, String parentId, String userTypeId) {
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

	public java.util.Map<String, ProductFileValueElement> getValues() {
		return values;
	}
	
	public java.util.Map<String, ProductFileProductElement> getProducts(){
		return products;
	}
	
	public java.util.Map<String, ProductFileMultiValueElement> getMultiValues(){
		return multiValues;
	}
	
	public java.util.Map<String, ProductFileClassificationElement> getClassifications(){
		return classifications;
	}
	
	public ProductFileValueElement getWorkingValue() {
		return this.workingValue;
	}
	
	public ProductFileMultiValueElement getWorkingMultiValue(){
		return this.workingMultiValue;
	}
	
	public ProductFileClassificationElement getWorkingClassification() {
		return this.workingClassification;
	}
	
	public void putAssetCrossReference(String assetId, String assetType) {
		assetCrossReference.put(assetId, assetType);
	}
	
	public java.util.Map<String, String> getAssetCrossReferences(){
		return assetCrossReference;
	}
	
	public void createList() {
		values = new java.util.HashMap<>();
	}
	
	public void createMultiValueList() {
		this.multiValues = new java.util.HashMap<>();
	}
	
	public void prepareValue(ProductFileValueElement value) {
		if(this.workingValue != null) {
			addValue();
		}
		this.workingValue = value;
	}
	
	public void prepareMultiValue(ProductFileMultiValueElement multiValues) {
		if(this.workingMultiValue != null) {
			addMultiValue();
		}
		this.workingMultiValue = multiValues;
	}
	
	public void prepareClassification(ProductFileClassificationElement classification) {
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
				this.values.put(workingValue.getAttributeId(), workingValue);
			}
			this.workingValue = null;
		}
	}
	
	public void addMultiValue() {
		if(workingMultiValue != null) {
			addValue();
			this.multiValues.put(workingMultiValue.getAttributeId(), workingMultiValue);
			this.workingMultiValue = null;
		}
	}
	
	public void addClassification() {
		if(workingClassification != null) {
			this.classifications.put(this.workingClassification.getId(), this.workingClassification);
			this.workingClassification = null;
		}
	}
	
	public void addProduct(ProductFileProductElement product) {
		this.products.put(product.getId(), product);
	}
	
}
