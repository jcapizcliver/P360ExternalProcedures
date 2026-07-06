package com.example.ei.forfun.logic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlsqlRunner {

    public static void main(String[] args) throws Exception {
        String file = null;

        for (int i = 0; i < args.length; i++) {
            if ("-f".equals(args[i]) && i + 1 < args.length) {
                file = args[++i];
            }
        }

        if (file == null) {
            throw new IllegalArgumentException("Uso: PlsqlRunner -f archivo.sql");
        }

        String url = firstNonBlank(
                System.getenv("ORACLE_JDBC_URL"),
                System.getenv("P360_EXPLOIT_JDBC_URL")
        );
        String user = firstNonBlank(
                System.getenv("ORACLE_JDBC_USER"),
                System.getenv("P360_EXPLOIT_JDBC_USER")
        );
        String password = firstNonBlank(
                System.getenv("ORACLE_JDBC_PASSWORD"),
                System.getenv("P360_EXPLOIT_JDBC_PASSWORD")
        );

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Faltan ORACLE_JDBC_URL / ORACLE_JDBC_USER / ORACLE_JDBC_PASSWORD");
        }

        List<String> statements = readSqlplusStyleBlocks(Path.of(file));

        try (Connection con = DriverManager.getConnection(url, user, password)) {
            con.setAutoCommit(true);

            int n = 0;
            for (String sql : statements) {
                n++;
                String executable = prepareForJdbc(sql);

                if (executable.isBlank()) {
                    continue;
                }

                System.out.println();
                System.out.println("---- Block " + n + " ----");
                System.out.println(firstLine(executable));

                try (Statement st = con.createStatement()) {
                    st.execute(executable);
                }

                System.out.println("OK");
            }
        }
    }

    private static List<String> readSqlplusStyleBlocks(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            if ("/".equals(trimmed)) {
                String block = current.toString().trim();
                if (!block.isBlank()) {
                    blocks.add(block);
                }
                current.setLength(0);
            } else {
                if (trimmed.toLowerCase().startsWith("set ")
                        || trimmed.toLowerCase().startsWith("show errors")
                        || trimmed.toLowerCase().startsWith("prompt ")) {
                    continue;
                }
                current.append(line).append(System.lineSeparator());
            }
        }

        String last = current.toString().trim();
        if (!last.isBlank()) {
            blocks.add(last);
        }

        return blocks;
    }

    private static String prepareForJdbc(String sql) {
        String s = sql.trim();
        String lower = s.toLowerCase();

        boolean plsql =
                   lower.startsWith("create or replace procedure")
                || lower.startsWith("create or replace function")
                || lower.startsWith("create or replace package")
                || lower.startsWith("create or replace trigger")
                || lower.startsWith("declare")
                || lower.startsWith("begin");

        if (!plsql && s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }

        return s;
    }

    private static String firstLine(String s) {
        String[] lines = s.split("\\R", 2);
        return lines.length == 0 ? s : lines[0];
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