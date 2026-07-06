package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CuantosAtributosNetosHay {

	private static final RESTWorkshop rw = new RESTWorkshop();
//	private static final java.util.concurrent.ConcurrentSkipListSet<String> chm = new java.util.concurrent.ConcurrentSkipListSet<>();
	private static final java.util.concurrent.ConcurrentMap<String, Long> chm = new java.util.concurrent.ConcurrentHashMap<>();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Set<String> refs = null;
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "EU4SKUs.csv"), java.nio.charset.StandardCharsets.UTF_8)){
			 refs = lns
				.parallel()
				.filter(l -> !l.startsWith("PIM_PRO"))
				.map(rw::parseLine)
				.map(a -> a[0])
				.collect( java.util.stream.Collectors.toSet() )
			;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Set<String> set = refs;
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "OtroFinalSorted.dat"), java.nio.charset.StandardCharsets.UTF_8)){
			lns
				.parallel()
				.filter(l -> !l.startsWith("PIM_PROD"))
				.map(rw::parseLine)
				.filter(a -> set.contains(a[1]))
				.forEach(a -> {
					Long f = chm.get(a[2]);
					chm.put(a[2], (f == null ? 0 : f) + 1);
				})
			;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.LinkedList<java.util.Map.Entry<String, Long>> lst = new java.util.LinkedList<>(chm.entrySet());
		java.util.Collections.sort(lst, (o1,o2)->o2.getValue().compareTo(o1.getValue()));
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "FrecuenciasPorAtributoConPlantilla.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			lst.forEach(entry -> pw.println( rw.serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println(chm.size());
		System.out.println("Done: " + rw.formatTime(System.currentTimeMillis() - init));
	}
	
}
