package com.jcapiz.memelos.misc;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CuentaDatosPorAtributo {

	
	public static void main(String[] args) {
		java.util.Set<String> vad = new java.util.TreeSet<>();
		java.util.Set<String> att = new java.util.TreeSet<>();
		java.util.Set<String> sbb = new java.util.TreeSet<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "CaracteristicasVaD.txt"))){
			lns.forEach(vad::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "CaracteristicasAtt.txt"))){
			lns.forEach(att::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "CaracteristicasSB.txt"))){
			lns.forEach(sbb::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		java.util.Map<String, Long> freqs = new java.util.TreeMap<>();
		java.util.Map<String, Long> freqsV = new java.util.TreeMap<>();
		java.util.Map<String, Long> freqsA = new java.util.TreeMap<>();
		java.util.Map<String, Long> freqsS = new java.util.TreeMap<>();
		Long[] f = new Long[1];
		f[0] = 0l;
		for(String at : vad) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Atributos", ("BotonDeInicio/ParadaVaD".equals( at ) ? "BotonDeInicio.ParadaVaD" : "GarantiaCompresor/MotorVaD".equals(at) ? "GarantiaCompresor.MotorVaD" : at) + ".csv"))){
				lns.forEach(s -> f[0]++);
			}catch(java.io.IOException e) {
				f[0] = 1l;
			}
			freqs.put(at, f[0] - 1);
			freqsV.put(at, f[0] - 1);
			f[0] = 0l;
		}
		f[0] = 0l;
		for(String at : att) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Atributos", at + ".csv"))){
				lns.forEach(s -> f[0]++);
			}catch(java.io.IOException e) {
				f[0] = 1l;
			}
			freqs.put(at, f[0] - 1);
			freqsA.put(at, f[0] - 1);
			f[0] = 0l;
		}
		f[0] = 0l;
		for(String at : sbb) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Atributos", at + ".csv"))){
				lns.forEach(s -> f[0]++);
			}catch(java.io.IOException e) {
				f[0] = 1l;
			}
			freqs.put(at, f[0] - 1);
			freqsS.put(at, f[0] - 1);
			f[0] = 0l;
		}
		RESTWrapper rw = new RESTWrapper();
		java.util.LinkedList<java.util.Map.Entry<String, Long>> lst = new java.util.LinkedList<>(freqs.entrySet());
		java.util.Collections.sort(lst, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
		java.util.LinkedList<java.util.Map.Entry<String, Long>> lstV = new java.util.LinkedList<>(freqsV.entrySet());
		java.util.Collections.sort(lstV, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
		java.util.LinkedList<java.util.Map.Entry<String, Long>> lstA = new java.util.LinkedList<>(freqsA.entrySet());
		java.util.Collections.sort(lstA, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
		java.util.LinkedList<java.util.Map.Entry<String, Long>> lstS = new java.util.LinkedList<>(freqsS.entrySet());
		java.util.Collections.sort(lstS, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "TablaAtributosCuenta.csv").toFile())))){
			pw.println( rw.getRw().serializeChunk(new String[] { "Atributo", "Cantidad_de_Registros" }) );
			lst.forEach(ent -> pw.println( rw.getRw().serializeChunk(new String[] {ent.getKey(), String.valueOf( ent.getValue() )}) ));
		}catch(java.io.IOException e) {
			
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "TablaAtributosCuentaVaD.csv").toFile())))){
			pw.println( rw.getRw().serializeChunk(new String[] { "Atributo", "Cantidad_de_Registros" }) );
			lstV.forEach(ent -> pw.println( rw.getRw().serializeChunk(new String[] {ent.getKey(), String.valueOf( ent.getValue() )}) ));
		}catch(java.io.IOException e) {
			
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "TablaAtributosCuentaAtt.csv").toFile())))){
			pw.println( rw.getRw().serializeChunk(new String[] { "Atributo", "Cantidad_de_Registros" }) );
			lstA.forEach(ent -> pw.println( rw.getRw().serializeChunk(new String[] {ent.getKey(), String.valueOf( ent.getValue() )}) ));
		}catch(java.io.IOException e) {
			
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "TablaAtributosCuentaSBB.csv").toFile())))){
			pw.println( rw.getRw().serializeChunk(new String[] { "Atributo", "Cantidad_de_Registros" }) );
			lstS.forEach(ent -> pw.println( rw.getRw().serializeChunk(new String[] {ent.getKey(), String.valueOf( ent.getValue() )}) ));
		}catch(java.io.IOException e) {
			
		}
		
		/*
		java.io.File[] files = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Atributos").toFile().listFiles(ff->ff.getName().endsWith(".csv"));
		java.util.Map<String, java.io.File> freqs = new java.util.TreeMap<>();
		java.util.Map<String, Long> counts = new java.util.TreeMap<>();
		Long[] f = new Long[0];
		f[0] = 0l;
		for(int i=0; i<files.length; i++) {
			freqs.put("BotonDeInicio.ParadaVaD.csv".equals( files[i].getName() ) ? "BotonDeInicio/ParadaVaD" : "GarantiaCompresor.MotorVaD.csv".equals( files[i].getName() ) ? "GarantiaCompresor/MotorVaD" : files[i].getName().replaceAll("\\.csv", ""), files[i]);
		}
		for(java.util.Map.Entry<String, java.io.File> entry : freqs.entrySet()) {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(entry.getValue().getAbsolutePath()))){
				lns.forEach(s -> f[0]++);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			counts.put(entry.getKey(), f[0] - 1);
			f[0] = 0l;
		}
		java.util.LinkedList<java.util.Map.Entry<String, Long>> lst = new java.util.LinkedList<>(counts.entrySet());
		java.util.Collections.sort(lst, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
		lst.forEach(System.out::println);
		*/
	}
	
}
