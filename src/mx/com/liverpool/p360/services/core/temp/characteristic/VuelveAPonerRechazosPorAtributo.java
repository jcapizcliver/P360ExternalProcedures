package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class VuelveAPonerRechazosPorAtributo {

	public static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
//		System.out.println("Basic Data...");
//		String[] attributes = obtenBasicData();
//		for(String a : attributes) {
//			System.out.println(a);
//		}
//		System.exit(0);
		determinaRechazosFaltantes( getData() );
	}

	private static void determinaRechazosFaltantes(String[][] data) {
		String[] vads = data[0];
		String[] rechazos = data[1];
		String[] categories = data[2];
		String[] etiquetas = data[3];
		java.util.ArrayList<String> rechazosArray = new java.util.ArrayList<>(java.util.Arrays.asList(rechazos));
		int a = 0;
		for(int i=0; i<vads.length; i++) {
			if(!rechazosArray.contains(vads[i] + "_Rechazo")) {
				creaRechazo(vads[i], categories[i], etiquetas[i]);
				a++;
//				System.out.println("--->" + vads[i] + " - " + categories[i]);
			}
		}
		System.out.println(a);
	}
	
	private static void createCategoryIfNotExists(String baseId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("POST", "/list/LookupValue/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + baseId + "'@'CharacteristicCategories'")).put("values", new org.json.JSONArray().put(true)))).toString());
		System.out.println(response == null ? rw.getRawResponse() : response);
		response = rw.makeRequest("POST", "/list/Characteristic/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Category")).put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + baseId + "'")).put("values", new org.json.JSONArray().put(baseId).put(true)))).toString());
		System.out.println(response == null ? rw.getRawResponse() : response);
	}
	
	private static void sendPostRequest(String message, String id) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("updateIfExists", "true");
		org.json.JSONObject response = null;
		int times = 0;
		do{
			if(times > 0) {
				System.out.println("Retrying... " + (times > 1 ? times : ""));
			}
			response = rw.makeRequest(id == null ? "POST" : "PUT", "/object/Characteristic" + (id != null ? "/'" + id + "'" : ""), qp, message);
			System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
			times++;
		}while( "upstream request timeout".equals(rw.getRawResponse()) );
	}
	
	private static void disableDependents(String baseId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		qp.put("query", "Characteristic.ParentCharacteristic equals \"" + baseId + "\"");
		qp.put("fields", "Characteristic.Identifier");
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null && response.getInt("totalSize") > 0) {
			rows = response.getJSONArray("rows");
			qp.clear();
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				System.out.println("Now disabling... (" + values.getString(0) + ")");
				response = rw.makeRequest("POST", "/list/Characteristic/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")).put("values", new org.json.JSONArray().put(false)))).toString());
				System.out.println(response == null ? "ERR disabling: " + rw.getRawResponse() : response);
			}
		}else {
			if(response == null)
				System.out.println("ERR: " + rw.getRawResponse());
			else
				System.out.println("Perhaps did not exist: " + baseId);
		}
	}
	
	private static boolean disableIfExists(String baseId) {
		boolean exists = false;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		qp.put("query", "Characteristic.Identifier equals \"" + baseId + "\"");
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null && response.getInt("totalSize") > 0) {
			exists = true;
			qp.clear();
			disableDependents(baseId);
			System.out.println("Now disabling... (" + baseId + ")");
			response = rw.makeRequest("POST", "/list/Characteristic/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + baseId + "'")).put("values", new org.json.JSONArray().put(false)))).toString());
			System.out.println(response == null ? "ERR disabling: " + rw.getRawResponse() : response);
		}else {
			if(response == null)
				System.out.println("ERR: " + rw.getRawResponse());
			else
				System.out.println("Perhaps did not exist: " + baseId);
		}
		return exists;
	}
	
	private static void enable(String baseId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		System.out.println("Now enabling... (" + baseId + ")");
		response = rw.makeRequest("POST", "/list/Characteristic/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + baseId + "'")).put("values", new org.json.JSONArray().put(true)))).toString());
		System.out.println(response == null ? "ERR disabling: " + rw.getRawResponse() : response);
	}
	
	private static void creaRechazo(String baseId, String category, String baseLabel) {
		boolean exists = disableIfExists(baseId + "_Rechazo");
		if(category == null || "".equals(category)) {
			createCategoryIfNotExists(baseId);
			category = baseId;
		}
		org.json.JSONObject characteristic = new org.json.JSONObject();
		characteristic.put("identifier", baseId + "_Rechazo");
		characteristic.put("category", new org.json.JSONObject().put("_code", category));
		if(!exists)
		characteristic.put("isActive", true);
		characteristic.put("entities", new org.json.JSONArray().put(new org.json.JSONObject().put("_key", "Product2G")));
		characteristic.put("dataType", new org.json.JSONObject().put("_code", "NONE"));
		characteristic.put("lowerBound", 0);
		characteristic.put("upperBound", 100);
		characteristic.put("isMultiValue", false);
		characteristic.put("isMultiLine", false);
		characteristic.put("isLanguageSpecific", false);
		characteristic.put("isReadOnly", false);
		characteristic.put("lang", new org.json.JSONArray()
				.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "esl"))).put("name", baseLabel + " (Rechazo)"))
				);
		sendPostRequest(characteristic.toString(), exists ? baseId + "_Rechazo" : null );
		if(exists) {
			enable(baseId + "_Rechazo");
		}
		crearHijoRechazo("mdr_", baseId, "RejectReazonType", "Motivo de Rechazo (" + baseId + ")", "Rejection Reason (" + baseId + ")");
		crearHijoRechazo("msj_", baseId, null, "Comentario de Rechazo (" + baseId + ")", "Rejection Reason (" + baseId + ")");
		crearHijoRechazo("rem_", baseId, "CommentStatus", "Estatus del Mensaje de Rechazo (" + baseId + ")", "Rejection Status (" + baseId + ")");
		crearHijoRechazo("rma_", baseId, "RechazoMensajeAccion", "Acción requerida (" + baseId + ")", "Action Needed (" + baseId + ")");
		crearHijoRechazo("rmum_", baseId, null, "Fecha del Mensaje de Rechazo (" + baseId + ")", "Rejection Timestamp (" + baseId + ")", "DATETIME");
		crearHijoRechazo("rrd_", baseId, "TargetRole", "Rol Destino del Rechazo (" + baseId + ")", "Target Role (" + baseId + ")");
		crearHijoRechazo("rre_", baseId, "TargetRole", "Rol Emisor del Rechazo (" + baseId + ")", "Submitting Role (" + baseId + ")");
	}
	
	private static void crearHijoRechazo(String prefijo, String nombreBase, String lookupId, String label, String labelEn) {
		crearHijoRechazo(prefijo, nombreBase, lookupId, label, labelEn, null);
	}
	
	private static void crearHijoRechazo(String prefijo, String nombreBase, String lookupId, String label, String labelEn, String dataType) {
		org.json.JSONObject characteristic = new org.json.JSONObject();
		characteristic.put("identifier", prefijo + nombreBase);
		characteristic.put("parentCharacteristic", new org.json.JSONObject().put("_code", nombreBase + "_Rechazo"));
		characteristic.put("rootCharacteristic", new org.json.JSONObject().put("_code", nombreBase + "_Rechazo"));
		characteristic.put("isActive", true);
		if(lookupId != null && !"".equals(lookupId)) {
			characteristic.put("lookup", new org.json.JSONObject().put("_code", lookupId));
			characteristic.put("dataType", new org.json.JSONObject().put("_code", "LOOKUP"));
		}else{
			if(dataType == null || "".equals(dataType)) {
				characteristic.put("dataType", new org.json.JSONObject().put("_code", "TEXT"));
			}else {
				characteristic.put("dataType", new org.json.JSONObject().put("_code", dataType));	
			}
		}
		characteristic.put("lowerBound", 0);
		characteristic.put("upperBound", 1);
		characteristic.put("isMultiValue", false);
		characteristic.put("isMultiLine", false);
		characteristic.put("isLanguageSpecific", false);
		characteristic.put("isReadOnly", false);
		characteristic.put("lang", new org.json.JSONArray()
				.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "esl"))).put("name", label))
				.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "en"))).put("name", labelEn))
				);
		sendPostRequest(characteristic.toString(), null);
	}
	
	private static String[][] getData(){
		java.util.ArrayList<String> elp = new java.util.ArrayList<>( java.util.Arrays.asList( obtenBasicData() ) );
		java.util.LinkedList<String> vals = new java.util.LinkedList<>();
		java.util.LinkedList<String> valsRechazo = new java.util.LinkedList<>();
		java.util.LinkedList<String> valsCategory = new java.util.LinkedList<>();
		java.util.LinkedList<String> valsEtiquetas = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query",
//				"Characteristic.Category->LookupValue.Code equals \"Master Data\""
//				"Characteristic.Identifier equals \"CostoNetoSinIVA\""
				
				"Characteristic.ParentCharacteristic is empty and (Characteristic.Category is empty or "
				+ "("
				+ " not Characteristic.Category->LookupValue.Code equals \"ConjuntoLook\""
				+ " and not Characteristic.Category->LookupValue.Code equals \"Master Data\""
				+ " and not Characteristic.Category->LookupValue.Code equals \"RechazoCategory\""
				+ " and not Characteristic.Category->LookupValue.Code equals \"ExcepcionPublicacion\""
				+ " and not Characteristic.Category->LookupValue.Code equals \"Operativa\""
				+ "))"
				
				); // "Characteristic.Identifier wildcard \"%_Rechazo\" or Characteristic.Identifier wildcard \"%Att\"");
		qp.put("fields", "Characteristic.Identifier,Characteristic.Category->LookupValue.Code,CharacteristicLang.Name(es)");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		
		int currentIndex = 0;
		int totalSize = 0;
		
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				totalSize = response.getInt("totalSize");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!values.getString(0).endsWith("_Rechazo") && !values.getString(0).matches("[a-z]+_.+") /* && elp.contains(values.getString(0)) */ ) {
						vals.addLast(values.getString(0));
						valsCategory.addLast(values.getString(1));
						valsEtiquetas.addLast(values.getString(2));
					}else if(!values.getString(0).matches("[a-z]+_.+")) {
						valsRechazo.addLast(values.getString(0));
					}
				}
			}else {
				System.out.println("Error en petición de características Att: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return new String[][] {
			vals.toArray(new String[] {}),
			valsRechazo  .toArray(new String[] {}),
			valsCategory .toArray(new String[] {}),
			valsEtiquetas.toArray(new String[] {})
		};
	}
	
	private static String[] obtenBasicData() {
		java.util.Set<String> vals = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query",
				"StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\" and StandardizationValue.PropertyValue equals \"Datos de Venta\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				);
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Characteristic->Characteristic.Category->LookupValue.Code,StandardizationValue.Characteristic->CharacteristicLang.Name(es)");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		
		int currentIndex = 0;
		int totalSize = 0;
		
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				totalSize = response.getInt("totalSize");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					vals.add(values.getString(0));
				}
			}else {
				System.out.println("Error en petición de características Att: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return vals.toArray(new String[] {});
	}
}
