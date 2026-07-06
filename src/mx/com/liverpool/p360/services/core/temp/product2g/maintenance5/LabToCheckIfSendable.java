package mx.com.liverpool.p360.services.core.temp.product2g.maintenance5;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class LabToCheckIfSendable {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Export Only IDs (40).csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "CategoryConCategoría.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LabToCheckIfSendable.yxy").toFile())))){
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
			java.util.Map<String, String[]> pid2Data = new java.util.HashMap<>();
			java.util.Map<String, String> artToProd = new java.util.HashMap<>();
			java.util.Map<String, String[]> artData = new java.util.HashMap<>();
			int a = 0;
			while((line = br.readLine()) != null) {
				ids.add(line);
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(line).append("'@1");
				a++;
				if(a == 1000) {
					qp.put("items", sb.toString());
					qp2.put("products", sb.toString());
					qp3.put("products", sb.toString());
					rw.collectData("list", "Article", null, "byProducts", qp3, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						artData.put(row.getJSONObject("object").getString("id"), new String[] { values.getString(0), values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0) });
					});
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						System.out.println(values);
						System.out.println("-->" + values.get(3) + "<--");
						pid2Data.put(values.getString(0), new String[] { values.getString(1).replace(".", ""), values.getString(2), values.getString(3), values.getString(4), values.getString(5), values.getString(6) });
					});
					rw.collectData("list", "Article", "ProductReference", "byProducts", qp2, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						artToProd.put(row.getJSONObject("object").getString("id"), values.getString(0));
					});
					a = 0;
					sb.setLength(0);
				}
			}
			if(a > 0) {
				qp.put("items", sb.toString());
				qp2.put("products", sb.toString());
				qp3.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp3, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					artData.put(row.getJSONObject("object").getString("id"), new String[] { values.getString(0), values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0) });
				});
				rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					System.out.println(values);
					System.out.println("-->" + values.get(3) + "<--");
					pid2Data.put(values.getString(0), new String[] { values.getString(1), values.getString(2), values.getString(3), values.getString(4), values.getString(5), values.getString(6) });
				});
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp2, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					artToProd.put(row.getJSONObject("object").getString("id"), values.getString(0));
				});
				a = 0;
				sb.setLength(0);
			}
			String pid = null;
			String[] pidData = null;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Data for those who are not in there.csv").toFile())))){
				pw.println( rw.getRw().serializeChunk( new Object[] { "ProductIdentifier", "SKU", "ProductName", "Name_Characteristic", "VariantIdentifier", "ProductImageURL_Congelada", "ProductImageDetailURL_Congelada", "CatIDs", "CurrentStatus", "PrevStatus", "Veredicto" } ) );
				for(java.util.Map.Entry<String, String[]> entry : artData.entrySet()) {
					pid = artToProd.get(entry.getKey());
					if(pid != null) {
						pidData = pid2Data.get(pid);
						if(pidData != null) {
							pw.println( rw.getRw().serializeChunk( veredicto( new String[] { pid, pidData[0], pidData[1], pidData[2], entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], pidData[3], pidData[4], pidData[5] }), "\"", ",", "\"" ));
						}else {
							System.out.println("Ora pues, no data for pa: " + pid);
						}
					}else {
						System.out.println("Ora pues, este no tuvo apá: " + entry.getKey());
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String arrayToString(org.json.JSONArray values) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<values.length(); i++) {
			sb.append(i == 0 ? "" : ",").append(values.getString(i));
		}
		return sb.toString();
	}
	
	private static String[] veredicto(String[] data) {
		String[] veredicto = java.util.Arrays.copyOf(data, data.length + 1);
		veredicto[veredicto.length - 1] = "OK";
		if( "".equals(data[5])  ) {
			veredicto[veredicto.length - 1] = "Faltan Imágenes \"Congeladas\"";
		}
		if( "".equals(data[2]) && "".equals(data[3]) ) {
			veredicto[veredicto.length - 1] = "OK".equals(veredicto[veredicto.length - 1]) ? "Falta Nombre del Producto" : veredicto[veredicto.length - 1] + "|Falta Nombre del Producto";
		}
		if( "".equals(data[7]) ) {
			veredicto[veredicto.length - 1] = "OK".equals(veredicto[veredicto.length - 1]) ? "Falta CatID" : veredicto[veredicto.length - 1] + "|Falta CatID";
		}
		return veredicto;
	}
}
