package com.example.ei.forfun.logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProfileProduct2GRefreshRunner {

    private static final String SCHEMA = "P360_EXPLOIT";

    public static void main(String[] args) throws Exception {
        boolean includeBase = true;
        boolean includeConfig = true;
        boolean includeDerived = true;
        boolean includeViews = true;

        for (String arg : args) {
            if ("--derived-only".equalsIgnoreCase(arg)) {
                includeBase = false;
                includeConfig = false;
                includeDerived = true;
            } else if ("--no-views".equalsIgnoreCase(arg)) {
                includeViews = false;
            } else if ("--base-only".equalsIgnoreCase(arg)) {
                includeBase = true;
                includeConfig = false;
                includeDerived = false;
            } else if ("--config-only".equalsIgnoreCase(arg)) {
                includeBase = false;
                includeConfig = true;
                includeDerived = false;
            }
        }

        String url = firstNonBlank(System.getenv("ORACLE_JDBC_URL"), System.getenv("P360_EXPLOIT_JDBC_URL"));
        String user = firstNonBlank(System.getenv("ORACLE_JDBC_USER"), System.getenv("P360_EXPLOIT_JDBC_USER"));
        String password = firstNonBlank(System.getenv("ORACLE_JDBC_PASSWORD"), System.getenv("P360_EXPLOIT_JDBC_PASSWORD"));

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Faltan ORACLE_JDBC_URL / ORACLE_JDBC_USER / ORACLE_JDBC_PASSWORD");
        }

        try (Connection con = DriverManager.getConnection(url, user, password)) {
            con.setAutoCommit(true);

            if (includeBase) {
                callProcedure(con, "RUN_PROFILE_PRODUCT2G_BASE");
                validateOneRowPerProduct(con, "TT_PROFILE_PRODUCT2G_BASE", "ArticleRevisionID");
                validateProductBase(con);
            }

            if (includeConfig) {
                callProcedure(con, "RUN_PROFILE_ATTR_CONFIG");
                validateAttrConfig(con);
            }

            if (includeDerived) {
                rebuildDerivedTables(con);
                callProcedure(con, "RUN_PROFILE_DEFINED_SUMMARY_BY_GROUP");
                validateDefinedSummary(con);
                rebuildFinalSummary(con);
                validateFinalSummary(con);
            }

            if (includeViews) {
                createViews(con);
                validateViews(con);
            }
        }
    }

    private static void rebuildDerivedTables(Connection con) throws SQLException {
        System.out.println("\n== Rebuilding derived tables ==");

        dropTableIfExists(con, "TT_PROFILE_PRODUCT_ATTR_SUMMARY");
        dropTableIfExists(con, "TT_PROFILE_PRODUCT_DEFINED_SUMMARY");
        dropTableIfExists(con, "TT_PROFILE_GROUP_REQ_SUMMARY");
        dropTableIfExists(con, "TT_PROFILE_GROUP_ATTR_REQ");
        dropTableIfExists(con, "TT_PROFILE_PRODUCT_GROUP_MAP");
        dropTableIfExists(con, "TT_PROFILE_PRODUCT2G_GROUPS");
        dropTableIfExists(con, "TT_PROFILE_REQ_ESTIMATE");
        dropTableIfExists(con, "TT_PROFILE_PRODUCT2G_BASE_GRP");

        execute(con, "CREATE TT_PROFILE_PRODUCT2G_BASE_GRP",
                "create table P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP nologging as " +
                "select \"Plantilla\", \"BusinessCode\", \"BusinessName\", count(*) \"ProductCount\" " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE " +
                "group by \"Plantilla\", \"BusinessCode\", \"BusinessName\"");
        createIndexIgnoreExists(con, "IX_TT_PROF_P2G_GRP_01", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GRP_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP(\"Plantilla\")");
        createIndexIgnoreExists(con, "IX_TT_PROF_P2G_GRP_02", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GRP_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP(\"BusinessCode\")");
        printQuery(con, "BASE_GRP", "select count(*) grupos, sum(\"ProductCount\") productos from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP");

        execute(con, "CREATE TT_PROFILE_PRODUCT2G_GROUPS",
                "create table P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS nologging as " +
                "select row_number() over (order by \"Plantilla\", \"BusinessCode\", \"BusinessName\") \"GroupID\", " +
                "\"Plantilla\", \"BusinessCode\", \"BusinessName\", \"ProductCount\" " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP");
        createIndexIgnoreExists(con, "IX_TT_PROF_P2G_GROUPS_01", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GROUPS_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS(\"GroupID\")");
        createIndexIgnoreExists(con, "IX_TT_PROF_P2G_GROUPS_02", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GROUPS_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS(\"Plantilla\",\"BusinessCode\")");

        execute(con, "CREATE TT_PROFILE_PRODUCT_GROUP_MAP",
                "create table P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP nologging as " +
                "select pb.\"ArticleRevisionID\", g.\"GroupID\" " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE pb " +
                "inner join P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS g " +
                "on nvl(pb.\"Plantilla\", chr(0)) = nvl(g.\"Plantilla\", chr(0)) " +
                "and nvl(pb.\"BusinessCode\", chr(0)) = nvl(g.\"BusinessCode\", chr(0)) " +
                "and nvl(pb.\"BusinessName\", chr(0)) = nvl(g.\"BusinessName\", chr(0))");
        createIndexIgnoreExists(con, "IX_TT_PROF_PROD_GRP_01", "create index P360_EXPLOIT.IX_TT_PROF_PROD_GRP_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP(\"ArticleRevisionID\")");
        createIndexIgnoreExists(con, "IX_TT_PROF_PROD_GRP_02", "create index P360_EXPLOIT.IX_TT_PROF_PROD_GRP_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP(\"GroupID\")");
        validateOneRowPerProduct(con, "TT_PROFILE_PRODUCT_GROUP_MAP", "ArticleRevisionID");

        execute(con, "CREATE TT_PROFILE_GROUP_ATTR_REQ",
                "create table P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ nologging as " +
                "with raw_req as (" +
                "select g.\"GroupID\", cfg.\"CharacteristicID\", cfg.\"CharacteristicIdentifier\", cfg.\"ConfigSource\", cfg.\"IsMandatory\" " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS g " +
                "inner join P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG cfg on cfg.\"ConfigSource\" = 'TEMPLATE' and cfg.\"Plantilla\" = g.\"Plantilla\" " +
                "where cfg.\"BusinessFilter\" is null or length(trim(cfg.\"BusinessFilter\")) = 0 " +
                "or (g.\"BusinessCode\" is not null and instr(upper(cfg.\"BusinessFilter\"), upper(g.\"BusinessCode\")) > 0) " +
                "or (g.\"BusinessName\" is not null and instr(upper(cfg.\"BusinessFilter\"), upper(g.\"BusinessName\")) > 0) " +
                "union all " +
                "select g.\"GroupID\", cfg.\"CharacteristicID\", cfg.\"CharacteristicIdentifier\", cfg.\"ConfigSource\", cfg.\"IsMandatory\" " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS g " +
                "inner join P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG cfg on cfg.\"ConfigSource\" = 'GLOBAL' " +
                "where cfg.\"BusinessFilter\" is null or length(trim(cfg.\"BusinessFilter\")) = 0 " +
                "or (g.\"BusinessCode\" is not null and instr(upper(cfg.\"BusinessFilter\"), upper(g.\"BusinessCode\")) > 0) " +
                "or (g.\"BusinessName\" is not null and instr(upper(cfg.\"BusinessFilter\"), upper(g.\"BusinessName\")) > 0)) " +
                "select \"GroupID\", \"CharacteristicID\", max(\"CharacteristicIdentifier\") \"CharacteristicIdentifier\", max(\"IsMandatory\") \"IsMandatory\", " +
                "case when max(case when \"ConfigSource\" = 'TEMPLATE' then 1 else 0 end) = 1 and max(case when \"ConfigSource\" = 'GLOBAL' then 1 else 0 end) = 1 then 'BOTH' " +
                "when max(case when \"ConfigSource\" = 'TEMPLATE' then 1 else 0 end) = 1 then 'TEMPLATE' else 'GLOBAL' end \"ConfigSourceResolved\" " +
                "from raw_req group by \"GroupID\", \"CharacteristicID\"");
        createIndexIgnoreExists(con, "IX_TT_PROF_GRP_REQ_01", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_01 on P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ(\"GroupID\",\"CharacteristicID\")");
        createIndexIgnoreExists(con, "IX_TT_PROF_GRP_REQ_02", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_02 on P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ(\"CharacteristicID\")");
        printQuery(con, "GROUP_ATTR_REQ", "select count(*) rows_count, count(distinct \"GroupID\") grupos, count(distinct \"CharacteristicID\") attrs, sum(case when \"IsMandatory\" = 1 then 1 else 0 end) mandatory_rows from P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ");

        execute(con, "CREATE TT_PROFILE_GROUP_REQ_SUMMARY",
                "create table P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY nologging as " +
                "select \"GroupID\", count(*) \"RequiredAttributeCount\", sum(case when \"IsMandatory\" = 1 then 1 else 0 end) \"RequiredMandatoryAttributeCount\" " +
                "from P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ group by \"GroupID\"");
        createIndexIgnoreExists(con, "IX_TT_PROF_GRP_REQ_SUM_01", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_SUM_01 on P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY(\"GroupID\")");
    }

    private static void rebuildFinalSummary(Connection con) throws SQLException {
        System.out.println("\n== Rebuilding final summary ==");
        dropTableIfExists(con, "TT_PROFILE_PRODUCT_ATTR_SUMMARY");

        execute(con, "CREATE TT_PROFILE_PRODUCT_ATTR_SUMMARY", "create table P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY nologging as " +
                "select pb.\"ArticleRevisionID\", pb.\"Product2GIdentifier\", pb.\"ProductSKU\", pb.\"EAN\", pb.\"Plantilla\", pb.\"VariantCount\", " +
                "pb.\"CurrentStatusID\", pb.\"CurrentStatusName\", pb.\"PrevStatusID\", pb.\"PrevStatusCode\", pb.\"PrevStatusName\", " +
                "pb.\"ExternalStatusID\", pb.\"ExternalStatusCode\", pb.\"ExternalStatusName\", pb.\"FirstDateApproved\", pb.\"LastDateApproved\", " +
                "pb.\"BusinessID\", pb.\"BusinessCode\", pb.\"BusinessName\", pb.\"ArticleDomainRows\", " +
                "pb.\"DireccionID\", pb.\"DireccionCode\", pb.\"DireccionName\", pb.\"SectionID\", pb.\"SectionCode\", pb.\"SectionName\", " +
                "pb.\"ItemGroupID\", pb.\"ItemGroupCode\", pb.\"ItemGroupName\", pb.\"ItemGroupS4HID\", pb.\"ItemGroupS4HCode\", pb.\"ItemGroupS4HName\", " +
                "pb.\"BrandNameID\", pb.\"BrandNameCode\", pb.\"BrandNameName\", pb.\"BRAND_ID_S4HID\", pb.\"BRAND_ID_S4HCode\", pb.\"BRAND_ID_S4HName\", " +
                "pb.\"NegocioID\", pb.\"NegocioCode\", pb.\"NegocioName\", pb.\"SAPObjectTypeID\", pb.\"SAPObjectTypeCode\", pb.\"SAPObjectTypeName\", " +
                "pb.\"SupplierID\", pb.\"SupplierPartNumber\", pgm.\"GroupID\", " +
                "nvl(grs.\"RequiredAttributeCount\",0) \"RequiredAttributeCount\", nvl(grs.\"RequiredMandatoryAttributeCount\",0) \"RequiredMandatoryAttributeCount\", " +
                "nvl(def.\"DefinedAttributeCount\",0) \"DefinedAttributeCount\", nvl(def.\"DefinedMandatoryAttributeCount\",0) \"DefinedMandatoryAttributeCount\", " +
                "nvl(grs.\"RequiredAttributeCount\",0) - nvl(def.\"DefinedAttributeCount\",0) \"MissingAttributeCount\", " +
                "nvl(grs.\"RequiredMandatoryAttributeCount\",0) - nvl(def.\"DefinedMandatoryAttributeCount\",0) \"MissingMandatoryAttributeCount\", " +
                "round(100 * nvl(def.\"DefinedAttributeCount\",0) / nullif(nvl(grs.\"RequiredAttributeCount\",0),0), 2) \"DefinedAttributePct\", " +
                "round(100 * nvl(def.\"DefinedMandatoryAttributeCount\",0) / nullif(nvl(grs.\"RequiredMandatoryAttributeCount\",0),0), 2) \"DefinedMandatoryAttributePct\" " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE pb " +
                "inner join P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP pgm on pgm.\"ArticleRevisionID\" = pb.\"ArticleRevisionID\" " +
                "left join P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY grs on grs.\"GroupID\" = pgm.\"GroupID\" " +
                "left join P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY def on def.\"ArticleRevisionID\" = pb.\"ArticleRevisionID\"");

        createIndexIgnoreExists(con, "IX_TT_PROF_ATTR_SUM_01", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"ArticleRevisionID\")");
        createIndexIgnoreExists(con, "IX_TT_PROF_ATTR_SUM_02", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"Product2GIdentifier\")");
        createIndexIgnoreExists(con, "IX_TT_PROF_ATTR_SUM_03", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_03 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"CurrentStatusID\")");
        createIndexIgnoreExists(con, "IX_TT_PROF_ATTR_SUM_04", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_04 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"MissingMandatoryAttributeCount\")");
    }

    private static void createViews(Connection con) throws SQLException {
        execute(con, "CREATE VW_PROFILE_ATTR_BY_STATUS", "create or replace view P360_EXPLOIT.VW_PROFILE_ATTR_BY_STATUS as " +
                "select \"CurrentStatusID\" CURRENT_STATUS_ID, \"CurrentStatusName\" CURRENT_STATUS_NAME, count(*) PRODUCT_COUNT, " +
                "round(avg(\"DefinedAttributePct\"),2) AVG_DEFINED_ATTRIBUTE_PCT, round(avg(\"DefinedMandatoryAttributePct\"),2) AVG_DEFINED_MANDATORY_ATTRIBUTE_PCT, " +
                "sum(\"RequiredAttributeCount\") REQUIRED_ATTRIBUTE_COUNT, sum(\"DefinedAttributeCount\") DEFINED_ATTRIBUTE_COUNT, sum(\"MissingAttributeCount\") MISSING_ATTRIBUTE_COUNT, " +
                "sum(\"RequiredMandatoryAttributeCount\") REQUIRED_MANDATORY_ATTRIBUTE_COUNT, sum(\"DefinedMandatoryAttributeCount\") DEFINED_MANDATORY_ATTRIBUTE_COUNT, sum(\"MissingMandatoryAttributeCount\") MISSING_MANDATORY_ATTRIBUTE_COUNT " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY group by \"CurrentStatusID\", \"CurrentStatusName\"");

        execute(con, "CREATE VW_PROFILE_APPROVED_TOP_MISSING", "create or replace view P360_EXPLOIT.VW_PROFILE_APPROVED_TOP_MISSING as " +
                "select MISSING_RANK, PRODUCT2G_IDENTIFIER, PRODUCT_SKU, EAN, PLANTILLA, BUSINESS_CODE, BUSINESS_NAME, CURRENT_STATUS_ID, CURRENT_STATUS_NAME, " +
                "REQUIRED_ATTRIBUTE_COUNT, DEFINED_ATTRIBUTE_COUNT, MISSING_ATTRIBUTE_COUNT, REQUIRED_MANDATORY_ATTRIBUTE_COUNT, DEFINED_MANDATORY_ATTRIBUTE_COUNT, MISSING_MANDATORY_ATTRIBUTE_COUNT, DEFINED_ATTRIBUTE_PCT, DEFINED_MANDATORY_ATTRIBUTE_PCT " +
                "from (select row_number() over (order by \"MissingMandatoryAttributeCount\" desc, \"MissingAttributeCount\" desc, \"Product2GIdentifier\") MISSING_RANK, " +
                "\"Product2GIdentifier\" PRODUCT2G_IDENTIFIER, \"ProductSKU\" PRODUCT_SKU, \"EAN\" EAN, \"Plantilla\" PLANTILLA, \"BusinessCode\" BUSINESS_CODE, \"BusinessName\" BUSINESS_NAME, " +
                "\"CurrentStatusID\" CURRENT_STATUS_ID, \"CurrentStatusName\" CURRENT_STATUS_NAME, \"RequiredAttributeCount\" REQUIRED_ATTRIBUTE_COUNT, \"DefinedAttributeCount\" DEFINED_ATTRIBUTE_COUNT, " +
                "\"MissingAttributeCount\" MISSING_ATTRIBUTE_COUNT, \"RequiredMandatoryAttributeCount\" REQUIRED_MANDATORY_ATTRIBUTE_COUNT, \"DefinedMandatoryAttributeCount\" DEFINED_MANDATORY_ATTRIBUTE_COUNT, " +
                "\"MissingMandatoryAttributeCount\" MISSING_MANDATORY_ATTRIBUTE_COUNT, \"DefinedAttributePct\" DEFINED_ATTRIBUTE_PCT, \"DefinedMandatoryAttributePct\" DEFINED_MANDATORY_ATTRIBUTE_PCT " +
                "from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY where \"CurrentStatusID\" = 1007) where MISSING_RANK <= 100");
    }

    private static void callProcedure(Connection con, String procedureName) throws SQLException {
        execute(con, "CALL " + procedureName, "begin P360_EXPLOIT." + procedureName + "; end;");
    }

    private static void validateProductBase(Connection con) throws SQLException {
        printQuery(con, "PRODUCT BASE STATUS", "select count(*) productos, sum(case when \"Plantilla\" is null then 1 else 0 end) sin_plantilla, sum(case when \"ArticleDomainRows\" is null then 1 else 0 end) sin_article_domain, sum(case when \"ArticleDomainRows\" > 1 then 1 else 0 end) con_mas_de_un_article_domain, max(\"ArticleDomainRows\") max_article_domain_rows from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE");
    }

    private static void validateAttrConfig(Connection con) throws SQLException {
        printQuery(con, "ATTR CONFIG BY SOURCE", "select \"ConfigSource\", count(*) total_config, count(distinct \"CharacteristicID\") distinct_characteristics, sum(case when \"IsMandatory\" = 1 then 1 else 0 end) mandatory_config, sum(case when \"BusinessFilter\" is not null and length(trim(\"BusinessFilter\")) > 0 then 1 else 0 end) with_business_filter from P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG group by \"ConfigSource\" order by \"ConfigSource\"");
        printQuery(con, "ATTR CONFIG NULL IDENTIFIERS", "select count(*) total_config, sum(case when \"CharacteristicIdentifier\" is null then 1 else 0 end) sin_characteristic_identifier from P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG");
    }

    private static void validateDefinedSummary(Connection con) throws SQLException {
        printQuery(con, "DEFINED SUMMARY", "select count(*) filas, count(distinct \"ArticleRevisionID\") productos_con_definidos, sum(\"DefinedAttributeCount\") total_defined_attrs, sum(\"DefinedMandatoryAttributeCount\") total_defined_mandatory_attrs from P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY");
    }

    private static void validateFinalSummary(Connection con) throws SQLException {
        validateOneRowPerProduct(con, "TT_PROFILE_PRODUCT_ATTR_SUMMARY", "ArticleRevisionID");
        printQuery(con, "FINAL RANGE", "select min(\"RequiredAttributeCount\") min_required, max(\"RequiredAttributeCount\") max_required, min(\"DefinedAttributeCount\") min_defined, max(\"DefinedAttributeCount\") max_defined, min(\"MissingAttributeCount\") min_missing, max(\"MissingAttributeCount\") max_missing, min(\"DefinedAttributePct\") min_pct, max(\"DefinedAttributePct\") max_pct from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY");
        printQuery(con, "FINAL NEGATIVES", "select count(*) productos_con_negativos from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY where \"MissingAttributeCount\" < 0 or \"MissingMandatoryAttributeCount\" < 0 or \"DefinedAttributePct\" > 100 or \"DefinedMandatoryAttributePct\" > 100");
    }

    private static void validateViews(Connection con) throws SQLException {
        printQuery(con, "VIEW BY STATUS", "select count(*) rows_count from P360_EXPLOIT.VW_PROFILE_ATTR_BY_STATUS");
        printQuery(con, "VIEW TOP MISSING", "select count(*) rows_count from P360_EXPLOIT.VW_PROFILE_APPROVED_TOP_MISSING");
    }

    private static void validateOneRowPerProduct(Connection con, String tableName, String columnName) throws SQLException {
        printQuery(con, tableName + " one-row validation", "select count(*) filas, count(distinct \"" + columnName + "\") productos, count(*) - count(distinct \"" + columnName + "\") diferencia from P360_EXPLOIT." + tableName);
    }

    private static void dropTableIfExists(Connection con, String tableName) throws SQLException {
        try (Statement st = con.createStatement()) {
            System.out.println("DROP TABLE " + tableName);
            st.execute("drop table " + SCHEMA + "." + tableName + " purge");
        } catch (SQLException e) {
            if (e.getErrorCode() == 942) {
                System.out.println("  no existe, ok");
            } else {
                throw e;
            }
        }
    }

    private static void createIndexIgnoreExists(Connection con, String indexName, String sql) throws SQLException {
        try {
            execute(con, "CREATE " + indexName, sql);
        } catch (SQLException e) {
            if (e.getErrorCode() == 955) {
                System.out.println("  " + indexName + " ya existe, ok");
            } else {
                throw e;
            }
        }
    }

    private static void execute(Connection con, String label, String sql) throws SQLException {
        Instant start = Instant.now();
        System.out.println("\n-- " + label + " --");
        try (Statement st = con.createStatement()) {
            st.setQueryTimeout(0);
            st.execute(sql);
        }
        long ms = Duration.between(start, Instant.now()).toMillis();
        System.out.println("OK ms=" + ms);
    }

    private static void printQuery(Connection con, String label, String sql) throws SQLException {
        System.out.println("\n-- " + label + " --");
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= cols; i++) {
                names.add(rs.getMetaData().getColumnLabel(i));
            }
            System.out.println(String.join(";", names));
            while (rs.next()) {
                List<String> values = new ArrayList<>();
                for (int i = 1; i <= cols; i++) {
                    values.add(String.valueOf(rs.getObject(i)));
                }
                System.out.println(String.join(";", values));
            }
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
