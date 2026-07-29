package mx.com.liverpool.p360.services.core.gcp.placeholder;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * Placeholder status updater backed by Product2G list update.
 */
public class PlaceholderStatusRestUpdater implements PlaceholderStatusUpdater {

    private static final String CONFIG_ENDPOINT = "p360.contingency.gcp.placeholder_status.update_endpoint";
    private static final String DEFAULT_ENDPOINT = "/list/Product2G/";

    private final RESTWrapper rw = new RESTWrapper();

    @Override
    public void update(java.util.List<PlaceholderStatusUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        String endpoint = PropertiesManager.get(CONFIG_ENDPOINT);
        if (endpoint == null || endpoint.trim().length() == 0) {
            endpoint = DEFAULT_ENDPOINT;
        }
        endpoint = endpoint.trim();

        org.json.JSONArray columns = new org.json.JSONArray()
                .put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"));
        org.json.JSONArray rows = new org.json.JSONArray();
        for (PlaceholderStatusUpdate update : updates) {
            rows.put(new org.json.JSONObject()
                    .put("object", new org.json.JSONObject().put("id", toProduct2GObjectId(update.getPlaceholderId())))
                    .put("values", new org.json.JSONArray().put(update.getStatus())));
        }
        org.json.JSONObject request = new org.json.JSONObject()
                .put("columns", columns)
                .put("rows", rows);

        org.json.JSONObject response = rw.getRw().makeRequest("POST", endpoint, null, request.toString());
        String rawResponse = rw.getRw().getRawResponse();
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            throw new IllegalStateException("Empty response from placeholder update service");
        }
        if (response != null && (response.has("error") || response.has("Error"))) {
            throw new IllegalStateException(rawResponse);
        }
    }

    private static String toProduct2GObjectId(String placeholderId) {
        String value = placeholderId == null ? "" : placeholderId.trim();
        if (value.startsWith("'") && value.endsWith("'@1")) {
            return value;
        }
        return "'" + value.replace("'", "") + "'@1";
    }
}
