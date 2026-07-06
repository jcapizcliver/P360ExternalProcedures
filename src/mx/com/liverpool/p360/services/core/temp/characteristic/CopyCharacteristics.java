package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CopyCharacteristics {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final java.util.Map<String, String> qp = new java.util.HashMap<>();
	
	static {
		rw.getRw().setBaseUrl("https://gcpcatqap01.liverpool.com.mx:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString( "rest:heiler".getBytes() ));
	}
	
	public static void main(String[] args) {
		java.util.List<String> idsQA = new java.util.ArrayList<>(20000);
		java.util.List<String> idsPR = new java.util.ArrayList<>(20000);
		RESTWrapper rwQA = new RESTWrapper();
		RESTWrapper rwPR = new RESTWrapper();
		rwQA.getRw().setBaseUrl("https://gcpcatqap01.liverpool.com.mx:1512/rest/V2.0");
		rwQA.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString( "rest:heiler".getBytes() ));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "not Characteristic.Identifier is empty");
		qp.put("pageSize", "20000");
		System.out.println("Pep");
		rwQA.collectData("list", "Characteristic", null, "bySearch", qp, row -> idsQA.add(row.getJSONArray("values").getString(0)));
		System.out.println("Mock");
		rwPR.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			if(!idsQA.contains( row.getJSONArray("values").getString(0)) )
				idsPR.add(row.getJSONArray("values").getString(0));
		});
		System.out.println(idsPR.size());
		StringBuilder sb = new StringBuilder();
		java.util.List<org.json.JSONObject> sinPas = new java.util.ArrayList<>();
		java.util.Map<String, org.json.JSONObject> board = new java.util.HashMap<>();
		rwPR.getRw().getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		qp.clear();
		for(int i = 0; i < idsPR.size(); i++) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'");
			sb.append(idsPR.get(i)); // 'ColoursLiverpoolAtt','TamanoUnico','AE416','AnchoVaD_Rechazo'
			sb.append("'");
			if(i % 100 == 0) {
				qp.put("items", sb.toString());
				rwPR.getRw().makeRequest("POST", "/object/Characteristic/byItems", qp, null);
				org.json.JSONObject object = null;
				org.json.JSONArray objects = new org.json.JSONArray(rwPR.getRw().getRawResponse());
				for(int j = 0; j<objects.length(); j++) {
					object = objects.getJSONObject(j).getJSONObject("_data");
					if(!object.has("_hijos")) {
						object.put("_hijos", new org.json.JSONArray());
					}
					if(object.has("parentCharacteristic") && !"".equals(object.getJSONObject("parentCharacteristic").getString("_code"))) {
						String parent = object.getJSONObject("parentCharacteristic").getString("_code");
						org.json.JSONObject pa = board.get(parent);
						if(pa == null) {
							pa = new org.json.JSONObject();
							board.put(parent, pa);
							pa.put("_hijos", new org.json.JSONArray());
						}
						pa.getJSONArray("_hijos").put(object);
					} else {
						sinPas.add(object);
					}
					org.json.JSONObject me = board.get(object.getString("identifier"));
					if(me == null) {
						board.put(object.getString("identifier"), object);
					}
				}
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			rwPR.getRw().getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
			qp.clear();
			qp.put("items", sb.toString());
			rwPR.getRw().makeRequest("POST", "/object/Characteristic/byItems", qp, null);
			org.json.JSONObject object = null;
			org.json.JSONArray objects = new org.json.JSONArray(rwPR.getRw().getRawResponse());
			for(int j = 0; j<objects.length(); j++) {
				object = objects.getJSONObject(j).getJSONObject("_data");
				if(!object.has("_hijos")) {
					object.put("_hijos", new org.json.JSONArray());
				}
				if(object.has("parentCharacteristic") && !"".equals(object.getJSONObject("parentCharacteristic").getString("_code"))) {
					String parent = object.getJSONObject("parentCharacteristic").getString("_code");
					org.json.JSONObject pa = board.get(parent);
					if(pa == null) {
						pa = new org.json.JSONObject();
						board.put(parent, pa);
						pa.put("_hijos", new org.json.JSONArray());
					}
					pa.getJSONArray("_hijos").put(object);
				}else {
					sinPas.add(object);
				}
				org.json.JSONObject me = board.get(object.getString("identifier"));
				if(me == null) {
					board.put(object.getString("identifier"), object);
				}
			}
		}
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println(sinPas.size());
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		for(org.json.JSONObject sinPa : sinPas ) {
			treePrint(sinPa, "");
		}
	}
	
	private static void treePrint(org.json.JSONObject o, String offset) {
//		if(!o.has("_hijos")) {
//			System.out.println("---->" + o + "<----");
//			return;
//		}
		org.json.JSONArray hijos = (org.json.JSONArray) o.remove("_hijos");
		rw.writeData("object", "Characteristic", null, qp, o, System.out::println);
		System.out.println(offset + o.getString("identifier"));
		for(int i=0; i<hijos.length(); i++) {
			treePrint(hijos.getJSONObject(i), offset + "\t");
		}
	}
}
