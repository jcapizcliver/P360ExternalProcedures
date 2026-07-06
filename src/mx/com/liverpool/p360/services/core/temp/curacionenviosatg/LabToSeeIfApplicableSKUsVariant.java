package mx.com.liverpool.p360.services.core.temp.curacionenviosatg;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class LabToSeeIfApplicableSKUsVariant {

	private static final RESTWrapper rw = new RESTWrapper();

	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "No existen en ATG 30 Mayo.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUs variantes a analizar.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260521_184137 _ SKUs variantes nuevos respecto a anterior (20 contra 19 de Mayo).csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUs.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "EI.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260521_180701 _ SKUs variantes nuevos respecto a anterior (20 contra 19 de Mayo).csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260520_112511.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Variantes no existen en ATG_2.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Variantes no existen en ATG.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "UnosSKUsSinCatID_2.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "UnosSKUsSinCatID__1.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "skus para empuje con precio e inventario 170526___restante1.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "skus para empuje con precio e inventario 170526.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LabToCheckIfSendableSKUsArticle_LosDeLos60mil.yxy").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LabToCheckIfSendableSKUsArticle_OtraTanda.yxy").toFile())))){ // esto es lo de mkp
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "13DeMayo.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsVars__proc_16052026.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "Remanentes", "Remanentes.csv").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "No visibles", "Marketplace", "15052026", "SinProductoAsociado.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "No visibles", "Marketplace", "15052026", "SKUs no Existentes en ATG.txt").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LabToCheckIfSendableSKUsArticle_Track_10699.yxy").toFile())))){
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LabToCheckIfSendableSKUsArticle.yxy").toFile())))){
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
			qp.put("pageSize", "6000");
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
					+ ",Article.SKU"
					+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
					+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImageDetail_URL',-1)"
				);
			qp3.put("pageSize", "6000");
			java.util.Map<String, String[]> pid2Data = new java.util.HashMap<>();
			java.util.Map<String, String> artToProd = new java.util.HashMap<>();
			java.util.Map<String, String[]> artData = new java.util.HashMap<>();

			java.util.List<String> aids = new java.util.ArrayList<>();
			java.util.Map<String, String> qp11 = new java.util.HashMap<>();
			qp11.put("fields", "Article.SupplierAID,Article.SKU,SimpleArticleCharacteristicValueLang.Value('SKU',-1)");
			qp11.put("pageSize", "6000");
			java.util.Map<String, String> sku1ToAID = new java.util.HashMap<>();
			StringBuilder sb11 = new StringBuilder();
			int s = 0;
			java.util.Map<String, String> artIdToSKU = new java.util.HashMap<>();
			java.util.List<String> skus = new java.util.ArrayList<>();
			java.util.Map<String, Integer> freqs = new java.util.HashMap<>();
			java.util.Set<String> internalIDs = new java.util.TreeSet<>();
			java.util.Map<String, String> internalToSKU = new java.util.HashMap<>();
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					skus.add(line);
					sb11.append(sb11.length() == 0 ? "" : ",").append(line.replace("SB", ""));
					s++;
					if(s%1000 == 0) {
						// https://172.18.251.2:1512/rest/V2.0/list/Article/bySearch?query=Article.SKU in (978771,951426,951426)&fields=Article.SupplierAID,Article.SKU,SimpleArticleCharacteristicValueLang.Value('SKU',-1)
						qp11.put("query", "Article.SKU in (" + sb11.toString() + ")");
						rw.collectData("list", "Article", null, "bySearch", qp11, row -> {
							org.json.JSONArray values = row.getJSONArray("values");
							aids.add(values.getString(0));
							String sku1 = values.getString(1);
							Integer f = freqs.get(sku1);
							freqs.put(sku1, (f == null ? 0 : f) + 1);
							sku1ToAID.put(sku1, values.getString(0));
							artIdToSKU.put(values.getString(0), sku1);
							internalIDs.add(row.getJSONObject("object").getString("id"));
							internalToSKU.put(row.getJSONObject("object").getString("id"), sku1);
						});
						sb11.setLength(0);
					}
				}
			}
			if(sb11.length() > 0) {
				qp11.put("query", "Article.SKU in (" + sb11.toString() + ")");
				rw.collectData("list", "Article", null, "bySearch", qp11, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					aids.add(values.getString(0));
					String sku1 = values.getString(1);
					Integer f = freqs.get(sku1);
					freqs.put(sku1, (f == null ? 0 : f) + 1);
					sku1ToAID.put(sku1, values.getString(0));
					artIdToSKU.put(values.getString(0), sku1);
					internalIDs.add(row.getJSONObject("object").getString("id"));
				});
				s = 0;
				sb11.setLength(0);
			}
			System.out.println( "SKUs leidos: " + skus.size() + ", artículos encontrados: " + artIdToSKU.size() + " || " + sku1ToAID.size() );
			String[] laref = skus.toArray(new String[] {});
			java.util.Arrays.sort(laref);
			java.util.List<java.util.Map.Entry<String, Integer>> lst = new java.util.ArrayList<>( freqs.entrySet() );
			java.util.Collections.sort( lst, (o1,o2) -> o2.getValue().compareTo(o1.getValue()) );
//			lst.forEach( entry -> { if(entry.getValue() > 1) System.out.println(entry.getKey() + ";" + entry.getValue()); } );
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUsVariantesNoEncontrados.csv").toFile())))){
				for(String sku : skus) {
					if(!sku1ToAID.containsKey(sku)) {
						pw.println(sku);
					}
				}
			}
			
			int a = 0;
			java.util.Set<String> idsProductos = new java.util.TreeSet<>();
			java.util.Map<String, String> qp00 = new java.util.HashMap<>();
			qp00.put("fields", "ProductReference.ReferencedSupplierAid");
			qp00.put("pageSize", "6000");
			int m = 0;
			StringBuilder sb0 = new StringBuilder();
			int n = 0;
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
						artToProd.put(row.getJSONObject("object").getString("id"), values.getString(0));
					});
					sb0.setLength(0);
				}
			}
			if(m > 0) {
				qp00.put("items", sb0.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp00, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					idsProductos.add(values.getString(0));
					artToProd.put(row.getJSONObject("object").getString("id"), values.getString(0));
				});
				sb0.setLength(0);
				m = 0;
			}
			System.out.println("NaN");
			for( String unacala : artToProd.keySet() ) {
				if(!internalIDs.contains(unacala)) {
					System.out.println( internalToSKU.get(unacala) );
				}
			}
			System.out.println("/NaN");
			System.out.println("Leímos: " + n + " artículos. Fueron: " + idsProductos.size() + " productos. || " + artToProd.size());
			java.util.Set<String> esekaus = new java.util.TreeSet<>();
			for(String l : idsProductos) {
				line = l;
				ids.add(line);
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(line).append("'@1");
				a++;
				if(a == 1000) {
					qp.put("items", sb.toString());
					qp2.put("products", sb.toString());
					qp3.put("products", sb.toString());
					rw.collectData("list", "Article", null, "byProducts", qp3, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						if(java.util.Arrays.binarySearch(laref, values.getString(3)) > -1) {
							artData.put(row.getJSONObject("object").getString("id"), new String[] { values.getString(0), values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0), values.getString(3), values.getJSONArray(4).getString(0), values.getJSONArray(5).getString(0) });
							esekaus.add(values.getString(3));
						}
					});
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						pid2Data.put(values.getString(0), new String[] { values.getString(1).replace(".", ""), values.getString(2), values.getString(3), values.getString(4), values.getString(5), values.getString(6)});
					});
					a = 0;
					sb.setLength(0);
				}
			}
			if(a > 0) {
				qp.put("items", sb.toString());
				qp2.put("products", sb.toString());
				qp3.put("products", sb.toString());
				System.out.println(qp3);
				rw.collectData("list", "Article", null, "byProducts", qp3, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					if(java.util.Arrays.binarySearch(laref, values.getString(3)) > -1) {
						artData.put(row.getJSONObject("object").getString("id"), new String[] { values.getString(0), values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0), values.getString(3), values.getJSONArray(4).getString(0), values.getJSONArray(5).getString(0) });
						esekaus.add(values.getString(3));
					}
					System.out.println(values);
				});
				rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					pid2Data.put(values.getString(0), new String[] { values.getString(1), values.getString(2), values.getString(3), values.getString(4), values.getString(5), values.getString(6) });
				});
				a = 0;
				sb.setLength(0);
			}
			System.out.println( "Terminamos con " + artData.size() + " artículos. ( " + skus.size() + " ) || " + esekaus.size() );
			String pid = null;
			String[] pidData = null;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Data for those who are not in there From Lista de Variantes.csv").toFile())))){
				pw.println( rw.getRw().serializeChunk( new Object[] { "ProductIdentifier", "SKU", "ProductName", "Name_Characteristic", "VariantIdentifier", "VariantSKU", "ProductImageURL_Congelada", "ProductImageDetailURL_Congelada", "CatIDs", "CurrentStatus", "PrevStatus", "ProductImageURL_tubería", "ProductImageDetailURL_tubería", "Veredicto" } ) );
				for(java.util.Map.Entry<String, String[]> entry : artData.entrySet()) {
					pid = artToProd.get(entry.getKey());
					if(pid != null) {
						pidData = pid2Data.get(pid);
						if(pidData != null) {
							pw.println( rw.getRw().serializeChunk( veredicto( new String[] { 
									  pid
									, pidData[0]
									, pidData[1]
									, pidData[2]
									, entry.getValue()[0]
									, entry.getValue()[3]
									, entry.getValue()[1]
									, entry.getValue()[2]
									, pidData[3]
									, pidData[4]
									, pidData[5]
									, entry.getValue()[4]
									, entry.getValue()[5]

								}), "\"", ",", "\"" ));
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
	
	private static String[] veredicto(String[] data) {
		String[] veredicto = java.util.Arrays.copyOf(data, data.length + 1);
		veredicto[veredicto.length - 1] = "OK";
		if( "".equals(data[6]) ) {
			veredicto[veredicto.length - 1] = "Faltan Imágenes \"Congeladas\"";
		}
		if( "".equals(data[2]) && "".equals(data[3]) ) {
			veredicto[veredicto.length - 1] = "OK".equals(veredicto[veredicto.length - 1]) ? "Falta Nombre del Producto" : veredicto[veredicto.length - 1] + "|Falta Nombre del Producto";
		}
		if( "".equals(data[8]) ) {
			veredicto[veredicto.length - 1] = "OK".equals(veredicto[veredicto.length - 1]) ? "Falta CatID" : veredicto[veredicto.length - 1] + "|Falta CatID";
		}
		if( "".equals(data[11]) && "".equals(data[12]) ) {
			veredicto[veredicto.length - 1] = "OK".equals(veredicto[veredicto.length - 1]) ? "Sin imágenes de tubería" : veredicto[veredicto.length - 1] + "|Sin imágenes de tubería";
		}
		return veredicto;
	}
}
