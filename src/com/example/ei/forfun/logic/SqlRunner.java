package com.example.ei.forfun.logic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Ejecuta SQL desde:
 *  - Primer argumento como SQL literal, o
 *  - Archivo: -f /ruta/al/archivo.sql   (o --file)
 *
 * Salida CSV:
 *  - Opcionalmente puedes pasar una ruta de salida como argumento adicional.
 *  - Si no se especifica, genera un CSV en el directorio actual.
 *
 * Credenciales por env vars:
 *   ORACLE_JDBC_URL       (ej: jdbc:oracle:thin:@//host:1521/service)
 *   ORACLE_JDBC_USER
 *   ORACLE_JDBC_PASSWORD
 *
 * Ejemplos:
 *   java ... SqlRunner "SELECT 1 AS X FROM dual"
 *   java ... SqlRunner "SELECT 1 AS X FROM dual" salida.csv
 *   java ... SqlRunner -f /tmp/job.sql
 *   java ... SqlRunner -f /tmp/job.sql /tmp/resultado.csv
 *
 * Nota:
 *  - Divide por ';' a nivel línea (ignora ';' dentro de comillas simples/dobles).
 *  - Si hay múltiples SELECT con ResultSet, todos se escriben en el mismo CSV.
 *  - Entre ResultSets se inserta una línea en blanco.
 *  - Para statements sin ResultSet, se escribe una línea de comentario con el resultado.
 */
public class SqlRunner {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Uso: SqlRunner \"<sql>\" [salida.csv]  |  SqlRunner -f <archivo.sql> [salida.csv]");
        }

        String sqlText;
        Path outputPath;

        if (isFileMode(args)) {
            if (args.length < 2) {
                throw new IllegalArgumentException("Uso: SqlRunner -f <archivo.sql> [salida.csv]");
            }
            String sqlFilePath = args[1];
            sqlText = readFile(sqlFilePath);
            outputPath = resolveOutputPath(args, 2);
        } else {
            sqlText = args[0];
            outputPath = resolveOutputPath(args, 1);
        }

        sqlText = normalizeSqlText(sqlText);
        List<String> statements = splitStatements(sqlText);

        if (statements.isEmpty()) {
            System.out.println("No hay statements para ejecutar.");
            return;
        }

        String url = mustEnv("ORACLE_JDBC_URL");
        String usr = mustEnv("ORACLE_JDBC_USER");
        String pwd = mustEnv("ORACLE_JDBC_PASSWORD");

        ensureParentDirectoryExists(outputPath);

        try (Connection c = DriverManager.getConnection(url, usr, pwd);
             BufferedWriter writer = Files.newBufferedWriter(
                     outputPath,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE)) {

            c.setAutoCommit(true);

            int i = 0;
            boolean wroteAnything = false;

            for (String stmt : statements) {
                i++;
                System.out.println("\n---- Statement " + i + " ----");
                System.out.println(stmt);

                boolean wroteThisStatement = executeOne(c, stmt, writer);

                if (wroteAnything && wroteThisStatement) {
                    // nada adicional; la separación ya la maneja executeOne para ResultSets múltiples
                }
                wroteAnything = wroteAnything || wroteThisStatement;
            }

            writer.flush();
        }

        System.out.println("CSV generado en: " + outputPath.toAbsolutePath());
    }

    private static boolean isFileMode(String[] args) {
        if (args.length < 1) return false;
        String a0 = args[0].toLowerCase(Locale.ROOT);
        return a0.equals("-f") || a0.equals("--file");
    }

    private static String readFile(String path) throws IOException {
        return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    }

    private static Path resolveOutputPath(String[] args, int outputArgIndex) {
        if (args.length > outputArgIndex && args[outputArgIndex] != null && !args[outputArgIndex].isBlank()) {
            return Paths.get(args[outputArgIndex].trim());
        }
        String fileName = "sqlrunner_" + LocalDateTime.now().format(TS_FMT) + ".csv";
        return Paths.get(fileName);
    }

    private static void ensureParentDirectoryExists(Path outputPath) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String normalizeSqlText(String s) {
        if (s == null) return "";
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1);
        }
        return s.trim();
    }

    private static boolean executeOne(Connection c, String sql, Writer writer) throws SQLException, IOException {
        boolean isPlsqlBlock = looksLikePlsql(sql);

        if (isPlsqlBlock) {
            try (CallableStatement cs = c.prepareCall(sql)) {
                cs.execute();
                writeCommentLine(writer, "OK (PL/SQL ejecutado)");
                System.out.println("OK (PL/SQL ejecutado).");
                return true;
            }
        }

        try (Statement st = c.createStatement()) {
            boolean hasResultSet = st.execute(sql);

            if (hasResultSet) {
                try (ResultSet rs = st.getResultSet()) {
                    int rowCount = writeResultSetAsCsv(rs, writer);
                    System.out.println("ROWS=" + rowCount);
                }
                writer.write(System.lineSeparator());
                return true;
            } else {
                int updated = st.getUpdateCount();
                writeCommentLine(writer, "OK. UpdateCount=" + updated);
                System.out.println("OK. UpdateCount=" + updated);
                return true;
            }
        }
    }

    private static boolean looksLikePlsql(String sql) {
        String s = sql.trim().toUpperCase(Locale.ROOT);
        return s.startsWith("BEGIN") || s.startsWith("DECLARE") || s.startsWith("CALL") || s.startsWith("CREATE OR REPLACE");
    }

    /**
     * Split por ';' respetando comillas simples y dobles.
     * También ignora líneas en blanco y comentarios tipo "--".
     */
    private static List<String> splitStatements(String input) {
        Objects.requireNonNull(input);

        StringBuilder cleaned = new StringBuilder();
        String[] lines = input.split("\\R", -1);
        for (String line : lines) {
            String l = line;
            int idx = l.indexOf("--");
            if (idx >= 0) {
                l = l.substring(0, idx);
            }
            cleaned.append(l).append('\n');
        }

        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);

            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
                cur.append(ch);
                continue;
            }
            if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
                cur.append(ch);
                continue;
            }

            if (ch == ';' && !inSingle && !inDouble) {
                String stmt = cur.toString().trim();
                if (!stmt.isEmpty()) out.add(stmt);
                cur.setLength(0);
                continue;
            }

            cur.append(ch);
        }

        String last = cur.toString().trim();
        if (!last.isEmpty()) out.add(last);

        return out;
    }

    private static int writeResultSetAsCsv(ResultSet rs, Writer writer) throws SQLException, IOException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        for (int i = 1; i <= cols; i++) {
            if (i > 1) writer.write(',');
            writer.write(csvEscape(md.getColumnLabel(i)));
        }
        writer.write(System.lineSeparator());

        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
            for (int i = 1; i <= cols; i++) {
                if (i > 1) writer.write(',');
                Object v = rs.getObject(i);
                writer.write(csvEscape(formatCell(v)));
            }
            writer.write(System.lineSeparator());
        }

        return rowCount;
    }

    private static void writeCommentLine(Writer writer, String text) throws IOException {
        writer.write(csvEscape("# " + text));
        writer.write(System.lineSeparator());
    }

    private static String formatCell(Object v) throws SQLException {
        if (v == null) return "";
        if (v instanceof Clob) {
            Clob c = (Clob) v;
            long len = c.length();
            long n = Math.min(len, 4000);
            return c.getSubString(1, (int) n) + (len > n ? "…(clob)" : "");
        }
        if (v instanceof Timestamp) {
            return ((Timestamp) v).toString();
        }
        if (v instanceof Date) {
            return ((Date) v).toString();
        }
        if (v instanceof Time) {
            return ((Time) v).toString();
        }
        return String.valueOf(v);
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        boolean mustQuote =
                value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r");

        StringBuilder sb = new StringBuilder(value.length() + 16);

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (ch == '"') {
                boolean alreadyEscaped = i > 0 && value.charAt(i - 1) == '\\';
                if (!alreadyEscaped) {
                    sb.append('\\');
                }
                sb.append('"');
            } else {
                sb.append(ch);
            }
        }

        String escaped = sb.toString();
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static String mustEnv(String k) {
        String v = System.getenv(k);
        if (v == null || v.isBlank()) throw new IllegalStateException("Falta env var: " + k);
        return v.trim();
    }
}