package com.example.ei.forfun.logic;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class P360DynamicQueryBuilder {

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

    public String buildQuery(Path requestedColumnsCsv, Path characteristicsCsv, String productWhereClause) throws IOException {
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

        String productSubquery = buildEntitySubquery(Level.PRODUCT, productColumns, characteristicMap, productWhereClause);
        String articleSubquery = buildEntitySubquery(Level.ARTICLE, articleColumns, characteristicMap, null);

        StringBuilder sql = new StringBuilder();
        sql.append("select\n");
        sql.append("      prod.*\n");
        sql.append("    , art.*\n");
        sql.append("from\n");
        sql.append("(\n");
        sql.append(indent(productSubquery, 1)).append("\n");
        sql.append(") prod\n");
        sql.append("inner join\n");
        sql.append("    \"ArticleReference\" ar\n");
        sql.append("on\n");
        sql.append("    prod.\"Identifier\" = ar.\"RefExtArtIdentifier\" and ar.\"DeletionTimestamp\" = ").append(ACTIVE_TS).append("\n");
        sql.append("inner join\n");
        sql.append("(\n");
        sql.append(indent(articleSubquery, 1)).append("\n");
        sql.append(") art\n");
        sql.append("on\n");
        sql.append("    ar.\"ArticleRevisionID\" = art.ID");

        return sql.toString();
    }

    private String buildEntitySubquery(Level level,
                                       List<RequestedColumn> columns,
                                       Map<String, CharacteristicMeta> characteristicMap,
                                       String whereClause) {

        String arAlias = "aa";
        String adAlias = "bb";
        int entityId = level == Level.PRODUCT ? 1100 : 1000;

        List<String> selects = new ArrayList<>();
        List<String> joins = new ArrayList<>();
        Set<String> joinKeys = new HashSet<>();

        selects.add(arAlias + ".\"Identifier\"");
        selects.add(arAlias + ".ID");

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
                case DETAIL -> {
                    selects.add(adAlias + ".\"" + col.field + "\" \"" + col.outputName + "\"");
                }

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
                    if (meta == null) {
                        throw new IllegalArgumentException("No encontré metadata para la característica: " + col.name);
                    }
                    if (meta.characteristicId == null) {
                        throw new IllegalArgumentException("La característica no trae CharacteristicID: " + col.name);
                    }

                    String acv = "acv" + charCounter;
                    joins.add("left outer join\n" +
                            "    \"" + "ArticleCharactValue" + "\" " + acv + "\n" +
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

                case CURRENT_STATUS_LABEL -> selects.add(buildCurrentStatusCase(adAlias + ".\"CurrentStatus\"", col.outputName));
            }
        }

        StringBuilder sql = new StringBuilder();
        sql.append("select\n");
        sql.append("      ").append(String.join("\n    , ", selects)).append("\n");
        sql.append("FROM\n");
        sql.append("    \"ArticleRevision\" ").append(arAlias).append("\n");

        for (String join : joins) {
            sql.append(join).append("\n");
        }

        if (whereClause != null && !whereClause.isBlank()) {
            sql.append("where\n");
            sql.append("    ").append(whereClause).append("\n");
        }

        return sql.toString().trim();
    }

    private void ensureDomainJoin(List<String> joins, Set<String> joinKeys, String arAlias, Level level) {
        if (joinKeys.add("DOMAIN")) {
            joins.add("left outer join\n" +
                    "    \"ArticleDomain\" dd\n" +
                    "on\n" +
                    "    " + arAlias + ".ID = dd.\"ArticleRevisionID\" and dd.\"EntityID\" = " + (level.equals(Level.PRODUCT) ? "21006" : "21106" ));
        }
    }

    private boolean isLookup(CharacteristicMeta meta) {
        return meta.dataType != null &&
                meta.dataType.toUpperCase(Locale.ROOT).contains("LOOKUP") &&
                meta.lookupId != null;
    }

    private int resolveDomainLookupId(String field, Level type) {
    	if(Level.PRODUCT.equals(type)) {
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
    	}else if(Level.ARTICLE.equals(type)) {
    		return switch (field) {
	            case "Res_Int_01" -> 2035;
	            case "Res_Int_02" -> 2103;
	            case "Res_Int_03" -> 2066;
	            default -> throw new IllegalArgumentException("No tengo mapeado LookupID para ArticleDomain field: " + field);
	        };
    	}else {
    		throw new IllegalArgumentException("Tipo de producto desconocido: " + type);
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
                    if (values == null || values.length == 0) return;
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
                    if (values == null || values.length == 0) return;
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
                    cm.lookupId = parseInteger(firstNonBlank(
                            row.get("LookupID")
                    ));
                    cm.lookupIdentifier = trimToNull(firstNonBlank(
                            row.get("LookupIdentifier")
                    ));

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
        if (t == null) return null;
        return Integer.parseInt(t);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }

    private int nullSafeInt(Integer v, int defaultValue) {
        return v == null ? defaultValue : v;
    }

    private String indent(String s, int level) {
        String pad = "    ".repeat(level);
        String[] lines = s.split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append("\n");
            out.append(pad).append(lines[i]);
        }
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("Uso: P360DynamicQueryBuilder <RequestedColumns.csv> <CharacteristicsOfInterest.csv> [productWhereClause]");
        }

        Path requestedColumns = Path.of(args[0]);
        Path characteristics = Path.of(args[1]);
        String where = args.length >= 3 ? args[2] : "bb.\"Res_Int_01\" = 245869";

        P360DynamicQueryBuilder b = new P360DynamicQueryBuilder();
        String sql = b.buildQuery(requestedColumns, characteristics, where);
        System.out.println(sql);
    }
}