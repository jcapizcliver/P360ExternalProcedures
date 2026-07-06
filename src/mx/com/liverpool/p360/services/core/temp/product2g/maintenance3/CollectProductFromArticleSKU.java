package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.CliTest;

public class CollectProductFromArticleSKU {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "Article.SKU in (5014523768,5014664795,5014484185,5014484215,5014484223,5015149053,5015149061,5015149151,5015120519,5015120501,5014783417,5014782861,5014777395,5015148910,5015148791,5013652505,5013658066,5013658201,5013658431,5014863470,5014779487,5014778570,5014450990,5014554205,5013438287,5014614241,5014509471,5015046785,5015046726,5014615123,5014615077,5014529634,5012346656,5012831341,5014629248,5014629299,5014305116,5014881168,5014751388,5014540123,5013996824,5014966325,5014883314,5014882571,5014528301,5015049806,5015050057,5015078270,5015089522,5013984460,5014010884,5014261828,5015205808,5015205905,5013262235,5015169291,5014939361,5013983901,5014458087,5014602137,5014012801,5014012852,5014226763,5014226780,5014226801,5014226810,5015255589,5014613821,5014645651,5014644867,5012646081,5014573242,5013749762,5013714217,5013653498,5013776689,5014616731,5014629728,5014013221,5014780132,5013658228)");
		qp.put("pageSize", "5000");
		java.util.List<String> aid = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			aid.add(row.getJSONObject("object").getString("id"));
		});
		StringBuilder sb = new StringBuilder();
		int a = 0;
		qp.clear();
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		java.util.Set<String> data = new java.util.TreeSet<>();
		for(String id : aid) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
			a++;
			if(a == 1000) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					data.add(values.getString(0));
				});
				a = 0;
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("items", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				data.add(values.getString(0));
			});
			a = 0;
			sb.setLength(0);
		}
		System.out.println("Found: " + data.size() + " datas.");
		a = 0;
		for(String d : data) {
			a++;
			System.out.println("Sending " + a + " data (" + d + ")");
			CliTest.enviaDataPropuesta(d);
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	public static void collectParentIDs(java.util.List<String> articleSKUs, java.util.Set<String> data) {
		StringBuilder sb = new StringBuilder();
		int a = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("pageSize", "5000");
		java.util.List<String> aid = new java.util.ArrayList<>();
		for(String asku : articleSKUs) {
			sb.append(sb.length() == 0 ? "" : ",").append(asku);
			a++;
			if(a == 1000) {
				qp.put("query", "Article.SKU in (" + sb.toString() + ")");
				rw.collectData("list", "Article", null, "bySearch", qp, row -> {
					aid.add(row.getJSONObject("object").getString("id"));
				});
				a = 0;
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("query", "Article.SKU in (" + sb.toString() + ")");
			rw.collectData("list", "Article", null, "bySearch", qp, row -> {
				aid.add(row.getJSONObject("object").getString("id"));
			});
			a = 0;
			sb.setLength(0);
		}
		qp.clear();
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		for(String id : aid) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
			a++;
			if(a == 1000) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					data.add(values.getString(0));
				});
				a = 0;
				sb.setLength(0);
			}
		}
		if(sb.length() > 0) {
			qp.put("items", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byItems", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				data.add(values.getString(0));
			});
			a = 0;
			sb.setLength(0);
		}
	}
	
}
