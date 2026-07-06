package mx.com.liverpool.p360.services.core.xml;

public class ProductFileValueElement {

	private String attributeId;
	private String id;
	private String text;
	private String unidadId;
	
	public ProductFileValueElement(String attributeId, String id, String unidadId) {
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
