package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class PonlesNegocioALosQueNoTienen {

	private static final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
	
	public static void main(String[] args) {
		PonlesNegocioALosQueNoTienen pn = new PonlesNegocioALosQueNoTienen();
		pn.recuperaCososSinBusiness();
	}
	
	private void recuperaCososSinBusiness() {
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rowsPayload);
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('Business',root,\"0000.0000.RK\",'Business',-1)"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('EXTWG_S4H',root,\"0000.0000.RK\",'EXTWG_S4H')->LookupValue.Code"
				+ ",Product2G.ProductNo"
			);
//		qp.put("query", "characteristic('Business') is empty");
		qp.put("pageSize", "20000");
		int a = 0;
		int b = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		String business = null;
		int c = 0;
		Object value = null;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "aLosQueLesPuseBusiness").toFile(), true), java.nio.charset.StandardCharsets.UTF_8))){
			do {
				qp.put("startIndex", String.valueOf(a));
				response = rw.makeRequest("GET", "/list/Product2G/byCatalog", qp, null);
				if(response != null && response.has("totalSize")) {
					b = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						values = rows.getJSONObject(i).getJSONArray("values");
						business = values.getJSONArray(0).getString(0);
						if("".equals(business)) {
							pw.println(values.getString(3));
							c++;
							value = determineBusiness( values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0) );
							if(value != null) {
								rowsPayload.put(new org.json.JSONObject().put("object", rows.getJSONObject(i).getJSONObject("object")).put("values", new org.json.JSONArray().put( new org.json.JSONArray().put( value ))));
								if(rowsPayload.length() == 10000) {
									rw.makeRequest("POST", "/list/Product2G/", empty, request.toString());
									while(rowsPayload.length() > 0) {
										rowsPayload.remove(0);
									}
								}
							}
						}
					}
					a += response.getInt("pageSize");
					System.out.println(a + "/" + b);
				}else {
					System.out.println(rw.getRawResponse());
				}
			}while(a < b);
			a = 0;
			System.out.println("Found " + c + " missing boys. " + rowsPayload);
			if(rowsPayload.length() > 0) {
				rw.makeRequest("POST", "/list/Product2G/", empty, request.toString());
				System.out.println(rw.getRawResponse());
				while(rowsPayload.length() > 0) {
					rowsPayload.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private Object determineBusiness(String negocio, String extwgS4h) {
		return "".equals(negocio) && "".equals(extwgS4h) ? null : new org.json.JSONObject().put("id", "'" + ("".equals(negocio) && !"".equals(extwgS4h) ? "SBB": "MARKETPLACE".equals(negocio) ? "MKP" : "LVP") + "'@'BusinessQualified'" );
	}
	
}
