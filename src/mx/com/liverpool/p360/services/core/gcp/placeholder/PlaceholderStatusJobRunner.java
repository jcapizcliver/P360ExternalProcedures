package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.gcp.storage.GcpBucketLogger;

/**
 * Shared orchestrator used by HTTP services and scheduled workers.
 */
public class PlaceholderStatusJobRunner {

    private static final Logger LOGGER = GcpBucketLogger.getLogger();

    private final PlaceholderStatusLocalFileImporter localFileImporter;
    private final PlaceholderStatusBucketFileImporter bucketFileImporter;
    private final PlaceholderStatusFileJob fileJob;

    public PlaceholderStatusJobRunner() {
        PlaceholderStatusFileStorage storage = new PlaceholderStatusFileStorage();
        this.localFileImporter = new PlaceholderStatusLocalFileImporter(storage, LOGGER);
        this.bucketFileImporter = new PlaceholderStatusBucketFileImporter(storage, LOGGER);
        this.fileJob = new PlaceholderStatusFileJob(LOGGER);
    }

    public PlaceholderStatusProcessResult run(String sourceFilePath) throws Exception {
        if (sourceFilePath != null && sourceFilePath.trim().length() > 0) {
            return runFromLocalFile(sourceFilePath.trim());
        }
        return runFromBucket();
    }

    public PlaceholderStatusProcessResult runFromLocalFile(String sourceFilePath) throws Exception {
        PlaceholderStatusStoredFile storedFile = localFileImporter.importFile(sourceFilePath);
        return fileJob.process(storedFile);
    }

    public PlaceholderStatusProcessResult runFromBucket() throws Exception {
        PlaceholderStatusStoredFile storedFile = bucketFileImporter.importFile();
        return fileJob.process(storedFile);
    }
}
