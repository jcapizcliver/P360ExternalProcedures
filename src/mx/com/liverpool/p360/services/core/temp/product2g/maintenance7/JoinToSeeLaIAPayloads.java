package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class JoinToSeeLaIAPayloads {

	
	public static void main(String[] args) {
		java.util.Map<String, String> inputs = new java.util.HashMap<>();
		java.util.Map<String, String> peticionesIA = new java.util.HashMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "AnInputsForIA.csv").toFile())))){
			String line = null;
			int a = 0;
			int b = 0;
			while((line = br.readLine()) != null) {
				a = line.indexOf("|");
				b = line.indexOf("|", a+1);
				inputs.put(line.substring(0,a) + "<:>" + line.substring(a+1,b), line.substring(b+1));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "PeticionesIA_ItemGroup.csv").toFile())))){
			String line = null;
			int a = 0;
			int b = 0;
			String input = null;
			while((line = br.readLine()) != null) {
				a = line.indexOf("|");
				b = line.indexOf("|", a+1);
				if(b == -1) {
					System.out.println("Bad (" + b + "): " + line);
					System.exit(0);
				}
				input = inputs.get(line.substring(0,a) + "<:>" + line.substring(a+1,b));
				if(input != null) {
					try{
						org.json.JSONObject anInput = new org.json.JSONObject( input );
						org.json.JSONArray products = anInput.getJSONArray("products");
						if(products.getJSONObject(0).has("proposalId")) {
							peticionesIA.put(line.substring(0,a) + "<:>" + line.substring(a+1,b), line.substring(b+1));
						}
					}catch(org.json.JSONException e) {
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		int found = 0;
		int a = 0;
		int vollstandig = 0;
		int ciegas = 0;
		int badJSONs = 0;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Entregable Asignaciones IA 21 de Abril del 2026.csv").toFile()))); java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "RespuestasIA_ItemGroup.csv").toFile())))){
			String line = null;
			String input = null;
			int b = 0;
			int c = 0;
			RESTWrapper rw = new RESTWrapper();
			pw.println( rw.getRw().serializeChunk( buildPredictionHeader() ) );
			while((line = br.readLine()) != null) {
				b = line.indexOf("|");
				c = line.indexOf("|", b+1);
				if(c == -1) {
					System.out.println("Bad (" + a + "): " + line);
					return;
				}
				input = inputs.get(line.substring(0,b) + "<:>" + line.substring(b+1,c));
				if(input != null) {
					found++;
					try{
						org.json.JSONObject anInput = new org.json.JSONObject( input );
						org.json.JSONArray products = anInput.getJSONArray("products");
						if(products.length() > 1) {
							vollstandig++;
						}
						if(!products.getJSONObject(0).has("proposalId")) {
							ciegas++;
						}else {
							pw.println( rw.getRw().serializeChunk( buildPredictionRow(products.getJSONObject(0).getString("proposalId"), new org.json.JSONArray(line.substring(c + 1)).getJSONObject(0).toString(), peticionesIA.get(line.substring(0,b) + "<:>" + line.substring(b+1,c))) ) );
						}
					}catch(org.json.JSONException e) {
						badJSONs++;
					}
				}
				a++;
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Found: " + found + "/" + a);
		System.out.println("Lela:  " + vollstandig);
		System.out.println("ciegas:" + ciegas);
		System.out.println("bads:  " + badJSONs);
		
	}
	
	public static String[] buildPredictionHeader() {
	    return new String[] {
	        "Identifier",
	        "direction",
	        "direction_confidence",
	        "section",
	        "section_confidence",
	        "item_group",
	        "item_group_confidence",
	        "item_group_llm_latency_ms",
	        "item_group_ml_top1",
	        "item_group_ml_top1_confidence",
	        "item_group_name",
	        "item_group_selected_by",
	        "item_group_selection_rationale",
	        "pim_product_name",
	        "pim_template_id",
	        "product_description",
	        "product_type_sap",
	        "image",
	        "RawStringResponse"
	    };
	}
	
	public static String[] buildPredictionRow(String identifier, String rawStringResponse, String raw) {
	    org.json.JSONObject json = new org.json.JSONObject(rawStringResponse);

	    return new String[] {
	        identifier == null ? "" : identifier,
	        jsonValueAsString(json, "direction"),
	        jsonValueAsString(json, "direction_confidence"),
	        jsonValueAsString(json, "section"),
	        jsonValueAsString(json, "section_confidence"),
	        jsonValueAsString(json, "item_group"),
	        jsonValueAsString(json, "item_group_confidence"),
	        jsonValueAsString(json, "item_group_llm_latency_ms"),
	        jsonValueAsString(json, "item_group_ml_top1"),
	        jsonValueAsString(json, "item_group_ml_top1_confidence"),
	        jsonValueAsString(json, "item_group_name"),
	        jsonValueAsString(json, "item_group_selected_by"),
	        jsonValueAsString(json, "item_group_selection_rationale"),
	        jsonValueAsString(json, "pim_product_name"),
	        jsonValueAsString(json, "pim_template_id"),
	        jsonValueAsString(json, "product_description"),
	        jsonValueAsString(json, "product_type_sap"),
	        jsonValueAsString(json, "image"),
	        raw == null ? "" : raw
	    };
	}
	
	private static String jsonValueAsString(org.json.JSONObject json, String key) {
	    if (json == null || key == null || !json.has(key) || json.isNull(key)) {
	        return "";
	    }

	    Object value = json.get(key);
	    return String.valueOf(value);
	}
	
}
