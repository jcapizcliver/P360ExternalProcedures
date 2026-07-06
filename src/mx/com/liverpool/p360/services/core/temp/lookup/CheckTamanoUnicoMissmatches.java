package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CheckTamanoUnicoMissmatches {

	
	public static void main(String[] args) throws ServiceUnavailableException {
		
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		rw.putParameter("lookup", "TamanoUnicoLOV");
		rw.putParameter("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		rw.putParameter("pageSize", "1200");
		response = rw.makeRequest("GET", "/list/LookupValue/byLookup");
		int c = 0;
		do {
			if(response != null) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!values.getString(0).equals(values.getString(1))) {
						System.out.println("-->" + values);
						c++;
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println(rw.getRawResponse());
			}
		}while(a < b);
		System.out.println(c);
	}
	
}
