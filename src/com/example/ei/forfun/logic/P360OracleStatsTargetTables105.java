package com.example.ei.forfun.logic;

import java.sql.*;
import java.util.*;

public class P360OracleStatsTargetTables105 {

    private final String jdbcUrl;
    private final String user;
    private final String pass;

    // Default: “tablas pendejas” del análisis + tablas del manual
    private static final List<String> DEFAULT_TABLES = List.of(
//            "LookupValueReference",
//            "LookupValue",
//           "ArticleAttribute",
//            "ArticleAttributeValue",
//            "ArticleLang",
//            "ArticleDetail"
//	      "Article",
//	      "ArticleCharactValue"
    );

    public P360OracleStatsTargetTables105(String jdbcUrl, String user, String pass) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.user = Objects.requireNonNull(user, "user");
        this.pass = Objects.requireNonNull(pass, "pass");
    }

    /** Manual P360 (DB dedicada): deshabilitar autotasks relevantes. */
    public void disableAutoTasks(Connection c) throws SQLException {
        execPlsql(c,
                "BEGIN\n" +
                "  DBMS_AUTO_TASK_ADMIN.DISABLE(\n" +
                "    CLIENT_NAME => 'sql tuning advisor',\n" +
                "    OPERATION   => NULL,\n" +
                "    WINDOW_NAME => NULL\n" +
                "  );\n" +
                "END;"
        );
        execPlsql(c,
                "BEGIN\n" +
                "  DBMS_AUTO_TASK_ADMIN.DISABLE(\n" +
                "    CLIENT_NAME => 'auto optimizer stats collection',\n" +
                "    OPERATION   => NULL,\n" +
                "    WINDOW_NAME => NULL\n" +
                "  );\n" +
                "END;"
        );
    }

    /** Manual P360: gather, 100%, skewonly, cascade, degree 8. */
    public void gatherTableStatsSkewOnly(Connection c, String schema, String table) throws SQLException {
        String plsql =
                "BEGIN\n" +
                "  DBMS_STATS.GATHER_TABLE_STATS(\n" +
                "    OWNNAME          => ?,\n" +
                "    TABNAME          => ?,\n" +
                "    OPTIONS          => 'gather',\n" +
                "    ESTIMATE_PERCENT => 100,\n" +
                "    METHOD_OPT       => 'for all columns size skewonly',\n" +
                "    CASCADE          => TRUE,\n" +
                "    DEGREE           => 8\n" +
                "  );\n" +
                "END;";
	String tabQuoted = "\"" + table + "\"";
        try (CallableStatement cs = c.prepareCall(plsql)) {
            cs.setString(1, schema);
            cs.setString(2, tabQuoted);
            cs.execute();
        }
    }

    /** Manual P360: borrar height-balanced histograms en NVARCHAR2 con NDV>254 (en schemas PIM_*). */
    public void deleteHeightBalancedHistogramsForNvarchar2(Connection c, List<String> schemas) throws SQLException {
        String inList = toSqlInList(schemas);
        String plsql =
                "BEGIN\n" +
                "  FOR col_item IN (\n" +
                "    SELECT stats.OWNER, stats.TABLE_NAME, stats.COLUMN_NAME\n" +
                "    FROM DBA_TAB_COL_STATISTICS stats\n" +
                "    INNER JOIN DBA_TAB_COLUMNS cols\n" +
                "      ON stats.TABLE_NAME = cols.TABLE_NAME\n" +
                "     AND stats.COLUMN_NAME = cols.COLUMN_NAME\n" +
                "    WHERE cols.DATA_TYPE = 'NVARCHAR2'\n" +
                "      AND stats.HISTOGRAM <> 'NONE'\n" +
                "      AND stats.NUM_DISTINCT > 254\n" +
                "      AND stats.OWNER = cols.OWNER\n" +
                "      AND stats.OWNER IN " + inList + "\n" +
                "    ORDER BY stats.OWNER, stats.TABLE_NAME, stats.COLUMN_NAME\n" +
                "  ) LOOP\n" +
                "    DBMS_STATS.DELETE_COLUMN_STATS(\n" +
                "      OWNNAME       => col_item.OWNER,\n" +
                "      TABNAME       => '\"' || col_item.TABLE_NAME  || '\"',\n" +
                "      COLNAME       => '\"' || col_item.COLUMN_NAME || '\"',\n" +
                "      COL_STAT_TYPE => 'HISTOGRAM'\n" +
                "    );\n" +
                "  END LOOP;\n" +
                "END;";
        execPlsql(c, plsql);
    }

    /** Manual (>=12.2, aplica 19c): borrar system-generated extended stats en PIM_%. */
    public void dropSystemGeneratedExtendedStats(Connection c) throws SQLException {
        String plsql =
                "BEGIN\n" +
                "  FOR r IN (\n" +
                "    SELECT OWNER, TABLE_NAME, DBMS_LOB.SUBSTR(EXTENSION, 3000) AS X\n" +
                "    FROM DBA_STAT_EXTENSIONS\n" +
                "    WHERE OWNER LIKE 'PIM_%'\n" +
                "      AND CREATOR = 'SYSTEM'\n" +
                "      AND DROPPABLE = 'YES'\n" +
                "    ORDER BY OWNER, TABLE_NAME, X\n" +
                "  ) LOOP\n" +
                "    DBMS_STATS.DROP_EXTENDED_STATS(r.OWNER, '\"' || r.TABLE_NAME || '\"', r.X);\n" +
                "  END LOOP;\n" +
                "END;";
        execPlsql(c, plsql);
    }

    /** Manual (>=12.2, aplica 19c): AUTO_STAT_EXTENSIONS=OFF por tabla en schemas PIM_* (o los que pases). */
    public void disableAutoStatExtensionsForSchemas(Connection c, List<String> schemas) throws SQLException {
        String inList = toSqlInList(schemas);
        String plsql =
                "BEGIN\n" +
                "  FOR t IN (\n" +
                "    SELECT OWNER, TABLE_NAME\n" +
                "    FROM DBA_TABLES\n" +
                "    WHERE OWNER IN " + inList + "\n" +
                "    ORDER BY OWNER, TABLE_NAME\n" +
                "  ) LOOP\n" +
                "    DBMS_STATS.SET_TABLE_PREFS(\n" +
                "      t.OWNER,\n" +
                "      '\"' || t.TABLE_NAME || '\"',\n" +
                "      'AUTO_STAT_EXTENSIONS',\n" +
                "      'OFF'\n" +
                "    );\n" +
                "  END LOOP;\n" +
                "END;";
        execPlsql(c, plsql);
    }

    /** Ejecuta la recolección para tablas objetivo (default + extras), validando que existan. */
    public void run(String schema, List<String> targetTables, boolean validateExistence) throws SQLException {
        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pass)) {
            c.setAutoCommit(true);

            // Si vas full “manual P360” en DB dedicada, normalmente esto va ONCE:
            // - disableAutoTasks
            // - dropSystemGeneratedExtendedStats
            // - disableAutoStatExtensionsForSchemas
            // Los dejo opcionales para que no te metas el pie en prod sin querer.
            boolean doOneTimeManualSteps = Boolean.parseBoolean(envOrDefault("P360_ONE_TIME_STEPS", "false"));
            if (doOneTimeManualSteps) {
                disableAutoTasks(c);
                dropSystemGeneratedExtendedStats(c);
                // si quieres, pásame P360_SCHEMAS con PIM_MAIN,PIM_MASTER... (o deja solo schema)
                List<String> schemas = parseCsvEnvOrDefault("P360_SCHEMAS", List.of(schema));
                disableAutoStatExtensionsForSchemas(c, schemas);
                deleteHeightBalancedHistogramsForNvarchar2(c, schemas);
            }

            for (String t : targetTables) {
                if (validateExistence && !tableExists(c, schema, t)) {
                    System.out.println("SKIP (no existe): " + schema + "." + t);
                    continue;
                }
                gatherTableStatsSkewOnly(c, schema, t);
                System.out.println("OK gather_table_stats: " + schema + "." + t);
            }
        }
    }

private static boolean tableExists(Connection c, String schema, String table) throws SQLException {
    String sql = "SELECT 1 FROM all_tables WHERE owner = ? AND table_name = ?";

    // 1) Intento tal cual (para tablas con comillas / PascalCase)
    try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, schema);     // NO upper
        ps.setString(2, table);      // NO upper
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return true;
        }
    }

    // 2) Fallback (para tablas normales sin comillas)
    try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, schema.toUpperCase(Locale.ROOT));
        ps.setString(2, table.toUpperCase(Locale.ROOT));
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }
}

    private static void execPlsql(Connection c, String plsql) throws SQLException {
        try (CallableStatement cs = c.prepareCall(plsql)) {
            cs.execute();
        }
    }

    private static String toSqlInList(List<String> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("schemas vacío");
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("'").append(items.get(i).replace("'", "''")).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    private static List<String> parseCsvEnvOrDefault(String key, List<String> def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        List<String> out = new ArrayList<>();
        for (String s : v.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out.isEmpty() ? def : out;
    }

    private static String envOrDefault(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static String mustEnv(String k) {
        String v = System.getenv(k);
        if (v == null || v.isBlank()) throw new IllegalStateException("Falta env var: " + k);
        return v;
    }

    public static void main(String[] args) throws Exception {
        String url = mustEnv("ORACLE_JDBC_URL");
        String usr = mustEnv("ORACLE_JDBC_USER");
        String pwd = mustEnv("ORACLE_JDBC_PASSWORD");
        String schema = mustEnv("ORACLE_JDBC_SCHEMA"); // ej: PIM_MAIN

        // Default tables + extras opcionales
        // P360_EXTRA_TABLES=OTRA1,OTRA2
        Set<String> targets = new LinkedHashSet<>(DEFAULT_TABLES);
        String extra = System.getenv("P360_EXTRA_TABLES");
        if (extra != null && !extra.isBlank()) {
            for (String t : extra.split(",")) {
                t = t.trim();
                if (!t.isEmpty()) targets.add(t);
            }
        }

        boolean validate = Boolean.parseBoolean(envOrDefault("P360_VALIDATE_TABLES", "true"));

        P360OracleStatsTargetTables105 job = new P360OracleStatsTargetTables105(url, usr, pwd);
        job.run(schema, new ArrayList<>(targets), validate);

        System.out.println("Listo. Tablas objetivo: " + targets);
    }
}
