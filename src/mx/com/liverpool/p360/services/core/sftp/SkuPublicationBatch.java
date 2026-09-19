package mx.com.liverpool.p360.services.core.sftp;

import java.util.*;
import java.util.function.*;
import org.json.*;

/** Synchronous SKU write -> broker confirmation. The caller retains the XML on failure. */
public final class SkuPublicationBatch {
    public interface Writer { void write(String entity, JSONObject request); }
    private final Writer writer;
    private final Function<String, String> publisher;
    private final Function<String, String> parentLookup;
    private final Consumer<String> log;
    private final Map<String, JSONObject> pending = new LinkedHashMap<>();
    private String source;

    public SkuPublicationBatch(Writer writer, Function<String, String> publisher,
            Function<String, String> parentLookup, Consumer<String> log) {
        this.writer = writer; this.publisher = publisher;
        this.parentLookup = parentLookup; this.log = log;
    }
    public void begin(String source) {
        // The preceding failed XML remains at the source for a complete retry.
        pending.clear(); this.source = source;
    }
    public void add(String entity, String id, String sku, String parent) {
        if (blank(sku)) return; // Empty SAP values do not erase existing values.
        if (blank(id)) throw new IllegalStateException("SKU_TARGET_MISSING source=" + source + " sku=" + sku);
        if (!"Product2G".equals(entity) && !"Article".equals(entity)) throw new IllegalArgumentException(entity);
        JSONObject entry = new JSONObject().put("entity", entity).put("id", id).put("sku", sku);
        if (!blank(parent)) entry.put("parent", parent);
        pending.put(entity + ":" + id, entry);
    }
    public void flushIfReady() { if (pending.size() >= 100) flush(false); }
    public void finish() { flush(true); }

    private void flush(boolean requireAll) {
        List<JSONObject> ready = new ArrayList<>();
        for (JSONObject entry : pending.values()) {
            if ("Article".equals(entry.getString("entity")) && blank(entry.optString("parent", null))) {
                String parent = parentLookup.apply(entry.getString("id"));
                if (!blank(parent)) entry.put("parent", parent);
            }
            if ("Product2G".equals(entry.getString("entity")) || !blank(entry.optString("parent", null))) ready.add(entry);
        }
        // Keep already-confirmed records out of retries of the same immutable XML.
        java.util.Iterator<JSONObject> receipts=ready.iterator();
        while(receipts.hasNext()) {JSONObject e=receipts.next();if(DurableSftpQueue.confirmed(DurableSftpQueue.publicationKey(e))) {
            pending.remove(e.getString("entity")+":"+e.getString("id"));receipts.remove();
        }}
        // Bound request size even when deferred relationships become available together.
        for (int start = 0; start < ready.size(); start += 100) {
            List<JSONObject> batch = ready.subList(start, Math.min(start + 100, ready.size()));
            for (String entity : Arrays.asList("Product2G", "Article")) {
                JSONArray rows = new JSONArray();
                for (JSONObject entry : batch) if (entity.equals(entry.getString("entity"))) {
                    rows.put(new JSONObject().put("object", new JSONObject().put("id", "'" + entry.getString("id") + "'@1"))
                            .put("values", new JSONArray().put(entry.getString("sku"))));
                }
                if (rows.length() > 0) writer.write(entity, new JSONObject()
                        .put("columns", new JSONArray().put(new JSONObject().put("identifier", entity + ".SKU"))).put("rows", rows));
            }
            log.accept("SKU_P360_CONFIRMED source=" + source + " targets=" + batch.size());
            JSONArray products = new JSONArray();
            for (JSONObject entry : batch) {
                boolean product = "Product2G".equals(entry.getString("entity"));
                JSONObject message = new JSONObject().put("proposalId", product ? entry.getString("id") : entry.getString("parent"));
                if (product) message.put("header", new JSONObject().put("SKU", entry.getString("sku")));
                else message.put("variants", new JSONArray().put(new JSONObject().put("variantId", entry.getString("id")).put("SKU", entry.getString("sku"))));
                products.put(message);
            }
            DurableSftpQueue.beforePublish(batch);
            String messageId = publisher.apply(new JSONObject().put("products", products).toString());
            if (blank(messageId)) throw new IllegalStateException("SKU_PUBSUB_UNCONFIRMED source=" + source + " targets=" + batch.size());
            for (JSONObject entry : batch) {
                DurableSftpQueue.confirm(DurableSftpQueue.publicationKey(entry));
                log.accept("SKU_PUBSUB_CONFIRMED source=" + source + " messageId=" + messageId
                        + " product=" + ("Product2G".equals(entry.getString("entity")) ? entry.getString("id") : entry.getString("parent"))
                        + " entity=" + entry.getString("entity") + " identifier=" + entry.getString("id") + " sku=" + entry.getString("sku"));
                pending.remove(entry.getString("entity") + ":" + entry.getString("id"));
            }
        }
        if (requireAll && !pending.isEmpty()) throw new IllegalStateException("SKU_PARENT_PENDING source=" + source + " targets=" + pending.values());
    }
    public static void checkWrite(String response) {
        try {
            JSONObject result = new JSONObject(response);
            JSONObject counters = result.optJSONObject("counters");
            if (counters != null && counters.optInt("errors", -1) == 0 && counters.optInt("objectsWithErrors", -1) == 0) return;
        } catch (RuntimeException ignored) { }
        throw new IllegalStateException("SKU_P360_UNCONFIRMED response=" + response);
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim()); }
}
