package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class RevisaFechas {

	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US);
		java.util.Map<java.time.LocalDateTime, Long> dateFreqs = null;
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp"))){
			dateFreqs = lns.parallel().map(rw::parseLine).map(a -> a[10]).filter(a -> !"FCH_CARGA".equals(a) && !"".equals(a)).map(a -> java.time.LocalDateTime.parse(a, formatter)).collect(java.util.stream.Collectors.groupingByConcurrent(java.util.function.Function.identity(), java.util.stream.Collectors.counting()));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.LinkedList<java.util.Map.Entry<java.time.LocalDateTime, Long>> lst = new java.util.LinkedList<>( dateFreqs.entrySet() );
		java.util.Collections.sort(lst, (o1,o2)->o2.getValue().compareTo(o1.getValue()));
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "MainTableDateDristribution1.dat").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			lst.forEach(en -> pw.println( rw.serializeChunk(new Object[] { formatter.format( en.getKey() ), en.getValue() }) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Collections.sort(lst, (o1,o2)->o2.getKey().compareTo(o1.getKey()));
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "MainTableDateDristribution2.dat").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			lst.forEach(en -> pw.println( rw.serializeChunk(new Object[] { formatter.format( en.getKey() ), en.getValue() }) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + rw.formatTime(System.currentTimeMillis() - init));
	}
	
}
