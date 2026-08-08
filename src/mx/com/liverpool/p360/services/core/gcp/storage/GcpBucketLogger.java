package mx.com.liverpool.p360.services.core.gcp.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Shared diagnostic logger for the placeholder GCS flow.
 */
public final class GcpBucketLogger {

    private static final Logger LOGGER = createLogger();

    private GcpBucketLogger() {
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    private static Logger createLogger() {
        Logger logger = Logger.getLogger("mx.com.liverpool.p360.services.core.gcp.bucket");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        if (logger.getHandlers().length > 0) {
            return logger;
        }
        try {
            Path logFile = Paths.get("/u01/test-logs/gcp-bucket.log");
            Files.createDirectories(logFile.getParent());
            FileHandler fileHandler = new FileHandler(logFile.toString(), true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            Logger.getLogger(GcpBucketLogger.class.getName()).log(Level.WARNING,
                    "Could not initialize GCP bucket diagnostic logger", e);
        }
        return logger;
    }
}
