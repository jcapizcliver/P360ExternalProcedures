package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.Yep;

public class CSVProyections {

	public static void main(String[] args) {
		if(args.length < 5) {
			System.out.println("Must specify: <source file> <delimiter> <separator> <escape> <column names>*\nAlso file must contain first row as headers.");
			System.exit(0);
		}
		CSVProyections cp = new CSVProyections();
		cp.theSelection(args[0], args[1], args[2], args[3], java.util.Arrays.asList( java.util.Arrays.copyOfRange(args, 4, args.length) ));
	}
	
	private void theSelection( String sourcePathRaw, String delim, String sep, String esc, java.util.List<String> columns ) {
		RESTWorkshop rw = new RESTWorkshop();
		Yep y = new Yep();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(sourcePathRaw).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = br.readLine();
			String[] header = y.parseLine(line, delim, sep, esc);
			java.util.List<String> headerList = java.util.Arrays.asList(header);
			int[] indexes = new int[columns.size()];
			for(int i=0; i<indexes.length; i++) {
				indexes[i] = headerList.indexOf(columns.get(i));
			}
			String[] pieces = null;
			String[] subPieces = new String[indexes.length];
			int i;
			while((line = br.readLine()) != null) {
				pieces = y.parseLine(line, delim, sep, esc);
				for(i = 0; i<indexes.length; i++) {
					subPieces[i] = pieces[indexes[i]];
				}
				System.out.println( rw.serializeChunk(subPieces, delim, sep, esc) );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
