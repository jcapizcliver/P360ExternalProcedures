package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class LetMeKnowAboutTemplates {

	
	public static void main(String[] args) {
		
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("http://172.18.237.162:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields",
				   "StandardizationValue.Value" 
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		qp.put("pageSize", "10000");
		java.util.List<String[]> rows = new java.util.ArrayList<>(500000);
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			rows.add(new String[] { 
					  values.getString(0)
					, values.getString(1)
					, values.getString(2)
					, values.getString(3)
					, values.getString(4)
					, values.getString(5)
				});
		});
		java.util.Collections.sort(rows, (o1,o2) -> compareArrays( o1, o2 ) );
		java.util.Set<String> plantillaCaracteristica = new java.util.TreeSet<>();
		rows.forEach(r -> plantillaCaracteristica.add(rw.getRw().serializeChunk(new String[] { r[1], r[2] })));
		java.util.Map<String, Integer> plantillaCuantasCaracteristicas = new java.util.TreeMap<>();
		Integer freq = 0;
		String[] pieces = null;
		for(String r : plantillaCaracteristica) {
			pieces = rw.getRw().parseLine(r);
			freq = plantillaCuantasCaracteristicas.get(pieces[0]);
			plantillaCuantasCaracteristicas.put(pieces[0], (freq == null ? 0 : freq) + 1);
		}
		java.util.List<java.util.Map.Entry<String, Integer>> lst = new java.util.ArrayList<>( plantillaCuantasCaracteristicas.entrySet() );
		java.util.Collections.sort(lst, (o1,o2) -> o2.getValue().compareTo(o1.getValue()));
		lst.forEach( en -> System.out.println( en.getKey() + " - " + en.getValue() ) );
	}
	
	public static int compareArrays(String[] s1, String[] s2) {
		int r = s1[1].compareTo(s2[1]);
		if(r == 0) {
			r = s1[2].compareTo(s2[2]);
			if(r == 0) {
				r = s1[4].compareTo(s2[4]);
			}
		}
		return r;
	}
	
}
