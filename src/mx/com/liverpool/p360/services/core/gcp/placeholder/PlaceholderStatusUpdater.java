package mx.com.liverpool.p360.services.core.gcp.placeholder;

/**
 * Updates one placeholder status in P360.
 */
public interface PlaceholderStatusUpdater {
    void update(String placeholderId, String status) throws Exception;
}
