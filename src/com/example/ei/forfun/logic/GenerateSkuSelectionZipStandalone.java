package com.example.ei.forfun.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class GenerateSkuSelectionZipStandalone {

    private static final String[] SKU_COLUMN_CANDIDATES = { "SKU", "SKU Producto", "SKUVariante" };
    private static final DateTimeFormatter FILE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 2) {
            System.err.println("Uso:");
            System.err.println("java com.example.ei.forfun.logic.GenerateSkuSelectionZipStandalone <base.csv> <request.txt> [output.zip]");
            System.err.println("");
            System.err.println("Formato del request.txt:");
            System.err.println("[COLUMNS]");
            System.err.println("Identifier");
            System.err.println("ProductName");
            System.err.println("[SKUS]");
            System.err.println("99986912411");
            System.err.println("1181041834");
            return;
        }

        Path baseCsv = Paths.get(args[0]);
        Path requestFile = Paths.get(args[1]);
        Path outputZip = args.length >= 3
            ? Paths.get(args[2])
            : Paths.get("sku_selection_" + FILE_TS_FORMAT.format(LocalDateTime.now()) + ".zip");

        if (!Files.exists(baseCsv) || !Files.isRegularFile(baseCsv)) {
            throw new IllegalArgumentException("No existe el archivo base: " + baseCsv);
        }

        if (!Files.exists(requestFile) || !Files.isRegularFile(requestFile)) {
            throw new IllegalArgumentException("No existe el archivo request: " + requestFile);
        }

        byte[] requestBytes = Files.readAllBytes(requestFile);
        UserRequest userRequest = parseUserRequest(requestBytes);
        validateUserRequest(userRequest);

        writeZip(baseCsv, outputZip, userRequest);

        System.out.println("ZIP generado en: " + outputZip.toAbsolutePath());
    }

    private static void writeZip(Path baseCsv, Path outputZip, UserRequest userRequest) throws IOException {
        Files.createDirectories(outputZip.toAbsolutePath().getParent() == null
            ? Paths.get(".").toAbsolutePath().normalize()
            : outputZip.toAbsolutePath().getParent());

        final List<String>[] headerHolder = new List[] { null };
        final Map<String, Integer>[] headerIndexesHolder = new Map[] { null };
        final Integer[] skuIndexHolder = new Integer[] { null };
        final String[] realSkuHeaderHolder = new String[] { null };

        final BufferedWriter[] writerHolder = new BufferedWriter[] { null };
        final long[] matches = new long[] { 0L };
        final boolean[] headerProcessed = new boolean[] { false };

        final RuntimeException[] runtimeError = new RuntimeException[] { null };

        try (
            ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(outputZip), StandardCharsets.UTF_8)
        ) {
            String innerCsvName = outputZip.getFileName().toString().replaceAll("\\.zip$", "") + ".csv";
            zipOut.putNextEntry(new ZipEntry(innerCsvName));

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zipOut, StandardCharsets.UTF_8));
            writerHolder[0] = writer;

            SimpleDelimitedFileParser parser =
                new SimpleDelimitedFileParser('"', ',', '\\', "\n", StandardCharsets.UTF_8, values -> {
                    try {
                        List<String> row = new ArrayList<>(Arrays.asList(values));

                        if (!headerProcessed[0]) {
                            headerProcessed[0] = true;

                            headerHolder[0] = row;
                            headerIndexesHolder[0] = buildHeaderIndexes(row);
                            realSkuHeaderHolder[0] = resolveRealSkuHeader(row, headerIndexesHolder[0]);
                            skuIndexHolder[0] = headerIndexesHolder[0].get(normalizeKey(realSkuHeaderHolder[0]));

                            if (skuIndexHolder[0] == null) {
                                throw new IllegalStateException(
                                    "El archivo base no contiene ninguna de las columnas SKU esperadas: "
                                    + Arrays.toString(SKU_COLUMN_CANDIDATES)
                                    + ". Header real: " + row
                                );
                            }

                            List<String> outputColumns = buildOutputColumns(
                                userRequest.getRequestedColumns(),
                                row,
                                headerIndexesHolder[0],
                                realSkuHeaderHolder[0]
                            );

                            writeCsvRow(writerHolder[0], outputColumns);
                            return;
                        }

                        if (skuIndexHolder[0].intValue() >= row.size()) {
                            return;
                        }

                        String skuValue = safe(row.get(skuIndexHolder[0])).trim();

                        if (!userRequest.getSkus().contains(skuValue)) {
                            return;
                        }

                        List<String> outputColumns = buildOutputColumns(
                            userRequest.getRequestedColumns(),
                            headerHolder[0],
                            headerIndexesHolder[0],
                            realSkuHeaderHolder[0]
                        );
                        List<Integer> outputIndexes = resolveOutputIndexes(outputColumns, headerIndexesHolder[0]);

                        List<String> outputRow = new ArrayList<>(outputIndexes.size());
                        for (Integer index : outputIndexes) {
                            outputRow.add(index.intValue() < row.size() ? row.get(index.intValue()) : "");
                        }

                        writeCsvRow(writerHolder[0], outputRow);
                        matches[0]++;
                    } catch (Exception e) {
                        runtimeError[0] = new RuntimeException(e);
                        throw runtimeError[0];
                    }
                });

            parser.parse(baseCsv);

            if (runtimeError[0] != null) {
                throw runtimeError[0];
            }

            writer.flush();
            zipOut.closeEntry();

            System.out.println("Coincidencias encontradas: " + matches[0]);
        }
    }

    private static UserRequest parseUserRequest(byte[] bytes) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        List<String> trimmed = new ArrayList<>(lines.size());
        for (String line : lines) {
            trimmed.add(line == null ? "" : line.trim());
        }

        int columnsMarker = findMarker(trimmed, "[COLUMNS]");
        int skusMarker = findMarker(trimmed, "[SKUS]");

        if (columnsMarker >= 0 && skusMarker > columnsMarker) {
            List<String> columns = collectNonBlank(trimmed, columnsMarker + 1, skusMarker);
            List<String> skus = collectNonBlank(trimmed, skusMarker + 1, trimmed.size());
            return new UserRequest(columns, skus);
        }

        List<List<String>> blocks = splitByBlankLines(trimmed);
        if (blocks.size() < 2) {
            throw new IllegalArgumentException("El archivo debe contener 2 secciones: columnas y SKUs");
        }

        return new UserRequest(blocks.get(0), blocks.get(1));
    }

    private static void validateUserRequest(UserRequest userRequest) {
        if (userRequest.getRequestedColumns().isEmpty()) {
            throw new IllegalArgumentException("No se encontraron columnas solicitadas");
        }

        if (userRequest.getSkus().isEmpty()) {
            throw new IllegalArgumentException("No se encontraron SKUs");
        }
    }

    private static String resolveRealSkuHeader(List<String> header, Map<String, Integer> headerIndexes) {
        for (String candidate : SKU_COLUMN_CANDIDATES) {
            Integer idx = headerIndexes.get(normalizeKey(candidate));
            if (idx != null) {
                return header.get(idx.intValue());
            }
        }

        throw new IllegalStateException(
            "El archivo base no contiene ninguna de las columnas SKU esperadas: "
            + Arrays.toString(SKU_COLUMN_CANDIDATES)
            + ". Header real: " + header
        );
    }

    private static List<String> buildOutputColumns(
        List<String> requestedColumns,
        List<String> header,
        Map<String, Integer> headerIndexes,
        String realSkuHeader
    ) {
        List<String> outputColumns = new ArrayList<>();

        if (!containsIgnoreCase(requestedColumns, realSkuHeader)) {
            outputColumns.add(realSkuHeader);
        }

        for (String requestedColumn : requestedColumns) {
            Integer index = headerIndexes.get(normalizeKey(requestedColumn));

            if (index == null) {
                throw new IllegalArgumentException(
                    "La columna solicitada no existe en el archivo base: " + requestedColumn
                    + ". Header real: " + header
                );
            }

            String realHeaderName = header.get(index.intValue());

            if (!containsIgnoreCase(outputColumns, realHeaderName)) {
                outputColumns.add(realHeaderName);
            }
        }

        return outputColumns;
    }

    private static List<Integer> resolveOutputIndexes(List<String> outputColumns, Map<String, Integer> headerIndexes) {
        List<Integer> indexes = new ArrayList<>(outputColumns.size());

        for (String outputColumn : outputColumns) {
            Integer index = headerIndexes.get(normalizeKey(outputColumn));

            if (index == null) {
                throw new IllegalStateException("No se pudo resolver el índice para columna de salida: " + outputColumn);
            }

            indexes.add(index);
        }

        return indexes;
    }

    private static Map<String, Integer> buildHeaderIndexes(List<String> header) {
        Map<String, Integer> indexes = new HashMap<>();

        for (int i = 0; i < header.size(); i++) {
            indexes.put(normalizeKey(header.get(i)), Integer.valueOf(i));
        }

        return indexes;
    }

    private static String normalizeKey(String value) {
        String normalized = value == null ? "" : value.trim();
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        return normalized.toUpperCase();
    }

    private static boolean containsIgnoreCase(List<String> values, String expected) {
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    private static int findMarker(List<String> lines, String marker) {
        for (int i = 0; i < lines.size(); i++) {
            if (marker.equalsIgnoreCase(lines.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> collectNonBlank(List<String> lines, int fromInclusive, int toExclusive) {
        List<String> values = new ArrayList<>();

        for (int i = fromInclusive; i < toExclusive; i++) {
            String line = lines.get(i);

            if (line != null && !line.trim().isEmpty()) {
                values.add(line.trim());
            }
        }

        return values;
    }

    private static List<List<String>> splitByBlankLines(List<String> lines) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                if (!current.isEmpty()) {
                    blocks.add(new ArrayList<>(current));
                    current.clear();
                }
            } else {
                current.add(line.trim());
            }
        }

        if (!current.isEmpty()) {
            blocks.add(new ArrayList<>(current));
        }

        return blocks;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
        writer.write(serializeExcelChunk(values.toArray(new Object[0])));
        writer.write("\r\n");
    }
    

	private static String serializeExcelLine(String value, String delimiter, String separator) {
	  if (value == null) {
	    return "";
	  }

	  boolean mustQuote =
	      value.contains(separator)
	      || value.contains(delimiter)
	      || value.contains("\n")
	      || value.contains("\r");

	  String escaped = value.replace(delimiter, delimiter + delimiter);

	  return mustQuote ? delimiter + escaped + delimiter : escaped;
	}

	private static String serializeExcelChunk(Object[] pieces) {
	  return serializeExcelChunk(pieces, "\"", ",");
	}

	private static String serializeExcelChunk(Object[] pieces, String delimiter, String separator) {
	  StringBuilder sb = new StringBuilder();

	  for (int i = 0; i < pieces.length; i++) {
	    if (i > 0) {
	      sb.append(separator);
	    }
	    sb.append(serializeExcelLine(String.valueOf(pieces[i]), delimiter, separator));
	  }

	  return sb.toString();
	}

    private static final class UserRequest {
        private final List<String> requestedColumns;
        private final Set<String> skus;

        private UserRequest(List<String> requestedColumns, List<String> skus) {
            this.requestedColumns = new ArrayList<>();

            for (String value : requestedColumns) {
                if (value != null && !value.trim().isEmpty()) {
                    this.requestedColumns.add(value.trim());
                }
            }

            this.skus = new LinkedHashSet<>();

            for (String value : skus) {
                if (value != null && !value.trim().isEmpty()) {
                    this.skus.add(value.trim());
                }
            }
        }

        public List<String> getRequestedColumns() {
            return this.requestedColumns;
        }

        public Set<String> getSkus() {
            return this.skus;
        }
    }

    private static final class CsvRecordReader implements AutoCloseable {
        private final Reader reader;
        private int pushback = -2;
        private boolean eof;

        private CsvRecordReader(Reader reader) {
            this.reader = reader;
        }

        public List<String> readRecord() throws IOException {
            if (eof) {
                return null;
            }

            List<String> values = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean sawAnyChar = false;

            while (true) {
                int ch = read();
                if (ch == -1) {
                    eof = true;
                    if (!sawAnyChar && current.length() == 0 && values.isEmpty()) {
                        return null;
                    }
                    values.add(current.toString());
                    return values;
                }

                sawAnyChar = true;
                char c = (char) ch;

                if (inQuotes) {
                    if (c == '"') {
                        int next = read();
                        if (next == '"') {
                            current.append('"');
                        } else {
                            inQuotes = false;
                            unread(next);
                        }
                    } else {
                        current.append(c);
                    }
                    continue;
                }

                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    values.add(current.toString());
                    current.setLength(0);
                } else if (c == '\n') {
                    values.add(current.toString());
                    return values;
                } else if (c == '\r') {
                    int next = read();
                    if (next != '\n') {
                        unread(next);
                    }
                    values.add(current.toString());
                    return values;
                } else {
                    current.append(c);
                }
            }
        }

        private int read() throws IOException {
            if (pushback != -2) {
                int tmp = pushback;
                pushback = -2;
                return tmp;
            }
            return reader.read();
        }

        private void unread(int ch) {
            pushback = ch;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}