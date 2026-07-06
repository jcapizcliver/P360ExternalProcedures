package mx.com.liverpool.p360.services.core.temp.structuregroups;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class PlantillasQueNoEstánEnUso {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.concurrent.ConcurrentLinkedQueue<String> plantillasConReg = new java.util.concurrent.ConcurrentLinkedQueue<>();
		java.util.concurrent.ConcurrentLinkedQueue<String> plantillasEnAlgunProducto = new java.util.concurrent.ConcurrentLinkedQueue<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "New folder", "plantillas_con_algun_registro.txt"))){
			lns.parallel().filter(s -> !"".equals(s)).map(s -> s.split("\\=")).forEach(s -> plantillasConReg.add(s[0]));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "New folder", "plantillas_que_existen_referenciadas_por_algún_producto.txt"))){
			lns.parallel().filter(s -> !"".equals(s)).map(s -> s.split("\\=")).forEach(s -> plantillasEnAlgunProducto.add(s[0]));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.concurrent.ConcurrentLinkedQueue<String> noHanSidoUsadas = new java.util.concurrent.ConcurrentLinkedQueue<>();
		plantillasConReg.parallelStream().forEach(s -> {
			if(!plantillasEnAlgunProducto.contains(s)) {
				noHanSidoUsadas.add(s);
			}
		} );
		noHanSidoUsadas.forEach(System.out::println);
		System.out.println("No han sido usadas: " + noHanSidoUsadas.size());
	}
	
	
}
