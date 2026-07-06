package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EANsDeIndividuales {

	
	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> idsMap = new java.util.HashMap<>(); 
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		java.util.Map<String, String> productEANs = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.EAN");
		qp.put("query", "not Product2G.EAN is empty and Product2GExtraData.SAPObjectType(MX)->LookupValue.Code = \"00\" and not Product2G.ProductNo startsWith \"17546116\"");
		qp.put("pageSize", "2000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ProductosIndividualesMigrados").toFile())))){
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				idsMap.put(row.getJSONArray("values").getString(0), row.getJSONObject("object").getString("id"));
				productEANs.put(values.getString(0), values.getString(1));
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		qp.put("fields", "Article.SupplierAID,Article.EAN");
		qp.remove("query");
		java.util.Map<String, String[]> artData = new java.util.HashMap<>();
		StringBuilder sb = new StringBuilder();
		int a = 0;
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", "ProductReference.ReferencedSupplierAid");
		qp0.put("pageSize", "5000");
		java.util.Map<String, String> artToProd = new java.util.HashMap<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ArticulosDeProductosMigrados").toFile())))){
			for(java.util.Map.Entry<String, String> entry : idsMap.entrySet()) {
				sb.append(sb.length() == 0 ? "" : ",").append(entry.getValue());
				a++;
				if(a % 1000 == 0) {
					qp.put("products", sb.toString());
					rw.collectData("list", "Article", null, "byProducts", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						artData.put(row.getJSONObject("object").getString("id"), new String[] { values.getString(0), values.getString(1) });
					});
					qp0.put("products", sb.toString());
					rw.collectData("list", "Article", "ProductReference", "byProducts", qp0, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						artToProd.put(row.getJSONObject("object").getString("id"), values.getString(0) );
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					artData.put(row.getJSONObject("object").getString("id"), new String[] { values.getString(0), values.getString(1) });
				});
				qp0.put("products", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp0, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					artToProd.put(row.getJSONObject("object").getString("id"), values.getString(0) );
				});
				sb.setLength(0);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Map<String, Long> freqs = new java.util.HashMap<>();
		Long freq = null;
		String pid = null;
		for(java.util.Map.Entry<String, String> entry : artToProd.entrySet()) {
			pid = idsMap.get(entry.getValue());
			freq = freqs.get(pid);
			freqs.put(pid, (freq == null ? 0 : freq) + 1);
		}
		java.util.Iterator<java.util.Map.Entry<String, Long>> iter = freqs.entrySet().iterator();
		java.util.Map.Entry<String, Long> ent = null;
		while(iter.hasNext()) {
			ent = iter.next();
			if(ent.getValue() > 1)
				iter.remove();
		}
		String artEAN = null;
		String prodEAN = null;
		java.util.List<String> toDelete = new java.util.ArrayList<>();
		for(java.util.Map.Entry<String, String> entry : artToProd.entrySet()) {
			if(freqs.containsKey(entry.getValue())) {
				artEAN = artData.get(entry.getKey())[1];
				prodEAN = productEANs.get(entry.getValue());
				if(artEAN != null && prodEAN != null && artEAN.equals(prodEAN)) {
					toDelete.add(entry.getValue());
				}
			}
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LosElegidos").toFile())))){
			toDelete.forEach(System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
