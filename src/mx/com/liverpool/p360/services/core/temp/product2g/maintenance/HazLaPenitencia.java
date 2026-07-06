package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class HazLaPenitencia {

	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("http://localhost:8080/process-engine/public/rt");
		rw.getRw().getRc().getHeader().remove("Authorization");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("/", "u01", "workshop", "tomcat", "apache-tomcat-9.0.104", "logs", "tupenitenciashort").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			int i = 0;
			int skip = 78 + 84;
			while((line = br.readLine()) != null) {
				if(skip > 0) {
					skip--;
				}else {
					rw.getRw().makeRequest("POST", "/CreateProposal", qp, new org.json.JSONObject().put("input", line).toString());
					System.out.println(rw.getRw().getRawResponse());
				}
				i++;
				System.out.println(i + "/1131");
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
