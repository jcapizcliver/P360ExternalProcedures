package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ColocaBusinessAPartirDeNegocio {

	
	private static final RESTWrapper rw = new RESTWrapper();
	private static int a = 0;
	private static String nBusiness = null;
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.Business")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			a++;
			if(a == 1) {
				return;
			}
			
			if(row.length == 0) {
				return;
			}
			
			nBusiness = determineBusiness(row[1], "");
			if(nBusiness == null) {
				System.out.println("FAULT: " + java.util.Arrays.asList(row));
			}else {
//				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[0] + "'@1")).put("values", new org.json.JSONArray().put(nBusiness)));
			}
			
		} );
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "BusinessNull_NegocioNotNull_20260721_230035.csv"));
//		rh.sendData();
	}

	private static final String determineBusiness(String negocio, String extwgS4h) {
		return "".equals(negocio) && "".equals(extwgS4h) ? null : "".equals(negocio) && !"".equals(extwgS4h) ? "SBB": "MARKETPLACE".equals(negocio) ? "MKP" : "LVP";
	}
	
}
