package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class CheckTemplateVsAdmin {

	private static int products = 0;
	private static int articles = 0;
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args0) {
		if(args0.length == 0) {
			args0 = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\Product2G.csv", "C:\\opt\\LVP\\desorden\\PROD\\Article.csv", "C:\\opt\\LVP\\desorden\\PROD\\Source1" };
		}
		String[] args = args0;
		long init = System.currentTimeMillis();
		java.util.Map<String, String> productoNegocio = new java.util.HashMap<>();
		java.util.Map<String, String> productoPlantilla = new java.util.HashMap<>();
		java.util.Map<String, String> artículoProducto = new java.util.HashMap<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0 && !"Identifier".equals(arr[0])) {
				productoNegocio.put(arr[0], arr[4]);
				productoPlantilla.put(arr[0], arr.length > 15 ? arr[15] : "");
				products++;
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		System.out.println("Read " + productoNegocio.size());
		System.out.println("************ Now articles ************");
		parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0 && !"variant".equals(arr[0])) {
				artículoProducto.put(arr[6], arr[1]);
				articles ++;
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[1]));
		System.out.println("Read " + artículoProducto.size());
		try(
			java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("ConPlantilla").toFile())));
			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("SinPlantilla").toFile())));
			java.io.PrintWriter pw3 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("SinProducto").toFile())));
		){
			parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				if(arr.length > 0 ) {
					String product = artículoProducto.get(arr[0]);
					if(product != null) {
						String template = productoPlantilla.get(product);
						if(template == null) {
							pw2.println(arr[0]);
						}else {
							pw1.println( rw.getRw().serializeChunk( new Object[] { arr[0], template} ) );
						}
					}else {
						pw3.println(arr[0]);
					}
				}
				System.out.println(arr[0]);
			} );
			parser.parse(java.nio.file.Paths.get(args[2]));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Products: " + products);
		System.out.println("Articles: " + articles);
		System.out.println("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
	}
	
}
