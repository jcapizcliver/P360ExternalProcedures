package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RevisaLasPropuestasQueEncontróJoaquín {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, Object[]> data = new java.util.TreeMap<>();
		java.util.Map<String, String> dataJ = new java.util.TreeMap<>();
		Object[] d = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_stage_dir"), "STEPXMLChildParentProposals").toString())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ",", "\\");
				data.put(pieces[0], new Object[] { pieces[1], pieces[2], pieces[3] });
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_stage_dir"), "products sin variantes.csv").toString())))){
			String line = null;
			String[] pieces = null;
			br.readLine();
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, "\"", ",", "\\");
				dataJ.put(pieces[0], pieces[1]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(
				java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_stage_dir"), "MatchesWithPropuestasJoaquín").toString())));
				java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_stage_dir"), "NoMatchesWithPropuestasJoaquín").toString())))
			){
			for(java.util.Map.Entry<String, String> entry : dataJ.entrySet()) {
				d = data.get(entry.getKey());
				if(d == null) {
					pw2.println( rw.getRw().serializeChunk(new String[] { entry.getKey() }, "\"", ",", "\\") );
				}else {
					pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), d[0], d[1], d[2] }, "\"", ",", "\\") );
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
