package mx.com.liverpool.p360.services.core.temp.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ParentStatusTransitionTimesToCsv {

    private static final Pattern ES_PATTERN = Pattern.compile(
            "^El usuario .* ha establecido el estado \"([^\"]+)\" el ([0-9]{1,2}/[0-9]{1,2}/[0-9]{4} [0-9]{1,2}:[0-9]{2})\\.?$"
    );

    private static final Pattern EN_PATTERN = Pattern.compile(
            "^The user .* set the status \"([^\"]+)\" on ([0-9]{1,2}/[0-9]{1,2}/[0-9]{4} [0-9]{1,2}:[0-9]{2}(?: [AP]M)?)\\.?$"
    );

    private static final DateTimeFormatter ES_FMT =
            DateTimeFormatter.ofPattern("d/M/yyyy H:mm", new Locale("es", "MX"));

    private static final DateTimeFormatter EN_FMT_12H =
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm a", Locale.ENGLISH);

    private static final DateTimeFormatter EN_FMT_24H =
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm", Locale.ENGLISH);

    public static void main(String[] args) throws Exception {
        Path input = args != null && args.length > 0 && args[0] != null && !args[0].isBlank()
                ? Paths.get(args[0].trim())
                : Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260410_112847.csv");

        Path output = args != null && args.length > 1 && args[1] != null && !args[1].isBlank()
                ? Paths.get(args[1].trim())
                : Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260410_112847_status_transition_times.csv");

        if (output.toAbsolutePath().getParent() != null) {
            Files.createDirectories(output.toAbsolutePath().getParent());
        }

        final Map<String, Integer> header = new LinkedHashMap<>();
        final Map<Key, Agg> agg = new LinkedHashMap<>();
        final int[] rowNum = {0};

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                '"',
                ',',
                '\\',
                "\n",
                StandardCharsets.UTF_8,
                values -> {
                    rowNum[0]++;

                    if (rowNum[0] == 1) {
                        for (int i = 0; i < values.length; i++) {
                            header.put(values[i], i);
                        }
                        validateHeader(header);
                        return;
                    }

                    String idPapa = get(values, header, "IDPapá");
                    String bitacoraPapa = get(values, header, "BitácoraPapá");

                    if (idPapa == null || idPapa.isBlank() || bitacoraPapa == null || bitacoraPapa.isEmpty()) {
                        return;
                    }

                    processParentLog(idPapa, bitacoraPapa, agg);
                }
        );

        parser.parse(input);

        try (BufferedWriter writer = Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8)) {

            writeRow(writer, new String[] {
                    "IdentificadorPapá",
                    "EstadoOrigen",
                    "EstadoDestino",
                    "TiempoTotalMinutos",
                    "TiempoPromedioMinutos"
            });

            for (Map.Entry<Key, Agg> e : agg.entrySet()) {
                Key k = e.getKey();
                Agg a = e.getValue();

                String totalMinutes = String.valueOf(a.totalMinutes);
                String avgMinutes = formatAverage(a.totalMinutes, a.count);

                writeRow(writer, new String[] {
                        k.parentId,
                        k.originStatus,
                        k.destinationStatus,
                        totalMinutes,
                        avgMinutes
                });
            }

            writer.flush();
        }

        System.out.println("Archivo generado: " + output.toAbsolutePath());
        System.out.println("Grupos generados: " + agg.size());
    }

    private static void processParentLog(String idPapa, String bitacoraPapa, Map<Key, Agg> agg) {
        String normalized = bitacoraPapa
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        String[] lines = normalized.split("\n");
        Event[] events = new Event[lines.length];

        for (int i = 0; i < lines.length; i++) {
            events[i] = parseEvent(lines[i]);
        }

        for (int i = 0; i < events.length - 1; i++) {
            Event newer = events[i];
            Event older = events[i + 1];

            if (newer == null || older == null) {
                continue;
            }

            long minutes = Duration.between(older.timestamp, newer.timestamp).toMinutes();

            if (minutes < 0) {
                continue;
            }

            Key key = new Key(idPapa, older.status, newer.status);
            Agg a = agg.get(key);
            if (a == null) {
                a = new Agg();
                agg.put(key, a);
            }
            a.totalMinutes += minutes;
            a.count++;
        }
    }

    private static Event parseEvent(String line) {
        if (line == null) {
            return null;
        }

        String s = line.trim();
        if (s.isEmpty()) {
            return null;
        }

        Matcher es = ES_PATTERN.matcher(s);
        if (es.matches()) {
            String status = es.group(1);
            String ts = es.group(2);
            return new Event(status, LocalDateTime.parse(ts, ES_FMT));
        }

        Matcher en = EN_PATTERN.matcher(s);
        if (en.matches()) {
            String status = en.group(1);
            String ts = en.group(2);

            LocalDateTime dt;
            if (containsAmPm(ts)) {
                dt = LocalDateTime.parse(ts, EN_FMT_12H);
            } else {
                dt = LocalDateTime.parse(ts, EN_FMT_24H);
            }

            return new Event(status, dt);
        }

        return null;
    }

    private static boolean containsAmPm(String ts) {
        return ts != null && (
                ts.toUpperCase(Locale.ROOT).contains(" AM") ||
                ts.toUpperCase(Locale.ROOT).contains(" PM")
        );
    }

    private static void validateHeader(Map<String, Integer> header) {
        String[] required = {
                "IDPapá",
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

    private static String formatAverage(long totalMinutes, long count) {
        if (count <= 0) {
            return "0";
        }
        double avg = (double) totalMinutes / (double) count;
        return String.format(Locale.US, "%.2f", avg);
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

    private static final class Event {
        private final String status;
        private final LocalDateTime timestamp;

        private Event(String status, LocalDateTime timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    private static final class Key {
        private final String parentId;
        private final String originStatus;
        private final String destinationStatus;

        private Key(String parentId, String originStatus, String destinationStatus) {
            this.parentId = parentId;
            this.originStatus = originStatus;
            this.destinationStatus = destinationStatus;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return java.util.Objects.equals(parentId, key.parentId)
                    && java.util.Objects.equals(originStatus, key.originStatus)
                    && java.util.Objects.equals(destinationStatus, key.destinationStatus);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(parentId, originStatus, destinationStatus);
        }
    }

    private static final class Agg {
        private long totalMinutes;
        private long count;
    }
}