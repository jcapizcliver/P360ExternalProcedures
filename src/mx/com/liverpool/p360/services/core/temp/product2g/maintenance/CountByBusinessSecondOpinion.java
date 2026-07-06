package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CountByBusinessSecondOpinion {
	
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.251.2:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, String> prodToBusiness = new java.util.HashMap<>();
		java.util.Map<String, String[]> internalToExternalArticleId = new java.util.HashMap<>();
		java.util.Map<String, String> internalItemToProduct = new java.util.HashMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		Integer[] cuentas = new Integer[4];
		cuentas[0] = 0;
		cuentas[1] = 0;
		cuentas[2] = 0;
		cuentas[3] = 0;
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('EXTWG_S4H',root,\"0000.0000.RK\",'EXTWG_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			);
		qp.put("pageSize", "50000");
		Thread t1 = new Thread(()->{
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "hola").toFile()), java.nio.charset.StandardCharsets.UTF_8));
					java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "logProdCollector").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					String business = null;
					String sku = values.getJSONArray(4).getString(0);
					if(!"".equals(sku)) {
						if("".equals(values.getJSONArray(1).getString(0))) {
							String negocio = values.getJSONArray(2).getString(0);
							String extwgS4h = values.getJSONArray(3).getString(0);
							if(!"".equals(extwgS4h) && "".equals(negocio)) {
								business = "SBB";
							}else if("".equals(extwgS4h) && !"".equals(negocio)) {
								if("MARKETPLACE".equals(negocio)) {
									business = "MKP";
								}else {
									business = "LVP";
								}
							}
						}else {
							business = values.getJSONArray(1).getString(0);
						}
						if(business != null) {
							prodToBusiness.put(values.getString(0), business);
							if("LVP".equals(business)) {
								cuentas[0]++;
							}else if("MKP".equals(business)) {
								cuentas[1]++;
							}else if("SBB".equals(business)) {
								cuentas[2]++;
							}else {
								cuentas[3]++;
							}
						}else {
							pw.println(values);
						}
					}
				}, pw1::println);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			System.out.println("Finishing product collector.");
			System.out.println(prodToBusiness.size());
			System.out.println("Las cuentas (Product Level):");
			System.out.println("LVP:   " + cuentas[0]);
			System.out.println("MKP:   " + cuentas[1]);
			System.out.println("SBB:   " + cuentas[2]);
			System.out.println("Otros: " + cuentas[3]);
		});
		t1.start();
		Thread t2 = new Thread(()->{
			java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
			qp1.put("fields", 
					   "Article.SupplierAID"
					+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				);
			qp1.put("pageSize", "50000");
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "logItemDataCollector").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				rw.collectData("list", "Article", null, "byCatalog", qp1, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					String sku = values.getJSONArray(1).getString(0);
					if(!"".equals(sku)) {
						internalToExternalArticleId.put(row.getJSONObject("object").getString("id"), new String[] { values.getString(0), sku});
					}
				}, pw::println);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			System.out.println("Finishing item collector.");
		});
		t2.start();
		Thread t3 = new Thread(()->{
			java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
			qp1.put("fields", "ProductReference.ReferencedSupplierAid");
			qp1.put("pageSize", "50000");
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "logItemToProductCollector").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				rw.collectData("list", "Article", "ProductReference", "byCatalog", qp1, row -> {
					internalItemToProduct.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
				}, pw::println);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			System.out.println("Finishing item to product collector.");
		});
		t3.start();
		System.out.println("Now waiting...");
		try{ t1.join(); }catch(InterruptedException e) { e.printStackTrace(); }
		try{ t2.join(); }catch(InterruptedException e) { e.printStackTrace(); }
		try{ t3.join(); }catch(InterruptedException e) { e.printStackTrace(); }
		java.util.Set<String> bsns = new java.util.TreeSet<>( prodToBusiness.values() );
		System.out.println(bsns);
		String[] data = null;
		String business = null;
		for(java.util.Map.Entry<String, String> entry : internalItemToProduct.entrySet()) {
			data = internalToExternalArticleId.get(entry.getKey());
			if(data != null) {
				business = prodToBusiness.get(data[1]);
				if(business != null) {
					if("LVP".equals(business)) {
						cuentas[0]++;
					}else if("MKP".equals(business)) {
						cuentas[1]++;
					}else if("SBB".equals(business)) {
						cuentas[2]++;
					}else {
						cuentas[3]++;
					}
				}
			}
		}
		System.out.println("Las cuentas:");
		System.out.println("LVP:   " + cuentas[0]);
		System.out.println("MKP:   " + cuentas[1]);
		System.out.println("SBB:   " + cuentas[2]);
		System.out.println("Otros: " + cuentas[3]);
	}

}
