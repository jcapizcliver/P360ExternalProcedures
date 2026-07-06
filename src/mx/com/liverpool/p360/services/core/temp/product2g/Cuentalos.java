package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class Cuentalos {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.251.2:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		int[] cuentas = new int[4];
		cuentas[0] = 0;
		cuentas[1] = 0;
		cuentas[2] = 0;
		cuentas[3] = 0;
		java.util.Map<String, String> data = new java.util.HashMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business',-1)->LookupValue.Code");
		qp.put("query", "not characteristic('Business') is empty");
		qp.put("pageSize", "50000");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String id = values.getString(0);
			String business = values.getJSONArray(1).getString(0);
			if(id.length() < 15) {
				data.put(id, business);
				if("LVP".equals(business)) {
					cuentas[0]++;
				}else if("SBB".equals(business)) {
					cuentas[2]++;
				}else if("MKP".equals(business)) {
					cuentas[1]++;
				}else {
					cuentas[3]++;
				}
			}
		});
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		qp.remove("query");
		rw.collectData("list", "Article", "ProductReference", "withProduct", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String business = data.get(values.getString(0));
			if(business == null) {
				
			} else {
				if("LVP".equals(business)) {
					cuentas[0]++;
				}else if("SBB".equals(business)) {
					cuentas[2]++;
				}else if("MKP".equals(business)) {
					cuentas[1]++;
				}else {
					cuentas[3]++;
				}
			}
		}, System.out::println);
		System.out.println("LVP: " + cuentas[0]);
		System.out.println("MKP: " + cuentas[1]);
		System.out.println("SBB: " + cuentas[2]);
		System.out.println("Otros: " + cuentas[3]);
	}
	
}
