package com.example.ei.forfun.logic;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class P360SplitMacroPlanBuilder {

    private static final String ACTIVE_TS = "TIMESTAMP '9999-12-31 00:00:00.0'";
    private static final String SCHEMA_MASTER = "PIM_MASTER";
    private static final String SCHEMA_MAIN = "PIM_MAIN";

    public enum Level {
        PRODUCT,
        ARTICLE
    }

    public enum Kind {
        DETAIL,
        LANG,
        DOMAIN,
        DOMAIN_LOOKUP_CODE,
        DOMAIN_LOOKUP_LANG,
        STRUCTURE,
        CHARACTERISTIC,
        CURRENT_STATUS_LABEL
    }

    public enum SortPreference {
        AUTO,
        FORCE_DB_ORDER_BY,
        FORCE_EXTERNAL_SORT
    }

    public enum SortMode {
        DB_ORDER_BY,
        EXTERNAL_SORT,
        NONE
    }

    public enum StageType {
        PRODUCT_BASE_KEYS,
        ARTICLE_REFERENCE_MAP,
        ARTICLE_BASE_KEYS,
        PRODUCT_ENRICHMENT,
        ARTICLE_ENRICHMENT
    }

    public static class RequestedColumn {
        Level level;
        Kind kind;
        String name;
        String outputName;
        String field;
        Integer languageId;
        Integer structureId;
    }

    public static class CharacteristicMeta {
        String characteristicIdentifier;
        Integer characteristicId;
        String dataType;
        Integer lookupId;
        String lookupIdentifier;
    }

    public static class PlanStage {
        private final StageType stageType;
        private final String stageName;
        private final String outputFileName;
        private final List<String> keyColumns;
        private final SortMode sortMode;
        private final String sql;

        public PlanStage(StageType stageType,
                         String stageName,
                         String outputFileName,
                         List<String> keyColumns,
                         SortMode sortMode,
                         String sql) {
            this.stageType = stageType;
            this.stageName = stageName;
            this.outputFileName = outputFileName;
            this.keyColumns = List.copyOf(keyColumns);
            this.sortMode = sortMode;
            this.sql = sql;
        }

        public StageType getStageType() {
            return stageType;
        }

        public String getStageName() {
            return stageName;
        }

        public String getOutputFileName() {
            return outputFileName;
        }

        public List<String> getKeyColumns() {
            return keyColumns;
        }

        public SortMode getSortMode() {
            return sortMode;
        }

        public String getSql() {
            return sql;
        }
    }

    public static class MacroBuildPlan {
        private final List<PlanStage> stages;

        public MacroBuildPlan(List<PlanStage> stages) {
            this.stages = List.copyOf(stages);
        }

        public List<PlanStage> getStages() {
            return stages;
        }
    }

    public MacroBuildPlan buildPlan(Path requestedColumnsCsv,
                                    Path characteristicsCsv,
                                    String productWhereClause,
                                    SortPreference sortPreference) throws IOException {
        List<RequestedColumn> requested = loadRequestedColumns(requestedColumnsCsv);
        Map<String, CharacteristicMeta> characteristicMap = loadCharacteristics(characteristicsCsv);

        List<RequestedColumn> productColumns = new ArrayList<>();
        List<RequestedColumn> articleColumns = new ArrayList<>();

        for (RequestedColumn rc : requested) {
            if (rc.level == Level.PRODUCT) {
                productColumns.add(rc);
            } else {
                articleColumns.add(rc);
            }
        }

        List<PlanStage> stages = new ArrayList<>();

        stages.add(buildProductBaseKeysStage(productWhereClause, sortPreference));
        stages.add(buildArticleReferenceMapStage(sortPreference));
        stages.add(buildArticleBaseKeysStage(sortPreference));

        stages.addAll(buildEnrichmentStages(Level.PRODUCT, productColumns, characteristicMap, sortPreference));
        stages.addAll(buildEnrichmentStages(Level.ARTICLE, articleColumns, characteristicMap, sortPreference));

        return new MacroBuildPlan(stages);
    }

    private PlanStage buildProductBaseKeysStage(String productWhereClause, SortPreference sortPreference) {
        StringBuilder sql = new StringBuilder();
        sql.append("select\n");
        sql.append("      aa.ID \"ProductID\"\n");
        sql.append("    , aa.\"Identifier\" \"ProductIdentifier\"\n");
        sql.append("from\n");
        sql.append("    \"ArticleRevision\" aa\n");
        sql.append("inner join\n");
        sql.append("    \"ArticleDetail\" bb\n");
        sql.append("on\n");
        sql.append("    aa.ID = bb.\"ArticleRevisionID\" and aa.\"DeletionTimestamp\" = ").append(ACTIVE_TS)
            .append(" and bb.\"DeletionTimestamp\" = ").append(ACTIVE_TS)
            .append(" and aa.\"EntityID\" = 1100 and aa.\"RevisionID\" = 1\n");

        if (productWhereClause != null && !productWhereClause.isBlank()) {
            sql.append("where\n");
            sql.append("    ").append(productWhereClause).append("\n");
        }

        SortMode sortMode = decideBaseSortMode(sortPreference);
        if (sortMode == SortMode.DB_ORDER_BY) {
            sql.append("order by\n");
            sql.append("    aa.\"Identifier\", aa.ID");
        }

        return new PlanStage(
            StageType.PRODUCT_BASE_KEYS,
            "PRODUCT_BASE_KEYS",
            "01_product_base_keys.csv",
            List.of("ProductIdentifier", "ProductID"),
            sortMode,
            sql.toString()
        );
    }

    private PlanStage buildArticleReferenceMapStage(SortPreference sortPreference) {
        StringBuilder sql = new StringBuilder();
        sql.append("select\n");
        sql.append("      ar.\"RefExtArtIdentifier\" \"ProductIdentifier\"\n");
        sql.append("    , ar.\"ArticleRevisionID\" \"ArticleID\"\n");
        sql.append("from\n");
        sql.append("    \"ArticleReference\" ar\n");
        sql.append("where\n");
        sql.append("    ar.\"DeletionTimestamp\" = ").append(ACTIVE_TS).append("\n");

        SortMode sortMode = decideBaseSortMode(sortPreference);
        if (sortMode == SortMode.DB_ORDER_BY) {
            sql.append("order by\n");
            sql.append("    ar.\"RefExtArtIdentifier\", ar.\"ArticleRevisionID\"");
        }

        return new PlanStage(
            StageType.ARTICLE_REFERENCE_MAP,
            "ARTICLE_REFERENCE_MAP",
            "02_article_reference_map.csv",
            List.of("ProductIdentifier", "ArticleID"),
            sortMode,
            sql.toString()
        );
    }

    private PlanStage buildArticleBaseKeysStage(SortPreference sortPreference) {
        StringBuilder sql = new StringBuilder();
        sql.append("select\n");
        sql.append("      aa.ID \"ArticleID\"\n");
        sql.append("    , aa.\"Identifier\" \"ArticleIdentifier\"\n");
        sql.append("from\n");
        sql.append("    \"ArticleRevision\" aa\n");
        sql.append("inner join\n");
        sql.append("    \"ArticleDetail\" bb\n");
        sql.append("on\n");
        sql.append("    aa.ID = bb.\"ArticleRevisionID\" and aa.\"DeletionTimestamp\" = ").append(ACTIVE_TS)
            .append(" and bb.\"DeletionTimestamp\" = ").append(ACTIVE_TS)
            .append(" and aa.\"EntityID\" = 1000 and aa.\"RevisionID\" = 1\n");

        SortMode sortMode = decideBaseSortMode(sortPreference);
        if (sortMode == SortMode.DB_ORDER_BY) {
            sql.append("order by\n");
            sql.append("    aa.ID, aa.\"Identifier\"");
        }

        return new PlanStage(
            StageType.ARTICLE_BASE_KEYS,
            "ARTICLE_BASE_KEYS",
            "03_article_base_keys.csv",
            List.of("ArticleID", "ArticleIdentifier"),
            sortMode,
            sql.toString()
        );
    }

    private List<PlanStage> buildEnrichmentStages(Level level,
                                                  List<RequestedColumn> columns,
                                                  Map<String, CharacteristicMeta> characteristicMap,
                                                  SortPreference sortPreference) {
        List<PlanStage> stages = new ArrayList<>();
        Map<String, List<RequestedColumn>> groups = groupColumns(level, columns, characteristicMap);

        for (Map.Entry<String, List<RequestedColumn>> entry : groups.entrySet()) {
            String groupKey = entry.getKey();
            List<RequestedColumn> groupColumns = entry.getValue();

            String sql = buildEnrichmentSql(level, groupColumns, characteristicMap, sortPreference);
            String prefix = level == Level.PRODUCT ? "P" : "A";
            StageType stageType = level == Level.PRODUCT ? StageType.PRODUCT_ENRICHMENT : StageType.ARTICLE_ENRICHMENT;
            String outputFile = prefix + "_" + sanitizeFilePart(groupKey) + ".csv";
            List<String> keys = level == Level.PRODUCT
                ? List.of("ProductID")
                : List.of("ArticleID");

            stages.add(new PlanStage(
                stageType,
                level.name() + "_" + groupKey,
                outputFile,
                keys,
                decideEnrichmentSortMode(groupColumns, sortPreference),
                sql
            ));
        }

        return stages;
    }

    private Map<String, List<RequestedColumn>> groupColumns(Level level,
                                                            List<RequestedColumn> columns,
                                                            Map<String, CharacteristicMeta> characteristicMap) {
        Map<String, List<RequestedColumn>> groups = new LinkedHashMap<>();

        for (RequestedColumn col : columns) {
            String key = buildGroupKey(level, col, characteristicMap);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(col);
        }

        return groups;
    }

    private String buildGroupKey(Level level,
                                 RequestedColumn col,
                                 Map<String, CharacteristicMeta> characteristicMap) {
        return switch (col.kind) {
            case DETAIL, CURRENT_STATUS_LABEL -> "DETAIL_CORE";
            case LANG -> "LANG_" + nullSafeInt(col.languageId, 10);
            case DOMAIN, DOMAIN_LOOKUP_CODE, DOMAIN_LOOKUP_LANG -> "DOMAIN";
            case STRUCTURE -> "STRUCTURE_" + nullSafeInt(col.structureId, 12000);
            case CHARACTERISTIC -> {
                CharacteristicMeta meta = characteristicMap.get(col.name);
                if (meta == null || meta.characteristicId == null) {
                    throw new IllegalArgumentException("No encontré CharacteristicID para: " + col.name);
                }
                yield "CHAR_" + meta.characteristicId;
            }
        };
    }

    private String buildEnrichmentSql(Level level,
                                      List<RequestedColumn> columns,
                                      Map<String, CharacteristicMeta> characteristicMap,
                                      SortPreference sortPreference) {
        String arAlias = "aa";
        String adAlias = "bb";
        int entityId = level == Level.PRODUCT ? 1100 : 1000;

        List<String> selects = new ArrayList<>();
        List<String> joins = new ArrayList<>();
        Set<String> joinKeys = new HashSet<>();

        selects.add(level == Level.PRODUCT
            ? arAlias + ".ID \"ProductID\""
            : arAlias + ".ID \"ArticleID\"");

        joins.add("inner join\n" +
            "    \"ArticleDetail\" " + adAlias + "\n" +
            "on\n" +
            "    " + arAlias + ".ID = " + adAlias + ".\"ArticleRevisionID\" and " +
            arAlias + ".\"DeletionTimestamp\" = " + ACTIVE_TS + " and " +
            adAlias + ".\"DeletionTimestamp\" = " + ACTIVE_TS + " and " +
            arAlias + ".\"EntityID\" = " + entityId + " and " +
            arAlias + ".\"RevisionID\" = 1");

        int langCounter = 0;
        int lookupCounter = 0;
        int structureCounter = 0;
        int charCounter = 0;

        for (RequestedColumn col : columns) {
            switch (col.kind) {
                case DETAIL -> selects.add(adAlias + ".\"" + col.field + "\" \"" + col.outputName + "\"");

                case CURRENT_STATUS_LABEL ->
                    selects.add(buildCurrentStatusCase(adAlias + ".\"CurrentStatus\"", col.outputName));

                case LANG -> {
                    String alias = "alang" + langCounter++;
                    joins.add("left outer join\n" +
                        "    \"ArticleLang\" " + alias + "\n" +
                        "on\n" +
                        "    " + arAlias + ".ID = " + alias + ".\"ArticleRevisionID\" and " +
                        alias + ".\"DeletionTimestamp\" = " + ACTIVE_TS + " and " +
                        alias + ".\"LanguageID\" = " + nullSafeInt(col.languageId, 10));
                    selects.add(alias + ".\"" + col.field + "\" \"" + col.outputName + "\"");
                }

                case DOMAIN -> {
                    ensureDomainJoin(joins, joinKeys, arAlias, level);
                    selects.add("dd.\"" + col.field + "\" \"" + col.outputName + "\"");
                }

                case DOMAIN_LOOKUP_CODE -> {
                    ensureDomainJoin(joins, joinKeys, arAlias, level);
                    String lvr = "lvr" + lookupCounter++;
                    joins.add("left outer join\n" +
                        "    " + SCHEMA_MAIN + ".\"LookupValueRevision\" " + lvr + "\n" +
                        "on\n" +
                        "    " + lvr + ".\"LookupValueID\" = dd.\"" + col.field + "\" and " +
                        lvr + ".\"LookupID\" = " + resolveDomainLookupId(col.field, level) + " and " +
                        lvr + ".\"RevisionID\" = 1 and " +
                        lvr + ".\"DeletionTimestamp\" = " + ACTIVE_TS);
                    selects.add(lvr + ".\"Code\" \"" + col.outputName + "\"");
                }

                case DOMAIN_LOOKUP_LANG -> {
                    ensureDomainJoin(joins, joinKeys, arAlias, level);
                    String lvr = "lvr" + lookupCounter;
                    String lvl = "lvl" + lookupCounter;
                    lookupCounter++;

                    joins.add("left outer join\n" +
                        "    " + SCHEMA_MAIN + ".\"LookupValueRevision\" " + lvr + "\n" +
                        "on\n" +
                        "    " + lvr + ".\"LookupValueID\" = dd.\"" + col.field + "\" and " +
                        lvr + ".\"LookupID\" = " + resolveDomainLookupId(col.field, level) + " and " +
                        lvr + ".\"RevisionID\" = 1 and " +
                        lvr + ".\"DeletionTimestamp\" = " + ACTIVE_TS);

                    joins.add("left outer join\n" +
                        "    " + SCHEMA_MAIN + ".\"LookupValueLang\" " + lvl + "\n" +
                        "on\n" +
                        "    " + lvl + ".\"LookupValueRevisionID\" = " + lvr + ".\"ID\" and " +
                        lvl + ".\"DeletionTimestamp\" = " + ACTIVE_TS + " and " +
                        lvl + ".\"LanguageID\" = " + nullSafeInt(col.languageId, 10));

                    selects.add(lvl + ".\"Name\" \"" + col.outputName + "\"");
                }

                case STRUCTURE -> {
                    String alias = "sw" + structureCounter++;
                    joins.add("left outer join\n" +
                        "(\n" +
                        "    select\n" +
                        "          asm.\"ArticleRevisionID\"\n" +
                        "        , LISTAGG(asm.\"StructureGroupIdentifier\", '; ') WITHIN GROUP (ORDER BY asm.\"StructureGroupIdentifier\") \"" + col.outputName + "\"\n" +
                        "    from\n" +
                        "    (\n" +
                        "        select distinct\n" +
                        "              asm.\"ArticleRevisionID\"\n" +
                        "            , asm.\"StructureGroupIdentifier\"\n" +
                        "        from\n" +
                        "            " + SCHEMA_MASTER + ".\"ArticleStructureMap\" asm\n" +
                        "        where\n" +
                        "            asm.\"DeletionTimestamp\" = " + ACTIVE_TS + "\n" +
                        "            and asm.\"StructureID\" = " + nullSafeInt(col.structureId, 12000) + "\n" +
                        "    ) asm\n" +
                        "    group by\n" +
                        "        asm.\"ArticleRevisionID\"\n" +
                        ") " + alias + "\n" +
                        "on\n" +
                        "    " + arAlias + ".ID = " + alias + ".\"ArticleRevisionID\"");
                    selects.add(alias + ".\"" + col.outputName + "\"");
                }

                case CHARACTERISTIC -> {
                    CharacteristicMeta meta = characteristicMap.get(col.name);
                    if (meta == null || meta.characteristicId == null) {
                        throw new IllegalArgumentException("No encontré metadata para la característica: " + col.name);
                    }

                    String acv = "acv" + charCounter;
                    joins.add("left outer join\n" +
                        "    \"ArticleCharactValue\" " + acv + "\n" +
                        "on\n" +
                        "    " + arAlias + ".ID = " + acv + ".\"ArticleRevisionID\" and " +
                        acv + ".\"CharacteristicID\" = " + meta.characteristicId + " and " +
                        acv + ".\"DeletionTimestamp\" = " + ACTIVE_TS);

                    if (isLookup(meta)) {
                        String lvr = "acv_lvr" + charCounter;
                        String lvl = "acv_lvl" + charCounter;

                        joins.add("left outer join\n" +
                            "    " + SCHEMA_MAIN + ".\"LookupValueRevision\" " + lvr + "\n" +
                            "on\n" +
                            "    " + lvr + ".\"LookupValueID\" = " + acv + ".\"LookupValueID\" and " +
                            lvr + ".\"LookupID\" = " + meta.lookupId + " and " +
                            lvr + ".\"RevisionID\" = 1 and " +
                            lvr + ".\"DeletionTimestamp\" = " + ACTIVE_TS);

                        joins.add("left outer join\n" +
                            "    " + SCHEMA_MAIN + ".\"LookupValueLang\" " + lvl + "\n" +
                            "on\n" +
                            "    " + lvl + ".\"LookupValueRevisionID\" = " + lvr + ".\"ID\" and " +
                            lvl + ".\"DeletionTimestamp\" = " + ACTIVE_TS + " and " +
                            lvl + ".\"LanguageID\" = " + nullSafeInt(col.languageId, 10));

                        selects.add(lvl + ".\"Name\" \"" + col.outputName + "\"");
                    } else {
                        selects.add(acv + ".\"Value\" \"" + col.outputName + "\"");
                    }

                    charCounter++;
                }
            }
        }

        StringBuilder sql = new StringBuilder();
        sql.append("select\n");
        sql.append("      ").append(String.join("\n    , ", selects)).append("\n");
        sql.append("from\n");
        sql.append("    \"ArticleRevision\" ").append(arAlias).append("\n");

        for (String join : joins) {
            sql.append(join).append("\n");
        }

        SortMode sortMode = decideEnrichmentSortMode(columns, sortPreference);
        if (sortMode == SortMode.DB_ORDER_BY) {
            sql.append("order by\n");
            sql.append("    ").append(arAlias).append(".ID");
        }

        return sql.toString().trim();
    }

    private SortMode decideBaseSortMode(SortPreference sortPreference) {
        return switch (sortPreference) {
            case FORCE_DB_ORDER_BY -> SortMode.DB_ORDER_BY;
            case FORCE_EXTERNAL_SORT -> SortMode.EXTERNAL_SORT;
            case AUTO -> SortMode.DB_ORDER_BY;
        };
    }

    private SortMode decideEnrichmentSortMode(List<RequestedColumn> columns, SortPreference sortPreference) {
        if (sortPreference == SortPreference.FORCE_DB_ORDER_BY) {
            return SortMode.DB_ORDER_BY;
        }
        if (sortPreference == SortPreference.FORCE_EXTERNAL_SORT) {
            return SortMode.EXTERNAL_SORT;
        }

        boolean heavy = false;
        for (RequestedColumn col : columns) {
            if (col.kind == Kind.CHARACTERISTIC || col.kind == Kind.STRUCTURE) {
                heavy = true;
                break;
            }
        }

        return heavy ? SortMode.EXTERNAL_SORT : SortMode.DB_ORDER_BY;
    }

    private void ensureDomainJoin(List<String> joins, Set<String> joinKeys, String arAlias, Level level) {
        if (joinKeys.add("DOMAIN")) {
            joins.add("left outer join\n" +
                "    \"ArticleDomain\" dd\n" +
                "on\n" +
                "    " + arAlias + ".ID = dd.\"ArticleRevisionID\" and dd.\"EntityID\" = " +
                (level == Level.PRODUCT ? "21006" : "21106"));
        }
    }

    private boolean isLookup(CharacteristicMeta meta) {
        return meta.dataType != null
            && meta.dataType.toUpperCase(Locale.ROOT).contains("LOOKUP")
            && meta.lookupId != null;
    }

    private int resolveDomainLookupId(String field, Level level) {
        if (level == Level.PRODUCT) {
            return switch (field) {
                case "Res_Int_01" -> 2036;
                case "Res_Int_02" -> 2229;
                case "Res_Int_03" -> 2023;
                case "Res_Int_04" -> 2078;
                case "Res_Int_05" -> 2001;
                case "Res_Int_06" -> 2138;
                case "Res_Int_07" -> 2006;
                case "Res_Int_08" -> 2066;
                case "Std_Int_10" -> 4545;
                default -> throw new IllegalArgumentException("No tengo mapeado LookupID para ArticleDomain field: " + field);
            };
        } else {
            return switch (field) {
                case "Res_Int_01" -> 2035;
                case "Res_Int_02" -> 2103;
                case "Res_Int_03" -> 2066;
                default -> throw new IllegalArgumentException("No tengo mapeado LookupID para ArticleDomain field: " + field);
            };
        }
    }

    private String buildCurrentStatusCase(String expr, String outputName) {
        return "CASE " + expr +
            " WHEN 1001 THEN 'Propuesta Generada'" +
            " WHEN 1002 THEN 'Pendiente Inicio Enriquecimiento'" +
            " WHEN 1003 THEN 'Revisión Compras'" +
            " WHEN 1004 THEN 'Carga de Imagen'" +
            " WHEN 1005 THEN 'Rechazada'" +
            " WHEN 1006 THEN 'Por Actualizar'" +
            " WHEN 1007 THEN 'Aprobada'" +
            " WHEN 1008 THEN 'Modificación'" +
            " WHEN 1009 THEN 'Cancelado'" +
            " WHEN 1010 THEN 'En Proceso Liverpool'" +
            " WHEN 1011 THEN 'En Proceso de Envío'" +
            " WHEN 1020 THEN 'Creación de SKU'" +
            " WHEN 1021 THEN 'Gobierno de Datos'" +
            " WHEN 1022 THEN 'Revisión QA'" +
            " WHEN 1023 THEN 'Category'" +
            " WHEN 1024 THEN 'Rechazo Publicación'" +
            " WHEN 1025 THEN 'Eliminada'" +
            " WHEN 1026 THEN 'En Proceso Foro'" +
            " WHEN 10031 THEN 'Borrador'" +
            " WHEN 1027 THEN 'Rechazo Compras'" +
            " WHEN 1028 THEN 'Rechazo QA'" +
            " WHEN 1029 THEN 'Rechazo Gobierno'" +
            " WHEN 1030 THEN 'Rechazo Category'" +
            " WHEN 1031 THEN 'Repoblamiento'" +
            " WHEN 1032 THEN 'Excepción de Catalogación'" +
            " ELSE 'Desconocido' END \"" + outputName + "\"";
    }

    private List<RequestedColumn> loadRequestedColumns(Path csv) throws IOException {
        List<RequestedColumn> out = new ArrayList<>();
        final String[][] headerRef = {null};

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
            '"', ',', '\\', "\n", StandardCharsets.UTF_8,
            values -> {
                if (values == null || values.length == 0) {
                    return;
                }
                if (headerRef[0] == null) {
                    headerRef[0] = values;
                    return;
                }

                Map<String, String> row = mapRow(headerRef[0], values);
                RequestedColumn rc = new RequestedColumn();
                rc.level = Level.valueOf(row.getOrDefault("Level", "").trim().toUpperCase(Locale.ROOT));
                rc.kind = Kind.valueOf(row.getOrDefault("Kind", "").trim().toUpperCase(Locale.ROOT));
                rc.name = trimToNull(row.get("Name"));
                rc.outputName = row.getOrDefault("OutputName", "").trim();
                rc.field = trimToNull(row.get("Field"));
                rc.languageId = parseInteger(row.get("LanguageID"));
                rc.structureId = parseInteger(row.get("StructureID"));
                out.add(rc);
            }
        );

        parser.parse(csv);
        return out;
    }

    private Map<String, CharacteristicMeta> loadCharacteristics(Path csv) throws IOException {
        Map<String, CharacteristicMeta> out = new HashMap<>();
        final String[][] headerRef = {null};

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
            '"', ',', '\\', "\n", StandardCharsets.UTF_8,
            values -> {
                if (values == null || values.length == 0) {
                    return;
                }
                if (headerRef[0] == null) {
                    headerRef[0] = values;
                    return;
                }

                Map<String, String> row = mapRow(headerRef[0], values);
                CharacteristicMeta cm = new CharacteristicMeta();
                cm.characteristicIdentifier = trimToNull(firstNonBlank(
                    row.get("CharacteristicIdentifier"),
                    row.get("Identifier"),
                    row.get("Name")
                ));
                cm.characteristicId = parseInteger(firstNonBlank(
                    row.get("CharacteristicID"),
                    row.get("ID")
                ));
                cm.dataType = trimToNull(firstNonBlank(
                    row.get("CharacteristicDataType"),
                    row.get("DataType")
                ));
                cm.lookupId = parseInteger(row.get("LookupID"));
                cm.lookupIdentifier = trimToNull(row.get("LookupIdentifier"));

                if (cm.characteristicIdentifier != null) {
                    out.put(cm.characteristicIdentifier, cm);
                }
            }
        );

        parser.parse(csv);
        return out;
    }

    private Map<String, String> mapRow(String[] headers, String[] values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            row.put(headers[i], i < values.length ? values[i] : "");
        }
        return row;
    }

    private Integer parseInteger(String s) {
        String t = trimToNull(s);
        return t == null ? null : Integer.parseInt(t);
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private int nullSafeInt(Integer v, int defaultValue) {
        return v == null ? defaultValue : v;
    }

    private String sanitizeFilePart(String s) {
        return s.replaceAll("[^a-zA-Z0-9_\\-]+", "_").toLowerCase(Locale.ROOT);
    }
}