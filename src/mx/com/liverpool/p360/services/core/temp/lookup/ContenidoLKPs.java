package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ContenidoLKPs {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Lookup.Identifier,LookupLang.Name(es)");
		qp.put("pageSize", "10000");
		qp.put("query", "not Lookup.Identifier is empty");
		java.util.Map<String, String> lookups = new java.util.HashMap<>();
		rw.collectData("list", "Lookup", null, "bySearch", qp, row -> {
			lookups.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
		});
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es),LookupValue.IsActive");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "DEV", "lkps").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println( rw.getRw().serializeChunk(new Object[] { "Lookup", "Nombre (Español)", "Code", "Etiqueta (Español)", "Activo" }) );
			for(java.util.Map.Entry<String, String> lkp : lookups.entrySet()) {
				qp.put("lookup", "'" + lkp.getKey() + "'");
				System.out.println("Querying data for: " + lkp.getKey());
				rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					pw.println( rw.getRw().serializeChunk(new Object[] { lkp.getKey(), lkp.getValue(), values.getString(0), values.getString(1), values.get(2) }) ); 
				});
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
