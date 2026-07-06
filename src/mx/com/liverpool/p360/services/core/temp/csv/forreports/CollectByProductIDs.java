package mx.com.liverpool.p360.services.core.temp.csv.forreports;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class CollectByProductIDs {

	private static final RESTWrapper rw = new RESTWrapper(); // Este lo pidieron de gobierno de producto (Julio Erick Rodriguez Pacheco 20042026)
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.List<String> ofInterestData = new java.util.ArrayList<>();
		SimpleDelimitedFileParser sdfpS = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				ofInterestData.add(row[0]);
			}
		} );
		sdfpS.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDsData.csv"));
		System.out.println("Read: " + ofInterestData.size() + " of interest ");
		int[] foundMKP = new int[]{0};
		int[] foundLVP = new int[]{0};
		int[] foundData = new int[]{0};
		int[] count = new int[] {0};
		try( java.io.PrintWriter pwData = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outData.csv").toFile()))); ){
			SimpleDelimitedFileParser sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				if(row.length > 0) {
					if(!"Identifier".equals(row[0])) {
						if( ofInterestData.contains( row[0] ) ) {
							pwData.println( rw.getRw().serializeChunk(row) );
							foundData[0]++;
						}
						count[0]++;
					} else {
						pwData.println( rw.getRw().serializeChunk( row ) );
					}
				}
			} );
			sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260420_110603.csv"));
//			sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260420_101230.csv"));
			System.out.println("Found: " + foundMKP[0] + " mkp matches.");
			System.out.println("Found: " + foundLVP[0] + " lvp matches.");
			System.out.println("Found: " + foundData[0] + " sbb matches.");
			System.out.println("Read: " + count[0] + " rows.");
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
}
