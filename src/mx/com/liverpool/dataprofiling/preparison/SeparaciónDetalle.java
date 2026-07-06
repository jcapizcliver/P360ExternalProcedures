package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SeparaciónDetalle {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args){
		int[] cnt = new int[1];
		cnt[0] = 0;
		java.util.Set<String> mdf = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.Category->LookupValue.Code = \"Master Data\" or Characteristic.Category->LookupValue.Code = \"DatosLogisticos\"");
		qp.put("pageSize", "10000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> mdf.add(row.getJSONArray("values").getString(0)) );
		mdf.removeIf( a -> a.endsWith("_Rechazo") );
		String header = "Template,PIM_PROD_ID,PIM_ATRIBUTO_ID,PIM_ATRIBUTO_VAL_ID,PIM_ATRIBUTO_VAL,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_TIPO_NOM";
		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "OtroFinalSortedPlantillas.dat"), java.nio.charset.StandardCharsets.UTF_8)){
			try(
				java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "AtributosPlantillas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8));
				java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosMaestrosPlantillas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8));
				java.io.PrintWriter pw3 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "AtributosNoPlantillas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8));
				java.io.PrintWriter pw4 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosMaestrosNoPlantillas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))
			){
				pw1.println(header);
				pw2.println(header);
				pw3.println(header);
				pw4.println(header);
				lines.filter(l -> !header.equals(l))
					.map(workshop::parseLine)
					.forEach(a -> {
						if(
								   "Negocio".equals(a[2]) 
								|| "EXTWG_S4H".equals(a[2]) /* ||  CreateTemplatesAsTables.definingAttributes.contains(a[2]) */ 
								|| a[2].endsWith("VaD") 
								|| a[2].endsWith("VAD") 
								|| (a[2].endsWith("Att") && !"ColoursLiverpoolAtt".equals(a[2]))
								|| a[2].startsWith("SB_") 
								|| a[2].length() == 5)
						{
							if(a[0].startsWith("EU4")) {
								if(!mdf.contains(a[2])) {
									pw1.println( workshop.serializeChunk(a) );
								}else {
									pw2.println( workshop.serializeChunk(a) );
								}
							}else {
								if(!mdf.contains(a[2])) {
									pw3.println( workshop.serializeChunk(a) );
								}else {
									pw4.println( workshop.serializeChunk(a) );
								}
							}
						}else {
							if(a[0].startsWith("EU4")) {
								pw2.println( workshop.serializeChunk(a) );
							}else {
								pw4.println( workshop.serializeChunk(a) );
							}
						}
						cnt[0]++;
						if(cnt[0] % 10_000_000 == 0) {
							System.out.print(".");
							if(cnt[0] % 100_000_000 == 0) {
								System.out.println(cnt[0]);
							}
						}
					});
				System.out.println(cnt[0]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
