package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.temp.xml.local.LoadProductDataSecondOpinionFTW;

public class RecalcStatsForMigradas {

	private static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		LoadProductDataSecondOpinionFTW l = new LoadProductDataSecondOpinionFTW();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus")).put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")).put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', '|', '\\', "\r\n", java.nio.charset.StandardCharsets.ISO_8859_1, arr -> {
			String calculatedWF_Att = arr[9];
			String stateSKU = arr[10];
			String fotoTomadaLiverpool = arr[4];
			String cs = arr[3];
//			if("".equals(cs)) {
				String[] bundle = l.computeStatus(calculatedWF_Att, stateSKU, fotoTomadaLiverpool);
				String currentStatus = bundle[0] == null ? "" : bundle[0];
	    		String prevStatus = bundle[1] == null ? "" : bundle[1];
	    		String externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
	    		if(!"".equals(currentStatus)) {
	    			System.out.println(java.util.Arrays.asList( new String[] { arr[0], prevStatus, currentStatus, externalStatus } ));
		    		if(!"Propuesta".equals(arr[0])) {
			    		org.json.JSONObject row = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + arr[0] + "'@1")).put("values", new org.json.JSONArray().put(prevStatus).put(currentStatus).put(externalStatus));
			    		rh.addRow(row);
		    		}
	    		}
//			}else {
//				System.out.println(java.util.Arrays.asList(arr));
//			}
		});
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Standard Template - BGGForo2.csv"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Standard Template - BGGForo.csv"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Standard Template - BGG2.csv"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Standard Template - BGG Proveedores migrados.csv"));
		rh.sendData();
	}
	

    private static final java.util.Map<String, String> internalToExternalStatusMap = loadExternalStatusMap();

    private static java.util.Map<String, String> loadExternalStatusMap() {
    	java.util.Map<String, String> internalToExternalStatusMap = new java.util.HashMap<>();
    	try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", "ExternalStatus").toFile())))){
    		String line = null;
    		String[] pieces = null;
    		while((line = br.readLine()) != null) {
    			pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
    			internalToExternalStatusMap.put(pieces[0], pieces[1]);
    		}
    	}catch(java.io.IOException e) {
    		e.printStackTrace();
    	}
    	return internalToExternalStatusMap;
    }
	
}
