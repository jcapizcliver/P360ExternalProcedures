package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ExploraArchivos {

	private static final XMLMisc xmm = new XMLMisc();
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final String baseUrl = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		String line = null;
		boolean foundSERVV = false;
		boolean foundMVGR5 = false;
		boolean foundMVGR2 = false;
		java.io.File[] files = new java.io.File( /* "D:\\tmp\\muestras\\BGP_DWH" */  "D:\\tmp\\EjemploBGPArchivoIntegraciones"  ).listFiles(ff->ff.getName().endsWith(".xml"));
		for(java.io.File f : files) {
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f.getAbsolutePath())))) {
				while((line = br.readLine()) != null) {
					if(line.contains("Value AttributeID=\"SERVV\"")) {
						foundSERVV = true;
						System.out.println("Found SERVV in " + f.getName());
						try{
							System.out.println("\t" + java.util.regex.Pattern.compile("\\>(.+\\)<").matcher(line).group() );
						}catch(IllegalStateException e) {
							System.out.println("\t" + line);
						}
					}else
						if(line.contains("Value AttributeID=\"MVGR2\"")) {
							System.out.println("Found MVGR2" + f.getName());
							try{
								System.out.println("\t" + java.util.regex.Pattern.compile("\\>(.+)\\<").matcher(line).group() );
							}catch(IllegalStateException e) {
								System.out.println("\t" + line);
							}
							foundMVGR2 = true;
					}
					else
						if(line.contains("Value AttributeID=\"MVGR5\"")) {
							System.out.println("Found MVGR5" + f.getName());
							try{
								System.out.println("\t" + java.util.regex.Pattern.compile("\\>(.+)\\<").matcher(line).group() );
							}catch(IllegalStateException e) {
								System.out.println("\t" + line);
							}
							foundMVGR5 = true;
					}
				}
				if(foundSERVV) {
					foundSERVV = false;
				}
				if(foundMVGR2) {
					foundMVGR2 = false;
				}
				if(foundMVGR5) {
					foundMVGR5 = false;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
	}


	private static String formatMillis(long millis){
	  	int days = (int)(millis/(1000*60*60*24));
	 	millis -= days*1000*60*60*24;
	  	int hours = (int) (millis/(1000*60*60));
	  	millis -= hours*1000*60*60;
	  	int minutes = (int) (millis/(1000*60));
	  	millis -= minutes*1000*60;
	  	int seconds = (int) (millis/1000);
	  	millis -= seconds*1000;
	  	return
	  		    (days < 10 ? "0" : "") + days + ":"
	  		+ (hours < 10 ? "0" : "") + hours + ":"
	  		+ (minutes < 10 ? "0" : "") + minutes + ":"
	  		+ (seconds < 10 ? "0" : "") + seconds
	  		+ "." + millis;
	  }
}
