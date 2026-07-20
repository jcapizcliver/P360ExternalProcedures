package mx.com.liverpool.p360.services.core.temp.gcp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import mx.com.liverpool.p360.services.core.gcp.GcpBucketFileProcessor;

/**
 * Local probe for the GCP file processor.
 *
 * <p>Run as Java Application from Eclipse to validate the CSV extraction and
 * P360 update preparation without requiring GCP, Pub/Sub, service account, or
 * bucket access. The processor defaults to dry-run mode unless the
 * product_description_update.dry_run property is explicitly set to false.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * mx.com.liverpool.p360.services.core.temp.gcp.GcpLocalFileProcessorProbe
 * mx.com.liverpool.p360.services.core.temp.gcp.GcpLocalFileProcessorProbe C:/path/file.csv
 * </pre>
 *
 * <p>When no argument is provided it reads:
 * Memelos/test-data/gcp/test-file.csv.csv</p>
 */
public class GcpLocalFileProcessorProbe {

    public static void main(String[] args) throws Exception {
        Path inputPath = args != null && args.length > 0
                ? Paths.get(args[0])
                : Paths.get("Memelos", "test-data", "gcp", "test-file.csv.csv");

        try (InputStream inputStream = new FileInputStream(inputPath.toFile())) {
            new GcpBucketFileProcessor().process(inputPath.toString(), inputStream);
        }
    }
}
