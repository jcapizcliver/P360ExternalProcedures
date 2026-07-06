package mx.com.liverpool.p360.services.core.temp.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class FilterParentTripleMatchesToCsv {

    private static final Timestamp MIN_PARENT_CREATION_TS = Timestamp.valueOf("2026-01-01 00:00:00");

    public static void main(String[] args) throws Exception {
        Path input = args != null && args.length > 0 && args[0] != null && !args[0].isBlank()
                ? Paths.get(args[0].trim())
                : Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260409_210116.csv");

        Path output = args != null && args.length > 1 && args[1] != null && !args[1].isBlank()
                ? Paths.get(args[1].trim())
                : Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260409_210116_matches.csv");

        Files.createDirectories(output.toAbsolutePath().getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8)) {

            final int[] rowNum = {0};
            final int[] written = {0};
            final Map<String, Integer> header = new HashMap<>();

            SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                    '"',
                    ',',
                    '\\',
                    "\n",
                    StandardCharsets.UTF_8,
                    values -> {
                        try {
                            rowNum[0]++;

                            if (rowNum[0] == 1) {
                                for (int i = 0; i < values.length; i++) {
                                    header.put(values[i], i);
                                }
                                validateHeader(header);
                                writeRow(writer, values);
                                written[0]++;
                                return;
                            }

                            String creationTsPapa = get(values, header, "CreationTimestampPapá");
                            String bitacoraPapa = get(values, header, "BitácoraPapá");

                            if (isOnOrAfterMinTimestamp(creationTsPapa) && containsDesiredTriple(bitacoraPapa)) {
                                writeRow(writer, values);
                                written[0]++;
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            parser.parse(input);
            writer.flush();

            System.out.println("Archivo generado: " + output.toAbsolutePath());
            System.out.println("Filas escritas (incluyendo header): " + written[0]);
        }
    }

    private static void validateHeader(Map<String, Integer> header) {
        String[] required = {
                "CreationTimestampPapá",
                "BitácoraPapá"
        };

        for (String col : required) {
            if (!header.containsKey(col)) {
                throw new IllegalStateException("No encontré la columna requerida: " + col);
            }
        }
    }

    private static String get(String[] values, Map<String, Integer> header, String col) {
        Integer idx = header.get(col);
        if (idx == null || idx < 0 || idx >= values.length) {
            return null;
        }
        return values[idx];
    }

    private static boolean isOnOrAfterMinTimestamp(String raw) {
//        if (raw == null || raw.isBlank()) {
//            return false;
//        }
    	return true;
//        String s = raw.trim();
//
//        try {
//            return Timestamp.valueOf(s.replace('T', ' ')).compareTo(MIN_PARENT_CREATION_TS) >= 0;
//        } catch (IllegalArgumentException e) {
//            throw new IllegalStateException("Timestamp inválido en CreationTimestampPapá: [" + raw + "]", e);
//        }
    }

    private static boolean containsDesiredTriple(String bitacora) {
        if (bitacora == null || bitacora.isEmpty()) {
            return false;
        }

        String normalized = bitacora
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        String[] lines = normalized.split("\n");

        for (int i = 0; i <= lines.length - 3; i++) {
            String s1 = extractStatus(lines[i]);
            String s2 = extractStatus(lines[i + 1]);
            String s3 = extractStatus(lines[i + 2]);

            if (isApproved(s1) && isCategory(s2) && isQaRevision(s3)) {
                return true;
            }
        }

        return false;
    }

    private static String extractStatus(String line) {
        if (line == null) {
            return null;
        }

        int firstQuote = line.indexOf('"');
        if (firstQuote < 0) {
            return null;
        }

        int secondQuote = line.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return null;
        }

        return line.substring(firstQuote + 1, secondQuote);
    }

    private static boolean isApproved(String s) {
        return true; //"Aprobada".equals(s) || "Approved".equals(s);
    }

    private static boolean isCategory(String s) {
        return true; //"Category".equals(s);
    }

    private static boolean isQaRevision(String s) {
        return true; //"Revisión QA".equals(s) || "QA Revision".equals(s);
    }

    private static void writeRow(BufferedWriter writer, String[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(csvEscape(values[i]));
        }
        writer.write(System.lineSeparator());
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

        String escaped = value.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }
}