package mx.com.liverpool.p360.services.core;

public class PubSubManual {


	public static void main(String[] args) {
//		doItOverAField("FIBER_CODE1");
//		doItOverAField("FIBER_CODE2");
//		doItOverAField("FIBER_CODE3");
//		doItOverAField("FIBER_CODE4");
//		doItOverAField("FIBER_CODE5");
//		doItOverAField("Currency");
//		refreshOtherPeople();
//		ejele();
//		System.exit(0);

//		PubSubGCP psD = new PubSubGCP("C:\\opt\\LVP\\dev\\crp-dev-dig-vccatalog-b74410667aea.json", "crp-dev-dig-vccatalog", "idmc_put_value-list-relation");
//		PubSubGCP psQ = new PubSubGCP("C:\\opt\\LVP\\dev\\crp-qas-dig-vccatalog-416185bab156.json", "crp-qas-dig-vccatalog", "idmc_put_value-list-relation");
		PubSubGCP psP = new PubSubGCP("C:\\opt\\LVP\\dev\\crp-pro-dig-vccatalog-e9c005804015.json", "crp-pro-dig-vccatalog", "idmc_put_products");
		org.json.JSONObject json = new org.json.JSONObject();
//		json.put("valueListRelations", new org.json.JSONArray().put(new org.json.JSONObject().put("idTemplate", "EU4-6026766").put("attributeName", "AlturaTaconPlataformaVaD")));
		String message = "LVP1033609783";
//				json.toString();
//		System.out.println("Sending message: " + message);
//		psD.publishMessage(message);
//		psQ.publishMessage(message);
		psP.publishMessage(
				"{\"products\":[{\"internalStatus\":\"Creación de SKU\",\"upcEan\":\"\",\"entityType\":\"Variant\",\"externalStatus\":\"En Revisión\",\"sku\":\"\",\"proposalId\":\"1754611647219984\",\"updatedAt\":\"2025-08-31T16:55:20.200Z\",\"previousStatus\":\"Creación de SKU\"}]}"
//				"{\"products\":[{\"internalStatus\":\"Creación de SKU\",\"upcEan\":\"\",\"entityType\":\"Generic\",\"externalStatus\":\"En Revisión\",\"sku\":\"\",\"nameProduct\":\"Body cuello V escote V para mujer\",\"proposalId\":\"1754611647219982\",\"updatedAt\":\"2025-08-31T16:55:19.160Z\",\"previousStatus\":\"Revisión Compras\"}]}"
//				"{\"products\":[{\"internalStatus\":\"Revisión Compras\",\"upcEan\":\"\",\"entityType\":\"Generic\",\"externalStatus\":\"En Revisión\",\"sku\":\"\",\"nameProduct\":\"Body cuello escote para mujer\",\"proposalId\":\"1754611647219982\",\"updatedAt\":\"2025-08-29T15:55:13.340Z\",\"previousStatus\":\"Propuesta Generada\"}]}"
				);
	}

	private static void doItOverAField(String field) {
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.Characteristic->Characteristic.Identifier equals \"" + field + "\" and StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\""
				);
		qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("pageSize", "900");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONArray valueListRelations = new org.json.JSONArray();
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
//				if(!withFilter.contains(values.getString(0))) {
					valueListRelations.put(new org.json.JSONObject().put("idTemplate", values.getString(0)).put("attributeName", values.getString(1)));
					if(valueListRelations.length() == 100) {
						sendMessageToPubSub(new org.json.JSONObject().put("valueListRelations", valueListRelations).toString());
						while(valueListRelations.length() > 0) {
							valueListRelations.remove(0);
						}
					}
//				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(valueListRelations.length() > 0) {
			sendMessageToPubSub(new org.json.JSONObject().put("valueListRelations", valueListRelations).toString());
			while(valueListRelations.length() > 0) {
				valueListRelations.remove(0);
			}
		}
	}

	private static void sendMessageToPubSub(String message) {
		PubSubGCP psD = new PubSubGCP("C:\\opt\\LVP\\dev\\crp-dev-dig-vccatalog-b74410667aea.json", "crp-dev-dig-vccatalog", "idmc_put_value-list-relation");
		PubSubGCP psQ = new PubSubGCP("C:\\opt\\LVP\\dev\\crp-qas-dig-vccatalog-416185bab156.json", "crp-qas-dig-vccatalog", "idmc_put_value-list-relation");
		System.out.println("Sending message: " + message);
		psD.publishMessage(message);
		psQ.publishMessage(message);
	}

	private static void refreshOtherPeople() {
		java.util.ArrayList<String> withFilter = getWithFilter();
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.Property->LookupValue.Code equals \"ListOfValues\""
				);
		qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("pageSize", "900");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONArray valueListRelations = new org.json.JSONArray();
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
//				if(!withFilter.contains(values.getString(0))) {
					valueListRelations.put(new org.json.JSONObject().put("idTemplate", values.getString(0)).put("attributeName", values.getString(1)));
					if(valueListRelations.length() == 100) {
						sendMessageToPubSub(new org.json.JSONObject().put("valueListRelations", valueListRelations).toString());
						while(valueListRelations.length() > 0) {
							valueListRelations.remove(0);
						}
					}
//				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(valueListRelations.length() > 0) {
			sendMessageToPubSub(new org.json.JSONObject().put("valueListRelations", valueListRelations).toString());
			while(valueListRelations.length() > 0) {
				valueListRelations.remove(0);
			}
		}
	}

	private static java.util.ArrayList<String> getWithFilter(){
		java.util.LinkedList<String> abc = new java.util.LinkedList<>();
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\""
				);
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("pageSize", "900");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				abc.addLast(values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return new java.util.ArrayList<>(abc);
	}

	private static void ejele() throws ServiceUnavailableException {

		java.util.LinkedList<String> toDelete = new java.util.LinkedList<>();
		java.util.Map<String, java.util.Map<String,String>> mapas = new java.util.TreeMap<>();
		java.util.Map<String, String> map = null;
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query",
				"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\""
				);
		qp.put("fields", "StandardizationValue.Value,StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier,StandardizationValue.PropertyValue");
		qp.put("pageSize", "900");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		String identifier = null;
		String lookup = null;
		String filter = null;
		String nf = null;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				if("".equals(values.getString(2))) {
					toDelete.addLast(values.getString(0));
				}else {
					identifier = values.getString(0);
					lookup = values.getString(1);
					filter = values.getString(2);
					map = mapas.get(lookup);
					if(map == null) {
						map = getValues(lookup);
						mapas.put(lookup, map);
					}
					nf = reescribeValor(filter, map);
					if("".equals(nf)) {
						toDelete.addLast(identifier);
					}else if(!nf.equals(filter)) {
						makeAnUpdate(identifier, nf);
					}else {
						System.out.println("Skipping update. " + identifier);
					}
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		toDelete.forEach(System.out::println);
		StringBuilder sb = new StringBuilder();
		for(String td : toDelete) {
			sb.append(sb.length() > 0 ? "," : "");
			sb.append("\"");
			sb.append(td);
			sb.append("\"");
		}
		if(sb.length() > 0) {
			RESTWorkshop rw = new RESTWorkshop();
			rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
			rw.putParameter("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
			rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and StandardizationValue.Value in (" + sb.toString() + ")");
			try {
				response = rw.makeRequest("DELETE", "/list/StandardizationValue/bySearch");
			} catch (ServiceUnavailableException e) {
				e.printStackTrace();
			}
			System.out.println(response == null ? rw.getRawResponse() : response);
		}else {
			System.out.println("Nothing to delete...");
		}
	}

	private static void makeAnUpdate(String value, String filter) {
		RESTWorkshop rw = new RESTWorkshop();
		org.json.JSONObject response = null;
		response = rw.makeRequest("POST", "/list/StandardizationValue", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + value + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(filter)))).toString());
		if(response != null) {
			System.out.println("Updated... " + response);
		}else {
			System.out.println(rw.getRawResponse());
		}
	}

	private static String reescribeValor(String value, java.util.Map<String, String> map) {
		java.util.LinkedList<String> lst = new java.util.LinkedList<>(java.util.Arrays.asList(value.split(",")));
		StringBuilder sb = new StringBuilder();
		for(String val : lst) {
			if(map.containsKey(val)) {
				sb.append(sb.length() == 0 ? "" : ",");
				sb.append(val);
			}else {
				System.out.println("\tDroping value -->" + val + "<--");
			}
		}
		return sb.toString();
	}

	private static java.util.Map<String, String> getValues(String lookup) throws ServiceUnavailableException {
		java.util.Map<String, String> losdesos = new java.util.TreeMap<>();

		RESTWorkshop rw = new RESTWorkshop();
		rw.putParameter("lookup", lookup);
		rw.putParameter("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		rw.putParameter("query", "LookupValue.IsActive = true");
		rw.putParameter("pageSize", "1200");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/bySearch");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					losdesos.put(values.getString(0), values.getString(1));
				}
			}else {
				System.out.println(rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return losdesos;
	}

}
