package mx.com.liverpool.p360.services.core.temp.structuregroups;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class DeleteStructureGroups {

	
	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		RESTWorkshop rw0 = new RESTWorkshop("Accept: application/json", "Content-Type: application/x-www-form-urlencoded", "Authorization: " + rw.getRc().getHeader().get("Authorization"));
		org.json.JSONObject response = null;
		org.json.JSONObject jo = null;
		org.json.JSONObject jo2 = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONObject> board = new java.util.TreeMap<>();
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		java.util.Map<String, String> theirParents = new java.util.TreeMap<>();
		java.util.LinkedList<org.json.JSONArray> valuesList = new java.util.LinkedList<>();
		java.util.LinkedList<org.json.JSONObject> level0 = new java.util.LinkedList<>();
		java.util.LinkedList<org.json.JSONObject> singleLevel0 = new java.util.LinkedList<>();
		String prevLevel = null;
		String parent = null;
		StringBuilder sb = new StringBuilder();
		int currentIndex = 0;
		int totalSize = 0;
		
		qp.put("fields", "StructureGroup.Identifier,StructureGroup.Level,StructureGroup.ParentIdentifier,StructureGroupLang.Name(es)");
		qp.put("structure", "Sitios Web");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StructureGroup/byStructure", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					currentIndex++;
					jo = board.get(values.getString(0));
					if(jo == null) {
						jo = new org.json.JSONObject();
						jo.put("values", values);
						jo.put("identifier", values.getString(0));
						jo.put("parent", values.getString(2));
						jo.put("name", values.getString(3));
						jo.put("children", new org.json.JSONArray());
						board.put(values.getString(0), jo);
					}
					if(!jo.has("values")) {
						jo.put("values", values);
					}
					jo2 = board.get(values.getString(2));
					if(jo2 == null) {
						jo2 = new org.json.JSONObject();
						jo2.put("identifier", values.getString(2));
						jo2.put("children", new org.json.JSONArray());
						board.put(values.getString(2), jo2);
					}
					jo2.getJSONArray("children").put(jo);
					if("1".equals(values.getString(1))) {
						level0.addLast(jo);
						System.out.println("Adding: " + jo.getString("identifier"));
					}
					if(values.getString(0).startsWith("StructureGroup"))
						valuesList.addLast(values);
				}
				System.out.println(currentIndex + "/" + totalSize);
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		System.out.println("Now sorting...");
		java.util.Collections.sort( valuesList, (o1,o2)-> Integer.compare(Integer.parseInt(o2.getString(1)), Integer.parseInt(o1.getString(1))) );
		valuesList.forEach(System.out::println);
		qp0.put("structure", "Sitios Web");
		for(org.json.JSONArray vls : valuesList) {
			if(prevLevel != null && !prevLevel.equals(vls.getString(1))) {
				qp0.put("query", "StructureGroup.Identifier in (" + sb.toString() + ")");
				rw0.makeRequest("DELETE", "/list/StructureGroup/bySearch", qp0, null);
				System.out.println("From deleting (" + prevLevel + "): " + rw0.getRawResponse() );
				sb.setLength(0);
			}
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("\"");
			sb.append(vls.getString(0));
			sb.append("\"");
			prevLevel = vls.getString(1);
		}
		qp0.put("query", "StructureGroup.Identifier in (" + sb.toString() + ")");
		rw0.makeRequest("DELETE", "/list/StructureGroup/bySearch", qp0, null);
		System.out.println("From deleting (" + prevLevel + "): " + rw0.getRawResponse() );
		sb.setLength(0);
		for(org.json.JSONObject jo0 : level0) {
//			System.out.println(jo0);
			if(jo0.getJSONArray("children").length() == 0) {
				sb.append(sb.length() == 0 ? "" : ",");
				sb.append("\"");
				sb.append(jo0.getString("identifier"));
				sb.append("\"");
			}else {
				singleLevel0.addLast(jo0);
			}
		}
		qp0.put("query", "StructureGroup.Identifier in (" + sb.toString() + ")");
		rw0.makeRequest("DELETE", "/list/StructureGroup/bySearch", qp0, null);
		System.out.println("From deleting (" + prevLevel + "): " + rw0.getRawResponse() );
		sb.setLength(0);
		java.util.Map<String, String> oConEtiqueta = new java.util.TreeMap<>();
		java.util.Map<String, String> etiquetaConId = new java.util.TreeMap<>();
		java.util.LinkedList<String[]> losQueNoTengo = new java.util.LinkedList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\muestras multisitios\\WebHierarchy.txt")))){
			String ln = null;
			String[] pieces = null;
			String[] header = null;
			header = br.readLine().split("\t");
			while((ln = br.readLine()) != null) {
				pieces = ln.split("\t");
				if(!"Classification 1 root".equals(pieces[2])) {
					theirParents.put(pieces[0], pieces[2]);
					oConEtiqueta.put(pieces[1], pieces[2]);
					etiquetaConId.put(pieces[1], pieces[0]);
					if(!board.containsKey(pieces[0])) {
						losQueNoTengo.addLast(pieces);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		for(org.json.JSONObject jo0 : level0) {
			
			parent = theirParents.get(jo0.getString("identifier"));
			if(parent != null) {
				if(!"WebHierarchyRoot".equals(parent)) {
					System.out.println("This needs to be reclassified: " + jo0.getString("identifier"));
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + jo0.getString("identifier") + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(parent)));
					if(rows.length() == 10) {
						rw.makeRequest("POST", "/list/StructureGroup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))).put("rows", rows).toString());
						System.out.println(rw.getRawResponse());
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}else {
					System.out.println("Already: " + jo0.getString("identifier"));
				}
			}else {
				System.out.println("Este que pedo: " + jo0.getString("identifier"));
//				losQueNoTengo.addLast(new String[] { jo0.getString("identifier"), jo0.getString("name"), jo0.getString("parent") });
			}
			
		}
		if(rows.length() > 0) {
			rw.makeRequest("POST", "/list/StructureGroup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))).put("rows", rows).toString());
			System.out.println(rw.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

		if(!losQueNoTengo.isEmpty()) {
			System.out.println("Los que no tengo son: " + losQueNoTengo.size());
			rows = new org.json.JSONArray();
			for(String[] pcs : losQueNoTengo) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pcs[0] + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(pcs[1]).put(pcs[1])));
				if(rows.length() == 10) {
					rw.makeRequest("POST", "/list/StructureGroup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))).put("rows", rows).toString());
					System.out.println(rw.getRawResponse());
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				rw.makeRequest("POST", "/list/StructureGroup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)"))).put("rows", rows).toString());
				System.out.println(rw.getRawResponse());
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
			for(String[] pcs : losQueNoTengo) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pcs[0] + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(pcs[2])));
				if(rows.length() == 10) {
					rw.makeRequest("POST", "/list/StructureGroup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))).put("rows", rows).toString());
					System.out.println(rw.getRawResponse());
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				rw.makeRequest("POST", "/list/StructureGroup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))).put("rows", rows).toString());
				System.out.println(rw.getRawResponse());
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		rows = new org.json.JSONArray();
		java.util.LinkedList<org.json.JSONArray> toDelete = new java.util.LinkedList<>();
		for(org.json.JSONObject jo0 : singleLevel0) {
			parent = theirParents.get(jo0.getString("identifier"));
			if(parent != null) {
				System.out.println("Found parent for: " + jo0.getString("identifier") + ": " + parent);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + jo0.getString("identifier") + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(parent)));
			}else {
				parent = oConEtiqueta.get(jo0.getString("identifier"));
				if(parent != null) {
					System.out.println("Found con la etiqueta: " + jo0.getString("identifier") + ", " + parent);
					if(etiquetaConId.containsKey(jo0.getString("identifier")) && !board.containsKey(etiquetaConId.get(jo0.getString("identifier")))) {
						System.out.println("\tGarche: " + jo0.getString("identifier"));
						rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + jo0.getString("identifier") + "'@'Sitios Web'")).put("values", new org.json.JSONArray().put(parent)));
					}else {
						System.out.println("Found uno que ya se llama como su ai dí: " + board.get(etiquetaConId.get(jo0.getString("identifier"))));
						grabAllChildrenInvolved(jo0, toDelete);
					}
				}else {
					System.out.println("Didn't find a parent for: " + jo0.getString("identifier"));
				}
			}
		}
		if(rows.length() > 0) {
			rw.makeRequest("POST", "/list/StructureGroup", new java.util.TreeMap<>(), new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))).put("rows", rows).toString());
			System.out.println(rw.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		
		java.util.Collections.sort( toDelete, (o1,o2)-> Integer.compare(Integer.parseInt(o2.getString(1)), Integer.parseInt(o1.getString(1))) );
		valuesList.forEach(System.out::println);
		qp0.put("structure", "Sitios Web");
		for(org.json.JSONArray vls : toDelete) {
			if(prevLevel != null && !prevLevel.equals(vls.getString(1))) {
				qp0.put("query", "StructureGroup.Identifier in (" + sb.toString() + ")");
				rw0.makeRequest("DELETE", "/list/StructureGroup/bySearch", qp0, null);
				System.out.println("From deleting (" + prevLevel + "): " + rw0.getRawResponse() );
				sb.setLength(0);
			}
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("\"");
			sb.append(vls.getString(0));
			sb.append("\"");
			prevLevel = vls.getString(1);
		}
		qp0.put("query", "StructureGroup.Identifier in (" + sb.toString() + ")");
		rw0.makeRequest("DELETE", "/list/StructureGroup/bySearch", qp0, null);
	}
	
	private static void grabAllChildrenInvolved(org.json.JSONObject boy, java.util.LinkedList<org.json.JSONArray> ladata) {
		ladata.addLast(boy.getJSONArray("values"));
		org.json.JSONArray children = boy.getJSONArray("children");
		for(int i=0; i<children.length(); i++) {
			grabAllChildrenInvolved(children.getJSONObject(i), ladata);
		}
	}
	
}
