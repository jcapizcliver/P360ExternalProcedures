package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class SetEnriquecidoEnForo {

	public static void main(String[] args) throws ServiceUnavailableException {
		SetEnriquecidoEnForo s = new SetEnriquecidoEnForo();
		java.util.LinkedList<String> data = s.revisionQA(null);
		data.forEach(s::processProposal);
	}
	
	private void processProposal(String proposalId) {
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONArray rechazos = new org.json.JSONArray();
		org.json.JSONObject data = null;
		org.json.JSONArray characteristicRecords = null;
		qp.put("entityFilter", "Product2GCharacteristicValue");
		qp.put("includeIds", "true");
		qp.put("includeLabels", "true");
		org.json.JSONObject response = workshop.makeRequest("GET", "/object/Product2G/" + proposalId, qp, null);
		org.json.JSONArray characteristicRecordsForUpdate = new org.json.JSONArray();
		if(response != null) {                                                                                                                                                                                                                                                                                                                                                                                                    
			data = response.getJSONObject("_data");
			characteristicRecords = data.has("_characteristicRecords") ? data.getJSONArray("_characteristicRecords") : new org.json.JSONArray();
			characteristicsToMap(characteristicRecords, characteristicRecordsMap, rechazos);
			boolean wereYouInForo = wereYouInForo(
					getSimpleValue("FirstDateApprove", characteristicRecordsMap),
					data.getString("statusModification")
				);
			addCharacteristicValue(characteristicRecordsForUpdate, "EnriquecidoEnForo", String.valueOf(wereYouInForo), false, false);
			sendUpdateObjectAPI(proposalId, new org.json.JSONObject().put("_characteristicRecords", characteristicRecordsForUpdate), workshop);
		}
		
	}
	
	private void sendUpdateObjectAPI(String proposalId, org.json.JSONObject data, RESTWorkshop workshop) {
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("PUT", "/object/Product2G/'" + proposalId + "'@'MASTER'", qp, data.toString());
		log(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	}
	
	private void addCharacteristicValue(org.json.JSONArray characteristicArray, String characteristicId, Object value, boolean isCode, boolean isLabel ) {
		org.json.JSONObject characteristicValue  = new org.json.JSONObject();
		characteristicValue.put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicId)));
		characteristicValue.put("_recordLang",    new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(isCode ? new org.json.JSONObject().put("_code", value) : isLabel ? new org.json.JSONObject().put("_label", value) : value))));
		characteristicArray.put( characteristicValue );
	}

	private String getSimpleValue(String characteristicIdentifier, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicRecordsMap) {
		java.util.LinkedList<org.json.JSONObject> list = characteristicRecordsMap.get(characteristicIdentifier);
		if(list != null) {
			return list.getLast().getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
		}
		return null;
	}
		
	private void characteristicsToMap(org.json.JSONArray characteristicRecords, java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicRecordsMap, org.json.JSONArray rechazos){
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
	
	private java.util.LinkedList<String> revisionQA(String baseUrl) throws ServiceUnavailableException{
		java.util.LinkedList<String> data = new java.util.LinkedList<>();
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		rw.putParameter("pageSize", "1200");
		rw.putParameter("fields", "Product2G.ProductNo");
		rw.putParameter("query", "(Product2G.CurrentStatus equals \"Revisión QA\") and characteristic('EnriquecidoEnForo',-1) is empty");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		do {
			rw.putParameter("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch");
			if(response != null) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					data.addLast(values.getString(0));
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
		return data;
	}
	
	private boolean wereYouInForo(String lastDateApproved, String lastStatusChangeRaw) {
		if(lastStatusChangeRaw == null || "".equals(lastStatusChangeRaw))
			return false;
		String[] records = lastStatusChangeRaw.split("\\r\\n");
		if(lastDateApproved == null || "".equals(lastDateApproved)) {
			java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
			java.util.regex.Matcher m = null;
			String datePart = null;
			java.util.Date currentLogDate = null;
			String prevStatus = null;
			String currentStatus = null;
			java.util.regex.Pattern statusPattern = java.util.regex.Pattern.compile("(?<=\")(.+)(?=\")");
			java.util.regex.Matcher mSP = null;
			String[][] tuplas = collectStatusInformation();
			java.util.Map<String, String> espMap = fromTuples(tuplas, 1);
			java.util.Map<String, String> engMap = fromTuples(tuplas, 2);
			log("Spanish map: " + espMap);
			log("English map: " + engMap);
			for(int i=0; i<records.length; i++) {
				 m = datePattern.matcher(records[i]);
				 if(m.find()) {
					 try {
						 datePart = m.group();
						 if(records[i].startsWith("El usuario")) {
							 currentLogDate = new java.text.SimpleDateFormat("dd/MM/yyyy").parse(datePart);
							 log("Parsed date from Spanish message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else if(records[i].startsWith("The user")) {
							 currentLogDate = new java.text.SimpleDateFormat("MM/dd/yyyy").parse(datePart);
							 log("Parsed date from English message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else {
							 currentLogDate = null;
						 }
						 if(currentLogDate != null) {
							 mSP = statusPattern.matcher(records[i]);
							 if(mSP.find()) {
								 if(records[i].startsWith("El usuario")) {
									 currentStatus = espMap.get(mSP.group());
									 log("Parsed status from Spanish message: " + currentStatus + " ( from: " + mSP.group() + ")" );
								 }else if(records[i].startsWith("The user")) {
									 currentStatus = engMap.get(mSP.group());
									 log("Parsed status from English message: " + currentStatus + " ( from: " + mSP.group() + ")" );
								 }else {
									 currentStatus = null;
								 }
								 if(currentStatus != null) {
									 if(prevStatus != null && "1022".equals(prevStatus) && ("1026".equals(currentStatus) /* || "1002".equals(currentStatus) */)) {
										 log("Found a transition from Foro Process to QA within last approved time frame.");
										 return true;
									 }
									 prevStatus = currentStatus;
								 }
							 }
						 }
					 }catch(java.text.ParseException e) {
						 logE(e);
					 }
				 }
			 }
		}else {
			java.util.Date lastDateApprovedDate = null;
			java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
			java.util.regex.Matcher m = null;
			String datePart = null;
			java.util.Date currentLogDate = null;
			String prevStatus = null;
			String currentStatus = null;
			java.util.regex.Pattern statusPattern = java.util.regex.Pattern.compile("(?<=\")(.+)(?=\")");
			java.util.regex.Matcher mSP = null;
			String[][] tuplas = collectStatusInformation();
			java.util.Map<String, String> espMap = fromTuples(tuplas, 1);
			java.util.Map<String, String> engMap = fromTuples(tuplas, 2);
			try{
				 lastDateApprovedDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse( lastDateApproved.replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.") );
				 log("Got last date approved: " + lastDateApproved + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(lastDateApprovedDate) + ")");
				 for(int i=0; i<records.length; i++) {
					 m = datePattern.matcher(records[i]);
					 if(m.find()) {
						 datePart = m.group();
						 if(records[i].startsWith("El usuario")) {
							 currentLogDate = new java.text.SimpleDateFormat("dd/MM/yyyy").parse(datePart);
							 log("Parsed date from Spanish message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else if(records[i].startsWith("The user")) {
							 currentLogDate = new java.text.SimpleDateFormat("MM/dd/yyyy").parse(datePart);
							 log("Parsed date from English message: " + datePart + " (" + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + ")");
						 }else {
							 currentLogDate = null;
						 }
						 if(currentLogDate != null) {
							 log("Comparing dates (currentLogDate vs lastDateApproved, " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(currentLogDate) + " vs " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(lastDateApprovedDate) + "): " + currentLogDate.compareTo(lastDateApprovedDate));
							 if(currentLogDate.compareTo(lastDateApprovedDate) < 0) {
								 break;
							 }else {
								 mSP = statusPattern.matcher(records[i]);
								 if(mSP.find()) {
									 if(records[i].startsWith("El usuario")) {
										 currentStatus = espMap.get(mSP.group());
										 log("Parsed status from Spanish message: " + currentStatus + " ( from: " + mSP.group() + ")" );
									 }else if(records[i].startsWith("The user")) {
										 currentStatus = engMap.get(mSP.group());
										 log("Parsed status from English message: " + currentStatus + " ( from: " + mSP.group() + ")" );
									 }else {
										 currentStatus = null;
									 }
									 if(currentStatus != null) {
										 if(prevStatus != null && "1022".equals(prevStatus) && "1026".equals(currentStatus)) {
											 log("Found a transition from Foro Process to QA within last approved time frame.");
											 return true;
										 }
										 prevStatus = currentStatus;
									 }
								 }
							 }
						 }
					 }
				 }
			}catch(java.text.ParseException e) {
				logE(e);
			}
		}
		return false;
	}
	
	private java.util.Map<String, String> fromTuples(String[][] tuplas, int index){
		java.util.Map<String, String> map = new java.util.TreeMap<>();
		if(tuplas != null && tuplas.length > 0) {
			if(index > 0 && index < tuplas[0].length) {
				for(int i=0; i<tuplas.length; i++) {
					map.put(tuplas[i][index], tuplas[i][0]);
				}
			}
		}
		return map;
	}
	
	private String[][] collectStatusInformation(){
		RESTWorkshop rw = new RESTWorkshop();
//		rw.setBaseUrl(rw.getBaseUrl());
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("GET", "/enum/Enum.ProductStatus", qp, null);
		java.util.Map<String, String> esp = new java.util.TreeMap<>();
		if(response != null) {
			org.json.JSONArray entries = response.getJSONArray("entries");
			for(int i=0; i<entries.length(); i++) {
				esp.put(entries.getJSONObject(i).getString("key"), entries.getJSONObject(i).getString("label"));
			}
		}
		rw.getRc().getHeader().put("Accept-Language", "en");
		response = rw.makeRequest("GET", "/enum/Enum.ProductStatus", qp, null);
		java.util.Map<String, String> eng = new java.util.TreeMap<>();
		if(response != null) {
			org.json.JSONArray entries = response.getJSONArray("entries");
			for(int i=0; i<entries.length(); i++) {
				eng.put(entries.getJSONObject(i).getString("key"), entries.getJSONObject(i).getString("label"));
			}
		}
		java.util.LinkedList<String[]> tuplas = new java.util.LinkedList<>();
		for(java.util.Map.Entry<String, String> entry : esp.entrySet()) {
			tuplas.addLast(new String[] {entry.getKey(), entry.getValue(), eng.get(entry.getKey())});
		}
		return tuplas.toArray(new String[][] {});
	}
	
	private void log(String m) {
		System.out.println(m);
	}
	
	private void logE(Exception e) {
		e.printStackTrace();
	}
}
