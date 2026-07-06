package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SetSAPObjectType {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		rw.writeData("list", "Article", null, qp, 
				new org.json.JSONObject()
					.put("columns", 
						new org.json.JSONArray().put(
								new org.json.JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)")
							)
						)
					.put("rows", 
						new org.json.JSONArray()
							.put(new org.json.JSONObject()
									.put("object", new org.json.JSONObject().put("id", "'1754611668830165'@1"))
									.put("values", new org.json.JSONArray().put("00")) )
						)
					, System.out::println);
	}
	
}
