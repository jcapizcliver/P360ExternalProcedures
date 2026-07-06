package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class LoadPPHL4TemplateDataFromFileSBB {

	
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
		java.util.Map<String, String> matkllovs4hData = getLkpValues("MATKLLOV_S4H");
		java.util.Map<String, String> sb0002lovData = getLkpValues("SB_0002LOV");
		java.util.Map<String, String> itemgroupproductosbblovData = getLkpValues("ItemGroupConProductoSBBLOV");
		rwq.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(MATKLLOV_S4H)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(SB_0002LOV)"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(ItemGroupConProductoSBBLOV)"));
		request.put("columns", columns);
		request.put("rows", rows);
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( java.nio.file.Paths.get("C:","opt","LVP","desorden","Libro de producto","Libro S4H Julio - Libro.csv").toString() )))){
			String ln = null;
			ln = br.readLine();
			while((ln = br.readLine()) != null) {
				pieces = rw.parseLine(ln);
				listOfPieces.addLast(pieces);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		java.util.Collections.sort(listOfPieces, (o1,o2)-> o1[0].compareTo(o2[0]));
		for(String[] pcs : listOfPieces) {
			if( prevPieces != null && !prevPieces[0].equals(pcs[0]) ) {
				for(String gpa : gpas) {
					if(matkllovs4hData.containsKey(gpa))
						valuesGpas.put(gpa);
				}
				for(String producto : productos) {
					if(sb0002lovData.containsKey(producto))
						valuesProductos.put(producto);
				}
				for(String gpaProducto : gpaProductos) {
					if(itemgroupproductosbblovData.containsKey(gpaProducto))
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
					if(rows.length() == 5) {
						rwq.makeRequest("POST", "/list/LookupValue", qp, request.toString());
						System.out.println(rwq.getRawResponse());
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
			}
			productos.addLast(pcs[2]);
			gpas.add(pcs[4]);
			gpaProductos.addLast(pcs[10]);
			prevPieces = pcs;
		}
		for(String gpa : gpas) {
			valuesGpas.put(gpa);
		}
		for(String producto : productos) {
			valuesProductos.put(producto);
		}
		for(String gpaProducto : gpaProductos) {
			valuesGpasProductos.put(gpaProducto);
		}
		
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
	
	private static java.util.Map<String, String> getLkpValues(String lkp) throws ServiceUnavailableException{
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		RESTWorkshop rw  = new RESTWorkshop();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
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
