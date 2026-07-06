package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class EliminaVariantesConIDIgualAlDelProductoQueNoSonIndividuales {

	/*
	 *	Clase que quita objetos Article que tienen un ID igual al de un producto con más de un Artículo relacionado
	 *
	 *********************************************************************************/
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		int[] counter = {0};
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "1000");
		StringBuilder sb = new StringBuilder();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			counter[0]++;
			if(counter[0] == 1) {
				
			}else {
				if(arr.length > 0) {
					sb.append(sb.length() == 0 ? "" : ",").append("'").append(arr[0]).append("'@1");
					if( (counter[0] - 1) % 1000 == 0 ) {
						qp.put("items", sb.toString());
						rw.deleteData("list", "Article", null, "byItems", qp, System.out::println);
						sb.setLength(0);
					}
				}
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		if( sb.length() > 0 ) {
			qp.put("items", sb.toString());
			rw.deleteData("list", "Article", null, "byItems", qp, System.out::println);
			sb.setLength(0);
		}
	}
	
	
}
