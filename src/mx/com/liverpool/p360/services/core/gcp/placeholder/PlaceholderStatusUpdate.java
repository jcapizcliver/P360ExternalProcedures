package mx.com.liverpool.p360.services.core.gcp.placeholder;

/**
 * One Product2G status update prepared from the placeholder status file.
 */
public class PlaceholderStatusUpdate {

    private final String placeholderId;
    private final String status;
    private final String comment;

    public PlaceholderStatusUpdate(String placeholderId, String status) {
        this(placeholderId, status, null);
    }

    public PlaceholderStatusUpdate(String placeholderId, String status, String comment) {
        this.placeholderId = placeholderId;
        this.status = status;
        this.comment = comment;
    }

    public String getPlaceholderId() {
        return placeholderId;
    }

    public String getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public boolean hasComment() {
        return comment != null && comment.trim().length() > 0;
    }
}
