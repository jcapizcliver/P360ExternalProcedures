package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EscribeIDs {

	
	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.CurrentStatus,Product2G.PrevStatus,Product2G.ExternalStatus->LookupValue.Code");
		qp.put("pageSize", "50000");
		try(
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "ProductNo").toFile()), java.nio.charset.StandardCharsets.UTF_8))
;			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "SupplierAID").toFile()), java.nio.charset.StandardCharsets.UTF_8))
		){
			rw.collectData("list", "Product2G", null, "withItem", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getString(1)))
					pw.println( values.getString(0) ); 
			});
			qp.put("fields", "Article.SupplierAID,Article.CurrentStatus,Article.PrevStatus,Article.ExternalStatus->LookupValue.Code");
			rw.collectData("list", "Article", null, "withProduct", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(!"".equals(values.getString(1)))
					pw2.println( values.getString(0) ); 
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
