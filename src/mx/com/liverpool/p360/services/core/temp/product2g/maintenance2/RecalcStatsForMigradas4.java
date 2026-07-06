package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.xml.local.LoadProductDataSecondOpinionFTW;

public class RecalcStatsForMigradas4 {

	private static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		PubSubGCP pub = new PubSubGCP();
		LoadProductDataSecondOpinionFTW l = new LoadProductDataSecondOpinionFTW();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", 
					   "Product2G.ProductNo"
					+ ",SimpleProduct2GCharacteristicValueLang.Value('CalculatedWF_Att',-1)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('StateSKU',-1)->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',-1)->LookupValue.Code"
				);
		qp0.put("pageSize", "2000");
		qp0.put("query", "Product2G.CurrentStatus is empty");
		org.json.JSONArray losesos = new org.json.JSONArray();
		rw.collectData("list", "Product2G", null, "bySearch", qp0, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String calculatedWF_Att = values.getJSONArray(1).getString(0);
			String stateSKU = values.getJSONArray(2).getString(0);
			String fotoTomadaLiverpool = values.getJSONArray(3).getString(0);
			if(!"".equals(calculatedWF_Att) && !"".equals(stateSKU) && !"".equals(fotoTomadaLiverpool)) {
				String[] bundle = l.computeStatus(calculatedWF_Att, stateSKU, fotoTomadaLiverpool);
				String currentStatus = bundle[0] == null ? "" : bundle[0];
	    		String prevStatus = bundle[1] == null ? "" : bundle[1];
	    		String externalStatus = null;
	    		externalStatus = getExternalStatus(currentStatus);
	    		org.json.JSONObject jr = 
						new org.json.JSONObject()
							.put("proposalId", values.getString(0))
							.put("internalStatus", getStatusLabel(currentStatus))
							.put("externalStatus", externalStatus)
							.put("previousStatus", getStatusLabel(prevStatus))
							.put("entityType", "Generic")
							;
	    		losesos.put(jr);
	    		if(losesos.length() == 1000) {
		    		pub.publishMessage( 
							 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
							 PropertiesManager.get( "p360.contingency.gcp.idmc_put_products" ), 
							 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
							 new org.json.JSONObject().put("products", losesos).toString()
							);
		    		while(losesos.length() > 0) {
		    			losesos.remove(0);
		    		}
	    		}
			}
		});
		if(losesos.length() > 0) {
			pub.publishMessage( 
					 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
					 PropertiesManager.get( "p360.contingency.gcp.idmc_put_products" ), 
					 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
					 new org.json.JSONObject().put("products", losesos).toString()
				);
	   		while(losesos.length() > 0) {
	   			losesos.remove(0);
	   		}
		}
	}
	
	private static String getExternalStatus(String currentStatus) {
		String externalStatusCode = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", "ExternalStatus").toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(currentStatus.equals(pieces[0])) {
					externalStatusCode = pieces[1];
					break;
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		if(externalStatusCode == null) {
		}else {
//			System.out.println( "Returning: " + externalStatusCode );
			String lbl = getLookupCodeName(externalStatusCode, "ExternalStatus");
//			System.out.println("Now got: " + lbl);
			return lbl;
		}
		return null;

	}
	
	private static String getLookupCodeName(String code, String lookup) {

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "global_lookups", lookup).toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(code.equals(pieces[0])) {
					return pieces[1];
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String getStatusLabel(String key) {
		return 
			  "1001".equals(key) ? "Propuesta Generada"
			: "1002".equals(key) ? "Pendiente Inicio Enriquecimiento"
			: "1003".equals(key) ? "Revisi\u00f3n Compras"
			: "1004".equals(key) ? "Carga de Imagen"
			: "1005".equals(key) ? "Rechazada"
			: "1006".equals(key) ? "Por Actualizar "
			: "1007".equals(key) ? "Aprobada"
			: "1008".equals(key) ? "Modificaci\u00f3n "
			: "1009".equals(key) ? "Cancelado"
			: "1010".equals(key) ? "En Proceso Liverpool"
			: "1011".equals(key) ? "En Proceso de Env\u00edo"
			: "1020".equals(key) ? "Creaci\u00f3n de SKU"
			: "1021".equals(key) ? "Gobierno de Datos"
			: "1022".equals(key) ? "Revisi\u00f3n QA"
			: "1023".equals(key) ? "Category"
			: "1024".equals(key) ? "Rechazo Publicaci\u00f3n"
			: "1025".equals(key) ? "Eliminada"
			: "1026".equals(key) ? "En Proceso Foro"
			: "1027".equals(key) ? "Rechazo Compras"
			: "1028".equals(key) ? "Rechazo QA"
			: "1029".equals(key) ? "Rechazo Gobierno"
			: "1030".equals(key) ? "Rechazo Category"
			: "1031".equals(key) ? "Repoblamiento"
			: "1032".equals(key) ? "Excepci\u00f3n de Catalogaci\u00f3n"
			: "";
	}

}
