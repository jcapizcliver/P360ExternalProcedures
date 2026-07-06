package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ResendStatusToPubSub {


	public static void main(String[] args) {
		String[] pieces = ("1698767480968848\r\n"
				+ "1698767480968725\r\n"
				+ "1698767480968581\r\n"
				+ "1698767480968278\r\n"
				+ "1698767480968272\r\n"
				+ "1698767480968233\r\n"
				+ "1698767480968032\r\n"
				+ "1698767480968257\r\n"
				+ "1698767480968182\r\n"
				+ "1698767480968086\r\n"
				+ "1698767480968284").split("\\r\\n");

		String ean = null;
		String sku = null;
		String previousStatus = null;
		String internalStatus = null;
		String externalStatus = null;
		String creationDate = null;

		org.json.JSONObject response = null;
		org.json.JSONObject data = null;
		org.json.JSONArray characteristicRecords = null;
		org.json.JSONArray logArray = null;
		org.json.JSONArray rechazos = new org.json.JSONArray();

		org.json.JSONObject jsonResponse = new org.json.JSONObject();

		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicRecordsMap = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeIds", "true");
		qp.put("includeLabels", "true");
		qp.put("entityFilter", "Product2G,Product2GLog,Product2GCharacteristicValue");
		RESTWorkshop workshop = new RESTWorkshop();
		PubSubGCP pub = new PubSubGCP();
		for(String elese : pieces) {
			response = workshop.makeRequest("GET", "/object/Product2G/'" + elese + "'@'MASTER'" , qp, "");
			data = response.getJSONObject("_data");
			characteristicRecords = data.getJSONArray("_characteristicRecords");
			characteristicsToMap(characteristicRecords, characteristicRecordsMap, rechazos);
			logArray = data.getJSONArray("log");
			for(int i=0; i<logArray.length(); i++) {
				if("HPM".equals(logArray.getJSONObject(i).getJSONObject("_qualification").getJSONObject("channel").getString("_key"))) {
					if(logArray.getJSONObject(i).has("modificationDate")) {
						creationDate = logArray.getJSONObject(i).getString("modificationDate");
					}else {
						creationDate = "";
					}
				}
			}
			ean = getSimpleValue("MainBarCode", characteristicRecordsMap);
			sku = getSimpleValue("SKU", characteristicRecordsMap);
			previousStatus = !data.has("previousStatus") ? "" : data.getJSONObject("previousStatus").getString("_label");
			internalStatus = !data.has("currentStatus") ? "" : data.getJSONObject("currentStatus").getString("_label");
			externalStatus = !data.has("externalStatus") ? "" : data.getJSONObject("externalStatus").getString("_label");

			jsonResponse = new org.json.JSONObject().put("products", new org.json.JSONArray().put(new org.json.JSONObject().put("proposalId", elese).put("updatedAt", creationDate).put("internalStatus", internalStatus).put("externalStatus", externalStatus).put("previousStatus", previousStatus).put("sku", sku == null ? "" : sku).put("upcEan", ean == null ? "" : ean).put("entityType", "Generic")));

			pub.publishMessage("crp-dev-dig-vccatalog", "idmc_put_products", "D:\\tmp\\crp-dev-dig-vccatalog-b74410667aea.json", jsonResponse.toString());
			pub.publishMessage("crp-qas-dig-vccatalog", "idmc_put_products", "D:\\tmp\\crp-qas-dig-vccatalog-416185bab156.json", jsonResponse.toString());
			System.out.println("Sent: " + jsonResponse.toString());

		}
	}

	private static String getSimpleValue(String characteristicIdentifier, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicRecordsMap) {
		java.util.LinkedList<org.json.JSONObject> list = characteristicRecordsMap.get(characteristicIdentifier);
		if(list != null) {
			return list.getLast().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
		}
		return null;
	}

	private static void characteristicsToMap(org.json.JSONArray characteristicRecords, java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap, org.json.JSONArray rechazos){
		java.util.LinkedList<org.json.JSONObject> lst = null;
		org.json.JSONObject characteristicRecord = null;
		String characteristicIdentifier = null;
		org.json.JSONArray children = null;
		org.json.JSONObject child = null;
		boolean notEnough = true;
		for(int i=0; i<characteristicRecords.length(); i++) {
			characteristicRecord = characteristicRecords.getJSONObject(i);
			characteristicIdentifier = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = characteristicRecordsMap.get(characteristicIdentifier);
			if(lst == null) {
				lst = new java.util.LinkedList<>();
				characteristicRecordsMap.put(characteristicIdentifier, lst);
			}
			lst.addLast(characteristicRecord);
			if(characteristicIdentifier.endsWith("_Rechazo") || characteristicIdentifier.equals("Comentario")) {
				children = characteristicRecord.getJSONArray("_children");
				if(children != null) {
					for(int j=0; j<children.length(); j++) {
						child = children.getJSONObject(j);
						if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").startsWith("rre_")) {
							notEnough = false;
						}
					}
				}else {
					rechazos.put(characteristicRecord);
				}
				if(notEnough) {
					rechazos.put(characteristicRecord);
				}
				notEnough = true;
			}
		}
	}

}
