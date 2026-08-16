package mx.com.liverpool.p360.services.core;

import java.io.Closeable;
import java.io.IOException;

import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class TemplateAttributesForo implements Closeable {


	private DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
		
		@Override
		public void logE(Exception e) {
			TemplateAttributesForo.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			TemplateAttributesForo.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);

	public Object processRequest(String[] args) {
		org.json.JSONObject generalResponse = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		String templateId = null;
		String baseURL = null;
		String encoded = null;
		String fields = null;
		RestClient rc = null;
		java.util.Set<String> masterDataCharacteristics = new java.util.TreeSet<>();
		java.util.Set<String> atributosInternet = new java.util.TreeSet<>();
		java.util.Set<String> atributosSAP = new java.util.TreeSet<>();
		org.json.JSONObject characteristics = new org.json.JSONObject();
		org.json.JSONObject properties = new org.json.JSONObject();
		String currentId = null;
		try{
			templateId = args[0];
			baseURL = args[1];
			encoded = args[2];
			fields = args[3];
			String r = null;
			r = dr.getTemplateCharacteristicMetaDataByTemplate(new org.json.JSONArray().put(templateId));
			try {
				org.json.JSONObject jr = new org.json.JSONObject(r);
				org.json.JSONArray items = jr.getJSONArray("items");
				log("Rec: " + characteristics);
				log("Val: " + r);
				characteristics = moveToUpper( items.getJSONObject(0) );
			}catch(org.json.JSONException e) {
				logE(e);
			}
			rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);
//			do {
//				rawResponse = rc.getRequest("GET", baseURL + "/list/StandardizationValue/bySearch?"
//						+ "dictionaryProxy=" + java.net.URLEncoder.encode("'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'", "UTF-8")
//						+ "&query="
//						+ java.net.URLEncoder.encode(
//								  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" "
//								+ "and StandardizationValue.StructureGroup->LookupValue.Code equals \"" + templateId + "\" "
//								+ "and StandardizationValue.CreationType equals CreateProposal", "UTF-8")
//						+ "&fields=" + java.net.URLEncoder.encode(
//								  "StandardizationValue.Characteristic->Characteristic.Identifier"
//								+ ",StandardizationValue.Property->LookupValue.Code"
//								+ ",StandardizationValue.PropertyValue"
//							, "UTF-8")
//						+ "&orderBy=0-ASC"
//						+ "&metaData=true"
//						+ "&pageSize=900"
//						+ "&startIndex=" + currentIndex
//						, null);
//				response = new org.json.JSONObject(rawResponse);
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					currentIndex++;
//					values = rows.getJSONObject(i).getJSONArray("values");
//					currentId = values.getString(0);
//					templateCharacteristics.add(currentId);
//					if(prevId != null && !prevId.equals(currentId)) {
//						characteristics.put(prevId, properties);
//						properties = new org.json.JSONObject();
//					}
//					properties.put(values.getString(1), values.getString(2));
//					prevId = currentId;
//				}
//				if(properties.length() > 0) {
//					characteristics.put(prevId, properties);
//				}
//			}while(currentIndex < totalSize);
//			currentIndex = 0;

			if(characteristics.length() == 0) {
				System.out.println(generalResponse =  new org.json.JSONObject().put("Error", "No characteristic information found for template: " + templateId));
				return generalResponse;
			}
/*
			do {
				rawResponse = rc.getRequest("GET", baseURL + "/list/CharacteristicAttributeGroup/bySearch?"
						+ "dictionaryProxy=" + java.net.URLEncoder.encode("'CharacteristicAttributeGroup'", "UTF-8")
						+ "&query="
						+ java.net.URLEncoder.encode(
								  "CharacteristicAttributeGroup.Dictionary->GeneralPurposeDictionary.Identifier equals \"CharacteristicAttributeGroup\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("CharacteristicAttributeGroup.Characteristic->Characteristic.Identifier,CharacteristicAttributeGroup.AttributeGroup", "UTF-8")
						+ "&orderBy=0-ASC"
						+ "&metaData=true"
						+ "&pageSize=900"
						+ "&startIndex=" + currentIndex
						, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					currentId = values.getString(0);
					attributeGroups = values.getJSONArray(1);
					for(int j=0; j<attributeGroups.length(); j++) {
						if(gasap.equals(attributeGroups.getString(j))) {
							atributosSAP.add(currentId);
						}
						if(attInt.contains(attributeGroups.getString(j))) {
							atributosInternet.add(currentId);
						}
					}
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
*/
			java.util.Map<String, String> attr = null;
			attr = seleccionaLasDesas("CategorySpecificAttributesLVP", baseURL, rc.getHeader().get("Authorization"));
			attr.keySet().forEach(k->atributosInternet.add(k));
			attr = seleccionaLasDesas("CategorySpecificAttributesS4H", baseURL, rc.getHeader().get("Authorization"));
			attr.keySet().forEach(k->atributosInternet.add(k));
			attr = seleccionaLasDesas("CategorySpecificAttributesSAP", baseURL, rc.getHeader().get("Authorization"));
			attr.keySet().forEach(k->atributosSAP.add(k));

			do {
				rawResponse = rc.getRequest("GET", baseURL + "/list/Characteristic/bySearch?"
						+ "&query="
						+ java.net.URLEncoder.encode("Characteristic.Category equals \"Master Data\" and not Characteristic.Identifier wildcard \"%_Rechazo\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "CharacteristicLang.Name(es),"
								+ "CharacteristicLang.Description(es),"
								+ "CharacteristicLang.DefaultValue(es),"
								+ "Characteristic.Order,"
								+ "Characteristic.LowerBound,"
								+ "Characteristic.UpperBound,"
								+ "Characteristic.IsReadOnly,"
								+ "Characteristic.IsMultiValue,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Lookup->Lookup.Identifier"
							, "UTF-8")
						+ "&orderBy=0-ASC"
						+ "&metaData=true"
						+ "&pageSize=900"
						+ "&startIndex=" + currentIndex
						, null);
				try {
					response = new org.json.JSONObject(rawResponse);
					if(response != null && response.has("totalSize")) {
						totalSize = response.getInt("totalSize");
						rows = response.getJSONArray("rows");
						for(int i=0; i<rows.length(); i++) {
							currentIndex++;
							values = rows.getJSONObject(i).getJSONArray("values");
							currentId = values.getString(0);
							masterDataCharacteristics.add(currentId);
							properties = new org.json.JSONObject();
							properties.put("FriendlyName", values.getString(1));
							properties.put("AttributeHelpInformation", values.getString(2));
							if(values.getJSONArray(3).length() > 0 && (values.getJSONArray(3).length() == 1 && !"".equals(values.getJSONArray(3).get(0)))) {
								properties.put("DefaultValue", values.get(3));
							}
							properties.put("Order", values.get(4));
							properties.put("Min", values.get(5));
							properties.put("Max", values.get(6));
							properties.put("IsEditable", !values.getBoolean(7) ? "1" : "0");
							properties.put("IsMultiValue", values.getString(8));
							properties.put("DataType", values.getString(9));
							if(values.getString(9).equals("LOOKUP")) {
								properties.put("ListOfValues", values.getString(10));
							}
							properties.put("IsMandatory", values.getInt(5) > 0);
							if(values.getString(9).equals("TEXT")) {
								properties.put("MaxLength", 2000);
							}
		
							characteristics.put(currentId, properties);
						}
					}else {
						log("ERROR: " + rawResponse);
					}
				}catch(org.json.JSONException e) {
					log("ERROR: " + rawResponse);
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;

			org.json.JSONObject atributosInternetObject = new org.json.JSONObject();
			org.json.JSONObject atributosSAPObject = new org.json.JSONObject();
			org.json.JSONObject obj = null;
			for(String atributoInternet : atributosInternet) {
				if(characteristics.has(atributoInternet)) {
					obj = (org.json.JSONObject) characteristics.remove(atributoInternet);
					if(obj.has("IsEditable")) {
						obj.put("IsEditable", "1".equals(obj.getString("IsEditable")) ? "true" : "0".equals(obj.getString("IsEditable")) ? "false" : obj.getString("IsEditable"));
					}
					if(obj.has("senttoVendorCenter")) {
						obj.put("senttoVendorCenter", "1".equals(obj.getString("senttoVendorCenter")) ? "true" : "0".equals(obj.getString("senttoVendorCenter")) ? "false" : obj.getString("senttoVendorCenter"));
					}
					atributosInternetObject.put(atributoInternet, obj);
				}
			}
			atributosInternetObject.put("refundPolicy", characteristics.remove("refundPolicy"));
			atributosInternetObject.put("EmbedCodeWEB", characteristics.remove("EmbedCodeWEB"));
			atributosInternetObject.put("EmbedCodeWAP", characteristics.remove("EmbedCodeWAP"));
			for(String atributoSAP : atributosSAP) {
				if(characteristics.has(atributoSAP)) {
					obj = (org.json.JSONObject) characteristics.remove(atributoSAP);
					if(obj.has("isEditable")) {
						obj.put("isEditable", obj.get("isEditable") instanceof String ? "1".equals(obj.getString("isEditable")) ? "true" : "0".equals(obj.getString("isEditable")) ? "false" : obj.getString("isEditable") : obj.get("isEditable"));
					}
					if(obj.has("senttoVendorCenter")) {
						obj.put("senttoVendorCenter", "1".equals(obj.getString("senttoVendorCenter")) ? "true" : "0".equals(obj.getString("senttoVendorCenter")) ? "false" : obj.getString("senttoVendorCenter"));
					}
					atributosSAPObject.put(atributoSAP, obj);
				}
			}
			// ProductName, NameGuide, NameException
			String[] templateName = new String[1];
			templateName[0] = "";
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", "StructureGroupLang.Name(es)");
			qp.put("items", "'" + templateId + "'@'PrimaryProductTaxonomy'");
			qp.put("structure", "PrimaryProductTaxonomy");
			RESTWrapper rw = new RESTWrapper();
			rw.collectData("list", "StructureGroup", null, "byItems", qp, row -> {
				templateName[0] = row.getJSONArray("values").getString(0);
			}, this::log);
			response = new org.json.JSONObject();
			response.put("template", templateId);
			response.put("templateName", templateName[0]);
			response.put("AtributosInternet", atributosInternetObject);
			response.put("AtributosSAP", atributosSAPObject);
			generalResponse = response;
			getExtraInformation( generalResponse, fields, templateId, baseURL, "Basic " + encoded );
//			log("--->" + generalResponse);
			// EU4-113578
//			System.out.println(generalResponse = response);
		}catch(Exception e) {
			logE(e);
			System.out.println(generalResponse = new org.json.JSONObject().put("Erorr", "Couldn't parse request"));
		}
		
		return generalResponse;
	}
	
	private void getExtraInformation(org.json.JSONObject details, String fields, String plantilla, String baseUrl, String auth) {
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

	private java.util.Map<String, String> seleccionaLasDesas(String attributeGroup, String baseUrl, String auth){
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.setBaseUrl(baseUrl);
		workshop.addHeader("Authorization", auth);
		java.util.Map<String, String> lasdesas = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "Characteristics");
		qp.put("query", "LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code in (\"" + attributeGroup + "\")");
		qp.put("fields", "LookupValue.Code,LookupValueIdentifier.Code(ECC)");
		qp.put("pageSize", "2500");

		int currentIndex = 0;
		int totalSize = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					lasdesas.put(values.getString(0),values.getString(1));
				}
			}else{
				log( workshop.getRawResponse() );
				break;
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return lasdesas;
	}

	private void log(String message){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/gtet_template_foro.log", true)))){
		  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "]  " + message);
		}catch(java.io.IOException e){}
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/gtet_template_foro.log", true)))){
		  ex.printStackTrace(pw);
		}catch(java.io.IOException e){}
	}
	
	private static java.util.Map<String, String> toMap(String[] arr){
		java.util.Map<String, String> data = new java.util.HashMap<>();
		String[] a = null;
		for(int i=0; i<arr.length; i++) {
			a = arr[i].split("\t");
			data.put(a[1], a[0]);
		}
		return data;
	}
	
	private org.json.JSONObject moveToUpper(org.json.JSONObject o){
		org.json.JSONObject a = new org.json.JSONObject();
		String eq = null;
		for(String name : org.json.JSONObject.getNames(o)) {
			org.json.JSONObject d = o.getJSONObject(name);
			org.json.JSONObject p = new org.json.JSONObject();
			a.put(name, p);
			if(d.length() > 0) {
				for(String nm : org.json.JSONObject.getNames(d)) {
					eq = data.get(nm);
					if(eq == null) {
					}else {
						p.put(eq, d.get(nm));
					}
				}
			}
		}
		return a;
	}
	
	public static final java.util.Map<String, String> data = toMap( ("ATG	atg\r\n"
			+ "ATGSection	atgSection\r\n"
			+ "AttributeHelpInformation	attributeHelpInformation\r\n"
			+ "BulletAttribute	isBulletAttribute\r\n"
			+ "Business	allowedBusiness\r\n"
			+ "CaptureLevel	captureLevel\r\n"
			+ "CreationType	Tipo de Creación			tipoDeCreacion\r\n"
			+ "DataType	dataType\r\n"
			+ "DefaultValue	defaultValue\r\n"
			+ "DependentAttribute	dependentAttribute\r\n"
			+ "DependentValues	dependentValues\r\n"
			+ "DescripcionLargaPlantilla	Descripción Larga Plantilla			descripcionLargaPlantilla\r\n"
			+ "DocumentoAyudaPlantilla	documentoDeAyudaparaLaPlantilla\r\n"
			+ "ECC	ecc\r\n"
			+ "ECC16	ecc16\r\n"
			+ "EsServicio	esUnServicio\r\n"
			+ "ExpressMandatory	Mandatorio en creación express			mandatorioEnCreacionExpress\r\n"
			+ "GuiaPlantilla	Guía Plantilla			guiaPlantilla\r\n"
			+ "isConfigurable	isConfigurable\r\n"
			+ "IsEditable	isEditable\r\n"
			+ "IsFaceted	filtrableATG\r\n"
			+ "IsMandatory	isMandatory\r\n"
			+ "IsMultiselect	isMultiselect\r\n"
			+ "ListOfValues	listofValues\r\n"
			+ "ListOfValuesFilter	listofValuesValidValues\r\n"
			+ "Max	max\r\n"
			+ "MaxLength	maxLength\r\n"
			+ "Min	min\r\n"
			+ "NameEnglish	Name (English)			nameEnglish\r\n"
			+ "NumeroVersion	templateVersion\r\n"
			+ "OtroDato	otroAtributo\r\n"
			+ "PIM	pim\r\n"
			+ "PlaceholderMandatory	mandatorioEnPlaceholder\r\n"
			+ "ReglaTituloSAP	Reglas creación Titulo SAP?			reglasCreacionTituloSAP\r\n"
			+ "RelevantForATG	relevantForATG\r\n"
			+ "RequiereRepoblamiento	requiereRepoblamento\r\n"
			+ "S4H	s4h\r\n"
			+ "SentToVendorCenter	senttoVendorCenter\r\n"
			+ "Twins	twins\r\n"
			+ "UsedByAI	usedbyAI\r\n"
			+ "ValidationOrCalculationRule	validationorCalculusRule\r\n"
			+ "VariantLevel	variantLevel\r\n"
			+ "VendorCenterSection	vendorCenterSection\r\n"
			+ "VendorCenterSectionSequence	vendorCenterSectionSequence").split("\\r\\n") );

	@Override
	public void close() throws IOException {
		dastub.close();
	}
	
}
