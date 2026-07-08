package mx.com.liverpool.p360.services.core;

import org.json.JSONObject;

public class GetTemplateInformation {

	private String prevBusiness = null;
	private String prevCharacteristic = null;
	private String prevCharacteristicIdentifier = null;
	private org.json.JSONObject jsonProperties = new org.json.JSONObject();
	private org.json.JSONObject globalProperties = new org.json.JSONObject();
	private java.util.Map<String, String> properties = new java.util.TreeMap<>();
	private String allowedBusiness = null;
	private String sendToVendorCenter = null;
	private String structureGroupId;

	private String response = null;

	private String baseUrl = "http://172.18.237.162:1512/rest/V2.0";
	private String creationType = "CreateProposal";
	private String extraAttributeValues = "";

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public String handleStart(String[] args, boolean aSAPInt) throws ArrayIndexOutOfBoundsException, ServiceUnavailableException {
		return handleStart(args);
	}
	
	public String processRequest(String plantilla, String negocio, String structureFeatures, String baseUrl, String encoded, String creationType) throws ServiceUnavailableException {
		try {
			RESTWorkshop rw = new RESTWorkshop();
			if(baseUrl != null)
				rw.setBaseUrl(baseUrl);
			if(encoded != null) {
				rw.getRc().getHeader().put("Authorization", "Basic: " + encoded);
			}
			if (creationType == null)
		    {
		        creationType = this.creationType;
		    }
			log("Using baseUrl: " + rw.getBaseUrl());
			rw.putParameter("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
			rw.putParameter("fields", 
					   "StandardizationValue.StructureGroup->LookupValue.Code"
					+ ",StandardizationValue.StructureGroup->LookupValueLang.Name(es)"
					+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
					+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
					+ ",StandardizationValue.PropertyValue"
					+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
					+ ",StandardizationValueLog.ModificationDate(PIM)"
					+ ",StandardizationValueLog.CreationDate(PIM)"
					+ ",StandardizationValue.Characteristic->Characteristic.DataType"
					+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
				);
			rw.putParameter("query", 
					  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
					+ " and StandardizationValue.StructureGroup->LookupValue.Code equals \"" + plantilla + "\" and StandardizationValue.Characteristic->Characteristic.IsActive = true"
					+ " and StandardizationValue.CreationType->LookupValue.Code equals \"" + creationType + "\""
				);
			rw.putParameter("orderBy", "2-ASC");
			rw.putParameter("pageSize", "1200");
			java.util.Date lastChangeDateTemplateCharacteristicMetadata = null;
			java.util.Date currDate = null;
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			org.json.JSONObject response = null;
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			int totalSize = 0;
			int currentIndex = 0;
			java.util.Map<String, org.json.JSONObject> atributos = new java.util.TreeMap<>();
			org.json.JSONObject detail = new org.json.JSONObject();
			org.json.JSONArray prevValues = null;
			java.util.Map<String, org.json.JSONArray> secciones = new java.util.TreeMap<>();
			org.json.JSONArray seccion = null;
			String currentSection = null;
			java.util.Map<String, String> translation = cargaMapaDeEquivalenciaDeSeccionDeVendorCenter(baseUrl, rw.getRc().getHeader().get("Authorization")); // SKUs sin error divided by SKUs enriquecidos
			log("Saying hi... =)");
			java.util.Date lastChangeDateGlobalMetadata = addGlobalData(negocio, atributos, baseUrl, rw.getRc().getHeader().get("Authorization"));
			org.json.JSONObject helperDetail = null;
			do {
				rw.putParameter("startIndex", String.valueOf(currentIndex));
				response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
				if(response != null) {
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						values = rows.getJSONObject(i).getJSONArray("values");
						if(prevValues != null && !prevValues.getString(2).equals(values.getString(2))) {
							try {
								currDate = sdf.parse( processDateTimeFromP360( prevValues.getString(!"".equals(prevValues.getString(6)) ? 6 : 7)) );
								lastChangeDateTemplateCharacteristicMetadata = lastChangeDateTemplateCharacteristicMetadata == null ? currDate : lastChangeDateTemplateCharacteristicMetadata.compareTo(currDate) < 0 ? currDate : lastChangeDateTemplateCharacteristicMetadata;
							}catch(java.text.ParseException e) {
								e.printStackTrace();
							}
							if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") /* && !globalSections.contains( detail.getString("vendorCenterSection")) */ ) {
								helperDetail = atributos.get(prevValues.getString(2));
								if(helperDetail != null) {
									for(String nm : org.json.JSONObject.getNames(detail)) {
										helperDetail.put(nm, detail.get(nm));
									}
									atributos.put(prevValues.getString(2), helperDetail);
								}else {
									atributos.put(prevValues.getString(2), detail);
								}
							}
							detail = new org.json.JSONObject();
						}
						detail.put("templateId", values.getString(0));
						detail.put("templateName", values.getString(1));
						detail.put("characteristic", values.getString(2));
						detail.put("friendlyName", values.getString(5));
						detail.put(values.getString(3), values.getString(4));
						detail.put("dataType", "LOOKUP".equals(values.getString(8)) ? "List of Values" : values.getString(8) );
						if("LOOKUP".equals(values.getString(8))) {
							detail.put("listofValues", values.getString(9));
						}
						prevValues = values;
					}
					currentIndex += response.getInt("pageSize");
					log(currentIndex + "/" + totalSize);
				}else {
					log("ERR: " + rw.getRawResponse());
					if(rw.getException() != null) {
						logE(rw.getException());
					}
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
			if(detail.length() > 0) {
				if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") /* && !globalSections.contains( detail.getString("vendorCenterSection")) */ ) {
					atributos.put(prevValues.getString(2), detail);
				}
				detail = null;
			}
			if(lastChangeDateGlobalMetadata != null) {
				lastChangeDateTemplateCharacteristicMetadata = lastChangeDateTemplateCharacteristicMetadata == null ? lastChangeDateGlobalMetadata : lastChangeDateTemplateCharacteristicMetadata.compareTo(lastChangeDateGlobalMetadata) < 0 ? lastChangeDateGlobalMetadata : lastChangeDateTemplateCharacteristicMetadata;
			}else {
				log("Got null date from global part.");
			}
			lastChangeDateGlobalMetadata = getLastChangeDateStructureGroup(plantilla, baseUrl, rw.getRc().getHeader().get("Authorization"));
			lastChangeDateTemplateCharacteristicMetadata = lastChangeDateTemplateCharacteristicMetadata == null ? lastChangeDateGlobalMetadata : lastChangeDateTemplateCharacteristicMetadata.compareTo(lastChangeDateGlobalMetadata) < 0 ? lastChangeDateGlobalMetadata : lastChangeDateTemplateCharacteristicMetadata;
			org.json.JSONArray dependentAttributes = null;
			for(java.util.Map.Entry<String, org.json.JSONObject> entry : atributos.entrySet()) {
				entry.getValue().put("name", entry.getKey());
				if(entry.getValue().has("dependentAttribute")) {
					if(atributos.get(entry.getValue().getString("dependentAttribute")) == null) {
						log("Not having \"dependentAttribute\": " + entry.getValue().getString("dependentAttribute"));
					}else {
						if(atributos.get(entry.getValue().getString("dependentAttribute")).has("dependentAttributes")) {
							dependentAttributes = atributos.get(entry.getValue().getString("dependentAttribute")).getJSONArray("dependentAttributes");
						}else {
							dependentAttributes = new org.json.JSONArray();
							atributos.get(entry.getValue().getString("dependentAttribute")).put("dependentAttributes", dependentAttributes);
						}
						dependentAttributes.put(entry.getValue());
						dependentAttributes = null;
					}
				}else {
					currentSection = entry.getValue().getString("vendorCenterSection");
					seccion = secciones.get(currentSection);
					if(seccion == null) {
						seccion = new org.json.JSONArray();
						secciones.put(currentSection, seccion);
					}
					seccion.put(entry.getValue());
				}
				if(!entry.getValue().has("dependentAttributes")) {
					entry.getValue().put("dependentAttributes", new org.json.JSONArray());
				}
				entry.getValue().remove("vendorCenterSection");
				entry.getValue().remove("templateId");
				entry.getValue().remove("templateName");
			}
			org.json.JSONObject masterObject = new org.json.JSONObject();
			secciones.forEach((k,v) -> masterObject.put(translation.get( k ),v));
			try {
				masterObject.put("lastModified", sdf.format(lastChangeDateTemplateCharacteristicMetadata));
			}catch(NullPointerException e) {
				masterObject.put("lastModified", sdf.format(new java.util.Date()));
			}
			if(structureFeatures != null && !"".equals(structureFeatures)) {
				getExtraInformation(masterObject, structureFeatures, plantilla, baseUrl, rw.getRc().getHeader().get("Authorization"));
				return stringJSON(masterObject, new String[]{"producto", "basicData", "datosVenta","attributes", "logisticData", "photos", "multiMedia", "header", "lastModified","extraInformation"});
			}else {
				return stringJSON(masterObject, new String[]{"producto", "basicData", "datosVenta","attributes", "logisticData", "photos", "multiMedia", "header", "lastModified"});
			}
		}catch(NullPointerException e) {
			logE(e);
			throw e;
		}
	}
	
	private java.util.Map<String, String> cargaMapaDeEquivalenciaDeSeccionDeVendorCenter(String baseUrl, String auth) throws ServiceUnavailableException{
		java.util.Map<String, String> translation = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		rw.addHeader("Authorization", auth);
		rw.putParameter("dictionary", "SeccionesEntradaUnicaCatalogacion");
		rw.putParameter("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary");
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length() ; i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				translation.put(values.getString(0), values.getString(1));
			}
		}else {
			log("ERR: " + rw.getRawResponse());
			if(rw.getException() != null) {
				logE(rw.getException());
			}
		}
		return translation;
	}
	
	private String processDateTimeFromP360(String rawDate) {
		return rawDate.replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.");
	}
	
	public java.util.Date getLastChangeDateStructureGroup(String template, String baseUrl, String authorization) throws ServiceUnavailableException{
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null)
			rw.setBaseUrl(baseUrl);
		rw.addHeader("Authorization", authorization);
		rw.putParameter("structure", "PrimaryProductTaxonomy");
		rw.putParameter("fields", 
				     "StructureGroupLog.ModificationDate(PIM)"
				   + ",StructureGroupLog.CreationDate(PIM)"
			);
		rw.putParameter("query", 
				  "StructureGroup.Identifier equals \"" + template + "\""
			);
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		response = rw.makeRequest("GET", "/list/StructureGroup/bySearch");
		if(response != null) {
			rows = response.getJSONArray("rows");
			values = rows.getJSONObject(0).getJSONArray("values");
			log("From last change in StructureGroup: " + values);
			try{
				return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse( processDateTimeFromP360( values.getString( "".equals(values.getString(0)) ? 1 : 0) ) );
			}catch(java.text.ParseException e) {
				e.printStackTrace();
			}
		}else {
			log("ERR: " + rw.getRawResponse());
			if(rw.getException() != null) {
				logE(rw.getException());
			}
		}
		return null;
	}
	
	private java.util.Date addGlobalData(String negocio, java.util.Map<String, org.json.JSONObject> attributeDetails, String baseUrl, String auth) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null)
			rw.setBaseUrl(baseUrl);
		rw.addHeader("Authorization", auth);
		rw.putParameter("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
		rw.putParameter("fields", 
				   "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
				+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValueLog.ModificationDate(PIM)"
				+ ",StandardizationValueLog.CreationDate(PIM)"
			);
		rw.putParameter("query", 
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
			);
		rw.putParameter("orderBy", "0-ASC");
		rw.putParameter("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		org.json.JSONObject detail = new org.json.JSONObject();
		org.json.JSONArray prevValues = null;
		java.util.Date currDate = null;
		java.util.Date lastChangeDateTemplateCharacteristicMetadata = null;
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				log("TZ: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(prevValues != null && !prevValues.getString(0).equals(values.getString(0))) {
						try {
							currDate = sdf.parse( processDateTimeFromP360( prevValues.getString(!"".equals(prevValues.getString(4)) ? 4 : 5)) );
							lastChangeDateTemplateCharacteristicMetadata = lastChangeDateTemplateCharacteristicMetadata == null ? currDate : lastChangeDateTemplateCharacteristicMetadata.compareTo(currDate) < 0 ? currDate : lastChangeDateTemplateCharacteristicMetadata;
						}catch(java.text.ParseException e) {
							e.printStackTrace();
						}
						if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") ) {
							attributeDetails.put(prevValues.getString(0), detail);
						}
						detail = new org.json.JSONObject();
					}
					detail.put("characteristic", values.getString(0));
					detail.put("friendlyName", values.getString(1));
					detail.put(values.getString(2), values.getString(3));
					prevValues = values;
				}
			}else {
				log("ERR: " + rw.getRawResponse());
				if(rw.getException() != null) {
					logE(rw.getException());
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(detail.length() > 0) {
			if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") ) {
				attributeDetails.put(prevValues.getString(0), detail);
			}
			detail = null;
		}
		return lastChangeDateTemplateCharacteristicMetadata;
	}

	public void getExtraInformation(org.json.JSONObject details, String fields, String plantilla, String baseUrl, String auth) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		rw.addHeader("Authorization", auth);
		String[] pcs = fields.split(",");
		StringBuilder sb = new StringBuilder();
		for(String pc : pcs) {
			sb.append(sb.length() == 0 ? "" : ",").append("StructureGroupAttributeValue.Value(\"" + pc + "\",es,DEFAULT)");
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", sb.toString());
		qp.put("structure", "PrimaryProductTaxonomy");
		qp.put("query", "StructureGroup.Identifier equals \"" + plantilla + "\"");
		org.json.JSONObject r = rw.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
		org.json.JSONObject extraInfo = new org.json.JSONObject();
		if(r != null) {
			org.json.JSONArray rws = r.getJSONArray("rows");
			org.json.JSONArray values = null;
			if(rws.length() > 0) {
				values = rws.getJSONObject(0).getJSONArray("values");
				for(int k=0; k<pcs.length; k++) {
					extraInfo.put(pcs[k], values.getString(k));
				}
			}
			log(".. " + extraInfo);
		}else {
			log(". " + rw.getRawResponse());
		}
		details.put("extraInformation", extraInfo);
	}
	
	public String handleStart(String[] args) throws ArrayIndexOutOfBoundsException, ServiceUnavailableException {
		String template = null;
		String business = null;
		String creationType = null;
		String encoded = null;
		if(args.length < 1) {
			System.out.println(new org.json.JSONObject().put("message", "Missing query parameters: template, business"));
			return null;
		}else if(args.length < 2) {
			System.out.println(new org.json.JSONObject().put("message", "Incomplete query parameters, required: template, business"));
		}
		log("Working with: " + java.util.Arrays.asList(args));
		template = args[0];
		business = args[1];
		if(args.length > 2) {
			creationType = "".equals( args[2] ) ? "" : args[2];
			if(args.length > 3) {
				extraAttributeValues = args[3];
				log("Assigned: " + extraAttributeValues);
				if(args.length > 4) {
					baseUrl = args[4];
					workshop.setBaseUrl(baseUrl);
					if(args.length > 5) {
						encoded = args[5];
						workshop.getRc().getHeader().put("Authorization", "Basic " + encoded);
					}
				}
			}
		}
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("fields",
				  "StandardizationValue.StructureGroup->LookupValue.Code,"
				+ "StandardizationValue.Characteristic->CharacteristicLang.Name(es),"
				+ "StandardizationValue.Property->LookupValueLang.Name(es),"
				+ "StandardizationValue.PropertyValue,"
				+ "StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("orderBy", "4-ASC");
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
						+ "StandardizationValue.StructureGroup->LookupValue.Code equals \"" + template + "\" and "
						+ "StandardizationValue.CreationType->LookupValue.Code equals \"" + this.creationType + "\"");
		qp.put("pageSize", "750");
		log(qp.get("query"));
		java.util.LinkedList<org.json.JSONArray> rws = new java.util.LinkedList<>();
		String aux = null;
		java.util.regex.Matcher m = null;
		java.util.regex.Matcher m1 = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("(^[A-Z]+)[A-Z0-9](.+)?");
		java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("(^[A-Z])[^A-Z0-9](.+)?");
		String ext = null;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			log("TotalSize: " + totalSize + " (properties for template)");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
					aux = values.getString(2);
					aux = aux.replaceAll("[^A-Za-z]+", "");
					m = p.matcher(aux);
					m1 = p1.matcher(aux);
					if(m.find()) {
						ext = m.group(1).toLowerCase();
						values.put(2, ext + aux.substring(ext.length()) );
					}else if(m1.find()) {
						ext = m1.group(1).toLowerCase();
						values.put(2, ext + aux.substring(ext.length()) );
					}
					rws.addLast(values);
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		java.util.Collections.sort(rws, (o1,o2)->o1.getString(1).compareTo(o2.getString(1)));
		for(org.json.JSONArray vals : rws) {
			values = vals;
			handleRow(values.getString(1), values.getString(0), values.getString(2), values.getString(3), business, template, creationType, values.getString(4));
		}
		return prepareResponse();
	}

	private void handleRow(String characteristic, String structureGroupId, String property, String propertyValue, String business, String templateId, String creationType, String characteristicIdentifier) {
		this.structureGroupId = structureGroupId;
		log("$$$$$$$$$$$$$$$$$$$$$$$$$ " + characteristicIdentifier);
		if(prevCharacteristicIdentifier != null && !prevCharacteristicIdentifier.equals(characteristicIdentifier)){
			allowedBusiness = properties.get("allowedBusiness");
			sendToVendorCenter = properties.get("senttoVendorCenter");
			if("EsSostenible".equals(prevCharacteristicIdentifier)) {
				log("<::::>Allowed Business: " + allowedBusiness + ", Send to Vendor Center: " + sendToVendorCenter + ", Prev Business: " + prevBusiness + ", PI: " + prevCharacteristicIdentifier);
			}
			if(allowedBusiness != null && allowedBusiness.toUpperCase().contains(prevBusiness.toUpperCase()) && sendToVendorCenter != null && "1".equals(sendToVendorCenter)){
				try{
					for(java.util.Map.Entry<String, String> entry : properties.entrySet()){
						jsonProperties.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().trim());
					}
					globalProperties.put(prevCharacteristicIdentifier, jsonProperties.put("characteristic", prevCharacteristic).put("characteristicIdentifier", prevCharacteristicIdentifier));
				}catch(org.json.JSONException e){
				}
			}
			jsonProperties = new org.json.JSONObject();
			properties.clear();
		}
		if(property != null) {
			properties.put(property, propertyValue);
		}
		prevBusiness = business;
		prevCharacteristic = characteristic;
		prevCharacteristicIdentifier = characteristicIdentifier;
		if("EsSostenible".equals(characteristicIdentifier)) {
			log("## " + characteristic + " ## " + characteristicIdentifier + " #Template: " + structureGroupId + " #Property: " + property + ": " + propertyValue);
		}
	}

	private String prepareResponse() throws ServiceUnavailableException {

		response = "{}";
		allowedBusiness = properties.remove("allowedBusiness");
		sendToVendorCenter = properties.get("senttoVendorCenter");

		if("EsSostenible".equals(prevCharacteristicIdentifier)) {
			log("<::::>Allowed Business: " + allowedBusiness + ", Send to Vendor Center: " + sendToVendorCenter + ", Prev Business: " + prevBusiness + ", PI: " + prevCharacteristicIdentifier);
		}
		if(allowedBusiness != null && allowedBusiness.toUpperCase().contains(prevBusiness.toUpperCase()) && sendToVendorCenter != null && "1".equals(sendToVendorCenter)){
			try{
				for(java.util.Map.Entry<String, String> entry : properties.entrySet()){
					jsonProperties.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().trim());
				}
				if(globalProperties == null || prevCharacteristic == null){
					log("Maybe this was null (prevCharacteristic)" + prevCharacteristic);
				}
				globalProperties.put(prevCharacteristic.replaceAll(".+\\[", "").replaceAll("\\]", ""), jsonProperties.put("characteristic", prevCharacteristic).put("characteristicIdentifier", prevCharacteristicIdentifier));
			}catch(org.json.JSONException e){
			}
		}

		org.json.JSONArray header = new org.json.JSONArray();
		org.json.JSONArray basicData = new org.json.JSONArray();
		org.json.JSONArray datosVenta = new org.json.JSONArray();
		org.json.JSONArray attributes = new org.json.JSONArray();
		org.json.JSONArray logisticData = new org.json.JSONArray();
		org.json.JSONArray photos = new org.json.JSONArray();
		org.json.JSONArray multiMedia = new org.json.JSONArray();
		org.json.JSONArray producto = new org.json.JSONArray();
		String parent = null;
		String vcs = null;
		org.json.JSONObject json = null;

		RestClient rc = new RestClient();
		java.util.Map<String, String> headers = new java.util.HashMap<>();
		headers.put( "Content-Type", "application/json" );
		headers.put( "Accept", "application/json" );
		headers.put( "Authorization", "Basic cmVzdDpoZWlsZXI=" );
		headers.put( "Accept-Language", "es_ES");

		String rawResp = null;
		org.json.JSONObject resp = null;
		String lastModified = null;
		try{
			rawResp = rc.getRequest( "GET", workshop.getBaseUrl() + "/list/StructureGroup/bySearch?structure=PrimaryProductTaxonomy&query=" + 	java.net.URLEncoder.encode("StructureGroup.Identifier equals \"" + structureGroupId + "\"", "UTF-8") + "&metaData=true&fields=StructureGroup.LastModified&pageSize=2&startIndex=" + 0, null, headers );
			resp = new org.json.JSONObject(rawResp);
			lastModified = resp.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
		}catch(Exception e){
			logE(e);
		}
		org.json.JSONObject jsonRes = new org.json.JSONObject();
		if(globalProperties.length() > 0) {
			try{

				java.util.LinkedList<org.json.JSONObject> list = new java.util.LinkedList<>();
				for(String keyName : org.json.JSONObject.getNames(globalProperties)){
					json = globalProperties.getJSONObject(keyName);
					json.put("dependentAttributes", new org.json.JSONArray());
					json.remove("listofValuesValidValues");
					json.remove("ecC");
					json.remove("sH");
					json.remove("sendtoVendorCenter");
					vcs = (String) json.remove("vendorCenterSection");
					vcs = vcs == null ? "" : vcs;
					parent = json.has("dependentAttribute") ? json.getString("dependentAttribute") : null;
					if(parent != null){
						list.addLast(json);
						continue;
					}

					if("Header".equals(vcs)){
						header.put(json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic")));
					}else if("Atributos".equals(vcs)){
						attributes.put(json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic")));
					}else if("Datos de Venta".equals(vcs)){
						datosVenta.put(json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic")));
					}else if("Datos Básicos".equals(vcs)){
						basicData.put(json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic")));
					}else if("Fotografías".equals(vcs)){
						photos.put(json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic")));
					}else if("Multimedia".equals(vcs)){
						multiMedia.put(json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic")));
					}else if(vcs.startsWith("Datos Logísticos")){
						logisticData.put(json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic")));
					}else if(vcs.startsWith("Producto")){
						producto.put(json.put("name", json
								.getString("characteristicIdentifier"))
							.put("friendlyName",
									json.getString("characteristic")));
					}
				}
				java.util.Iterator<org.json.JSONObject> iter = list.listIterator();
				org.json.JSONObject holder = null;
				while(iter.hasNext()){
					json = iter.next();
					json.put("name", json.getString("characteristicIdentifier")).put("friendlyName", json.getString("characteristic"));
					iter.remove();
					if(globalProperties.has(json.getString("dependentAttribute"))){
						holder = globalProperties.getJSONObject(json.getString("dependentAttribute"));
						holder.getJSONArray("dependentAttributes").put(json);
					}
				}
				jsonRes.put("producto", producto).put("basicData", basicData).put("datosVenta", datosVenta).put("attributes", attributes).put("logisticData", logisticData).put("photos", photos).put("multiMedia", multiMedia).put("header", header);
				if(lastModified != null) {
					jsonRes.put("lastModified", lastModified);
				}
				if(!"".equals(extraAttributeValues)) {
					log("Working with: " + extraAttributeValues);
					String[] pcs = extraAttributeValues.split(",");
					StringBuilder sb = new StringBuilder();
					for(String pc : pcs) {
						sb.append(sb.length() == 0 ? "" : ",").append("StructureGroupAttributeValue.Value(\"" + pc + "\",es,DEFAULT)");
					}
					java.util.Map<String, String> qp = new java.util.TreeMap<>();
					qp.put("fields", sb.toString());
					qp.put("structure", "PrimaryProductTaxonomy");
					qp.put("query", "StructureGroup.Identifier equals \"" + structureGroupId + "\"");
					org.json.JSONObject r = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
					org.json.JSONArray rws = r.getJSONArray("rows");
					org.json.JSONArray values = null;
					org.json.JSONObject extraInfo = new org.json.JSONObject();
					if(rws.length() > 0) {
						values = rws.getJSONObject(0).getJSONArray("values");
						for(int k=0; k<pcs.length; k++) {
							extraInfo.put(pcs[k], values.getString(k));
						}
					}
					log("Added extra info: " + extraInfo);
					jsonRes.put("extraInformation", extraInfo);
					response = stringJSON(jsonRes, new String[]{"producto", "basicData", "datosVenta","attributes", "logisticData", "photos", "multiMedia", "header", "lastModified","extraInformation"});
				}else {
					response = stringJSON(jsonRes, new String[]{"producto", "basicData", "datosVenta","attributes", "logisticData", "photos", "multiMedia", "header", "lastModified"});
				}
			}catch(org.json.JSONException e){
				response = new org.json.JSONObject().put("messagea", e.toString()).toString();
			}catch(NullPointerException e){
				logE(e);
				response = new org.json.JSONObject().put("messagea", "There was a null in global properties json names. (" + globalProperties.length() + ")").toString();
			}
		} else {

		}
		System.out.println(response);

		jsonProperties = new org.json.JSONObject();
		globalProperties = new org.json.JSONObject();
		properties = new java.util.TreeMap<>();
		prevCharacteristic = null;

		return response;
	}

	private String stringJSON(JSONObject j, String[] names){
		StringBuilder sb = new StringBuilder();
		int i = 0;
		org.json.JSONArray photos = null;
		for(String name : names){
			if("photos".equals(name)) {
				photos = j.has("photos") ? j.getJSONArray("photos") : new org.json.JSONArray();
				java.util.Map<String, org.json.JSONObject> map = new java.util.TreeMap<>();
				for(int ji=0; ji<photos.length(); ji++) {
					map.put(photos.getJSONObject(ji).getString("name"), photos.getJSONObject(ji));
				}
				photos = new org.json.JSONArray();
				if(map.get("ProductImage") != null) {
					photos.put(map.get("ProductImage"));
				}
				if(map.get("ProductImageDetail") != null) {
					photos.put(map.get("ProductImageDetail"));
				}
				if(map.get("Illustration") != null) {
					photos.put(map.get("Illustration"));
				}
				if(map.get("ProductImageSmosh") != null) {
					photos.put(map.get("ProductImageSmosh"));
				}
			}
			sb.append(i == 0 ? "" : ",").append("\"").append(name).append("\":").append(!j.has(name) ? "[]" : "photos".equals(name) ? photos :  j.get(name) instanceof org.json.JSONArray ? j.getJSONArray(name) : j.get(name) instanceof JSONObject ? j.getJSONObject(name) : "\"" + String.valueOf(j.get(name)).replaceAll("(?=\")", "\\\\") + "\"");
			i++;
		}
		return "{" + sb.toString() + "}";
	}

	private void log(String message){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/get_template_information.log", true)))){
		  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date())) + "] (" + this.hashCode() + ") " + message);
		}catch(java.io.IOException e){}
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/get_template_information.log", true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}

}
