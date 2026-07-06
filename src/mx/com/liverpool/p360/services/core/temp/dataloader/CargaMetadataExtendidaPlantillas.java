package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaMetadataExtendidaPlantillas {

	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Se necesita especificar la ruta al archivo que hay que cargar como primer argumento del programa. La primera columna debe ser PropertyValue y la segunda la llave, "
					+ "que consta de: Plantilla_Característica_Proposal_Propiedad, ejemplo: EU4-55075981_FIBER_PART1_Proposal_AttributeHelpInformation. La palabra \"Proposal\" se "
					+ "escribe literal, lo que cambia es: código de la plantilla, característica y la propiedad a establecer. Se espera que el archivo contenga encabezado y que sea separado por comas.\n"
					+ "Opcionalmente, se puede proporcionar como segundo argumento del programa una URL base para sobreescribir la URL predeterminada hacia P360 desarrollo, y un tercer parámetro, que es una cadena en Base64 correspondiente con las credenciales de un usuario a usar para la autenticación de P360.");
			return;
		}
		RESTWorkshop rw = new RESTWorkshop();
		if(args.length > 1) {
			rw.setBaseUrl(args[1]);
		}
		if(args.length > 2) {
			rw.getRc().getHeader().put("Authorization", args[2]);
		}
		java.util.LinkedList<String[]> content = new java.util.LinkedList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(args[0]),java.nio.charset.StandardCharsets.ISO_8859_1))){
			br.readLine();
			String delim = "\"";
			String sep = ",";
			String esc = "";
			String line = null;
			String[] partes = null;
			while((line = br.readLine()) != null) {
				partes = rw.parseLine(line, delim, sep, esc);
				content.addLast(partes);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		String[] parts = null;
		org.json.JSONObject row = null;
		for(String[] pieces : content) {
			parts = pieces[1].split("_");
			System.out.println(parts.length);
//			System.out.println(java.util.Arrays.asList(pieces));
			rows.put(row = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[1] + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'"))
					.put("values", new org.json.JSONArray()
							.put(new org.json.JSONObject().put("id", "'" + parts[0] + "'@'PPH_L4_Templates'") )
							.put(new org.json.JSONObject().put("id", "'" + parts[1] + "_" + parts[2] + "'") )
							.put(new org.json.JSONObject().put("id", "'CreateProposal'@'CreationType'") )
							.put(new org.json.JSONObject().put("id", "'" + parts[4].replaceAll("B.sicos", "Básicos").replaceAll("\\?$", "")
									.replaceAll("SendtoVendorCenter", "SentToVendorCenter")
									.replaceAll("ListofValues", "ListOfValues")
									.replaceAll("AllowedBusiness", "Business") + "'@'GroupCharacteristicMetadataExtensionProperty'") )
							.put(pieces[0])
						)
					);
//			System.out.println(row);
			if(rows.length() == 400) {
				response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
				if(response == null) {
					System.out.println("ERR: " + rw.getRawResponse());
					return;
				}else {
					System.out.println("RESP: " + response);
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
			if(response == null) {
				System.out.println("ERR: " + rw.getRawResponse());
			}else {
				System.out.println("RESP: " + response);
			}
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
}
