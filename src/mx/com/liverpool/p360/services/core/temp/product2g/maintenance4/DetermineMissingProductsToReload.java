package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DetermineMissingProductsToReload {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> list = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("salidas", "IDsProduct2G.csv").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				list.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		String[] ref = list.toArray(new String[] {});
		list.clear();
		list = null;
		System.out.println("Ref is: " + ref.length + " elements");
		java.util.Arrays.sort(ref);
		int a = 0;
		int b = 0;
		try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("Missings").toFile()))) ){
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("IndexProduct").toFile())))){
				String line = null;
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = rw.getRw().parseLine(line);
					if( java.util.Arrays.binarySearch(ref, pieces[0]) < 0 ) {
						a++;
						pw.println(line);
					}else {
						b++;
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("a = " + a + "\nb = " + b);
	}
	
}
