package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CopyLookupValuesRelations {

	
	public static void main(String[] args) {
//		copyLookupValues("ItemGroupProductLOV");
//		copyLookupValues("ItemGroupConProductoSBBLOV");
//		copyLookupValues("MATKLLOV");
//		copyLookupValues("MATKLLOV_S4H");
//		copyLookupValues("ZCOMALOV");
//		copyLookupValues("BRAND_IDLOV_S4H");
//		copyLookupValues("Party");
//		System.exit(0);
		RESTWrapper rwp = new RESTWrapper();
		RESTWrapper rwq = new RESTWrapper();
		rwq.getRw().setBaseUrl("https://gcpcatqap01.liverpool.com.mx:1512/rest/V2.0");
		rwq.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
//		String lookup = "Party";
		String lookup = "MATKLLOV";
		java.util.Map<String, String> qpp = new java.util.HashMap<>();
		qpp.put("lookup", "'" + lookup + "'");
		qpp.put("fields", 
				   "LookupValue.Code"
				+ ",LookupValueReference.LookupValues('ItemGroupProductLOV')->LookupValue.Code"
				+ ",LookupValueReference.LookupValues('ItemGroupConProductoSBBLOV')->LookupValue.Code"
				+ ",LookupValueReference.LookupValues('MATKLLOV')->LookupValue.Code"
				+ ",LookupValueReference.LookupValues('MATKLLOV_S4H')->LookupValue.Code"
				+ ",LookupValueReference.LookupValues('ZCOMALOV')->LookupValue.Code"
				+ ",LookupValueReference.LookupValues('BRAND_IDLOV_S4H')->LookupValue.Code"
			);
		qpp.put("pageSize", "20000");
		RequestHandler rh = new RequestHandler( 
			new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ItemGroupProductLOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ItemGroupConProductoSBBLOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV_S4H')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BRAND_IDLOV_S4H')"))
			, 5000
			, request -> { 
				rwq.writeData("list", "LookupValue", null, qpp, request, System.out::println);
			} );
		rwp.collectData("list", "LookupValue", null, "byLookup", qpp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rh.addRow( new org.json.JSONObject()
				.put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'" + lookup + "'"))
				.put("values", new org.json.JSONArray()
					.put( cureArray( values.getJSONArray(1) ))
					.put( cureArray( values.getJSONArray(2) ))
					.put( cureArray( values.getJSONArray(3) ))
					.put( cureArray( values.getJSONArray(4) ))
					.put( cureArray( values.getJSONArray(5) ))
					.put( cureArray( values.getJSONArray(6) ))
				) );
		});
		rh.sendData();
	}
	
	public static org.json.JSONArray cureArray(org.json.JSONArray array){
		return array != null && array.length() == 1 && "".equals(array.getString(0)) ? new org.json.JSONArray() : array;
	}
	
	public static void copyLookupValues(String lookup) {

		RESTWrapper rwp = new RESTWrapper();
		RESTWrapper rwq = new RESTWrapper();
		rwq.getRw().setBaseUrl("https://gcpcatqap01.liverpool.com.mx:1512/rest/V2.0");
		rwq.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qpp = new java.util.HashMap<>();
		qpp.put("lookup", "'" + lookup + "'");
		qpp.put("fields", 
				   "LookupValue.Code"
				+ ",LookupValueLang.Name(es)"
			);
		qpp.put("pageSize", "5000");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
				.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
			, 5000, request -> rwq.writeData("list", "LookupValue", null, qpp, request, System.out::println) );
		rwp.collectData("list", "LookupValue", null, "byLookup", qpp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rh.addRow( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'" + lookup + "'")).put("values", new org.json.JSONArray()
					.put(values.getString(1))
					.put(true)
				) );
		});
		rh.sendData();

	
	}
	
}
