package mx.com.liverpool.p360.services.core;

import org.json.JSONObject;

public class GetVariantByEanOrBrandAndModel {

	private static final RESTWrapper rw = new RESTWrapper();
	private String listAPIArticlebySearchURL;
	private String baseUrl = null;

	private final RestClient rc = new RestClient();
	private String input;

	public GetVariantByEanOrBrandAndModel(String baseUrl) {
		this.baseUrl = baseUrl;
		listAPIArticlebySearchURL = baseUrl + "/list/Article/bySearch?query=";
	}

	public static void main(String[] args) {

	}

	public void loadInput(String inputFileData) {
		this.input = inputFileData;
	}

	public String processFile(String encoded) {
		java.util.Map<String, String> headers = new java.util.HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");
		headers.put("Authorization", "Basic " + encoded);
		headers.put("Accept-Language", "es");

		String rawResponse = null;
		JSONObject response = null;
		JSONObject json = null;
		org.json.JSONArray rows = null;

		String marca = null;
		String modelo = null;

		String plantilla = null;
		String nombrePlantilla = null;
		String varianteId = null;
		String propuestaId = null;
		String nombreArticuloEs = null;
		String nombreArticuloEn = null;
		String descripcionArticuloEs = null;
		String descripcionArticuloEn = null;
		String ean = null;
		String negocio = null;
		String sku = null;
		String urlImagenPrincipal = null;
		String message = null;
		String[] productData = null;
		int successCode = 0;

		org.json.JSONObject theFinalResponse = new org.json.JSONObject();
		org.json.JSONArray responses = new org.json.JSONArray();

		String articleCharacteristicsToFind = 
				   "Article.SupplierAID"
				+ ",ArticleStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier"
				+ ",ArticleStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(en)"
				+ ",ArticleLang.DescriptionShort(en)" 
				+ ",ArticleLang.DescriptionShort(es)"
				+ ",ArticleLang.DescriptionLong(en)" 
				+ ",ArticleLang.DescriptionLong(es)" 
				+ ",Article.EAN"
				+ ",Article.Business->LookupValueLang.Name(en)" 
				+ ",Article.SKU" 
				+ ",Article.ProductImageURL"
			;

		log(input);

		try {
			response = new org.json.JSONObject(new JSONObject(input).getString("input"));

			rows = response.getJSONArray("variants");

			log("Request of: " + rows.length() + " possible variants!");

			for (int i = 0; i < rows.length(); i++) {
				message = "";
				successCode = 0;
				varianteId = null;
				json = rows.getJSONObject(i);
				ean = json.has("ean") ? json.getString("ean") : null;
				marca = json.has("brand") ? json.getString("brand") : null;
				modelo = json.has("model") ? json.getString("model") : null;

				productData = micosito(baseUrl, ean, marca, modelo);
				log("Going for micosito: " + productData);
				if (productData != null) {
					urlImagenPrincipal = "";
					propuestaId = productData[0];
					negocio = productData[1];
					plantilla = productData[4];
					nombrePlantilla = productData[5];
					nombreArticuloEs = productData[2];
					descripcionArticuloEs = productData[3];
					ean = !"".equals(productData[7]) ? productData[7] : productData[8];
					sku = productData[6];
					successCode = 1;
				} else {
					log("Not found through micosito");
					// Searching by EAN
					if (ean != null && !"".equals(ean)) {
						log("busqueda por ean de " + ean);
						long init = System.currentTimeMillis();
						rawResponse = this.rc.getRequest("GET", listAPIArticlebySearchURL
								+ java.net.URLEncoder.encode("Article.EAN = \"" + ean + "\"", "UTF-8") + "&fields="
								+ java.net.URLEncoder.encode(articleCharacteristicsToFind, "UTF-8"), null, headers);
						log("By pure ean search on article: " + rw.getRw().formatTime(System.currentTimeMillis() - init));
						response = new JSONObject(rawResponse);

						if (response.getInt("rowCount") > 0) {
							varianteId 				= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
							plantilla 				= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0);
							nombrePlantilla 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0);
							nombreArticuloEn 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(3);
							nombreArticuloEs 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(4);
							descripcionArticuloEn 	= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(5);
							descripcionArticuloEs 	= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(6);
							ean 					= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(7);
							negocio 				= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(8);
							sku 					= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(9);
							urlImagenPrincipal 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(10);
							successCode = 1;
						} else {
							successCode = 0;
							message = "Ninguna variante ha sido creada con el EAN: '" + ean + "'. ";
						}
					}

					if (marca != null && !"".equals(marca) && modelo != null && !"".equals(modelo)
							&& successCode != 1) {
						/*
						log("busqueda por marca y modelo");
						long init = System.currentTimeMillis();
						rawResponse = this.rc.getRequest("GET", listAPIArticlebySearchURL
								+ java.net.URLEncoder.encode("ArticleExtraData.SupplierPartNumber(MX) equals \""
										+ modelo
										+ "\" and (ArticleExtraData.BrandName(MX)->LookupValueLang.Name(es) = \""
										+ marca
										+ "\" or ArticleExtraData.BRAND_ID_S4H(MX)->LookupValueLang.Name(es) = \""
										+ marca + "\")", "UTF-8")
								+ "&fields=" + java.net.URLEncoder.encode(articleCharacteristicsToFind, "UTF-8"), null,
								headers);
						log("By pure ean search on article: " + rw.getRw().formatTime(System.currentTimeMillis() - init));
						response = new JSONObject(rawResponse);

						if (response.getInt("rowCount") > 0) {
							varianteId 				= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
							plantilla 				= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0);
							nombrePlantilla 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0);
							nombreArticuloEn 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(3);
							nombreArticuloEs 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(4);
							descripcionArticuloEn 	= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(5);
							descripcionArticuloEs 	= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(6);
							ean 					= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(7).getString(0);
							negocio 				= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0);
							sku 					= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(9).getString(0);
							urlImagenPrincipal 		= response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
							successCode = 1;
						}
						 */
					} else {
						successCode = 0;
						message = java.net.URLEncoder.encode(message + "Ninguna variante ha sido creada con la marca: '"
								+ marca + "' y modelo: '" + modelo + "'.", "UTF-8");
					}
				}

				if (varianteId == null && propuestaId == null) {
					responses.put(new JSONObject().put("status", "Not found").put("message", message));
					log(responses.toString());
				} else {
					JSONObject jsonRes = new org.json.JSONObject();
					jsonRes.put("urlImagenPrincipal", urlImagenPrincipal).put("sku", sku).put("negocio", negocio)
							.put("ean", ean).put("descripcionArticuloEn", descripcionArticuloEn)
							.put("descripcionArticuloEs", descripcionArticuloEs)
							.put("nombreArticuloEn", nombreArticuloEn).put("nombreArticuloEs", nombreArticuloEs)
							.put("varianteId", varianteId).put("propuestaId", propuestaId)
							.put("nombrePlantilla", nombrePlantilla).put("plantilla", plantilla);

					responses.put(jsonRes);
				}
			}

		} catch (Exception e) {
			logE(e);
			log("Raw response: " + rawResponse);
		}

		theFinalResponse = theFinalResponse.put("values", responses);
		log(theFinalResponse.toString());
		return theFinalResponse.toString();

	}

	private String[] micosito(String baseUrl, String ean, String brand, String model) {
		RESTWorkshop rw = new RESTWorkshop();
		if (baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		rw.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		String[] data = null;
		long init = System.currentTimeMillis();
		if( (ean != null && !"".equals(ean)) || (brand != null && !"".equals(brand) && model != null && !"".equals(model))) {
			qp.put("query",
					"" + (ean != null && !"".equals(ean) && brand != null && !"".equals(brand) && model != null && !"".equals(brand) ? "(Product2G.EAN = \"" + ean + "\")"  + " or " + "(" + "("
							+ "Product2GExtraData.BrandName(MX)->LookupValueLang.Name(es) = \"" + brand + "\"" + " or "
							+ "Product2GExtraData.BRAND_ID_S4H(MX)->LookupValueLang.Name(es) = \"" + brand + "\"" + ")"
							+ " and (Product2GExtraData.SupplierPartNumber(MX) = \"" + model + "\")" + ")" : ean != null && !"".equals(ean) ? "Product2G.EAN = \"" + ean + "\")" : brand != null && !"".equals(brand) && model != null && !"".equals(model) ? "Product2GExtraData.BRAND_ID_S4H(MX)->LookupValueLang.Name(es) = \"" + brand + "\""+ " and (Product2GExtraData.SupplierPartNumber(MX) = \"" + model + "\")" : "" ));
			qp.put("fields",
							   "Product2G.ProductNo" 
							+ ",Product2G.Business->LookupValueLang.Name(es)" 
							+ ",Product2GLang.ProductName(es)"
							+ ",Product2GLang.DescriptionLong(es)"
							+ ",Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier"
							+ ",Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es)"
							+ ",Product2G.SKU" 
							+ ",Product2G.EAN");
	
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			log("Micosito req took: " + rw.formatTime(System.currentTimeMillis() - init));
			if (response != null) {
				rows = response.getJSONArray("rows");
				if (rows.length() > 0) {
					log("EIP: " + String.valueOf(rows.getJSONObject(0).getJSONArray("values")));
				}
				data = rows.length() > 0
						? new String[] { rows.getJSONObject(0).getJSONArray("values").getString(0),
								rows.getJSONObject(0).getJSONArray("values").getString(1),
								rows.getJSONObject(0).getJSONArray("values").getString(2),
								rows.getJSONObject(0).getJSONArray("values").getString(3),
								rows.getJSONObject(0).getJSONArray("values").getJSONArray(4).getString(0),
								rows.getJSONObject(0).getJSONArray("values").getJSONArray(5).getString(0),
								rows.getJSONObject(0).getJSONArray("values").getString(6),
								rows.getJSONObject(0).getJSONArray("values").getString(7),
								"" }
						: null;
				log("Collected from micosito: " + data);
			} else {
				log("ERROR querying data for product check: " + rw.getRawResponse());
			}
		}
		return data;
	}

	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/getVariantByEanOrBrandAndModel.log", true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	private static void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/getVariantByEanOrBrandAndModel.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
