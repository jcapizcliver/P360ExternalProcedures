package mx.com.liverpool.p360.services.core.gcp.placeholder;

/**
 * Summary for one placeholder status file execution.
 */
public class PlaceholderStatusProcessResult {

    private final int rowsRead;
    private final int processed;
    private final int success;
    private final int errors;
    private final int ignored;
    private final String backupObjectName;

    public PlaceholderStatusProcessResult(int rowsRead, int processed, int success, int errors, int ignored, String backupObjectName) {
        this.rowsRead = rowsRead;
        this.processed = processed;
        this.success = success;
        this.errors = errors;
        this.ignored = ignored;
        this.backupObjectName = backupObjectName;
    }

    public int getRowsRead() {
        return rowsRead;
    }

    public int getProcessed() {
        return processed;
    }

    public int getSuccess() {
        return success;
    }

    public int getErrors() {
        return errors;
    }

    public int getIgnored() {
        return ignored;
    }

    public String getBackupObjectName() {
        return backupObjectName;
    }

    public org.json.JSONObject toJson() {
        return new org.json.JSONObject()
                .put("status", "finished")
                .put("rowsRead", rowsRead)
                .put("processed", processed)
                .put("success", success)
                .put("errors", errors)
                .put("ignored", ignored)
                .put("backupObjectName", backupObjectName == null ? "" : backupObjectName);
    }
}
