package mx.com.liverpool.p360.services.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class GetAttributeValuesForo implements Closeable {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final Logger LOGGER = Logger.getLogger(GetAttributeValuesForo.class.getName());
	private static final Object ATTRIBUTE_GROUPS_LOCK = new Object();
	private static volatile boolean attributeGroupsLoaded = false;
	private static final java.util.Set<String> atributosInternet = new java.util.TreeSet<>();
	private static final java.util.Set<String> atributosSAP = new java.util.TreeSet<>();
	private static final String[] MEDIA_ROOTS = {
			"ProductVideo", "LiverpoolManual", "OwnersManual", "NOM",
			"ProductImage", "ProductImageDetail", "Illustration", "ProductImageSmosh"
	};
	private static final java.util.Set<String> OTROS_DE_INTERES =
			java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<String>(java.util.Arrays.asList((
				  "Name\r\n"
				+ "ColoursLiverpoolAtt\r\n"
				+ "TamanoUnico\r\n"
				+ "SupplierName\r\n"
				+ "SkuType\r\n"
				+ "MTART_S4H\r\n"
				+ "CompletenessAttSAP\r\n"
				+ "UniversalMainBarCode\r\n"
				+ "MainBarCode\r\n"
				+ "MainBarCodeS4H\r\n"
				+ "ParentSKU\r\n"
				+ "Direction\r\n"
				+ "StateSKU\r\n"
				+ "SupplierPartNumber\r\n"
				+ "Section\r\n"
				+ "ItemGroup2\r\n"
				+ "ItemGroup\r\n"
				+ "ItemGroupS4H\r\n"
				+ "BrandNameATG\r\n"
				+ "BrandName\r\n"
				+ "BRAND_ID_S4H\r\n"
				+ "TipoDeToma\r\n"
				+ "GeneroVAD\r\n"
				+ "GenderAtt\r\n"
				+ "ProductType\r\n"
				+ "ObjectTypeName\r\n"
				+ "FirstDateApprove\r\n"
				+ "SKU\r\n"
				+ "StylistWorld\r\n"
				+ "SupplierID\r\n"
				+ "SupplierName\r\n"
				+ "ProductName\r\n"
				+ "DescriptionLong\r\n"
				+ "refundPolicy\r\n"
				+ "EmbedCodeWEB\r\n"
				+ "EmbedCodeWAP\r\n"
				+ "AssignTakeNoTake\r\n"
				+ "AssignTakeNoTakeReason\r\n"
				+ "AssignTakeNoTakeVideo").split("\\r\\n"))));

	private final java.util.concurrent.ConcurrentLinkedQueue<org.json.JSONObject> responses =
			new java.util.concurrent.ConcurrentLinkedQueue<>();

	private final DBAccessDataStub dastub = new DBAccessDataStub(new ELog() {
		@Override
		public void logE(Exception e) {
			GetAttributeValuesForo.this.logE(e);
		}

		@Override
		public void log(String message) {
			GetAttributeValuesForo.this.log(message);
		}
	});

	static {
		try {
			LOGGER.setUseParentHandlers(false);
			FileHandler fileHandler = new FileHandler("../logs/getProposalsForo-%g.log", 5 * 1024 * 1024, 5, true);
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
							java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
					return "[" + timestamp + "] [" + record.getLevel() + "] "
							+ formatMessage(record) + System.lineSeparator();
				}
			});
			LOGGER.addHandler(fileHandler);
			LOGGER.setLevel(Level.ALL);
		} catch (IOException e) {
			throw new RuntimeException("No se pudo inicializar el logger", e);
		}
	}

	public GetAttributeValuesForo() {
		ensureAttributeGroups();
	}

	private void ensureAttributeGroups() {
		if (attributeGroupsLoaded) {
			return;
		}
		synchronized (ATTRIBUTE_GROUPS_LOCK) {
			if (attributeGroupsLoaded) {
				return;
			}

			LOGGER.info("Collecting atributos SAP y de Internet directamente de BD");
			java.util.Map<String, java.util.Set<String>> groups =
					dastub.getSourceLookupValueCodesByReferencedLookupValueCodes(
							"Characteristics",
							"AttributeGroup",
							java.util.Arrays.asList(
									"CategorySpecificAttributesLVP",
									"CategorySpecificAttributesS4H",
									"CategorySpecificAttributesSAP"));

			java.util.Set<String> lvp = groups.get("CategorySpecificAttributesLVP");
			java.util.Set<String> s4h = groups.get("CategorySpecificAttributesS4H");
			java.util.Set<String> sap = groups.get("CategorySpecificAttributesSAP");
			if (lvp != null) atributosInternet.addAll(lvp);
			if (s4h != null) atributosInternet.addAll(s4h);
			if (sap != null) atributosSAP.addAll(sap);

			if (!atributosInternet.isEmpty() || !atributosSAP.isEmpty()) {
				attributeGroupsLoaded = true;
				LOGGER.info("Atributos cargados desde BD. Internet=" + atributosInternet.size()
						+ ", SAP=" + atributosSAP.size());
			} else {
				LOGGER.warning("No se pudieron resolver AttributeGroups desde BD; se reintentará en la siguiente instancia.");
			}
		}
	}

	private class Worker implements Runnable {
		private final int id;
		private final java.util.concurrent.ArrayBlockingQueue<Object[]> tasks;
		private final java.util.Map<String, org.json.JSONObject> productsById;
		private final java.util.Map<String, java.util.Set<String>> variantsByProduct;
		private final java.util.Map<String, org.json.JSONObject> variantsById;

		Worker(
				int id,
				java.util.concurrent.ArrayBlockingQueue<Object[]> tasks,
				java.util.Map<String, org.json.JSONObject> productsById,
				java.util.Map<String, java.util.Set<String>> variantsByProduct,
				java.util.Map<String, org.json.JSONObject> variantsById) {

			this.id = id;
			this.tasks = tasks;
			this.productsById = productsById;
			this.variantsByProduct = variantsByProduct;
			this.variantsById = variantsById;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void run() {
			Object[] pn;
			while ((pn = tasks.poll()) != null) {
				try {
					String productId = (String) pn[0];
					java.util.Set<String> losesos = (java.util.Set<String>) pn[1];
					processDataFromDB(productId, losesos);
				} catch (JSONException e) {
					logE(e);
				}
			}
			log("(" + id + ") Now exiting...");
		}

		private void processDataFromDB(
				String currentProductId,
				java.util.Set<String> losesos) {

			log("# " + id + ". Querying DB for " + currentProductId);

			org.json.JSONObject product =
					productsById.get(currentProductId);

			if (product == null || !product.optBoolean("found", false)) {
				log("No Product2G data found for: " + currentProductId);
				return;
			}

			org.json.JSONObject response = new org.json.JSONObject();
			org.json.JSONObject sap = new org.json.JSONObject();
			org.json.JSONObject internet = new org.json.JSONObject();
			org.json.JSONObject readOnly = new org.json.JSONObject();

			org.json.JSONObject productDetail =
					product.optJSONObject("detail");
			org.json.JSONObject productLanguage =
					languageRow(product, 10);
			org.json.JSONObject productDomain =
					domainRow(product, 21006);

			if (productDetail == null) {
				productDetail = new org.json.JSONObject();
			}
			if (productLanguage == null) {
				productLanguage = new org.json.JSONObject();
			}
			if (productDomain == null) {
				productDomain = new org.json.JSONObject();
			}

			String productSKU =
					jsonString(productDetail.opt("Res_Int_02"));
			String gtin =
					productDetail.optString("EAN", "");

			response.put(
					"currentStatus",
					statusLabel(numberValue(productDetail, "CurrentStatus")));
			response.put(
					"previousStatus",
					statusLabel(numberValue(productDetail, "Res_Int_03")));
			response.put(
					"externalStatus",
					lookupName(
							product.optJSONObject("detailLookups"),
							"Res_Int_04"));

			putReadOnlyText(
					readOnly,
					"DescriptionLong",
					productLanguage.optString("DescriptionLong", ""));
			putReadOnlyText(
					readOnly,
					"DescriptionLong2",
					productLanguage.optString("Res_Text2G_01", ""));
			putReadOnlyText(
					readOnly,
					"EmbedCodeWEB",
					productDetail.optString("Res_Text2G_02", ""));
			putReadOnlyText(
					readOnly,
					"EmbedCodeWAP",
					productDetail.optString("Res_Text2G_03", ""));
			putReadOnlyText(
					readOnly,
					"refundPolicy",
					productDetail.optString("Res_Text2G_04", ""));

			populateProductBaseValues(
					product,
					productDetail,
					productLanguage,
					productDomain,
					sap,
					internet,
					readOnly);

			org.json.JSONArray productCharacteristics =
					characteristicRows(product, 1160, 1161, -1);

			for (int i = 0; i < productCharacteristics.length(); i++) {
				org.json.JSONObject row =
						productCharacteristics.getJSONObject(i);
				String characteristicId =
						row.optString("identifier", "");

				if (characteristicId.isEmpty()
						|| isMediaCharacteristic(characteristicId)) {

					continue;
				}

				putClassified(
						characteristicId,
						responseValue(row),
						sap,
						internet,
						readOnly);

				if ("SKU".equals(characteristicId)) {
					productSKU =
							row.optString("value", productSKU);
				} else if ((gtin == null || gtin.isEmpty())
						&& ("MainBarCode".equals(characteristicId)
								|| "MainBarCodeS4H".equals(
										characteristicId))) {

					gtin = row.optString("value", "");
				}
			}

			putMediaArray(
					response,
					"productVideos",
					mediaElements(
							productCharacteristics,
							"ProductVideo"));
			putMediaArray(
					response,
					"liverpoolManuals",
					mediaElements(
							productCharacteristics,
							"LiverpoolManual"));
			putMediaArray(
					response,
					"ownerManuals",
					mediaElements(
							productCharacteristics,
							"OwnersManual"));
			putMediaArray(
					response,
					"noms",
					mediaElements(
							productCharacteristics,
							"NOM"));

			java.util.Set<String> variantIdentifiers =
					variantsByProduct.get(currentProductId);

			if (variantIdentifiers == null) {
				variantIdentifiers =
						java.util.Collections.emptySet();
			}

			if ((gtin == null || gtin.isEmpty())
					&& variantIdentifiers.size() == 1) {

				String onlyVariant =
						variantIdentifiers.iterator().next();
				org.json.JSONObject onlyVariantData =
						variantsById.get(onlyVariant);
				org.json.JSONObject onlyVariantDetail =
						onlyVariantData == null
								? null
								: onlyVariantData.optJSONObject("detail");

				if (onlyVariantDetail != null) {
					gtin = onlyVariantDetail.optString("EAN", "");
				}
			}

			if (gtin != null && !gtin.isEmpty()) {
				readOnly.put(
						"MainBarCode",
						new org.json.JSONObject()
								.put("value", gtin));
			}

			org.json.JSONArray variantResponses =
					new org.json.JSONArray();

			for (String externalVariantId : variantIdentifiers) {
				org.json.JSONObject variant =
						variantsById.get(externalVariantId);

				if (variant == null
						|| !variant.optBoolean("found", false)) {

					continue;
				}

				org.json.JSONObject variantDetail =
						variant.optJSONObject("detail");
				org.json.JSONObject variantLanguage =
						languageRow(variant, 10);
				org.json.JSONObject variantDomain =
						domainRow(variant, 21106);

				if (variantDetail == null) {
					variantDetail = new org.json.JSONObject();
				}
				if (variantLanguage == null) {
					variantLanguage = new org.json.JSONObject();
				}
				if (variantDomain == null) {
					variantDomain = new org.json.JSONObject();
				}

				org.json.JSONObject variantResponse =
						new org.json.JSONObject();
				org.json.JSONObject sapVariante =
						new org.json.JSONObject();
				org.json.JSONObject internetVariante =
						new org.json.JSONObject();
				org.json.JSONObject readOnlyVariante =
						new org.json.JSONObject();

				variantResponse.put(
						"currentStatus",
						statusLabel(
								numberValue(
										variantDetail,
										"CurrentStatus")));
				variantResponse.put(
						"previousStatus",
						statusLabel(
								numberValue(
										variantDetail,
										"Res_Int_03")));
				variantResponse.put(
						"externalStatus",
						lookupName(
								variant.optJSONObject(
										"detailLookups"),
								"Res_Int_04"));

				populateArticleBaseValues(
						variant,
						variantDetail,
						variantLanguage,
						variantDomain,
						sapVariante,
						internetVariante,
						readOnlyVariante);

				String variantSKU =
						jsonString(
								variantDetail.opt(
										"Res_Int_02"));
				String variantEAN =
						variantDetail.optString("EAN", "");

				org.json.JSONArray variantCharacteristics =
						characteristicRows(
								variant,
								1060,
								1061,
								-1);

				for (int j = 0;
						j < variantCharacteristics.length();
						j++) {

					org.json.JSONObject row =
							variantCharacteristics
									.getJSONObject(j);
					String characteristicId =
							row.optString(
									"identifier",
									"");

					if (characteristicId.isEmpty()
							|| isMediaCharacteristic(
									characteristicId)) {

						continue;
					}

					putClassified(
							characteristicId,
							responseValue(row),
							sapVariante,
							internetVariante,
							readOnlyVariante);

					if ("SKU".equals(characteristicId)) {
						variantSKU =
								row.optString(
										"value",
										variantSKU);
					}

					if ("MainBarCode".equals(
									characteristicId)
							|| "MainBarCodeS4H".equals(
									characteristicId)) {

						variantEAN =
								row.optString(
										"value",
										variantEAN);
					}
				}

				if (variantSKU != null
						&& !variantSKU.isEmpty()) {

					readOnlyVariante.put(
							"SKU",
							new org.json.JSONObject()
									.put(
										"value",
										variantSKU));
				}

				if (variantEAN != null
						&& !variantEAN.isEmpty()) {

					readOnlyVariante.put(
							"MainBarCode",
							new org.json.JSONObject()
									.put(
										"value",
										variantEAN));
				}

				log(
					"Elesecau: "
					+ externalVariantId
					+ " <::>"
					+ losesos
					+ "<::>");

				if (losesos != null
						&& losesos.contains(
								externalVariantId)) {

					org.json.JSONObject takeNoTake =
							readOnlyVariante
									.optJSONObject(
											"AssignTakeNoTake");

					if (takeNoTake != null
							&& "TOMADO".equals(
									takeNoTake.optString(
											"value",
											""))) {

						takeNoTake.put(
								"value",
								"NO TOMAR");
					}
				}

				if (productSKU != null
						&& !productSKU.isEmpty()) {

					readOnlyVariante.put(
							"ParentSKU",
							new org.json.JSONObject()
									.put(
										"value",
										productSKU));
				}

				org.json.JSONObject photos =
						new org.json.JSONObject();

				org.json.JSONArray productImages =
						mediaElements(
								variantCharacteristics,
								"ProductImage");

				if (productImages.length() > 0) {
					photos.put(
							"ProductImage",
							productImages.getJSONObject(
									productImages.length() - 1));
				}

				putMediaArray(
						photos,
						"ProductImageDetail",
						mediaElements(
								variantCharacteristics,
								"ProductImageDetail"));
				putMediaArray(
						photos,
						"Illustration",
						mediaElements(
								variantCharacteristics,
								"Illustration"));
				putMediaArray(
						photos,
						"ProductImageSmosh",
						mediaElements(
								variantCharacteristics,
								"ProductImageSmosh"));

				variantResponse.put(
						"sap",
						sapVariante);
				variantResponse.put(
						"internet",
						internetVariante);
				variantResponse.put(
						"readOnly",
						readOnlyVariante);
				variantResponse.put(
						"photos",
						photos);
				variantResponse.put(
						"variantId",
						externalVariantId);

				variantResponses.put(
						variantResponse);
			}

			if (variantResponses.length() > 0) {
				response.put(
						"variants",
						variantResponses);
			}

			if (sap.length() > 0) {
				response.put("SAP", sap);
			}

			if (internet.length() > 0) {
				response.put(
						"Internet",
						internet);
			}

			if (readOnly.length() > 0) {
				response.put(
						"readOnly",
						readOnly);
			}

			response.put(
					"productId",
					currentProductId);
			response.put(
					"template",
					structureGroup(
							product,
							"PrimaryProductTaxonomy"));

			log(
				"# "
				+ id
				+ " Preparing: "
				+ response);

			responses.add(response);
		}
	}


	private void populateProductBaseValues(
			org.json.JSONObject product,
			org.json.JSONObject detail,
			org.json.JSONObject language,
			org.json.JSONObject domain,
			org.json.JSONObject sap,
			org.json.JSONObject internet,
			org.json.JSONObject readOnly) {

		putClassifiedLookup(
				"Business",
				lookup(
						product.optJSONObject(
								"detailLookups"),
						"Res_Int_01"),
				sap,
				internet,
				readOnly);

		putClassifiedLookup(
				"Direction",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_01"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"Section",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_02"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"ItemGroup",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_03"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"ItemGroupS4H",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_04"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"BrandName",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_05"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"BRAND_ID_S4H",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_06"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"Negocio",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_07"),
				sap,
				internet,
				readOnly);

		org.json.JSONObject sapObjectType =
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_08");

		putClassifiedLookup(
				"SAPObjectType",
				sapObjectType,
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"ObjectTypeName",
				sapObjectType,
				sap,
				internet,
				readOnly);

		org.json.JSONObject supplier =
				lookup(
						domain.optJSONObject("lookups"),
						"Std_Int_10");

		putClassifiedLookup(
				"SupplierID",
				supplier,
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"SupplierName",
				supplier == null
						? ""
						: supplier.optString("Name", ""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"SupplierPartNumber",
				domain.optString(
						"Res_Text250_01",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"ProductName",
				language.optString(
						"Res_Text250_01",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"Name",
				language.optString(
						"DescriptionShort",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"FirstDateApprove",
				detail.optString(
						"Res_DateTime_02",
						""),
				sap,
				internet,
				readOnly);
	}


	private void populateArticleBaseValues(
			org.json.JSONObject article,
			org.json.JSONObject detail,
			org.json.JSONObject language,
			org.json.JSONObject domain,
			org.json.JSONObject sap,
			org.json.JSONObject internet,
			org.json.JSONObject readOnly) {

		putClassifiedLookup(
				"Business",
				lookup(
						article.optJSONObject(
								"detailLookups"),
						"Res_Int_01"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"TamanoUnico",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_01"),
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"ColoursLiverpoolAtt",
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_02"),
				sap,
				internet,
				readOnly);

		org.json.JSONObject sapObjectType =
				lookup(
						domain.optJSONObject("lookups"),
						"Res_Int_03");

		putClassifiedLookup(
				"SAPObjectType",
				sapObjectType,
				sap,
				internet,
				readOnly);
		putClassifiedLookup(
				"ObjectTypeName",
				sapObjectType,
				sap,
				internet,
				readOnly);

		putClassifiedText(
				"SupplierPartNumber",
				domain.optString(
						"Res_Text250_01",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"ProductName",
				language.optString(
						"Res_Text250_01",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"Name",
				language.optString(
						"DescriptionShort",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"DescriptionLong",
				language.optString(
						"DescriptionLong",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"DescriptionLong2",
				language.optString(
						"Res_Text2G_01",
						""),
				sap,
				internet,
				readOnly);
		putClassifiedText(
				"FirstDateApprove",
				detail.optString(
						"Res_DateTime_02",
						""),
				sap,
				internet,
				readOnly);
	}


	private void putClassifiedLookup(
			String property,
			org.json.JSONObject lookup,
			org.json.JSONObject sap,
			org.json.JSONObject internet,
			org.json.JSONObject readOnly) {

		if (lookup == null || lookup.length() == 0) {
			return;
		}

		org.json.JSONObject value =
				new org.json.JSONObject();

		String code =
				lookup.optString("Code", "");
		String name =
				lookup.optString("Name", "");

		if (!code.isEmpty()) {
			value.put("code", code);
		}

		value.put("value", name);

		putClassified(
				property,
				value,
				sap,
				internet,
				readOnly);
	}


	private void putClassifiedText(
			String property,
			String value,
			org.json.JSONObject sap,
			org.json.JSONObject internet,
			org.json.JSONObject readOnly) {

		if (value == null || value.isEmpty()) {
			return;
		}

		putClassified(
				property,
				new org.json.JSONObject()
						.put("value", value),
				sap,
				internet,
				readOnly);
	}


	private void putClassified(
			String characteristicId,
			org.json.JSONObject value,
			org.json.JSONObject sap,
			org.json.JSONObject internet,
			org.json.JSONObject readOnly) {

		if (atributosSAP.contains(characteristicId)) {
			sap.put(characteristicId, value);
		} else if (atributosInternet.contains(characteristicId)) {
			internet.put(characteristicId, value);
		} else if (OTROS_DE_INTERES.contains(characteristicId)) {
			readOnly.put(characteristicId, value);
		}
	}


	private org.json.JSONObject languageRow(
			org.json.JSONObject entity,
			int languageID) {

		org.json.JSONArray languages =
				entity == null
						? null
						: entity.optJSONArray("languages");

		if (languages == null) {
			return null;
		}

		for (int i = 0; i < languages.length(); i++) {
			org.json.JSONObject row =
					languages.optJSONObject(i);

			if (row != null
					&& row.optInt(
							"LanguageID",
							Integer.MIN_VALUE)
						== languageID) {

				return row;
			}
		}

		return null;
	}


	private org.json.JSONObject domainRow(
			org.json.JSONObject entity,
			int domainEntityID) {

		org.json.JSONArray domains =
				entity == null
						? null
						: entity.optJSONArray("domains");

		if (domains == null) {
			return null;
		}

		for (int i = 0; i < domains.length(); i++) {
			org.json.JSONObject row =
					domains.optJSONObject(i);

			if (row != null
					&& row.optInt(
							"EntityID",
							Integer.MIN_VALUE)
						== domainEntityID) {

				return row;
			}
		}

		return null;
	}


	private String structureGroup(
			org.json.JSONObject entity,
			String structureIdentifier) {

		org.json.JSONArray structures =
				entity == null
						? null
						: entity.optJSONArray("structures");

		if (structures == null) {
			return "";
		}

		for (int i = 0; i < structures.length(); i++) {
			org.json.JSONObject row =
					structures.optJSONObject(i);

			if (row != null
					&& structureIdentifier.equals(
							row.optString(
									"StructureIdentifier",
									""))) {

				return row.optString(
						"StructureGroupIdentifier",
						"");
			}
		}

		return "";
	}


	private org.json.JSONObject lookup(
			org.json.JSONObject lookups,
			String column) {

		return lookups == null
				? null
				: lookups.optJSONObject(column);
	}


	private String lookupName(
			org.json.JSONObject lookups,
			String column) {

		org.json.JSONObject lookup =
				lookup(lookups, column);

		return lookup == null
				? ""
				: lookup.optString("Name", "");
	}


	private org.json.JSONArray characteristicRows(
			org.json.JSONObject entity,
			int characteristicEntityID,
			int languageEntityID,
			int languageID) {

		org.json.JSONArray result =
				new org.json.JSONArray();
		org.json.JSONArray characteristics =
				entity == null
						? null
						: entity.optJSONArray(
								"characteristics");

		if (characteristics == null) {
			return result;
		}

		for (int i = 0;
				i < characteristics.length();
				i++) {

			org.json.JSONObject characteristic =
					characteristics.optJSONObject(i);

			if (characteristic == null
					|| characteristic.optInt(
							"EntityID",
							Integer.MIN_VALUE)
						!= characteristicEntityID) {

				continue;
			}

			org.json.JSONObject row =
					new org.json.JSONObject();

			row.put(
					"identifier",
					characteristic.optString(
							"Identifier",
							""));
			row.put(
					"recordKey",
					characteristic.optString(
							"RecordKey",
							""));
			row.put(
					"parentRecordKey",
					characteristic.optString(
							"ParentRecordKey",
							""));

			org.json.JSONObject selectedLanguage =
					characteristicLanguageValue(
							characteristic,
							languageEntityID,
							languageID);

			org.json.JSONObject selectedLookup =
					selectedLanguage != null
							&& selectedLanguage
									.optJSONObject("lookup")
									!= null
						? selectedLanguage.optJSONObject(
								"lookup")
						: characteristic.optJSONObject(
								"lookup");

			String selectedText =
					selectedLanguage != null
							&& selectedLanguage.has("Value")
						? selectedLanguage.optString(
								"Value",
								"")
						: characteristic.optString(
								"Value",
								"");

			if (selectedLookup != null
					&& !selectedLookup
							.optString("Code", "")
							.isEmpty()) {

				row.put(
						"code",
						selectedLookup.optString(
								"Code",
								""));
				row.put(
						"value",
						selectedLookup.optString(
								"Name",
								""));
			} else {
				row.put(
						"value",
						selectedText);
			}

			result.put(row);
		}

		return result;
	}


	private org.json.JSONObject characteristicLanguageValue(
			org.json.JSONObject characteristic,
			int languageEntityID,
			int languageID) {

		org.json.JSONArray languageValues =
				characteristic == null
						? null
						: characteristic.optJSONArray(
								"languageValues");

		if (languageValues == null) {
			return null;
		}

		for (int i = 0;
				i < languageValues.length();
				i++) {

			org.json.JSONObject row =
					languageValues.optJSONObject(i);

			if (row != null
					&& row.optInt(
							"EntityID",
							Integer.MIN_VALUE)
						== languageEntityID
					&& row.optInt(
							"LanguageID",
							Integer.MIN_VALUE)
						== languageID) {

				return row;
			}
		}

		return null;
	}


	private Number numberValue(
			org.json.JSONObject object,
			String property) {

		if (object == null || !object.has(property)) {
			return null;
		}

		Object value = object.opt(property);
		return value instanceof Number
				? (Number) value
				: null;
	}


	private String jsonString(Object value) {
		return value == null
				|| value == org.json.JSONObject.NULL
			? ""
			: String.valueOf(value);
	}


	private String statusLabel(Number status) {
		if (status == null) {
			return "";
		}

		switch (status.intValue()) {
			case 1001: return "Propuesta Generada";
			case 1002: return "Pendiente Inicio Enriquecimiento";
			case 1003: return "Revisión Compras";
			case 1004: return "Carga de Imagen";
			case 1005: return "Rechazada";
			case 1006: return "Por Actualizar";
			case 1007: return "Aprobada";
			case 1008: return "Modificación";
			case 1009: return "Cancelado";
			case 1010: return "En Proceso Liverpool";
			case 1011: return "En Proceso de Envío";
			case 1020: return "Creación de SKU";
			case 1021: return "Gobierno de Datos";
			case 1022: return "Revisión QA";
			case 1023: return "Category";
			case 1024: return "Rechazo Publicación";
			case 1025: return "Eliminada";
			case 1026: return "En Proceso Foro";
			case 1027: return "Rechazo Compras";
			case 1028: return "Rechazo QA";
			case 1029: return "Rechazo Gobierno";
			case 1030: return "Rechazo Category";
			case 1031: return "Repoblamiento";
			case 1032: return "Excepción de Catalogación";
			case 10031: return "Borrador";
			default: return "Desconocido";
		}
	}


	private org.json.JSONObject responseValue(org.json.JSONObject row) {
		org.json.JSONObject result = new org.json.JSONObject();
		String code = row.optString("code", "");
		if (!code.isEmpty()) {
			result.put("code", code);
		}
		result.put("value", row.optString("value", ""));
		return result;
	}

	private void putReadOnlyText(org.json.JSONObject readOnly, String property, String value) {
		if (value != null && !value.isEmpty()) {
			readOnly.put(property, new org.json.JSONObject().put("value", value));
		}
	}

	private boolean isMediaCharacteristic(String characteristicId) {
		for (String root : MEDIA_ROOTS) {
			if (root.equals(characteristicId) || characteristicId.startsWith(root + "_")) {
				return true;
			}
		}
		return false;
	}

	private org.json.JSONArray mediaElements(org.json.JSONArray rows, String root) {
		java.util.Map<String, org.json.JSONObject> byRecordKey = new java.util.LinkedHashMap<>();
		java.util.List<String> rootKeys = new java.util.ArrayList<>();

		for (int i = 0; i < rows.length(); i++) {
			org.json.JSONObject row = rows.getJSONObject(i);
			if (!root.equals(row.optString("identifier", ""))) continue;
			String recordKey = row.optString("recordKey", "");
			if (recordKey.isEmpty()) recordKey = "__root_" + i;
			if (!byRecordKey.containsKey(recordKey)) {
				byRecordKey.put(recordKey, new org.json.JSONObject());
				rootKeys.add(recordKey);
			}
		}

		for (int i = 0; i < rows.length(); i++) {
			org.json.JSONObject row = rows.getJSONObject(i);
			String id = row.optString("identifier", "");
			if (!id.startsWith(root + "_")) continue;

			String parentRecordKey = row.optString("parentRecordKey", "");
			String recordKey = row.optString("recordKey", "");
			String key = parentRecordKey;
			if (!byRecordKey.containsKey(key) && byRecordKey.containsKey(recordKey)) {
				key = recordKey;
			}
			org.json.JSONObject media = byRecordKey.get(key);
			if (media == null) continue; // same semantics as Object API: orphan children are ignored

			String v = row.optString("value", "");
			if (id.endsWith("_Name")) {
				media.put("MediaAssetName", v);
			} else if (id.endsWith("_URL")) {
				media.put("MediaAssetURL", v);
			} else if (id.endsWith("_Status")) {
				media.put("MediaAssetType", v);
			}
		}

		org.json.JSONArray result = new org.json.JSONArray();
		for (String key : rootKeys) {
			org.json.JSONObject media = byRecordKey.get(key);
			if (media != null && media.length() > 0) {
				result.put(media);
			}
		}
		return result;
	}

	private void putMediaArray(org.json.JSONObject target, String property, org.json.JSONArray values) {
		if (values != null && values.length() > 0) {
			target.put(property, values);
		}
	}

	public Object agrupamelos(String rawRequest, String baseURL, String encoded) {
		long init = System.currentTimeMillis();
		org.json.JSONObject generalResponse = null;
		org.json.JSONObject request;
		try {
			org.json.JSONObject hola = new org.json.JSONObject(rawRequest);
			request = hola.has("root") ? hola.getJSONObject("root") : hola;
		} catch (org.json.JSONException e) {
			logE(e);
			log(generalResponse = new org.json.JSONObject().put("Error", "Bad request"));
			return generalResponse;
		}

		org.json.JSONArray products;
		try {
			products = request.getJSONArray("products");
		} catch (org.json.JSONException e) {
			log(generalResponse = new org.json.JSONObject().put("Error", "Missing array \"products\""));
			return generalResponse;
		}
		if (products.length() == 0) {
			return new org.json.JSONObject().put("Responses", new org.json.JSONArray());
		}

		try {
			DataRequestor dr = new DataRequestor(dastub);
			org.json.JSONArray skus = new org.json.JSONArray();
			for (int i = 0; i < products.length(); i++) {
				org.json.JSONObject productElement = products.getJSONObject(i);
				if (productElement.has("sku")) skus.put(productElement.getString("sku"));
			}
			java.util.Map<String, java.util.Set<String>> skusResponse = dr.articleBySKUsWithSKUs(skus);
			org.json.JSONArray result = new org.json.JSONArray();
			for (java.util.Map.Entry<String, java.util.Set<String>> entry : skusResponse.entrySet()) {
				org.json.JSONArray variants = new org.json.JSONArray();
				for (String v : entry.getValue()) variants.put(v);
				result.put(new org.json.JSONObject().put("skuPadre", entry.getKey()).put("skusHijo", variants));
			}
			return result;
		} catch (Exception e) {
			logE(e);
			log((generalResponse = new org.json.JSONObject().put("Error", "Couldn't parse request")).toString());
		}
		log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		return generalResponse;
	}

	public Object procesamelo(String rawRequest, String baseURL, String encoded) {
		long init = System.currentTimeMillis();
		responses.clear();
		org.json.JSONObject generalResponse = null;
		org.json.JSONObject request;
		try {
			log("Parsing: " + rawRequest);
			org.json.JSONObject hola = new org.json.JSONObject(rawRequest);
			request = hola.has("root") ? hola.getJSONObject("root") : hola;
		} catch (org.json.JSONException e) {
			logE(e);
			log(generalResponse = new org.json.JSONObject().put("Error", "Bad request"));
			return generalResponse;
		}

		org.json.JSONArray products;
		try {
			products = request.getJSONArray("products");
		} catch (org.json.JSONException e) {
			log(generalResponse = new org.json.JSONObject().put("Error", "Missing array \"products\""));
			return generalResponse;
		}
		if (products.length() == 0) {
			return new org.json.JSONObject().put("Responses", new org.json.JSONArray());
		}

		try {
			DataRequestor dr = new DataRequestor(dastub);
			org.json.JSONArray skus = new org.json.JSONArray();
			log("Going over: " + products.length() + " products.");
			for (int i = 0; i < products.length(); i++) {
				org.json.JSONObject productElement = products.getJSONObject(i);
				if (productElement.has("sku")) skus.put(productElement.getString("sku"));
			}

			log("About to request data: " + skus.length() + " for SKUs");
			java.util.Map<String, java.util.Set<String>> skusResponse = dr.articleBySKUs(skus);
			log("Got a response of: " + skusResponse.size() + " products in total.");

			/*
			 * Phase 1: preserve the old semantics: AgarraloONo runs before the
			 * data used to build the response is read. Products for which checale
			 * fails are not included, exactly as in the previous Worker flow.
			 */
			AgarraloONo aono = new AgarraloONo(dastub);
			java.util.Map<String, java.util.Set<String>> readyProducts =
					new java.util.LinkedHashMap<>();

			for (java.util.Map.Entry<String, java.util.Set<String>> entry : skusResponse.entrySet()) {
				try {
					log("Checandole...");
					aono.checale(entry.getKey(), rw.getRw().getBaseUrl(), entry.getValue());
					readyProducts.put(entry.getKey(), entry.getValue());
				} catch (JSONException | ServiceUnavailableException e) {
					logE(e);
				}
			}

			/*
			 * Phase 2: one bulk read for every product in this request, one bulk
			 * relation read, and one bulk read for every variant. DBAccessDataStub
			 * handles Oracle IN-list chunking internally, so query count grows by
			 * chunks, never by product or variant.
			 */
			java.util.Set<String> productIdentifiers =
					new java.util.LinkedHashSet<>(readyProducts.keySet());

			log("Bulk loading " + productIdentifiers.size() + " Product2G entities from DB...");
			java.util.Map<String, org.json.JSONObject> productsById =
					dastub.getEntityData(1100, productIdentifiers);

			java.util.Map<String, java.util.Set<String>> variantsByProduct =
					dastub.getProductVariants(productIdentifiers);

			java.util.Set<String> allVariantIdentifiers =
					new java.util.LinkedHashSet<>();
			for (java.util.Set<String> productVariants : variantsByProduct.values()) {
				if (productVariants != null) {
					allVariantIdentifiers.addAll(productVariants);
				}
			}

			log("Bulk loading " + allVariantIdentifiers.size() + " Article entities from DB...");
			java.util.Map<String, org.json.JSONObject> variantsById =
					dastub.getEntityData(1000, allVariantIdentifiers);

			java.util.concurrent.ArrayBlockingQueue<Object[]> tasks =
					new java.util.concurrent.ArrayBlockingQueue<>(Math.max(1, readyProducts.size()));
			for (java.util.Map.Entry<String, java.util.Set<String>> entry : readyProducts.entrySet()) {
				tasks.add(new Object[] { entry.getKey(), entry.getValue() });
			}

			new Worker(
					1,
					tasks,
					productsById,
					variantsByProduct,
					variantsById).run();

			org.json.JSONArray result = new org.json.JSONArray();
			for (org.json.JSONObject j : responses) result.put(j);
			log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
			return result;
		} catch (Exception e) {
			logE(e);
			log((generalResponse = new org.json.JSONObject().put("Error", "Couldn't parse request")).toString());
		}
		log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		return generalResponse;
	}

	private void log(Object message) {
		LOGGER.info(String.valueOf(message));
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/getProposalsForo.err", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
			// Logging must not break the request.
		}
	}

	@Override
	public void close() throws IOException {
		dastub.close();
	}
}
