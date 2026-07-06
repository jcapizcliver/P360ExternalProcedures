package mx.com.liverpool.p360.services.core.process;

import java.util.Set;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.process.ImageCheck.Result;

public class CatalogacionPorAPIsMkt {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = new RESTWorkshop();
	
	public void run() {
		String baseUrl = PropertiesManager.get("p360.contingency.mkt.apis.catalogacion.base_url");
		workshop.setBaseUrl(baseUrl);
		workshop.getRc().getHeader().clear();
		workshop.addHeader("apikey", PropertiesManager.get("p360.contingency.mkt.apis.apikey") /* "adb633ca-13a3-46fa-881d-fcec4ee6f4b3" */);
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fileName", "export");
		qp.put("responseFormat", "json");
		qp.put("acceptance_status", "NEW,TO_REVIEW");
		workshop.makeRequest("GET", "/mkpcatalogingmanagement/products/export", qp, null);
		org.json.JSONArray objects = null;
		try {
			objects = new org.json.JSONArray(workshop.getRawResponse());
			String r = null;
			java.util.Map<String, java.util.List<org.json.JSONObject>> pairs = new java.util.TreeMap<>();
			pairData(objects, pairs);
			pairs.forEach((k,v)-> System.out.println(k + " - " + v.size() + " (" + v.get(0).getJSONObject("data").getString("ZNUMVMKP") + ")") );
			System.exit(0);
			for(int i=0; i<objects.length(); i++) {
//				consultaTiposDeDato(objects.getJSONObject(i)).entrySet().forEach(System.out::println);
//				r = processObject(objects.getJSONObject(i), consultaTiposDeDato(objects.getJSONObject(i)));
				if(r != null) {
					System.out.println("An error: " + r);
				}else {
					System.out.println("It was ok.");
				}
			}
		}catch(org.json.JSONException e) {
			System.out.println("Got: " + workshop.getRawResponse());
			e.printStackTrace();
		}
	}
	
	private java.util.Map<String, String> consultaTiposDeDato(org.json.JSONObject o){
		java.util.Map<String, String> dataMap = new java.util.HashMap<>();
		if(o != null && o.has("data")) {
			org.json.JSONObject data = o.getJSONObject("data");
			StringBuilder sb = new StringBuilder();
			for(String name : org.json.JSONObject.getNames(data)) {
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(name).append("'");
			}
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", "Characteristic.Identifier,Characteristic.DataType");
			qp.put("items", sb.toString());
			rw.collectData("list", "Characteristic", null, "byItems", qp, row -> dataMap.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)));
		}
		return dataMap;
	}
	
	private String processRules(String name, String value) {
		if("ParentID".equals(name)) {
			return value.split("\\|")[0];
		}
		return name;
	}
	
	private void pairData( org.json.JSONArray objects, java.util.Map<String, java.util.List<org.json.JSONObject>> groups ) {
		org.json.JSONObject o = null;
		org.json.JSONObject data = null;
		for(int i=0; i<objects.length(); i++) {
			o = objects.getJSONObject(i);
			data = o.getJSONObject("data");
			String vGroupId = data.has("mirakl-variant-group-id") ? data.getString("mirakl-variant-group-id") : null;
//			vGroupId = "".equals(vGroupId) ? null : vGroupId;
			java.util.List<org.json.JSONObject> partners = groups.get(vGroupId);
			if(partners != null) {
				partners.add(o);
			}else {
				partners = new java.util.ArrayList<>();
				partners.add(o);
				groups.put(vGroupId, partners);
			}
		}
	}
	
	private String processObject(java.util.List<org.json.JSONObject> partners, java.util.Map<String, String> dataTypes /*, java.util.Map<String, java.util.List<org.json.JSONObject>> groups */) {
		if(partners != null && !partners.isEmpty()) {
			for(org.json.JSONObject o : partners) {
				org.json.JSONObject data = o.getJSONObject("data");
				org.json.JSONObject cuerpo = new org.json.JSONObject();
				org.json.JSONArray crs = new org.json.JSONArray();
				cuerpo.put("_characteristicRecords", crs);
				String mainBarCode = data.has("mirakl-MainBarCode") ? data.getString("mirakl-MainBarCode") : null;
				String vGroupId = data.has("mirakl-variant-group-id") ? data.getString("mirakl-variant-group-id") : null;
				String znumvmkp = data.has("ZNUMVMKP") ? data.getString("ZNUMVMKP") : null;
				int numVariantes = -1;
				if(!"".equals(znumvmkp)) {
					try {
						numVariantes = Integer.parseInt(znumvmkp);
					}catch(NumberFormatException e) {
						
					}
				}
				processLasImages(data);
				vGroupId = "".equals(vGroupId) ? null : vGroupId;
				if(mainBarCode != null) {
					java.util.List<String> lst = idsMainBarCode(mainBarCode);
					if(!lst.isEmpty()) {
						// eanreject
						return "eanreject";
					}
					String typeMainBarCodeMkt = data.getString("TypeMainBarCode");
					if(typeMainBarCodeMkt != null && !"".equals(typeMainBarCodeMkt)) {
						int lmbc = mainBarCode == null ? 0 : mainBarCode.length();
						String typeMainBarCode = null;
						try{
							typeMainBarCode = (mainBarCode == null || "".equals(mainBarCode)) ? ("IE") : ( lmbc == 8 ? "HK" : lmbc >= 6 && lmbc <= 12 ? "UC" : lmbc == 13 ? Long.parseLong(mainBarCode) < 3000_000_000_000l ? "EE" : "HE" : lmbc == 14 ? "IC" : "IE" );
						}catch(NumberFormatException e) {
							// teanreject
							return "teanreject";
	//						variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El código EAN no corresponde con un número válido.").put("fields", new org.json.JSONArray().put( "MainBarCode" )));
						}
						if("".equals(typeMainBarCode) && lmbc > 0) {
							// teanreject
	//						variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El código EAN no corresponde con una longitud válida.").put("fields", new org.json.JSONArray().put( "MainBarCode" )));
							return "teanreject";
						}else if(!typeMainBarCode.equals(typeMainBarCodeMkt)) {
							// teanreject
							return "teanreject";
						}
					}
				}
	
				String container = "MarketplaceSupplierToShop";
				java.util.Map<String, String> dictionaryData = readStdValues(container.replaceAll("/", "<::>"));
				String supplier = data.has("SupplierID") ? data.getString("SupplierID") : null;
				String shopIdRef = supplier != null ? dictionaryData.get(supplier) : null;
				String shopId = data.has("supplierShopId") ? data.getString("supplierShopId") : null;
				if(shopIdRef != null && !shopIdRef.equals(shopId)) {
					return "shopreject";
				}
				
				String parentId = data.has("ParentID") ? data.getString("ParentID") : null;
				if(parentId != null) {
					cuerpo.put("structureGroupMap", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'EU4-" + parentId.replaceAll(".+-", "") + "'@'PrimaryProductTaxonomy'")))));
				}
				
				crs.put( createCharacteristicValueObject("mirakl_product_id", o.getString("mirakl_product_id")) );
				crs.put( createCharacteristicValueObject("Business", new org.json.JSONObject().put("_code", "MKP")) );
				crs.put( createCharacteristicValueObject("SkuType", new org.json.JSONObject().put("_code", "SERV")) );
				crs.put( createCharacteristicValueObject("SAPSpart", new org.json.JSONObject().put("_code", "DZ")) );
				crs.put( createCharacteristicValueObject("Negocio", new org.json.JSONObject().put("_code", "ART. MARKETPLACE")) );
				crs.put( createCharacteristicValueObject("TAXESSAP", new org.json.JSONObject().put("_code", "E210")) );
				crs.put( createCharacteristicValueObject("CostobrutoSinIVA",   "0.01") );
				crs.put( createCharacteristicValueObject("PrecioSugeridocIVAS", new org.json.JSONObject().put("_code", "0003")) );
				crs.put( createCharacteristicValueObject("Status", new org.json.JSONObject().put("_code", "01")) );
				crs.put( createCharacteristicValueObject("BaseUnitOfMeasure", new org.json.JSONObject().put("_code", "UN")) );
				crs.put( createCharacteristicValueObject("FotoTomadaLiverpool", new org.json.JSONObject().put("_code", "N")) );
				crs.put( createCharacteristicValueObject("IndicadordeImpuesto", new org.json.JSONObject().put("_code", "E2")) );
				crs.put( createCharacteristicValueObject("ImpuestoALaVenta", new org.json.JSONObject().put("_code", "1")) );
				crs.put( createCharacteristicValueObject("IEPS", new org.json.JSONObject().put("_code", "0")) );
				crs.put( createCharacteristicValueObject("AnoEstacion", new java.text.SimpleDateFormat("yyyy").format(new java.util.Date())) );
				crs.put( createCharacteristicValueObject("EnvioMirakl", new org.json.JSONObject().put("_code", "1")) );
				crs.put( createCharacteristicValueObject("SistemaOrigen", new org.json.JSONObject().put("_code", "4")) );
	//			crs.put( createCharacteristicValueObject("SAPObjectType", new org.json.JSONObject().put("_code", "")) );
				crs.put( createCharacteristicValueObject("MainBarCode", mainBarCode) );
				
				System.exit(0);
				String dataType = null;
				String value = null;
				for(String name : org.json.JSONObject.getNames(data)) {
					if(!"".equals( data.getString(name) )) {
						dataType = dataTypes.get(name);
						value = processRules(name, data.getString(name));
						if(dataType != null)
							crs.put( createCharacteristicValueObject(name, "LOOKUP".equals(dataType) ? new org.json.JSONObject().put("_code", value) : value) );
					}
				}
				
				System.out.println( cuerpo );
			}
		}
		return null;
	}
	
	private void processLasImages(org.json.JSONObject data) {
		java.util.Map<String, String> imagesData = new java.util.TreeMap<>();
		for(String name : org.json.JSONObject.getNames(data)) {
			if(name.startsWith("mirakl-Image")) {
				if(!"".equals(data.getString(name))) {
					imagesData.put(name, data.getString(name));
				}
			}
		}
		Result r = null;
		for(java.util.Map.Entry<String, String> entry : imagesData.entrySet()) {
			r = validaImagen(entry.getValue());
			System.out.println(r);
		}
	}
	
	private Result validaImagen(String url) {
		 Result r = ImageCheck.validateUrl(
	                url,
	                Set.of("jpeg", "png", "webp", "gif"),
	                5L * 1024 * 1024,
	                4000,
	                4000,
	                20_000_000L
	        );
		 return r;
	}
	
	private java.util.Map<String, String> readStdValues(String container){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				if(pieces.length > 1) {
					data.put(pieces[0], pieces[1]);
				}else {
					log("Malformed line in file " + container + ", please check correct format, skipping.");
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	private java.util.List<String> idsMainBarCode(String mainBarCode){
		java.util.List<String> lst = new java.util.ArrayList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "(characteristic('MainBarCode') = \"" + mainBarCode + "\" or characteristic('MainBarCode',-1) = \"" + mainBarCode + "\" ) and characteristic('Business') = 'LVP'@'BusinessQualified' ");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> lst.add(row.getJSONArray("values").getString(0)));
		if(lst.isEmpty()) {
			qp.clear();
			qp.put("fields", "Article.SupplierAID");
			qp.put("query", "(characteristic('MainBarCode') = \"" + mainBarCode + "\" or characteristic('MainBarCode',-1) = \"" + mainBarCode + "\" )");
			rw.collectData("list", "Article", null, "bySearch", qp, row -> lst.add(row.getJSONArray("values").getString(0)));
		}
		return lst;
	}

	protected org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return 
			new org.json.JSONObject()
				.put("_recordLang", 
						new org.json.JSONArray()
							.put(
								new org.json.JSONObject()
									.put("values", new org.json.JSONArray().put(value))
									.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
								)
					)
				.put("_qualification", 
						new org.json.JSONObject()
							.put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}

	protected String getCharacteristicValue(org.json.JSONObject characteristic, boolean getCode) {
		if(characteristic == null) {
			return "";
		}
		String dataType = characteristic.has("_datatype") ? characteristic.getString("_datatype") : "";
		if("LOOKUP".equals(dataType)) {
			try{
				return characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).has(getCode ? "_code" : "_label") ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString(getCode ? "_code" : "_label") : "";
			}catch(org.json.JSONException e){
				throw e;
			}
		}else {
			return String.valueOf( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) );
		}
	}

	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/catalogaciónPorAPIMkt.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/catalogaciónPorAPIMkt.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
	
	public static void main(String[] args) {
//		RESTWrapper rw = new RESTWrapper();
//		rw.getRw().setBaseUrl("https://172.18.237.210:1512/rest/V2.0");
//		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("includeObjectsInProtocol", "false");
//		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue")), 5000, request -> rw.writeData("list", "StandardizationValue", null, qp, request, System.out::println) );
//		String[] par = null;
//		for(String parPlano : PARES) {
//			par = parPlano.split("\t");
//			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + par[0] + "'@'MarketplaceSupplierToShop'")).put("values", new org.json.JSONArray().put(par[1])));
//		}
//		rh.sendData();
//		System.exit(0);
		CatalogacionPorAPIsMkt cat = new CatalogacionPorAPIsMkt();
		cat.run();
	}
	
}
