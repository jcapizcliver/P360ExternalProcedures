package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class LoQueFaltaPubSubPOST {

	
	private static final PubSubGCP pubPostProducts = new PubSubGCP(
		    PropertiesManager.get("p360.contingency.gcp.service_account_back"),
		    PropertiesManager.get("p360.contingency.gcp.project_back"),
		    PropertiesManager.get("p360.contingency.gcp.post_products_topic")
		);
	
	private static int count = 0;
	
	public static void main(String[] args) {
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject body = new org.json.JSONObject();
		body.put("products", rows);
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length == 0) {
				return;
			}
			if( (!"".equals(row[5]) && !row[5].equals(row[1])) || (!"".equals(row[6]) && !row[6].equals(row[2])) || (!"".equals(row[7]) && !row[7].equals(row[3])) ) {
				if(count < 100) {
					System.out.println(new org.json.JSONObject().put("currentStatus", row[5]).put("previousStatus", row[6]).put("externalStatus", row[7]).put("proposalId", row[4]));
					count++;
				}
				rows.put(new org.json.JSONObject().put("currentStatus", row[5]).put("previousStatus", row[6]).put("externalStatus", row[7]).put("proposalId", row[4]));
				if(rows.length() == 200) {
					pubPostProducts.publishMessage( body.toString() );
					while(rows.length() > 0 ) {
						rows.remove(0);
					}
				}
			}
		});
		parser.parse(java.nio.file.Paths.get("/", "u01", "stage", "OutputMissmatchForStatusEUC_P360.csv"));
		if(rows.length() > 0) {
			pubPostProducts.publishMessage( body.toString() );
			while(rows.length() > 0 ) {
				rows.remove(0);
			}
		}
	}
	
}
