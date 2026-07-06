package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DeleteSalesItemWrong {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.List<String> lines = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("IDsProduct2G.csv").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				lines.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		String[] ref = lines.toArray(new String[] {});
		lines.clear();
		lines = null;
		System.out.println("read: " + ref.length + " elements.");
		java.util.Arrays.sort( ref );
		StringBuilder sb = new StringBuilder();
		int a = 0;
		int b  = 0;
		int c = 0;
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("ToDeleteAsProduct2G").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				b++;
				if("S49691689".equals(line)) {
					System.out.println("Here. " + java.util.Arrays.binarySearch(ref, line));
				}
				if( java.util.Arrays.binarySearch(ref, line) > -1 ) {
					c++;
					sb.append( sb.length() == 0 ? "" : "," ).append("'").append(line).append("'@1");
					a++;
					if(a == 1000) {
						System.out.println("Sending deletion...");
						qp.put("items", sb.toString());
						rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
						a = 0;
						sb.setLength(0);;
					}
				}
			}
			if( sb.length() > 0 ) {
				System.out.println("Sending last deletion... " + sb.toString());
				qp.put("items", sb.toString());
				rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
				a = 0;
				sb.setLength(0);;
			}
			System.out.println("Matches: " + c);
			System.out.println("Read: " + b + " attempts");
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
}
