package mx.com.liverpool.p360.services.core.gcp.placeholder;

/**
 * Updates placeholder statuses in P360.
 */
public interface PlaceholderStatusUpdater {
    void update(java.util.List<PlaceholderStatusUpdate> updates) throws Exception;
}
