package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SeparaciónDetalleParaAgrupación {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args){
		int[] cnt = new int[1];
		cnt[0] = 0;
		String header = "Plantilla,PIM_PROD_ID,PIM_ATRIBUTO_ID,PIM_ATRIBUTO_VAL_ID,PIM_ATRIBUTO_VAL,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_TIPO_NOM";
		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "OtroFinalSortedPlantillas.dat"), java.nio.charset.StandardCharsets.UTF_8)){
			try(
				java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "DataParaDeterminarDuplicados.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8));
			){
				pw1.println(header);
				lines.filter(l -> !header.equals(l))
					.map(workshop::parseLine)
					.forEach(a -> {
						if(a[0].startsWith("EU4")) {
							pw1.println( workshop.serializeChunk(a) );
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
