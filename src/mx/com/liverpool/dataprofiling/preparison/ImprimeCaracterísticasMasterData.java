package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ImprimeCaracterísticasMasterData {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Set<String> mdf = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.Category->LookupValue.Code = \"Master Data\" or Characteristic.Category->LookupValue.Code = \"DatosLogisticos\"");
		qp.put("pageSize", "10000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> mdf.add(row.getJSONArray("values").getString(0)) );
		System.out.println(mdf.size());
		mdf.removeIf( a -> a.endsWith("_Rechazo") );
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "MasterDatas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println("Characteristic");
			mdf.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
