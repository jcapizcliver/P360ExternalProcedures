package mx.com.liverpool.p360.services.core.temp.move.characteristics;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class MoveCharacteristics {

	
	public static void main(String[] args) {
		MoveCharacteristics m = new MoveCharacteristics();
		GeneralOperations go = new GeneralOperations();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, org.json.JSONArray> data = null;
		java.util.Map<String, org.json.JSONArray> data2 = null;
		java.util.LinkedList<org.json.JSONArray> brandNew = new java.util.LinkedList<>();
		org.json.JSONArray values = null;
		RESTWorkshop rw0 = new RESTWorkshop();
		rw0.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rw0.addHeader("Authorization", "Basic: " + java.util.Base64.getEncoder().encodeToString(("rest:heiler").getBytes()));
		System.out.println("Collecting values source 1");
		data = go.collectCharacteristic(rw0);
		System.out.println("Collecting values source 2");
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", "Basic: " + java.util.Base64.getEncoder().encodeToString(("jcapizc:algolindo").getBytes()));
		data2 = go.collectCharacteristic(rw);
		System.out.println("Now performing...");
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Identifier"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Category"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Entities"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Order"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.LowerBound"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.UpperBound"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsMultiValue"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsMultiLine"));
		columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsLanguageSpecific"));
		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(en)"));
		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Description(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Description(en)"));
		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.DefaultValue(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.DefaultValue(en)"));
//		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicIdentifier.AlternativeIdentifier(ECC)"));
//		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"));
//		columns.put(new org.json.JSONObject().put("identifier", "CharacteristicIdentifier.AlternativeIdentifier(ATG)"));
		System.out.println("Data: " + data.size());
		for(java.util.Map.Entry<String, org.json.JSONArray> entry : data.entrySet()) {
			values = data2.get(entry.getKey());
			if(values != null) {
				System.out.print("Ev: " + entry.getKey() + " . " + entry.getValue()); 
				go.chooseToApply(entry.getValue(), values, columns, rw, m::modifyCharacteristic);
			}else {
				brandNew.addLast(entry.getValue());
			}
		}
		request.put("columns", columns);
		request.put("rows", rows);
		System.out.println("Brand new: " + brandNew.size());
		for(org.json.JSONArray nv : brandNew) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + nv.getString(0) + "'"))
					.put("values", nv));
			if(rows.length() == 300) {
				rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
				System.out.println(rw.getRawResponse());
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			rw.makeRequest("POST", "/list/Characteristic", qp, request.toString());
			System.out.println(rw.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private void modifyCharacteristic(org.json.JSONArray columns, org.json.JSONArray values, RESTWorkshop rw) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		System.out.println(values);
		rw.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")).put("values", new org.json.JSONArray().put(false)) ) ).toString() );
		System.out.println("From disabling characteristic " + values.getString(0) + ": " + rw.getRawResponse());
		rw.makeRequest("POST", "/list/Characteristic", qp, adjustData(values, columns).toString());
		System.out.println("From updating characteristic: " + values.getString(0) + ": " + rw.getRawResponse());
		rw.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", new org.json.JSONArray().put( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")).put("values", new org.json.JSONArray().put(true)) ) ).toString() );
		System.out.println("From enabling characteristic: " + values.getString(0) + ": " + rw.getRawResponse());
	}
	
	public org.json.JSONObject adjustData(org.json.JSONArray values, org.json.JSONArray columns){
		org.json.JSONArray nv = new org.json.JSONArray();
		org.json.JSONArray nc = new org.json.JSONArray();
		for(int i=0; i<values.length() ; i++) {
			nv.put(values.get(i));
			nc.put(columns.get(i));
		}
		for(int i=values.length(); i<values.length(); i++) {
			if(!"".equals(values.getString(i))) {
				nv.put(values.get(i));
				nc.put(columns.get(i));
			}
		}
		return new org.json.JSONObject().put("columns", nc).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")).put("values", nv)));
	}

}
