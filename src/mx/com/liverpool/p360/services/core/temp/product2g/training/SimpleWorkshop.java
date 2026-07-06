package mx.com.liverpool.p360.services.core.temp.product2g.training;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SimpleWorkshop {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String args[]) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		
		// https://webctep360pro.liverpool.com.mx/rest/V2.0/list/Product2G/bySearch?query=Product2G.CurrentStatus = "Eliminada"&fields=Product2G.ProductNo,Product2G.SKU,Product2G.EAN,Product2G.CurrentStatus,Product2G.PrevStatus,Product2G.ExternalStatus
		qp.put("query", "Product2G.CurrentStatus = \"Eliminada\"");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2G.SKU"
				+ ",Product2G.EAN"
				+ ",Product2G.CurrentStatus"
				+ ",Product2G.PrevStatus"
				+ ",Product2G.ExternalStatus"
			);
		qp.put("pageSize", "1000");
		qp.put("metaData", "true");
		/*
		  
		 {
		  ...
		  "columns": []
		  "rows": [
		  	  {"object": {"id": "'176543434434352'@1"}, "values": ["", "", "", "", ""]}
		  	, 
		  ]
		 } 
		  
		 **/
		rw.collectData("list", "Product2G", null, "bySearch", qp, SimpleWorkshop::rowProcessor);
		
	}
	
	private static void rowProcessor(org.json.JSONObject row) {
		org.json.JSONArray values = row.getJSONArray("values");
		System.out.println( values );
	}
	
}
