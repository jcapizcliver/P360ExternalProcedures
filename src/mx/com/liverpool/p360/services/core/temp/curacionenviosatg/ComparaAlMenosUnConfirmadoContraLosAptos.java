package mx.com.liverpool.p360.services.core.temp.curacionenviosatg;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ComparaAlMenosUnConfirmadoContraLosAptos {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Set<String> confirmadosAlMenosEnEncoladoHaciaATG = new java.util.TreeSet<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "ConfirmadosAlMenosEnUnEncolado.csv").toFile())))){
			String line = br.readLine();
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					confirmadosAlMenosEnEncoladoHaciaATG.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "AptosParaPublicar.csv").toFile())))){
			java.util.Set<String> idsProductos = new java.util.TreeSet<>();
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "NuevosAceptables.csv").toFile())))){
				String line = br.readLine();
				int a = 0;
				java.util.List<String> internalIds = new java.util.ArrayList<>();
				java.util.Map<String, String> qp = new java.util.HashMap<>();
				qp.put("pageSize", "2000");
				while((line = br.readLine()) != null) {
					if(!"".equals(line)) {
						if(!confirmadosAlMenosEnEncoladoHaciaATG.contains(line)) {
							sb.append(sb.length() == 0 ? "" : ",").append(line);
							a++;
							if(a%1000 == 0) {
								qp.put("query", "Article.SKU in (" + sb.toString() + ")");
								rw.collectData("list", "Article", null, "bySearch", qp, row -> {
									internalIds.add(row.getJSONObject("object").getString("id"));
								});
								sb.setLength(0);
							}
							pw.println(line);
						}
					}
				}
				if(sb.length() > 0) {
					qp.put("query", "Article.SKU in (" + sb.toString() + ")");
					rw.collectData("list", "Article", null, "bySearch", qp, row -> {
						internalIds.add(row.getJSONObject("object").getString("id"));
					});
				}
				java.util.Map<String, String> qp00 = new java.util.HashMap<>();
				qp00.put("fields", "ProductReference.ReferencedSupplierAid");
				qp00.put("pageSize", "6000");
				int m = 0;
				StringBuilder sb0 = new StringBuilder();
				int n = 0;
				for(String l0 : internalIds) {
					line = l0;
					n++;
					sb0.append(sb0.length() == 0 ? "" : ",").append(line);
					m++;
					if(m%1000 == 0) {
						qp00.put("items", sb0.toString());
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
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Publicación", "NuevosChallengersPn.csv").toFile())))){
				idsProductos.forEach(pw::println);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
