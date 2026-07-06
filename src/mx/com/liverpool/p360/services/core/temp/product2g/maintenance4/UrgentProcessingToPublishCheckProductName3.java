package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.product2g.maintenance4.ProductNameResolver.ResolvedName;

public class UrgentProcessingToPublishCheckProductName3 {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {

		java.util.List<String> ids = new java.util.ArrayList<>();
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDs.txt").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				ids.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Read.");
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "5000");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GLang.ProductName(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('Name',-1)"
				+ ",Product2GExtraData.BrandName(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.BRAND_ID_S4H(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.SupplierID(MX)->LookupValueLang.Name(es)"
				+ ",Product2GExtraData.SupplierPartNumber(MX)"
			);
		
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", 
				   "Article.SupplierAID"
				+ ",Article.SKU"
				+ ",Article.EAN"
				+ ",ArticleExtraData.ColoursLiverpoolAtt(MX)->LookupValueLang.Name(es)"
				+ ",ArticleExtraData.TamanoUnico(MX)->LookupValueLang.Name(es)"
			);
		qp0.put("pageSize", "2000");
		java.util.Map<String, String> qp1 = new java.util.HashMap<>();
		qp1.put("fields", "ProductReference.ReferencedSupplierAid");
		qp1.put("pageSize", "2000");
		java.util.Map<String, org.json.JSONArray> productData = new java.util.HashMap<>();
		java.util.Map<String, org.json.JSONArray> articleData = new java.util.HashMap<>();
		java.util.Map<String, String> articleProduct = new java.util.HashMap<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "TO_APPLY_NAME_as_ProductName.csv").toFile())))){
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i<ids.size(); i++) {
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(ids.get(i)).append("'@1");
				if( (i+1) % 1000 == 0 ) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						String cpn = values.getString(1);
						String cn = values.getJSONArray(2).getString(0);
						String name = null;
						ResolvedName rn = ProductNameResolver.resolve(cpn, cn);
						name = rn.value();
						values.put(1, name);
						productData.put(values.getString(0), values);
					});
					qp0.put("products", sb.toString());
					rw.collectData("list", "Article", null, "byProducts", qp0, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						articleData.put(row.getJSONObject("object").getString("id"), values);
					});
					qp1.put("products", sb.toString());
					rw.collectData("list", "Article", "ProductReference", "byProducts", qp1, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						articleProduct.put(row.getJSONObject("object").getString("id"), values.getString(0));
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					String cpn = values.getString(1);
					String cn = values.getJSONArray(2).getString(0);
					String name = null;
					ResolvedName rn = ProductNameResolver.resolve(cpn, cn);
					name = rn.value();
					values.put(1, name);
					productData.put(values.getString(0), values);
				});
				qp0.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp0, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					articleData.put(row.getJSONObject("object").getString("id"), values);
				});
				qp1.put("products", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp1, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					articleProduct.put(row.getJSONObject("object").getString("id"), values.getString(0));
				});
				sb.setLength(0);
			}
			org.json.JSONArray prodValues = null;
			org.json.JSONArray artValues = null;
			org.json.JSONArray nv = null;
			for( java.util.Map.Entry<String, String> entry : articleProduct.entrySet() ) {
				artValues = articleData.get(entry.getKey());
				prodValues = productData.get(entry.getValue());
				if(artValues != null && prodValues != null ) {
					nv = new org.json.JSONArray();
					for(int i=0; i<prodValues.length(); i++) {
						nv.put(prodValues.get(i));
					}
					for(int i=0; i<artValues.length(); i++) {
						nv.put(artValues.get(i));
					}
					pw.println( rw.getRw().serializeChunk( toArray(nv) ));
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static String[] toArray(org.json.JSONArray values) {
		java.util.List<String> vls = new java.util.ArrayList<>();
		for(int i=0; i<values.length(); i++) {
			vls.add( values.get(i) instanceof org.json.JSONArray ? values.getJSONArray(i).getString(0) : values.getString(i));
		}
		return vls.toArray(new String[] {});
	}
	
}
