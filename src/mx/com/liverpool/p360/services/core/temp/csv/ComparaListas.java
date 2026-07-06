package mx.com.liverpool.p360.services.core.temp.csv;

public class ComparaListas {

	
	public static void main(String[] args) {
		if(args.length < 4) {
			System.out.println("Debe ingresar 4 parámetros: Archivo de referencia, Archivo con la data a buscar en la referencia, Archivo de salida con ausencias, Archivo de salida con coincidencias");
			System.exit(0);
		}
		java.util.List<String> prototype = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					prototype.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		String[] ref = prototype.toArray( new String[] {} );
		java.util.Arrays.sort(ref);
		prototype.clear();
		prototype = null;
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[1]).toFile())))){
			String line = null;
			try(
				java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(args[2]).toFile())));
				java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(args[3]).toFile())))
			){
				while((line = br.readLine()) != null) {
					if(!"".equals(line)) {
						if( java.util.Arrays.binarySearch(ref, line) < 0 ) {
							pw.println( line );
						}else {
							pw2.println( line );
						}
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
