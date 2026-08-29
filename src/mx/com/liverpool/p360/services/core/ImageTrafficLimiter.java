package mx.com.liverpool.p360.services.core;

/**
 * Bulkhead for the image services. It rejects excess work immediately instead of
 * letting Tomcat/P360 accumulate hundreds of blocked requests.
 *
 * Optional properties:
 *   p360.images.max_concurrent   (default 2)
 *   p360.images.delete_batch_size (default 25)
 */
public final class ImageTrafficLimiter {

    private static final int MAX_CONCURRENT = readInt("p360.images.max_concurrent", 200, 1, 200);
    private static final int DELETE_BATCH_SIZE = readInt("p360.images.delete_batch_size", 175, 1, 200);
    private static final java.util.concurrent.Semaphore PERMITS = new java.util.concurrent.Semaphore(MAX_CONCURRENT, true);

    private ImageTrafficLimiter() {
    }

    public static boolean tryAcquire() {
        return PERMITS.tryAcquire();
    }

    public static void release() {
        PERMITS.release();
    }

    public static int getMaxConcurrent() {
        return MAX_CONCURRENT;
    }

    public static int getDeleteBatchSize() {
        return DELETE_BATCH_SIZE;
    }

    public static int getInFlight() {
        return MAX_CONCURRENT - PERMITS.availablePermits();
    }

    public static String busyResponse() {
        return "{\"Error\":\"IMAGE_SERVICE_BUSY\",\"retryable\":true,\"message\":\"Image service concurrency limit reached; retry later\"}";
    }

    public static boolean isBusyResponse(String value) {
        return value != null && value.contains("\"IMAGE_SERVICE_BUSY\"");
    }

    private static int readInt(String key, int defaultValue, int min, int max) {
        String value = null;
        try {
            value = PropertiesManager.get(key);
        } catch (Exception ignored) {
        }
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
