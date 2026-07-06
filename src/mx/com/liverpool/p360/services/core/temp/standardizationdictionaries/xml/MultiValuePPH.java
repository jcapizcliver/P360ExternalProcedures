package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml;

public class MultiValuePPH {

	private String attributeId;
	private java.util.LinkedList<ValuePPH> values = new java.util.LinkedList<>();
	private ValuePPH currentValue;
	
	public ValuePPH getCurrentValue() {
		return this.currentValue;
	}
	
	public void addValue() {
		if(this.currentValue != null) {
			this.values.addLast(this.currentValue);
			this.currentValue = null;
		}
	}
	
	public void setCurrentValue(ValuePPH currentValue) {
		if(this.currentValue != null) {
			addValue();
		}
		this.currentValue = currentValue;
	}
	
	public String getAttributeId() {
		return attributeId;
	}
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}
	public java.util.LinkedList<ValuePPH> getValues() {
		return values;
	}
	
}
