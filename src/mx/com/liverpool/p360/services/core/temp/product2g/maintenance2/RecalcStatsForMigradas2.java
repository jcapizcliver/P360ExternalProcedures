package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.xml.local.LoadProductDataSecondOpinionFTW;

public class RecalcStatsForMigradas2 {

	private static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		LoadProductDataSecondOpinionFTW l = new LoadProductDataSecondOpinionFTW();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus")).put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")).put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", 
					   "Product2G.ProductNo"
					+ ",SimpleProduct2GCharacteristicValueLang.Value('CalculatedWF_Att',-1)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('StateSKU',-1)->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',-1)->LookupValue.Code"
				);
		qp0.put("pageSize", "2000");
		qp0.put("query",
					"Product2G.ProductNo = \"S95003218\""
//					"Product2G.CurrentStatus is empty"
				);
		java.util.List<org.json.JSONObject> filas = new java.util.ArrayList<>();
		java.util.List<String> sinWf = new java.util.ArrayList<>();
		java.util.List<String> sinState = new java.util.ArrayList<>();
		java.util.List<String> sinFotoTomadaLiverpool = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp0, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(!values.getString(0).startsWith("LVP")) {
				String calculatedWF_Att = values.getJSONArray(1).getString(0);
				String stateSKU = values.getJSONArray(2).getString(0);
				String fotoTomadaLiverpool = values.getJSONArray(3).getString(0);
				if(!"".equals(calculatedWF_Att)) {
					String[] bundle = l.computeStatus(calculatedWF_Att, stateSKU, fotoTomadaLiverpool);
					String currentStatus = bundle[0] == null ? "" : bundle[0];
		    		String prevStatus = bundle[1] == null ? "" : bundle[1];
		    		String externalStatus = currentStatus == null || "".equals(currentStatus) ? "" : internalToExternalStatusMap.get(currentStatus);
		    		if(!"".equals(currentStatus)) {
			    		org.json.JSONObject row0 = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", row.getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put(prevStatus).put(currentStatus).put(externalStatus));
			    		System.out.println("Colocando: " + row0);
			    		filas.add(row0);
		    		}
				}else {
					System.out.println("Algo no: " + calculatedWF_Att + "\n\t" + stateSKU + ",\n\t" + fotoTomadaLiverpool);
					String id = values.getString(0);
					if("".equals(calculatedWF_Att))
						sinWf.add(id);
					if("".equals(stateSKU))
						sinState.add(id);
					if("".equals(fotoTomadaLiverpool))
						sinFotoTomadaLiverpool.add(id);
				}
			}
		});
		for(org.json.JSONObject row0 : filas)
			rh.addRow(row0);
		rh.sendData();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("SinWF_Att").toFile())))){
			sinWf.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("SinStateSKU").toFile())))){
			sinState.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("SinFotoTomadaLiverpool").toFile())))){
			sinFotoTomadaLiverpool.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
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
