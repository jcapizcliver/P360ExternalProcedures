package com.example.ei.forfun.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvMergeUtils {

    private CsvMergeUtils() {
    }

    public static void leftJoinUniqueRight(Path leftFile,
                                           Path rightFile,
                                           Path outputFile,
                                           char delimiter,
                                           String leftKeyColumn,
                                           String rightKeyColumn) throws IOException {

        CsvTable right = CsvTable.load(rightFile, delimiter);

        int rightKeyIdx = right.indexOf(rightKeyColumn);
        Map<String, List<String>> rightByKey = new LinkedHashMap<>();

        for (List<String> row : right.rows) {
            String key = CsvRowUtils.cell(row, rightKeyIdx);
            if (rightByKey.putIfAbsent(key, row) != null) {
                throw new IllegalStateException("Duplicado en archivo derecho para llave [" + key + "] en " + rightFile);
            }
        }

        CsvTable left = CsvTable.load(leftFile, delimiter);
        int leftKeyIdx = left.indexOf(leftKeyColumn);

        List<String> rightExtraHeaders = new ArrayList<>();
        List<Integer> rightExtraIndexes = new ArrayList<>();

        for (int i = 0; i < right.header.size(); i++) {
            if (i != rightKeyIdx && !left.header.contains(right.header.get(i))) {
                rightExtraHeaders.add(right.header.get(i));
                rightExtraIndexes.add(i);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            List<String> outputHeader = new ArrayList<>(left.header);
            outputHeader.addAll(rightExtraHeaders);
            CsvRowUtils.writeRow(writer, outputHeader, delimiter);
            writer.newLine();

            for (List<String> leftRow : left.rows) {
                String key = CsvRowUtils.cell(leftRow, leftKeyIdx);
                List<String> joined = new ArrayList<>(leftRow);
                List<String> rightRow = rightByKey.get(key);

                if (rightRow != null) {
                    for (Integer idx : rightExtraIndexes) {
                        joined.add(CsvRowUtils.cell(rightRow, idx));
                    }
                } else {
                    for (int i = 0; i < rightExtraIndexes.size(); i++) {
                        joined.add("");
                    }
                }

                CsvRowUtils.writeRow(writer, joined, delimiter);
                writer.newLine();
            }
        }
    }

    public static void leftJoinAllowManyRight(Path leftFile,
                                              Path rightFile,
                                              Path outputFile,
                                              char delimiter,
                                              String leftKeyColumn,
                                              String rightKeyColumn) throws IOException {

        CsvTable right = CsvTable.load(rightFile, delimiter);
        int rightKeyIdx = right.indexOf(rightKeyColumn);
        Map<String, List<List<String>>> rightByKey = new LinkedHashMap<>();

        for (List<String> row : right.rows) {
            String key = CsvRowUtils.cell(row, rightKeyIdx);
            rightByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        CsvTable left = CsvTable.load(leftFile, delimiter);
        int leftKeyIdx = left.indexOf(leftKeyColumn);

        List<String> rightExtraHeaders = new ArrayList<>();
        List<Integer> rightExtraIndexes = new ArrayList<>();

        for (int i = 0; i < right.header.size(); i++) {
            if (i != rightKeyIdx && !left.header.contains(right.header.get(i))) {
                rightExtraHeaders.add(right.header.get(i));
                rightExtraIndexes.add(i);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            List<String> outputHeader = new ArrayList<>(left.header);
            outputHeader.addAll(rightExtraHeaders);
            CsvRowUtils.writeRow(writer, outputHeader, delimiter);
            writer.newLine();

            for (List<String> leftRow : left.rows) {
                String key = CsvRowUtils.cell(leftRow, leftKeyIdx);
                List<List<String>> matches = rightByKey.get(key);

                if (matches == null || matches.isEmpty()) {
                    List<String> joined = new ArrayList<>(leftRow);
                    for (int i = 0; i < rightExtraIndexes.size(); i++) {
                        joined.add("");
                    }
                    CsvRowUtils.writeRow(writer, joined, delimiter);
                    writer.newLine();
                    continue;
                }

                for (List<String> rightRow : matches) {
                    List<String> joined = new ArrayList<>(leftRow);
                    for (Integer idx : rightExtraIndexes) {
                        joined.add(CsvRowUtils.cell(rightRow, idx));
                    }
                    CsvRowUtils.writeRow(writer, joined, delimiter);
                    writer.newLine();
                }
            }
        }
    }

    private static class CsvTable {
        private final List<String> header;
        private final List<List<String>> rows;

        private CsvTable(List<String> header, List<List<String>> rows) {
            this.header = header;
            this.rows = rows;
        }

        private static CsvTable load(Path file, char delimiter) throws IOException {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IllegalArgumentException("Archivo vacío: " + file);
                }

                List<String> header = CsvRowUtils.parseLine(headerLine, delimiter);
                List<List<String>> rows = new ArrayList<>();

                String line;
                while ((line = reader.readLine()) != null) {
                    rows.add(CsvRowUtils.parseLine(line, delimiter));
                }

                return new CsvTable(header, rows);
            }
        }

        private int indexOf(String columnName) {
            for (int i = 0; i < header.size(); i++) {
                if (header.get(i).equals(columnName)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("No existe la columna [" + columnName + "]");
        }
    }
}