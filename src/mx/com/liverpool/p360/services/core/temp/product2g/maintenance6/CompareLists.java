package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

public class CompareLists {

	
	public static void main(String[] args) throws java.io.IOException {
		java.util.List<String> a = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(args[0]));
		java.util.List<String> b = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(args[1]));
		String[] aa = a.toArray(new String[] {});
		java.util.Arrays.sort(aa);
		java.util.List<String> d = new java.util.ArrayList<>();
		java.util.List<String> e = new java.util.ArrayList<>();
		for(String c : b) {
			if(java.util.Arrays.binarySearch(aa, c) > -1){
				d.add(c);
			}else {
				e.add(c);
			}
		}
		b.removeAll(d);
		System.out.println("Los que sí:");
		d.forEach(System.out::println);
		System.out.println("Los extra:");
		e.forEach(System.out::println);
	}
	
}
