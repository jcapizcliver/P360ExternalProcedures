package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class SendSKUDataToAdminFromStagedFiles {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> skusVariantes = new java.util.ArrayList<>();
		SimpleDelimitedFileParser sdfp = new SimpleDelimitedFileParser( arr -> {
			skusVariantes.add(arr[0]);
		});
		sdfp.parse(java.nio.file.Paths.get(args[0]));
		
	}
	
}
