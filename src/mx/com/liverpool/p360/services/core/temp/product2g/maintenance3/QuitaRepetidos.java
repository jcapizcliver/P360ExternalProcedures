package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QuitaRepetidos {
 /*
  * 
  *	Aquí voy a quitar del ProductVariantInfoAdmin002 los identificadores repetidos. 
  * 
  ************************************************************************************/
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String[]> data = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			String[] pieces = null;
			br.readLine();
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				data.add(pieces);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
