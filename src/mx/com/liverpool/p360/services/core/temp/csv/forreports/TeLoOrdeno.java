package mx.com.liverpool.p360.services.core.temp.csv.forreports;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class TeLoOrdeno {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final String TMP = "C:\\opt\\LVP\\desorden\\PROD\\tmp";
	
	public static void main(String[] args) {
		
		boolean[] processed = new boolean[] {false};
		int[] counter = new int[] {0};
		int[] foil = new int[] {0};
		java.util.List<String[]> rows = new java.util.ArrayList<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"',',','\\',"\n",java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				if(!processed[0]) {
					processed[0] = true;
				}else {
					rows.add(row);
					counter[0]++;
					if(counter[0] % 10000 == 0) {
						foil[0]++;
						sort( rows, foil[0], args[1] );
					}
				}
			}
		});
		parser.parse(java.nio.file.Paths.get(args[0]));
		if(!rows.isEmpty()) {
			foil[0]++;
			sort( rows, foil[0], args[1] );
		}
		System.out.println("Now merging for: " + args[0]);
		aggregate( args[1] );
	}
	
	private static void aggregate( String prefix ) {
		java.io.File[] files = new java.io.File(TMP).listFiles(ff -> ff.getName().startsWith(prefix));
		java.util.List<java.util.Map.Entry<String[], Object[]>> tombola = new java.util.ArrayList<>();
		java.util.List< java.util.Map.Entry<String[], Object[]>> openFiles = new java.util.ArrayList<>();
		java.io.BufferedReader br = null;
		String line = null;
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("LaMasa_" + prefix + ".csv").toFile())))) {
			java.util.Map.Entry<String[], Object[]> entry0 = null;
			for(int i=0; i<files.length; i++) {
				br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(files[i])));
				line = br.readLine();
				entry0 = new java.util.AbstractMap.SimpleEntry<>( rw.getRw().parseLine( line ), new Object[] { br, files[i] });
				openFiles.add(entry0);
				tombola.add(entry0);
			}
			java.util.Collections.sort(tombola, (o1,o2)->o1.getKey()[0].compareTo(o2.getKey()[0]));
			while(!tombola.isEmpty()){
				entry0 = tombola.remove(0);
				pw.println( rw.getRw().serializeChunk(entry0.getKey()) );
				line = ((java.io.BufferedReader)entry0.getValue()[0]).readLine();
				if(line != null) {
					entry0 = new java.util.AbstractMap.SimpleEntry<>( rw.getRw().parseLine( line ), entry0.getValue() );
					tombola.add(0, entry0);
					java.util.Collections.sort(tombola, (o1,o2)->o1.getKey()[0].compareTo(o2.getKey()[0]));
				}else {
					((java.io.BufferedReader) entry0.getValue()[0]).close();
					java.nio.file.Files.delete(((java.io.File)entry0.getValue()[1]).toPath());
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void sort(java.util.List<String[]> rows, int foil, String prefix) {
		java.util.Collections.sort(rows, (o1,o2)->o1[0].compareTo(o2[0]));
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(TMP, prefix + "_" + foil + ".dat").toFile())))){
			rows.forEach(row -> {
				for(int i=0; i<row.length; i++) {
					row[i] = row[i].replaceAll("\n", "\\n");
				}
				pw.println( rw.getRw().serializeChunk(row) );
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		rows.clear();
	}
	
}
