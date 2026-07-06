package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CalculaSKUGenMKP {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> data = new java.util.ArrayList<>();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Product2G.Business->LookupValue.Code = \"MKP\" and Product2G.ProductNo startsWith \"17546116\"");
//		qp.put("query", "Product2G.SKU = 9991754611");
//		qp.put("query", "Product2G.Business->LookupValue.Code = \"MKP\" and Product2G.SKU is empty");
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "5000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
//		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.SKU")), 2000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(!"LVP".startsWith(values.getString(0)))
				data.add(values.getString(0));
		});
		for(String a : data) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + a + "'@1")).put("values", new org.json.JSONArray().put( "999" + a.substring(a.length() == 16 ? 9 : 1, a.length()) )));
		}
		rh.sendData();
	}
	
}
