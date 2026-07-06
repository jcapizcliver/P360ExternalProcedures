package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CheckMigratedIDs {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.List<String> prodIDs = new java.util.ArrayList<>();
		java.util.List<String> artIDs = new java.util.ArrayList<>();
		java.util.Map<String, String[]> indexData = new java.util.HashMap<>();
		java.util.Map<String, java.util.Set<String>> padres = new java.util.HashMap<>();
		java.util.Map<String, java.util.Set<String>> tipos = new java.util.HashMap<>();
		String[] pids = null;
		String[] aids = null;
		String[] pieces = null;
		String[] chunk = null;
		String[] subData = null;
		String[] chunkies = null;
		java.util.Set<String> padresSet = null;
		java.util.Set<String> tiposSet = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				prodIDs.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Read: " + prodIDs.size() + " products.");
		pids = prodIDs.toArray(new String[] {});
		prodIDs.clear();
		prodIDs = null;
		java.util.Arrays.sort(pids);
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[1]).toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				artIDs.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Read: " + artIDs.size() + " articles.");
		aids = artIDs.toArray(new String[] {});
		artIDs.clear();
		artIDs = null;
		java.util.Arrays.sort(aids);
		try(
			java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("Más de un tipo").toFile())));
			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("Mas de un papá").toFile())));
		){
			pw1.println( rw.getRw().serializeChunk( new String[] { "ID", "Tipo" } ) );
			pw2.println( rw.getRw().serializeChunk( new String[] { "ID", "Parent1" } ) );
			System.out.println("Reading first index...");
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[2]).toFile())))){
				String line = null;
				while((line = br.readLine()) != null) {
					pieces = rw.getRw().parseLine(line);
					if(pieces.length > 0) {
						subData = rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
						for(int m = 0; m<subData.length; m++) {
							chunkies = rw.getRw().parseLine(subData[m], "\"", "|", "\\");
							if(chunkies.length > 3) {
								System.out.println("Abnormal entry size (0): " + line);
							}else {
								chunk = indexData.get(pieces[0]);
								if(chunk == null) {
									indexData.put(pieces[0], chunkies);
								}else {
									for(int i=0; i<chunk.length-1; i++) {
										if(!chunk[i].equals(chunkies[i])) {
											if(i == 1) {
												padresSet = padres.get(pieces[0]);
												if(padresSet == null) {
													padresSet = new java.util.TreeSet<>();
													padres.put(pieces[0], padresSet);
												}
												padresSet.add(chunkies[i]);
											}else {
												tiposSet = padres.get(pieces[0]);
												if(tiposSet == null) {
													tiposSet = new java.util.TreeSet<>();
													tipos.put(pieces[0], tiposSet);
												}
												tiposSet.add(chunkies[i]);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			System.out.println("Reading second index...");
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[3]).toFile())))){
				String line = null;
				while((line = br.readLine()) != null) {
					pieces = rw.getRw().parseLine(line);
					if(pieces.length > 0) {
						subData = rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
						for(int m=0; m<subData.length; m++) {
							chunkies = rw.getRw().parseLine(subData[m], "\"", "|", "\\");
							if(chunkies.length > 3) {
								System.out.println("Abnormal entry size: " + line);
							}else {
								chunk = indexData.get(pieces[0]);
								if(chunk == null) {
									indexData.put(pieces[0], chunkies);
								}else {
									for(int i=0; i<chunk.length-1; i++) {
										if(!chunk[i].equals(chunkies[i])) {
											if(i == 1) {
												padresSet = padres.get(pieces[0]);
												if(padresSet == null) {
													padresSet = new java.util.TreeSet<>();
													padres.put(pieces[0], padresSet);
												}
												padresSet.add(chunkies[i]);
											}else {
												tiposSet = padres.get(pieces[0]);
												if(tiposSet == null) {
													tiposSet = new java.util.TreeSet<>();
													tipos.put(pieces[0], tiposSet);
												}
												tiposSet.add(chunkies[i]);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			java.util.List<java.util.Map.Entry<String, java.util.Set<String>>> entriesPadres = new java.util.ArrayList<>( padres.entrySet() );
			java.util.Collections.sort( entriesPadres, (o1,o2) -> o1.getKey().compareTo(o2.getKey()) );
			padres.clear();
			padres = null;
			java.util.List<java.util.Map.Entry<String, java.util.Set<String>>> entriesTipos = new java.util.ArrayList<>( tipos.entrySet() );
			java.util.Collections.sort( entriesTipos, (o1,o2) -> o1.getKey().compareTo(o2.getKey()) );
			for(java.util.Map.Entry<String, java.util.Set<String>> entry : entriesTipos) {
				if( entry.getValue().size() > 1 ) {
					for( String a : entry.getValue() ) {
						pw1.println( new String[] { entry.getKey(), a } );
					}
				}
			}
			for(java.util.Map.Entry<String, java.util.Set<String>> entry : entriesPadres) {
				if( entry.getValue().size() > 1 ) {
					for( String a : entry.getValue() ) {
						pw2.println( new String[] { entry.getKey(), a } );
					}
				}
			}
			entriesTipos.clear();
			entriesTipos = null;
			entriesPadres.clear();
			entriesPadres = null;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Now searching matches...");
		try(
			java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("FaltanPorCargar").toFile())));
			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("PadresMalUbicados").toFile())));
			java.io.PrintWriter pw3 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("HijosMalUbicados").toFile())));
			java.io.PrintWriter pw4 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("PasQueSí").toFile())));
			java.io.PrintWriter pw5 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("HijQueSí").toFile())));
		){
			pw2.println( rw.getRw().serializeChunk( new String[] { "ID", "Tipo", "Padre" } ) );
			for(java.util.Map.Entry<String, String[]> datas : indexData.entrySet()) {
				if( java.util.Arrays.binarySearch( pids, datas.getKey() ) < -1 ) {
					if( java.util.Arrays.binarySearch( aids , datas.getKey() ) < -1 ) {
						pw1.println(datas.getKey());
					}else {
						if( !( datas.getValue()[0].equals("SalesItemVariant") || (datas.getValue()[0].equals("SalesItem") && datas.getValue()[1].matches("^(S?)[0-9]+$") ) ) ) {
							// Padre mal ubicado, no es padre, es individual de Mkp (seguramente dice: SalesItem)
							pw3.println( rw.getRw().serializeChunk( new String[] { datas.getKey(), datas.getValue()[0], datas.getValue()[1] } ) );
						}else {
							pw5.println( datas.getKey() );
						}
					}
				}else {
					if( !( datas.getValue()[0].startsWith("SalesItemFamily") || !datas.getValue()[1].matches("^(S?[0-9]+)$") ) ) {
						// Padre mal ubicado, no es padre, es individual de Mkp (seguramente dice: SalesItem)
						pw2.println( rw.getRw().serializeChunk( new String[] { datas.getKey(), datas.getValue()[0], datas.getValue()[1] } ) );
					}else {
						pw4.println( datas.getKey() );
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + rw.getRw().formatTime( System.currentTimeMillis() - init ));
	}
	
}
