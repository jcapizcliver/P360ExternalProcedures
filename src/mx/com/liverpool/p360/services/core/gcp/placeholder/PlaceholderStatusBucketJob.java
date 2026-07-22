package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.gcp.placeholder.PlaceholderStatusFileProcessor.ProcessedFile;
import mx.com.liverpool.p360.services.core.gcp.storage.GcpStorageClient;

/**
 * Scheduled/manual job that reads the fixed placeholder status file from GCS,
 * updates P360, and writes a backup file with row-level errors.
 */
public class PlaceholderStatusBucketJob {

    private static final String CONFIG_SERVICE_ACCOUNT = "p360.contingency.gcp.storage.service_account";
    private static final String CONFIG_BUCKET = "p360.contingency.gcp.placeholder_status.bucket";
    private static final String CONFIG_OBJECT = "p360.contingency.gcp.placeholder_status.object";
    private static final String CONFIG_BACKUP_PREFIX = "p360.contingency.gcp.placeholder_status.backup_prefix";
    private static final String CONFIG_UPDATE_ENDPOINT = "p360.contingency.gcp.placeholder_status.update_endpoint";

    private static final Logger LOGGER = Logger.getLogger(PlaceholderStatusBucketJob.class.getName());

    public static void main(String[] args) throws Exception {
        PlaceholderStatusProcessResult result = new PlaceholderStatusBucketJob().run();
        System.out.println(result.toJson());
    }

    public PlaceholderStatusProcessResult run() throws Exception {
        Config config = Config.load();
        GcpStorageClient storageClient = new GcpStorageClient(config.serviceAccountFile);
        PlaceholderStatusFileProcessor processor =
                new PlaceholderStatusFileProcessor(new PlaceholderStatusRestUpdater(), LOGGER);

        LOGGER.info("Starting placeholder status bucket job. bucket=" + config.bucket + ", object=" + config.objectName);
        byte[] sourceContent = storageClient.readBytes(config.bucket, config.objectName);
        ProcessedFile processedFile = processor.process(new ByteArrayInputStream(sourceContent));

        String backupObjectName = config.createBackupObjectName();
        storageClient.writeBytes(config.bucket, backupObjectName,
                processedFile.getBackupContent().getBytes(StandardCharsets.UTF_8), "text/csv; charset=UTF-8");
        storageClient.deleteObject(config.bucket, config.objectName);

        LOGGER.info("Finished placeholder status bucket job. processed=" + processedFile.getProcessed()
                + ", success=" + processedFile.getSuccess()
                + ", errors=" + processedFile.getErrors()
                + ", ignored=" + processedFile.getIgnored()
                + ", backupObjectName=" + backupObjectName);

        return new PlaceholderStatusProcessResult(
                processedFile.getRowsRead(),
                processedFile.getProcessed(),
                processedFile.getSuccess(),
                processedFile.getErrors(),
                processedFile.getIgnored(),
                backupObjectName);
    }

    private static class Config {
        private final String serviceAccountFile;
        private final String bucket;
        private final String objectName;
        private final String backupPrefix;
        private final String updateEndpoint;

        private Config(String serviceAccountFile, String bucket, String objectName, String backupPrefix, String updateEndpoint) {
            this.serviceAccountFile = serviceAccountFile;
            this.bucket = bucket;
            this.objectName = objectName;
            this.backupPrefix = backupPrefix.endsWith("/") ? backupPrefix : backupPrefix + "/";
            this.updateEndpoint = updateEndpoint;
        }

        private static Config load() {
            return new Config(
                    required(CONFIG_SERVICE_ACCOUNT),
                    required(CONFIG_BUCKET),
                    required(CONFIG_OBJECT),
                    required(CONFIG_BACKUP_PREFIX),
                    required(CONFIG_UPDATE_ENDPOINT));
        }

        private String createBackupObjectName() {
            String fileName = objectName;
            int slash = fileName.lastIndexOf('/');
            if (slash >= 0) {
                fileName = fileName.substring(slash + 1);
            }
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
            return backupPrefix + timestamp + "_" + fileName;
        }
    }

    private static String required(String key) {
        String value = PropertiesManager.get(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    static {
        try {
            LOGGER.setUseParentHandlers(false);
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("../logs/gcp"));
            FileHandler fileHandler = new FileHandler("../logs/gcp/placeholder-status-%g.log", 25 * 1024 * 1024, 10, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis()));
                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record)
                            + System.lineSeparator();
                }
            });
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not initialize placeholder status logger", e);
        }
    }
}
