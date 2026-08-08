package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class Test {



	public static void main(String[] args) {
		RESTWrapper rw0 = new RESTWrapper();
		rw0.getRw().setBaseUrl("https://pro-api.liverpool.com.mx/api/cataloging/productmanagement/proposals");
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		rw0.getRw().addHeader("Content-Type", "application/json");
		rw0.getRw().addHeader("apikey", "66a831ee-d57d-49fa-a830-fa185323cb8f");
		rw0.getRw().removeHeader("Authorization");
		rw0.getRw().removeHeader("Accept");
		System.out.println(rw0.getRw().getRc().getHeader());
		System.out.println("Resp from del method: " + rw0.getRw().makeRequest("DELETE", "/upc-eans", qp, new org.json.JSONObject().put("upcEans", new org.json.JSONArray().put("883901181876")).toString()));
	}
	
}
