package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses the placeholder status file and appends row-level errors to the backup
 * content.
 */
public class PlaceholderStatusFileProcessor {

    public static final String ID_COLUMN = "id_placeholder";
    public static final String STATUS_COLUMN = "status_of_the_placeholder";
    public static final String COMMENTS_COLUMN = "comments";
    public static final String ERRORS_COLUMN = "errors";

    private final PlaceholderStatusUpdater updater;
    private final PlaceholderStatusDictionary statusDictionary;
    private final Logger logger;

    public PlaceholderStatusFileProcessor(PlaceholderStatusUpdater updater, Logger logger) {
        this.updater = updater;
        this.statusDictionary = new PlaceholderStatusDictionary();
        this.logger = logger;
    }

    public ProcessedFile process(InputStream inputStream) throws IOException {
        List<String> errorLines = new ArrayList<>();
        int idIndex = -1;
        int statusIndex = -1;
        int commentsIndex = -1;
        int rowsRead = 0;
        int processed = 0;
        int success = 0;
        int errors = 0;
        int ignored = 0;
        List<PreparedUpdate> preparedUpdates = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                List<String> values = parseCsvLine(line);

                if (lineNumber == 1) {
                    idIndex = indexOf(values, ID_COLUMN);
                    statusIndex = indexOf(values, STATUS_COLUMN);
                    commentsIndex = indexOf(values, COMMENTS_COLUMN);
                    if (idIndex < 0 || statusIndex < 0 || commentsIndex < 0) {
                        throw new IllegalArgumentException("Required columns not found: " + ID_COLUMN + ", "
                                + STATUS_COLUMN + ", " + COMMENTS_COLUMN);
                    }
                    errorLines.add(toCsvLineWithError(values, ERRORS_COLUMN));
                    continue;
                }

                rowsRead++;
                String error = "";
                String rawPlaceholderCode = getValue(values, idIndex);
                String status = getValue(values, statusIndex);
                String comments = getValue(values, commentsIndex);

                if (!isUsableValue(rawPlaceholderCode) || !isUsableValue(status)) {
                    ignored++;
                    logger.info("Ignoring placeholder row because required values are empty or invalid. id_placeholder="
                            + rawPlaceholderCode + ", status=" + status);
                    continue;
                }

                if (requiresComments(status) && !isUsableValue(comments)) {
                    error = "El campo comments es requerido para el estatus: " + status;
                    errors++;
                    errorLines.add(toCsvLineWithError(values, error));
                    logger.warning("Skipping placeholder row because comments is required. id_placeholder="
                            + rawPlaceholderCode + ", status=" + status);
                    continue;
                }

                processed++;
                try {
                    String p360Status = statusDictionary.resolve(status);
                    String action = statusDictionary.resolveAction(status);
                    if (p360Status == null || p360Status.trim().length() == 0) {
                        throw new IllegalStateException("Status value not found in dictionary: " + status);
                    }
                    if (action == null || action.trim().length() == 0) {
                        throw new IllegalStateException("Status action not found in dictionary: " + status);
                    }
                    String placeholderId = rawPlaceholderCode.trim();
                    logger.info("Prepared placeholder update: " + placeholderId + " fileStatus=" + status
                            + " p360Status=" + p360Status + " action=" + action);
                    preparedUpdates.add(new PreparedUpdate(values,
                            new PlaceholderStatusUpdate(placeholderId, p360Status, action, comments)));
                } catch (Exception e) {
                    error = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
                    logger.warning("Error processing placeholder source=" + rawPlaceholderCode + " error=" + error);
                }

                if (error.length() > 0) {
                    errors++;
                }
                if (error.length() > 0) {
                    errorLines.add(toCsvLineWithError(values, error));
                }
            }
        }

        if (!preparedUpdates.isEmpty()) {
            try {
                List<PlaceholderStatusUpdate> updates = new ArrayList<>();
                for (PreparedUpdate preparedUpdate : preparedUpdates) {
                    updates.add(preparedUpdate.getUpdate());
                }
                logger.info("Sending Product2G status update batch. rows=" + updates.size());
                updater.update(updates);
                success += updates.size();
            } catch (Exception e) {
                String error = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
                errors += preparedUpdates.size();
                logger.warning("Error sending Product2G status update batch. rows=" + preparedUpdates.size()
                        + " error=" + error);
                for (PreparedUpdate preparedUpdate : preparedUpdates) {
                    errorLines.add(toCsvLineWithError(preparedUpdate.getSourceValues(), error));
                }
            }
        }

        return new ProcessedFile(toFileContent(errorLines), rowsRead, processed, success, errors, ignored);
    }

    private static boolean isUsableValue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        if (normalized.length() == 0) {
            return false;
        }
        normalized = normalized.toUpperCase(java.util.Locale.ROOT);
        return !"NULL".equals(normalized)
                && !"UNDEFINED".equals(normalized)
                && !"NULO".equals(normalized)
                && !"NA".equals(normalized)
                && !"N/A".equals(normalized);
    }

    private static boolean requiresComments(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(java.util.Locale.ROOT);
        return "rechazado".equals(normalized)
                || "rechazado para modificación".equals(normalized);
    }

    private static String getValue(List<String> values, int index) {
        if (index < 0 || values.size() <= index) {
            return "";
        }
        return values.get(index).trim();
    }

    private static int indexOf(List<String> values, String columnName) {
        for (int i = 0; i < values.size(); i++) {
            if (columnName.equalsIgnoreCase(values.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    private static String toCsvLineWithError(List<String> values, String error) {
        List<String> output = new ArrayList<>(values);
        output.add(error);
        return toCsvLine(output);
    }

    private static String toFileContent(List<String> outputLines) {
        StringBuilder sb = new StringBuilder();
        for (String line : outputLines) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(line);
        }
        if (!outputLines.isEmpty()) {
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String toCsvLine(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(values.get(i)));
        }
        return sb.toString();
    }

    private static String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        boolean quote = safeValue.indexOf(',') >= 0 || safeValue.indexOf('"') >= 0
                || safeValue.indexOf('\n') >= 0 || safeValue.indexOf('\r') >= 0;
        if (!quote) {
            return safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    public static class ProcessedFile {
        private final String backupContent;
        private final int rowsRead;
        private final int processed;
        private final int success;
        private final int errors;
        private final int ignored;

        private ProcessedFile(String backupContent, int rowsRead, int processed, int success, int errors, int ignored) {
            this.backupContent = backupContent;
            this.rowsRead = rowsRead;
            this.processed = processed;
            this.success = success;
            this.errors = errors;
            this.ignored = ignored;
        }

        public String getBackupContent() {
            return backupContent;
        }

        public int getRowsRead() {
            return rowsRead;
        }

        public int getProcessed() {
            return processed;
        }

        public int getSuccess() {
            return success;
        }

        public int getErrors() {
            return errors;
        }

        public int getIgnored() {
            return ignored;
        }
    }

    private static class PreparedUpdate {
        private final List<String> sourceValues;
        private final PlaceholderStatusUpdate update;

        private PreparedUpdate(List<String> sourceValues, PlaceholderStatusUpdate update) {
            this.sourceValues = new ArrayList<>(sourceValues);
            this.update = update;
        }

        private List<String> getSourceValues() {
            return sourceValues;
        }

        private PlaceholderStatusUpdate getUpdate() {
            return update;
        }
    }
}
