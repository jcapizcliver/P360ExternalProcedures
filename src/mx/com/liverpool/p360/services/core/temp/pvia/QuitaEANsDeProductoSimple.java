package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class QuitaEANsDeProductoSimple {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile().getName())))){
			String line = br.readLine();
			String[] pieces = null;
			org.json.JSONArray items = new org.json.JSONArray();
			DataRequestor dr = new DataRequestor();
			int count = 0;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				items.put(pieces[1]);
				count++;
				if(items.length() == 2000) {
					dr.retiraEANProductNo(items);
					while(items.length() > 0) {
						items.remove(0);
					}
					System.out.println(count);
				}
			}
			if(items.length() > 0) {
				dr.retiraEANProductNo(items);
				while(items.length() > 0) {
					items.remove(0);
				}
				System.out.println(count);
			}
			System.out.println("Done.");
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println(rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
}
