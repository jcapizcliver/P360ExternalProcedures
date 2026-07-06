package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class LoadPPHL4TemplateDataFromFileLVP {

	
	public static void main(String[] args) throws ServiceUnavailableException {
		
		RESTWorkshop rwq = new RESTWorkshop();
		RESTWorkshop rw  = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		String[] pieces = null;
		String[] prevPieces = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray valuesGpas = new org.json.JSONArray();
		org.json.JSONArray valuesProductos = new org.json.JSONArray();
		org.json.JSONArray valuesGpasProductos = new org.json.JSONArray();
		java.util.Set<String> gpas = new java.util.TreeSet<>();
		java.util.LinkedList<String> productos = new java.util.LinkedList<>();
		java.util.LinkedList<String> gpaProductos = new java.util.LinkedList<>();
		java.util.LinkedList<String[]> listOfPieces = new java.util.LinkedList<>();
		java.util.Map<String, String> matkllovData = getLkpValues("MATKLLOV");
		java.util.Map<String, String> pe000lovData = getLkpValues("PE000LOV");
		java.util.Map<String, String> itemgroupproductlovData = getLkpValues("ItemGroupProductLOV");
		rwq.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rwq.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
//		rwq.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(MATKLLOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(PE000LOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(ItemGroupProductLOV)"));
		request.put("columns", columns);
		request.put("rows", rows);
		
		try(
				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get("C:","opt","LVP","tmp","hola2_lvp.csv").toString() )))
//				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get("C:","opt","LVP","desorden","Libro de producto","LDP_julio_LVP - Sheet1.csv").toString() )))
		){
			String ln = null;
			ln = br.readLine();
			while((ln = br.readLine()) != null) {
				pieces = rw.parseLine(ln);
				if(!"Plantilla pendiente".equals(pieces[0]))
					listOfPieces.addLast(pieces);
				System.out.println(pieces[0]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		java.util.Collections.sort(listOfPieces, (o1,o2)-> o1[0].compareTo(o2[0]));
		for(String[] pcs : listOfPieces) {
			if( prevPieces != null && !prevPieces[0].equals(pcs[0]) ) {
				for(String gpa : gpas) {
					if(matkllovData.containsKey(gpa))
						valuesGpas.put(gpa);
				}
				for(String producto : productos) {
					if(pe000lovData.containsKey(producto))
						valuesProductos.put(producto);
				}
				for(String gpaProducto : gpaProductos) {
					if(itemgroupproductlovData.containsKey(gpaProducto))
						valuesGpasProductos.put(gpaProducto);
				}
				
				if(valuesGpas.length() > 0 || valuesProductos.length() > 0 || valuesGpasProductos.length() > 0) {
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevPieces[0] + "'@'PPH_L4_Templates'"))
							.put("values", new org.json.JSONArray()
									.put(true)
									.put(valuesGpas)
									.put(valuesProductos)
									.put(valuesGpasProductos)
								)
						);
					if("EU4-113567".equals(prevPieces[0])) {
						System.out.println(rows.get(rows.length() - 1));
					}
					if(rows.length() == 250) {
						rwq.makeRequest("POST", "/list/LookupValue", qp, request.toString());
						System.out.println(prevPieces[0] + " - " + rwq.getRawResponse());
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}else {
					System.out.println("No data here: " + prevPieces[0]);
				}
				
				valuesGpas = new org.json.JSONArray();
				valuesProductos = new org.json.JSONArray();
				valuesGpasProductos = new org.json.JSONArray();

				productos.clear();
				gpas.clear();
				gpaProductos.clear();
				
			}
			productos.addLast(pcs[2]);
			gpas.add(pcs[4]);
			gpaProductos.addLast(pcs[10]);
			prevPieces = pcs;
		}
		for(String gpa : gpas) {
			if(matkllovData.containsKey(gpa))
				valuesGpas.put(gpa);
		}
		for(String producto : productos) {
			if(pe000lovData.containsKey(producto))
				valuesProductos.put(producto);
		}
		for(String gpaProducto : gpaProductos) {
			if(itemgroupproductlovData.containsKey(gpaProducto))
				valuesGpasProductos.put(gpaProducto);
		}
		
		if(rows.length() > 0) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevPieces[0] + "'@'PPH_L4_Templates'"))
					.put("values", new org.json.JSONArray()
							.put(true)
							.put(valuesGpas)
							.put(valuesProductos)
							.put(valuesGpasProductos)
						)
					);
			rwq.makeRequest("POST", "/list/LookupValue", qp, request.toString());
			System.out.println(rwq.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
	private static java.util.Map<String, String> getLkpValues(String lkp) throws ServiceUnavailableException{
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		RESTWorkshop rw  = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360pro.liverpool.com.mx/rest/V2.0");
		rw.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
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
	
}
