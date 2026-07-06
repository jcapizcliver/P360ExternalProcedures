package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ValoresPorAtributoPorPlantilla {

	private static final RESTWorkshop rw = new RESTWorkshop();
	private static final java.util.concurrent.ConcurrentHashMap<String, String[]> chm = new java.util.concurrent.ConcurrentHashMap<>();

	private static final java.util.Map<String, java.util.Map<String, Long>> atributosPorPlantilla = new java.util.TreeMap<>();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp"), java.nio.charset.StandardCharsets.UTF_8)){
			lns
				.parallel()
				.filter(s -> !"PIM_PROD_ID,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_NOM,PIM_PROD_TIPO_NOM,PIM_NIVEL_ID,PIM_PLANTILLA_ID,PIM_ATRIB_PRODUCTTYPESAP,PIM_ATRIB_PRODUCTNAME,PIM_ATRIB_ITEMGROUP,FCH_CARGA".equals(s))
				.map(rw::parseLine)
				.forEach(a -> chm.put(a[0], new String[] { a[1], cure( a[2] ), cure( a[6] ), cure( a[9] ), cure( a[4] ), cure( a[9] )  }))
			;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Now reading detail...");
		java.util.Set<String> notFound = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Final.dat").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			java.util.Map<String, Long> set = null;
			String[] pieces = null;
			String[] parentData = null;
			Long freq = null;
			long cnt = 0;
			while((line = br.readLine()) != null) {
				pieces = rw.parseLine(line);
				parentData = chm.get(pieces[0]);
				if(parentData == null) {
//					System.out.println("No parent data found for: " + pieces[0] + " || " + line);
//					System.exit(0);
					notFound.add(pieces[0]);
				}else {
					set = atributosPorPlantilla.get(parentData[2]);
					if(set == null) {
						set = new java.util.TreeMap<>();
						atributosPorPlantilla.put(parentData[2], set);
					}
					freq = set.get(pieces[1]);
					set.put(pieces[1], (freq == null ? 0 : freq) + 1);
				}
				cnt++;
				if(cnt % 100000 == 0) {
					System.out.print(".");
					if(cnt % 10000000 == 0) {
						System.out.println(cnt);
					}
				}
			}
			System.out.println(cnt);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Not found: " + notFound.size());
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "TemplateCharacteristic_IG_NF.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			notFound.forEach(pw::println);
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
		System.out.println("Found: " + atributosPorPlantilla.size() + " tuplas.");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "TemplateCharacteristic_.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println( rw.serializeChunk(new Object[] { "Template", "Attribute", "Freq" }) );
			java.util.LinkedList<java.util.Map.Entry<String, Long>> lst = null;
			for(java.util.Map.Entry<String, java.util.Map<String, Long>> entry : atributosPorPlantilla.entrySet()) {
				lst = new java.util.LinkedList<>(entry.getValue().entrySet());
				java.util.Collections.sort(lst, (o1,o2) -> o2.getValue().compareTo(o1.getValue()));
				for(java.util.Map.Entry<String, Long> entry0 : lst) {
					pw.println( rw.serializeChunk( new Object[] { entry.getKey(), entry0.getKey(), entry0.getValue() } ) );
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done: " + rw.formatTime(System.currentTimeMillis() - init));
	}
	
	private static String cure(String s) {
		return "null".equals(s) ? "" : s;
	}
	
	
}
