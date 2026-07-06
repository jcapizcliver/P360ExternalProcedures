package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MergeIndexes {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String[]> data = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("Index").toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				if(pieces.length > 0) {
					String[] sd = data.get(pieces[0]);
					if(sd == null) {
						sd = rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
						data.put(pieces[0], sd);
					}else {
						java.util.List<String> elements = new java.util.ArrayList<>( java.util.Arrays.asList(sd) );
						String[] sd0 = rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
						for(int i=0; i<sd0.length; i++) {
							if(!elements.contains(sd0[i])) {
								elements.add(sd0[i]);
							}
						}
						data.put(pieces[0], elements.toArray( new String[] {} ));
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("Index2").toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				if(pieces.length > 0) {
					String[] sd = data.get(pieces[0]);
					if(sd == null) {
						sd = rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
						data.put(pieces[0], sd);
					}else {
						java.util.List<String> elements = new java.util.ArrayList<>( java.util.Arrays.asList(sd) );
						String[] sd0 = rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
						for(int i=0; i<sd0.length; i++) {
							if(!elements.contains(sd0[i])) {
								elements.add(sd0[i]);
							}
						}
						data.put(pieces[0], elements.toArray( new String[] {} ));
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("IDX").toFile())))){
			data.entrySet().forEach(entry -> pw.println( rw.getRw().serializeChunk( new Object[] { entry.getKey(), rw.getRw().serializeChunk(entry.getValue(), "\"", ";", "\\") } ) ) );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
