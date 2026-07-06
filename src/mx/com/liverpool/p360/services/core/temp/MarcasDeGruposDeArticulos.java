package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class MarcasDeGruposDeArticulos {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		String delim = "\"";
		String sep = "\t";
		String esc = "\\";
		String[] pieces = null;
		String mailov;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject row = new org.json.JSONObject();
		org.json.JSONArray values = new org.json.JSONArray();
		org.json.JSONObject object = new org.json.JSONObject();
		row.put("object", object);
		row.put("values", new org.json.JSONArray().put( values ));
		rows.put(row);
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"));
		request.put("columns", columns);
		request.put("rows", rows);

		java.util.Map<String, java.util.Set<String>> marcasDeGruposDeArticulo = new java.util.TreeMap<>();
		java.util.Set<String> marcas = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación grupo+Marca SAP QA LV.tsv")))){
			String line = null;
			System.out.println(java.util.Arrays.asList(workshop.parseLine(br.readLine(), delim, sep, esc)));
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				marcas = marcasDeGruposDeArticulo.get(pieces[1]);
				if(marcas == null) {
					marcas = new java.util.TreeSet<>();
					marcasDeGruposDeArticulo.put(pieces[1], marcas);
				}
				marcas.add(pieces[2]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Elepa: " + marcasDeGruposDeArticulo.size());
		for(java.util.Map.Entry<String, java.util.Set<String>> entry : marcasDeGruposDeArticulo.entrySet() ) {
			marcas = entry.getValue();
			for(String marca : marcas) {
				values.put(marca);
			}
			object.put("id", "'" + entry.getKey() + "'@'MATKLLOV'");
			response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
			System.out.println(response == null ? workshop.getRawResponse() : response);
			while(values.length() > 0) {
				values.remove(0);
			}
		}
	}

}
