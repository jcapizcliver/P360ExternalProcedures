package com.example.ei.forfun.logic;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;

public class P360OracleStatsTargetColumns105 {

    private final String jdbcUrl;
    private final String user;
    private final String pass;

    public P360OracleStatsTargetColumns105(String jdbcUrl, String user, String pass) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.user = Objects.requireNonNull(user, "user");
        this.pass = Objects.requireNonNull(pass, "pass");
    }

    public void gatherTargetColumnHistograms(Connection c, String schema, String table, boolean cascade) throws Exception {
    String plsql =
            "BEGIN\n" +
            "  DBMS_STATS.GATHER_TABLE_STATS(\n" +
            "    OWNNAME          => ?,\n" +
            "    TABNAME          => ?,\n" +
            "    ESTIMATE_PERCENT => DBMS_STATS.AUTO_SAMPLE_SIZE,\n" +
            "    METHOD_OPT       => 'FOR COLUMNS SIZE 254 \"DeletionTimestamp\", \"EntityID\"',\n" +
            "    CASCADE          => " + (cascade ? "TRUE" : "FALSE") + ",\n" +
            "    NO_INVALIDATE    => DBMS_STATS.AUTO_INVALIDATE\n" +
            "  );\n" +
            "END;";

    try (CallableStatement cs = c.prepareCall(plsql)) {
        cs.setString(1, schema);
        cs.setString(2, quoteIdentifier( table ));
        cs.execute();
    }
}

private static String quoteIdentifier(String s) {
    if (s == null || s.isBlank()) throw new IllegalArgumentException("identifier vacío");
    if (s.startsWith("\"") && s.endsWith("\"")) return s;
    return "\"" + s + "\"";
}

    public void run(String schema, String table, boolean cascade) throws Exception {
        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pass)) {
            c.setAutoCommit(true);
            gatherTargetColumnHistograms(c, schema, table, cascade);
            System.out.println("OK targeted histograms: " + schema + "." + table);
            System.out.println("METHOD_OPT = FOR COLUMNS SIZE 254 \"DeletionTimestamp\", \"EntityID\"");
            System.out.println("CASCADE = " + cascade);
        }
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

        String schema = envOrDefault("ORACLE_JDBC_SCHEMA", "PIM_MASTER");
        String table = envOrDefault("P360_TABLE", "ArticleCharactValue");
        boolean cascade = Boolean.parseBoolean(envOrDefault("P360_CASCADE", "false"));

        P360OracleStatsTargetColumns105 job = new P360OracleStatsTargetColumns105(url, usr, pwd);
        job.run(schema, table, cascade);
    }
}
