package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class FlattenEveryFieldValue {

	
	private static final RESTWrapper rw = new RESTWrapper();


	public static void main(String[] args) {
		String[][] header = new String[1][];
		int[] count = new int[] {0};
		try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("MasaMadre.csv").toFile()))) ) {
			SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				if(row.length > 0) {
					count[0]++;
					if(count[0] == 1) {
						header[0] = row;
						pw.println( rw.getRw().serializeChunk(row) );
					}else {
						String[] nd = java.util.Arrays.copyOf(row, header[0].length);
						if(row.length > header[0].length) {
							System.out.println("PANIC.");
						}
						for(int i=0; i<header[0].length; i++) {
							nd[i] = flattenValue( i < row.length ? row[i] : "");
						}
						pw.println( rw.getRw().serializeChunk(nd) );
					}
				}
			} );
			parser.parse(java.nio.file.Paths.get(args[0]));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}

	
    private static String flattenValue(String value){
        return value == null ? value : value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\r\\n");
    }
	
}
