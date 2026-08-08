package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.gcp.storage.GcpBucketLogger;

/**
 * Imports a file that already exists on the application server.
 */
public class PlaceholderStatusLocalFileImporter {

    private static final Logger LOGGER = GcpBucketLogger.getLogger();

    private final PlaceholderStatusFileStorage storage;
    private final Logger logger;

    public PlaceholderStatusLocalFileImporter() {
        this(new PlaceholderStatusFileStorage(), LOGGER);
    }

    public PlaceholderStatusLocalFileImporter(PlaceholderStatusFileStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public PlaceholderStatusStoredFile importFile(String sourceFilePath) throws IOException {
        return importFile(Paths.get(sourceFilePath));
    }

    public PlaceholderStatusStoredFile importFile(Path sourceFile) throws IOException {
        if (!Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
            throw new FileNotFoundException("Local placeholder status file not found: "
                    + sourceFile.toAbsolutePath());
        }

        Path storedFile = storage.copyLocalFile(sourceFile);
        logger.info("Imported local placeholder status file. sourceFile=" + sourceFile
                + ", storedFile=" + storedFile);
        return new PlaceholderStatusStoredFile(sourceFile, storedFile, null);
    }
}
