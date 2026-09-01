package mx.com.liverpool.p360.services.core.temp.product2g.maintenance9;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class UploadSKUEAN {

	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final java.util.Map<String, String> qp = new java.util.HashMap<>();
	private static final RequestHandler rhS = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SKU")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
	private static final RequestHandler rhE = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.EAN")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );

	private static int count = 0;
	
	public static void main(String[] args) {
		qp.put("includeObjectsInProtocol", "false");
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( row -> {
			count++;
			if(count == 1 || row.length == 0) return;
			if("".equals(row[2])) {
				System.out.println( java.util.Arrays.asList(row) );
				return;
			}
			if(!"".equals(row[0]))
				rhE.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[2] + "'@1")).put("values", new org.json.JSONArray().put(row[0])));
			if(!"".equals(row[1]))
				rhS.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[2] + "'@1")).put("values", new org.json.JSONArray().put(row[1])));
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		rhS.sendData();
		rhE.sendData();
	}
	
}
