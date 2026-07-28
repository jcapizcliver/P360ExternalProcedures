package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.nio.file.Path;

/**
 * Local copy that will be used by the placeholder status process.
 */
public class PlaceholderStatusStoredFile {

    private final Path sourceFile;
    private final Path processedFile;
    private final String sourceObjectName;

    public PlaceholderStatusStoredFile(Path sourceFile, Path processedFile, String sourceObjectName) {
        this.sourceFile = sourceFile;
        this.processedFile = processedFile;
        this.sourceObjectName = sourceObjectName;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public Path getProcessedFile() {
        return processedFile;
    }

    public String getSourceObjectName() {
        return sourceObjectName;
    }
}
