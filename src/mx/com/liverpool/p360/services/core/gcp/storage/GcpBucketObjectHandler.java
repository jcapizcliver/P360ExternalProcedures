package mx.com.liverpool.p360.services.core.gcp.storage;

import java.io.InputStream;

/**
 * Business callback for bucket-triggered executions.
 */
public interface GcpBucketObjectHandler {
    void handle(String bucket, String objectName, InputStream inputStream) throws Exception;
}
