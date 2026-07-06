package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CopyDataToArticle extends RESTWrapper{

	public static void main(String[] args) {
		CopyDataToArticle m = new CopyDataToArticle();
		m.moveData();
	}
	
	private void moveData() {
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('SB_0002',root,\"0000.0000.RK\",'SB_0002')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
			);
		qp.put("pageSize", "25000");
		java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
		qp1.put("fields", "ProductReference.ReferencedSupplierAid");
		qp1.put("pageSize", "20000");
		RequestHandler rh = new RequestHandler(
				new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('BrandName',root,\"0000.0000.RK\",'BrandName',-1)"))
					.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H',-1)"))
					.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ProductTypeSAP',root,\"0000.0000.RK\",'ProductTypeSAP',-1)"))
					.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SB_0002',root,\"0000.0000.RK\",'SB_0002',-1)"))
					.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ItemGroup',root,\"0000.0000.RK\",'ItemGroup',-1)"))
					.put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H',-1)"))
				, 1000, request ->{
			writeData( "list", "Article", null, qp0, request, System.out::println );
		});
		java.util.Map<String, org.json.JSONArray> variantsWithTheirParent = new java.util.TreeMap<>();
		
		collectData( "list", "Product2G", null, "withItem", qp, row ->{
			org.json.JSONArray values = row.getJSONArray("values");
			variantsWithTheirParent.put(values.getString(0), values);
				// IUO -> Manejan el inventario de la página, para que sea visible para BigTicket, necesita el registro en esa tabla y dos banderas, estatus y condición, 1 -> activo/inactivo, 2. descontinuado, ---. En el tiempo 1 no genera esos estatus, cuando
		} );
		
		collectData( "list", "Article", "ProductReference", "withProduct", qp1, row0->{
			org.json.JSONArray values0 = row0.getJSONArray("values");
			org.json.JSONArray values = variantsWithTheirParent.get(values0.getString(0));
			org.json.JSONObject rw = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", row0.getJSONObject("object").getString("id") )).put("values", 
					new org.json.JSONArray()
					.put("".equals( values.getJSONArray(1).getString(0) ) ? "" :  values.getJSONArray(1).getString(0) ) 
					.put("".equals( values.getJSONArray(2).getString(0) ) ? "" :  values.getJSONArray(2).getString(0) ) 
					.put("".equals( values.getJSONArray(3).getString(0) ) ? "" :  values.getJSONArray(3).getString(0) ) 
					.put("".equals( values.getJSONArray(4).getString(0) ) ? "" :  values.getJSONArray(4).getString(0) ) 
					.put("".equals( values.getJSONArray(5).getString(0) ) ? "" :  values.getJSONArray(5).getString(0) ) 
					.put("".equals( values.getJSONArray(6).getString(0) ) ? "" :  values.getJSONArray(6).getString(0) ) 
				);
			rh.addRow( rw );
		}, System.out::println, false );
		
		rh.sendData();
	}
	
}
