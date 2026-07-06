package com.jcapiz.memelos.misc;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ParteLaData {

	
	public static void main(String[] args) {
		RESTWrapper  rw = new RESTWrapper();
//		java.util.Set<String> atributos = new java.util.TreeSet<>();
		java.util.concurrent.ConcurrentHashMap<String, String> chm = new java.util.concurrent.ConcurrentHashMap<>();
		java.util.Map<String, java.io.PrintWriter> escritores = new java.util.TreeMap<>();
		String header = null;
//		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ATRIBUTO.csv"))){
//			lns.map(s -> rw.getRw().parseLine(s)).forEach(a -> atributos.add(a[0]));
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
//		atributos.forEach(System.out::println);
//		System.exit(0);
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ATRIBUTO.csv"))){
			java.util.Optional<String> op = lns.findFirst();
			if(!op.isEmpty()) {
				header = op.get();
				System.out.println("Using header: " + header);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ATRIBUTO.csv"))){
			lns.parallel().map(s -> rw.getRw().parseLine(s)).forEach(a -> chm.put(a[0], a[0]));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		for(String a : chm.keySet()) {
			try {
//				if(a.contains("/")) {
					java.io.PrintWriter pw = null;
					escritores.put(a, pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "Atributos", a.replace("/", ".") + ".csv").toFile()))));
					pw.println(header);
//				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VDWH.VDIM_PIM_ATRIBUTO.csv"))){
			lns.map(s -> rw.getRw().parseLine(s)).forEach(a -> {
				java.io.PrintWriter pw = escritores.get(a[0]);
				if(pw == null) {
					System.out.println("Column not found: " + a[0]);
				}else {
					pw.println( rw.getRw().serializeChunk(a) );
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		for(java.util.Map.Entry<String, java.io.PrintWriter> entry : escritores.entrySet()) {
			entry.getValue().close();
		}
	}
	
}
