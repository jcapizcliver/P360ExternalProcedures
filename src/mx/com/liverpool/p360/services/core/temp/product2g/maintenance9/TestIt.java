package mx.com.liverpool.p360.services.core.temp.product2g.maintenance9;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class TestIt {

	
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		String externalId = "1754611686549546";
		java.util.Map<String, String> qp00 = new java.util.HashMap<>();
		qp00.put("includeLabels", "true");
		qp00.put("includeIds", "true");
		qp00.put("entityFilter", "Product2GCharacteristicValue");
		qp00.put("qualificationFilter", "characteristic(SistemaOrigen)");
		org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
		System.out.println(jResp);
	}
	
}
