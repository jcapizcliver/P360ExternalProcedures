package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class TryParsingFilesProductVariantInfoAdmin002 {

	private static final java.nio.file.Path articleDataPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.product_data_file"));
	private static int a = 0;
	
	public static void main(String[] args) {
		SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> { a++; } );
		fileParser.parse(articleDataPath);
		System.out.println("Done. " + a);
	}
	
}
