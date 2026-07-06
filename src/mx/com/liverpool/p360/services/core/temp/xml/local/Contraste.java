package mx.com.liverpool.p360.services.core.temp.xml.local;

public class Contraste {

	
	public static void main(String[] args) {
		java.util.Set<String> ext = new java.util.TreeSet<>();
		java.util.Set<String> me = new java.util.TreeSet<>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "ext_sp"))){
			stream.forEach(ext::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "sin_pas"))){
			stream.forEach(me::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Los que no encontraron...");
		for(String m : me) {
			if(!ext.contains(m)) {
				System.out.println(m);
			}
		}
		System.out.println("Los que no encontré:");
		for(String m :ext) {
			if(!me.contains(m)) {
				System.out.println(m);
			}
		}
		
	}
	
}
