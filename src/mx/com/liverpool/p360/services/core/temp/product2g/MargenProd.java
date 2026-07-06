package mx.com.liverpool.p360.services.core.temp.product2g;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class MargenProd {

	private static final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
	private static final String CHAR_ID = "MargenS4H";
	private static final java.nio.file.Path workingFilePath = java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "respaldo", CHAR_ID, "prod_productos_" + CHAR_ID);
	
	public static void main(String[] args) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		MargenProd mp = new MargenProd();
//		mp.collectMargen();
//		mp.updateMargen();
//		mp.restoreMargen();
	}
	
	private void restoreMargen() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('" + CHAR_ID + "',root,\"0000.0000.RK\",'" + CHAR_ID + "',-1)"));
		org.json.JSONArray values = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(workingFilePath.toString())))){
			String line = null;
			while((line = br.readLine()) != null) {
				values = new org.json.JSONArray(line);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@1")).put("values", new org.json.JSONArray().put(values.getJSONArray(2))));
				if(rows.length() == 500) {
					rw.makeRequest("POST", "/list/Product2G", qp, request.toString());
					System.out.println(rw.getRawResponse());
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(rows.length() > 0) {
			rw.makeRequest("POST", "/list/Product2G", qp, request.toString());
			System.out.println(rw.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private void updateMargen() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('" + CHAR_ID + "',root,\"0000.0000.RK\",'" + CHAR_ID + "',-1)"));
		org.json.JSONArray values = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(workingFilePath.toString())))){
			String line = null;
			while((line = br.readLine()) != null) {
				values = new org.json.JSONArray(line);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@1")).put("values", new org.json.JSONArray().put(new org.json.JSONArray())));
				if(rows.length() == 100) {
					rw.makeRequest("POST", "/list/Product2G", qp, request.toString());
					System.out.println(rw.getRawResponse());
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(rows.length() > 0) {
			rw.makeRequest("POST", "/list/Product2G", qp, request.toString());
			System.out.println(rw.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private void collectMargen() throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code,Product2GCharacteristicValueLang.Value('" + CHAR_ID + "',root,\"0000.0000.RK\",'" + CHAR_ID + "',-1)");
		qp.put("query", "not characteristic('" + CHAR_ID + "',-1) is empty");
		qp.put("pageSize", "1250");
		int a = 0;
		int b = 0;
		try {
			java.nio.file.Files.createDirectories(java.nio.file.Paths.get("C:","opt", "LVP", "desorden", "respaldo", CHAR_ID));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(workingFilePath.toString())))){
			do {
				qp.put("startIndex", String.valueOf(a));
				response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
				if(response != null && response.has("totalSize")) {
					b = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						values = rows.getJSONObject(i).getJSONArray("values");
						pw.println(values);
					}
					a += response.getInt("pageSize");
				}else {
					System.out.println("ERROR: " + rw.getRawResponse());
				}
			}while(a < b);
			a = 0;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
