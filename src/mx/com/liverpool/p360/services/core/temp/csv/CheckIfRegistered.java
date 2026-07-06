package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class CheckIfRegistered {

	
	public static void main(String[] args) {
		java.util.List<String> skusInP360 = new java.util.ArrayList<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0 && !"SKU".equals(arr[0])) {
				skusInP360.add(arr[0]);
			}
		} );
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260406_133607.csv"));
		String[] data = skusInP360.toArray( new String[] {} );
		java.util.Arrays.sort(data);
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsNotInP360.csv").toFile())))){
			pw.println("SKU");
			parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\r\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				if(arr.length > 0 && java.util.Arrays.binarySearch(data, arr[0]) < 0) {
					pw.println(arr[0]);
				}
			} );
			parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUs Migración Foro.txt"));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsNotInP360Expl.csv").toFile())))){
			parser = new SimpleDelimitedFileParser( '"', '|', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				if(arr.length > 3 && java.util.Arrays.binarySearch(data, arr[3]) < 0) {
					System.out.println(arr[3]);
					pw.println(arr[3]);
				}
			} );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "familias completas de existentes de los 75k .txt"));
	}
	
}
