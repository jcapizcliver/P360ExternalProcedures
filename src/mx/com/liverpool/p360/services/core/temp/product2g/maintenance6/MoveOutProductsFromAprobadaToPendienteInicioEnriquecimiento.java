package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class MoveOutProductsFromAprobadaToPendienteInicioEnriquecimiento {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		if(args.length == 0) {
			args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\sqlrunner_PIM_MASTER_20260601_085647.csv" };
		}
		java.util.Set<String> pids = new java.util.TreeSet<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '"', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			
			if(row.length > 0) {
				if(!"Product2GIdentifier".equals(row[0])) {
					pids.add(row[0]);
				}
			}
			
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		System.out.println( pids.size() );
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		for(String pid : pids) {
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pid + "'@1")).put("values", new org.json.JSONArray().put("Pendiente Inicio Enriquecimiento")));
		}
		rh.sendData();
	}
	
}
