package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class Ensayo {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		String fileName = "C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación grupos caracteristicas.tsv";

		java.util.Map<String, String> mapa = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> mapaDeMapas = new java.util.TreeMap<>();

		java.util.LinkedList<String[]> piezas = new java.util.LinkedList<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(fileName), java.nio.charset.Charset.forName("UTF-8")))){
			String line = null;
			String[] pieces = null;
			String delim = "\"";
			String sep = "\t";
			String esc = "\\";
			String[] header = workshop.parseLine(br.readLine(), delim, sep, esc);
			System.out.print(java.util.Arrays.asList(header));
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, esc);
				piezas.addLast(pieces);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

		java.util.Collections.sort(piezas, (o1,o2)-> o1[7].compareTo(o2[7]) );

		String prevAttribute = null;

		for(String[] partes : piezas) {
			if(prevAttribute != null && !prevAttribute.equals(partes[7])) {
				mapaDeMapas.put(prevAttribute, mapa);
				mapa = new java.util.TreeMap<>();
			}
			mapa.put(partes[9], partes[10]);
			prevAttribute = partes[7];
		}
		mapaDeMapas.put(prevAttribute, mapa);
		mapa = new java.util.TreeMap<>();

		mapaDeMapas.forEach((k,v)->System.out.println(k + ": " + v.size()));

	}

	public static java.util.Map<String, String> getLookupContent(String lookup) throws ServiceUnavailableException{

		RESTWorkshop workshop = new RESTWorkshop();

		java.util.Map<String, String> lookupValueContent = new java.util.TreeMap<>();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		workshop.putParameter("lookup", lookup);
		workshop.putParameter("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		workshop.putParameter("pageSize", "900");

		int currentIndex = 0;
		int totalSize = 0;

		do {
			workshop.putParameter("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/byLookup");
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				lookupValueContent.put(values.getString(0), values.getString(1));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return lookupValueContent;
	}

}
