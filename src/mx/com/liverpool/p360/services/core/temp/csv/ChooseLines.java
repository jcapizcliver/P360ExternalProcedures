package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ChooseLines {

	private static final RESTWrapper rw = new RESTWrapper();
	private static int cnt = 0;
	
	public static void main(String[] args) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0) {
				lines.add(arr[0]);
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		String[] ids = lines.toArray(new String[] {});
		java.util.Arrays.sort(ids);
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(args[2]).toFile())))){
			parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				cnt++;
				if(arr.length > 0) {
					if(cnt == 1) {
						pw.println( rw.getRw().serializeChunk( arr ) );
					}else if( java.util.Arrays.binarySearch(ids, arr[0]) > -1 ) {
						arr[2] = getStatusLabel(arr[2]);
						pw.println( rw.getRw().serializeChunk( arr ) );
					}
				}
			} );
			parser.parse(java.nio.file.Paths.get(args[1]));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getStatusLabel(String key) {
		return 
			  "1001".equals(key) ? "Propuesta Generada"
			: "1002".equals(key) ? "Pendiente Inicio Enriquecimiento"
			: "1003".equals(key) ? "Revisi\u00f3n Compras"
			: "1004".equals(key) ? "Carga de Imagen"
			: "1005".equals(key) ? "Rechazada"
			: "1006".equals(key) ? "Por Actualizar "
			: "1007".equals(key) ? "Aprobada"
			: "1008".equals(key) ? "Modificaci\u00f3n "
			: "1009".equals(key) ? "Cancelado"
			: "1010".equals(key) ? "En Proceso Liverpool"
			: "1011".equals(key) ? "En Proceso de Env\u00edo"
			: "1020".equals(key) ? "Creaci\u00f3n de SKU"
			: "1021".equals(key) ? "Gobierno de Datos"
			: "1022".equals(key) ? "Revisi\u00f3n QA"
			: "1023".equals(key) ? "Category"
			: "1024".equals(key) ? "Rechazo Publicaci\u00f3n"
			: "1025".equals(key) ? "Eliminada"
			: "1026".equals(key) ? "En Proceso Foro"
			: "1027".equals(key) ? "Rechazo Compras"
			: "1028".equals(key) ? "Rechazo QA"
			: "1029".equals(key) ? "Rechazo Gobierno"
			: "1030".equals(key) ? "Rechazo Category"
			: "1031".equals(key) ? "Repoblamiento"
			: "1032".equals(key) ? "Excepci\u00f3n de Catalogaci\u00f3n"
			: "";
	}
	
}
