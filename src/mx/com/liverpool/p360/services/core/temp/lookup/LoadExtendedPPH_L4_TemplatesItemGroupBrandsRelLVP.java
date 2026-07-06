package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class LoadExtendedPPH_L4_TemplatesItemGroupBrandsRelLVP {

	
	public static void main(String[] args) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.LinkedList<String[]> tuples = new java.util.LinkedList<>();
		try(
			java.io.BufferedReader br =new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "hola2_lvp.csv").toString()))) 
//			java.io.BufferedReader br =new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "libro de producto mas proveedores y marcas.csv").toString()))) 
		){
			String ln = null;
			ln = br.readLine();
			String delim = "\"";
			String sep = "\t";
			String esc = "";
			String[] pieces = null;
			String itemGroup = null; // index 2
			String itemGroupProduct = null; // index 7
			String templateId = null; // index 8
			String marcas = null; // index 12
			String business = null; // index 13
			String[] tuple = null;
			while((ln = br.readLine()) != null) {
				pieces = rw.parseLine(ln, delim, sep, esc);
				itemGroup = pieces[2];
				itemGroupProduct = pieces[7];
				templateId = pieces[8];
				marcas = pieces[12];
				business = pieces[13];
				if(business.contains("Liverpool")) {
					tuple = new String[5];
					tuple[0] = itemGroup;
					tuple[1] = templateId;
					tuple[2] = marcas;
					tuple[3] = business;
					tuple[4] = itemGroupProduct;
					tuples.addLast(tuple);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Collections.sort(tuples, (o1,o2) -> o1[1].compareTo(o2[1]) );
		String template = null;
		String prevTemplate = null;
		if(tuples.isEmpty()) {
			System.exit(0);
		}
//		tuples.forEach(arr -> System.out.println(java.util.Arrays.asList(arr)) );
//		System.exit(0);
		org.json.JSONArray itemGroups = new org.json.JSONArray();
		org.json.JSONArray itemGroupProducts = new org.json.JSONArray();
		org.json.JSONArray brands = new org.json.JSONArray();
		java.util.Set<String> itemGroupSet = new java.util.TreeSet<>();
		java.util.Set<String> itemGroupProductSet = new java.util.TreeSet<>();
		java.util.Set<String> brandSet = new java.util.TreeSet<>();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
//		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ItemGroupConProductoSBBLOV')"));
//		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BRAND_IDLOV_S4H')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ItemGroupProductLOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV')"));
		request.put("columns", columns);
		request.put("rows", rows);
		GeneralOperations go = new GeneralOperations();
//		java.util.Map<String ,String> dataZCOMA =  go.collectLookupValueData(rw, "BRAND_IDLOV_S4H");
		java.util.Map<String ,String> dataZCOMA =  go.collectLookupValueData(rw, "ZCOMALOV");
		java.util.Map<String ,String> dataMATKL =  go.collectLookupValueData(rw, "MATKLLOV");
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		java.util.Map<String, String> itemgroupproductlovData = getLkpValues(rw.getRc().getHeader().get("Authorization"), "ItemGroupConProductoSBBLOV");
		java.util.Map<String, String> itemgroupproductlovData = getLkpValues(rw.getRc().getHeader().get("Authorization"), "ItemGroupProductLOV");
		for(String[] tup : tuples) {
			template = tup[1];
			if(prevTemplate != null && !prevTemplate.equals(template)) {
				for(String a : itemGroupSet) {
					if(dataMATKL.containsKey(a))
						itemGroups.put(a);
				}
				for(String a : itemGroupProductSet) {
					if(itemgroupproductlovData.containsKey(a))
						itemGroupProducts.put(a);
				}
				for(String a : brandSet) {
					if(dataZCOMA.containsKey(a))
						brands.put(a);
				}
				if("EU4-2505136".equals(prevTemplate)) {
					System.out.print( prevTemplate + " - ");
					for(int m=0; m<itemGroups.length(); m++) {
						System.out.print(";" + itemGroups.getString(m));
					}
					System.out.println();
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevTemplate + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray()
							.put(itemGroupProducts)
							.put(brands)
							.put(itemGroups)
							)
							);
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
//				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevTemplate + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray()
//						.put(itemGroupProducts)
//						.put(brands)
//						.put(itemGroups)
//					)
//				);
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
				itemGroups = new org.json.JSONArray();
				itemGroupProducts = new org.json.JSONArray();
				brands = new org.json.JSONArray();
				itemGroupSet.clear();
				itemGroupProductSet.clear();
				brandSet.clear();
			}
			itemGroupSet.add(tup[0]);
			itemGroupProductSet.add(tup[4]);
			processBrandArray(tup[2], brandSet);
			prevTemplate = template;
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
	
	private static java.util.Map<String, String> getLkpValues(String authorization, String lkp) throws ServiceUnavailableException{
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		RESTWorkshop rw  = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", authorization);
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		rw.putParameter("lookup", "'" + lkp + "'");
		rw.putParameter("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		rw.putParameter("pageSize", "1200");
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/byLookup");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i = 0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values.getString(1));
				}
				currentIndex += response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		
		return data;
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
