package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SeparaDataDeSoloPlantillas {
	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp"), java.nio.charset.StandardCharsets.UTF_8)){
			try(
				java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "EU4SKUs.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8));
				java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "OtrosNoEU4SKUs.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))
			){
				pw.println("PIM_PROD_ID,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_NOM,PIM_PROD_TIPO_NOM,PIM_NIVEL_ID,PIM_PLANTILLA_ID,PIM_ATRIB_PRODUCTTYPESAP,PIM_ATRIB_PRODUCTNAME,PIM_ATRIB_ITEMGROUP,FCH_CARGA");
				pw2.println("PIM_PROD_ID,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_NOM,PIM_PROD_TIPO_NOM,PIM_NIVEL_ID,PIM_PLANTILLA_ID,PIM_ATRIB_PRODUCTTYPESAP,PIM_ATRIB_PRODUCTNAME,PIM_ATRIB_ITEMGROUP,FCH_CARGA");
				lines.map(workshop::parseLine).forEach(a->{
					if(a[6].startsWith("EU4-")) {
						pw.println( workshop.serializeChunk( a ) );
					}else {
						pw2.println( workshop.serializeChunk( a ) );
					}
				});
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

}
