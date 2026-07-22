package mx.com.liverpool.p360.services.core.gcp.placeholder;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * Placeholder status updater backed by a configurable REST endpoint.
 *
 * <p>The final request shape can be adjusted when the target service contract is
 * available. Until then, this class isolates all service-call code from the file
 * processor.</p>
 */
public class PlaceholderStatusRestUpdater implements PlaceholderStatusUpdater {

    private static final String CONFIG_ENDPOINT = "p360.contingency.gcp.placeholder_status.update_endpoint";

    private final RESTWrapper rw = new RESTWrapper();

    @Override
    public void update(String placeholderId, String status) {
        String endpoint = PropertiesManager.get(CONFIG_ENDPOINT);
        if (endpoint == null || endpoint.trim().length() == 0) {
            throw new IllegalStateException("Missing property: " + CONFIG_ENDPOINT);
        }

        org.json.JSONObject request = new org.json.JSONObject()
                .put("id_placeholder", placeholderId)
                .put("status_of_the_placeholder", status);

        org.json.JSONObject response = rw.getRw().makeRequest("POST", endpoint, null, request.toString());
        String rawResponse = rw.getRw().getRawResponse();
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            throw new IllegalStateException("Empty response from placeholder update service");
        }
        if (response != null && (response.has("error") || response.has("Error"))) {
            throw new IllegalStateException(rawResponse);
        }
    }
}
