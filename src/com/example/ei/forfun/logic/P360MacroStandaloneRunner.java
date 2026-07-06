package com.example.ei.forfun.logic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public class P360MacroStandaloneRunner {

    private static final String ENV_JDBC_URL = "ORACLE_JDBC_URL";
    private static final String ENV_JDBC_USER = "ORACLE_JDBC_USER";
    private static final String ENV_JDBC_PASSWORD = "ORACLE_JDBC_PASSWORD";

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 3) {
            throw new IllegalArgumentException(
                "Uso: P360MacroStandaloneRunner <RequestedColumns.csv> <Characteristics.csv> <workDir> [whereClause]"
            );
        }

        String jdbcUrl = requireEnv(ENV_JDBC_URL);
        String user = requireEnv(ENV_JDBC_USER);
        String password = requireEnv(ENV_JDBC_PASSWORD);

        Path requestedColumns = Path.of(args[0]);
        Path characteristics = Path.of(args[1]);
        Path workDir = Path.of(args[2]);
        String whereClause = args.length >= 4 ? args[3] : null;

        validateInputFile(requestedColumns, "RequestedColumns.csv");
        validateInputFile(characteristics, "Characteristics.csv");
        Files.createDirectories(workDir);

        System.out.println("Iniciando P360MacroStandaloneRunner...");
        System.out.println("RequestedColumns: " + requestedColumns.toAbsolutePath());
        System.out.println("Characteristics: " + characteristics.toAbsolutePath());
        System.out.println("WorkDir: " + workDir.toAbsolutePath());
        System.out.println("WhereClause: " + (whereClause == null || whereClause.isBlank() ? "<vacío>" : whereClause));
        System.out.println("JDBC URL: " + maskJdbcUrl(jdbcUrl));
        System.out.println("JDBC User: " + user);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            P360SplitMacroPlanBuilder builder = new P360SplitMacroPlanBuilder();

            P360SplitMacroPlanBuilder.MacroBuildPlan plan =
                builder.buildPlan(
                    requestedColumns,
                    characteristics,
                    whereClause,
                    P360SplitMacroPlanBuilder.SortPreference.AUTO
                );

            printPlan(plan);

            P360MacroPlanExecutor executor =
                new P360MacroPlanExecutor(connection, ',', 1000, 50000);

            P360MacroPlanExecutor.ExecutionResult result =
                executor.executePlan(plan, workDir);

            System.out.println("Macro final: " + result.getFinalMacroFile().toAbsolutePath());
            System.out.println("Proceso terminado.");
        }
    }

    private static String requireEnv(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                "Falta la variable de entorno requerida: " + envName
            );
        }
        return value.trim();
    }

    private static void validateInputFile(Path file, String logicalName) {
        if (file == null) {
            throw new IllegalArgumentException(logicalName + " es null");
        }
        if (!Files.exists(file)) {
            throw new IllegalArgumentException(logicalName + " no existe: " + file.toAbsolutePath());
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(logicalName + " no es archivo regular: " + file.toAbsolutePath());
        }
        if (!Files.isReadable(file)) {
            throw new IllegalArgumentException(logicalName + " no se puede leer: " + file.toAbsolutePath());
        }
    }

    private static void printPlan(P360SplitMacroPlanBuilder.MacroBuildPlan plan) {
        System.out.println("===== PLAN =====");
        for (P360SplitMacroPlanBuilder.PlanStage stage : plan.getStages()) {
            System.out.println("Stage: " + stage.getStageName());
            System.out.println("Type: " + stage.getStageType());
            System.out.println("Output: " + stage.getOutputFileName());
            System.out.println("Keys: " + stage.getKeyColumns());
            System.out.println("SortMode: " + stage.getSortMode());
            System.out.println(stage.getSql());
            System.out.println("==============================================");
        }
    }

    private static String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "<vacío>";
        }
        return jdbcUrl;
    }
}