package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ConfiguraciónEsSostenibleVaD {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.nio.file.Path p = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "plantillas", "pes");
		java.nio.file.Path p2Templates = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "plantillas", "templates");
		java.util.Set<String> templates = new java.util.TreeSet<>();
		java.util.LinkedList<String> templatesForEsSostenibleVaD = new java.util.LinkedList<>();
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler(
				new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"))
				.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))
				, 1000, response->{
					rw.writeData("list", "StandardizationValue", null, qp0, response, System.out::println);
				});
		if(!java.nio.file.Files.exists(p2Templates)) {
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code");
			qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
			qp.put("pageSize", "20000");
			qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and not StandardizationValue.StructureGroup is empty");
			rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> templates.add(row.getJSONArray("values").getString(0)), System.out::println);
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(p2Templates.toFile())))){
				templates.forEach(pw::println);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(p2Templates)){
				lns.forEach(templates::add);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(p)){
			lns.forEach(templatesForEsSostenibleVaD::addLast);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		for(String toi : templatesForEsSostenibleVaD) {
			if(templates.contains(toi)) {
				addValues(toi, "EsSostenibleVaD", rh);
			}
		}
		rh.sendData();
	}
	
	private static void addValues(String template, String characteristic, RequestHandler rh) {
		addValue(template, characteristic, "Business", "Liverpool Suburbia Marketplace", rh);
		addValue(template, characteristic, "SentToVendorCenter", "1", rh);
		addValue(template, characteristic, "VendorCenterSection", "Atributos", rh);
		addValue(template, characteristic, "IsMandatory", "true", rh);
	}
	
	private static void addValue(String template, String attribute, String property, String value, RequestHandler rh) {
		String key = String.join("<::>", new String[] { template, attribute, "CreateProposal", property });
		rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + key + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(template).put(attribute).put("CreateProposal").put(property).put(value)));
	}
}
