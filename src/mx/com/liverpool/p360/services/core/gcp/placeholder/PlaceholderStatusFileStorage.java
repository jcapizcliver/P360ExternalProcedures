package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import mx.com.liverpool.p360.services.core.PropertiesManager;

/**
 * Handles the local storage folder used by placeholder status executions.
 */
public class PlaceholderStatusFileStorage {

    private static final String CONFIG_STORAGE_DIR = "p360.contingency.placeholder_status.storage_dir";
    private static final String DEFAULT_STORAGE_DIR = "/u0/storage";

    public Path copyLocalFile(Path sourceFile) throws IOException {
        Path storedFile = createStoredFileForName(sourceFile.getFileName().toString());
        Files.copy(sourceFile, storedFile, StandardCopyOption.REPLACE_EXISTING);
        return storedFile;
    }

    public Path createStoredFileForName(String fileName) throws IOException {
        Path storageDir = storageDir();
        Files.createDirectories(storageDir);
        return storageDir.resolve(createStampedFileName(fileName));
    }

    private static Path storageDir() {
        String configured = PropertiesManager.get(CONFIG_STORAGE_DIR);
        if (configured == null || configured.trim().length() == 0) {
            configured = DEFAULT_STORAGE_DIR;
        }
        return Paths.get(configured.trim());
    }

    private static String createStampedFileName(String fileName) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot) + "_" + timestamp + fileName.substring(dot);
        }
        return fileName + "_" + timestamp;
    }
}
