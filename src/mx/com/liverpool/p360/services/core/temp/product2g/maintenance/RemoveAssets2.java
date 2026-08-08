package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.EliminaImagenesDeVariantes;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RemoveAssets2 {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		final java.util.Map<String, String> qp = new java.util.HashMap<>();
//		qp.put("query", "Product2G.ProductNo startsWith \"175461166\" and Product2G.CurrentStatus = \"Aprobada\" and characteristic('Business') = 'MKP'@'BusinessQualified'");
		qp.put("query", "Product2G.ProductNo IN (\"1754611668465937\",\"1754611668466068\",\"1754611668471364\",\"1754611668471358\",\"1754611668471303\",\"1754611668471298\",\"1754611668471243\",\"1754611668465997\",\"1754611668467719\",\"1754611668465947\",\"1754611668467654\",\"1754611668470802\",\"1754611668471238\")'");
		qp.put("pageSize", "10000");
		java.util.List<String> ids = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> ids.add(row.getJSONObject("object").getString("id")));
		StringBuilder sb = new StringBuilder();
		for(String id : ids) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
		}
		qp.remove("query");
		qp.put("products", sb.toString());
		sb.setLength(0);
		java.util.List<String> idsV = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "byProducts", qp, row -> idsV.add(row.getJSONObject("object").getString("id")));
		for(String id : idsV) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
		}
		EliminaImagenesDeVariantes eliminator = new EliminaImagenesDeVariantes();
		eliminator.deleteAssets2(sb.toString());
		sb.setLength(0);
	}
	
}
