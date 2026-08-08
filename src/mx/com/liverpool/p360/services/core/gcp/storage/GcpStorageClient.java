package mx.com.liverpool.p360.services.core.gcp.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.gcp.GcpCredentialsProvider;

/**
 * Minimal Google Cloud Storage client backed by the JSON API.
 *
 * <p>The project already includes google-auth, but not the full
 * google-cloud-storage client. Keep bucket transport concerns here so business
 * jobs can read objects without knowing GCS HTTP details.</p>
 */
public class GcpStorageClient {

    private static final String STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.read_only";
    private static final String STORAGE_BASE_URL = "https://storage.googleapis.com/storage/v1/b/";
    private static final String OBJECT_LIST_FIELDS = "items(name,size,updated),nextPageToken";
    private static final Logger GCP_LOGGER = GcpBucketLogger.getLogger();

    private final GcpCredentialsProvider credentialsProvider;

    public GcpStorageClient() {
        this((String) null);
    }

    public GcpStorageClient(String impersonateServiceAccount) {
        this(new GcpCredentialsProvider(impersonateServiceAccount, Arrays.asList(STORAGE_SCOPE), GCP_LOGGER));
    }

    public GcpStorageClient(GcpCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public InputStream read(String bucket, String objectName) throws IOException {
        return new ByteArrayInputStream(readBytes(bucket, objectName));
    }

    public byte[] readBytes(String bucket, String objectName) throws IOException {
        URL url = new URL(objectUrl(bucket, objectName) + "?alt=media");
        GCP_LOGGER.info("Reading GCS object. bucket=" + bucket + ", object=" + objectName + ", url=" + url);
        return executeGet(url);
    }

    public org.json.JSONArray listObjectNames(String bucket, String prefix, int maxResults) throws IOException {
        StringBuilder url = new StringBuilder(STORAGE_BASE_URL)
                .append(encodePathPart(bucket))
                .append("/o?fields=")
                .append(encodePathPart(OBJECT_LIST_FIELDS));
        if (prefix != null && prefix.trim().length() > 0) {
            url.append("&prefix=").append(encodePathPart(prefix.trim()));
        }
        if (maxResults > 0) {
            url.append("&maxResults=").append(maxResults);
        }

        GCP_LOGGER.info("Listing GCS objects. bucket=" + bucket + ", prefix=" + prefix + ", maxResults=" + maxResults);
        byte[] response = executeGet(new URL(url.toString()));
        org.json.JSONObject json = new org.json.JSONObject(new String(response, StandardCharsets.UTF_8));
        org.json.JSONArray items = json.optJSONArray("items");
        return items == null ? new org.json.JSONArray() : items;
    }

    private byte[] executeGet(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(0);

        int status = connection.getResponseCode();
        try (InputStream in = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
            byte[] response = readFully(in);
            GCP_LOGGER.info("GCS request completed. method=GET, status=" + status + ", url=" + url
                    + ", responseBytes=" + response.length);
            if (status < 200 || status >= 300) {
                throw new IOException("GCS request failed. method=GET, status=" + status + ", url=" + url
                        + ", response=" + new String(response, StandardCharsets.UTF_8));
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private String objectUrl(String bucket, String objectName) throws IOException {
        return STORAGE_BASE_URL + encodePathPart(bucket) + "/o/" + encodePathPart(objectName);
    }

    private String getAccessToken() throws IOException {
        return credentialsProvider.getAccessToken();
    }

    private static String encodePathPart(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }

    private static byte[] readFully(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

}
