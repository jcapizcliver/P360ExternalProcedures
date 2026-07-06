package mx.com.liverpool.p360.services.core.temp.csv.forreports;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class CollectByProductSKU_ForLalo {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.List<String> ofInterestData = new java.util.ArrayList<>();
		SimpleDelimitedFileParser sdfpS = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				ofInterestData.add(row[0]);
			}
		} );
		sdfpS.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Genericos.csv"));
//		sdfpS.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Listado4.txt"));
//		sdfpS.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Listado3.txt"));
//		sdfpS.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Listado2.txt"));
//		sdfpS.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Listado1.txt"));
		System.out.println("Read: " + ofInterestData.size() + " of interest LVP");
		int[] foundData = new int[]{0};
		int[] count = new int[] {0};
		try( java.io.PrintWriter pwP = 
				new java.io.PrintWriter(new java.io.OutputStreamWriter(
						new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260422_150642.csv").toFile()))) 
//				new java.io.PrintWriter(new java.io.OutputStreamWriter(
//						new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outGDDStatus.csv").toFile()))) 
//				new java.io.PrintWriter(new java.io.OutputStreamWriter(
//						new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outIA.csv").toFile()))) 
//				new java.io.PrintWriter(new java.io.OutputStreamWriter(
//						new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outAnalisisIA.csv").toFile()))) 
//				new java.io.PrintWriter(new java.io.OutputStreamWriter(
//						new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "outGDDStatus.csv").toFile()))) 
		){
			SimpleDelimitedFileParser sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				if(row.length > 0) {
					if(!"Identifier".equals(row[0])) {
						if( ofInterestData.contains( row[0] ) ) {
							pwP.println( rw.getRw().serializeChunk(row) );
							foundData[0]++;
						}
						count[0]++;
					} else {
						pwP.println( rw.getRw().serializeChunk( row ) );
					}
				}
			} );
			sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260422_150642.csv"));
//			sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ConsultabaseGdP.csv"));
			System.out.println("Found: " + foundData[0] + " matches.");
			System.out.println("Read: " + count[0] + " rows.");
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
}
