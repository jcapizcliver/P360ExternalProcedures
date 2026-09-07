package mx.com.liverpool.p360.services.core.completeness;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

/**
 * Persiste únicamente Product2G.MandatoryCompleteness por List API.
 *
 * El campo lógico habilitado por Repository Manager se apoya en
 * ArticleDetail.Res_BigDecimal12_01, pero aquí NO escribimos Oracle directo.
 */
public final class MandatoryCompletenessP360Writer {

    public static final String FIELD = "Product2G.MandatoryCompleteness";

    private final Connection connection;
    private final CompletenessSnapshotDao snapshotDao;
    private final RESTWrapper rw;
    private final int restBatchSize;

    public MandatoryCompletenessP360Writer(
            Connection connection,
            CompletenessSnapshotDao snapshotDao,
            int restBatchSize) {
        this.connection = connection;
        this.snapshotDao = snapshotDao;
        this.restBatchSize = restBatchSize;
        this.rw = new RESTWrapper();
        String apiHost = System.getenv("P360_COMPLETENESS_API_HOST");
        if (apiHost != null && !apiHost.isBlank()) {
            try {
                java.net.URI configured = java.net.URI.create(rw.getRw().getBaseUrl());
                rw.getRw().setBaseUrl(new java.net.URI(configured.getScheme(), null,
                        apiHost, configured.getPort(), configured.getPath(), null, null).toString());
                System.out.println("Completeness List API node: " + apiHost);
            } catch (java.net.URISyntaxException e) {
                throw new IllegalArgumentException("Invalid P360_COMPLETENESS_API_HOST", e);
            }
        }
    }

    public boolean write(Collection<CompletenessResult> results) throws SQLException {
        return write(results, true);
    }

    public void preflight() {
        Map<String, String> query = new HashMap<>();
        query.put("fields", FIELD);
        query.put("pageSize", "1");
        query.put("query", "Product2G.ProductNo equals \"__mandatory_preflight__\"");
        org.json.JSONObject response = rw.getRw().makeRequest(
                "GET", "/list/Product2G/bySearch", query, null);
        if (response == null || !response.has("totalSize")
                || response.optJSONArray("rows") == null) {
            throw new IllegalStateException("Preflight: List API could not read " + FIELD);
        }
        System.out.println("Preflight: List API field readable: " + FIELD);
    }

    public boolean write(Collection<CompletenessResult> results, boolean stopOnRestError) throws SQLException {
        if (results == null || results.isEmpty()) return true;

        Map<String, String> qp = new HashMap<>();
        qp.put("includeObjectsInProtocol", "false");

        AtomicBoolean allOk = new AtomicBoolean(true);

        RequestHandler requestHandler = new RequestHandler(
                new org.json.JSONArray()
                        .put(new org.json.JSONObject().put("identifier", FIELD)),
                restBatchSize,
                request -> {
                    List<String> ids = extractIds(request);
                    AtomicBoolean ok = new AtomicBoolean(false);
                    AtomicReference<String> raw = new AtomicReference<>();

                    try {
                        rw.writeData(
                                "list",
                                "Product2G",
                                null,
                                qp,
                                request,
                                response -> {
                                    raw.set(response);
                                    ok.set(isSuccessfulWriteResponse(response));
                                });
                    } catch (RuntimeException e) {
                        raw.set(e.toString());
                        ok.set(false);
                    }

                    if (!ok.get()) allOk.set(false);

                    try {
                        snapshotDao.markMandatorySync(
                                ids,
                                ok.get() ? "SUCCESS" : "FAILED",
                                raw.get());
                        connection.commit();
                    } catch (SQLException e) {
                        allOk.set(false);
                        try { connection.rollback(); } catch (SQLException ignored) {}
                        throw new RuntimeException(e);
                    }
                    // RequestHandler reuses its rows; also clear them on transport failures.
                    org.json.JSONArray sentRows = request.optJSONArray("rows");
                    if (sentRows != null) {
                        while (sentRows.length() > 0) sentRows.remove(0);
                    }
                    if (!ok.get() && stopOnRestError) {
                        throw new IllegalStateException("List API failed; remaining sub-batches were not sent");
                    }
                });

        for (CompletenessResult result : results) {
            if (result == null || result.getMandatory() == null) continue;
            if (result.getMandatory().getPercentage() == null) continue;

            requestHandler.addRow(
                    new org.json.JSONObject()
                            .put("object",
                                    new org.json.JSONObject()
                                            .put("id", "'" + result.getProductIdentifier() + "'@1"))
                            .put("values",
                                    new org.json.JSONArray()
                                            .put(result.getMandatory()
                                                    .getPercentage()
                                                    .toPlainString())));
        }

        requestHandler.sendData();
        return allOk.get();
    }

    public void markDryRun(Collection<CompletenessResult> results) throws SQLException {
        List<String> ids = new ArrayList<>();
        for (CompletenessResult r : results) {
            if (r != null) ids.add(r.getProductIdentifier());
        }
        snapshotDao.markMandatorySync(ids, "DRY_RUN", "No se escribió Product2G.MandatoryCompleteness");
        connection.commit();
    }

    private static List<String> extractIds(org.json.JSONObject request) {
        List<String> ids = new ArrayList<>();
        org.json.JSONArray rows = request.optJSONArray("rows");
        if (rows == null) return ids;

        for (int i = 0; i < rows.length(); i++) {
            org.json.JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            org.json.JSONObject object = row.optJSONObject("object");
            if (object == null) continue;
            String rawId = object.optString("id", "");
            String id = rawId;
            if (id.startsWith("'") && id.endsWith("'@1") && id.length() > 4) {
                id = id.substring(1, id.length() - 3);
            }
            if (!id.isBlank()) ids.add(id);
        }
        return ids;
    }

    private static boolean isSuccessfulWriteResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) return false;
        try {
            org.json.JSONObject response = new org.json.JSONObject(rawResponse);
            org.json.JSONObject counters = response.optJSONObject("counters");
            return counters != null
                    && counters.optInt("errors", -1) == 0
                    && counters.optInt("objectsWithErrors", -1) == 0;
        } catch (org.json.JSONException e) {
            return false;
        }
    }
}
