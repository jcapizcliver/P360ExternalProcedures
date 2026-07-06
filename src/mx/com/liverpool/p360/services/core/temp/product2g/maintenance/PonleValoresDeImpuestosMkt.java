package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class PonleValoresDeImpuestosMkt extends RESTWrapper {

	
	public static void main(String[] args) {
		PonleValoresDeImpuestosMkt p = new PonleValoresDeImpuestosMkt();
		p.losMkt();
	}
	
	private void losMkt() {
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
/*
		RequestHandler rh = new RequestHandler(
				new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IndicadordeImpuesto',root,\"0000.0000.RK\",'IndicadordeImpuesto',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('IEPS',root,\"0000.0000.RK\",'IEPS',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ImpuestoALaVenta',root,\"0000.0000.RK\",'ImpuestoALaVenta',-1)"))
				, 1000, request -> writeData("list", "Product2G", null, qp0, request, System.out::println) );
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "characteristic('Business') equals 'MKP'@'BusinessQualified'");
		qp.put("pageSize", "25000");
		collectData("list", "Product2G", null, "bySearch", qp, row->{
			rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(new org.json.JSONArray().put("E2")).put(new org.json.JSONArray().put("0")).put(new org.json.JSONArray().put("1"))));
		}, System.out::println);
		rh.sendData();
*/
		RequestHandler rh = new RequestHandler(
				new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXKM1_S4H',root,\"0000.0000.RK\",'TAXKM1_S4H',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXKM2_S4H',root,\"0000.0000.RK\",'TAXKM2_S4H',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TAXM3_S4H',root,\"0000.0000.RK\",'TAXM3_S4H',-1)"))
				, 1000, request -> writeData("list", "Product2G", null, qp0, request, System.out::println) );
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "characteristic('Business') equals 'MKP'@'BusinessQualified'");
		qp.put("pageSize", "25000");
		collectData("list", "Product2G", null, "bySearch", qp, row->{
			rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("").put("").put("")));
		}, System.out::println);
		rh.sendData();
	}
	
}
