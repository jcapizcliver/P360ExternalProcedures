package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ReadToResendToPubSub {

	private static final PubSubGCP pub = new PubSubGCP(
			 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ),
			 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
			 PropertiesManager.get( "p360.contingency.gcp.post_products_topic" )
			);
	
	public static void main(String[] args) {
		
		long init = System.currentTimeMillis();
		org.json.JSONArray jps = new org.json.JSONArray();
		org.json.JSONObject body = new org.json.JSONObject().put("products", jps);
		int[] cnt = new int[] {0};
		finale.forEach(System.out::println);
		System.out.println("HOLA");
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length > 0 && !"Identifier".equals(row[0]) && finale.contains(row[0])) {
				org.json.JSONObject jp = new org.json.JSONObject();
				jp.put("proposalId", row[0]);
				jp.put("previousStatus", row[1]);
				jp.put("currentStatus" ,  row[2]);
				jp.put("externalStatus", row[8]);
				jp.put("Business", row[4]);
				jp.put("SKU", row[5]);
				jp.put("SBB".equals(row[3]) ? "MainBarCodeS4H" : "MainBarCode", row[6]);
				jp.put("Direction", row[10]);
				jp.put("Section", row[12]);
				jps.put(jp);
				cnt[0]++;
				if(jps.length() == 200) {
					pub.publishMessage( body.toString() );
					System.out.println(body);
					System.exit(0);
					while(jps.length() > 0) {
						jps.remove(0);
					}
					System.out.print(".");
					if(cnt[0] % 1000000 == 0) {
						System.out.println(cnt[0]);
					}
				}
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		if(jps.length() > 0) {
			pub.publishMessage( body.toString() );
			System.out.println(body);
			System.exit(0);
			while(jps.length() > 0) {
				jps.remove(0);
			}
		}
		System.out.println(cnt[0]);
		System.out.println("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
		
	}
	
	
	static final java.util.List<String> finale = java.util.Arrays.asList(("1754611671157241\r\n"
			+ "1754611671156860\r\n"
			+ "1754611671158890\r\n"
			+ "1754611671161020\r\n"
			+ "1754611671161245\r\n"
			+ "1754611671161423\r\n"
			+ "1754611671187899\r\n"
			+ "1754611672776575\r\n"
			+ "1754611672776769\r\n"
			+ "17544611672776889\r\n"
			+ "1754611672777570\r\n"
			+ "1754611672778563\r\n"
			+ "1754611672780184\r\n"
			+ "1754611672782190\r\n"
			+ "1754611672784882\r\n"
			+ "1754611672786141\r\n"
			+ "1754611672787325\r\n"
			+ "1754611671161746\r\n"
			+ "1754611671162099\r\n"
			+ "1754611671162516\r\n"
			+ "1754611671162760\r\n"
			+ "1754611671155403\r\n"
			+ "1754611672801288\r\n"
			+ "1754611672801829\r\n"
			+ "1754611672802208\r\n"
			+ "1754611672802658\r\n"
			+ "1754611672802788\r\n"
			+ "1754611672803045\r\n"
			+ "1754611672803258\r\n"
			+ "1754611672803363\r\n"
			+ "1754611672803631\r\n"
			+ "1754611672804020\r\n"
			+ "1754611672804292\r\n"
			+ "1754611672804544\r\n"
			+ "1754611672804655\r\n"
			+ "1754611672804983\r\n"
			+ "1754611672805350").split("\\r\\n"));
	
}
