package mx.com.liverpool.p360.services.core.dq;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class NameAndProductName extends RESTDQRuleImpl {

	private final long myId;
	private final String proposalId;
	private final String orderOfAttributesForName;
	private final org.json.JSONArray genericFieldErrors;
	private String sourceTemplate = null;
	private String productImageURL = null;
	
	public NameAndProductName(String proposalId, String orderOfAttributesForName, long myId, org.json.JSONArray genericFieldErrors) {
		this.myId = myId;
		this.proposalId = proposalId;
		this.orderOfAttributesForName = orderOfAttributesForName;
		this.genericFieldErrors = genericFieldErrors;
	}
	
	public void setSourceTemplate(String sourceTemplate) {
		this.sourceTemplate = sourceTemplate;
	}
	
	public void setProductImageURL(String productImageURL) {
		this.productImageURL = productImageURL;
	}
	
	@Override
	public void processData(Map<String, JSONObject> sourceData, JSONArray records) {
		if(proposalId != null) {
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
//			qp.put("entityFilter", "structureMap,Product2GLang,Product2GCharacteristicValue");
			qp.put("includeLabels", "true");
			qp.put("includeIds", "true");
			org.json.JSONObject objectResponse = rw.getRw().makeRequest("GET", "/object/Product2G/'" + proposalId + "'@1", qp, null);
			log("Got prop ID " + proposalId);
			if(objectResponse != null && objectResponse.has("_data") && objectResponse.getJSONObject("_data").has("_characteristicRecords")) {
				log("got characteristic records");
				
				org.json.JSONArray cr = objectResponse.getJSONObject("_data").getJSONArray("_characteristicRecords");
				org.json.JSONObject json = null;
				java.util.Map<String, org.json.JSONObject> characteristicsMap = new java.util.TreeMap<>();
				String template = null;
				String templateName = null;
				String orderOfAttributesForName = null;
				for(int i=0; i<cr.length(); i++) {
					json = cr.getJSONObject(i);
					characteristicsMap.put(json.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), json);
					log("\t" + proposalId + " - " + json.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") + "||" + json );
				}
				java.util.Map<String, org.json.JSONObject> characteristicsMap2 = sourceData;
				if(characteristicsMap2.isEmpty()) {
					org.json.JSONObject json2 = null;
					for(int i=0; i<records.length(); i++) {
						json2 = records.getJSONObject(i);
						characteristicsMap2.put(json2.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), json2);
					}
				}
				String[] td = new String[3];
				td[0] = null;
				td[1] = null;
				td[2] = null;
				java.util.Map<String, String> qp00 = new java.util.TreeMap<>();
				qp00.put("fields", "Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier,Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es),Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupAttributeValue.Value(\"OrderOfAtributesForName\",es,DEFAULT)");
				qp00.put("items", "'" + proposalId + "'@1");
				log("gonna request product structure data...");
				rw.collectData("list", "Product2G", null, "byItems", qp00, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					log("Got this data: " + values);
					org.json.JSONArray sgc = values.getJSONArray(0);
					org.json.JSONArray sgn = values.getJSONArray(1);
					td[0] = sgc.getString(0);
					td[1] = sgn.getString(0);
					td[2] = values.getJSONArray(2).getString(0);
				});
				log("La plantilla que debemos tener desde arriba: " + this.sourceTemplate);
				if(td[0] != null) {
					template = td[0];
					templateName = td[1];
					orderOfAttributesForName = "".equals( td[2] ) ? this.orderOfAttributesForName : td[2];
				}else {
					orderOfAttributesForName = this.orderOfAttributesForName;
				}
				if(template == null) {
					template = this.sourceTemplate;
					java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
					qp0.put("fields", "StructureGroupLang.Name(es),StructureGroupAttributeValue.Value(\"OrderOfAtributesForName\",es,DEFAULT)");
					qp0.put("items", "'" + template + "'@'PrimaryProductTaxonomy'");
					String[] templateName2 = new String[1];
					templateName2[0] = null;
					String[]  order = new String[1];
					order[0] = null;
					rw.collectData("list", "StructureGroup", null, "byItems", qp0, row -> {
						templateName2[0] = row.getJSONArray("values").getString(0);
						order[0] = row.getJSONArray("values").getString(1);
					});
					templateName = templateName2[0];
				}
				log("Got template: " + template);
				log("Got template name: " + templateName);
				org.json.JSONObject esLang = null;
				if(objectResponse.has("lang")) {
					org.json.JSONArray lang = objectResponse.getJSONArray("lang");
					for(int i=0; i<lang.length(); i++) {
						if(10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key")) {
							esLang = lang.getJSONObject(i);
						}
					}
					if(esLang != null) {
						
					}
				}
				String negocio = objectResponse.has("business") ? objectResponse.getJSONObject("business").getString("_label") : getCharacteristicValue( characteristicsMap.get("Business"), false );
				String prevPN = esLang != null && esLang.has("descriptionShort") ? esLang.getString("descriptionShort") : null;
				String prevProductName = esLang != null && esLang.has("productName") ? esLang.getString("productName") : null;
				String descriptionLong = esLang != null && esLang.has("descriptionLong") ? esLang.getString("descriptionLong") : null;
				String productTypeSAPLabel = getCharacteristicValue( characteristicsMap.get("ProductTypeSAP"), false );
				String itemGroup = null;
				if(objectResponse.getJSONObject("_data").has("productExtraData")) {
					if(objectResponse.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("itemGroup")) {
						itemGroup = objectResponse.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("itemGroup").getString("_label");
					}else if(objectResponse.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).has("itemGroupS4H")) {
						itemGroup = objectResponse.getJSONObject("_data").getJSONArray("productExtraData").getJSONObject(0).getJSONObject("itemGroupS4H").getString("_label");
					}
				}
				if(itemGroup == null || "".equals(itemGroup)) {
					itemGroup = getCharacteristicValue( characteristicsMap.get("ItemGroup") );
				}
				if(itemGroup == null || "".equals(itemGroup)) {
					itemGroup = getCharacteristicValue( characteristicsMap.get("ItemGroupS4H") );
				}
				log("Order of attributes for name: " + orderOfAttributesForName);
				log("Got these:");
				log(negocio);
				log(prevPN);
				log(prevProductName);
				log(descriptionLong);
				log(productTypeSAPLabel);
				log("/gt.");
				if(orderOfAttributesForName != null && !"".equals(orderOfAttributesForName) /* && (prevPN == null || prevPN.isEmpty()) */ ) {
					log("Came here to loop");
					String[] elements = orderOfAttributesForName.split(",");
					StringBuilder sb = new StringBuilder();
					String val = null;
					for(String element : elements) {
						val = getCharacteristicValue( characteristicsMap.get(element) );
						if(val == null || "".equals(val)) {
							val = getCharacteristicValue( characteristicsMap2.get(element) );
						}
						log(proposalId + " - " + element + " || " + ("ProductTypeSAP".equals(element) ? "$ " + getCharacteristicValue( characteristicsMap.get(element) ).replaceAll("^\\d+ - ", "") + " $" : " -->" + getCharacteristicValue( characteristicsMap.get(element) ) + "<-- " + characteristicsMap.get(element) + " <::> "));
						if("ProductTypeSAP".equals(element)) {
							sb.append(sb.length() == 0 ? "" : ", ").append(val.replaceAll("^\\d+ - ", ""));
						}else
							if(!element.contains("\"")) {
								sb
								.append(sb.length() == 0 ? "" : ", ")
								.append(val);
							}else {
								sb
								.append(sb.length() == 0 ? "" : ", ")
								.append(element.replaceAll("\"", ""));
							}
					}
					String productName = sb.toString().replaceAll(", +?,", ",").replaceAll(",(?! )", ", ").replaceAll(" ,", ",").replaceAll(",", "").replaceAll(" {2,}", " ").trim();
//					ResolvedName rn = ProductNameResolver.resolve(productName, prevPN);
//					productName = rn.value();
					if("Marketplace".equals(negocio)) {
						try {
							getItemGroupFromIA(productName, template, productTypeSAPLabel, descriptionLong, templateName, records);
						} catch (java.io.IOException e) {
							logE(e);
						}
					}else { log("Not mkp"); }
					
					log("PN: " + productName);
					org.json.JSONObject pno = createCharacteristicValueObject("ProductName", prevProductName == null || prevProductName.isEmpty() ? productName : prevProductName);
					org.json.JSONObject no  = createCharacteristicValueObject("Name", prevPN == null || prevPN.isEmpty() ? productName : prevPN);
					records.put(pno);
					records.put(no);
					sourceData.put("ProductName", pno);
					sourceData.put("Name", no);
				}
				org.json.JSONObject data = null;
//				if(prevPN != null && !"".equals(prevPN)) {
//					data = characteristicsMap.get("ProductName");
//					if(data != null) {
//						sourceData.put("ProductName", data);
//					}else { log("No ProductName"); }
//					data = characteristicsMap.get("Name");
//					if(data != null){
//						sourceData.put("Name", characteristicsMap.get("Name"));
//					}else { log("No name..."); }
//				}
				data = characteristicsMap.get("BrandName");
				if(data != null) {
					sourceData.put("BrandName", data);
				}else { log("No BrandName"); }
				data = characteristicsMap.get("BRAND_ID_S4H");
				if(data != null) {
					sourceData.put("BRAND_ID_S4H", data);
				}else { log("No b_id_s4h"); }
			} else { log("Just no u.u"); }
		}else {
			log("A boy with no known proposalId...");
			java.util.Map<String, org.json.JSONObject> characteristicsMap = sourceData;
			if(characteristicsMap.isEmpty()) {
				org.json.JSONObject json = null;
				for(int i=0; i<records.length(); i++) {
					json = records.getJSONObject(i);
					characteristicsMap.put(json.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), json);
				}
			}
			String template = sourceTemplate;
			java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
			qp0.put("fields", "StructureGroupLang.Name(es),StructureGroupAttributeValue.Value(\"OrderOfAtributesForName\",es,DEFAULT)");
			qp0.put("items", "'" + template + "'@'PrimaryProductTaxonomy'");
			String[] templateName = new String[1];
			templateName[0] = null;
			String[]  order = new String[1];
			order[0] = null;
			rw.collectData("list", "StructureGroup", null, "byItems", qp0, row -> {
				templateName[0] = row.getJSONArray("values").getString(0);
				order[0] = row.getJSONArray("values").getString(1);
			});
			String orderOfAttributesForName = null;
			if(order[0] == null || "".equals(order[0])) {
				orderOfAttributesForName = this.orderOfAttributesForName;
				log("using parent order of attributes for name: " + this.orderOfAttributesForName);
			}else {
				orderOfAttributesForName = order[0];
			}
			String negocio = getCharacteristicValue( characteristicsMap.get("Business"), false );
			String prevPN = getCharacteristicValue( characteristicsMap.get("Name") );
			String descriptionLong = getCharacteristicValue( characteristicsMap.get("DescriptionLong") );
			String productTypeSAPLabel = getCharacteristicValue( characteristicsMap.get("ProductTypeSAP"), false );
			String itemGroup = getCharacteristicValue( characteristicsMap.get("ItemGroup") );
			if(itemGroup == null || "".equals(itemGroup)) {
				itemGroup = getCharacteristicValue( characteristicsMap.get("ItemGroupS4H") );
			}
			log("Business: " + negocio);
			log("orderOfAttributesForName: " + orderOfAttributesForName);
			log("PrevPN: " + prevPN);
			if(orderOfAttributesForName != null && !"".equals(orderOfAttributesForName) && (prevPN == null || prevPN.isEmpty()) ) {
				String[] elements = orderOfAttributesForName.split(",");
				StringBuilder sb = new StringBuilder();
				for(String element : elements) {
					log("Element for PN Calc: " + element);
					if("ProductTypeSAP".equals(element)) {
						sb.append(sb.length() == 0 ? "" : ", ").append(itemGroup.replaceAll("^\\d+ - ", ""));
					}else if(!element.startsWith("ItemGroup")) {
						if(!element.contains("\"")) {
							sb
							.append(sb.length() == 0 ? "" : ", ")
							.append(getCharacteristicValue( characteristicsMap.get(element) ));
						}else {
							sb
							.append(sb.length() == 0 ? "" : ", ")
							.append(element.replaceAll("\"", ""));
						}
					}else {
						log("Skipping item group since this is not known...");
					}
				}
				String productName = sb.toString().replaceAll(", +?,", ",").replaceAll(",(?! )", ", ").replaceAll(" ,", ",").replaceAll(",", "").replaceAll(" {2,}", " ").trim();

				if("Marketplace".equals(negocio)) {
					try {
						log("Using la IA: " + Boolean.parseBoolean(PropertiesManager.get("p360.contingency.use_ia")));
						log("dL: " + descriptionLong);
						log("pn: " + productName);
						if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.use_ia")))
							getItemGroupFromIA(productName, template, productTypeSAPLabel, descriptionLong, templateName[0], records);
					} catch (java.io.IOException e) {
						logE(e);
					}
				}
				org.json.JSONObject pno = createCharacteristicValueObject("ProductName", productName);
				org.json.JSONObject no  = createCharacteristicValueObject("Name", productName);
				records.put(pno);
				records.put(no);
				sourceData.put("ProductName", pno);
				sourceData.put("Name", no);
			}else {
				if(! ( prevPN == null || prevPN.isEmpty() ) ) {
					log("Using previous ProductName: " + prevPN);
				}else {
					log("No orderOfAttributesForName...");
				}
			}
			log("Got here (LaIA)...");
			org.json.JSONObject data = null;
			if(prevPN != null && !"".equals(prevPN)) {
				data = characteristicsMap.get("ProductName");
				if(data != null) {
					sourceData.put("ProductName", data);
				}
				data = characteristicsMap.get("Name");
				if(data != null){
					sourceData.put("Name", characteristicsMap.get("Name"));
				}
			}
			data = characteristicsMap.get("BrandName");
			if(data != null) {
				sourceData.put("BrandName", data);
			}
			data = characteristicsMap.get("BRAND_ID_S4H");
			if(data != null) {
				sourceData.put("BRAND_ID_S4H", data);
			}
		}
	}
	
	public void getItemGroupFromIA(String productName, String template, String productTypeSAP, String productDescription, String templateName, org.json.JSONArray newCharacteristicRecords) throws IOException {
		long init = System.currentTimeMillis();
		String itemGroup = null;
//		String jsonKeyPath = PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_sa"); // "/u01/stage/dev.json";// "/P360shared/IDMC/dev.json";
        String targetAudience = PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_url_ta"); // "https://service-idga-prediction-335803992526.us-central1.run.app/api/post_iga_prediction";
        
		try {
			// Transporte HTTP
            HttpTransport transport = new NetHttpTransport(); log("Querying: " + PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_url"));

            // Carga credenciales y crea ID token (para Cloud Run personalizado)
//            IdTokenCredentials credentials = IdTokenCredentials.newBuilder()
//                .setIdTokenProvider((IdTokenProvider) GoogleCredentials.fromStream(new FileInputStream(jsonKeyPath)))
//                .setTargetAudience(PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_url"))
//                .build();

//            credentials.refresh();
//            String idToken = credentials.getAccessToken().getTokenValue();

            // Construye URL destino
            GenericUrl url = new GenericUrl(targetAudience);
            log("Querying IA for Item group with: ProductName: " + productName + ", Template: " + template + ", ProductTypeSAP: " + productTypeSAP + ", Desc: " + productDescription);
			org.json.JSONObject body = new org.json.JSONObject().put("input", new org.json.JSONArray().put( new org.json.JSONObject()
					.put("pim_product_name", productName)
					.put("pim_template_id", template)
					.put("product_type_sap", productTypeSAP == null ? "" : productTypeSAP.toLowerCase())
					.put("product_description", productDescription == null || "".equals(productDescription) ? templateName : productDescription)
					.put("image", productImageURL == null ? "" : productImageURL)));
            HttpContent content = new ByteArrayContent("application/json", 
            		body.toString().getBytes());
            log("Using body for AI ItemGroup request: " + body);
            // Construye request
            HttpRequestFactory requestFactory = transport.createRequestFactory();
            HttpRequest request = requestFactory.buildPostRequest(url, content);
//            request.getHeaders().setAuthorization("Bearer " + idToken);
            // Timeouts opcionales
            request.setConnectTimeout(Integer.parseInt( PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_connect_timeout") ));
            request.setReadTimeout( Integer.parseInt( PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_read_timeout") ) );

            // Ejecuta la petición
            HttpResponse response = request.execute();
            log("Response status: " + response.getStatusCode());
            String rsp = response.parseAsString();
            log("Response body: " + rsp);
            org.json.JSONObject jsonResponse = null;
            jsonResponse = new org.json.JSONArray( rsp ).getJSONObject(0);
			
			String direction = String.valueOf( jsonResponse.get("direction") );
			String section = String.valueOf( jsonResponse.get("section") );
			itemGroup = String.valueOf( jsonResponse.get("item_group") );
			

			/****************/
				int month = Integer.parseInt( new java.text.SimpleDateFormat("MM").format(new java.util.Date()) );
				int year = Integer.parseInt( new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) ) + (month < 11 ? 0 : 1);
				newCharacteristicRecords.put( createCharacteristicValueObject("AnoEstacion", String.valueOf(year) ) );
				newCharacteristicRecords.put( createCharacteristicValueObject("Temporada", new org.json.JSONObject().put("_code", "0003") ) );
				String sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase");
				if(sapBehvo != null && !"".equals(sapBehvo)) {
					newCharacteristicRecords.put( createCharacteristicValueObject("SAP_BEHVO", new org.json.JSONObject().put("_code", sapBehvo.substring(0,2) )) );
					log("Got " + sapBehvo + " for SAP_BEHVO.");
					String thevalue = "1";
					try{
						org.json.JSONArray rws = new org.json.JSONObject( rw.getRw().getRc().getRequest("GET", rw.getRw().getBaseUrl() + "/list/StandardizationValue/bySearch"
								+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8")
								+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \"" + sapBehvo.substring(0,2) + "\"", "UTF-8")
								+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8")
								, null) ).getJSONArray("rows");
						log("Checking sapBehvo: " + sapBehvo);
						if(rws.length() > 0) {
							thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
						}
					}catch(org.json.JSONException e) {
						logE(e);
					}
					newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", thevalue) ) );
					log("Placing value: " + thevalue + " for ProductType");
				}else {
					log("No SAP_BEHVO found, placing value 1.");
					newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "1") ) );
				}
			/****************/
			
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroup",  new org.json.JSONObject().put("_code", itemGroup) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("Section",  new org.json.JSONObject().put("_code", section) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("Direction",  new org.json.JSONObject().put("_code", direction) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroupIAConfidenceDir", String.valueOf( jsonResponse.optDouble("direction_confidence", 0d) ) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroupIAConfidenceSec", String.valueOf( jsonResponse.optDouble("section_confidence", 0d) ) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroupIAConfidenceIG",  String.valueOf( jsonResponse.optDouble("item_group_confidence", 0d) ) ) );
		}catch(Exception e) {
			genericFieldErrors.put(new org.json.JSONObject().put("message", "Error al calcular grupo de artículos desde la IA.").put("fields", new org.json.JSONArray() /* .put("ProductTypeSAP").put("Name") */ ));
			logE(e);
		}
		log("La IA took: " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
	}


	private String lookupValue(String value, String standardizationDictionary) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", standardizationDictionary).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] partes = null;
			while((line = br.readLine()) != null) {
				partes = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(partes.length == 2)
					if(partes[0].equals(value)) return partes[1];
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
//		String rr = null;
//		org.json.JSONObject resp = null;
//		org.json.JSONArray rows = null;
//		rr = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'" + standardizationDictionary + "'", "UTF-8")
//			+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8") + "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + standardizationDictionary + "\" and StandardizationValue.Value equals \"" + value + "\"", "UTF-8"), null);
//		log("Value: " + value + " in " + standardizationDictionary + ": " + rr);
//		resp = new org.json.JSONObject(rr);
//		rows = resp.getJSONArray("rows");
//		if(rows.length() > 0) {
//			return rows.getJSONObject(0).getJSONArray("values").getString(0);
//		}
		return null;
	}

	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
					+ "] (" + myId + ") " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
	public static void main(String[] args) {
//		NameAndProductName nn = new NameAndProductName("1698767481747223", "", 0, new org.json.JSONArray());
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("http://172.18.237.162:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp00 = new java.util.TreeMap<>();
		qp00.put("fields", "Product2G.ProductNo,Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier,Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es)");
		qp00.put("items", "'" + "1698767481747223" + "'@1");
		rw.collectData("list", "Product2G", null, "byItems", qp00, row -> {
			System.out.println(row);
		}, System.out::println);
	}

}
