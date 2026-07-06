package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class DeshabilitaYElimina {

	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		deactivate( getVaD()[0] );
	}

	private static void dontLeaveUntillItIsDeactivated(String characteristic) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "Characteristic.Identifier equals \"" + characteristic + "\"");
		qp.put("fields", "Characteristic.IsActive");
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		org.json.JSONObject response = new org.json.JSONObject();
		boolean driver = true;
		do {
			System.out.println(characteristic + ". Deactivating...");
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response == null) {
				
			}else {
				rw.makeRequest("POST", "/list/Characteristic", empty, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + characteristic + "'")).put("values", new org.json.JSONArray().put(false)))).toString());
				driver = response.getJSONArray("rows").length() > 0 && "true".equals( response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) );
				System.out.println(response.getJSONArray("rows"));
			}
		}while(driver);
		delete(characteristic);
	}
	
	private static void delete(String characteristicId) {
		RESTWorkshop rw = new RESTWorkshop();
		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "Characteristic.Identifier equals \"" + characteristicId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("DELETE", "/list/Characteristic/bySearch", qp, null);
		System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
	}
	
	private static void deactivate(String[] ides) {
		System.out.println("Deactivating");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"));
		org.json.JSONObject response = null;
		java.util.LinkedList<String> hola = new java.util.LinkedList<>();
		for(int i=0; i<ides.length; i++) {
			collectChildren(ides[i], hola);
//			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ides[i] + "'")).put("values", new org.json.JSONArray().put(false)));
			for(String child : hola) {
				dontLeaveUntillItIsDeactivated(child);
//				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + child + "'")).put("values", new org.json.JSONArray().put(false)));
//				response = rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
//				if(response == null) {
//					System.out.println("ERR: " + rw.getRawResponse() + " || " + response);
//				}else {
//					System.out.println(response);
//				}
//				while(rows.length() > 0) {
//					rows.remove(0);
//				}
			}
			dontLeaveUntillItIsDeactivated(ides[i]);
//			hola.clear();
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
			if(response == null) {
				System.out.println("ERR: " + rw.getRawResponse());
			}else {
				System.out.println(response);
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private static void collectChildren(String charId, java.util.LinkedList<String> children) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.ParentCharacteristic = \"" + charId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				children.addLast(values.getString(0));
			}
		}else {
			System.out.println("ERR: " + rw.getRawResponse());
		}
		
	}
	
	private static String[][] getVaD(){
		java.util.LinkedList<String> mdr = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "Characteristic.Category->LookupValue.Code wildcard \"MasterData_%\"");
		qp.put("fields", "Characteristic.Identifier,Characteristic.ValueProviderId,Characteristic.Category->LookupValue.Code");
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
					System.out.println(values);
					if(values.getString(2).contains("Master")) {
						mdr.addLast(values.getString(0));
//						System.out.println(values.getString(0) + " - " + values.getString(2));
					}
				}
			}else {
				System.out.println("Error en petición de características VaD: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return new String[][] {
			mdr.toArray(new String[] {})
		};
	}
}
