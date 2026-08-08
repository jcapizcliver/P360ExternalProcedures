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
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SetBusiness.csv").toFile())))){
//		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "BusinessMissmatch.csv").toFile())))){
//			pw.println( rw.getRw().serializeChunk(new String[] { "IdentificadorProducto", "Negocio previo P360", "Negocio LVP", "Negocio SBB", "Negocio Calculado" }) );
			pw.println( rw.getRw().serializeChunk(new String[] { "IdentificadorProducto", "Negocio Calculado" }) );
			SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				a++;
				if(a == 1) {
					return;
				}
				if(row.length == 0) {
					return;
				}
				
				String businessClc = determineBusiness(row[6], row[8]);
				if(businessClc != null) {
					pw.println( rw.getRw().serializeChunk(new String[] { row[0], businessClc }) );
				}
				if(businessClc != null && !businessClc.equals(row[2])) {
//					pw.println( rw.getRw().serializeChunk(new String[] { row[0], row[2], row[5], row[8], businessClc }) );
	//				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[0] + "'@1")).put("values", new org.json.JSONArray().put(businessClc)));
				}
	//			if("S73190532".equals(row[0])) {
	//				System.out.println(businessClc + "  --  " + java.util.Arrays.asList(row));
	//			}
				
			});
			parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Business_20260723_231114.csv"));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
//		rh.sendData();
		
	}
    
	private static String determineBusiness(String negocio, String extwgS4h) {
		return     "".equals(negocio) 
				&& "".equals(extwgS4h) ? null : 
					(!"".equals(extwgS4h) ? "SBB": "ART. MARKETPLACE".equals(negocio) ? "MKP" : "LVP" );
	}
	
	
}
