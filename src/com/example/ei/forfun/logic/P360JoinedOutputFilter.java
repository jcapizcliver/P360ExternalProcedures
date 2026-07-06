package com.example.ei.forfun.logic;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class P360JoinedOutputFilter {

    private static final String SEP = ",";
    private static final String EOL = "\n";
    private static final String PRODUCT_IDENTIFIER_COLUMN = "Product2G_ProductNo";
private static final String DELIMITER = "\"";
private static final String SEPARATOR = ",";
private static final String ESCAPE = "\\";

public String serializeLine(String value) {
    try {
        return value == null ? "" :
                value.contains(SEPARATOR) ||
                value.contains(DELIMITER) ||
                value.contains("\\".equals(ESCAPE) ? "\\" : ESCAPE) ||
                value.contains("\n")
                        ? DELIMITER + value.replaceAll(
                                "(?=[" + DELIMITER + ("\\".equals(ESCAPE) ? "\\\\" : ESCAPE) + "])",
                                "\\".equals(ESCAPE) ? "\\\\" : ESCAPE
                        ) + DELIMITER
                        : value;
    } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
    }
}

public String serializeChunk(Object[] pieces) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pieces.length; i++) {
        sb.append(i == 0 ? "" : SEPARATOR)
          .append(serializeLine(String.valueOf(pieces[i])));
    }
    return sb.toString();
}

    public void filterByRequestedProducts(
            Path requestedProductsFile,
            Path joinedOutputFile,
            Path outputFile) throws IOException {

        Set<String> requestedIds = loadRequestedIds(requestedProductsFile);
        if (requestedIds.isEmpty()) {
            throw new IllegalStateException("No se encontraron identificadores de producto en el archivo de entrada");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            final boolean[] firstRow = {true};
            final int[] productColumnIndex = {-1};
            final long[] matched = {0};

            SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                    '"',
                    ',',
                    '\\',
                    "\n",
                    StandardCharsets.UTF_8,
                    values -> {
                        if (values == null || values.length == 0) {
                            return;
                        }

                        if (firstRow[0]) {
                            firstRow[0] = false;
                            productColumnIndex[0] = findColumnIndex(values, PRODUCT_IDENTIFIER_COLUMN);
                            if (productColumnIndex[0] < 0) {
                                throw new IllegalStateException(
                                        "No se encontró la columna \"" + PRODUCT_IDENTIFIER_COLUMN + "\" en el archivo del molote"
                                );
                            }

                            writeCsvLine(writer, values);
                            return;
                        }

                        String productId = get(values, productColumnIndex[0]).trim();
                        if (requestedIds.contains(productId)) {
                            writeCsvLine(writer, values);
                            matched[0]++;
                        }
                    }
            );

            parser.parse(joinedOutputFile);
            writer.flush();

            System.out.println("Productos solicitados: " + requestedIds.size());
            System.out.println("Filas coincidentes: " + matched[0]);
            System.out.println("Archivo generado: " + outputFile.toAbsolutePath());
        }
    }

    private Set<String> loadRequestedIds(Path requestedProductsFile) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        final boolean[] firstRow = {true};

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                '"',
                ',',
                '\\',
                "\n",
                StandardCharsets.UTF_8,
                values -> {
                    if (values == null || values.length == 0) {
                        return;
                    }

                    String first = get(values, 0).trim();
                    if (first.isEmpty()) {
                        return;
                    }

                    if (firstRow[0]) {
                        firstRow[0] = false;
                        if (PRODUCT_IDENTIFIER_COLUMN.equalsIgnoreCase(first)
                                || "ProductNo".equalsIgnoreCase(first)
                                || "Identificador".equalsIgnoreCase(first)
                                || "IdentificadorProducto".equalsIgnoreCase(first)) {
                            return;
                        }
                    }

                    ids.add(first);
                }
        );

        parser.parse(requestedProductsFile);
        return ids;
    }

    private int findColumnIndex(String[] headers, String expectedHeader) {
        for (int i = 0; i < headers.length; i++) {
            if (expectedHeader.equals(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private String get(String[] arr, int index) {
        if (arr == null || index < 0 || index >= arr.length || arr[index] == null) {
            return "";
        }
        return arr[index];
    }

    private void writeCsvLine(BufferedWriter writer, String[] values) {
        try {
		 writer.write(serializeChunk(values));
        	writer.write(EOL);
        } catch (IOException e) {
            throw new RuntimeException("Error escribiendo CSV", e);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 3) {
            throw new IllegalArgumentException(
                    "Uso: P360JoinedOutputFilter <archivo_productos.csv> <molote.csv> <salida.csv>"
            );
        }

        Path requestedProductsFile = Path.of(args[0]);
        Path joinedOutputFile = Path.of(args[1]);
        Path outputFile = Path.of(args[2]);

        new P360JoinedOutputFilter().filterByRequestedProducts(
                requestedProductsFile,
                joinedOutputFile,
                outputFile
        );
    }
}
