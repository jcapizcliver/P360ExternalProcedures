package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class UnalignProduct2GStatus {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("http://172.18.237.162:1512/rest/V2.0");
		rw.getRw().addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.PrevStatus,Product2G.CurrentStatus,Product2G.ExternalStatus->LookupValue.Code");
		qp.put("pageSize", "20000");
		java.util.List<org.json.JSONArray> data = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "withItem", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(values.getString(1).equals(values.getString(2)) && !"".equals(values.getString(1)))
				data.add(values);
		});
		data.forEach(d -> System.out.println(d.getString(0)));
	}
	
}
