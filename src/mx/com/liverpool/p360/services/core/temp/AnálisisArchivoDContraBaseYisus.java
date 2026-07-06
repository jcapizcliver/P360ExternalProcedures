package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class AnálisisArchivoDContraBaseYisus {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		String archivoD = "D:\\tmp\\ejemploArchivoD.txt";
		String baseYisus = "C:\\Users\\jcapizc\\Downloads\\Base_SAP_QA - Base atributos LOV.tsv";
		String laOtraReferencia = "C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación grupos caracteristicas.tsv";
		java.util.Map<String, java.util.Map<String, String>> mapas = null;
		java.util.Map<String, java.util.Map<String, String>> atributosValores = null;
		java.util.Map<String, java.util.Map<String, String>> mapas2 = null;
		mapas = mapasBaseYisus(baseYisus);
		mapas2 = mapassssLaOtraReferencia(laOtraReferencia);
		atributosValores = mapassss(archivoD);

		java.util.Map<String, String> hola = null;
		java.util.Set<String> fieldsNotFoundInData = new java.util.TreeSet<>();
		java.util.Set<String> losQueSi = new java.util.TreeSet<>();
		java.util.Set<String> losKissi2 = new java.util.TreeSet<>();
		String refValue = null;
		java.util.LinkedList<String> valueNotFound = new java.util.LinkedList<>();
		for(java.util.Map.Entry<String, java.util.Map<String, String>> entries : atributosValores.entrySet()) {
			for(java.util.Map.Entry<String, String> subEntry : entries.getValue().entrySet()) {
				hola = mapas.get(subEntry.getKey());
				if(hola == null) {
					hola = mapas2.get(subEntry.getKey());
					if(hola == null) {
						fieldsNotFoundInData.add(subEntry.getKey());
					}else {
						losKissi2.add(subEntry.getKey());
						refValue = hola.get(subEntry.getValue());
						if(refValue == null) {
							valueNotFound.addLast("Valor no encontrado en las acotaciones proporcionadas: " + subEntry.getValue() + " asignado al atributo: " + subEntry.getKey() + "<::>" + hola);
						}
					}
				}else {
					losQueSi.add(subEntry.getKey());
					refValue = hola.get(subEntry.getValue());
					if(refValue == null) {
						valueNotFound.addLast("Valor no encontrado en las acotaciones proporcionadas: " + subEntry.getValue() + " asignado al atributo: " + subEntry.getKey() + "<::>" + hola);
					}
				}
			}
		}

		losQueSi.forEach(System.out::println);
		System.out.println("** Not found **");
		fieldsNotFoundInData.forEach(System.out::println);
		System.out.println("** Found in second opinion **");
		losKissi2.forEach(System.out::println);
		System.out.println("** Hola **");
		valueNotFound.forEach(System.out::println);
//		System.out.println(mapas.size());
//		mapas.forEach((k,v)-> System.out.println(k + ": " + v.size()) );
//		System.out.println(atributosValores);
	}

	private static java.util.Map<String, java.util.Map<String, String>> mapassss(String archivoD){
		java.util.Map<String, java.util.Map<String, String>> hola = new java.util.TreeMap<>();
		java.util.Map<String, String> esos = new java.util.TreeMap<>();
		java.util.LinkedList<String[]> lineas = new java.util.LinkedList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(archivoD)))){
			String line = null;
			String delim = "";
			String sep = "|";
			String esc = "";
			String[] encabezado = workshop.parseLine(br.readLine(), delim, sep, esc);
			String id = null;
			String prevId = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				lineas.addLast(pieces = workshop.parseLine(line, delim, sep, esc));
				id = pieces[0];
				if(prevId != null && !prevId.equals(id)) {
					hola.put(prevId, esos);
					esos = new java.util.TreeMap<>();
				}
				esos.put(pieces[11], pieces[12]);
				prevId = pieces[0];
			}
			hola.put(prevId, esos);
			esos = new java.util.TreeMap<>();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return hola;
	}

	private static java.util.Map<String, java.util.Map<String, String>> mapasBaseYisus(String fileName){
		java.util.Map<String, java.util.Map<String, String>> mapas = new java.util.TreeMap<>();
		java.util.LinkedList<String[]> losesos = new java.util.LinkedList<>();
		String attId = null;
		String lovId = null;
		String code = null;
		String value = null;
		String prevAttId = null;
		String prevLovId = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(fileName)))){
			String linea = br.readLine();
			String delim = "\"";
			String sep = "\t";
			String esc = "\\";
			String[] encabezado = workshop.parseLine(linea, delim, sep, esc);
			String[] pieces = null;
			while((linea = br.readLine()) != null) {
				pieces = workshop.parseLine(linea, delim, sep, esc);
				losesos.addLast(pieces);
			}
			System.out.println(java.util.Arrays.asList(encabezado));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Collections.sort(losesos, (o1,o2)-> o1[1].compareTo(o2[1]) == 0 ? o1[2].compareTo(o2[2]) : o1[1].compareTo(o2[1]) );
		java.util.Map<String, String> diccionario = new java.util.TreeMap<>();
		for(String[] pieces : losesos) {
			attId = pieces[1];
			lovId = pieces[2];
			code = pieces[4];
			value = pieces[5];
			if( prevAttId != null && prevLovId != null && (!prevAttId.equals(attId) || !prevLovId.equals(lovId)) ) {
				mapas.put(prevAttId, diccionario);
				diccionario = new java.util.TreeMap<>();
			}
			diccionario.put(code, value);
			prevAttId = attId;
			prevLovId = lovId;
		}
		mapas.put(prevAttId + "_" + prevLovId, diccionario);
		diccionario = new java.util.TreeMap<>();
		return mapas;

	}

	private static java.util.Map<String, java.util.Map<String, String>> mapassssLaOtraReferencia(String fileName){
		java.util.Map<String, java.util.Map<String, String>> mapas = new java.util.TreeMap<>();
		java.util.LinkedList<String[]> losesos = new java.util.LinkedList<>();
		String attId = null;
		String code = null;
		String value = null;
		String prevAttId = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(fileName)))){
			String linea = br.readLine();
			String delim = "\"";
			String sep = "\t";
			String esc = "\\";
			String[] encabezado = workshop.parseLine(linea, delim, sep, esc);
			String[] pieces = null;
			while((linea = br.readLine()) != null) {
				pieces = workshop.parseLine(linea, delim, sep, esc);
				losesos.addLast(pieces);
			}
			System.out.println(java.util.Arrays.asList(encabezado));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Collections.sort(losesos, (o1,o2)-> o1[1].compareTo(o2[1]) == 0 ? o1[2].compareTo(o2[2]) : o1[1].compareTo(o2[1]) );
		java.util.Map<String, String> diccionario = new java.util.TreeMap<>();
		for(String[] pieces : losesos) {
			attId = pieces[7];
			code = pieces[9];
			value = pieces[10];
			if( prevAttId != null && !prevAttId.equals(attId) ) {
				mapas.put(prevAttId, diccionario);
				diccionario = new java.util.TreeMap<>();
			}
			diccionario.put(code, value);
			prevAttId = attId;
		}
		mapas.put(prevAttId, diccionario);
		diccionario = new java.util.TreeMap<>();
		return mapas;

	}

}
