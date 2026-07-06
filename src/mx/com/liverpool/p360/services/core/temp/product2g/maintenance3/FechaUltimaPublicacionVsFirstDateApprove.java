package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class FechaUltimaPublicacionVsFirstDateApprove {

	
	private static final RESTWrapper rw = new RESTWrapper();
	/*
	 *	 Elabora rutina donde: Product2G.StatusChange contenga Aprobado y Category, que además no tenga Product2G.FirstDateApproved, luego lees los archivos de /u01/workshop/stage/ToATG, toma la estampa de tiempo del archivo y con ese ponle el dato (pero debes ordenar de menor a mayor y quedarte con el primero)
	 * 
	 ********/
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,SimpleProduct2GCharacteristicValueLang.Value('FechaUltimaPublicacion',-1)");
		qp.put("query", "Product2G.FirstDateApproved is empty and not characteristic('FechaUltimaPublicacion',-1) is empty and Product2GExtraData.SAPObjectType(MX)->LookupValue.Code = \"00\" and Product2GLog.CreationDate(PIM) > 2026-01-31T00:00:00");
		qp.put("pageSize", "2000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.FirstDateApproved")) , 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println));
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
//			rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(values.getJSONArray(1).getString(0))));
		});
		rh.sendData();
	}
	
}
