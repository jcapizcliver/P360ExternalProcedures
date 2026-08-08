package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.gcp.storage.GcpBucketLogger;
import mx.com.liverpool.p360.services.core.gcp.storage.GcpStorageClient;

/**
 * Imports the configured GCS object into local storage.
 */
public class PlaceholderStatusBucketFileImporter {

    private static final String CONFIG_BUCKET = "p360.contingency.gcp.placeholder_status.bucket";
    private static final String CONFIG_OBJECT = "p360.contingency.gcp.placeholder_status.object";
    private static final String CONFIG_IMPERSONATE_SERVICE_ACCOUNT = "p360.contingency.gcp.placeholder_status.impersonate_service_account";
    private static final Logger LOGGER = GcpBucketLogger.getLogger();

    private final PlaceholderStatusFileStorage storage;
    private final Logger logger;

    public PlaceholderStatusBucketFileImporter() {
        this(new PlaceholderStatusFileStorage(), LOGGER);
    }

    public PlaceholderStatusBucketFileImporter(PlaceholderStatusFileStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public PlaceholderStatusStoredFile importFile() throws Exception {
        Config config = Config.load();
        GcpStorageClient storageClient = new GcpStorageClient(config.serviceAccount);

        logger.info("Importing placeholder status file from bucket. bucket=" + config.bucket
                + ", object=" + config.objectName
                + ", serviceAccount=" + (config.serviceAccount == null ? "ADC default" : config.serviceAccount));
        logVisibleObjects(storageClient, config);
        byte[] sourceContent = storageClient.readBytes(config.bucket, config.objectName);
        Path storedFile = storage.createStoredFileForName(config.getOriginalFileName());
        Files.write(storedFile, sourceContent);
        logger.info("Imported bucket placeholder status file. storedFile=" + storedFile);

        return new PlaceholderStatusStoredFile(storedFile, storedFile, config.objectName);
    }

    private void logVisibleObjects(GcpStorageClient storageClient, Config config) {
        try {
            org.json.JSONArray objects = storageClient.listObjectNames(config.bucket, config.getParentPrefix(), 25);
            logger.info("Visible GCS objects sample. bucket=" + config.bucket
                    + ", prefix=" + config.getParentPrefix()
                    + ", count=" + objects.length()
                    + ", objects=" + objects);
        } catch (Exception e) {
            logger.warning("Could not list visible GCS objects before import. bucket=" + config.bucket
                    + ", prefix=" + config.getParentPrefix()
                    + ", error=" + (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
        }
    }

    private static class Config {
        private final String bucket;
        private final String objectName;
        private final String serviceAccount;

        private Config(String bucket, String objectName, String serviceAccount) {
            this.bucket = bucket;
            this.objectName = objectName;
            this.serviceAccount = serviceAccount;
        }

        private static Config load() {
            return new Config(
                    required(CONFIG_BUCKET),
                    required(CONFIG_OBJECT),
                    optionalServiceAccount());
        }

        private String getOriginalFileName() {
            String fileName = objectName;
            int slash = fileName.lastIndexOf('/');
            if (slash >= 0) {
                fileName = fileName.substring(slash + 1);
            }
            return fileName;
        }

        private String getParentPrefix() {
            int slash = objectName.lastIndexOf('/');
            if (slash < 0) {
                return "";
            }
            return objectName.substring(0, slash + 1);
        }
    }

    private static String required(String key) {
        String value = PropertiesManager.get(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    private static String optionalServiceAccount() {
        return  normalize(PropertiesManager.get(CONFIG_IMPERSONATE_SERVICE_ACCOUNT));
    }

    private static String normalize(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }
}
