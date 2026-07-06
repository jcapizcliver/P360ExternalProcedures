package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class EvaluacionGenericosMkt {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	
	public static void main(String[] args) {
		java.util.LinkedList<String[]> cosos = new java.util.LinkedList<>();
		EvaluacionGenericosMkt ev = new EvaluacionGenericosMkt();
		ev.loadMktProposalsToCheck(cosos);
		cosos.forEach(c -> {
			for(int i=0; i<c.length; i++) {
				System.out.print((i == 0 ? "" : ",") + c[i]);
			}
			System.out.println();
			ev.checkParentVariantsCompleteness(c[0], new org.json.JSONArray(), c[1], c[2]);
		});
	}
	
	private void loadMktProposalsToCheck(java.util.LinkedList<String[]> cosos) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				  "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool')->LookupValue.Code"
				+ ",Product2G.CurrentStatus");
		qp.put("query", "characteristic('SKU',-1) is empty and characteristic('Business',-1) = 'MKP'@'BusinessQualified' and Product2G.CurrentStatus = 1020");
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex ++;
					values = rows.getJSONObject(i).getJSONArray("values");
					cosos.addLast(new String[] { values.getString(0), values.getJSONArray(1).getString(0), values.getString(2) });
				}
			}else {
				log("ERROR: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
	}

	private void checkParentVariantsCompleteness(String productId, org.json.JSONArray characteristicRecords, String fotosTomaLiverpool, String currentStatus) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				if("".equals(values.getJSONArray(1).getString(0)))
					return;
			}
			addValue("MensajeCreacionSKU", "Actualizado " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSSZ").format(new java.util.Date()), characteristicRecords );
			addValue("SKU", productId, characteristicRecords );
			sendWriteRequest("Product2G", productId, characteristicRecords, fotosTomaLiverpool, currentStatus);
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
	}
	
	private void addValue(String name, Object value, org.json.JSONArray values) {
		if(value == null)
			return;
		values.put( new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", name))).put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put( value )))) );
	}
	
	private void sendWriteRequest(String entity, String id, org.json.JSONArray characteristicRecords, String fotoTomadaLiverpool, String currentStatus) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("_characteristicRecords", characteristicRecords);
		if("Product2G".equals(entity) && currentStatus != null && "1020".equals(currentStatus)) {
			collectNumberOfImages(id, fotoTomadaLiverpool, request);
		}
		org.json.JSONObject response = null;
		log("/object/" + entity + "/'" + id + "'@'MASTER'");
		response = workshop.makeRequest("PUT", "/object/" + entity + "/'" + id + "'@'MASTER'", qp, request.toString());
		if(response != null) {
			log("\tWriting: " + characteristicRecords + "\nNot really an error from writing id: " + id + ": " + response);
		}else {
			log("ERR: " + workshop.getRawResponse());
		}
	}

	private void collectNumberOfImages(String productId, String fotosTomaLiverpool, org.json.JSONObject data) {
		int lacuenta = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"Article.SupplierAID"
				+ ",ProductReference.ReferencedSupplierAid(\"" + productId + "\")"
				+ ",ArticleCharacteristicValueLang.Value(ProductImageDetail,\"0000.0000.RK\",\"0000.0000.RK\",ProductImageDetail_URL,-1)"
				+ ",ArticleCharacteristicValueLang.Value(ProductImage,\"0000.0000.RK\",\"0000.0000.RK\",ProductImage_URL,-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null) {
			org.json.JSONArray rows = response.getJSONArray("rows");
			org.json.JSONArray values = null;
			org.json.JSONArray details = null;
			org.json.JSONArray principal = null;
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				details = values.getJSONArray(2);
				principal = values.getJSONArray(3);
				for(int j=0; j<details.length(); j++) {
					if(!"".equals(details.getString(j)))
						lacuenta++;
				}
				for(int j=0; j<principal.length(); j++){
					if(!"".equals(principal.getString(j)))
						lacuenta++;
				}
			}
			if("Corregido".equals(fotosTomaLiverpool) || ("N".equals(fotosTomaLiverpool) && lacuenta > 0 )) {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1022));
			}else if("Y".equals(fotosTomaLiverpool)) {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1002));
			}else {
				data.put("currentStatus", new org.json.JSONObject().put("_key", 1004));
			}
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
	}
	
	private void agregaClasificacion(String itemGroup, org.json.JSONObject data) {
		org.json.JSONArray structureGroupMap = null; //		
		if(data.has("structureGroupMap")) {
			structureGroupMap = data.getJSONArray("structureGroupMap") ;
		}else {
			structureGroupMap = new org.json.JSONArray();
			data.put("structureGroupMap", structureGroupMap);
		}
		for(int i=0; i<structureGroupMap.length(); i++) {
			if(structureGroupMap.getJSONObject(i).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").endsWith("'@'CommercialECC'")) {
				return;
			}
		}
		structureGroupMap.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + itemGroup + "-L5ECC'@'CommercialECC'"))));
	}

	private void log(String message) {
		System.out.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
				+ "]  " + message);
	}
	
}
