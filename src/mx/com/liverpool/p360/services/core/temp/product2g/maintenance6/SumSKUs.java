package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SumSKUs {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "UnosEsos.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LabToCheckIfSendableSKUs.yxy").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LabToCheckIfSendableSKUs.yxy").toFile())))){
			String line = null;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("fields", 
					   "Product2G.ProductNo"
					+ ",Product2G.SKU"
					+ ",Product2GLang.ProductName(es)"
					+ ",SimpleProduct2GCharacteristicValueLang.Value('Name',-1)"
					+ ",Product2GStructureMap.StructureGroup('Sitios Web')->StructureGroup.Identifier"
					+ ",Product2G.CurrentStatus"
					+ ",Product2G.PrevStatus"
				);
			qp.put("formatData", "true");
			qp.put("pageSize", "1200");
			java.util.List<String> ids = new java.util.ArrayList<>();
			StringBuilder sb = new StringBuilder();
			java.util.Map<String, String> qp2 = new java.util.HashMap<>();
			qp2.put("fields", "ProductReference.ReferencedSupplierAid");
			qp2.put("pageSize", "6000");
			java.util.Map<String, String> qp3 = new java.util.HashMap<>();
			qp3.put("fields", 
					   "Article.SupplierAID"
					+ ",ArticleCharacteristicValueLang.Value('ProductImage2',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL2',-1)"
					+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail2',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImageDetail_URL2',-1)"
				);
			qp3.put("pageSize", "6000");
			java.util.Set<String> idsProductos = new java.util.TreeSet<>();
			java.util.Map<String, String> qp00 = new java.util.HashMap<>();
			qp00.put("fields", "Product2G.ProductNo,Product2G.SKU");
			qp00.put("pageSize", "6000");
			java.util.Map<String, String> skuToPn = new java.util.HashMap<>();
			int m = 0;
			StringBuilder sb0 = new StringBuilder();
			int n = 0;
			java.util.Set<String> losesos = new java.util.TreeSet<>();
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					losesos.add(line);
					n++;
					sb0.append(sb0.length() == 0 ? "" : ",").append(line.replace("SB", ""));
					m++;
					if(m%1000 == 0) {
						qp00.put("query", "Product2G.SKU in (" + sb0.toString() + ")");
						rw.collectData("list", "Product2G", null, "bySearch", qp00, row -> {
							org.json.JSONArray values = row.getJSONArray("values");
							idsProductos.add(values.getString(0));
							skuToPn.put( values.getString(1), values.getString(0) );
						});
						sb0.setLength(0);
						m = 0;
					}
				}
			}
			if(m > 0) {
				qp00.put("query", "Product2G.SKU in (" + sb0.toString() + ")");
				rw.collectData("list", "Product2G", null, "bySearch", qp00, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					idsProductos.add(values.getString(0));
					skuToPn.put( values.getString(1), values.getString(0) );
				});
				sb0.setLength(0);
				m = 0;
			}
			java.util.List<String> aids = new java.util.ArrayList<>();
			StringBuilder sb000000 = new StringBuilder();
			for( String elese : losesos ) {
				if(!skuToPn.containsKey(elese)) {
					sb000000.append( sb000000.length() == 0 ? "" : "," ).append(elese);
				}
			}
			java.util.Map<String, String> qp000 = new java.util.HashMap<>();
			qp000.put("fields", "Article.SupplierAID");
			qp000.put("pageSize", "6000");
			qp000.put("products", "");
			qp000.put("query", "Article.SKU in (" + sb000000.toString() + ")");
			rw.collectData("list", "Article", null, "bySearch", qp000, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				aids.add(values.getString(0));
			});
			sb000000.setLength(0);
			qp00 = new java.util.HashMap<>();
			qp00.put("fields", "ProductReference.ReferencedSupplierAid");
			qp00.put("pageSize", "6000");
			m = 0;
			sb0 = new StringBuilder();
			n = 0;
			for(String l0 : aids) {
				line = l0;
				n++;
				sb0.append(sb0.length() == 0 ? "" : ",").append("'").append(line).append("'@1");
				m++;
				if(m%1000 == 0) {
					qp00.put("items", sb0.toString());
					// https://172.18.251.2:1512/rest/V2.0/list/Article/ProductReference/byItems?items='S92848718'@1,'S92848713'@1&fields=Article.SupplierAID,Article.SKU,SimpleArticleCharacteristicValueLang.Value('SKU',-1)
					rw.collectData("list", "Article", "ProductReference", "byItems", qp00, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						idsProductos.add(values.getString(0));
					});
					sb0.setLength(0);
				}
			}
			if(m > 0) {
				qp00.put("items", sb0.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp00, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					idsProductos.add(values.getString(0));
				});
				sb0.setLength(0);
				m = 0;
			}
			idsProductos.forEach(System.out::println);
			System.exit(0);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
