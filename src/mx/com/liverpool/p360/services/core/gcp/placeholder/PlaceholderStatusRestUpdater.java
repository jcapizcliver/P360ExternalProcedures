package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.gcp.storage.GcpBucketLogger;

/**
 * Updates the complete Product2G status flow from placeholder status events.
 */
public class PlaceholderStatusRestUpdater implements PlaceholderStatusUpdater {

    private final RESTWrapper rw = new RESTWrapper();
    private final Product2GUserRemarksUpdater userRemarksUpdater;
    private final Logger logger;

    public PlaceholderStatusRestUpdater() {
        this(GcpBucketLogger.getLogger());
    }

    public PlaceholderStatusRestUpdater(Logger logger) {
        this.logger = logger;
        this.userRemarksUpdater = new Product2GUserRemarksUpdater(rw, logger);
    }

    @Override
    public void update(java.util.List<PlaceholderStatusUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        try (ProposalStatusFlow statusFlow = new ProposalStatusFlow(logger)) {
            for (PlaceholderStatusUpdate update : updates) {
                updateStatus(update, statusFlow);
            }
        }

        logger.info("Product2G status flow update finished. rows=" + updates.size());
        userRemarksUpdater.updateRemarks(updates);
    }

    private void updateStatus(PlaceholderStatusUpdate update, ProposalStatusFlow statusFlow) {
        org.json.JSONObject product = getProduct2GObject(update.getPlaceholderId());
        org.json.JSONObject data = product.getJSONObject("_data");
        String previousStatus = getStatusCode(data, "previousStatus");
        String currentStatus = getStatusCode(data, "currentStatus");
        ProposalStatusFlow.Transition transition = statusFlow.resolve(
                previousStatus, currentStatus, update.getAction());

        String internalId = product.getJSONObject("_entityItem").getString("_internalId");
        org.json.JSONObject request = new org.json.JSONObject()
                .put("previousStatus", statusCode(transition.getPreviousStatus()))
                .put("currentStatus", statusCode(transition.getCurrentStatus()))
                .put("externalStatus", statusCode(transition.getExternalStatus()));

        logger.info("Updating Product2G status flow. placeholderId=" + update.getPlaceholderId()
                + ", previousStatus=" + transition.getPreviousStatus()
                + ", currentStatus=" + transition.getCurrentStatus()
                + ", externalStatus=" + transition.getExternalStatus());
        org.json.JSONObject response = rw.getRw().makeRequest("PUT",
                "/object/Product2G/" + internalId + "?includeLabels=true", null, request.toString());
        validateResponse(response, "Product2G status flow update");
    }

    private org.json.JSONObject getProduct2GObject(String placeholderId) {
        String value = placeholderId == null ? "" : placeholderId.trim().replace("'", "");
        String path = "/object/Product2G/'" + value + "'@'MASTER'"
                + "?entityFilter=Product2G&includeLabels=true&includeIds=true";
        org.json.JSONObject response = rw.getRw().makeRequest("GET", path,
                new java.util.TreeMap<String, String>(), null);
        validateResponse(response, "Product2G status read");
        return response;
    }

    private void validateResponse(org.json.JSONObject response, String operation) {
        String rawResponse = rw.getRw().getRawResponse();
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            throw new IllegalStateException("Empty response from " + operation);
        }
        if (response == null || response.has("error") || response.has("Error")) {
            throw new IllegalStateException(rawResponse);
        }
    }

    private static String getStatusCode(org.json.JSONObject data, String property) {
        if (!data.has(property)) {
            return "";
        }
        org.json.JSONObject status = data.getJSONObject(property);
        if (status.has("_key")) {
            return String.valueOf(status.get("_key"));
        }
        return status.has("_code") ? status.getString("_code") : "";
    }

    private static org.json.JSONObject statusCode(String code) {
        return new org.json.JSONObject().put("_code", code);
    }
}
