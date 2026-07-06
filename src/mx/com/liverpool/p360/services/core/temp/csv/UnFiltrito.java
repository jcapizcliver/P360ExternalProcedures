package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class UnFiltrito {

	
	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		int count = 0;
		java.util.Map<String, Integer> sups = new java.util.TreeMap<>();
		java.util.LinkedList<String[]> data = new java.util.LinkedList<>();
		Integer freq = 0;
		int exemplars = 20;
		int m = 0;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(
				"C:", "opt", "LVP", "desorden", "Migración", "second09092025", "flattened3.csv"
		).toString())));
		java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", 
				"desorden", "Migración", "second09092025", "orale.csv").toString())))){
			String line = null;
			String delim = "\"";
			String sep = ",";
			String esc = "\\";
			String[] pieces = null;
			java.util.ArrayList<String> header = new java.util.ArrayList<>( java.util.Arrays.asList( rw.parseLine(line = br.readLine(), delim, sep, esc) ) );
			pw.println(line);
			System.out.println(header);
			int i = header.indexOf("SupplierID");
			int a = header.indexOf("SupplierName");
			int b = header.indexOf("SAPObjectType");
			int c = header.indexOf("SkuType");
			int d = header.indexOf("LastDateApprove");
			while((line = br.readLine()) != null) {
				pieces = rw.parseLine(line, delim, sep, esc);
				if(PROVEEDORES.contains(pieces[i])) {
					data.addLast(pieces);
					freq = sups.get(pieces[i]);
					sups.put(pieces[i], (freq == null ? 0 : freq) + 1);
					pw.println(line);
//					if(pieces[d].contains("."))
//					System.out.println(pieces[d]);
				}
				if("".equals(pieces[i]) /* && "01".equals(pieces[b]) */ ) {
					if(exemplars > 0) {
						System.out.println(pieces[0] + " - " + pieces[b]);
						exemplars--;
					}
					m++;
				}
				count++;
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Missings: " + m);
		System.out.println(count - data.size());
		java.util.LinkedList<java.util.Map.Entry<String, Integer>> entr = new java.util.LinkedList<>(sups.entrySet());
		java.util.Collections.sort(entr, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
		entr.forEach(System.out::println);
		System.out.println("***");
		System.out.println(sups.size());
		System.out.println("***");
		System.out.println(PROVEEDORES.size());
		for(String p : PROVEEDORES) {
			if(!sups.containsKey(p)) {
				System.out.println("-->" + p);
			}
		}
	}
	
	
	private static final java.util.Set<String> PROVEEDORES = new java.util.TreeSet<>(java.util.Arrays.asList(("146779\r\n"
			+ "134706\r\n"
			+ "155692\r\n"
			+ "10207\r\n"
			+ "157142\r\n"
			+ "104910\r\n"
			+ "152748\r\n"
			+ "380\r\n"
			+ "128026\r\n"
			+ "107758\r\n"
			+ "132729\r\n"
			+ "153860\r\n"
			+ "153077\r\n"
			+ "160729\r\n"
			+ "150581\r\n"
			+ "124617\r\n"
			+ "155873\r\n"
			+ "138946\r\n"
			+ "54961\r\n"
			+ "147921\r\n"
			+ "156547\r\n"
			+ "153671\r\n"
			+ "159118\r\n"
			+ "148676\r\n"
			+ "146482\r\n"
			+ "132611\r\n"
			+ "129278\r\n"
			+ "148906\r\n"
			+ "160137\r\n"
			+ "158308\r\n"
			+ "155504\r\n"
			+ "157179\r\n"
			+ "153529\r\n"
			+ "158420\r\n"
			+ "156764\r\n"
			+ "150148\r\n"
			+ "160408\r\n"
			+ "156774\r\n"
			+ "156299\r\n"
			+ "159092\r\n"
			+ "151484\r\n"
			+ "156030\r\n"
			+ "158364\r\n"
			+ "159240").split("\r\n")));
	
}
