package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EliminaRegistrosDeMasDeVerdad {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> articleInternalIds = new java.util.ArrayList<>();
		java.util.List<String> productExternalIds = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = line.split(",");
				if(pieces.length > 0) {
					if(pieces.length > 1) {
						productExternalIds.add(pieces[1]);
						articleInternalIds.add(pieces[0]);
					}
				}
			}
			java.util.Set<String> p2g = new java.util.TreeSet<>(productExternalIds);
			java.util.Set<String> art = new java.util.TreeSet<>(articleInternalIds);
			productExternalIds.clear();
			productExternalIds = null;
			articleInternalIds.clear();
			articleInternalIds = null;
			int a = 0;
			StringBuilder sb = new StringBuilder();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("pageSize", "5000");
			int b = 0;
			for(String s : art) {
				sb.append(sb.length() == 0 ? "" : ",").append(s);
				a++;
				if(a == 10) {
					qp.put("items", sb.toString());
					rw.deleteData("list", "Article", null, "byItems", qp, System.out::println);
					b += a;
					System.out.println( b + "/" + art.size() + " items deleted" );
					a = 0;
					sb.setLength(0);
				}
			}
			if(a > 0) {
				qp.put("items", sb.toString());
				rw.deleteData("list", "Article", null, "byItems", qp, System.out::println);
				b += a;
				System.out.println( b + "/" + art.size() + " items deleted" );
				a = 0;
				sb.setLength(0);
			}
			b = 0;
			for(String s : p2g) {
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(s).append("'@1");
				a++;
				if(a == 10) {
					qp.put("items", sb.toString());
					rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
					b += a;
					System.out.println( b + "/" + p2g.size() + " items deleted" );
					a = 0;
					sb.setLength(0);
				}
			}
			if(a > 0) {
				qp.put("items", sb.toString());
				rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
				b += a;
				System.out.println( b + "/" + p2g.size() + " items deleted" );
				a = 0;
				sb.setLength(0);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
