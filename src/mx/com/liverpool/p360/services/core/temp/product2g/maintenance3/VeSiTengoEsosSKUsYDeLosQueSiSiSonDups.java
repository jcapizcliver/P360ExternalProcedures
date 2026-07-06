package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class VeSiTengoEsosSKUsYDeLosQueSiSiSonDups {

	
	/*
	 * Esta clase trabaja con SKUs a nivel artículo.
	 * 
	 * Sirve para determinar qué SKUs quitar, y también determina los IDs que no sobreviven,
	 * pero falta agregar una lógica de eliminación de los productos que están "de más".
	 * 
	 **********************************************************/
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "PROD", "Source1").toFile())))){
			String line = null;
			StringBuilder sb = new StringBuilder();
			int a = 0;
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("fields", "Article.SupplierAID,Article.SKU");
			qp.put("pageSize", "1000");
			java.util.Map<String, java.util.List<String>> data = new java.util.HashMap<>();
			java.util.List<String> skus = new java.util.ArrayList<>();
			while((line = br.readLine()) != null) {
				skus.add(line);
				sb.append(sb.length() > 0 ? ",":"").append(line);
				a++;
				if(a % 1000 == 0) {
					qp.put("query", "Article.SKU in (" + sb.toString() + ")");
					rw.collectData("list", "Article", null, "bySearch", qp, row -> {
						org.json.JSONArray values = row.getJSONArray("values");
						java.util.List<String> refs = data.get(values.getString(1));
						if(refs == null) {
							refs = new java.util.ArrayList<>();
							data.put(values.getString(1), refs);
						}
						refs.add(values.getString(0));
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("query", "Article.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Article", null, "bySearch", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					java.util.List<String> refs = data.get(values.getString(1));
					if(refs == null) {
						refs = new java.util.ArrayList<>();
						data.put(values.getString(1), refs);
					}
					refs.add(values.getString(0));
				});
				sb.setLength(0);
			}
			java.util.List<String> rugals = null;
			String sap = null;
			String p360 = null;
			String step = null;
			String toDelete = null;
			java.util.List<String> toDeleteList = new java.util.ArrayList<>();
			java.util.List<String> losEse = new java.util.ArrayList<>();
			java.util.List<String> losDiesiseis = new java.util.ArrayList<>();
			java.util.List<String> losSAP = new java.util.ArrayList<>();
			for(String sku : skus) {
				rugals = data.get(sku);
				if(rugals == null) {
					
				}else if(rugals.size() > 1) {
					if(rugals.size() == 2) {
						sap = rugals.get(0).startsWith("LVP") || rugals.get(0).startsWith("SBB") ? rugals.get(0) : rugals.get(1).startsWith("LVP") || rugals.get(1).startsWith("SBB") ? rugals.get(1) : null ;
						p360 = rugals.get(0).length() == 16 ? rugals.get(0) : rugals.get(1).length() == 16 ? rugals.get(1) : null;
						if(sap == null && p360 != null) {
							step = p360.equals(rugals.get(0)) ? rugals.get(1) : rugals.get(0);
						}else if(sap != null && p360 == null) {
							step = sap.equals( rugals.get(0)) ? rugals.get(1) : rugals.get(0);
						}
						if( sap != null && step != null ) {
							toDelete = sap;
						}else if(step != null && p360 != null) {
							toDelete = step;
						}else {
							System.out.println("You check this: SAP: " + sap + ", STEP: " + step + ", P360: " + p360 + ", 0: " + rugals.get(0) + ", 1: " + rugals.get(1));
						}
						if(toDelete != null) {
							toDeleteList.add(toDelete);
						}
						sap = null;
						step = null;
						p360 = null;
						toDelete = null;
					}else {
						System.out.println("----");
						rugals.forEach(System.out::println);
						for(String r : rugals) {
							if(r.startsWith("LVP") || r.startsWith("SBB")) {
								losSAP.add(r);
							}else if(r.length() == 16) {
								losDiesiseis.add(r);
							}else {
								losEse.add(r);
							}
						}
						if(!losDiesiseis.isEmpty()) {
							toDeleteList.addAll(losSAP);
							toDeleteList.addAll(losEse);
						}else if(!losEse.isEmpty()) {
							toDeleteList.addAll(losSAP);
						}
						losDiesiseis.clear();
						losSAP.clear();
						losEse.clear();
					}
				}
			}
			System.out.println("To delete");
			toDeleteList.forEach(System.out::println);
			System.out.println("***");
			sb.setLength(0);
			a = 0;
			qp.clear();
			qp.put("fields", "ProductReference.ReferencedSupplierAid");
			qp.put("pageSize", "5000");
			java.util.Map<String, String> artToProd = new java.util.HashMap<>();
			for(String td : toDeleteList) {
				sb.append( sb.length() == 0 ? "" : ",").append("'").append(td).append("'@1");
				a++;
				if(a % 1000 == 0) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
						artToProd.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
					artToProd.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0));
				});
				sb.setLength(0);
			}
			java.util.Set<String> prodsToDelete = new java.util.TreeSet<>( artToProd.values() );
			java.util.Map<String, String> qp0 = new java.util.HashMap<>();
			qp0.put("includeObjectsInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")) , 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
			for(String td : prodsToDelete) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + td + "'@1")).put("values", new org.json.JSONArray().put("Eliminada")));
			}
			rh.sendData();
			rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.SKU")) , 1000, request -> rw.writeData("list", "Article", null, qp0, request, System.out::println) );
			for(String td : toDeleteList) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + td + "'@1")).put("values", new org.json.JSONArray().put("")));
			}
			rh.sendData();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
