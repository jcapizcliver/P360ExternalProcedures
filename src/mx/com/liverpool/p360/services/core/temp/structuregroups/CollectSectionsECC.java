package mx.com.liverpool.p360.services.core.temp.structuregroups;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectSectionsECC {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es)");
		qp.put("query", "StructureGroup.Identifier wildcard \"%-L3ECC\"");
		qp.put("structure", "CommercialECC");
		qp.put("pageSize", "10000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "SeccionesECC.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println( rw.getRw().serializeChunk( new Object[] { "Identifier", "Label" } ) );
			rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				pw.println( rw.getRw().serializeChunk( new Object[] { values.getString(0), values.getString(1) } ) );
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
