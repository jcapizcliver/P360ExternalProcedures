package mx.com.liverpool.p360.services.core.temp.gcp;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ParseFilesToCheck {
	
	private static RESTWrapper rw = new RESTWrapper();

	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
//		long[] cnt = new long[1];
//		cnt[0] = 0;
//		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Detalle.csv.bkp"))){
//			lns.parallel().forEach(l -> cnt[0]++);
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
//		System.out.println("Done. (" + cnt[0] + ") " + rw.getRw().formatTime(System.currentTimeMillis() - init));

        java.util.concurrent.ConcurrentSkipListSet<String> processedIds = new java.util.concurrent.ConcurrentSkipListSet<>();
        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "IDs_"))){
        	lns.parallel().forEach(processedIds::add);
        }catch(java.io.IOException e) {
        	e.printStackTrace();
        }
		System.out.println("Done. (" + processedIds.size() + ") " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		java.util.Set<String> s1 = new java.util.TreeSet<>( processedIds );
		processedIds.clear();
		init = System.currentTimeMillis();
		java.util.concurrent.ConcurrentSkipListSet<String> cs = new java.util.concurrent.ConcurrentSkipListSet<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp"))){
			lns.parallel().map(l -> rw.getRw().parseLine(l)[0]).filter(s -> !s1.contains(s)).forEach(cs::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. (" + cs.size() + ") " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		init = System.currentTimeMillis();
		cs.forEach(System.out::println);
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
}
