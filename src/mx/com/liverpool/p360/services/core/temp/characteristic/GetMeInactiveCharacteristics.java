package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class GetMeInactiveCharacteristics extends RESTWrapper {

	
	
	public static void main(String[] args) {
		GetMeInactiveCharacteristics gm = new GetMeInactiveCharacteristics();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.IsActive = false");
		gm.collectData("list", "Characteristic", null, "bySearch", qp, System.out::println);
	}
	
}
