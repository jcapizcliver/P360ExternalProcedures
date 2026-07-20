package mx.com.liverpool.p360.services.core.gcp;

import org.json.JSONObject;

/**
 * Value object for a Cloud Storage file event delivered through Pub/Sub.
 *
 * <p>The expected Pub/Sub data payload is the JSON produced by a Cloud Storage
 * object notification, with fields such as:</p>
 *
 * <pre>
 * {
 *   "bucket": "bucket-name",
 *   "name": "incoming/file.csv",
 *   "generation": "123456789"
 * }
 * </pre>
 *
 * <p>The parser also accepts a simple wrapper with a nested {@code message}
 * object to make local/manual tests easier.</p>
 */
public class GcpBucketFileEvent {

    private final String bucket;
    private final String objectName;
    private final String generation;
    private final String eventType;

    public GcpBucketFileEvent(String bucket, String objectName, String generation, String eventType) {
        this.bucket = bucket;
        this.objectName = objectName;
        this.generation = generation;
        this.eventType = eventType;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getGeneration() {
        return generation;
    }

    public String getEventType() {
        return eventType;
    }

    /**
     * Builds a file event from the Pub/Sub message payload.
     *
     * @throws IllegalArgumentException when bucket or object name are missing.
     */
    public static GcpBucketFileEvent fromPubSubPayload(String payload) {
        JSONObject json = new JSONObject(payload);
        String bucket = json.optString("bucket", null);
        String objectName = json.optString("name", null);
        String generation = json.optString("generation", null);
        String eventType = json.optString("eventType", json.optString("kind", null));

        if ((bucket == null || objectName == null) && json.has("message")) {
            JSONObject message = json.getJSONObject("message");
            bucket = firstNonBlank(bucket, message.optString("bucket", null));
            objectName = firstNonBlank(objectName, message.optString("name", null));
            generation = firstNonBlank(generation, message.optString("generation", null));
            eventType = firstNonBlank(eventType, message.optString("eventType", null));
        }

        if (bucket == null || objectName == null) {
            throw new IllegalArgumentException("Pub/Sub payload does not contain bucket/name: " + payload);
        }

        return new GcpBucketFileEvent(bucket, objectName, generation, eventType);
    }

    private static String firstNonBlank(String current, String fallback) {
        if (current != null && current.trim().length() > 0) {
            return current;
        }
        if (fallback != null && fallback.trim().length() > 0) {
            return fallback;
        }
        return null;
    }

    @Override
    public String toString() {
        return "GcpBucketFileEvent{bucket='" + bucket + "', objectName='" + objectName + "', generation='" + generation
                + "', eventType='" + eventType + "'}";
    }
}
