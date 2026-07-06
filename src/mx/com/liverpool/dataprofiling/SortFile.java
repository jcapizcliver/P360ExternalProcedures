package mx.com.liverpool.dataprofiling;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class SortFile {

	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		RESTWorkshop w = new RESTWorkshop();
		java.util.concurrent.ArrayBlockingQueue<String> lasLineas = new java.util.concurrent.ArrayBlockingQueue<>(1100000);
		SortFileInBlocks sfib = new SortFileInBlocks("ItemGroup,PIM_PROD_ID,PIM_ATRIBUTO_ID,PIM_ATRIBUTO_VAL_ID,PIM_ATRIBUTO_VAL,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_TIPO_NOM", lasLineas);
		Thread t = new Thread(sfib);
		t.setPriority(Thread.currentThread().getPriority() - 1);
		t.setDaemon(false);
		t.start();
		System.out.println("Now reading...");
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "OtroFinalUnsortedGrupoDeArtículos.csv"))){
//		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Detalle.csv.bkp"))){
			lns.parallel().filter(l -> !l.startsWith("ItemGroup,")).forEach(t1 -> {
				try {
					lasLineas.put(t1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		sfib.endRunning();
		System.out.println("Now waiting for sorter threads to finish...");
		try {
			t.join();
		}catch(java.lang.InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + w.formatTime(System.currentTimeMillis() - init));
	}
	
}
