package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class PutBrandsToItemGroups {
	
	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
//		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		java.util.Map<String, java.util.Set<String>> brandsByGroupOfArticles = new java.util.TreeMap<>();
		java.util.Set<String> brandSet = null;
		try( 
				java.io.BufferedReader br =new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "hola2_lvp.csv").toString())))
//				java.io.BufferedReader br =new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "libro de producto mas proveedores y marcas.csv").toString())))
		){
			String ln = null;
			ln = br.readLine();
			String delim = "\"";
			String sep = "\t";
			String esc = "";
			String[] pieces = null;
			String itemGroup = null; // index 2
			String marcas = null; // index 12
			while((ln = br.readLine()) != null) {
				pieces = rw.parseLine(ln, delim, sep, esc);
				itemGroup = pieces[2];
				marcas = pieces[12];
				brandSet = brandsByGroupOfArticles.get(itemGroup);
				if(brandSet == null) {
					brandSet = new java.util.TreeSet<>();
					brandsByGroupOfArticles.put(itemGroup, brandSet);
				}
				processBrandArray(marcas, brandSet);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
//		brandsByGroupOfArticles.forEach((k,v)->System.out.println(k + " - " + v));
//		System.exit(0);
		org.json.JSONArray brands = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"));
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		GeneralOperations go = new GeneralOperations();
		java.util.Map<String ,String> dataZCOMA =  go.collectLookupValueData(rw, "ZCOMALOV");
		for(java.util.Map.Entry<String, java.util.Set<String>> entry : brandsByGroupOfArticles.entrySet()) {
			setToJSONArray(entry.getValue(), brands, dataZCOMA);
			System.out.println(entry.getKey() + " - " + brands);
			if(brands.length() > 0) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'MATKLLOV'")).put("values", new org.json.JSONArray()
					.put(brands)));
			}
			if(rows.length() == 400) {
				response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				if(response != null) {
						System.out.println(response.has("counters") ? response.getJSONObject("counters").getInt("objectsWithErrors") > 0 ? "There were errors, see: " + rw.getRawResponse() : response.get("counters") : response);
				}else {
					System.out.println("Error: " + rw.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
			brands = new org.json.JSONArray();
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/LookupValue", qp, request.toString());
			if(response != null) {
				System.out.println(response.has("counters") ? response.get("counters") : response);
			}else {
				System.out.println("Error: " + rw.getRawResponse());
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private static void setToJSONArray(java.util.Set<String> set, org.json.JSONArray arr, java.util.Map<String, String> ref) {
		for(String a : set) {
			if(ref.containsKey(a))
			arr.put(a);
		}
	}
	
	private static void processBrandArray(String brand, java.util.Set<String> brandSet) {
		String[] pcs = brand.split(",");
		String[] subCoins = null;
		for(int i=0; i<pcs.length; i++) {
			if(!pcs[i].contains("Sin acotación")) {
				if(pcs[i].contains("&")) {
					subCoins = pcs[i].split(" & ");
					for(int j=0; j<subCoins.length; j++) {
						brandSet.add(subCoins[j].trim());
					}
				}else {
					brandSet.add(pcs[i].trim());
				}
			}
		}
	}
	
	
}
