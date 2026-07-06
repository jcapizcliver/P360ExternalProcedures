package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.xml.local.LoadProductDataSecondOpinionFTW;

public class PasameLosCargaDeImagenMigrados {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "10000");
		java.util.List<String> withItems = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "withItem", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			withItems.add(values.getString(0));
		});
		qp.put("query", "Product2G.CurrentStatus = \"Carga de Imagen\" and (Product2G.ProductNo startsWith \"S\" or not Product2G.ProductNo wildcard \"________________\")");
		String[] productNos = withItems.toArray(new String[] {});
		java.util.Arrays.sort(productNos);
		try(java.io.PrintWriter pw  = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("ProductNo.txt").toFile())))){
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				if(java.util.Arrays.binarySearch(productNos, values.getString(0)) > -1) {
					pw.println(values.getString(0));
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try {
			LoadProductDataSecondOpinionFTW.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
