package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.product2g.maintenance4.ProductNameResolver.ResolvedName;

public class UrgentProcessingToPublishPonNameEnProductName {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Product2GLang.Remarks(en) = \"" + UrgentProcessingToPublish.URGENT + "\"");
		qp.put("pageSize", "5000");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GLang.ProductName(es)"
				+ ",Product2GLang.DescriptionShort(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('ProductName',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('Name',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Negocio')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValue.Code"
			    + ",SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('SB_0002')->LookupValueLang.Name(es)"
				);
		
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "Product2GLang.ProductName(es)"))
				.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
				, 2000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		int[] non = new int[] {0};
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String cpn = values.getString(1);
			String cn = values.getJSONArray(4).getString(0);
			String name = null;
			ResolvedName rn = ProductNameResolver.resolve(cpn, cn);
			name = rn.value();
			if(name != null) {
				rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(name).put("Category")));
			}else {
				non[0]++;
			}
		});
		rh.sendData();
		System.out.println("Estos fueron non: " + non[0]);
		
	}
	
}
