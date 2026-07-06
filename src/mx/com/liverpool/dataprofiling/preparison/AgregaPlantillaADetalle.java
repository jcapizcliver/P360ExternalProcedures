package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AgregaPlantillaADetalle {

	private static final RESTWrapper rw = new RESTWrapper();	
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis(); 
		java.util.concurrent.ConcurrentMap<String, String[]> productTableID = null;
		try( java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp")) ){
			productTableID = lines
					.parallel()
					.filter(l -> !"PIM_PROD_ID,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_NOM,PIM_PROD_TIPO_NOM,PIM_NIVEL_ID,PIM_PLANTILLA_ID,PIM_ATRIB_PRODUCTTYPESAP,PIM_ATRIB_PRODUCTNAME,PIM_ATRIB_ITEMGROUP,FCH_CARGA".equals(l))
					.map(workshop::parseLine)
					.collect(java.util.stream.Collectors.toConcurrentMap(a -> a[0], a -> new String[] { "null".equals( a[1] ) ? "" : a[1], "null".equals( a[2] ) ? "" : a[2], "null".equals( a[4] ) ? "" : a[4], "null".equals( a[9] ) ? "" : a[9] }))
				;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Built map. " + workshop.formatTime(System.currentTimeMillis() - init));
		System.out.println("Now writing new file...");
		java.util.concurrent.ConcurrentMap<String, String[]> meh = productTableID;
		try( 
			java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Detalle.csv.bkp"), java.nio.charset.StandardCharsets.UTF_8);
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "OtroFinalUnsortedGrupoDeArtículos.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8)) 
		){
			pw.println( workshop.serializeChunk(new Object[] { "ItemGroup", "PIM_PROD_ID", "PIM_ATRIBUTO_ID", "PIM_ATRIBUTO_VAL_ID", "PIM_ATRIBUTO_VAL", "PIM_SKU_CVE", "PIM_PADRE_SKU_CVE", "PIM_PROD_TIPO_NOM" }) );
			lines
				.filter(l -> !l.startsWith("PIM_PROD_ID"))
				.map(workshop::parseLine)
				.forEach(a -> {
					String[] pieces = meh.get(a[0]);
					pw.println( workshop.serializeChunk( new Object[] { pieces[3], a[0], a[1], a[2], a[3], pieces[0], pieces[1], pieces[2] } ) );
				})
			;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(productTableID.size());
	}
	
}
