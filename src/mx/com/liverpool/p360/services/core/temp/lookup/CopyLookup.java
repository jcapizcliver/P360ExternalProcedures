package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CopyLookup {

	public static void main(String[] args) throws ServiceUnavailableException {
		CopyLookup cl = new CopyLookup();
//		cl.checkContent();
		cl.moveLookup("ATTYPLOV");
//		cl.moveLookup("BusinessQualified");
//		cl.moveLookup("CharacteristicPurposes");
//		cl.moveLookup("CharacteristicCategories");
//		cl.moveLookup("ExternalSystems");
//		cl.moveLookup("ATTYPLOV");
	}
	
	private void checkContent() throws ServiceUnavailableException {
		java.util.LinkedList<String> aa = checkLookups();
		RESTWorkshop rwq = new RESTWorkshop();
		rwq.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rwq.addHeader("Authorization", java.util.Base64.getEncoder().encodeToString( "jcapizc:algolindo".getBytes() ));
		rwq.putParameter("fields", "LookupValue.Code");
		rwq.putParameter("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int b = 0;
		for(String a0 : aa) {
			rwq.putParameter("lookup", "'" + a0 + "'");
			int a = 0;
			java.util.LinkedList<String> agg = new java.util.LinkedList<>();
			rwq.putParameter("startIndex", String.valueOf(a));
			response = rwq.makeRequest("GET", "/list/LookupValue/byLookup");
			if(response != null) {
				b = response.getInt("totalSize");
				if(b == 0) {
					System.out.println(a0);
					moveLookup(a0);
				}
			}else {
//				System.out.println(response);
			}
		}
	}
	
	private java.util.LinkedList<String> checkLookups() throws ServiceUnavailableException {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rwq = new RESTWorkshop();
		rwq.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rwq.addHeader("Authorization", java.util.Base64.getEncoder().encodeToString( "jcapizc:algolindo".getBytes() ));
		rwq.putParameter("fields", "Lookup.Identifier");
		rwq.putParameter("pageSize", "1200");
		rwq.putParameter("query", "not Lookup.Identifier is empty");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		int a = 0;
		int b = 0;
		java.util.LinkedList<String> agg = new java.util.LinkedList<>();
		do {
			rwq.putParameter("startIndex", String.valueOf(a));
			response = rwq.makeRequest("GET", "/list/Lookup/bySearch");
			if(response != null) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					agg.addLast(values.getString(0));
				}
				a += response.getInt("pageSize");
			}else {
			}
		}while(a < b);
		a = 0;
		return agg;
	}
	
	private void moveLookup(String lookup) throws ServiceUnavailableException {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rwd = new RESTWorkshop();
		RESTWorkshop rwq = new RESTWorkshop();
		rwq.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rwq.addHeader("Authorization", java.util.Base64.getEncoder().encodeToString( "jcapizc:algolindo".getBytes() ));
		rwd.putParameter("fields", "LookupValue.Code,LookupValueLang.Name(es),LookupValue.IsActive");
		rwd.putParameter("pageSize", "1200");
		rwd.putParameter("lookup", lookup);
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		int a = 0;
		int b = 0;
		org.json.JSONObject writeResponse = null;
		org.json.JSONArray objects = null;
		java.util.Map<String, String> toInternalIds = new java.util.TreeMap<>();
		java.util.ArrayList<String> currentIds = null;
		java.util.LinkedList<String> agg = new java.util.LinkedList<>();
		do {
			rwd.putParameter("startIndex", String.valueOf(a));
			response = rwd.makeRequest("GET", "/list/LookupValue/byLookup");
			if(response != null) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					agg.addLast(rows.getJSONObject(i).getJSONObject("object").getString("id"));
					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'" + lookup + "'")).put("values", new org.json.JSONArray().put(values.getString(1)).put(values.getBoolean(2))));
					if(rowsPayload.length() == 40) {
						writeResponse = rwq.makeRequest("POST", "/list/LookupValue", qp, request.toString());
						if(writeResponse != null) {
							objects = writeResponse.getJSONArray("objects");
							currentIds = new java.util.ArrayList<>(agg);
							for(int j = 0; j<objects.length(); j++) {
								toInternalIds.put(currentIds.get(j), objects.getJSONObject(j).getJSONObject("object").getString("id"));
							}
						}
						agg.clear();
						System.out.println(rwq.getRawResponse());
						while(rowsPayload.length() > 0) {
							rowsPayload.remove(0);
						}
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println(rwd.getRawResponse());
				rwd.getException().printStackTrace();
			}
		}while(a < b);
		a = 0;
		if(rowsPayload.length() > 0) {
			writeResponse = rwq.makeRequest("POST", "/list/LookupValue", qp, request.toString());
			if(writeResponse != null) {
				objects = writeResponse.getJSONArray("objects");
				currentIds = new java.util.ArrayList<>(agg);
				for(int j = 0; j<objects.length(); j++) {
					toInternalIds.put(currentIds.get(j), objects.getJSONObject(j).getJSONObject("object").getString("id"));
				}
			}
			agg.clear();
			System.out.println(rwq.getRawResponse());
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
		
		/***
		 * 
		 * 
		 * 	Building
		 * 
		 *******************/
		/*
		rwd.clearParameters();
		rwd.putParameter("lookup", lookup);
		rwd.putParameter("fields", "LookupValueReference.Lookup->Lookup.Identifier,LookupValueReference.LookupValues->LookupValue.Code");
		rwd.putParameter("pageSize", "1200");
		rwd.putParameter("includeIds", "true");
		response = rwd.makeRequest("GET", "/list/LookupValue/LookupValueReference/byLookup");
		a = 0;
		b = 0;
		String id = null;
		do {
			rwd.putParameter("startIndex", String.valueOf(a));
			response = rwd.makeRequest("GET", "/list/LookupValue/LookupValueReference/byLookup");
			if(response != null) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					id = toInternalIds.get(rows.getJSONObject(i).getJSONObject("object").getString("id"));
					if(id != null) {
						rowsPayload.put(
								new org.json.JSONObject()
									.put("object", new org.json.JSONObject().put("id", id))
									.put("values", new org.json.JSONArray().put(values.getJSONArray(1)))
									.put("qualification", new org.json.JSONObject().put("refLookup", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'"))));
						if(rowsPayload.length() == 200) {
							rwq.makeRequest("POST", "/list/LookupValue/LookupValueReference", qp, request.toString());
							while(rowsPayload.length() > 0) {
								rowsPayload.remove(0);
							}
						}
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println(rwd.getRawResponse());
			}
		}while(a < b);
		a = 0;
		if(rowsPayload.length() > 0) {
			rwq.makeRequest("POST", "/list/LookupValue/LookupValueReference", qp, request.toString());
			while(rowsPayload.length() > 0) {
				rowsPayload.remove(0);
			}
		}
		*/
	}
	
}
