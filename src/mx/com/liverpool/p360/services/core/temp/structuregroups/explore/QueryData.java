package mx.com.liverpool.p360.services.core.temp.structuregroups.explore;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QueryData {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		// structure=CommercialECC
		// &fields=
		// &query=StructureGroupLang.Name(es) wildcard "%BICI%"
		// &metaData=true 
		qp.put("structure", "CommercialECC");// https://webctep360pro.liverpool.com.mx/rest/V2.0/list/StructureGroup/bySearch?
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es),StructureGroupLang.Name(en),StructureGroup.ParentIdentifier");
		qp.put("query", "StructureGroupLang.Name(es) containsIC \"BICI\"");
		rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			java.util.List<String> data = new java.util.ArrayList<>();
			for(int i=0; i<values.length(); i++) {
				data.add( values.getString(i) );
			}
			System.out.println( rw.getRw().serializeChunk( data.toArray(new String[] {}) ) );
		});
		qp.put("structure", "PrimaryProductTaxonomy");// https://webctep360pro.liverpool.com.mx/rest/V2.0/list/StructureGroup/bySearch?
		qp.put("query", "StructureGroupLang.Name(es) containsIC \"Bici\"");
		rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			java.util.List<String> data = new java.util.ArrayList<>();
			for(int i=0; i<values.length(); i++) {
				data.add( values.getString(i) );
			}
			System.out.println( rw.getRw().serializeChunk( data.toArray(new String[] {}) ) );
		});
		
	}
}
