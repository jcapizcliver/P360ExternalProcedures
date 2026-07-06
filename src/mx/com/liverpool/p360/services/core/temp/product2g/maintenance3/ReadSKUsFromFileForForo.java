package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.net.CliTest;

public class ReadSKUsFromFileForForo {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
//		main2("SBB-24.03.2026-0492 SATELITE.csv");
//		main2("SBB-24.03.2026-0532 CUAUTITLAN.csv");
//		main2("SBB-24.03.2026-CATMEXSG19MAR.csv");
//		main2("SBB-23.03.2026-0528 AEROPUERTO.csv");
//		main2("LVP-24.03.2026-VIA PLAN.csv");
//		main2("ForForo");
		main2(args[0]);
	}
	
	
	public static void main2(String args) {
		long init = System.currentTimeMillis();
		java.util.Set<String> skus = new java.util.TreeSet<>();
		SimpleDelimitedFileParser sdfp = new SimpleDelimitedFileParser('"', ',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			skus.add(arr[0]);
		} );
		sdfp.parse(java.nio.file.Paths.get(args));
		java.util.Set<String> data = new java.util.TreeSet<>();
		CollectProductFromArticleSKU.collectParentIDs(new java.util.ArrayList<>( skus ), data);
		System.out.println("Going over: " + data.size() + " unique propducts for refresh.");
		int a = 0;
		for(String d : data) {
			a++;
			System.out.println("Sending " + a + " data (" + d + ")");
			CliTest.enviaDataPropuesta(d);
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
}
