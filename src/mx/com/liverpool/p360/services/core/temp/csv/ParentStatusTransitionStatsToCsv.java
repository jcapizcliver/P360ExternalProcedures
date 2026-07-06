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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ParentStatusTransitionStatsToCsv {

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
                : Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_20260410_112847_status_transition_stats.csv");

        if (output.toAbsolutePath().getParent() != null) {
            Files.createDirectories(output.toAbsolutePath().getParent());
        }

        final Map<String, Integer> header = new LinkedHashMap<>();
        final Map<Key, Stats> statsByKey = new LinkedHashMap<>();
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

                    String negocioPapa = trimToEmpty(get(values, header, "NegocioPapá"));
                    String bitacoraPapa = get(values, header, "BitácoraPapá");

                    if (bitacoraPapa == null || bitacoraPapa.isEmpty()) {
                        return;
                    }

                    processParentLog(negocioPapa, bitacoraPapa, statsByKey);
                }
        );

        parser.parse(input);

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writeRow(writer, new String[] {
                    "NegocioPapá",
                    "EstadoOrigen",
                    "EstadoDestino",
                    "Conteo",
                    "TiempoTotalMin",
                    "TiempoPromedioMin",
                    "MedianaMin",
                    "P95Min",
                    "Min",
                    "Max"
            });

            for (Map.Entry<Key, Stats> e : statsByKey.entrySet()) {
                Key k = e.getKey();
                Stats s = e.getValue();

                writeRow(writer, new String[] {
                        k.negocioPapa,
                        k.estadoOrigen,
                        k.estadoDestino,
                        String.valueOf(s.count()),
                        String.valueOf(s.total()),
                        format2(s.average()),
                        format2(s.median()),
                        format2(s.p95()),
                        String.valueOf(s.min()),
                        String.valueOf(s.max())
                });
            }

            writer.flush();
        }

        System.out.println("Archivo generado: " + output.toAbsolutePath());
        System.out.println("Grupos generados: " + statsByKey.size());
    }
    
    private static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }

        switch (status.trim()) {
            case "Propuesta Generada":
            case "Proposal Generated":
                return "Propuesta Generada";

            case "Purchase Revision":
            case "Revisión Compras":
            	return "Revisión Compras";
                
            case "Carga de Imagen":
            case "Image Load":
                return "Carga de Imagen";

            case "Aprobada":
            case "Approved":
                return "Aprobada";

            case "Data Governance":
            case "Data Gobernance":
            case "Gobierno de Datos":
            	return "Gobierno de Datos";
                
            case "Revisión QA":
            case "QA Revision":
                return "Revisión QA";

            case "Category":
                return "Category";

            default:
                return status.trim();
        }
    }

    private static void processParentLog(String negocioPapa, String bitacoraPapa, Map<Key, Stats> statsByKey) {
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

            Key key = new Key(negocioPapa, older.status, newer.status);
            Stats stats = statsByKey.get(key);
            if (stats == null) {
                stats = new Stats();
                statsByKey.put(key, stats);
            }
            stats.add(minutes);
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
            String status = normalizeStatus(es.group(1));
            String ts = es.group(2);
            return new Event(status, LocalDateTime.parse(ts, ES_FMT));
        }

        Matcher en = EN_PATTERN.matcher(s);
        if (en.matches()) {
            String status = normalizeStatus(en.group(1));
            String ts = en.group(2);
            LocalDateTime dt = containsAmPm(ts)
                    ? LocalDateTime.parse(ts, EN_FMT_12H)
                    : LocalDateTime.parse(ts, EN_FMT_24H);
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
                "NegocioPapá",
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

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String format2(double v) {
        return String.format(Locale.US, "%.2f", v);
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
        private final String negocioPapa;
        private final String estadoOrigen;
        private final String estadoDestino;

        private Key(String negocioPapa, String estadoOrigen, String estadoDestino) {
            this.negocioPapa = negocioPapa;
            this.estadoOrigen = estadoOrigen;
            this.estadoDestino = estadoDestino;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return java.util.Objects.equals(negocioPapa, key.negocioPapa)
                    && java.util.Objects.equals(estadoOrigen, key.estadoOrigen)
                    && java.util.Objects.equals(estadoDestino, key.estadoDestino);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(negocioPapa, estadoOrigen, estadoDestino);
        }
    }

    private static final class Stats {
        private final List<Long> values = new ArrayList<>();
        private long total = 0;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;
        private boolean sorted = false;

        private void add(long minutes) {
            values.add(minutes);
            total += minutes;
            if (minutes < min) min = minutes;
            if (minutes > max) max = minutes;
            sorted = false;
        }

        private int count() {
            return values.size();
        }

        private long total() {
            return total;
        }

        private long min() {
            return values.isEmpty() ? 0 : min;
        }

        private long max() {
            return values.isEmpty() ? 0 : max;
        }

        private double average() {
            return values.isEmpty() ? 0.0 : ((double) total / (double) values.size());
        }

        private double median() {
            if (values.isEmpty()) return 0.0;
            ensureSorted();
            int n = values.size();
            if ((n & 1) == 1) {
                return values.get(n / 2);
            }
            return (values.get((n / 2) - 1) + values.get(n / 2)) / 2.0;
        }

        private double p95() {
            if (values.isEmpty()) return 0.0;
            ensureSorted();
            int n = values.size();
            int idx = (int) Math.ceil(0.95 * n) - 1;
            if (idx < 0) idx = 0;
            if (idx >= n) idx = n - 1;
            return values.get(idx);
        }

        private void ensureSorted() {
            if (!sorted) {
                values.sort(Long::compareTo);
                sorted = true;
            }
        }
    }
}