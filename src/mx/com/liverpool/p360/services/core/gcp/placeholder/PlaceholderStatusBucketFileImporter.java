package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.gcp.storage.GcpStorageClient;

/**
 * Imports the configured GCS object into local storage.
 */
public class PlaceholderStatusBucketFileImporter {

    private static final String CONFIG_SERVICE_ACCOUNT = "p360.contingency.gcp.storage.service_account";
    private static final String CONFIG_BUCKET = "p360.contingency.gcp.placeholder_status.bucket";
    private static final String CONFIG_OBJECT = "p360.contingency.gcp.placeholder_status.object";
    private static final Logger LOGGER = Logger.getLogger(PlaceholderStatusBucketFileImporter.class.getName());

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
        GcpStorageClient storageClient = new GcpStorageClient(config.serviceAccountFile);

        logger.info("Importing placeholder status file from bucket. bucket=" + config.bucket
                + ", object=" + config.objectName);
        byte[] sourceContent = storageClient.readBytes(config.bucket, config.objectName);
        Path storedFile = storage.createStoredFileForName(config.getOriginalFileName());
        Files.write(storedFile, sourceContent);
        logger.info("Imported bucket placeholder status file. storedFile=" + storedFile);

        return new PlaceholderStatusStoredFile(storedFile, storedFile, config.objectName);
    }

    private static class Config {
        private final String serviceAccountFile;
        private final String bucket;
        private final String objectName;

        private Config(String serviceAccountFile, String bucket, String objectName) {
            this.serviceAccountFile = serviceAccountFile;
            this.bucket = bucket;
            this.objectName = objectName;
        }

        private static Config load() {
            return new Config(
                    required(CONFIG_SERVICE_ACCOUNT),
                    required(CONFIG_BUCKET),
                    required(CONFIG_OBJECT));
        }

        private String getOriginalFileName() {
            String fileName = objectName;
            int slash = fileName.lastIndexOf('/');
            if (slash >= 0) {
                fileName = fileName.substring(slash + 1);
            }
            return fileName;
        }
    }

    private static String required(String key) {
        String value = PropertiesManager.get(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }
}
