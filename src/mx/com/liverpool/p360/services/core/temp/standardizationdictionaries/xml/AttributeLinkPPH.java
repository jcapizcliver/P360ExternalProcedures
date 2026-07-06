package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml;

public class AttributeLinkPPH {

	private String attributeId;
	private boolean mandatory = false;
	private java.util.LinkedList<ValuePPH> filterValues = new java.util.LinkedList<>();
	private java.util.LinkedList<ValuePPH> metaDataValues = new java.util.LinkedList<>();
	private boolean valueFilter = false;
	private boolean metaData = false;
	private ValuePPH currentValue = null;
	private ValuePPH currentMetaDataValue = null;
	
	public ValuePPH getCurrentMetaDataValue() {
		return this.currentMetaDataValue;
	}
	
	public void addMetaDataValue() {
		if(this.currentMetaDataValue != null) {
			metaDataValues.addLast(this.currentMetaDataValue);
			this.currentMetaDataValue = null;
		}
	}
	
	public void setCurrentMetaDataValue(ValuePPH currentMetaDataValue) {
		if(this.currentMetaDataValue != null) {
			addMetaDataValue();
		}
		this.currentMetaDataValue = currentMetaDataValue;
	}
	
	public boolean isMetaData() {
		return this.metaData;
	}
	
	public void setMetaData(boolean metaData) {
		this.metaData = metaData;
	}
	
	public boolean isValueFilter() {
		return this.valueFilter;
	}
	
	public void setValueFilter(boolean valueFilter) {
		this.valueFilter = valueFilter;
	}
	
	public ValuePPH getCurrentValue() {
		return this.currentValue;
	}
	
	public void addValue() {
		if(this.currentValue != null) {
			filterValues.addLast(this.currentValue);
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
	
	public boolean isMandatory() {
		return mandatory;
	}
	
	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
	
	public java.util.LinkedList<ValuePPH> getFilterValues() {
		return filterValues;
	}
	
	public java.util.LinkedList<ValuePPH> getMetaDataValues() {
		return metaDataValues;
	}
	
}
