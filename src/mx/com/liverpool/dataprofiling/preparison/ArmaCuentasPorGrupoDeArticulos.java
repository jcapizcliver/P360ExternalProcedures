package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ArmaCuentasPorGrupoDeArticulos {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final  RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Map<String, String> templates = new java.util.TreeMap<>();
		java.util.Map<String, String> characteristics = new java.util.TreeMap<>();
		getTemplateLabels(templates);
		getCharacteristicLabels(characteristics);
		System.out.println("Now collecting refs...");
		java.util.Map<String, Long> registrosPorPlantilla = new java.util.TreeMap<>();;
		java.util.Map<String, Long> templateWithIDs = new java.util.TreeMap<>();
		java.util.Map<String, Long> templateWithAttributes = new java.util.TreeMap<>();
		java.util.Map<String, Long> templateAttributeEspaciosMultiples = new java.util.TreeMap<>();
		java.util.Map<String, Long> templateAttributeEspaciosAlInicioOFinal = new java.util.TreeMap<>();
		java.util.Map<String, Long> templateAttributeCaracteresEspeciales = new java.util.TreeMap<>();
		java.util.Map<String, Long> templateAttributeCaracteresTotalesALV = new java.util.TreeMap<>();
		Long freq = null;
		java.util.Map<String, java.util.Set<String>> templateFrequencies = new java.util.TreeMap<>();
		java.util.Set<String> misValores = null;
		try(java.io.BufferedReader br = 
				new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "ItemGroup", "Attributes", "AtributosPlantillas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] pieces = null;
			br.readLine();
			String key = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line);
				key = workshop.serializeChunk(new Object[] { pieces[0], pieces[1] });
				freq = templateWithIDs.get(key);
				templateWithIDs.put( key, (freq == null ? 0 : freq)  );
				key = workshop.serializeChunk( new Object[] { pieces[0], pieces[2] } );
				misValores = templateFrequencies.get(key);
				if(misValores == null) {
					misValores = new java.util.TreeSet<>();
					templateFrequencies.put(key, misValores);
				}
				misValores.add(pieces[3]);
				freq = templateWithAttributes.get( key );
				templateWithAttributes.put(key, (freq == null ? 0 : freq) + 1);
				freq = templateAttributeEspaciosMultiples.get(key);
				templateAttributeEspaciosMultiples.put(key, (freq == null ? 0 : freq) + ( pieces[3].matches(".*\\s{2,}.*") ? 1 : 0 ));
				freq = templateAttributeEspaciosAlInicioOFinal.get(key);
				templateAttributeEspaciosAlInicioOFinal.put(key, (freq == null ? 0 : freq) + ( pieces[3].matches("^(\\s+.*)|(.*\\s+)$") ? 1 : 0 ));
				freq = templateAttributeCaracteresEspeciales.get(key);
				templateAttributeCaracteresEspeciales.put(key, (freq == null ? 0 : freq) + caracteresEspeciales(pieces[3]));
				freq = templateAttributeCaracteresTotalesALV.get(key);
				templateAttributeCaracteresTotalesALV.put(key, (freq == null ? 0 : freq) + pieces[3].length());
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		/*
		 * 
		 * 
		 * 	Análisis de Impacto.
		 * 
		 * 
		 */
		String[] pieces = null;
		for(java.util.Map.Entry<String, Long> pair : templateWithIDs.entrySet()) {
			pieces = workshop.parseLine(pair.getKey());
			freq = registrosPorPlantilla.get(pieces[0]);
			registrosPorPlantilla.put(pieces[0], (freq == null ? 0 : freq) + 1);
		}
		System.out.println("Now building true ones");
		System.out.println("Now printing...");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "ItemGroup", "CompletitudDeAtributosPorPlantilla.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println( workshop.serializeChunk( new Object[] { 
					  "ItemGroup"
					, "Attribute"
					, "Attribute_Label"
					, "Completes"
					, "Cantidad_de_Valores_Distintos"
					, "Total_de_Valores"
					, "Espacios_Multiples"
					, "Espacios_Inicio_Fin" 
					, "Caracteres_Especiales" 
				} ) );
			templateWithAttributes.entrySet().forEach(entry -> {
				String[] pcs = workshop.parseLine(entry.getKey());
				Long totalDePlantilla = registrosPorPlantilla.get( pcs[0] );
				Long fullCharLength = templateAttributeCaracteresTotalesALV.get(entry.getKey());
				if(!"".equals(pcs[0]))
					pw.println( workshop.serializeChunk( 
						new Object[] {
							  pcs[0]
							, pcs[1]
							, characteristics.get(pcs[1])
							, new java.math.BigDecimal(entry.getValue())
								.multiply(java.math.BigDecimal.TEN.pow(2))
								.divide(new java.math.BigDecimal(totalDePlantilla), 4, java.math.RoundingMode.HALF_UP)
							, templateFrequencies.get( entry.getKey() ).size()
							, entry.getValue()
							, new java.math.BigDecimal( templateAttributeEspaciosMultiples.get(entry.getKey()) )
								.multiply(java.math.BigDecimal.TEN.pow(2))
								.divide(new java.math.BigDecimal(totalDePlantilla), 4, java.math.RoundingMode.HALF_UP)
							, new java.math.BigDecimal( templateAttributeEspaciosAlInicioOFinal.get(entry.getKey()) )
								.multiply(java.math.BigDecimal.TEN.pow(2))
								.divide(new java.math.BigDecimal(totalDePlantilla), 4, java.math.RoundingMode.HALF_UP)
							, 0l == fullCharLength ? java.math.BigDecimal.ZERO : new java.math.BigDecimal( templateAttributeCaracteresEspeciales.get(entry.getKey()) )
							}
						)
					);
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + workshop.formatTime(System.currentTimeMillis() - init));
	}
	
	private static int caracteresEspeciales(String v) {
		int cosos = 0;
		if(v != null) {
			for(int i=0; i<v.length(); i++) {
				if( !v.substring(i, i+1).matches("[A-Za-z0-9 áéíóúÁÉÍÓÚÑñüÜ]") ) {
					cosos++;
				}
			}
		}
		return cosos;
	}
	
	private static void getTemplateLabels(java.util.Map<String, String> templates) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es)");
		qp.put("query", "StructureGroup.Identifier wildcard \"EU4-%\"");
		qp.put("structure", "PrimaryProductTaxonomy");
		qp.put("pageSize", "5000");
		rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			templates.put(values.getString(0), values.getString(1));
		});
	}
	
	private static void getCharacteristicLabels(java.util.Map<String, String> characteristics) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,CharacteristicLang.Name(es)");
		qp.put("query", "not Characteristic.Identifier wildcard \"_Rechazo%\" and Characteristic.ParentCharacteristic is empty");
		qp.put("pageSize", "5000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			characteristics.put(values.getString(0), values.getString(1));
		});
	}
	
}
