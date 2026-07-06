package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RevisaEstadoDeProductosDesdeLista extends RESTWrapper {

	public static void main(String[] args) {
		RevisaEstadoDeProductosDesdeLista r = new RevisaEstadoDeProductosDesdeLista();
		r.collectArticleDetails();
//		r.collectDetails();
	}
	
	private void collectDetails() {
		StringBuilder sb = new StringBuilder();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Migración", "Flujos", "ordenados"))){
			lns.forEach(s -> {
				sb.append(sb.length() == 0 ? "'" : ",'").append( s ).append("'@1");
			});
		}catch(java.io.IOException e) {
		}
		System.out.println(sb.toString());
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "Product2G.ProductNo"
				+ ",Product2G.CurrentStatus"
				+ ",Product2G.StatusModification"
			);
		qp.put("items", sb.toString());
		qp.put("pageSize", "1000");
		collectData("list", "Product2G", null, "byItems", qp, row -> {
			if(row.getJSONArray("values").getString(2).contains("Aprobada")) {
				System.out.println(row.getJSONArray("values").getString(0));
			}
		});
	}
	
	private void collectArticleDetails() {
		StringBuilder sb = new StringBuilder();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Migración", "Flujos", "ordenadosSupplierAID"))){
			lns.forEach(s -> {
				sb.append(sb.length() == 0 ? "'" : ",'").append( s ).append("'@1");
			});
		}catch(java.io.IOException e) {
		}
		System.out.println(sb.toString());
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"Article.SupplierAID"
						+ ",Article.CurrentStatus"
						+ ",Article.StatusModification"
						+ ",SimpleArticleCharacteristicValueLang.Value('ProcedeNoProcede',-1)"
				);
		qp.put("items", sb.toString());
		qp.put("pageSize", "1200");
		collectData("list", "Article", null, "byItems", qp, row -> {
//			if(row.getJSONArray("values").getJSONArray(3).getString(0).contains("false")) {
//				System.out.println(row.getJSONArray("values").getString(0));
//			}
		});
	}
	
	
	
}
