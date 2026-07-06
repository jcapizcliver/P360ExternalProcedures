package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QuitaSostenible {

	private static RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Value");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("pageSize", "2000");
		java.util.concurrent.ConcurrentLinkedQueue<String> lst = new java.util.concurrent.ConcurrentLinkedQueue<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "standardizationValues", "yesno"))){
			lns.parallel().map( QuitaSostenible::split ).filter( QuitaSostenible::filter ).map(a -> a[0]).forEach( lst::add );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		int a = 0;
		for(String l : lst) {
			sb.append(a == 0 ? "" : ",");
			sb.append("\"");
			sb.append(l);
			sb.append("\"");
			a++;
			if(a == 100) {
				qp.put("query", "StandardizationValue.Characteristic->Characteristic.Identifier in (\"CertificadoSostenible\",\"AE485\",\"EsSostenibleVaD\") and StandardizationValue.StructureGroup->LookupValue.Code in (" + sb.toString() + ")");
				rw.deleteData("list", "StandardizationValue", null, "bySearch", qp, System.out::println);
				a = 0;
				sb.setLength(0);
			}
		}
		if(a > 0) {
			rw.deleteData("list", "StandardizationValue", null, "bySearch", qp, System.out::println);
		}
	}
	
	private static boolean filter(String[] pieces) {
		return "N".equals(pieces[1].toUpperCase());
	}
	
	private static String[] split(String a) {
		return a.split("\t");
	}
	
}
