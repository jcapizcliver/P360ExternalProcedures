package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Export fijo Standard Template - Category IA para Product2G seleccionados. */
final class CategoryIaDataReader
{
  private static final String ACTIVE_TS =
      "timestamp '9999-12-31 00:00:00.0'";
  private static final String GTT = "P360_EXPLOIT.\"P360_EXPORT_KEYS\"";
  private static final int DIRECT_IN_LIMIT = 1000;
  private static final int FETCH_SIZE = 1000;
  private static final int QUERY_TIMEOUT_SECONDS = 30;
  private static final int EXCEL_CELL_LIMIT = 32767;
  private static final String MULTI_VALUE_SEPARATOR = " | ";
  private static final String CSV_ENTRY_NAME = "Standard_Template_Category_IA.csv";
  private static final DateTimeFormatter FILE_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private static final List<String> CHARACTERISTICS = Collections.unmodifiableList(
      Arrays.asList(
          "ProductTypeSAP",
          "ProductTypeSAPTEMPSBB",
          "EXTWG_S4H",
          "GeneroVaD",
          "GenderAtt",
          "DisciplinaVaD",
          "DisciplineAtt"
      )
  );

  private static final List<ReportColumn> REPORT_COLUMNS = Collections.unmodifiableList(
      Arrays.asList(
          new ReportColumn("Propuesta", "Identifier"),
          new ReportColumn("Codigo SKU", "SKU"),
          new ReportColumn("Tipo de SKU", "SAPObjectType"),
          new ReportColumn("Nombre del Producto", "ProductName"),
          new ReportColumn("ProductTypeSAP", "ProductTypeSAP"),
          new ReportColumn("ProductTypeSAPTEMPSBB", "ProductTypeSAPTEMPSBB"),
          new ReportColumn("Direccion", "Direction"),
          new ReportColumn("Seccion", "Section"),
          new ReportColumn("Grupo de Articulos ECC", "ItemGroup"),
          new ReportColumn("Grupo de Articulos S4H", "ItemGroupS4H"),
          new ReportColumn("Num Proveedor", "SupplierIDCode"),
          new ReportColumn("Nombre Proveedor", "SupplierID"),
          new ReportColumn("Marca", "BrandName"),
          new ReportColumn("Marca SH4", "BRAND_ID_S4H"),
          new ReportColumn("Clasificacion Web", "CatIDs"),
          new ReportColumn("Nombre Plantilla", "TemplateName"),
          new ReportColumn("Tipo de Negocio (LVP y MKP)", "Negocio"),
          new ReportColumn("Tipo de Negocio (SBB)", "EXTWG_S4H"),
          new ReportColumn("EXTWG_S4H", "EXTWG_S4H"),
          new ReportColumn("GeneroVaD", "GeneroVaD"),
          new ReportColumn("GenderAtt", "GenderAtt"),
          new ReportColumn("Nombre de Publicación", "Name"),
          new ReportColumn("Disciplina", "DisciplinaVaD"),
          new ReportColumn("Disciplina2", "DisciplineAtt"),
          new ReportColumn("GA", "ItemGroup"),
          new ReportColumn("Negocio SAP", "NegocioCode"),
          new ReportColumn("Tipo de Negocio", "BusinessCode")
      )
  );

  private StructureIds cachedStructures;
  private Map<String, CharMeta> cachedCharacteristics;

  ExportResult export(
      Connection connection,
      List<String> selectedIdentifiers,
      Path outputDirectory,
      ProgressListener listener) throws SQLException, IOException
  {
    List<String> identifiers = uniqueNonBlank(selectedIdentifiers);
    if (identifiers.isEmpty())
    {
      throw new IllegalArgumentException("No hay Product2G seleccionados");
    }

    Files.createDirectories(outputDirectory);
    Path zipFile = Files.createTempFile(
        outputDirectory,
        "Standard_Template_Category_IA_",
        ".zip"
    );
    boolean success = false;

    try
    {
      notify(listener, "Resolviendo metadatos...");
      if (cachedStructures == null) cachedStructures = resolveStructureIds(connection);
      StructureIds structureIds = cachedStructures;
      if (cachedCharacteristics == null) cachedCharacteristics = resolveCharacteristics(connection, CHARACTERISTICS);
      Map<String, CharMeta> characteristics = cachedCharacteristics;

      notify(listener, "Consultando Product2G seleccionados...");
      List<RowData> products = loadProducts(connection, identifiers);

      notify(listener, "Consultando plantilla y clasificación web...");
      loadStructureValues(connection, products, structureIds);

      notify(listener, "Consultando características...");
      loadCharacteristicValues(connection, products, characteristics);

      notify(listener, "Armando " + CSV_ENTRY_NAME + "...");
      OutputCounts counts = writeZip(zipFile, identifiers, products);

      success = true;
      return new ExportResult(
          zipFile,
          "Standard_Template_Category_IA_"
              + LocalDateTime.now().format(FILE_TIMESTAMP)
              + ".zip",
          counts.productRows,
          counts.notFoundRows
      );
    }
    finally
    {
      clearTemporaryKeysQuietly(connection);
      if (!success)
      {
        Files.deleteIfExists(zipFile);
      }
    }
  }

  private List<RowData> loadProducts(
      Connection connection,
      List<String> identifiers) throws SQLException
  {
    Selection selection = prepareSelection(connection, identifiers);
    String hint;
    String fromClause;
    String identifierWhere;


      hint = "/*+ qb_name(category_ia_product_in) "
          + "leading(ar ad) use_nl(ad dom alang) "
          + "index(ar XAK2_ArticleRevision) */";
      fromClause =
          "from PIM_MASTER.\"ArticleRevision\" ar "
        + "inner join PIM_MASTER.\"ArticleDetail\" ad "
        + "on ad.\"ArticleRevisionID\" = ar.\"ID\" ";
      identifierWhere =
          "and ar.\"Identifier\" in (" + placeholders(identifiers.size()) + ") ";
    

    String sql =
        "select " + hint + " "
      + "ar.\"ID\" as \"ArticleRevisionID\", "
      + "ar.\"Identifier\" as \"Identifier\", "
      + "ad.\"Res_Int_02\" as \"SKU\", "
      + lookupColumns("business", "Business") + ", "
      + lookupColumns("direction_lov", "Direction") + ", "
      + lookupColumns("section_lov", "Section") + ", "
      + lookupColumns("item_group", "ItemGroup") + ", "
      + lookupColumns("item_group_s4h", "ItemGroupS4H") + ", "
      + lookupColumns("brand", "BrandName") + ", "
      + lookupColumns("brand_s4h", "BRAND_ID_S4H") + ", "
      + lookupColumns("business_domain", "Negocio") + ", "
      + lookupColumns("sap_type", "SAPObjectType") + ", "
      + lookupColumns("supplier", "SupplierID") + ", "
      + "alang.\"Res_Text250_01\" as \"ProductName\", "
      + "alang.\"DescriptionShort\" as \"Name\" "
      + fromClause
      + "left join PIM_MASTER.\"ArticleDomain\" dom "
      + "on dom.\"ArticleRevisionID\" = ar.\"ID\" "
      + "and dom.\"EntityID\" = 21006 and dom.\"ChannelID\" = 1 and dom.\"TargetMarket\" = 'MX' "
      + "and dom.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "left join PIM_MASTER.\"ArticleLang\" alang "
      + "on alang.\"ArticleRevisionID\" = ar.\"ID\" "
      + "and alang.\"LanguageID\" = 10 and alang.\"ChannelID\" = 1 and alang.\"EntityID\" = 1105 "
      + "and alang.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + lookupJoin("business", "ad.\"Res_Int_01\"")
      + lookupJoin("direction_lov", "dom.\"Res_Int_01\"")
      + lookupJoin("section_lov", "dom.\"Res_Int_02\"")
      + lookupJoin("item_group", "dom.\"Res_Int_03\"")
      + lookupJoin("item_group_s4h", "dom.\"Res_Int_04\"")
      + lookupJoin("brand", "dom.\"Res_Int_05\"")
      + lookupJoin("brand_s4h", "dom.\"Res_Int_06\"")
      + lookupJoin("business_domain", "dom.\"Res_Int_07\"")
      + lookupJoin("sap_type", "dom.\"Res_Int_08\"")
      + lookupJoin("supplier", "dom.\"Std_Int_10\"")
      + "where ar.\"EntityID\" = 1100 "
      + "and ar.\"RevisionID\" = 1 "
      + "and ar.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "and ad.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + identifierWhere;

    List<RowData> rows = new ArrayList<RowData>();
    Map<Long, RowData> merged = new LinkedHashMap<Long, RowData>();
    try (PreparedStatement statement = connection.prepareStatement(sql))
    {
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      statement.setFetchSize(FETCH_SIZE);
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      if (!selection.temporary)
      {
        bindStrings(statement, identifiers);
      }

      try (ResultSet result = statement.executeQuery())
      {
        while (result.next())
        {
          long revision = result.getLong("ArticleRevisionID");
          RowData row = merged.get(revision);
          if (row == null) { row = new RowData(revision); merged.put(revision,row); rows.add(row); }
          row.matchKey = result.getString("Identifier");
          for (String column : Arrays.asList(
              "Identifier", "SKU", "Business", "BusinessCode",
              "Direction", "DirectionCode", "Section", "SectionCode",
              "ItemGroup", "ItemGroupCode", "ItemGroupS4H", "ItemGroupS4HCode",
              "BrandName", "BrandNameCode", "BRAND_ID_S4H", "BRAND_ID_S4HCode",
              "Negocio", "NegocioCode", "SAPObjectType", "SAPObjectTypeCode",
              "SupplierID", "SupplierIDCode", "ProductName", "Name"))
          {
            row.add(column, result.getString(column));
          }
        }
      }
    }
    return rows;
  }

  private void loadStructureValues(
      Connection connection,
      List<RowData> rows,
      StructureIds structures) throws SQLException
  {
    if (rows.isEmpty())
    {
      return;
    }

    Map<Long, RowData> byId = new HashMap<Long, RowData>();
    List<String> revisionIds = new ArrayList<String>();
    for (RowData row : rows)
    {
      byId.put(Long.valueOf(row.revisionId), row);
      revisionIds.add(String.valueOf(row.revisionId));
    }

    revisionIds = unique(revisionIds);
    Selection selection = prepareSelection(connection, revisionIds);
    String hint;
    String fromClause;
    String revisionWhere;


      hint = "/*+ qb_name(category_ia_structure_in) "
          + "leading(input_key asm sgr sgl) use_nl(asm sgr sgl) "
          + "index(asm IX_ASM_TUNE_01) */";
      fromClause = "from (" + inputKeys(revisionIds.size()) + ") input_key inner join PIM_MASTER.\"ArticleStructureMap\" asm on asm.\"ArticleRevisionID\" = input_key.\"ID\" ";
      revisionWhere = "";
    

    String sql =
        "select " + hint + " "
      + "asm.\"ArticleRevisionID\" as \"ArticleRevisionID\", "
      + "asm.\"StructureID\" as \"StructureID\", "
      + "asm.\"StructureGroupIdentifier\" as \"StructureGroupIdentifier\", "
      + "sgl.\"Name\" as \"StructureGroupName\" "
      + fromClause
      + "left join PIM_MAIN.\"StructureGroupRevision\" sgr "
      + "on sgr.\"Identifier\" = asm.\"StructureGroupIdentifier\" "
      + "and sgr.\"StructureID\" = asm.\"StructureID\" "
      + "and sgr.\"RevisionID\" = 1 "
      + "and sgr.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "left join PIM_MAIN.\"StructureGroupLang\" sgl "
      + "on sgl.\"StructureGroupRevisionID\" = sgr.\"ID\" "
      + "and sgl.\"LanguageID\" = 10 "
      + "and sgl.\"Name\" is not null "
      + "and sgl.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "where asm.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "and asm.\"StructureID\" in (" + structures.template + ", "
      + structures.catIds + ") "
      + revisionWhere
      + "order by asm.\"ArticleRevisionID\", asm.\"StructureID\", "
      + "asm.\"StructureGroupIdentifier\", sgl.\"Name\"";

    try (PreparedStatement statement = connection.prepareStatement(sql))
    {
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      statement.setFetchSize(FETCH_SIZE);
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      if (!selection.temporary)
      {
        for (int index = 0; index < revisionIds.size(); index++)
        {
          statement.setLong(index + 1, Long.parseLong(revisionIds.get(index)));
        }
      }

      try (ResultSet result = statement.executeQuery())
      {
        while (result.next())
        {
          RowData row = byId.get(Long.valueOf(result.getLong("ArticleRevisionID")));
          if (row == null)
          {
            continue;
          }

          long structureId = result.getLong("StructureID");
          String identifier = result.getString("StructureGroupIdentifier");
          if (structureId == structures.template)
          {
            row.add(
                "TemplateName",
                firstNotEmpty(result.getString("StructureGroupName"), identifier)
            );
          }
          else if (structureId == structures.catIds)
          {
            row.add("CatIDs", identifier);
          }
        }
      }
    }
  }

  private void loadCharacteristicValues(
      Connection connection,
      List<RowData> rows,
      Map<String, CharMeta> characteristics) throws SQLException
  {
    if (rows.isEmpty() || characteristics.isEmpty())
    {
      return;
    }

    Map<Long, RowData> byId = new HashMap<Long, RowData>();
    List<String> revisionIds = new ArrayList<String>();
    for (RowData row : rows)
    {
      byId.put(Long.valueOf(row.revisionId), row);
      revisionIds.add(String.valueOf(row.revisionId));
    }

    revisionIds = unique(revisionIds);
    Selection selection = prepareSelection(connection, revisionIds);
    String hint;
    String fromClause;
    String revisionWhere;


      hint = "/*+ qb_name(category_ia_characteristic_in) "
          + "leading(input_key acv cr lvr lvl) use_nl(acv cr lvr lvl) "
          + "index(acv XAK1_ArticleCharactValue) */";
      fromClause = "from (" + inputKeys(revisionIds.size()) + ") input_key inner join PIM_MASTER.\"ArticleCharactValue\" acv on acv.\"ArticleRevisionID\" = input_key.\"ID\" ";
      revisionWhere = "";
    

    List<Long> characteristicIds = new ArrayList<Long>();
    for (CharMeta meta : characteristics.values())
    {
      if (!characteristicIds.contains(Long.valueOf(meta.id)))
      {
        characteristicIds.add(Long.valueOf(meta.id));
      }
    }

    String sql =
        "select " + hint + " "
      + "acv.\"ArticleRevisionID\" as \"ArticleRevisionID\", "
      + "cr.\"Identifier\" as \"CharacteristicIdentifier\", "
      + "acv.\"Value\" as \"DirectValue\", "
      + "lvr.\"Code\" as \"LookupCode\", "
      + "lvl.\"Name\" as \"LookupName\" "
      + fromClause
      + "inner join PIM_MAIN.\"CharacteristicRevision\" cr "
      + "on cr.\"CharacteristicID\" = acv.\"CharacteristicID\" "
      + "and cr.\"RevisionID\" = 1 "
      + "and cr.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "left join PIM_MAIN.\"LookupValueRevision\" lvr "
      + "on lvr.\"LookupValueID\" = acv.\"LookupValueID\" "
      + "and lvr.\"LookupID\" = cr.\"LookupID\" "
      + "and lvr.\"RevisionID\" = 1 "
      + "and lvr.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "left join PIM_MAIN.\"LookupValueLang\" lvl "
      + "on lvl.\"LookupValueRevisionID\" = lvr.\"ID\" "
      + "and lvl.\"LanguageID\" = 10 "
      + "and lvl.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "where acv.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "and acv.\"CharacteristicID\" in (" + longList(characteristicIds) + ") "
      + revisionWhere
      + "order by acv.\"ArticleRevisionID\", acv.\"RecordKey\" nulls first, "
      + "acv.\"ParentRecordKey\" nulls first, acv.\"ID\"";

    try (PreparedStatement statement = connection.prepareStatement(sql))
    {
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      statement.setFetchSize(FETCH_SIZE);
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      if (!selection.temporary)
      {
        for (int index = 0; index < revisionIds.size(); index++)
        {
          statement.setLong(index + 1, Long.parseLong(revisionIds.get(index)));
        }
      }

      try (ResultSet result = statement.executeQuery())
      {
        while (result.next())
        {
          RowData row = byId.get(Long.valueOf(result.getLong("ArticleRevisionID")));
          if (row == null)
          {
            continue;
          }

          CharMeta meta = characteristics.get(
              lower(result.getString("CharacteristicIdentifier"))
          );
          if (meta == null)
          {
            continue;
          }

          if (meta.lookup)
          {
            row.add(
                meta.identifier,
                firstNotEmpty(
                    result.getString("LookupName"),
                    result.getString("LookupCode"),
                    result.getString("DirectValue")
                )
            );
            row.add(meta.identifier + "Code", result.getString("LookupCode"));
          }
          else
          {
            row.add(meta.identifier, result.getString("DirectValue"));
          }
        }
      }
    }
  }

  private Map<String, CharMeta> resolveCharacteristics(
      Connection connection,
      List<String> requestedNames) throws SQLException
  {
    String sql =
        "select /*+ qb_name(category_ia_characteristics) "
      + "index(cr XAK2_CharacteristicRevision) */ "
      + "cr.\"CharacteristicID\", cr.\"Identifier\", cr.\"LookupID\" "
      + "from PIM_MAIN.\"CharacteristicRevision\" cr "
      + "where cr.\"Identifier\" in (" + placeholders(requestedNames.size()) + ") "
      + "and cr.\"RevisionID\" = 1 "
      + "and cr.\"DeletionTimestamp\" = " + ACTIVE_TS;

    Map<String, CharMeta> result = new LinkedHashMap<String, CharMeta>();
    try (PreparedStatement statement = connection.prepareStatement(sql))
    {
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      bindStrings(statement, requestedNames);
      try (ResultSet rows = statement.executeQuery())
      {
        while (rows.next())
        {
          String identifier = rows.getString("Identifier");
          result.put(
              lower(identifier),
              new CharMeta(
                  rows.getLong("CharacteristicID"),
                  identifier,
                  rows.getObject("LookupID") != null
              )
          );
        }
      }
    }

    List<String> missing = new ArrayList<String>();
    for (String requested : requestedNames)
    {
      if (!result.containsKey(lower(requested)))
      {
        missing.add(requested);
      }
    }
    if (!missing.isEmpty())
    {
      throw new IllegalArgumentException(
          "No se encontraron características activas: " + join(missing, ", ")
      );
    }
    return result;
  }

  private StructureIds resolveStructureIds(Connection connection) throws SQLException
  {
    String sql =
        "select sr.\"Identifier\", sr.\"StructureID\" "
      + "from PIM_MAIN.\"StructureRevision\" sr "
      + "where sr.\"Identifier\" in (?, ?) "
      + "and sr.\"RevisionID\" = 1 "
      + "and sr.\"DeletionTimestamp\" = " + ACTIVE_TS;

    Map<String, List<Long>> values = new HashMap<String, List<Long>>();
    try (PreparedStatement statement = connection.prepareStatement(sql))
    {
      statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
      statement.setString(1, "PrimaryProductTaxonomy");
      statement.setString(2, "Sitios Web");
      try (ResultSet result = statement.executeQuery())
      {
        while (result.next())
        {
          String identifier = result.getString("Identifier");
          String key = lower(identifier);
          List<Long> ids = values.get(key);
          if (ids == null)
          {
            ids = new ArrayList<Long>();
            values.put(key, ids);
          }
          ids.add(Long.valueOf(result.getLong("StructureID")));
        }
      }
    }

    return new StructureIds(
        exactlyOne(values, "PrimaryProductTaxonomy"),
        exactlyOne(values, "Sitios Web")
    );
  }

  private OutputCounts writeZip(
      Path zipFile,
      List<String> selectedIdentifiers,
      List<RowData> products) throws IOException
  {
    Map<String, List<RowData>> productByIdentifier = groupByMatchKey(products);
    long productRows = 0L;
    long notFoundRows = 0L;

    try (OutputStream output = Files.newOutputStream(
             zipFile,
             StandardOpenOption.TRUNCATE_EXISTING,
             StandardOpenOption.WRITE);
         ZipOutputStream zip = new ZipOutputStream(output))
    {
      zip.putNextEntry(new ZipEntry(CSV_ENTRY_NAME));
      BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(zip, StandardCharsets.UTF_8)
      );
      writer.write('\uFEFF');
      writeHeader(writer);

      for (String selectedIdentifier : selectedIdentifiers)
      {
        List<RowData> matching = productByIdentifier.get(lower(selectedIdentifier));
        if (matching == null || matching.isEmpty())
        {
          RowData empty = new RowData(-1L);
          empty.put("Identifier", selectedIdentifier);
          writeRow(writer, empty);
          productRows++;
          notFoundRows++;
          continue;
        }

        for (RowData product : matching)
        {
          writeRow(writer, product);
          productRows++;
        }
      }

      writer.flush();
      zip.closeEntry();
      zip.finish();
    }
    return new OutputCounts(productRows, notFoundRows);
  }

  private void writeHeader(BufferedWriter writer) throws IOException
  {
    for (int index = 0; index < REPORT_COLUMNS.size(); index++)
    {
      if (index > 0)
      {
        writer.write(',');
      }
      writeCsvValue(writer, REPORT_COLUMNS.get(index).header);
    }
    writer.write('\n');
  }

  private void writeRow(BufferedWriter writer, RowData row) throws IOException
  {
    for (int index = 0; index < REPORT_COLUMNS.size(); index++)
    {
      if (index > 0)
      {
        writer.write(',');
      }
      ReportColumn column = REPORT_COLUMNS.get(index);
      String value = "";
      for (String source : column.sources)
      {
        value = row.value(source);
        if (!value.isEmpty())
        {
          break;
        }
      }
      writeCsvValue(writer, oneLine(truncate(value)));
    }
    writer.write('\n');
  }

  private void writeCsvValue(BufferedWriter writer, String value) throws IOException
  {
    writer.write('"');
    writer.write(safe(value).replace("\"", "\"\""));
    writer.write('"');
  }

  private String lookupJoin(String alias, String valueExpression)
  {
    return
        "left join PIM_MAIN.\"LookupValueRevision\" " + alias + "_value "
      + "on " + alias + "_value.\"LookupValueID\" = " + valueExpression + " "
      + "and " + alias + "_value.\"RevisionID\" = 1 "
      + "and " + alias + "_value.\"DeletionTimestamp\" = " + ACTIVE_TS + " "
      + "left join PIM_MAIN.\"LookupValueLang\" " + alias + "_lang "
      + "on " + alias + "_lang.\"LookupValueRevisionID\" = " + alias + "_value.\"ID\" "
      + "and " + alias + "_lang.\"LanguageID\" = 10 "
      + "and " + alias + "_lang.\"DeletionTimestamp\" = " + ACTIVE_TS + " ";
  }

  private String lookupColumns(String alias, String output)
  {
    return alias + "_lang.\"Name\" as \"" + output + "\", "
        + alias + "_value.\"Code\" as \"" + output + "Code\"";
  }

  private String inputKeys(int count) {
    StringBuilder sql = new StringBuilder();
    for (int i=0;i<count;i++) { if(i>0)sql.append(" union all "); sql.append("select cast(? as number) as \"ID\" from dual"); }
    return sql.toString();
  }
  private Selection prepareSelection(Connection connection, List<String> values) {
    if (values.size() > 100) throw new IllegalArgumentException("Batch exceeds 100");
    return new Selection(false);
  }
  private void clearTemporaryKeysQuietly(Connection connection) { }

  private Map<String, List<RowData>> groupByMatchKey(List<RowData> rows)
  {
    Map<String, List<RowData>> result = new LinkedHashMap<String, List<RowData>>();
    for (RowData row : rows)
    {
      String key = lower(row.matchKey);
      List<RowData> grouped = result.get(key);
      if (grouped == null)
      {
        grouped = new ArrayList<RowData>();
        result.put(key, grouped);
      }
      grouped.add(row);
    }
    return result;
  }

  private List<String> uniqueNonBlank(List<String> values)
  {
    LinkedHashSet<String> unique = new LinkedHashSet<String>();
    if (values != null)
    {
      for (String value : values)
      {
        String normalized = safe(value).trim();
        if (!normalized.isEmpty())
        {
          unique.add(normalized);
        }
      }
    }
    return new ArrayList<String>(unique);
  }

  private List<String> unique(List<String> values)
  {
    return new ArrayList<String>(new LinkedHashSet<String>(values));
  }

  private void bindStrings(PreparedStatement statement, List<String> values)
      throws SQLException
  {
    for (int index = 0; index < values.size(); index++)
    {
      statement.setString(index + 1, values.get(index));
    }
  }

  private String placeholders(int count)
  {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < count; index++)
    {
      if (index > 0)
      {
        result.append(',');
      }
      result.append('?');
    }
    return result.toString();
  }

  private String longList(List<Long> values)
  {
    StringBuilder result = new StringBuilder();
    for (int index = 0; index < values.size(); index++)
    {
      if (index > 0)
      {
        result.append(',');
      }
      result.append(values.get(index).longValue());
    }
    return result.toString();
  }

  private String truncate(String value)
  {
    String safeValue = safe(value);
    return safeValue.length() <= EXCEL_CELL_LIMIT
        ? safeValue
        : safeValue.substring(0, EXCEL_CELL_LIMIT);
  }

  private String oneLine(String value)
  {
    return safe(value).replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
  }

  private String firstNotEmpty(String... values)
  {
    for (String value : values)
    {
      if (value != null && !value.trim().isEmpty())
      {
        return value;
      }
    }
    return "";
  }

  private long exactlyOne(Map<String, List<Long>> values, String identifier)
  {
    List<Long> ids = values.get(lower(identifier));
    if (ids == null || ids.isEmpty())
    {
      throw new IllegalArgumentException(
          "No se encontró la estructura activa: " + identifier
      );
    }
    if (ids.size() != 1)
    {
      throw new IllegalArgumentException(
          "Se encontraron varias estructuras activas para: " + identifier
      );
    }
    return ids.get(0).longValue();
  }

  private String join(List<String> values, String separator)
  {
    StringBuilder result = new StringBuilder();
    for (String value : values)
    {
      if (result.length() > 0)
      {
        result.append(separator);
      }
      result.append(value);
    }
    return result.toString();
  }

  private void notify(ProgressListener listener, String phase)
  {
    if (listener != null)
    {
      listener.phase(phase);
    }
  }

  private static String lower(String value)
  {
    return safe(value).trim().toLowerCase(Locale.ROOT);
  }

  private static String safe(String value)
  {
    return value == null ? "" : value;
  }

  interface ProgressListener
  {
    void phase(String phase);
  }

  static final class ExportResult
  {
    final Path zipFile;
    final String filename;
    final long productRows;
    final long notFoundRows;

    ExportResult(
        Path zipFile,
        String filename,
        long productRows,
        long notFoundRows)
    {
      this.zipFile = zipFile;
      this.filename = filename;
      this.productRows = productRows;
      this.notFoundRows = notFoundRows;
    }
  }

  private static final class ReportColumn
  {
    private final String header;
    private final List<String> sources;

    private ReportColumn(String header, String... sources)
    {
      this.header = header;
      this.sources = Arrays.asList(sources);
    }
  }

  private static final class StructureIds
  {
    private final long template;
    private final long catIds;

    private StructureIds(long template, long catIds)
    {
      this.template = template;
      this.catIds = catIds;
    }
  }

  private static final class CharMeta
  {
    private final long id;
    private final String identifier;
    private final boolean lookup;

    private CharMeta(long id, String identifier, boolean lookup)
    {
      this.id = id;
      this.identifier = identifier;
      this.lookup = lookup;
    }
  }

  private static final class RowData
  {
    private final long revisionId;
    private String matchKey = "";
    private final Map<String, ValueSet> values =
        new LinkedHashMap<String, ValueSet>();

    private RowData(long revisionId)
    {
      this.revisionId = revisionId;
    }

    private void put(String column, String value)
    {
      ValueSet set = new ValueSet();
      set.add(value);
      this.values.put(lower(column), set);
    }

    private void add(String column, String value)
    {
      String key = lower(column);
      ValueSet set = this.values.get(key);
      if (set == null)
      {
        set = new ValueSet();
        this.values.put(key, set);
      }
      set.add(value);
    }

    private String value(String column)
    {
      ValueSet set = this.values.get(lower(column));
      return set == null ? "" : set.joined();
    }
  }

  private static final class ValueSet
  {
    private final LinkedHashSet<String> values = new LinkedHashSet<String>();

    private void add(String value)
    {
      if (value != null && !value.trim().isEmpty())
      {
        this.values.add(value.trim());
      }
    }

    private String joined()
    {
      StringBuilder result = new StringBuilder();
      for (String value : this.values)
      {
        if (result.length() > 0)
        {
          result.append(MULTI_VALUE_SEPARATOR);
        }
        if (result.length() + value.length() > EXCEL_CELL_LIMIT)
        {
          int available = EXCEL_CELL_LIMIT - result.length();
          if (available > 0)
          {
            result.append(value.substring(0, Math.min(available, value.length())));
          }
          break;
        }
        result.append(value);
      }
      return result.toString();
    }
  }

  private static final class Selection
  {
    private final boolean temporary;

    private Selection(boolean temporary)
    {
      this.temporary = temporary;
    }
  }

  private static final class OutputCounts
  {
    private final long productRows;
    private final long notFoundRows;

    private OutputCounts(long productRows, long notFoundRows)
    {
      this.productRows = productRows;
      this.notFoundRows = notFoundRows;
    }
  }
}
