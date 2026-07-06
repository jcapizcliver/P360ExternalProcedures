package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DameNumeros {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.251.2:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('EXTWG_S4H',root,\"0000.0000.RK\",'EXTWG_S4H')->LookupValue.Code"
			);
		qp.put("pageSize", "100000");
		java.util.Map<String, Integer> freqsProds = new java.util.TreeMap<>();
		java.util.Map<String, Integer> freqsArticles = new java.util.TreeMap<>();
		java.util.Map<String, String> productBusiness = new java.util.TreeMap<>();
		java.util.List<String> pids = new java.util.ArrayList<>();
		System.out.println("Now collecting products...");
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String business = values.getJSONArray(0).getString(0);
			String sku = values.getJSONArray(1).getString(0);
			String negocio = values.getJSONArray(3).getString(0);
			String extwgS4H = values.getJSONArray(4).getString(0);
			if(!"".equals(sku)) {
				if("".equals(business)) {
					business = determineBusiness(negocio, extwgS4H);
					business = business == null ? "" : business;
				}
				Integer freq = freqsProds.get(business);
				freqsProds.put(business, (freq == null ? 0 : 1) + 1);
				pids.add(values.getString(2));
				productBusiness.put(values.getString(2), business);
			}
		});
		System.out.println(pids.size() + " products.");
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		java.util.Map<String, String> primalArticleToProduct = new java.util.TreeMap<>();
		java.util.List<String> internalIds = new java.util.ArrayList<>();
		StringBuilder sb = new StringBuilder();
		System.out.println("Now collecting article data from prev products...");
		for(int i=0; i<pids.size(); i++) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'");
			sb.append(pids.get(i));
			sb.append("'@1");
			if(i % 1000 == 0) {
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
					String id = row.getJSONObject("object").getString("id");
					org.json.JSONArray values = row.getJSONArray("values");
					primalArticleToProduct.put(id, values.getString(0));
					internalIds.add(id);
				});
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("products", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row -> {
				String id = row.getJSONObject("object").getString("id");
				org.json.JSONArray values = row.getJSONArray("values");
				primalArticleToProduct.put(id, values.getString(0));
				internalIds.add(id);
			});
		}
		pids.clear();
		System.out.println(internalIds.size() + " articles from prev products.");
		String pid;
		String business;
		Integer freq;
		System.out.println("Now counting articles...");
		for(int i=0; i<internalIds.size(); i++) {
			pid = primalArticleToProduct.get(internalIds.get(i));
			business = productBusiness.get(pid);
			freq = freqsArticles.get(business);
			freqsArticles.put(business, (freq == null ? 0 : freq) + 1);
		}
		System.out.println("Now merging counts...");
		for(java.util.Map.Entry<String, Integer> entry : freqsProds.entrySet()) {
			freq = freqsArticles.get(entry.getKey());
			freqsArticles.put(entry.getKey(), (freq == null ? 0 : freq) + entry.getValue());
		}
		System.out.println("Now sorting...");
		java.util.List<java.util.Map.Entry<String, Integer>> lst = new java.util.ArrayList<>(freqsArticles.entrySet());
		java.util.Collections.sort(lst, (o1,o2)->o2.getValue().compareTo(o1.getValue()));
		System.out.println("Now printing...");
		lst.forEach(entry -> System.out.println(entry.getKey() + " - " + entry.getValue()));
	}

    
	private static String determineBusiness(String negocio, String extwgS4h) {
		return "".equals(negocio) && "".equals(extwgS4h) ? null : ("".equals(negocio) && !"".equals(extwgS4h) ? "SBB": "MARKETPLACE".equals(negocio) ? "MKP" : "LVP" );
	}
	
}
