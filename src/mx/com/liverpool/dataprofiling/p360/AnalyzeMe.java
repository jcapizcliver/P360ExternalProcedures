package mx.com.liverpool.dataprofiling.p360;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AnalyzeMe {
	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		java.util.Map<String, java.util.Set<String>> sections = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.PropertyValue");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Property->LookupValue.Code = \"VendorCenterSection\"");
		qp.put("pageSize", "25000");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			java.util.Set<String> set = sections.get(values.getString(1));
			if(set == null) {
				set = new java.util.TreeSet<>();
				sections.put(values.getString(1), set);
			}
			if(!"".equals(values.get(0)))
				set.add(values.getString(0));
		});
		qp.put("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			java.util.Set<String> set = sections.get(values.getString(1));
			if(set == null) {
				set = new java.util.TreeSet<>();
				sections.put(values.getString(1), set);
			}
			if(!"".equals(values.get(0)))
				set.add(values.getString(0));
		});
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "VendorCenterSections.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println( workshop.serializeChunk(new Object[] { "VendorCenterSection", "Characteristic" }) );
			sections.entrySet().forEach(en -> {
				for(String val : en.getValue()) {
					pw.println( workshop.serializeChunk( new Object[] { en.getKey(), val} ));
				}
		});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

}
