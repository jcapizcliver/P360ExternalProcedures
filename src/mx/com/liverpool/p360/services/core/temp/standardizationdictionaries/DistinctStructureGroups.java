package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DistinctStructureGroups extends RESTWrapper {

	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.StructureGroup->LookupValueLang.Name(es)");
		qp.put("pageSize", "25000");
		qp.put("dictionary", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		java.util.Map<String, Long> freqs = new java.util.TreeMap<>();
		DistinctStructureGroups d = new DistinctStructureGroups();
		d.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String name = values.getString(0) + " (" + values.getString(1) + ")";
			Long freq = freqs.get(name);
			freqs.put(name, (freq == null ? 0 : freq ) + 1);
		});
		java.util.LinkedList<java.util.Map.Entry<String, Long>> lst = new java.util.LinkedList<>( freqs.entrySet() );
		java.util.Collections.sort( lst, (o1,o2) -> o2.getValue().compareTo(o1.getValue()) );
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ListaPlantillasConfiguradas.csv").toFile())))){
			pw.println( d.getRw().serializeChunk(new Object[] { "Plantilla", "Cantidad de Propiedades por Atributo configurado" }) );
			lst.forEach(en -> pw.println( d.getRw().serializeChunk(new Object[] { en.getKey(), en.getValue() }) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
