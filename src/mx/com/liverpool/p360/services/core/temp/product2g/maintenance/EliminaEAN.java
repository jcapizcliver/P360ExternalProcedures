package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EliminaEAN {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<ids.length; i++) {
			sb.append(i == 0 ? "" : ",").append("'").append(ids[i]).append("'@1");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ids[i] + "'@1")).put("values", new org.json.JSONArray().put("Eliminada").put("").put("")));
		}
		rw.writeData("list", "Product2G", null, qp, request, System.out::println);
		qp.put("products", sb.toString());
		qp.put("pageSize", "10000");
		java.util.List<String> vids = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			vids.add(row.getJSONObject("object").getString("id"));
		});
		columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"));
		request.put("columns", columns);
		for(String vid : vids) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", vid)).put("values", new org.json.JSONArray().put("").put("")));
		}
		rw.writeData("list", "Article", null, qp, request, System.out::println);
	}
	
	private static final String[] ids = java.util.Arrays.asList(
			"1754611668479029",
            "1754611668479059",
            "1754611668479072",
            "1754611668479082",
            "1754611668479142",
            "1754611668479153",
            "1754611668479160",
            "1754611668479163",
            "1754611668479168",
            "1754611668479173",
            "1754611668479178",
            "1754611668479191",
            "1754611668479200",
            "1754611668479217",
            "1754611668479229",
            "1754611668479249",
            "1754611668479254",
            "1754611668479272",
            "1754611668479275",
            "1754611668479297",
            "1754611668479310",
            "1754611668479327",
            "1754611668479332",
            "1754611668479337",
            "1754611668479342",
            "1754611668479347",
            "1754611668479352",
            "1754611668479371",
            "1754611668479388",
            "1754611668479394",
            "1754611668479399",
            "1754611668479404",
            "1754611668479409",
            "1754611668479414",
            "1754611668479419",
            "1754611668479424",
            "1754611668479452",
            "1754611668479456",
            "1754611668479477",
            "1754611668479482",
            "1754611668479487",
            "1754611668479492",
            "1754611668479497",
            "1754611668479502",
            "1754611668479507",
            "1754611668479525",
            "1754611668479530",
            "1754611668479543",
            "1754611668479570",
            "1754611668479592",
            "1754611668479597",
            "1754611668479644",
            "1754611668479651",
            "1754611668479666",
            "1754611668479670",
            "1754611668479697",
            "1754611668479705",
            "1754611668479710",
            "1754611668479736",
            "1754611668479741",
            "1754611668479744",
            "1754611668479775",
            "1754611668479781",
            "1754611668479784",
            "1754611668479787",
            "1754611668479790",
            "1754611668479793",
            "1754611668479796",
            "1754611668479799",
            "1754611668479802",
            "1754611668479805",
            "1754611668479808",
            "1754611668479811",
            "1754611668479814",
            "1754611668479817",
            "1754611668479820",
            "1754611668479823",
            "1754611668479826",
            "1754611668479829",
            "1754611668479832",
            "1754611668479835",
            "1754611668479838",
            "1754611668479841",
            "1754611668479844",
            "1754611668479847",
            "1754611668479850",
            "1754611668479853",
            "1754611668479856",
            "1754611668479859",
            "1754611668479862",
            "1754611668479865",
            "1754611668479868",
            "1754611668479871",
            "1754611668479877",
            "1754611668479880",
            "1754611668479883",
            "1754611668479889",
            "1754611668479895",
            "1754611668479900",
            "1754611668479905",
            "1754611668479910",
            "1754611668479915",
            "1754611668479920",
            "1754611668479925",
            "1754611668479930",
            "1754611668479935",
            "1754611668479940",
            "1754611668479945",
            "1754611668479950",
            "1754611668479955",
            "1754611668479960",
            "1754611668479965",
            "1754611668479970",
            "1754611668479975",
            "1754611668479980",
            "1754611668479985",
            "1754611668479990",
            "1754611668480005",
            "1754611668480062",
            "1754611668480065",
            "1754611668480068",
            "1754611668480071",
            "1754611668480074",
            "1754611668480077",
            "1754611668480080",
            "1754611668480083",
            "1754611668480086",
            "1754611668480089",
            "1754611668480092",
            "1754611668480095",
            "1754611668480122",
            "1754611668480125",
            "1754611668480131",
            "1754611668480137",
            "1754611668480143",
            "1754611668480149",
            "1754611668480162",
            "1754611668480167",
            "1754611668480182",
            "1754611668480187",
            "1754611668480197",
            "1754611668480213",
            "1754611668480218",
            "1754611668480228",
            "1754611668480233"
			).toArray(new String[] {});
	
}
