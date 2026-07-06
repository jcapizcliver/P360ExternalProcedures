package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QuitaleLosEANs {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Set<String> a = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(args[0])))){
			String line = null;
			String[] pieces = null;
			int index = Integer.parseInt(args[1]);
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				a.add(pieces[index]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.List<String> lst = new java.util.ArrayList<>();
		int b = 0;
		for(String c : a) {
			b++;
			lst.add(c);
			if(b % 10000 == 0) {
				lst.add(0, "product");
				RetiraEANsPVIA.main(lst.toArray(new String[] {}));
				lst.clear();
			}
		}
		if(!lst.isEmpty()) {
			lst.add(0, "product");
			RetiraEANsPVIA.main(lst.toArray(new String[] {}));
			lst.clear();
		}
	}
	
}
