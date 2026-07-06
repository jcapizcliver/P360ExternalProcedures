package com.example.ei.forfun.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.ei.forfun.logic.P360SplitMacroPlanBuilder.StageType;

public class P360MacroPlanExecutor {

    private final Connection connection;
    private final char delimiter;
    private final int fetchSize;
    private final int sortChunkSize;

    public P360MacroPlanExecutor(Connection connection, char delimiter, int fetchSize, int sortChunkSize) {
        this.connection = connection;
        this.delimiter = delimiter;
        this.fetchSize = fetchSize;
        this.sortChunkSize = sortChunkSize <= 0 ? 50000 : sortChunkSize;
    }

    public ExecutionResult executePlan(P360SplitMacroPlanBuilder.MacroBuildPlan plan, Path workDir) throws Exception {
        if (plan == null) {
            throw new IllegalArgumentException("plan es null");
        }
        if (workDir == null) {
            throw new IllegalArgumentException("workDir es null");
        }

        Files.createDirectories(workDir);

        Map<String, StageExecutionResult> stageResultsByName = new LinkedHashMap<>();

        for (P360SplitMacroPlanBuilder.PlanStage stage : plan.getStages()) {
            Path output = workDir.resolve(stage.getOutputFileName());
            executeStage(stage, output);

            Path finalStageFile = output;
            if (stage.getSortMode() == P360SplitMacroPlanBuilder.SortMode.EXTERNAL_SORT) {
                Path sorted = workDir.resolve(replaceCsvSuffix(stage.getOutputFileName(), "_sorted.csv"));
                CsvExternalSorter.sortCsv(
                    output,
                    sorted,
                    delimiter,
                    stage.getKeyColumns(),
                    sortChunkSize
                );
                finalStageFile = sorted;
            }

            stageResultsByName.put(
                stage.getStageName(),
                new StageExecutionResult(stage, finalStageFile)
            );
        }

        Path macroBase = workDir.resolve("04_macro_base.csv");
        buildMacroBase(
            findStage(stageResultsByName, StageType.PRODUCT_BASE_KEYS),
            findStage(stageResultsByName, StageType.ARTICLE_REFERENCE_MAP),
            findStage(stageResultsByName, StageType.ARTICLE_BASE_KEYS),
            macroBase
        );

        Path currentMacro = macroBase;

        for (StageExecutionResult result : stageResultsByName.values()) {
            StageType stageType = result.getStage().getStageType();

            if (stageType == StageType.PRODUCT_ENRICHMENT) {
                Path next = workDir.resolve("merge_" + result.getStage().getStageName() + ".csv");
                CsvMergeUtils.leftJoinUniqueRight(
                    currentMacro,
                    result.getFile(),
                    next,
                    delimiter,
                    "ProductID",
                    "ProductID"
                );
                currentMacro = next;
            } else if (stageType == StageType.ARTICLE_ENRICHMENT) {
                Path next = workDir.resolve("merge_" + result.getStage().getStageName() + ".csv");
                CsvMergeUtils.leftJoinUniqueRight(
                    currentMacro,
                    result.getFile(),
                    next,
                    delimiter,
                    "ArticleID",
                    "ArticleID"
                );
                currentMacro = next;
            }
        }

        Path finalMacro = workDir.resolve("macroarchivo_final.csv");
        Files.copy(currentMacro, finalMacro, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        return new ExecutionResult(finalMacro, stageResultsByName);
    }

    private void executeStage(P360SplitMacroPlanBuilder.PlanStage stage, Path output) throws SQLException, IOException {
        try (PreparedStatement ps = connection.prepareStatement(stage.getSql());
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {

            if (fetchSize > 0) {
                ps.setFetchSize(fetchSize);
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                List<String> headers = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    String label = meta.getColumnLabel(i);
                    if (label == null || label.trim().isEmpty()) {
                        label = meta.getColumnName(i);
                    }
                    headers.add(label);
                }

                CsvRowUtils.writeRow(writer, headers, delimiter);
                writer.newLine();

                while (rs.next()) {
                    List<String> row = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        row.add(value == null ? "" : String.valueOf(value));
                    }
                    CsvRowUtils.writeRow(writer, row, delimiter);
                    writer.newLine();
                }
            }
        }
    }

    private void buildMacroBase(StageExecutionResult productBase,
                                StageExecutionResult refMap,
                                StageExecutionResult articleBase,
                                Path macroBase) throws Exception {

        Path productRef = macroBase.getParent().resolve("tmp_product_ref.csv");

        CsvMergeUtils.leftJoinAllowManyRight(
            productBase.getFile(),
            refMap.getFile(),
            productRef,
            delimiter,
            "ProductIdentifier",
            "ProductIdentifier"
        );

        CsvMergeUtils.leftJoinUniqueRight(
            productRef,
            articleBase.getFile(),
            macroBase,
            delimiter,
            "ArticleID",
            "ArticleID"
        );
    }

    private StageExecutionResult findStage(Map<String, StageExecutionResult> stageResultsByName, StageType stageType) {
        for (StageExecutionResult result : stageResultsByName.values()) {
            if (result.getStage().getStageType() == stageType) {
                return result;
            }
        }
        throw new IllegalStateException("No encontré stage de tipo: " + stageType);
    }

    private String replaceCsvSuffix(String fileName, String replacement) {
        if (fileName.toLowerCase().endsWith(".csv")) {
            return fileName.substring(0, fileName.length() - 4) + replacement;
        }
        return fileName + replacement;
    }

    public static class StageExecutionResult {
        private final P360SplitMacroPlanBuilder.PlanStage stage;
        private final Path file;

        public StageExecutionResult(P360SplitMacroPlanBuilder.PlanStage stage, Path file) {
            this.stage = stage;
            this.file = file;
        }

        public P360SplitMacroPlanBuilder.PlanStage getStage() {
            return stage;
        }

        public Path getFile() {
            return file;
        }
    }

    public static class ExecutionResult {
        private final Path finalMacroFile;
        private final Map<String, StageExecutionResult> stageResultsByName;

        public ExecutionResult(Path finalMacroFile, Map<String, StageExecutionResult> stageResultsByName) {
            this.finalMacroFile = finalMacroFile;
            this.stageResultsByName = Map.copyOf(stageResultsByName);
        }

        public Path getFinalMacroFile() {
            return finalMacroFile;
        }

        public Map<String, StageExecutionResult> getStageResultsByName() {
            return stageResultsByName;
        }
    }
}