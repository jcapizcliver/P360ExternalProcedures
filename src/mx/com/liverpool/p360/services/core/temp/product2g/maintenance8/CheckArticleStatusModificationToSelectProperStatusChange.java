package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import java.util.HashMap;
import java.util.Map;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class CheckArticleStatusModificationToSelectProperStatusChange {
	
	private static final RESTWrapper rw = new RESTWrapper();
	private static int count = 0;
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> englishToSpanish = createEnglishToSpanishMap();
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]+)\"");
		java.util.Map<String, String> productStatus = new java.util.HashMap<>();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		java.util.Set<String> set = new java.util.TreeSet<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', null, "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			count++;
			if(row.length == 0) {
				System.out.println("Going back.");
				return;
			}
			if(count == 1) {
				return;
			}
			if(row.length < 3) {
				System.out.println( java.util.Arrays.asList(row) );
				return;
			}
			String[] pieces = row[1].split("\n");
			String status = null;
//			System.out.println(java.util.Arrays.asList(row));
			System.out.println(row[2] + "->" + productStatus.get(row[2]));
			if(pieces.length <= 1) {
				status = productStatus.get(row[2]);
				if(status == null) {
					productStatus.put(row[2], "Revisión QA");
				}
			}else {
				status = productStatus.get(row[2]);
				if(status == null) {
					for(int i=0; i<pieces.length; i++) {
						if(!pieces[i].contains("Carga de Imagen") && !pieces[i].contains("Image Load") && pieces[i].matches("[^\"]+\"[^\"]+\"[^\"]+")) {
							java.util.regex.Matcher m = p.matcher(pieces[1]);
							if(m.find()) {
								String raw = m.group(1);
								String translated = englishToSpanish.get(raw);
								String statusText = translated != null ? translated : raw;
								if("Eliminada".equals(statusText)) {
									
								}else {
									if("Borrador".equals(statusText)) {
										
									}
									if("Carga de Imagen".equals(statusText)) {
										statusText = "Revisión QA";
									}
									productStatus.put(row[2], statusText);
									break;
								}
							}
						}
					}
				}
			}
		} );
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_PIM_MASTER_20260804_105010.csv"));
		productStatus.forEach((k,v) ->{ System.out.println(k + " - " + v); set.add(v); });
		System.out.println("***");
		set.forEach(System.out::println);
		System.out.println(count);
		productStatus.forEach((k,v) ->{ rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + k + "'@1")).put("values", new org.json.JSONArray().put(v))); });
		rh.sendData();
		
	}
	
	private static Map<String, String> createEnglishToSpanishMap() {
        Map<String, String> enToEs = new HashMap<>();
        enToEs.put("Canceled", "Cancelado");
        enToEs.put("Data Gobernance", "Gobierno de Datos");
        enToEs.put("Draft", "Borrador");
        enToEs.put("In Foro Process", "En Proceso Foro");
        enToEs.put("Purchase Rejected", "Rechazo Compras");
        enToEs.put("Purchase Revision", "Revisión Compras");
        enToEs.put("QA Revision", "Revisión QA");
        enToEs.put("QA Rejected", "Rechazo QA");
        enToEs.put("SKU Creation", "Creación de SKU");
        enToEs.put("Proposal Generated", "Propuesta Generada");
        enToEs.put("Pending Enrichment", "Pendiente Inicio Enriquecimiento");
        enToEs.put("Image Load", "Carga de Imagen");
        enToEs.put("Rejected", "Rechazada");
        enToEs.put("To Be Updated", "Por Actualizar");
        enToEs.put("Approved", "Aprobada");
        enToEs.put("Modified", "Modificación");
        enToEs.put("Liverpool in progress", "En Proceso Liverpool");
        enToEs.put("Sending in progress", "En Proceso de Envío");
        enToEs.put("Publish Rejected", "Rechazo Publicación");
        enToEs.put("Deleted", "Eliminada");
        enToEs.put("Repopulation", "Repoblamiento");
        enToEs.put("Cataloguing Exception", "Excepción de Catalogación");
        enToEs.put("Accepted", "Aceptado");
        enToEs.put("Accepted with arrangements", "Aceptado con ajustes");
        return enToEs;
    }
	
}
