package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.util.Locale;

/**
 * One Product2G status update prepared from the placeholder status file.
 */
public class PlaceholderStatusUpdate {

    private final String placeholderId;
    private final String status;
    private final String action;
    private final String comment;

    public PlaceholderStatusUpdate(String placeholderId, String status) {
        this(placeholderId, status, null, null);
    }

    public PlaceholderStatusUpdate(String placeholderId, String status, String comment) {
        this(placeholderId, status, null, comment);
    }

    public PlaceholderStatusUpdate(String placeholderId, String status, String action, String comment) {
        this.placeholderId = placeholderId;
        this.status = status;
        this.action = action;
        this.comment = comment;
    }

    public String getPlaceholderId() {
        return placeholderId;
    }

    public String getStatus() {
        return status;
    }

    public String getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public boolean hasComment() {
        if (comment == null) {
            return false;
        }
        String normalized = comment.trim();
        if (normalized.length() == 0) {
            return false;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return !"NULL".equals(normalized)
                && !"UNDEFINED".equals(normalized)
                && !"NULO".equals(normalized)
                && !"NA".equals(normalized)
                && !"N/A".equals(normalized);
    }
}
