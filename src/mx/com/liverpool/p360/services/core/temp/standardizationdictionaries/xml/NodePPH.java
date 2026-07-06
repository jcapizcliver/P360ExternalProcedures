package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml;

public class NodePPH {
	
	private String id;
	private String parentId;
	private String userTypeId;
	private String name;
	
	private java.util.LinkedList<ValuePPH> values = new java.util.LinkedList<>();
	private java.util.LinkedList<MultiValuePPH> multiValues = new java.util.LinkedList<>();
	private java.util.LinkedList<AttributeLinkPPH> attributeLinks = new java.util.LinkedList<>();
	private java.util.LinkedList<NodePPH> productos = new java.util.LinkedList<>();
	
	private ValuePPH currentValue = null;
	private MultiValuePPH currentMultiValue = null;
	private AttributeLinkPPH currentAttributeLink = null;
	private NodePPH currentNode = null;
	
	public java.util.LinkedList<AttributeLinkPPH> getAttributeLinks(){
		return this.attributeLinks;
	}
	
	public ValuePPH getCurrentValue() {
		return this.currentValue;
	}
	
	public MultiValuePPH getCurrentMultiValue() {
		return this.currentMultiValue;
	}
	
	public AttributeLinkPPH getCurrentAttributeLink() {
		return currentAttributeLink;
	}
	
	public void addAttributeLink() {
		if(this.currentAttributeLink != null) {
			this.attributeLinks.addLast(this.currentAttributeLink);
			this.currentAttributeLink = null;
		}
	}
	
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	
	public String getParentId() {
		return parentId;
	}
	
	public void setCurrentAttributeLink(AttributeLinkPPH currentAttributeLink) {
		if(this.currentAttributeLink != null) {
			addAttributeLink();
		}
		this.currentAttributeLink = currentAttributeLink;
	}
	
	public void setCurrentValue(ValuePPH currentValue) {
		if(this.currentValue != null) {
			addValue();
		}
		this.currentValue = currentValue;
	}
	
	public void setCurrentMultiValue(MultiValuePPH currentMultiValue) {
		if(this.currentMultiValue != null) {
			addMultiValue();
		}
		this.currentMultiValue = currentMultiValue;
	}
	
	public void setCurrentNode(NodePPH currentNode) {
		if(this.currentNode != null) {
			addNode();
		}
		this.currentNode = currentNode;
	}
	
	public void addValue() {
		if(this.currentValue != null) {
			this.values.addLast(this.currentValue);
			this.currentValue = null;
		}
	}
	
	public void addMultiValue() {
		if(this.currentMultiValue != null) {
			if(this.currentValue != null) {
				addValue();
			}
			this.multiValues.addLast(this.currentMultiValue);
			this.currentMultiValue = null;
		}
	}
	
	public void addNode() {
		if(this.currentNode != null) {
			this.productos.addLast(this.currentNode);
			this.currentNode = null;
		}
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getUserTypeId() {
		return userTypeId;
	}
	
	public void setUserTypeId(String userTypeId) {
		this.userTypeId = userTypeId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public java.util.LinkedList<ValuePPH> getValues() {
		return values;
	}
	
	public java.util.LinkedList<MultiValuePPH> getMultiValues() {
		return multiValues;
	}
	
	public java.util.LinkedList<NodePPH> getProductos() {
		return productos;
	}

	
}
