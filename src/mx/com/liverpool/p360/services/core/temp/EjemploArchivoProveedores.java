package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class EjemploArchivoProveedores {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) throws ServiceUnavailableException {

		java.util.Set<String> nombreAtributoDistintos = new java.util.TreeSet<>();

		String delimitador = "\"";
		String separador = "\t";
		String escape = "\\";
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - Grupos.tsv"), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] piezas = null;
			String nombreAtributo = null;
			line = br.readLine();
			String[] encabezados = workshop.parseLine(line, delimitador, separador, escape);
			int indiceNombreAtributo = java.util.Arrays.asList(encabezados).indexOf("Nombre característ.");
			while((line = br.readLine()) != null) {
				piezas = workshop.parseLine(line, delimitador, separador, escape);
				nombreAtributo = piezas[indiceNombreAtributo];
				nombreAtributoDistintos.add(nombreAtributo);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		workshop.putParameter("fields", "Characteristic.Identifier,CharacteristicIdentifier.AlternativeIdentifier(ECC)");
		workshop.putParameter("query", "not CharacteristicIdentifier.AlternativeIdentifier(ECC) is empty");
		workshop.putParameter("pageSize", "50");
		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> identificadoresCaracteristicas = new java.util.TreeMap<>();

		do{
			workshop.putParameter("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch");
			if(response == null) {
				System.out.println("Problema: " + workshop.getRawResponse());
				System.exit(1);
			}
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				identificadoresCaracteristicas.put(values.getString(1), values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		java.util.Set<String> encontrados = new java.util.TreeSet<>();
		java.util.Set<String> noEncontrados = new java.util.TreeSet<>();
		String atributoAlternativo = null;
		for(String nombreAtributo : nombreAtributoDistintos) {
			atributoAlternativo = identificadoresCaracteristicas.get(nombreAtributo);
			if(atributoAlternativo == null) {
				noEncontrados.add(nombreAtributo);
			}else {
				encontrados.add(nombreAtributo);
			}
		}

		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\AtributosEncontradosECCQA")))){
			for(String nombreAtributo : encontrados) {
				pw.println(nombreAtributo);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\AtributosNOEncontradosECCQA")))){
			for(String nombreAtributo : noEncontrados) {
				pw.println(nombreAtributo);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\ElQuePedí")))){
			pw.println( workshop.serializeChunk(new String[] { "AtributoSAP", "AtributoP360" }, delimitador, separador, escape) );
			for(String nombreAtributo : nombreAtributoDistintos) {
				atributoAlternativo = identificadoresCaracteristicas.get(nombreAtributo);
				pw.println( workshop.serializeChunk(new String[] { nombreAtributo, atributoAlternativo == null ? "" : atributoAlternativo }, delimitador, separador, escape) );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}



	}

}
