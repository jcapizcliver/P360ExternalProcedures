package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class RemovePurposes {

	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		updateTheNutrias( getStructureGroupAttributesWithPurposes() );
		removeValueProviderId( getVaD()[0] );
	}
	
	private static void removeValueProviderId(String[] ids) {
		System.out.println("Updating Purposes");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"));
		request.put("columns", columns);
		request.put("rows", rows);
		for(int i=0; i<ids.length; i++) {
			System.out.println("ID: " + ids[i]);
			rows.put(
					new org.json.JSONObject()
						.put("object", new org.json.JSONObject().put("id", "'" + ids[i] + "'"))
						.put("values", new org.json.JSONArray().put(new org.json.JSONArray()))
					);
			if(rows.length() == 200) {
				response = rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
				System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
			System.out.println(response == null ? "ERR: " + rw.getRawResponse() : response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private static String[][] getVaD(){
		java.util.LinkedList<String> vals = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "not Characteristic.Purposes is empty");
		qp.put("fields", "Characteristic.Identifier");
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
					vals.addLast(values.getString(0));
				}
			}else {
				System.out.println("Error en petición de características VaD: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return new String[][] {
			vals.toArray(new String[] {}), 
		};
	}
	
	private static org.json.JSONObject[] getStructureGroupAttributesWithPurposes(){
		java.util.LinkedList<org.json.JSONObject> vals = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StructureGroupAttribute.Purpose");
		qp.put("structure", "O_BIS_T");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		
		int currentIndex = 0;
		int totalSize = 0;
		
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StructureGroup/StructureGroupAttribute/byStructure", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				totalSize = response.getInt("rowCount");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!"".equals(values.getJSONArray(0).getString(0))) {
						System.out.println(rows.getJSONObject(i));
						vals.add(rows.getJSONObject(i));
					}
				}
			}else {
				System.out.println("Error en petición de características VaD: " + rw.getRawResponse());
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex > 0 && currentIndex < totalSize);
		currentIndex = 0;
		return vals.toArray(new org.json.JSONObject[] {});
	}

	
	private static void updateTheNutrias(org.json.JSONObject[] rows){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		for(int i=0; i<rows.length; i++) {
			rows[i].put("values", new org.json.JSONArray().put(new org.json.JSONArray()));
		}
		response = rw.makeRequest("POST", "/list/StructureGroup/StructureGroupAttribute", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroupAttribute.Purpose"))).put("rows", rows).toString());
		System.out.println( response == null ? "ERR: " + rw.getRawResponse() : response );
	}

}
