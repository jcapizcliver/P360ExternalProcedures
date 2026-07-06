package mx.com.liverpool.p360.services.core.temp.exports;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ArmaConjuntoLookSTEP {

	private final java.util.Map<String, String> mapaDeAtributosFechas;
	
	public ArmaConjuntoLookSTEP(java.util.Map<String, String> mapaDeAtributosFechas) {
		this.mapaDeAtributosFechas = mapaDeAtributosFechas;
	}
	
	public Element handleCLK(
			String externalId,
			org.json.JSONObject data,
			java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> dataMap,
			Element webHierarchyRoot, 
			java.util.LinkedList<org.json.JSONObject> rescataLaRaiz, 
			java.util.Map<String, java.util.Map<String, org.json.JSONObject>> globalMap, 
			Document doc,
        	java.util.Map<String, Element> assetMap,
        	java.util.Map<String, java.util.LinkedList<String>> assetReferencesMap,
        	Element assets,
        	Element attributes,
        	java.util.Map<String, String> atgGroups,
        	String baseUrl
	) {
		java.util.LinkedList<org.json.JSONObject> lst = null;
		Element product = doc.createElement("Product");
		Element productCrossReference = null;
		lst = dataMap.get("FechaUltimaPublicacion");
		product.setAttribute("ID", externalId);
		product.setAttribute("UserTypeID", "ConjuntoLook");
		product.setAttribute("Republished", lst == null ? "false" : "true");
		org.json.JSONObject esLang = getEs(data.getJSONArray("lang"));
		String displayName = null;
		String description = null;
		String origin = null;
		if(esLang != null) {
			Element name = doc.createElement("Name");
			name.setTextContent( displayName = esLang.getString("descriptionShort") );
			product.appendChild(name);
			if(esLang.has("descriptionLong")){
				description = esLang.getString("descriptionLong");
			}
		}
		String[] webCategory = data.has("structureGroupMap") ? getWebCategory(data.getJSONArray("structureGroupMap")) : new String[] {};

		java.util.Map<String, org.json.JSONObject> hierarchyHelper = null;
		org.json.JSONObject entry = null;
		org.json.JSONObject entryHelper = null;
    	Element helperElement = null;
    	Element prevHelperElement = null;
    	java.util.Map<String, Element> tableroDeControl = new java.util.TreeMap<>();
    	if(webCategory != null) {
        	for (String element : webCategory) {
        		hierarchyHelper = globalMap.get(element);
        		if(hierarchyHelper != null) {
        			entry = hierarchyHelper.get(element);
        			entryHelper = entry;
        			helperElement = pacheleWeb(entryHelper, doc);
        			if(!tableroDeControl.containsKey(entryHelper.getString("identifier"))) {
        				tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
        			}
        			while(entryHelper.has("parentIdentifier") && !"".equals(entryHelper.get("parentIdentifier")) && !tableroDeControl.containsKey(entryHelper.getString("parentIdentifier"))) {
        				prevHelperElement = helperElement;
        				entryHelper = hierarchyHelper.get(entryHelper.getString("parentIdentifier"));
        				helperElement = pacheleWeb(entryHelper, doc);
        				helperElement.appendChild(prevHelperElement);
        				tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
        			}
        		}
        		Element classificationReference = null;
        		classificationReference = doc.createElement("ClassificationReference");
        		classificationReference.setAttribute("ClassificationID", element);
        		classificationReference.setAttribute("Type", "WebsiteLink");
        		classificationReference.setAttribute("Changed", "true");
        		product.appendChild(classificationReference);
        	}
    	}
    	for(org.json.JSONObject laRaiz : rescataLaRaiz) {
    		helperElement = tableroDeControl.get(laRaiz.getString("identifier"));
    		if(helperElement != null) {
    			webHierarchyRoot.appendChild(helperElement);
    		}
    	}
 		lst = dataMap.get("CLReference");
		if(lst != null) {
			Element metaData = null;
			Element value = null;
			String id = null;
			org.json.JSONArray children = null;
			org.json.JSONObject co = null;
			String sku = null;
			Integer sequence = null;
			Boolean main = null;
			String status = null;
			for(org.json.JSONObject obj : lst) {
				productCrossReference = doc.createElement("ProductCrossReference");
				productCrossReference.setAttribute("ID", externalId + obj.getJSONObject("_qualification").getString("recordKey").replaceAll("[^0-9]", ""));
				productCrossReference.setAttribute("Type", "ConjuntoLook");
				productCrossReference.setAttribute("Changed", "true");
				metaData = doc.createElement("MetaData");
				children = obj.getJSONArray("_children");
				for(int j=0; j<children.length(); j++) {
					co = children.getJSONObject(j);
					id = co.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
					if("CLReference_Status".equals(id)) {
						status = co.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						value = doc.createElement("Value");
						value.setAttribute("AttributeID", "ProductStatusInConjuntoLook");
						value.setAttribute("ID", String.valueOf(status));
						value.setAttribute("Changed", "true");
						value.setTextContent(String.valueOf(status));
						metaData.appendChild(value);
					}else if("CLReference_Sequence".equals(id)) {
						sequence = co.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getInt(0);
						value = doc.createElement("Value");
						value.setAttribute("AttributeID", "ConjuntoLookDisplaySequence");
						value.setAttribute("Changed", "true");
						value.setTextContent( padd( String.valueOf(sequence), 3) );
						metaData.appendChild(value);
					}else if("CLReference_IsMain".equals(id)) {
						main = co.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getBoolean(0);
						value = doc.createElement("Value");
						value.setAttribute("AttributeID", "ConjuntoLookArtPpal");
						value.setAttribute("ID", main ? "Y" : "N");
						value.setAttribute("Changed", "true");
						value.setAttribute("Changed", "true");
						value.setTextContent(String.valueOf(main));
						metaData.appendChild(value);
					}else if("CLReference_SKU".equals(id)) {
						sku = co.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						Element keyValue = doc.createElement("KeyValue");
						keyValue.setAttribute("KeyID", "SKUID");
						keyValue.setTextContent(sku);
						productCrossReference.appendChild(keyValue);
						String[] info = checkProductBySKU(sku, baseUrl);
						if(info != null) {
							if( info[1] != null && !"".equals(info[1]) ) {
								Element eanValue = doc.createElement("KeyValue");
								keyValue.setAttribute("KeyID", "EANKey");
								keyValue.setTextContent(info[1]);
								productCrossReference.appendChild(eanValue);
							}else if( info[2] != null && !"".equals(info[2]) ) {
								Element eanValue = doc.createElement("KeyValue");
								keyValue.setAttribute("KeyID", "EANKey");
								keyValue.setTextContent(info[2]);
								productCrossReference.appendChild(eanValue);
							}
						}
					}
				}
				value = doc.createElement("Value");
				value.setAttribute("AttributeID", "CalculatedConjuntoLookID");
				value.setAttribute("Changed", "true");
				value.setTextContent(externalId);
				metaData.appendChild(value);
				productCrossReference.appendChild(metaData);
				product.appendChild(productCrossReference);
			}
			lst = dataMap.get("ProductImage");
			if(lst != null && !lst.isEmpty()) {
				org.json.JSONObject pimg = lst.getFirst();
				org.json.JSONObject imgc = null;
				String url = null;
				if(pimg.has("_children")) {
					children = pimg.getJSONArray("_children");
					for(int a = 0; a<children.length(); a++) {
						imgc = children.getJSONObject(a);
						if( "ProductImage_URL".equals(imgc.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code")) ) {
							url = imgc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							appendMediaAsset(
									externalId,
									url,
									"PrimaryProductImage", // String assetType,
									"0000.0000.RK",
									"Imagen Producto", // String assetValueTextContent,
									"ImageURL", // String assetValueAttributeId,
									"ProductImage", // String assetUserTypeId,
									"ProductImage", // String assetKeyPrefix,
									externalId,
									pimg,
									"ProductImage", // String baseAssetTypeName,
									assetMap,
									assetReferencesMap,
									product,
									assets,
									doc,
									externalId
									);
							break;
						}
					}
				}
			}
			Element attributeValues = doc.createElement("Values");
			String charId = null;
			org.json.JSONObject characteristic = null;
			java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas = new java.util.TreeMap<>();
			org.json.JSONObject properties = null;
			for(java.util.Map.Entry<String, java.util.LinkedList<org.json.JSONObject>> dmEntry : dataMap.entrySet()) {
				if(!"SKU".equals(dmEntry.getKey()) && !"ProductImage".equals(dmEntry.getKey()) && !"CLReference".equals(dmEntry.getKey()) && !"FechaUltimaPublicacion".equals(dmEntry.getKey())) {
					properties = propiedadesCaracteristicas.get(dmEntry.getKey());
					if(properties == null) {
						properties = collectCharacteristicProperties(dmEntry.getKey(), baseUrl);
						propiedadesCaracteristicas.put(
								"FechaUltimaAprobacion".equals(dmEntry.getKey()) ? "ApprovedDateCalc" :
								"StartDate".equals(dmEntry.getKey()) ? "ConjuntoLookStartDate" :
								"EndDate".equals(dmEntry.getKey()) ? "ConjuntoLookEndDate" :
								dmEntry.getKey()
								, properties);
					}
					lst = dmEntry.getValue();
					charId = dmEntry.getKey();
					characteristic = lst.getFirst();
					if("FechaUltimaAprobacion".equals(dmEntry.getKey())) {
						appendPlainElementValue(
								String.valueOf( parseDateForSpecificDateFields( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0), dmEntry.getKey()) ),
								null,
								"ApprovedDateCalc",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas,
								atgGroups);
					} else if("StartDate".equals(dmEntry.getKey())) {
						appendPlainElementValue(
								String.valueOf( parseDateForSpecificDateFields( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0), dmEntry.getKey()) ),
								null,
								"ConjuntoLookStartDate",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas,
								atgGroups);
					} else if("EndDate".equals(dmEntry.getKey())) {
						appendPlainElementValue(
								String.valueOf( parseDateForSpecificDateFields( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0), dmEntry.getKey()) ),
								null,
								"ConjuntoLookEndDate",
								attributeValues,
								attributes,
								doc,
								propiedadesCaracteristicas,
								atgGroups);
					} else if("ConjuntoLookOrigin".equals(dmEntry.getKey())) {
						origin = characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
					} else {
						if("LOOKUP".equals(characteristic.getString("_datatype"))){
							appendPlainElementValue(
									characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"),
									characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code"),
									charId,
									attributeValues,
									attributes,
									doc,
									propiedadesCaracteristicas,
									atgGroups);
						}else if(!"NONE".equals(characteristic.getString("_datatype"))) {
							java.util.LinkedList<String> vals = new java.util.LinkedList<>();
							for(int m=0; m<characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length(); m++) {
								vals.addLast( String.valueOf( parseDateForSpecificDateFields( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(m), charId)) );
							}
							appendPlainElementValue(
									String.join(",", vals),
									null,
									charId,
									attributeValues,
									attributes,
									doc,
									propiedadesCaracteristicas,
									atgGroups);
						}
					}
				}
			}
			if(displayName != null) {
				appendPlainElementValue(
						displayName,
						null,
						"ConjuntoLookDisplayName",
						attributeValues,
						attributes,
						doc,
						propiedadesCaracteristicas,
						atgGroups);
			}
			if(description != null) {
				appendPlainElementValue(
						description,
						null,
						"ConjuntoLookDescription",
						attributeValues,
						attributes,
						doc,
						propiedadesCaracteristicas,
						atgGroups);
			}
			appendPlainElementValue(
					origin != null ? origin : "PIM",
					null,
					"ConjuntoLookOrigin",
					attributeValues,
					attributes,
					doc,
					propiedadesCaracteristicas,
					atgGroups);
			appendPlainElementValue(
					externalId,
					null,
					"ConjuntoLookID",
					attributeValues,
					attributes,
					doc,
					propiedadesCaracteristicas,
					atgGroups);
			appendPlainElementValue(
					"Conjunto Look",
					"CLK",
					"SAPObjectType",
					attributeValues,
					attributes,
					doc,
					propiedadesCaracteristicas,
					atgGroups);
			product.appendChild(attributeValues);
		}
		return product;
	}
	
	private org.json.JSONObject collectCharacteristicProperties(String charId, String baseUrl){
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				    "Characteristic.Identifier"
				  + ",CharacteristicLang.Name(es)"
				  + ",CharacteristicLang.Description(es)"
				  + ",Characteristic.Order"
				  + ",Characteristic.LowerBound"
				  + ",Characteristic.IsMultiValue"
				 );
		qp.put("query", "Characteristic.Identifier equals \"" + charId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				values = rows.getJSONObject(0).getJSONArray("values");
				org.json.JSONObject properties = new org.json.JSONObject();
				properties.put("name", values.getString(1));
				properties.put("description", values.getString(2));
				properties.put("order", values.getString(3));
				properties.put("IsMandatory", Integer.parseInt(values.getString(4)) > 0 ? "1" : "0");
				properties.put("IsMultiselect", Boolean.parseBoolean(values.getString(5)) ? "1" : "0");
				return properties;
			}
		}
		return null;
	}
	
	private void appendPlainElementValue(
			String textValue, 
			String code, 
			String attributeId, 
			Element attributeValues, 
			Element attributes, 
			Document doc, 
			java.util.Map<String, org.json.JSONObject> propiedadesCaracteristicas, 
			java.util.Map<String, String> atgGroups
	) {
		org.json.JSONObject prop = null;
		Element attributeValue = doc.createElement("Value");
		attributeValues.appendChild(attributeValue);
		attributeValue.setAttribute("AttributeID", attributeId);
		if(code != null) {
			attributeValue.setAttribute("ID", code);
		}
		attributeValue.setTextContent(textValue);

		attributeValue.setAttribute("Changed", "true");
		Element metaData = doc.createElement("MetaData");
		Element valueElement = null;
		Element metaDataMultiValue = null;
		String groupLabel = null;
		java.util.LinkedList<String> grupos = null;
		Element attribute = doc.createElement("Attribute");
		attribute.setAttribute("ID", attributeId);
		prop = propiedadesCaracteristicas.get(attributeId);
		if(prop != null) {
			attribute.setAttribute("MultiValued", 
					prop.has("IsMultiselect") ? "1".equals(prop.getString("IsMultiselect")) ? "true" : "false" : "false");
			attribute.setAttribute("Mandatory", 
					prop.has("IsMandatory") ? "1".equals(prop.getString("IsMandatory")) ? "true" : "false" : "false");
			if(!prop.has("name")) {
//				log("No Name found for: " + attributeId);
			}else {
				Element metadataAttribute = doc.createElement("Name");
				metadataAttribute.setTextContent(prop.getString("name"));
				attribute.appendChild(metadataAttribute);
				if(prop.has("order")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "DisplaySequence");
					metadataAttribute.setTextContent(prop.getString("order"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("name")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "DisplayName");
					metadataAttribute.setTextContent(prop.getString("name"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("description")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "AttributeHelpText");
					metadataAttribute.setTextContent(prop.getString("description"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("isConfigurable")) {
					metadataAttribute = doc.createElement("Value");
					metadataAttribute.setAttribute("AttributeID", "isConfigurable");
					metadataAttribute.setTextContent(prop.getString("isConfigurable"));
					metaData.appendChild(metadataAttribute);
				}
				if(prop.has("purposes")) {
					org.json.JSONArray purposes = prop.getJSONArray("purposes");
					grupos = new java.util.LinkedList<>();
					for(int i=0; i<purposes.length(); i++) {
						if("CreationModificationAtributesIIEP".equals(purposes.getString(i))) {
						}else if("isFaceted".equals(purposes.getString(i))) {
							valueElement = doc.createElement("Value");
							valueElement.setTextContent("true");
							valueElement.setAttribute("ID", "Y");
							valueElement.setAttribute("AttributeID", purposes.getString(i));
							metaData.appendChild(valueElement);
						}else if("isConfigurable".equals(purposes.getString(i))) {
							valueElement = doc.createElement("Value");
							valueElement.setTextContent("true");
							valueElement.setAttribute("ID", "Y");
							valueElement.setAttribute("AttributeID", purposes.getString(i));
							metaData.appendChild(valueElement);
						}else {
							if(purposes.getString(i).endsWith("GPO")) {
								grupos.addLast(purposes.getString(i));
							}
						}
					}
					if(!grupos.isEmpty()) {
						metaDataMultiValue = doc.createElement("MultiValue");
						for(String grupo : grupos) {
							groupLabel = atgGroups.get(grupo);
							if(groupLabel != null) {
								valueElement = doc.createElement("Value");
								valueElement.setTextContent(groupLabel);
								valueElement.setAttribute("ID", grupo);
								metaDataMultiValue.appendChild(valueElement);
							}
						}
						if(metaDataMultiValue.getChildNodes().getLength() > 0) {
							metaDataMultiValue.setAttribute("AttributeID", "isAttInGroupAtt");
							metaData.appendChild(metaDataMultiValue);
						}
					}
				}
			}
		}else {
			// PANIC
//			log("PANIC: No property was found for characteristic: " + attributeId);
		}
		attribute.setAttribute("FullTextIndexed", "false");
		attribute.setAttribute("ProductMode", "Normal");
		attribute.setAttribute("ExternallyMaintained", "true");
		attribute.setAttribute("Derived", "false");
		attribute.setAttribute("HierarchicalFiltering", "false");
		attribute.setAttribute("ClassificationHierarchicalFiltering", "false");
		attribute.setAttribute("Referenced", "true");
		attributes.appendChild(attribute);
		attribute.appendChild(metaData);
		if(prop != null && prop.has("VendorCenterSectionSequence")) {
			Element attributeMetaDataValue = doc.createElement("Value");
			attributeMetaDataValue.setAttribute("AttributeID", "DisplaySequence");
			attributeMetaDataValue.setTextContent(prop.getString("VendorCenterSectionSequence"));
			metaData.appendChild(attributeMetaDataValue);
		}
		Element attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "AtributoCalculadoObjetos");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("Ultimo Usuario: N/A |  Fecha: N/A");
		metaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttVaDySAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("0");
		metaData.appendChild(attributeMetaDataValue);
		attributeMetaDataValue = doc.createElement("Value");
		attributeMetaDataValue.setAttribute("AttributeID", "CompletenessAttSAP");
		attributeMetaDataValue.setAttribute("Derived", "true");
		attributeMetaDataValue.setTextContent("N/A");
		metaData.appendChild(attributeMetaDataValue);
	}

	private String[] checkProductBySKU(String sku, String baseUrl) {
		RESTWorkshop workshop = new RESTWorkshop();
		if(baseUrl != null) {
			workshop.setBaseUrl(baseUrl);
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query",  "characteristic('SKU',-1) equals \"" + sku + "\"");
		qp.put("fields", 
				  "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('MainBarCode',root,\"0000.0000.RK\",'MainBarCode')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool')->LookupValue.Code"
				+ ",Product2G.CurrentStatus"
				);
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		return (response != null && response.getJSONArray("rows").length() > 0) ? new String[] { 
				  response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3).getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(4)
				} : null;
	}
	
	private Object parseDateForSpecificDateFields(Object value, String charId) {
		if(value == null)
			return null;
		String formato = mapaDeAtributosFechas.get(charId);
		if(formato != null) {
			try {
				return new java.text.SimpleDateFormat( formato ).format( new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse( ((String)value).replaceFirst("(\\d{2}:\\d{2}:\\d{2}):", "$1.") ) );
			}catch(java.text.ParseException e) {
				
			}
		}else if(value instanceof String) {
			try {
				return new java.text.SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format( new java.text.SimpleDateFormat("yyyy-MM-dd").parse( ((String)value) ) );
			}catch(java.text.ParseException e) {
				
			}
		}
		return value;
	}
	
	private String padd(String value, int padd) {
		StringBuilder sb = new StringBuilder();
		for(int i=value.length(); i<padd; i++) {
			sb.append("0");
		}
		sb.append(value);
		return sb.toString();
	}
	
	private void appendMediaAsset(
			String name,
			String url,
			String assetType,
			String assetKey,
			String assetValueTextContent,
			String assetValueAttributeId,
			String assetUserTypeId,
			String assetKeyPrefix,
			String itemId,
			org.json.JSONObject characteristic,
			String baseAssetTypeName,
			java.util.Map <String, Element> assetMap,
			java.util.Map <String, java.util.LinkedList <String>> assetReferencesMap,
			Element product,
			Element assets,
			Document doc,
			String seedId
		) {
		Element an = null;
		Element assetCrossReference = doc.createElement("AssetCrossReference");
		org.json.JSONObject cc = null;
		String assetId = assetKeyPrefix + "-" + seedId + (assetKey != null ? assetKey : characteristic.getJSONObject("_qualification").getString("recordKey"));
		if(name != null) {
			an = doc.createElement("Name");
			an.setTextContent(name);
			assetCrossReference.setAttribute("AssetID", assetId);
			assetCrossReference.setAttribute("Type", assetType);
			assetCrossReference.setAttribute("Changed", "true");
			product.appendChild(assetCrossReference);
		}else {
			cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_Name");
			if(cc != null) {
				assetCrossReference.setAttribute("AssetID", assetId);
				assetCrossReference.setAttribute("Type", assetType);
				assetCrossReference.setAttribute("Changed", "true");
				product.appendChild(assetCrossReference);
			}
		}
		Element asset = assetMap.get(assetId);
		Element assetName = null;
		Element assetValues = null;
		Element assetValue = null;
		java.util.LinkedList<String> referencesList = null;
		if(asset == null) {
			asset = doc.createElement("Asset");
			assetMap.put(assetId, asset);
			asset.setAttribute("ID", assetId);
			asset.setAttribute("UserTypeID", assetUserTypeId /* "Video" */);
			asset.setAttribute("Selected", "false");
			asset.setAttribute("Referenced", "true");
			if(cc != null) {
    			assetName = doc.createElement("Name");
    			assetName.setTextContent(cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
    			asset.appendChild(assetName);
			}else {
				asset.appendChild(an);
			}
			assetValues = doc.createElement("Values");
			asset.appendChild(assetValues);
			assetValue = doc.createElement("Value");
			if(!"ProductVideo".equals(assetUserTypeId)) {
				assetValues.appendChild(assetValue);
			}
			assetValue.setAttribute("AttributeID", "getObjectType");
			assetValue.setTextContent(assetValueTextContent /* "Video Producto" */);
			assetValue = doc.createElement("Value");
			assetValue.setAttribute("AttributeID", assetValueAttributeId /* "VideoURL" */);
			if(url != null) {
				if("ProductImage".equals(assetUserTypeId)) {
					assetValue.setTextContent("largeImage=" + url + ",smallImage=" + url + ",thumbnail=" + url);
				}else {
					assetValue.setTextContent(url);
				}
				assetValues.appendChild(assetValue);
			}else {
				cc = getMeAssetChildValue(characteristic, baseAssetTypeName + "_URL");
				if(cc != null) {
					if("ProductImage".equals(assetUserTypeId)) {
						url = cc.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
						assetValue.setTextContent("largeImage=" + url + ",smallImage=" + url + ",thumbnail=" + url);
					}else {
						assetValue.setTextContent(url);
					}
					assetValues.appendChild(assetValue);
				}
			}
			if("ProductImage".equals(assetUserTypeId)) {
				assetValue = doc.createElement("Value");
				assetValues.appendChild(assetValue);
				assetValue.setAttribute("AttributeID", "ImageKey");
				assetValue.setTextContent("sm-Imagen Producto,lg-Imagen Producto,xl-Imagen Producto");
			}
			referencesList = new java.util.LinkedList<>();
			referencesList.addLast(itemId);
			assetReferencesMap.put(assetId, referencesList);
			assets.appendChild(asset);
		}else {
			referencesList = assetReferencesMap.get(assetId);
			if(!referencesList.contains(assetId)) {
				referencesList.addLast(assetId);
			}
		}
	}
	
	private org.json.JSONObject getMeAssetChildValue(org.json.JSONObject hola, String childCharacteristic) {
		if(hola == null || (!hola.has("_children"))) {
			return null;
		}
		org.json.JSONArray children = hola.getJSONArray("_children");
		for(int i=0; i<children.length(); i++) {
			if(children.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").equals(childCharacteristic)) {
				return children.getJSONObject(i);
			}
		}
		return null;
	}
	
	private Element pacheleWeb(JSONObject node, Document doc) {
		Element metaData = null;
		Element value = null;
		String aux = null;
        if (node != null) {
        	metaData = doc.createElement("MetaData");
            Element classificationElement = doc.createElement("Classification");
            classificationElement.setAttribute("Selected", "true");
            classificationElement.appendChild(metaData);
            if (node.has("name_es")) {
            	value = doc.createElement("Value");
            	value.setAttribute("AttributeID", "DisplayName");
            	value.setTextContent
            	(node.getString("name_es"));
            	metaData.appendChild(value);
            }
            if (node.has("identifier")) {
                classificationElement.setAttribute("ID", node.optString("identifier", ""));
            }
            if (node.has("level")) {
            	aux = String.valueOf(node.get("level"));
                classificationElement.setAttribute("UserTypeID", "0".equals(aux) ? "WebsiteRoot" : "WebLevel" + aux );
            }
            if (node.has("parentIdentifier") && node.getString("parentIdentifier").startsWith("cat")) {
            	value = doc.createElement("Value");
            	value.setAttribute("AttributeID", "parentCategoryID");
            	value.setTextContent(node.getString("parentIdentifier"));
                metaData.appendChild(value);
            }
            return classificationElement;
        }
        return null;
    }

	private String[] getWebCategory(org.json.JSONArray classifications){
		java.util.LinkedList<String> webs = new java.util.LinkedList<>();
		org.json.JSONObject classification = null;
		String externalId = null;
		for(int i=0; i<classifications.length(); i++) {
			classification = classifications.getJSONObject(i);
			externalId = classification.getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId");
			if(externalId.endsWith("'Sitios Web'")) {
				webs.addLast( externalId.replaceAll("(^')|(('@'Sitios Web')$)", "") );
			}
		}
		return webs.toArray(new String[] {});
	}
	
	private org.json.JSONObject getEs(org.json.JSONArray lang){
		for(int i=0; i<lang.length(); i++) {
			if( 10 == lang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getInt("_key") )
				return lang.getJSONObject(i);
		}
		return null;
	}
	
	private java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> buildDataMap(org.json.JSONArray cr){
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> data = new java.util.TreeMap<>();
		String id;
		org.json.JSONObject obj = null;
		java.util.LinkedList<org.json.JSONObject> lst = null;
		for(int i=0; i<cr.length(); i++) {
			obj = cr.getJSONObject(i);
			id = obj.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			lst = data.get(id);
			if(lst == null) {
				lst = new java.util.LinkedList<>();
				data.put(id, lst);
			}
			lst.addLast(obj);
		}
		return data;
	}
	
}
