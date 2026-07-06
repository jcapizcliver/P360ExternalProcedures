package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class RechazosFaltantes {
	
	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		System.out.println("Running...");
		String[][] losesos = getVaD();
		java.util.ArrayList<String> vadOnly =  new java.util.ArrayList<>(java.util.Arrays.asList(losesos[0]));
		java.util.ArrayList<String> rechazos = new java.util.ArrayList<>(java.util.Arrays.asList(losesos[1]));
		String[] withValueProvider = losesos[2];
		System.out.println("VaD only: " + vadOnly.size() + " vs " + rechazos.size() + " (valueProvider: " + losesos[2].length + "), " + losesos[3].length);
		deactivate(losesos[3]);
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
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ides[i] + "'")).put("values", new org.json.JSONArray().put(false)));
			for(String child : hola) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + child + "'")).put("values", new org.json.JSONArray().put(false)));
					response = rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
					if(response == null) {
						System.out.println("ERR: " + rw.getRawResponse() + " || " + response);
					}else {
						System.out.println(response);
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
			}
			hola.clear();
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
		qp.put("query", "Characteristic.ParentCharacteristic = " + charId);
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
		java.util.LinkedList<String> valsRechazo = new java.util.LinkedList<>();
		java.util.LinkedList<String> vals = new java.util.LinkedList<>();
		java.util.LinkedList<String> withValueProvider = new java.util.LinkedList<>();
		java.util.LinkedList<String> mdr = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "Characteristic.IsActive = true and ( Characteristic.Identifier wildcard \"%VaD\" or Characteristic.Identifier wildcard \"%VaD_Rechazo\" )");
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
					if(values.getString(0).endsWith("VaD_Rechazo")) {
						valsRechazo.addLast(values.getString(0));
					}else{
						vals.addLast(values.getString(0));
					}
					if(!"".equals(values.getString(1))) {
						withValueProvider.addLast(values.getString(1));
					}
					if(values.getString(2).contains("Master")) {
						mdr.addLast(values.getString(0));
						System.out.println(values.getString(0) + " - " + values.getString(2));
					}
				}
			}else {
				System.out.println("Error en petición de características VaD: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return new String[][] {
			vals.toArray(new String[] {}), 
			valsRechazo.toArray(new String[] {}), 
			withValueProvider.toArray(new String[] {}), 
			mdr.toArray(new String[] {})
		};
	}

}
