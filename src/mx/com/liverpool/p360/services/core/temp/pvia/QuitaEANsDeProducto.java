package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class QuitaEANsDeProducto {
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile().getName())))){
			String line = br.readLine();
			String[] pieces = null;
			org.json.JSONArray items = new org.json.JSONArray();
			try(DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
				
				@Override
				public void logE(Exception e) {
				}
				
				@Override
				public void log(String message) {
				}
			} )){
				DataRequestor dr = new DataRequestor(dastub);
				int count = 0;
				while((line = br.readLine()) != null) {
					pieces = rw.getRw().parseLine(line);
					if("1100".equals(pieces[2])) {
						items.put(pieces[3]);
						count++;
						if(items.length() == 2000) {
							dr.retiraEANProductNo(items);
							while(items.length() > 0) {
								items.remove(0);
							}
							System.out.println(count);
						}
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
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
}
