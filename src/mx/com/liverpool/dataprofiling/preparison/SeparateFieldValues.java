package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SeparateFieldValues {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		java.util.Set<String> mdf = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.Category->LookupValue.Code = \"Master Data\"");
		qp.put("pageSize", "10000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> mdf.add(row.getJSONArray("values").getString(0)) );
		System.out.println(mdf.size());
		mdf.removeIf( a -> a.endsWith("_Rechazo") );
		System.out.println(mdf.size());
		try(
			java.io.PrintWriter pw  = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "DetalleDatosMaestros.dat").toFile()), java.nio.charset.StandardCharsets.UTF_8));
			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "DetalleAtributos.dat").toFile())));
			java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Final.dat"))
		){
			pw.println(  workshop.serializeChunk(new Object[] {"PIM_PROD_ID", "PIM_ATRIBUTO_ID", "PIM_ATRIBUTO_VAL_ID", "PIM_ATRIBUTO_VAL"}) );
			pw2.println( workshop.serializeChunk(new Object[] {"PIM_PROD_ID", "PIM_ATRIBUTO_ID", "PIM_ATRIBUTO_VAL_ID", "PIM_ATRIBUTO_VAL"}) );
			lines.map(workshop::parseLine).forEach(a -> {
				if(!"PIM_PROD_ID".equals(a[0])) {
					if(mdf.contains(a[1])) {
						pw.println( workshop.serializeChunk( a ) );
					}else {
						pw2.println( workshop.serializeChunk(a) ); 
					}
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	 }
	
}
