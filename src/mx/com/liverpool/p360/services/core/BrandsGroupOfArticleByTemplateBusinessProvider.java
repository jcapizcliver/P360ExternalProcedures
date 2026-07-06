package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class BrandsGroupOfArticleByTemplateBusinessProvider {


	private static RESTWrapper rw = new RESTWrapper();
	
	public String otroRun(String[] args, RESTWorkshop workshop) {
		long init = System.currentTimeMillis();
		org.json.JSONObject generalResponse = new org.json.JSONObject();
		org.json.JSONArray elements = new org.json.JSONArray();
		org.json.JSONArray errors = new org.json.JSONArray();
		boolean algunEmpty = false;
		boolean alMenosUnYeah = false;
		String proveedor = args[0];
		String plantilla = args[1];
		String business = args[2];
		String processedTag = null;
		boolean foundParty = true;
		business = business == null ? "" : business;
		business = "liverpool".equals(business.toLowerCase()) ? "LVP" : "suburbia".equals(business.toLowerCase()) ? "SBB" : "marketplace".equals(business.toLowerCase()) ? "MKP" : "";
		log("Working with: " + java.util.Arrays.asList(args));
		java.util.LinkedList<String[]> productos = null;
		java.util.Map<String, String> nombreGrupoArt = new java.util.TreeMap<>();
		String nombrePlantilla = null;
		org.json.JSONObject resp = null;
		java.util.LinkedList<String> coins = new java.util.LinkedList<>();
		java.util.LinkedList<String> gas1 = new java.util.LinkedList<>();
		java.util.LinkedList<String> gas2 = new java.util.LinkedList<>();
		org.json.JSONArray nombresGruposDeArticulos = null;
		org.json.JSONArray marcasYeah = null;
		try {
			org.json.JSONArray businessForProvider = getBusinessForProvider(proveedor, workshop);
			workshop.putParameter("lookup", "'PPH_L4_Templates'");
			workshop.putParameter("query", "LookupValue.IsActive = true and LookupValue.Code equals \"" + plantilla + "\"");
			workshop.putParameter("fields",
					  "LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "MATKLLOV_S4H"    : "MATKLLOV") + "')->LookupValue.Code,"
					+ "LookupValueLang.Name(es),"
					+ "LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "BRAND_IDLOV_S4H" : "ZCOMALOV") + "')->LookupValue.Code,"
					+ "LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "MATKLLOV_S4H"    : "MATKLLOV") + "')->LookupValueLang.Name(es)");
			resp = workshop.makeRequest("GET", "/list/LookupValue/bySearch");
			if(resp != null && resp.has("rows") && resp.getJSONArray("rows").length() > 0) {
				nombrePlantilla = resp.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(1);
				org.json.JSONArray gruposDeArticulo = resp.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(0);
				nombresGruposDeArticulos = resp.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3);
				log("Got " + gruposDeArticulo.length() + " grupos de artículo para " + plantilla + " (" + ("SBB".equals(business) ? "MATKLLOV_S4H" : "MATKLLOV") + ").");
				for(int a = 0; a<gruposDeArticulo.length(); a++) {
					nombreGrupoArt.put(gruposDeArticulo.getString(a), nombresGruposDeArticulos.getString(a));
				}
				if(gruposDeArticulo != null && gruposDeArticulo.length() > 0) {
					workshop.putParameter("lookup", "'Party'");
					workshop.putParameter("query", "LookupValue.IsActive = true and LookupValue.Code equals \"" + proveedor + "\" and LookupValueReference.LookupValues('BusinessQualified')->LookupValue.Code in (\"" + business + "\")");
					workshop.putParameter("fields", "LookupValue.Code,LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "MATKLLOV_S4H" : "MATKLLOV") + "')->LookupValue.Code");
					resp = workshop.makeRequest("GET", "/list/LookupValue/bySearch");
					if(resp != null && resp.has("rows") && resp.getJSONArray("rows").length() > 0) {
						org.json.JSONArray gas = resp.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1);
						gas1 = toLinkedLists(gruposDeArticulo);
						gas2 = toLinkedLists(gas);
						if(!gas2.isEmpty()) {
							for(String value : gas1) {
								if(gas2.contains(value)) {
									coins.addLast(value);
								}
							}
						}else if(!gas1.isEmpty()) {
							for(String value : gas1) {
								coins.addLast(value);
							}
						}
					}else {
						foundParty = false;
						log("No response: " + workshop.getRawResponse());
					}
				}
			}
			if(nombrePlantilla != null) {
				log("Nombre del proveedor: " + proveedor + " $$$$$");
				log("Código de la plantilla: " + plantilla + " $$$$$");
				log("Nombre de la plantilla: " + nombrePlantilla + " $$$$$");
				generalResponse.put("idProveedor", proveedor);
				generalResponse.put("idPlantilla", plantilla);
				generalResponse.put("plantilla", nombrePlantilla);
				log("___" + foundParty + "___");
				if(!foundParty) {
					generalResponse.put("reglas", new org.json.JSONArray());
					generalResponse.put("todasLasMarcas", new org.json.JSONArray());
					log("Done: " + workshop.formatTime(System.currentTimeMillis() - init));
					return generalResponse.toString();
				}
				if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.use_ia"))) {
					if(business.toLowerCase().equals("mkp")) {
						generalResponse.put("reglas", new org.json.JSONArray());
						generalResponse.put("todasLasMarcas", toJSONArray( todasLasMarcas(workshop) ));
						org.json.JSONArray todasLasMarcas = generalResponse.getJSONArray("todasLasMarcas");
						java.util.LinkedList<String> itemGroups = new java.util.LinkedList<>();
						java.util.LinkedList<String> brands = new java.util.LinkedList<>();
						collectProhibitedBrandsAndItemGroups(itemGroups, brands);
						org.json.JSONArray nm = new org.json.JSONArray();
						for(int i=0; i<todasLasMarcas.length(); i++) {
							if(!"".equals(todasLasMarcas.getString(i)) && !brands.contains(todasLasMarcas.getString(i))) {
								nm.put(todasLasMarcas.getString(i));
							}
						}
						generalResponse.put("todasLasMarcas", nm);
						log("Done: " + workshop.formatTime(System.currentTimeMillis() - init));
						return generalResponse.toString();
					}
				}
				java.util.Set<String> marcasProveedor = consigeMarcasProveedor(proveedor, business, workshop);
				log("Marcas del proveedor: " + marcasProveedor);
				log("GPAs que coinciden: " + coins);
				
				for(String coin : coins) {
					log("Came to check: " + coin + ", business: " + business + ". Coins: " + coins.size());
					productos = productsFromGPA(plantilla, coin, business, workshop);
					log("Got " + productos.size() + " products.");
					if(marcasProveedor == null || marcasProveedor.isEmpty()) {
						marcasYeah = consigeMarcasGPA(coin, workshop);
					}else { 
						marcasYeah = consigeMarcasGPA(coin, marcasProveedor, business, workshop);
					}
					log("Checando GPA: " + coin + ", marcasYeah: " + marcasYeah);
					if(marcasYeah.length() == 0) {
						algunEmpty = true;
					}else {
						alMenosUnYeah = true;
					}
					if(marcasYeah != null && marcasYeah.length() > 0 && !"".equals(marcasYeah.get(0)))
						for(String[] product : productos) {
							processedTag = adapt(product[0],business,product[1]);
							log("Proceesed tag for " + product[1] + ": " + processedTag);
							if(processedTag != null) {
								elements
									.put(
										new org.json.JSONObject()
											.put("negocios", businessForProvider)
											.put("grupoDeArticulos", product[0])
											.put("nombreGrupoDeArticulos", processedTag)
											.put("marcas", marcasYeah))
									;
							}
						}
				}
			}
		}catch(ArrayIndexOutOfBoundsException e) {
			errors.put("Missing params, mandatory params are (in order): <SupplierId> <TemplateId> <Business>, optional: <dictionaryProxy>");
			logE(e);
		}catch(Exception e) {
			errors.put(e.getMessage());
			logE(e);
		}
		try{
			log("Al menos un yeah: " + alMenosUnYeah);
			log("Algún empty: " + algunEmpty);
			if(alMenosUnYeah) {
				org.json.JSONArray holi = new org.json.JSONArray();
				for(int a=0; a<elements.length(); a++) {
					if(elements.getJSONObject(a).getJSONArray("marcas").length() > 0) {
						holi.put(elements.getJSONObject(a));
					}
				}
				elements = holi;
				algunEmpty = false;
			}

			if(errors.length() > 0) {
				generalResponse.put("errors", errors);
			}
			log("Algun empty: " + algunEmpty);
			if(algunEmpty || elements.length() == 0) {
				log("...");
				if(!"MKP".equals(business) && Boolean.parseBoolean(PropertiesManager.get("p360.contingency.use_ia"))) {
					generalResponse.put("reglas", new org.json.JSONArray());
					generalResponse.put("idProveedor", proveedor);
					generalResponse.put("idPlantilla", plantilla);
					generalResponse.put("plantilla", getTemplateName(plantilla, workshop));
					generalResponse.put("todasLasMarcas", toJSONArray( todasLasMarcas(workshop) ));
					log("Done: " + workshop.formatTime(System.currentTimeMillis() - init));
					return generalResponse.toString();
				}
			}else if("MKP".equals(business) && Boolean.parseBoolean(PropertiesManager.get("p360.contingency.use_ia"))) {
			}else {
				generalResponse.put("reglas", elements);
			}
		}catch(org.json.JSONException e){}
		String outputJSON = generalResponse.toString();
		generalResponse = new org.json.JSONObject();
		elements = new org.json.JSONArray();
		errors = new org.json.JSONArray();
		log("Done: " + workshop.formatTime(System.currentTimeMillis() - init));
		return outputJSON;
	}
	
	private void collectProhibitedBrandsAndItemGroups(java.util.LinkedList<String> itemGroups, java.util.LinkedList<String> brands) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'BannedElementsForMarketplacePublication'");
		qp.put("fields", "LookupValue.Code"
				+ ",LookupValueReference.LookupValues('MATKLLOV')->LookupValueLang.Name(es)"
				+ ",LookupValueReference.LookupValues('ZCOMALOV')->LookupValueLang.Name(es)");
		rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String item = values.getString(0);
			org.json.JSONArray items = null;
			if("ItemGroups".equals(item)) {
				items = values.getJSONArray(1);
				for(int i=0; i<items.length(); i++) {
					itemGroups.addLast(items.getString(i));
				}
			}else if("Brands".equals(item)) {
				items = values.getJSONArray(2);
				for(int i=0; i<items.length(); i++) {
					brands.addLast(items.getString(i));
				}
			}
		});
	}
	
	public static void main(String[] args) {
		BrandsGroupOfArticleByTemplateBusinessProvider b = new BrandsGroupOfArticleByTemplateBusinessProvider();
		b.collectProhibitedBrandsAndItemGroups(new java.util.LinkedList<>(), new java.util.LinkedList<>());
	}
	
	private org.json.JSONArray setToArray(java.util.Set<String> data){
		org.json.JSONArray values = new org.json.JSONArray();
		for(String d : data) {
			values.put(d);
		}
		return values;
	}
	/*
	private org.json.JSONArray gpaWithBrands(String proveedor, String business, RESTWorkshop workshop){
		String processedTag = null;
		org.json.JSONArray elements = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "MATKLLOV" + ("SBB".equals(business) ? "_S4H" : ""));
		qp.put("query", "not LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "BRAND_IDLOV_S4H" : "ZCOMALOV") + "') is empty");
		qp.put("fields", 
				  "LookupValue.Code"
				+ ",LookupValueLang.Name(es)"
				+ ",LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "BRAND_IDLOV_S4H" : "ZCOMALOV") + "')->LookupValueLang.Name(es)"
			);
		qp.put("pageSize", "4000");

		org.json.JSONObject r = null;
		org.json.JSONArray rws = null;
		org.json.JSONArray vls = null;

		int ci = 0;
		int tz = 0;

		org.json.JSONArray businessForProvider = getBusinessForProvider(proveedor, workshop);
		log("Business for provider: "  + businessForProvider);
		do {
			qp.put("startIndex", String.valueOf(ci));
			r = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(r != null) {	
				tz = r.getInt("totalSize");
				rws = r.getJSONArray("rows");
				for(int i=0; i<rws.length(); i++) {
					vls = rws.getJSONObject(i).getJSONArray("values");
					if(!vls.getString(1).equals("")) {
						processedTag = adapt("".equals(vls.getString(1)) ? vls.getString(0) : vls.getString(1));
						if(processedTag != null) {
							elements
								.put(
									new org.json.JSONObject()
										.put("negocios", businessForProvider)
										.put("grupoDeArticulos", vls.getString(0))
										.put("nombreGrupoDeArticulos", processedTag)
										.put("marcas", vls.getJSONArray(2)))
							;
						}
					}
				}
				ci+=r.getInt("pageSize"); 
			}else { 
				log(workshop.getRawResponse()); logE(workshop.getException()); 
			}
			log("gpas " + ci + "/" + tz);
		}while(ci < tz);
		ci = 0;
		return elements;
	}
	*/
	private String adapt(String value, String business, String label) {
		if(value == null)
			return null;
		String code = value.length() > 5 ? value.substring(5) : value;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		String path = null;
		log("Looking for: " + (path = "/object/LookupValue/'" + code + "'@'" + ("SBB".equals(business) ? (value.length() > 5 ? "SB_0002LOV" : "MATKLLOV_S4H") : (value.length() > 5 ? "PE000LOV" : "MATKLLOV") ) + "'"));
		org.json.JSONObject response = rw.getRw().makeRequest("GET", path, qp, null);
		if(response != null) {
			org.json.JSONArray lang = response.getJSONObject("_data").getJSONArray("lang");
			for(int i=0; i<lang.length(); i++) {
				org.json.JSONObject j = lang.getJSONObject(i);
				if("esl".equals(j.getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
					return j.getString("name");
				}
			}
		}else {
			log("ERROR: " + rw.getRw().getRawResponse());
		}
		value = value.trim().replaceAll("/$", "").trim();
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^/]+)$").matcher(label);
		if(m.find()) {
			return m.group().replaceAll("^[^A-Za-z0-9]+", "").trim();
		}else {
			return null;
		}
	}
	
//	private String adaptaEtiqueta(String valor) {
//		StringBuilder sb = new StringBuilder();
//		String[] pieces = valor.split(" - ");
//		if(pieces != null && pieces.length > 1) {
//			if(pieces[0].length() < 5) {
//				sb.append(valor);
//			}else {
//				sb.append(valor.subSequence(0, 5));
//				if(pieces[0].length() > 5) {
//					java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<=\\/)(.+)$").matcher(pieces[1]);
//					if(m.find()) {
//						sb.append(" - ");
//						sb.append(m.group(2));
//					}else {
//						log("No match found on string: " + pieces[1]);
//					}
//				}
//			}
//		}else if(pieces != null) {
//			sb.append(valor);
//		}
//		return sb.toString();
//	}

	private org.json.JSONArray getBusinessForProvider(String provider, RESTWorkshop rw){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "Party");
		qp.put("query", "LookupValue.Code equals \"" + provider + "\"");
		qp.put("fields", "LookupValueReference.LookupValues('BusinessQualified')->LookupValueLang.Name(es)");

		org.json.JSONObject response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);

		return response != null && response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(0) : null;
	}

	private String getTemplateName(String template, RESTWorkshop rw) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "PPH_L4_Templates");
		qp.put("query", "LookupValue.Code equals \"" + template + "\"");
		qp.put("fields", "LookupValueLang.Name(es)");

		org.json.JSONObject response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);

		return response != null && response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : null;
	}

	private org.json.JSONArray toJSONArray(java.util.LinkedList<String[]> elepas){
		org.json.JSONArray todas = new org.json.JSONArray();
		for(String[] hola : elepas) {
			todas.put(hola[1]);
		}
		return todas;
	}
	
	private java.util.LinkedList<String[]> todasLasMarcas(RESTWorkshop workshop){
		java.util.LinkedList<String[]> marcas = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("lookup", "'ZCOMALOV'");
		qp.put("pageSize", "3000");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values;
		int currentIndex = 0;
		int totalSize = 0;
		log("Vamos por todas las marcas");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					marcas.addLast(new String[] {values.getString(0), values.getString(1)});
				}
			}else {
				log("nel: " + workshop.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		log("Las marcas tenemos: " + marcas.size());
		return marcas;
	}
	
	private java.util.LinkedList<String[]> productsFromRealGPA(String gpa, String business, RESTWorkshop workshop){
		java.util.LinkedList<String[]> productos = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "SBB".equals(business) ? "'ItemGroupConProductoSBBLOV'" : "'ItemGroupProductLOV'");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.Code wildcard \"" + gpa + "%\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				productos.addLast(new String[] { values.getString(0), values.getString(1) });
			}
		}
		return productos;
	}
	
	private java.util.LinkedList<String> productsForTemplate(String template, String business, RESTWorkshop workshop){
		java.util.LinkedList<String> productos = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'PPH_L4_Templates'");
		qp.put("fields", "SBB".equals(business) ? "'ItemGroupConProductoSBBLOV'" : "'ItemGroupProductLOV'");
		qp.put("query", "LookupValue.Code wildcard \"" + template + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values").getJSONArray(0);
				for(int j=0; j<values.length(); j++)
					productos.addLast(values.getString(j));
			}
		}
		return productos;
	}

	private java.util.LinkedList<String[]> productsFromGPA(String template, String itemGroup, String business, RESTWorkshop workshop){
		java.util.LinkedList<String[]> productos = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		String fltr = null;
		qp.put("lookup", "'PPH_L4_Templates'");
		qp.put("fields", fltr = "LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "ItemGroupConProductoSBBLOV" : "ItemGroupProductLOV") + "')->LookupValue.Code,LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "ItemGroupConProductoSBBLOV" : "ItemGroupProductLOV") + "')->LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.Code equals \"" + template + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray miniValues = null;
		response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			log("Using filter: " + fltr + ", got rows: " + rows + " <::> " + workshop.getBaseUrl());
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				miniValues = values.getJSONArray(0);
				for(int j=0; j<miniValues.length(); j++) {
					if(!"".equals(miniValues.getString(j)) && miniValues.getString(j).startsWith(itemGroup)) {
						productos.addLast(new String[] { miniValues.getString(j), values.getJSONArray(1).getString(j) });
					}else if(!"".equals(miniValues.getString(j))) {
						log("*** " + miniValues.getString(j) + " starts with: " + itemGroup);
					}
				}
			}
		}
		return productos;
		/*
		java.util.LinkedList<String[]> products = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query",  "StructureGroup.Identifier wildcard \"" + gpa + ("SBB".equals(business) ? "%-L5SH\"" : "%-L5ECC\""));
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es)");
		qp.put("structure", "SBB".equals(business) ? "CommercialS4H" : "CommercialECC");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values;
		response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			log("Got GPAs for: " + gpa + " in " + business + ": " + rows);
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				products.addLast(new String[] {values.getString(0).replaceAll("-.+", ""), values.getString(1).replaceAll(".+ - ", "")});
			}
		}else {
			log("Problem querying products for " + gpa + " in " + business + ", " + workshop.getRawResponse());
		}
		return products;
		*/
	}

	private org.json.JSONArray consigeMarcasGPA(String gpa, RESTWorkshop workshop){
		org.json.JSONArray gpas = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValueReference.LookupValues('ZCOMALOV')->LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.Code equals \"" + gpa + "\"");
		qp.put("lookup", "MATKLLOV");
		qp.put("metaData", "true");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray subValues = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				values = rows.getJSONObject(0).getJSONArray("values");
				subValues = values.getJSONArray(0);
				for(int i=0; i<subValues.length(); i++) {
					gpas.put(subValues.getString(i));
				}
			}
		}else {
//			System.out.println("Problem " + workshop.getRawResponse());
		}
		return gpas;
	}

	private org.json.JSONArray consigeMarcasGPA(String gpa, java.util.Set<String> marcasProveedor, String business, RESTWorkshop workshop){
		org.json.JSONArray gpas = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "BRAND_IDLOV_S4H" : "ZCOMALOV") + "')->LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.Code equals \"" + gpa + "\"");
		qp.put("lookup", "'" + ("SBB".equals(business) ? "MATKLLOV_S4H": "MATKLLOV") + "'");
		qp.put("metaData", "true");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray subValues = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				values = rows.getJSONObject(0).getJSONArray("values");
				subValues = values.getJSONArray(0);
				for(int i=0; i<subValues.length(); i++) {
					if(marcasProveedor.contains(subValues.getString(i))) {
						gpas.put(subValues.getString(i));
					}
				}
			}
		}else {
//			System.out.println("Problem " + workshop.getRawResponse());
		}
		return gpas;
	}

	private java.util.Set<String> consigeMarcasProveedor(String proveedor, String business, RESTWorkshop workshop){
		java.util.Set<String> gpas = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValueReference.LookupValues('" + ("SBB".equals(business) ? "BRAND_IDLOV_S4H" : "ZCOMALOV") + "')->LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.Code equals \"" + proveedor + "\"");
		qp.put("lookup", "Party");
		qp.put("metaData", "true");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray subValues = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				values = rows.getJSONObject(0).getJSONArray("values");
				subValues = values.getJSONArray(0);
				for(int i=0; i<subValues.length(); i++) {
					gpas.add(subValues.getString(i));
				}
			}
		}else {
//			System.out.println("Problem " + workshop.getRawResponse());
		}
		return gpas;
	}

	private java.util.LinkedList<String> toLinkedLists(org.json.JSONArray values){
		java.util.LinkedList<String> list = new java.util.LinkedList<>();
		for(int i=0; i<values.length(); i++) {
			list.addLast(values.getString(i));
		}
		return list;
	}

	public String run(String[] args) {
		org.json.JSONObject generalResponse = new org.json.JSONObject();
		boolean firstRow = true;
		org.json.JSONArray elements = new org.json.JSONArray();
		org.json.JSONArray errors = new org.json.JSONArray();
		org.json.JSONObject element = null;
		try {
			String proveedor = args[0];
			String plantilla = args[1];
			String negocio = args[2];
			String dictionaryProxy = null;
			if(args.length > 3) {
				dictionaryProxy = args[3];
			}
			// http://172.18.237.162:1512/rest/V2.0
			log("Working with: " + java.util.Arrays.asList(args));
			String url = "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/GroupOfArticleTemplateValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode( (dictionaryProxy == null || "".equals(dictionaryProxy) ? "24004" : dictionaryProxy), "UTF-8")
			+ "&query="
					+ java.net.URLEncoder.encode(
							"GroupOfArticleTemplateValue.System contains \"" + negocio.toUpperCase() + "\" and GroupOfArticleTemplateValue.Template->LookupValue.Code equals \"" + plantilla + "\" and GroupOfArticleTemplateValue.Supplier contains \"" + proveedor + "\"", "UTF-8") + ""
					+ "&fields=" + java.net.URLEncoder.encode(
							"GroupOfArticleTemplateValue.Supplier,"
							+ "GroupOfArticleTemplateValue.System,"
							+ "GroupOfArticleTemplateValue.Template->LookupValue.Code,"
							+ "GroupOfArticleTemplateValue.Template->LookupValueLang.Name(es),"
							+ "GroupOfArticleTemplateValue.GroupOfArticle,"
							+ "GroupOfArticleTemplateValue.Brand,"
							+ "GroupOfArticleTemplateValue.GroupOfArticleName"
						, "UTF-8") + "&metaData=false";
//			log("Requesting: " + url);
			String rawResponse = null;
			String encoded = "cmVzdDpoZWlsZXI=";
			RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded);
			rawResponse = rc.getRequest("GET", url, null);
//			log("Got response: " + rawResponse);
			org.json.JSONObject response = null;
			response = new org.json.JSONObject(rawResponse);
			org.json.JSONArray rows = null;
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				for(int a = 0; a<rows.length(); a++) {
					org.json.JSONArray values = null;
					values = rows.getJSONObject(a).getJSONArray("values");
					// 'http://172.18.237.162:1512/rest/V2.0/list/GroupOfArticleTemplateValue/bySearch?dictionaryProxy='||iif( isnull(DictionaryProxy) or '' = DictionaryProxy, '24004', DictionaryProxy )||'&query='||URL_PATH_ELEMENT_ENCODE('GroupOfArticleTemplateValue.System contains "'||UPPER(Negocio)||'" and GroupOfArticleTemplateValue.Template equals "'||Plantilla||'" and GroupOfArticleTemplateValue.Supplier equals "'||Proveedor||'"')||'&fields=GroupOfArticleTemplateValue.Supplier,GroupOfArticleTemplateValue.System,GroupOfArticleTemplateValue.Template->LookupValue.Code,GroupOfArticleTemplateValue.Template->LookupValueLang.Name(es),GroupOfArticleTemplateValue.GroupOfArticle,GroupOfArticleTemplateValue.Brand,GroupOfArticleTemplateValue.GroupOfArticleName&metaData=true&pageSize='
					String inputJSON = new org.json.JSONObject()
							.put("idProveedor", values.getString(0))
							.put("negocios", values.get(1))
							.put("idPlantilla", values.get(2))
							.put("plantilla", values.get(3))
							.put("grupoDeArticulos", values.get(4))
							.put("nombreGrupoDeArticulos", values.get(6))
							.put("marcas", values.get(5))
							.toString();
					try{
						element = new org.json.JSONObject(inputJSON);
						if(firstRow){
							generalResponse.put("idProveedor", element.getString("idProveedor"));
							generalResponse.put("idPlantilla", element.getString("idPlantilla"));
							generalResponse.put("plantilla", element.getString("plantilla"));
							firstRow = false;
						}
						elements.put(new org.json.JSONObject().put("negocios", element.getJSONArray("negocios")).put("grupoDeArticulos", element.getString("grupoDeArticulos")).put("nombreGrupoDeArticulos", element.getString("nombreGrupoDeArticulos")).put("marcas", element.getJSONArray("marcas")));
						log("Came element: " + element);
					}catch(org.json.JSONException e){
						logE(e);
						errors.put(e.getMessage());
//						System.out.println(new org.json.JSONObject().put("response", "Invaid input. ->" + (inputJSON == null ? "" : inputJSON) + "<-"));
					}
				}
			}
		}catch(ArrayIndexOutOfBoundsException e) {
			errors.put("Missing params, mandatory params are (in order): <SupplierId> <TemplateId> <Business>, optional: <dictionaryProxy>");
			logE(e);
		}catch(Exception e) {
			errors.put(e.getMessage());
			logE(e);
		}
		try{
			if(elements.length() > 0) {
				generalResponse.put("reglas", elements);
			}
			if(errors.length() > 0) {
				generalResponse.put("errors", errors);
			}
		}catch(org.json.JSONException e){}
		String outputJSON = generalResponse.toString();
//		System.out.println(outputJSON);
		generalResponse = new org.json.JSONObject();
		elements = new org.json.JSONArray();
		firstRow = true;
		errors = new org.json.JSONArray();
		return outputJSON;
	}

	
	private static final Logger LOGGER = Logger.getLogger(BrandsGroupOfArticleByTemplateBusinessProvider.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/pep.%g.log", 15 * 1024 * 1024, 10, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    java.time.LocalDateTime dateTime =
                        java.time.Instant.ofEpochMilli(record.getMillis())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();

                    String timestamp = dateTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    return "[" + timestamp + "]  " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }
    
	private static void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream("../logs/pep.log", true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//					+ "]  " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	private static void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/pep.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
