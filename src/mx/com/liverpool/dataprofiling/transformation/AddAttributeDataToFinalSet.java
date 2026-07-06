package mx.com.liverpool.dataprofiling.transformation;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AddAttributeDataToFinalSet {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	
	public static void main(String[] args) {
		PrepareDataForModernization.main(args);
		long init = System.currentTimeMillis();
		java.nio.file.Path source = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosModernización.csv");
		java.nio.file.Path sourceAttributeValuesFile = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "AtributosPlantillas.csv");
		java.nio.file.Path target = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosModernizaciónConNoSpot.csv");
		java.util.Map<String, String> productNoSpotValues = new java.util.HashMap<>();
		java.util.Map<String, String> productNoSpotValuesIndividuales = new java.util.HashMap<>();
		int count = 0;
		try( java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(sourceAttributeValuesFile, java.nio.charset.StandardCharsets.UTF_8) ){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line);
				if(pieces[7].startsWith("SalesItemFamily")) {
					if(attributesToAdd.contains(pieces[2])) {
						if(!"".equals(pieces[3])) {
							productNoSpotValues.put(pieces[5], workshop.serializeChunk(new Object[] { pieces[3], pieces[4] }, "\"", ";", "\\"));
						}
					}
				}else if( "SalesItem".equals(pieces[7]) && "".equals(pieces[6]) ) {
					if(!"".equals(pieces[3])) {
						productNoSpotValuesIndividuales.put(pieces[1], workshop.serializeChunk(new Object[] { pieces[3], pieces[4] }, "\"", ";", "\\"));
					}
				}
				count++;
				if(count % 100000 == 0) {
					System.out.print(".");
					if(count % 10000000 == 0) {
						System.out.println(count);
					}
				}
			}
			System.out.println(count);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		int hits = 0;
		System.out.println("Done building... (" + productNoSpotValues.size() + ", " + productNoSpotValuesIndividuales.size() + ") " + workshop.formatTime(System.currentTimeMillis() - init));
		try( java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(source, java.nio.charset.StandardCharsets.UTF_8) ){
			try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target), java.nio.charset.StandardCharsets.UTF_8)) ){
				String line = br.readLine();
				String[] header = workshop.parseLine(line);
				String[] na = null;
				na = java.util.Arrays.copyOf(header, header.length + 1);
				na[header.length] = "NoSpot_AE416";
				pw.println( workshop.serializeChunk( na ) );
				String ae416 = null;
				count = 0;
				while((line = br.readLine()) != null) {
					na = java.util.Arrays.copyOf( workshop.parseLine(line), header.length + 1 );
					if(productNoSpotValues.containsKey(na[3]) || ("SalesItem".equals(na[4]) && "".equals(na[3]) && productNoSpotValuesIndividuales.containsKey(na[1]) )) {
						hits++;
					}
					ae416 = "".equals(na[3]) ? null : "SalesItem".equals(na[4]) && "".equals(na[3]) ? productNoSpotValuesIndividuales.get(na[1]) : productNoSpotValues.get(na[3]);
					na[header.length] = ae416 == null ? "" : ae416;
					pw.println( workshop.serializeChunk( na ) );
					count++;
					if(count % 100000 == 0) {
						System.out.print(".");
						if(count % 10000000 == 0) {
							System.out.println(count);
						}
					}
				}
				System.out.println(count);
				System.out.println("Hits: " + hits);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		/*
		try{
			java.nio.file.Files.lines(sourceAttributeValuesFile).parallel()
				.map(workshop::parseLine)
				.filter(a -> !a[0].startsWith("Template") && a[7].startsWith("SalesItemFamily") && attributesToAdd.contains(a[1]))
				.collect(java.util.stream.Collectors.groupingByConcurrent(a -> a[1], java.util.concurrent.ConcurrentHashMap::new, java.util.stream.Collectors.toConcurrentMap(a -> a[2], a -> !"".equals(a[3]) ? a[3] : workshop.serializeChunk(new String[] {a[3], a[4]}, "\"", ";", "\\"), (ov, nv) -> nv, java.util.concurrent.ConcurrentHashMap::new)));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		*/
	}
	
	private static final java.util.List<String> attributesToAdd = java.util.Arrays.asList(
			"AE416"
		);
	
}
