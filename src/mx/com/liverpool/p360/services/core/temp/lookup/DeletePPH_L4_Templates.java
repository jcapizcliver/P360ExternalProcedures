package mx.com.liverpool.p360.services.core.temp.lookup;

import java.io.IOException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class DeletePPH_L4_Templates {

	public static void main(String[] args) {
		try {
			collectData();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	private static void collectData() throws ServiceUnavailableException {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rwd = new RESTWorkshop();
		RESTWorkshop rw = new RESTWorkshop();
		String lookup = "PPH_L4_Templates";
		rwd.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rw.putParameter("lookup", lookup);
		rw.putParameter("fields", 
				  "LookupValue.Code"
//				+ ",LookupValueLang.Name(es)"
//				+ ",LookupValueReference.LookupValues(MATKLLOV)"
//				+ ",LookupValueReference.LookupValues(ZCOMALOV)"
//				+ ",LookupValueReference.LookupValues(PE000LOV)"
//				+ ",LookupValueReference.LookupValues(ItemGroupProductLOV)"
//				+ ",LookupValueReference.LookupValues(MATKLLOV_S4H)"
//				+ ",LookupValueReference.LookupValues(BRAND_IDLOV_S4H)"
//				+ ",LookupValueReference.LookupValues(ItemGroupConProductoSBBLOV)"
//				+ ",LookupValueReference.LookupValues(SB_0002LOV)"
			);
		rw.putParameter("pageSize", "3");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(MATKLLOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(ZCOMALOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(PE000LOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(ItemGroupProductLOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(MATKLLOV_S4H)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(BRAND_IDLOV_S4H)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(ItemGroupConProductoSBBLOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(SB_0002LOV)"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int a = 0;
		java.nio.file.Path paths = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "resp", "qa", "lookup");
		try {
			java.nio.file.Files.createDirectories(paths);
		} catch (IOException e) {
			e.printStackTrace();
		}
		paths = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "resp", "qa", "lookup", "pph_l4_templates.dat");
		int b = 0;
			do {
				rw.putParameter("startIndex", String.valueOf(a));
				response = rw.makeRequest("GET", "/list/LookupValue/byLookup");
				if(response != null) {
					b = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", rows.getJSONObject(i).getJSONObject("object").getString("id"))
								).put("values", 
									new org.json.JSONArray()
										.put(new org.json.JSONArray())
										.put(new org.json.JSONArray())
										.put(new org.json.JSONArray())
										.put(new org.json.JSONArray())
										.put(new org.json.JSONArray())
										.put(new org.json.JSONArray())
										.put(new org.json.JSONArray())
										.put(new org.json.JSONArray())
									)
							);
					}
					rwd.makeRequest("POST", "/list/LookupValue/", qp, request.toString());
					System.out.println(rwd.getRawResponse());
					while(rowsPayload.length() > 0) {
						rowsPayload.remove(0);
					}
					a += response.getInt("pageSize");
//					System.out.println(a);
//					rwd.putParameter("items", sb.toString());
//					rwd.makeRequest("DELETE", "/list/LookupValue/byItems");
//					System.out.println(rwd.getRawResponse());
//					sb.setLength(0);
				}else {
					System.out.println("ERROR: " + rw.getRawResponse());
				}
				System.out.println(a + "/" + b);
			}while(a < b);
			a = 0;
	}
	
}
