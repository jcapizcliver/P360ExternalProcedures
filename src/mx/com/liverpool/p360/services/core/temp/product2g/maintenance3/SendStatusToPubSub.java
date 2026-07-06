package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SendStatusToPubSub {


	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		PubSubGCP pub = new PubSubGCP();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "LookupValueLang.Name(es)");
		qp.put("lookup", "'ExternalStatus'");
		java.util.Map<String, String> externalStatus = new java.util.HashMap<>();
		rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> externalStatus.put(row.getJSONObject("object").getString("id").replaceAll("@.+", ""), row.getJSONArray("values").getString(0)));
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			String current = null;
			String prev = null;
			String external = null;
			org.json.JSONArray items = new org.json.JSONArray();
			org.json.JSONObject item = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				current = statusMap.get(pieces[1]);
				prev = statusMap.get(pieces[2]);
				external = externalStatus.get(pieces[3]);
				item = new org.json.JSONObject();
				item.put("proposalId", pieces[0])
				.put("internalStatus", current)
				.put("externalStatus", external)
				.put("previousStatus", prev);
				items.put(item);
//				System.out.println(item);
//				if(items.length() == 10) {
//					System.exit(0);
//				}
				if(items.length() == 1000) {
					pub.publishMessage( 
							PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
							PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
							PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
							new org.json.JSONObject().put("products", items).toString()
							);
					while(items.length() > 0) {
						items.remove(0);
					}
				}
			}
			if(items.length() > 0) {
				pub.publishMessage( 
						PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
						PropertiesManager.get( "p360.contingency.gcp.idmc_post_products" ), 
						PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), 
						new org.json.JSONObject().put("products", items).toString()
						);
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static java.util.Map<String, String> toMap (String[] values){
		java.util.Map<String, String> data = new java.util.HashMap<>();
		String[] chunk = null;
		for(int i=0; i<values.length; i++) {
			chunk = values[i].split("\t");
			data.put(chunk[0], chunk[1]);
		}
		return data;
	}
	
	private static final java.util.Map<String, String> statusMap = toMap( ("1001	Proposal Generated\r\n"
			+ "1002	Pending Enrichment\r\n"
			+ "1003	Purchase Revision\r\n"
			+ "1004	Image Load\r\n"
			+ "1005	Rejected\r\n"
			+ "1006	To Be Updated\r\n"
			+ "1007	Approved\r\n"
			+ "1008	Modified\r\n"
			+ "1009	Canceled\r\n"
			+ "1010	Liverpool in progress\r\n"
			+ "1011	Sending in progress\r\n"
			+ "1020	SKU Creation\r\n"
			+ "1021	Data Gobernance\r\n"
			+ "1022	QA Revision\r\n"
			+ "1023	Category\r\n"
			+ "1024	Publish Rejected\r\n"
			+ "1025	Deleted\r\n"
			+ "1026	In Foro Process\r\n"
			+ "10031	Draft\r\n"
			+ "1027	Purchase Rejected\r\n"
			+ "1028	QA Rejected\r\n"
			+ "1029	Governance Rejected\r\n"
			+ "1030	Category Rejected\r\n"
			+ "1031	Repopulation\r\n"
			+ "1032	Cataloguing Exception\r\n"
			).split("\\r\\n") );
}
