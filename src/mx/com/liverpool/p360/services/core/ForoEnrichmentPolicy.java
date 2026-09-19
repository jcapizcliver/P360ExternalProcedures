package mx.com.liverpool.p360.services.core;

/** Rules for the current enrichment cycle. A missing foroUser means non-Foro. */
public final class ForoEnrichmentPolicy {
    private ForoEnrichmentPolicy() { }
    public static Boolean transition(String oldStatus, String newStatus) {
        if (!"1026".equals(oldStatus)) return null;
        if ("1004".equals(newStatus)) return Boolean.FALSE;
        if ("1022".equals(newStatus) || "1023".equals(newStatus)) return Boolean.TRUE;
        return null;
    }
    public static Boolean proposal(boolean foroUser, boolean fresh, String oldStatus, String newStatus) {
        Boolean transitionValue = transition(oldStatus, newStatus);
        if (transitionValue != null) return transitionValue;
        if (foroUser) return Boolean.TRUE;
        if (fresh || ("1007".equals(oldStatus) && "1008".equals(newStatus))) return Boolean.FALSE;
        return null; // Preserve participation already established in this cycle.
    }
    public static Boolean apply(org.json.JSONObject request, org.json.JSONObject product,
            boolean fresh, String oldStatus, String newStatus) {
        Boolean value = proposal(product.optBoolean("foroUser", false), fresh, oldStatus, newStatus);
        if (value == null) return null;
        org.json.JSONArray records = request.optJSONArray("_characteristicRecords");
        if (records == null) { records = new org.json.JSONArray(); request.put("_characteristicRecords", records); }
        for (int i = records.length() - 1; i >= 0; i--) {
            org.json.JSONObject q = records.getJSONObject(i).optJSONObject("_qualification");
            org.json.JSONObject c = q == null ? null : q.optJSONObject("characteristic");
            if (c != null && "EnriquecidoEnForo".equals(c.optString("_code"))) records.remove(i);
        }
        records.put(new org.json.JSONObject()
            .put("_qualification", new org.json.JSONObject()
                .put("characteristic", new org.json.JSONObject().put("_code", "EnriquecidoEnForo"))
                .put("recordKey", "0000.0000.RK").put("parentRecordKey", "root"))
            .put("_datatype", "BOOLEAN")
            .put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject()
                .put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_key", -1)))
                .put("values", new org.json.JSONArray().put(value)))));
        return value;
    }
}
