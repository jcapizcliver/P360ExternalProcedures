package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class StageSpecificLookupValues extends RESTWrapper {

	
	public static void main(String[] args) {
		StageSpecificLookupValues ss = new StageSpecificLookupValues();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("pageSize", "5000");
		qp.put("lookup", "'" + args[0] + "'");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("")))){
			ss.collectData("list", "LookupValue", null, "byLookup", qp, row -> pw.println( ss.getRw().serializeChunk( new String[] { row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1) }, "\"", ";", "\\" ) ), null);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
