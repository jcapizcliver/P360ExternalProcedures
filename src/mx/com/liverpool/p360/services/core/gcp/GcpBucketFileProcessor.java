package mx.com.liverpool.p360.services.core.gcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * Processing entry point for files received from the GCP bucket flow.
 *
 * <p>The processor reads a CSV/text stream, extracts two columns from each data
 * row and uses them to update the product description in P360. The transport
 * concerns stay in {@link GcpBucketFileListener}; this class owns the file
 * mapping and P360 write logic.</p>
 *
 * <p>Configuration:</p>
 * <pre>
 * p360.contingency.gcp.product_description_update.code_column=vendor_article
 * p360.contingency.gcp.product_description_update.description_column=color
 * p360.contingency.gcp.product_description_update.description_identifier=Product2GLang.DescriptionLong(es)
 * p360.contingency.gcp.product_description_update.batch_size=2000
 * p360.contingency.gcp.product_description_update.dry_run=true
 * </pre>
 *
 * <p>{@code dry_run} defaults to true. Set it to false only after validating the
 * extracted code/description pairs with a real file.</p>
 */
public class GcpBucketFileProcessor {

    private static final String CONFIG_CODE_COLUMN = "p360.contingency.gcp.product_description_update.code_column";
    private static final String CONFIG_DESCRIPTION_COLUMN = "p360.contingency.gcp.product_description_update.description_column";
    private static final String CONFIG_DESCRIPTION_IDENTIFIER = "p360.contingency.gcp.product_description_update.description_identifier";
    private static final String CONFIG_BATCH_SIZE = "p360.contingency.gcp.product_description_update.batch_size";
    private static final String CONFIG_DRY_RUN = "p360.contingency.gcp.product_description_update.dry_run";

    private static final String DEFAULT_CODE_COLUMN = "vendor_article";
    private static final String DEFAULT_DESCRIPTION_COLUMN = "color";
    private static final String DEFAULT_DESCRIPTION_IDENTIFIER = "Product2GLang.DescriptionLong(es)";
    private static final int DEFAULT_BATCH_SIZE = 2000;

    /**
     * Processes one file stream.
     *
     * @param sourceName human-readable source, usually bucket/objectName.
     * @param inputStream file content; this method closes the stream.
     */
    public void process(String sourceName, InputStream inputStream) throws IOException {
        Config config = Config.load();
        Result result = processCsv(sourceName, inputStream, config);

        System.out.println("GCP file source: " + sourceName);
        System.out.println("Third row: " + (result.thirdRow == null ? "<not found>" : result.thirdRow));
        System.out.println("Rows read: " + result.rowsRead);
        System.out.println("Rows skipped: " + result.rowsSkipped);
        System.out.println("Rows ready for P360 update: " + result.rowsToUpdate);
        System.out.println("Dry run: " + config.dryRun);
    }

    private Result processCsv(String sourceName, InputStream inputStream, Config config) throws IOException {
        Result result = new Result();
        RESTWrapper rw = config.dryRun ? null : new RESTWrapper();
        org.json.JSONObject request = createRequest(config.descriptionIdentifier);
        org.json.JSONArray rows = request.getJSONArray("rows");
        Map<String, String> qp = new HashMap<>();
        qp.put("includeObjectsInProtocol", "false");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            int codeIndex = -1;
            int descriptionIndex = -1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 3) {
                    result.thirdRow = line;
                }

                List<String> values = parseCsvLine(line);
                if (lineNumber == 1) {
                    codeIndex = indexOf(values, config.codeColumn);
                    descriptionIndex = indexOf(values, config.descriptionColumn);
                    if (codeIndex < 0 || descriptionIndex < 0) {
                        throw new IllegalArgumentException("Required columns not found. codeColumn=" + config.codeColumn
                                + ", descriptionColumn=" + config.descriptionColumn + ", source=" + sourceName);
                    }
                    continue;
                }

                result.rowsRead++;
                if (values.size() <= codeIndex || values.size() <= descriptionIndex) {
                    result.rowsSkipped++;
                    continue;
                }

                String codeId = values.get(codeIndex).trim();
                String description = values.get(descriptionIndex).trim();
                if (codeId.length() == 0 || description.length() == 0) {
                    result.rowsSkipped++;
                    continue;
                }

                result.rowsToUpdate++;
                if (config.dryRun) {
                    logDryRun(result.rowsToUpdate, codeId, description);
                    continue;
                }

                rows.put(new org.json.JSONObject()
                        .put("object", new org.json.JSONObject().put("id", "'" + codeId + "'@1"))
                        .put("values", new org.json.JSONArray().put(description)));

                if (rows.length() >= config.batchSize) {
                    flush(rw, qp, request);
                }
            }
        }

        if (!config.dryRun && rows.length() > 0) {
            flush(rw, qp, request);
        }
        return result;
    }

    private org.json.JSONObject createRequest(String descriptionIdentifier) {
        org.json.JSONObject request = new org.json.JSONObject();
        org.json.JSONArray columns = new org.json.JSONArray();
        org.json.JSONArray rows = new org.json.JSONArray();
        request.put("columns", columns);
        request.put("rows", rows);
        columns.put(new org.json.JSONObject().put("identifier", descriptionIdentifier));
        return request;
    }

    private void flush(RESTWrapper rw, Map<String, String> qp, org.json.JSONObject request) {
        rw.writeData("list", "Product2G", null, qp, request, response -> {
            System.out.println("P360 update response: " + response);
        });
    }

    private static void logDryRun(int index, String codeId, String description) {
        if (index <= 10 || index % 1000 == 0) {
            System.out.println("DRY_RUN update #" + index + ": codeId=" + codeId + ", description=" + description);
        }
    }

    private static int indexOf(List<String> values, String columnName) {
        for (int i = 0; i < values.size(); i++) {
            if (columnName.equalsIgnoreCase(values.get(i).trim())) {
                return i;
            }
        }
        return -1;
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

    private static String getConfig(String key, String defaultValue) {
        try {
            String value = PropertiesManager.get(key);
            return value == null || value.trim().length() == 0 ? defaultValue : value.trim();
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static int getIntConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(getConfig(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean getBooleanConfig(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getConfig(key, String.valueOf(defaultValue)));
    }

    private static class Config {
        private final String codeColumn;
        private final String descriptionColumn;
        private final String descriptionIdentifier;
        private final int batchSize;
        private final boolean dryRun;

        private Config(String codeColumn, String descriptionColumn, String descriptionIdentifier, int batchSize,
                boolean dryRun) {
            this.codeColumn = codeColumn;
            this.descriptionColumn = descriptionColumn;
            this.descriptionIdentifier = descriptionIdentifier;
            this.batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
            this.dryRun = dryRun;
        }

        private static Config load() {
            return new Config(
                    getConfig(CONFIG_CODE_COLUMN, DEFAULT_CODE_COLUMN),
                    getConfig(CONFIG_DESCRIPTION_COLUMN, DEFAULT_DESCRIPTION_COLUMN),
                    getConfig(CONFIG_DESCRIPTION_IDENTIFIER, DEFAULT_DESCRIPTION_IDENTIFIER),
                    getIntConfig(CONFIG_BATCH_SIZE, DEFAULT_BATCH_SIZE),
                    getBooleanConfig(CONFIG_DRY_RUN, true));
        }
    }

    private static class Result {
        private String thirdRow;
        private int rowsRead;
        private int rowsSkipped;
        private int rowsToUpdate;
    }
}
