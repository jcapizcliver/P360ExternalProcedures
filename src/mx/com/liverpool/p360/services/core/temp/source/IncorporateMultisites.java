package mx.com.liverpool.p360.services.core.temp.source;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class IncorporateMultisites {

	private static final java.util.Map<String, String> map = new java.util.TreeMap<>();
	
	public static void main(String[] args) {
		java.util.LinkedList<String[]> rows = new java.util.LinkedList<>();
//		java.util.Map<String, String> tagToId = new java.util.TreeMap<>();
//		tagToId.put("", "");
		String currentRoot = null;
		String currentLvl1 = null;
		String currentLvl2 = null;
		String currentLvl3 = null;
		String currentLvl4 = null;
		java.util.Map<String, String> lvl0 = new java.util.TreeMap<>();
		java.util.Map<String, String> lvl1 = new java.util.TreeMap<>();
		java.util.Map<String, String> lvl2 = new java.util.TreeMap<>();
		java.util.Map<String, String> lvl3 = new java.util.TreeMap<>();
		java.util.Map<String, String> lvl4 = new java.util.TreeMap<>();
		java.util.Map<String, String> lvl6 = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\muestras multisitios\\muestra multisitios.txt")))){
			String line = null;
			String[] pieces = null;
			line = br.readLine();
			while((line = br.readLine()) != null) {
				pieces = line.split("\t");
				rows.addLast(pieces);
				if(pieces.length == 0 || ("".equals(pieces[0]) && "".equals(pieces[1]) && "".equals(pieces[2]) && "".equals(pieces[3]) && "".equals(pieces[4]) && "".equals(pieces[6]) )) {
					continue;
				}
//				System.out.println("-->" + pieces[0] + "<---->" + pieces[1] + "<---->" + pieces[2] + "<---->" + pieces[3] + "<---->" + pieces[4] + "<---->" + pieces[5] + "<---->" + pieces[6] + "<--");
				for(int i=1; i<pieces.length; i++) {
					if(!"".equals(pieces[i])) {
						map.put(pieces[i], pieces[i]);
					}
				}
				if(!"".equals(pieces[0])) {
					currentRoot = pieces[0];
					lvl0.put(currentRoot, "Sitios Web");
					map.put(currentRoot, pieces[0]);
				}
				if(!"".equals(pieces[1])) {
					currentLvl1 = pieces[1];
					lvl1.put(currentLvl1, currentRoot);
				}
				if(!"".equals(pieces[2])) {
					currentLvl2 = pieces[2];
					lvl2.put(currentLvl2, currentLvl1);
				}
				if(!"".equals(pieces[3])) {
					currentLvl3 = pieces[3];
					lvl3.put(currentLvl3, currentLvl2);
				}
				if(!"".equals(pieces[4])) {
					currentLvl4 = pieces[4];
					lvl4.put(currentLvl4, currentLvl3);
				}
				if(!"".equals(pieces[6])) {
					for(int i=0; i<pieces.length; i++) {
						if(i != 5 && !"".equals(pieces[i])) {
							lvl6.put(pieces[6], pieces[i]);
							break;
						}
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Loading data...");
		java.util.Map<String, String> etiquetasIDs = new java.util.TreeMap<>(); //collectInfo();
//		if(etiquetasIDs == null || etiquetasIDs.isEmpty()) {
//			System.out.println("No data to work with...");
//			System.exit(0);
//		}
//		uploadLevel(lvl0, etiquetasIDs, true);
		uploadLevel(lvl1, etiquetasIDs, false);
		uploadLevel(lvl2, etiquetasIDs, false);
		uploadLevel(lvl3, etiquetasIDs, false);
		uploadLevel(lvl4, etiquetasIDs, false);
		uploadLevel(lvl6, etiquetasIDs, false);
		
	}
	
	private static void uploadLevel(java.util.Map<String, String> level, java.util.Map<String, String> etiquetasIDs, boolean nou) {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		String holder = null;
		String holder2 = null;
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"));
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"));
		org.json.JSONObject o = null;
		for(java.util.Map.Entry<String, String> entry : level.entrySet()) {
			System.out.println("Writing: " + entry.getKey() + " - " + entry.getValue() + " | " + etiquetasIDs.get(entry.getKey()) + " - " + etiquetasIDs.get(entry.getValue()));
//			if(nou) {
//				holder = etiquetasIDs.get( entry.getKey() );
//				if(holder == null) {
//					System.out.println("No ID found for: " + entry.getKey());
//				}else {
			holder = entry.getKey();
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + (holder == null ? entry.getKey() : holder) + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(entry.getKey()).put(entry.getKey())));
//				}
//			}else {
//				holder = etiquetasIDs.get( entry.getKey() );
//				if(holder == null) {
//					System.out.println("No ID found for: " + entry.getKey());
//				}else {
//					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + (holder) + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(entry.getKey()).put(entry.getKey())));
//				}
//			}
		}
		rw.makeRequest("POST", "/list/StructureGroup", qp, request.toString());
		System.out.println(rw.getRawResponse());
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"));
		rows = new org.json.JSONArray();
		request.put("rows", rows);
		if(nou)
		System.exit(0);
		for(java.util.Map.Entry<String, String> entry : level.entrySet()) {
			if(!"".equals(entry.getValue())) {
				holder = etiquetasIDs.get( entry.getKey() );
				holder2 = etiquetasIDs.get( entry.getValue() );
//				if(holder == null || holder2 == null) {
//					System.out.println("No id found for: " + (holder == null ? entry.getKey() : entry.getValue()) );
//				}else {
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(entry.getKey()).put(entry.getKey()).put(entry.getValue() )));
//				}
			}else {
				System.out.println("Undesired: " + entry);
			}
		}
		System.out.println("Req: " + request.toString());
		rw.makeRequest("POST", "/list/StructureGroup", qp, request.toString());
		System.out.println(rw.getRawResponse());
	}
	
	private static java.util.Map<String, String> collectInfo(){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		qp.put("fields", "StructureGroupLang.Name(es),StructureGroup.Identifier");
		qp.put("structure", "Sitios Web");
		qp.put("pageSize", "800");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StructureGroup/byStructure", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values.getString(1));
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
}
