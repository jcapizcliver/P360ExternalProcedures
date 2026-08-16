package mx.com.liverpool.p360.services.core;

import java.io.Closeable;
import java.io.IOException;

import org.json.JSONObject;

public class GetTemplateInformation implements Closeable {

	private String prevBusiness;
	private String prevCharacteristic;
	private String prevCharacteristicIdentifier;
	private org.json.JSONObject jsonProperties = new org.json.JSONObject();
	private org.json.JSONObject globalProperties = new org.json.JSONObject();
	private java.util.Map<String, String> properties = new java.util.TreeMap<>();
	private String allowedBusiness;
	private String sendToVendorCenter;
	private String structureGroupId;
	private String response;
	private String creationType = "CreateProposal";
	private String extraAttributeValues = "";

	private final ELog dbLog = new ELog() {
		@Override
		public void logE(Exception e) {
			GetTemplateInformation.this.logE(e);
		}

		@Override
		public void log(String message) {
			GetTemplateInformation.this.log(message);
		}
	};

	private final DBAccessDataStub dastub = new DBAccessDataStub(dbLog);

	public String handleStart(String[] args, boolean aSAPInt)
			throws ArrayIndexOutOfBoundsException, ServiceUnavailableException {
		return handleStart(args);
	}

	/*
	 * baseUrl y encoded se conservan en la firma para no romper consumidores.
	 * Ya no se usan porque este flujo no realiza solicitudes HTTP.
	 */
	public String processRequest(
			String plantilla,
			String negocio,
			String structureFeatures,
			String baseUrl,
			String encoded) throws ServiceUnavailableException {

		long init = System.currentTimeMillis();
		if (creationType == null)
	    {
	        creationType = this.creationType;
	    }
		try {
			java.util.Map<String, org.json.JSONObject> atributos =
					new java.util.TreeMap<>();

			java.util.Map<String, String> translation =
					dastub.getDictionaryValueAlternativeValueMap(
							"SeccionesEntradaUnicaCatalogacion");

			java.util.Date lastModified =
					addGlobalData(negocio, atributos);

			java.util.Map<String, org.json.JSONObject> templateMetadata =
					dastub.getTemplateCharacteristicPropertiesForVendorCenter(
							plantilla);

			for (java.util.Map.Entry<String, org.json.JSONObject> entry
					: templateMetadata.entrySet()) {

				String characteristic = entry.getKey();
				org.json.JSONObject detail = copy(entry.getValue());

				String friendlyName = detail.optString("_friendlyName", characteristic);
				String dataType = detail.optString("_dataType", "");
				String lookup = detail.optString("_lookup", "");

				detail.remove("_friendlyName");
				detail.remove("_dataType");
				detail.remove("_lookup");

				detail.put("characteristic", characteristic);
				detail.put("friendlyName", friendlyName);
				detail.put(
						"dataType",
						"LOOKUP".equals(dataType)
								? "List of Values"
								: dataType);

				if ("LOOKUP".equals(dataType)) {
					detail.put("listofValues", lookup);
				}

				if (isEligibleForBusiness(detail, negocio)) {
					mergeAttribute(atributos, characteristic, detail);
				}
			}

			lastModified = latest(
					lastModified,
					dastub.getTemplateCharacteristicMetadataLastChangeDate(
							plantilla));

			lastModified = latest(
					lastModified,
					dastub.getStructureGroupLastModified(plantilla));

			org.json.JSONObject masterObject =
					buildSectionedResponse(atributos, translation);

			java.text.SimpleDateFormat sdf =
					new java.text.SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss.SSSZ");

			masterObject.put(
					"lastModified",
					sdf.format(lastModified == null
							? new java.util.Date()
							: lastModified));

			if (structureFeatures != null
					&& !structureFeatures.isBlank()) {

				addExtraInformation(
						masterObject,
						structureFeatures,
						plantilla);

				return stringJSON(
						masterObject,
						new String[] {
							"producto",
							"basicData",
							"datosVenta",
							"attributes",
							"logisticData",
							"photos",
							"multiMedia",
							"header",
							"lastModified",
							"extraInformation"
						});
			}

			return stringJSON(
					masterObject,
					new String[] {
						"producto",
						"basicData",
						"datosVenta",
						"attributes",
						"logisticData",
						"photos",
						"multiMedia",
						"header",
						"lastModified"
					});

		} catch (NullPointerException e) {
			logE(e);
			throw e;
		} finally {
			dastub.close();
			log("processRequest: "
					+ (System.currentTimeMillis() - init)
					+ " ms");
		}
	}

	/*
	 * Se conserva la firma pública anterior. baseUrl y authorization ya no
	 * intervienen: el dato sale de StructureGroupRevision por JDBC.
	 */
	public java.util.Date getLastChangeDateStructureGroup(
			String template,
			String baseUrl,
			String authorization) throws ServiceUnavailableException {

		try {
			return dastub.getStructureGroupLastModified(template);
		} finally {
			dastub.close();
		}
	}

	private java.util.Date addGlobalData(
			String negocio,
			java.util.Map<String, org.json.JSONObject> attributeDetails) {

		org.json.JSONObject globalMetadata =
				dastub.getGlobalMetadata(creationType);

		String[] characteristicIdentifiers =
				org.json.JSONObject.getNames(globalMetadata);

		if (characteristicIdentifiers != null) {
			java.util.Arrays.sort(characteristicIdentifiers);

			for (String characteristicIdentifier
					: characteristicIdentifiers) {

				org.json.JSONObject detail = copy(
						globalMetadata.getJSONObject(
								characteristicIdentifier));

				org.json.JSONObject characteristicData =
						dastub.getCharacteristicData(
								characteristicIdentifier);

				detail.put("characteristic", characteristicIdentifier);
				detail.put(
						"friendlyName",
						characteristicData.optString(
								"name",
								characteristicIdentifier));

				if (isEligibleForBusiness(detail, negocio)) {
					attributeDetails.put(characteristicIdentifier, detail);
				}
			}
		}

		return dastub.getDictionaryLastChangeDate(
				"GlobalTemplateAttributeConfiguration");
	}

	private boolean isEligibleForBusiness(
			org.json.JSONObject detail,
			String business) {

		return business != null
				&& detail.has("senttoVendorCenter")
				&& "1".equals(detail.optString("senttoVendorCenter"))
				&& detail.has("allowedBusiness")
				&& detail.optString("allowedBusiness").contains(business)
				&& detail.has("vendorCenterSection");
	}

	private void mergeAttribute(
			java.util.Map<String, org.json.JSONObject> attributes,
			String characteristic,
			org.json.JSONObject override) {

		org.json.JSONObject merged = attributes.get(characteristic);
		if (merged == null) {
			attributes.put(characteristic, override);
			return;
		}

		String[] names = org.json.JSONObject.getNames(override);
		if (names != null) {
			for (String name : names) {
				merged.put(name, override.get(name));
			}
		}
	}

	private org.json.JSONObject buildSectionedResponse(
			java.util.Map<String, org.json.JSONObject> atributos,
			java.util.Map<String, String> translation) {

		java.util.Map<String, org.json.JSONArray> sections =
				new java.util.TreeMap<>();

		for (java.util.Map.Entry<String, org.json.JSONObject> entry
				: atributos.entrySet()) {

			org.json.JSONObject detail = entry.getValue();
			detail.put("name", entry.getKey());

			if (detail.has("dependentAttribute")) {
				org.json.JSONObject parent =
						atributos.get(detail.optString("dependentAttribute"));

				if (parent == null) {
					log("Not having dependentAttribute: "
							+ detail.optString("dependentAttribute"));
				} else {
					org.json.JSONArray dependentAttributes =
							parent.optJSONArray("dependentAttributes");

					if (dependentAttributes == null) {
						dependentAttributes = new org.json.JSONArray();
						parent.put("dependentAttributes", dependentAttributes);
					}

					dependentAttributes.put(detail);
				}
			} else {
				String currentSection =
						detail.optString("vendorCenterSection", "");

				org.json.JSONArray section = sections.get(currentSection);
				if (section == null) {
					section = new org.json.JSONArray();
					sections.put(currentSection, section);
				}

				section.put(detail);
			}

			if (!detail.has("dependentAttributes")) {
				detail.put("dependentAttributes", new org.json.JSONArray());
			}

			detail.remove("vendorCenterSection");
			detail.remove("templateId");
			detail.remove("templateName");
		}

		org.json.JSONObject masterObject = new org.json.JSONObject();

		for (java.util.Map.Entry<String, org.json.JSONArray> section
				: sections.entrySet()) {

			String translated = translation.get(section.getKey());
			masterObject.put(
					translated == null || translated.isBlank()
							? section.getKey()
							: translated,
					section.getValue());
		}

		return masterObject;
	}

	public void getExtraInformation(
			org.json.JSONObject details,
			String fields,
			String plantilla,
			String baseUrl,
			String auth) throws ServiceUnavailableException {

		try {
			addExtraInformation(details, fields, plantilla);
		} finally {
			dastub.close();
		}
	}

	private void addExtraInformation(
			org.json.JSONObject details,
			String fields,
			String plantilla) {

		java.util.List<String> requestedFields = splitFields(fields);
		java.util.Map<String, String> values =
				dastub.getTemplateStructureGroupAttributeValues(
						plantilla,
						10,
						requestedFields);

		org.json.JSONObject extraInfo = new org.json.JSONObject();
		for (String field : requestedFields) {
			extraInfo.put(
					field,
					java.util.Objects.toString(values.get(field), ""));
		}

		log("Added extra info: " + extraInfo);
		details.put("extraInformation", extraInfo);
	}

	private java.util.List<String> splitFields(String fields) {
		java.util.List<String> requestedFields = new java.util.ArrayList<>();
		if (fields == null || fields.isBlank()) {
			return requestedFields;
		}

		for (String field : fields.split(",")) {
			String trimmed = field.trim();
			if (!trimmed.isEmpty() && !requestedFields.contains(trimmed)) {
				requestedFields.add(trimmed);
			}
		}

		return requestedFields;
	}

	public String handleStart(String[] args)
			throws ArrayIndexOutOfBoundsException, ServiceUnavailableException {

		if (args.length < 1) {
			System.out.println(
					new org.json.JSONObject()
							.put(
									"message",
									"Missing query parameters: template, business"));
			return null;
		}

		if (args.length < 2) {
			System.out.println(
					new org.json.JSONObject()
							.put(
									"message",
									"Incomplete query parameters, required: template, business"));
			return null;
		}

		resetLegacyState();
		log("Working with: " + java.util.Arrays.asList(args));

		String template = args[0];
		String business = args[1];
		String requestedCreationType = creationType;

		extraAttributeValues =
				args.length > 3
						? java.util.Objects.toString(args[3], "")
						: "";

		try {
			java.util.List<org.json.JSONObject> rows =
					dastub.getTemplateCharacteristicPropertyRowsByLocalizedName(
							template,
							requestedCreationType,
							10);

			for (org.json.JSONObject row : rows) {
				handleRow(
						row.optString("characteristicName", ""),
						row.optString("structureGroup", template),
						normalizePropertyName(row.optString("property", "")),
						row.optString("propertyValue", ""),
						business,
						template,
						requestedCreationType,
						row.optString("characteristicIdentifier", ""));
			}

			return prepareResponse();
		} finally {
			dastub.close();
		}
	}

	private String normalizePropertyName(String property) {
		String compact = java.util.Objects.toString(property, "")
				.replaceAll("[^A-Za-z]+", "");

		java.util.regex.Matcher acronym =
				java.util.regex.Pattern
						.compile("(^[A-Z]+)[A-Z0-9](.+)?")
						.matcher(compact);

		java.util.regex.Matcher firstLetter =
				java.util.regex.Pattern
						.compile("(^[A-Z])[^A-Z0-9](.+)?")
						.matcher(compact);

		if (acronym.find()) {
			String prefix = acronym.group(1).toLowerCase();
			return prefix + compact.substring(prefix.length());
		}

		if (firstLetter.find()) {
			String prefix = firstLetter.group(1).toLowerCase();
			return prefix + compact.substring(prefix.length());
		}

		return compact;
	}

	private void handleRow(
			String characteristic,
			String structureGroupId,
			String property,
			String propertyValue,
			String business,
			String templateId,
			String creationType,
			String characteristicIdentifier) {

		this.structureGroupId = structureGroupId;

		if (prevCharacteristicIdentifier != null
				&& !prevCharacteristicIdentifier.equals(characteristicIdentifier)) {

			flushLegacyCharacteristic();
			jsonProperties = new org.json.JSONObject();
			properties.clear();
		}

		if (property != null && !property.isBlank()) {
			properties.put(property, propertyValue);
		}

		prevBusiness = business;
		prevCharacteristic = characteristic;
		prevCharacteristicIdentifier = characteristicIdentifier;
	}

	private void flushLegacyCharacteristic() {
		allowedBusiness = properties.get("allowedBusiness");
		sendToVendorCenter = properties.get("senttoVendorCenter");

		if (allowedBusiness == null
				|| prevBusiness == null
				|| !allowedBusiness.toUpperCase().contains(prevBusiness.toUpperCase())
				|| !"1".equals(sendToVendorCenter)) {
			return;
		}

		for (java.util.Map.Entry<String, String> entry : properties.entrySet()) {
			jsonProperties.put(
					entry.getKey(),
					entry.getValue() == null
							? null
							: entry.getValue().trim());
		}

		globalProperties.put(
				prevCharacteristicIdentifier,
				jsonProperties
						.put("characteristic", prevCharacteristic)
						.put(
								"characteristicIdentifier",
								prevCharacteristicIdentifier));
	}

	private String prepareResponse() throws ServiceUnavailableException {
		response = "{}";
		flushLegacyCharacteristic();

		org.json.JSONArray header = new org.json.JSONArray();
		org.json.JSONArray basicData = new org.json.JSONArray();
		org.json.JSONArray datosVenta = new org.json.JSONArray();
		org.json.JSONArray attributes = new org.json.JSONArray();
		org.json.JSONArray logisticData = new org.json.JSONArray();
		org.json.JSONArray photos = new org.json.JSONArray();
		org.json.JSONArray multiMedia = new org.json.JSONArray();
		org.json.JSONArray producto = new org.json.JSONArray();

		org.json.JSONObject jsonRes = new org.json.JSONObject();
		java.util.Date lastModifiedDate =
				dastub.getStructureGroupLastModified(structureGroupId);

		String lastModified = lastModifiedDate == null
				? null
				: new java.text.SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ss.SSSZ")
						.format(lastModifiedDate);

		if (globalProperties.length() > 0) {
			try {
				java.util.LinkedList<org.json.JSONObject> dependent = new java.util.LinkedList<>();
				String[] keyNames = org.json.JSONObject.getNames(globalProperties);
				if (keyNames != null) {
					for (String keyName : keyNames) {
						org.json.JSONObject json = globalProperties.getJSONObject(keyName);
						json.put("dependentAttributes", new org.json.JSONArray());
						json.remove("listofValuesValidValues");
						json.remove("ecC");
						json.remove("sH");
						json.remove("sendtoVendorCenter");

						String vendorCenterSection =
								java.util.Objects.toString(
										json.remove("vendorCenterSection"),
										"");

						if (json.has("dependentAttribute")) {
							dependent.addLast(json);
							continue;
						}

						addToLegacySection(
								json,
								vendorCenterSection,
								header,
								basicData,
								datosVenta,
								attributes,
								logisticData,
								photos,
								multiMedia,
								producto);
					}
				}

				for (org.json.JSONObject child : dependent) {
					decorateLegacyAttribute(child);
					org.json.JSONObject parent = globalProperties.optJSONObject(child.optString("dependentAttribute"));
					if (parent != null) {
						parent.getJSONArray("dependentAttributes").put(child);
					}
				}

				jsonRes
						.put("producto", producto)
						.put("basicData", basicData)
						.put("datosVenta", datosVenta)
						.put("attributes", attributes)
						.put("logisticData", logisticData)
						.put("photos", photos)
						.put("multiMedia", multiMedia)
						.put("header", header);

				if (lastModified != null) {
					jsonRes.put("lastModified", lastModified);
				}

				if (!extraAttributeValues.isBlank()) {
					addExtraInformation(
							jsonRes,
							extraAttributeValues,
							structureGroupId);

					response = stringJSON(
							jsonRes,
							new String[] {
								"producto",
								"basicData",
								"datosVenta",
								"attributes",
								"logisticData",
								"photos",
								"multiMedia",
								"header",
								"lastModified",
								"extraInformation"
							});
				} else {
					response = stringJSON(
							jsonRes,
							new String[] {
								"producto",
								"basicData",
								"datosVenta",
								"attributes",
								"logisticData",
								"photos",
								"multiMedia",
								"header",
								"lastModified"
							});
				}
			} catch (org.json.JSONException e) {
				response = new org.json.JSONObject()
						.put("messagea", e.toString())
						.toString();
			} catch (NullPointerException e) {
				logE(e);
				response = new org.json.JSONObject()
						.put(
								"messagea",
								"There was a null in global properties json names. ("
										+ globalProperties.length()
										+ ")")
						.toString();
			}
		}

		System.out.println(response);
		String result = response;
		resetLegacyState();
		return result;
	}

	private void addToLegacySection(
			org.json.JSONObject json,
			String section,
			org.json.JSONArray header,
			org.json.JSONArray basicData,
			org.json.JSONArray datosVenta,
			org.json.JSONArray attributes,
			org.json.JSONArray logisticData,
			org.json.JSONArray photos,
			org.json.JSONArray multiMedia,
			org.json.JSONArray producto) {

		decorateLegacyAttribute(json);

		if ("Header".equals(section)) {
			header.put(json);
		} else if ("Atributos".equals(section)) {
			attributes.put(json);
		} else if ("Datos de Venta".equals(section)) {
			datosVenta.put(json);
		} else if ("Datos Básicos".equals(section)) {
			basicData.put(json);
		} else if ("Fotografías".equals(section)) {
			photos.put(json);
		} else if ("Multimedia".equals(section)) {
			multiMedia.put(json);
		} else if (section.startsWith("Datos Logísticos")) {
			logisticData.put(json);
		} else if (section.startsWith("Producto")) {
			producto.put(json);
		}
	}

	private void decorateLegacyAttribute(org.json.JSONObject json) {
		json.put("name", json.optString("characteristicIdentifier", ""));
		json.put("friendlyName", json.optString("characteristic", ""));
	}

	private void resetLegacyState() {
		prevBusiness = null;
		prevCharacteristic = null;
		prevCharacteristicIdentifier = null;
		jsonProperties = new org.json.JSONObject();
		globalProperties = new org.json.JSONObject();
		properties = new java.util.TreeMap<>();
		allowedBusiness = null;
		sendToVendorCenter = null;
		structureGroupId = null;
		response = null;
	}

	private java.util.Date latest(
			java.util.Date first,
			java.util.Date second) {

		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.before(second) ? second : first;
	}

	private org.json.JSONObject copy(org.json.JSONObject source) {
		org.json.JSONObject copy = new org.json.JSONObject();
		if (source == null) {
			return copy;
		}

		String[] names = org.json.JSONObject.getNames(source);
		if (names != null) {
			for (String name : names) {
				copy.put(name, source.get(name));
			}
		}
		return copy;
	}

	private String stringJSON(JSONObject j, String[] names) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		org.json.JSONArray photos = null;

		for (String name : names) {
			if ("photos".equals(name)) {
				photos = j.has("photos")
						? j.getJSONArray("photos")
						: new org.json.JSONArray();

				java.util.Map<String, org.json.JSONObject> map =
						new java.util.TreeMap<>();

				for (int ji = 0; ji < photos.length(); ji++) {
					map.put(
							photos.getJSONObject(ji).getString("name"),
							photos.getJSONObject(ji));
				}

				photos = new org.json.JSONArray();
				String[] photoOrder = {
					"ProductImage",
					"ProductImageDetail",
					"Illustration",
					"ProductImageSmosh"
				};

				for (String photoName : photoOrder) {
					if (map.get(photoName) != null) {
						photos.put(map.get(photoName));
					}
				}
			}

			sb.append(i == 0 ? "" : ",")
					.append("\"")
					.append(name)
					.append("\":")
					.append(!j.has(name)
							? "[]"
							: "photos".equals(name)
									? photos
									: j.get(name) instanceof org.json.JSONArray
											? j.getJSONArray(name)
											: j.get(name) instanceof org.json.JSONObject
													? j.getJSONObject(name)
													: "\""
															+ String.valueOf(j.get(name))
																	.replaceAll("(?=\")", "\\\\")
															+ "\"");
			i++;
		}

		return "{" + sb + "}";
	}

	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.OutputStreamWriter(
						new java.io.FileOutputStream(
								"../logs/get_template_information.log",
								true)))) {

			pw.println("["
					+ new java.text.SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss.SSS")
							.format(new java.util.Date())
					+ "] ("
					+ this.hashCode()
					+ ") "
					+ message);
		} catch (java.io.IOException e) {
			// Se conserva el comportamiento original: el fallo del log no rompe el servicio.
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.OutputStreamWriter(
						new java.io.FileOutputStream(
								"../logs/get_template_information.log",
								true)))) {

			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
			// Se conserva el comportamiento original: el fallo del log no rompe el servicio.
		}
	}

	@Override
	public void close() {
		dastub.close();
	}
}
