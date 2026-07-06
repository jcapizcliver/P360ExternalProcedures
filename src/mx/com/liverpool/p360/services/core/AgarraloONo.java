package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class AgarraloONo {
	
	private final RESTWrapper rw = new RESTWrapper();

	public void checale(String externalId, String baseUrl) throws JSONException, ServiceUnavailableException {
		checale(externalId, baseUrl, new java.util.TreeSet<>());
	}
	
	public void checale(String externalId, String baseUrl, java.util.Set<String> losesos) throws JSONException, ServiceUnavailableException {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeIds", "true");
		org.json.JSONObject response = null;
		String takeNoTakeReason = ""; 
		String take = "NO TOMAR"; 
		String stylistWorld = null;
		String tipoDeToma = null;
		String business = null;
		String itemGroup = null;
		String section = null;
		String itemGroupS4H = null;
		String brandName = null;
		String brandIdS4H = null;
		String supplierId = null;
		String sku = null;
		String assignTakeNoTake = null;
		String brand = null;
		String seccion = null;
		String template = null;
		org.json.JSONArray productCrs = new org.json.JSONArray();
		DataRequestor dr = new DataRequestor();
		String resp = dr.getProductData(new org.json.JSONArray().put(externalId));
		org.json.JSONObject j = new org.json.JSONObject(resp);
		org.json.JSONArray items = j.getJSONArray("items");
		org.json.JSONObject dta = items.getJSONObject(0);
		section =  dta.getString("Section");
		itemGroup = dta.getString("ItemGroup");
		itemGroupS4H = dta.getString("ItemGroupS4H");
		brandName = dta.getString("BrandName");
		brandIdS4H = dta.getString("BRAND_ID_S4H");
		business = dta.getString("Business");
		supplierId = dta.getString("SupplierID");
		sku = dta.getString("SKU");
		template = dta.getString("Template");
		assignTakeNoTake = dta.getString("AssignTakeNoTake");
		brand = brandName != null && !brandName.isEmpty() ? brandName : brandIdS4H;
		itemGroup = itemGroup != null && !itemGroup.isEmpty() ? itemGroup : itemGroupS4H;
		seccion = section;
			log("Got data to calculate take no take."); 
			log("Gonna checkit...");
			String takeNoTakeReasonHola = checkForoException( section, itemGroup, itemGroupS4H, brandName, brandIdS4H
					, business, supplierId, sku, baseUrl);
			log("Would have been foro exception: " + takeNoTakeReasonHola);
			if( new PublicationExceptions().isException(rw.getRw(), "'" + externalId + "'@1") ) {
				takeNoTakeReason = "El producto se encuentra bajo excepción de catalogación."; 
				log("Product is a publication exception.");
			} else {
				log("Que onda bro...");
			}
			stylistWorld = queryLookupValue(seccion + brand, "FTNT_StylistWorld", baseUrl); 
			tipoDeToma = queryLookupValue(seccion + itemGroup, "FTNT_TipoDeToma", baseUrl); 
			log("Sección: " + seccion); 
			log("Brand: " + brand); 
			log("Item Group: " + itemGroup); 
			log("Tipo de Toma: " + tipoDeToma); 
			log("Stylist World: " + stylistWorld); 
			if (tipoDeToma != null) {
				addCharacteristicRecord("TipoDeToma", tipoDeToma, false, productCrs);
			}
			if (stylistWorld != null) {
				addCharacteristicRecord("StylistWorld", stylistWorld, false, productCrs);
			}
			if (takeNoTakeReason != null && !"".equals(takeNoTakeReason)) 
			{
				addCharacteristicRecord("AssignTakeNoTakeReason", takeNoTakeReason, false, productCrs); 
				addCharacteristicRecord("AssignTakeNoTake", take, false, productCrs); 
				noTakeToVariants(externalId, takeNoTakeReason, stylistWorld, tipoDeToma , baseUrl);
				log("Assigned take no take reason to product level: " + productCrs); 
			} else {
				log("Product was not an exception for take/no take."); 
				if ("SBB".equals(business)) 
				{
					log("Business is Suburbia, brand: " + brandIdS4H + ", itemGroup: " + itemGroupS4H);  
				} else {
					log("Product is not Suburbia: " + business + ", brand: " + brand + ", itemGroup: " + itemGroup);   
				}
				if (collectVariants(externalId, itemGroup, brand, business, stylistWorld, tipoDeToma, template, baseUrl, losesos)) {
					addCharacteristicRecord("AssignTakeNoTakeReason", takeNoTakeReason = "", false, productCrs);  
					take = "TOMAR"; 
					log("°°°°°°°°°°°" + externalId + "°°°°°°°°°°°°");  
				} else {
					addCharacteristicRecord("AssignTakeNoTakeReason", takeNoTakeReason = "Talla no configurada", false, productCrs);
				}
				log("section: " + seccion + ", stylist world (" + seccion + brand + "): " + stylistWorld   
						+ ", tipo de toma (" 
						+ seccion + itemGroup + "): " + tipoDeToma + ", template: " + template);  
			}
			addCharacteristicRecord("AssignTakeNoTake", take, false, productCrs);
			String a = template != null 
					&& !"".equals(template) 
					? checkLookup(template, "PlantillasTomaDeVideo", baseUrl) 
							? "NO TOMAR".equals(take) ? "false" : "true"   
							: "false"
					: "false";
			addCharacteristicRecord("AssignTakeNoTakeVideo", a, false, productCrs);
			log("Added takeNoTake: " + take + ", and takeNoTakeVideo: "  
					+ (template != null && !"".equals(template) ? checkLookup(template, "PlantillasTomaDeVideo", baseUrl)  
							? "NO TOMAR".equals(take) ? "false" : "true"   
							: "false" : "false"));  
			if("".equals(assignTakeNoTake)) {
				response = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + externalId + "'@'MASTER'", qp,   
						new org.json.JSONObject().put("_characteristicRecords", productCrs) 
								.toString());
				if (response != null) {
					log("Producto actualizado para TomarNoTomar. " + response);
				} else {
					log("Error en actualización de producto para tomar no tomar: " + rw.getRw().getRawResponse()); 
				}
			}else {
				log("Already calculated.");
			}
	}

	private void noTakeToVariants(String externalId, String takeNoTakeReason, String stylistWorld, String tipoDeToma, String baseUrl) throws org.json.JSONException {
		log("Entering to variants."); 
		java.util.LinkedList<String> varIds = new java.util.LinkedList<>();
		java.util.Map<String, org.json.JSONArray> mapaCar = new java.util.TreeMap<>();
		new java.util.TreeMap<>();
		org.json.JSONArray crs = null;
		new java.util.TreeMap<>();
		new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		DataRequestor dr = new DataRequestor();
		java.util.Set<String> variants = dr.getVariants(externalId);
		org.json.JSONArray items = new org.json.JSONArray();
		org.json.JSONObject response = null;
		if(variants == null || variants.isEmpty()) {
			return;
		}
		for(String va : variants) {
			items.put(va);
		}
		String rp = dr.getArticleData(items);
		try {
			response = new org.json.JSONObject(rp);
		}catch(org.json.JSONException e) {
			e.printStackTrace();
		}
		
		org.json.JSONObject item = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		String articleId = null;
		String admissionDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") 
				.format(new java.util.Date());
		if (response != null) {
			rows = response.getJSONArray("items");
			for (int i = 0; i < rows.length(); i++) {
				item = rows.getJSONObject(i);
				articleId = items.getString(i);
				values = new org.json.JSONArray();
				values.put(articleId);
				values.put(new org.json.JSONArray().put( item.getString("ColoursLiverpoolAtt")));
				values.put(new org.json.JSONArray().put( item.getString("TamanoUnico")));
				values.put(new org.json.JSONArray().put( item.getString("ProductImage")));
				values.put(new org.json.JSONArray().put( item.getString("AssignTakeNoTake")));
				articleId = values.getString(0);
				crs = mapaCar.get(articleId);
				if (crs == null) {
					crs = new org.json.JSONArray();
					mapaCar.put(articleId, crs);
				}
				varIds.addLast(articleId);
			}
		}
		for (String varid : varIds) {
			crs = mapaCar.get(varid);
			try {
				addCharacteristicRecord("AssignTakeNoTake", "NO TOMAR", false, crs); 
				addCharacteristicRecord("AdmissionDate", admissionDate, false, crs);
				addCharacteristicRecord("StylistWorld", stylistWorld, false, crs);
				addCharacteristicRecord("TipoDeToma", tipoDeToma, false, crs);
				addCharacteristicRecord("AssignTakeNoTakeReason", takeNoTakeReason, false, crs);
				response = rw.getRw().makeRequest("PUT", "/object/Article/'" + varid + "'@'MASTER'", qp,   
						new org.json.JSONObject().put("_characteristicRecords", crs) 
								.toString());
				log("Variant updated: " + (response == null ? rw.getRw().getRawResponse() : response)); 
			} catch (org.json.JSONException e) {
				logE(e);
			}
		}
	}

	private boolean collectVariants(String externalId, String itemGroup, String marca, String business,
			String stylistWorld, String tipoDeToma, String template, String baseUrl, java.util.Set<String> losesos) throws org.json.JSONException {
		log("Entering to variants.");
		java.util.Map<String, org.json.JSONArray> artValues = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONArray> mapaCar = new java.util.TreeMap<>();
		org.json.JSONArray crs = null;
		java.util.Map<String, java.util.LinkedList<String>> articulosPorColor = new java.util.TreeMap<>();
		java.util.LinkedList<String> lst = null;
		java.util.Map<String, Integer> prioridades = null;
		java.util.LinkedList<String> variantesDelMismoColor = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.LinkedList<java.util.Map.Entry<String, Integer>> entries = null;
		java.util.Set<String> toWrite = new java.util.TreeSet<>();
		DataRequestor dr = new DataRequestor();
		java.util.Set<String> variants = dr.getVariants(externalId);
		org.json.JSONArray items = new org.json.JSONArray();
		org.json.JSONObject response = null;
		if(variants == null || variants.isEmpty()) {
			return false;
		}
		for(String va : variants) {
			items.put(va);
		}
		String rp = dr.getArticleData(items);
		try {
			response = new org.json.JSONObject(rp);
		}catch(org.json.JSONException e) {
			e.printStackTrace();
		}
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray cv = null;
		String color = null;
		String talla = null;
		String tnt = null;
		String articleId = null;
		String prioridad = null;
		String admissionDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") 
				.format(new java.util.Date());
		log("Assigning: " + admissionDate + " as admission date.");
		java.util.List<String> losTomar = new java.util.ArrayList<>();
		java.util.List<String> losNoMeLosPidieron = new java.util.ArrayList<>();
		java.util.List<String> losSinConfiguracion = new java.util.ArrayList<>();
		java.util.List<String> losTeLoGanaron = new java.util.ArrayList<>();
		boolean atLeast = false;
		boolean colorTocado = false;
		java.util.List<String> losConImagen = new java.util.ArrayList<>();
		org.json.JSONObject item = null;
		if (response != null) {
			rows = response.getJSONArray("items");
			for (int i = 0; i < rows.length(); i++) {
				item = rows.getJSONObject(i);
				if(!"".equals(item.getString("ProductImage"))) {
					losConImagen.add(articleId);
				}
				articleId = items.getString(i);
				values = new org.json.JSONArray();
				values.put(articleId);
				values.put(new org.json.JSONArray().put( item.getString("ColoursLiverpoolAtt")) );
				values.put(new org.json.JSONArray().put( item.getString("TamanoUnico")) );
				values.put(new org.json.JSONArray().put( item.getString("ProductImage")) );
				values.put(new org.json.JSONArray().put( item.getString("AssignTakeNoTake")) );
				log("---->" + values);
				artValues.put(articleId, values);
				cv = values.getJSONArray(1);
				color = cv.getString(0);
				cv = values.getJSONArray(2);
				crs = mapaCar.get(articleId);
				if (crs == null) {
					crs = new org.json.JSONArray();
					mapaCar.put(articleId, crs);
				}
				lst = articulosPorColor.get(color);
				if (lst == null) {
					lst = new java.util.LinkedList<>();
					articulosPorColor.put(color, lst);
				}
				lst.addLast(articleId);
			}
			String a =
					template != null 
							&& !"".equals(template) 
									? checkLookup(template, "PlantillasTomaDeVideo", baseUrl) 
											? "true"   
											: "false" 
									: "false";
			for (java.util.Map.Entry<String, java.util.LinkedList<String>> colorEntry : articulosPorColor.entrySet()) {
				log("For color: " + colorEntry.getKey()); 
				variantesDelMismoColor = colorEntry.getValue();
				if (variantesDelMismoColor != null && !variantesDelMismoColor.isEmpty()) {
					prioridades = new java.util.TreeMap<>();
					for (String varianteDelColor : variantesDelMismoColor) {
						values = artValues.get(varianteDelColor);
						tnt = values.getJSONArray(4).getString(0);
						if("TOMAR".equals(tnt) || "TOMADO".equals(tnt)) {
							losTomar.add(varianteDelColor);
							colorTocado = true;
						}else {
							toWrite.add(varianteDelColor);
						}
					}
					for (String varianteDelColor : variantesDelMismoColor) {
						values = artValues.get(varianteDelColor);
						color = values.getJSONArray(1).getString(0);
						talla = values.getJSONArray(2).getString(0);
						tnt = values.getJSONArray(4).getString(0);
						prioridad = "SBB".equals(business) ? queryLookupValue(itemGroup + marca + talla, "FTNT_Prioridad_Tallas_SBB", baseUrl) 
								: queryLookupValue(itemGroup + talla, "FTNT_Prioridad_Tallas_LVP", baseUrl);
						if(prioridad == null) {
							losSinConfiguracion.add(varianteDelColor);
						}else if(!losesos.contains(varianteDelColor)) {
							losNoMeLosPidieron.add(varianteDelColor);
						}else if(colorTocado) {
							losTeLoGanaron.add(varianteDelColor);
						}
						prioridades.put(varianteDelColor, 
								prioridad == null ? null 
										: !losesos.isEmpty() && !losesos.contains(varianteDelColor) ? null 
												: colorTocado ? null 
														: losConImagen.contains(varianteDelColor) ? null 
																: losTomar.contains(varianteDelColor) ? null 
																		: Integer.parseInt(prioridad) );
						if(!losesos.isEmpty() && !losesos.contains(varianteDelColor)) {
							log("BEP");
							log("Tenemos filtro y la variante del color no está en ese set, esta es la variante que no vino: " + varianteDelColor);
							log("Esta es la lista de las variantes: " + losesos);
							log("/BEP");
						}
						log("\tFor variant: " + varianteDelColor); 
						log("\tColor: " + color); 
						log("\tPrioridad: " + prioridad); 
						log("\tNegocio: " + business); 
						log("\tItemGroup: " + itemGroup); 
						log("\tMarca: " + marca); 
					}
					
					colorTocado = false;
					entries = new java.util.LinkedList<>(prioridades.entrySet());
					java.util.Collections.sort(entries, (o1, o2) -> o1.getValue() == null ? 1
							: o2.getValue() == null ? -1 : o1.getValue().compareTo(o2.getValue()));
					java.util.Map.Entry<String, Integer> winner = entries.removeFirst();
					new java.util.LinkedList<>();
					if (winner.getValue() == null) {
						entries.addFirst(winner);
						for (String varianteDelMismoColor : variantesDelMismoColor) {
							if(!losTomar.contains(varianteDelMismoColor)){
								atLeast = true;
							}
							if(toWrite.contains(varianteDelMismoColor)) {
								crs = mapaCar.get(varianteDelMismoColor);
								addCharacteristicRecord("AssignTakeNoTake", losTomar.contains(varianteDelMismoColor) ? "TOMADO" : "NO TOMAR", false, crs);  
								addCharacteristicRecord("AdmissionDate", admissionDate, false, crs); 
								addCharacteristicRecord("StylistWorld", stylistWorld, false, crs); 
								addCharacteristicRecord("TipoDeToma", tipoDeToma, false, crs); 
								addCharacteristicRecord("AssignTakeNoTakeReason",
											losSinConfiguracion.contains(varianteDelMismoColor) ? "Talla no configurada"
													: losNoMeLosPidieron.contains(varianteDelMismoColor) ? "Otra talla del mismo color es tomar"
															: losTeLoGanaron.contains(varianteDelMismoColor) ? "Otra talla del mismo color es tomar"
																	: losTomar.contains(varianteDelMismoColor) ? "Recibido previamente o en proceso"
																			: losConImagen.contains(varianteDelMismoColor) ? "Tiene imagen principal" 
																					: "Otra talla del mismo color es tomar"
										, false, crs);  
								response = rw.getRw().makeRequest("PUT", "/object/Article/'" + varianteDelMismoColor + "'@'MASTER'",   
										qp, new org.json.JSONObject().put("_characteristicRecords", crs) 
												.toString());
								log(response == null ? "ERR (affecting winner variant): " + rw.getRw().getRawResponse() 
										: "Variant affected (" + colorEntry.getKey() + "): " + response + "<::>"   
												+ crs);
							}else {
								crs = mapaCar.get(varianteDelMismoColor);
								addCharacteristicRecord("AssignTakeNoTake", losTomar.contains(varianteDelMismoColor) ? "TOMADO" : "NO TOMAR", false, crs);  
								addCharacteristicRecord("AdmissionDate", admissionDate, false, crs); 
								addCharacteristicRecord("StylistWorld", stylistWorld, false, crs); 
								addCharacteristicRecord("TipoDeToma", tipoDeToma, false, crs); 
								addCharacteristicRecord("AssignTakeNoTakeReason",
											losSinConfiguracion.contains(varianteDelMismoColor) ? "Talla no configurada"
													: losTomar.contains(varianteDelMismoColor) ? "Recibido previamente o en proceso"
														: losConImagen.contains(varianteDelMismoColor) ? "Tiene imagen principal" 
															: losNoMeLosPidieron.contains(varianteDelMismoColor) ? "Otra talla del mismo color es tomar"
																	: losTeLoGanaron.contains(varianteDelMismoColor) ? "Otra talla del mismo color es tomar"
																						: "Otra talla del mismo color es tomar"
										, false, crs);  
								response = rw.getRw().makeRequest("PUT", "/object/Article/'" + varianteDelMismoColor + "'@'MASTER'",   
										qp, new org.json.JSONObject().put("_characteristicRecords", crs) 
												.toString());
								log(response == null ? "ERR (affecting winner variant): " + rw.getRw().getRawResponse() 
										: "Variant affected (" + colorEntry.getKey() + "): " + response + "<::>"   
												+ crs);
//								log("Ignorando variante porque ya había sido calculada.");
							}
//							}else {
//								
//							}
						}
					} else {
						if(toWrite.contains(winner.getKey())) {
							atLeast = true;
							crs = mapaCar.get(winner.getKey());
							addCharacteristicRecord("AssignTakeNoTake", "TOMAR", false, crs);  
							addCharacteristicRecord("AdmissionDate", admissionDate, false, crs);
							addCharacteristicRecord("StylistWorld", stylistWorld, false, crs); 
							addCharacteristicRecord("TipoDeToma", tipoDeToma, false, crs); 
							addCharacteristicRecord("AssignTakeNoTakeVideo", a, false, crs); 
							addCharacteristicRecord("AssignTakeNoTakeReason", "", false, crs);  
							addCharacteristicRecord("PrioridadDeTalla", winner.getValue(), false, crs); 
							response = rw.getRw().makeRequest("PUT", "/object/Article/'" + winner.getKey() + "'@'MASTER'", qp,   
									new org.json.JSONObject().put("_characteristicRecords", crs) 
											.toString());
							log(response == null ? "ERR (affecting winner variant): " + rw.getRw().getRawResponse() 
									: "Winner variant affected (" + colorEntry.getKey() + "): " + response + "<;;>" + crs);  
						}else {
							log("Ya había sido calculado el que salió winner: " + winner.getKey());
						}
						for (java.util.Map.Entry<String, Integer> varianteDelMismoColor : entries) {
							if(toWrite.contains(varianteDelMismoColor.getKey())) {
								log("setting NO TOMAR to " + varianteDelMismoColor); 
								crs = mapaCar.get(varianteDelMismoColor.getKey());
								addCharacteristicRecord("AssignTakeNoTake", "NO TOMAR", false, crs);  
								addCharacteristicRecord("AssignTakeNoTakeReason", "Otra talla del mismo color es tomar", false, crs);  
								addCharacteristicRecord("AdmissionDate", admissionDate, false, crs);
								addCharacteristicRecord("StylistWorld", stylistWorld, false, crs);
								addCharacteristicRecord("TipoDeToma", tipoDeToma, false, crs);
								addCharacteristicRecord("AssignTakeNoTakeVideo", a, false, crs); 
								addCharacteristicRecord("PrioridadDeTalla", varianteDelMismoColor.getValue(), false, crs); 
								response = rw.getRw().makeRequest("PUT", 
										"/object/Article/'" + varianteDelMismoColor.getKey() + "'@'MASTER'", qp,  
										new org.json.JSONObject().put("_characteristicRecords", crs) 
												.toString());
								log(response == null ? "ERR (affecting winner variant): " + rw.getRw().getRawResponse() 
										: "e.e Variant affected (" + colorEntry.getKey() + "): " + response + "<ÑÑ>" + crs);
							}else {
								log("Ya se había calculado para: " + varianteDelMismoColor);
							}
						}
					}
				} else {
					// ¿No hubieron variantes?
					log("No se obtuvieron variantes."); 
				}
			}
		} else {
			log("ERR: " + rw.getRw().getRawResponse()); 
		}
		return atLeast;
	}
	
	private void addCharacteristicRecord(String charId, Object value, boolean isCode,
			org.json.JSONArray characteristicRecords) throws org.json.JSONException {
		if (value == null) {
			return;
		}
		org.json.JSONObject rlobject = new org.json.JSONObject();
		rlobject.put("_qualification", 
				new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")));   
		rlobject.put("values", 
				new org.json.JSONArray().put(isCode ? new org.json.JSONObject().put("_code", value) : value)); 

		org.json.JSONObject cr = new org.json.JSONObject();
		cr.put("_recordLang", new org.json.JSONArray().put(rlobject)); 
		cr.put("_qualification", 
				new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", charId)));  

		characteristicRecords.put(cr);
	}

	private boolean checkSeccionMarca(String code, String marca, String sku, String baseUrl) throws org.json.JSONException, ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null) rw.setBaseUrl(baseUrl);
		rw.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		rw.putParameter("lookup", "ExcepcionSeccionMarca");
		rw.putParameter("fields", "LookupValue.Code,LookupValueReference.LookupValues('SKUsPermitidosExcepcionCatalogacion')->LookupValue.Code");
		rw.putParameter("query", 
				  "LookupValue.Code equals \"" + (code + "|" + marca) + "\" and LookupValue.IsActive = true"  
						); 
		org.json.JSONObject response = rw.makeRequest("GET", "/list/LookupValue/bySearch");
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows != null && rows.length() > 0) {
				if(!"".equals(sku)) {
					values = rows.getJSONObject(0).getJSONArray("values");
					org.json.JSONArray content = values.getJSONArray(1);
					for(int i=0; i<content.length(); i++) {
						if(sku.equals(content.getString(i)))
							return false;
					}
					return true;
				}else {
					return true;
				}
			}
		}
		return false;
	}
	
	public static void main(String[] args) throws JSONException, ServiceUnavailableException {
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put( "lookup", "'ExcepcionSeccionProveedor'");
		qp.put( "fields", "LookupValue.Code,LookupValueReference.LookupValues('SKUsPermitidosExcepcionCatalogacion')->LookupValue.Code" );
		rw.collectData("list", "LookupValue", null, "byLookup", qp, System.out::println);
//		AgarraloONo a = new AgarraloONo();
//		System.out.println( a.checkSeccionMarca("874", "1122", "", "https://webctep360dev.liverpool.com.mx/rest/V2.0") );
//		System.out.println( a.checkSeccionProveedor("533", "2290", "1033574341", "https://webctep360dev.liverpool.com.mx/rest/V2.0") );
	}

	private boolean checkSeccionProveedor(String code, String proveedor, String sku, String baseUrl) throws org.json.JSONException, ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null)
			rw.setBaseUrl(baseUrl);
		rw.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		rw.putParameter("lookup", "ExcepcionSeccionProveedor");  
		rw.putParameter("fields", "LookupValue.Code,LookupValueReference.LookupValues('SKUsPermitidosExcepcionCatalogacion')->LookupValue.Code");
		rw.putParameter("query", "LookupValue.Code equals \"" + code + "|" + proveedor + "\" and LookupValue.IsActive = true"); 
		org.json.JSONObject response = rw.makeRequest("GET", "/list/LookupValue/bySearch");
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(response != null) {
			System.out.println(response);
			rows = response.getJSONArray("rows");
			if(rows != null && rows.length() > 0) {
				if(!"".equals(sku)) {
					values = rows.getJSONObject(0).getJSONArray("values");
					org.json.JSONArray content = values.getJSONArray(1);
					for(int i=0; i<content.length(); i++) {
						if(sku.equals(content.getString(i)))
							return false;
					}
					return true;
				}else {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkSeccionNegocio(String code, String business, String sku, String baseUrl) throws org.json.JSONException, ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(baseUrl);
		rw.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		rw.putParameter("lookup", "'ExcepcionProveedorNegocio'");  
		rw.putParameter("fields", "LookupValue.Code,LookupValueReference.LookupValues('SKUsPermitidosExcepcionCatalogacion')->LookupValue.Code");
		rw.putParameter("query", "LookupValue.Code equals \"" + code + "|" + business + "\" and LookupValue.IsActive = true"); 
		org.json.JSONObject response = rw.makeRequest("GET", "/list/LookupValue/bySearch");  
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows != null && rows.length() > 0) {
				if(!"".equals(sku)) {
					values = rows.getJSONObject(0).getJSONArray("values");
					org.json.JSONArray content = values.getJSONArray(1);
					for(int i=0; i<content.length(); i++) {
						if(sku.equals(content.getString(i)))
							return false;
					}
					return true;
				}else {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkLookup(String code, String lookup, String baseUrl) throws org.json.JSONException {
		return queryLkp(code, lookup) != null;
	}
	
	private String queryLkp(String value, String lkp) {
		String container = lkp.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, delim, sep, escp);
				if(value.equals(pieces[0]))
					return pieces[1];
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String checkForoException(
			  String section
			, String itemGroup
			, String itemGroupS4H
			, String marca
			, String marcaS4H
			, String business
			, String supplier
			, String sku
			, String baseUrl
	) throws org.json.JSONException, ServiceUnavailableException {
		String aux = null;
		log("Arremángala bei bi:\n\tsection: " + section + "\n\tig: "  + itemGroup + "\n\tmarca: " + marca
				+ "\n\tmarcas4h: " + marcaS4H
				+ "\n\tbusiness: " + business
				+ "\n\tsupplier: " + supplier
				+ "\n\tsku: " + sku
				);
		if(marca == null || "".equals(marca)) {
			marca = marcaS4H;
		}
		if (itemGroup == null || "".equals(itemGroup)) 
		{
			if (itemGroupS4H != null && !"".equals(itemGroupS4H)) 
			{
				if (checkLookup(itemGroupS4H, "ExcepcionCatalogacionGrupoArticulos", baseUrl)) 
				{
					if (!checkLookup(sku, "SKUsPermitidosExcepcionCatalogacion",baseUrl))
						return "Excepción de grupo de artículo"; 
				}
			}
		} else {
			if (checkLookup(itemGroup, "ExcepcionCatalogacionGrupoArticulos", baseUrl)) 
			{
				if (!checkLookup(sku, "SKUsPermitidosExcepcionCatalogacion",baseUrl))
					return "Excepción de grupo de artículo"; 
			}
		}
		if (section != null && !"".equals(section))
		{
			if (marca != null && !"".equals(marca)) 
			{
				if (checkSeccionMarca(section, marca, sku, baseUrl)) {
					if (!checkLookup(sku, "SKUsPermitidosExcepcionCatalogacion", baseUrl)) 
						return "Excepción de sección y marca"; 
				}
			}
			if (business != null && !"".equals(business))
			{
				if (checkSeccionNegocio(section, business, sku, baseUrl)) {

					if (!checkLookup(sku, "SKUsPermitidosExcepcionCatalogacion", baseUrl)) 
						return "Excepción de sección y negocio"; 
				}
			}
			if (supplier != null && !"".equals(supplier)) 
			{
				if (checkSeccionProveedor(section, supplier, sku, baseUrl)) {
					if (!checkLookup(sku, "SKUsPermitidosExcepcionCatalogacion", baseUrl)) 
						return "Excepción de sección y proveedor"; 
				}
			}
		}
		if (sku != null && !"".equals(sku)) 
		{
			aux = queryLookupValue(sku, "ExcepcionesPorSKU", baseUrl); 

			if (aux != null) {
				return "Excepción por SKU"; 
			}
		}
		return null;
	}

	public java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> buildCharacteristicsMap(
			org.json.JSONArray characteristicRecords) throws org.json.JSONException {
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> map = new java.util.TreeMap<>();
		java.util.LinkedList<org.json.JSONObject> characteristics = new java.util.LinkedList<>();
		org.json.JSONObject characteristic = null;
		String id = null;
		for (int i = 0; i < characteristicRecords.length(); i++) {
			characteristic = characteristicRecords.getJSONObject(i);
			id = characteristic.getJSONObject("_qualification") 
					.getJSONObject("characteristic") 
					.getString("_code"); 
			characteristics = map.get(id);
			if (characteristics == null) {
				characteristics = new java.util.LinkedList<>();
				map.put(id, characteristics);
			}
			characteristics.addLast(characteristic);
		}
		return map;
	}

	private String queryDictionary(String value, String dictionary) {
		if(value == null)
			return null;
		String container = dictionary.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line, delim, sep, escp);
				if(value.equals(pieces[0]))
					return pieces[1];
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String queryLookupValue(String value, String dictionary, String baseUrl) throws org.json.JSONException {
		return queryDictionary(value, dictionary);
	}

	public String grabSimpleValue(String characteristicName,
			java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap) throws org.json.JSONException {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), false); 
	}

	public String grabCharacteristicValue(org.json.JSONObject characteristicObject, boolean getLabel)
			throws org.json.JSONException {
		String value = ""; 
		Object o = characteristicObject.getJSONArray("_recordLang") 
				.getJSONObject(0).getJSONArray("values") 
				.get(0);
		if (o instanceof org.json.JSONObject) {
			value = ((org.json.JSONObject) o).getString(getLabel ? "_label" : "_code");  
		} else if (o instanceof java.lang.String) {
			value = (String) o;
		} else if (o instanceof java.lang.Integer) {
			value = String.valueOf(o);
		} else {
			log("No data type identified: " + o + (o == null ? "null"  
					: " " + o.getClass() 
							.getName()));
		}
		return value;
	}

	private static final Logger LOGGER = Logger.getLogger(AgarraloONo.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/takeNoTakeCalc-%g.log", 5 * 1024 * 1024, 10, true);
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

                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }

	private void log(String message) {
		LOGGER.info(message);
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(
//				new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/takeNoTakeCalc.log", 
//						true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))  
//					+ "]  " + message); 
//		} catch (java.io.IOException e) {
//		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/takeNoTakeCalc.log", 
						true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

}
