package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class UnalignVariantStatusCheck {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("http://172.18.237.162:1512/rest/V2.0");
		rw.getRw().addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.PrevStatus,Product2G.CurrentStatus,Product2G.ExternalStatus->LookupValue.Code");
		qp.put("pageSize", "20000");
		java.util.List<org.json.JSONArray> data = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "withItem", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			data.add(values);
		});
		qp.clear();
		qp.put("fields", "Article.PrevStatus,Article.CurrentStatus,Article.ExternalStatus->LookupValue.Code,Article.SupplierAID");
		qp.put("pageSize", "20000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", "ProductReference.ReferencedSupplierAid");
		qp0.put("pageSize", "20000");
		java.util.Map<String, String> eia = new java.util.TreeMap<>();
		java.util.Map<String, String> internalToExternal = new java.util.HashMap<>();
		java.util.Map<String, String[]> dict = new java.util.HashMap<>();
		StringBuilder sb = new StringBuilder();
		java.util.List<String> prevSameCurrent = new java.util.ArrayList<>();
		java.util.List<String> wrongPrev = new java.util.ArrayList<>();
		java.util.List<String> wrongCurrent = new java.util.ArrayList<>();
		java.util.List<String> wrongExternal = new java.util.ArrayList<>();
		int count = 0;
		for(org.json.JSONArray values : data) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'").append(values.getString(0)).append("'@1");
			dict.put(values.getString(0), new String[] { values.getString(1), values.getString(2), values.getString(3) });
			count++;
			if(count % 1000 == 0) {
				qp0.put("products", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp0, row -> internalToExternal.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0)));
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp, row -> {
					org.json.JSONArray values0 = row.getJSONArray("values");
					String[] rvls = dict.get( internalToExternal.get(row.getJSONObject("object").getString("id")) );
					if(values0.getString(0).equals(values0.getString(1)) && !"".equals(values0.getString(0))) {
						prevSameCurrent.add(values0.getString(3));
					}
					if(!values0.getString(0).equals(rvls[0])) {
						wrongPrev.add(values0.getString(3));
					}
					if(!values0.getString(1).equals(rvls[1])) {
						wrongCurrent.add(values0.getString(3));
					}
					if(!values0.getString(2).equals(rvls[2])) {
						wrongExternal.add(values0.getString(3));
					}
					eia.put(values0.getString(3), row.getJSONObject("object").getString("id"));
				});
				sb.setLength(0);
			}
		}
		if(count % 1000 != 0) {
			qp0.put("products", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byProducts", qp0, row -> internalToExternal.put(row.getJSONObject("object").getString("id"), row.getJSONArray("values").getString(0)));
			qp.put("products", sb.toString());
			rw.collectData("list", "Article", null, "byProducts", qp, row -> {
				org.json.JSONArray values0 = row.getJSONArray("values");
				String[] rvls = dict.get( internalToExternal.get(row.getJSONObject("object").getString("id")) );
				if(values0.getString(0).equals(values0.getString(1)) && !"".equals(values0.getString(0))) {
					prevSameCurrent.add(values0.getString(3));
				}
				if(!values0.getString(0).equals(rvls[0])) {
					wrongPrev.add(values0.getString(3));
				}
				if(!values0.getString(1).equals(rvls[1])) {
					wrongCurrent.add(values0.getString(3));
				}
				if(!values0.getString(2).equals(rvls[2])) {
					wrongExternal.add(values0.getString(3));
				}
				eia.put(values0.getString(3), row.getJSONObject("object").getString("id"));
			});
			sb.setLength(0);
		}
		System.out.println("Prevs:");
		wrongPrev.forEach(System.out::println);
		System.out.println("Currents:");
		wrongCurrent.forEach(System.out::println);
		System.out.println("Externals:");
		wrongExternal.forEach(System.out::println);
		System.out.println("Same prev as current:");
		prevSameCurrent.forEach(System.out::println);
		qp.clear();
		qp.put("includeObjectsInProtocol", "false");
		java.util.Set<String> st = new java.util.TreeSet<>();
		st.addAll(wrongPrev);
		st.addAll(wrongCurrent);
		st.addAll(wrongExternal);
		st.addAll(prevSameCurrent);
		RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.PrevStatus")).put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus")).put(new org.json.JSONObject().put("identifier", "Article.ExternalStatus")), 3000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println));
		String[] elements = st.toArray(new String[] {});
		for(int i=0; i<elements.length; i++) {
			String internal = eia.get( elements[i] );
			String[] ei = dict.get( internalToExternal.get( internal ) );
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", internal)).put("values", new org.json.JSONArray().put("".equals(ei[0]) ? "" : Integer.parseInt(ei[0])).put("".equals(ei[1]) ? "" : Integer.parseInt(ei[1])).put(ei[2])));
		}
		rh.sendData();
	}
	
}
