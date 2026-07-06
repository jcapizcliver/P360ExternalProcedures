package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class PonlesNegocioALosQueNoTienen2 {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		PonlesNegocioALosQueNoTienen2 pn = new PonlesNegocioALosQueNoTienen2();
		pn.recuperaCososSinBusiness();
	}
	
	private void recuperaCososSinBusiness() {
		long init = System.currentTimeMillis();
		java.util.concurrent.ConcurrentLinkedQueue<Object[]> elements = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "samples", "Productos3"))){
			stream.parallel().map(this::retrieveData).filter(this::filterData).map(this::calculateBusiness).forEach( elements::add );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Data read, parsed and collected took: " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		System.out.println("Total elements: " + elements.size());
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler setBusiness = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Business',root,\"0000.0000.RK\",'Business',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println));
		elements.forEach( arr -> setBusiness.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + arr[0] + "'@1")).put("values", new org.json.JSONArray().put(arr[1]))) );
		setBusiness.sendData();
	}
	
	private String[] retrieveData(String s) {
		String[] pieces = rw.getRw().parseLine(s);
		String[] data = new String[] { pieces[0], pieces[1], pieces[6], pieces[8] };
		return data;
	}
	
	private boolean filterData(String[] data) {
		if("S83418257".equals(data[0])) {
			System.out.println( rw.getRw().serializeChunk(data) );
		}
		return "".equals(data[1]) && (!"".equals(data[2]) || !"".equals(data[3]));
	}
	
	private Object[] calculateBusiness(String[] data) {
		return new Object[] { data[0], determineBusiness(data[2], data[3]) };
	}
	
	private Object determineBusiness(String negocio, String extwgS4h) {
		return "".equals(negocio) && "".equals(extwgS4h) ? null : new org.json.JSONObject().put("id", "'" + ("".equals(negocio) && !"".equals(extwgS4h) ? "SBB": "MARKETPLACE".equals(negocio) ? "MKP" : "LVP") + "'@'BusinessQualified'" );
	}
	
}
