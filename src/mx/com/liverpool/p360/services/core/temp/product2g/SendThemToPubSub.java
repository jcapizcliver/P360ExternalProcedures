package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SendThemToPubSub {

	public static final RESTWrapper rwp = new RESTWrapper();
	public static final RESTWorkshop rw = rwp.getRw();
	
	public static void main(String[] args) {
//		java.util.List<String> lst = new java.util.ArrayList<>();
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "Product2G.ProductNo");
//		qp.put("query", "characteristic('SupplierID') = \"156275\" and Product2G.ProductNo startsWith \"175461166\" and Product2G.CurrentStatus = \"Creación de SKU\"");
//		qp.put("pageSize", "1000");
//		rwp.collectData("list", "Product2G", null, "bySearch", qp, row -> {
//			org.json.JSONArray values = row.getJSONArray("values");
//			System.out.println(values);
//			lst.add(values.getString(0));
//		});
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDsToPubSub").toFile())))){
//			String line = null;
//			while((line = br.readLine()) != null) {
//				sendIt(null, line);
//			}
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
		java.util.List<String> lst = java.util.Arrays.asList((
				        "1754611683602471\r\n"
				        + "1754611683602521\r\n"
				        + "1754611683602524\r\n"
				        + "1754611683602531\r\n"
				        + "1754611683602536\r\n"
				        + "1754611683602546\r\n"
				        + "1754611683602561\r\n"
				        + "1754611683602551\r\n"
				        + "1754611683602566\r\n"
				        + "1754611683602581\r\n"
				        + "1754611683602582\r\n"
				        + "1754611683602606\r\n"
				        + "1754611683602611\r\n"
				        + "1754611683602736\r\n"
				        + "1754611683602781\r\n"
				        + "1754611683602791\r\n"
				        + "1754611683602796\r\n"
				        + "1754611683602801\r\n"
				        + "1754611683602841\r\n"
				        + "1754611683602866\r\n"
				        + "1754611683602888\r\n"
				        + "1754611683602879\r\n"
				        + "1754611683602906\r\n"
				        + "1754611683602910\r\n"
				        + "1754611683602921\r\n"
				        + "1754611683602951\r\n"
				        + "1754611683602956\r\n"
				        + "1754611683602965\r\n"
				        + "1754611683602973\r\n"
				        + "1754611683602977\r\n"
				        + "1754611683602981\r\n"
				        + "1754611683602989\r\n"
				        + "1754611683602993\r\n"
				        + "1754611683603006\r\n"
				        + "1754611683603002\r\n"
				        + "1754611683602456\r\n"
				        + "1754611683602466\r\n"
				        + "1754611683638498\r\n"
				        + "1754611683638521\r\n"
				        + "1754611683638538\r\n"
				        + "1754611683638683\r\n"
				        + "1754611683638763\r\n"
				        + "1754611683638881\r\n"
				        + "1754611683638248\r\n"
				        + "1754611683638348\r\n"
				        + "1754611683638530\r\n"
				        + "1754611683638638\r\n"
				        + "1754611683638753\r\n"
				        + "1754611683602541\r\n"
				        + "1754611683602576\r\n"
				        + "1754611683602726\r\n"
				        + "1754611683602900\r\n"
				        + "1754611683602969\r\n"
				        + "1754611683638153\r\n"
				        + "1754611683637909\r\n"
				        + "1754611683637934\r\n"
				        + "1754611683637939\r\n"
				        + "1754611683637949\r\n"
				        + "1754611683637979\r\n"
				        + "1754611683638010\r\n"
				        + "1754611683638020\r\n"
				        + "1754611683638070\r\n"
				        + "1754611683638138\r\n"
				        + "1754611683638203\r\n"
				        + "1754611683638213\r\n"
				        + "1754611683638283\r\n"
				        + "1754611683638303\r\n"
				        + "1754611683638338\r\n"
				        + "1754611683638333\r\n"
				        + "1754611683638343\r\n"
				        + "1754611683638353\r\n"
				        + "1754611683638358\r\n"
				        + "1754611683638398\r\n"
				        + "1754611683638418\r\n"
				        + "1754611683638468\r\n"
				        + "1754611683638478\r\n"
				        + "1754611683638488\r\n"
				        + "1754611683638510\r\n"
				        + "1754611683653626\r\n"
				        + "1754611683653635\r\n"
				).split("\\r\\n"));
//				getS();
		for(String value : lst) {
			sendIt(null, value);
		}
	}
	
	private static void sendIt(String sku, String id) {
		String sa = PropertiesManager.get("p360.contingency.gcp.service_account_back");
//		String pubSubDevSA = "C:\\opt\\LVP\\dev\\crp-dev-dig-vccatalog-b74410667aea.json";
//		String pubSubQaSA = "C:\\opt\\LVP\\dev\\crp-qas-dig-vccatalog-416185bab156.json";
//		String pubSubDevProject = "crp-dev-dig-vccatalog";
		String pubSubQaProject = PropertiesManager.get("p360.contingency.gcp.project_back"); // "crp-qas-dig-vccatalog";
//		String topic = PropertiesManager.get("p360.contingency.gcp.idmc_put_products"); // "idmc_post_products";
		String topic = PropertiesManager.get("p360.contingency.gcp.post_products_topic"); // "idmc_post_products";
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("http://172.18.251.7:8080/process-engine/public/rt/GetProposals");
//		rw.setBaseUrl("http://172.18.237.165:8080/process-engine/public/rt/GetProposals");
		rw.getRc().getHeader().remove("Authorization");
		org.json.JSONObject holi = null;
		org.json.JSONObject resp = rw.makeRequest("POST", "", qp, (holi = new org.json.JSONObject().put("input", new org.json.JSONObject().put("products", new org.json.JSONArray().put(new org.json.JSONObject().put("sku", sku == null ? "" : sku).put("proposalId", id))).toString())).toString());
		System.out.println("-->" + rw.getRawResponse());
		org.json.JSONArray losesos = new org.json.JSONArray(rw.getRawResponse());
//		System.out.println(sa);
//		System.out.println(pubSubQaProject);
//		System.out.println(topic);
		if(losesos.getJSONObject(0).has("currentStatus")) {
//			new PubSubGCP(pubSubDevSA, pubSubDevProject, topic).publishMessage(losesos.getJSONObject(0).toString());
			new PubSubGCP(sa,  pubSubQaProject,  topic).publishMessage( new org.json.JSONObject().put("products", losesos).toString());
			
			System.out.println("Sent.");
		}else {
			System.out.println("Not sent.");
		}
	}
	
	public String getIdFromSKU(String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "characteristic('SKU',-1) wildcard \"%" + sku + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		if(response == null) {
		}else{
			rows = response.getJSONArray("rows");
			if(rows.length() > 0 ) {
				return rows.getJSONObject(0).getJSONArray("values").getString(0);
			}else {
				System.out.println("SKU not found: " + sku);
			}
		}
		return null;
	}
	
	private void yeah() {
//		jsonResponse = new org.json.JSONObject()
//				.put("products", new org.json.JSONArray()
//						.put(new org.json.JSONObject()
//								.put("proposalId", externalId)
//								.put("updatedAt", creationDate)
//								.put("internalStatus", internalStatus)
//								.put("externalStatus", externalStatus)
//								.put("previousStatus", previousStatus)
//								.put("sku", sku == null ? "" : sku)
//								.put("upcEan", ean == null ? "" : ean)
//								.put("entityType", "Variant")));
	}
	
	private static final String[] sample = (""
//			"1701976676071136\r\n"
//			+ "1701976676071228\r\n"
//			+ "1701976676071243\r\n"
//			+ "1701976676071247\r\n"
//			+ "1701976676071254\r\n"
//			+ "1701976676071265\r\n"
//			+ "1701976676071280\r\n"
//			+ "1701976676071288\r\n"
//			+ "1701976676071309\r\n"
//			+ "1701976676071328\r\n"
//			+ "1701976676071351\r\n"
//			+ "1701976676071357\r\n"
//			+ "1701976676071371\r\n"
//			+ "1701976676071401\r\n"
//			+ "1701976676071405\r\n"
//			+ "1701976676071412\r\n"
//			+ "1701976676071424\r\n"
//			+ "1701976676071434\r\n"
//			+ "1701976676071461\r\n"
//			+ "1701976676071486\r\n"
//			+ "1701976676071546\r\n"
//			+ "1701976676071565\r\n"
//			+ "1701976676071569\r\n"
//			+ "1701976676071575\r\n"
//			+ "1701976676071582\r\n"
//			+ "1701976676071602\r\n"
//			+ "1701976676071606\r\n"
//			+ "1701976676071614\r\n"
//			+ "1701976676071628\r\n"
//			+ "1701976676071658\r\n"
//			+ "1701976676071681\r\n"
//			+ "1701976676071711\r\n"
//			+ "1701976676071743\r\n"
//			+ "1701976676071754\r\n"
//			+ "1701976676071763\r\n"
//			+ "1701976676071781\r\n"
//			+ "1701976676071794\r\n"
//			+ "1701976676071804\r\n"
//			+ "1701976676071819\r\n"
//			+ "1701976676071859\r\n"
//			+ "1701976676071864\r\n"
//			+ "1701976676071871\r\n"
//			+ "1701976676071896\r\n"
//			+ "1701976676071911\r\n"
//			+ "1701976676071921\r\n"
//			+ "1701976676071931\r\n"
//			+ "1701976676071954\r\n"
//			+ "1701976676071959\r\n"
//			+ "1701976676071976\r\n"
//			+ "1701976676073035"
//			    "1701976676071134\r\n"
//			    + "1701976676071140\r\n"
//			    + "1701976676071144\r\n"
//			    + "1701976676071145\r\n"
//			    + "1701976676071146\r\n"
//			    + "1701976676071147\r\n"
//			    + "1701976676071148\r\n"
//			    + "1701976676071152\r\n"
//			    + "1701976676071154\r\n"
//			    + "1701976676071158\r\n"
//			    + "1701976676071159\r\n"
//			    + "1701976676071160\r\n"
//			    + "1701976676071161\r\n"
//			    + "1701976676071162\r\n"
//			    + "1701976676071163\r\n"
//			    + "1701976676071164\r\n"
//			    + "1701976676071165\r\n"
//			    + "1701976676071166\r\n"
//			    + "1701976676071167\r\n"
//			    + "1701976676071168\r\n"
//			    + "1701976676071169\r\n"
//			    + "1701976676071170\r\n"
//			    + "1701976676071171\r\n"
//			    + "1701976676071172\r\n"
//			    + "1701976676071173\r\n"
//			    + "1701976676071174\r\n"
//			    + "1701976676071175\r\n"
//			    + "1701976676071176\r\n"
//			    + "1701976676071178\r\n"
//			    + "1701976676071179\r\n"
//			    + "1701976676071180\r\n"
//			    + "1701976676071181\r\n"
//			    + "1701976676071183\r\n"
//			    + "1701976676071184\r\n"
//			    + "1701976676071186\r\n"
//			    + "1701976676071187\r\n"
//			    + "1701976676071188\r\n"
//			    + "1701976676071189\r\n"
//			    + "1701976676071191\r\n"
//			    + "1701976676071192\r\n"
//			    + "1701976676071193\r\n"
//			    + "1701976676071195\r\n"
//			    + "1701976676071196\r\n"
//			    + "1701976676071197\r\n"
//			    + "1701976676071198\r\n"
//			    + "1701976676071199\r\n"
//			    + "1701976676071200\r\n"
//			    + "1701976676071201\r\n"
//			    + "1701976676071202\r\n"
//			    + "1701976676071205\r\n"
//			    + "1701976676071206\r\n"
//			    + "1701976676071207\r\n"
//			    + "1701976676071208\r\n"
//			    + "1701976676071209\r\n"
//			    + "1701976676071210\r\n"
//			    + "1701976676071212\r\n"
//			    + "1701976676071215\r\n"
//			    + "1701976676071216\r\n"
//			    + "1701976676071217\r\n"
//			    + "1701976676071218\r\n"
//			    + "1701976676071220\r\n"
//			    + "1701976676071223\r\n"
//			    + "1701976676071224\r\n"
//			    + "1701976676071225\r\n"
//			    + "1701976676071226\r\n"
//			    + "1701976676071227"
//			  + "LVP1033615155"
			).split("\\r\\n");
}
