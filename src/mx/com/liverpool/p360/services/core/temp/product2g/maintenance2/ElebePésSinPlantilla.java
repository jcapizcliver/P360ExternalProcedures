package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ElebePésSinPlantilla {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "1000");
		qp.put("query", "Product2G.ProductNo startsWith \"LVP\" and Product2GStructureMap.StructureGroup is empty");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LVPSinPlantilla.txt").toFile())))){
			rw.collectData("list", "Product2G", null, "bySearch", qp, row ->{
				org.json.JSONArray values = row.getJSONArray("values");
				pw.println(values.getString(0));
			} );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}

