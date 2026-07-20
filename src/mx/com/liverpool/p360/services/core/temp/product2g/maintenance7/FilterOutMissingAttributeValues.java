package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class FilterOutMissingAttributeValues {

	private static String[] header = null;
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		if(args.length == 0) {
			args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\RefTemplateCharacteristics.csv", "C:\\opt\\LVP\\desorden\\PROD\\Template_1_Mayo_P360_EXPLOIT_20260630_092651.csv" };
		}
		java.util.List<String> keys = new java.util.ArrayList<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if( row.length == 0 ) {
				
			}else {
				keys.add( row[0] + "<:>" + row[1] );
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		String[] keyRef = keys.toArray(new String[] {});
		java.util.Arrays.sort(keyRef);
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "TemplateAttributesByEcommercePublication.csv").toFile())))){
			parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				if( row.length == 0 ) {
					
				}else {
					if(header == null) {
						header = row;
						pw.println( rw.getRw().serializeChunk(header) );
						return;
					}
					if( java.util.Arrays.binarySearch(keyRef, row[0] + "<:>" + row[6]) >= 0 ) {
						pw.println( rw.getRw().serializeChunk( row ) ); 
					}
				}
			} );
			parser.parse(java.nio.file.Paths.get(args[1]));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
