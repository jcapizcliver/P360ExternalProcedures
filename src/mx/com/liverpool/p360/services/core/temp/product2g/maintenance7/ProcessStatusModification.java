package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ProcessStatusModification {

    private static final Pattern PATTERN_STATUS_ES = Pattern.compile(
            "estado\\s+\"([^\"]+)\"\\s+el\\s+(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern PATTERN_STATUS_EN = Pattern.compile(
            "status\\s+\"([^\"]+)\"\\s+(?:on|at)\\s+(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter
            .ofPattern("d/M/uuuu H:mm", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter OUTPUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT);

    private static final Map<String, String> EN_TO_ES = createEnglishToSpanishMap();

    public static void main(String[] args) throws IOException {
        Path input = args.length >= 1
                ? Paths.get(args[0])
                : Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Data_SM_MKP_20260717_024435.csv");
//        : Paths.get("C:", "opt", "LVP", "desorden", "PROD", "Data_SM_20260713_172029.csv");

        Path outputDirectory = args.length >= 2
                ? Paths.get(args[1])
                : input.toAbsolutePath().getParent();

        if (outputDirectory == null) {
            outputDirectory = Paths.get(".").toAbsolutePath().normalize();
        }

        Files.createDirectories(outputDirectory);

        Processor processor = new Processor(outputDirectory);

        try (processor) {
            SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                    '"',
                    ',',
                    '\\',
                    "\n",
                    StandardCharsets.UTF_8,
                    processor::accept);

            parser.parse(input);
            processor.writeSummaries();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        System.out.println("Archivo procesado: " + input.toAbsolutePath());
        System.out.println("Directorio de salida: " + outputDirectory.toAbsolutePath());
        System.out.println("Productos leídos: " + processor.getProductCount());
        System.out.println("Ingresos a estados detectados: " + processor.getStatusEntryCount());
        System.out.println("Transiciones detectadas: " + processor.getTransitionCount());
        System.out.println("Líneas no reconocidas: " + processor.getUnrecognizedLineCount());
    }

    private static final class Processor implements AutoCloseable {

        private final Path outputDirectory;
        private final Map<String, Integer> columnIndexes = new HashMap<>();
        private final Map<String, Long> entriesByStatus = new TreeMap<>();
        private final Map<TransitionKey, TransitionStats> transitionStats = new HashMap<>();

        private final BufferedWriter detailWriter;
        private final BufferedWriter errorWriter;

        private boolean headerRead;
        private long productCount;
        private long statusEntryCount;
        private long transitionCount;
        private long unrecognizedLineCount;

        private Processor(Path outputDirectory) throws IOException {
            this.outputDirectory = outputDirectory;
            this.detailWriter = newUtf8CsvWriter(outputDirectory.resolve("detalle_transiciones_estado.csv"));
            this.errorWriter = newUtf8CsvWriter(outputDirectory.resolve("lineas_status_no_reconocidas.csv"));

            writeCsvRow(detailWriter,
                    "Identifier",
                    "EAN",
                    "SKU",
                    "EstadoOrigen",
                    "FechaOrigen",
                    "EstadoDestino",
                    "FechaDestino",
                    "DuracionSegundos",
                    "DuracionHoras",
                    "DuracionExcel");

            writeCsvRow(errorWriter,
                    "Identifier",
                    "LineaNoReconocida");
        }

        private void accept(String[] row) {
            try {
                if (row == null || row.length == 0) {
                    return;
                }

                if (!headerRead) {
                    readHeader(row);
                    return;
                }

                processProduct(row);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void readHeader(String[] header) {
            for (int i = 0; i < header.length; i++) {
                String columnName = header[i] == null ? "" : header[i].trim();
                if (i == 0 && columnName.startsWith("\uFEFF")) {
                    columnName = columnName.substring(1);
                }
                columnIndexes.put(columnName, i);
            }

            requireColumn("Identifier");
            requireColumn("EAN");
            requireColumn("SKU");
            requireColumn("StatusModification");

            headerRead = true;
        }

        private void requireColumn(String columnName) {
            if (!columnIndexes.containsKey(columnName)) {
                throw new IllegalArgumentException("No se encontró la columna requerida: " + columnName);
            }
        }

        private void processProduct(String[] row) throws IOException {
            productCount++;

            String identifier = getColumn(row, "Identifier");
            String ean = getColumn(row, "EAN");
            String sku = getColumn(row, "SKU");
            String statusModification = getColumn(row, "StatusModification");

            if (statusModification.isBlank()) {
                return;
            }

            List<StatusChange> changes = parseChanges(identifier, statusModification);
            changes.sort(Comparator.comparing(StatusChange::timestamp));

            for (StatusChange change : changes) {
                entriesByStatus.merge(change.status(), 1L, Long::sum);
                statusEntryCount++;
            }

            for (int i = 1; i < changes.size(); i++) {
                StatusChange origin = changes.get(i - 1);
                StatusChange destination = changes.get(i);
                long durationSeconds = Duration.between(origin.timestamp(), destination.timestamp()).getSeconds();

                if (durationSeconds < 0) {
                    writeCsvRow(errorWriter,
                            identifier,
                            "Fecha destino anterior a fecha origen: "
                                    + origin.status() + " " + origin.timestamp()
                                    + " -> "
                                    + destination.status() + " " + destination.timestamp());
                    unrecognizedLineCount++;
                    continue;
                }

                TransitionKey key = new TransitionKey(origin.status(), destination.status());
                transitionStats.computeIfAbsent(key, ignored -> new TransitionStats())
                        .add(durationSeconds);
                transitionCount++;

                writeCsvRow(detailWriter,
                        identifier,
                        ean,
                        sku,
                        origin.status(),
                        OUTPUT_DATE_FORMAT.format(origin.timestamp()),
                        destination.status(),
                        OUTPUT_DATE_FORMAT.format(destination.timestamp()),
                        Long.toString(durationSeconds),
                        decimal(durationSeconds / 3600.0),
                        decimal(durationSeconds / 86400.0));
            }
        }

        private List<StatusChange> parseChanges(String identifier, String statusModification) throws IOException {
            List<StatusChange> changes = new ArrayList<>();

            for (String rawLine : statusModification.split("\\R")) {
                String line = rawLine == null ? "" : rawLine.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.toLowerCase(Locale.ROOT).contains("el estado fue eliminado")
                        || line.toLowerCase(Locale.ROOT).contains("the status was deleted")) {
                    continue;
                }

                Matcher matcherEs = PATTERN_STATUS_ES.matcher(line);
                if (matcherEs.find()) {
                    addChange(identifier, line, matcherEs.group(1), matcherEs.group(2), changes);
                    continue;
                }

                Matcher matcherEn = PATTERN_STATUS_EN.matcher(line);
                if (matcherEn.find()) {
                    String statusEs = EN_TO_ES.get(matcherEn.group(1));

                    if (statusEs == null) {
                        writeCsvRow(errorWriter,
                                identifier,
                                "No existe conversión al español para el estado: " + matcherEn.group(1));
                        unrecognizedLineCount++;
                        continue;
                    }

                    addChange(identifier, line, statusEs, matcherEn.group(2), changes);
                    continue;
                }

                writeCsvRow(errorWriter, identifier, line);
                unrecognizedLineCount++;
            }

            return changes;
        }

        private void addChange(
                String identifier,
                String completeLine,
                String status,
                String dateText,
                List<StatusChange> changes) throws IOException {
            try {
                LocalDateTime timestamp = LocalDateTime.parse(dateText, INPUT_DATE_FORMAT);
                changes.add(new StatusChange(status, timestamp));
            } catch (DateTimeParseException e) {
                writeCsvRow(errorWriter,
                        identifier,
                        "Fecha no reconocida en: " + completeLine);
                unrecognizedLineCount++;
            }
        }

        private void writeSummaries() throws IOException {
            writeEntriesByStatus();
            writeTransitionSummary();
        }

        private void writeEntriesByStatus() throws IOException {
            Path output = outputDirectory.resolve("cantidad_ingresos_estado.csv");

            try (BufferedWriter writer = newUtf8CsvWriter(output)) {
                writeCsvRow(writer,
                        "Estado",
                        "CantidadIngresos");

                for (Map.Entry<String, Long> entry : entriesByStatus.entrySet()) {
                    writeCsvRow(writer,
                            entry.getKey(),
                            Long.toString(entry.getValue()));
                }
            }
        }

        private void writeTransitionSummary() throws IOException {
            Path output = outputDirectory.resolve("tiempo_promedio_entre_estados.csv");

            try (BufferedWriter writer = newUtf8CsvWriter(output)) {
                writeCsvRow(writer,
                        "EstadoOrigen",
                        "EstadoDestino",
                        "CantidadTransiciones",
                        "TiempoPromedioSegundos",
                        "TiempoPromedioHoras",
                        "TiempoPromedioExcel",
                        "TiempoMinimoSegundos",
                        "TiempoMaximoSegundos");

                transitionStats.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            TransitionKey key = entry.getKey();
                            TransitionStats stats = entry.getValue();
                            double averageSeconds = stats.averageSeconds();

                            try {
                                writeCsvRow(writer,
                                        key.origin,
                                        key.destination,
                                        Long.toString(stats.count),
                                        decimal(averageSeconds),
                                        decimal(averageSeconds / 3600.0),
                                        decimal(averageSeconds / 86400.0),
                                        Long.toString(stats.minimumSeconds),
                                        Long.toString(stats.maximumSeconds));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }

        private String getColumn(String[] row, String columnName) {
            int index = columnIndexes.get(columnName);
            return index < row.length && row[index] != null ? row[index] : "";
        }

        private long getProductCount() {
            return productCount;
        }

        private long getStatusEntryCount() {
            return statusEntryCount;
        }

        private long getTransitionCount() {
            return transitionCount;
        }

        private long getUnrecognizedLineCount() {
            return unrecognizedLineCount;
        }

        @Override
        public void close() throws IOException {
            IOException first = null;

            try {
                detailWriter.close();
            } catch (IOException e) {
                first = e;
            }

            try {
                errorWriter.close();
            } catch (IOException e) {
                if (first == null) {
                    first = e;
                } else {
                    first.addSuppressed(e);
                }
            }

            if (first != null) {
                throw first;
            }
        }
    }

    private static final class StatusChange {
        private final String status;
        private final LocalDateTime timestamp;

        private StatusChange(String status, LocalDateTime timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        private String status() {
            return status;
        }

        private LocalDateTime timestamp() {
            return timestamp;
        }
    }

    private static final class TransitionKey implements Comparable<TransitionKey> {
        private final String origin;
        private final String destination;

        private TransitionKey(String origin, String destination) {
            this.origin = origin;
            this.destination = destination;
        }

        @Override
        public int compareTo(TransitionKey other) {
            int originComparison = origin.compareTo(other.origin);
            return originComparison != 0
                    ? originComparison
                    : destination.compareTo(other.destination);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof TransitionKey)) {
                return false;
            }
            TransitionKey other = (TransitionKey) object;
            return Objects.equals(origin, other.origin)
                    && Objects.equals(destination, other.destination);
        }

        @Override
        public int hashCode() {
            return Objects.hash(origin, destination);
        }
    }

    private static final class TransitionStats {
        private long count;
        private long totalSeconds;
        private long minimumSeconds = Long.MAX_VALUE;
        private long maximumSeconds = Long.MIN_VALUE;

        private void add(long seconds) {
            count++;
            totalSeconds += seconds;
            minimumSeconds = Math.min(minimumSeconds, seconds);
            maximumSeconds = Math.max(maximumSeconds, seconds);
        }

        private double averageSeconds() {
            return count == 0 ? 0.0 : (double) totalSeconds / count;
        }
    }

    private static BufferedWriter newUtf8CsvWriter(Path path) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        return writer;
    }

    private static void writeCsvRow(BufferedWriter writer, String... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escapeCsv(values[i]));
        }
        writer.newLine();
    }

    private static String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        boolean quote = safeValue.indexOf(',') >= 0
                || safeValue.indexOf('"') >= 0
                || safeValue.indexOf('\n') >= 0
                || safeValue.indexOf('\r') >= 0;

        if (!quote) {
            return safeValue;
        }

        return '"' + safeValue.replace("\"", "\"\"") + '"';
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.10f", value);
    }

    private static Map<String, String> createEnglishToSpanishMap() {
        Map<String, String> enToEs = new HashMap<>();
        enToEs.put("Canceled", "Cancelado");
        enToEs.put("Data Gobernance", "Gobierno de Datos");
        enToEs.put("Draft", "Borrador");
        enToEs.put("In Foro Process", "En Proceso Foro");
        enToEs.put("Purchase Rejected", "Rechazo Compras");
        enToEs.put("Purchase Revision", "Revisión Compras");
        enToEs.put("QA Revision", "Revisión QA");
        enToEs.put("SKU Creation", "Creación de SKU");
        enToEs.put("Proposal Generated", "Propuesta Generada");
        enToEs.put("Pending Enrichment", "Pendiente Inicio Enriquecimiento");
        enToEs.put("Image Load", "Carga de Imagen");
        enToEs.put("Rejected", "Rechazada");
        enToEs.put("To Be Updated", "Por Actualizar");
        enToEs.put("Approved", "Aprobada");
        enToEs.put("Modified", "Modificación");
        enToEs.put("Liverpool in progress", "En Proceso Liverpool");
        enToEs.put("Sending in progress", "En Proceso de Envío");
        enToEs.put("Publish Rejected", "Rechazo Publicación");
        enToEs.put("Deleted", "Eliminada");
        enToEs.put("Repopulation", "Repoblamiento");
        enToEs.put("Cataloguing Exception", "Excepción de Catalogación");
        enToEs.put("Accepted", "Aceptado");
        enToEs.put("Accepted with arrangements", "Aceptado con ajustes");
        return enToEs;
    }
}