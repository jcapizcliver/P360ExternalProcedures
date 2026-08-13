package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.gcp.placeholder.PlaceholderStatusFileProcessor.ProcessedFile;
import mx.com.liverpool.p360.services.core.gcp.storage.GcpBucketLogger;

/**
 * Processes a placeholder status file that already exists in local storage.
 */
public class PlaceholderStatusFileJob {

    private static final Logger LOGGER = GcpBucketLogger.getLogger();

    private final PlaceholderStatusFileProcessor processor;
    private final Logger logger;

    public PlaceholderStatusFileJob() {
        this(LOGGER);
    }

    public PlaceholderStatusFileJob(Logger logger) {
        this.processor = new PlaceholderStatusFileProcessor(new PlaceholderStatusRestUpdater(), logger);
        this.logger = logger;
    }

    public PlaceholderStatusProcessResult process(PlaceholderStatusStoredFile storedFile) throws IOException {
        Path processedFile = storedFile.getProcessedFile();
        logger.info("Starting placeholder status file job. sourceFile=" + storedFile.getSourceFile()
                + ", processedFile=" + processedFile);

        ProcessedFile processed;
        try (FileInputStream in = new FileInputStream(processedFile.toFile())) {
            processed = processor.process(in);
        }

        Path errorsFile = null;
        if (processed.getErrors() > 0) {
            errorsFile = processedFile.resolveSibling(createErrorsFileName(processedFile.getFileName().toString()));
            Files.write(errorsFile, processed.getBackupContent().getBytes(StandardCharsets.UTF_8));
        }

        logger.info("Finished placeholder status file job. processed=" + processed.getProcessed()
                + ", success=" + processed.getSuccess()
                + ", errors=" + processed.getErrors()
                + ", ignored=" + processed.getIgnored()
                + ", processedFilePath=" + processedFile
                + ", errorsFilePath=" + (errorsFile == null ? "" : errorsFile));

        return new PlaceholderStatusProcessResult(
                processed.getRowsRead(),
                processed.getProcessed(),
                processed.getSuccess(),
                processed.getErrors(),
                processed.getIgnored(),
                storedFile.getSourceObjectName(),
                storedFile.getSourceFile().toAbsolutePath().toString(),
                processedFile.toAbsolutePath().toString(),
                errorsFile == null ? "" : errorsFile.toAbsolutePath().toString());
    }

    private static String createErrorsFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot) + "_errors" + fileName.substring(dot);
        }
        return fileName + "_errors";
    }
}
