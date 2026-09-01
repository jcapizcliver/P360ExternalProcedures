package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ComparaSKUs {

	
	
	public static void main(String[] args) {
		java.util.List<String> matnrs = new java.util.ArrayList<>();
		int[] algo = new int[]{0};
		SimpleDelimitedFileParser sdfp = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0) {
				matnrs.add(row[0]);
				algo[0]++;
				if(algo[0] % 100000 == 0) {
					System.out.print(".");
					if(algo[0] % 1000000 == 0) {
						System.out.println("." + algo[0]);
					}
				}
			}
		} );
		sdfp.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260420_090200.csv"));
		System.out.println();
		System.out.println("Data read: " + matnrs.size());
		algo[0] = 0;
		String[] datas = matnrs.toArray(new String[] {});
		java.util.Arrays.sort(datas);
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt" ,"LVP", "desorden", "PROD", "AbsentSKUs.csv").toFile())))){
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "bq-results-20260416-152649-1776353222106.csv").toFile())))){
				String line = null;
				String[] row = null;
				while((line = br.readLine()) != null) {
					row = line.split(",");
					if(row.length > 0 && !"MATNR".equals(row[0])) {
						if(!"".equals(row[0]) && java.util.Arrays.binarySearch(datas, row[0].replaceAll("^0+", "")) < 0) {
							pw.println(row[0]);
						}
//					}else if("MATNR".equals(row[0])) {
//						System.out.println("Header found.");
//					}else {
//						System.out.println( row.length + " elements.");
					}
					algo[0]++;
					if(algo[0] % 1000 == 0) {
						System.out.print(".");
						if(algo[0] % 1000000 == 0) {
							System.out.println("." + algo[0]);
						}
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
