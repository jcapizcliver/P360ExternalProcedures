package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class FixBusiness {

	
	private static final RESTWrapper rw = new RESTWrapper();
	private static int a = 0;
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.Business")), 5000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			a++;
			if(a == 1) {
				return;
			}
			if(row.length == 0) {
				return;
			}
			
			String businessClc = determineBusiness(row[6], row[8]);
			if(businessClc != null && !businessClc.equals(row[2])) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[0] + "'@1")).put("values", new org.json.JSONArray().put(businessClc)));
			}
//			if("S73190532".equals(row[0])) {
//				System.out.println(businessClc + "  --  " + java.util.Arrays.asList(row));
//			}
			
		});
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Business_20260723_231114.csv"));
		rh.sendData();
		
	}
    
	private static String determineBusiness(String negocio, String extwgS4h) {
		return     "".equals(negocio) 
				&& "".equals(extwgS4h) ? null : 
					(!"".equals(extwgS4h) ? "SBB": "ART. MARKETPLACE".equals(negocio) ? "MKP" : "LVP" );
	}
	
	
}
