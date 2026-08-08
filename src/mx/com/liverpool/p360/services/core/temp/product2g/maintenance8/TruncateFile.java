package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class TruncateFile {

	private static final RESTWrapper rw = new RESTWrapper();
	private static int a = 0;
	
	public static void main(String[] args) {
		
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Projection.csv").toFile())))){
			SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				a++;
				if(row.length == 0) {
					return;
				}
				pw.println( rw.getRw().serializeChunk(new String[] { row[0], row[2], row[3], row[4], row[5], row[7], row[14], row[15], row[16], row[17], row[18], row[19], row[20], row[21], row.length == 23 ? "" : row[23] }) );
			} );
			parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "CAT_desanchezn_20260807095128562.csv"));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
