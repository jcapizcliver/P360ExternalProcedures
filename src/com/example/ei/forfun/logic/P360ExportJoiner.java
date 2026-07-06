package com.example.ei.forfun.logic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class P360ExportJoiner {

	public static final String SEP = ",";
	private static final String DELIMITER = "\"";
	private static final String SEPARATOR = ",";
	private static final String ESCAPE = "\"";

	public String serializeLine(String value) {
		try {
			return value == null ? ""
					: value.contains(SEPARATOR)
							|| value.contains(DELIMITER) || value.contains("\\"
									.equals(ESCAPE) ? "\\"
											: ESCAPE)
							|| value.contains("\n")
									? DELIMITER + value.replaceAll(
											"(?=[" + DELIMITER + ("\\".equals(ESCAPE) ? "\\\\" : ESCAPE) + "])",
											"\\".equals(ESCAPE) ? "\\\\" : ESCAPE) + DELIMITER
									: value;
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	public String serializeChunk(Object[] pieces) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < pieces.length; i++) {
			sb.append(i == 0 ? "" : SEPARATOR).append(serializeLine(String.valueOf(pieces[i])));
		}
		return sb.toString();
	}

	public void join(Path baseFile, Path structuresFile, Path productAcvFile, Path articleAcvFile, Path lookupFile,
			Path outputFile) throws IOException {

		Map<String, Row> baseRows = loadBase(baseFile);
		Map<String, Row> structuresByProduct = loadByKey(structuresFile, "ProductRevisionID");
		Map<String, Row> productAcvByProduct = loadByKey(productAcvFile, "ProductRevisionID");
		Map<String, Row> articleAcvByArticle = loadByKey(articleAcvFile, "ArticleRevisionID");
		Map<String, LookupRow> lookupsByValueId = loadLookups(lookupFile);

		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
			writer.write(buildHeader());
			writer.write("\n");

			for (Row base : baseRows.values()) {
				Row merged = new Row();

				merged.putAll(base);

				String productRevisionId = base.get("ProductRevisionID");
				String articleRevisionId = base.get("ArticleRevisionID");

				Row structures = structuresByProduct.get(productRevisionId);
				if (structures != null) {
					merged.putAll(structures);
				}

				Row productAcv = productAcvByProduct.get(productRevisionId);
				if (productAcv != null) {
					merged.putAll(productAcv);
				}

				Row articleAcv = articleAcvByArticle.get(articleRevisionId);
				if (articleAcv != null) {
					merged.putAll(articleAcv);
				}

				resolveLookup(merged, lookupsByValueId, "BusinessLookupValueID", "Product2G.Business->LookupValue.Code",
						"Product2G.Business->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "ItemGroupLookupValueID",
						"Product2GExtraData.ItemGroup(MX)->LookupValue.Code",
						"Product2GExtraData.ItemGroup(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "BrandNameLookupValueID",
						"Product2GExtraData.BrandName(MX)->LookupValue.Code",
						"Product2GExtraData.BrandName(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "SectionLookupValueID",
						"Product2GExtraData.Section(MX)->LookupValue.Code",
						"Product2GExtraData.Section(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "DireccionLookupValueID",
						"Product2GExtraData.Direccion(MX)->LookupValue.Code",
						"Product2GExtraData.Direccion(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "ItemGroupS4HLookupValueID",
						"Product2GExtraData.ItemGroupS4H(MX)->LookupValue.Code",
						"Product2GExtraData.ItemGroupS4H(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "SupplierIDLookupValueID",
						"Product2GExtraData.SupplierID(MX)->LookupValue.Code",
						"Product2GExtraData.SupplierID(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "BRAND_ID_S4HLookupValueID",
						"Product2GExtraData.BRAND_ID_S4H(MX)->LookupValue.Code",
						"Product2GExtraData.BRAND_ID_S4H(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "ColoursLiverpoolAttLookupValueID",
						"ArticleExtraData.ColoursLiverpoolAtt(MX)->LookupValue.Code",
						"ArticleExtraData.ColoursLiverpoolAtt(MX)->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "TamanoUnicoLookupValueID",
						"ArticleExtraData.TamanoUnico(MX)->LookupValue.Code",
						"ArticleExtraData.TamanoUnico(MX)->LookupValueLang.Name(es)");

				resolveLookup(merged, lookupsByValueId, "IsPublishExceptionLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('IsPublishException')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('IsPublishException')->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "SistemaOrigenLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('SistemaOrigen')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('SistemaOrigen')->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "FotoTomadaLiverpoolLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool')->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "IdentificaNegocioLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('IdentificaNegocio')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('IdentificaNegocio')->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "GenderAttLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "WHERLLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('WHERL')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('WHERL')->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "EXTWG_S4HLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValueLang.Name(es)");
				resolveLookup(merged, lookupsByValueId, "ProductTypeSAPLookupValueID",
						"SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValue.Code",
						"SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValueLang.Name(es)");

				writer.write(buildOutputLine(merged));
				writer.write("\n");
			}
		}
	}

	private Map<String, Row> loadBase(Path file) throws IOException {
		Map<String, Row> map = new LinkedHashMap<>();
		for (Row row : parse(file)) {
			String key = buildBaseKey(row.get("ProductRevisionID"), row.get("ArticleRevisionID"));
			map.put(key, row);
		}
		return map;
	}

	private Map<String, Row> loadByKey(Path file, String keyColumn) throws IOException {
		Map<String, Row> map = new LinkedHashMap<>();
		for (Row row : parse(file)) {
			map.put(row.get(keyColumn), row);
		}
		return map;
	}

	private Map<String, LookupRow> loadLookups(Path file) throws IOException {
		Map<String, LookupRow> map = new LinkedHashMap<>();
		for (Row row : parse(file)) {
			LookupRow lookup = new LookupRow(row.get("LookupValueID"), row.get("LookupID"), row.get("Code"),
					row.get("Name"));
			map.put(lookup.lookupValueId, lookup);
		}
		return map;
	}

	private void resolveLookup(Row row, Map<String, LookupRow> lookups, String idColumn, String codeColumn,
			String nameColumn) {
		String lookupValueId = row.get(idColumn);
		if (lookupValueId == null || lookupValueId.isBlank()) {
			row.put(codeColumn, "");
			row.put(nameColumn, "");
			return;
		}

		LookupRow lookup = lookups.get(lookupValueId);
		if (lookup == null) {
			row.put(codeColumn, "");
			row.put(nameColumn, "");
			return;
		}

		row.put(codeColumn, nullToEmpty(lookup.code));
		row.put(nameColumn, nullToEmpty(lookup.name));
	}

	private String buildBaseKey(String productRevisionId, String articleRevisionId) {
		return nullToEmpty(productRevisionId) + "||" + nullToEmpty(articleRevisionId);
	}

	private String buildHeader() {
		String[] headers = new String[] { "ProductRevisionID", "Product2G_ProductNo", "Product2G_SKU", "Product2G_EAN",
				"Product2G_CurrentStatus", "Product2G_PrevStatus", "Product2G_Business_Code",
				"Product2G_BusinessLang_Name_es", "Product2GExtraData_ItemGroup_Code",
				"Product2GExtraData_ItemGroup_Name_es", "Product2GExtraData_BrandName_Code",
				"Product2GExtraData_BrandName_Name_es", "Product2GExtraData_Section_Code",
				"Product2GExtraData_Section_Name_es", "Product2GExtraData_Direccion_Code",
				"Product2GExtraData_Direccion_Name_es", "Product2GExtraData_ItemGroupS4H_Code",
				"Product2GExtraData_ItemGroupS4H_Name_es", "Product2GExtraData_SupplierID_Code",
				"Product2GExtraData_SupplierID_Name_es", "Product2GExtraData_BRAND_ID_S4H_Code",
				"Product2GExtraData_BRAND_ID_S4H_Name_es", "Product2GExtraData_SupplierPartNumber",
				"Product2GLang_DescriptionShort_es", "Product2GLang_ProductName_es", "Product2GLog_CreationDate(PIM)",
				"Product2GLog_ModificationDate(PIM)", "Product2G_LastDateApproved", "PrimaryProductTaxonomyCodes",
				"PrimaryProductTaxonomyNames", "SitiosWebCodes", "SitiosWebNames", "EnrichmentRejectionMessage",
				"IsPublishException_Code", "IsPublishException_Name_es", "SistemaOrigen_Code", "SistemaOrigen_Name_es",
				"Footnote", "FotoTomadaLiverpool_Code", "FotoTomadaLiverpool_Name_es", "IdentificaNegocio_Code",
				"IdentificaNegocio_Name_es", "GenderAtt_Code", "GenderAtt_Name_es", "ApprovedDateCalc", "BrandNameATG",
				"WHERL_Code", "WHERL_Name_es", "IDLastParent", "FechaUltimaAprobacion", "AssignTakeNoTake",
				"EnriquecidoEnForo", "EXTWG_S4H_Code", "EXTWG_S4H_Name_es", "ProductTypeSAP_Code",
				"ProductTypeSAP_Name_es", "BrandOwner", "ArticleRevisionID", "Article_SupplierAID", "Article_SKU",
				"Article_EAN", "Article_ColoursLiverpoolAtt_Code", "Article_ColoursLiverpoolAtt_Name_es",
				"Article_TamanoUnico_Code", "Article_TamanoUnico_Name_es", "Article_SupplierPartNumber",
				"Article_ProcedeNoProcede", "AIEnriched", "AIEnrichementDate", "TamanoUnicoSTD", "clothingSize",
				"SizeVaD", "ArticleLog_CreationDate", "ArticleLog_ModificationDate", "Article_LastDateApproved" };
		return serializeChunk(headers);
	}

	private String buildOutputLine(Row row) {
		Object[] pieces = new Object[] { row.get("ProductRevisionID"), row.get("Product2G.ProductNo"),
				row.get("Product2G.SKU"), row.get("Product2G.EAN"), row.get("Product2G.CurrentStatus"),
				row.get("Product2G.PrevStatus"), row.get("Product2G.Business->LookupValue.Code"),
				row.get("Product2G.Business->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.ItemGroup(MX)->LookupValue.Code"),
				row.get("Product2GExtraData.ItemGroup(MX)->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.BrandName(MX)->LookupValue.Code"),
				row.get("Product2GExtraData.BrandName(MX)->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.Section(MX)->LookupValue.Code"),
				row.get("Product2GExtraData.Section(MX)->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.Direccion(MX)->LookupValue.Code"),
				row.get("Product2GExtraData.Direccion(MX)->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.ItemGroupS4H(MX)->LookupValue.Code"),
				row.get("Product2GExtraData.ItemGroupS4H(MX)->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.SupplierID(MX)->LookupValue.Code"),
				row.get("Product2GExtraData.SupplierID(MX)->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.BRAND_ID_S4H(MX)->LookupValue.Code"),
				row.get("Product2GExtraData.BRAND_ID_S4H(MX)->LookupValueLang.Name(es)"),
				row.get("Product2GExtraData.SupplierPartNumber(MX)"), row.get("Product2GLang.DescriptionShort(es)"),
				row.get("Product2GLang.ProductName(es)"), row.get("Product2GLog.CreationDate(PIM)"),
				row.get("Product2GLog.ModificationDate(PIM)"), row.get("Product2G.LastDateApproved"),
				row.get("PrimaryProductTaxonomyCodes"), row.get("PrimaryProductTaxonomyNames"),
				row.get("SitiosWebCodes"), row.get("SitiosWebNames"), row.get("EnrichmentRejectionMessage"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('IsPublishException')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('IsPublishException')->LookupValueLang.Name(es)"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('SistemaOrigen')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('SistemaOrigen')->LookupValueLang.Name(es)"),
				row.get("Footnote"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool')->LookupValueLang.Name(es)"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('IdentificaNegocio')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('IdentificaNegocio')->LookupValueLang.Name(es)"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValueLang.Name(es)"),
				row.get("ApprovedDateCalc"), row.get("BrandNameATG"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('WHERL')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('WHERL')->LookupValueLang.Name(es)"),
				row.get("IDLastParent"), row.get("FechaUltimaAprobacion"), row.get("AssignTakeNoTake"),
				row.get("EnriquecidoEnForo"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValueLang.Name(es)"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValue.Code"),
				row.get("SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValueLang.Name(es)"),
				row.get("BrandOwner"), row.get("ArticleRevisionID"), row.get("Article.SupplierAID"),
				row.get("Article.SKU"), row.get("Article.EAN"),
				row.get("ArticleExtraData.ColoursLiverpoolAtt(MX)->LookupValue.Code"),
				row.get("ArticleExtraData.ColoursLiverpoolAtt(MX)->LookupValueLang.Name(es)"),
				row.get("ArticleExtraData.TamanoUnico(MX)->LookupValue.Code"),
				row.get("ArticleExtraData.TamanoUnico(MX)->LookupValueLang.Name(es)"),
				row.get("ArticleExtraData.SupplierPartNumber(MX)"), row.get("Article.ProcedeNoProcede"),
				row.get("AIEnriched"), row.get("AIEnrichementDate"), row.get("TamanoUnicoSTD"), row.get("clothingSize"),
				row.get("SizeVaD"), row.get("ArticleLog.CreationDate(PIM)"),
				row.get("ArticleLog.ModificationDate(PIM)"), row.get("Article.LastDateApproved") };
		return serializeChunk(pieces);
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private Iterable<Row> parse(Path file) throws IOException {
		// Aquí adapta tu SimpleDelimitedFileParser.
		// La idea es que devuelvas filas como mapa nombreColumna -> valor.
		// throw new UnsupportedOperationException("Conecta aquí tu
		// SimpleDelimitedFileParser");
		final java.util.List<Row> rows = new java.util.ArrayList<>();
		final java.util.List<String[]> rawLines = new java.util.ArrayList<>();

		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n",
				java.nio.charset.StandardCharsets.UTF_8, rawLines::add);

		parser.parse(file);

		if (rawLines.isEmpty()) {
			return rows;
		}

		String[] headers = rawLines.get(0);

		for (int i = 1; i < rawLines.size(); i++) {
			String[] values = rawLines.get(i);

			if (values == null || values.length == 0) {
				continue;
			}

			if (values.length > 0 && values[0] != null && values[0].startsWith("#")) {
				continue;
			}

			Row row = new Row();
			for (int j = 0; j < headers.length; j++) {
				String header = headers[j];
				String value = j < values.length ? values[j] : "";
				row.put(header, value);
			}

			rows.add(row);
		}

		return rows;
	}

	static class Row extends LinkedHashMap<String, String> {
		@Override
		public String get(Object key) {
			return super.getOrDefault(key, "");
		}
	}

	static class LookupRow {
		final String lookupValueId;
		final String lookupId;
		final String code;
		final String name;

		LookupRow(String lookupValueId, String lookupId, String code, String name) {
			this.lookupValueId = Objects.toString(lookupValueId, "");
			this.lookupId = Objects.toString(lookupId, "");
			this.code = Objects.toString(code, "");
			this.name = Objects.toString(name, "");
		}
	}
}
