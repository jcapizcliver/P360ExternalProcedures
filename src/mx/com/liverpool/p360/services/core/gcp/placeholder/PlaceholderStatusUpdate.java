package mx.com.liverpool.p360.services.core.gcp.placeholder;

/**
 * One Product2G status update prepared from the placeholder status file.
 */
public class PlaceholderStatusUpdate {

    private final String placeholderId;
    private final String status;

    public PlaceholderStatusUpdate(String placeholderId, String status) {
        this.placeholderId = placeholderId;
        this.status = status;
    }

    public String getPlaceholderId() {
        return placeholderId;
    }

    public String getStatus() {
        return status;
    }
}
