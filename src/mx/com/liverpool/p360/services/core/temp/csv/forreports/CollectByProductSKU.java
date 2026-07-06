package mx.com.liverpool.p360.services.core.temp.csv.forreports;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class CollectByProductSKU {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.List<String> ofInterestSBB = new java.util.ArrayList<>();
		java.util.List<String> ofInterestMKP = new java.util.ArrayList<>();
		java.util.List<String> ofInterestLVP = new java.util.ArrayList<>();
		SimpleDelimitedFileParser sdfpS = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				ofInterestSBB.add(row[0]);
			}
		} );
		sdfpS.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDsSBB.txt"));
		System.out.println("Read: " + ofInterestSBB.size() + " of interest SBB");
		SimpleDelimitedFileParser sdfpM = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				ofInterestMKP.add(row[0]);
			}
		} );
		sdfpM.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDsMKP.txt"));
		System.out.println("Read: " + ofInterestMKP.size() + " of interest MKP");
		SimpleDelimitedFileParser sdfpL = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				ofInterestLVP.add(row[0]);
			}
		} );
		sdfpL.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDsLVP.txt"));
		System.out.println("Read: " + ofInterestLVP.size() + " of interest LVP");
		int[] foundMKP = new int[]{0};
		int[] foundLVP = new int[]{0};
		int[] foundSBB = new int[]{0};
		int[] count = new int[] {0};
		try( java.io.PrintWriter pwLVP = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outLVP.csv").toFile()))); java.io.PrintWriter pwSBB = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outSBB.csv").toFile()))); java.io.PrintWriter pwMKP = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outMKP.csv").toFile()))) ){
			SimpleDelimitedFileParser sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				if(row.length > 0) {
					if(!"Identifier".equals(row[0])) {
						if( ofInterestLVP.contains( row[2] ) ) {
							pwLVP.println( rw.getRw().serializeChunk(row) );
							foundLVP[0]++;
						}else if( ofInterestSBB.contains( row[2] ) ) {
							pwSBB.println( rw.getRw().serializeChunk(row) );
							foundSBB[0]++;
						}else if( ofInterestMKP.contains( row[2] ) ) {
							pwMKP.println( rw.getRw().serializeChunk(row) );
							foundMKP[0]++;
						}
						count[0]++;
					} else {
						pwMKP.println( rw.getRw().serializeChunk( row ) );
						pwLVP.println( rw.getRw().serializeChunk( row ) );
						pwSBB.println( rw.getRw().serializeChunk( row ) );
					}
				}
			} );
			sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260419_141649.csv"));
			System.out.println("Found: " + foundMKP[0] + " mkp matches.");
			System.out.println("Found: " + foundLVP[0] + " lvp matches.");
			System.out.println("Found: " + foundSBB[0] + " sbb matches.");
			System.out.println("Read: " + count[0] + " rows.");
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
}
