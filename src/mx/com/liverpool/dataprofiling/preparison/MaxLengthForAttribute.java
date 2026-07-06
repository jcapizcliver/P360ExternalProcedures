package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MaxLengthForAttribute {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp"), java.nio.charset.StandardCharsets.UTF_8)){
			java.util.Optional<Integer> op = lines.parallel().filter(l -> !l.startsWith("PIM_PROD_ID")).map(workshop::parseLine).map(a -> a[0]).map(l -> l.length()).collect(java.util.stream.Collectors.maxBy((o1,o2)->o1.compareTo(o2)));
			System.out.println(op.get());
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
//		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp"), java.nio.charset.StandardCharsets.UTF_8)){
//			java.util.Optional<Integer> op = lines.filter(l -> !l.startsWith("PIM_PROD_ID")).map(workshop::parseLine).map(a -> a[3]).map(l -> l.length()).collect(java.util.stream.Collectors.maxBy((o1,o2)->o1.compareTo(o2)));
//			System.out.println(op.get());
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
	}
	
}
