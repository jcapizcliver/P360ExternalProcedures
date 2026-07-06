package com.example.ei.forfun.logic;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class CsvRowUtils {

    private CsvRowUtils() {
    }

    public static List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        values.add(current.toString());
        return values;
    }

    public static void writeRow(Writer writer, List<String> values, char delimiter) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(delimiter);
            }
            writer.write(escape(values.get(i), delimiter));
        }
    }

    public static List<Integer> resolveColumnIndexes(List<String> header, List<String> keyColumns) {
        List<Integer> indexes = new ArrayList<>();

        for (String keyColumn : keyColumns) {
            boolean found = false;
            for (int i = 0; i < header.size(); i++) {
                if (header.get(i).equals(keyColumn)) {
                    indexes.add(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("No encontré la columna clave [" + keyColumn + "]");
            }
        }

        return indexes;
    }

    public static String cell(List<String> row, int idx) {
        return idx >= 0 && idx < row.size() ? row.get(idx) : "";
    }

    private static String escape(String value, char delimiter) {
        String safe = value == null ? "" : value;
        boolean needsQuotes =
            safe.indexOf(delimiter) >= 0 ||
            safe.indexOf('"') >= 0 ||
            safe.indexOf('\n') >= 0 ||
            safe.indexOf('\r') >= 0;

        if (!needsQuotes) {
            return safe;
        }

        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}