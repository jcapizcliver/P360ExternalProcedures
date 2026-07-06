package mx.com.liverpool.p360.services.core.temp.hierarchies;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ReclassifyBros {
	
	public static void main(String[] args) {
		ReclassifyBros rb = new ReclassifyBros();
		java.util.LinkedList<String[]> ids = new java.util.LinkedList<>();
		rb.getMeTheBros(ids);
		rb.classifyECC(ids);
		
	}

	private void classifyECC(java.util.LinkedList<String[]> ids) {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject response = null;
		for(String[] pieces : ids) {
			System.out.println("Placing: " + pieces[2] + pieces[1] + " for: " + pieces[0]);
			System.out.println( rw.makeRequest("PUT", "/object/Product2G/'" + pieces[0] + "'@'MASTER'", qp, new org.json.JSONObject().put("structureGroupMap", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + pieces[2] + pieces[1] + "-L5ECC'@'CommercialECC'"))))).toString() ) );
//			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'MASTER'")).put("values", new org.json.JSONArray().put(new org.json.JSONArray().put(new org.json.JSONObject().put("id", "'" + pieces[2] + pieces[1] + "-L5ECC'@'CommercialECC'")))));
//			if(rows.length() == 2) {
//				response = rw.makeRequest("POST", "/list/Product2G", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.StructureGroup(CommercialECC)->StructureGroup.Identifier"))).put("rows", rows).toString());
//				if(response == null) {
//					System.out.println("Error: " + rw.getRawResponse());
//				}else {
//					System.out.println("Processed: " + response);
//				}
//				while(rows.length() > 0) {
//					rows.remove(0);
//				}
//			}
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/Product2G", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.StructureGroup(CommercialECC)"))).put("rows", rows).toString());
			if(response == null) {
				System.out.println("Error: " + rw.getRawResponse());
			}else {
				System.out.println("Processed: " + response);
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private void getMeTheBros(java.util.LinkedList<String[]> ids) {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy) is empty and characteristic('Business',-1) = 'LVP'@'BusinessQualified' and not characteristic('ProductTypeSAP',-1) is empty" /* and not Product2GCharacteristicValue.LookupValue('ProductTypeSAPTEMP',root,\"0000.0000.RK\",'ProductTypeSAPTEMP',-1) is empty" */);
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP',-1)->LookupValue.Code,Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup',-1)->LookupValue.Code"/*Product2GCharacteristicValue.LookupValue('ProductTypeSAPTEMP',root,\"0000.0000.RK\",'ProductTypeSAPTEMP',-1)->LookupValue.Code"*/);
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		String[] pieces = null;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					pieces = new String[3];
					pieces[0] = values.getString(0);
					pieces[1] = values.getJSONArray(1).getString(0);
					pieces[2] = values.getJSONArray(2).getString(0);
					ids.addLast(pieces);
					System.out.println(rows.getJSONObject(i).getJSONArray("values"));
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		System.out.println("Total size: " + totalSize);
	}
	
}
