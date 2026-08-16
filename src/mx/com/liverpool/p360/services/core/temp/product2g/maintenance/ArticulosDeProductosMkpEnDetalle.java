package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class ArticulosDeProductosMkpEnDetalle {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
//		qp.put("query", "characteristic('Business') = 'MKP'@'BusinessQualified' and Product2GLog.CreationDate(PIM) >= 2026-02-03T00:00:00 and Product2G.CurrentStatus = \"Creación de SKU\"");
		qp.put("query", "Product2G.ProductNo in ("
				+ "\"1754611668474120\""
				+ ",\"1754611668474170\""
				+ ",\"1754611668474235\""
				+ ",\"1754611668474242\""
				+ ",\"1754611668474285\""
				+ ",\"1754611668474290\""
				+ ",\"1754611668474345\""
//				+ "\"1754611668474120\""
//				+ ",\"1754611668474170\""
//				+ ",\"1754611668474235\""
//				+ ",\"1754611668474242\""
//				+ ",\"1754611668474285\""
//				+ ",\"1754611668474290\""
//				+ ",\"1754611668474345\""
				+ ")");
		
		qp.put("pageSize", "10000");
		qp.put("fields", "Product2G.ProductNo");
		java.util.List<String> lst = new java.util.ArrayList<>();
		java.util.List<String> pids = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			lst.add(row.getJSONObject("object").getString("id"));
			pids.add(row.getJSONArray("values").getString(0));
		});
		StringBuilder sb = new StringBuilder();
		for(String id : lst) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
		}
		qp.remove("query");
		qp.put("products", sb.toString());
		qp.put("fields", "Article.SupplierAID");
		java.util.List<String> extVarId = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			System.out.println(row.getJSONArray("values").getString(0));
			extVarId.add(row.getJSONArray("values").getString(0));
		});
		System.out.println("Now sending product data");
//		for(String id : pids) {
//			CliTest.enviaDataProducto(id);
//		}
		System.out.println("Now sending articles treatment...");
		try(DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
			
			@Override
			public void logE(Exception e) {
			}
			
			@Override
			public void log(String message) {
			}
		} )){
			DataRequestor dr = new DataRequestor(dastub);
			for(String id : extVarId) {
				System.out.println("On delete as variant as product (" + id + "): " + dr.retiraProducto(new org.json.JSONArray().put(id)));
			}
		}
	}
	
}
