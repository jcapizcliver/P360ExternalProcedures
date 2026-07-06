package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CountByBusiness {
	
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.251.2:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, String> prodToBusiness = new java.util.TreeMap<>();
		java.util.Map<String, Integer> frecuenciasPorNegocio = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('EXTWG_S4H',root,\"0000.0000.RK\",'EXTWG_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU')"
			);
		qp.put("pageSize", "20000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "hola").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
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
						}else {
							System.out.println("Caso extraño: " + values);
						}
					}else {
						business = values.getJSONArray(1).getString(0);
					}
					if(business != null) {
						prodToBusiness.put(values.getString(0), business);
						Integer freq = frecuenciasPorNegocio.get(business);
						frecuenciasPorNegocio.put(business, (freq == null ? 0 : freq) + 1);
					}else {
						pw.println(values.getString(0));
					}
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println(prodToBusiness.size());
		java.util.Set<String> bsns = new java.util.TreeSet<>( prodToBusiness.values() );
		System.out.println(bsns);
		System.out.println(frecuenciasPorNegocio);
		int a = 0;
		StringBuilder sb = new StringBuilder();
		qp0.put("fields", "ProductReference.ReferencedSupplierAid");
		qp0.put("pageSize", "10000");
		long soFar = 0;
		for(String id : prodToBusiness.keySet()) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'");
			sb.append(id);
			sb.append("'@1");
			a++;
			if(a == 1000) {
				qp0.put("items", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp0, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					String productNo = values.getString(0);
					String business = prodToBusiness.get(productNo);
					if(business != null) {
						Integer freq = frecuenciasPorNegocio.get(business);
						frecuenciasPorNegocio.put(business, (freq == null ? 0 : freq) + 1);
					}
				});
				a = 0;
				sb.setLength(0);
				soFar += 1000;
				System.out.println("---->" + soFar + "/" + prodToBusiness.size() + " - ");
			}
		}
		if(a > 0) {
			qp0.put("items", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byItems", qp0, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				String productNo = values.getString(0);
				String business = prodToBusiness.get(productNo);
				if(business != null) {
					Integer freq = frecuenciasPorNegocio.get(business);
					frecuenciasPorNegocio.put(business, (freq == null ? 0 : freq) + 1);
				}
			});
			a = 0;
			sb.setLength(0);
		}
		System.out.println(frecuenciasPorNegocio);
	}

}
