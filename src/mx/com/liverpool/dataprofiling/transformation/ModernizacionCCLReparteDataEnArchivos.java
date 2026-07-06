package mx.com.liverpool.dataprofiling.transformation;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ModernizacionCCLReparteDataEnArchivos {

	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		
		java.nio.file.Path source = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosModernizacionConNoSpot.csv");
		java.nio.file.Path target = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "CCL", "DatosModernizaciónConNoSpot_");
		java.nio.file.Path sorted = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "CCL", "DatosModernizaciónConNoSpot_Sorted.csv");
		java.nio.file.Path targetIndex = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "CCL", "Index.csv");
		java.util.List<String[]> indice = new java.util.ArrayList<>(100);
		int count = 0;
		int a = 0;
		int bs = 500000;
		java.io.File f = null;
		java.util.List<String[]> data = new java.util.ArrayList<>(bs);
		java.util.Map<java.io.File, java.io.BufferedReader> readers = new java.util.HashMap<>();
		try( java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(source) ){
			String line = br.readLine();
			String[] pieces = workshop.parseLine(line);
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line);
				data.add(pieces);
				a++;
				if( a == bs ) {
					java.util.Collections.sort(data, (o1,o2)-> o1[2].compareTo(o2[2]) );
					count++;
					try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( f = java.nio.file.Paths.get(target.toString() + String.valueOf(count) + ".csv" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
						for(String[] tuple : data) {
							pw.println( workshop.serializeChunk(tuple) );
						}
					}
					readers.put(f, java.nio.file.Files.newBufferedReader(f.toPath()));
					data.clear();
					data = new java.util.ArrayList<>();
					a = 0;
				}
			}
			if(!data.isEmpty()) {
				java.util.Collections.sort(data, (o1,o2)-> o1[2].compareTo(o2[2]) );
				count++;
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( f = java.nio.file.Paths.get(target.toString() + String.valueOf(count) + ".csv" ).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
					for(String[] tuple : data) {
						pw.println( workshop.serializeChunk(tuple) );
					}
				}
				readers.put(f, java.nio.file.Files.newBufferedReader(f.toPath()));
				data.clear();
				data = null;
				a = 0;
			}
			System.out.println("Now merging...");
			java.util.List<java.util.Map.Entry<String[], java.util.Map.Entry<java.io.File, java.io.BufferedReader>>> pairs = new java.util.ArrayList<>( readers.size() );
			for(java.util.Map.Entry<java.io.File, java.io.BufferedReader> r : readers.entrySet()) {
				pairs.add(new java.util.AbstractMap.SimpleEntry<>( workshop.parseLine( r.getValue().readLine() ), new java.util.AbstractMap.SimpleEntry<>( r.getKey(), r.getValue())));
			}
			readers.clear();
			java.util.Collections.sort(pairs, (o1,o2) -> o1.getKey()[2].compareTo( o2.getKey()[2] ) );
			java.io.File faux = null;
			java.io.BufferedReader br2 = null; 
			try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( sorted.toFile() ), java.nio.charset.StandardCharsets.UTF_8)) ){
				String[] npcs = null;
				java.util.Map.Entry<String[], java.util.Map.Entry<java.io.File, java.io.BufferedReader>> entry = null;
				while(!pairs.isEmpty()) {
					entry = pairs.remove(0);
					pieces = entry.getKey();
					pw.println( workshop.serializeChunk( pieces ) );
					line = entry.getValue().getValue().readLine();
					if(line == null) {
						entry.getValue().getValue().close();
						try{
							java.nio.file.Files.delete( java.nio.file.Paths.get( entry.getValue().getKey().getAbsolutePath()));
						}catch(java.nio.file.NoSuchFileException e) {
							
						}
					}else {
						npcs = workshop.parseLine(line);
						faux = entry.getValue().getKey();
						br2 = entry.getValue().getValue();
						java.util.Map.Entry<String[], java.util.Map.Entry<java.io.File, java.io.BufferedReader>> entry2 = new java.util.AbstractMap.SimpleEntry<>( workshop.parseLine( line ), new java.util.AbstractMap.SimpleEntry<>( faux, br2));
						pairs.add(0, entry2);
						if(npcs[2].compareTo(pieces[2]) <= 0) {
						}else {
							java.util.Collections.sort(pairs, (o1,o2) -> o1.getKey()[2].compareTo( o2.getKey()[2] ) );
						}
					}
				}
			}
			int b = 0;
			String fst = null;
			String lst = null;
			try(java.io.BufferedReader br1 = java.nio.file.Files.newBufferedReader(sorted)){
				String ln = null;
				String pl = null;
				String[] pcs = null;
				java.io.PrintWriter cpw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(f = java.nio.file.Paths.get(target.toString() + String.valueOf(a)).toFile()), java.nio.charset.StandardCharsets.UTF_8));
				while((ln = br1.readLine()) != null) {
					b++;
					cpw.println(ln);
					if(b == 1) {
						pcs = workshop.parseLine(ln);
						fst = pcs[2];
					}
					if(b == 100000) {
						pcs = workshop.parseLine(ln);
						lst = pcs[2];
						indice.add(new String[] { f.getName(), fst, lst });
						fst = null;
						lst = null;
						b = 0;
						cpw.close();
						a++;
						cpw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(f = java.nio.file.Paths.get(target.toString() + String.valueOf(a)).toFile()), java.nio.charset.StandardCharsets.UTF_8));
					}
					pl = ln;
				}
				if(b > 0) {
					pcs = workshop.parseLine(pl);
					lst = pcs[2];
					indice.add(new String[] { f.getName(), fst, lst });
					fst = null;
					lst = null;
					b = 0;
					cpw.close();
					a++;
					cpw = null;
				
				}
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(targetIndex.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				pw.println( workshop.serializeChunk(new Object[] { "File", "Min", "Max" }) );
				for(String[] row : indice) {
					pw.println( workshop.serializeChunk(row) );
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
