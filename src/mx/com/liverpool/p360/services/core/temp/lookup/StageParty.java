package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class StageParty {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "LookupValue.Code"
				+ ",LookupValueLang.Name(es)"
				+ ",LookupValueReference.LookupValues('TipoDeProveedorLOV')->LookupValue.Code"
				+ ",LookupValueReference.LookupValues('TipoProveedorSAPAttLOV')->LookupValue.Code"
				+ ",LookupValueReference.LookupValues('BusinessQualified')->LookupValue.Code"
			);
		qp.put("pageSize", "2000");
		qp.put("lookup", "'Party'");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "party").toString())))){
			rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> pw.println( rw.getRw().serializeChunk(new String[] { 
					  row.getJSONArray("values").getString(0)
					, row.getJSONArray("values").getString(1)
					, row.getJSONArray("values").getJSONArray(2).getString(0)
					, row.getJSONArray("values").getJSONArray(3).getString(0)
					, row.getJSONArray("values").getJSONArray(4).getString(0) 
				}, "\"", ";", "\\")), System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}

