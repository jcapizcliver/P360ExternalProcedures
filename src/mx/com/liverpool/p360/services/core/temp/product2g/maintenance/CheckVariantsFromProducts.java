package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CheckVariantsFromProducts {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Article.SupplierAID");
		StringBuilder sb = new StringBuilder();
		java.util.List<String> ids = java.util.Arrays.asList("1754611668474120"
				, "1754611668474170"
				, "1754611668474235"
				, "1754611668474242"
				, "1754611668474285"
				, "1754611668474290"
				, "1754611668474345"
			);
		for(String id : ids) {
			sb.append(sb.length() == 0  ? "" : ",").append("'").append(id).append("'@1");
		}
		qp.put("products", sb.toString());
		qp.put("pageSize", "10000");
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			System.out.println(row.getJSONArray("values").getString(0));
		} );
	}
	
}
