package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;

public class EnviaEstadosPubSubPOST {

	
	private static final PubSubGCP pubPostProducts = new PubSubGCP(
		    PropertiesManager.get("p360.contingency.gcp.service_account_back"),
		    PropertiesManager.get("p360.contingency.gcp.project_back"),
		    PropertiesManager.get("p360.contingency.gcp.post_products_topic")
		);
	
	public static void main(String[] args) {
		java.io.File[] jsons = new java.io.File("C:\\USers\\Juan Capiz Castro\\Downloads\\json").listFiles();
		for(java.io.File f : jsons) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				String line = null;
				StringBuilder sb = new StringBuilder();
				while((line = br.readLine()) != null) {
					sb.append(line);
				}
				org.json.JSONObject message = new org.json.JSONObject(sb.toString());
				pubPostProducts.publishMessage(message.toString());
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
